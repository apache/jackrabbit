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
package org.apache.jackrabbit.jcr2spi.hierarchy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.collections4.iterators.IteratorChain;
import org.apache.jackrabbit.commons.iterator.RangeIteratorAdapter;
import org.apache.jackrabbit.jcr2spi.operation.AddNode;
import org.apache.jackrabbit.jcr2spi.operation.AddProperty;
import org.apache.jackrabbit.jcr2spi.operation.Move;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.operation.Remove;
import org.apache.jackrabbit.jcr2spi.operation.ReorderNodes;
import org.apache.jackrabbit.jcr2spi.operation.SetMixin;
import org.apache.jackrabbit.jcr2spi.operation.SetPrimaryType;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.jcr2spi.util.StateUtility;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>NodeEntryImpl</code> implements common functionality for child
 * node entry implementations.
 */
public class NodeEntryImpl extends HierarchyEntryImpl implements NodeEntry {

    private static Logger log = LoggerFactory.getLogger(NodeEntryImpl.class);

    /**
     * UniqueID identifying this NodeEntry or <code>null</code> if either
     * the underlying state has not been loaded yet or if it cannot be
     * identified with a unique ID.
     */
    private String uniqueID;

    /**
     * Insertion-ordered collection of NodeEntry objects.
     */
    private final ChildNodeEntries childNodeEntries;

    /**
     * Map used to remember transiently removed or moved childNodeEntries, that
     * must not be retrieved from the persistent storage.
     */
    private final ChildNodeAttic childNodeAttic;

    /**
     * Map of properties.<br>
     * Key = {@link Name} of property,<br>
     * Value = {@link PropertyEntry}.
     */
    private final ChildPropertyEntries properties;

    /**
     * Map of properties which are deleted and have been re-created as transient
     * property with the same name.
     */
    private final Map<Name, PropertyEntry> propertiesInAttic;

    /**
     * Upon transient 'move' ('rename') or 'reorder' of SNSs this
     * <code>NodeEntry</code> remembers the original parent, name and index
     * for later revert as well as for the creation of the
     * {@link #getWorkspaceId() workspace id}. Finally the revertInfo is
     * used to find the target of an <code>Event</code> indicating external
     * modification.
     *
     * @see #refresh(Event)
     */
    private RevertInfo revertInfo;

    /**
     * Creates a new <code>NodeEntryImpl</code>
     *
     * @param parent    the <code>NodeEntry</code> that owns this child item
     *                  reference.
     * @param name      the name of the child node.
     * @param factory   the entry factory.
     */
    private NodeEntryImpl(NodeEntryImpl parent, Name name, String uniqueID,
                          EntryFactory factory) {
        super(parent, name, factory);
        this.uniqueID = uniqueID; // NOTE: don't use setUniqueID (for mod only)

        properties = new ChildPropertyEntriesImpl(this, factory);
        childNodeEntries = new ChildNodeEntriesImpl(this, factory, null);

        propertiesInAttic = new HashMap<Name, PropertyEntry>();
        childNodeAttic = new ChildNodeAttic();

        factory.notifyEntryCreated(this);
    }

    /**
     * @return the entry corresponding to the root node.
     */
    static NodeEntry createRootEntry(EntryFactory factory) {
        return new NodeEntryImpl(null, NameConstants.ROOT, null, factory);
    }

    /**
     * @param parent
     * @param name
     * @param uniqueId
     * @param factory
     * @return the created entry.
     */
    static NodeEntry createNodeEntry(NodeEntryImpl parent, Name name, String uniqueId, EntryFactory factory) {
        return new NodeEntryImpl(parent, name, uniqueId, factory);
    }

    //-----------------------------------------------------< HierarchyEntry >---
    /**
     * Returns true.
     *
     * @see HierarchyEntry#denotesNode()
     */
    public boolean denotesNode() {
        return true;
    }

    /**
     * If 'recursive' is true, the complete hierarchy below this entry is
     * traversed and reloaded. Otherwise only this entry and the direct
     * descendants are reloaded.
     *
     * @see HierarchyEntry#reload(boolean)
     */
    @Override
    public void reload(boolean recursive) {
        // reload this entry
        super.reload(recursive);

        // reload all children unless 'recursive' is false and the reload above
        // did not cause this entry to be removed -> therefore check status.
        if (recursive && !Status.isTerminal(getStatus())) {
            // recursively reload all entries including props that are in the attic.
            for (Iterator<HierarchyEntry> it = getAllChildEntries(true); it.hasNext();) {
                HierarchyEntry ce = it.next();
                ce.reload(recursive);
            }
        }
    }

    /**
     * Calls {@link HierarchyEntryImpl#revert()} and moves all properties from the
     * attic back into the properties map. If this HierarchyEntry has been
     * transiently moved, it is in addition moved back to its old parent.
     * Similarly reordering of child node entries is reverted.
     *
     * @see HierarchyEntry#revert()
     */
    @Override
    public void revert() throws RepositoryException {
        // move all properties from attic back to properties map
        if (!propertiesInAttic.isEmpty()) {
            properties.addAll(propertiesInAttic.values());
            propertiesInAttic.clear();
        }
        // NOTE: childNodeAttic must not be cleared for the move of child entries
        // will be separately reverted.

        // now make sure the attached state is reverted to the original state
        super.revert();
    }

    /**
     * @see HierarchyEntry#transientRemove()
     */
    @Override
    public void transientRemove() throws RepositoryException {
        for (Iterator<HierarchyEntry> it = getAllChildEntries(false); it.hasNext();) {
            HierarchyEntry ce = it.next();
            ce.transientRemove();
        }

        if (!propertiesInAttic.isEmpty()) {
            // move all properties from attic back to properties map
            properties.addAll(propertiesInAttic.values());
            propertiesInAttic.clear();
        }

        // execute for this entry as well
        super.transientRemove();
    }

    /**
     * @see HierarchyEntry#remove()
     */
    @Override
    public void remove() {
        // handle this entry first
        super.internalRemove(false);
        boolean staleParent = (getStatus() == Status.STALE_DESTROYED);
        // now remove all child-entries (or mark them accordingly)
        for (Iterator<HierarchyEntry> it = getAllChildEntries(true); it.hasNext();) {
            HierarchyEntryImpl ce = (HierarchyEntryImpl) it.next();
            ce.internalRemove(staleParent);
        }
    }

    @Override
    void internalRemove(boolean staleParent) {
        // handle this entry first
        super.internalRemove(staleParent);
        staleParent = (staleParent || (getStatus() == Status.STALE_DESTROYED));

        // now remove all child-entries (or mark them accordingly)
        for (Iterator<HierarchyEntry> it = getAllChildEntries(true); it.hasNext();) {
            HierarchyEntryImpl ce = (HierarchyEntryImpl) it.next();
            ce.internalRemove(staleParent);
        }
    }

