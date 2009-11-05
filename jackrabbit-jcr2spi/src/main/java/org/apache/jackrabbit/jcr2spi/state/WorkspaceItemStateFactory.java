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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Collections;
import java.util.Set;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        IdFactory idFactory = service.getIdFactory();
        PathFactory pf = service.getPathFactory();

        return createNodeState(idFactory.createNodeId((String) null, pf.getRootPath()), entry);
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
            Iterator<? extends ItemInfo> infos = service.getItemInfos(sessionInfo, nodeId);
            NodeState nodeState = createItemStates(nodeId, infos, entry, false);

            if (nodeState == null) {
                throw new ItemNotFoundException("HierarchyEntry does not belong to any existing ItemInfo.");
            }
            return nodeState;
        } catch (PathNotFoundException e) {
            throw new ItemNotFoundException(e.getMessage());
        }
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#createDeepNodeState(NodeId,NodeEntry)
     */
    public NodeState createDeepNodeState(NodeId nodeId, NodeEntry anyParent) throws ItemNotFoundException, RepositoryException {
        try {
            Iterator<? extends ItemInfo> infos = service.getItemInfos(sessionInfo, nodeId);
            return createItemStates(nodeId, infos, anyParent, true);
        } catch (PathNotFoundException e) {
            throw new ItemNotFoundException(e.getMessage());
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
            PropertyState propState = createDeepPropertyState(info, anyParent, null);
            assertValidState(propState, info);
            return propState;
        } catch (PathNotFoundException e) {
            throw new ItemNotFoundException(e.getMessage());
        }
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#getChildNodeInfos(NodeId)
     * @param nodeId
     */
    public Iterator<ChildInfo> getChildNodeInfos(NodeId nodeId)
            throws ItemNotFoundException, RepositoryException {
        return service.getChildInfos(sessionInfo, nodeId);
    }

    /**
     * @inheritDoc
     * @see ItemStateFactory#getNodeReferences(NodeState,org.apache.jackrabbit.spi.Name,boolean)
     */
    public Iterator<PropertyId> getNodeReferences(NodeState nodeState, Name propertyName, boolean weak) {
        NodeEntry entry = nodeState.getNodeEntry();
        // shortcut
        if (entry.getUniqueID() == null
                || !entry.hasPropertyEntry(NameConstants.JCR_UUID)) {
            // for sure not referenceable
            Set<PropertyId> t = Collections.emptySet();
            return t.iterator();
        }

        // nodestate has a unique ID and is potentially mix:referenceable
        // => try to retrieve references
        try {
            return service.getReferences(sessionInfo, entry.getWorkspaceId(), propertyName, weak);
        } catch (RepositoryException e) {
            log.debug("Unable to determine references to {}", nodeState);
            Set<PropertyId> t = Collections.emptySet();
            return t.iterator();
        }
    }

    //------------------------------------------------------------< private >---
    /**
     *
     * @param nodeId
     * @param itemInfos
     * @param entry
     * @param isDeep
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    private synchronized NodeState createItemStates(NodeId nodeId, Iterator<? extends ItemInfo> itemInfos,
                                                    NodeEntry entry, boolean isDeep)
            throws ItemNotFoundException, RepositoryException {
        NodeState nodeState;
        ItemInfos infos = new ItemInfos(itemInfos);
        // first entry in the iterator is the originally requested Node.
        if (infos.hasNext()) {
            NodeInfo first = (NodeInfo) infos.next();
            if (isDeep) {
                // for a deep state, the hierarchy entry does not correspond to
                // the given NodeEntry -> retrieve NodeState before executing
                // validation check.
                nodeState = createDeepNodeState(first, entry, infos);
                assertValidState(nodeState, first);
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
        // Assuming locality of the itemInfos, we keep an estimate of a parent entry.
        // This reduces the part of the hierarchy to traverse. For large batches this
        // optimization results in about 25% speed up.
        NodeEntry approxParentEntry = nodeState.getNodeEntry();
        while (infos.hasNext()) {
            ItemInfo info = infos.next();
            if (info.denotesNode()) {
                approxParentEntry = createDeepNodeState((NodeInfo) info, approxParentEntry, infos).getNodeEntry();
            } else {
                createDeepPropertyState((PropertyInfo) info, approxParentEntry, infos);
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
        // this make not be the case, if the hierarchy has not been completely
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

        int previousStatus = entry.getStatus();
        if (Status.isTransient(previousStatus) || Status.isStale(previousStatus)) {
            log.debug("Node has pending changes; omit resetting the state.");
            return entry.getNodeState();
        }

        // update NodeEntry from the information present in the NodeInfo (prop entries)
        List<Name> propNames = new ArrayList<Name>();
        for (Iterator<PropertyId> it = info.getPropertyIds(); it.hasNext(); ) {
            PropertyId pId = it.next();
            Name propertyName = pId.getName();
            propNames.add(propertyName);
        }
        try {
            entry.setPropertyEntries(propNames);
        } catch (ItemExistsException e) {
            // should not get here
            log.warn("Internal error", e);
        }

        // unless the child-info are omitted by the SPI impl -> make sure
        // the child entries the node entry are initialized or updated.
        Iterator<ChildInfo> childInfos = info.getChildInfos();
        if (childInfos != null) {
            entry.setNodeEntries(childInfos);
        }

        // now build or update the nodestate itself
        NodeState tmp = new NodeState(entry, info, this, definitionProvider);
        entry.setItemState(tmp);

        NodeState nState = entry.getNodeState();
        if (previousStatus == Status._UNDEFINED_) {
            // tmp state was used as resolution for the given entry i.e. the
            // entry was not available before. otherwise the 2 states were
            // merged. see HierarchyEntryImpl#setItemState
            notifyCreated(nState);
        } else {
            notifyUpdated(nState, previousStatus);
        }
        return nState;
    }

    /**
     * Creates the property with information retrieved from <code>info</code>.
     *
     * @param info   the <code>PropertyInfo</code> to use to create the
     *               <code>PropertyState</code>.
     * @param entry
     * @return the new <code>PropertyState</code>.
     */
    private PropertyState createPropertyState(PropertyInfo info, PropertyEntry entry)
            throws RepositoryException {
        // make sure uuid part of id is correct
        String uniqueID = info.getId().getUniqueID();
        if (uniqueID != null) {
            // uniqueID always applies to a parent NodeEntry -> get parentEntry
            NodeEntry parent = getAncestor(entry, info.getId().getPath().getLength());
            parent.setUniqueID(uniqueID);
        }

        int previousStatus = entry.getStatus();
        if (Status.isTransient(previousStatus) || Status.isStale(previousStatus)) {
            log.debug("Property has pending changes; omit resetting the state.");
            return entry.getPropertyState();
        }

        // now build or update the nodestate itself
        PropertyState tmp = new PropertyState(entry, info, this, definitionProvider);
        entry.setItemState(tmp);

        PropertyState pState = entry.getPropertyState();
        if (previousStatus == Status._UNDEFINED_) {
            // tmp state was used as resolution for the given entry i.e. the
            // entry was not available before. otherwise the 2 states were
            // merged. see HierarchyEntryImpl#setItemState
            notifyCreated(pState);
        }  else {
            notifyUpdated(pState, previousStatus);
        }
        return pState;
    }

    /**
     *
     * @param info
     * @param anyParent
     * @return
     * @throws RepositoryException
     */
    private NodeState createDeepNodeState(NodeInfo info, NodeEntry anyParent, ItemInfos infos) throws RepositoryException {
        try {
            // node for nodeId exists -> build missing entries in hierarchy
            // Note, that the path contained in NodeId does not reveal which
            // entries are missing -> calculate relative path.
            Path anyParentPath = anyParent.getWorkspacePath();
            Path relPath = anyParentPath.computeRelativePath(info.getPath());
            Path.Element[] missingElems = relPath.getElements();

            if (startsWithIllegalElement(missingElems)) {
                log.error("Relative path to NodeEntry starts with illegal element -> ignore NodeInfo with path " + info.getPath());
                return null;
            }

            NodeEntry entry = anyParent;
            for (int i = 0; i < missingElems.length; i++) {
                if (missingElems[i].denotesParent()) {
                    // Walk up the hierarchy for 'negative' paths
                    // until the smallest common root is found
                    entry = entry.getParent();
                }
                else if (missingElems[i].denotesName()) {
                    // Add missing elements starting from the smallest common root
                    Name name = missingElems[i].getName();
                    int index = missingElems[i].getNormalizedIndex();
                    entry = createIntermediateNodeEntry(entry, name, index, infos);
                }
                // else denotesCurrent -> ignore
            }
            if (entry == anyParent) {
                throw new RepositoryException("Internal error while getting deep itemState");
            }
            return createNodeState(info, entry);
        } catch (PathNotFoundException e) {
            throw new ItemNotFoundException(e.getMessage());
        }
    }

    /**
     *
     * @param info
     * @param anyParent
     * @return
     * @throws RepositoryException
     */
    private PropertyState createDeepPropertyState(PropertyInfo info, NodeEntry anyParent, ItemInfos infos) throws RepositoryException {
        try {
            // prop for propertyId exists -> build missing entries in hierarchy
            // Note, that the path contained in PropertyId does not reveal which
            // entries are missing -> calculate relative path.
            Path anyParentPath = anyParent.getWorkspacePath();
            Path relPath = anyParentPath.computeRelativePath(info.getPath());
            Path.Element[] missingElems = relPath.getElements();

            // make sure the missing elements don't start with . or .. in which
            // case the info is not within the tree as it is expected
            // (see also JCR-1797)
            if (startsWithIllegalElement(missingElems)) {
                log.error("Relative path to PropertyEntry starts with illegal element -> ignore PropertyInfo with path " + info.getPath());
                return null;
            }

            NodeEntry entry = anyParent;
            int i = 0;
            // NodeEntries except for the very last 'missingElem'
            while (i < missingElems.length - 1) {
                if (missingElems[i].denotesParent()) {
                    // Walk up the hierarchy for 'negative' paths
                    // until the smallest common root is found
                    entry = entry.getParent();
                }
                else if (missingElems[i].denotesName()) {
                    // Add missing elements starting from the smallest common root
                    Name name = missingElems[i].getName();
                    int index = missingElems[i].getNormalizedIndex();
                    entry = createIntermediateNodeEntry(entry, name, index, infos);
                }
                // else denotesCurrent -> ignore
                i++;
            }
            // create PropertyEntry for the last element if not existing yet
            Name propName = missingElems[i].getName();
            PropertyEntry propEntry = entry.getOrAddPropertyEntry(propName);

            return createPropertyState(info, propEntry);
        } catch (PathNotFoundException e) {
            throw new ItemNotFoundException(e.getMessage());
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
    private static NodeEntry createIntermediateNodeEntry(NodeEntry parentEntry,
                                                         Name name, int index,
                                                         ItemInfos infos) throws RepositoryException {
        if (infos != null) {
            Iterator<ChildInfo> childInfos = infos.getChildInfos(parentEntry.getWorkspaceId());
            if (childInfos != null) {
                parentEntry.setNodeEntries(childInfos);
            }
        }
        NodeEntry entry = parentEntry.getOrAddNodeEntry(name, index, null);
        return entry;
    }

    /**
     * Validation check: make sure the state is not null (was really created)
     * and matches with the specified ItemInfo (path).
     *
     * @param state
     * @param info
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    private static void assertValidState(ItemState state, ItemInfo info)
            throws ItemNotFoundException, RepositoryException {
        if (state == null) {
            throw new ItemNotFoundException("HierarchyEntry does not belong to any existing ItemInfo. No ItemState was created.");
        }
        assertMatchingPath(info, state.getHierarchyEntry());
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
     * Returns true if the given <code>missingElems</code> start with
     * the root element, in which case the info is not within
     * the tree as it is expected.
     * See also #JCR-1797 for the corresponding enhancement request.
     *
     * @param missingElems
     * @return true if the first element doesn't denote a named element.
     */
    private static boolean startsWithIllegalElement(Path.Element[] missingElems) {
        if (missingElems.length > 0) {
            return missingElems[0].denotesRoot();
        }
        return false;
    }

    /**
     * @param entry
     * @param degree
     * @return the ancestor entry at the specified degree.
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

    //--------------------------------------------------------------------------
    /**
     * Iterator
     */
    private class ItemInfos implements Iterator<ItemInfo> {

        private final List<ItemInfo> prefetchQueue = new ArrayList<ItemInfo>();
        private final Map<NodeId, NodeInfo> nodeInfos = new HashMap<NodeId, NodeInfo>();
        private final Iterator<? extends ItemInfo> infos;

        private ItemInfos(Iterator<? extends ItemInfo> infos) {
            super();
            this.infos = infos;
        }

        // ------------------------------------------------------< Iterator >---
        /**
         * @see Iterator#hasNext()
         */
        public boolean hasNext() {
            if (!prefetchQueue.isEmpty()) {
                return true;
            } else {
                return prefetch();
            }
        }

        /**
         * @see Iterator#next()
         */
        public ItemInfo next() {
            if (prefetchQueue.isEmpty()) {
                throw new NoSuchElementException();
            } else {
                ItemInfo next = prefetchQueue.remove(0);
                if (next instanceof NodeInfo) {
                    nodeInfos.remove(((NodeInfo) next).getId());
                }
                return next;
            }
        }

        /**
         * @see Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

        // -------------------------------------------------------< private >---
        /**
         * @param parentId
         * @return The children <code>NodeInfo</code>s for the parent identified
         * by the given <code>parentId</code> or <code>null</code> if the parent
         * has not been read yet, has already been processed (childInfo is up
         * to date) or does not provide child infos.
         */
        private Iterator<ChildInfo> getChildInfos(NodeId parentId) {
            NodeInfo nodeInfo = nodeInfos.get(parentId);
            while (nodeInfo == null && prefetch()) {
                nodeInfo = nodeInfos.get(parentId);
            }
            return nodeInfo == null? null : nodeInfo.getChildInfos();
        }

        /**
         * @return <code>true</code> if the next info could be retrieved.
         */
        private boolean prefetch() {
            if (infos.hasNext()) {
                ItemInfo info = infos.next();
                prefetchQueue.add(info);
                if (info.denotesNode()) {
                    NodeInfo nodeInfo = (NodeInfo) info;
                    nodeInfos.put(nodeInfo.getId(), nodeInfo);
                }
                return true;
            } else {
                return false;
            }
        }
    }
}
