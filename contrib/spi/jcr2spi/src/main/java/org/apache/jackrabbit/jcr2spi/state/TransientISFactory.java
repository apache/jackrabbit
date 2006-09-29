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

/**
 * <code>TransientISFactory</code>...
 */
final class TransientISFactory implements TransientItemStateFactory {

    private static Logger log = LoggerFactory.getLogger(TransientISFactory.class);

    private final IdFactory idFactory;
    private final ItemStateManager parent;

    private ItemStateLifeCycleListener listener;

    TransientISFactory(IdFactory idFactory, ItemStateManager parent) {
        this.idFactory = idFactory;
        this.parent = parent;
    }

    void setListener(ItemStateLifeCycleListener listener) {
        this.listener = listener;
    }
    //------------------------------------------< TransientItemStateFactory >---
    /**
     * @inheritDoc
     * @see TransientItemStateFactory#createNewNodeState(QName, String, NodeState, QName, QNodeDefinition)
     */
    public NodeState createNewNodeState(QName name, String uuid,
                                        NodeState parent, QName nodetypeName,
                                        QNodeDefinition definition) {
        NodeState nodeState = new NodeState(name, uuid, parent, nodetypeName,
            definition, ItemState.STATUS_NEW, this, idFactory);
        // get a notification when this item state is saved or invalidated
        nodeState.addListener(listener);
        // notify listener that a node state has been created
        listener.statusChanged(nodeState, ItemState.STATUS_NEW);
        return nodeState;
    }

    /**
     * @inheritDoc
     * @see TransientItemStateFactory#createNewPropertyState(QName, NodeState, QPropertyDefinition)
     */
    public PropertyState createNewPropertyState(QName name, NodeState parent, QPropertyDefinition definition) {
        PropertyState propState = new PropertyState(name, parent,
            definition, ItemState.STATUS_NEW, idFactory);
        // get a notification when this item state is saved or invalidated
        propState.addListener(listener);
        // notify listener that a property state has been created
        listener.statusChanged(propState, ItemState.STATUS_NEW);
        return propState;
    }

    //---------------------------------------------------< ItemStateFactory >---
    /**
     * @inheritDoc
     * @see ItemStateFactory#createRootState(ItemStateManager)
     */
    public NodeState createRootState(ItemStateManager ism) throws ItemStateException {
        // retrieve state to overlay
        NodeState overlayedState = (NodeState) parent.getRootState();
        NodeState nodeState = new NodeState(overlayedState, null,
            ItemState.STATUS_EXISTING, this, idFactory);
        nodeState.addListener(listener);
        return nodeState;
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#createNodeState(NodeId, ItemStateManager)
     */
    public NodeState createNodeState(NodeId nodeId, ItemStateManager ism)
        throws NoSuchItemStateException, ItemStateException {
        // retrieve state to overlay
        NodeState overlayedState = (NodeState) parent.getItemState(nodeId);
        NodeState overlayedParent = overlayedState.getParent();
        NodeState parentState = null;
        if (overlayedParent != null) {
            parentState = (NodeState) ism.getItemState(overlayedParent.getId());
        }
        NodeState nodeState = new NodeState(overlayedState, parentState,
            ItemState.STATUS_EXISTING, this, idFactory);
        nodeState.addListener(listener);
        return nodeState;
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#createNodeState(NodeId, NodeState)
     */
    public NodeState createNodeState(NodeId nodeId, NodeState parentState)
        throws NoSuchItemStateException, ItemStateException {
        // retrieve state to overlay
        NodeState overlayedState = (NodeState) parent.getItemState(nodeId);
        NodeState nodeState = new NodeState(overlayedState, parentState,
            ItemState.STATUS_EXISTING, this, idFactory);
        nodeState.addListener(listener);
        return nodeState;
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#createPropertyState(PropertyId, NodeState)
     */
    public PropertyState createPropertyState(PropertyId propertyId,
                                             NodeState parentState)
        throws NoSuchItemStateException, ItemStateException {
        // retrieve state to overlay
        PropertyState overlayedState = (PropertyState) parent.getItemState(propertyId);
        PropertyState propState = new PropertyState(overlayedState, parentState,
            ItemState.STATUS_EXISTING, idFactory);
        propState.addListener(listener);
        return propState;
    }
}