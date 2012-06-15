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
package org.apache.jackrabbit.core.security.authorization.acl;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.cache.GrowingLRUMap;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.authorization.AccessControlModifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Map;

/**
 * <code>CachingEntryCollector</code> extends <code>EntryCollector</code> by
 * keeping a cache of ACEs per access controlled nodeId.
 */
class CachingEntryCollector extends EntryCollector {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(CachingEntryCollector.class);

    /**
     * Cache to look up the list of access control entries defined at a given
     * nodeID (key). The map only contains an entry if the corresponding Node
     * is access controlled.
     */
    private final EntryCache cache;

    /**
     * Create a new instance.
     * 
     * @param systemSession A system session.
     * @param rootID The id of the root node.
     * @throws RepositoryException If an error occurs.
     */
    CachingEntryCollector(SessionImpl systemSession, NodeId rootID) throws RepositoryException {
        super(systemSession, rootID);
        cache = new EntryCache();
    }

    @Override
    protected void close() {
        super.close();
        cache.clear();
    }

    //-----------------------------------------------------< EntryCollector >---
    /**
     * @see EntryCollector#getEntries(org.apache.jackrabbit.core.NodeImpl)
     */
    @Override
    protected Entries getEntries(NodeImpl node) throws RepositoryException {
        NodeId nodeId = node.getNodeId();
        Entries entries = cache.get(nodeId);
        if (entries == null) {
            // fetch entries and update the cache
            entries = updateCache(node);
        }
        return entries;
    }

    /**
     * @see EntryCollector#getEntries(org.apache.jackrabbit.core.id.NodeId)
     */
    @Override
    protected Entries getEntries(NodeId nodeId) throws RepositoryException {
        Entries entries = cache.get(nodeId);
        if (entries == null) {
            // fetch entries and update the cache
            NodeImpl n = getNodeById(nodeId);
            entries = updateCache(n);
        }
        return entries;
    }

    /**
     * Read the entries defined for the specified node and update the cache
     * accordingly.
     * 
     * @param node The target node
     * @return The list of entries present on the specified node or an empty list.
     * @throws RepositoryException If an error occurs.
     */
    private Entries updateCache(NodeImpl node) throws RepositoryException {
        Entries entries = super.getEntries(node);
        if (!entries.isEmpty()) {
            // adjust the 'nextId' to point to the next access controlled
            // ancestor node instead of the parent and remember the entries.
            entries.setNextId(getNextID(node));
            cache.put(node.getNodeId(), entries);
        } // else: not access controlled -> ignore.
        return entries;
    }

    /**
     * Find the next access control ancestor in the hierarchy 'null' indicates
     * that there is no ac-controlled ancestor.
     *
     * @param node The target node for which the cache needs to be updated.
     * @return The NodeId of the next access controlled ancestor in the hierarchy
     * or null
     */
    private NodeId getNextID(NodeImpl node) throws RepositoryException {
        NodeImpl n = node;
        NodeId nextId = null;
        while (nextId == null && !isRootId(n.getNodeId())) {
            NodeId parentId = n.getParentId();
            if (cache.containsKey(parentId)) {
                nextId = parentId;
            } else {
                NodeImpl parent = (NodeImpl) n.getParent();
                if (hasEntries(parent)) {
                    nextId = parentId;
                } else {
                    // try next ancestor
                    n = parent;
                }
            }
        }
        return nextId;
    }

    /**
     * Returns {@code true} if the specified {@code nodeId} is the ID of the
     * root node; false otherwise.
     *
     * @param nodeId The identifier of the node to be tested.
     * @return {@code true} if the given id is the identifier of the root node.
     */
    private boolean isRootId(NodeId nodeId) {
        return rootID.equals(nodeId);
    }

    /**
     * Evaluates if the given node is access controlled and holds a non-empty
     * rep:policy child node.
     * 
     * @param n The node to test.
     * @return true if the specified node is access controlled and holds a
     * non-empty policy child node.
     * @throws RepositoryException If an error occurs.
     */
    private static boolean hasEntries(NodeImpl n) throws RepositoryException {
        if (ACLProvider.isAccessControlled(n)) {
            NodeImpl aclNode = n.getNode(N_POLICY);
            return aclNode.hasNodes();
        }

        // no ACL defined here
        return false;
    }

