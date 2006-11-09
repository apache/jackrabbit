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

import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.jcr2spi.state.entry.ChildNodeEntry;
import org.apache.jackrabbit.jcr2spi.state.entry.ChildPropertyEntry;
import org.apache.jackrabbit.jcr2spi.state.entry.PropertyReference;
import org.apache.jackrabbit.value.QValue;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;

/**
 * <code>NodeState</code> represents the state of a <code>Node</code>.
 */
public class NodeState extends ItemState {

    private static Logger log = LoggerFactory.getLogger(NodeState.class);

    /**
     * the name of this node's primary type
     */
    private QName nodeTypeName;

    /**
     * The UUID of this node state or <code>null</code> if this node state
     * cannot be identified with a uuid.
     */
    private String uuid;

    /**
     * The name of this node state
     */
    private QName name;

    /**
     * Definition of this node state
     */
    private QNodeDefinition definition;

    /**
     * the names of this node's mixin types
     */
    private QName[] mixinTypeNames = QName.EMPTY_ARRAY;

    /**
     * insertion-ordered collection of ChildNodeEntry objects
     * TODO: cache needs to be notified when a child node entry is traversed or NodeState is created
     */
    private ChildNodeEntries childNodeEntries = new ChildNodeEntries(this);

    /**
     * Map of properties. Key = {@link QName} of property. Value = {@link
     * PropertyReference}.
     */
    private HashMap properties = new HashMap();

    /**
     * Map of properties which are deleted and have been re-created as transient
     * property with the same name.
     */
    private HashMap propertiesInAttic = new HashMap();

    /**
     * NodeReferences for this node state.
     */
    private NodeReferences references;

    /**
     * Constructs a new node state that is not connected.
     *
     * @param name          the name of this NodeState
     * @param uuid          the uuid of this NodeState or <code>null</code> if
     *                      this node state cannot be identified with a UUID.
     * @param parent        the parent of this NodeState
     * @param nodeTypeName  node type of this node
     * @param definition
     * @param initialStatus the initial status of the node state object
     * @param isf           the item state factory responsible for creating node
     *                      states.
     * @param idFactory     the <code>IdFactory</code> to create new id
     */
    protected NodeState(QName name, String uuid, NodeState parent,
                        QName nodeTypeName, QNodeDefinition definition,
                        int initialStatus, ItemStateFactory isf,
                        IdFactory idFactory, boolean isWorkspaceState) {
        super(parent, initialStatus, isf, idFactory, isWorkspaceState);
        this.name = name;
        this.uuid = uuid;
        this.nodeTypeName = nodeTypeName;
        this.definition = definition;
    }

    /**
     * Constructs a new <code>NodeState</code> that is initially connected to an
     * overlayed state.
     *
     * @param overlayedState the backing node state being overlayed
     * @param parent         the parent of this NodeState
     * @param initialStatus  the initial status of the node state object
     * @param idFactory      the <code>IdFactory</code> to create new id
     *                       instance.
     */
    protected NodeState(NodeState overlayedState, NodeState parent,
                        int initialStatus, ItemStateFactory isf,
                        IdFactory idFactory) {
        super(overlayedState, parent, initialStatus, isf, idFactory);
        if (overlayedState != null) {
            synchronized (overlayedState) {
                NodeState wspState = (NodeState) overlayedState;
                name = wspState.name;
                uuid = wspState.uuid;
                nodeTypeName = wspState.nodeTypeName;
                definition = wspState.definition;

                init(wspState.getMixinTypeNames(), wspState.getChildNodeEntries(), wspState.getPropertyNames(), wspState.getNodeReferences());
            }
        }
    }

    /**
     *
     * @param mixinTypeNames
     * @param childEntries
     * @param propertyNames
     * @param references
     */
    void init(QName[] mixinTypeNames, Collection childEntries, Collection propertyNames, NodeReferences references) {
        if (mixinTypeNames != null) {
            this.mixinTypeNames = mixinTypeNames;
        }
        // re-create property references
        propertiesInAttic.clear();
        properties.clear();
        Iterator it = propertyNames.iterator();
        while (it.hasNext()) {
            QName propName = (QName) it.next();
            ChildPropertyEntry pe = PropertyReference.create(this, propName, isf, idFactory);
            properties.put(propName, pe);
        }
        // re-create child node entries
        childNodeEntries.removeAll();
        it = childEntries.iterator();
        while (it.hasNext()) {
            ChildNodeEntry cne = (ChildNodeEntry) it.next();
            childNodeEntries.add(cne.getName(), cne.getUUID(), cne.getIndex());
        }
        // set the node references
        this.references = references;
    }

    //----------------------------------------------------------< ItemState >---
    /**
     * Determines if this item state represents a node.
     *
     * @return always true
     * @see ItemState#isNode
     */
    public final boolean isNode() {
        return true;
    }

    /**
     * @see ItemState#getQName()
     */
    public final QName getQName() {
        return name;
    }

    /**
     * {@inheritDoc}
     * @see ItemState#getId()
     */
    public ItemId getId() {
        return getNodeId();
    }

    //----------------------------------------------------------< NodeState >---
    /**
     * Returns the id of this node state.
     *
     * @return the id of this node state.
     */
    public NodeId getNodeId() {
        NodeState parent = getParent();
        if (uuid != null) {
            return idFactory.createNodeId(uuid);
        } else if (parent != null) {
            // find this in parent child node entries
            for (Iterator it = parent.getChildNodeEntries(name).iterator(); it.hasNext(); ) {
                ChildNodeEntry cne = (ChildNodeEntry) it.next();
                try {
                    if (cne.getNodeState() == this) {
                        Path relPath = Path.create(cne.getName(), cne.getIndex());
                        return idFactory.createNodeId(parent.getNodeId(), relPath);
                    }
                } catch (ItemStateException e) {
                    log.warn("Unable to access child node entry: " + cne.getId());
                }
            }
        } else {
            // root node
            return idFactory.createNodeId((String) null, Path.ROOT);
        }
        // TODO: replace with ItemStateException instead of error.
        throw new InternalError("Unable to retrieve NodeId for NodeState");
    }

