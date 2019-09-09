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

import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.state.TransientItemStateFactory;
import org.apache.jackrabbit.jcr2spi.util.LogUtil;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * <code>HierarchyManagerImpl</code> implements the <code>HierarchyManager</code>
 * interface.
 */
public class HierarchyManagerImpl implements HierarchyManager {

    private static Logger log = LoggerFactory.getLogger(HierarchyManagerImpl.class);

    private final NodeEntry rootEntry;
    private final UniqueIdResolver uniqueIdResolver;
    private final IdFactory idFactory;
    private NamePathResolver resolver;

    public HierarchyManagerImpl(TransientItemStateFactory isf, IdFactory idFactory,
                                PathFactory pathFactory) {
        uniqueIdResolver = new UniqueIdResolver(isf);
        rootEntry = new EntryFactory(isf, idFactory, uniqueIdResolver, pathFactory).createRootEntry();
        this.idFactory = idFactory;
    }

    public void setResolver(NamePathResolver resolver) {
        this.resolver = resolver;
        if (rootEntry instanceof HierarchyEntryImpl) {
            ((HierarchyEntryImpl) rootEntry).factory.setResolver(resolver);
        }
    }

    //---------------------------------------------------< HierarchyManager >---
    /**
     * @see HierarchyManager#dispose()
     */
    public void dispose() {
        uniqueIdResolver.dispose();
    }

    /**
     * @see HierarchyManager#getRootEntry()
     */
    public NodeEntry getRootEntry() {
        return rootEntry;
    }

    /**
     * @see HierarchyManager#lookup(ItemId)
     */
    public HierarchyEntry lookup(ItemId workspaceItemId) {
        String uniqueID = workspaceItemId.getUniqueID();
        if (uniqueID == null) {
            return rootEntry.lookupDeepEntry(workspaceItemId.getPath());
        } else {
            NodeEntry nEntry = uniqueIdResolver.lookup(uniqueID);
            Path path = workspaceItemId.getPath();
            if (path == null) {
                return nEntry;
            } else {
                return nEntry != null ? nEntry.lookupDeepEntry(path) : null;
            }
        }
    }

    /**
     * @see HierarchyManager#lookup(Path)
     */
    public HierarchyEntry lookup(Path workspacePath) {
        return rootEntry.lookupDeepEntry(workspacePath);
    }

    /**
     * @see HierarchyManager#getNodeEntry(NodeId)
     */
    public NodeEntry getNodeEntry(NodeId nodeId)
            throws ItemNotFoundException, RepositoryException {
        String uniqueID = nodeId.getUniqueID();
        if (uniqueID == null) {
            return getNodeEntry(nodeId.getPath());
        } else {
            if (nodeId.getPath() == null) {
                NodeEntry nEntry = uniqueIdResolver.resolve(nodeId, rootEntry);
                return nEntry;
            } else {
                NodeEntry nEntry = uniqueIdResolver.resolve(idFactory.createNodeId(uniqueID), rootEntry);
                return nEntry.getDeepNodeEntry(nodeId.getPath());
            }
        }
    }

    /**
     * @see HierarchyManager#getNodeEntry(Path)
     */
    public NodeEntry getNodeEntry(Path qPath) throws PathNotFoundException, RepositoryException {
        NodeEntry rootEntry = getRootEntry();
        // shortcut
        if (qPath.denotesRoot()) {
            return rootEntry;
        }
        if (!qPath.isCanonical()) {
            String msg = "Path is not canonical";
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        return rootEntry.getDeepNodeEntry(qPath);
    }

    /**
     * @see HierarchyManager#getPropertyEntry(PropertyId)
     */
    public PropertyEntry getPropertyEntry(PropertyId propertyId)
            throws ItemNotFoundException, RepositoryException {
        String uniqueID = propertyId.getUniqueID();
        if (uniqueID == null) {
            return getPropertyEntry(propertyId.getPath());
        } else {
            if (propertyId.getPath() == null) {
                // a property id always contains a Path part.
                throw new ItemNotFoundException("No property found for id " + LogUtil.saveGetIdString(propertyId, resolver));
            } else {
                NodeEntry nEntry = uniqueIdResolver.resolve(idFactory.createNodeId(uniqueID), rootEntry);
                return nEntry.getDeepPropertyEntry(propertyId.getPath());
            }
        }
    }

    /**
     * @see HierarchyManager#getPropertyEntry(Path)
     */
    public PropertyEntry getPropertyEntry(Path qPath)
            throws PathNotFoundException, RepositoryException {
        // shortcut
        if (qPath.denotesRoot()) {
            throw new PathNotFoundException("The root path never points to a Property.");
        }
        if (!qPath.isCanonical()) {
            String msg = "Path is not canonical";
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        return getRootEntry().getDeepPropertyEntry(qPath);
    }

    /**
     * @see HierarchyManager#getNodeState(Path)
     */
    public NodeState getNodeState(Path qPath) throws PathNotFoundException, RepositoryException {
        NodeEntry entry = getNodeEntry(qPath);
        try {
            NodeState state = entry.getNodeState();
            if (state.isValid()) {
                return state;
            } else {
                throw new PathNotFoundException(LogUtil.safeGetJCRPath(qPath, resolver));
            }
        } catch (ItemNotFoundException e) {
            throw new PathNotFoundException(e);
        }
    }

    /**
     * @see HierarchyManager#getPropertyState(Path)
     */
    public PropertyState getPropertyState(Path qPath) throws PathNotFoundException, RepositoryException {
        PropertyEntry entry = getPropertyEntry(qPath);
        try {
            PropertyState state = entry.getPropertyState();
            if (state.isValid()) {
                return state;
            } else {
                throw new PathNotFoundException(LogUtil.safeGetJCRPath(qPath, resolver));
            }
        } catch (ItemNotFoundException e) {
            throw new PathNotFoundException(e);
        }
    }

    /**
     * @see HierarchyManager#getDepth(HierarchyEntry)
     */
    public int getDepth(HierarchyEntry hierarchyEntry) throws ItemNotFoundException, RepositoryException {
        int depth = Path.ROOT_DEPTH;
        NodeEntry parentEntry = hierarchyEntry.getParent();
        while (parentEntry != null) {
            depth++;
            hierarchyEntry = parentEntry;
            parentEntry = hierarchyEntry.getParent();
        }
        return depth;
    }

    /**
     * @see HierarchyManager#getRelativeDepth(NodeEntry, HierarchyEntry)
     */
    public int getRelativeDepth(NodeEntry ancestor, HierarchyEntry descendant)
            throws ItemNotFoundException, RepositoryException {
        if (ancestor.equals(descendant)) {
            return 0;
        }
        int depth = 1;
        NodeEntry parent = descendant.getParent();
        while (parent != null) {
            if (parent.equals(ancestor)) {
                return depth;
            }
            depth++;
            descendant = parent;
            parent = descendant.getParent();
        }
        // not an ancestor
        return -1;
    }
}
