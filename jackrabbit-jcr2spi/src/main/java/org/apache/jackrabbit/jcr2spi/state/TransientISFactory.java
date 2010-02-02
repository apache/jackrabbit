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

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>TransientISFactory</code>...
 */
public final class TransientISFactory extends AbstractItemStateFactory implements TransientItemStateFactory, ItemStateCreationListener  {

    private static Logger log = LoggerFactory.getLogger(TransientISFactory.class);

    private final ItemStateFactory workspaceStateFactory;
    private final ItemDefinitionProvider defProvider;

    public TransientISFactory(AbstractItemStateFactory workspaceStateFactory, ItemDefinitionProvider defProvider) {
        this.workspaceStateFactory = workspaceStateFactory;
        this.defProvider = defProvider;
        // start listening to 'creations' on the workspaceStateFactory (and
        // consequently skip an extra notification if the has been built by the
        // workspaceStateFactory.
        workspaceStateFactory.addCreationListener(this);
    }

    //------------------------------------------< TransientItemStateFactory >---
    /**
     * @see TransientItemStateFactory#createNewNodeState(NodeEntry , Name, QNodeDefinition)
     */
    public NodeState createNewNodeState(NodeEntry entry, Name nodetypeName,
                                        QNodeDefinition definition) {

        NodeState nodeState = new NodeState(entry, nodetypeName, Name.EMPTY_ARRAY, this, definition, defProvider);

        // notify listeners that a node state has been created
        notifyCreated(nodeState);

        return nodeState;
    }

    /**
     * @see TransientItemStateFactory#createNewPropertyState(PropertyEntry, QPropertyDefinition, QValue[], int)
     */
    public PropertyState createNewPropertyState(PropertyEntry entry, QPropertyDefinition definition, QValue[] values, int propertyType) throws RepositoryException {
        PropertyState propState = new PropertyState(entry, this, definition, defProvider, values, propertyType);
        // notify listeners that a property state has been created
        notifyCreated(propState);
        return propState;
    }

    //---------------------------------------------------< ItemStateFactory >---
    /**
     * @see ItemStateFactory#createRootState(NodeEntry)
     */
    public NodeState createRootState(NodeEntry entry) throws ItemNotFoundException, RepositoryException {
        NodeState state = workspaceStateFactory.createRootState(entry);
        return state;
    }

    /**
     * @see ItemStateFactory#createNodeState(NodeId,NodeEntry)
     */
    public NodeState createNodeState(NodeId nodeId, NodeEntry entry)
            throws ItemNotFoundException, RepositoryException {
        NodeState state = workspaceStateFactory.createNodeState(nodeId, entry);
        return state;
    }

    /**
     * @see ItemStateFactory#createDeepNodeState(NodeId, NodeEntry)
     */
    public NodeState createDeepNodeState(NodeId nodeId, NodeEntry anyParent)
            throws ItemNotFoundException, RepositoryException {
        NodeState state = workspaceStateFactory.createDeepNodeState(nodeId, anyParent);
        return state;
    }

    /**
     * @see ItemStateFactory#createPropertyState(PropertyId, PropertyEntry)
     */
    public PropertyState createPropertyState(PropertyId propertyId,
                                             PropertyEntry entry)
            throws ItemNotFoundException, RepositoryException {
        PropertyState state = workspaceStateFactory.createPropertyState(propertyId, entry);
        return state;

    }

    /**
     * @see ItemStateFactory#createDeepPropertyState(PropertyId, NodeEntry)
     */
    public PropertyState createDeepPropertyState(PropertyId propertyId, NodeEntry anyParent) throws ItemNotFoundException, RepositoryException {
        PropertyState state = workspaceStateFactory.createDeepPropertyState(propertyId, anyParent);
        return state;
    }

    /**
     * @see ItemStateFactory#getChildNodeInfos(NodeId)
     */
    public Iterator<ChildInfo> getChildNodeInfos(NodeId nodeId) throws ItemNotFoundException, RepositoryException {
        return workspaceStateFactory.getChildNodeInfos(nodeId);
    }

    /**
     * @see ItemStateFactory#getNodeReferences(NodeState,org.apache.jackrabbit.spi.Name,boolean)
     */
    public Iterator<PropertyId> getNodeReferences(NodeState nodeState, Name propertyName, boolean weak) {
        if (nodeState.getStatus() == Status.NEW) {
            Set<PropertyId> t = Collections.emptySet();
            return t.iterator();
        }
        return workspaceStateFactory.getNodeReferences(nodeState, propertyName, weak);
    }

    //------------------------------------------< ItemStateCreationListener >---
    /**
     * @see ItemStateCreationListener#created(ItemState)
     */
    public void created(ItemState state) {
        log.debug("ItemState created by WorkspaceItemStateFactory");
        notifyCreated(state);
    }

    /**
     * @see ItemStateCreationListener#statusChanged(ItemState, int)
     */
    public void statusChanged(ItemState state, int previousStatus) {
        // ignore
    }
}