    /**
     * @return the UUID of this node state or <code>null</code> if this
     * node cannot be identified with a UUID.
     */
    public String getUUID() {
        return uuid;
    }

    /**
     * Modify the uuid of this state and make sure, that the parent state
     * contains a proper childNodeEntry for this state. If the given uuid is
     * not different from the uuid of this state, the method returns silently
     * without changing neither the parent nor this state.
     *
     * @param uuid
     */
    private void setUUID(String uuid) {
        String oldUUID = this.uuid;
        boolean mod = (oldUUID == null) ? uuid != null : !oldUUID.equals(uuid);
        if (mod) {
            this.uuid = uuid;
            if (getParent() != null) {
                getParent().childNodeEntries.replaceEntry(this);
            }
        }
    }

    /**
     * Returns the index of this node state.
     *
     * @return the index.
     */
    public int getIndex() throws ItemNotFoundException {
        if (parent == null) {
            // the root state may never have siblings
            return Path.INDEX_DEFAULT;
        }

        if (getDefinition().allowsSameNameSiblings()) {
            ChildNodeEntry entry = getParent().getChildNodeEntry(this);
            if (entry == null) {
                String msg = "Unable to retrieve index for: " + this;
                throw new ItemNotFoundException(msg);
            }
            return entry.getIndex();
        } else {
            return Path.INDEX_DEFAULT;
        }
    }

    /**
     * Returns the name of this node's node type.
     *
     * @return the name of this node's node type.
     */
    public QName getNodeTypeName() {
        return nodeTypeName;
    }

    /**
     * Returns the names of this node's mixin types.
     *
     * @return a set of the names of this node's mixin types.
     */
    public synchronized QName[] getMixinTypeNames() {
        return mixinTypeNames;
    }

    /**
     * Return all nodetype names that apply to this <code>NodeState</code>
     * including the primary nodetype and the mixins.
     *
     * @return
     */
    public synchronized QName[] getNodeTypeNames() {
        // mixin types
        QName[] mixinNames = getMixinTypeNames();
        QName[] types = new QName[mixinNames.length + 1];
        System.arraycopy(mixinNames, 0, types, 0, mixinNames.length);
        // primary type
        types[types.length - 1] = getNodeTypeName();
        return types;
    }

    /**
     * Returns the {@link QNodeDefinition definition} defined for this
     * node state. Note, that the definition has been set upon creation or
     * upon move.
     *
     * @return definition of this state
     */
    public QNodeDefinition getDefinition() {
        return definition;
    }


    /**
     * Return the <code>NodeReferences</code> present on this state or
     * <code>null</code>.
     *
     * @return references
     */
    NodeReferences getNodeReferences() {
        return references;
    }

    /**
     * Determines if there are any valid child node entries.
     *
     * @return <code>true</code> if there are child node entries,
     * <code>false</code> otherwise.
     */
    public boolean hasChildNodeEntries() {
        return containsValidChildNodeEntry(childNodeEntries);
    }

    /**
     * Determines if there is a valid <code>ChildNodeEntry</code> with the
     * specified <code>name</code>.
     *
     * @param name <code>QName</code> object specifying a node name
     * @return <code>true</code> if there is a <code>ChildNodeEntry</code> with
     *         the specified <code>name</code>.
     */
    public synchronized boolean hasChildNodeEntry(QName name) {
        return containsValidChildNodeEntry(childNodeEntries.get(name));
    }

    /**
     * Determines if there is a <code>ChildNodeEntry</code> with the
     * specified <code>name</code> and <code>index</code>.
     *
     * @param name  <code>QName</code> object specifying a node name
     * @param index 1-based index if there are same-name child node entries
     * @return <code>true</code> if there is a <code>ChildNodeEntry</code> with
     * the specified <code>name</code> and <code>index</code>.
     */
    public synchronized boolean hasChildNodeEntry(QName name, int index) {
        return childNodeEntries.get(name, index) != null;
    }

    /**
     * Returns the <code>ChildNodeEntry</code> with the specified name and index
     * or <code>null</code> if there's no matching entry.
     *
     * @param nodeName <code>QName</code> object specifying a node name
     * @param index    1-based index if there are same-name child node entries
     * @return the <code>ChildNodeEntry</code> with the specified name and index
     *         or <code>null</code> if there's no matching entry.
     */
    public synchronized ChildNodeEntry getChildNodeEntry(QName nodeName, int index) {
        return childNodeEntries.get(nodeName, index);
    }

    /**
     * Returns the <code>ChildNodeEntry</code> with the specified
     * <code>NodeId</code> or <code>null</code> if there's no matching
     * entry.
     *
     * @param nodeId the id of the child node state.
     * @return the <code>ChildNodeEntry</code> with the specified
     * <code>NodeId</code> or <code>null</code> if there's no matching entry.
     */
    public synchronized ChildNodeEntry getChildNodeEntry(NodeId nodeId) {
        String uuid = nodeId.getUUID();
        Path path = nodeId.getPath();
        if (uuid != null && path == null) {
            // retrieve child-entry by uuid
            return childNodeEntries.get(null, uuid);
        } else {
           // retrieve child-entry by name and index
            Path.PathElement nameElement = path.getNameElement();
            return childNodeEntries.get(nameElement.getName(), nameElement.getIndex());
        }
    }

    /**
     * Returns the <code>ChildNodeEntry</code> with the specified
     * <code>NodeState</code> or <code>null</code> if there's no matching
     * entry.
     *
     * @param nodeState the child node state.
     * @return the <code>ChildNodeEntry</code> with the specified
     * <code>NodeState</code> or <code>null</code> if there's no matching entry.
     */
    private synchronized ChildNodeEntry getChildNodeEntry(NodeState nodeState) {
        return childNodeEntries.get(nodeState);
    }

