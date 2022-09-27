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
package org.apache.jackrabbit.jcr2spi.nodetype;

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.commons.nodetype.NodeTypeStorage;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.util.Map;
import java.util.Iterator;
import java.util.WeakHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>NodeTypeCache</code> implements a cache for <code>QNodeTypeDefinition</code>s
 * on a userId basis.
 */
public class NodeTypeCache {

    /**
     * The caches per repository service instance
     */
    private static final Map<RepositoryService, Map<String, NodeTypeCache>> CACHES_PER_SERVICE = new WeakHashMap<RepositoryService, Map<String, NodeTypeCache>>();

    /**
     * Maps node type Names to QNodeTypeDefinition
     */
    private final Map<Name, QNodeTypeDefinition> nodeTypes = new HashMap<Name, QNodeTypeDefinition>();

    /**
     * @param service the repository service.
     * @param userId  the userId. If <code>null</code> this method will return a
     *                new cache instance for each such call.
     * @return the <code>NodeTypeCache</code> instance for the given
     *         <code>service</code> and <code>userId</code>.
     */
    public static NodeTypeCache getInstance(RepositoryService service, String userId) {
        // if no userId is provided do not keep the cache
        if (userId == null) {
            return new NodeTypeCache();
        }
        Map<String, NodeTypeCache> caches;
        synchronized (CACHES_PER_SERVICE) {
            caches = CACHES_PER_SERVICE.get(service);
            if (caches == null) {
                // use soft references for the node type caches
                caches = new ReferenceMap<>(ReferenceStrength.HARD, ReferenceStrength.SOFT);
                CACHES_PER_SERVICE.put(service, caches);
            }
        }
        synchronized (caches) {
            NodeTypeCache cache = caches.get(userId);
            if (cache == null) {
                cache = new NodeTypeCache();
                caches.put(userId, cache);
            }
            return cache;
        }
    }

    private NodeTypeCache() {
    }

    /**
     * Returns an Iterator over all node type definitions registered.
     *
     * @return
     * @throws javax.jcr.RepositoryException
     */
    public Iterator<QNodeTypeDefinition> getAllDefinitions(NodeTypeStorage storage)
            throws RepositoryException {
        Map<Name, QNodeTypeDefinition> allNts = new HashMap<Name, QNodeTypeDefinition>();
        for (Iterator<QNodeTypeDefinition> it = storage.getAllDefinitions(); it.hasNext(); ) {
            QNodeTypeDefinition def = it.next();
            allNts.put(def.getName(), def);
        }
        // update the cache
        synchronized (nodeTypes) {
            nodeTypes.clear();
            nodeTypes.putAll(allNts);
        }
        return allNts.values().iterator();
    }

    /**
     * Returns the <code>QNodeTypeDefinition</code>s for the given node type
     * names. The implementation is free to return additional definitions e.g.
     * dependencies.
     *
     * @param nodeTypeNames
     * @return
     * @throws javax.jcr.nodetype.NoSuchNodeTypeException
     * @throws RepositoryException
     */
    public Iterator<QNodeTypeDefinition> getDefinitions(NodeTypeStorage storage, Name[] nodeTypeNames)
            throws NoSuchNodeTypeException, RepositoryException {
        List<QNodeTypeDefinition> nts = new ArrayList<QNodeTypeDefinition>();
        List<Name> missing = null;
        synchronized (nodeTypes) {
            for (int i = 0; i < nodeTypeNames.length; i++) {
                QNodeTypeDefinition def = nodeTypes.get(nodeTypeNames[i]);
                if (def == null) {
                    if (missing == null) {
                        missing = new ArrayList<Name>();
                    }
                    missing.add(nodeTypeNames[i]);
                } else {
                    nts.add(def);
                }
            }
        }
        if (missing != null) {
            Name[] ntNames = missing.toArray(new Name[missing.size()]);
            Iterator<QNodeTypeDefinition> it = storage.getDefinitions(ntNames);
            synchronized (nodeTypes) {
                while (it.hasNext()) {
                    QNodeTypeDefinition def = it.next();
                    nts.add(def);
                    nodeTypes.put(def.getName(), def);
                }
            }
        }
        return nts.iterator();
    }

    public void registerNodeTypes(NodeTypeStorage storage,
                                  QNodeTypeDefinition[] nodeTypeDefs,
                                  boolean allowUpdate)
            throws RepositoryException {
        storage.registerNodeTypes(nodeTypeDefs, allowUpdate);
    }

    public void unregisterNodeTypes(NodeTypeStorage storage,
                                    Name[] nodeTypeNames)
            throws NoSuchNodeTypeException, RepositoryException {
        storage.unregisterNodeTypes(nodeTypeNames);
    }

    /**
     * Wraps this <code>NodeTypeCache</code> around the passed
     * <code>storage</code> and exposes itself again as a
     * <code>NodeTypeStorage</code>.
     *
     * @param storage the node type storage to wrap.
     * @return node type storage instance using this cache.
     */
    public NodeTypeStorage wrap(final NodeTypeStorage storage) {
        return new NodeTypeStorage() {
            public Iterator<QNodeTypeDefinition> getAllDefinitions() throws RepositoryException {
                return NodeTypeCache.this.getAllDefinitions(storage);
            }
            public Iterator<QNodeTypeDefinition> getDefinitions(Name[] nodeTypeNames)
                    throws NoSuchNodeTypeException, RepositoryException {
                return NodeTypeCache.this.getDefinitions(storage, nodeTypeNames);
            }
            public void registerNodeTypes(QNodeTypeDefinition[] nodeTypeDefs, boolean allowUpdate)
                    throws RepositoryException {
                NodeTypeCache.this.registerNodeTypes(storage, nodeTypeDefs, allowUpdate);
            }
            public void unregisterNodeTypes(Name[] nodeTypeNames)
                    throws NoSuchNodeTypeException, RepositoryException {
                NodeTypeCache.this.unregisterNodeTypes(storage, nodeTypeNames);
            }
        };
    }
}
