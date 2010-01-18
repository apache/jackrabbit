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
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionException;

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
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.state.StaleItemStateException;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.version.VersionHistoryInfo;
import org.apache.jackrabbit.core.version.InternalVersionManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ItemImpl</code> implements the <code>Item</code> interface.
 */
public abstract class ItemImpl implements Item {

    private static Logger log = LoggerFactory.getLogger(ItemImpl.class);

    protected static final int STATUS_NORMAL = 0;
    protected static final int STATUS_MODIFIED = 1;
    protected static final int STATUS_DESTROYED = 2;
    protected static final int STATUS_INVALIDATED = 3;

    protected final ItemId id;

    /**
     * <code>Session</code> through which this <code>Item</code> was acquired
     */
    protected final SessionImpl session;

    /**
     * the <code>Repository</code> object
     */
    protected final RepositoryImpl rep;

    /**
     * Item data associated with this item.
     */
    protected final ItemData data;

    /**
     * <code>ItemManager</code> that created this <code>Item</code>
     */
    protected final ItemManager itemMgr;

    /**
     * <code>SessionItemStateManager</code> associated with this <code>Item</code>
     */
    protected final SessionItemStateManager stateMgr;

    /**
     * Package private constructor.
     *
     * @param itemMgr   the <code>ItemManager</code> that created this <code>Item</code>
     * @param session   the <code>Session</code> through which this <code>Item</code> is acquired
     * @param data      ItemData of this <code>Item</code>
     */
    ItemImpl(ItemManager itemMgr, SessionImpl session, ItemData data) {
        this.session = session;
        rep = (RepositoryImpl) session.getRepository();
        stateMgr = session.getItemStateManager();
        this.id = data.getId();
        this.itemMgr = itemMgr;
        this.data = data;
    }

    /**
     * Performs a sanity check on this item and the associated session.
     *
     * @throws RepositoryException if this item has been rendered invalid for some reason
     */
    protected void sanityCheck() throws RepositoryException {
        // check session status
        session.sanityCheck();

        // check status of this item for read operation
        final int status = data.getStatus();
        if (status == STATUS_DESTROYED || status == STATUS_INVALIDATED) {
            throw new InvalidItemStateException(id + ": the item does not exist anymore");
        }
    }

    protected boolean isTransient() {
        return getItemState().isTransient();
    }

    protected abstract ItemState getOrCreateTransientItemState() throws RepositoryException;

    protected abstract void makePersistent() throws InvalidItemStateException;

    /**
     * Marks this instance as 'removed' and notifies its listeners.
     * The resulting state is either 'temporarily invalidated' or
     * 'permanently invalidated', depending on the initial state.
     *
     * @throws RepositoryException if an error occurs
     */
    protected void setRemoved() throws RepositoryException {
        final int status = data.getStatus();
        if (status == STATUS_INVALIDATED || status == STATUS_DESTROYED) {
            // this instance is already 'invalid', get outta here
            return;
        }

        ItemState transientState = getOrCreateTransientItemState();
        if (transientState.getStatus() == ItemState.STATUS_NEW) {
            // this is a 'new' item, simply dispose the transient state
            // (it is no longer used); this will indirectly (through
            // stateDiscarded listener method) invalidate this instance permanently
            stateMgr.disposeTransientItemState(transientState);
        } else {
            // this is an 'existing' item (i.e. it is backed by persistent
            // state), mark it as 'removed'
            transientState.setStatus(ItemState.STATUS_EXISTING_REMOVED);
            // transfer the transient state to the attic
            stateMgr.moveTransientItemStateToAttic(transientState);

            // set state of this instance to 'invalid'
            data.setStatus(STATUS_INVALIDATED);
            // notify the manager that this instance has been
            // temporarily invalidated
            itemMgr.itemInvalidated(id, data);
        }
    }

    /**
     * Returns the item-state associated with this <code>Item</code>.
     *
     * @return state associated with this <code>Item</code>
     */
    ItemState getItemState() {
        return data.getState();
    }

    /**
     * Return the id of this <code>Item</code>.
     *
     * @return the id of this <code>Item</code>
     */
    public ItemId getId() {
        return id;
    }

    /**
     * Returns the primary path to this <code>Item</code>.
     *
     * @return the primary path to this <code>Item</code>
     */
    public Path getPrimaryPath() throws RepositoryException {
        return session.getHierarchyManager().getPath(id);
    }

