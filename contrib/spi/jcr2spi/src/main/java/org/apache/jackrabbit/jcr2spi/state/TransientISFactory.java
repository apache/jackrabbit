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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.jcr2spi.state.entry.ChildNodeEntry;

/**
 * <code>TransientISFactory</code>...
 */
final class TransientISFactory implements TransientItemStateFactory {

    private static Logger log = LoggerFactory.getLogger(TransientISFactory.class);

    private final IdFactory idFactory;
    private final ItemStateManager workspaceItemStateMgr;

    private ItemStateCache cache;
    private ItemStateCreationListener listener;

    TransientISFactory(IdFactory idFactory, ItemStateManager workspaceItemStateMgr) {
        this.idFactory = idFactory;
        this.workspaceItemStateMgr = workspaceItemStateMgr;
    }

    //------------------------------------------< TransientItemStateFactory >---
    /**
     * @inheritDoc
     * @see TransientItemStateFactory#createNewNodeState(QName, String, NodeState, QName, QNodeDefinition)
     */
    public NodeState createNewNodeState(QName name, String uniqueID,
                                        NodeState parent, QName nodetypeName,
                                        QNodeDefinition definition) {
        NodeState nodeState = new NodeState(name, uniqueID, parent, nodetypeName,
            definition, Status.NEW, this, idFactory, false);

        // notify listeners when this item state is saved or invalidated
        nodeState.addListener(cache);
        nodeState.addListener(listener);

        // notify listeners that a node state has been created
        cache.created(nodeState);
        listener.created(nodeState);

        return nodeState;
    }

    /**
     * @inheritDoc
     * @see TransientItemStateFactory#createNewPropertyState(QName, NodeState, QPropertyDefinition)
     */
    public PropertyState createNewPropertyState(QName name, NodeState parent, QPropertyDefinition definition) {
        PropertyState propState = new PropertyState(name, parent,
            definition, Status.NEW, this, idFactory, false);

        // get a notification when this item state is saved or invalidated
        propState.addListener(cache);
        propState.addListener(listener);

        // notify listeners that a property state has been created
        cache.created(propState);
        listener.created(propState);

        return propState;
    }

    /**
     * @inheritDoc
     * @see TransientItemStateFactory#setListener(ItemStateCreationListener)
     */
    public void setListener(ItemStateCreationListener listener) {
        this.listener = listener;
    }

    //---------------------------------------------------< ItemStateFactory >---
    /**
     * @inheritDoc
     * @see ItemStateFactory#createRootState(ItemStateManager)
     */
    public NodeState createRootState(ItemStateManager ism) throws ItemStateException {
        // retrieve state to overlay
        NodeState overlayedState = (NodeState) workspaceItemStateMgr.getRootState();
        NodeState nodeState = new NodeState(overlayedState, null, Status.EXISTING, this, idFactory);

        nodeState.addListener(cache);
        cache.created(nodeState);
        return nodeState;
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#createNodeState(NodeId, ItemStateManager)
     */
    public NodeState createNodeState(NodeId nodeId, ItemStateManager ism)
        throws NoSuchItemStateException, ItemStateException {

        NodeState nodeState = cache.getNodeState(nodeId);
        if (nodeState == null) {
            // retrieve state to overlay
            NodeState overlayedState = (NodeState) workspaceItemStateMgr.getItemState(nodeId);
            NodeState overlayedParent = overlayedState.getParent();

            if (overlayedParent == null) {
                // special case root state
                return createRootState(ism);
            }
            
            NodeState parentState = (NodeState) overlayedParent.getSessionState();
            if (parentState == null) {
                parentState = (NodeState) ism.getItemState(overlayedParent.getId());
            }

            ChildNodeEntry cne = parentState.getChildNodeEntry(nodeId);
            if (cne != null) {
                nodeState = cne.getNodeState();
                nodeState.addListener(cache);
                cache.created(nodeState);
            } else {
                throw new NoSuchItemStateException("No such item " + nodeId.toString());
            }
        }
        return nodeState;
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#createNodeState(NodeId, NodeState)
     */
    public NodeState createNodeState(NodeId nodeId, NodeState parentState)
        throws NoSuchItemStateException, ItemStateException {

        NodeState nodeState = cache.getNodeState(nodeId);
        if (nodeState == null) {
            // retrieve state to overlay
            NodeState overlayedState = (NodeState) workspaceItemStateMgr.getItemState(nodeId);
            nodeState = new NodeState(overlayedState, parentState, Status.EXISTING, this, idFactory);

            nodeState.addListener(cache);
            cache.created(nodeState);
        }
        return nodeState;
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#createPropertyState(PropertyId, NodeState)
     */
    public PropertyState createPropertyState(PropertyId propertyId,
                                             NodeState parentState)
        throws NoSuchItemStateException, ItemStateException {

        PropertyState propState = cache.getPropertyState(propertyId);
        if (propState == null) {
            // retrieve state to overlay
            PropertyState overlayedState = (PropertyState) workspaceItemStateMgr.getItemState(propertyId);
            propState = new PropertyState(overlayedState, parentState, Status.EXISTING, this, idFactory);

            propState.addListener(cache);
            cache.created(propState);
        }
        return propState;
    }

    public ChildNodeEntries getChildNodeEntries(NodeState nodeState) throws NoSuchItemStateException, ItemStateException {
        if (nodeState.getStatus() == Status.NEW) {
            return new ChildNodeEntries(nodeState);
        } else {
            NodeState overlayed = (NodeState) nodeState.getWorkspaceState();
            ChildNodeEntries overlayedEntries = overlayed.isf.getChildNodeEntries(overlayed);
            return new ChildNodeEntries(nodeState, overlayedEntries);
        }
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#setCache(ItemStateCache)
     */
    public void setCache(ItemStateCache cache) {
        this.cache = cache;
    }
}