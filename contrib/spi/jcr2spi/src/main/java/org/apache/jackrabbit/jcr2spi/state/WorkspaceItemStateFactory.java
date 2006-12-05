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
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.value.QValue;
import org.apache.jackrabbit.jcr2spi.WorkspaceManager;
import org.apache.jackrabbit.jcr2spi.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeConflictException;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.ItemNotFoundException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;

/**
 * <code>WorkspaceItemStateFactory</code>...
 */
public class WorkspaceItemStateFactory implements ItemStateFactory {

    private static Logger log = LoggerFactory.getLogger(WorkspaceItemStateFactory.class);

    private final RepositoryService service;
    private final SessionInfo sessionInfo;
    private final WorkspaceManager wspManager;

    private ItemStateCache cache;

    public WorkspaceItemStateFactory(RepositoryService service, SessionInfo sessionInfo, WorkspaceManager wspManager) {
        this.service = service;
        this.sessionInfo = sessionInfo;
        this.wspManager = wspManager;
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#createRootState(ItemStateManager)
     */
    public NodeState createRootState(ItemStateManager ism) throws ItemStateException {
        try {
            NodeInfo info = service.getNodeInfo(sessionInfo, service.getRootId(sessionInfo));
            return createNodeState(info, null);
        } catch (RepositoryException e) {
            throw new ItemStateException("Internal error while building root state.");
        }
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
            NodeId parentId = info.getParentId();
            if (parentId != null) {
                NodeState parent = (NodeState) ism.getItemState(parentId);
                return parent.getChildNodeEntry(nodeId).getNodeState();
            } else {
                // special case for root state
                return createNodeState(info, null);
            }
        } catch (ItemNotFoundException e) {
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
     * @param info the <code>NodeInfo</code> to use to create the
     * <code>NodeState</code>.
     * @param parent the parent <code>NodeState</code>.
     * @return the new <code>NodeState</code>.
     */
    private NodeState createNodeState(NodeInfo info, NodeState parent)
            throws NoSuchItemStateException, ItemStateException {
        try {
            // retrieve definition
            QNodeDefinition definition;
            if (parent == null) {
                // special case for root state
                definition = wspManager.getNodeTypeRegistry().getRootNodeDef();
            } else {
                EffectiveNodeType ent = wspManager.getNodeTypeRegistry().getEffectiveNodeType(parent.getNodeTypeNames());
                definition = ent.getApplicableNodeDefinition(info.getQName(), info.getNodetype());
            }

            // build the node state
            String uuid = null;
            if (info.getId().getPath() == null) {
                uuid = info.getId().getUUID();
            }
            NodeState state = new NodeState(info.getQName(), uuid, parent, info.getNodetype(),
                definition, Status.EXISTING, this, service.getIdFactory(), true);

            // names of child property entries
            Set propNames = new HashSet();
            for (IdIterator it = info.getPropertyIds(); it.hasNext(); ) {
                PropertyId pId = (PropertyId) it.nextId();
                propNames.add(pId.getQName());
            }

            // Build node-references object even if the state is not refereceable yet.
            PropertyId[] references = info.getReferences();
            NodeReferences nodeRefs = new NodeReferencesImpl(state, references);

            state.init(info.getMixins(), propNames, nodeRefs);

            state.addListener(cache);
            cache.created(state);

            return state;
        } catch (NodeTypeConflictException e) {
            String msg = "Internal error: failed to retrieve node definition.";
            log.debug(msg);
            throw new ItemStateException(msg, e);
        } catch (ConstraintViolationException e) {
            String msg = "Internal error: failed to retrieve node definition.";
            log.debug(msg);
            throw new ItemStateException(msg, e);
        } catch (NoSuchNodeTypeException e) {
            String msg = "internal error: failed to retrieve node definition.";
            log.debug(msg);
            throw new ItemStateException(msg, e);
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

    public ChildNodeEntries getChildNodeEntries(NodeState nodeState)
        throws NoSuchItemStateException, ItemStateException {
        try {
            ChildNodeEntries entries = new ChildNodeEntries(nodeState);
            Collection childInfos = service.getChildNodeInfos(sessionInfo, nodeState.getNodeId());
            for (Iterator it = childInfos.iterator(); it.hasNext();) {
                ChildInfo ci = (ChildInfo) it.next();
                entries.add(ci.getName(), ci.getUUID(), ci.getIndex());
            }
            return entries;
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
    private PropertyState createPropertyState(PropertyInfo info, NodeState parent)
        throws ItemStateException {
        try {

            // retrieve property definition
            EffectiveNodeType ent = wspManager.getNodeTypeRegistry().getEffectiveNodeType(parent.getNodeTypeNames());
            QPropertyDefinition def = ent.getApplicablePropertyDefinition(info.getQName(), info.getType(), info.isMultiValued());

            // build the PropertyState
            PropertyState state = new PropertyState(info.getQName(), parent,
                def, Status.EXISTING, this, service.getIdFactory(), true);

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

            state.init(info.getType(), qValues);
            state.addListener(cache);
            cache.created(state);

            return state;
        } catch (IOException e) {
            throw new ItemStateException(e.getMessage(), e);
        } catch (NodeTypeConflictException e) {
            String msg = "internal error: failed to build property state.";
            log.debug(msg);
            throw new ItemStateException(msg, e);
        } catch (RepositoryException e) {
            String msg = "internal error: failed to build property state.";
            log.debug(msg);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     *
     * @param cache
     * @see ItemStateFactory#setCache(ItemStateCache)
     */
    public void setCache(ItemStateCache cache) {
        this.cache = cache;
    }

    //-----------------------------------------------------< NodeReferences >---
    /**
     * <code>NodeReferences</code> represents the references (i.e. properties of
     * type <code>REFERENCE</code>) to a particular node (denoted by its uuid).
     */
    private class NodeReferencesImpl implements NodeReferences {

        private NodeState nodeState;

        /**
         * Private constructor
         *
         * @param nodeState
         * @param referenceIds
         */
        private NodeReferencesImpl(NodeState nodeState, PropertyId[] referenceIds) {
            this.nodeState = nodeState;

            // TODO: improve. make usage of the references returned
            // with NodeInfo that was just retrieved and implement a notification
            // mechanism that updates this NodeReference object if references
            // are modified.
        }

        //-------------------------------------------------< NodeReferences >---
        /**
         * @see NodeReferences#isEmpty()
         */
        public boolean isEmpty() {
            // shortcut
            if (nodeState.getUUID() == null) {
                return true;
            }
            // nodestate has a uuid and is potentially mix:referenceable
            // => try to retrieve references
            try {
                NodeInfo info = service.getNodeInfo(sessionInfo, nodeState.getNodeId());
                return info.getReferences().length > 0;
            } catch (RepositoryException e) {
                log.error("Internal error.",e);
                return false;
            }
        }

        /**
         * @see NodeReferences#iterator()
         */
        public Iterator iterator() {
            // shortcut
            if (nodeState.getUUID() == null) {
                return Collections.EMPTY_SET.iterator();
            }
            // nodestate has a uuid and is potentially mix:referenceable
            // => try to retrieve references
            try {
                NodeInfo info = service.getNodeInfo(sessionInfo, nodeState.getNodeId());
                if (info.getReferences().length > 0) {
                    Set referenceIds = new HashSet();
                    referenceIds.addAll(Arrays.asList(info.getReferences()));
                    return Collections.unmodifiableSet(referenceIds).iterator();
                } else {
                    return Collections.EMPTY_SET.iterator();
                }
            } catch (RepositoryException e) {
                log.error("Internal error.", e);
                return Collections.EMPTY_SET.iterator();
            }
        }
    }
}