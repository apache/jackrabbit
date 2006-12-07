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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.NodeId;

import java.util.Map;

/**
 * <code>ItemStateCache</code>...
 */
public class ItemStateCache implements ItemStateCreationListener {

    private static Logger log = LoggerFactory.getLogger(ItemStateCache.class);

    /**
     * Maps a String uuid to a {@link NodeState}.
     */
    private final Map uniqueId2NodeState;

    /**
     * Creates a new <code>CachingItemStateManager</code>.
     *
     */
    public ItemStateCache() {
        this.uniqueId2NodeState = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
    }


    public NodeState getNodeState(String uniqueID) {
        return (NodeState) uniqueId2NodeState.get(uniqueID);
    }

    public NodeState getNodeState(NodeId nodeId) {
        String uid = nodeId.getUniqueID();
        if (uid != null && nodeId.getPath() == null) {
            return getNodeState(uid);
        } else {
            // TODO: missing caching for NodeState that are not only identified by uuid.
            return null;
        }
    }

    public PropertyState getPropertyState(PropertyId propertyId) {
        // TODO: missing caching.
        return null;
    }

    //-----------------------------------------< ItemStateLifeCycleListener >---

    /**
     * Updates the internal cache
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
                    uniqueId2NodeState.remove(uniqueID);
                }
            }
            state.removeListener(this);
        } else {
            putToCache(state);
        }
    }

    //------------------------------------------< ItemStateCreationListener >---
    /**
     * Updates the internal cache
     *
     * @param state
     * @see ItemStateCreationListener#created(ItemState)
     */
    public void created(ItemState state) {
        putToCache(state);
    }

    /**
     * Put the given <code>ItemState</code> in the internal cache.
     *
     * @param state
     */
    private void putToCache(ItemState state) {
        if (state.isNode() && (state.getStatus() == Status.EXISTING || state.getStatus() == Status.MODIFIED)) {
            NodeState nodeState = (NodeState) state;
            // NOTE: uniqueID is retrieved from the state and not from the NodeId.
            String uniqueID = nodeState.getUniqueID();
            if (uniqueID != null) {
                uniqueId2NodeState.put(uniqueID, nodeState);
            }
        }
        // TODO: add caching for other items as well
    }
}