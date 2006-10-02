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
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.IdFactory;

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
        // collect set of removed node ids
        Set removedNodeIds = new HashSet();
        List eventList = new ArrayList();
        while (events.hasNext()) {
            Event e = events.nextEvent();
            eventList.add(e);
        }
        if (eventList.isEmpty()) {
            return;
        }

        for (Iterator it = eventList.iterator(); it.hasNext(); ) {
            Event e = (Event) it.next();
            ItemId itemId = e.getItemId();
            NodeId parentId = e.getParentId();
            ItemState state;
            NodeState parent;
            switch (e.getType()) {
                case Event.NODE_ADDED:
                case Event.PROPERTY_ADDED:
                    state = lookup(itemId);
                    if (state != null) {
                        // TODO: item already exists ???
                        // invalidate
                        state.refresh();
                    }
                    parent = (NodeState) lookup(parentId);
                    if (parent != null) {
                        // discard and let wsp manager reload state when accessed next time
                        parent.refresh();
                    }
                    break;
                case Event.NODE_REMOVED:
                case Event.PROPERTY_REMOVED:
                    state = lookup(itemId);
                    if (state != null) {
                        state.notifyStateDestroyed();
                    }
                    state = lookup(parentId);
                    if (state != null) {
                        parent = (NodeState) state;
                        // check if removed as well
                        if (removedNodeIds.contains(parent.getId())) {
                            // do not invalidate here
                        } else {
                            // discard and let wsp manager reload state when accessed next time
                            parent.refresh();
                        }
                    }
                    break;
                case Event.PROPERTY_CHANGED:
                    state = lookup(itemId);
                    // discard and let wsp manager reload state when accessed next time
                    if (state != null) {
                        state.refresh();
                    }
            }
        }
    }

    public void onEvent(EventIterator events, ChangeLog changeLog) {
        if (changeLog == null) {
            throw new IllegalArgumentException("ChangeLog must not be null.");
        }
        // apply the transient changes in changeLog to the ItemStates in the
        // workspace layer and synchronize the changes recorded in the changelog
        // with the events sent.
        changeLog.persist(events);
    }
}