    /**
     * Returns a unmodifiable collection of <code>ChildNodeEntry</code> objects
     * denoting the child nodes of this node.
     *
     * @return collection of <code>ChildNodeEntry</code> objects
     */
    public synchronized Collection getChildNodeEntries() {
        // NOTE: 'childNodeEntries' are already unmodifiable
        return childNodeEntries;
    }

    /**
     * Returns a unmodifiable list of <code>ChildNodeEntry</code>s with the
     * specified name.
     *
     * @param nodeName name of the child node entries that should be returned
     * @return list of <code>ChildNodeEntry</code> objects
     */
    public synchronized List getChildNodeEntries(QName nodeName) {
        // NOTE: SubList retrieved from 'ChildNodeEntries' is already unmodifiable
        return childNodeEntries.get(nodeName);
    }

    /**
     * Determines if there is a property entry with the specified
     * <code>QName</code>.
     *
     * @param propName <code>QName</code> object specifying a property name
     * @return <code>true</code> if there is a property entry with the specified
     *         <code>QName</code>.
     */
    public synchronized boolean hasPropertyName(QName propName) {
        ChildPropertyEntry entry = (ChildPropertyEntry) properties.get(propName);
        if (entry == null) {
            return false;
        }
        if (entry.isAvailable()) {
            try {
                return entry.getPropertyState().isValid();
            } catch (ItemStateException e) {
                // probably deleted in the meantime
                return false;
            }
        } else {
            // then it must be valid // TODO check if this assumption is correct.
            return true;
        }
    }

    /**
     * Returns the names of this node's properties as a set of
     * <code>QNames</code> objects.
     *
     * @return set of <code>QNames</code> objects
     */
    public synchronized Collection getPropertyNames() {
        Collection names;
        if (getStatus() == Status.EXISTING_MODIFIED) {
            names = new ArrayList();
            for (Iterator it = getPropertyEntries().iterator(); it.hasNext(); ) {
                names.add(((ChildPropertyEntry) it.next()).getName());
            }
        } else {
            // this node state is unmodified, return all
            names = properties.keySet();
        }
        return Collections.unmodifiableCollection(names);
    }

    /**
     * Returns the complete collection of {@link ChildPropertyEntry}s.
     *
     * @return unmodifiable collection of <code>ChildPropertyEntry</code> objects
     */
    public synchronized Collection getPropertyEntries() {
        Collection props;
        if (getStatus() == Status.EXISTING_MODIFIED) {
            // filter out removed properties
            props = new ArrayList();
            for (Iterator it = properties.values().iterator(); it.hasNext(); ) {
                ChildPropertyEntry propEntry = (ChildPropertyEntry) it.next();
                if (propEntry.isAvailable()) {
                    try {
                        if (propEntry.getPropertyState().isValid()) {
                            props.add(propEntry);
                        }
                    } catch (ItemStateException e) {
                        // removed in the meantime -> ignore
                    }
                } else {
                    // never been accessed before, assume valid
                    props.add(propEntry);
                }
            }
        } else {
            // no need to filter out properties, there are no removed properties
            props = properties.values();
        }
        return Collections.unmodifiableCollection(props);
    }

    /*
     * Returns the property state with the given name.
     *
     * @param propertyName The name of the property state to return.
     * @throws NoSuchItemStateException If there is no (valid) property state
     * with the given name.
     * @throws ItemStateException If an error occurs while retrieving the
     * property state.
     */
    public synchronized PropertyState getPropertyState(QName propertyName)
        throws NoSuchItemStateException, ItemStateException {

        ChildPropertyEntry propEntry = (ChildPropertyEntry) properties.get(propertyName);
        if (propEntry == null) {
            throw new NoSuchItemStateException(idFactory.createPropertyId(getNodeId(), propertyName).toString());
        } else {
            PropertyState propState = propEntry.getPropertyState();
            if (propState.isValid()) {
                return propState;
            } else {
                throw new NoSuchItemStateException(idFactory.createPropertyId(getNodeId(), propertyName).toString());
            }
        }
    }

    /**
     *
     * @param propEntry
     */
    private void addPropertyEntry(ChildPropertyEntry propEntry) {
        QName propName = propEntry.getName();
        properties.put(propName, propEntry);
        try {
            if (isWorkspaceState() && isUuidOrMixin(propName)) {
                if (QName.JCR_UUID.equals(propName) && uuid == null) {
                    PropertyState ps = propEntry.getPropertyState();
                    setUUID(ps.getValue().getString());
                } else if (QName.JCR_MIXINTYPES.equals(propName) && (mixinTypeNames == null || mixinTypeNames.length == 0)) {
                    PropertyState ps = propEntry.getPropertyState();
                    mixinTypeNames = getMixinNames(ps);
                }
            }
        } catch (ItemStateException e) {
            log.error("Internal Error", e);
        } catch (RepositoryException e) {
            log.error("Internal Error", e);
        }
    }

    /**
     *
     * @param propName
     */
    private void removePropertyEntry(QName propName) {
        if (properties.remove(propName) != null) {
            if (isWorkspaceState()) {
                if (QName.JCR_UUID.equals(propName)) {
                    setUUID(null);
                } else if (QName.JCR_MIXINTYPES.equals(propName)) {
                    mixinTypeNames = QName.EMPTY_ARRAY;
                }
            }
        }
    }