    /**
     * @see HierarchyEntry#complete(Operation)
     */
    public void complete(Operation operation) throws RepositoryException {
        if (operation instanceof AddNode) {
            complete((AddNode) operation);
        } else if (operation instanceof AddProperty) {
            complete((AddProperty) operation);
        } else if (operation instanceof SetMixin) {
            complete((SetMixin) operation);
        } else if (operation instanceof SetPrimaryType) {
            complete((SetPrimaryType) operation);
        } else if (operation instanceof Remove) {
            complete((Remove) operation);
        } else if (operation instanceof ReorderNodes) {
            complete((ReorderNodes) operation);
        } else if (operation instanceof Move) {
            complete((Move) operation);
        } else {
            throw new IllegalArgumentException();
        }
    }
    //----------------------------------------------------------< NodeEntry >---
    /**
     * @see NodeEntry#getId()
     */
    public NodeId getId() throws InvalidItemStateException, RepositoryException {
        return getId(false);
    }

    /**
     * @see NodeEntry#getWorkspaceId()
     */
    public NodeId getWorkspaceId() throws InvalidItemStateException, RepositoryException {
        return getId(true);
    }

    private NodeId getId(boolean wspId) throws RepositoryException {
        if (parent == null) { // shortcut for root
            return getIdFactory().createNodeId((String) null, getPathFactory().getRootPath());
        }
        else if (uniqueID != null) { // shortcut for uniqueID based IDs
            return getIdFactory().createNodeId(uniqueID);
        }
        else {
            return buildNodeId(this, getPathFactory(), getIdFactory(), wspId);
        }
    }

    private static NodeId buildNodeId(NodeEntryImpl entry, PathFactory pathFactory, IdFactory idFactory,
            boolean wspId) throws RepositoryException {

        PathBuilder pathBuilder = new PathBuilder(pathFactory);
        while (entry.getParent() != null && entry.getUniqueID() == null) {
            pathBuilder.addFirst(entry.getName(wspId), entry.getIndex(wspId));
            entry = (wspId && entry.revertInfo != null)
                ? entry.revertInfo.oldParent
                : entry.parent;
        }

        // We either walked up to an entry below root or up to an uniqueID. In the former
        // case we construct an NodeId with an absolute path. In the latter case we construct
        // a NodeId from an uuid and a relative path.
        if (entry.getParent() == null) {
            pathBuilder.addRoot();
            return idFactory.createNodeId((String) null, pathBuilder.getPath());
        }
        else {
            return idFactory.createNodeId(entry.getUniqueID(), pathBuilder.getPath());
        }
    }

    /**
     * @see NodeEntry#getUniqueID()
     */
    public String getUniqueID() {
        return uniqueID;
    }

    /**
     * @see NodeEntry#setUniqueID(String)
     */
    public void setUniqueID(String uniqueID) {
        String old = this.uniqueID;
        boolean mod = (uniqueID == null) ? old != null : !uniqueID.equals(old);
        if (mod) {
            this.uniqueID = uniqueID;
            factory.notifyIdChange(this, old);
        }
    }

    /**
     * @see NodeEntry#getIndex()
     */
    public int getIndex() throws InvalidItemStateException, RepositoryException {
        return getIndex(false);
    }

    /**
     * @see NodeEntry#getNodeState()
     */
    public NodeState getNodeState() throws ItemNotFoundException, RepositoryException {
        return (NodeState) getItemState();
    }

    /**
     * @see NodeEntry#getDeepNodeEntry(Path)
     */
    public NodeEntry getDeepNodeEntry(Path path) throws PathNotFoundException, RepositoryException {
        NodeEntryImpl entry = this;
        Path.Element[] elems = path.getElements();
        for (int i = 0; i < elems.length; i++) {
            Path.Element elem = elems[i];
            // check for root element
            if (elem.denotesRoot()) {
                if (entry.getParent() != null) {
                    throw new RepositoryException("NodeEntry out of 'hierarchy' " + path.toString());
                }
                continue;
            }

            int index = elem.getNormalizedIndex();
            Name name = elem.getName();

            // first try to resolve to known node or property entry
            NodeEntry cne = entry.getNodeEntry(name, index, false);
            if (cne != null) {
                entry = (NodeEntryImpl) cne;
            } else {
                // no valid entry
                // -> if cnes are complete -> assume that it doesn't exist.
                //    refresh will bring up new entries added in the mean time
                //    on the persistent layer.
                if (entry.childNodeEntries.isComplete()) {
                    throw new PathNotFoundException(factory.saveGetJCRPath(path));
                }
                // -> check for moved child entry in node-attic
                // -> check if child points to a removed/moved sns
                List<NodeEntry> siblings = entry.childNodeEntries.get(name);
                if (entry.containsAtticChild(siblings, name, index)) {
                    throw new PathNotFoundException(factory.saveGetJCRPath(path));
                }
                // shortcut: entry is NEW and still unresolved remaining path
                // elements -> hierarchy doesn't exist anyway.
                if (entry.getStatus() == Status.NEW) {
                    throw new PathNotFoundException(factory.saveGetJCRPath(path));
                }
               /*
                * Unknown entry (not-existing or not yet loaded):
                * Skip all intermediate entries and directly try to load the ItemState
                * (including building the intermediate entries. If that fails
                * ItemNotFoundException is thrown.
                *
                * Since 'path' might be ambiguous (Node or Property):
                * 1) first try Node
                * 2) if the NameElement does not have SNS-index => try Property
                * 3) else throw
                */
                PathBuilder pb = new PathBuilder(getPathFactory());
                for (int j = i; j < elems.length; j++) {
                    pb.addLast(elems[j]);
                }
                Path remainingPath = pb.getPath();

                NodeId parentId = entry.getWorkspaceId();
                IdFactory idFactory = factory.getIdFactory();

                NodeId nodeId = idFactory.createNodeId(parentId, remainingPath);
                NodeEntry ne = entry.loadNodeEntry(nodeId);
                if (ne != null) {
                    return ne;
                } else {
                    throw new PathNotFoundException(factory.saveGetJCRPath(path));
                }
            }
        }
        return entry;
    }

