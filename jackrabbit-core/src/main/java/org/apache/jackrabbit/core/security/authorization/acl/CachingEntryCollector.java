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
import javax.jcr.security.AccessControlEntry;
import java.util.List;
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
    private final Map<NodeId, CacheEntry> cache;
    private final Object monitor = new Object();

    /**
     * Create a new instance.
     *
     * @param systemSession A system session.
     * @param rootID The id of the root node.
     * @throws RepositoryException If an error occurs.
     */
    @SuppressWarnings("unchecked")    
    CachingEntryCollector(SessionImpl systemSession, NodeId rootID) throws RepositoryException {
        super(systemSession, rootID);

        cache = new GrowingLRUMap(1024, 5000);
    }

    @Override
    protected void close() {
        super.close();
        synchronized (monitor) {
            cache.clear();
        }
    }

    //-----------------------------------------------------< EntryCollector >---
    /**
     * @see EntryCollector#getEntries(org.apache.jackrabbit.core.NodeImpl)
     */
    @Override    
    protected List<AccessControlEntry> getEntries(NodeImpl node) throws RepositoryException {
        List<AccessControlEntry> entries;
        NodeId nodeId = node.getNodeId();
        synchronized (monitor) {
            CacheEntry ce = cache.get(nodeId);
            if (ce != null) {
                entries = ce.entries;
            } else {
                // fetch entries and update the cache
                entries = updateCache(node);
            }
        }
        return entries;
    }
    
    /**
     * @see EntryCollector#getEntries(org.apache.jackrabbit.core.id.NodeId)
     */
    @Override
    protected List<AccessControlEntry> getEntries(NodeId nodeId) throws RepositoryException {
        List<AccessControlEntry> entries;
        synchronized (monitor) {
            CacheEntry ce = cache.get(nodeId);
            if (ce != null) {
                entries = ce.entries;
            } else {
                // fetch entries and update the cache
                NodeImpl n = getNodeById(nodeId);
                entries = updateCache(n);
            }
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
    private List<AccessControlEntry> updateCache(NodeImpl node) throws RepositoryException {
        List<AccessControlEntry> entries = super.getEntries(node);
        if (!entries.isEmpty()) {
            // find the next access control ancestor in the hierarchy
            // 'null' indicates that there is no ac-controlled ancestor.
            NodeId nextId = null;
            NodeImpl n = node;            
            while (nextId == null && !rootID.equals(n.getNodeId())) {
                if (cache.containsKey(n.getNodeId())) {
                    nextId = n.getNodeId();
                } else if (cache.containsKey(n.getParentId())) {
                    nextId = n.getParentId();
                } else {
                    n = (NodeImpl) n.getParent();
                    if (hasEntries(n)) {
                        nextId = n.getNodeId();
                    } // else: not access controlled -> test next ancestors
                }
            }

            // build a new cacheEntry and add it to the cache
            CacheEntry ce = new CacheEntry(entries, nextId);
            cache.put(node.getNodeId(), ce);
            
            log.debug("Update cache for node with ID {0}: {1}", node, ce);
        } // else: not access controlled -> ignore.
        return entries;
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

        // no acl defined here
        return false;
    }

    /**
     * Returns the id of the next access-controlled ancestor if the specified
     * is contained in the cache. Otherwise the method of the super-class is called.
     *
     * @param nodeId The id of the node.
     * @return the id of the next access-controlled ancestor if the specified
     * is contained in the cache; otherwise the id of the parent.
     * @throws RepositoryException
     * @see EntryCollector#getParentId(org.apache.jackrabbit.core.id.NodeId)
     */
    @Override
    protected NodeId getParentId(NodeId nodeId) throws RepositoryException {
        synchronized (monitor) {
            CacheEntry ce = cache.get(nodeId);
            if (ce != null) {
                return ce.nextAcNodeId;
            } else {
                // no cache entry
                return super.getParentId(nodeId);
            }
        }
    }

    /**
     * @see EntryCollector#notifyListeners(org.apache.jackrabbit.core.security.authorization.AccessControlModifications)
     */
    @Override
    public void notifyListeners(AccessControlModifications modifications) {
        /* Update cache for all affected access controlled nodes */
        for (Object key : modifications.getNodeIdentifiers()) {
            if (!(key instanceof NodeId)) {
                log.warn("Cannot process AC modificationMap entry. Keys must be NodeId.");
                continue;
            }
            NodeId nodeId = (NodeId) key;
            int type = modifications.getType(nodeId);
            synchronized (monitor) {
                if ((type & POLICY_ADDED) == POLICY_ADDED) {
                    // clear the complete cache since the nextAcNodeId may
                    // have changed due to the added acl.
                    cache.clear();
                    break; // no need for further processing.
                } else if ((type & POLICY_REMOVED) == POLICY_REMOVED) {
                    // clear the entry and change the entries having a nextID
                    // pointing to this node.
                    CacheEntry ce = cache.remove(nodeId);
                    if (ce != null) {
                        NodeId nextId = ce.nextAcNodeId;
                        for (CacheEntry entry : cache.values()) {
                            if (nodeId.equals(entry.nextAcNodeId)) {
                                entry.nextAcNodeId = nextId;
                            }
                        }
                    }
                } else if ((type & POLICY_MODIFIED) == POLICY_MODIFIED) {
                    // simply clear the cache entry -> reload upon next access.
                    cache.remove(nodeId);
                } else if ((type & MOVE) == MOVE) {
                    // some sort of move operation that may affect the cache
                    cache.clear();
                    break; // no need for further processing.
                }
            }
        }
        super.notifyListeners(modifications);
    }

    //--------------------------------------------------------------------------
    /**
     *
     */
    private static class CacheEntry {

        private final List<AccessControlEntry> entries;
        private NodeId nextAcNodeId;

        private CacheEntry(List<AccessControlEntry> entries, NodeId nextAcNodeId) {
            this.entries = entries;
            this.nextAcNodeId = nextAcNodeId;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("size = ").append(entries.size()).append(", ");
            sb.append("nextAcNodeId = ").append(nextAcNodeId);
            return sb.toString();
        }
    }
}