    /**
     * TODO: find a better way to provide the index of a child node entry
     * Returns the index of the given <code>ChildNodeEntry</code> and with
     * <code>name</code>.
     *
     * @param cne  the <code>ChildNodeEntry</code> instance.
     * @return the index of the child node entry or <code>Path.INDEX_UNDEFINED</code>
     * if it is not found in this <code>NodeState</code>.
     */
    public int getChildNodeIndex(ChildNodeEntry cne) {
        List sns = childNodeEntries.get(cne.getName());
        // index is one based
        int index = 1;
        for (Iterator it = sns.iterator(); it.hasNext(); ) {
            ChildNodeEntry entry = (ChildNodeEntry) it.next();
            if (entry == cne) {
                return index;
            }
            // skip entries that belong to removed or invalidated states.
            // NOTE, that in this case the nodestate must be available from the cne.
            if (entry.isAvailable()) {
                try {
                    if (entry.getNodeState().isValid()) {
                        index++;
                    }
                } catch (ItemStateException e) {
                    // probably removed or stale
                }
            } else {
                // cne has not been resolved yet -> increase counter.
                // TODO: check if assuption is correct
                index++;
            }
        }
        // not found
        return Path.INDEX_UNDEFINED;
    }
    //--------------------------------------------------< Workspace - State >---
    /**
     *
     * @param event
     * @see ItemState#refresh(Event)
     */
    synchronized void refresh(Event event) {
        checkIsWorkspaceState();

        NodeId id = getNodeId();
        QName name = event.getQPath().getNameElement().getName();
        switch (event.getType()) {
            case Event.NODE_ADDED:
                int index = event.getQPath().getNameElement().getNormalizedIndex();
                NodeId evId = (NodeId) event.getItemId();
                String uuid = (evId.getPath() != null) ? null : evId.getUUID();

                // add new childNodeEntry if it has not been added by
                // some earlier 'add' event
                // TODO: TOBEFIXED for SNSs
                ChildNodeEntry cne = (uuid != null) ? childNodeEntries.get(name, uuid) : childNodeEntries.get(name, index);
                if (cne == null) {
                    cne = childNodeEntries.add(name, uuid, index);
                }
                // and let the transiently modified session state now, that
                // its workspace state has been touched.
                setStatus(Status.MODIFIED);
                break;

            case Event.PROPERTY_ADDED:
                // create a new property reference if it has not been
                // added by some earlier 'add' event
                if (!hasPropertyName(name)) {
                    ChildPropertyEntry re = PropertyReference.create(this, name, isf, idFactory);
                    addPropertyEntry(re);
                }
                // and let the transiently modified session state now, that
                // its workspace state has been touched.
                setStatus(Status.MODIFIED);
                break;

            case Event.NODE_REMOVED:
                if (id.equals(event.getParentId())) {
                    index = event.getQPath().getNameElement().getNormalizedIndex();
                    childNodeEntries.remove(name, index);
                    setStatus(Status.MODIFIED);
                } else if (id.equals(event.getItemId())) {
                    setStatus(Status.REMOVED);
                } else {
                    // ILLEGAL
                    throw new IllegalArgumentException("Illegal event type " + event.getType() + " for NodeState.");
                }
                break;

            case Event.PROPERTY_REMOVED:
                removePropertyEntry(name);
                setStatus(Status.MODIFIED);
                break;

            case Event.PROPERTY_CHANGED:
                if (QName.JCR_UUID.equals(name) || QName.JCR_MIXINTYPES.equals(name)) {
                    try {
                        PropertyState ps = getPropertyState(name);
                        adjustNodeState(this, new PropertyState[] {ps});
                    } catch (ItemStateException e) {
                        // should never occur.
                        log.error("Internal error while updating node state.", e);
                    }
                }
                break;
            default:
                // ILLEGAL
                throw new IllegalArgumentException("Illegal event type " + event.getType() + " for NodeState.");
        }
    }

