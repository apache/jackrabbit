/*
 * $Id$
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.jackrabbit.jcr2spi.hierarchy;

import org.apache.jackrabbit.jcr2spi.state.ItemStateCreationListener;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateLifeCycleListener;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.jcr2spi.state.ItemStateFactory;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.NoSuchItemStateException;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.commons.collections.map.ReferenceMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.Map;
import java.util.Iterator;

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
    private final Map lookUp;

    /**
     * Creates a new <code>UniqueIdResolver</code>.
     */
    public UniqueIdResolver(ItemStateFactory isf) {
        this.lookUp = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
        this.isf = isf;
        isf.addCreationListener(this);
    }

    public void dispose() {
        isf.removeCreationListener(this);
        lookUp.clear();
    }

    public NodeEntry lookup(NodeId nodeId) {
        if (nodeId.getPath() != null) {
            throw new IllegalArgumentException();
        }
        NodeEntry entry = (NodeEntry) lookUp.get(nodeId.getUniqueID());
        return (entry != null) ? entry : null;
    }

    public NodeEntry resolve(NodeId nodeId, NodeEntry rootEntry) throws PathNotFoundException, RepositoryException {
        NodeEntry entry = lookup(nodeId);
        if (entry == null) {
            try {
                NodeState state = isf.createDeepNodeState(nodeId, rootEntry);
                entry = state.getNodeEntry();
            } catch (NoSuchItemStateException e) {
                throw new PathNotFoundException(e);
            } catch (ItemStateException e) {
                throw new RepositoryException(e);
            }
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
        if (Status.isTerminal(state.getStatus())) {
            if (state.isNode()) {
                NodeState nodeState = (NodeState) state;
                String uniqueID = nodeState.getUniqueID();
                if (uniqueID != null) {
                    lookUp.remove(uniqueID);
                }
            }
            state.removeListener(this);
        } else {
            putToCache(state);
        }
    }

    //------------------------------------------< ItemStateCreationListener >---
    /**
     * Updates the internal id-lookup if the created state is a NodeState that
     * is identified by a uniqueID.
     *
     * @param state
     * @see ItemStateCreationListener#created(ItemState)
     */
    public void created(ItemState state) {
        putToCache(state);
    }

    //-------------------------------------< EntryFactory.NodeEntryListener >---
    /**
     * @see EntryFactory.NodeEntryListener#entryCreated(NodeEntry)
     */
    public void entryCreated(NodeEntry entry) {
        String uniqueID = entry.getUniqueID();
        if (uniqueID != null) {
            Object previous = lookUp.put(uniqueID, entry);
            if (previous != null) {
                ((NodeEntry) previous).remove();
            }
        }
    }

    /**
     * @see EntryFactory.NodeEntryListener#uniqueIdChanged(NodeEntry, String)
     */
    public void uniqueIdChanged(NodeEntry entry, String previousUniqueID) {
        synchronized (lookUp) {
            if (previousUniqueID != null) {
                lookUp.remove(previousUniqueID);
            }
            String uniqueID = entry.getUniqueID();
            if (uniqueID != null) {
                Object previous = lookUp.put(uniqueID, entry);
                if (previous != null && previous != entry) {
                    // some other entry existed before with the same uniqueID
                    ((NodeEntry) previous).remove();
                }
            }
        }
    }
    //------------------------------------------------------------< private >---
    /**
     * Put the given <code>ItemState</code> in the internal cache.
     *
     * @param state
     */
    private void putToCache(ItemState state) {
        if (!state.isNode()) {
            return;
        }

        if (state.getStatus() == Status.EXISTING || state.getStatus() == Status.MODIFIED) {
            NodeEntry entry = ((NodeState)state).getNodeEntry();
            // NOTE: uniqueID is retrieved from the state and not from the NodeId.
            String uniqueID = entry.getUniqueID();
            synchronized (lookUp) {
                if (uniqueID != null) {
                    if (lookUp.get(uniqueID) != entry) {
                        lookUp.put(uniqueID, entry);
                    }
                } else {
                    // ev. uniqueID was removed -> remove the entry from the lookUp
                    if (lookUp.containsValue(entry)) {
                        for (Iterator it = lookUp.entrySet().iterator(); it.hasNext();) {
                            Map.Entry next = (Map.Entry) it.next();
                            if (next.getValue() == entry) {
                                it.remove();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
