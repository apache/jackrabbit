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

import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

/**
 * <code>CachingItemStateManager</code> implements an {@link ItemStateManager}
 * and decorates it with a caching facility.
 */
public class CachingItemStateManager implements ItemStateManager {

    /**
     * The logger instance for this class.
     */
    private static Logger log = LoggerFactory.getLogger(CachingItemStateManager.class);

    /**
     * The item state factory to create <code>ItemState</code>s that are not
     * present in the cache.
     */
    private final ItemStateFactory isf;

    /**
     * Cache
     */
    private final ItemStateCache cache;

    /**
     * The root node of the workspace or <code>null</code> if it has not been
     * retrieved yet.
     */
    private NodeState root;

    /**
     * The Id factory.
     */
    private final IdFactory idFactory;

    /**
     * Creates a new <code>CachingItemStateManager</code>.
     *
     * @param isf       the item state factory to create item state instances.
     * @param idFactory the id factory.
     */
    public CachingItemStateManager(ItemStateFactory isf, IdFactory idFactory) {
        this.isf = isf;
        this.idFactory = idFactory;
        this.cache = new ItemStateCache();

        isf.setCache(cache);
    }

    //---------------------------------------------------< ItemStateManager >---

    public NodeState getRootState() throws ItemStateException {
        if (root == null) {
            root = isf.createRootState(this);
        }
        return root;
    }

    /**
     * @inheritDoc
     * @see ItemStateManager#getItemState(ItemId)
     */
    public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
        return resolve(id);
    }

    /**
     * @inheritDoc
     * @see ItemStateManager#hasItemState(ItemId)
     */
    public boolean hasItemState(ItemId id) {
        try {
            resolve(id);
        } catch (ItemStateException e) {
            return false;
        }
        return true;
    }

    /**
     * @inheritDoc
     * @see ItemStateManager#getReferingStates(NodeState)
     * @param nodeState
     */
    public Collection getReferingStates(NodeState nodeState) throws ItemStateException {
        Set refStates = new HashSet();
        Iterator it =  nodeState.getNodeReferences().iterator();
        while (it.hasNext()) {
            PropertyId pId = (PropertyId) it.next();
            refStates.add(getItemState(pId));
        }
        if (refStates.isEmpty()) {
            return Collections.EMPTY_SET;
        } else {
            return Collections.unmodifiableCollection(refStates);
        }
    }

    /**
     * @inheritDoc
     * @see ItemStateManager#hasReferingStates(NodeState)
     * @param nodeState
     */
    public boolean hasReferingStates(NodeState nodeState) {
        NodeReferences nr = nodeState.getNodeReferences();
        return !nr.isEmpty();
    }

    //------------------------------< internal >--------------------------------

    /**
     * @return the item state factory of this <code>ItemStateManager</code>.
     */
    protected final ItemStateFactory getItemStateFactory() {
        return isf;
    }

    /**
     * Resolves the id into an <code>ItemState</code>.
     *
     * @param id the id of the <code>ItemState</code> to resolve.
     * @return the <code>ItemState</code>.
     * @throws NoSuchItemStateException if there is no <code>ItemState</code>
     *                                  with <code>id</code>
     * @throws ItemStateException       if any other error occurs.
     */
    private ItemState resolve(ItemId id) throws NoSuchItemStateException, ItemStateException {
        String uuid = id.getUUID();
        Path path = id.getPath();

        NodeState nodeState;
        // resolve uuid part
        if (uuid != null) {
            nodeState = cache.getNodeState(uuid);
            if (nodeState == null) {
                // state identified by the uuid is not yet cached -> get from ISF
                NodeId refId = (path == null) ? (NodeId) id : idFactory.createNodeId(uuid);
                nodeState = isf.createNodeState(refId, this);
            }
        } else {
            // start with root node if no uuid part in id
            nodeState = getRootState();
        }

        ItemState s = nodeState;
        if (path != null) {
            s = PathResolver.resolve(nodeState, path);
        }
        return s;
    }

    /**
     * Looks up the <code>ItemState</code> with id but does not try to load the
     * item if it is not present in the cache.
     *
     * @param id the id of the ItemState to look up.
     * @return the cached <code>ItemState</code> or <code>null</code> if it is not
     *         present in the cache.
     */
    protected ItemState lookup(ItemId id) {
        NodeState start;
        // resolve UUID
        if (id.getUUID() != null) {
            start = cache.getNodeState(id.getUUID());
            if (start == null) {
                // not cached
                return null;
            }
        } else {
            // start from root
            try {
                start = getRootState();
            } catch (ItemStateException e) {
                // should never occur
                log.error("Error while retrieving root node state:" + e.getMessage());
                return null;
            }
        }

        if (id.getPath() == null) {
            // path is null -> id points to a state identified by uuid
            return start;
        } else {
            // resolve path part
            try {
                return PathResolver.lookup(start, id.getPath());
            } catch (NoSuchItemStateException e) {
                log.debug("exception while looking up state with id: " + id);
                return null;
            } catch (ItemStateException e) {
                log.debug("exception while looking up state with id: " + id);
                return null;
            }
        }
    }
}