    //----------------------------------------------------< Session - State >---
    /**
     * {@inheritDoc}
     * @see ItemState#refresh(ChangeLog)
     */
    Set refresh(ChangeLog changeLog) throws IllegalStateException {

        // remember parent states that have need to adjust their uuid/mixintypes
        // or that got a new child entry added or existing entries removed.
        Map modParents = new HashMap();
        Set processedIds = new HashSet();

        // process deleted states from the changelog
        for (Iterator it = changeLog.deletedStates(); it.hasNext();) {
            ItemState state = (ItemState) it.next();
            state.setStatus(Status.REMOVED);
            state.overlayedState.setStatus(Status.REMOVED);

            // adjust parent states unless the parent is removed as well
            NodeState parent = state.getParent();
            if (!changeLog.deletedStates.contains(parent)) {
                NodeState overlayedParent = (NodeState) parent.overlayedState;
                if (state.isNode()) {
                    overlayedParent.childNodeEntries.remove((NodeState)state.overlayedState);
                } else {
                    overlayedParent.removePropertyEntry(state.overlayedState.getQName());
                }
                modifiedParent(parent, state, modParents);
            }
            // don't remove processed state from changelog, but from event list
            // state on changelog is used for check if parent is deleted as well.
            processedIds.add(state.getId());
        }

        // process added states from the changelog. since the changlog maintains
        // LinkedHashSet for its entries, the iterator will not return a added
        // entry before its NEW parent.
        for (Iterator it = changeLog.addedStates(); it.hasNext();) {
            ItemState addedState = (ItemState) it.next();
            NodeState parent = addedState.getParent();
            // TODO: only retrieve overlayed state, if necessary
            try {
                // adjust parent child-entries
                NodeState overlayedParent = (NodeState) parent.overlayedState;
                QName addedName = addedState.getQName();
                if (addedState.isNode()) {
                    int index = parent.getChildNodeEntry((NodeState) addedState).getIndex();
                    ChildNodeEntry cne;
                    if (overlayedParent.hasChildNodeEntry(addedName, index)) {
                        cne = overlayedParent.getChildNodeEntry(addedName, index);
                    } else {
                        cne = overlayedParent.childNodeEntries.add(addedState.getQName(), null, index);
                    }
                    NodeState overlayed = cne.getNodeState();
                    if (overlayed.getUUID() != null) {
                        overlayedParent.childNodeEntries.replaceEntry(overlayed);
                    }
                    addedState.connect(overlayed);
                } else {
                    ChildPropertyEntry pe;
                    if (overlayedParent.hasPropertyName(addedName)) {
                        pe = (ChildPropertyEntry) overlayedParent.properties.get(addedName);
                    } else {
                        pe = PropertyReference.create(overlayedParent, addedName, overlayedParent.isf,  overlayedParent.idFactory);
                        overlayedParent.addPropertyEntry(pe);
                    }
                    addedState.connect(pe.getPropertyState());
                }

                // make sure the new state gets updated (e.g. uuid created by server)
                addedState.reset();
                // and mark the added-state existing
                addedState.setStatus(Status.EXISTING);
                // if parent is modified -> remember for final status reset
                if (parent.getStatus() == Status.EXISTING_MODIFIED) {
                    modifiedParent(parent, addedState, modParents);
                }

                it.remove();
                processedIds.add(addedState.getId());
            } catch (ItemStateException e) {
                log.error("Internal error.", e);
            }
        }

        for (Iterator it = changeLog.modifiedStates(); it.hasNext();) {
            ItemState modState = (ItemState) it.next();
            if (modState.isNode()) {
                NodeState modNodeState = (NodeState) modState;
                // handle moved nodes
                if (isMovedState(modNodeState)) {
                    // move overlayed state as well
                    NodeState newParent = (NodeState) modState.parent.overlayedState;
                    NodeState overlayed = (NodeState) modState.overlayedState;
                    ItemId removedId = overlayed.getId();
                    try {
                        overlayed.parent.moveEntry(newParent, overlayed, modNodeState.getQName(), modNodeState.getDefinition());
                    } catch (RepositoryException e) {
                        // should never occur
                        log.error("Internal error while moving childnode entries.", e);
                    }
                    // and mark the moved state existing
                    modNodeState.setStatus(Status.EXISTING);
                    it.remove();

                    processedIds.add(removedId);
                    processedIds.add(modNodeState.getId());
                } else {
                    modifiedParent((NodeState)modState, null, modParents);
                }
            } else {
                // push changes down to overlayed state
                int type = ((PropertyState) modState).getType();
                QValue[] values = ((PropertyState) modState).getValues();
                ((PropertyState) modState.overlayedState).init(type, values);

                modState.setStatus(Status.EXISTING);
                // if property state defines a modified jcr:mixinTypes
                // the parent is listed as modified state and needs to be
                // processed at the end.
                if (isUuidOrMixin(modState.getQName())) {
                    modifiedParent(modState.getParent(), modState, modParents);
                }
                it.remove();
                // remove the property-modification event from the set
                processedIds.add(modState.getId());
            }
        }

        /* process all parent states that are marked modified and eventually
           need their uuid or mixin-types being adjusted because that property
           has been added, modified or removed */
        for (Iterator it = modParents.keySet().iterator(); it.hasNext();) {
            NodeState parent = (NodeState) it.next();
            List l = (List) modParents.get(parent);
            adjustNodeState(parent, (PropertyState[]) l.toArray(new PropertyState[l.size()]));
        }

        /* finally check if all entries in the changelog have been processed
           and eventually force a reload in order not to have any states with
           wrong transient status floating around. */
        Iterator[] its = new Iterator[] {changeLog.addedStates(), changeLog.deletedStates(), changeLog.modifiedStates()};
        IteratorChain chain = new IteratorChain(its);
        while (chain.hasNext()) {
            ItemState state = (ItemState) chain.next();
            if (!(state.getStatus() == Status.EXISTING || state.getStatus() == Status.REMOVED)) {
                // error: state has not been processed
                // TODO: discard state and force reload of all data
            }
        }

        return processedIds;
    }

    /**
     * {@inheritDoc}
     * @see ItemState#reset()
     */
    synchronized void reset() {
        checkIsSessionState();

        if (overlayedState != null) {
            synchronized (overlayedState) {
                NodeState wspState = (NodeState) overlayedState;
                name = wspState.name;
                setUUID(wspState.uuid);
                nodeTypeName = wspState.nodeTypeName;
                definition = wspState.definition;

                mixinTypeNames = wspState.mixinTypeNames;

                // remove all entries in the attic
                propertiesInAttic.clear();

                // merge prop-names
                Collection wspPropNames = wspState.getPropertyNames();
                for (Iterator it = wspPropNames.iterator(); it.hasNext();) {
                    QName propName = (QName) it.next();
                    if (!hasPropertyName(propName)) {
                        addPropertyEntry(PropertyReference.create(this, propName, isf, idFactory));
                    }
                }
                for (Iterator it = properties.keySet().iterator(); it.hasNext();) {
                    // remove all prop-entries in the session state that are
                    // not present in the wsp-state.
                    if (!wspPropNames.contains(it.next())) {
                        it.remove();
                    }
                }

                // merge child node entries
                for (Iterator it = wspState.getChildNodeEntries().iterator(); it.hasNext();) {
                    ChildNodeEntry cne = (ChildNodeEntry) it.next();
                    int index = cne.getIndex();
                    if (!childNodeEntries.contains(cne.getName(), index, cne.getUUID())) {
                        childNodeEntries.add(cne.getName(), cne.getUUID(), index);
                    }
                }
                List toRemove = new ArrayList();
                for (Iterator it = getChildNodeEntries().iterator(); it.hasNext();) {
                    ChildNodeEntry cne = (ChildNodeEntry) it.next();
                    if (!wspState.childNodeEntries.contains(cne.getName(), cne.getIndex(), cne.getUUID())) {
                        toRemove.add(cne);
                    }
                }
                for (Iterator it = toRemove.iterator(); it.hasNext();) {
                    ChildNodeEntry cne = (ChildNodeEntry) it.next();
                    childNodeEntries.remove(cne.getName(), cne.getIndex());
                }
                // set the node references
                references = wspState.references;
            }
        }
    }

