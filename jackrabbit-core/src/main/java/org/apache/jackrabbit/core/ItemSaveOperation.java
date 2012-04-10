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
package org.apache.jackrabbit.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.session.SessionWriteOperation;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.state.StaleItemStateException;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.version.InternalVersionManager;
import org.apache.jackrabbit.core.version.VersionHistoryInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The session operation triggered by {@link Item#save()}.
 */
class ItemSaveOperation implements SessionWriteOperation<Object> {

    /**
     * Logger instance.
     */
    private static final Logger log =
        LoggerFactory.getLogger(ItemSaveOperation.class);

    private final ItemState state;

    public ItemSaveOperation(ItemState state) {
        this.state = state;
    }

    public Object perform(SessionContext context) throws RepositoryException {
        SessionItemStateManager stateMgr = context.getItemStateManager();

        /**
         * build list of transient (i.e. new & modified) states that
         * should be persisted
         */
        Collection<ItemState> dirty;
        try {
            dirty = getTransientStates(context.getItemStateManager());
        } catch (ConcurrentModificationException e) {
            String msg = "Concurrent modification; session is closed";
            log.error(msg, e);
            context.getSessionImpl().logout();
            throw e;
        }
        if (dirty.size() == 0) {
            // no transient items, nothing to do here
            return this;
        }

        /**
         * build list of transient descendants in the attic
         * (i.e. those marked as 'removed')
         */
        Collection<ItemState> removed =
            getRemovedStates(context.getItemStateManager());

        // All affected item states. The keys are used to look up whether
        // an item is affected, and the values are iterated through below
        Map<ItemId, ItemState> affected =
            new HashMap<ItemId, ItemState>(dirty.size() + removed.size());
        for (ItemState state : dirty) {
            affected.put(state.getId(), state);
        }
        for (ItemState state : removed) {
            affected.put(state.getId(), state);
        }

        /**
         * make sure that this save operation is totally 'self-contained'
         * and independent; items within the scope of this save operation
         * must not have 'external' dependencies;
         * (e.g. moving a node requires that the target node including both
         * old and new parents are saved)
         */
        for (ItemState transientState : affected.values()) {
            if (transientState.isNode()) {
                NodeState nodeState = (NodeState) transientState;
                Set<NodeId> dependentIDs = new HashSet<NodeId>();
                if (nodeState.hasOverlayedState()) {
                    NodeState overlayedState =
                            (NodeState) nodeState.getOverlayedState();
                    NodeId oldParentId = overlayedState.getParentId();
                    NodeId newParentId = nodeState.getParentId();
                    if (oldParentId != null) {
                        if (newParentId == null) {
                            // node has been removed, add old parents
                            // to dependencies
                            if (overlayedState.isShareable()) {
                                dependentIDs.addAll(overlayedState.getSharedSet());
                            } else {
                                dependentIDs.add(oldParentId);
                            }
                        } else {
                            if (!oldParentId.equals(newParentId)) {
                                // node has been moved to a new location,
                                // add old and new parent to dependencies
                                dependentIDs.add(oldParentId);
                                dependentIDs.add(newParentId);
                            } else {
                                // parent id hasn't changed, check whether
                                // the node has been renamed (JCR-1034)
                                if (!affected.containsKey(newParentId)
                                        && stateMgr.hasTransientItemState(newParentId)) {
                                    try {
                                        NodeState parent = (NodeState) stateMgr.getTransientItemState(newParentId);
                                        // check parent's renamed child node entries
                                        for (ChildNodeEntry cne : parent.getRenamedChildNodeEntries()) {
                                            if (cne.getId().equals(nodeState.getId())) {
                                                // node has been renamed,
                                                // add parent to dependencies
                                                dependentIDs.add(newParentId);
                                            }
                                        }
                                    } catch (ItemStateException ise) {
                                        // should never get here
                                        log.warn("failed to retrieve transient state: " + newParentId, ise);
                                    }
                                }
                            }
                        }
                    }
                }

                // removed child node entries
                for (ChildNodeEntry cne : nodeState.getRemovedChildNodeEntries()) {
                    dependentIDs.add(cne.getId());
                }
                // added child node entries
                for (ChildNodeEntry cne : nodeState.getAddedChildNodeEntries()) {
                    dependentIDs.add(cne.getId());
                }

                // now walk through dependencies and check whether they
                // are within the scope of this save operation
                for (NodeId id : dependentIDs) {
                    if (!affected.containsKey(id)) {
                        // JCR-1359 workaround: check whether unresolved
                        // dependencies originate from 'this' session;
                        // otherwise ignore them
                        if (stateMgr.hasTransientItemState(id)
                                || stateMgr.hasTransientItemStateInAttic(id)) {
                            // need to save dependency as well
                            String msg =
                                context.getItemManager().safeGetJCRPath(id)
                                + " needs to be saved as well.";
                            log.debug(msg);
                            throw new ConstraintViolationException(msg);
                        }
                    }
                }
            }
        }

        // validate access and node type constraints
        // (this will also validate child removals)
        validateTransientItems(context, dirty, removed);

        // start the update operation
        try {
            stateMgr.edit();
        } catch (IllegalStateException e) {
            throw new RepositoryException("Unable to start edit operation", e);
        }

        boolean succeeded = false;
        try {
            // process transient items marked as 'removed'
            removeTransientItems(context.getItemStateManager(), removed);

            // process transient items that have change in mixins
            processShareableNodes(
                    context.getRepositoryContext().getNodeTypeRegistry(),
                    dirty);

            // initialize version histories for new nodes (might generate new transient state)
            if (initVersionHistories(context, dirty)) {
                // re-build the list of transient states because the previous call
                // generated new transient state
                dirty = getTransientStates(context.getItemStateManager());
            }

            // process 'new' or 'modified' transient states
            persistTransientItems(context.getItemManager(), dirty);

            // dispose the transient states marked 'new' or 'modified'
            // at this point item state data is pushed down one level,
            // node instances are disconnected from the transient
            // item state and connected to the 'overlayed' item state.
            // transient item states must be removed now. otherwise
            // the session item state provider will return an orphaned
            // item state which is not referenced by any node instance.
            for (ItemState transientState : dirty) {
                // dispose the transient state, it is no longer used
                stateMgr.disposeTransientItemState(transientState);
            }

            // end update operation
            stateMgr.update();
            // update operation succeeded
            succeeded = true;
        } catch (StaleItemStateException e) {
            throw new InvalidItemStateException(
                    "Unable to update a stale item: " + this, e);
        } catch (ItemStateException e) {
            throw new RepositoryException(
                    "Unable to update item: " + this, e);
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                stateMgr.cancel();

                // JCR-288: if an exception has been thrown during
                // update() the transient changes have already been
                // applied by persistTransientItems() and we need to
                // restore transient state, i.e. undo the effect of
                // persistTransientItems()
                restoreTransientItems(context, dirty);
            }
        }

        // now it is safe to dispose the transient states:
        // dispose the transient states marked 'removed'.
        // item states in attic are removed after store, because
        // the observation mechanism needs to build paths of removed
        // items in store().
        for (ItemState transientState : removed) {
            // dispose the transient state, it is no longer used
            stateMgr.disposeTransientItemStateInAttic(transientState);
        }

        return this;
    }

    /**
     * Builds a list of transient (i.e. new or modified) item states that are
     * within the scope of <code>this.{@link #perform(SessionContext)}</code>. The collection
     * returned is ordered depth-first, i.e. the item itself (if transient)
     * comes last.
     *
     * @return list of transient item states
     * @throws InvalidItemStateException
     * @throws RepositoryException
     */
    private Collection<ItemState> getTransientStates(
            SessionItemStateManager sism)
            throws InvalidItemStateException, RepositoryException {
        // list of transient states that should be persisted
        ArrayList<ItemState> dirty = new ArrayList<ItemState>();

        if (state.isNode()) {
            // build list of 'new' or 'modified' descendants
            for (ItemState transientState
                    : sism.getDescendantTransientItemStates(state.getId())) {
                // fail-fast test: check status of transient state
                switch (transientState.getStatus()) {
                    case ItemState.STATUS_NEW:
                    case ItemState.STATUS_EXISTING_MODIFIED:
                        // add modified state to the list
                        dirty.add(transientState);
                        break;

                    case ItemState.STATUS_STALE_DESTROYED:
                        throw new InvalidItemStateException(
                                "Item cannot be saved because it has been "
                                + "deleted externally: " + this);

                    case ItemState.STATUS_UNDEFINED:
                        throw new InvalidItemStateException(
                                "Item cannot be saved; it seems to have been "
                                + "removed externally: " + this);

                    default:
                        log.warn("Unexpected item state status: "
                                + transientState.getStatus() + " of " + this);
                        // ignore
                        break;
                }
            }
        }
        // fail-fast test: check status of this item's state
        if (state.isTransient()) {
            switch (state.getStatus()) {
                case ItemState.STATUS_EXISTING_MODIFIED:
                    // add this item's state to the list
                    dirty.add(state);
                    break;

                case ItemState.STATUS_NEW:
                    throw new RepositoryException(
                            "Cannot save a new item: " + this);

                case ItemState.STATUS_STALE_DESTROYED:
                    throw new InvalidItemStateException(
                            "Item cannot be saved because it has been"
                            + " deleted externally:" + this);

                case ItemState.STATUS_UNDEFINED:
                    throw new InvalidItemStateException(
                            "Item cannot be saved; it seems to have been"
                            + " removed externally: " + this);

                default:
                    log.warn("Unexpected item state status:"
                            + state.getStatus() + " of " + this);
                    // ignore
                    break;
            }
        }

        return dirty;
    }

    /**
     * Builds a list of transient descendant item states in the attic
     * (i.e. those marked as 'removed') that are within the scope of
     * <code>this.{@link #perform(SessionContext)}</code>.
     *
     * @return list of transient item states
     * @throws InvalidItemStateException
     * @throws RepositoryException
     */
    private Collection<ItemState> getRemovedStates(
            SessionItemStateManager sism)
            throws InvalidItemStateException, RepositoryException {
        if (state.isNode()) {
            ArrayList<ItemState> removed = new ArrayList<ItemState>();
            for (ItemState transientState
                    : sism.getDescendantTransientItemStatesInAttic(state.getId())) {
                // check if stale
                if (transientState.getStatus() == ItemState.STATUS_STALE_DESTROYED) {
                    throw new InvalidItemStateException(
                            "Item can't be removed because it has already"
                            + " been deleted externally: "
                            + transientState.getId());
                }
                removed.add(transientState);
            }
            return removed;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * the following validations/checks are performed on transient items:
     *
     * for every transient item:
     * - if it is 'modified' or 'new' check the corresponding write permission.
     * - if it is 'removed' check the REMOVE permission
     *
     * for every transient node:
     * - if it is 'new' check that its node type satisfies the
     *   'required node type' constraint specified in its definition
     * - check if 'mandatory' child items exist
     *
     * for every transient property:
     * - check if the property value satisfies the value constraints
     *   specified in the property's definition
     *
     * note that the protected flag is checked in Node.addNode/Node.remove
     * (for adding/removing child entries of a node), in
     * Node.addMixin/removeMixin/setPrimaryType (for type changes on nodes)
     * and in Property.setValue (for properties to be modified).
     */
    private void validateTransientItems(
            SessionContext context,
            Iterable<ItemState> dirty, Iterable<ItemState> removed)
            throws RepositoryException {
        SessionImpl session = context.getSessionImpl();
        ItemManager itemMgr = context.getItemManager();
        SessionItemStateManager stateMgr = context.getItemStateManager();

        AccessManager accessMgr = context.getAccessManager();
        NodeTypeManagerImpl ntMgr = context.getNodeTypeManager();
        // walk through list of dirty transient items and validate each
        for (ItemState itemState : dirty) {
            ItemDefinition def;
            if (itemState.isNode()) {
                def = itemMgr.getDefinition((NodeState) itemState);
            } else {
                def = itemMgr.getDefinition((PropertyState) itemState);
            }
            /* check permissions for non-protected items. protected items are
               only added through API methods which need to assert that
               permissions are not violated.
             */
            if (!def.isProtected()) {
                /* detect the effective set of modification:
                   - new added node -> add_node perm on the child
                   - new property added -> set_property permission
                   - property modified -> set_property permission
                   - modified nodes can be ignored for changes only included
                     child-item addition or removal or changes of protected
                     properties such as mixin-types which are covered separately
                   note: removed items are checked later on.
                   note: reordering of child nodes has been covered upfront as
                         this information isn't available here.
                */
                Path path = stateMgr.getHierarchyMgr().getPath(itemState.getId());
                boolean isGranted = true;
                if (itemState.isNode()) {
                    if (itemState.getStatus() == ItemState.STATUS_NEW) {
                        isGranted = accessMgr.isGranted(path, Permission.ADD_NODE);
                    } // else: modified node (see comment above)
                } else {
                    // modified or new property: set_property permission
                    isGranted = accessMgr.isGranted(path, Permission.SET_PROPERTY);
                }

                if (!isGranted) {
                    String msg = itemMgr.safeGetJCRPath(path) + ": not allowed to add or modify item";
                    log.debug(msg);
                    throw new AccessDeniedException(msg);
                }
            }

            if (itemState.isNode()) {
                // the transient item is a node
                NodeState nodeState = (NodeState) itemState;
                ItemId id = nodeState.getNodeId();
                NodeDefinition nodeDef = (NodeDefinition) def;
                // primary type
                NodeTypeImpl pnt = ntMgr.getNodeType(nodeState.getNodeTypeName());
                // effective node type (primary type incl. mixins)
                EffectiveNodeType ent = getEffectiveNodeType(
                        context.getRepositoryContext().getNodeTypeRegistry(),
                        nodeState);
                /**
                 * if the transient node was added (i.e. if it is 'new') or if
                 * its primary type has changed, check its node type against the
                 * required node type in its definition
                 */
                boolean primaryTypeChanged =
                        nodeState.getStatus() == ItemState.STATUS_NEW;
                if (!primaryTypeChanged) {
                    NodeState overlaid =
                            (NodeState) nodeState.getOverlayedState();
                    if (overlaid != null) {
                        Name newName = nodeState.getNodeTypeName();
                        Name oldName = overlaid.getNodeTypeName();
                        primaryTypeChanged = !newName.equals(oldName);
                    }
                }
                if (primaryTypeChanged) {
                    for (NodeType ntReq : nodeDef.getRequiredPrimaryTypes()) {
                        Name ntName = ((NodeTypeImpl) ntReq).getQName();
                        if (!(pnt.getQName().equals(ntName)
                                || pnt.isDerivedFrom(ntName))) {
                            /**
                             * the transient node's primary node type does not
                             * satisfy the 'required primary types' constraint
                             */
                            String msg = itemMgr.safeGetJCRPath(id)
                                    + " must be of node type " + ntReq.getName();
                            log.debug(msg);
                            throw new ConstraintViolationException(msg);
                        }
                    }
                }

                // mandatory child properties
                for (QPropertyDefinition pd : ent.getMandatoryPropDefs()) {
                    if (pd.getDeclaringNodeType().equals(NameConstants.MIX_VERSIONABLE)
                            || pd.getDeclaringNodeType().equals(NameConstants.MIX_SIMPLE_VERSIONABLE)) {
                        /**
                         * todo FIXME workaround for mix:versionable:
                         * the mandatory properties are initialized at a
                         * later stage and might not exist yet
                         */
                        continue;
                    }
                    String msg = itemMgr.safeGetJCRPath(id)
                                + ": mandatory property " + pd.getName()
                                + " does not exist";
                    if (!nodeState.hasPropertyName(pd.getName())) {
                        log.debug(msg);
                        throw new ConstraintViolationException(msg);
                    } else {
                        /*
                        there exists a property with the mandatory-name.
                        make sure the property really has the expected mandatory
                        property definition (and not another non-mandatory def,
                        such as e.g. multivalued residual instead of single-value
                        mandatory, named def).
                        */
                        PropertyId pi = new PropertyId(nodeState.getNodeId(), pd.getName());
                        ItemData childData = itemMgr.getItemData(pi, null, false);
                        if (!childData.getDefinition().isMandatory()) {
                            throw new ConstraintViolationException(msg);
                        }
                    }
                }
                // mandatory child nodes
                for (QItemDefinition cnd : ent.getMandatoryNodeDefs()) {
                    String msg = itemMgr.safeGetJCRPath(id)
                                + ": mandatory child node " + cnd.getName()
                                + " does not exist";
                    if (!nodeState.hasChildNodeEntry(cnd.getName())) {
                        log.debug(msg);
                        throw new ConstraintViolationException(msg);
                    } else {
                        /*
                        there exists a child node with the mandatory-name.
                        make sure the node really has the expected mandatory
                        node definition.
                        */
                        boolean hasMandatoryChild = false;
                        for (ChildNodeEntry cne : nodeState.getChildNodeEntries(cnd.getName())) {
                            ItemData childData = itemMgr.getItemData(cne.getId(), null, false);
                            if (childData.getDefinition().isMandatory()) {
                                hasMandatoryChild = true;
                                break;
                            }
                        }
                        if (!hasMandatoryChild) {
                            throw new ConstraintViolationException(msg);
                        }
                    }
                }
            } else {
                // the transient item is a property
                PropertyState propState = (PropertyState) itemState;
                ItemId propId = propState.getPropertyId();
                org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl propDef = (org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl) def;

                /**
                 * check value constraints
                 * (no need to check value constraints of protected properties
                 * as those are set by the implementation only, i.e. they
                 * cannot be set by the user through the api)
                 */
                if (!def.isProtected()) {
                    String[] constraints = propDef.getValueConstraints();
                    if (constraints != null) {
                        InternalValue[] values = propState.getValues();
                        try {
                            EffectiveNodeType.checkSetPropertyValueConstraints(
                                    propDef.unwrap(), values);
                        } catch (RepositoryException e) {
                            // repack exception for providing more verbose error message
                            String msg = itemMgr.safeGetJCRPath(propId) + ": " + e.getMessage();
                            log.debug(msg);
                            throw new ConstraintViolationException(msg);
                        }

                        /**
                         * need to manually check REFERENCE value constraints
                         * as this requires a session (target node needs to
                         * be checked)
                         */
                        if (constraints.length > 0
                                && (propDef.getRequiredType() == PropertyType.REFERENCE
                                    || propDef.getRequiredType() == PropertyType.WEAKREFERENCE)) {
                            for (InternalValue internalV : values) {
                                boolean satisfied = false;
                                String constraintViolationMsg = null;
                                try {
                                    NodeId targetId = internalV.getNodeId();
                                    if (propDef.getRequiredType() == PropertyType.WEAKREFERENCE
                                        && !itemMgr.itemExists(targetId)) {
                                        // target of weakref doesn;t exist, skip
                                        continue;
                                    }
                                    Node targetNode = session.getNodeById(targetId);
                                    /**
                                     * constraints are OR-ed, i.e. at least one
                                     * has to be satisfied
                                     */
                                    for (String constrNtName : constraints) {
                                        /**
                                         * a [WEAK]REFERENCE value constraint specifies
                                         * the name of the required node type of
                                         * the target node
                                         */
                                        if (targetNode.isNodeType(constrNtName)) {
                                            satisfied = true;
                                            break;
                                        }
                                    }
                                    if (!satisfied) {
                                        NodeType[] mixinNodeTypes = targetNode.getMixinNodeTypes();
                                        String[] targetMixins = new String[mixinNodeTypes.length];
                                        for (int j = 0; j < mixinNodeTypes.length; j++) {
                                            targetMixins[j] = mixinNodeTypes[j].getName();
                                        }
                                        String targetMixinsString = Text.implode(targetMixins, ", ");
                                        String constraintsString = Text.implode(constraints, ", ");
                                        constraintViolationMsg = itemMgr.safeGetJCRPath(propId)
                                                + ": is constraint to ["
                                                + constraintsString
                                                + "] but references [primaryType="
                                                + targetNode.getPrimaryNodeType().getName()
                                                + ", mixins="
                                                + targetMixinsString + "]";
                                    }
                                } catch (RepositoryException re) {
                                    String msg = itemMgr.safeGetJCRPath(propId)
                                            + ": failed to check "
                                            + ((propDef.getRequiredType() == PropertyType.REFERENCE) ? "REFERENCE" : "WEAKREFERENCE")
                                            + " value constraint";
                                    log.debug(msg);
                                    throw new ConstraintViolationException(msg, re);
                                }
                                if (!satisfied) {
                                    log.debug(constraintViolationMsg);
                                    throw new ConstraintViolationException(constraintViolationMsg);
                                }
                            }
                        }
                    }
                }

                /**
                 * no need to check the protected flag as this is checked
                 * in PropertyImpl.setValue(Value)
                 */
            }
        }

        // walk through list of removed transient items and check REMOVE permission
        for (ItemState itemState : removed) {
            QItemDefinition def;
            try {
                if (itemState.isNode()) {
                    def = itemMgr.getDefinition((NodeState) itemState).unwrap();
                } else {
                    def = itemMgr.getDefinition((PropertyState) itemState).unwrap();
                }
            } catch (ConstraintViolationException e) {
                // since identifier of assigned definition is not stored anymore
                // with item state (see JCR-2170), correct definition cannot be
                // determined for items which have been removed due to removal
                // of a mixin (see also JCR-2130 & JCR-2408)
                continue;
            }
            if (!def.isProtected()) {
                Path path = stateMgr.getAtticAwareHierarchyMgr().getPath(itemState.getId());
                // check REMOVE permission
                int permission = (itemState.isNode()) ? Permission.REMOVE_NODE : Permission.REMOVE_PROPERTY;
                if (!accessMgr.isGranted(path, permission)) {
                    String msg = itemMgr.safeGetJCRPath(path)
                            + ": not allowed to remove item";
                    log.debug(msg);
                    throw new AccessDeniedException(msg);
                }
            }
        }
    }

    /**
     * walk through list of transient items marked 'removed' and
     * definitively remove each one
     */
    private void removeTransientItems(
            SessionItemStateManager sism, Iterable<ItemState> states) throws StaleItemStateException {
        for (ItemState transientState : states) {
            ItemState persistentState = transientState.getOverlayedState();
            // remove persistent state
            // this will indirectly (through stateDestroyed listener method)
            // permanently invalidate all Item instances wrapping it
            assert persistentState != null;
            if (transientState.getModCount() != persistentState.getModCount()) {
                throw new StaleItemStateException(transientState.getId() + " has been modified externally");
            }
            sism.destroy(persistentState);
        }
    }

    /**
     * Process all items given in iterator and check whether <code>mix:shareable</code>
     * or (some derived node type) has been added or removed:
     * <ul>
     * <li>If the mixin <code>mix:shareable</code> (or some derived node type),
     * then initialize the shared set inside the state.</li>
     * <li>If the mixin <code>mix:shareable</code> (or some derived node type)
     * has been removed, throw.</li>
     * </ul>
     */
    private void processShareableNodes(
            NodeTypeRegistry registry, Iterable<ItemState> states)
            throws RepositoryException {
        for (ItemState is : states) {
            if (is.isNode()) {
                NodeState ns = (NodeState) is;
                boolean wasShareable = false;
                if (ns.hasOverlayedState()) {
                    NodeState old = (NodeState) ns.getOverlayedState();
                    EffectiveNodeType ntOld = getEffectiveNodeType(registry, old);
                    wasShareable = ntOld.includesNodeType(NameConstants.MIX_SHAREABLE);
                }
                EffectiveNodeType ntNew = getEffectiveNodeType(registry, ns);
                boolean isShareable = ntNew.includesNodeType(NameConstants.MIX_SHAREABLE);

                if (!wasShareable && isShareable) {
                    // mix:shareable has been added
                    ns.addShare(ns.getParentId());

                } else if (wasShareable && !isShareable) {
                    // mix:shareable has been removed: not supported
                    String msg = "Removing mix:shareable is not supported.";
                    log.debug(msg);
                    throw new UnsupportedRepositoryOperationException(msg);
                }
            }
        }
    }

    /**
     * Initialises the version history of all new nodes of node type
     * <code>mix:versionable</code>.
     *
     * @param states
     * @return true if this call generated new transient state; otherwise false
     * @throws RepositoryException
     */
    private boolean initVersionHistories(
            SessionContext context, Iterable<ItemState> states)
            throws RepositoryException {
        SessionImpl session = context.getSessionImpl();
        ItemManager itemMgr = context.getItemManager();

        // walk through list of transient items and search for new versionable nodes
        boolean createdTransientState = false;
        for (ItemState itemState : states) {
            if (itemState.isNode()) {
                NodeState nodeState = (NodeState) itemState;
                EffectiveNodeType nt = getEffectiveNodeType(
                        context.getRepositoryContext().getNodeTypeRegistry(),
                        nodeState);
                if (nt.includesNodeType(NameConstants.MIX_VERSIONABLE)) {
                    if (!nodeState.hasPropertyName(NameConstants.JCR_VERSIONHISTORY)) {
                        NodeImpl node = (NodeImpl) itemMgr.getItem(itemState.getId(), false);
                        InternalVersionManager vMgr = session.getInternalVersionManager();
                        /**
                         * check if there's already a version history for that
                         * node; this would e.g. be the case if a versionable
                         * node had been exported, removed and re-imported with
                         * either IMPORT_UUID_COLLISION_REMOVE_EXISTING or
                         * IMPORT_UUID_COLLISION_REPLACE_EXISTING;
                         * otherwise create a new version history
                         */
                        VersionHistoryInfo history =
                            vMgr.getVersionHistory(session, nodeState, null);
                        InternalValue historyId = InternalValue.create(
                                history.getVersionHistoryId());
                        InternalValue versionId = InternalValue.create(
                                history.getRootVersionId());
                        node.internalSetProperty(
                                NameConstants.JCR_VERSIONHISTORY, historyId);
                        node.internalSetProperty(
                                NameConstants.JCR_BASEVERSION, versionId);
                        node.internalSetProperty(
                                NameConstants.JCR_ISCHECKEDOUT,
                                InternalValue.create(true));
                        node.internalSetProperty(
                                NameConstants.JCR_PREDECESSORS,
                                new InternalValue[] { versionId });
                        createdTransientState = true;
                    }
                } else if (nt.includesNodeType(NameConstants.MIX_SIMPLE_VERSIONABLE)) {
                    // we need to check the version manager for an existing
                    // version history, since simple versioning does not
                    // expose it's reference in a property
                    InternalVersionManager vMgr = session.getInternalVersionManager();
                    vMgr.getVersionHistory(session, nodeState, null);

                    // create isCheckedOutProperty if not already exists
                    NodeImpl node = (NodeImpl) itemMgr.getItem(itemState.getId(), false);
                    if (!nodeState.hasPropertyName(NameConstants.JCR_ISCHECKEDOUT)) {
                        node.internalSetProperty(
                                NameConstants.JCR_ISCHECKEDOUT,
                                InternalValue.create(true));
                        createdTransientState = true;
                    }
                }
            }
        }
        return createdTransientState;
    }

    /**
     * walk through list of transient items and persist each one
     */
    private void persistTransientItems(
            ItemManager itemMgr, Iterable<ItemState> states)
            throws RepositoryException {
        for (ItemState state : states) {
            // persist state of transient item
            itemMgr.getItem(state.getId(), false).makePersistent();
        }
    }

    /**
     * walk through list of transient states and re-apply transient changes
     */
    private void restoreTransientItems(
            SessionContext context, Iterable<ItemState> items) {
        ItemManager itemMgr = context.getItemManager();
        SessionItemStateManager stateMgr = context.getItemStateManager();

        for (ItemState itemState : items) {
            ItemId id = itemState.getId();
            ItemImpl item;

            try {
                if (stateMgr.isItemStateInAttic(id)) {
                    // If an item has been removed and then again created, the
                    // item is lost after persistTransientItems() and the
                    // TransientItemStateManager will bark because of a deleted
                    // state in its attic. We therefore have to forge a new item
                    // instance ourself.
                    item = itemMgr.createItemInstance(itemState);
                    itemState.setStatus(ItemState.STATUS_NEW);
                } else {
                    try {
                        item = itemMgr.getItem(id, false);
                    } catch (ItemNotFoundException infe) {
                        // itemState probably represents a 'new' item and the
                        // ItemImpl instance wrapping it has already been gc'ed;
                        // we have to re-create the ItemImpl instance
                        item = itemMgr.createItemInstance(itemState);
                        itemState.setStatus(ItemState.STATUS_NEW);
                    }
                }
                // re-apply transient changes
                // for persistent nodes undo effect of item.makePersistent()
                if (item.isNode()) {
                    NodeImpl node = (NodeImpl) item;
                    node.restoreTransient((NodeState) itemState);
                } else {
                    PropertyImpl prop = (PropertyImpl) item;
                    prop.restoreTransient((PropertyState) itemState);
                }
            } catch (RepositoryException re) {
                // something went wrong, log exception and carry on
                String msg = itemMgr.safeGetJCRPath(id)
                    + ": failed to restore transient state";
                if (log.isDebugEnabled()) {
                    log.warn(msg, re);
                } else {
                    log.warn(msg);
                }
            }
        }
    }

    /**
     * Helper method that builds the effective (i.e. merged and resolved)
     * node type representation of the specified node's primary and mixin
     * node types.
     *
     * @param state
     * @return the effective node type
     * @throws RepositoryException
     */
    private EffectiveNodeType getEffectiveNodeType(
            NodeTypeRegistry registry, NodeState state)
            throws RepositoryException {
        try {
            return registry.getEffectiveNodeType(
                    state.getNodeTypeName(), state.getMixinTypeNames());
        } catch (NodeTypeConflictException e) {
            throw new RepositoryException(
                    "Failed to build effective node type of node state "
                    + state.getId(), e);
        }
    }


    //--------------------------------------------------------------< Object >

    /**
     * Returns a string representation of this operation.
     */
    public String toString() {
        return "item.save()";
    }

}