    /**
     * @see NodeEntry#getDeepPropertyEntry(Path)
     */
    public PropertyEntry getDeepPropertyEntry(Path path) throws PathNotFoundException, RepositoryException {
        NodeEntryImpl entry = this;
        Path.Element[] elems = path.getElements();
        int i = 0;
        for (; i < elems.length-1; i++) {
            Path.Element elem = elems[i];
            if (elems[i].denotesRoot()) {
                if (entry.getParent() != null) {
                    throw new RepositoryException("NodeEntry out of 'hierarchy' " + path.toString());
                }
                continue;
            }

            int index = elem.getNormalizedIndex();
            Name name = elem.getName();

            // first try to resolve to known node or property entry
            NodeEntry cne = entry.getNodeEntry(name, index, false);
            if (cne != null) {
                entry = (NodeEntryImpl) cne;
            } else {
                // no valid ancestor node entry
                // -> if cnes are complete -> assume that it doesn't exist.
                //    refresh will bring up new entries added in the mean time
                //    on the persistent layer.
                if (entry.childNodeEntries.isComplete()) {
                    throw new PathNotFoundException(factory.saveGetJCRPath(path));
                }
                // -> check for moved child entry in node-attic
                // -> check if child points to a removed/moved sns
                List<NodeEntry> siblings = entry.childNodeEntries.get(name);
                if (entry.containsAtticChild(siblings, name, index)) {
                    throw new PathNotFoundException(factory.saveGetJCRPath(path));
                }
                // break out of the loop and start deep loading the property
                break;
            }
        }

        int st = entry.getStatus();
        PropertyEntry pe;
        if (i == elems.length-1 && Status.INVALIDATED != st && Status._UNDEFINED_ != st) {
            // all node entries present in the hierarchy and the direct ancestor
            // has already been resolved and isn't invalidated -> no need to
            // retrieve property entry from SPI
            pe = entry.properties.get(path.getName());
        } else {
            /*
            * Unknown parent entry (not-existing or not yet loaded) or a parent
            * entry that has been invalidated:
            * Skip all intermediate entries and directly try to load the
            * PropertyState (including building the intermediate entries. If that
            * fails ItemNotFoundException is thrown.
            */
            PathBuilder pb = new PathBuilder(getPathFactory());
            for (int j = i; j < elems.length; j++) {
                pb.addLast(elems[j]);
            }
            Path remainingPath = pb.getPath();

            IdFactory idFactory = getIdFactory();
            NodeId parentId = entry.getWorkspaceId();
            if (remainingPath.getLength() != 1) {
                parentId = idFactory.createNodeId(parentId, remainingPath.getAncestor(1));
            }
            PropertyId propId = idFactory.createPropertyId(parentId, remainingPath.getName());
            pe = entry.loadPropertyEntry(propId);
        }

        if (pe == null) {
            throw new PathNotFoundException(factory.saveGetJCRPath(path));
        }
        return pe;
    }

    /**
     * @see NodeEntry#lookupDeepEntry(Path)
     */
    public HierarchyEntry lookupDeepEntry(Path workspacePath) {
        NodeEntryImpl entry = this;
        for (int i = 0; i < workspacePath.getLength(); i++) {
            Path.Element elem = workspacePath.getElements()[i];
            // check for root element
            if (elem.denotesRoot()) {
                if (getParent() != null) {
                    log.warn("NodeEntry out of 'hierarchy'" + workspacePath.toString());
                    return null;
                }
                continue;
            }

            int index = elem.getNormalizedIndex();
            Name childName = elem.getName();

            // first try to resolve node
            NodeEntry cne = entry.lookupNodeEntry(null, childName, index);
            if (cne != null) {
                entry = (NodeEntryImpl) cne;
            } else if (index == Path.INDEX_DEFAULT && i == workspacePath.getLength() - 1) {
                // property must not have index && must be final path element
                return entry.lookupPropertyEntry(childName);
            } else {
                return null;
            }
        }
        return entry;
    }

    /**
     * @see NodeEntry#hasNodeEntry(Name)
     */
    public synchronized boolean hasNodeEntry(Name nodeName) {
        List<NodeEntry> namedEntries = childNodeEntries.get(nodeName);
        if (namedEntries.isEmpty()) {
            return false;
        } else {
            return EntryValidation.containsValidNodeEntry(namedEntries.iterator());
        }
    }

    /**
     * @see NodeEntry#hasNodeEntry(Name, int)
     */
    public synchronized boolean hasNodeEntry(Name nodeName, int index) {
        try {
            return getNodeEntry(nodeName, index) != null;
        } catch (RepositoryException e) {
            log.debug("Unable to determine if a child node with name " + nodeName + " exists.");
            return false;
        }
    }

    /**
     * @see NodeEntry#getNodeEntry(Name, int)
     */
    public synchronized NodeEntry getNodeEntry(Name nodeName, int index) throws RepositoryException {
        return getNodeEntry(nodeName, index, false);
    }

    /**
     * @see NodeEntry#getNodeEntry(Name, int, boolean)
     */
    public NodeEntry getNodeEntry(Name nodeName, int index, boolean loadIfNotFound) throws RepositoryException {
        List<NodeEntry> entries = childNodeEntries.get(nodeName);
        NodeEntry cne = null;
        if (entries.size() >= index) {
            // position of entry might differ from index-1 if a SNS with lower
            // index has been transiently removed.
            int eIndex = 1;
            for (int i = 0; i < entries.size() && cne == null; i++) {
                NodeEntry ne = entries.get(i);
                if (EntryValidation.isValidNodeEntry(ne)) {
                    if (eIndex == index) {
                        cne = ne;
                    }
                    eIndex++;
                }
            }
        }

        if (cne == null && loadIfNotFound
                && !containsAtticChild(entries, nodeName, index)
                && !childNodeEntries.isComplete()) {

            NodeId cId = getIdFactory().createNodeId(getWorkspaceId(),
                    getPathFactory().create(nodeName, index));
            cne = loadNodeEntry(cId);
        }
        return cne;
    }

    /**
     * @see NodeEntry#getNodeEntries()
     */
    @SuppressWarnings("unchecked")
    public synchronized Iterator<NodeEntry> getNodeEntries() throws RepositoryException {
        Collection<NodeEntry> entries = new ArrayList<NodeEntry>();
        for (Iterator<NodeEntry> it = getCompleteChildNodeEntries().iterator(); it.hasNext();) {
            NodeEntry entry = it.next();
            if (EntryValidation.isValidNodeEntry(entry)) {
                entries.add(entry);
            }
        }
        return new RangeIteratorAdapter(Collections.unmodifiableCollection(entries));
    }

    /**
     * @see NodeEntry#getNodeEntries(Name)
     */
    public synchronized List<NodeEntry> getNodeEntries(Name nodeName) throws RepositoryException {
        List<NodeEntry> namedEntries = getCompleteChildNodeEntries().get(nodeName);
        if (namedEntries.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<NodeEntry> entries = new ArrayList<NodeEntry>();
            // get array of the list, since during validation the childNodeEntries
            // may be modified if upon NodeEntry.getItemState the entry gets removed.
            NodeEntry[] arr = namedEntries.toArray(new NodeEntry[namedEntries.size()]);
            for (int i = 0; i < arr.length; i++) {
                NodeEntry cne = arr[i];
                if (EntryValidation.isValidNodeEntry(cne)) {
                    entries.add(cne);
                }
            }
            return Collections.unmodifiableList(entries);
        }
    }

    /**
     * @see NodeEntry#setNodeEntries(Iterator)
     */
    public void setNodeEntries(Iterator<ChildInfo> childInfos) throws RepositoryException {
        if (childNodeAttic.isEmpty()) {
            ((ChildNodeEntriesImpl) childNodeEntries).update(childInfos);
        } else {
            // filter those entries that have been moved to the attic.
            List<ChildInfo> remaining = new ArrayList<ChildInfo>();
            while (childInfos.hasNext()) {
                ChildInfo ci = childInfos.next();
                if (!childNodeAttic.contains(ci.getName(), ci.getIndex(), ci.getUniqueID())) {
                    remaining.add(ci);
                }
            }
            ((ChildNodeEntriesImpl) childNodeEntries).update(remaining.iterator());
        }
    }

