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

import org.apache.commons.collections.map.LinkedMap;
import org.apache.jackrabbit.util.WeakIdentityCollection;
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
     * Listeners (weak references)
     */
    private final transient Collection listeners = new WeakIdentityCollection(3);

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
            parent = nodeState.parent; // TODO: parent from wrong ism layer
            nodeTypeName = nodeState.nodeTypeName;
            mixinTypeNames = nodeState.mixinTypeNames;
            def = nodeState.def;
            // re-create property references
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
                    ChildNodeEntry cne = parentState.getChildNodeEntry(getNodeId());
                    EffectiveNodeType ent = ntRegistry.getEffectiveNodeType(parentState.getNodeTypeNames());
                    def = ent.getApplicableNodeDefinition(cne.getName(), getNodeTypeName());
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
     * Returns the <code>ChildNodeEntry</code> with the specified <code>NodeId</code> or
     * <code>null</code> if there's no matching entry.
     *
     * @param id the id of the child node
     * @return the <code>ChildNodeEntry</code> with the specified <code>NodeId</code> or
     *         <code>null</code> if there's no matching entry.
     * @see #addChildNodeEntry
     */
    public synchronized ChildNodeEntry getChildNodeEntry(NodeId id) {
        return childNodeEntries.get(id);
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
        ChildNodeEntry entry = childNodeEntries.add(nodeName, uuid);
        notifyNodeAdded(entry);
        return entry;
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
        ChildNodeEntry cne;
        if (uuid != null) {
            cne = new UUIDReference(child, isf);
        } else {
            cne = new PathElementReference(child, isf, idFactory);
        }
        childNodeEntries.add(cne);
        markModified();
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
                // already removed
            }
        }
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
        if (properties.containsKey(propertyName)) {
            throw new ItemExistsException(propertyName.toString());
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
     *
     * @param insertNodeId
     * @param beforeNodeId
     */
    synchronized void reorderChildNodeEntries(NodeId insertNodeId, NodeId beforeNodeId)
        throws NoSuchItemStateException {
        // validate existance of child node entries even if this has been
        // checked within NodeImpl.
        if (childNodeEntries.get(insertNodeId) == null) {
            throw new NoSuchItemStateException("No such child node entry: " + insertNodeId);
        }
        if (beforeNodeId != null && childNodeEntries.get(insertNodeId) == null) {
            throw new NoSuchItemStateException("No such child node entry: " + beforeNodeId);
        }

        // TODO: check again. Reorder with SPI-Id
        ArrayList nodeEntries = new ArrayList(childNodeEntries);
        int srcInd = -1, destInd = -1;
        for (int i = 0; i < nodeEntries.size(); i++) {
            ChildNodeEntry entry = (ChildNodeEntry) nodeEntries.get(i);
            if (srcInd == -1) {
                if (entry.getId().equals(insertNodeId)) {
                    srcInd = i;
                }
            }
            if (destInd == -1 && beforeNodeId != null) {
                if (entry.getId().equals(beforeNodeId)) {
                    destInd = i;
                    if (srcInd != -1) {
                        break;
                    }
                }
            } else {
                if (srcInd != -1) {
                    break;
                }
            }
        }

        // check if resulting order would be different to current order
        if (destInd == -1) {
            if (srcInd == nodeEntries.size() - 1) {
                // no change, we're done
                return;
            }
        } else {
            if ((destInd - srcInd) == Path.INDEX_DEFAULT) {
                // no change, we're done
                return;
            }
        }
        // reorder list
        if (destInd == -1) {
            nodeEntries.add(nodeEntries.remove(srcInd));
        } else {
            if (srcInd < destInd) {
                nodeEntries.add(destInd, nodeEntries.get(srcInd));
                nodeEntries.remove(srcInd);
            } else {
                nodeEntries.add(destInd, nodeEntries.remove(srcInd));
            }
        }

        // re-create child node entries
        childNodeEntries.clear(); // TODO: any mre cleanup work to do? try some kind of merging?
        for (Iterator it = nodeEntries.iterator(); it.hasNext(); ) {
            ChildNodeEntry cne = (ChildNodeEntry) it.next();
            childNodeEntries.add(cne.getName(), cne.getUUID());
        }
        // TODO: correct?
        notifyNodesReplaced();
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
    // TODO: review. move with SPI Ids
    synchronized void moveChildNodeEntry(NodeState newParent, NodeState childState, QName newName)
        throws RepositoryException {
        // rename only
        ChildNodeEntry oldEntry = childNodeEntries.remove(childState);
        if (oldEntry != null) {
            if (newParent == this) {
                ChildNodeEntry newEntry = childNodeEntries.add(name, childState.getUUID());
                notifyNodeAdded(newEntry);
                notifyNodeRemoved(oldEntry);
            } else {
                notifyNodeRemoved(oldEntry);
                // re-parent target node
                childState.setParent(newParent);
                // add child node entry to new parent
                newParent.addChildNodeEntry(newName, childState.getUUID());
            }
        } else {
            throw new RepositoryException("Unexpected error: Child state to be renamed does not exist.");
        }
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

    //---------------------------------------------------< Listener support >---
    /**
     * {@inheritDoc}
     * <p/>
     * If the listener passed is at the same time a <code>NodeStateListener</code>
     * we add it to our list of specialized listeners.
     */
    public void addListener(ItemStateListener listener) {
        if (listener instanceof NodeStateListener) {
            synchronized (listeners) {
                if (listeners.contains(listener)) {
                    log.debug("listener already registered: " + listener);
                    // no need to add to call ItemState.addListener()
                    return;
                } else {
                    listeners.add(listener);
                }
            }
        }
        super.addListener(listener);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * If the listener passed is at the same time a <code>NodeStateListener</code>
     * we remove it from our list of specialized listeners.
     */
    public void removeListener(ItemStateListener listener) {
        if (listener instanceof NodeStateListener) {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }
        super.removeListener(listener);
    }

    //----------------------------------------------< Listener notification >---
    /**
     * Notify the listeners that a child node entry has been added
     */
    private void notifyNodeAdded(ChildNodeEntry added) {
        synchronized (listeners) {
            Iterator iter = listeners.iterator();
            while (iter.hasNext()) {
                NodeStateListener l = (NodeStateListener) iter.next();
                if (l != null) {
                    l.nodeAdded(this, added.getName(), added.getIndex(), added.getId());
                }
            }
        }
    }

    /**
     * Notify the listeners that the child node entries have been replaced
     */
    private void notifyNodesReplaced() {
        synchronized (listeners) {
            Iterator iter = listeners.iterator();
            while (iter.hasNext()) {
                NodeStateListener l = (NodeStateListener) iter.next();
                if (l != null) {
                    l.nodesReplaced(this);
                }
            }
        }
    }

    /**
     * Notify the listeners that a child node entry has been removed
     */
    private void notifyNodeRemoved(ChildNodeEntry removed) {
        synchronized (listeners) {
            Iterator iter = listeners.iterator();
            while (iter.hasNext()) {
                NodeStateListener l = (NodeStateListener) iter.next();
                if (l != null) {
                    l.nodeRemoved(this, removed.getName(),
                            removed.getIndex(), removed.getId());
                }
            }
        }
    }

    //------------------------------------------------------< inner classes >---

    /**
     * <code>ChildNodeEntries</code> represents an insertion-ordered
     * collection of <code>ChildNodeEntry</code>s that also maintains
     * the index values of same-name siblings on insertion and removal.
     * <p/>
     * <code>ChildNodeEntries</code> also provides an unmodifiable
     * <code>List</code> view.
     */
    private class ChildNodeEntries implements Collection {

        // TODO: turn this into a linked set. NodeId cannot be use as key!
        // insertion-ordered map of entries (key=NodeId, value=entry)
        private final Map entries = new LinkedMap();
        // map used for lookup by name
        // (key=name, value=either a single entry or a list of sns entries)
        private final Map nameMap = new HashMap();

        ChildNodeEntry get(NodeId id) {
            return (ChildNodeEntry) entries.get(id);
        }

        List get(QName nodeName) {
            Object obj = nameMap.get(nodeName);
            if (obj == null) {
                return Collections.EMPTY_LIST;
            }
            if (obj instanceof ArrayList) {
                // map entry is a list of siblings
                return Collections.unmodifiableList((ArrayList) obj);
            } else {
                // map entry is a single child node entry
                return Collections.singletonList(obj);
            }
        }

        ChildNodeEntry get(QName nodeName, int index) {
            if (index < Path.INDEX_DEFAULT) {
                throw new IllegalArgumentException("index is 1-based");
            }

            Object obj = nameMap.get(nodeName);
            if (obj == null) {
                return null;
            }
            if (obj instanceof ArrayList) {
                // map entry is a list of siblings
                ArrayList siblings = (ArrayList) obj;
                if (index <= siblings.size()) {
                    return (ChildNodeEntry) siblings.get(index - 1);
                }
            } else {
                // map entry is a single child node entry
                if (index == Path.INDEX_DEFAULT) {
                    return (ChildNodeEntry) obj;
                }
            }
            return null;
        }

        ChildNodeEntry add(QName nodeName, String uuid) {
            List siblings = null;
            Object obj = nameMap.get(nodeName);
            if (obj != null) {
                if (obj instanceof ArrayList) {
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

            ChildNodeEntry entry = createChildNodeEntry(nodeName, uuid);
            if (siblings != null) {
                siblings.add(entry);
            } else {
                nameMap.put(nodeName, entry);
            }
            entries.put(idFactory.createNodeId(uuid), entry);

            return entry;
        }

        void add(ChildNodeEntry cne) {
            QName nodeName = cne.getName();
            List siblings = null;
            Object obj = nameMap.get(nodeName);
            if (obj != null) {
                if (obj instanceof ArrayList) {
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

            if (siblings != null) {
                siblings.add(cne);
            } else {
                nameMap.put(nodeName, cne);
            }
            entries.put(cne.getId(), cne);
        }

        void addAll(List entriesList) {
            Iterator iter = entriesList.iterator();
            while (iter.hasNext()) {
                ChildNodeEntry entry = (ChildNodeEntry) iter.next();
                // delegate to add(QName, String) to maintain consistency
                add(entry.getName(), entry.getUUID());
            }
        }

        public ChildNodeEntry remove(QName nodeName, int index) {
            if (index < Path.INDEX_DEFAULT) {
                throw new IllegalArgumentException("index is 1-based");
            }

            Object obj = nameMap.get(nodeName);
            if (obj == null) {
                return null;
            }

            if (obj instanceof ChildNodeEntry) {
                // map entry is a single child node entry
                if (index != Path.INDEX_DEFAULT) {
                    return null;
                }
                ChildNodeEntry removedEntry = (ChildNodeEntry) obj;
                nameMap.remove(nodeName);
                entries.remove(removedEntry.getId());
                return removedEntry;
            }

            // map entry is a list of siblings
            List siblings = (ArrayList) obj;
            if (index > siblings.size()) {
                return null;
            }

            // remove from siblings list
            ChildNodeEntry removedEntry = (ChildNodeEntry) siblings.remove(index - 1);
            // remove from ordered entries map
            entries.remove(removedEntry.getId());

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
                    if (tmp.getNodeState() == nodeState) {
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

        //-------------------------------------------< unmodifiable List view >
        public boolean contains(Object o) {
            if (o instanceof ChildNodeEntry) {
                return entries.containsKey(((ChildNodeEntry) o).getId());
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
            return new EntriesIterator();
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
            Iterator iter = entries.values().iterator();
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

        //----------------------------------------------------< inner classes >

        class EntriesIterator implements Iterator {

            private final Iterator mapIter;

            EntriesIterator() {
                mapIter = entries.values().iterator();
            }

            public boolean hasNext() {
                return mapIter.hasNext();
            }

            public Object next() {
                return mapIter.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
    }
}