    /**
     * @see EntryCollector#notifyListeners(org.apache.jackrabbit.core.security.authorization.AccessControlModifications)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void notifyListeners(AccessControlModifications modifications) {
        /* Update cache for all affected access controlled nodes */
        for (Object key : modifications.getNodeIdentifiers()) {
            if (!(key instanceof NodeId)) {
                log.warn("Cannot process AC modificationMap entry. Keys must be NodeId.");
                continue;
            }
            NodeId nodeId = (NodeId) key;
            int type = modifications.getType(nodeId);
            if ((type & POLICY_ADDED) == POLICY_ADDED) {
                // clear the complete cache since the nextAcNodeId may
                // have changed due to the added ACL.
                log.debug("Policy added, clearing the cache");
                cache.clear();
                break; // no need for further processing.
            } else if ((type & POLICY_REMOVED) == POLICY_REMOVED) {
                // clear the entry and change the entries having a nextID
                // pointing to this node.
                cache.remove(nodeId, true);
            } else if ((type & POLICY_MODIFIED) == POLICY_MODIFIED) {
                // simply clear the cache entry -> reload upon next access.
                cache.remove(nodeId, false);
            } else if ((type & MOVE) == MOVE) {
                // some sort of move operation that may affect the cache
                log.debug("Move operation, clearing the cache");
                cache.clear();
                break; // no need for further processing.
            }
        }
        super.notifyListeners(modifications);
    }

    //--------------------------------------------------------------------------
    /**
     * A cache to lookup the ACEs defined on a given (access controlled)
     * node. The internal map uses the ID of the node as key while the value
     * consists of {@Entries} objects that not only provide the ACEs defined
     * for that node but also the ID of the next access controlled parent node.
     */
    private class EntryCache {

        private final Map<NodeId, Entries> cache;
        private Entries rootEntries;

        @SuppressWarnings("unchecked")
        public EntryCache() {
            int maxsize = 5000;
            String propname = "org.apache.jackrabbit.core.security.authorization.acl.CachingEntryCollector.maxsize";
            try {
                maxsize = Integer.parseInt(System.getProperty(propname, Integer.toString(maxsize)));
            } catch (NumberFormatException ex) {
                log.debug("Parsing system property " + propname + " with value: " + System.getProperty(propname), ex);
            }

            log.info("Creating cache with max size of: " + maxsize);

            cache = new GrowingLRUMap(1024, maxsize);
        }

        public boolean containsKey(NodeId id) {
            if (isRootId(id)) {
                return rootEntries != null;
            } else {
                synchronized (cache) {
                    return cache.containsKey(id);
                }
            }
        }

        public void clear() {
            rootEntries = null;
            synchronized (cache) {
                cache.clear();
            }
        }

        public Entries get(NodeId id) {
            Entries result;

            if (isRootId(id)) {
                result = rootEntries;
            } else {
                synchronized (cache) {
                    result = cache.get(id);
                }
            }

            if (result != null) {
                log.debug("Cache hit for nodeId {}", id);
            } else {
                log.debug("Cache miss for nodeId {}", id);
            }

            return result;
        }

        public void put(NodeId id, Entries entries) {
            log.debug("Updating cache for nodeId {}", id);

            // fail early on potential cache corruption
            if (id.equals(entries.getNextId())) {
                throw new IllegalArgumentException("Trying to update cache entry for " + id + " with a circular reference");
            }

            if (isRootId(id)) {
                rootEntries = entries;
            } else {
                synchronized (cache) {
                    cache.put(id, entries);
                }
            }
        }

        public void remove(NodeId id, boolean adjustNextIds) {
            log.debug("Removing nodeId {} from cache", id);
            Entries result;
            synchronized (cache) {
                if (isRootId(id)) {
                    result = rootEntries;
                    rootEntries = null;
                } else {
                    result = cache.remove(id);
                }

                if (adjustNextIds && result != null) {
                    NodeId nextId = result.getNextId();
                    for (Entries entry : cache.values()) {
                        if (id.equals(entry.getNextId())) {
                            // fail early on potential cache corruption
                            if (id.equals(nextId)) {
                                throw new IllegalArgumentException("Trying to update cache entry for " + id + " with a circular reference");
                            }
                            entry.setNextId(nextId);
                        }
                    }
                }
            }
        }
    }
}