    /**
     * @inheritDoc
     * @see ItemState#remove()
     */
    void remove() throws ItemStateException {
        checkIsSessionState();

        if (!isValid()) {
            throw new ItemStateException("cannot remove an invalid NodeState");
        }
        // first remove all properties
        for (Iterator it = properties.values().iterator(); it.hasNext(); ) {
            ChildPropertyEntry cpe = ((ChildPropertyEntry) it.next());
            if (cpe.isAvailable()) {
                PropertyState pState = cpe.getPropertyState();
                if (pState.isValid()) {
                    pState.remove();
                } else {
                    // remove invalid property state from properties map
                    it.remove();
                }
            } else {
                // remove unresolved entry from properties map
                it.remove();
            }
        }
        // move all properties from attic back to properties map
        properties.putAll(propertiesInAttic);
        propertiesInAttic.clear();

        // then remove child node entries
        for (Iterator it = childNodeEntries.iterator(); it.hasNext(); ) {
            NodeState nodeState = ((ChildNodeEntry) it.next()).getNodeState();
            if (nodeState.isValid()) {
                nodeState.remove();
            } else {
                // already removed
            }
        }
        if (getStatus() == Status.EXISTING || getStatus() == Status.EXISTING_MODIFIED) {
            setStatus(Status.EXISTING_REMOVED);
        } else if (getStatus() == Status.NEW) {
            setStatus(Status.REMOVED);
        }
        // now inform parent
        getParent().childNodeStateRemoved(this);
    }

    /**
     * Reverts all property and child node states that belong to this
     * <code>NodeState</code> and finally reverts this <code>NodeState</code>.
     *
     * @inheritDoc
     * @see ItemState#revert(Set)
     */
    void revert(Set affectedItemStates) {
        // TODO: TOBEFIXED. revert must include an update with the latest state present on the server
        checkIsSessionState();

        // copy to new list, when a property is reverted it may call this node
        // state to remove itself from properties.
        List props = new ArrayList(properties.values());
        for (Iterator it = props.iterator(); it.hasNext(); ) {
            ChildPropertyEntry entry = (ChildPropertyEntry) it.next();
            if (entry.isAvailable()) {
                try {
                    PropertyState propState = entry.getPropertyState();
                    propState.revert(affectedItemStates);
                } catch (ItemStateException e) {
                    // should not happen because PropertyReference is resolved
                    log.warn("Unable to get PropertyState from resolved PropertyReference");
                }
            } else {
                // not touched or accessed before
            }
        }

        // revert property states in attic
        props.clear();
        props.addAll(propertiesInAttic.values());
        for (Iterator it = props.iterator(); it.hasNext(); ) {
            PropertyReference ref = (PropertyReference) it.next();
            try {
                PropertyState propState = ref.getPropertyState();
                propState.revert(affectedItemStates);
            } catch (ItemStateException e) {
                // probably stale destroyed property
                // cleaned up when propertiesInAttic is cleared
            }
        }
        propertiesInAttic.clear();

        // now revert child node states
        List children = new ArrayList(childNodeEntries);
        for (Iterator it = children.iterator(); it.hasNext(); ) {
            ChildNodeEntry entry = (ChildNodeEntry) it.next();
            if (entry.isAvailable()) {
                try {
                    NodeState nodeState = entry.getNodeState();
                    nodeState.revert(affectedItemStates);
                } catch (ItemStateException e) {
                    // should not happen because ChildNodeReference is resolved
                    log.warn("Unable to get NodeState from resolved ChildNodeReference");
                }
            } else {
                // not touched or accessed before
            }
        }

        // now revert this node state
        switch (getStatus()) {
            case Status.EXISTING:
                // nothing to do
                break;
            case Status.EXISTING_MODIFIED:
            case Status.EXISTING_REMOVED:
            case Status.STALE_MODIFIED:
                // revert state from overlayed
                reset();
                setStatus(Status.EXISTING);
                affectedItemStates.add(this);
                break;
            case Status.NEW:
                // set removed
                setStatus(Status.REMOVED);
                // remove from parent
                getParent().childNodeStateRemoved(this);
                affectedItemStates.add(this);
                break;
            case Status.REMOVED:
                // shouldn't happen actually, because a 'removed' state is not
                // accessible anymore
                log.warn("trying to revert an already removed node state");
                getParent().childNodeStateRemoved(this);
                break;
            case Status.STALE_DESTROYED:
                // overlayed state does not exist anymore
                getParent().childNodeStateRemoved(this);
                affectedItemStates.add(this);
                break;
        }
    }

    /**
     * @inheritDoc
     * @see ItemState#collectTransientStates(Collection)
     */
    void collectTransientStates(Collection transientStates) {
        checkIsSessionState();

        switch (getStatus()) {
            case Status.EXISTING_MODIFIED:
            case Status.EXISTING_REMOVED:
            case Status.NEW:
            case Status.STALE_DESTROYED:
            case Status.STALE_MODIFIED:
                transientStates.add(this);
        }
        // call available property states
        for (Iterator it = properties.values().iterator(); it.hasNext(); ) {
            ChildPropertyEntry entry = (ChildPropertyEntry) it.next();
            if (entry.isAvailable()) {
                try {
                    entry.getPropertyState().collectTransientStates(transientStates);
                } catch (ItemStateException e) {
                    // should not happen because ref is available
                }
            }
        }
        // add all properties in attic
        transientStates.addAll(propertiesInAttic.values());
        // call available child node states
        for (Iterator it = childNodeEntries.iterator(); it.hasNext(); ) {
            ChildNodeEntry cne = (ChildNodeEntry) it.next();
            if (cne.isAvailable()) {
                try {
                    cne.getNodeState().collectTransientStates(transientStates);
                } catch (ItemStateException e) {
                    // should not happen because cne is available
                }
            }
        }
    }

