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

import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropertyDefinitionImpl;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.state.StaleItemStateException;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.version.VersionManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.uuid.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * <code>ItemImpl</code> implements the <code>Item</code> interface.
 */
public abstract class ItemImpl implements Item, ItemStateListener {

    private static Logger log = LoggerFactory.getLogger(ItemImpl.class);

    protected static final int STATUS_NORMAL = 0;
    protected static final int STATUS_MODIFIED = 1;
    protected static final int STATUS_DESTROYED = 2;
    protected static final int STATUS_INVALIDATED = 3;

    protected int status;

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
     * <code>ItemState</code> associated with this <code>Item</code>
     */
    protected ItemState state;

    /**
     * <code>ItemManager</code> that created this <code>Item</code>
     */
    protected final ItemManager itemMgr;

    /**
     * <code>SessionItemStateManager</code> associated with this <code>Item</code>
     */
    protected final SessionItemStateManager stateMgr;

    /**
     * Listeners (weak references)
     */
    protected final Map listeners =
            Collections.synchronizedMap(new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK));

    /**
     * Package private constructor.
     *
     * @param itemMgr   the <code>ItemManager</code> that created this <code>Item</code>
     * @param session   the <code>Session</code> through which this <code>Item</code> is acquired
     * @param id        id of this <code>Item</code>
     * @param state     state associated with this <code>Item</code>
     * @param listeners listeners on life cylce changes of this <code>ItemImpl</code>
     */
    ItemImpl(ItemManager itemMgr, SessionImpl session, ItemId id, ItemState state,
             ItemLifeCycleListener[] listeners) {
        this.session = session;
        rep = (RepositoryImpl) session.getRepository();
        stateMgr = session.getItemStateManager();
        this.id = id;
        this.itemMgr = itemMgr;
        this.state = state;
        status = STATUS_NORMAL;

        if (listeners != null) {
            for (int i = 0; i < listeners.length; i++) {
                addLifeCycleListener(listeners[i]);
            }
        }
        notifyCreated();
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
        if (status == STATUS_DESTROYED || status == STATUS_INVALIDATED) {
            throw new InvalidItemStateException(id + ": the item does not exist anymore");
        }
    }

    protected boolean isTransient() {
        return state.isTransient();
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
            status = STATUS_INVALIDATED;
            // notify the listeners that this instance has been
            // temporarily invalidated
            notifyInvalidated();
        }
    }

    /**
     * Returns the item-state associated with this <code>Item</code>.
     *
     * @return state associated with this <code>Item</code>
     */
    ItemState getItemState() {
        return state;
    }

    /**
     * Notify the listeners that this instance has been discarded
     * (i.e. it has been temporarily rendered 'invalid').
     */
    private void notifyCreated() {
        // copy listeners to array to avoid ConcurrentModificationException
        ItemLifeCycleListener[] la =
                (ItemLifeCycleListener[]) listeners.values().toArray(
                        new ItemLifeCycleListener[listeners.size()]);
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].itemCreated(this);
            }
        }
    }

    /**
     * Notify the listeners that this instance has been invalidated
     * (i.e. it has been temporarily rendered 'invalid').
     */
    protected void notifyInvalidated() {
        // copy listeners to array to avoid ConcurrentModificationException
        ItemLifeCycleListener[] la =
                (ItemLifeCycleListener[]) listeners.values().toArray(
                        new ItemLifeCycleListener[listeners.size()]);
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].itemInvalidated(id, this);
            }
        }
    }

    /**
     * Notify the listeners that this instance has been destroyed
     * (i.e. it has been permanently rendered 'invalid').
     */
    protected void notifyDestroyed() {
        // copy listeners to array to avoid ConcurrentModificationException
        ItemLifeCycleListener[] la =
                (ItemLifeCycleListener[]) listeners.values().toArray(
                        new ItemLifeCycleListener[listeners.size()]);
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].itemDestroyed(id, this);
            }
        }
    }

    /**
     * Add an <code>ItemLifeCycleListener</code>
     *
     * @param listener the new listener to be informed on life cycle changes
     */
    void addLifeCycleListener(ItemLifeCycleListener listener) {
        if (!listeners.containsKey(listener)) {
            listeners.put(listener, listener);
        }
    }

    /**
     * Remove an <code>ItemLifeCycleListener</code>
     *
     * @param listener an existing listener
     */
    void removeLifeCycleListener(ItemLifeCycleListener listener) {
        listeners.remove(listener);
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
     * within the scope of <code>this.{@link #save()}</code>.
     *
     * @return list of transient item states
     * @throws InvalidItemStateException
     * @throws RepositoryException
     */
    private Collection getTransientStates()
            throws InvalidItemStateException, RepositoryException {
        // list of transient states that should be persisted
        ArrayList dirty = new ArrayList();
        ItemState transientState;

        // fail-fast test: check status of this item's state
        if (isTransient()) {
            switch (state.getStatus()) {
                case ItemState.STATUS_EXISTING_MODIFIED:
                    // add this item's state to the list
                    dirty.add(state);
                    break;

                case ItemState.STATUS_NEW:
                    {
                        String msg = safeGetJCRPath()
                                + ": cannot save a new item.";
                        log.debug(msg);
                        throw new RepositoryException(msg);
                    }

                case ItemState.STATUS_STALE_MODIFIED:
                    {
                        String msg = safeGetJCRPath()
                                + ": the item cannot be saved because it has been modified externally.";
                        log.debug(msg);
                        throw new InvalidItemStateException(msg);
                    }

                case ItemState.STATUS_STALE_DESTROYED:
                    {
                        String msg = safeGetJCRPath()
                                + ": the item cannot be saved because it has been deleted externally.";
                        log.debug(msg);
                        throw new InvalidItemStateException(msg);
                    }

                case ItemState.STATUS_UNDEFINED:
                    {
                        String msg = safeGetJCRPath()
                                + ": the item cannot be saved; it seems to have been removed externally.";
                        log.debug(msg);
                        throw new InvalidItemStateException(msg);
                    }

                default:
                    log.debug("unexpected state status (" + state.getStatus() + ")");
                    // ignore
                    break;
            }
        }

        if (isNode()) {
            // build list of 'new' or 'modified' descendants
            Iterator iter = stateMgr.getDescendantTransientItemStates((NodeId) id);
            while (iter.hasNext()) {
                transientState = (ItemState) iter.next();
                // fail-fast test: check status of transient state
                switch (transientState.getStatus()) {
                    case ItemState.STATUS_NEW:
                    case ItemState.STATUS_EXISTING_MODIFIED:
                        // add modified state to the list
                        dirty.add(transientState);
                        break;

                    case ItemState.STATUS_STALE_MODIFIED:
                        {
                            String msg = transientState.getId()
                                    + ": the item cannot be saved because it has been modified externally.";
                            log.debug(msg);
                            throw new InvalidItemStateException(msg);
                        }

                    case ItemState.STATUS_STALE_DESTROYED:
                        {
                            String msg = transientState.getId()
                                    + ": the item cannot be saved because it has been deleted externally.";
                            log.debug(msg);
                            throw new InvalidItemStateException(msg);
                        }

                    case ItemState.STATUS_UNDEFINED:
                        {
                            String msg = safeGetJCRPath()
                                    + ": the item cannot be saved; it seems to have been removed externally.";
                            log.debug(msg);
                            throw new InvalidItemStateException(msg);
                        }

                    default:
                        log.debug("unexpected state status (" + transientState.getStatus() + ")");
                        // ignore
                        break;
                }
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
    private Collection getRemovedStates()
            throws InvalidItemStateException, RepositoryException {
        ArrayList removed = new ArrayList();
        ItemState transientState;

        if (isNode()) {
            Iterator iter = stateMgr.getDescendantTransientItemStatesInAttic((NodeId) id);
            while (iter.hasNext()) {
                transientState = (ItemState) iter.next();
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

    private void validateTransientItems(Iterator dirtyIter, Iterator removedIter)
            throws AccessDeniedException, ConstraintViolationException,
            RepositoryException {
        /**
         * the following validations/checks are performed on transient items:
         *
         * for every transient item:
         * - if it is 'modified' check the WRITE permission
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

        AccessManager accessMgr = session.getAccessManager();
        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        ItemValidator validator = new ItemValidator(ntMgr.getNodeTypeRegistry(),
                session.getHierarchyManager(), session);
        // walk through list of dirty transient items and validate each
        while (dirtyIter.hasNext()) {
            ItemState itemState = (ItemState) dirtyIter.next();

            if (itemState.getStatus() != ItemState.STATUS_NEW) {
                // transient item is not 'new', therefore it has to be 'modified'

                // check WRITE permission
                ItemId id = itemState.getId();
                if (!accessMgr.isGranted(id, AccessManager.WRITE)) {
                    String msg = itemMgr.safeGetJCRPath(id)
                            + ": not allowed to modify item";
                    log.debug(msg);
                    throw new AccessDeniedException(msg);
                }
            }

            if (itemState.isNode()) {
                // the transient item is a node
                NodeState nodeState = (NodeState) itemState;
                ItemId id = nodeState.getNodeId();
                NodeDefinition def = ntMgr.getNodeDefinition(nodeState.getDefinitionId());
                // primary type
                NodeTypeImpl pnt = ntMgr.getNodeType(nodeState.getNodeTypeName());
                // effective node type (primary type incl. mixins)
                EffectiveNodeType ent = validator.getEffectiveNodeType(nodeState);
                /**
                 * if the transient node was added (i.e. if it is 'new') or if
                 * its primary type has changed, check its node type against the
                 * required node type in its definition
                 */
                if (nodeState.getStatus() == ItemState.STATUS_NEW
                        || !nodeState.getNodeTypeName().equals(
                            ((NodeState) nodeState.getOverlayedState()).getNodeTypeName())) {
                    NodeType[] nta = def.getRequiredPrimaryTypes();
                    for (int i = 0; i < nta.length; i++) {
                        NodeTypeImpl ntReq = (NodeTypeImpl) nta[i];
                        if (!(pnt.getQName().equals(ntReq.getQName())
                                || pnt.isDerivedFrom(ntReq.getQName()))) {
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
                PropDef[] pda = ent.getMandatoryPropDefs();
                for (int i = 0; i < pda.length; i++) {
                    PropDef pd = pda[i];
                    if (pd.getDeclaringNodeType().equals(NameConstants.MIX_VERSIONABLE)) {
                        /**
                         * todo FIXME workaround for mix:versionable:
                         * the mandatory properties are initialized at a
                         * later stage and might not exist yet
                         */
                        continue;
                    }
                    if (!nodeState.hasPropertyName(pd.getName())) {
                        String msg = itemMgr.safeGetJCRPath(id)
                                + ": mandatory property " + pd.getName()
                                + " does not exist";
                        log.debug(msg);
                        throw new ConstraintViolationException(msg);
                    }
                }
                // mandatory child nodes
                NodeDef[] cnda = ent.getMandatoryNodeDefs();
                for (int i = 0; i < cnda.length; i++) {
                    NodeDef cnd = cnda[i];
                    if (!nodeState.hasChildNodeEntry(cnd.getName())) {
                        String msg = itemMgr.safeGetJCRPath(id)
                                + ": mandatory child node " + cnd.getName()
                                + " does not exist";
                        log.debug(msg);
                        throw new ConstraintViolationException(msg);
                    }
                }
            } else {
                // the transient item is a property
                PropertyState propState = (PropertyState) itemState;
                ItemId propId = propState.getPropertyId();
                PropertyDefinitionImpl def =
                        ntMgr.getPropertyDefinition(propState.getDefinitionId());

                /**
                 * check value constraints
                 * (no need to check value constraints of protected properties
                 * as those are set by the implementation only, i.e. they
                 * cannot be set by the user through the api)
                 */
                if (!def.isProtected()) {
                    String[] constraints = def.getValueConstraints();
                    if (constraints != null) {
                        InternalValue[] values = propState.getValues();
                        try {
                            EffectiveNodeType.checkSetPropertyValueConstraints(
                                    def.unwrap(), values);
                        } catch (RepositoryException e) {
                            // repack exception for providing verboser error message
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
                                && def.getRequiredType() == PropertyType.REFERENCE) {
                            for (int i = 0; i < values.length; i++) {
                                boolean satisfied = false;
                                String constraintViolationMsg = null;
                                try {
                                    UUID targetUUID = values[i].getUUID();
                                    Node targetNode = session.getNodeByUUID(targetUUID);
                                    /**
                                     * constraints are OR-ed, i.e. at least one
                                     * has to be satisfied
                                     */
                                    for (int j = 0; j < constraints.length; j++) {
                                        /**
                                         * a REFERENCE value constraint specifies
                                         * the name of the required node type of
                                         * the target node
                                         */
                                        String ntName = constraints[j];
                                        if (targetNode.isNodeType(ntName)) {
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
                                            + ": failed to check REFERENCE value constraint";
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
        while (removedIter.hasNext()) {
            ItemState itemState = (ItemState) removedIter.next();
            ItemId id = itemState.getId();
            // check REMOVE permission
            if (!accessMgr.isGranted(id, AccessManager.REMOVE)) {
                String msg = itemMgr.safeGetJCRPath(id)
                        + ": not allowed to remove item";
                log.debug(msg);
                throw new AccessDeniedException(msg);
            }
        }
    }

    private void removeTransientItems(Iterator iter) {

        /**
         * walk through list of transient items marked 'removed' and
         * definitively remove each one
         */
        while (iter.hasNext()) {
            ItemState transientState = (ItemState) iter.next();
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

    private void persistTransientItems(Iterator iter)
            throws RepositoryException {

        // walk through list of transient items and persist each one
        while (iter.hasNext()) {
            ItemState itemState = (ItemState) iter.next();
            ItemImpl item = itemMgr.getItem(itemState);
            // persist state of transient item
            item.makePersistent();
        }
    }

    private void restoreTransientItems(Iterator iter) {

        // walk through list of transient states and reapply transient changes
        while (iter.hasNext()) {
            ItemState itemState = (ItemState) iter.next();
            ItemId id = itemState.getId();
            ItemImpl item;

            try {
                if (stateMgr.isItemStateInAttic(id)) {
                    // If an item has been removed and then again created, the
                    // item is lost after persistTransientItems() and the
                    // TransientItemStateManager will bark because of a deleted
                    // state in its attic. We therefore have to forge a new item
                    // instance ourself.
                    if (itemState.isNode()) {
                        item = itemMgr.createNodeInstance((NodeState) itemState);
                    } else {
                        item = itemMgr.createPropertyInstance((PropertyState) itemState);
                    }
                    itemState.setStatus(ItemState.STATUS_NEW);
                } else {
                    try {
                        item = itemMgr.getItem(id);
                    } catch (ItemNotFoundException infe) {
                        // itemState probably represents a 'new' item and the
                        // ItemImpl instance wrapping it has already been gc'ed;
                        // we have to re-create the ItemImpl instance
                        if (itemState.isNode()) {
                            item = itemMgr.createNodeInstance((NodeState) itemState);
                        } else {
                            item = itemMgr.createPropertyInstance((PropertyState) itemState);
                        }
                        itemState.setStatus(ItemState.STATUS_NEW);
                    }
                }
                if (!item.isTransient()) {
                    // reapply transient changes (i.e. undo effect of item.makePersistent())
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
     * Initializes the version history of all new nodes of node type
     * <code>mix:versionable</code>.
     * <p/>
     * Called by {@link #save()}.
     *
     * @param iter
     * @return true if this call generated new transient state; otherwise false
     * @throws RepositoryException
     */
    private boolean initVersionHistories(Iterator iter) throws RepositoryException {
        // walk through list of transient items and search for new versionable nodes
        boolean createdTransientState = false;
        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        ItemValidator validator = new ItemValidator(ntMgr.getNodeTypeRegistry(),
                session.getHierarchyManager(), session);
        while (iter.hasNext()) {
            ItemState itemState = (ItemState) iter.next();
            if (itemState.isNode()) {
                NodeState nodeState = (NodeState) itemState;
                EffectiveNodeType nt = validator.getEffectiveNodeType(nodeState);
                if (nt.includesNodeType(NameConstants.MIX_VERSIONABLE)) {
                    if (!nodeState.hasPropertyName(NameConstants.JCR_VERSIONHISTORY)) {
                        NodeImpl node = (NodeImpl) itemMgr.getItem(itemState.getId());
                        VersionManager vMgr = session.getVersionManager();
                        /**
                         * check if there's already a version history for that
                         * node; this would e.g. be the case if a versionable
                         * node had been exported, removed and re-imported with
                         * either IMPORT_UUID_COLLISION_REMOVE_EXISTING or
                         * IMPORT_UUID_COLLISION_REPLACE_EXISTING;
                         * otherwise create a new version history
                         */
                        VersionHistory vh = vMgr.getVersionHistory(session, nodeState);
                        if (vh == null) {
                            vh = vMgr.createVersionHistory(session, nodeState);
                        }
                        node.internalSetProperty(NameConstants.JCR_VERSIONHISTORY, InternalValue.create(new UUID(vh.getUUID())));
                        node.internalSetProperty(NameConstants.JCR_BASEVERSION, InternalValue.create(new UUID(vh.getRootVersion().getUUID())));
                        node.internalSetProperty(NameConstants.JCR_ISCHECKEDOUT, InternalValue.create(true));
                        node.internalSetProperty(NameConstants.JCR_PREDECESSORS,
                                new InternalValue[]{InternalValue.create(new UUID(vh.getRootVersion().getUUID()))});
                        createdTransientState = true;
                    }
                }
            }
        }
        return createdTransientState;
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

        Path.Element thisName = getPrimaryPath().getNameElement();

        // check if protected
        if (isNode()) {
            NodeImpl node = (NodeImpl) this;
            // check if this is the root node
            if (node.getDepth() == 0) {
                String msg = safeGetJCRPath() + ": cannot remove root node";
                log.debug(msg);
                throw new RepositoryException(msg);
            }

            NodeDefinition def = node.getDefinition();
            // check protected flag
            if (!noChecks && def.isProtected()) {
                String msg = safeGetJCRPath() + ": cannot remove a protected node";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        } else {
            PropertyImpl prop = (PropertyImpl) this;
            PropertyDefinition def = prop.getDefinition();
            // check protected flag
            if (!noChecks && def.isProtected()) {
                String msg = safeGetJCRPath() + ": cannot remove a protected property";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }

        NodeImpl parentNode = (NodeImpl) getParent();

        // verify that parent node is checked-out
        if (!noChecks && !parentNode.internalIsCheckedOut()) {
            String msg = parentNode.safeGetJCRPath() + ": cannot remove a child of a checked-in node";
            log.debug(msg);
            throw new VersionException(msg);
        }

        // check protected flag of parent node
        if (!noChecks && parentNode.getDefinition().isProtected()) {
            String msg = parentNode.safeGetJCRPath() + ": cannot remove a child of a protected node";
            log.debug(msg);
            throw new ConstraintViolationException(msg);
        }

        // check lock status
        if (!noChecks) {
            parentNode.checkLock();
        }

        // delegate the removal of the child item to the parent node
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

    //----------------------------------------------------< ItemStateListener >
    /**
     * {@inheritDoc}
     */
    public void stateCreated(ItemState created) {
        status = STATUS_NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    public void stateDestroyed(ItemState destroyed) {
        if (state == destroyed) {
            // set state of this instance to 'destroyed'
            status = STATUS_DESTROYED;
            // dispose state
            if (state == destroyed) {
                state = null;
            }
            /**
             * notify the listeners that this instance has been
             * permanently invalidated
             */
            notifyDestroyed();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stateModified(ItemState modified) {
        if (state == modified) {
            status = STATUS_MODIFIED;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stateDiscarded(ItemState discarded) {
        if (state == discarded) {
            /**
             * the state of this item has been discarded, probably as a result
             * of calling Item.refresh(false) or ItemImpl.setRemoved()
             */
            if (isTransient()) {
                switch (state.getStatus()) {
                    /**
                     * persistent item that has been transiently removed
                     */
                    case ItemState.STATUS_EXISTING_REMOVED:
                        /**
                         * persistent item that has been transiently modified
                         */
                    case ItemState.STATUS_EXISTING_MODIFIED:
                        /**
                         * persistent item that has been transiently modified or removed
                         * and the underlying persistent state has been externally
                         * modified since the transient modification/removal.
                         */
                    case ItemState.STATUS_STALE_MODIFIED:
                        ItemState persistentState = state.getOverlayedState();
                        /**
                         * the state is a transient wrapper for the underlying
                         * persistent state, therefore restore the
                         * persistent state and resurrect this item instance
                         * if necessary
                         */
                        stateMgr.disconnectTransientItemState(state);
                        state = persistentState;

                        return;

                        /**
                         * persistent item that has been transiently modified or removed
                         * and the underlying persistent state has been externally
                         * destroyed since the transient modification/removal.
                         */
                    case ItemState.STATUS_STALE_DESTROYED:
                        /**
                         * first notify the listeners that this instance has been
                         * permanently invalidated
                         */
                        notifyDestroyed();
                        // now set state of this instance to 'destroyed'
                        status = STATUS_DESTROYED;
                        state = null;
                        return;

                        /**
                         * new item that has been transiently added
                         */
                    case ItemState.STATUS_NEW:
                        /**
                         * first notify the listeners that this instance has been
                         * permanently invalidated
                         */
                        notifyDestroyed();
                        // now set state of this instance to 'destroyed'
                        status = STATUS_DESTROYED;
                        // finally dispose state
                        state = null;
                        return;
                }
            }

            /**
             * first notify the listeners that this instance has been
             * invalidated
             */
            notifyInvalidated();
            // now render this instance 'invalid'
            status = STATUS_INVALIDATED;
        }
    }

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
        return state.isTransient() && state.getOverlayedState() == null;
    }

    /**
     * checks if this item is new. running outside of transactions, this
     * is the same as {@link #isNew()} but within a transaction an item can
     * be saved but not yet persisted.
     */
    protected boolean isTransactionalNew() {
        return state.getStatus() == ItemState.STATUS_NEW;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isModified() {
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
            Collection dirty = getTransientStates();
            if (dirty.size() == 0) {
                // no transient items, nothing to do here
                return;
            }

            /**
             * build list of transient descendants in the attic
             * (i.e. those marked as 'removed')
             */
            Collection removed = getRemovedStates();

            /**
             * build set of item id's which are within the scope of
             * (i.e. affected by) this save operation
             */
            Set affectedIds = new HashSet(dirty.size() + removed.size());
            for (Iterator it =
                    new IteratorChain(dirty.iterator(), removed.iterator());
                 it.hasNext();) {
                affectedIds.add(((ItemState) it.next()).getId());
            }

            /**
             * make sure that this save operation is totally 'self-contained'
             * and independant; items within the scope of this save operation
             * must not have 'external' dependencies;
             * (e.g. moving a node requires that the target node including both
             * old and new parents are saved)
             */
            for (Iterator it =
                    new IteratorChain(dirty.iterator(), removed.iterator());
                 it.hasNext();) {
                ItemState transientState = (ItemState) it.next();
                if (transientState.isNode()) {
                    NodeState nodeState = (NodeState) transientState;
                    Set dependentIDs = new HashSet();
                    if (nodeState.hasOverlayedState()) {
                        NodeState overlayedState =
                                (NodeState) nodeState.getOverlayedState();
                        NodeId oldParentId = overlayedState.getParentId();
                        NodeId newParentId = nodeState.getParentId();
                        if (oldParentId != null) {
                            if (newParentId == null) {
                                // node has been removed, add old parent
                                // to dependencies
                                dependentIDs.add(oldParentId);
                            } else {
                                if (!oldParentId.equals(newParentId)) {
                                    // node has been moved to a new location,
                                    // add old and new parent to dependencies
                                    dependentIDs.add(oldParentId);
                                    dependentIDs.add(newParentId);
                                } else {
                                    // parent id hasn't changed, check whether
                                    // the node has been renamed (JCR-1034)
                                    if (!affectedIds.contains(newParentId)
                                            && stateMgr.hasTransientItemState(newParentId)) {
                                        try {
                                            NodeState parent = (NodeState) stateMgr.getTransientItemState(newParentId);
                                            // check parent's renamed child node entries
                                            for (Iterator cneIt =
                                                    parent.getRenamedChildNodeEntries().iterator();
                                                 cneIt.hasNext();) {
                                                NodeState.ChildNodeEntry cne =
                                                        (NodeState.ChildNodeEntry) cneIt.next();
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
                    for (Iterator cneIt =
                            nodeState.getRemovedChildNodeEntries().iterator();
                         cneIt.hasNext();) {
                        NodeState.ChildNodeEntry cne =
                                (NodeState.ChildNodeEntry) cneIt.next();
                        dependentIDs.add(cne.getId());
                    }
                    // added child node entries
                    for (Iterator cneIt =
                            nodeState.getAddedChildNodeEntries().iterator();
                         cneIt.hasNext();) {
                        NodeState.ChildNodeEntry cne =
                                (NodeState.ChildNodeEntry) cneIt.next();
                        dependentIDs.add(cne.getId());
                    }

                    // now walk through dependencies and check whether they
                    // are within the scope of this save operation
                    Iterator depIt = dependentIDs.iterator();
                    while (depIt.hasNext()) {
                        NodeId id = (NodeId) depIt.next();
                        if (!affectedIds.contains(id)) {
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

            /**
             * validate access and node type constraints
             * (this will also validate child removals)
             */
            validateTransientItems(dirty.iterator(), removed.iterator());

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
                removeTransientItems(removed.iterator());

                // initialize version histories for new nodes (might generate new transient state)
                if (initVersionHistories(dirty.iterator())) {
                    // re-build the list of transient states because the previous call
                    // generated new transient state
                    dirty = getTransientStates();
                }

                // process 'new' or 'modified' transient states
                persistTransientItems(dirty.iterator());

                // dispose the transient states marked 'new' or 'modified'
                // at this point item state data is pushed down one level,
                // node instances are disconnected from the transient
                // item state and connected to the 'overlayed' item state.
                // transient item states must be removed now. otherwise
                // the session item state provider will return an orphaned
                // item state which is not referenced by any node instance.
                for (Iterator it = dirty.iterator(); it.hasNext();) {
                    ItemState transientState = (ItemState) it.next();
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
                String msg = safeGetJCRPath() + ": unable to update item.";
                log.debug(msg);
                throw new RepositoryException(msg, e);
            } finally {
                if (!succeeded) {
                    // update operation failed, cancel all modifications
                    stateMgr.cancel();

                    // JCR-288: if an exception has been thrown during
                    // update() the transient changes have already been
                    // applied by persistTransientItems() and we need to
                    // restore transient state, i.e. undo the effect of
                    // persistTransientItems()
                    restoreTransientItems(dirty.iterator());
                }
            }

            // now it is safe to dispose the transient states:
            // dispose the transient states marked 'removed'.
            // item states in attic are removed after store, because
            // the observation mechanism needs to build paths of removed
            // items in store().
            for (Iterator it = removed.iterator(); it.hasNext();) {
                ItemState transientState = (ItemState) it.next();
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
        ArrayList list = new ArrayList();
        ItemState transientState;

        // check status of this item's state
        if (isTransient()) {
            transientState = state;
            switch (transientState.getStatus()) {
                case ItemState.STATUS_STALE_MODIFIED:
                case ItemState.STATUS_STALE_DESTROYED:
                case ItemState.STATUS_EXISTING_MODIFIED:
                    // add this item's state to the list
                    list.add(transientState);
                    break;

                case ItemState.STATUS_NEW:
                    {
                        String msg = safeGetJCRPath() + ": cannot refresh a new item.";
                        log.debug(msg);
                        throw new RepositoryException(msg);
                    }

                default:
                    log.debug("unexpected state status (" + transientState.getStatus() + ")");
                    // ignore
                    break;
            }
        }

        if (isNode()) {
            // build list of 'new', 'modified' or 'stale' descendants
            Iterator iter = stateMgr.getDescendantTransientItemStates((NodeId) id);
            while (iter.hasNext()) {
                transientState = (ItemState) iter.next();
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
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            transientState = (ItemState) iter.next();
            // dispose the transient state, it is no longer used;
            // this will indirectly (through stateDiscarded listener method)
            // either restore or permanently invalidate the wrapping Item instances
            stateMgr.disposeTransientItemState(transientState);
        }

        if (isNode()) {
            // discard all transient descendants in the attic (i.e. those marked
            // as 'removed'); this will resurrect the removed items
            iter = stateMgr.getDescendantTransientItemStatesInAttic((NodeId) id);
            while (iter.hasNext()) {
                transientState = (ItemState) iter.next();
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
     * @see http://issues.apache.org/jira/browse/JCR-911
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
}
