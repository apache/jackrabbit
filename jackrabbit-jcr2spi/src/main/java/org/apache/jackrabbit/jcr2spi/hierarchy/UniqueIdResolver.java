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

import org.apache.jackrabbit.jcr2spi.state.ItemStateCreationListener;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateLifeCycleListener;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.jcr2spi.state.ItemStateFactory;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import java.util.Map;

/**
 * <code>UniqueIdResolver</code> allows to retrieve <code>NodeEntry</code> instances
 * that are identified by a uniqueID.
 */
public class UniqueIdResolver implements ItemStateCreationListener, EntryFactory.NodeEntryListener {

    private static Logger log = LoggerFactory.getLogger(UniqueIdResolver.class);

    private final ItemStateFactory isf;

    /**
     * Maps a String uniqueID to a {@link NodeEntry}.
     */
    private final Map<String, NodeEntry> lookUp;

    /**
     * Creates a new <code>UniqueIdResolver</code>.
     */
    public UniqueIdResolver(ItemStateFactory isf) {
        this.lookUp = new ReferenceMap<>(ReferenceStrength.HARD, ReferenceStrength.SOFT);
        this.isf = isf;
        isf.addCreationListener(this);
    }

    public void dispose() {
        isf.removeCreationListener(this);
        lookUp.clear();
    }

    public NodeEntry lookup(String uniqueId) {
        if (uniqueId == null) {
            throw new IllegalArgumentException();
        }
        return lookUp.get(uniqueId);
    }

    public NodeEntry resolve(NodeId nodeId, NodeEntry rootEntry) throws ItemNotFoundException, RepositoryException {
        NodeEntry entry = lookup(nodeId.getUniqueID());
        if (entry == null) {
            NodeState state = isf.createDeepNodeState(nodeId, rootEntry);
            entry = state.getNodeEntry();
        }
        return entry;
    }

    //-----------------------------------------< ItemStateLifeCycleListener >---
    /**
     * Updates the internal id-lookup if the given state
     * - is identify by a uniqueID and got removed
     * - was modified and now is identified by a uniqueID
     * - was modified and is not identified by a uniqueID any more
     *
     * @param state
     * @param previousStatus
     * @see ItemStateLifeCycleListener#statusChanged(ItemState, int)
     */
    public void statusChanged(ItemState state, int previousStatus) {
        synchronized (lookUp) {
            if (Status.isTerminal((state.getStatus()))) {
                if (state.isNode()) {
                    NodeEntry entry = (NodeEntry) state.getHierarchyEntry();
                    String uniqueID = entry.getUniqueID();
                    if (uniqueID != null) {
                        NodeEntry mapEntry = lookUp.get(uniqueID);
                        if (mapEntry == entry) {
                            lookUp.remove(uniqueID);
                        } // else: removed entry is not present in lookup but
                          //       only it's replacement -> ignore
                    }
                }
                // stop listening if a state reached status REMOVED.
                if (Status.REMOVED == state.getStatus()) {
                    state.removeListener(this);
                }
            } // else: any other status than REMOVED -> ignore.
        }
    }

    //------------------------------------------< ItemStateCreationListener >---
    /**
     * Nothing to do. The lookUp is filled entry creation and/or modification
     * of its uniqueID
     *
     * @param state
     * @see ItemStateCreationListener#created(ItemState)
     */
    public void created(ItemState state) {
        if (state.isNode()) {
            NodeEntry entry = (NodeEntry) state.getHierarchyEntry();
            String uniqueID = entry.getUniqueID();
            if (uniqueID != null) {
                if (!lookUp.containsKey(uniqueID) || lookUp.get(uniqueID) != entry) {
                    log.error("Created NodeState identified by UniqueID that is not contained in the lookup.");
                }
            }
        }
    }

    //-------------------------------------< EntryFactory.NodeEntryListener >---
    /**
     * @see EntryFactory.NodeEntryListener#entryCreated(NodeEntry)
     */
    public void entryCreated(NodeEntry entry) {
        synchronized (lookUp) {
            String uniqueID = entry.getUniqueID();
            if (uniqueID != null) {
                putToLookup(uniqueID, entry);
            }
        }
    }

    /**
     * @see EntryFactory.NodeEntryListener#uniqueIdChanged(NodeEntry, String)
     */
    public void uniqueIdChanged(NodeEntry entry, String previousUniqueID) {
        synchronized (lookUp) {
            if (previousUniqueID != null) {
                Object previous = lookUp.get(previousUniqueID);
                if (previous == entry) {
                    lookUp.remove(previousUniqueID);
                } // else: previousUniqueID points to another entry -> ignore
            }
            String uniqueID = entry.getUniqueID();
            if (uniqueID != null) {
                putToLookup(uniqueID, entry);
            }
        }
    }

    //------------------------------------------------------------< private >---
    private void putToLookup(String uniqueID, NodeEntry entry) {
        Object previous = lookUp.put(uniqueID, entry);
        if (previous != null) {
            // some other entry existed before with the same uniqueID
            if (!sameEntry((NodeEntry) previous, entry)) {
                // if the new entry represents the externally moved/renamed
                // correspondence of the previous the latter needs to marked
                // removed/stale-destroyed.
                // otherwise (both represent the same entry) the creation
                // of entry is the result of gc of the node or any of the
                // ancestors. in this case there is not need to 'remove'
                // the previous entry. instead it is just removed from this
                // cache and left for collection.
                ((NodeEntry) previous).remove();
            } else {
                log.debug("Replacement of NodeEntry identified by UniqueID");
            }
        }
    }

    private static boolean sameEntry(NodeEntry previous, NodeEntry entry) {
        if (previous == entry) {
            return true;
        } else if (Status.REMOVED != previous.getStatus() &&
                previous.getName().equals(entry.getName())) {

            NodeEntry parent = previous.getParent();
            NodeEntry parent2 = entry.getParent();
            if (parent == parent2) {
                return true;
            } else {
                try {
                    return parent.getPath().equals(parent2.getPath());
                } catch (RepositoryException e) {
                    // TODO: add some fallback
                }
            }
        }
        return false;
    }
}
