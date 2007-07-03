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
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.util.StateUtility;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.NodeInfo;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import java.util.ArrayList;

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
    private final QName nodeTypeName;

    /**
     * Definition of this node state
     */
    private QNodeDefinition definition;

    /**
     * the names of this node's mixin types
     */
    private QName[] mixinTypeNames = QName.EMPTY_ARRAY;

    /**
     * Constructs a NEW NodeState
     *
     * @param entry
     * @param nodeTypeName
     * @param mixinTypeNames
     * @param isf
     * @param definition
     * @param definitionProvider
     */
    protected NodeState(NodeEntry entry, QName nodeTypeName, QName[] mixinTypeNames,
                        ItemStateFactory isf, QNodeDefinition definition,
                        ItemDefinitionProvider definitionProvider) {
        super(Status.NEW, entry, isf, definitionProvider);
        this.nodeTypeName = nodeTypeName;
        setMixinTypeNames(mixinTypeNames);
        this.definition = definition;
    }

    /**
     * Constructs an EXISTING NodeState
     *
     * @param entry
     * @param nInfo
     * @param isf
     * @param definition
     * @param definitionProvider
     */
    protected NodeState(NodeEntry entry, NodeInfo nInfo, ItemStateFactory isf,
                        QNodeDefinition definition,
                        ItemDefinitionProvider definitionProvider) {
        super(entry, isf, definitionProvider);
        this.nodeTypeName = nInfo.getNodetype();
        setMixinTypeNames(nInfo.getMixins());
        this.definition = definition;
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
     * {@inheritDoc}
     * @see ItemState#getId()
     */
    public ItemId getId() {
        return getNodeId();
    }

    /**
     * {@inheritDoc}
     * @see ItemState#getWorkspaceId()
     */
    public ItemId getWorkspaceId() {
        return getNodeEntry().getWorkspaceId();
    }

    /**
     * @see ItemState#merge(ItemState, boolean)
     */
    public boolean merge(ItemState another, boolean keepChanges) {
        if (another == null || another == this) {
            return false;
        }
        if (!another.isNode()) {
            throw new IllegalArgumentException("Attempt to merge node state with property state.");
        }
        boolean modified = false;
        synchronized (another) {
            NodeState nState = (NodeState) another;

            if (nState.definition != null && !nState.definition.equals(definition)) {
                definition = nState.definition;
                modified = true;
            }

            // since 'mixinTypeNames' are modified upon save only, no special
            // merging is required here. just reset the mixinTypeNames.
            List mixN = Arrays.asList(nState.mixinTypeNames);
            if (mixN.size() != mixinTypeNames.length || !mixN.containsAll(Arrays.asList(mixinTypeNames))) {
                setMixinTypeNames(nState.mixinTypeNames);
                modified = true;
            }
        }
        return modified;
    }

    /**
     * @see ItemState#revert()
     * @return Always returns false unless the definition has been modified
     * along with a move operation.
     */
    public boolean revert() {
        // TODO: ev. reset the 'markModified' flag
        if (StateUtility.isMovedState(this)) {
            try {
                QNodeDefinition def = definitionProvider.getQNodeDefinition(this);
                if (!def.equals(definition)) {
                    definition = def;
                    return true;
                }
            } catch (RepositoryException e) {
                // should never get here
                log.warn("Internal error", e);
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * @see ItemState#persisted(ChangeLog)
     */
    void persisted(ChangeLog changeLog) throws IllegalStateException {
        // remember parent states that have need to adjust their uniqueID/mixintypes
        // or that got a new child entry added or existing entries removed.
        Map modParents = new HashMap();

        // process deleted states from the changelog
        for (Iterator it = changeLog.deletedStates(); it.hasNext();) {
            ItemState delState = (ItemState) it.next();
            if (Status.isTerminal(delState.getStatus())) {
                log.debug("Removal of State " + delState + " has already been completed.");
                continue;
            }
            delState.getHierarchyEntry().remove();

            // adjust parent states unless the parent is removed as well
            if (delState.getHierarchyEntry().getParent().isAvailable()) {
                try {
                    NodeState parent = delState.getParent();
                    if (!changeLog.containsDeletedState(parent)) {
                        modifiedParent(parent, delState, modParents);
                    }
                } catch (RepositoryException e) {
                    // ignore. if parent state cannot be retrieved for whatever
                    // reason, it doesn't need to be adjusted
                }
            }
        }

        // process added states from the changelog. since the changlog maintains
        // LinkedHashSet for its entries, the iterator will not return a added
        // entry before its NEW parent.
        for (Iterator it = changeLog.addedStates(); it.hasNext();) {
            ItemState addedState = (ItemState) it.next();
            NodeState parent;
            try {
                parent = addedState.getParent();
            } catch (RepositoryException e) {
                // TODO: handle properly
                log.error("Internal error:", e.getMessage());
                continue;
            }
            // if parent is modified -> remember for final status reset
            if (parent.getStatus() == Status.EXISTING_MODIFIED) {
                modifiedParent(parent, addedState, modParents);
            }
            if (addedState.getStatus() == Status.EXISTING) {
                log.debug("Adding new state " + addedState + " has already been completed.");
            } else {
                // connect the new state to its overlayed state (including update
                // via merging in order to be aware of autocreated values,
                // changed definition etc.
                addedState.reload(false);
            }
        }

        for (Iterator it = changeLog.modifiedStates(); it.hasNext();) {
            ItemState modState = (ItemState) it.next();
            if (modState.getStatus() == Status.EXISTING) {
                log.debug("Modified state has already been processed");
                continue;
            }
            if (modState.isNode()) {
                if (StateUtility.isMovedState((NodeState) modState)) {
                    // and mark the moved state existing
                    modState.setStatus(Status.EXISTING);
                } else {
                    // remember state as modified only for later processing
                    if (!modParents.containsKey(modState)) {
                        modParents.put(modState, new ArrayList(2));
                    }
                }
            } else {
                // peristed prop-state has status EXISTING now
                modState.setStatus(Status.EXISTING);

                // if property state defines a modified jcr:mixinTypes the parent
                // is listed as modified state and needs to be processed at the end.
                if (QName.JCR_MIXINTYPES.equals(modState.getQName())) {
                    try {
                        modifiedParent(modState.getParent(), modState, modParents);
                    } catch (RepositoryException e) {
                        // should never occur. since parent must be available otherwise
                        // the mixin could not been added/removed.
                        log.warn("Internal error:", e.getMessage());
                    }
                }
            }
        }

        /* process all parent states that are marked modified and eventually
           need their uniqueID or mixin-types being adjusted because that property
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
            if (!(state.getStatus() == Status.EXISTING ||
                  state.getStatus() == Status.REMOVED ||
                  state.getStatus() == Status.INVALIDATED)) {
                log.info("State " + state + " with Status " + Status.getName(state.getStatus()) + " has not been processed upon ChangeLog.persisted => invalidate");
                state.setStatus(Status.EXISTING);
            }
        }
    }

    //----------------------------------------------------------< NodeState >---
    /**
     * @return The <code>NodeEntry</code> associated with this state.
     */
    public NodeEntry getNodeEntry() {
        return (NodeEntry) getHierarchyEntry();
    }

    /**
     * Returns the id of this node state.
     *
     * @return the id of this node state.
     */
    public NodeId getNodeId() {
        return getNodeEntry().getId();
    }

    /**
     * @return the unique ID of this node state or <code>null</code> if this
     * node cannot be identified with a unique ID.
     */
    public String getUniqueID() {
        return getNodeEntry().getUniqueID();
    }

    /**
     * Returns true, if this <code>NodeState</code> represent the root node.
     *
     * @return true if this <code>NodeState</code> represent the root node.
     */
    public boolean isRoot() {
        return getHierarchyEntry().getParent() == null;
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
    public QName[] getMixinTypeNames() {
        return mixinTypeNames;
    }

    /**
     * TODO improve
     * Used by NodeEntryImpl and NodeState only
     *
     * @param mixinTypeNames
     */
    public void setMixinTypeNames(QName[] mixinTypeNames) {
        if (mixinTypeNames != null) {
            this.mixinTypeNames = mixinTypeNames;
        }
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
    public QNodeDefinition getDefinition() throws RepositoryException {
        if (definition == null) {
            definition = definitionProvider.getQNodeDefinition(this);
        }
        return definition;
    }

    /**
     * Return the <code>NodeReferences</code> present on this state or
     * <code>null</code>.
     *
     * @return references
     */
    public NodeReferences getNodeReferences() {
        return isf.getNodeReferences(this);
    }

    /**
     * Utility
     * Determines if there is a valid <code>NodeEntry</code> with the
     * specified <code>name</code> and <code>index</code>.
     *
     * @param name  <code>QName</code> object specifying a node name.
     * @param index 1-based index if there are same-name child node entries.
     * @return <code>true</code> if there is a <code>NodeEntry</code> with
     * the specified <code>name</code> and <code>index</code>.
     */
    public boolean hasChildNodeEntry(QName name, int index) {
        return getNodeEntry().hasNodeEntry(name, index);
    }

    /**
     * Utility
     * Returns the child <code>NodeState</code> with the specified name
     * and index. Throws <code>ItemNotFoundException</code> if there's no
     * matching, valid entry.
     *
     * @param nodeName <code>QName</code> object specifying a node name.
     * @param index 1-based index if there are same-name child node entries.
     * @return The <code>NodeState</code> with the specified name and index
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public NodeState getChildNodeState(QName nodeName, int index) throws ItemNotFoundException, RepositoryException {
        NodeEntry ne = getNodeEntry().getNodeEntry(nodeName, index, true);
        if (ne != null) {
            return ne.getNodeState();
        } else {
            // does not exist (any more) or is a property
            throw new ItemNotFoundException("Child node "+ nodeName +" with index " + index + " does not exist.");
        }
    }

    /**
     * Utility
     *
     * @param propName <code>QName</code> object specifying a property name
     * @return <code>true</code> if there is a valid property entry with the
     * specified <code>QName</code>.
     */
    public boolean hasPropertyName(QName propName) {
        return getNodeEntry().hasPropertyEntry(propName);
    }

    /**
     * Utility method that returns the property state with the given name or
     * throws an <code>ItemNotFoundException</code> if no matching, valid
     * property could be found.
     *
     * @param propertyName The name of the property state to return.
     * @throws ItemNotFoundException If there is no (valid) property state
     * with the given name.
     * @throws RepositoryException If an error occurs while retrieving the
     * property state.
     *
     * @see NodeEntry#getPropertyEntry(QName, boolean)
     * @see PropertyEntry#getPropertyState()
     */
    public PropertyState getPropertyState(QName propertyName) throws ItemNotFoundException, RepositoryException {
        PropertyEntry pe = getNodeEntry().getPropertyEntry(propertyName, true);
        if (pe != null) {
            return pe.getPropertyState();
        } else {
            throw new ItemNotFoundException("Child Property with name " + propertyName + " does not exist.");
        }
    }

    /**
     * Reorders the child node <code>insertNode</code> before the child node
     * <code>beforeNode</code>.
     *
     * @param insertNode the child node to reorder.
     * @param beforeNode the child node where to insert the node before. If
     * <code>null</code> the child node <code>insertNode</code> is moved to the
     * end of the child node entries.
     * @throws ItemNotFoundException if <code>insertNode</code> or
     * <code>beforeNode</code> is not a child node of this <code>NodeState</code>.
     */
    synchronized void reorderChildNodeEntries(NodeState insertNode, NodeState beforeNode)
        throws ItemNotFoundException, RepositoryException {

        NodeEntry before = (beforeNode == null) ? null : beforeNode.getNodeEntry();
        insertNode.getNodeEntry().orderBefore(before);

        // mark this state as modified
        markModified();
    }

    /**
     * Moves a <code>NodeEntry</code> to a new parent. If the new parent
     * is this <code>NodeState</code>, the child state is renamed and moved
     * to the end of the child entries collection.
     *
     * @param newParent
     * @param childState
     * @param newName <code>QName</code> object specifying the entry's new name
     * @throws RepositoryException if the given child state is not a child
     * of this node state.
     */
    synchronized void moveChildNodeEntry(NodeState newParent, NodeState childState,
                                         QName newName, QNodeDefinition newDefinition)
        throws RepositoryException {
        // move child entry
        childState.getNodeEntry().move(newName, newParent.getNodeEntry(), true);
        childState.definition = newDefinition;

        // mark both this and newParent modified
        markModified();
        newParent.markModified();
        childState.markModified();
    }

    /**
     *
     * @param childState
     * @param modParents
     */
    private static void modifiedParent(NodeState parent, ItemState childState, Map modParents) {
        List l;
        if (modParents.containsKey(parent)) {
            l = (List) modParents.get(parent);
        } else {
            l = new ArrayList(2);
            modParents.put(parent, l);
        }
        if (childState != null && !childState.isNode() && StateUtility.isUuidOrMixin(childState.getQName())) {
            l.add(childState);
        }
    }

    /**
     *
     * @param parent
     * @param props
     */
    private static void adjustNodeState(NodeState parent, PropertyState[] props) {
        for (int i = 0; i < props.length; i++) {
            PropertyState propState = props[i];
            if (QName.JCR_UUID.equals(propState.getQName())) {
                if (propState.getStatus() == Status.REMOVED) {
                    parent.getNodeEntry().setUniqueID(null);
                } else {
                    // retrieve uuid from persistent layer
                    propState.reload(false);
                }
            } else if (QName.JCR_MIXINTYPES.equals(propState.getQName())) {
                QName[] mixins = StateUtility.getMixinNames(propState);
                parent.setMixinTypeNames(mixins);
            } // else: ignore.
        }

        // set parent status to 'existing'
        parent.setStatus(Status.EXISTING);
        parent.reload(false);
    }
}
