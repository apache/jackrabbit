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
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.ItemStateManager;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.entry.ChildNodeEntry;
import org.apache.jackrabbit.jcr2spi.util.LogUtil;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * <code>HierarchyManagerImpl</code> implements the <code>HierarchyManager</code>
 * interface.
 */
public class HierarchyManagerImpl implements HierarchyManager {

    private static Logger log = LoggerFactory.getLogger(HierarchyManagerImpl.class);

    private final ItemStateManager itemStateManager;
    // used for outputting user-friendly paths and names
    private final NamespaceResolver nsResolver;

    public HierarchyManagerImpl(ItemStateManager itemStateManager,
                                NamespaceResolver nsResolver) {
        this.itemStateManager = itemStateManager;
        this.nsResolver = nsResolver;
    }

    //------------------------------------------------------------< private >---
    /**
     * Resolve a path into an item state. Recursively invoked method that may be
     * overridden by some subclass to either return cached responses or add
     * response to cache.
     *
     * @param path  full path of item to resolve
     * @param state intermediate state
     * @param next  next path element index to resolve
     * @return the state of the item denoted by <code>path</code>
     */
    private ItemState resolvePath(Path path, ItemState state, int next)
        throws PathNotFoundException, RepositoryException {

        Path.PathElement[] elements = path.getElements();
        if (elements.length == next) {
            return state;
        }
        Path.PathElement elem = elements[next];

        QName name = elem.getName();
        int index = elem.getNormalizedIndex();

        NodeState parentState = (NodeState) state;
        ItemState childState;

        if (parentState.hasChildNodeEntry(name, index)) {
            // child node
            ChildNodeEntry nodeEntry = parentState.getChildNodeEntry(name, index);
            try {
                childState = nodeEntry.getNodeState();
            } catch (ItemStateException e) {
                // should never occur
                throw new RepositoryException(e);
            }
        } else if (parentState.hasPropertyName(name)) {
            // property
            if (index > Path.INDEX_DEFAULT) {
                // properties can't have same name siblings
                throw new PathNotFoundException(LogUtil.safeGetJCRPath(path, nsResolver));
            } else if (next < elements.length - 1) {
                // property is not the last element in the path
                throw new PathNotFoundException(LogUtil.safeGetJCRPath(path, nsResolver));
            }
            try {
                childState = parentState.getPropertyState(name);
            } catch (ItemStateException e) {
                // should never occur
                throw new RepositoryException(e);
            }
        } else {
            // no such item
            throw new PathNotFoundException(LogUtil.safeGetJCRPath(path, nsResolver));
        }
        return resolvePath(path, childState, next + 1);
    }

    //---------------------------------------------------< HierarchyManager >---
    /**
     * @see HierarchyManager#getItemState(Path)
     */
    public ItemState getItemState(Path qPath) throws PathNotFoundException, RepositoryException {
        try {
            // retrieve root state first
            NodeState rootState = itemStateManager.getRootState();
            // shortcut
            if (qPath.denotesRoot()) {
                return rootState;
            }

            if (!qPath.isCanonical()) {
                String msg = "path is not canonical";
                log.debug(msg);
                throw new RepositoryException(msg);
            }

            return resolvePath(qPath, rootState, Path.INDEX_DEFAULT);
        } catch (ItemStateException e) {
            // should not occur
            throw new RepositoryException(e);
        }
    }

    /**
     * @see HierarchyManager#getDepth(ItemState)
     */
    public int getDepth(ItemState itemState) throws ItemNotFoundException, RepositoryException {
        int depth = Path.ROOT_DEPTH;
        NodeState parentState = itemState.getParent();
        while (parentState != null) {
            depth++;
            itemState = parentState;
            parentState = itemState.getParent();
        }
        return depth;
    }

    /**
     * @see HierarchyManager#getRelativeDepth(NodeState, ItemState)
     */
    public int getRelativeDepth(NodeState ancestor, ItemState descendant)
            throws ItemNotFoundException, RepositoryException {
        if (ancestor.equals(descendant)) {
            return 0;
        }
        int depth = 1;
        NodeState parent = descendant.getParent();
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

