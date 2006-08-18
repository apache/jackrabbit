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

import org.apache.commons.collections.list.AbstractLinkedList;
import org.apache.commons.collections.iterators.UnmodifiableIterator;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.jcr2spi.nodetype.EffectiveNodeType;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.ItemExistsException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.AbstractList;
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;

/**
 * <code>NodeState</code> represents the state of a <code>Node</code>.
 */
public class NodeState extends ItemState {

    private static Logger log = LoggerFactory.getLogger(NodeState.class);

    /**
     * A current element Path instance.
     */
    private static final Path CURRENT_PATH;

    static {
        try {
            Path.PathBuilder builder = new Path.PathBuilder();
            builder.addFirst(Path.CURRENT_ELEMENT);
            CURRENT_PATH = builder.getPath();
        } catch (MalformedPathException e) {
            // path is always valid
            throw new InternalError("unable to create path from '.'");
        }
    }

    /**
     * the name of this node's primary type
     */
    private QName nodeTypeName;

    /**
     * the names of this node's mixin types
     */
    private QName[] mixinTypeNames = new QName[0];

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
    private QNodeDefinition def;

    /**
     * insertion-ordered collection of ChildNodeEntry objects
     * TODO: cache needs to be notified when a child node entry is traversed or NodeState is created
     */
    private ChildNodeEntries childNodeEntries = new ChildNodeEntries();

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
     * The <code>ItemStateFactory</code> which is used to create new
     * <code>ItemState</code> instances.
     */
    private final ItemStateFactory isf;

    /**
     * Constructs a new node state that is not connected.
     *
     * @param name          the name of this NodeState
     * @param uuid          the uuid of this NodeState or <code>null</code> if
     *                      this node state cannot be identified with a UUID.
     * @param parent        the parent of this NodeState
     * @param nodeTypeName  node type of this node
     * @param initialStatus the initial status of the node state object
     * @param isTransient   flag indicating whether this state is transient or
     *                      not.
     * @param isf           the item state factory responsible for creating node
     *                      states.
     * @param idFactory     the <code>IdFactory</code> to create new id
     *                      instance.
     */
    protected NodeState(QName name, String uuid, NodeState parent,
                        QName nodeTypeName, int initialStatus, boolean isTransient,
                        ItemStateFactory isf, IdFactory idFactory) {
        super(parent, initialStatus, isTransient, idFactory);
        this.name = name;
        this.uuid = uuid;
        this.nodeTypeName = nodeTypeName;
        this.isf = isf;
    }

    /**
     * Constructs a new <code>NodeState</code> that is initially connected to an
     * overlayed state.
     *
     * @param overlayedState the backing node state being overlayed
     * @param parent         the parent of this NodeState
     * @param initialStatus  the initial status of the node state object
     * @param isTransient    flag indicating whether this state is transient or
     *                       not
     * @param idFactory      the <code>IdFactory</code> to create new id
     *                       instance.
     */
    protected NodeState(NodeState overlayedState, NodeState parent,
                        int initialStatus, boolean isTransient,
                        ItemStateFactory isf, IdFactory idFactory) {
        super(overlayedState, parent, initialStatus, isTransient, idFactory);
        pull();
        this.isf = isf;
    }

    /**
     * {@inheritDoc}
     */
    protected synchronized void copy(ItemState state) {
        synchronized (state) {
            NodeState nodeState = (NodeState) state;
            name = nodeState.name;
            uuid = nodeState.uuid;
            //parent = nodeState.parent; // TODO: parent from wrong ism layer
            nodeTypeName = nodeState.nodeTypeName;
            mixinTypeNames = nodeState.mixinTypeNames;
            def = nodeState.def;
            // re-create property references
            propertiesInAttic.clear();
            properties.clear(); // TODO: any more cleanup work to do? try some kind of merging?
            Iterator it = nodeState.getPropertyNames().iterator();
            while (it.hasNext()) {
                addPropertyName((QName) it.next());
            }
            // re-create child node entries
            childNodeEntries.clear(); // TODO: any mre cleanup work to do? try some kind of merging?
            it = nodeState.getChildNodeEntries().iterator();
            while (it.hasNext()) {
                ChildNodeEntry cne = (ChildNodeEntry) it.next();
                childNodeEntries.add(cne.getName(), cne.getUUID());
            }
        }
    }

    //--------------------< public READ methods and package private Setters >---

    /**
     * @return the name of this node state.
     */
    public final QName getName() {
        return name;
    }

    /**
     * @return the UUID of this node state or <code>null</code> if this
     * node cannot be identified with a UUID.
     */
    public final String getUUID() {
        return uuid;
    }

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
     * Sets the the parent <code>NodeState</code>.
     *
     * @param parent the parent <code>NodeState</code> or <code>null</code>
     * if either this node state should represent the root node or this node
     * state should be 'free floating', i.e. detached from the repository's
     * hierarchy.
     */
    private void setParent(NodeState parent) {
        this.parent = parent;
    }

