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
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.IdIterator;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.value.QValue;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import java.io.InputStream;
import java.io.IOException;

/**
 * <code>WorkspaceItemStateFactory</code>...
 */
public class WorkspaceItemStateFactory implements ItemStateFactory {

    private static Logger log = LoggerFactory.getLogger(WorkspaceItemStateFactory.class);

    private final RepositoryService service;
    private final SessionInfo sessionInfo;

    public WorkspaceItemStateFactory(RepositoryService service, SessionInfo sessionInfo) {
        this.service = service;
        this.sessionInfo = sessionInfo;
    }

    /**
     * Creates the node with information retrieved from the
     * <code>RepositoryService</code>.
     *
     * @inheritDoc
     * @see ItemStateFactory#createNodeState(NodeId, ItemStateManager)
     */
    public NodeState createNodeState(NodeId nodeId, ItemStateManager ism)
            throws NoSuchItemStateException, ItemStateException {
        try {
            NodeInfo info = service.getNodeInfo(sessionInfo, nodeId);

            // get parent
            NodeId parentId = (info.getParentId() != null) ? info.getParentId() : null;
            NodeState parent = (parentId != null) ? (NodeState) ism.getItemState(parentId) : null;

            return createNodeState(info, parent);
        } catch (PathNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (RepositoryException e) {
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    /**
     * Creates the node with information retrieved from the
     * <code>RepositoryService</code>.
     *
     * @inheritDoc
     * @see ItemStateFactory#createNodeState(NodeId, NodeState)
     */
    public NodeState createNodeState(NodeId nodeId, NodeState parent)
            throws NoSuchItemStateException, ItemStateException {
        try {
            NodeInfo info = service.getNodeInfo(sessionInfo, nodeId);
            return createNodeState(info, parent);
        } catch (PathNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (RepositoryException e) {
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    /**
     * Creates the node with information retrieved from <code>info</code>.
     *
     * @param info   the <code>NodeInfo</code> to use to create the
     *               <code>NodeState</code>.
     * @param parent the parent <code>NodeState</code>.
     * @return the new <code>NodeState</code>.
     */
    private NodeState createNodeState(NodeInfo info, NodeState parent)
            throws NoSuchItemStateException, ItemStateException {
        try {
            QName ntName = info.getNodetype();

            // build the node state
            // NOTE: unable to retrieve definitionId -> needs to be retrieved
            // by the itemManager upon Node creation.
            String uuid = null;
            if (info.getId().getRelativePath() == null) {
                uuid = info.getId().getUUID();
            }
            NodeState state = new NodeState(info.getQName(), uuid, parent, ntName,
                    ItemState.STATUS_EXISTING, false, this, service.getIdFactory());
            // set mixin nodetypes
            state.setMixinTypeNames(info.getMixins());

            // references to child items
            for (IdIterator it = info.getNodeIds(); it.hasNext(); ) {
                NodeInfo childInfo = service.getNodeInfo(sessionInfo, (NodeId) it.nextId());
                String childUUID = null;
                if (childInfo.getId().getRelativePath() == null) {
                    childUUID = childInfo.getId().getUUID();
                }
                state.addChildNodeEntry(childInfo.getQName(), childUUID);
            }

            // references to properties
            for (IdIterator it = info.getPropertyIds(); it.hasNext(); ) {
                PropertyId pId = (PropertyId) it.nextId();
                state.addPropertyName(pId.getQName());
            }

            // copied from local-state-mgr TODO... check
            // register as listener
            // TODO check if needed
            //state.addListener(this);
            return state;
        } catch (PathNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (RepositoryException e) {
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    /**
     * Creates the property with information retrieved from the
     * <code>RepositoryService</code>.
     *
     * @inheritDoc
     * @see ItemStateFactory#createPropertyState(PropertyId, ItemStateManager)
     */
    public PropertyState createPropertyState(PropertyId propertyId,
                                             ItemStateManager ism)
            throws NoSuchItemStateException, ItemStateException {
        try {
            PropertyInfo info = service.getPropertyInfo(sessionInfo, propertyId);
            NodeState parent = (NodeState) ism.getItemState(info.getParentId());
            return createPropertyState(info, parent);
        } catch (PathNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (RepositoryException e) {
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    /**
     * Creates the property with information retrieved from the
     * <code>RepositoryService</code>.
     *
     * @inheritDoc
     * @see ItemStateFactory#createPropertyState(PropertyId, NodeState)
     */
    public PropertyState createPropertyState(PropertyId propertyId,
                                             NodeState parent)
            throws NoSuchItemStateException, ItemStateException {
        try {
            PropertyInfo info = service.getPropertyInfo(sessionInfo, propertyId);
            return createPropertyState(info, parent);
        } catch (PathNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (RepositoryException e) {
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    /**
     * Creates the property with information retrieved from <code>info</code>.
     *
     * @param info   the <code>PropertyInfo</code> to use to create the
     *               <code>PropertyState</code>.
     * @param parent the parent <code>NodeState</code>.
     * @return the new <code>PropertyState</code>.
     * @throws ItemStateException if an error occurs while retrieving the
     *                            <code>PropertyState</code>.
     */
    private PropertyState createPropertyState(PropertyInfo info,
                                              NodeState parent)
            throws ItemStateException {
        try {
            // build the PropertyState
            // NOTE: unable to retrieve definitionId -> needs to be retrieved
            // by the itemManager upon Property creation.
            PropertyState state = new PropertyState(info.getQName(), parent,
                    ItemState.STATUS_EXISTING, false, service.getIdFactory());
            state.setMultiValued(info.isMultiValued());
            state.setType(info.getType());
            QValue[] qValues;
            if (info.getType() == PropertyType.BINARY) {
                InputStream[] ins = info.getValuesAsStream();
                qValues = new QValue[ins.length];
                for (int i = 0; i < ins.length; i++) {
                    qValues[i] = QValue.create(ins[i]);
                }
            } else {
                String[] str = info.getValues();
                qValues = new QValue[str.length];
                for (int i = 0; i < str.length; i++) {
                    qValues[i] = QValue.create(str[i], info.getType());
                }
            }

            state.internalSetValues(qValues);

            // register as listener
            // TODO check if needed
            // state.addListener(this);
            return state;
        } catch (IOException e) {
            throw new ItemStateException(e.getMessage(), e);
        }
    }
}