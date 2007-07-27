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
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
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
    private final ItemDefinitionProvider definitionProvider;

    public WorkspaceItemStateFactory(RepositoryService service, SessionInfo sessionInfo,
                                     ItemDefinitionProvider definitionProvider) {
        this.service = service;
        this.sessionInfo = sessionInfo;
        this.definitionProvider = definitionProvider;
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#createRootState(NodeEntry)
     */
    public NodeState createRootState(NodeEntry entry) throws ItemNotFoundException, RepositoryException {
        return createNodeState(service.getRootId(sessionInfo), entry);
    }

    /**
     * Creates the node with information retrieved from the
     * <code>RepositoryService</code>.
     *
     * @inheritDoc
     * @see ItemStateFactory#createNodeState(NodeId,NodeEntry)
     */
    public NodeState createNodeState(NodeId nodeId, NodeEntry entry)
            throws ItemNotFoundException, RepositoryException {
        // build new node state from server information
        try {
            NodeState nodeState;
            if (entry.getStatus() == Status.INVALIDATED) {
                // simple reload -> don't use batch-read
                NodeInfo nInfo = service.getNodeInfo(sessionInfo, nodeId);
                nodeState = createItemStates(nodeId, Collections.singletonList(nInfo).iterator(), entry, false);
            } else {
                Iterator infos = service.getItemInfos(sessionInfo, nodeId);
                nodeState = createItemStates(nodeId, infos, entry, false);
            }
            if (nodeState == null) {
                throw new ItemNotFoundException("HierarchyEntry does not belong to any existing ItemInfo.");
            }
            return nodeState;
        } catch (PathNotFoundException e) {
            throw new ItemNotFoundException(e.getMessage(), e);
        }
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#createDeepNodeState(NodeId,NodeEntry)
     */
    public NodeState createDeepNodeState(NodeId nodeId, NodeEntry anyParent) throws ItemNotFoundException, RepositoryException {
        try {
            Iterator infos = service.getItemInfos(sessionInfo, nodeId);
            return createItemStates(nodeId, infos, anyParent, true);
        } catch (PathNotFoundException e) {
            throw new ItemNotFoundException(e.getMessage(), e);
        }
    }

    /**
     * Creates the PropertyState with information retrieved from the
     * <code>RepositoryService</code>.
     *
     * @inheritDoc
     * @see ItemStateFactory#createPropertyState(PropertyId,PropertyEntry)
     */
    public PropertyState createPropertyState(PropertyId propertyId,
                                             PropertyEntry entry)
            throws ItemNotFoundException, RepositoryException {
        try {
            PropertyInfo info = service.getPropertyInfo(sessionInfo, propertyId);
            assertMatchingPath(info, entry);
            return createPropertyState(info, entry);
        } catch (PathNotFoundException e) {
            throw new ItemNotFoundException(e.getMessage());
        }
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#createDeepPropertyState(PropertyId,NodeEntry)
     */
    public PropertyState createDeepPropertyState(PropertyId propertyId, NodeEntry anyParent) throws ItemNotFoundException, RepositoryException {
        try {
            PropertyInfo info = service.getPropertyInfo(sessionInfo, propertyId);
            return createDeepPropertyState(info, anyParent);
        } catch (PathNotFoundException e) {
            throw new ItemNotFoundException(e.getMessage());
        }
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#getChildNodeInfos(NodeId)
     * @param nodeId
     */
    public Iterator getChildNodeInfos(NodeId nodeId)
            throws ItemNotFoundException, RepositoryException {
        return service.getChildInfos(sessionInfo, nodeId);
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#getNodeReferences(NodeState)
     * @param nodeState
     */
    public NodeReferences getNodeReferences(NodeState nodeState) {
        NodeEntry entry = nodeState.getNodeEntry();
        // shortcut
        if (entry.getUniqueID() == null || !entry.hasPropertyEntry(QName.JCR_UUID)) {
            // for sure not referenceable
            return EmptyNodeReferences.getInstance();
        }

        // nodestate has a unique ID and is potentially mix:referenceable
        // => try to retrieve references
        try {
            NodeInfo nInfo = service.getNodeInfo(sessionInfo, entry.getWorkspaceId());
            return new NodeReferencesImpl(nInfo.getReferences());
        } catch (RepositoryException e) {
            // ignore
        }
        // exception or no matching entry found.
        log.debug("Unable to determine references for NodeState " + nodeState);
        return EmptyNodeReferences.getInstance();
    }

    //------------------------------------------------------------< private >---
    /**
     *
     * @param nodeId
     * @param itemInfos
     * @param entry
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    private synchronized NodeState createItemStates(NodeId nodeId,
                                                    Iterator itemInfos,
                                                    NodeEntry entry,
                                                    boolean isDeep)
            throws ItemNotFoundException, RepositoryException {
        NodeState nodeState;
        // first entry in the iterator is the originally requested Node.
        if (itemInfos.hasNext()) {
            NodeInfo first = (NodeInfo) itemInfos.next();
            if (isDeep) {
                // for a deep state, the hierarchy entry does not correspond to
                // the given NodeEntry -> retrieve NodeState before executing
                // validation check.
                nodeState = createDeepNodeState(first, entry);
                assertMatchingPath(first, nodeState.getNodeEntry());
            } else {
                // 'isDeep' == false -> the given NodeEntry must match to the
                // first ItemInfo retrieved from the iterator.
                assertMatchingPath(first, entry);
                nodeState = createNodeState(first, entry);
            }
        } else {
            // empty iterator
            throw new ItemNotFoundException("Node with id " + nodeId + " could not be found.");
        }

        // deal with all additional ItemInfos that may be present.
        NodeEntry parentEntry = nodeState.getNodeEntry();
        if (parentEntry.getStatus() != Status.INVALIDATED) {
            while (itemInfos.hasNext()) {
                ItemInfo info = (ItemInfo) itemInfos.next();
                if (info.denotesNode()) {
                    createDeepNodeState((NodeInfo) info, parentEntry);
                } else {
                    createDeepPropertyState((PropertyInfo) info, parentEntry);
                }
            }
        }
        return nodeState;
    }

    /**
     * Creates the node with information retrieved from <code>info</code>.
     *
     * @param info the <code>NodeInfo</code> to use to create the <code>NodeState</code>.
     * @param entry
     * @return the new <code>NodeState</code>.
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    private NodeState createNodeState(NodeInfo info, NodeEntry entry) throws ItemNotFoundException, RepositoryException {
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

        // now build the nodestate itself
        NodeState state = new NodeState(entry, info, this, definitionProvider);

        // update NodeEntry from the information present in the NodeInfo (prop entries)
        List propNames = new ArrayList();
        for (Iterator it = info.getPropertyIds(); it.hasNext(); ) {
            PropertyId pId = (PropertyId) it.next();
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
    private PropertyState createPropertyState(PropertyInfo info, PropertyEntry entry) {
        // make sure uuid part of id is correct
        String uniqueID = info.getId().getUniqueID();
        if (uniqueID != null) {
            // uniqueID always applies to a parent NodeEntry -> get parentEntry
            NodeEntry parent = getAncestor(entry, info.getId().getPath().getLength());
            parent.setUniqueID(uniqueID);
        }

        // build the PropertyState
        PropertyState state = new PropertyState(entry, info, this, definitionProvider);

        notifyCreated(state);
        return state;
    }

    /**
     *
     * @param info
     * @param anyParent
     * @return
     * @throws RepositoryException
     */
    private NodeState createDeepNodeState(NodeInfo info, NodeEntry anyParent) throws RepositoryException {
        try {
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
                entry = createIntermediateNodeEntry(entry, name, index);
            }
            if (entry == anyParent) {
                throw new RepositoryException("Internal error while getting deep itemState");
            }
            return createNodeState(info, entry);
        } catch (PathNotFoundException e) {
            throw new ItemNotFoundException(e.getMessage(), e);
        } catch (MalformedPathException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
    }

    /**
     *
     * @param info
     * @param anyParent
     * @return
     * @throws RepositoryException
     */
    private PropertyState createDeepPropertyState(PropertyInfo info, NodeEntry anyParent) throws RepositoryException {
        try {
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
                entry = createIntermediateNodeEntry(entry, name, index);
                i++;
            }
            // create PropertyEntry for the last element if not existing yet
            QName propName = missingElems[i].getName();
            PropertyEntry propEntry = entry.getPropertyEntry(propName);
            if (propEntry == null) {
                propEntry = entry.addPropertyEntry(propName);
            }
            return createPropertyState(info, propEntry);
        } catch (PathNotFoundException e) {
            throw new ItemNotFoundException(e.getMessage());
        } catch (MalformedPathException e) {
            throw new RepositoryException(e.getMessage());
        }
    }

    /**
     *
     * @param parentEntry
     * @param name
     * @param index
     * @return
     * @throws RepositoryException
     */
    private static NodeEntry createIntermediateNodeEntry(NodeEntry parentEntry, QName name, int index) throws RepositoryException {
        NodeEntry entry;
        if (parentEntry.hasNodeEntry(name, index)) {
            entry = parentEntry.getNodeEntry(name, index);
        } else {
            entry = parentEntry.addNodeEntry(name, null, index);
        }
        return entry;
    }

    /**
     * Validation check: Path of the given ItemInfo must match to the Path of
     * the HierarchyEntry. This is required for Items that are identified by
     * a uniqueID that may move within the hierarchy upon restore or clone.
     *
     * @param info
     * @param entry
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    private static void assertMatchingPath(ItemInfo info, HierarchyEntry entry)
            throws ItemNotFoundException, RepositoryException {
        Path infoPath = info.getPath();
        if (!infoPath.equals(entry.getWorkspacePath())) {
            // TODO: handle external move of nodes (parents) identified by uniqueID
            throw new ItemNotFoundException("HierarchyEntry does not belong the given ItemInfo.");
        }
    }

    /**
     *
     * @param entry
     * @param degree
     * @return
     */
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