    /**
     * Builds a list of transient (i.e. new or modified) item states that are
     * within the scope of <code>this.{@link #save()}</code>. The collection
     * returned is ordered depth-first, i.e. the item itself (if transient)
     * comes last.
     *
     * @return list of transient item states
     * @throws InvalidItemStateException
     * @throws RepositoryException
     */
    private Collection<ItemState> getTransientStates()
            throws InvalidItemStateException, RepositoryException {
        // list of transient states that should be persisted
        ArrayList<ItemState> dirty = new ArrayList<ItemState>();
        ItemState transientState;

        if (isNode()) {
            // build list of 'new' or 'modified' descendants
            Iterator<ItemState> iter = stateMgr.getDescendantTransientItemStates((NodeId) id);
            while (iter.hasNext()) {
                transientState = iter.next();
                // fail-fast test: check status of transient state
                switch (transientState.getStatus()) {
                    case ItemState.STATUS_NEW:
                    case ItemState.STATUS_EXISTING_MODIFIED:
                        // add modified state to the list
                        dirty.add(transientState);
                        break;

                    case ItemState.STATUS_STALE_MODIFIED:
                        throw new InvalidItemStateException(
                                "Item cannot be saved because it has been "
                                + "modified externally: " + this);

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
        if (isTransient()) {
            final ItemState state = getItemState();
            switch (state.getStatus()) {
                case ItemState.STATUS_EXISTING_MODIFIED:
                    // add this item's state to the list
                    dirty.add(state);
                    break;

                case ItemState.STATUS_NEW:
                    throw new RepositoryException(
                            "Cannot save a new item: " + this);

                case ItemState.STATUS_STALE_MODIFIED:
                    throw new InvalidItemStateException(
                            "Item cannot be saved because it has been"
                            + " modified externally: " + this);

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
     * <code>this.{@link #save()}</code>.
     *
     * @return list of transient item states
     * @throws InvalidItemStateException
     * @throws RepositoryException
     */
    private Collection<ItemState> getRemovedStates()
            throws InvalidItemStateException, RepositoryException {
        ArrayList<ItemState> removed = new ArrayList<ItemState>();
        ItemState transientState;

        if (isNode()) {
            Iterator<ItemState> iter = stateMgr.getDescendantTransientItemStatesInAttic((NodeId) id);
            while (iter.hasNext()) {
                transientState = iter.next();
                // check if stale
                if (transientState.getStatus() == ItemState.STATUS_STALE_MODIFIED) {
                    String msg = transientState.getId()
                            + ": the item cannot be removed because it has been modified externally.";
                    log.debug(msg);
                    throw new InvalidItemStateException(msg);
                }
                if (transientState.getStatus() == ItemState.STATUS_STALE_DESTROYED) {
                    String msg = transientState.getId()
                            + ": the item cannot be removed because it has already been deleted externally.";
                    log.debug(msg);
                    throw new InvalidItemStateException(msg);
                }
                removed.add(transientState);
            }
        }
        return removed;
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
    private void validateTransientItems(Iterable<ItemState> dirty, Iterable<ItemState> removed)
            throws AccessDeniedException, ConstraintViolationException,
            RepositoryException {
        AccessManager accessMgr = session.getAccessManager();
        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
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
                EffectiveNodeType ent = getEffectiveNodeType(nodeState);
                /**
                 * if the transient node was added (i.e. if it is 'new') or if
                 * its primary type has changed, check its node type against the
                 * required node type in its definition
                 */
                if (nodeState.getStatus() == ItemState.STATUS_NEW
                        || !nodeState.getNodeTypeName().equals(
                            ((NodeState) nodeState.getOverlayedState()).getNodeTypeName())) {
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
    private void removeTransientItems(Iterable<ItemState> states) {
        for (ItemState transientState : states) {
            ItemState persistentState = transientState.getOverlayedState();
            /**
             * remove persistent state
             *
             * this will indirectly (through stateDestroyed listener method)
             * permanently invalidate all Item instances wrapping it
             */
            stateMgr.destroy(persistentState);
        }
    }

    /**
     * walk through list of transient items and persist each one
     */
    private void persistTransientItems(Iterable<ItemState> states)
            throws RepositoryException {
        for (ItemState state : states) {
            // persist state of transient item
            itemMgr.getItem(state.getId()).makePersistent();
        }
    }

    /**
     * walk through list of transient states and re-apply transient changes
     */
    private void restoreTransientItems(Iterable<ItemState> items) {
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
                        item = itemMgr.getItem(id);
                    } catch (ItemNotFoundException infe) {
                        // itemState probably represents a 'new' item and the
                        // ItemImpl instance wrapping it has already been gc'ed;
                        // we have to re-create the ItemImpl instance
                        item = itemMgr.createItemInstance(itemState);
                        itemState.setStatus(ItemState.STATUS_NEW);
                    }
                }
                if (!item.isTransient()) {
                    // re-apply transient changes (i.e. undo effect of item.makePersistent())
                    if (item.isNode()) {
                        NodeImpl node = (NodeImpl) item;
                        node.restoreTransient((NodeState) itemState);
                    } else {
                        PropertyImpl prop = (PropertyImpl) item;
                        prop.restoreTransient((PropertyState) itemState);
                    }
                }
            } catch (RepositoryException re) {
                // something went wrong, log exception and carry on
                String msg = itemMgr.safeGetJCRPath(id)
                    + ": failed to restore transient state";
                log.warn(msg, re);
            }
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
    private void processShareableNodes(Iterable<ItemState> states) throws RepositoryException {
        for (ItemState is : states) {
            if (is.isNode()) {
                NodeState ns = (NodeState) is;
                boolean wasShareable = false;
                if (ns.hasOverlayedState()) {
                    NodeState old = (NodeState) ns.getOverlayedState();
                    EffectiveNodeType ntOld = getEffectiveNodeType(old);
                    wasShareable = ntOld.includesNodeType(NameConstants.MIX_SHAREABLE);
                }
                EffectiveNodeType ntNew = getEffectiveNodeType(ns);
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
     * Initializes the version history of all new nodes of node type
     * <code>mix:versionable</code>.
     * <p/>
     * Called by {@link #save()}.
     *
     * @param states
     * @return true if this call generated new transient state; otherwise false
     * @throws RepositoryException
     */
    private boolean initVersionHistories(Iterable<ItemState> states) throws RepositoryException {
        // walk through list of transient items and search for new versionable nodes
        boolean createdTransientState = false;
        for (ItemState itemState : states) {
            if (itemState.isNode()) {
                NodeState nodeState = (NodeState) itemState;
                EffectiveNodeType nt = getEffectiveNodeType(nodeState);
                if (nt.includesNodeType(NameConstants.MIX_VERSIONABLE)) {
                    if (!nodeState.hasPropertyName(NameConstants.JCR_VERSIONHISTORY)) {
                        NodeImpl node = (NodeImpl) itemMgr.getItem(itemState.getId());
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
                    NodeImpl node = (NodeImpl) itemMgr.getItem(itemState.getId());
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
     * Helper method that builds the effective (i.e. merged and resolved)
     * node type representation of the specified node's primary and mixin
     * node types.
     *
     * @param state
     * @return the effective node type
     * @throws RepositoryException
     */
    private EffectiveNodeType getEffectiveNodeType(NodeState state)
            throws RepositoryException {
        try {
            NodeTypeRegistry registry =
                session.getNodeTypeManager().getNodeTypeRegistry();
            return registry.getEffectiveNodeType(
                    state.getNodeTypeName(), state.getMixinTypeNames());
        } catch (NodeTypeConflictException e) {
            throw new RepositoryException(
                    "Failed to build effective node type of node state "
                    + state.getId(), e);
        }
    }

    /**
     * Failsafe mapping of internal <code>id</code> to JCR path for use in
     * diagnostic output, error messages etc.
     *
     * @return JCR path or some fallback value
     */
    public String safeGetJCRPath() {
        return itemMgr.safeGetJCRPath(id);
    }

    /**
     * Same as <code>{@link Item#remove()}</code> except for the
     * <code>noChecks</code> parameter.
     *
     * @param noChecks
     * @throws VersionException
     * @throws LockException
     * @throws RepositoryException
     */
    protected void internalRemove(boolean noChecks)
            throws VersionException, LockException,
            ConstraintViolationException, RepositoryException {

        // check state of this instance
        sanityCheck();

        // check if this is the root node
        if (getDepth() == 0) {
            throw new RepositoryException("Cannot remove the root node");
        }

        NodeImpl parentNode = (NodeImpl) getParent();
        if (!noChecks) {
            // check if protected and not under retention/hold
            int options = ItemValidator.CHECK_CONSTRAINTS | ItemValidator.CHECK_HOLD |
                    ItemValidator.CHECK_RETENTION;
            session.getValidator().checkRemove(this, options, Permission.NONE);

            // parent node: make sure it is checked-out and not protected nor locked.
            options = ItemValidator.CHECK_LOCK | ItemValidator.CHECK_CHECKED_OUT |
                    ItemValidator.CHECK_CONSTRAINTS;
            session.getValidator().checkModify(parentNode, options, Permission.NONE);
        }

        // delegate the removal of the child item to the parent node
        Path.Element thisName = getPrimaryPath().getNameElement();
        if (isNode()) {
            parentNode.removeChildNode(thisName.getName(), thisName.getIndex());
        } else {
            parentNode.removeChildProperty(thisName.getName());
        }
    }

    /**
     * Same as <code>{@link Item#getName()}</code> except that
     * this method returns a <code>Name</code> instead of a
     * <code>String</code>.
     *
     * @return the name of this item as <code>Name</code>
     * @throws RepositoryException if an error occurs.
     */
    public abstract Name getQName() throws RepositoryException;

    //-----------------------------------------------------------------< Item >

    /**
     * {@inheritDoc}
     */
    public abstract void accept(ItemVisitor visitor)
            throws RepositoryException;

    /**
     * {@inheritDoc}
     */
    public abstract boolean isNode();

    /**
     * {@inheritDoc}
     */
    public abstract String getName() throws RepositoryException;

    /**
     * {@inheritDoc}
     */
    public abstract Node getParent()
            throws ItemNotFoundException, AccessDeniedException, RepositoryException;

    /**
     * {@inheritDoc}
     */
    public boolean isNew() {
        final ItemState state = getItemState();
        return state.isTransient() && state.getOverlayedState() == null;
    }

    /**
     * checks if this item is new. running outside of transactions, this
     * is the same as {@link #isNew()} but within a transaction an item can
     * be saved but not yet persisted.
     */
    protected boolean isTransactionalNew() {
        final ItemState state = getItemState();
        return state.getStatus() == ItemState.STATUS_NEW;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isModified() {
        final ItemState state = getItemState();
        return state.isTransient() && state.getOverlayedState() != null;
    }

    /**
     * {@inheritDoc}
     */
    public void remove()
            throws VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        internalRemove(false);
    }

    /**
     * {@inheritDoc}
     */
    public void save()
            throws AccessDeniedException, ItemExistsException,
            ConstraintViolationException, InvalidItemStateException,
            ReferentialIntegrityException, VersionException, LockException,
            NoSuchNodeTypeException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // synchronize on this session
        synchronized (session) {
            /**
             * build list of transient (i.e. new & modified) states that
             * should be persisted
             */
            Collection<ItemState> dirty;
            try {
                dirty = getTransientStates();
            } catch (ConcurrentModificationException e) {
                String msg = "Concurrent modification; session is closed";
                log.error(msg, e);
                session.logout();
                throw e;
            }
            if (dirty.size() == 0) {
                // no transient items, nothing to do here
                return;
            }

            /**
             * build list of transient descendants in the attic
             * (i.e. those marked as 'removed')
             */
            Collection<ItemState> removed = getRemovedStates();

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
                                String msg = itemMgr.safeGetJCRPath(id)
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
            validateTransientItems(dirty, removed);

            // start the update operation
            try {
                stateMgr.edit();
            } catch (IllegalStateException e) {
                String msg = "Unable to start edit operation";
                log.debug(msg);
                throw new RepositoryException(msg, e);
            }

            boolean succeeded = false;

            try {

                // process transient items marked as 'removed'
                removeTransientItems(removed);

                // process transient items that have change in mixins
                processShareableNodes(dirty);

                // initialize version histories for new nodes (might generate new transient state)
                if (initVersionHistories(dirty)) {
                    // re-build the list of transient states because the previous call
                    // generated new transient state
                    dirty = getTransientStates();
                }

                // process 'new' or 'modified' transient states
                persistTransientItems(dirty);

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
                throw new InvalidItemStateException(e.getMessage());
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
                    restoreTransientItems(dirty);
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
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void refresh(boolean keepChanges)
            throws InvalidItemStateException, RepositoryException {
        // check state of this instance
        sanityCheck();

        if (keepChanges) {
            /** todo FIXME should reset Item#status field to STATUS_NORMAL
             * of all descendent non-transient instances; maybe also
             * have to reset stale ItemState instances */
            return;
        }

        if (isNode()) {
            // check if this is the root node
            if (getDepth() == 0) {
                // optimization
                stateMgr.disposeAllTransientItemStates();
                return;
            }
        }

        // list of transient items that should be discarded
        ArrayList<ItemState> list = new ArrayList<ItemState>();
        ItemState transientState;

        // check status of this item's state
        if (isTransient()) {
            transientState = getItemState();
            switch (transientState.getStatus()) {
                case ItemState.STATUS_STALE_MODIFIED:
                case ItemState.STATUS_STALE_DESTROYED:
                    // add this item's state to the list
                    list.add(transientState);
                    break;

                case ItemState.STATUS_EXISTING_MODIFIED:
                    if (!transientState.getParentId().equals(
                            transientState.getOverlayedState().getParentId())) {
                        throw new RepositoryException(
                                "Cannot refresh a moved item: " + this +
                                " - possible solution: refresh the parent");
                    }
                    list.add(transientState);
                    break;

                case ItemState.STATUS_NEW:
                    throw new RepositoryException(
                            "Cannot refresh a new item: " + this);

                default:
                    log.warn("Unexpected item state status:"
                            + transientState.getStatus() + " of " + this);
                    // ignore
                    break;
            }
        }

        if (isNode()) {
            // build list of 'new', 'modified' or 'stale' descendants
            Iterator<ItemState> iter = stateMgr.getDescendantTransientItemStates((NodeId) id);
            while (iter.hasNext()) {
                transientState = iter.next();
                switch (transientState.getStatus()) {
                    case ItemState.STATUS_STALE_MODIFIED:
                    case ItemState.STATUS_STALE_DESTROYED:
                    case ItemState.STATUS_NEW:
                    case ItemState.STATUS_EXISTING_MODIFIED:
                        // add new or modified state to the list
                        list.add(transientState);
                        break;

                    default:
                        log.debug("unexpected state status (" + transientState.getStatus() + ")");
                        // ignore
                        break;
                }
            }
        }

        // process list of 'new', 'modified' or 'stale' transient states
        for (ItemState state : list) {
            // dispose the transient state, it is no longer used;
            // this will indirectly (through stateDiscarded listener method)
            // either restore or permanently invalidate the wrapping Item instances
            stateMgr.disposeTransientItemState(state);
        }

        if (isNode()) {
            // discard all transient descendants in the attic (i.e. those marked
            // as 'removed'); this will resurrect the removed items
            Iterator<ItemState> iter = stateMgr.getDescendantTransientItemStatesInAttic((NodeId) id);
            while (iter.hasNext()) {
                transientState = iter.next();
                // dispose the transient state; this will indirectly (through
                // stateDiscarded listener method) resurrect the wrapping Item instances
                stateMgr.disposeTransientItemStateInAttic(transientState);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Item getAncestor(int degree)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check state of this instance
        sanityCheck();

        if (degree == 0) {
            return itemMgr.getRootNode();
        }

        try {
            // Path.getAncestor requires relative degree, i.e. we need
            // to convert absolute to relative ancestor degree
            Path path = getPrimaryPath();
            int relDegree = path.getAncestorCount() - degree;
            if (relDegree < 0) {
                throw new ItemNotFoundException();
            }
            // shortcut
            if (relDegree == 0) {
                return this;
            }
            Path ancestorPath = path.getAncestor(relDegree);
            return itemMgr.getNode(ancestorPath);
        } catch (PathNotFoundException pnfe) {
            throw new ItemNotFoundException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getPath() throws RepositoryException {
        // check state of this instance
        sanityCheck();
        return session.getJCRPath(getPrimaryPath());
    }

    /**
     * {@inheritDoc}
     */
    public int getDepth() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        final ItemState state = getItemState();
        if (state.getParentId() == null) {
            // shortcut
            return 0;
        }
        return session.getHierarchyManager().getDepth(id);
    }

    /**
     * Returns the session associated with this item.
     * <p>
     * Since Jackrabbit 1.4 it is safe to use this method regardless
     * of item state.
     *
     * @see <a href="http://issues.apache.org/jira/browse/JCR-911">Issue JCR-911</a>
     * @return current session
     */
    public Session getSession() {
        return session;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSame(Item otherItem) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        if (this == otherItem) {
            return true;
        }
        if (otherItem instanceof ItemImpl) {
            ItemImpl other = (ItemImpl) otherItem;
            return id.equals(other.id)
                    && session.getWorkspace().getName().equals(
                            other.getSession().getWorkspace().getName());
        }
        return false;
    }

    //--------------------------------------------------------------< Object >

    /**
     * Returns the({@link #safeGetJCRPath() safe}) path of this item for use
     * in diagnostic output.
     *
     * @return "/path/to/item"
     */
    public String toString() {
        return safeGetJCRPath();
    }

}
