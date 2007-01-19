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

import org.apache.jackrabbit.jcr2spi.util.ReferenceChangeTracker;
import org.apache.jackrabbit.jcr2spi.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.operation.OperationVisitor;
import org.apache.jackrabbit.jcr2spi.operation.AddNode;
import org.apache.jackrabbit.jcr2spi.operation.AddProperty;
import org.apache.jackrabbit.jcr2spi.operation.Clone;
import org.apache.jackrabbit.jcr2spi.operation.Copy;
import org.apache.jackrabbit.jcr2spi.operation.Move;
import org.apache.jackrabbit.jcr2spi.operation.Remove;
import org.apache.jackrabbit.jcr2spi.operation.SetMixin;
import org.apache.jackrabbit.jcr2spi.operation.SetPropertyValue;
import org.apache.jackrabbit.jcr2spi.operation.ReorderNodes;
import org.apache.jackrabbit.jcr2spi.operation.Checkout;
import org.apache.jackrabbit.jcr2spi.operation.Checkin;
import org.apache.jackrabbit.jcr2spi.operation.Update;
import org.apache.jackrabbit.jcr2spi.operation.Restore;
import org.apache.jackrabbit.jcr2spi.operation.ResolveMergeConflict;
import org.apache.jackrabbit.jcr2spi.operation.Merge;
import org.apache.jackrabbit.jcr2spi.operation.LockOperation;
import org.apache.jackrabbit.jcr2spi.operation.LockRefresh;
import org.apache.jackrabbit.jcr2spi.operation.LockRelease;
import org.apache.jackrabbit.jcr2spi.operation.AddLabel;
import org.apache.jackrabbit.jcr2spi.operation.RemoveLabel;
import org.apache.jackrabbit.jcr2spi.operation.RemoveVersion;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.commons.collections.iterators.IteratorChain;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PropertyType;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.MergeException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.lock.LockException;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.io.InputStream;

/**
 * <code>SessionItemStateManager</code> ...
 */
public class SessionItemStateManager implements UpdatableItemStateManager, OperationVisitor {

    private static Logger log = LoggerFactory.getLogger(SessionItemStateManager.class);

    /**
     * State manager that allows updates
     */
    private final UpdatableItemStateManager workspaceItemStateMgr;

    /**
     * State manager for the transient items
     */
    private final TransientItemStateManager transientStateMgr;

    private final ItemStateValidator validator;

    private final QValueFactory qValueFactory;

    /**
     * Creates a new <code>SessionItemStateManager</code> instance.
     *
     * @param workspaceItemStateMgr
     */
    public SessionItemStateManager(UpdatableItemStateManager workspaceItemStateMgr,
                                   IdFactory idFactory,
                                   ItemStateValidator validator,
                                   QValueFactory qValueFactory) {
        this.workspaceItemStateMgr = workspaceItemStateMgr;
        this.transientStateMgr = new TransientItemStateManager(idFactory, workspaceItemStateMgr);
        this.validator = validator;
        this.qValueFactory = qValueFactory;
    }

    //---------------------------------------------------< ItemStateManager >---
    /**
     * {@inheritDoc}
     * @see ItemStateManager#getRootState()
     */
    public NodeState getRootState() throws ItemStateException {
        // always retrieve from transientStateMgr
        return transientStateMgr.getRootState();
    }

    /**
     * {@inheritDoc}
     * @see ItemStateManager#getItemState(ItemId)
     */
    public ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {

        ItemState itemState = transientStateMgr.getItemState(id);
        // check status of ItemState. Transient ISM also returns removed ItemStates
        if (itemState.isValid()) {
            return itemState;
        } else {
            throw new NoSuchItemStateException(id.toString());
        }
    }

