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
package org.apache.jackrabbit.jcr2spi.hierarchy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.jcr2spi.WorkspaceManager;
import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.jcr2spi.observation.InternalEventListener;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>HierarchyEventListener</code>...
 */
public class HierarchyEventListener implements InternalEventListener {

    private static Logger log = LoggerFactory.getLogger(HierarchyEventListener.class);

    private final HierarchyManager hierarchyMgr;
    private final Collection<EventFilter> eventFilter;

    public HierarchyEventListener(WorkspaceManager wspManager,
                                  HierarchyManager hierarchyMgr,
                                  CacheBehaviour cacheBehaviour) {
        this.hierarchyMgr = hierarchyMgr;
        if (cacheBehaviour == CacheBehaviour.OBSERVATION) {
            EventFilter filter = null;
            try {
                // listen to all events except 'local' ones
                Path root = wspManager.getPathFactory().getRootPath();
                filter = wspManager.createEventFilter(Event.ALL_TYPES, root, true, null, null, true);
            } catch (RepositoryException e) {
                // SPI does not support observation, or another error occurred.
                log.debug("Creating event filter for cache behavior observation failed", e);
            }
            if (filter == null) {
                this.eventFilter = Collections.emptyList();
            } else {
                this.eventFilter = Collections.singletonList(filter);
            }
        } else {
            this.eventFilter = Collections.emptyList();
        }
    }

    //----------------------------------------------< InternalEventListener >---
    /**
     * @see InternalEventListener#getEventFilters()
     */
    public Collection<EventFilter> getEventFilters() {
        return eventFilter;
    }

    /**
     * Processes <code>events</code> and invalidates cached <code>ItemState</code>s
     * accordingly. Note that this is performed for local changes only,
     * since workspace operations are reported as local and have been applied already.
     *
     * @param eventBundle the events.
     * @see InternalEventListener#onEvent(EventBundle)
     */
    public void onEvent(EventBundle eventBundle) {
        if (eventBundle.isLocal()) {
            log.debug("Local event bundle -> not processed by HierarchyEventListener.");
            return;
        }
        pushEvents(getEventCollection(eventBundle));
    }

    /**
     * Retrieve the workspace state(s) affected by the given event and refresh
     * them accordingly.
     *
     * @param events the events to process.
     */
    private void pushEvents(Collection<Event> events) {
        if (events.isEmpty()) {
            log.debug("Empty event bundle");
            return;
        }

        // TODO: handle new 283 event types and clean add/remove that is also present as move-event.

        // collect set of removed node ids
        Set<ItemId> removedEvents = new HashSet<ItemId>();
        // separately collect the add events
        Set<Event> addEvents = new HashSet<Event>();

        for (Iterator<Event> it = events.iterator(); it.hasNext();) {
            Event event = it.next();
            int type = event.getType();
            if (type == Event.NODE_REMOVED) {
                // remember removed nodes separately for proper handling later on.
                removedEvents.add(event.getItemId());
            } else if (type == Event.NODE_ADDED || type == Event.PROPERTY_ADDED) {
                addEvents.add(event);
                it.remove();
            }
        }

        /* Process ADD-events.
           In case of persisting transients modifications, the event-set may
           still contain events that are not covered by the changeLog such as
           new version-history or other autocreated properties and nodes.

           Add events need to be processed hierarchically, since its not possible
           to add a new child reference to a state that is not yet present in
           the state manager.
           The 'progress' flag is used to make sure, that during each loop at
           least one event has been processed and removed from the iterator.
           If this is not the case, there are not parent states present in the
           state manager that need to be updated and the remaining events may
           be ignored.
         */
        boolean progress = true;
        while (!addEvents.isEmpty() && progress) {
            progress = false;
            for (Iterator<Event> it = addEvents.iterator(); it.hasNext();) {
                Event ev = it.next();
                NodeId parentId = ev.getParentId();
                HierarchyEntry parent = null;
                if (parentId != null) {
                    parent = hierarchyMgr.lookup(parentId);
                    if (parent == null && ev.getPath() != null && parentId.getUniqueID() != null) {
                        // parentID contains a uniqueID part -> try to lookup
                        // the parent by path.
                        try {
                            Path parentPath = ev.getPath().getAncestor(1);
                            parent = hierarchyMgr.lookup(parentPath);
                        } catch (RepositoryException e) {
                            // should not occur
                            log.debug(e.getMessage());
                        }
                    }
                }
                if (parent != null && parent.denotesNode()) {
                    ((NodeEntry) parent).refresh(ev);
                    it.remove();
                    progress = true;
                }
            }
        }

        /* process all other events (removal, property changed) */
        for (Event event : events) {
            int type = event.getType();

            NodeId parentId = event.getParentId();
            NodeEntry parent = (parentId != null) ? (NodeEntry) hierarchyMgr.lookup(parentId) : null;
            switch (type) {
                case Event.NODE_REMOVED:
                case Event.PROPERTY_REMOVED:
                    // notify parent about removal if its child-entry.
                    // - if parent is 'null' (i.e. not yet loaded) the child-entry does
                    //   not exist either -> no need to inform child-entry
                    // - if parent got removed with the same event-bundle
                    //   only remove the parent an skip this event.
                    if (parent != null && !removedEvents.contains(parentId)) {
                        parent.refresh(event);
                    }
                    break;
                case Event.PROPERTY_CHANGED:
                    // notify parent in case jcr:mixinTypes or jcr:uuid was changed.
                    // if parent is 'null' (i.e. not yet loaded) the prop-entry does
                    // not exist either -> no need to inform propEntry
                    if (parent != null) {
                        parent.refresh(event);
                    }
                    break;
                case Event.NODE_MOVED:
                    // TODO: implementation missing
                    throw new UnsupportedOperationException("Implementation missing");
                    //break;
                case Event.PERSIST:
                    // TODO: implementation missing
                    throw new UnsupportedOperationException("Implementation missing");
                    //break;
                default:
                    // should never occur
                    throw new IllegalArgumentException("Invalid event type: " + event.getType());
            }
        }
    }

    private static Collection<Event> getEventCollection(EventBundle eventBundle) {
        List<Event> evs = new ArrayList<Event>();
        for (Iterator<Event> it = eventBundle.getEvents(); it.hasNext();) {
           evs.add(it.next());
        }
        return evs;
    }
}