    /**
     * @see NodeEntry#getOrAddNodeEntry(Name, int, String)
     */
    public NodeEntry getOrAddNodeEntry(Name nodeName, int index, String uniqueID) throws RepositoryException {
        NodeEntry ne = lookupNodeEntry(uniqueID, nodeName, index);
        if (ne == null) {
            ne = internalAddNodeEntry(nodeName, uniqueID, index);
        } else {
            log.debug("Child NodeEntry already exists -> didn't add.");
        }
        return ne;
    }

    /**
     * @see NodeEntry#addNewNodeEntry(Name, String, Name, QNodeDefinition)
     */
    public NodeEntry addNewNodeEntry(Name nodeName, String uniqueID,
                                     Name primaryNodeType, QNodeDefinition definition) throws RepositoryException {
        NodeEntry entry = internalAddNodeEntry(nodeName, uniqueID, Path.INDEX_UNDEFINED);
        NodeState state = getItemStateFactory().createNewNodeState(entry, primaryNodeType, definition);
        entry.setItemState(state);
        return entry;
    }

    /**
     * @see NodeEntry#hasPropertyEntry(Name)
     */
    public synchronized boolean hasPropertyEntry(Name propName) {
        PropertyEntry entry = properties.get(propName);
        return EntryValidation.isValidPropertyEntry(entry);
    }

    /**
     * @see NodeEntry#getPropertyEntry(Name)
     */
    public synchronized PropertyEntry getPropertyEntry(Name propName) {
        PropertyEntry entry = properties.get(propName);
        if (EntryValidation.isValidPropertyEntry(entry)) {
            return entry;
        } else {
            return null;
        }
    }

    /**
     * Ignores the <code>loadIfNotFound</code> flag due to the fact, that
     * {@link org.apache.jackrabbit.spi.NodeInfo#getPropertyIds()} returns the
     * complete list of property names currently available.
     * @see NodeEntry#getPropertyEntry(Name, boolean)
     */
    public PropertyEntry getPropertyEntry(Name propName, boolean loadIfNotFound) throws RepositoryException {
        return getPropertyEntry(propName);
    }

    /**
     * @see NodeEntry#getPropertyEntries()
     */
    @SuppressWarnings("unchecked")
    public synchronized Iterator<PropertyEntry> getPropertyEntries() {
        Collection<PropertyEntry> props;
        if (getStatus() == Status.EXISTING_MODIFIED) {
            // filter out removed properties
            props = new ArrayList<PropertyEntry>();
            // use array since upon validation the entry might be removed.
            Object[] arr = properties.getPropertyEntries().toArray();
            for (int i = 0; i < arr.length; i++) {
                PropertyEntry propEntry = (PropertyEntry) arr[i];
                if (EntryValidation.isValidPropertyEntry(propEntry)) {
                    props.add(propEntry);
                }
            }
        } else {
            // no need to filter out properties, there are no removed properties
            props = properties.getPropertyEntries();
        }
        return new RangeIteratorAdapter(Collections.unmodifiableCollection(props));
    }

    /**
     * @see NodeEntry#getOrAddPropertyEntry(Name)
     */
    public PropertyEntry getOrAddPropertyEntry(Name propName) throws ItemExistsException {
        PropertyEntry pe = lookupPropertyEntry(propName);
        if (pe == null) {
            pe = internalAddPropertyEntry(propName, true);
        }  else {
            log.debug("Child PropertyEntry already exists -> didn't add.");
        }
        return pe;
    }

    /**
     * @see NodeEntry#setPropertyEntries(Collection)
     */
    public void setPropertyEntries(Collection<Name> propNames) throws ItemExistsException, RepositoryException {
        Set<Name> diff = new HashSet<Name>();
        diff.addAll(properties.getPropertyNames());
        boolean containsExtra = diff.removeAll(propNames);

        // add all entries that are missing
        for (Iterator<Name> it = propNames.iterator(); it.hasNext();) {
            Name propName = it.next();
            if (!properties.contains(propName)) {
                // TODO: check again.
                // setPropertyEntries is used by WorkspaceItemStateFactory upon
                // creating a NodeState, in which case the uuid/mixins are set
                // anyway and not need exists to explicitly load the corresponding
                // property state in order to retrieve the values.
                internalAddPropertyEntry(propName, false);
            }
        }

        // if this entry has not yet been resolved or if it is 'invalidated'
        // all property entries, that are not contained within the specified
        // collection of property names are removed from this NodeEntry.
        ItemState state = internalGetItemState();
        if (containsExtra && (state == null || state.getStatus() == Status.INVALIDATED)) {
            for (Iterator<Name> it = diff.iterator(); it.hasNext();) {
                Name propName = it.next();
                PropertyEntry pEntry = properties.get(propName);
                if (pEntry != null) {
                    pEntry.remove();
                }
            }
        }
    }

    /**
     * @see NodeEntry#addNewPropertyEntry(Name, QPropertyDefinition, QValue[], int)
     */
    public PropertyEntry addNewPropertyEntry(Name propName, QPropertyDefinition definition, QValue[] values, int propertyType)
            throws ItemExistsException, RepositoryException {
        // check for an existing property
        PropertyEntry existing = properties.get(propName);
        if (existing != null) {
            try {
                PropertyState existingState = existing.getPropertyState();
                int status = existingState.getStatus();
                if (Status.isTerminal(status)) {
                    // an old property-entry that is not valid any more
                    properties.remove(existing);
                } else if (status == Status.EXISTING_REMOVED) {
                    // transiently removed -> move it to the attic
                    propertiesInAttic.put(propName, existing);
                } else {
                    // existing is still existing -> cannot add same-named property
                    throw new ItemExistsException(propName.toString());
                }
            } catch (ItemNotFoundException e) {
                // entry does not exist on the persistent layer
                // -> therefore remove from properties map
                properties.remove(existing);
            } catch (RepositoryException e) {
                // some other error -> remove from properties map
                properties.remove(existing);
            }
        }

        PropertyEntry entry = factory.createPropertyEntry(this, propName);
        PropertyState state = getItemStateFactory().createNewPropertyState(entry, definition, values, propertyType);
        entry.setItemState(state);

        // add the property entry if creating the new state was successful
        properties.add(entry);

        return entry;
    }

    /**
     * @see NodeEntry#orderBefore(NodeEntry)
     */
    public void orderBefore(NodeEntry beforeEntry) throws RepositoryException {
        if (Status.NEW == getStatus()) {
            // new states get remove upon revert
            parent.childNodeEntries.reorder(this, beforeEntry);
        } else {
            createRevertInfo();
            // now reorder child entries on parent
            parent.childNodeEntries.reorder(this, beforeEntry);
        }
    }