    /**
     * {@inheritDoc}
     * @see ItemStateManager#hasItemState(ItemId)
     */
    public boolean hasItemState(ItemId id) {
        // first check if the specified item exists at all in the transient ISM
        if (transientStateMgr.hasItemState(id)) {
            // retrieve item and check state
            try {
                ItemState itemState = transientStateMgr.getItemState(id);
                if (itemState.isValid()) {
                    return true;
                }
            } catch (ItemStateException e) {
                // has been removed in the meantime
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * Since node references cannot be managed within the transient space,
     * this call is delegated to the workspace itemstate manager.
     *
     * @see ItemStateManager#getReferingStates(NodeState)
     * @param nodeState
     */
    public Collection getReferingStates(NodeState nodeState) throws ItemStateException {
        NodeState wspState = (NodeState) nodeState.getWorkspaceState();
        if (wspState == null) {
            // new state => unable to determine references
            return Collections.EMPTY_SET;
        }

        Collection rs = workspaceItemStateMgr.getReferingStates(wspState);
        if (rs.isEmpty()) {
            return rs;
        } else {
            // retrieve session-propertystates
            Set refStates = new HashSet();
            for (Iterator it =  rs.iterator(); it.hasNext();) {
                PropertyState wState = (PropertyState) it.next();
                ItemState sState = wState.getSessionState();
                if (sState == null) {
                    // overlaying state has not been build up to now
                   sState = getItemState(wState.getPropertyId());
                }
                // add property state to list of refering states unless it has
                // be removed in the transient layer.
                if (sState.isValid()) {
                   refStates.add(sState);
                }
            }
            return Collections.unmodifiableCollection(refStates);
        }
    }

    /**
     * {@inheritDoc}
     * Since node references cannot be managed within the transient space,
     * this call is delegated to the workspace itemstate manager.
     *
     * @see ItemStateManager#hasReferingStates(NodeState)
     * @param nodeState
     */
    public boolean hasReferingStates(NodeState nodeState) {
        try {
            return !getReferingStates(nodeState).isEmpty();
        } catch (ItemStateException e) {
            log.warn("Internal error", e);
            return false;
        }
    }

    //------------------------------------------< UpdatableItemStateManager >---
    /**
     * {@inheritDoc}
     */
    public void execute(Operation operation) throws RepositoryException {
        operation.accept(this);
    }

    /**
     * {@inheritDoc}
     */
    public void execute(ChangeLog changes) throws RepositoryException {
        throw new UnsupportedOperationException("Not implemented for SessionItemStateManager");
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        // discard all transient changes
        transientStateMgr.dispose();
        // dispose our (i.e. 'local') state manager
        workspaceItemStateMgr.dispose();
    }

    //--------------------------------------------------------------------------
    /**
     * @return <code>true</code> if this manager has any transient state;
     *         <code>false</code> otherwise.
     */
    public boolean hasPendingChanges() {
        return transientStateMgr.hasPendingChanges();
    }

    /**
     * This will save <code>state</code> and all descendants items of
     * <code>state</code> that are transiently modified in a single step. If
     * this operation fails, no item will have been saved.
     *
     * @param state the root state of the update operation
     */
    public void save(ItemState state) throws ReferentialIntegrityException,
        RepositoryException, StaleItemStateException, ItemStateException {
        // shortcut, if no modifications are present
        if (!hasPendingChanges()) {
            return;
        }

        // collect the changes to be saved
        ChangeLog changeLog = getChangeLog(state, true);
        if (!changeLog.isEmpty()) {
            // only pass changelog if there are transient modifications available
            // for the specified item and its decendants.
            workspaceItemStateMgr.execute(changeLog);

            // remove states and operations just processed from the transient ISM
            transientStateMgr.dispose(changeLog);
            // now its save to clear the changeLog
            changeLog.reset();
        }
    }

    /**
     * This will undo all changes made to <code>state</code> and descendant
     * items of <code>state</code> inside this item state manager.
     *
     * @param itemState the root state of the cancel operation.
     * @throws ItemStateException if undoing changes made to <code>state</code>
     *                            and descendant items is not a closed set of
     *                            changes. That is, at least another item needs
     *                            to be canceled as well in another sub-tree.
     */
    public void undo(ItemState itemState) throws ItemStateException, ConstraintViolationException {
        ChangeLog changeLog = getChangeLog(itemState, false);
        if (!changeLog.isEmpty()) {
            // now do it for real
            // TODO: check if states are reverted in correct order
            Iterator[] its = new Iterator[] {changeLog.addedStates(), changeLog.deletedStates(), changeLog.modifiedStates()};
            IteratorChain chain = new IteratorChain(its);
            while (chain.hasNext()) {
                ItemState state = (ItemState) chain.next();
                state.revert();
            }

            // remove transient states and related operations from the t-statemanager
            transientStateMgr.dispose(changeLog);
            changeLog.reset();
        }
    }

    /**
     * Adjust references at the end of a successful {@link Session#importXML(String, InputStream, int) XML import}.
     *
     * @param refTracker
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public void adjustReferences(ReferenceChangeTracker refTracker) throws ConstraintViolationException, RepositoryException {
        Iterator it = refTracker.getReferences();
        while (it.hasNext()) {
            PropertyState propState = (PropertyState) it.next();
            boolean modified = false;
            QValue[] values = propState.getValues();
            QValue[] newVals = new QValue[values.length];
            for (int i = 0; i < values.length; i++) {
                QValue val = values[i];
                QValue adjusted = refTracker.getMappedReference(val, qValueFactory);
                if (adjusted != null) {
                    newVals[i] = adjusted;
                    modified = true;
                } else {
                    // reference doesn't need adjusting, just copy old value
                    newVals[i] = val;
                }
            }
            if (modified) {
                int options = ItemStateValidator.CHECK_LOCK |
                    ItemStateValidator.CHECK_VERSIONING |
                    ItemStateValidator.CHECK_CONSTRAINTS;
                setPropertyStateValue(propState, newVals, PropertyType.REFERENCE, options);
            }
        }
        // make sure all entries are removed
        refTracker.clear();
    }

    //-------------------------------------------< Transient state handling >---

    /**
     *
     * @param itemState
     * @param throwOnStale Throws StaleItemStateException if either the given
     * <code>ItemState</code> or any of its decendants is stale and the flag is true.
     * @return
     * @throws StaleItemStateException if a stale <code>ItemState</code> is
     * encountered while traversing the state hierarchy. The <code>changeLog</code>
     * might have been populated with some transient item states. A client should
     * therefore not reuse the <code>changeLog</code> if such an exception is thrown.
     * @throws ItemStateException if <code>state</code> is a new item state.
     */
    private ChangeLog getChangeLog(ItemState itemState, boolean throwOnStale) throws StaleItemStateException, ItemStateException, ConstraintViolationException {
        // build changelog for affected and decendant states only
        ChangeLog changeLog = new ChangeLog(itemState);
        // fail-fast test: check status of this item's state
        if (itemState.getStatus() == Status.NEW) {
            String msg = "Cannot save an item with status NEW (" +itemState+ ").";
            log.debug(msg);
            throw new ItemStateException(msg);
        }
        if (throwOnStale && Status.isStale(itemState.getStatus())) {
            String msg =  "Attempt to save an item, that has been externally modified (" +itemState+ ").";
            log.debug(msg);
            throw new StaleItemStateException(msg);
        }
        // collect transient/stale states that should be persisted or reverted
        itemState.collectStates(changeLog, throwOnStale);

        changeLog.collectOperations(transientStateMgr.getOperations());
        changeLog.checkIsSelfContained();
        return changeLog;
    }

    //--------------------------------------------------------------------------

    /**
     * @inheritDoc
     */
    public void visit(AddNode operation) throws LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        int options = ItemStateValidator.CHECK_LOCK | ItemStateValidator.CHECK_COLLISION
            | ItemStateValidator.CHECK_VERSIONING | ItemStateValidator.CHECK_CONSTRAINTS;

        NodeState parent = operation.getParentState();
        QNodeDefinition def = validator.getApplicableNodeDefinition(operation.getNodeName(), operation.getNodeTypeName(), parent);
        addNodeState(parent, operation.getNodeName(), operation.getNodeTypeName(), operation.getUuid(), def, options);

        transientStateMgr.addOperation(operation);
    }

    /**
     * @inheritDoc
     */
    public void visit(AddProperty operation) throws ValueFormatException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        NodeState parent = operation.getParentState();
        QName propertyName = operation.getPropertyName();
        QPropertyDefinition pDef = validator.getApplicablePropertyDefinition(propertyName, operation.getPropertyType(), operation.isMultiValued(), parent);
        int targetType = pDef.getRequiredType();
        if (targetType == PropertyType.UNDEFINED) {
            targetType = operation.getPropertyType();
            if (targetType == PropertyType.UNDEFINED) {
                targetType = PropertyType.STRING;
            }
        }
        int options = ItemStateValidator.CHECK_LOCK
            | ItemStateValidator.CHECK_COLLISION
            | ItemStateValidator.CHECK_VERSIONING
            | ItemStateValidator.CHECK_CONSTRAINTS;
        addPropertyState(parent, propertyName, targetType, operation.getValues(), pDef, options);

        transientStateMgr.addOperation(operation);
    }

    /**
     * @inheritDoc
     */
    public void visit(Move operation) throws LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {

        // retrieve states and assert they are modifiable
        NodeState srcState = operation.getSourceState();
        NodeState srcParent = operation.getSourceParentState();
        NodeState destParent = operation.getDestinationParentState();

        // state validation: move-Source can be removed from old/added to new parent
        validator.checkRemoveItem(srcState,
            ItemStateValidator.CHECK_ACCESS
            | ItemStateValidator.CHECK_LOCK
            | ItemStateValidator.CHECK_VERSIONING
            | ItemStateValidator.CHECK_CONSTRAINTS);
        validator.checkAddNode(destParent, operation.getDestinationName(),
            srcState.getNodeTypeName(),
            ItemStateValidator.CHECK_ACCESS
            | ItemStateValidator.CHECK_LOCK
            | ItemStateValidator.CHECK_VERSIONING
            | ItemStateValidator.CHECK_CONSTRAINTS);
        
        // retrieve applicable definition at the new place
        QNodeDefinition newDefinition = validator.getApplicableNodeDefinition(operation.getDestinationName(), srcState.getNodeTypeName(), destParent);

        // perform the move (modifying states)
        srcParent.moveChildNodeEntry(destParent, srcState, operation.getDestinationName(), newDefinition);

        // remember operation
        transientStateMgr.addOperation(operation);
    }

    /**
     * @inheritDoc
     */
    public void visit(Remove operation) throws ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        ItemState state = operation.getRemoveState();
        int options = ItemStateValidator.CHECK_LOCK
            | ItemStateValidator.CHECK_VERSIONING
            | ItemStateValidator.CHECK_CONSTRAINTS;
        removeItemState(state, options);
        // remember operation unless new state got removed
        if (!Status.isTerminal(state.getStatus())) {
            transientStateMgr.addOperation(operation);
        }
    }

