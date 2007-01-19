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
import org.apache.jackrabbit.jcr2spi.state.entry.ChildNodeEntry;
import org.apache.jackrabbit.jcr2spi.state.entry.ChildPropertyEntry;
import org.apache.jackrabbit.jcr2spi.state.entry.PropertyReference;
import org.apache.jackrabbit.jcr2spi.state.entry.ChildItemEntry;
import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

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
     * The unique ID of this node state or <code>null</code> if this node state
     * cannot be identified with a unique ID.
     */
    private String uniqueID;

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
     */
    private ChildNodeEntries childNodeEntries;

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
     * @param uniqueID      the uniqueID of this NodeState or <code>null</code> if
     *                      this node state cannot be identified with a UUID.
     * @param parent        the parent of this NodeState
     * @param nodeTypeName  node type of this node
     * @param definition
     * @param initialStatus the initial status of the node state object
     * @param isf           the item state factory responsible for creating node
     *                      states.
     * @param idFactory     the <code>IdFactory</code> to create new id
     */
    protected NodeState(QName name, String uniqueID, NodeState parent,
                        QName nodeTypeName, QNodeDefinition definition,
                        int initialStatus, ItemStateFactory isf,
                        IdFactory idFactory, boolean isWorkspaceState) {
        super(parent, initialStatus, isf, idFactory, isWorkspaceState);
        this.name = name;
        this.uniqueID = uniqueID;
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
                NodeState wspState = overlayedState;
                name = wspState.name;
                uniqueID = wspState.uniqueID;
                nodeTypeName = wspState.nodeTypeName;
                definition = wspState.definition;

                init(wspState.getMixinTypeNames(), wspState.getPropertyNames(), wspState.getNodeReferences());
            }
        }
    }

    /**
     *
     * @param mixinTypeNames
     * @param propertyNames
     * @param references
     */
    void init(QName[] mixinTypeNames, Collection propertyNames, NodeReferences references) {
        if (mixinTypeNames != null) {
            this.mixinTypeNames = mixinTypeNames;
        }
        // set the node references
        this.references = references;
        // add property references
        propertiesInAttic.clear();
        properties.clear();
        Iterator it = propertyNames.iterator();
        while (it.hasNext()) {
            QName propName = (QName) it.next();
            ChildPropertyEntry pe = PropertyReference.create(this, propName, isf, idFactory);
            properties.put(propName, pe);
        }
    }

    private ChildNodeEntries childNodeEntries() {
        if (childNodeEntries == null) {
            try {
                childNodeEntries = isf.getChildNodeEntries(this);
            } catch (ItemStateException e) {
                // TODO improve
                throw new IllegalStateException();
            }
        }
        return childNodeEntries;
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

    /**
     * {@inheritDoc}
     * @see ItemState#reload(boolean)
     */
    public void reload(boolean keepChanges) {
        // recursivly reload states from peristent storage including props
        // that are in the attic.
        for (Iterator it = getAllChildEntries(true, true); it.hasNext();) {
            ChildItemEntry ce = (ChildItemEntry) it.next();
            if (ce.isAvailable()) {
                try {
                    ce.getItemState().reload(keepChanges);
                } catch (ItemStateException e) {
                    // should not occur
                }
            }
        }

        if (isWorkspaceState()) {
            // reload from persistent storage ('keepChanges' not relevant).
            try {
                /* TODO: TOBEFIXED.
                   recreating nodestate not correct. parent still has entry pointing
                   to this state and subsequently retrieving the childentries fails,
                   since id of tmp state cannot be resolved.
                   -> add workaround until state hierarchy has been modified
                */
                NodeState tmp = isf.createNodeState(getNodeId(), getParent());
                tmp.childNodeEntries = isf.getChildNodeEntries(this);

                if (merge(tmp, false) || getStatus() == Status.INVALIDATED) {
                    setStatus(Status.MODIFIED);
                }
            } catch (NoSuchItemStateException e) {
                // remove entry from parent
                getParent().childNodeEntries().remove(this);
                // inform overlaying state and listeners
                setStatus(Status.REMOVED);
            } catch (ItemStateException e) {
                // TODO rather throw? remove from parent?
                log.warn("Exception while refreshing property state: " + e);
                log.debug("Stacktrace: ", e);
            }
        } else {
            /* session-state: if keepChanges is true only existing or invalidated
               states must be updated. otherwise the state gets updated and might
               be marked 'Stale' if transient changes are present and the
               workspace-state is modified. */
            if (!keepChanges || getStatus() == Status.EXISTING || getStatus() == Status.INVALIDATED) {
                // calling refresh on the workspace state will in turn reset this state
                overlayedState.reload(keepChanges);
            }
        }
    }

    boolean merge(ItemState another, boolean keepChanges) {
        if (another == null || another == this) {
            return false;
        }
        if (!another.isNode()) {
            throw new IllegalArgumentException("Attempt to merge node state with property state.");
        }
        boolean modified = false;
        synchronized (another) {
            NodeState nState = (NodeState) another;
            name = nState.name;
            setUniqueID(nState.uniqueID);
            nodeTypeName = nState.nodeTypeName;
            definition = nState.definition;

            // refs, mixin-types can be copied. they are never transiently changed.
            references = nState.references;
            List mixN = Arrays.asList(nState.mixinTypeNames);
            modified = (mixN.size() != mixinTypeNames.length || !mixN.containsAll(Arrays.asList(mixinTypeNames)));
            mixinTypeNames = nState.mixinTypeNames;

            if (!keepChanges && !propertiesInAttic.isEmpty()) {
                // remove all entries in the attic
                modified = true;
                propertiesInAttic.clear();
            }

            /* merge child entries without loosing valid entries and connected states. */
            // add all entry from wspState that are missing in this state
            for (Iterator it = nState.getAllChildEntries(false, false); it.hasNext();) {
                ChildItemEntry ce = (ChildItemEntry) it.next();
                QName childName = ce.getName();
                if (ce.denotesNode()) {
                    ChildNodeEntry cne = (ChildNodeEntry) ce;
                    int index = cne.getIndex();
                    if (!childNodeEntries().contains(childName, index, cne.getUniqueID())) {
                        modified = true;
                        childNodeEntries().add(childName, cne.getUniqueID(), index);
                    }
                } else {
                    if (!hasPropertyName(childName)) {
                        modified = true;
                        addPropertyEntry(PropertyReference.create(this, childName, isf, idFactory));
                    }
                }
            }

            // if keepChanges is false, remove all entries from this state,
            // that are missing in the given other state.
            if (!keepChanges) {
                for (Iterator it = getAllChildEntries(true, false); it.hasNext();) {
                    ChildItemEntry ce = (ChildItemEntry) it.next();
                    QName childName = ce.getName();
                    boolean toRemove = false;
                    if (ce.denotesNode()) {
                        ChildNodeEntry cne = (ChildNodeEntry) ce;
                        int index = cne.getIndex();
                        toRemove = !nState.childNodeEntries().contains(childName, index, cne.getUniqueID());
                    } else {
                        toRemove = !nState.properties.containsKey(childName);
                    }

                    if (toRemove) {
                        modified = true;
                        if (ce.isAvailable()) {
                            // TODO: check if correct.
                            try {
                                ItemState st = ce.getItemState();
                                if (st.getStatus() == Status.EXISTING_MODIFIED) {
                                    st.setStatus(Status.STALE_DESTROYED);
                                } else {
                                    st.setStatus(Status.REMOVED);
                                }
                            } catch (ItemStateException e) {
                                log.error("Internal error", e);
                            }
                        }
                        // and remove the corresponding entry
                        if (ce.denotesNode()) {
                            childNodeEntries().remove(childName, ((ChildNodeEntry)ce).getIndex());
                        } else {
                            properties.remove(childName);
                        }
                    }
                }
            }
        }

        return modified;
    }

    /**
     * {@inheritDoc}
     * @see ItemState#invalidate(boolean)
     */
    public void invalidate(boolean recursive) {
        if (recursive) {
            // invalidate all child entries including properties present in the
            // attic (removed props shadowed by a new property with the same name).
            for (Iterator it = getAllChildEntries(false, true); it.hasNext();) {
                ChildItemEntry ce = (ChildItemEntry) it.next();
                if (ce.isAvailable()) {
                    try {
                        ce.getItemState().invalidate(true);
                    } catch (ItemStateException e) {
                        // should not occur
                    }
                }
            }
        }
        // ... and invalidate this state
        if (isWorkspaceState()) {
            // workspace state
            setStatus(Status.INVALIDATED);
        } else {
            // TODO only invalidate if existing?
            if (getStatus() == Status.EXISTING) {
                // set workspace state invalidated, this will in turn invalidate
                // this (session) state as well
                overlayedState.setStatus(Status.INVALIDATED);
            }
        }
    }
    //----------------------------------------------------------< NodeState >---
    /**
     * Returns the id of this node state.
     *
     * @return the id of this node state.
     */
    public NodeId getNodeId() {
        if (uniqueID != null) {
            return idFactory.createNodeId(uniqueID);
        }

        NodeState parent = getParent();
        if (parent == null) {
           // root node
            return idFactory.createNodeId((String) null, Path.ROOT);
        } else {
            // find this in parent child node entries
            for (Iterator it = parent.childNodeEntries().get(name).iterator(); it.hasNext(); ) {
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
        }
        // TODO: replace with ItemStateException instead of error.
        throw new InternalError("Unable to retrieve NodeId for NodeState");
    }

    /**
     * @return the unique ID of this node state or <code>null</code> if this
     * node cannot be identified with a unique ID.
     */
    public String getUniqueID() {
        return uniqueID;
    }

    /**
     * Modify the uniqueID of this state and make sure, that the parent state
     * contains a proper childNodeEntry for this state. If the given uniqueID is
     * not different from the uniqueID of this state, the method returns silently
     * without changing neither the parent nor this state.
     *
     * @param uniqueID
     */
    private void setUniqueID(String uniqueID) {
        String oldUniqueID = this.uniqueID;
        boolean mod = (oldUniqueID == null) ? uniqueID != null : !oldUniqueID.equals(uniqueID);
        if (mod) {
            this.uniqueID = uniqueID;
            if (getParent() != null) {
                getParent().childNodeEntries().replaceEntry(this);
            }
        }
    }

    /**
     * Returns the index of this node state.
     *
     * @return the index.
     */
    public int getIndex() throws ItemNotFoundException {
        if (getParent() == null) {
            // the root state may never have siblings
            return Path.INDEX_DEFAULT;
        }

        if (getDefinition().allowsSameNameSiblings()) {
            ChildNodeEntry entry = getParent().childNodeEntries().get(this);
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
     * Return all nodetype names that are defined to this <code>NodeState</code>
     * including the primary nodetype and the mixins.
     *
     * @return array of NodeType names
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
        return containsValidChildNodeEntry(childNodeEntries());
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
        return containsValidChildNodeEntry(childNodeEntries().get(name));
    }

    /**
     * Determines if there is a valid <code>ChildNodeEntry</code> with the
     * specified <code>name</code> and <code>index</code>.
     *
     * @param name  <code>QName</code> object specifying a node name.
     * @param index 1-based index if there are same-name child node entries.
     * @return <code>true</code> if there is a <code>ChildNodeEntry</code> with
     * the specified <code>name</code> and <code>index</code>.
     */
    public synchronized boolean hasChildNodeEntry(QName name, int index) {
        return isValidChildNodeEntry(childNodeEntries().get(name, index));
    }

    /**
     * Returns the valid <code>ChildNodeEntry</code> with the specified name
     * and index or <code>null</code> if there's no matching entry.
     *
     * @param nodeName <code>QName</code> object specifying a node name.
     * @param index 1-based index if there are same-name child node entries.
     * @return The <code>ChildNodeEntry</code> with the specified name and index
     * or <code>null</code> if there's no matching entry.
     */
    public synchronized ChildNodeEntry getChildNodeEntry(QName nodeName, int index) {
        ChildNodeEntry cne = childNodeEntries().get(nodeName, index);
        if (isValidChildNodeEntry(cne)) {
            return cne;
        } else {
            return null;
        }
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
    synchronized ChildNodeEntry getChildNodeEntry(NodeId nodeId) {
        String uid = nodeId.getUniqueID();
        Path path = nodeId.getPath();
        ChildNodeEntry cne;
        if (uid != null && path == null) {
            // retrieve child-entry by uid
            cne = childNodeEntries().get(null, uid);
        } else {
           // retrieve child-entry by name and index
            Path.PathElement nameElement = path.getNameElement();
            cne = childNodeEntries().get(nameElement.getName(), nameElement.getIndex());
        }

        if (isValidChildNodeEntry(cne)) {
            return cne;
        } else {
            return null;
        }
    }

    /**
     * Returns a unmodifiable collection of <code>ChildNodeEntry</code> objects
     * denoting the child nodes of this node.
     *
     * @return collection of <code>ChildNodeEntry</code> objects
     */
    public synchronized Collection getChildNodeEntries() {
        Collection entries = new ArrayList();
        for (Iterator it = childNodeEntries().iterator(); it.hasNext();) {
            ChildNodeEntry cne = (ChildNodeEntry) it.next();
            if (isValidChildNodeEntry(cne)) {
                entries.add(cne);
            }
        }
        return Collections.unmodifiableCollection(entries);
    }

    /**
     * Returns a unmodifiable list of <code>ChildNodeEntry</code>s with the
     * specified name.
     *
     * @param nodeName name of the child node entries that should be returned
     * @return list of <code>ChildNodeEntry</code> objects
     */
    public synchronized List getChildNodeEntries(QName nodeName) {
        List entries = new ArrayList();
        for (Iterator it = childNodeEntries().get(nodeName).iterator(); it.hasNext();) {
            ChildNodeEntry cne = (ChildNodeEntry) it.next();
            if (isValidChildNodeEntry(cne)) {
                entries.add(cne);
            }
        }
        return Collections.unmodifiableList(entries);
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
        return isValidChildPropertyEntry(entry);
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
                if (isValidChildPropertyEntry(propEntry)) {
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
                if (QName.JCR_UUID.equals(propName) && uniqueID == null) {
                    PropertyState ps = propEntry.getPropertyState();
                    setUniqueID(ps.getValue().getString());
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
    private ChildPropertyEntry removePropertyEntry(QName propName) {
        ChildPropertyEntry cpe = (ChildPropertyEntry) properties.remove(propName);
        if (cpe != null) {
            if (isWorkspaceState()) {
                if (QName.JCR_UUID.equals(propName)) {
                    setUniqueID(null);
                } else if (QName.JCR_MIXINTYPES.equals(propName)) {
                    mixinTypeNames = QName.EMPTY_ARRAY;
                }
            }
        }
        return cpe;
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
        List sns = childNodeEntries().get(cne.getName());
        // index is one based
        int index = 1;
        for (Iterator it = sns.iterator(); it.hasNext(); ) {
            ChildNodeEntry entry = (ChildNodeEntry) it.next();
            if (entry == cne) {
                return index;
            }
            // skip entries that belong to removed or invalidated states.
            // NOTE, that in this case the nodestate must be available from the cne.
            if (isValidChildNodeEntry(entry)) {
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
                String uniqueID = (evId.getPath() != null) ? null : evId.getUniqueID();

                // add new childNodeEntry if it has not been added by
                // some earlier 'add' event
                // TODO: TOBEFIXED for SNSs
                ChildNodeEntry cne = (uniqueID != null) ? childNodeEntries().get(name, uniqueID) : childNodeEntries().get(name, index);
                if (cne == null) {
                    cne = childNodeEntries().add(name, uniqueID, index);
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
                    childNodeEntries().remove(name, index);
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
     * @see ItemState#persisted(ChangeLog, CacheBehaviour)
     */
    void persisted(ChangeLog changeLog, CacheBehaviour cacheBehaviour)
        throws IllegalStateException {
        checkIsSessionState();

        // remember parent states that have need to adjust their uniqueID/mixintypes
        // or that got a new child entry added or existing entries removed.
        Map modParents = new HashMap();

        // process deleted states from the changelog
        for (Iterator it = changeLog.deletedStates(); it.hasNext();) {
            ItemState state = (ItemState) it.next();
            state.setStatus(Status.REMOVED);
            state.overlayedState.setStatus(Status.REMOVED);

            // adjust parent states unless the parent is removed as well
            NodeState parent = state.getParent();
            if (!changeLog.containsDeletedState(parent)) {
                NodeState overlayedParent = (NodeState) parent.overlayedState;
                if (state.isNode()) {
                    overlayedParent.childNodeEntries().remove((NodeState)state.overlayedState);
                    parent.childNodeEntries().remove((NodeState)state);
                } else {
                    overlayedParent.removePropertyEntry(state.overlayedState.getQName());
                    parent.removePropertyEntry(state.getQName());
                }
                modifiedParent(parent, state, modParents);
            }
        }

        // process added states from the changelog. since the changlog maintains
        // LinkedHashSet for its entries, the iterator will not return a added
        // entry before its NEW parent.
        for (Iterator it = changeLog.addedStates(); it.hasNext();) {
            ItemState addedState = (ItemState) it.next();
            NodeState parent = addedState.getParent();
            // TODO: improve. only retrieve overlayed state, if necessary
            try {
                // adjust parent child-entries
                NodeState overlayedParent = (NodeState) parent.overlayedState;
                QName addedName = addedState.getQName();
                if (addedState.isNode()) {
                    int index = parent.childNodeEntries().get((NodeState)addedState).getIndex();
                    ChildNodeEntry cne;
                    // check for existing, valid child-node-entry
                    if (overlayedParent.hasChildNodeEntry(addedName, index)) {
                        cne = overlayedParent.getChildNodeEntry(addedName, index);
                    } else {
                        cne = overlayedParent.childNodeEntries().add(addedState.getQName(), null, index);
                    }
                    NodeState overlayed = cne.getNodeState();
                    if (overlayed.getUniqueID() != null) {
                        overlayedParent.childNodeEntries().replaceEntry(overlayed);
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

                // make sure the new state gets updated (e.g. uniqueID created by server)
                addedState.merge(addedState.overlayedState, true);
                // and mark the added-state existing
                addedState.setStatus(Status.EXISTING);
                // if parent is modified -> remember for final status reset
                if (parent.getStatus() == Status.EXISTING_MODIFIED) {
                    modifiedParent(parent, addedState, modParents);
                }
                it.remove();
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
                    NodeState newParent = (NodeState) modState.getParent().overlayedState;
                    NodeState overlayed = (NodeState) modState.overlayedState;
                    try {
                        overlayed.getParent().moveEntry(newParent, overlayed, modNodeState.getQName(), modNodeState.getDefinition());
                    } catch (RepositoryException e) {
                        // should never occur
                        log.error("Internal error while moving childnode entries.", e);
                    }
                    // and mark the moved state existing
                    modNodeState.setStatus(Status.EXISTING);
                    it.remove();
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
            }
        }

        /* process all parent states that are marked modified and eventually
           need their uniqueID or mixin-types being adjusted because that property
           has been added, modified or removed */
        for (Iterator it = modParents.keySet().iterator(); it.hasNext();) {
            NodeState parent = (NodeState) it.next();
            List l = (List) modParents.get(parent);
            if (cacheBehaviour == CacheBehaviour.OBSERVATION) {
                adjustNodeState(parent, (PropertyState[]) l.toArray(new PropertyState[l.size()]));
            } else {
                // TODO: improve. invalidate necessary states only
                parent.invalidate(false);
            }
        }

        /* finally check if all entries in the changelog have been processed
           and eventually force a reload in order not to have any states with
           wrong transient status floating around. */
        Iterator[] its = new Iterator[] {changeLog.addedStates(), changeLog.deletedStates(), changeLog.modifiedStates()};
        IteratorChain chain = new IteratorChain(its);
        while (chain.hasNext()) {
            ItemState state = (ItemState) chain.next();
            if (!(state.getStatus() == Status.EXISTING ||
                  state.getStatus() == Status.REMOVED ||
                  state.getStatus() == Status.INVALIDATED)) {
                log.info("State " + state + " with Status " + Status.getName(state.getStatus()) + " has not been processed upon ChangeLog.persisted => invalidate");
                state.invalidate(false);
            }
        }
    }

    /**
     * Recursively removes all child states and then calls {@link ItemState#remove()}.
     *
     * @inheritDoc
     * @see ItemState#remove()
     */
    void remove() throws ItemStateException {
        checkIsSessionState();
        if (!isValid()) {
            throw new ItemStateException("cannot remove an invalid NodeState");
        }
        for (Iterator it = getAllChildEntries(true, false); it.hasNext();) {
            ChildItemEntry ce = (ChildItemEntry) it.next();
            if (ce.isAvailable()) {
                ItemState childState = ce.getItemState();
                if (childState.isValid()) {
                    childState.remove();
                } else if (!ce.denotesNode()) {
                    // remove invalid property state from properties map
                    it.remove();
                    // TODO: check if for node-entries no action is required
                }
            } else if (!ce.denotesNode()) {
                // remove unresolved entry from properties map
                it.remove();
                // TODO check if for node entries no action required
            }
        }

        if (!propertiesInAttic.isEmpty()) {
            // move all properties from attic back to properties map
            properties.putAll(propertiesInAttic);
            propertiesInAttic.clear();
        }

        // process this state as well.
        super.remove();
    }

    /**
     * Calls {@link ItemState#revert()} and moves all properties from the attic
     * back into th properties map.
     *
     * @inheritDoc
     * @see ItemState#revert()
     */
    void revert() throws ItemStateException {
        super.revert();
        if (!propertiesInAttic.isEmpty()) {
            // move all properties from attic back to properties map
            properties.putAll(propertiesInAttic);
            propertiesInAttic.clear();
        }
    }

    /**
     * Adds this state to the changeLog if it is transiently modified, new or stale
     * and subsequently calls this method on all child states including those
     * property states that have been moved to the attic.
     *
     * @inheritDoc
     * @see ItemState#collectStates(ChangeLog, boolean)
     */
    void collectStates(ChangeLog changeLog, boolean throwOnStale) throws StaleItemStateException {
        super.collectStates(changeLog, throwOnStale);

        // collect transient child states including properties in attic.
        for (Iterator it = getAllChildEntries(false, true); it.hasNext();) {
            ChildItemEntry ce = (ChildItemEntry) it.next();
            if (ce.isAvailable()) {
                try {
                    ce.getItemState().collectStates(changeLog, throwOnStale);
                } catch (ItemStateException e) {
                    // should not happen because ref is available
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
        childNodeEntries().add(child);
        markModified();
    }

    /**
     * Adds a property state to this node state.
     *
     * @param propState the property state to add.
     * @throws ItemExistsException If <code>this</code> node state already
     * contains a valid property state with the same name as <code>propState</code>.
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
     * Notifies this node state that a child item state has been removed or
     * otherwise modified.
     *
     * @param childState the child item state that has been removed or modified.
     * @throws IllegalArgumentException if <code>this</code> is not the parent
     * of the given <code>ItemState</code>.
     */
    synchronized void childStatusChanged(ItemState childState, int previousStatus) {
        checkIsSessionState();
        if (childState.getParent() != this) {
            throw new IllegalArgumentException("This NodeState is not the parent of propState");
        }

        switch (childState.getStatus()) {
            case Status.EXISTING_REMOVED:
                markModified();
                break;
            case Status.REMOVED:
                if (childState.isNode()) {
                    childNodeEntries().remove((NodeState) childState);
                } else {
                    removePropertyEntry(childState.getQName());
                }
                // TODO: TOBEFIXED. removing a NEW state may even remove the 'modified'
                // flag from the parent, if this NEW state was the only modification.
                if (previousStatus != Status.NEW) {
                    markModified();
                }
                break;
            case Status.EXISTING:
                if (previousStatus == Status.EXISTING_REMOVED && !childState.isNode()) {
                    QName propName = childState.getQName();
                    if (propertiesInAttic.containsKey(propName)) {
                        properties.put(childState.getQName(), propertiesInAttic.remove(propName));
                    }
                }
        }
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

        childNodeEntries().reorder(insertNode, beforeNode);
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

    /**
     *
     * @param newParent
     * @param childState
     * @param newName
     * @param newDefinition
     * @throws RepositoryException
     */
    private void moveEntry(NodeState newParent, NodeState childState, QName newName, QNodeDefinition newDefinition) throws RepositoryException {
        ChildNodeEntry oldEntry = childNodeEntries().remove(childState);
        if (oldEntry != null) {
            childState.name = newName;
            // re-parent target node
            childState.parent = newParent;
            // set definition according to new definition required by the new parent
            childState.definition = newDefinition;
            // add child node entry to new parent
            newParent.childNodeEntries().add(childState);
        } else {
            throw new RepositoryException("Unexpected error: Child state to be moved does not exist.");
        }
    }

    /**
     *
     * @param createNewList if true, both properties and childNodeEntries are
     * copied to new list, since recursive calls may call this node state to
     * inform the removal of a child entry.
     *
     * @return
     */
    private Iterator getAllChildEntries(boolean createNewList, boolean includeAttic) {
        Iterator[] its;
        if (createNewList) {
            List props = new ArrayList(properties.values());
            List children = new ArrayList(childNodeEntries());
            if (includeAttic) {
                List attic = new ArrayList(propertiesInAttic.values());
                its = new Iterator[] {attic.iterator(), props.iterator(), children.iterator()};
            } else {
                its = new Iterator[] {props.iterator(), children.iterator()};
            }
        } else {
            if (includeAttic) {
                its = new Iterator[] {propertiesInAttic.values().iterator(), properties.values().iterator(), childNodeEntries().iterator()};
            } else {
                its = new Iterator[] {properties.values().iterator(), childNodeEntries().iterator()};
            }
        }
        IteratorChain chain = new IteratorChain(its);
        return chain;
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
        boolean hasValid = false;
        for (Iterator it = entries.iterator(); it.hasNext() && !hasValid; ) {
            ChildNodeEntry cne = (ChildNodeEntry) it.next();
            hasValid = isValidChildNodeEntry(cne);
        }
        return hasValid;
    }

    /**
     * Returns <code>true</code> if the given childnode entry is not
     * <code>null</code> and resolves to a NodeState, that is valid or if the
     * childnode entry has not been resolved up to now (assuming the corresponding
     * nodestate is still valid).
     *
     * @param cne ChildNodeEntry to check.
     * @return <code>true</code> if the given entry is valid.
     */
    private static boolean isValidChildNodeEntry(ChildNodeEntry cne) {
        // shortcut.
        if (cne == null) {
            return false;
        }
        boolean isValid = false;
        if (cne.isAvailable()) {
            try {
                isValid = cne.getNodeState().isValid();
            } catch (ItemStateException e) {
                // should not occur, if the cne is available.
            }
        } else {
            // then it has never been accessed and must exist
            // TODO: check if this assumption is correct
            isValid = true;
        }

        return isValid;
    }

    /**
     * Returns <code>true</code> if the given childproperty entry is not
     * <code>null</code> and resolves to a PropertyState, that is valid or if the
     * childproperty entry has not been resolved up to now (assuming the corresponding
     * PropertyState is still valid).
     *
     * @param cpe ChildPropertyEntry to check.
     * @return <code>true</code> if the given entry is valid.
     */
    private static boolean isValidChildPropertyEntry(ChildPropertyEntry cpe) {
        if (cpe == null) {
            return false;
        }
        boolean isValid = false;
        if (cpe.isAvailable()) {
            try {
                isValid = cpe.getPropertyState().isValid();
            } catch (ItemStateException e) {
                // probably deleted in the meantime. should not occur.
            }
        } else {
            // then it must be valid // TODO check if this assumption is correct.
            isValid = true;
        }
        return isValid;
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
                        String uniqueID = (props[i].getStatus() == Status.REMOVED) ? null : props[i].getValue().getString();
                        sState.setUniqueID(uniqueID);
                        overlayed.setUniqueID(uniqueID);
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
            sState.merge(overlayed, false);
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
        if (modState.getParent() == null) {
            // the root state cannot be moved
            return false;
        } else {
            return modState.overlayedState.getParent() != modState.getParent().overlayedState;
        }
    }
}
