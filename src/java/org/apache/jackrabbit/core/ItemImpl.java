/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.commons.collections.ReferenceMap;
import org.apache.jackrabbit.core.nodetype.ChildNodeDef;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropertyDefImpl;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.log4j.Logger;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
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
import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDef;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <code>ItemImpl</code> implements the <code>Item</code> interface.
 */
public abstract class ItemImpl implements Item, ItemStateListener, Constants {

    private static Logger log = Logger.getLogger(ItemImpl.class);

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

        // add this item as listener to events of the underlying state object
        this.state.addListener(this);
    }

    protected void finalize() throws Throwable {
        if (state != null) {
            try {
                state.removeListener(this);
            } catch (Throwable t) {
                // ignore
            }
        }
        super.finalize();
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
        switch (status) {
            case STATUS_NORMAL:
            case STATUS_MODIFIED:
                return;

            case STATUS_DESTROYED:
            case STATUS_INVALIDATED:
                throw new InvalidItemStateException(id + ": the item does not exist anymore");
        }
    }

    protected boolean isTransient() {
        return state.isTransient();
    }

    protected abstract ItemState getOrCreateTransientItemState() throws RepositoryException;

    protected abstract void makePersistent();

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
        ItemLifeCycleListener[] la = new ItemLifeCycleListener[listeners.size()];
        Iterator iter = listeners.values().iterator();
        int cnt = 0;
        while (iter.hasNext()) {
            la[cnt++] = (ItemLifeCycleListener) iter.next();
        }
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
        ItemLifeCycleListener[] la = new ItemLifeCycleListener[listeners.size()];
        Iterator iter = listeners.values().iterator();
        int cnt = 0;
        while (iter.hasNext()) {
            la[cnt++] = (ItemLifeCycleListener) iter.next();
        }
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
        ItemLifeCycleListener[] la = new ItemLifeCycleListener[listeners.size()];
        Iterator iter = listeners.values().iterator();
        int cnt = 0;
        while (iter.hasNext()) {
            la[cnt++] = (ItemLifeCycleListener) iter.next();
        }
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
    ItemId getId() {
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

        // check status of this item's state
        if (isTransient()) {
            switch (state.getStatus()) {
                case ItemState.STATUS_EXISTING_MODIFIED:
                    // add this item's state to the list
                    dirty.add(state);
                    break;

                case ItemState.STATUS_NEW:
                    {
                        String msg = safeGetJCRPath() + ": cannot save a new item.";
                        log.debug(msg);
                        throw new RepositoryException(msg);
                    }

                case ItemState.STATUS_STALE_MODIFIED:
                    {
                        String msg = safeGetJCRPath() + ": the item cannot be saved because it has been modified externally.";
                        log.debug(msg);
                        throw new InvalidItemStateException(msg);
                    }

                case ItemState.STATUS_STALE_DESTROYED:
                    {
                        String msg = safeGetJCRPath() + ": the item cannot be saved because it has been deleted externally.";
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
            // build list of 'new' or 'modified' descendents
            Iterator iter = stateMgr.getDescendantTransientItemStates(id);
            while (iter.hasNext()) {
                transientState = (ItemState) iter.next();
                switch (transientState.getStatus()) {
                    case ItemState.STATUS_NEW:
                    case ItemState.STATUS_EXISTING_MODIFIED:
                        // add modified state to the list
                        dirty.add(transientState);
                        break;

                    case ItemState.STATUS_STALE_MODIFIED:
                        {
                            String msg = transientState.getId() + ": the item cannot be saved because it has been modified externally.";
                            log.debug(msg);
                            throw new InvalidItemStateException(msg);
                        }

                    case ItemState.STATUS_STALE_DESTROYED:
                        {
                            String msg = transientState.getId() + ": the item cannot be saved because it has been deleted externally.";
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
            Iterator iter = stateMgr.getDescendantTransientItemStatesInAttic(id);
            while (iter.hasNext()) {
                transientState = (ItemState) iter.next();
                // check if stale
                if (transientState.getStatus() == ItemState.STATUS_STALE_MODIFIED) {
                    String msg = transientState.getId() + ": the item cannot be removed because it has been modified externally.";
                    log.debug(msg);
                    throw new InvalidItemStateException(msg);
                }
                if (transientState.getStatus() == ItemState.STATUS_STALE_DESTROYED) {
                    String msg = transientState.getId() + ": the item cannot be removed because it has already been deleted externally.";
                    log.debug(msg);
                    throw new InvalidItemStateException(msg);
                }
                removed.add(transientState);
            }
        }
        return removed;
    }

    private void validateTransientItems(Iterator dirtyIter, Iterator removerIter)
            throws AccessDeniedException, ConstraintViolationException, RepositoryException {
        /**
         * the following validations/checks are performed on transient items:
         *
         * for every transient item:
         * - if it is 'modified' check the WRITE permission
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
         * Node.addMixin/removeMixin (for mixin changes on nodes)
         * and in Property.setValue (for properties to be modified).
         */

        AccessManager accessMgr = session.getAccessManager();
        // walk through list of dirty transient items and validate each
        while (dirtyIter.hasNext()) {
            ItemState itemState = (ItemState) dirtyIter.next();

            if (itemState.getStatus() != ItemState.STATUS_NEW) {
                // transient item is not 'new', therefore it has to be 'modified'

                // check WRITE permission
                ItemId id = itemState.getId();
                if (!accessMgr.isGranted(id, AccessManager.WRITE)) {
                    String msg = itemMgr.safeGetJCRPath(id) + ": not allowed to modify item";
                    log.debug(msg);
                    throw new AccessDeniedException(msg);
                }
            }

            if (itemState.isNode()) {
                // the transient item is a node
                NodeState nodeState = (NodeState) itemState;
                ItemId id = nodeState.getId();
                NodeImpl node = (NodeImpl) itemMgr.getItem(id);
                NodeDef def = node.getDefinition();
                // primary type
                NodeTypeImpl pnt = (NodeTypeImpl) node.getPrimaryNodeType();
                // effective node type (primary type incl. mixins)
                EffectiveNodeType ent = node.getEffectiveNodeType();
                /**
                 * if the transient node was added (i.e. if it is 'new'),
                 * check its node's node type against the required node type
                 * in its definition
                 */
                if (nodeState.getStatus() == ItemState.STATUS_NEW) {
                    NodeType[] nta = def.getRequiredPrimaryTypes();
                    for (int i = 0; i < nta.length; i++) {
                        NodeTypeImpl ntReq = (NodeTypeImpl) nta[i];
                        if (!(pnt.getQName().equals(ntReq.getQName())
                                || pnt.isDerivedFrom(ntReq.getQName()))) {
                            /**
                             * the transient node's primary node type does not
                             * satisfy the 'required primary types' constraint
                             */
                            String msg = node.safeGetJCRPath() + " must be of node type " + ntReq.getName();
                            log.debug(msg);
                            throw new ConstraintViolationException(msg);
                        }
                    }
                }

                // mandatory child properties
                PropDef[] pda = ent.getMandatoryPropDefs();
                for (int i = 0; i < pda.length; i++) {
                    PropDef pd = pda[i];
                    if (!nodeState.hasPropertyEntry(pd.getName())) {
                        String msg = node.safeGetJCRPath() + ": mandatory property " + pd.getName() + " does not exist";
                        log.debug(msg);
                        throw new ConstraintViolationException(msg);
                    }
                }
                // mandatory child nodes
                ChildNodeDef[] cnda = ent.getMandatoryNodeDefs();
                for (int i = 0; i < cnda.length; i++) {
                    ChildNodeDef cnd = cnda[i];
                    if (!nodeState.hasChildNodeEntry(cnd.getName())) {
                        String msg = node.safeGetJCRPath() + ": mandatory child node " + cnd.getName() + " does not exist";
                        log.debug(msg);
                        throw new ConstraintViolationException(msg);
                    }
                }
            } else {
                // the transient item is a property
                PropertyState propState = (PropertyState) itemState;
                ItemId propId = propState.getId();
                PropertyImpl prop = (PropertyImpl) itemMgr.getItem(propId);
                PropertyDefImpl def = (PropertyDefImpl) prop.getDefinition();

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
                            NodeTypeImpl.checkSetPropertyValueConstraints(def, values);
                        } catch (RepositoryException e) {
                            // repack exception for providing verboser error message
                            String msg = prop.safeGetJCRPath() + ": " + e.getMessage();
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
                                try {
                                    UUID targetUUID = (UUID) values[i].internalValue();
                                    Node targetNode = session.getNodeByUUID(targetUUID.toString());
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
                                } catch (RepositoryException re) {
                                    String msg = prop.safeGetJCRPath()
                                            + ": failed to check REFERENCE value constraint";
                                    log.debug(msg);
                                    throw new ConstraintViolationException(msg, re);
                                }
                                if (!satisfied) {
                                    String msg = prop.safeGetJCRPath()
                                            + ": does not satisfy the value constraint "
                                            + constraints[0];   // just report the 1st
                                    log.debug(msg);
                                    throw new ConstraintViolationException(msg);
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
        while (removerIter.hasNext()) {
            ItemState itemState = (ItemState) removerIter.next();
            ItemId id = itemState.getId();
            // check WRITE permission
            if (!accessMgr.isGranted(id, AccessManager.REMOVE)) {
                String msg = itemMgr.safeGetJCRPath(id) + ": not allowed to remove item";
                log.debug(msg);
                throw new AccessDeniedException(msg);
            }
        }
    }

    private Collection checkReferences(Iterator iterDirty, Iterator iterRemoved)
            throws ReferentialIntegrityException, RepositoryException {

        // map of target (node) id's and modified NodeReferences objects
        HashMap dirtyNodeRefs = new HashMap();

        // walk through dirty items and process REFERENCE properties:
        // 1. verify that target node exists
        // 2. update and collect the affected NodeReferences objects of the
        //    target nodes in the dirtyNodeRefs map
        while (iterDirty.hasNext()) {
            ItemState transientState = (ItemState) iterDirty.next();
            if (!transientState.isNode()) {
                PropertyState propState = (PropertyState) transientState;
                int type = propState.getType();
                if (propState.getStatus() == ItemState.STATUS_EXISTING_MODIFIED) {
                    // this is a modified property, check old type...
                    PropertyState oldPropState = (PropertyState) propState.getOverlayedState();
                    int oldType = oldPropState.getType();
                    if (oldType == PropertyType.REFERENCE) {
                        // this is a modified REFERENCE property:
                        // remove the 'reference' stored in the old value
                        InternalValue[] vals = oldPropState.getValues();
                        for (int i = 0; vals != null && i < vals.length; i++) {
                            String uuid = vals[i].toString();
                            NodeReferencesId id = new NodeReferencesId(uuid);
                            NodeReferences refs;
                            if (dirtyNodeRefs.containsKey(id)) {
                                refs = (NodeReferences) dirtyNodeRefs.get(id);
                            } else {
                                try {
                                    refs = stateMgr.getNodeReferences(id);
                                } catch (ItemStateException e) {
                                    String msg = itemMgr.safeGetJCRPath(id)
                                            + ": failed to load node references";
                                    log.debug(msg);
                                    throw new RepositoryException(msg, e);
                                }
                                dirtyNodeRefs.put(id, refs);
                            }
                            // remove reference from target node
                            refs.removeReference((PropertyId) propState.getId());
                        }
                    }
                }
                if (type == PropertyType.REFERENCE) {
                    // this is a modified REFERENCE property:
                    // add the 'reference' stored in the new value
                    InternalValue[] vals = propState.getValues();
                    for (int i = 0; vals != null && i < vals.length; i++) {
                        String uuid = vals[i].toString();
                        NodeReferencesId refsId = new NodeReferencesId(uuid);
                        NodeId targetId = new NodeId(uuid);
                        // verify that target exists
                        if (!itemMgr.itemExists(targetId)) {
                            String msg = itemMgr.safeGetJCRPath(propState.getId())
                                    + ": target node of REFERENCE property does not exist";
                            log.warn(msg);
                            throw new ReferentialIntegrityException(msg);
                        }
                        // target is a new (unsaved) node; make sure that it is
                        // within the scope of the current save operation
                        // (by veryfying that it is a descendant of 'this' item)
                        NodeImpl target = (NodeImpl) itemMgr.getItem(targetId);
                        if (target.isNew()) {
                            try {
                                if (!target.getPrimaryPath().isDescendantOf(getPrimaryPath())) {
                                    String msg = itemMgr.safeGetJCRPath(propState.getId())
                                            + ": target node of REFERENCE property is a new node and must therefore either be saved first or be within the scope of the current save operation.";
                                    log.warn(msg);
                                    throw new ReferentialIntegrityException(msg);
                                }
                            } catch (MalformedPathException mpe) {
                                // should never get here...
                                String msg = itemMgr.safeGetJCRPath(propState.getId())
                                        + ": failed to verify existence of target node";
                                log.debug(msg);
                                throw new RepositoryException(msg, mpe);
                            }
                        }
                        NodeReferences refs;
                        if (dirtyNodeRefs.containsKey(refsId)) {
                            refs = (NodeReferences) dirtyNodeRefs.get(refsId);
                        } else {
                            try {
                                refs = stateMgr.getNodeReferences(refsId);
                            } catch (ItemStateException e) {
                                String msg = itemMgr.safeGetJCRPath(targetId)
                                        + ": failed to load node references";
                                log.debug(msg);
                                throw new RepositoryException(msg, e);
                            }
                            dirtyNodeRefs.put(refsId, refs);
                        }
                        // add reference to target node
                        refs.addReference((PropertyId) propState.getId());
                    }
                }
            }
        }

        // walk through 'removed' items:
        // 1. build list of removed nodes
        // 2. process REFERENCE properties (update and collect the affected
        //    NodeReferences objects of the target nodes)
        ArrayList removedNodes = new ArrayList();
        while (iterRemoved.hasNext()) {
            ItemState transientState = (ItemState) iterRemoved.next();
            if (transientState.isNode()) {
                // removed node: collect for later processing
                removedNodes.add(transientState);
            } else {
                PropertyState propState = (PropertyState) transientState;
                if (propState.getType() == PropertyType.REFERENCE) {
                    // this is a removed REFERENCE property:
                    // remove the 'reference' stored in the value
                    InternalValue[] vals = propState.getValues();
                    for (int i = 0; i < vals.length; i++) {
                        String uuid = vals[i].toString();
                        NodeReferencesId id = new NodeReferencesId(uuid);
                        NodeReferences refs;
                        if (dirtyNodeRefs.containsKey(id)) {
                            refs = (NodeReferences) dirtyNodeRefs.get(id);
                        } else {
                            try {
                                refs = stateMgr.getNodeReferences(id);
                            } catch (ItemStateException e) {
                                String msg = itemMgr.safeGetJCRPath(id)
                                        + ": failed to load node references";
                                log.debug(msg);
                                throw new RepositoryException(msg, e);
                            }
                            dirtyNodeRefs.put(id, refs);
                        }
                        // remove reference to target node
                        refs.removeReference((PropertyId) propState.getId());
                    }
                }
            }
        }

        // now that all NodeReferences objects have been updated,
        // walk through 'removed' nodes and verify that no node that is still
        // being referenced, is removed
        Iterator iter = removedNodes.iterator();
        while (iter.hasNext()) {
            NodeState nodeState = (NodeState) iter.next();
            // check if node is referenced
            NodeReferencesId id = new NodeReferencesId(nodeState.getUUID());
            NodeReferences refs;
            if (dirtyNodeRefs.containsKey(id)) {
                refs = (NodeReferences) dirtyNodeRefs.get(id);
            } else {
                try {
                    refs = stateMgr.getNodeReferences(id);
                } catch (ItemStateException e) {
                    String msg = itemMgr.safeGetJCRPath(id)
                            + ": failed to load node references";
                    log.debug(msg);
                    throw new RepositoryException(msg, e);
                }
            }
            if (refs.hasReferences()) {
                String msg = nodeState.getId()
                        + ": the node cannot be removed because it is being referenced.";
                log.warn(msg);
                throw new ReferentialIntegrityException(msg);
            }
        }

        // return dirty NodeReferences objects
        return dirtyNodeRefs.values();
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
            ItemImpl item = itemMgr.getItem(itemState.getId());
            // persist state of transient item
            item.makePersistent();
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
        // todo consolidate version history creation code (currently in NodeImpl.addMixin & ItemImpl.initVersionHistories
        // walk through list of transient items and search for new versionable nodes
        boolean createdTransientState = false;
        while (iter.hasNext()) {
            ItemState itemState = (ItemState) iter.next();
            if (itemState.isNode()) {
                NodeImpl node = (NodeImpl) itemMgr.getItem(itemState.getId());
                if (node.isNodeType(MIX_VERSIONABLE)) {
                    if (!node.hasProperty(JCR_VERSIONHISTORY)) {
                        VersionHistory hist = session.getVersionManager().createVersionHistory(node);
                        node.internalSetProperty(JCR_VERSIONHISTORY, InternalValue.create(new UUID(hist.getUUID())));
                        node.internalSetProperty(JCR_BASEVERSION, InternalValue.create(new UUID(hist.getRootVersion().getUUID())));
                        node.internalSetProperty(JCR_ISCHECKEDOUT, InternalValue.create(true));
                        node.internalSetProperty(JCR_PREDECESSORS, new InternalValue[]{InternalValue.create(new UUID(hist.getRootVersion().getUUID()))});
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
     * Same as <code>{@link Item#getName()}</code> except that
     * this method returns a <code>QName</code> instead of a
     * <code>String</code>.
     *
     * @return the name of this item as <code>QName</code>
     * @throws RepositoryException if an error occurs.
     */
    public abstract QName getQName() throws RepositoryException;

    //----------------------------------------------------< ItemStateListener >
    /**
     * @see ItemStateListener#stateCreated
     */
    public void stateCreated(ItemState created) {
        status = STATUS_NORMAL;
    }

    /**
     * @see ItemStateListener#stateDestroyed
     */
    public void stateDestroyed(ItemState destroyed) {
        // underlying state has been permanently destroyed

        // set state of this instance to 'destroyed'
        status = STATUS_DESTROYED;
        // dispose state
        if (state == destroyed) {
            state.removeListener(this);
            state = null;
        }
        /**
         * notify the listeners that this instance has been
         * permanently invalidated
         */
        notifyDestroyed();
    }

    /**
     * @see ItemStateListener#stateModified
     */
    public void stateModified(ItemState modified) {
        status = STATUS_MODIFIED;
    }

    /**
     * @see ItemStateListener#stateDiscarded
     */
    public void stateDiscarded(ItemState discarded) {
        /**
         * the state of this item has been discarded, probably as a result
         * of calling Node.revert() or ItemImpl.setRemoved()
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
                    state.removeListener(this);
                    state = persistentState;
                    state.addListener(this);

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
                    // finally dispose state
                    state.removeListener(this);
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
                    state.removeListener(this);
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

    //-----------------------------------------------------------------< Item >
    /**
     * @see Item#accept
     */
    public abstract void accept(ItemVisitor visitor)
            throws RepositoryException;

    /**
     * @see Item#isNode
     */
    public abstract boolean isNode();

    /**
     * @see Item#getName
     */
    public abstract String getName() throws RepositoryException;

    /**
     * @see Item#getParent
     */
    public abstract Node getParent()
            throws ItemNotFoundException, AccessDeniedException, RepositoryException;

    /**
     * @see Item#isNew
     */
    public boolean isNew() {
        return state.getStatus() == ItemState.STATUS_NEW;
    }

    /**
     * @see Item#isModified
     */
    public boolean isModified() {
        return state.isTransient() && state.getOverlayedState() != null;
    }

    /**
     * @see Item#remove
     */
    public void remove() throws VersionException, LockException, RepositoryException {
        internalRemove(false);
    }

    /**
     * @see Item#remove
     */
    protected void internalRemove(boolean noChecks)
            throws VersionException, LockException, RepositoryException {

        // check state of this instance
        sanityCheck();

        Path.PathElement thisName = getPrimaryPath().getNameElement();

        // check if protected
        if (isNode()) {
            NodeImpl node = (NodeImpl) this;
            // check if this is the repository root node
            if (node.isRepositoryRoot()) {
                String msg = safeGetJCRPath() + ": cannot remove root node";
                log.debug(msg);
                throw new RepositoryException(msg);
            }

            NodeDef def = node.getDefinition();
            // check protected flag
            if (!noChecks && def.isProtected()) {
                String msg = safeGetJCRPath() + ": cannot remove a protected node";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        } else {
            PropertyImpl prop = (PropertyImpl) this;
            PropertyDef def = prop.getDefinition();
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

        // delegate the removal of the child item to the parent node
        if (isNode()) {
            parentNode.removeChildNode(thisName.getName(), thisName.getIndex());
        } else {
            parentNode.removeChildProperty(thisName.getName());
        }
    }

    /**
     * @see Item#save
     */
    public void save()
            throws AccessDeniedException, ConstraintViolationException,
            InvalidItemStateException, ReferentialIntegrityException,
            VersionException, LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // synchronize on this session
        synchronized (session) {
            try {
                /**
                 * turn on temporary path caching for better performance
                 * (assuming that the paths won't change during this save() call)
                 */
                stateMgr.enablePathCaching(true);

                // build list of transient states that should be persisted
                Collection dirty = getTransientStates();
                if (dirty.size() == 0) {
                    // no transient items, nothing to do here
                    return;
                }

                ItemState transientState;

                /**
                 * check that parent node is also included in the dirty items list
                 * if dirty node was removed or added (adding/removing a parent/child
                 * link requires that both parent and child are saved)
                 */
                Iterator iter = dirty.iterator();
                while (iter.hasNext()) {
                    transientState = (ItemState) iter.next();
                    if (transientState.isNode()) {
                        NodeState nodeState = (NodeState) transientState;
                        ArrayList dirtyParents = new ArrayList();
                        // removed parents
                        dirtyParents.addAll(nodeState.getRemovedParentUUIDs());
                        // added parents
                        dirtyParents.addAll(nodeState.getAddedParentUUIDs());
                        Iterator parentsIter = dirtyParents.iterator();
                        while (parentsIter.hasNext()) {
                            NodeId id = new NodeId((String) parentsIter.next());
                            NodeState parentState;
                            try {
                                parentState = (NodeState) stateMgr.getTransientItemState(id);
                            } catch (ItemStateException ise) {
                                // should never get here...
                                String msg = "inconsistency: failed to retrieve transient state for " + itemMgr.safeGetJCRPath(id);
                                log.debug(msg);
                                throw new RepositoryException(msg);
                            }
                            // check if parent is also going to be saved
                            if (!dirty.contains(parentState)) {
                                // need to save the parent too
                                String msg = itemMgr.safeGetJCRPath(id) + " needs to be saved also.";
                                log.debug(msg);
                                throw new RepositoryException(msg);
                            }
                        }
                    }
                }

                /**
                 * build list of transient descendents in the attic
                 * (i.e. those marked as 'removed')
                 */
                Collection removed = getRemovedStates();

                /**
                 * validate access and node type constraints
                 * (this will also validate child removals)
                 */
                validateTransientItems(dirty.iterator(), removed.iterator());

                /**
                 * referential integrity checks:
                 * make sure that a referenced node cannot be removed and
                 * that all references are updated and persisted
                 */
                Collection dirtyRefs =
                        checkReferences(dirty.iterator(), removed.iterator());

                try {
                    // start the update operation
                    stateMgr.edit();

                    // process transient items marked as 'removed'
                    removeTransientItems(removed.iterator());

                    // initialize version histories for new nodes (might generate new transient state)
                    if (initVersionHistories(dirty.iterator())) {
                        /**
                         * re-build the list of transient states because the previous call
                         * generated new transient state
                         */
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
                    iter = dirty.iterator();
                    while (iter.hasNext()) {
                        transientState = (ItemState) iter.next();
                        // dispose the transient state, it is no longer used
                        stateMgr.disposeTransientItemState(transientState);
                    }

                    // store the references calculated above
                    for (Iterator it = dirtyRefs.iterator(); it.hasNext();) {
                        stateMgr.store((NodeReferences) it.next());
                    }

                    // end update operation
                    stateMgr.update();

                } catch (ItemStateException e) {

                    String msg = safeGetJCRPath() + ": unable to update item.";
                    log.debug(msg);
                    throw new RepositoryException(msg, e);

                }

                // now it is safe to dispose the transient states:
                // dispose the transient states marked 'removed'.
                // item states in attic are removed after store, because
                // the observation mechanism needs to build paths of removed
                // items in store().
                iter = removed.iterator();
                while (iter.hasNext()) {
                    transientState = (ItemState) iter.next();
                    // dispose the transient state, it is no longer used
                    stateMgr.disposeTransientItemStateInAttic(transientState);
                }

            } finally {
                // turn off temporary path caching
                stateMgr.enablePathCaching(false);
            }
        }
    }

    /**
     * @see Item#refresh
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
            // check if this is the repository root node
            if (((NodeImpl) this).isRepositoryRoot()) {
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
                        String msg = safeGetJCRPath() + ": cannot revert a new item.";
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
            // build list of 'new', 'modified' or 'stale' descendents
            Iterator iter = stateMgr.getDescendantTransientItemStates(id);
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

        // discard all transient descendents in the attic (i.e. those marked
        // as 'removed'); this will resurrect the removed items
        iter = stateMgr.getDescendantTransientItemStatesInAttic(id);
        while (iter.hasNext()) {
            transientState = (ItemState) iter.next();
            // dispose the transient state; this will indirectly (through
            // stateDiscarded listener method) resurrect the wrapping Item instances
            stateMgr.disposeTransientItemStateInAttic(transientState);
        }
    }

    /**
     * @see Item#getAncestor
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
            Path ancestorPath = path.getAncestor(relDegree);
            return itemMgr.getItem(ancestorPath);
        } catch (PathNotFoundException pnfe) {
            throw new ItemNotFoundException();
        }
    }

    /**
     * @see Item#getPath
     */
    public String getPath() throws RepositoryException {
        try {
            return getPrimaryPath().toJCRPath(session.getNamespaceResolver());
        } catch (NoPrefixDeclaredException npde) {
            // should never get here...
            String msg = "internal error: encountered unregistered namespace";
            log.debug(msg);
            throw new RepositoryException(msg, npde);
        }
    }

    /**
     * @see Item#getDepth
     */
    public int getDepth() throws RepositoryException {
        return getPrimaryPath().getAncestorCount();
    }

    /**
     * @see Item#getSession
     */
    public Session getSession() throws RepositoryException {
        return session;
    }

    /**
     * @see Item#isSame(Item)
     */
    public boolean isSame(Item otherItem) {
        if (this == otherItem) {
            return true;
        }
        if (otherItem instanceof ItemImpl) {
            ItemImpl other = (ItemImpl) otherItem;
            return id.equals(other.id);
        }
        return false;
    }
}
