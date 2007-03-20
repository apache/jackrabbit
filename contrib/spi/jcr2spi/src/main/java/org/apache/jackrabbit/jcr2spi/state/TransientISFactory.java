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
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeRegistry;

import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import java.util.Iterator;

/**
 * <code>TransientISFactory</code>...
 */
public final class TransientISFactory extends AbstractItemStateFactory implements TransientItemStateFactory  {

    private static Logger log = LoggerFactory.getLogger(TransientISFactory.class);

    private final ItemStateFactory workspaceStateFactory;
    private final NodeTypeRegistry ntReg;

    public TransientISFactory(ItemStateFactory workspaceStateFactory, NodeTypeRegistry ntReg) {
        this.workspaceStateFactory = workspaceStateFactory;
        this.ntReg = ntReg;
    }

    //------------------------------------------< TransientItemStateFactory >---
    /**
     * @inheritDoc
     * @see TransientItemStateFactory#createNewNodeState(NodeEntry , QName, QNodeDefinition)
     */
    public NodeState createNewNodeState(NodeEntry entry, QName nodetypeName,
                                        QNodeDefinition definition) {

        NodeState nodeState = new NodeState(entry, nodetypeName, QName.EMPTY_ARRAY, definition, Status.NEW, false, this, ntReg);

        // notify listeners that a node state has been created
        notifyCreated(nodeState);

        return nodeState;
    }

    /**
     * @inheritDoc
     * @see TransientItemStateFactory#createNewPropertyState(PropertyEntry, QPropertyDefinition)
     */
    public PropertyState createNewPropertyState(PropertyEntry entry, QPropertyDefinition definition) {
        PropertyState propState = new PropertyState(entry, definition.isMultiple(), definition, Status.NEW, false, this, ntReg);

        // notify listeners that a property state has been created
        notifyCreated(propState);

        return propState;
    }

    //---------------------------------------------------< ItemStateFactory >---
    /**
     * @inheritDoc
     * @see ItemStateFactory#createRootState(NodeEntry)
     */
    public NodeState createRootState(NodeEntry entry) throws ItemNotFoundException, RepositoryException {
        // retrieve state to overlay
        NodeState overlayedState = workspaceStateFactory.createRootState(entry);
        return buildNodeState(overlayedState, Status.EXISTING);
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#createNodeState(NodeId,NodeEntry)
     */
    public NodeState createNodeState(NodeId nodeId, NodeEntry entry)
            throws ItemNotFoundException, RepositoryException {
        // retrieve state to overlay
        NodeState overlayedState = workspaceStateFactory.createNodeState(nodeId, entry);
        return buildNodeState(overlayedState, getInitialStatus(entry.getParent()));
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#createDeepNodeState(NodeId, NodeEntry)
     */
    public NodeState createDeepNodeState(NodeId nodeId, NodeEntry anyParent)
            throws ItemNotFoundException, RepositoryException {
        NodeState overlayedState = workspaceStateFactory.createDeepNodeState(nodeId, anyParent);
        return buildNodeState(overlayedState, getInitialStatus(anyParent));
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#createPropertyState(PropertyId, PropertyEntry)
     */
    public PropertyState createPropertyState(PropertyId propertyId,
                                             PropertyEntry entry)
            throws ItemNotFoundException, RepositoryException {
        // retrieve state to overlay
        PropertyState overlayedState = workspaceStateFactory.createPropertyState(propertyId, entry);
        return buildPropertyState(overlayedState, getInitialStatus(entry.getParent()));
    }

    /**
     * @see ItemStateFactory#createDeepPropertyState(PropertyId, NodeEntry)
     */
    public PropertyState createDeepPropertyState(PropertyId propertyId, NodeEntry anyParent) throws ItemNotFoundException, RepositoryException {
        PropertyState overlayedState = workspaceStateFactory.createDeepPropertyState(propertyId, anyParent);
        return buildPropertyState(overlayedState, getInitialStatus(anyParent));
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#getChildNodeInfos(NodeId)
     */
    public Iterator getChildNodeInfos(NodeId nodeId) throws ItemNotFoundException, RepositoryException {
        return workspaceStateFactory.getChildNodeInfos(nodeId);
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#getNodeReferences(NodeState)
     */
    public NodeReferences getNodeReferences(NodeState nodeState) {
        if (nodeState.getStatus() == Status.NEW) {
            return EmptyNodeReferences.getInstance();
        }

        NodeState workspaceState = (NodeState) nodeState.getWorkspaceState();
        return workspaceStateFactory.getNodeReferences(workspaceState);
    }

    //------------------------------------------------------------< private >---
    /**
     *
     * @param overlayed
     * @return
     */
    private NodeState buildNodeState(NodeState overlayed, int initialStatus) {
        NodeState nodeState = new NodeState(overlayed, initialStatus, this);

        notifyCreated(nodeState);
        return nodeState;
    }


    /**
     *
     * @param overlayed
     * @return
     */
    private PropertyState buildPropertyState(PropertyState overlayed, int initialStatus) {
        PropertyState propState = new PropertyState(overlayed, initialStatus, this);

        notifyCreated(propState);
        return propState;
    }

    /**
     *
     * @param parent
     * @return
     */
    private static int getInitialStatus(NodeEntry parent) {
        int status = Status.EXISTING;
        // walk up hiearchy and check if any of the parents is transiently
        // removed, in which case the status must be set to EXISTING_REMOVED.
        while (parent != null) {
            if (parent.getStatus() == Status.EXISTING_REMOVED) {
                status = Status.EXISTING_REMOVED;
                break;
            }
            parent = parent.getParent();
        }
        return status;
    }
}