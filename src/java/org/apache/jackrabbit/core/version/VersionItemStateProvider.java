/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
import org.apache.jackrabbit.core.virtual.VirtualNodeState;
import org.apache.jackrabbit.core.state.*;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.log4j.Logger;

import javax.jcr.RepositoryException;

/**
 * This Class implements...
 *
 * @author tripod
 * @version $Revision:$, $Date:$
 */
public class VersionItemStateProvider implements VirtualItemStateProvider {

    /**
     * the default logger
     */
    private static Logger log = Logger.getLogger(VersionItemStateProvider.class);

    private final HistoryRootNodeState root;

    private final VersionManager vMgr;

    public VersionItemStateProvider(VersionManager vMgr, String rootId, String parentId) {
        this.vMgr = vMgr;
        this.root = new HistoryRootNodeState(this, rootId, parentId);
    }

    public VersionManager getVersionManager() {
        return vMgr;
    }

    public ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {
        if (id instanceof NodeId) {
            return getNodeState((NodeId) id);
        } else {
            return getPropertyState((PropertyId) id);
        }
    }

    public boolean hasNodeState(NodeId id) {
        if (id.equals(root.getId())) {
            return true;
        }
        // check version history
        if (vMgr.hasVersionHistory(id.getUUID())) {
            return true;
        }
        // check verion
        if (vMgr.hasVersion(id.getUUID())) {
            return true;
        }

        return false;
    }

    public NodeState getNodeState(NodeId id)
            throws NoSuchItemStateException, ItemStateException {

        // check if root
        if (id.equals(root.getId())) {
            return root;
        }

        // check version history
        try {
            InternalVersionHistory vh = vMgr.getVersionHistory(id.getUUID());
            if (vh!=null) {
                return new VersionHistoryNodeState(this, vh, root.getUUID());
            }
        } catch (RepositoryException e) {
            log.error("Unable to check for version history:" + e.toString());
            throw new ItemStateException(e);
        }

        // check version
        try {
            InternalVersion v = vMgr.getVersion(id.getUUID());
            if (v!=null) {
                return new VersionNodeState(this, v);
            }
        } catch (RepositoryException e) {
            log.error("Unable to check for version:" + e.toString());
            throw new ItemStateException(e);
        }

        // not found, throw
        throw new NoSuchItemStateException(id.toString());
    }

    public PropertyState getPropertyState(PropertyId id)
            throws NoSuchItemStateException, ItemStateException {

        // get parent state
        NodeState parent = getNodeState(new NodeId(id.getParentUUID()));

        // handle some default prop states
        if (parent instanceof VirtualNodeState) {
            return ((VirtualNodeState) parent).getPropertyState(id.getName());
        }
        throw new NoSuchItemStateException(id.toString());
    }

    public boolean hasPropertyState(PropertyId id) {

        try {
            // get parent state
            NodeState parent = getNodeState(new NodeId(id.getParentUUID()));

            // handle some default prop states
            if (parent instanceof VirtualNodeState) {
                return ((VirtualNodeState) parent).getPropertyState(id.getName())!=null;
            }
        } catch (ItemStateException e) {
            // ignore
        }
        return false;
    }

    public boolean hasItemState(ItemId id) {
        if (id instanceof NodeId) {
            return hasNodeState((NodeId) id);
        } else {
            return hasPropertyState((PropertyId) id);
        }
    }

    public NodeDefId getNodeDefId(QName nodename) {
        return vMgr.getNodeDefId(nodename);
    }

    public PropDefId getPropDefId(QName propname) {
        return vMgr.getPropDefId(propname);
    }

    /**
     * virtual item state provider do not have attics.
     *
     * @throws NoSuchItemStateException always
     */
    public ItemState getItemStateInAttic(ItemId id) throws NoSuchItemStateException {
        // never has states in attic
        throw new NoSuchItemStateException(id.toString());
    }

    /**
     * virtual item state provider do not have attics.
     *
     * @return <code>false</code>
     */
    public boolean hasItemStateInAttic(ItemId id) {
        // never has states in attic
        return false;
    }

}