   /**
    * @see NodeEntry#move(Name, NodeEntry, boolean)
    */
   public NodeEntry move(Name newName, NodeEntry newParent, boolean transientMove) throws RepositoryException {
       if (parent == null) {
           // the root may never be moved
           throw new RepositoryException("Root cannot be moved.");
       }

       // for existing nodeEntry that are 'moved' for the first time, the
       // original data must be stored and this entry is moved to the attic.
       if (transientMove) {
           createRevertInfo();
           if (Status.NEW != getStatus()) {
               if (newParent != revertInfo.oldParent) {
                   revertInfo.oldParent.childNodeAttic.add(this);
               } else {
                   // entry is either rename OR moved back to it's original
                   // parent. for the latter case make sure, there is no attic
                   // entry remaining referring to the entry that is being added.
                   revertInfo.oldParent.childNodeAttic.remove(this);
               }
           }
       }

       NodeEntry entry = parent.childNodeEntries.remove(this);
       if (entry != this) {
           // should never occur
           String msg = "Internal error. Attempt to move NodeEntry (" + getName() + ") which is not connected to its parent.";
           log.error(msg);
           throw new RepositoryException(msg);
       }
       // set name and parent to new values
       parent = (NodeEntryImpl) newParent;
       name = newName;
       // register entry with its new parent
       parent.childNodeEntries.add(this);
       return this;
   }

    /**
     * @see NodeEntry#isTransientlyMoved()
     */
    public boolean isTransientlyMoved() {
        return revertInfo != null && revertInfo.isMoved();
    }

    /**
     * @see NodeEntry#refresh(Event)
     */
    public void refresh(Event childEvent) {
        ItemId eventId = childEvent.getItemId();
        Path eventPath = childEvent.getPath();
        Name eventName = eventPath.getName();
        HierarchyEntry child = eventId == null ? null : lookupEntry(eventId, eventPath);

        switch (childEvent.getType()) {
            case Event.NODE_ADDED:
            case Event.PROPERTY_ADDED:
                if (child == null || child.getStatus() == Status.REMOVED) {
                    // no such child or a colliding new child existed but got
                    // removed already -> add the new entry.
                    if (childEvent.getType() ==  Event.NODE_ADDED) {
                        String uniqueChildID = (eventId.getPath() == null) ? eventId.getUniqueID() : null;
                        int index = eventPath.getNormalizedIndex();
                        internalAddNodeEntry(eventName, uniqueChildID, index);
                    } else {
                        internalAddPropertyEntry(eventName, true);
                    }
                } else {
                    // item already present
                    int status = child.getStatus();
                    if (Status.NEW == status) {
                        // event conflicts with a transiently added item on this
                        // node entry -> mark the parent node (this) stale.
                        internalGetItemState().setStatus(Status.MODIFIED);
                    } // else: child already added -> ignore
                }
                break;

            case Event.NODE_REMOVED:
            case Event.PROPERTY_REMOVED:
                if (child != null) {
                    int status = child.getStatus();
                    if (Status.EXISTING_REMOVED == status) {
                        // colliding item removal -> mark parent stale
                        internalGetItemState().setStatus(Status.MODIFIED);
                    }
                    child.remove();
                } // else: child-Entry has not been loaded yet -> ignore
                break;

            case Event.PROPERTY_CHANGED:
                if (child == null) {
                    // prop-Entry has not been loaded yet -> add propEntry
                    internalAddPropertyEntry(eventName, true);
                } else if (child.isAvailable()) {
                    int status = child.getStatus();
                    // if the child has pending changes -> stale.
                    // Reload data from server and try to merge them with the
                    // current session-state. if the latter is transiently
                    // modified and merge fails it must be marked STALE afterwards.
                    if (Status.isStale(status))  {
                        // ignore. nothing to do.
                    } else if (Status.isTransient(child.getStatus())) {
                        // pending changes -> don't reload entry but rather
                        // mark it stale
                        ((HierarchyEntryImpl) child).internalGetItemState().setStatus(Status.MODIFIED);
                    } else {
                        // no pending changes -> invalidate and force reload
                        // upon next access.
                        child.invalidate(false);
                        // special cases: jcr:uuid and jcr:mixinTypes affect the
                        // parent (i.e. this NodeEntry)
                        if (StateUtility.isUuidOrMixin(eventName)) {
                            notifyUUIDorMIXINModified((PropertyEntry) child);
                        }
                    }
                } // else: existing entry but state not yet built -> ignore event
                break;
            case Event.NODE_MOVED:
                // TODO: implementation missing
                throw new UnsupportedOperationException("Implementation missing");
                //break;
            case Event.PERSIST:
                // TODO: implementation missing
                throw new UnsupportedOperationException("Implementation missing");
            default:
                // ILLEGAL
                throw new IllegalArgumentException("Illegal event type " + childEvent.getType() + " for NodeState.");
        }
    }
    //-------------------------------------------------< HierarchyEntryImpl >---
    /**
     * @see HierarchyEntryImpl#doResolve()
     * <p>
     * Returns a <code>NodeState</code>.
     */
    @Override
    ItemState doResolve() throws ItemNotFoundException, RepositoryException {
        return getItemStateFactory().createNodeState(getWorkspaceId(), this);
    }

    /**
     * @see HierarchyEntryImpl#buildPath(boolean)
     */
    @Override
    Path buildPath(boolean wspPath) throws RepositoryException {
        PathFactory pf = getPathFactory();
        // shortcut for root state
        if (parent == null) {
            return pf.getRootPath();
        }
        // build path otherwise
        PathBuilder builder = new PathBuilder(pf);
        buildPath(builder, this, wspPath);
        return builder.getPath();
    }

    /**
     * Adds the path element of an item id to the path currently being built.
     * On exit, <code>builder</code> contains the path of this entry.
     *
     * @param builder
     * @param nEntry NodeEntryImpl of the state the path should be built for.
     * @param wspPath true if the workspace path should be built
     */
    private static void buildPath(PathBuilder builder, NodeEntryImpl nEntry, boolean wspPath) throws RepositoryException {
        NodeEntryImpl parentEntry = (wspPath && nEntry.revertInfo != null) ? nEntry.revertInfo.oldParent : nEntry.parent;
        // shortcut for root state
        if (parentEntry == null) {
            builder.addRoot();
            return;
        }

        // recursively build path of parent
        buildPath(builder, parentEntry, wspPath);

        int index = nEntry.getIndex(wspPath);
        Name name = nEntry.getName(wspPath);
        builder.addLast(name, index);
    }

    //-----------------------------------------------< private || protected >---
    /**
     * @param nodeName
     * @param uniqueID
     * @param index
     * @return the added entry.
     */
    private NodeEntry internalAddNodeEntry(Name nodeName, String uniqueID, int index) {
        NodeEntry entry = factory.createNodeEntry(this, nodeName, uniqueID);
        childNodeEntries.add(entry, index);
        return entry;
    }

    /**
     * Internal method that adds a PropertyEntry without checking of that entry
     * exists.
     *
     * @param propName
     * @param notifySpecial
     * @return the added entry.
     */
    private PropertyEntry internalAddPropertyEntry(Name propName, boolean notifySpecial) {
        PropertyEntry entry = factory.createPropertyEntry(this, propName);
        properties.add(entry);

        // if property-name is jcr:uuid or jcr:mixin this affects this entry
        // and the attached nodeState.
        if (notifySpecial && StateUtility.isUuidOrMixin(propName)) {
            notifyUUIDorMIXINModified(entry);
        }
        return entry;
    }

