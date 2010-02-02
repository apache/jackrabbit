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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.apache.jackrabbit.jcr2spi.util.StateUtility;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>NodeState</code> represents the state of a <code>Node</code>.
 */
public class NodeState extends ItemState {

    private static Logger log = LoggerFactory.getLogger(NodeState.class);

    /**
     * the name of this node's primary type
     */
    private Name nodeTypeName;

    /**
     * Definition of this node state
     */
    private QNodeDefinition definition;

    /**
     * the names of this node's mixin types
     */
    private Name[] mixinTypeNames = Name.EMPTY_ARRAY;

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
    protected NodeState(NodeEntry entry, Name nodeTypeName, Name[] mixinTypeNames,
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
     * @param definitionProvider
     */
    protected NodeState(NodeEntry entry, NodeInfo nInfo, ItemStateFactory isf,
                        ItemDefinitionProvider definitionProvider) {
        super(entry, isf, definitionProvider);
        this.nodeTypeName = nInfo.getNodetype();
        setMixinTypeNames(nInfo.getMixins());
    }

    //----------------------------------------------------------< ItemState >---
    /**
     * Determines if this item state represents a node.
     *
     * @return always true
     * @see ItemState#isNode
     */
    @Override
    public final boolean isNode() {
        return true;
    }

    /**
     * {@inheritDoc}
     * @see ItemState#getId()
     */
    @Override
    public ItemId getId() throws RepositoryException {
        return getNodeId();
    }

    /**
     * {@inheritDoc}
     * @see ItemState#getWorkspaceId()
     */
    @Override
    public ItemId getWorkspaceId() throws RepositoryException {
        return getNodeEntry().getWorkspaceId();
    }

    /**
     * @see ItemState#merge(ItemState, boolean)
     */
    @Override
    public MergeResult merge(ItemState another, boolean keepChanges) {
        boolean modified = false;
        if (another != null && another != this) {
            if (!another.isNode()) {
                throw new IllegalArgumentException("Attempt to merge node state with property state.");
            }
            synchronized (another) {
                NodeState nState = (NodeState) another;

                if (!nodeTypeName.equals(nState.nodeTypeName)) {
                    nodeTypeName = nState.nodeTypeName;
                    modified = true;
                }

                if (nState.definition != null && !nState.definition.equals(definition)) {
                    definition = nState.definition;
                    modified = true;
                }

                // since 'mixinTypeNames' are modified upon save only, no special
                // merging is required here. just reset the mixinTypeNames.
                List<Name> mixN = Arrays.asList(nState.mixinTypeNames);
                if (mixN.size() != mixinTypeNames.length || !mixN.containsAll(Arrays.asList(mixinTypeNames))) {
                    setMixinTypeNames(nState.mixinTypeNames);
                    modified = true;
                }
            }
        }
        return new SimpleMergeResult(modified);
    }

    /**
     * @see ItemState#revert()
     * @return Always returns false unless the definition has been modified
     * along with a move operation.
     */
    @Override
    public boolean revert() {
        // TODO: ev. reset the 'markModified' flag
        if (StateUtility.isMovedState(this)) {
            try {
                QNodeDefinition def = retrieveDefinition();
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
    public NodeId getNodeId() throws RepositoryException {
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
    public Name getNodeTypeName() {
        return nodeTypeName;
    }

    /**
     * Returns the names of this node's mixin types.
     *
     * @return a set of the names of this node's mixin types.
     */
    public Name[] getMixinTypeNames() {
        return mixinTypeNames;
    }

    /**
     * Used by NodeEntryImpl and NodeState only
     *
     * @param mixinTypeNames
     */
    public void setMixinTypeNames(Name[] mixinTypeNames) {
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
    public synchronized Name[] getNodeTypeNames() {
        // mixin types
        Name[] mixinNames = getMixinTypeNames();
        Name[] types = new Name[mixinNames.length + 1];
        System.arraycopy(mixinNames, 0, types, 0, mixinNames.length);
        // primary type
        types[types.length - 1] = getNodeTypeName();
        return types;
    }

    /**
     * TODO: clarify usage
     * In case the status of the given node state is not {@link Status#EXISTING}
     * the transiently added mixin types are taken into account as well.
     *
     * @return
     */
    public synchronized Name[] getAllNodeTypeNames() {
        Name[] allNtNames;
        if (getStatus() == Status.EXISTING) {
            allNtNames = getNodeTypeNames();
        } else {
            // TODO: check if correct (and only used for creating new)
            Name primaryType = getNodeTypeName();
            allNtNames = new Name[] { primaryType }; // default
            try {
                PropertyEntry pe = getNodeEntry().getPropertyEntry(NameConstants.JCR_MIXINTYPES, true);
                if (pe != null) {
                    PropertyState mixins = pe.getPropertyState();
                    QValue[] values = mixins.getValues();
                    allNtNames = new Name[values.length + 1];
                    for (int i = 0; i < values.length; i++) {
                        allNtNames[i] = values[i].getName();
                    }
                    allNtNames[values.length] = primaryType;
                } // else: no jcr:mixinTypes property exists -> ignore
            } catch (RepositoryException e) {
                // unexpected error: ignore
            }
        }
        return allNtNames;
    }

    /**
     * Returns true if the definition of this state has already been
     * calculated. False otherwise.
     *
     * @return true if definition has already been calculated.
     */
    public boolean hasDefinition() throws RepositoryException {
        return definition != null;
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
            definition = retrieveDefinition();
        }
        return definition;
    }

    /**
     * Returns the identifiers of all reference properties that point to
     * this node.
     *
     * @param propertyName name filter of referring properties to be returned;
     * if <code>null</code> then all references are returned.
     * @param weak Boolean flag indicating whether weak references should be
     * returned or not.
     * @return reference property identifiers
     */
    public Iterator<PropertyId> getNodeReferences(Name propertyName, boolean weak) {
        return isf.getNodeReferences(this, propertyName, weak);
    }

    /**
     * Utility
     * Determines if there is a valid <code>NodeEntry</code> with the
     * specified <code>name</code> and <code>index</code>.
     *
     * @param name  <code>Name</code> object specifying a node name.
     * @param index 1-based index if there are same-name child node entries.
     * @return <code>true</code> if there is a <code>NodeEntry</code> with
     * the specified <code>name</code> and <code>index</code>.
     */
    public boolean hasChildNodeEntry(Name name, int index) {
        return getNodeEntry().hasNodeEntry(name, index);
    }

    /**
     * Utility
     * Returns the child <code>NodeState</code> with the specified name
     * and index. Throws <code>ItemNotFoundException</code> if there's no
     * matching, valid entry.
     *
     * @param nodeName <code>Name</code> object specifying a node name.
     * @param index 1-based index if there are same-name child node entries.
     * @return The <code>NodeState</code> with the specified name and index
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public NodeState getChildNodeState(Name nodeName, int index) throws ItemNotFoundException, RepositoryException {
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
     * @param propName <code>Name</code> object specifying a property name
     * @return <code>true</code> if there is a valid property entry with the
     * specified <code>Name</code>.
     */
    public boolean hasPropertyName(Name propName) {
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
     * @see NodeEntry#getPropertyEntry(Name, boolean)
     * @see PropertyEntry#getPropertyState()
     */
    public PropertyState getPropertyState(Name propertyName) throws ItemNotFoundException, RepositoryException {
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
     * @param newName <code>Name</code> object specifying the entry's new name
     * @throws RepositoryException if the given child state is not a child
     * of this node state.
     */
    synchronized void moveChildNodeEntry(NodeState newParent, NodeState childState,
                                         Name newName, QNodeDefinition newDefinition)
        throws RepositoryException {
        // move child entry
        childState.getNodeEntry().move(newName, newParent.getNodeEntry(), true);
        childState.definition = newDefinition;

        // mark both this and newParent modified
        markModified();
        newParent.markModified();
        childState.markModified();
    }

    private QNodeDefinition retrieveDefinition() throws RepositoryException {
        QNodeDefinition def;
        if (isRoot()) {
            def = definitionProvider.getRootNodeDefinition();
        } else {
            /*
             Don't use getAllNodeTypeNames() to retrieve the definition:
             for NEW-states the definition is always set upon creation.
             for all other states the definition must be retrieved only taking
             the effective nodetypes present on the parent into account
             any kind of transiently added mixins must not have an effect
             on the definition retrieved for an state that has been persisted
             before. The effective NT must be evaluated as if it had been
             evaluated upon creating the workspace state.
             */
            NodeState parent = getParent();
            NodeId wspId = (NodeId) getWorkspaceId();
            def = definitionProvider.getQNodeDefinition(parent.getNodeTypeNames(), getName(), getNodeTypeName(), wspId);
        }
        return def;
    }
}
