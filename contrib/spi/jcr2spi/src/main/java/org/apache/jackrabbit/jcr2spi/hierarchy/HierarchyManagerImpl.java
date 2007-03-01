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

import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.NoSuchItemStateException;
import org.apache.jackrabbit.jcr2spi.state.TransientItemStateFactory;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;

/**
 * <code>HierarchyManagerImpl</code> implements the <code>HierarchyManager</code>
 * interface.
 */
public class HierarchyManagerImpl implements HierarchyManager {

    private static Logger log = LoggerFactory.getLogger(org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManagerImpl.class);

    private final NodeEntry rootEntry;
    private final UniqueIdResolver uniqueIdResolver;
    private final IdFactory idFactory;

    public HierarchyManagerImpl(TransientItemStateFactory isf, IdFactory idFactory) {
        uniqueIdResolver = new UniqueIdResolver(isf);
        rootEntry = new EntryFactory(isf, idFactory, uniqueIdResolver).createRootEntry();
        this.idFactory = idFactory;
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
            NodeEntry nEntry = uniqueIdResolver.lookup(idFactory.createNodeId(uniqueID));
            Path path = workspaceItemId.getPath();
            if (path == null) {
                return nEntry;
            } else {
                return nEntry.lookupDeepEntry(path);
            }
        }
    }

    /**
     * @see HierarchyManager#getHierarchyEntry(ItemId)
     */
    public HierarchyEntry getHierarchyEntry(ItemId itemId) throws PathNotFoundException, RepositoryException {
        String uniqueID = itemId.getUniqueID();
        if (uniqueID == null) {
            return getHierarchyEntry(itemId.getPath());
        } else {
            if (itemId.getPath() == null) {
                NodeEntry nEntry = uniqueIdResolver.resolve((NodeId) itemId, rootEntry);
                return nEntry;
            } else {
                NodeEntry nEntry = uniqueIdResolver.resolve(idFactory.createNodeId(uniqueID), rootEntry);
                return nEntry.getDeepEntry(itemId.getPath());
            }
        }
    }

    /**
     * @see HierarchyManager#getHierarchyEntry(Path)
     */
    public HierarchyEntry getHierarchyEntry(Path qPath) throws PathNotFoundException, RepositoryException {
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

        return rootEntry.getDeepEntry(qPath);
    }

    /**
     * @see HierarchyManager#getItemState(Path)
     */
    public ItemState getItemState(Path qPath) throws PathNotFoundException, RepositoryException {
        HierarchyEntry entry = getHierarchyEntry(qPath);
        try {
            ItemState state = entry.getItemState();
            if (state.isValid()) {
                return state;
            } else {
                throw new PathNotFoundException();
            }
        } catch (NoSuchItemStateException e) {
            throw new PathNotFoundException(e);
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
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
