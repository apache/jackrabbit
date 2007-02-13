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

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.NoSuchItemStateException;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ChangeLog;
import org.apache.jackrabbit.jcr2spi.state.StaleItemStateException;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.util.StateUtility;
import org.apache.commons.collections.iterators.IteratorChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.PathNotFoundException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * <code>NodeEntryImpl</code> implements common functionality for child
 * node entry implementations.
 */
public class NodeEntryImpl extends HierarchyEntryImpl implements NodeEntry {

    // TODO: TOBEFIXED attic for transiently removed or moved child-node-entries required, in order 'know' that they are remove instead of retrieving them from the server again.

    private static Logger log = LoggerFactory.getLogger(NodeEntryImpl.class);

    /**
     * UniqueID identifying this NodeEntry or <code>null</code> if either
     * the underlying state has not been loaded yet or if it cannot be
     * identified with a unique ID.
     */
    private String uniqueID;

    /**
     * insertion-ordered collection of NodeEntry objects
     */
    private ChildNodeEntries childNodeEntries;

    /**
     * Map of properties. Key = {@link QName} of property. Value = {@link
     * PropertyEntry}.
     */
    private final HashMap properties = new HashMap();

    /**
     * Map of properties which are deleted and have been re-created as transient
     * property with the same name.
     */
    private final HashMap propertiesInAttic = new HashMap();

    /**
     * Creates a new <code>NodeEntryImpl</code>
     *
     * @param parent    the <code>NodeEntry</code> that owns this child item
     *                  reference.
     * @param name      the name of the child node.
     * @param factory   the entry factory.
     */
    NodeEntryImpl(NodeEntryImpl parent, QName name, String uniqueID, EntryFactory factory) {
        super(parent, name, factory);
        this.uniqueID = uniqueID; // NOTE: don't use setUniqueID (for mod only)
    }