    /**
     * Adds a child node state to this node state.
     *
     * @param child the node state to add.
     * @throws IllegalArgumentException if <code>this</code> is not the parent
     *                                  of <code>child</code>.
     */
    synchronized void addChildNodeState(NodeState child) {
        checkIsSessionState();
        if (child.getParent() != this) {
            throw new IllegalArgumentException("This NodeState is not the parent of child");
        }
        childNodeEntries.add(child);
        markModified();
    }

    /**
     * Notifies this node state that a child node state has been removed.
     *
     * @param childState the node state that has been removed.
     * @throws IllegalArgumentException if <code>this</code> is not the parent
     *                                  of <code>nodeState</code>.
     */
    private synchronized void childNodeStateRemoved(NodeState childState) {
        checkIsSessionState();

        if (childState.getParent() != this) {
            throw new IllegalArgumentException("This NodeState is not the parent of nodeState");
        }
        // if nodeState does not exist anymore remove its child node entry
        if (childState.getStatus() == Status.REMOVED) {
            childNodeEntries.remove(childState);
        }
        markModified();
    }

    /**
     * Adds a property state to this node state.
     *
     * @param propState the property state to add.
     * @throws ItemExistsException      if <code>this</code> node state already
     *                                  contains a property state with the same
     *                                  name as <code>propState</code>.
     * @throws IllegalArgumentException if <code>this</code> is not the parent
     *                                  of <code>propState</code>.
     */
    synchronized void addPropertyState(PropertyState propState) throws ItemExistsException {
        checkIsSessionState();
        if (propState.getParent() != this) {
            throw new IllegalArgumentException("This NodeState is not the parent of propState");
        }
        QName propertyName = propState.getQName();
        // check for an existing property
        PropertyReference ref = (PropertyReference) properties.get(propertyName);
        if (ref != null) {
            PropertyState existingState = null;
            try {
                existingState = ref.getPropertyState();
            } catch (ItemStateException e) {
                // probably does not exist anymore, remove from properties map
                removePropertyEntry(propertyName);
            }
            if (existingState != null) {
                if (existingState.getStatus() == Status.EXISTING_REMOVED) {
                    // move to attic
                    propertiesInAttic.put(propertyName, ref);
                } else {
                    throw new ItemExistsException(propertyName.toString());
                }
            }
        }
        addPropertyEntry(PropertyReference.create(propState, isf, idFactory));
        markModified();
    }

    /**
     * Notifies this node state that a property state has been removed.
     *
     * @param propState the property state that has been removed.
     * @throws IllegalArgumentException if <code>this</code> is not the parent
     *                                  of <code>propState</code>.
     */
    synchronized void propertyStateRemoved(PropertyState propState) {
        checkIsSessionState();
        if (propState.getParent() != this) {
            throw new IllegalArgumentException("This NodeState is not the parent of propState");
        }
        // remove property state from map of properties if it does not exist
        // anymore, otherwise leave the property state in the map
        if (propState.getStatus() == Status.REMOVED) {
            removePropertyEntry(propState.getQName());
        }
        markModified();
    }
    /**
     * Reorders the child node <code>insertNode</code> before the child node
     * <code>beforeNode</code>.
     *
     * @param insertNode the child node to reorder.
     * @param beforeNode the child node where to insert the node before. If
     *                   <code>null</code> the child node <code>insertNode</code>
     *                   is moved to the end of the child node entries.
     * @throws NoSuchItemStateException if <code>insertNode</code> or
     *                                  <code>beforeNode</code> is not a child
     *                                  node of this <code>NodeState</code>.
     */
    synchronized void reorderChildNodeEntries(NodeState insertNode, NodeState beforeNode)
        throws NoSuchItemStateException {
        checkIsSessionState();

        childNodeEntries.reorder(insertNode, beforeNode);
        // mark this state as modified
        markModified();
    }

    /**
     * Moves a <code>ChildNodeEntry</code> to a new parent. If the new parent
     * is this <code>NodeState</code>, the child state is renamed and moved
     * to the end of the child entries collection.
     *
     * @param newParent
     * @param childState
     * @param newName
     * @param newName <code>QName</code> object specifying the entry's new name
     * @throws RepositoryException if the given child state is not a child
     * of this node state.
     */
    synchronized void moveChildNodeEntry(NodeState newParent, NodeState childState, QName newName, QNodeDefinition newDefinition)
        throws RepositoryException {
        checkIsSessionState();

        moveEntry(newParent, childState, newName, newDefinition);
        // mark both this and newParent modified
        markModified();
        childState.markModified();
        newParent.markModified();
    }

    private void moveEntry(NodeState newParent, NodeState childState, QName newName, QNodeDefinition newDefinition) throws RepositoryException {
        ChildNodeEntry oldEntry = childNodeEntries.remove(childState);
        if (oldEntry != null) {
            childState.name = newName;
            // re-parent target node
            childState.parent = newParent;
            // set definition according to new definition required by the new parent
            childState.definition = newDefinition;
            // add child node entry to new parent
            newParent.childNodeEntries.add(childState);
        } else {
            throw new RepositoryException("Unexpected error: Child state to be moved does not exist.");
        }
    }

    //---------------------------------------------------------< diff methods >

    /**
     * Returns a set of <code>QName</code>s denoting those properties that
     * do not exist in the overlayed node state but have been added to
     * <i>this</i> node state.
     *
     * @return set of <code>QName</code>s denoting the properties that have
     *         been added.
     */
    public synchronized Set getAddedPropertyNames() {
        checkIsSessionState();

        if (getStatus() == Status.NEW) {
            // state is new -> all
            return Collections.unmodifiableSet(properties.keySet());
        }

        NodeState other = (NodeState) getWorkspaceState();
        HashSet set = new HashSet(properties.keySet());
        set.removeAll(other.properties.keySet());
        return set;
    }