    /**
     * @inheritDoc
     */
    public void visit(SetMixin operation) throws ConstraintViolationException, AccessDeniedException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        // NOTE: nodestate is only modified upon save of the changes!
        QName[] mixinNames = operation.getMixinNames();
        NodeState nState = operation.getNodeState();

        // new array of mixinNames to be set on the nodestate (and corresponding property state)
        if (mixinNames != null && mixinNames.length > 0) {
            // update/create corresponding property state
            if (nState.hasPropertyName(QName.JCR_MIXINTYPES)) {
                // execute value of existing property
                try {
                    PropertyState pState = nState.getPropertyState(QName.JCR_MIXINTYPES);
                    int options = ItemStateValidator.CHECK_LOCK | ItemStateValidator.CHECK_VERSIONING;
                    setPropertyStateValue(pState, getQValues(mixinNames, qValueFactory), PropertyType.NAME, options);
                } catch (ItemStateException e) {
                    // should not occur, since existance has been asserted before
                    throw new RepositoryException(e);
                }
            } else {
                // create new jcr:mixinTypes property
                EffectiveNodeType ent = validator.getEffectiveNodeType(nState);
                QPropertyDefinition pd = ent.getApplicablePropertyDefinition(QName.JCR_MIXINTYPES, PropertyType.NAME, true);
                QValue[] mixinValue = getQValues(mixinNames, qValueFactory);
                int options = ItemStateValidator.CHECK_LOCK | ItemStateValidator.CHECK_VERSIONING;
                addPropertyState(nState, pd.getQName(), pd.getRequiredType(), mixinValue, pd, options);
            }
        } else {
            // remove the jcr:mixinTypes property state if already present
            if (nState.hasPropertyName(QName.JCR_MIXINTYPES)) {
                try {
                    PropertyState pState = nState.getPropertyState(QName.JCR_MIXINTYPES);
                    int options = ItemStateValidator.CHECK_LOCK | ItemStateValidator.CHECK_VERSIONING;
                    removeItemState(pState, options);
                } catch (ItemStateException e) {
                    // should not occur, since existance has been asserted before
                    throw new RepositoryException(e);
                }
            }
        }

