/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.jcr2spi.state;

import org.apache.jackrabbit.jcr2spi.observation.InternalEventListener;
import org.apache.jackrabbit.spi.EventIterator;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * <code>WorkspaceItemStateManager</code>
 */
public class WorkspaceItemStateManager extends CachingItemStateManager
    implements InternalEventListener {

    private static Logger log = LoggerFactory.getLogger(WorkspaceItemStateManager.class);

    public WorkspaceItemStateManager(ItemStateFactory isf, IdFactory idFactory) {
        super(isf, idFactory);
    }

    //-------------------------------< InternalEventListener >------------------

    /**
     * Processes <code>events</code> and invalidates cached <code>ItemState</code>s
     * accordingly. Note that this performed for both local and non-local changes,
     * since workspace operations are reported as local changes as well and
     * might have invoked changes (autocreated items etc.).
     *
     * @param events
     * @param isLocal
     */
    public void onEvent(EventIterator events, boolean isLocal) {
        onEvent(events, isLocal, null);
    }

    public void onEvent(EventIterator events, ChangeLog changeLog) {
        if (changeLog == null) {
            throw new IllegalArgumentException("ChangeLog must not be null.");
        }
        // inform all transient states, that they have been persisted and must
        // connect to their workspace state (and eventually reload the data).
        // TODO: TOBEFIXED. only used to set status to EXISTING in order (which is probably wrong)
        changeLog.persisted();

        // inform all existing workspace states about the transient modifications
        // that have been persisted now.
        onEvent(events, true, changeLog);
    }

    private void onEvent(EventIterator events, boolean isLocal, ChangeLog changeLog) {
        // collect set of removed node ids
        Set removedNodeIds = new HashSet();
        List addEventList = new ArrayList();
        List eventList = new ArrayList();
        while (events.hasNext()) {
            Event event = events.nextEvent();
            int type = event.getType();
            if (type == Event.NODE_ADDED || event.getType() == Event.PROPERTY_ADDED) {
                addEventList.add(event);
            } else if (type == Event.NODE_REMOVED) {
                // remember removed nodes separately for proper handling later on.
                removedNodeIds.add(event.getItemId());
                eventList.add(event);
            } else {
                eventList.add(event);
            }
        }
        if (eventList.isEmpty() && addEventList.isEmpty()) {
            return;
        }

        /* process remove and change events */
        for (Iterator it = eventList.iterator(); it.hasNext(); ) {
            Event event = (Event) it.next();
            int type = event.getType();

            ItemState state = lookup(event.getItemId());
            NodeState parent = (event.getParentId() != null) ? (NodeState) lookup(event.getParentId()) : null;

            if (type == Event.NODE_REMOVED || type == Event.PROPERTY_REMOVED) {
                if (state != null) {
                    state.refresh(event, changeLog);
                }
                if (parent != null) {
                    // invalidate parent only if it has not been removed
                    // with the same event bundle.
                    if (!removedNodeIds.contains(event.getParentId())) {
                        parent.refresh(event, changeLog);
                    }
                }
            } else if (type == Event.PROPERTY_CHANGED) {
                if (state != null) {
                    try {
                        // TODO: improve.
                        /* retrieve property value and type from server even if
                           changes were issued from this session (changelog).
                           this is currently the only way to update the workspace
                           state, which is not connected to its overlaying session-state.
                        */
                        PropertyState tmp = getItemStateFactory().createPropertyState((PropertyId) state.getId(), state.getParent());
                        ((PropertyState) state).init(tmp.getType(), tmp.getValues());
                        state.refresh(event, changeLog);
                    } catch (ItemStateException e) {
                        log.error("Unexpected error while updating modified property state.", e);
                    }
                }
                // TODO: check again. parent must be notified if mixintypes or jcr:uuid prop is changed.
                if (parent != null) {
                    parent.refresh(event, changeLog);
                }
            } else {
                // should never occur
                throw new IllegalArgumentException("Invalid event type: " + event.getType());
            }
        }

        /* Add events need to be processed hierarchically, since its not possible
           to add a new child reference to a state that is not yet present in
           the state manager.
           The 'progress' flag is used to make sure, that during each loop at
           least one event has been processed and removed from the iterator.
           If this is not the case, there are not parent states present in the
           state manager that need to be updated and the remaining events may
           be ignored.
         */
        boolean progress = true;
        while (!addEventList.isEmpty() && progress) {
            progress = false;
            for (Iterator it = addEventList.iterator(); it.hasNext();) {
                Event event = (Event) it.next();
                NodeState parent = (NodeState) lookup(event.getParentId());
                if (parent != null) {
                    parent.refresh(event, changeLog);
                    it.remove();
                    progress = true;
                }
            }
        }
    }
}
