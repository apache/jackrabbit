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

import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.log4j.Logger;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import java.util.Iterator;

/**
 * <code>HierarchyManager</code> implementation that is also able to
 * build/resolve paths of those items that have been moved or removed
 * (i.e. moved to the attic).
 *
 * todo make use of path caching
 */
public class ZombieHierarchyManager extends HierarchyManagerImpl {

    private static Logger log = Logger.getLogger(ZombieHierarchyManager.class);

    protected ItemStateManager attic;

    public ZombieHierarchyManager(String rootNodeUUID,
                                 ItemStateManager provider,
                                 ItemStateManager attic,
                                 NamespaceResolver nsResolver) {
        super(rootNodeUUID, provider, nsResolver);
        this.attic = attic;
    }

    /**
     * {@inheritDoc}
     *
     * Checks attic first.
     */
    protected ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {
        // always check attic first
        if (attic.hasItemState(id)) {
            return attic.getItemState(id);
        }
        // delegate to base class
        return super.getItemState(id);
    }

    /**
     * {@inheritDoc}
     *
     * Checks attic first.
     */
    protected boolean hasItemState(ItemId id) {
        // always check attic first
        if (attic.hasItemState(id)) {
            return true;
        }
        // delegate to base class
        return super.hasItemState(id);
    }

    /**
     * {@inheritDoc}
     *
     * Also allows for removed/renamed parent-child links.
     */
    protected void buildPath(Path.PathBuilder builder, ItemState state)
            throws ItemStateException, RepositoryException {

        // shortcut
        if (state.getId().equals(rootNodeId)) {
            builder.addRoot();
            return;
        }

        String parentUUID;
        if (state.hasOverlayedState()) {
            // use 'old' parent in case item has been removed
            parentUUID = state.getOverlayedState().getParentUUID();
        } else {
            parentUUID = state.getParentUUID();
        }
        if (parentUUID == null) {
            String msg = "failed to build path of " + state.getId() + ": orphaned item";
            log.debug(msg);
            throw new ItemNotFoundException(msg);
        }

        NodeState parent = (NodeState) getItemState(new NodeId(parentUUID));
        // recursively build path of parent
        buildPath(builder, parent);

        if (state.isNode()) {
            NodeState nodeState = (NodeState) state;
            String uuid = nodeState.getUUID();
            NodeState.ChildNodeEntry parentEntry = null;
            // check removed child node entries first
            Iterator iter = parent.getRemovedChildNodeEntries().iterator();
            while (iter.hasNext()) {
                NodeState.ChildNodeEntry entry =
                        (NodeState.ChildNodeEntry) iter.next();
                if (entry.getUUID().equals(uuid)) {
                    parentEntry = entry;
                    break;
                }
            }
            if (parentEntry == null) {
                // no removed child node entry found in parent,
                // check current child node entries
                parentEntry = parent.getChildNodeEntry(uuid);
            }
            if (parentEntry == null) {
                String msg = "failed to build path of " + state.getId() + ": "
                        + parent.getUUID() + " has no child entry for "
                        + uuid;
                log.debug(msg);
                throw new ItemNotFoundException(msg);
            }
            // add to path
            if (parentEntry.getIndex() == 1) {
                builder.addLast(parentEntry.getName());
            } else {
                builder.addLast(parentEntry.getName(), parentEntry.getIndex());
            }
        } else {
            PropertyState propState = (PropertyState) state;
            QName name = propState.getName();
            // add to path
            builder.addLast(name);
        }
    }
}