        nState.markModified();
        transientStateMgr.addOperation(operation);
    }

    /**
     * @inheritDoc
     */
    public void visit(SetPropertyValue operation) throws ValueFormatException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        PropertyState pState = operation.getPropertyState();
        int options = ItemStateValidator.CHECK_LOCK
            | ItemStateValidator.CHECK_VERSIONING
            | ItemStateValidator.CHECK_CONSTRAINTS;
        setPropertyStateValue(pState, operation.getValues(), operation.getPropertyType(), options);
        transientStateMgr.addOperation(operation);
    }

    /**
     * @inheritDoc
     */
    public void visit(ReorderNodes operation) throws ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        NodeState parent = operation.getParentState();
        // modify the parent node state
        try {
            parent.reorderChildNodeEntries(operation.getInsertNode(), operation.getBeforeNode());
        } catch (NoSuchItemStateException e) {
            // invalid reorder-ids
            throw new ItemNotFoundException(e);
        }
        // remember the operation
        transientStateMgr.addOperation(operation);
    }

    public void visit(Clone operation) throws NoSuchWorkspaceException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Clone cannot be handled by session ItemStateManager.");
    }

    public void visit(Copy operation) throws NoSuchWorkspaceException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Copy cannot be handled by session ItemStateManager.");
    }

    public void visit(Checkout operation) throws RepositoryException, UnsupportedRepositoryOperationException {
        throw new UnsupportedOperationException("Internal error: Checkout cannot be handled by session ItemStateManager.");
    }

    public void visit(Checkin operation) throws UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Checkin cannot be handled by session ItemStateManager.");
    }

    public void visit(Update operation) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Update cannot be handled by session ItemStateManager.");
    }

    public void visit(Restore operation) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Restore cannot be handled by session ItemStateManager.");
    }

    public void visit(Merge operation) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Merge cannot be handled by session ItemStateManager.");
    }

    public void visit(ResolveMergeConflict operation) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Update cannot be handled by session ItemStateManager.");
    }

    public void visit(LockOperation operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Lock cannot be handled by session ItemStateManager.");
    }

    public void visit(LockRefresh operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: LockRefresh cannot be handled by session ItemStateManager.");
    }

    public void visit(LockRelease operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: LockRelease cannot be handled by session ItemStateManager.");
    }

    public void visit(AddLabel operation) throws VersionException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: AddLabel cannot be handled by session ItemStateManager.");
    }

    public void visit(RemoveLabel operation) throws VersionException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: RemoveLabel cannot be handled by session ItemStateManager.");
    }

    public void visit(RemoveVersion operation) throws VersionException, AccessDeniedException, ReferentialIntegrityException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: RemoveVersion cannot be handled by session ItemStateManager.");
    }

    //--------------------------------------------< Internal State Handling >---
    /**
     *
     * @param parent
     * @param propertyName
     * @param propertyType
     * @param values
     * @param pDef
     * @param options int used to validate the given params. Note, that the options
     * differ depending if the 'addProperty' is called regularly or to create
     * auto-created (or protected) properties.
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws ItemExistsException
     * @throws NoSuchNodeTypeException
     * @throws UnsupportedRepositoryOperationException
     * @throws VersionException
     * @throws RepositoryException
     */
    private void addPropertyState(NodeState parent, QName propertyName,
                                  int propertyType, QValue[] values,
                                  QPropertyDefinition pDef, int options)
        throws LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {

        validator.checkAddProperty(parent, propertyName, pDef, options);

        // create property state
        transientStateMgr.createNewPropertyState(propertyName, parent, pDef, values, propertyType);
    }

    private void addNodeState(NodeState parent, QName nodeName, QName nodeTypeName,
                              String uuid, QNodeDefinition definition, int options)
        throws RepositoryException, ConstraintViolationException, AccessDeniedException,
        UnsupportedRepositoryOperationException, NoSuchNodeTypeException,
        ItemExistsException, VersionException {

        // check if add node is possible. note, that the options differ if
        // the 'addNode' is called from inside a regular add-node to create
        // autocreated child nodes that my are 'protected' by their def.
        validator.checkAddNode(parent, nodeName, nodeTypeName, options);

        try {
            validator.getEffectiveNodeType(new QName[]{nodeTypeName});
        } catch (NodeTypeConflictException e) {
            throw new RepositoryException("node type conflict: " + e.getMessage());
        }
        if (nodeTypeName == null) {
            // no primary node type specified,
            // try default primary type from definition
            nodeTypeName = definition.getDefaultPrimaryType();
            if (nodeTypeName == null) {
                String msg = "an applicable node type could not be determined for " + nodeName;
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }

        NodeState nodeState = transientStateMgr.createNewNodeState(nodeName, uuid, nodeTypeName, definition, parent);

        EffectiveNodeType ent = validator.getEffectiveNodeType(nodeState);
        // add 'auto-create' properties defined in node type
        QPropertyDefinition[] pda = ent.getAutoCreatePropDefs();
        for (int i = 0; i < pda.length; i++) {
            QPropertyDefinition pd = pda[i];
            QValue[] autoValue = computeSystemGeneratedPropertyValues(nodeState, pd);
            if (autoValue != null) {
                int propOptions = ItemStateValidator.CHECK_NONE;
                // execute 'addProperty' without adding operation.
                addPropertyState(nodeState, pd.getQName(), pd.getRequiredType(), autoValue, pd, propOptions);
            }
        }

        // recursively add 'auto-create' child nodes defined in node type
        QNodeDefinition[] nda = ent.getAutoCreateNodeDefs();
        for (int i = 0; i < nda.length; i++) {
            QNodeDefinition nd = nda[i];
            // execute 'addNode' without adding the operation.
            int opt = ItemStateValidator.CHECK_LOCK | ItemStateValidator.CHECK_COLLISION;
            addNodeState(nodeState, nd.getQName(), nd.getDefaultPrimaryType(), null, nd, opt);
        }
    }

    private void removeItemState(ItemState itemState, int options) throws RepositoryException {
        validator.checkRemoveItem(itemState, options);
        // recursively remove the given state and all child states.
        boolean success = false;
        try {
            itemState.remove();
            success = true;
        } catch (ItemStateException e) {
            throw new RepositoryException("Cannot remove item: " + e.getMessage(), e);
        } finally {
            if (!success) {
                // TODO: TOBEFIXED undo state modifications
            }
        }
    }

    /**
     *
     * @param propState
     * @param iva
     * @param valueType
     * @throws ValueFormatException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws ItemExistsException
     * @throws UnsupportedRepositoryOperationException
     * @throws VersionException
     * @throws RepositoryException
     */
    private void setPropertyStateValue(PropertyState propState, QValue[] iva,
                                       int valueType, int options)
        throws ValueFormatException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        // assert that the property can be modified.
        validator.checkSetProperty(propState, options);
        propState.setValues(iva, valueType);
    }

    /**
     * Computes the values of well-known system (i.e. protected) properties
     * as well as auto-created properties which define default value(s)
     *
     * @param parent
     * @param def
     * @return the computed values
     */
    private QValue[] computeSystemGeneratedPropertyValues(NodeState parent,
                                                          QPropertyDefinition def) {
        QValue[] genValues = null;
        QValue[] qDefaultValues = def.getDefaultValues();
        if (qDefaultValues != null && qDefaultValues.length > 0) {
            genValues = qDefaultValues;
        } else if (def.isAutoCreated()) {
            // handle known predefined nodetypes that declare auto-created
            // properties without default values
            QName declaringNT = def.getDeclaringNodeType();
            QName name = def.getQName();
            if (QName.MIX_REFERENCEABLE.equals(declaringNT) && QName.JCR_UUID.equals(name)) {
                // mix:referenceable node type defines jcr:uuid
                String uniqueID = parent.getUniqueID();
                if (uniqueID == null) {
                    uniqueID = UUID.randomUUID().toString();
                }
                genValues = new QValue[]{qValueFactory.create(uniqueID, PropertyType.REFERENCE)};
            } else if (QName.NT_BASE.equals(declaringNT)) {
                // nt:base node type
                if (QName.JCR_PRIMARYTYPE.equals(name)) {
                    // jcr:primaryType property
                    genValues = new QValue[]{qValueFactory.create(parent.getNodeTypeName())};
                } else if (QName.JCR_MIXINTYPES.equals(name)) {
                    // jcr:mixinTypes property
                    QName[] mixins = parent.getMixinTypeNames();
                    genValues = getQValues(mixins, qValueFactory);
                }
            } else if (QName.NT_HIERARCHYNODE.equals(declaringNT) && QName.JCR_CREATED.equals(name)) {
                // nt:hierarchyNode node type defines jcr:created property
                genValues = new QValue[]{qValueFactory.create(Calendar.getInstance())};
            } else if (QName.NT_RESOURCE.equals(declaringNT) && QName.JCR_LASTMODIFIED.equals(name)) {
                // nt:resource node type defines jcr:lastModified property
                genValues = new QValue[]{qValueFactory.create(Calendar.getInstance())};
            } else if (QName.NT_VERSION.equals(declaringNT) && QName.JCR_CREATED.equals(name)) {
                // nt:version node type defines jcr:created property
                genValues = new QValue[]{qValueFactory.create(Calendar.getInstance())};
            } else {
                // TODO: TOBEFIXED. other nodetype -> build some default value
                log.warn("Missing implementation. Nodetype " + def.getDeclaringNodeType() + " defines autocreated property " + def.getQName() + " without default value.");
            }
        }
        return genValues;
    }

    /**
     * @param qNames
     * @param factory
     * @return An array of QValue objects from the given <code>QName</code>s
     */
    private static QValue[] getQValues(QName[] qNames, QValueFactory factory) {
        QValue[] ret = new QValue[qNames.length];
        for (int i = 0; i < qNames.length; i++) {
            ret[i] = factory.create(qNames[i]);
        }
        return ret;
    }
}
