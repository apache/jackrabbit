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
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.jcr2spi.WorkspaceManager;
import org.apache.jackrabbit.jcr2spi.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEntry;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.MalformedPathException;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>WorkspaceItemStateFactory</code>...
 */
public class WorkspaceItemStateFactory extends AbstractItemStateFactory implements ItemStateFactory {

    private static Logger log = LoggerFactory.getLogger(WorkspaceItemStateFactory.class);

    private final RepositoryService service;
    private final SessionInfo sessionInfo;
    private final WorkspaceManager wspManager;

    public WorkspaceItemStateFactory(RepositoryService service, SessionInfo sessionInfo,
                                     WorkspaceManager wspManager) {
        this.service = service;
        this.sessionInfo = sessionInfo;
        this.wspManager = wspManager;
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#createRootState(NodeEntry)
     * @param entry
     */
    public NodeState createRootState(NodeEntry entry) throws ItemStateException {
        try {
            NodeInfo info = service.getNodeInfo(sessionInfo, service.getRootId(sessionInfo));
            return createNodeState(info, entry);
        } catch (RepositoryException e) {
            throw new ItemStateException("Internal error while building root state.");
        }
    }

    /**
     * Creates the node with information retrieved from the
     * <code>RepositoryService</code>.
     *
     * @inheritDoc
     * @see ItemStateFactory#createNodeState(NodeId,NodeEntry)
     */
    public NodeState createNodeState(NodeId nodeId, NodeEntry entry)
            throws NoSuchItemStateException, ItemStateException {
        try {
            NodeInfo info = service.getNodeInfo(sessionInfo, nodeId);
            return createNodeState(info, entry);
        } catch (PathNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (ItemNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (RepositoryException e) {
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    public NodeState createDeepNodeState(NodeId nodeId, NodeEntry anyParent) throws NoSuchItemStateException, ItemStateException {
        try {
            NodeInfo info = service.getNodeInfo(sessionInfo, nodeId);
            // node for nodeId exists -> build missing entries in hierarchy
            // Note, that the path contained in NodeId does not reveal which
            // entries are missing -> calculate relative path.
            Path anyParentPath = anyParent.getPath();
            Path relPath = anyParentPath.computeRelativePath(info.getPath());
            Path.PathElement[] missingElems = relPath.getElements();

            NodeEntry entry = anyParent;
            for (int i = 0; i < missingElems.length; i++) {
                QName name = missingElems[i].getName();
                int index = missingElems[i].getNormalizedIndex();
                if (entry.hasNodeEntry(name, index)) {
                    entry = entry.getNodeEntry(name, index);
                } else {
                    entry = entry.addNodeEntry(name, null, index);
                }
            }
            if (entry == anyParent) {
                throw new ItemStateException("Internal error while getting deep itemState");
            }
            return createNodeState(info, entry);
        } catch (PathNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (ItemNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        }  catch (RepositoryException e) {
            throw new ItemStateException(e.getMessage(), e);
        } catch (MalformedPathException e) {
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    /**
     * Creates the property with information retrieved from the
     * <code>RepositoryService</code>.
     *
     * @inheritDoc
     * @see ItemStateFactory#createPropertyState(PropertyId,PropertyEntry)
     */
    public PropertyState createPropertyState(PropertyId propertyId,
                                             PropertyEntry entry)
            throws NoSuchItemStateException, ItemStateException {
        try {
            PropertyInfo info = service.getPropertyInfo(sessionInfo, propertyId);
            return createPropertyState(info, entry);
        } catch (PathNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (ItemNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (RepositoryException e) {
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    public PropertyState createDeepPropertyState(PropertyId propertyId, NodeEntry anyParent) throws NoSuchItemStateException, ItemStateException {
        try {
            PropertyInfo info = service.getPropertyInfo(sessionInfo, propertyId);
            // prop for propertyId exists -> build missing entries in hierarchy
            // Note, that the path contained in PropertyId does not reveal which
            // entries are missing -> calculate relative path.
            Path anyParentPath = anyParent.getPath();
            Path relPath = anyParentPath.computeRelativePath(info.getPath());
            Path.PathElement[] missingElems = relPath.getElements();
            NodeEntry entry = anyParent;
            int i = 0;
            // NodeEntries except for the very last 'missingElem'
            while (i < missingElems.length - 1) {
                QName name = missingElems[i].getName();
                int index = missingElems[i].getNormalizedIndex();
                if (entry.hasNodeEntry(name, index)) {
                    entry = entry.getNodeEntry(name, index);
                } else {
                    entry = entry.addNodeEntry(name, null, index);
                }
                i++;
            }
            // create PropertyEntry for the last element if not existing yet
            QName propName = missingElems[i].getName();
            PropertyEntry propEntry;
            if (!entry.hasPropertyEntry(propName)) {
                propEntry = entry.addPropertyEntry(propName);
            } else {
                propEntry = entry.getPropertyEntry(propName);
            }
            return createPropertyState(info, propEntry);
        } catch (PathNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (ItemNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (RepositoryException e) {
            throw new ItemStateException(e.getMessage(), e);
        } catch (MalformedPathException e) {
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#getChildNodeInfos(NodeId)
     * @param nodeId
     */
    public Iterator getChildNodeInfos(NodeId nodeId)
        throws NoSuchItemStateException, ItemStateException {
        try {
            return service.getChildInfos(sessionInfo, nodeId);
        } catch (PathNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (ItemNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (RepositoryException e) {
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#getNodeReferences(NodeState)
     * @param nodeState
     */
    public NodeReferences getNodeReferences(NodeState nodeState) {
        nodeState.checkIsWorkspaceState();
        // shortcut
        if (nodeState.getUniqueID() == null || !nodeState.hasPropertyName(QName.JCR_UUID)) {
            // for sure not referenceable
            return EmptyNodeReferences.getInstance();
        }

        // nodestate has a unique ID and is potentially mix:referenceable
        // => try to retrieve references
        try {
            NodeInfo info = service.getNodeInfo(sessionInfo, nodeState.getNodeId());
            return new NodeReferencesImpl(info.getReferences());
        } catch (RepositoryException e) {
            log.debug("No references for NodeState " + nodeState);
            return EmptyNodeReferences.getInstance();
        }
    }

    //------------------------------------------------------------< private >---
    /**
     * Creates the node with information retrieved from <code>info</code>.
     *
     * @param info the <code>NodeInfo</code> to use to create the <code>NodeState</code>.
     * @param entry
     * @return the new <code>NodeState</code>.
     */
    private NodeState createNodeState(NodeInfo info, NodeEntry entry) throws NoSuchItemStateException, RepositoryException {
        assertMatchingPath(info, entry);

        // make sure the entry has the correct ItemId
        // this make not be the case, if the hierachy has not been completely
        // resolved yet -> if uniqueID is present, set it on this entry or on
        // the appropriate parent entry
        String uniqueID = info.getId().getUniqueID();
        Path path = info.getId().getPath();
        if (path == null) {
            entry.setUniqueID(uniqueID);
        } else if (uniqueID != null) {
            // uniqueID that applies to a parent NodeEntry -> get parentEntry
            NodeEntry parent = getAncestor(entry, path.getLength());
            parent.setUniqueID(uniqueID);
        }

        // retrieve definition
        NodeTypeRegistry ntReg = wspManager.getNodeTypeRegistry();
        QNodeDefinition definition = null;
        NodeEntry parent = entry.getParent();
        if (parent == null) {
            // special case for root state
            definition = wspManager.getNodeTypeRegistry().getRootNodeDef();
        } else if (parent.isAvailable() && parent.getStatus() != Status.INVALIDATED) {
            // try to retrieve definition if the parent is available
            try {
                NodeState parentState = parent.getNodeState();
                EffectiveNodeType ent = ntReg.getEffectiveNodeType(parentState.getNodeTypeNames());
                definition = ent.getApplicableNodeDefinition(info.getQName(), info.getNodetype(), ntReg);
            } catch (RepositoryException e) {
                // should not get here
                log.warn("Internal error", e);
            } catch (ItemStateException e) {
                // should not get here
                log.warn("Internal error", e);
            } catch (NodeTypeConflictException e) {
                // should not get here
               log.warn("Internal error", e);
            }
        }

        // now build the nodestate itself
        NodeState state = new NodeState(entry, info.getNodetype(), info.getMixins(), definition, Status.EXISTING, true, this, ntReg);

        // update NodeEntry from the information present in the NodeInfo (prop entries)
        List propNames = new ArrayList();
        for (IdIterator it = info.getPropertyIds(); it.hasNext(); ) {
            PropertyId pId = (PropertyId) it.nextId();
            QName propertyName = pId.getQName();
            propNames.add(propertyName);
        }
        try {
            entry.addPropertyEntries(propNames);
        } catch (ItemExistsException e) {
            // should not get here
            log.warn("Internal error", e);
        }

        notifyCreated(state);
        return state;
    }

    /**
     * Creates the property with information retrieved from <code>info</code>.
     *
     * @param info   the <code>PropertyInfo</code> to use to create the
     *               <code>PropertyState</code>.
     * @param entry
     * @return the new <code>PropertyState</code>.
     */
    private PropertyState createPropertyState(PropertyInfo info, PropertyEntry entry) throws NoSuchItemStateException, RepositoryException {
        assertMatchingPath(info, entry);

        // make sure uuid part of id is correct
        String uniqueID = info.getId().getUniqueID();
        if (uniqueID != null) {
            // uniqueID always applies to a parent NodeEntry -> get parentEntry
            NodeEntry parent = getAncestor(entry, info.getId().getPath().getLength());
            parent.setUniqueID(uniqueID);
        }

        QPropertyDefinition definition = null;
        // try to retrieve property definition
        if (entry.getParent().isAvailable() && entry.getStatus() != Status.INVALIDATED) {
            NodeState parentState = null;
            try {
                parentState = entry.getParent().getNodeState();
                EffectiveNodeType ent = wspManager.getNodeTypeRegistry().getEffectiveNodeType(parentState.getNodeTypeNames());
                QPropertyDefinition defs[] = ent.getApplicablePropertyDefinitions(info.getQName(), info.getType(), info.isMultiValued());
                if (defs.length == 1) {
                    definition = defs[0];
                }
                else {
                    definition = service.getPropertyDefinition(sessionInfo, entry.getId());
                }
            } catch (ItemStateException e) {
                // should not get here
                log.warn("Internal error", e);
            } catch (RepositoryException e) {
                // should not get here
                log.warn("Internal error", e);
            } catch (NodeTypeConflictException e) {
                // should not get here
                log.warn("Internal error", e);
            }
        }

        // build the PropertyState
        PropertyState state = new PropertyState(entry, info.isMultiValued(), definition, Status.EXISTING, true, this, wspManager.getNodeTypeRegistry());
        state.init(info.getType(), info.getValues());

        //state.addListener(cache);
        //cache.created(state);
        notifyCreated(state);

        return state;
    }

    /**
     * Validation check: Path of the given ItemInfo must match to the Path of
     * the HierarchyEntry. This is required for Items that are identified by
     * a uniqueID that may move within the hierarchy upon restore or clone.
     */
    private void assertMatchingPath(ItemInfo info, HierarchyEntry entry) throws NoSuchItemStateException, RepositoryException {
        if (!info.getPath().equals(entry.getPath())) {
            throw new NoSuchItemStateException("HierarchyEntry does not belong the given ItemInfo.");
        }
    }

    private static NodeEntry getAncestor(HierarchyEntry entry, int degree) {
        NodeEntry parent = entry.getParent();
        degree--;
        while (parent != null && degree > 0) {
            parent = parent.getParent();
            degree--;
        }
        if (degree != 0) {
            throw new IllegalArgumentException();
        }
        return parent;
    }
    //-----------------------------------------------------< NodeReferences >---
    /**
     * <code>NodeReferences</code> represents the references (i.e. properties of
     * type <code>REFERENCE</code>) to a particular node (denoted by its unique ID).
     */
    private class NodeReferencesImpl implements NodeReferences {

        private PropertyId[] references;

        /**
         * Private constructor
         *
         * @param references
         */
        private NodeReferencesImpl(PropertyId[] references) {
            this.references = references;
        }

        //-------------------------------------------------< NodeReferences >---
        /**
         * @see NodeReferences#isEmpty()
         */
        public boolean isEmpty() {
            return references.length <= 0;
        }

        /**
         * @see NodeReferences#iterator()
         */
        public Iterator iterator() {
            if (references.length > 0) {
                Set referenceIds = new HashSet();
                referenceIds.addAll(Arrays.asList(references));
                return Collections.unmodifiableSet(referenceIds).iterator();
            } else {
                return Collections.EMPTY_SET.iterator();
            }
        }
    }
}