    /**
     * {@inheritDoc}
     */
    public ItemId getId() {
        return getNodeId();
    }

    /**
     * Returns the id of this node state.
     *
     * @return the id of this node state.
     */
    public NodeId getNodeId() {
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
            return idFactory.createNodeId((String) null, CURRENT_PATH);
        }
        // TODO: replace with ItemStateException instead of error.
        throw new InternalError("Unable to retrieve NodeId for NodeState");
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
     * Set the node type name. Needed for deserialization and should therefore
     * not change the internal status.
     *
     * @param nodeTypeName node type name
     */
    synchronized void setNodeTypeName(QName nodeTypeName) {
        this.nodeTypeName = nodeTypeName;
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
     * Sets the names of this node's mixin types.
     *
     * @param mixinTypeNames set of names of mixin types
     */
    synchronized void setMixinTypeNames(QName[] mixinTypeNames) {
        if (mixinTypeNames != null) {
            this.mixinTypeNames = mixinTypeNames;
        } else {
            this.mixinTypeNames = new QName[0];
        }
    }

    /**
     * Return all nodetype names that apply to this <code>NodeState</code>
     * including the primary nodetype and the mixins.
     *
     * @return
     */
    public synchronized QName[] getNodeTypeNames() {
        // mixin types
        QName[] types = new QName[mixinTypeNames.length + 1];
        System.arraycopy(mixinTypeNames, 0, types, 0, mixinTypeNames.length);
        // primary type
        types[types.length - 1] = getNodeTypeName();
        return types;
    }

    /**
     * Returns the {@link QNodeDefinition definition} defined for this
     * node state or <code>null</code> if the definition has not been
     * set before (i.e. the corresponding item has not been accessed before).
     *
     * @return definition of this state
     * @see #getDefinition(NodeTypeRegistry) for the corresponding method
     * that never returns <code>null</code>.
     */
    public QNodeDefinition getDefinition() {
        return def;
    }

    /**
     * Returns the definition applicable to this node state. Since the definition
     * is not defined upon state creation this state may have to retrieve
     * the definition from the given <code>NodeTypeRegistry</code> first.
     *
     * @param ntRegistry
     * @return the definition of this state
     * @see #getDefinition()
     */
    public QNodeDefinition getDefinition(NodeTypeRegistry ntRegistry)
        throws RepositoryException {
        // make sure the state has the definition set now
        if (def == null) {
            NodeState parentState = getParent();
            if (parentState == null) {
                // special case for root state
                def = ntRegistry.getRootNodeDef();
            } else {
                try {
                    EffectiveNodeType ent = ntRegistry.getEffectiveNodeType(parentState.getNodeTypeNames());
                    def = ent.getApplicableNodeDefinition(getName(), getNodeTypeName());
                } catch (NodeTypeConflictException e) {
                    String msg = "internal error: failed to build effective node type.";
                    log.debug(msg);
                    throw new RepositoryException(msg, e);
                }
            }
        }
        return def;
    }

    /**
     * Sets the id of the definition applicable to this node state.
     *
     * @param def the definition
     */
    void setDefinition(QNodeDefinition def) {
        this.def = def;
    }

    /**
     * Determines if there are any child node entries.
     *
     * @return <code>true</code> if there are child node entries,
     *         <code>false</code> otherwise.
     */
    public boolean hasChildNodeEntries() {
        return !childNodeEntries.isEmpty();
    }

    /**
     * Determines if there is a <code>ChildNodeEntry</code> with the
     * specified <code>name</code>.
     *
     * @param name <code>QName</code> object specifying a node name
     * @return <code>true</code> if there is a <code>ChildNodeEntry</code> with
     *         the specified <code>name</code>.
     */
    public synchronized boolean hasChildNodeEntry(QName name) {
        return !childNodeEntries.get(name).isEmpty();
    }

    /**
     * Determines if there is a <code>ChildNodeEntry</code> with the
     * specified <code>name</code> and <code>index</code>.
     *
     * @param name  <code>QName</code> object specifying a node name
     * @param index 1-based index if there are same-name child node entries
     * @return <code>true</code> if there is a <code>ChildNodeEntry</code> with
     *         the specified <code>name</code> and <code>index</code>.
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
     * <code>NodeState</code> or <code>null</code> if there's no matching
     * entry.
     *
     * @param nodeState the child node state.
     * @return the <code>ChildNodeEntry</code> with the specified
     *         <code>NodeState</code> or <code>null</code> if there's no
     *         matching entry.
     * @see #addChildNodeEntry
     */
    public synchronized ChildNodeEntry getChildNodeEntry(NodeState nodeState) {
        return childNodeEntries.get(nodeState);
    }

    /**
     * Returns a unmodifiable collection of <code>ChildNodeEntry</code> objects
     * denoting the child nodes of this node.
     *
     * @return collection of <code>ChildNodeEntry</code> objects
     * @see #addChildNodeEntry
     */
    public synchronized Collection getChildNodeEntries() {
        // NOTE: List representation of 'ChildNodeEntries' is already unmodifiable
        return childNodeEntries;
    }

    /**
     * Returns a unmodifiable list of <code>ChildNodeEntry</code>s with the
     * specified name.
     *
     * @param nodeName name of the child node entries that should be returned
     * @return list of <code>ChildNodeEntry</code> objects
     * @see #addChildNodeEntry
     */
    public synchronized List getChildNodeEntries(QName nodeName) {
        // NOTE: SubList retrieved from 'ChildNodeEntries' is already unmodifiable
        return childNodeEntries.get(nodeName);
    }

    /**
     * Adds a new <code>ChildNodeEntry</code>.
     *
     * @param nodeName <code>QName</code> object specifying the name of the new
     *                 entry.
     * @param uuid     the uuid the new entry is refering to or
     *                 <code>null</code> if the child node state cannot be
     *                 identified with a uuid.
     * @return the newly added <code>ChildNodeEntry</code>
     */
    synchronized ChildNodeEntry addChildNodeEntry(QName nodeName,
                                                  String uuid) {
        return childNodeEntries.add(nodeName, uuid);
    }

    /**
     * TODO: move this method to a node state implementation which contains all transient related methods?
     *
     * Adds a child node state to this node state.
     *
     * @param child the node state to add.
     * @param uuid  the uuid of the child node state or <code>null</code> if
     *              <code>child</code> cannot be identified with a uuid.
     * @throws IllegalArgumentException if <code>this</code> is not the parent
     *                                  of <code>child</code>.
     */
    synchronized void addChildNodeState(NodeState child, String uuid) {
        if (child.getParent() != this) {
            throw new IllegalArgumentException("This NodeState is not the parent of child");
        }
        ChildNodeEntry cne = ChildNodeReference.create(child, isf, idFactory);
        childNodeEntries.add(cne);
        markModified();
    }

    /**
     * Renames this node to <code>newName</code>.
     *
     * @param newName the new name for this node state.
     * @throws IllegalStateException if this is the root node.
     */
    private synchronized void rename(QName newName) {
        if (parent == null) {
            throw new IllegalStateException("root node cannot be renamed");
        }
        name = newName;
    }

    /**
     * Notifies this node state that a child node state has been removed.
     *
     * @param nodeState the node state that has been removed.
     * @throws IllegalArgumentException if <code>this</code> is not the parent
     *                                  of <code>nodeState</code>.
     */
    private synchronized void childNodeStateRemoved(NodeState nodeState) {
        if (nodeState.getParent() != this) {
            throw new IllegalArgumentException("This NodeState is not the parent of nodeState");
        }
        // if nodeState does not exist anymore remove its child node entry
        if (nodeState.getStatus() == STATUS_REMOVED) {
            List entries = getChildNodeEntries(nodeState.getName());
            for (Iterator it = entries.iterator(); it.hasNext(); ) {
                ChildNodeEntry cne = (ChildNodeEntry) it.next();
                try {
                    if (cne.getNodeState() == nodeState) {
                        childNodeEntries.remove(cne);
                        break;
                    }
                } catch (ItemStateException e) {
                    // does not exist anymore? TODO: better error handling
                    log.warn("child node entry does not exist anymore", e);
                }
            }
        }
        markModified();
    }

    /**
     * @inheritDoc
     * @see ItemState#remove()
     */
    public void remove() throws ItemStateException {
        if (!isValid()) {
            throw new ItemStateException("cannot remove an invalid NodeState");
        }
        // first remove all properties
        for (Iterator it = properties.values().iterator(); it.hasNext(); ) {
            PropertyState propState = ((ChildPropertyEntry) it.next()).getPropertyState();
            if (propState.isValid()) {
                propState.remove();
            } else {
                // remove invalid property state from properties map
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
        if (status == STATUS_EXISTING || status == STATUS_EXISTING_MODIFIED) {
            setStatus(STATUS_EXISTING_REMOVED);
        } else if (status == STATUS_NEW) {
            setStatus(STATUS_REMOVED);
        }
        // now inform parent
        parent.childNodeStateRemoved(this);
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
        return properties.containsKey(propName);
    }

    /**
     * Returns the names of this node's properties as a set of
     * <code>QNames</code> objects.
     *
     * @return set of <code>QNames</code> objects
     * @see #addPropertyName
     */
    public synchronized Collection getPropertyNames() {
        return Collections.unmodifiableSet(properties.keySet());
    }

    /**
     * Returns the complete collection of {@link ChildPropertyEntry}s.
     *
     * @return unmodifiable collection of <code>ChildPropertyEntry</code> objects
     * @see #addPropertyName
     */
    public synchronized Collection getPropertyEntries() {
        return Collections.unmodifiableCollection(properties.values());
    }

    /**
     * Adds a property name entry. This method will not create a property!
     *
     * @param propName <code>QName</code> object specifying the property name
     */
    synchronized void addPropertyName(QName propName) {
        properties.put(propName, new PropertyReference(this, propName, isf, idFactory));
    }

    /**
     * TODO: move this method to a node state implementation which contains all transient related methods?
     *
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
                properties.remove(propertyName);
            }
            if (existingState != null) {
                if (existingState.getStatus() == STATUS_EXISTING_REMOVED) {
                    // move to attic
                    propertiesInAttic.put(propertyName, ref);
                } else {
                    throw new ItemExistsException(propertyName.toString());
                }
            }
        }
        properties.put(propertyName, new PropertyReference(propState, isf, idFactory));
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
        if (propState.getParent() != this) {
            throw new IllegalArgumentException("This NodeState is not the parent of propState");
        }
        // remove property state from map of properties if it does not exist
        // anymore, otherwise leave the property state in the map
        if (propState.getStatus() == STATUS_REMOVED) {
            properties.remove(propState.getQName());
        }
        markModified();
    }

    /*
     * Returns the property state with the given name.
     *
     * @param propertyName the name of the property state to return.
     * @throws NoSuchItemStateException if there is no property state with the
     *                                  given name.
     * @throws ItemStateException       if an error occurs while retrieving the
     *                                  property state.
     */
    public synchronized PropertyState getPropertyState(QName propertyName)
            throws NoSuchItemStateException, ItemStateException {
        PropertyReference propRef = (PropertyReference) properties.get(propertyName);
        if (propRef == null) {
            throw new NoSuchItemStateException(idFactory.createPropertyId(getNodeId(), propertyName).toString());
        }
        return propRef.getPropertyState();
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
        if (!hasOverlayedState()) {
            return Collections.unmodifiableSet(properties.keySet());
        }

        NodeState other = (NodeState) getOverlayedState();
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
        if (!hasOverlayedState()) {
            return childNodeEntries;
        }

        List added = new ArrayList();
        for (Iterator it = childNodeEntries.iterator(); it.hasNext(); ) {
            ChildNodeEntry cne = (ChildNodeEntry) it.next();
            try {
                if (cne.getNodeState().getStatus() == STATUS_NEW) {
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
        if (!hasOverlayedState()) {
            return Collections.EMPTY_SET;
        }

        NodeState other = (NodeState) getOverlayedState();
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
        if (!hasOverlayedState()) {
            return Collections.EMPTY_LIST;
        }

        List removed = new ArrayList();
        for (Iterator it = childNodeEntries.iterator(); it.hasNext(); ) {
            ChildNodeEntry cne = (ChildNodeEntry) it.next();
            try {
                if (cne.getNodeState().getStatus() == STATUS_EXISTING_REMOVED) {
                    removed.add(cne);
                }
            } catch (ItemStateException e) {
                log.warn("error retrieving child node state: " + e.getMessage());
            }
        }
        return removed;
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
        childNodeEntries.reorder(insertNode, beforeNode);
        // mark this state as modified
        markModified();
    }

    /**
     * Renames a new <code>ChildNodeEntry</code>.
     *
     * @param newParent
     * @param childState
     * @param newName
     * @param newName <code>QName</code> object specifying the entry's new name
     * @throws RepositoryException if the given child state is not a child
     * of this node state.
     */
    synchronized void moveChildNodeEntry(NodeState newParent, NodeState childState, QName newName)
        throws RepositoryException {
        ChildNodeEntry oldEntry = childNodeEntries.remove(childState);
        if (oldEntry != null) {
            childState.rename(newName);
            // re-parent target node
            childState.setParent(newParent);
            // add child node entry to new parent
            newParent.childNodeEntries.add(childState);
        } else {
            throw new RepositoryException("Unexpected error: Child state to be renamed does not exist.");
        }
    }

    /**
     * Return the <code>NodeReferences</code> present on this state or
     * <code>null</code>.
     *
     * @return references
     */
    NodeReferences getNodeReferences() {
        if (hasOverlayedState()) {
            return ((NodeState)getOverlayedState()).references;
        } else {
            return references;
        }
    }

    /**
     * Set the <code>NodeReferences</code> for this state.
     *
     * @param references
     */
    void setNodeReferences(NodeReferences references) {
        if (isTransient()) {
            // TODO: check again
            throw new UnsupportedOperationException("Cannot set references to a transient node state.");
        }
        this.references = references;
    }

    /**
     * TODO: find a better way to provide the index of a child node entry
     * Returns the index of the given <code>ChildNodeEntry</code> and with
     * <code>name</code>.
     *
     * @param name the name of the child node.
     * @param cne  the <code>ChildNodeEntry</code> instance.
     * @return the index of the child node entry or <code>0</code> if it is not
     *         found in this <code>NodeState</code>.
     */
    int getChildNodeIndex(QName name, ChildNodeEntry cne) {
        List sns = childNodeEntries.get(name);
        return sns.indexOf(cne) + 1;
    }

    //------------------------------------------------------< inner classes >---

    /**
     * <code>ChildNodeEntries</code> represents an insertion-ordered
     * collection of <code>ChildNodeEntry</code>s that also maintains
     * the index values of same-name siblings on insertion and removal.
     * <p/>
     * <code>ChildNodeEntries</code> also provides an unmodifiable
     * <code>Collection</code> view.
     */
    private class ChildNodeEntries implements Collection {

        /**
         * Linked list of {@link ChildNodeEntry} instances.
         */
        private final LinkedEntries entries = new LinkedEntries();

        /**
         * map used for lookup by name
         * (key=name, value=either a single {@link AbstractLinkedList.Node} or a
         * list of {@link AbstractLinkedList.Node}s which are sns entries)
         */
        private final Map nameMap = new HashMap();

        /**
         * Returns the <code>ChildNodeEntry</code> for the given
         * <code>nodeState</code>.
         *
         * @param nodeState the node state.
         * @return the <code>ChildNodeEntry</code> or <code>null</code> if there
         *         is no <code>ChildNodeEntry</code> for <code>nodeState</code>.
         */
        ChildNodeEntry get(NodeState nodeState) {
            Object o = nameMap.get(nodeState.getName());
            if (o == null) {
                // no matching child node entry
                return null;
            }
            if (o instanceof List) {
                // has same name sibling
                for (Iterator it = ((List) o).iterator(); it.hasNext(); ) {
                    LinkedEntries.LinkNode n = (LinkedEntries.LinkNode) it.next();
                    ChildNodeEntry cne = n.getChildNodeEntry();
                    // only check available child node entries
                    try {
                        if (cne.isAvailable() && cne.getNodeState() == nodeState) {
                            return cne;
                        }
                    } catch (ItemStateException e) {
                        log.warn("error retrieving a child node state", e);
                    }
                }
            } else {
                // single child node with this name
                ChildNodeEntry cne = ((LinkedEntries.LinkNode) o).getChildNodeEntry();
                try {
                    if (cne.isAvailable() && cne.getNodeState() == nodeState) {
                        return cne;
                    }
                } catch (ItemStateException e) {
                    log.warn("error retrieving a child node state", e);
                }
            }
            // not found
            return null;
        }

        /**
         * Returns a <code>List</code> of <code>ChildNodeEntry</code>s for the
         * given <code>nodeName</code>.
         *
         * @param nodeName the child node name.
         * @return same name sibling nodes with the given <code>nodeName</code>.
         */
        List get(QName nodeName) {
            Object obj = nameMap.get(nodeName);
            if (obj == null) {
                return Collections.EMPTY_LIST;
            }
            if (obj instanceof List) {
                final List sns = (List) obj;
                // map entry is a list of siblings
                return Collections.unmodifiableList(new AbstractList() {

                    public Object get(int index) {
                        return ((LinkedEntries.LinkNode) sns.get(index)).getChildNodeEntry();
                    }

                    public int size() {
                        return sns.size();
                    }

                    public Iterator iterator() {
                        return new Iterator() {

                            private Iterator iter = sns.iterator();

                            public void remove() {
                                throw new UnsupportedOperationException("remove");
                            }

                            public boolean hasNext() {
                                return iter.hasNext();
                            }

                            public Object next() {
                                return ((LinkedEntries.LinkNode) iter.next()).getChildNodeEntry();
                            }
                        };
                    }
                });
            } else {
                // map entry is a single child node entry
                return Collections.singletonList(
                        ((LinkedEntries.LinkNode) obj).getChildNodeEntry());
            }
        }

        /**
         * Returns the <code>ChildNodeEntry</code> with the given
         * <code>nodeName</code> and <code>index</code>.
         *
         * @param nodeName name of the child node entry.
         * @param index    the index of the child node entry.
         * @return the <code>ChildNodeEntry</code> or <code>null</code> if there
         *         is no such <code>ChildNodeEntry</code>.
         */
        ChildNodeEntry get(QName nodeName, int index) {
            if (index < Path.INDEX_DEFAULT) {
                throw new IllegalArgumentException("index is 1-based");
            }

            Object obj = nameMap.get(nodeName);
            if (obj == null) {
                return null;
            }
            if (obj instanceof List) {
                // map entry is a list of siblings
                List siblings = (List) obj;
                if (index <= siblings.size()) {
                    return ((LinkedEntries.LinkNode) siblings.get(index - 1)).getChildNodeEntry();
                }
            } else {
                // map entry is a single child node entry
                if (index == Path.INDEX_DEFAULT) {
                    return ((LinkedEntries.LinkNode) obj).getChildNodeEntry();
                }
            }
            return null;
        }

        /**
         * Adds a <code>ChildNodeEntry</code> for a child node with the given
         * name and an optional <code>uuid</code>.
         *
         * @param nodeName the name of the child node.
         * @param uuid     the UUID of the child node if it can be identified
         *                 with a UUID; otherwise <code>null</code>.
         * @return the created ChildNodeEntry.
         */
        ChildNodeEntry add(QName nodeName, String uuid) {
            List siblings = null;
            Object obj = nameMap.get(nodeName);
            if (obj != null) {
                if (obj instanceof List) {
                    // map entry is a list of siblings
                    siblings = (List) obj;
                } else {
                    // map entry is a single child node entry,
                    // convert to siblings list
                    siblings = new ArrayList();
                    siblings.add(obj);
                    nameMap.put(nodeName, siblings);
                }
            }

            ChildNodeEntry entry = createChildNodeEntry(nodeName, uuid);
            LinkedEntries.LinkNode ln = entries.add(entry);

            if (siblings != null) {
                siblings.add(ln);
            } else {
                nameMap.put(nodeName, ln);
            }

            return entry;
        }

        /**
         * Adds a <code>ChildNodeEntry</code> to the end of the list.
         *
         * @param cne the <code>ChildNodeEntry</code> to add.
         */
        void add(ChildNodeEntry cne) {
            QName nodeName = cne.getName();
            List siblings = null;
            Object obj = nameMap.get(nodeName);
            if (obj != null) {
                if (obj instanceof List) {
                    // map entry is a list of siblings
                    siblings = (ArrayList) obj;
                } else {
                    // map entry is a single child node entry,
                    // convert to siblings list
                    siblings = new ArrayList();
                    siblings.add(obj);
                    nameMap.put(nodeName, siblings);
                }
            }

            LinkedEntries.LinkNode ln = entries.add(cne);

            if (siblings != null) {
                siblings.add(ln);
            } else {
                nameMap.put(nodeName, ln);
            }
        }

        /**
         * Adds a <code>childNode</code> to the end of the list.
         *
         * @param childNode the <code>NodeState</code> to add.
         * @return the <code>ChildNodeEntry</code> which was created for
         *         <code>childNode</code>.
         */
        ChildNodeEntry add(NodeState childNode) {
            ChildNodeEntry cne = ChildNodeReference.create(childNode, isf, idFactory);
            add(cne);
            return cne;
        }

        /**
         * Appends a list of <code>ChildNodeEntry</code>s to this list.
         *
         * @param entriesList the list of <code>ChildNodeEntry</code>s to add.
         */
        void addAll(List entriesList) {
            Iterator iter = entriesList.iterator();
            while (iter.hasNext()) {
                ChildNodeEntry entry = (ChildNodeEntry) iter.next();
                // delegate to add(QName, String) to maintain consistency
                add(entry.getName(), entry.getUUID());
            }
        }

        /**
         * Removes the child node entry with the given <code>nodeName</code> and
         * <code>index</code>.
         *
         * @param nodeName the name of the child node entry to remove.
         * @param index    the index of the child node entry to remove.
         * @return the removed <code>ChildNodeEntry</code> or <code>null</code>
         *         if there is no matching <code>ChildNodeEntry</code>.
         */
        public ChildNodeEntry remove(QName nodeName, int index) {
            if (index < Path.INDEX_DEFAULT) {
                throw new IllegalArgumentException("index is 1-based");
            }

            Object obj = nameMap.get(nodeName);
            if (obj == null) {
                return null;
            }

            if (obj instanceof LinkedEntries.LinkNode) {
                // map entry is a single child node entry
                if (index != Path.INDEX_DEFAULT) {
                    return null;
                }
                LinkedEntries.LinkNode ln = (LinkedEntries.LinkNode) obj;
                nameMap.remove(nodeName);
                // remove LinkNode from entries
                ln.remove();
                return ln.getChildNodeEntry();
            }

            // map entry is a list of siblings
            List siblings = (List) obj;
            if (index > siblings.size()) {
                return null;
            }

            // remove from siblings list
            LinkedEntries.LinkNode ln = (LinkedEntries.LinkNode) siblings.remove(index - 1);
            ChildNodeEntry removedEntry = ln.getChildNodeEntry();
            // remove from ordered entries
            ln.remove();

            // clean up name lookup map if necessary
            if (siblings.size() == 0) {
                // no more entries with that name left:
                // remove from name lookup map as well
                nameMap.remove(nodeName);
            } else if (siblings.size() == 1) {
                // just one entry with that name left:
                // discard siblings list and update name lookup map accordingly
                nameMap.put(nodeName, siblings.get(0));
            }

            // we're done
            return removedEntry;
        }

        /**
         * Removes the child node entry refering to the node state.
         *
         * @param nodeState the node state whose entry is to be removed.
         * @return the removed entry or <code>null</code> if there is no such entry.
         */
        ChildNodeEntry remove(NodeState nodeState) {
            ChildNodeEntry entry = null;
            for (Iterator it = get(nodeState.getName()).iterator(); it.hasNext(); ) {
                ChildNodeEntry tmp = (ChildNodeEntry) it.next();
                try {
                    if (tmp.isAvailable() && tmp.getNodeState() == nodeState) {
                        entry = tmp;
                        break;
                    }
                } catch (ItemStateException e) {
                    log.warn("error accessing child node state: " + e.getMessage());
                }
            }
            if (entry != null) {
                return remove(entry.getName(), entry.getIndex());
            }
            return entry;
        }

        /**
         * Removes the given child node entry.
         *
         * @param entry entry to be removed.
         * @return the removed entry or <code>null</code> if there is no such entry.
         */
        public ChildNodeEntry remove(ChildNodeEntry entry) {
            return remove(entry.getName(), entry.getIndex());
        }

        /**
         * Removes all child node entries
         */
        public void removeAll() {
            nameMap.clear();
            entries.clear();
        }

        /**
         * Reorders an existing <code>NodeState</code> before another
         * <code>NodeState</code>. If <code>beforeNode</code> is
         * <code>null</code> <code>insertNode</code> is moved to the end of the
         * child node entries.
         *
         * @param insertNode the node state to move.
         * @param beforeNode the node state where <code>insertNode</code> is
         *                   reordered to.
         * @throws NoSuchItemStateException if <code>insertNode</code> or
         *                                  <code>beforeNode</code> does not
         *                                  have a <code>ChildNodeEntry</code>
         *                                  in this <code>ChildNodeEntries</code>.
         */
        public void reorder(NodeState insertNode, NodeState beforeNode)
                throws NoSuchItemStateException {
            // the link node to move
            LinkedEntries.LinkNode insertLN;
            // the link node where insertLN is ordered before
            LinkedEntries.LinkNode beforeLN = null;

            Object insertObj = nameMap.get(insertNode.getName());
            if (insertObj == null) {
                // no matching child node entry
                throw new NoSuchItemStateException(insertNode.getName().toString());
            }
            insertLN = getLinkNode(insertObj, insertNode);

            // now retrieve LinkNode for beforeNode
            if (beforeNode != null) {
                Object beforeObj = nameMap.get(beforeNode.getName());
                if (beforeObj == null) {
                    throw new NoSuchItemStateException(beforeNode.getName().toString());
                }
                beforeLN = getLinkNode(beforeObj, beforeNode);
            }

            if (insertObj instanceof List) {
                // adapt name lookup lists
                List insertList = (List) insertObj;
                if (beforeNode == null) {
                    // simply move to end of list
                    insertList.remove(insertLN);
                    insertList.add(insertLN);
                } else {
                    // move based on position of beforeLN

                    // count our same name siblings until we reach beforeLN
                    int snsCount = 0;
                    QName insertName = insertNode.getName();
                    for (Iterator it = entries.linkNodeIterator(); it.hasNext(); ) {
                        LinkedEntries.LinkNode ln = (LinkedEntries.LinkNode) it.next();
                        if (ln == beforeLN) {
                            insertList.remove(insertLN);
                            insertList.add(snsCount, insertLN);
                            break;
                        } else if (ln == insertLN) {
                            // do not increment snsCount for node to reorder
                        } else if (ln.getChildNodeEntry().getName().equals(insertName)) {
                            snsCount++;
                        }
                    }
                }
            } else {
                // no same name siblings -> nothing to do.
            }

            // reorder in linked list
            entries.reorderNode(insertLN, beforeLN);
        }

        /**
         * Creates a <code>ChildNodeEntry</code> instance based on
         * <code>nodeName</code> and an optional <code>uuid</code>.
         *
         * @param nodeName the name of the child node.
         * @param uuid     the UUID of the child node. If <code>null</code> the
         *                 child node cannot be identified with a UUID.
         * @return the created child node entry.
         */
        private ChildNodeEntry createChildNodeEntry(QName nodeName, String uuid) {
            if (uuid == null) {
                return new PathElementReference(NodeState.this, nodeName,
                        isf, idFactory);
            } else {
                return new UUIDReference(NodeState.this,
                        idFactory.createNodeId(uuid), isf, nodeName);
            }
        }

        /**
         * Returns the matching <code>LinkNode</code> from a list or a single
         * <code>LinkNode</code>.
         *
         * @param listOrLinkNode List of <code>LinkNode</code>s or a single
         *                       <code>LinkNode</code>.
         * @param nodeState      the <code>NodeState</code> which is the value
         *                       of on of the <code>LinkNode</code>s.
         * @return the matching <code>LinkNode</code>.
         * @throws NoSuchItemStateException if none of the <code>LinkNode</code>s
         *                                  matches.
         */
        private LinkedEntries.LinkNode getLinkNode(Object listOrLinkNode,
                                                   NodeState nodeState)
                throws NoSuchItemStateException {
            if (listOrLinkNode instanceof List) {
                // has same name sibling
                for (Iterator it = ((List) listOrLinkNode).iterator(); it.hasNext();) {
                    LinkedEntries.LinkNode n = (LinkedEntries.LinkNode) it.next();
                    ChildNodeEntry cne = n.getChildNodeEntry();
                    // only check available child node entries
                    try {
                        if (cne.isAvailable() && cne.getNodeState() == nodeState) {
                            return n;
                        }
                    } catch (ItemStateException e) {
                        log.warn("error retrieving a child node state", e);
                    }
                }
            } else {
                // single child node with this name
                ChildNodeEntry cne = ((LinkedEntries.LinkNode) listOrLinkNode).getChildNodeEntry();
                try {
                    if (cne.isAvailable() && cne.getNodeState() == nodeState) {
                        return (LinkedEntries.LinkNode) listOrLinkNode;
                    }
                } catch (ItemStateException e) {
                    log.warn("error retrieving a child node state", e);
                }
            }
            throw new NoSuchItemStateException(nodeState.getName().toString());
        }

        //--------------------------------------< unmodifiable Collection view >

        public boolean contains(Object o) {
            if (o instanceof ChildNodeEntry) {
                // narrow down to same name sibling nodes and check list
                return get(((ChildNodeEntry) o).getName()).contains(o);
            } else {
                return false;
            }
        }

        public boolean containsAll(Collection c) {
            Iterator iter = c.iterator();
            while (iter.hasNext()) {
                if (!contains(iter.next())) {
                    return false;
                }
            }
            return true;
        }

        public boolean isEmpty() {
            return entries.isEmpty();
        }

        public Iterator iterator() {
            return UnmodifiableIterator.decorate(entries.iterator());
        }

        public int size() {
            return entries.size();
        }

        public Object[] toArray() {
            ChildNodeEntry[] array = new ChildNodeEntry[size()];
            return toArray(array);
        }

        public Object[] toArray(Object[] a) {
            if (!a.getClass().getComponentType().isAssignableFrom(ChildNodeEntry.class)) {
                throw new ArrayStoreException();
            }
            if (a.length < size()) {
                a = new ChildNodeEntry[size()];
            }
            Iterator iter = entries.iterator();
            int i = 0;
            while (iter.hasNext()) {
                a[i++] = iter.next();
            }
            while (i < a.length) {
                a[i++] = null;
            }
            return a;
        }

        public boolean add(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean removeAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public boolean retainAll(Collection c) {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * An implementation of a linked list which provides access to the internal
     * LinkNode which links the entries of the list.
     */
    private static final class LinkedEntries extends AbstractLinkedList {

        LinkedEntries() {
            super();
            init();
        }

        /**
         * Adds a child node entry to this list.
         *
         * @param cne the child node entry to add.
         * @return the LinkNode which refers to the added <code>ChildNodeEntry</code>.
         */
        LinkNode add(ChildNodeEntry cne) {
            LinkNode ln = (LinkNode) createNode(cne);
            addNode(ln, header);
            return ln;
        }

        /**
         * Reorders an existing <code>LinkNode</code> before another existing
         * <code>LinkNode</code>. If <code>before</code> is <code>null</code>
         * the <code>insert</code> node is moved to the end of the list.
         *
         * @param insert the node to reorder.
         * @param before the node where to reorder node <code>insert</code>.
         */
        void reorderNode(LinkNode insert, LinkNode before) {
            removeNode(insert);
            if (before == null) {
                addNode(insert, header);
            } else {
                addNode(insert, before);
            }
        }

        /**
         * Create a new <code>LinkNode</code> for a given {@link ChildNodeEntry}
         * <code>value</code>.
         *
         * @param value a child node entry.
         * @return a wrapping {@link LinkedEntries.LinkNode}.
         */
        protected Node createNode(Object value) {
            return new LinkNode(value);
        }

        /**
         * @return a new <code>LinkNode</code>.
         */
        protected Node createHeaderNode() {
            return new LinkNode();
        }

        /**
         * Returns an iterator over all
         * @return
         */
        Iterator linkNodeIterator() {
            return new Iterator() {

                private LinkNode next = ((LinkNode) header).getNextLinkNode();

                private int expectedModCount = modCount;

                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }

                public boolean hasNext() {
                    if (expectedModCount != modCount) {
                        throw new ConcurrentModificationException();
                    }
                    return next != header;
                }

                public Object next() {
                    if (expectedModCount != modCount) {
                        throw new ConcurrentModificationException();
                    }
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    LinkNode n = next;
                    next = next.getNextLinkNode();
                    return n;
                }
            };
        }

        //-----------------------------------------------------------------------

        /**
         * Extends the <code>AbstractLinkedList.Node</code>.
         */
        private final class LinkNode extends AbstractLinkedList.Node {

            protected LinkNode() {
                super();
            }

            protected LinkNode(Object value) {
                super(value);
            }

            /**
             * @return the wrapped <code>ChildNodeEntry</code>.
             */
            public ChildNodeEntry getChildNodeEntry() {
                return (ChildNodeEntry) super.getValue();
            }

            /**
             * Removes this <code>LinkNode</code> from the linked list.
             */
            public void remove() {
                removeNode(this);
            }

            /**
             * @return the next LinkNode.
             */
            public LinkNode getNextLinkNode() {
                return (LinkNode) super.getNextNode();
            }
        }
    }
}