    /**
     *
     * @param childEntry
     */
    void internalRemoveChildEntry(HierarchyEntry childEntry) {
        if (childEntry.denotesNode()) {
            if (childNodeEntries.remove((NodeEntry) childEntry) == null) {
                childNodeAttic.remove((NodeEntry) childEntry);
            }
        } else {
            Name propName = childEntry.getName();
            PropertyEntry atticEntry = propertiesInAttic.get(propName);
            if (atticEntry == null) {
                properties.remove((PropertyEntry) childEntry);
            } else if (atticEntry == childEntry) {
                propertiesInAttic.remove(propName);
            } // else: no such prop-entry. should not get here

            // special properties
            if (StateUtility.isUuidOrMixin(propName)) {
                notifyUUIDorMIXINRemoved(propName);
            }
        }
    }

    @Override
    protected void invalidateInternal(boolean recursive) {
        if (recursive) {
            // invalidate all child entries including properties present in the
            // attic (removed props shadowed by a new property with the same name).
            for (Iterator<HierarchyEntry> it = getAllChildEntries(true); it.hasNext();) {
                HierarchyEntry ce = it.next();
                ce.invalidate(true);
            }
        }
        super.invalidateInternal(true);
    }

    /**
     * @param oldName
     * @param oldIndex
     * @return <code>true</code> if the given oldName and oldIndex match
     * {@link #getName(boolean)} and {@link #getIndex(boolean)}, respectively.
     */
    boolean matches(Name oldName, int oldIndex) {
        try {
            return getName(true).equals(oldName) && getIndex(true) == oldIndex;
        } catch (RepositoryException e) {
            // should not get here
            return false;
        }
    }

    /**
     * @param oldName
     * @return <code>true</code> if the given oldName matches
     * {@link #getName(boolean)}.
     */
    boolean matches(Name oldName) {
        return getName(true).equals(oldName);
    }


    private Name getName(boolean wspName) {
        if (wspName && revertInfo != null) {
            return revertInfo.oldName;
        } else {
            return name;
        }
    }

    private int getIndex(boolean wspIndex) throws InvalidItemStateException, RepositoryException {
        if (parent == null) {
            // the root state may never have siblings
            return Path.INDEX_DEFAULT;
        }

        if (wspIndex && revertInfo != null) {
            return revertInfo.oldIndex;
        } else {
            NodeState state = (NodeState) internalGetItemState();
            if (state == null || !state.hasDefinition() || state.getDefinition().allowsSameNameSiblings()) {
                return parent.getChildIndex(this, wspIndex);
            } else {
                return Path.INDEX_DEFAULT;
            }
        }
    }

