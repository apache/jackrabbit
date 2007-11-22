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

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ChangeLog;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateLifeCycleListener;
import org.apache.jackrabbit.jcr2spi.util.StateUtility;
import org.apache.jackrabbit.name.NameConstants;
import org.apache.jackrabbit.name.PathBuilder;
import org.apache.commons.collections.iterators.IteratorChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.InvalidItemStateException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.LinkedHashMap;

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
    private ChildNodeEntries childNodeEntries;

    /**
     * Map used to remember transiently removed or moved childNodeEntries, that
     * must not be retrieved from the persistent storage.
     */
    private ChildNodeAttic childNodeAttic;

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
    private final Map propertiesInAttic;

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
    private NodeEntryImpl(NodeEntryImpl parent, Name name, String uniqueID, EntryFactory factory) {
        super(parent, name, factory);
        this.uniqueID = uniqueID; // NOTE: don't use setUniqueID (for mod only)

        properties = new ChildPropertyEntriesImpl(this, factory);

        propertiesInAttic = new HashMap();
        childNodeAttic = new ChildNodeAttic();

        factory.notifyEntryCreated(this);
    }

    /**
     *
     * @return
     */
    static NodeEntry createRootEntry(EntryFactory factory) {
        return new NodeEntryImpl(null, NameConstants.ROOT, null, factory);
    }

    /**
     *
     * @param parent
     * @param name
     * @param uniqueId
     * @param factory
     * @return
     */
    static NodeEntry createNodeEntry(NodeEntryImpl parent, Name name, String uniqueId, EntryFactory factory) {
        return new NodeEntryImpl(parent, name, uniqueId, factory);
    }

    //-----------------------------------------------------< HierarchyEntry >---
    /**
     * Returns true.
     *
     * @inheritDoc
     * @see HierarchyEntry#denotesNode()
     */
    public boolean denotesNode() {
        return true;
    }

    /**
     * @inheritDoc
     * @see HierarchyEntry#invalidate(boolean)
     */
    public void invalidate(boolean recursive) {
        if (recursive) {
            // invalidate all child entries including properties present in the
            // attic (removed props shadowed by a new property with the same name).
            for (Iterator it = getAllChildEntries(true); it.hasNext();) {
                HierarchyEntry ce = (HierarchyEntry) it.next();
                ce.invalidate(recursive);
            }
        }
        // invalidate 'childNodeEntries'
        if (getStatus() != Status.NEW && childNodeEntries != null) {
            childNodeEntries.setStatus(ChildNodeEntries.STATUS_INVALIDATED);
        }
        // ... and invalidate the resolved state (if available)
        super.invalidate(recursive);
    }

    /**
     * If 'recursive' is true, the complete hierarchy below this entry is
     * traversed and reloaded. Otherwise only this entry and the direct
     * decendants are reloaded.
     *
     * @see HierarchyEntry#reload(boolean, boolean)
     */
    public void reload(boolean keepChanges, boolean recursive) {
        // reload this entry
        super.reload(keepChanges, recursive);

        // reload all children unless 'recursive' is false and the reload above
        // did not cause this entry to be removed -> therefore check status.
        if (recursive && !Status.isTerminal(getStatus())) {
            // recursivly reload all entries including props that are in the attic.
            for (Iterator it = getAllChildEntries(true); it.hasNext();) {
                HierarchyEntry ce = (HierarchyEntry) it.next();
                ce.reload(keepChanges, recursive);
            }
        }
    }

    /**
     * Calls {@link HierarchyEntryImpl#revert()} and moves all properties from the
     * attic back into th properties map. If this HierarchyEntry has been
     * transiently moved, it is in addition moved back to its old parent.
     * Similarly reordering of child node entries is reverted.
     *
     * @inheritDoc
     * @see HierarchyEntry#revert()
     */
    public void revert() throws RepositoryException {
        // move all properties from attic back to properties map
        if (!propertiesInAttic.isEmpty()) {
            properties.addAll(propertiesInAttic.values());
            propertiesInAttic.clear();
        }

        revertTransientChanges();

        // now make sure the attached state is reverted to the original state
        super.revert();
    }

    /**
     * @see HierarchyEntry#transientRemove()
     */
    public void transientRemove() throws RepositoryException {
        for (Iterator it = getAllChildEntries(false); it.hasNext();) {
            HierarchyEntry ce = (HierarchyEntry) it.next();
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
    public void remove() {
        removeEntry(this);
        if (getStatus() != Status.STALE_DESTROYED && parent.childNodeEntries != null) {
            NodeEntry removed = parent.childNodeEntries.remove(this);
            if (removed == null) {
                // try attic
                parent.childNodeAttic.remove(this);
            }
        }

        // TODO: deal with childNodeAttic
        // now traverse all child-entries and mark the attached states removed
        // without removing the child-entries themselves. this is not required
        // since this (i.e. the parent is removed as well).
        for (Iterator it = getAllChildEntries(true); it.hasNext();) {
            HierarchyEntryImpl ce = (HierarchyEntryImpl) it.next();
            removeEntry(ce);
        }
    }

    /**
     * If the underlying state is available and transiently modified, new or
     * stale, it gets added to the changeLog. Subsequently this call is repeated
     * recursively to collect all child states that meet the condition,
     * including those property states that have been moved to the attic.
     *
     * @inheritDoc
     * @see HierarchyEntry#collectStates(ChangeLog, boolean)
     */
    public synchronized void collectStates(ChangeLog changeLog, boolean throwOnStale) throws InvalidItemStateException {
        super.collectStates(changeLog, throwOnStale);

        // collect transient child states including properties in attic.
        for (Iterator it = getAllChildEntries(true); it.hasNext();) {
            HierarchyEntry ce = (HierarchyEntry) it.next();
            ce.collectStates(changeLog, throwOnStale);
        }
    }

    //----------------------------------------------------------< NodeEntry >---
    /**
     * @inheritDoc
     * @see NodeEntry#getId()
     */
    public NodeId getId() {
        IdFactory idFactory = factory.getIdFactory();
        if (uniqueID != null) {
            return idFactory.createNodeId(uniqueID);
        } else {
            PathFactory pf = factory.getPathFactory();
            if (parent == null) {
                // root node
                return idFactory.createNodeId((String) null, pf.getRootPath());
            } else {
                Path p = pf.create(getName(), getIndex());
                return idFactory.createNodeId(parent.getId(), p);
            }
        }
    }

    /**
     * @see NodeEntry#getWorkspaceId()
     */
    public NodeId getWorkspaceId() {
        IdFactory idFactory = factory.getIdFactory();
        if (uniqueID != null || parent == null) {
            // uniqueID and root-node -> internal id is always the same as getId().
            return getId();
        } else {
            PathFactory pf = factory.getPathFactory();
            NodeId parentId = (revertInfo != null) ? revertInfo.oldParent.getWorkspaceId() : parent.getWorkspaceId();
            return idFactory.createNodeId(parentId, pf.create(getWorkspaceName(), getWorkspaceIndex()));
        }
    }

    /**
     * @inheritDoc
     * @see NodeEntry#getUniqueID()
     */
    public String getUniqueID() {
        return uniqueID;
    }

    /**
     * @inheritDoc
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
     * @inheritDoc
     * @see NodeEntry#getIndex()
     */
    public int getIndex() {
        if (parent == null) {
            // the root state may never have siblings
            return Path.INDEX_DEFAULT;
        }

        NodeState state = (NodeState) internalGetItemState();
        try {
            if (state == null || !state.hasDefinition() || state.getDefinition().allowsSameNameSiblings()) {
                return parent.getChildIndex(this);
            } else {
                return Path.INDEX_DEFAULT;
            }
        } catch (RepositoryException e) {
            log.error("Error while building Index. ", e.getMessage());
            return Path.INDEX_UNDEFINED;
        }
    }

    /**
     * @inheritDoc
     * @see NodeEntry#getNodeState()
     */
    public NodeState getNodeState() throws ItemNotFoundException, RepositoryException {
        return (NodeState) getItemState();
    }

    /**
     * @inheritDoc
     * @see NodeEntry#getDeepEntry(Path)
     */
    public HierarchyEntry getDeepEntry(Path path) throws PathNotFoundException, RepositoryException {
        NodeEntryImpl entry = this;
        Path.Element[] elems = path.getElements();
        for (int i = 0; i < elems.length; i++) {
            Path.Element elem = (Path.Element) elems[i];
            // check for root element
            if (elem.denotesRoot()) {
                if (getParent() != null) {
                    throw new RepositoryException("NodeEntry out of 'hierarchy'" + path.toString());
                }
                continue;
            }

            int index = elem.getNormalizedIndex();
            Name name = elem.getName();

            // first try to resolve to known node or property entry
            NodeEntry cne = (entry.childNodeEntries == null) ? null : entry.getNodeEntry(name, index, false);
            if (cne != null) {
                entry = (NodeEntryImpl) cne;
            } else if (index == Path.INDEX_DEFAULT && i == path.getLength() - 1 && entry.properties.contains(name)) {
                // property must not have index && must be final path element
                return entry.properties.get(name);
            } else {
                // no valid entry
                // -> check for moved child entry in node-attic
                // -> check if child points to a removed/moved sns
                if (entry.childNodeEntries != null) {
                    List siblings = entry.childNodeEntries.get(name);
                    if (entry.containsAtticChild(siblings, name, index)) {
                        throw new PathNotFoundException(path.toString());
                    }
                }
               /*
                * Unknown entry (not-existing or not yet loaded):
                * Skip all intermediate entries and directly try to load the ItemState
                * (including building the itermediate entries. If that fails
                * ItemNotFoundException is thrown.
                *
                * Since 'path' might be ambigous (Node or Property):
                * 1) first try Node
                * 2) if the NameElement does not have SNS-index => try Property
                * 3) else throw
                */
                PathBuilder pb = new PathBuilder(factory.getPathFactory());
                for (int j = i; j < elems.length; j++) {
                    pb.addLast(elems[j]);
                }
                Path remainingPath = pb.getPath();

                NodeId parentId = entry.getId();
                IdFactory idFactory = factory.getIdFactory();

                NodeId nodeId = idFactory.createNodeId(parentId, remainingPath);
                NodeEntry ne = entry.loadNodeEntry(nodeId);
                if (ne != null) {
                    return ne;
                } else {
                    if (index != Path.INDEX_DEFAULT) {
                        throw new PathNotFoundException(path.toString());
                    }
                    // maybe a property entry exists
                    parentId = (remainingPath.getLength() == 1) ? parentId : idFactory.createNodeId(parentId, remainingPath.getAncestor(1));
                    PropertyId propId = idFactory.createPropertyId(parentId, remainingPath.getNameElement().getName());
                    PropertyEntry pe = entry.loadPropertyEntry(propId);
                    if (pe != null) {
                        return pe;
                    } else {
                        throw new PathNotFoundException(path.toString());
                    }
                }
            }
        }
        return entry;
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
     * @inheritDoc
     * @see NodeEntry#hasNodeEntry(Name)
     */
    public synchronized boolean hasNodeEntry(Name nodeName) {
        try {
            List namedEntries = childNodeEntries().get(nodeName);
            if (namedEntries.isEmpty()) {
                return false;
            } else {
                return EntryValidation.containsValidNodeEntry(namedEntries.iterator());
            }
        } catch (RepositoryException e) {
            log.debug("Unable to determine if a child node with name " + nodeName + " exists.");
            return false;
        }
    }

    /**
     * @inheritDoc
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
     * @inheritDoc
     * @see NodeEntry#getNodeEntry(Name, int)
     */
    public synchronized NodeEntry getNodeEntry(Name nodeName, int index) throws RepositoryException {
        return getNodeEntry(nodeName, index, false);
    }

    /**
     * @inheritDoc
     * @see NodeEntry#getNodeEntry(Name, int, boolean)
     */
    public NodeEntry getNodeEntry(Name nodeName, int index, boolean loadIfNotFound) throws RepositoryException {
        // TODO: avoid loading the child-infos if childNodeEntries == null
        List entries = childNodeEntries().get(nodeName);
        NodeEntry cne = null;
        if (entries.size() >= index) {
            // position of entry might differ from index-1 if a SNS with lower
            // index has been transiently removed.
            for (int i = index-1; i < entries.size() && cne == null; i++) {
                NodeEntry ne = (NodeEntry) entries.get(i);
                if (EntryValidation.isValidNodeEntry(ne)) {
                    cne = ne;
                }
            }
        } else if (loadIfNotFound
                && !containsAtticChild(entries, nodeName, index)
                && Status.NEW != getStatus()) {

            PathFactory pf = factory.getPathFactory();
            NodeId cId = factory.getIdFactory().createNodeId(getId(), pf.create(nodeName, index));
            cne = loadNodeEntry(cId);
        }
        return cne;
    }

    /**
     * @inheritDoc
     * @see NodeEntry#getNodeEntries()
     */
    public synchronized Iterator getNodeEntries() throws RepositoryException {
        Collection entries = new ArrayList();
        for (Iterator it = childNodeEntries().iterator(); it.hasNext();) {
            NodeEntry entry = (NodeEntry) it.next();
            if (EntryValidation.isValidNodeEntry(entry)) {
                entries.add(entry);
            }
        }
        return Collections.unmodifiableCollection(entries).iterator();
    }

    /**
     * @see NodeEntry#getNodeEntries(Name)
     */
    public synchronized List getNodeEntries(Name nodeName) throws RepositoryException {
        List namedEntries = childNodeEntries().get(nodeName);
        if (namedEntries.isEmpty()) {
            return Collections.EMPTY_LIST;
        } else {
            List entries = new ArrayList();
            // get array of the list, since during validation the childNodeEntries
            // may be modified if upon NodeEntry.getItemState the entry gets removed.
            Object[] arr = namedEntries.toArray();
            for (int i = 0; i < arr.length; i++) {
                NodeEntry cne = (NodeEntry) arr[i];
                if (EntryValidation.isValidNodeEntry(cne)) {
                    entries.add(cne);
                }
            }
            return Collections.unmodifiableList(entries);
        }
    }

    /**
     * @inheritDoc
     * @see NodeEntry#addNodeEntry(Name, String, int)
     */
    public NodeEntry addNodeEntry(Name nodeName, String uniqueID, int index) throws RepositoryException {
        return internalAddNodeEntry(nodeName, uniqueID, index, childNodeEntries());
    }

    /**
     * @inheritDoc
     * @see NodeEntry#addNewNodeEntry(Name, String, Name, QNodeDefinition)
     */
    public NodeState addNewNodeEntry(Name nodeName, String uniqueID,
                                     Name primaryNodeType, QNodeDefinition definition) throws RepositoryException {
        NodeEntry entry = internalAddNodeEntry(nodeName, uniqueID, Path.INDEX_UNDEFINED, childNodeEntries());
        NodeState state = factory.getItemStateFactory().createNewNodeState(entry, primaryNodeType, definition);
        if (!entry.isAvailable()) {
            entry.setItemState(state);
        }
        return state;
    }

    /**
     *
     * @param nodeName
     * @param uniqueID
     * @param index
     * @param childEntries
     * @return
     */
    private NodeEntry internalAddNodeEntry(Name nodeName, String uniqueID,
                                           int index, ChildNodeEntries childEntries) {
        NodeEntry entry = factory.createNodeEntry(this, nodeName, uniqueID);
        childEntries.add(entry, index);
        return entry;
    }

    /**
     * @inheritDoc
     * @see NodeEntry#hasPropertyEntry(Name)
     */
    public synchronized boolean hasPropertyEntry(Name propName) {
        PropertyEntry entry = properties.get(propName);
        return EntryValidation.isValidPropertyEntry(entry);
    }

    /**
     * @inheritDoc
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
     * @inheritDoc
     * @see NodeEntry#getPropertyEntry(Name, boolean)
     */
    public PropertyEntry getPropertyEntry(Name propName, boolean loadIfNotFound) throws RepositoryException {
        PropertyEntry entry = properties.get(propName);
        if (entry == null && loadIfNotFound && Status.NEW != getStatus()) {
            PropertyId propId = factory.getIdFactory().createPropertyId(getId(), propName);
            entry = loadPropertyEntry(propId);
        } else if (!EntryValidation.isValidPropertyEntry(entry)) {
            entry = null;
        }
        return entry;
    }

    /**
     * @inheritDoc
     * @see NodeEntry#getPropertyEntries()
     */
    public synchronized Iterator getPropertyEntries() {
        Collection props;
        if (getStatus() == Status.EXISTING_MODIFIED) {
            // filter out removed properties
            props = new ArrayList();
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
        return Collections.unmodifiableCollection(props).iterator();
    }

    /**
     * @inheritDoc
     * @see NodeEntry#addPropertyEntry(Name)
     */
    public PropertyEntry addPropertyEntry(Name propName) throws ItemExistsException {
        // TODO: check for existing prop.
        return internalAddPropertyEntry(propName, true);
    }

    /**
     * Internal method that adds a PropertyEntry without checking of that entry
     * exists.
     *
     * @param propName
     * @param notifySpecial
     * @return
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
     * @inheritDoc
     * @see NodeEntry#addPropertyEntries(Collection)
     */
    public void addPropertyEntries(Collection propNames) throws ItemExistsException, RepositoryException {
        Set diff = new HashSet();
        diff.addAll(properties.getPropertyNames());
        boolean containsExtra = diff.removeAll(propNames);

        // add all entries that are missing
        for (Iterator it = propNames.iterator(); it.hasNext();) {
            Name propName = (Name) it.next();
            if (!properties.contains(propName)) {
                // TODO: check again.
                // addPropertyEntries is used by WorkspaceItemStateFactory upon
                // creating a NodeState, in which case the uuid/mixins are set
                // anyway and not need exists to explicitely load the corresponding
                // property state in order to retrieve the values.
                internalAddPropertyEntry(propName, false);
            }
        }

        // if this entry has not yet been resolved or if it is 'invalidated'
        // all property entries, that are not contained within the specified
        // collection of property names are removed from this NodeEntry.
        ItemState state = internalGetItemState();
        if (containsExtra && (state == null || state.getStatus() == Status.INVALIDATED)) {
            for (Iterator it = diff.iterator(); it.hasNext();) {
                Name propName = (Name) it.next();
                PropertyEntry pEntry = properties.get(propName);
                if (pEntry != null) {
                    pEntry.remove();
                }
            }
        }
    }

    /**
     * @inheritDoc
     * @see NodeEntry#addNewPropertyEntry(Name, QPropertyDefinition)
     */
    public PropertyState addNewPropertyEntry(Name propName, QPropertyDefinition definition)
            throws ItemExistsException, RepositoryException {
        // check for an existing property
        PropertyEntry existing = properties.get(propName);
        if (existing != null) {
            try {
                PropertyState existingState = existing.getPropertyState();
                int status = existingState.getStatus();

                if (Status.isTerminal(status)) {
                    // an old property-entry that is not valid any more
                    properties.remove(propName);
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
                properties.remove(propName);
            } catch (RepositoryException e) {
                // some other error -> remove from properties map
                properties.remove(propName);
            }
        }

        // add the property entry
        PropertyEntry entry = factory.createPropertyEntry(this, propName);
        properties.add(entry);

        PropertyState state = factory.getItemStateFactory().createNewPropertyState(entry, definition);
        if (!entry.isAvailable()) {
            entry.setItemState(state);
        }

        return state;
    }

    /**
     * @param propName
     */
    void internalRemovePropertyEntry(Name propName) {
        if (!properties.remove(propName)) {
            propertiesInAttic.remove(propName);
        }
        // special properties
        if (StateUtility.isUuidOrMixin(propName)) {
            notifyUUIDorMIXINRemoved(propName);
        }
    }

    /**
     * @inheritDoc
     * @see NodeEntry#orderBefore(NodeEntry)
     */
    public void orderBefore(NodeEntry beforeEntry) throws RepositoryException {
        if (Status.NEW == getStatus()) {
            // new states get remove upon revert
            parent.childNodeEntries().reorder(this, beforeEntry);
        } else {
            createSiblingRevertInfos();
            parent.createRevertInfo();
            // now reorder child entries on parent
            NodeEntry previousBefore = parent.childNodeEntries().reorder(this, beforeEntry);
            parent.revertInfo.reordered(this, previousBefore);
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
       if (transientMove && !isTransientlyMoved() && Status.NEW != getStatus()) {
           createSiblingRevertInfos();
           createRevertInfo();
           parent.childNodeAttic.add(this);
       }

       NodeEntryImpl entry = (NodeEntryImpl) parent.childNodeEntries().remove(this);
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
       parent.childNodeEntries().add(this);
       return this;
   }

    /**
     * @see NodeEntry#isTransientlyMoved()
     */
    public boolean isTransientlyMoved() {
        return revertInfo != null && revertInfo.isMoved();
    }

    /**
     * @param childEvent
     * @see NodeEntry#refresh(Event)
     */
    public void refresh(Event childEvent) {
        Name eventName = childEvent.getPath().getNameElement().getName();
        switch (childEvent.getType()) {
            case Event.NODE_ADDED:
                if (childNodeEntries == null) {
                    // childNodeEntries not yet loaded -> ignore
                    return;
                }

                int index = childEvent.getPath().getNameElement().getNormalizedIndex();
                String uniqueChildID = null;
                if (childEvent.getItemId().getPath() == null) {
                    uniqueChildID = childEvent.getItemId().getUniqueID();
                }

                // TODO: TOBEFIXED for SNSs
                // first check if no matching child entry exists.
                NodeEntry cne;
                if (uniqueChildID != null) {
                    cne = childNodeEntries.get(eventName, uniqueChildID);
                    if (cne == null) {
                        // entry may exist but without having uniqueID resolved
                        cne = childNodeEntries.get(eventName, index);
                    }
                } else {
                    cne = childNodeEntries.get(eventName, index);
                }
                if (cne == null) {
                    internalAddNodeEntry(eventName, uniqueChildID, index, childNodeEntries);
                } else {
                    // child already exists -> deal with NEW entries, that were
                    // added by some other session.
                    // TODO: TOBEFIXED
                }
                break;

            case Event.PROPERTY_ADDED:
                // create a new property reference if it has not been
                // added by some earlier 'add' event
                HierarchyEntry child = lookupEntry(childEvent.getItemId(), childEvent.getPath());
                if (child == null) {
                    internalAddPropertyEntry(eventName, true);
                } else {
                    child.reload(false, true);
                }
                break;

            case Event.NODE_REMOVED:
            case Event.PROPERTY_REMOVED:
                child = lookupEntry(childEvent.getItemId(), childEvent.getPath());
                if (child != null) {
                    child.remove();
                } // else: child-Entry has not been loaded yet -> ignore
                break;

            case Event.PROPERTY_CHANGED:
                child = lookupEntry(childEvent.getItemId(), childEvent.getPath());
                if (child == null) {
                    // prop-Entry has not been loaded yet -> add propEntry
                    internalAddPropertyEntry(eventName, true);
                } else if (child.isAvailable()) {
                    // Reload data from server and try to merge them with the
                    // current session-state. if the latter is transiently
                    // modified and merge fails it must be marked STALE afterwards.
                    child.reload(false, false);
                    // special cases: jcr:uuid and jcr:mixinTypes affect the parent
                    // (i.e. this NodeEntry) since both props are protected
                    if (StateUtility.isUuidOrMixin(eventName)) {
                        notifyUUIDorMIXINModified((PropertyEntry) child);
                    }
                } // else: existing entry but state not yet built -> ignore event
                break;
            default:
                // ILLEGAL
                throw new IllegalArgumentException("Illegal event type " + childEvent.getType() + " for NodeState.");
        }
    }
    //-------------------------------------------------< HierarchyEntryImpl >---
    /**
     * @inheritDoc
     * @see HierarchyEntryImpl#doResolve()
     * <p/>
     * Returns a <code>NodeState</code>.
     */
    ItemState doResolve() throws ItemNotFoundException, RepositoryException {
        return factory.getItemStateFactory().createNodeState(getWorkspaceId(), this);
    }

    /**
     * @see HierarchyEntryImpl#buildPath(boolean)
     */
    Path buildPath(boolean wspPath) throws RepositoryException {
        PathFactory pf = factory.getPathFactory();
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

        int index = (wspPath) ? nEntry.getWorkspaceIndex() : nEntry.getIndex();
        Name name = (wspPath) ? nEntry.getWorkspaceName() : nEntry.getName();
        // add to path
        if (index == Path.INDEX_UNDEFINED) {
            throw new RepositoryException("Invalid index " + index + " with nodeEntry " + nEntry);
        }

        // TODO: check again. special treatment for default index for consistency with PathFormat.parse
        builder.addLast(name, index);
    }

    //-----------------------------------------------< private || protected >---
    /**
     * @throws IllegalArgumentException if <code>this</code> is not the parent
     * of the given <code>ItemState</code>.
     */
    synchronized void revertPropertyRemoval(PropertyEntry propertyEntry) {
        if (propertyEntry.getParent() != this) {
            throw new IllegalArgumentException("Internal error: Parent mismatch.");
        }
        Name propName = propertyEntry.getName();
        if (propertiesInAttic.containsKey(propName)) {
            properties.add((PropertyEntry) propertiesInAttic.remove(propName));
        } // else: propEntry has never been moved to the attic (see 'addPropertyEntry')
    }

    /**
     *
     * @param oldName
     * @param oldIndex
     * @return
     */
    boolean matches(Name oldName, int oldIndex) {
        return getWorkspaceName().equals(oldName) && getWorkspaceIndex() == oldIndex;
    }

    /**
     *
     * @param oldName
     * @return
     */
    boolean matches(Name oldName) {
        return getWorkspaceName().equals(oldName);
    }


    private Name getWorkspaceName() {
        if (revertInfo != null) {
            return revertInfo.oldName;
        } else {
            return getName();
        }
    }

    private int getWorkspaceIndex() {
        if (revertInfo != null) {
            return revertInfo.oldIndex;
        } else {
            return getIndex();
        }
    }

    /**
     *
     * @param childId
     * @return
     */
    private NodeEntry loadNodeEntry(NodeId childId) throws RepositoryException {
        try {
            NodeState state = factory.getItemStateFactory().createDeepNodeState(childId, this);
            return state.getNodeEntry();
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    /**
     *
     * @param childId
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    private PropertyEntry loadPropertyEntry(PropertyId childId) throws RepositoryException {
        try {
            PropertyState state = factory.getItemStateFactory().createDeepPropertyState(childId, this);
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
     * @return
     */
    private HierarchyEntry lookupEntry(ItemId eventId, Path eventPath) {
        Name childName = eventPath.getNameElement().getName();
        HierarchyEntry child;
        if (eventId.denotesNode()) {
            String uniqueChildID = (eventId.getPath() == null) ? eventId.getUniqueID() : null;
            int index = eventPath.getNameElement().getNormalizedIndex();
            child = lookupNodeEntry(uniqueChildID, childName, index);
        } else {
            child = lookupPropertyEntry(childName);
        }
        // a NEW hierarchyEntry may never be affected by an external modification
        // -> return null.
        return (child == null || child.getStatus() == Status.NEW) ? null : child;
    }

    private NodeEntry lookupNodeEntry(String uniqueChildId, Name childName, int index) {
        NodeEntry child = null;
        if (uniqueChildId != null) {
            child = childNodeAttic.get(uniqueChildId);
            if (child == null && childNodeEntries != null) {
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
        PropertyEntry child = (PropertyEntry) propertiesInAttic.get(childName);
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
    private ChildNodeEntries childNodeEntries() throws InvalidItemStateException, RepositoryException {
        try {
            if (childNodeEntries == null) {
                childNodeEntries = new ChildNodeEntriesImpl(this, factory);
            } else if (childNodeEntries.getStatus() == ChildNodeEntries.STATUS_INVALIDATED) {
                childNodeEntries.reload();
            }
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
     * @return
     */
    private Iterator getAllChildEntries(boolean includeAttic) {
        IteratorChain chain = new IteratorChain();
        // attic
        if (includeAttic) {
            Collection attic = propertiesInAttic.values();
            chain.addIterator(new ArrayList(attic).iterator());
        }
        // add props
        synchronized (properties) {
            Collection props = properties.getPropertyEntries();
            chain.addIterator(props.iterator());
        }
        // add childNodeEntries
        if (childNodeEntries != null) {
            chain.addIterator(childNodeEntries.iterator());
        }
        return chain;
    }

    /**
     * Returns the index of the given <code>NodeEntry</code>.
     *
     * @param cne  the <code>NodeEntry</code> instance.
     * @return the index of the child node entry.
     * @throws ItemNotFoundException if the given entry isn't a valid child of
     * this <code>NodeEntry</code>.
     */
    private int getChildIndex(NodeEntry cne) throws ItemNotFoundException, RepositoryException {
        List sns = childNodeEntries().get(cne.getName());
        // index is one based
        int index = Path.INDEX_DEFAULT;
        for (Iterator it = sns.iterator(); it.hasNext(); ) {
            NodeEntry entry = (NodeEntry) it.next();
            if (entry == cne) {
                return index;
            }
            // skip entries that belong to removed or invalid states.
            // NOTE, that in this case the nodestate must be available from the cne.
            if (EntryValidation.isValidNodeEntry(entry)) {
                index++;
            }
        }
        // not found (should not occur)
        throw new ItemNotFoundException("No valid child entry for NodeEntry " + cne);
    }

    /**
     * Returns true if the attic contains a matching child entry or if any of
     * the remaining child entries present in the siblings list has been modified
     * in a way that its original index is equal to the given child index.
     *
     * @param siblings
     * @param childName
     * @param childIndex
     * @return
     */
    private boolean containsAtticChild(List siblings, Name childName, int childIndex) {
        // check if a matching entry exists in the attic
        if (childNodeAttic.contains(childName, childIndex)) {
            return true;
        }
        // in case of reordered/moved SNSs we also have to look for a child
        // entry, which hold the given index before
        if (getStatus() == Status.EXISTING_MODIFIED) {
            for (Iterator it = siblings.iterator(); it.hasNext();) {
                NodeEntryImpl child = (NodeEntryImpl) it.next();
                if (!EntryValidation.isValidNodeEntry(child) || (child.revertInfo != null && child.revertInfo.oldIndex == childIndex)) {
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
    private void createRevertInfo() {
        if (revertInfo == null) {
            revertInfo = new RevertInfo(parent, name, getIndex());
        }
    }

    /**
     * Special handling for MOVE and REORDER with same-name-siblings
     */
    private void createSiblingRevertInfos() throws RepositoryException {
        if (revertInfo != null) {
            return; // nothing to do
        }
        // for SNSs without UniqueID remember original index in order to
        // be able to build the workspaceID TODO: improve
        List sns = parent.childNodeEntries().get(name);
        if (sns.size() > 1) {
            for (Iterator it = sns.iterator(); it.hasNext();) {
                NodeEntryImpl sibling = (NodeEntryImpl) it.next();
                if (sibling.getUniqueID() == null && Status.NEW != sibling.getStatus()) {
                    sibling.createRevertInfo();
                }
            }
        }
    }

    /**
     * Revert a transient move and reordering of child entries
     */
    private void revertTransientChanges() throws RepositoryException {
        if (revertInfo == null) {
            return; // nothing to do
        }

        if (isTransientlyMoved())  {
            // move NodeEntry back to its original parent
            // TODO improve for simple renaming
            parent.childNodeEntries().remove(this);
            revertInfo.oldParent.childNodeAttic.remove(this);

            // now restore moved entry with the old name and index and re-add
            // it to its original parent (unless it got destroyed)
            parent = revertInfo.oldParent;
            name = revertInfo.oldName;
            ItemState state = internalGetItemState();
            if (state != null && !Status.isTerminal(state.getStatus())) {
                parent.childNodeEntries().add(this, revertInfo.oldIndex);
            }
        }
        // revert reordering of child-node-entries
        revertInfo.revertReordering();

        revertInfo.dispose();
        revertInfo = null;
    }

    /**
     * This entry has be set to 'EXISTING' again -> move and/or reordering of
     * child entries has been completed and the 'revertInfo' needs to be
     * reset/removed.
     */
    private void completeTransientChanges() {
        // old parent can forget this one
        // root entry does not have oldParent
        if (revertInfo.oldParent != null) {
            revertInfo.oldParent.childNodeAttic.remove(this);
        }
        revertInfo.dispose();
        revertInfo = null;
    }

    //--------------------------------------------------------< inner class >---
    /**
     * Upon move of this entry or upon reorder of its child-entries store
     * original hierarchy information for later revert and in order to be able
     * to build the workspace id(s).
     */
    private class RevertInfo implements ItemStateLifeCycleListener {

        private NodeEntryImpl oldParent;
        private Name oldName;
        private int oldIndex;

        private Map reorderedChildren;

        private RevertInfo(NodeEntryImpl oldParent, Name oldName, int oldIndex) {
            this.oldParent = oldParent;
            this.oldName = oldName;
            this.oldIndex = oldIndex;

            ItemState state = internalGetItemState();
            if (state != null) {
                state.addListener(this);
            } // else: should never be null.
        }

        private void dispose() {
            ItemState state = internalGetItemState();
            if (state != null) {
                state.removeListener(this);
            }

            if (reorderedChildren != null) {
                // special handling of SNS-children  TODO: improve
                // since reordered sns-children are not marked modified (unless they
                // got modified by some other action, their revertInfo
                // must be disposed manually
                for (Iterator it = reorderedChildren.keySet().iterator(); it.hasNext();) {
                    NodeEntry ne = (NodeEntry) it.next();
                    List sns = childNodeEntries.get(ne.getName());
                    if (sns.size() > 1) {
                        for (Iterator snsIt = sns.iterator(); snsIt.hasNext();) {
                            NodeEntryImpl sibling = (NodeEntryImpl) snsIt.next();
                            if (sibling.revertInfo != null && Status.EXISTING == sibling.getStatus()) {
                                sibling.revertInfo.dispose();
                                sibling.revertInfo = null;
                            }
                        }
                    }
                }
                reorderedChildren.clear();
            }
        }

        private boolean isMoved() {
            return oldParent != getParent() || !getName().equals(oldName);
        }

        private void reordered(NodeEntry insertEntry, NodeEntry previousBefore) {
            if (reorderedChildren == null) {
                reorderedChildren = new LinkedHashMap();
            }
            reorderedChildren.put(insertEntry, previousBefore);
        }

        private void revertReordering() {
            if (reorderedChildren == null) {
                return; // nothing to do
            }
            // revert all 'reorder' calls in in reverse other they were performed
            NodeEntry[] reordered = (NodeEntry[]) reorderedChildren.keySet().toArray(new NodeEntry[reorderedChildren.size()]);
            for (int i = reordered.length-1; i >= 0; i--) {
                NodeEntry ordered = reordered[i];
                if (isValidReorderedChild(ordered)) {
                    NodeEntry previousBefore = (NodeEntry) reorderedChildren.get(ordered);
                    if (previousBefore == null || isValidReorderedChild(previousBefore)) {
                        childNodeEntries.reorder(ordered, previousBefore);
                    }
                }
            }
        }

        private boolean isValidReorderedChild(NodeEntry child) {
            if (Status.isTerminal(child.getStatus())) {
                log.warn("Cannot revert reordering. 'previousBefore' does not exist any more.");
                return false;
            }
            if (child.isTransientlyMoved()) {
                // child has been moved away -> move back
                try {
                    child.revert();
                } catch (RepositoryException e) {
                    log.error("Internal error", e);
                    return false;
                }
            }
            return true;
        }

        /**
         * @see ItemStateLifeCycleListener#statusChanged(ItemState, int)
         */
        public void statusChanged(ItemState state, int previousStatus) {
            switch (state.getStatus()) {
                case Status.EXISTING:
                    // stop listening
                    state.removeListener(this);
                    completeTransientChanges();
                    break;

                case Status.REMOVED:
                case Status.STALE_DESTROYED:
                    // stop listening
                    state.removeListener(this);
                    // remove from the attic
                    try {
                        revertTransientChanges();
                    } catch (RepositoryException e) {
                        log.warn("Internal error", e);
                    }
                    break;
            }
        }
    }
}