    /**
     * Returns a collection of child node entries that do not exist in the
     * overlayed node state but have been added to <i>this</i> node state.
     *
     * @return collection of added child node entries
     */
    public synchronized Collection getAddedChildNodeEntries() {
        checkIsSessionState();

        if (getStatus() == Status.NEW) {
            // state is new -> all child nodes are new too
            return childNodeEntries;
        }

        List added = new ArrayList();
        for (Iterator it = childNodeEntries.iterator(); it.hasNext(); ) {
            ChildNodeEntry cne = (ChildNodeEntry) it.next();
            try {
                if (cne.getNodeState().getStatus() == Status.NEW) {
                    added.add(cne);
                }
            } catch (ItemStateException e) {
                log.warn("error retrieving child node state: " + e.getMessage());
            }
        }
        return added;
    }

    /**
     * Returns a set of <code>QName</code>s denoting those properties that
     * exist in the overlayed node state but have been removed from
     * <i>this</i> node state.
     *
     * @return set of <code>QName</code>s denoting the properties that have
     *         been removed.
     */
    public synchronized Set getRemovedPropertyNames() {
        checkIsSessionState();

        if (getStatus() == Status.NEW) {
            return Collections.EMPTY_SET;
        }

        NodeState other = (NodeState) getWorkspaceState();
        HashSet set = new HashSet(other.properties.keySet());
        set.removeAll(properties.keySet());
        return set;
    }

    /**
     * Returns a collection of child node entries, that exist in the overlayed
     * node state but have been removed from <i>this</i> node state.
     *
     * @return collection of removed child node entries
     */
    public synchronized Collection getRemovedChildNodeEntries() {
        checkIsSessionState();

        if (getStatus() == Status.NEW) {
            return Collections.EMPTY_LIST;
        }

        List removed = new ArrayList();
        for (Iterator it = childNodeEntries.iterator(); it.hasNext(); ) {
            ChildNodeEntry cne = (ChildNodeEntry) it.next();
            try {
                if (cne.getNodeState().getStatus() == Status.EXISTING_REMOVED) {
                    removed.add(cne);
                }
            } catch (ItemStateException e) {
                log.warn("error retrieving child node state: " + e.getMessage());
            }
        }
        return removed;
    }

    //-------------------------------< internal >-------------------------------
    /**
     * Returns <code>true</code> if the collection of child node
     * <code>entries</code> contains at least one valid <code>ChildNodeEntry</code>.
     *
     * @param entries the collection to check.
     * @return <code>true</code> if one of the entries is valid; otherwise
     *         <code>false</code>.
     */
    private static boolean containsValidChildNodeEntry(Collection entries) {
        for (Iterator it = entries.iterator(); it.hasNext(); ) {
            ChildNodeEntry cne = (ChildNodeEntry) it.next();
            if (cne.isAvailable()) {
                try {
                    if (cne.getNodeState().isValid()) {
                        return true;
                    }
                } catch (ItemStateException e) {
                    // probably removed in the meantime, check next
                }
            } else {
                // then it has never been accessed and must exist
                // TODO: check if this assumption is correct
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param ps
     * @return
     * @throws RepositoryException
     */
    private static QName[] getMixinNames(PropertyState ps) throws RepositoryException {
        assert QName.JCR_MIXINTYPES.equals(ps.getQName());

        QValue[] values = ps.getValues();
        QName[] newMixins = new QName[values.length];
        for (int i = 0; i < values.length; i++) {
            newMixins[i] = QName.valueOf(values[i].getString());
        }
        return newMixins;
    }

    private static boolean isUuidOrMixin(QName propName) {
        return QName.JCR_UUID.equals(propName) || QName.JCR_MIXINTYPES.equals(propName);
    }

    private static void modifiedParent(NodeState parent, ItemState child, Map modParents) {
        List l;
        if (modParents.containsKey(parent)) {
            l = (List) modParents.get(parent);
        } else {
            l = new ArrayList(2);
            modParents.put(parent, l);
        }
        if (child != null && !child.isNode() && isUuidOrMixin(child.getQName())) {
            l.add(child);
        }
    }

    /**
     *
     * @param parent
     * @param props
     */
    private static void adjustNodeState(NodeState parent, PropertyState[] props) {
        NodeState overlayed = (parent.isWorkspaceState()) ? parent : (NodeState) parent.overlayedState;
        NodeState sState = (parent.isWorkspaceState()) ? (NodeState) overlayed.getSessionState() : parent;

        if (overlayed != null) {
            for (int i = 0; i < props.length; i++) {
                try {
                    if (QName.JCR_UUID.equals(props[i].getQName())) {
                        String uuid = (props[i].getStatus() == Status.REMOVED) ? null : props[i].getValue().getString();
                        sState.setUUID(uuid);
                        overlayed.setUUID(uuid);
                    } else if (QName.JCR_MIXINTYPES.equals(props[i].getQName())) {
                        QName[] mixins = (props[i].getStatus() == Status.REMOVED) ? QName.EMPTY_ARRAY : getMixinNames(props[i]);

                        sState.mixinTypeNames = mixins;
                        overlayed.mixinTypeNames = mixins;
                    } // else: ignore.
                } catch (RepositoryException e) {
                    // should never occur.
                    log.error("Internal error while updating node state.", e);
                }
            }

            // make sure all other modifications on the overlayed state are
            // reflected on the session-state.
            sState.reset();
            // make sure, the session-state gets its status reset to Existing.
            if (sState.getStatus() == Status.EXISTING_MODIFIED) {
                sState.setStatus(Status.EXISTING);
            }
        } else {
            // should never occur.
            log.warn("Error while adjusting nodestate: Overlayed state is missing.");
        }
    }

    private static boolean isMovedState(NodeState modState) {
        return modState.overlayedState.parent != modState.parent.overlayedState;
    }
}