    /**
     *
     * @param childId
     * @return the entry or <code>null</code> if building the corresponding
     * <code>NodeState</code> failed with <code>ItemNotFoundException</code>.
     */
    private NodeEntry loadNodeEntry(NodeId childId) throws RepositoryException {
        try {
            NodeState state = getItemStateFactory().createDeepNodeState(childId, this);
            return state.getNodeEntry();
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    /**
     * @param childId
     * @return the entry or <code>null</code> if building the corresponding
     * <code>PropertyState</code> failed with <code>ItemNotFoundException</code>.
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    private PropertyEntry loadPropertyEntry(PropertyId childId) throws RepositoryException {
        try {
            PropertyState state = getItemStateFactory().createDeepPropertyState(childId, this);
            return (PropertyEntry) state.getHierarchyEntry();
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    /**
     * Searches the child-entries of this NodeEntry for a matching child.
     * Since {@link #refresh(Event)} must always be called on the parent
     * NodeEntry, there is no need to check if a given event id would point
     * to this NodeEntry itself.
     *
     * @param eventId
     * @param eventPath
     * @return the entry or <code>null</code> if the matching entry has a status
     * <code>Status#NEW</code>.
     */
    private HierarchyEntry lookupEntry(ItemId eventId, Path eventPath) {
        Name childName = eventPath.getName();
        HierarchyEntry child;
        if (eventId.denotesNode()) {
            String uniqueChildID = (eventId.getPath() == null) ? eventId.getUniqueID() : null;
            int index = eventPath.getNormalizedIndex();
            child = lookupNodeEntry(uniqueChildID, childName, index);
        } else {
            child = lookupPropertyEntry(childName);
        }
        return child;
    }

    private NodeEntry lookupNodeEntry(String uniqueChildId, Name childName, int index) {
        NodeEntry child = null;
        if (uniqueChildId != null) {
            child = childNodeAttic.get(uniqueChildId);
            if (child == null) {
                child = childNodeEntries.get(childName, uniqueChildId);
            }
        }
        if (child == null) {
            child = childNodeAttic.get(childName, index);
            if (child == null && childNodeEntries != null) {
                child = childNodeEntries.get(childName, index);
            }
        }
        return child;
    }

    private PropertyEntry lookupPropertyEntry(Name childName) {
        // for external prop-removal the attic must be consulted first
        // in order not access a NEW prop shadowing a transiently removed
        // property with the same name.
        PropertyEntry child = propertiesInAttic.get(childName);
        if (child == null) {
            child = properties.get(childName);
        }
        return child;
    }

    /**
     * Deals with modified jcr:uuid and jcr:mixinTypes property.
     * See {@link #notifyUUIDorMIXINRemoved(Name)}
     *
     * @param child
     */
    private void notifyUUIDorMIXINModified(PropertyEntry child) {
        try {
            if (NameConstants.JCR_UUID.equals(child.getName())) {
                PropertyState ps = child.getPropertyState();
                setUniqueID(ps.getValue().getString());
            } else if (NameConstants.JCR_MIXINTYPES.equals(child.getName())) {
                NodeState state = (NodeState) internalGetItemState();
                if (state != null) {
                    PropertyState ps = child.getPropertyState();
                    state.setMixinTypeNames(StateUtility.getMixinNames(ps));
                } // nodestate not yet loaded -> ignore change
            }
        } catch (ItemNotFoundException e) {
            log.debug("Property with name " + child.getName() + " does not exist (anymore)");
        } catch (RepositoryException e) {
            log.debug("Unable to access child property " + child.getName(), e.getMessage());
        }
    }

    /**
     * Deals with removed jcr:uuid and jcr:mixinTypes property.
     * See {@link #notifyUUIDorMIXINModified(PropertyEntry)}
     *
     * @param propName
     */
    private void notifyUUIDorMIXINRemoved(Name propName) {
        if (NameConstants.JCR_UUID.equals(propName)) {
            setUniqueID(null);
        } else if (NameConstants.JCR_MIXINTYPES.equals(propName)) {
            NodeState state = (NodeState) internalGetItemState();
            if (state != null) {
                state.setMixinTypeNames(Name.EMPTY_ARRAY);
            }
        }
    }

    /**
     * @return The <code>ChildNodeEntries</code> defined for this
     * <code>NodeEntry</code>. Please note, that this method never returns
     * <code>null</code>, since the child node entries are loaded/reloaded
     * in case they have not been loaded yet.
     */
    private ChildNodeEntries getCompleteChildNodeEntries() throws InvalidItemStateException, RepositoryException {
        try {
            childNodeEntries.reload();
        } catch (ItemNotFoundException e) {
            log.debug("NodeEntry does not exist (anymore) -> remove.");
            remove();
            throw new InvalidItemStateException(e);
        }
        return childNodeEntries;
    }

    /**
     * Returns an Iterator over all children entries, that currently are loaded
     * with this NodeEntry. NOTE, that if the childNodeEntries have not been
     * loaded yet, no attempt is made to do so.
     *
     * @param includeAttic
     * @return iterator over all children entries, that currently are loaded
     * with this NodeEntry
     */
    private Iterator<HierarchyEntry> getAllChildEntries(boolean includeAttic) {
        IteratorChain<HierarchyEntry> chain = new IteratorChain<>();
        // attic
        if (includeAttic) {
            Collection<PropertyEntry> attic = propertiesInAttic.values();
            chain.addIterator(new ArrayList<PropertyEntry>(attic).iterator());
        }
        // add props
        synchronized (properties) {
            Collection<PropertyEntry> props = properties.getPropertyEntries();
            chain.addIterator(props.iterator());
        }
        // add childNodeEntries
        synchronized (childNodeEntries) {
            chain.addIterator(childNodeEntries.iterator());
        }
        return chain;
    }

    /**
     * Returns the index of the given <code>NodeEntry</code>.
     *
     * @param cne  the <code>NodeEntry</code> instance.
     * @param wspIndex if <code>true</code> transiently removed siblings are respected.
     * @return the index of the child node entry.
     * @throws ItemNotFoundException if the given entry isn't a valid child of
     * this <code>NodeEntry</code>.
     */
    private int getChildIndex(NodeEntry cne, boolean wspIndex) throws ItemNotFoundException, RepositoryException {
        List<NodeEntry> sns = new ArrayList<NodeEntry>(childNodeEntries.get(cne.getName()));

        if (wspIndex) {
            List<NodeEntryImpl> atticSiblings = childNodeAttic.get(cne.getName());
            for (Iterator<NodeEntryImpl> it = atticSiblings.iterator(); it.hasNext();) {
                NodeEntryImpl sibl = it.next();
                if (sibl.revertInfo != null) {
                    sns.add(sibl.revertInfo.oldIndex - 1, sibl);
                } else {
                    log.error("Sibling in attic doesn't have revertInfo....");
                }
            }
        }

        if (sns.isEmpty()) {
            // the given node entry is not connected with his parent any more
            // -> throw
            String msg = "NodeEntry " + cne.getName() + " is disconnected from its parent -> remove.";
            cne.remove();
            throw new InvalidItemStateException(msg);

        } else if (sns.size() == 1) {
            // no siblings -> simply return the default index.
            return Path.INDEX_DEFAULT;

        } else {
            // siblings exist.
            int index = Path.INDEX_DEFAULT;
            for (Iterator<NodeEntry> it = sns.iterator(); it.hasNext(); ) {
                NodeEntry entry = it.next();
                if (entry == cne) { // TODO see below
                    return index;
                }
                // for wsp index ignore all transiently added items.
                // otherwise: skip entries that belong to removed or invalid states.
                // NOTE, that in this case the nodestate must be available from the cne.
                boolean isValid = (wspIndex) ?
                        EntryValidation.isValidWorkspaceNodeEntry(entry) :
                        EntryValidation.isValidNodeEntry(entry);
                if (isValid) {
                    index++;
                }
            }
            // not found, since child entries are only connected with soft refs
            // to the LinkNode in ChildNodeEntries, equality may not determine
            // the correct matching entry -> return default index.
            return Path.INDEX_DEFAULT;
        }
    }

    /**
     * Returns <code>true</code> if the attic contains a matching child entry or
     * if any of the remaining child entries present in the siblings list has
     * been modified in a way that its original index is equal to the given
     * child index.
     *
     * @param siblings
     * @param childName
     * @param childIndex
     * @return <code>true</code> if there is a child entry in the attic that
     * matches the given name/index or if the siblings list contain a reordered
     * entry that matches.
     */
    private boolean containsAtticChild(List<NodeEntry> siblings, Name childName, int childIndex) {
        // check if a matching entry exists in the attic
        if (childNodeAttic.contains(childName, childIndex)) {
            return true;
        }
        // special treatment for potentially moved/reordered/removed sns
        // TODO: check again
        if (childIndex > Path.INDEX_DEFAULT) {
            List<NodeEntryImpl> siblingsInAttic = childNodeAttic.get(childName);
            if (siblings.size() < childIndex && childIndex <= siblings.size() + siblingsInAttic.size()) {
                return true;
            }
        }
        if (getStatus() == Status.EXISTING_MODIFIED) {
            for (Iterator<NodeEntry> it = siblings.iterator(); it.hasNext();) {
                NodeEntry child = it.next();
                if (!EntryValidation.isValidNodeEntry(child) || ((NodeEntryImpl)child).revertInfo != null && ((NodeEntryImpl)child).revertInfo.oldIndex == childIndex) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * If 'revertInfo' is null it gets created from the current information
     * present on this entry.
     */
    private void createRevertInfo() throws RepositoryException {
        if (revertInfo == null && getStatus() != Status.NEW) {
            revertInfo = new RevertInfo();
        }
    }

    private void complete(AddNode operation) throws RepositoryException {
        if (operation.getParentState().getHierarchyEntry() != this) {
            throw new IllegalArgumentException();
        }

        for (Iterator<ItemState> it = operation.getAddedStates().iterator(); it.hasNext();) {
            HierarchyEntry he = it.next().getHierarchyEntry();
            if (he.getStatus() == Status.NEW) {
                switch (operation.getStatus()) {
                    case Operation.STATUS_PERSISTED:
                        ((HierarchyEntryImpl) he).internalGetItemState().setStatus(Status.EXISTING);
                        he.invalidate(false);
                        break;
                    case Operation.STATUS_UNDO:
                        he.revert();
                        break;
                    default: // ignore
                }
            } // entry isn't NEW any more -> ignore
        }
    }

    private void complete(AddProperty operation) throws RepositoryException {
        if (operation.getParentState().getHierarchyEntry() != this) {
            throw new IllegalArgumentException();
        }
        PropertyEntry pe = getPropertyEntry(operation.getPropertyName());
        if (pe != null && pe.getStatus() == Status.NEW) {
            switch (operation.getStatus()) {
                case Operation.STATUS_PERSISTED:
                    // for autocreated/protected props, mark to be reloaded
                    // upon next access.
                    PropertyState addedState = (PropertyState) ((PropertyEntryImpl) pe).internalGetItemState();
                    addedState.setStatus(Status.EXISTING);
                    QPropertyDefinition pd = addedState.getDefinition();
                    if (pd.isAutoCreated() || pd.isProtected()) {
                        pe.invalidate(true);
                    } // else: assume added property is up to date.
                    break;
                case Operation.STATUS_UNDO:
                    pe.revert();
                    break;
                default: // ignore
            }
        } // else: no such prop entry or entry has already been persisted
          //       e.g due to external modifications merged into this NodeEntry.
    }

    private void complete(Remove operation) throws RepositoryException {
        HierarchyEntry rmEntry = operation.getRemoveState().getHierarchyEntry();
        if (rmEntry.getParent() != this) {
            throw new IllegalArgumentException();
        }
        switch (operation.getStatus()) {
            case Operation.STATUS_PERSISTED:
                if (Status.isTerminal(rmEntry.getStatus())) {
                    log.debug("Removal of State " + rmEntry + " has already been completed.");
                }
                rmEntry.remove();
                break;
            case Operation.STATUS_UNDO:
                if (!rmEntry.denotesNode()) {
                    Name propName = rmEntry.getName();
                    if (propertiesInAttic.containsKey(propName)) {
                        properties.add(propertiesInAttic.remove(propName));
                    } // else: propEntry has never been moved to the attic (see 'addPropertyEntry')
                }
                rmEntry.revert();
                break;
            default: // ignore
        }

    }

    private void complete(SetMixin operation) throws RepositoryException {
        if (operation.getNodeState().getHierarchyEntry() != this) {
            throw new IllegalArgumentException();
        }
        PropertyEntry pe = getPropertyEntry(NameConstants.JCR_MIXINTYPES);
        if (pe != null) {
            PropertyState pState = pe.getPropertyState();
            switch (operation.getStatus()) {
                case Operation.STATUS_PERSISTED:
                    Name[] mixins = StateUtility.getMixinNames(pState);
                    getNodeState().setMixinTypeNames(mixins);
                    if (pState.getStatus() == Status.NEW || pState.getStatus() == Status.EXISTING_MODIFIED) {
                        pState.setStatus(Status.EXISTING);
                    }
                    break;
                case Operation.STATUS_UNDO:
                    pe.revert();
                    break;
                default: // ignore
            }
        } // else: no such prop-Entry (should not occur)
    }

    private void complete(SetPrimaryType operation) throws RepositoryException {
        if (operation.getNodeState().getHierarchyEntry() != this) {
            throw new IllegalArgumentException();
        }
        PropertyEntry pe = getPropertyEntry(NameConstants.JCR_PRIMARYTYPE);
        if (pe != null) {
            PropertyState pState = pe.getPropertyState();
            switch (operation.getStatus()) {
                case Operation.STATUS_PERSISTED:
                    // NOTE: invalidation of this node entry is performed by
                    // ChangeLog.persisted...
                    // TODO: check if correct
                    if (pState.getStatus() == Status.NEW || pState.getStatus() == Status.EXISTING_MODIFIED) {
                        pState.setStatus(Status.EXISTING);
                    }
                    break;
                case Operation.STATUS_UNDO:
                    pe.revert();
                    break;
                default: // ignore
            }
        } // else: no such prop-Entry (should not occur)
    }

    private void complete(ReorderNodes operation) throws RepositoryException {
        HierarchyEntry he = operation.getInsertNode().getHierarchyEntry();
        if (he != this) {
            throw new IllegalArgumentException();
        }
        // NOTE: if reorder occurred in combination with a 'move' the clean-up
        // of the revertInfo is postponed until {@link #complete(Move)}.
        switch (operation.getStatus()) {
            case Operation.STATUS_PERSISTED:
                if (revertInfo != null && !revertInfo.isMoved()) {
                    revertInfo.dispose(true);
                }
                break;
            case Operation.STATUS_UNDO:
                if (he.getStatus() == Status.NEW) {
                    he.revert();
                } else if (revertInfo != null && !revertInfo.isMoved()) {
                    revertInfo.dispose(false);
                }
                break;
            default: // ignore
        }
    }

    private void complete(Move operation) throws RepositoryException {
        HierarchyEntry he = operation.getSourceState().getHierarchyEntry();
        if (he != this) {
            throw new IllegalArgumentException();
        }
        switch (operation.getStatus()) {
            case Operation.STATUS_PERSISTED:
                if (getStatus() != Status.NEW && revertInfo != null) {
                    revertInfo.oldParent.childNodeAttic.remove(this);
                    revertInfo.dispose(true);
                }
                // and mark the moved state existing
                // internalGetItemState().setStatus(Status.EXISTING);
                break;
            case Operation.STATUS_UNDO:
                if (getStatus() == Status.NEW) {
                    revert();
                } else if (revertInfo != null) {
                    revertMove();
                    revertInfo.dispose(false);
                }
                break;
            default: // ignore
        }
    }

    private void revertMove() {
        NodeEntryImpl oldParent = revertInfo.oldParent;
        if (oldParent == parent) {
            // simple renaming
            parent.childNodeEntries.remove(this);
        } else {
            // move NodeEntry back to its original parent
            parent.childNodeEntries.remove(this);
            oldParent.childNodeAttic.remove(this);

            // now restore moved entry with the old name and index and re-add
            // it to its original parent (unless it got destroyed)
            parent = oldParent;
        }
        // now restore moved entry with the old name and index and re-add
        // it to its original parent
        name = revertInfo.oldName;
        parent.childNodeEntries.add(this, revertInfo.oldIndex, revertInfo.oldSuccessor);
    }

    //--------------------------------------------------------< inner class >---
    /**
     * Upon move or reorder of this entry the original hierarchy information is
     * stored in the RevertInfo for later operation undo and in order to be able
     * to build the workspace id / path.
     */
    private class RevertInfo {

        private final NodeEntryImpl oldParent;
        private final Name oldName;
        private final int oldIndex;
        private final NodeEntry oldSuccessor;
        private final NodeEntry oldPredecessor;

        private RevertInfo() throws InvalidItemStateException, RepositoryException {
            this.oldParent = parent;
            this.oldName = name;
            this.oldIndex = getIndex();
            this.oldSuccessor = ((ChildNodeEntriesImpl) parent.childNodeEntries).getNext(NodeEntryImpl.this);
            this.oldPredecessor = ((ChildNodeEntriesImpl) parent.childNodeEntries).getPrevious(NodeEntryImpl.this);
        }

        private boolean isMoved() {
            return oldParent != getParent() || !getName().equals(oldName);
        }

        private void dispose(boolean persisted) {
            if (!persisted) {
                NodeEntry ne = NodeEntryImpl.this;
                ChildNodeEntriesImpl parentCNEs = (ChildNodeEntriesImpl) parent.childNodeEntries;
                parentCNEs.reorderAfter(ne, revertInfo.oldPredecessor);
                try {
                    if (oldIndex != ne.getIndex()) {
                        // TODO: TOBEFIXED
                        log.warn("Reverting didn't restore the correct index.");
                    }
                } catch (RepositoryException e) {
                    log.warn("Unable to calculate index. {}", e.getMessage());
                }
            }
            revertInfo = null;
        }
    }

}
