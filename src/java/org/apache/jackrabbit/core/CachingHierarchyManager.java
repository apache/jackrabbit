/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core;

import org.apache.commons.collections.ReferenceMap;
import org.apache.log4j.Logger;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.Map;

/**
 * <code>CachingHierarchyManager</code> is a simple wrapper for a
 * <code>HierarchyManager</code> that caches the <code>ItemId</code> to <code>Path</code>
 * mappings returned by the underlying <code>HierarchyManager</code> for better
 * performance.
 * <p/>
 * Please keep in mind that this cache of <code>Path</code>s is not automatically
 * updated when the underlying hierarchy is changing. Therefore it should only be
 * used with caution and in special situations (usually only locally within a
 * narrow scope) where the underlying hierarchy is not expected to change.
 */
public class CachingHierarchyManager implements HierarchyManager {

    private static Logger log = Logger.getLogger(CachingHierarchyManager.class);

    private final HierarchyManager delegatee;

    // map of item id to list of paths
    private Map pathCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.HARD);
    private Map zombiePathCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.HARD);

    // map of path to item id
    private Map idCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.HARD);

    /**
     * @param hierMgr
     */
    public CachingHierarchyManager(HierarchyManager hierMgr) {
        delegatee = hierMgr;
    }

    /**
     * Returns the wrapped <code>HierarchyManager</code> instance
     *
     * @return the wrapped <code>HierarchyManager</code> instance
     */
    public HierarchyManager unwrap() {
        return delegatee;
    }

    /**
     * Clears the cache.
     */
    public synchronized void clearCache() {
        pathCache.clear();
        zombiePathCache.clear();
        idCache.clear();
    }

    //-----------------------------------------------------< HierarchyManager >
    /**
     * @see HierarchyManager#listParents(ItemId)
     */
    public NodeId[] listParents(ItemId id) throws ItemNotFoundException, RepositoryException {
        return delegatee.listParents(id);
    }

    /**
     * @see HierarchyManager#listChildren(NodeId)
     */
    public ItemId[] listChildren(NodeId id) throws ItemNotFoundException, RepositoryException {
        return delegatee.listChildren(id);
    }

    /**
     * @see HierarchyManager#listZombieChildren(NodeId)
     */
    public ItemId[] listZombieChildren(NodeId id)
            throws ItemNotFoundException, RepositoryException {
        return delegatee.listZombieChildren(id);
    }

    /**
     * @see HierarchyManager#resolvePath(Path)
     */
    public synchronized ItemId resolvePath(Path path)
            throws PathNotFoundException, RepositoryException {
        // check cache first
        ItemId id = (ItemId) idCache.get(path);
        if (id != null) {
            return id;
        }
        id = delegatee.resolvePath(path);
        idCache.put(path, id);
        return id;
    }

    /**
     * @see HierarchyManager#getPath(ItemId)
     */
    public synchronized Path getPath(ItemId id) throws ItemNotFoundException, RepositoryException {
        return getAllPaths(id, false)[0];
    }

    /**
     * @see HierarchyManager#getName(ItemId)
     */
    public QName getName(ItemId itemId) throws ItemNotFoundException, RepositoryException {
        if (itemId.denotesNode()) {
            return getPath(itemId).getNameElement().getName();
        } else {
            PropertyId propId = (PropertyId) itemId;
            return propId.getName();
        }
    }

    /**
     * @see HierarchyManager#getAllPaths(ItemId)
     */
    public synchronized Path[] getAllPaths(ItemId id) throws ItemNotFoundException, RepositoryException {
        return getAllPaths(id, false);
    }

    /**
     * @see HierarchyManager#getAllPaths(ItemId, boolean)
     */
    public synchronized Path[] getAllPaths(ItemId id, boolean includeZombies)
            throws ItemNotFoundException, RepositoryException {
        // check cache first
        Path[] paths;
        if (includeZombies) {
            paths = (Path[]) zombiePathCache.get(id);
            if (paths != null) {
                return paths;
            }
            paths = delegatee.getAllPaths(id, includeZombies);
            zombiePathCache.put(id, paths);
        } else {
            paths = (Path[]) pathCache.get(id);
            if (paths != null) {
                return paths;
            }
            paths = delegatee.getAllPaths(id, includeZombies);
            pathCache.put(id, paths);
        }
        return paths;
    }
}