    /**
     *
     * @return
     */
    static NodeEntry createRootEntry(EntryFactory factory) {
        return new NodeEntryImpl(null, QName.ROOT, null, factory);
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
            for (Iterator it = getAllChildEntries(false, true); it.hasNext();) {
                HierarchyEntry ce = (HierarchyEntry) it.next();
                ce.invalidate(recursive);
            }
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
            for (Iterator it = getAllChildEntries(true, true); it.hasNext();) {
                HierarchyEntry ce = (HierarchyEntry) it.next();
                ce.reload(keepChanges, recursive);
            }
        }
    }

    /**
     * Calls {@link HierarchyEntryImpl#revert()} and moves all properties from the
     * attic back into th properties map. If this HierarchyEntry has been
     * transiently moved, it is in addition moved back to its old parent.
     *
     * @inheritDoc
     * @see HierarchyEntry#revert()
     */
    public void revert() throws ItemStateException {
        // move all properties from attic back to properties map
        if (!propertiesInAttic.isEmpty()) {
            properties.putAll(propertiesInAttic);
            propertiesInAttic.clear();
        }
        // if this entry has been moved before -> move back
        NodeState state = (NodeState) internalGetItemState();
        if (state != null && StateUtility.isMovedState(state)) {
            // move NodeEntry back to its original parent
            parent.childNodeEntries().remove(this);
            NodeEntryImpl oldEntry = (NodeEntryImpl) state.getWorkspaceState().getHierarchyEntry();
            oldEntry.parent.childNodeEntries().add(oldEntry);

            factory.notifyEntryMoved(this, oldEntry);
        }
        // now make sure the underlying state is reverted to the original state
        super.revert();
    }

    /**
     * @see HierarchyEntry#transientRemove()
     */
    public void transientRemove() throws ItemStateException {
        for (Iterator it = getAllChildEntries(true, false); it.hasNext();) {
            HierarchyEntry ce = (HierarchyEntry) it.next();
            ce.transientRemove();
        }

        if (!propertiesInAttic.isEmpty()) {
            // move all properties from attic back to properties map
            properties.putAll(propertiesInAttic);
            propertiesInAttic.clear();
        }

        // execute for this entry as well
        super.transientRemove();
    }

    /**
     * @see HierarchyEntry#remove()
     */
    public void remove() {
        ItemState state = internalGetItemState();
        if (state != null) {
            if (state.getStatus() == Status.NEW) {
                state.setStatus(Status.REMOVED);
            } else {
                state.getWorkspaceState().setStatus(Status.REMOVED);
            }
        }
        parent.childNodeEntries().remove(this);

        // now traverse all child-entries and mark the attached states removed
        // without removing the child-entries themselves. this is not required
        // since this (i.e. the parent is removed as well).
        for (Iterator it = getAllChildEntries(true, true); it.hasNext();) {
            HierarchyEntryImpl ce = (HierarchyEntryImpl) it.next();
            state = ce.internalGetItemState();
            if (state != null) {
                if (state.getStatus() == Status.NEW) {
                    state.setStatus(Status.REMOVED);
                } else {
                    state.getWorkspaceState().setStatus(Status.REMOVED);
                }
            }
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
    public void collectStates(ChangeLog changeLog, boolean throwOnStale) throws StaleItemStateException {
        super.collectStates(changeLog, throwOnStale);

        // collect transient child states including properties in attic.
        for (Iterator it = getAllChildEntries(false, true); it.hasNext();) {
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
            if (parent == null) {
                // root node
                return idFactory.createNodeId((String) null, Path.ROOT);
            } else {
                return factory.getIdFactory().createNodeId(parent.getId(), Path.create(getQName(), getIndex()));
            }
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
            if (state == null || state.getDefinition().allowsSameNameSiblings()) {
                return parent.getChildIndex(this);
            } else {
                return Path.INDEX_DEFAULT;
            }
        } catch (RepositoryException e) {
            log.error("Error while building Index. ", e);
            return Path.INDEX_UNDEFINED;
        }
    }

    /**
     * @inheritDoc
     * @see NodeEntry#getNodeState()
     */
    public NodeState getNodeState()
            throws NoSuchItemStateException, ItemStateException {
        return (NodeState) getItemState();
    }

    /**
     * @inheritDoc
     * @see NodeEntry#getDeepEntry(Path)
     */
    public HierarchyEntry getDeepEntry(Path path) throws PathNotFoundException, RepositoryException {
        NodeEntryImpl entry = this;
        Path.PathElement[] elems = path.getElements();
        for (int i = 0; i < elems.length; i++) {
            Path.PathElement elem = elems[i];
            // check for root element
            if (elem.denotesRoot()) {
                if (getParent() != null) {
                    throw new RepositoryException("NodeEntry out of 'hierarchy'" + path.toString());
                } else {
                    continue;
                }
            }

            int index = elem.getNormalizedIndex();
            QName name = elem.getName();

            // first try to resolve to nodeEntry or property entry
            NodeEntry cne = (entry.childNodeEntries == null) ? null : entry.childNodeEntries.get(name, index);
            if (cne != null) {
                entry = (NodeEntryImpl) cne;
            } else if (index == Path.INDEX_DEFAULT && entry.properties.containsKey(name)
                && i == path.getLength() - 1) {
                // property must not have index && must be final path element
                PropertyEntry pe = (PropertyEntry) entry.properties.get(name);
                return pe;
            } else {
                /*
                * Unknown entry (not-existing or not yet loaded):
                * Skip all intermediate entries and directly try to load the ItemState
                * (including building the itermediate entries. If that fails
                * NoSuchItemStateException is thrown.
                *
                * Since 'path' might be ambigous (Node or Property):
                * 1) first try Node
                * 2) if the NameElement does not have SNS-index => try Property
                * 3) else throw
                */
                Path remainingPath;
                try {
                    Path.PathBuilder pb = new Path.PathBuilder();
                    for (int j = i; j < elems.length; j++) {
                        pb.addLast(elems[j]);
                    }
                    remainingPath = pb.getPath();
                } catch (MalformedPathException e) {
                    // should not get here
                    throw new RepositoryException("Invalid path");
                }

                NodeId anyId = entry.getId();
                IdFactory idFactory = entry.factory.getIdFactory();
                NodeId nodeId = idFactory.createNodeId(anyId, remainingPath);
                try {
                    NodeState state = entry.factory.getItemStateFactory().createDeepNodeState(nodeId, entry);
                    return state.getHierarchyEntry();
                } catch (NoSuchItemStateException e) {
                    if (index != Path.INDEX_DEFAULT) {
                        throw new PathNotFoundException(path.toString(), e);
                    }
                    // possibly  propstate
                    try {
                        nodeId = (remainingPath.getLength() == 1) ? anyId : idFactory.createNodeId(anyId, remainingPath.getAncestor(1));
                        PropertyId id = idFactory.createPropertyId(nodeId, remainingPath.getNameElement().getName());
                        PropertyState state = entry.factory.getItemStateFactory().createDeepPropertyState(id, entry);
                        return state.getHierarchyEntry();
                    } catch (NoSuchItemStateException ise) {
                        throw new PathNotFoundException(path.toString());
                    } catch (ItemStateException ise) {
                        throw new RepositoryException("Internal error", ise);
                    }
                } catch (ItemStateException e) {
                    throw new RepositoryException("Internal error", e);
                }
            }
        }
        return entry;
    }

    /**
     * @inheritDoc
     * @see NodeEntry#hasNodeEntry(QName)
     */
    public synchronized boolean hasNodeEntry(QName nodeName) {
        List namedEntries = childNodeEntries().get(nodeName);
        if (namedEntries.isEmpty()) {
            return false;
        } else {
            // copy list since during validation the childNodeEntries may be
            // modified if upon NodeEntry.getItemState the entry is removed.
            List l = new ArrayList(namedEntries.size());
            l.addAll(namedEntries);
            return EntryValidation.containsValidNodeEntry(l.iterator());
        }
    }

    /**
     * @inheritDoc
     * @see NodeEntry#hasNodeEntry(QName, int)
     */
    public synchronized boolean hasNodeEntry(QName nodeName, int index) {
        return EntryValidation.isValidNodeEntry(childNodeEntries().get(nodeName, index));
    }

    /**
     * @inheritDoc
     * @see NodeEntry#getNodeEntry(QName, int)
     */
    public synchronized NodeEntry getNodeEntry(QName nodeName, int index) {
        NodeEntry cne = childNodeEntries().get(nodeName, index);
        if (EntryValidation.isValidNodeEntry(cne)) {
            return cne;
        } else {
            return null;
        }
    }


    /**
     * @inheritDoc
     * @see NodeEntry#getNodeEntry(NodeId)
     */
    public synchronized NodeEntry getNodeEntry(NodeId childId) {
        String uid = childId.getUniqueID();
        Path path = childId.getPath();
        NodeEntry cne;
        if (uid != null && path == null) {
            // retrieve child-entry by uid
            cne = childNodeEntries().get(null, uid);
        } else {
           // retrieve child-entry by name and index
            Path.PathElement nameElement = path.getNameElement();
            cne = childNodeEntries().get(nameElement.getName(), nameElement.getIndex());
        }

        if (EntryValidation.isValidNodeEntry(cne)) {
            return cne;
        } else {
            return null;
        }
    }

    /**
     * @inheritDoc
     * @see NodeEntry#getNodeEntries()
     */
    public synchronized Iterator getNodeEntries() {
        Collection entries = new ArrayList();
        Object[] arr = childNodeEntries().toArray();
        for (int i = 0; i < arr.length; i++) {
            NodeEntry cne = (NodeEntry) arr[i];
            if (EntryValidation.isValidNodeEntry(cne)) {
                entries.add(cne);
            }
        }
        return Collections.unmodifiableCollection(entries).iterator();
    }

    /**
     * Returns a unmodifiable list of <code>NodeEntry</code>s with the
     * specified name.
     *
     * @param nodeName name of the child node entries that should be returned
     * @return list of <code>NodeEntry</code> objects
     */
    public synchronized List getNodeEntries(QName nodeName) {
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
     * @see NodeEntry#addNodeEntry(QName, String, int)
     */
    public NodeEntry addNodeEntry(QName nodeName, String uniqueID, int index) {
        return internalAddNodeEntry(nodeName, uniqueID, index, childNodeEntries());
    }

    /**
     * @inheritDoc
     * @see NodeEntry#addNewNodeEntry(QName, String, QName, QNodeDefinition)
     */
    public NodeState addNewNodeEntry(QName nodeName, String uniqueID, QName primaryNodeType, QNodeDefinition definition) throws ItemExistsException {
        NodeEntryImpl entry = internalAddNodeEntry(nodeName, uniqueID, Path.INDEX_UNDEFINED, childNodeEntries());
        NodeState state = factory.getItemStateFactory().createNewNodeState(entry, primaryNodeType, definition);
        entry.internalSetItemState(state);
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
    private NodeEntryImpl internalAddNodeEntry(QName nodeName, String uniqueID, int index, ChildNodeEntries childEntries) {
        NodeEntryImpl entry = new NodeEntryImpl(this, nodeName, uniqueID, factory);
        childEntries.add(entry, index);
        return entry;
    }

    /**
     * @see NodeEntry#moveNodeEntry(NodeState, QName, NodeEntry)
     */
    public NodeEntry moveNodeEntry(NodeState childState, QName newName, NodeEntry newParent) throws RepositoryException {
        NodeEntry oldEntry = childNodeEntries().remove(childState.getNodeEntry());
        if (oldEntry != null) {
            NodeEntryImpl movedEntry = (NodeEntryImpl) newParent.addNodeEntry(newName, oldEntry.getUniqueID(), Path.INDEX_UNDEFINED);
            movedEntry.internalSetItemState(childState);

            factory.notifyEntryMoved(oldEntry, movedEntry);
            return movedEntry;
        } else {
            // should never occur
            String msg = "Internal error. Attempt to move NodeEntry (" + childState + ") which is not connected to its parent.";
            log.error(msg);
            throw new RepositoryException(msg);
        }
    }

    /**
     * @inheritDoc
     * @see NodeEntry#hasPropertyEntry(QName)
     */
    public synchronized boolean hasPropertyEntry(QName propName) {
        PropertyEntry entry = (PropertyEntry) properties.get(propName);
        return EntryValidation.isValidPropertyEntry(entry);
    }

    /**
     * @inheritDoc
     * @see NodeEntry#getPropertyEntry(QName)
     */
    public synchronized PropertyEntry getPropertyEntry(QName propName) {
        PropertyEntry entry = (PropertyEntry) properties.get(propName);
        if (EntryValidation.isValidPropertyEntry(entry)) {
            return entry;
        } else {
            return null;
        }
    }

    /**
     * @inheritDoc
     * @see NodeEntry#getPropertyEntries()
     */
    public synchronized Iterator getPropertyEntries() {
        Collection props;
        ItemState state = internalGetItemState();
        if (state != null && state.getStatus() == Status.EXISTING_MODIFIED) {
            // filter out removed properties
            props = new ArrayList();
            // use array since upon validation the entry might be removed.
            Object[] arr = properties.values().toArray();
            for (int i = 0; i < arr.length; i++) {
                PropertyEntry propEntry = (PropertyEntry) arr[i];
                if (EntryValidation.isValidPropertyEntry(propEntry)) {
                    props.add(propEntry);
                }
            }
        } else {
            // no need to filter out properties, there are no removed properties
            props = properties.values();
        }
        return Collections.unmodifiableCollection(props).iterator();
    }

    /**
     * @inheritDoc
     * @see NodeEntry#addPropertyEntry(QName)
     */
    public PropertyEntry addPropertyEntry(QName propName) throws ItemExistsException {
        // TODO: check if correct, that check for existing prop can be omitted.
        PropertyEntry entry = PropertyEntryImpl.create(this, propName, factory);
        properties.put(propName, entry);

        // if property-name is jcr:uuid or jcr:mixin this affects this entry
        // and the attached nodeState.
        if (StateUtility.isUuidOrMixin(propName)) {
            notifyUUIDorMIXINModified(entry);
        }
        return entry;
    }

    /**
     * @inheritDoc
     * @see NodeEntry#addPropertyEntries(Collection)
     */
    public void addPropertyEntries(Collection propNames) throws ItemExistsException {
        Set diff = new HashSet();
        diff.addAll(properties.keySet());
        boolean containsExtra = diff.removeAll(propNames);

        // add all entries that are missing
        for (Iterator it = propNames.iterator(); it.hasNext();) {
            QName propName = (QName) it.next();
            if (!properties.containsKey(propName)) {
                addPropertyEntry(propName);
            }
        }

        // if this entry has not yet been resolved or if it is 'invalidated'
        // all property entries, that are not contained within the specified
        // collection of property names are removed from this NodeEntry.
        ItemState state = internalGetItemState();
        if (containsExtra && (state == null || state.getStatus() == Status.INVALIDATED)) {
            for (Iterator it = diff.iterator(); it.hasNext();) {
                QName propName = (QName) it.next();
                PropertyEntry pEntry = (PropertyEntry) properties.get(propName);
                pEntry.remove();
            }
        }
    }

    /**
     * @inheritDoc
     * @see NodeEntry#addNewPropertyEntry(QName, QPropertyDefinition)
     */
    public PropertyState addNewPropertyEntry(QName propName, QPropertyDefinition definition) throws ItemExistsException {
        // check for an existing property
        PropertyEntry existing = (PropertyEntry) properties.get(propName);
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
            } catch (ItemStateException e) {
                // entry probably does not exist on the persistent layer
                // -> therefore remove from properties map
                properties.remove(propName);
            }
        }

        // add the property entry
        PropertyEntry entry = PropertyEntryImpl.create(this, propName, factory);
        properties.put(propName, entry);

        PropertyState state = factory.getItemStateFactory().createNewPropertyState(entry, definition);
        ((PropertyEntryImpl) entry).internalSetItemState(state);

        return state;
    }

    /**
     * @param propName
     */
    PropertyEntry internalRemovePropertyEntry(QName propName) {
        PropertyEntry cpe = (PropertyEntry) properties.remove(propName);
        if (cpe == null) {
            cpe = (PropertyEntry) propertiesInAttic.remove(propName);
        }
        // special properties
        if (StateUtility.isUuidOrMixin(propName)) {
            notifyUUIDorMIXINRemoved(propName);
        }
        return cpe;
    }

    /**
     * @inheritDoc
     * @see NodeEntry#orderBefore(NodeEntry)
     */
    public boolean orderBefore(NodeEntry beforeEntry) {
        return parent.childNodeEntries().reorder(this, beforeEntry);
    }

    /**
     * @param childEvent
     * @see NodeEntry#refresh(Event)
     */
    public synchronized void refresh(Event childEvent) {
        boolean modified = false; // TODO: see todo below
        QName eventName = childEvent.getQPath().getNameElement().getName();
        switch (childEvent.getType()) {
            case Event.NODE_ADDED:
                int index = childEvent.getQPath().getNameElement().getNormalizedIndex();
                String uniqueChildID = (childEvent.getItemId().getPath() == null) ? childEvent.getItemId().getUniqueID() : null;
                // first check if no matching child entry exists.
                // TODO: TOBEFIXED for SNSs
                NodeEntry cne = (uniqueChildID != null) ? childNodeEntries().get(eventName, uniqueChildID) : childNodeEntries().get(eventName, index);
                if (cne == null) {
                    cne = internalAddNodeEntry(eventName, uniqueChildID, index, childNodeEntries());
                    modified = true;
                } else {
                    // child already exists -> deal with NEW entries, that were
                    // added by some other session.
                    // TODO: TOBEFIXED

                }
                break;

            case Event.PROPERTY_ADDED:
                // create a new property reference if it has not been
                // added by some earlier 'add' event
                if (!hasPropertyEntry(eventName)) {
                    try {
                        addPropertyEntry(eventName);
                        modified = true;
                    } catch (ItemExistsException e) {
                        log.warn("Internal error", e);
                        // TODO
                    }
                } else {
                    // TODO: TOBEFIXED deal with NEW entries
                }
                break;

            case Event.NODE_REMOVED:
            case Event.PROPERTY_REMOVED:
                HierarchyEntry child = getEntryForExternalEvent(childEvent.getItemId(), childEvent.getQPath());
                if (child != null) {
                    child.remove();
                    modified = true;
                } // else: child-Entry has not been loaded yet -> ignore
                break;

            case Event.PROPERTY_CHANGED:
                child = getEntryForExternalEvent(childEvent.getItemId(), childEvent.getQPath());
                if (child != null) {
                    // Reload data from server and try to merge them with the
                    // current session-state. if the latter is transiently
                    // modified and merge fails it must be marked STALE afterwards.
                    child.reload(false, false);
                    // special cases: jcr:uuid and jcr:mixinTypes affect the parent
                    // (i.e. this NodeEntry) since both props are protected
                    if (StateUtility.isUuidOrMixin(eventName)) {
                        notifyUUIDorMIXINModified((PropertyEntry) child);
                        modified = true;
                    }
                } else {
                    // prop-Entry has not been loaded yet -> add propEntry
                    try {
                        addPropertyEntry(eventName);
                        modified = true;
                    } catch (ItemExistsException e) {
                        log.warn("Internal error", e);
                        // TODO
                    }
                }
                break;
            default:
                // ILLEGAL
                throw new IllegalArgumentException("Illegal event type " + childEvent.getType() + " for NodeState.");
        }

        // TODO: check if status of THIS_state must be marked modified...
    }

    //------------------------------------------------------< HierarchyEntryImpl >---
    /**
     * @inheritDoc
     * @see HierarchyEntryImpl#doResolve()
     * <p/>
     * Returns a <code>NodeState</code>.
     */
    ItemState doResolve()
        throws NoSuchItemStateException, ItemStateException {
        return factory.getItemStateFactory().createNodeState(getId(), this);
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
        QName propName = propertyEntry.getQName();
        if (propertiesInAttic.containsKey(propName)) {
            properties.put(propName, propertiesInAttic.remove(propName));
        } // else: propEntry has never been moved to the attic (see 'addPropertyEntry')
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
    private HierarchyEntry getEntryForExternalEvent(ItemId eventId, Path eventPath) {
        QName childName = eventPath.getNameElement().getName();
        HierarchyEntry child = null;
        if (eventId.denotesNode()) {
            String uniqueChildID = (eventId.getPath() == null) ? eventId.getUniqueID() : null;
            if (uniqueChildID != null) {
                child = childNodeEntries().get(childName, uniqueID);
            }
            if (child == null) {
                child = childNodeEntries().get(childName, eventPath.getNameElement().getNormalizedIndex());
            }
        } else {
            // for external prop-removal the attic must be consulted first
            // in order not access a NEW prop shadowing a transiently removed
            // property with the same name.
            child = (HierarchyEntry) propertiesInAttic.get(childName);
            if (child == null) {
                child = (HierarchyEntry) properties.get(childName);
            }
        }
        if (child != null) {
            // a NEW hierarchyEntry may never be affected by an external
            // modification -> return null.
            ItemState state = ((HierarchyEntryImpl) child).internalGetItemState();
            if (state != null && state.getStatus() == Status.NEW) {
                return null;
            }
        }
        return child;
    }

    /**
     * Deals with modified jcr:uuid and jcr:mixinTypes property.
     * See {@link #notifyUUIDorMIXINRemoved(QName)}
     *
     * @param child
     */
    private void notifyUUIDorMIXINModified(PropertyEntry child) {
        try {
            if (QName.JCR_UUID.equals(child.getQName())) {
                PropertyState ps = child.getPropertyState();
                setUniqueID(ps.getValue().getString());
            } else if (QName.JCR_MIXINTYPES.equals(child.getQName())) {
                NodeState state = (NodeState) internalGetItemState();
                if (state != null) {
                    PropertyState ps = child.getPropertyState();
                    state.setMixinTypeNames(StateUtility.getMixinNames(ps));
                } // nodestate not yet loaded -> ignore change
            }
        } catch (ItemStateException e) {
            log.error("Internal Error", e);
        } catch (RepositoryException e) {
            log.error("Internal Error", e);
        }
    }

    /**
     * Deals with removed jcr:uuid and jcr:mixinTypes property.
     * See {@link #notifyUUIDorMIXINModified(PropertyEntry)}
     *
     * @param propName
     */
    private void notifyUUIDorMIXINRemoved(QName propName) {
        if (QName.JCR_UUID.equals(propName)) {
            setUniqueID(null);
        } else if (QName.JCR_MIXINTYPES.equals(propName)) {
            NodeState state = (NodeState) internalGetItemState();
            if (state != null) {
                state.setMixinTypeNames(QName.EMPTY_ARRAY);
            }
        }
    }

    /**
     *
     * @return
     */
    private ChildNodeEntries childNodeEntries() {
        if (childNodeEntries == null) {
            ItemState state = internalGetItemState();
            if (state != null) {
                if (state.getStatus() == Status.NEW) {
                    childNodeEntries = new ChildNodeEntries(this);
                } else if (StateUtility.isMovedState((NodeState) state)) {
                    // TODO: TOBEFIXED need to retrieve the original id. currently this will fail in case of SNS
                    // since, the index cannot be determined from the original parent any more
                    NodeId originalID = ((NodeState) state.getWorkspaceState()).getNodeId();
                    childNodeEntries = loadChildNodeEntries(originalID);
                } else {
                    childNodeEntries = loadChildNodeEntries(getId());
                }
            } else {
                childNodeEntries = loadChildNodeEntries(getId());
            }
        }
        return childNodeEntries;
    }

    private ChildNodeEntries loadChildNodeEntries(NodeId id) {
        ChildNodeEntries cnes = new ChildNodeEntries(this);
        try {
            Iterator it = factory.getItemStateFactory().getChildNodeInfos(id);
            while (it.hasNext()) {
                ChildInfo ci = (ChildInfo) it.next();
                internalAddNodeEntry(ci.getName(), ci.getUniqueID(), ci.getIndex(), cnes);
            }
        } catch (NoSuchItemStateException e) {
            log.error("Cannot retrieve child node entries.", e);
            // ignore (TODO correct?)
        } catch (ItemStateException e) {
            log.error("Cannot retrieve child node entries.", e);
            // ignore (TODO correct?)
        }
        return cnes;
    }

    /**
     * Returns an Iterator over all children entries, that currently are loaded
     * with this NodeEntry. NOTE, that if the childNodeEntries have not been
     * loaded yet, no attempt is made to do so.
     *
     * @param createNewList if true, both properties and childNodeEntries are
     * copied to new list, since recursive calls may call this node state to
     * inform the removal of a child entry.
     * @param includeAttic
     * @return
     */
    private Iterator getAllChildEntries(boolean createNewList, boolean includeAttic) {
        Iterator[] its;
        if (createNewList) {
            List props = new ArrayList(properties.values());
            List children = (childNodeEntries == null) ? Collections.EMPTY_LIST : new ArrayList(childNodeEntries);
            if (includeAttic) {
                List attic = new ArrayList(propertiesInAttic.values());
                its = new Iterator[] {attic.iterator(), props.iterator(), children.iterator()};
            } else {
                its = new Iterator[] {props.iterator(), children.iterator()};
            }
        } else {
            Iterator children = (childNodeEntries == null) ? Collections.EMPTY_LIST.iterator() : childNodeEntries.iterator();
            if (includeAttic) {
                its = new Iterator[] {propertiesInAttic.values().iterator(), properties.values().iterator(), children};
            } else {
                its = new Iterator[] {properties.values().iterator(), children};
            }
        }
        IteratorChain chain = new IteratorChain(its);
        return chain;
    }

    /**
     * Returns the index of the given <code>NodeEntry</code>.
     *
     * @param cne  the <code>NodeEntry</code> instance.
     * @return the index of the child node entry or <code>Path.INDEX_UNDEFINED</code>
     * if the given entry isn't a valid child of this <code>NodeEntry</code>.
     */
    private int getChildIndex(NodeEntry cne) {
        List sns = childNodeEntries().get(cne.getQName());
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
        return Path.INDEX_UNDEFINED;
    }
}
