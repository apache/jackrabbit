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
import org.apache.jackrabbit.core.nodetype.*;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.observation.ObservationManagerFactory;
import org.apache.jackrabbit.core.state.*;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.version.VersionManager;
import org.apache.log4j.Logger;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDef;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import java.util.*;

/**
 * <code>ItemImpl</code> implements the <code>Item</code> interface.
 */
public abstract class ItemImpl implements Item, ItemStateListener {

    private static Logger log = Logger.getLogger(ItemImpl.class);

    // some constants used in derived classes
    // system properties (values are system generated)
    // jcr:uuid
    public static final QName PROPNAME_UUID =
            new QName(NamespaceRegistryImpl.NS_JCR_URI, "uuid");
    // jcr:primaryType
    public static final QName PROPNAME_PRIMARYTYPE =
            new QName(NamespaceRegistryImpl.NS_JCR_URI, "primaryType");
    // jcr:mixinTypes
    public static final QName PROPNAME_MIXINTYPES =
            new QName(NamespaceRegistryImpl.NS_JCR_URI, "mixinTypes");
    // jcr:created
    public static final QName PROPNAME_CREATED =
            new QName(NamespaceRegistryImpl.NS_JCR_URI, "created");
    // jcr:lastModified
    public static final QName PROPNAME_LAST_MODIFIED =
            new QName(NamespaceRegistryImpl.NS_JCR_URI, "lastModified");
    // jcr:mergeFailed
    public static final QName PROPNAME_MERGE_FAILED =
            new QName(NamespaceRegistryImpl.NS_JCR_URI, "mergeFailed");

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
    protected final SessionItemStateManager itemStateMgr;

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
        itemStateMgr = session.getItemStateManager();
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

    protected abstract void makePersistent() throws RepositoryException;

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
            itemStateMgr.disposeTransientItemState(transientState);
        } else {
            // this is an 'existing' item (i.e. it is backed by persistent
            // state), mark it as 'removed'
            transientState.setStatus(ItemState.STATUS_EXISTING_REMOVED);
            // transfer the transient state to the attic
            itemStateMgr.moveTransientItemStateToAttic(transientState);

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
     * Notify the listeners that this instance has been resurrected
     * (i.e. it has been rendered 'valid' again).
     */
    protected void notifyResurrected() {
        // copy listeners to array to avoid ConcurrentModificationException
        ItemLifeCycleListener[] la = new ItemLifeCycleListener[listeners.size()];
        Iterator iter = listeners.values().iterator();
        int cnt = 0;
        while (iter.hasNext()) {
            la[cnt++] = (ItemLifeCycleListener) iter.next();
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].itemResurrected(this);
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
                        log.error(msg);
                        throw new RepositoryException(msg);
                    }

                case ItemState.STATUS_STALE_MODIFIED:
                    {
                        String msg = safeGetJCRPath() + ": the item cannot be saved because it has been modified externally.";
                        log.error(msg);
                        throw new InvalidItemStateException(msg);
                    }

                case ItemState.STATUS_STALE_DESTROYED:
                    {
                        String msg = safeGetJCRPath() + ": the item cannot be saved because it has been deleted externally.";
                        log.error(msg);
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
            Iterator iter = itemStateMgr.getDescendantTransientItemStates(id);
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
                            log.error(msg);
                            throw new InvalidItemStateException(msg);
                        }

                    case ItemState.STATUS_STALE_DESTROYED:
                        {
                            String msg = transientState.getId() + ": the item cannot be saved because it has been deleted externally.";
                            log.error(msg);
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
            Iterator iter = itemStateMgr.getDescendantTransientItemStatesInAttic(id);
            while (iter.hasNext()) {
                transientState = (ItemState) iter.next();
                // check if stale
                if (transientState.getStatus() == ItemState.STATUS_STALE_MODIFIED) {
                    String msg = transientState.getId() + ": the item cannot be removed because it has been modified externally.";
                    log.error(msg);
                    throw new InvalidItemStateException(msg);
                }
                if (transientState.getStatus() == ItemState.STATUS_STALE_DESTROYED) {
                    String msg = transientState.getId() + ": the item cannot be removed because it has already been deleted externally.";
                    log.error(msg);
                    throw new InvalidItemStateException(msg);
                }
                removed.add(transientState);
            }
        }
        return removed;
    }

    private void validateTransientItems(Iterator iter)
            throws AccessDeniedException, ConstraintViolationException, RepositoryException {
        /**
         * the following validations/checks are performed on transient items:
         *
         * for every transient node:
         * - if it is 'new', check that its node type satisfies the
         *   'required node type' constraint specified in its definition
         * - if new child nodes have been added to the node in question,
         *   check the WRITE permission
         * - if child items have been removed from the node in question,
         *   check the WRITE permission
         * - check if 'mandatory' child items exist
         *
         * for every transient property:
         * - check the WRITE permission
         * - check if the property value satisfies the value constraints
         *   specified in the property's definition
         *
         * note that the protected flag is checked in Node.addNode/Node.remove
         * (for adding/removing child entries of a node), in
         * Node.addMixin/removeMixin (for mixin changes on nodes)
         * and in Property.setValue (for properties to be modified).
         */

        AccessManagerImpl accessMgr = session.getAccessManager();
        // walk through list of transient items and validate each
        while (iter.hasNext()) {
            ItemState itemState = (ItemState) iter.next();

            if (itemState.isNode()) {
                // the transient item is a node
                NodeState nodeState = (NodeState) itemState;
                ItemId id = nodeState.getId();
                NodeImpl node = (NodeImpl) itemMgr.getItem(id);
                NodeDef def = node.getDefinition();
                NodeTypeImpl nt = (NodeTypeImpl) node.getPrimaryNodeType();
                /**
                 * if the transient node was added (i.e. if it is 'new'),
                 * check its node's node type against the required node type
                 * in its definition
                 */
                NodeType[] nta = def.getRequiredPrimaryTypes();
                for (int i = 0; i < nta.length; i++) {
                    NodeTypeImpl ntReq = (NodeTypeImpl) nta[i];
                    if (nodeState.getStatus() == ItemState.STATUS_NEW
                            && !(nt.getQName().equals(ntReq.getQName())
                            || nt.isDerivedFrom(ntReq.getQName()))) {
                        /**
                         * the transient node's node type does not satisfy the
                         * 'required primary types' constraint
                         */
                        String msg = node.safeGetJCRPath() + " must be of node type " + ntReq.getName();
                        log.warn(msg);
                        throw new ConstraintViolationException(msg);
                    }
                }

                // check child removals
                if (!nodeState.getRemovedChildNodeEntries().isEmpty() || !nodeState.getRemovedPropertyEntries().isEmpty()) {
                    // check WRITE permission
                    if (!accessMgr.isGranted(id, AccessManager.WRITE)) {
                        String msg = node.safeGetJCRPath() + ": not allowed to remove a child item";
                        log.error(msg);
                        throw new AccessDeniedException(msg);
                    }

                    /**
                     * no need to check the protected flag as this is checked
                     * in NodeImpl.remove(String)
                     */
                }

                // check child additions
                // added child nodes
                Iterator addedIter = nodeState.getAddedChildNodeEntries().iterator();
                while (addedIter.hasNext()) {
                    NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) addedIter.next();
                    Node childNode = (Node) itemMgr.getItem(new NodeId(entry.getUUID()));
                    NodeDef childDef = childNode.getDefinition();
                    if (!childDef.isAutoCreate()) {
                        // check WRITE permission
                        if (!accessMgr.isGranted(id, AccessManager.WRITE)) {
                            String msg = node.safeGetJCRPath() + ": not allowed to add node " + childNode.getName();
                            log.error(msg);
                            throw new AccessDeniedException(msg);
                        }
                    }
                }
                // mandatory child properties
                PropertyDef[] propDefs = nt.getMandatoryPropertyDefs();
                for (int i = 0; i < propDefs.length; i++) {
                    PropertyDefImpl pd = (PropertyDefImpl) propDefs[i];
                    if (!nodeState.hasPropertyEntry(pd.getQName())) {
                        String msg = node.safeGetJCRPath() + ": mandatory property " + pd.getName() + " does not exist";
                        log.warn(msg);
                        throw new ConstraintViolationException(msg);
                    }
                }
                // mandatory child nodes
                NodeDef[] nodeDefs = nt.getMandatoryNodeDefs();
                for (int i = 0; i < nodeDefs.length; i++) {
                    NodeDefImpl nd = (NodeDefImpl) nodeDefs[i];
                    if (!nodeState.hasChildNodeEntry(nd.getQName())) {
                        String msg = node.safeGetJCRPath() + ": mandatory child node " + nd.getName() + " does not exist";
                        log.warn(msg);
                        throw new ConstraintViolationException(msg);
                    }
                }
            } else {
                // the transient item is a property
                PropertyState propState = (PropertyState) itemState;
                ItemId propId = propState.getId();
                NodeId nodeId = new NodeId(propState.getParentUUID());
                PropertyImpl prop = (PropertyImpl) itemMgr.getItem(propId);
                PropertyDefImpl def = (PropertyDefImpl) prop.getDefinition();

                if (!def.isAutoCreate()) {
                    // check WRITE permission on property
                    if (!accessMgr.isGranted(propId, AccessManager.WRITE)) {
                        String msg = itemMgr.safeGetJCRPath(nodeId) + ": not allowed to set property " + prop.getName();
                        log.error(msg);
                        throw new AccessDeniedException(msg);
                    }
                    if (propState.getOverlayedState() == null) {
                        // property has been added, check WRITE permission on parent
                        if (!accessMgr.isGranted(nodeId, AccessManager.WRITE)) {
                            String msg = itemMgr.safeGetJCRPath(nodeId) + ": not allowed to set property " + prop.getName();
                            log.error(msg);
                            throw new AccessDeniedException(msg);
                        }
                    }
                }

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
                            log.warn(msg);
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
                                    log.error(msg, re);
                                    throw new ConstraintViolationException(msg, re);
                                }
                                if (!satisfied) {
                                    String msg = prop.safeGetJCRPath()
                                            + ": does not satisfy the value constraint "
                                            + constraints[0];   // just report the 1st
                                    log.warn(msg);
                                    throw new ConstraintViolationException(msg);
                                }
                            }
                        }
                    }
                }

                /**
                 * no need to check the protected flag* as this is checked
                 * in PropertyImpl.setValue(Value)
                 */
            }
        }
    }

    private void checkReferences(Iterator iterDirty, Iterator iterRemoved,
                                 ReferenceManager refMgr)
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
                if (propState.getType() == PropertyType.REFERENCE) {
                    if (propState.getStatus() == ItemState.STATUS_EXISTING_MODIFIED) {
                        // this is a modified REFERENCE property:
                        // remove the 'reference' stored in the old value
                        PropertyState oldPropState = (PropertyState) propState.getOverlayedState();
                        InternalValue[] vals = oldPropState.getValues();
                        for (int i = 0; vals != null && i < vals.length; i++) {
                            String uuid = vals[i].toString();
                            NodeId targetId = new NodeId(uuid);
                            NodeReferences refs;
                            if (dirtyNodeRefs.containsKey(targetId)) {
                                refs = (NodeReferences) dirtyNodeRefs.get(targetId);
                            } else {
                                refs = refMgr.get(targetId);
                                dirtyNodeRefs.put(targetId, refs);
                            }
                            // remove reference from target node
                            refs.removeReference((PropertyId) propState.getId());
                        }
                    }
                    // add the reference stored in the new value
                    InternalValue[] vals = propState.getValues();
                    for (int i = 0; vals != null && i < vals.length; i++) {
                        String uuid = vals[i].toString();
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
                                log.error(msg, mpe);
                                throw new RepositoryException(msg, mpe);
                            }
                        }
                        NodeReferences refs;
                        if (dirtyNodeRefs.containsKey(targetId)) {
                            refs = (NodeReferences) dirtyNodeRefs.get(targetId);
                        } else {
                            refs = refMgr.get(targetId);
                            dirtyNodeRefs.put(targetId, refs);
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
                        NodeId targetId = new NodeId(uuid);
                        NodeReferences refs;
                        if (dirtyNodeRefs.containsKey(targetId)) {
                            refs = (NodeReferences) dirtyNodeRefs.get(targetId);
                        } else {
                            refs = refMgr.get(targetId);
                            dirtyNodeRefs.put(targetId, refs);
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
            NodeId targetId = (NodeId) nodeState.getId();
            NodeReferences refs;
            if (dirtyNodeRefs.containsKey(targetId)) {
                refs = (NodeReferences) dirtyNodeRefs.get(targetId);
            } else {
                refs = refMgr.get(targetId);
            }
            if (refs.hasReferences()) {
                String msg = nodeState.getId()
                        + ": the node cannot be removed because it is being referenced.";
                log.warn(msg);
                throw new ReferentialIntegrityException(msg);
            }
        }

        // persist dirty NodeReferences objects
        iter = dirtyNodeRefs.values().iterator();
        while (iter.hasNext()) {
            NodeReferences refs = (NodeReferences) iter.next();
            refMgr.save(refs);
        }
    }

    private void removeTransientItems(Iterator iter) throws RepositoryException {
        /**
         * walk through list of transient items marked 'removed' and
         * definitively remove each one
         */
        while (iter.hasNext()) {
            ItemState transientState = (ItemState) iter.next();
            PersistableItemState persistentState = (PersistableItemState) transientState.getOverlayedState();
            /**
             * remove persistent state (incl. descendents, if there are any)
             *
             * this will indirectly (through stateDestroyed listener method)
             * permanently invalidate all Item instances wrapping it
             */
            try {
                persistentState.destroy();
            } catch (ItemStateException ise) {
                String msg = "failed to remove item " + transientState.getId();
                log.error(msg, ise);
                throw new RepositoryException(msg, ise);
            }
        }
    }

    private void persistTransientItems(Iterator iter) throws RepositoryException {
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
                if (node.isNodeType(NodeTypeRegistry.MIX_VERSIONABLE)) {
                    if (!node.hasProperty(VersionManager.PROPNAME_VERSION_HISTORY)) {
                        VersionHistory hist = session.versionMgr.createVersionHistory(node);
                        node.internalSetProperty(VersionManager.PROPNAME_VERSION_HISTORY, InternalValue.create(new UUID(hist.getUUID())));
                        node.internalSetProperty(VersionManager.PROPNAME_BASE_VERSION, InternalValue.create(new UUID(hist.getRootVersion().getUUID())));
                        node.internalSetProperty(VersionManager.PROPNAME_IS_CHECKED_OUT, InternalValue.create(true));
                        node.internalSetProperty(VersionManager.PROPNAME_PREDECESSORS, new InternalValue[]{InternalValue.create(new UUID(hist.getRootVersion().getUUID()))});
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
        // notify the listeners that this instance has been
        // permanently invalidated
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
        // the state of this item has been discarded, probably as a result
        // of calling Node.revert() or ItemImpl.setRemoved()
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
                    // the state is a transient wrapper for the underlying
                    // persistent state, therefor restore the
                    // persistent state and resurrect this item instance
                    // if necessary
                    state.removeListener(this);
                    state = persistentState;
                    state.addListener(this);

                    if (status == STATUS_INVALIDATED) {
                        // resurrect this instance
                        status = STATUS_NORMAL;
                        // notify the listeners
                        notifyResurrected();
                    }
                    return;

                    /**
                     * persistent item that has been transiently modified or removed
                     * and the underlying persistent state has been externally
                     * destroyed since the transient modification/removal.
                     */
                case ItemState.STATUS_STALE_DESTROYED:
                    // set state of this instance to 'destroyed'
                    status = STATUS_DESTROYED;
                    // dispose state
                    state.removeListener(this);
                    state = null;
                    // notify the listeners that this instance has been
                    // permanently invalidated
                    notifyDestroyed();
                    return;

                    /**
                     * new item that has been transiently added
                     */
                case ItemState.STATUS_NEW:
                    // set state of this instance to 'destroyed'
                    status = STATUS_DESTROYED;
                    // dispose state
                    state.removeListener(this);
                    state = null;
                    // notify the listeners that this instance has been
                    // permanently invalidated
                    notifyDestroyed();
                    return;
            }
        }

        // render this instance 'invalid'
        status = STATUS_INVALIDATED;
        // notify the listeners
        notifyInvalidated();
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
    public void remove() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        Path.PathElement thisName = getPrimaryPath().getNameElement();

        // check if protected
        if (isNode()) {
            NodeImpl node = (NodeImpl) this;
            // check if this is the repository root node
            if (node.isRepositoryRoot()) {
                String msg = safeGetJCRPath() + ": cannot remove root node";
                log.error(msg);
                throw new RepositoryException(msg);
            }

            NodeDef def = node.getDefinition();
            // check protected flag
            if (def.isProtected()) {
                String msg = safeGetJCRPath() + ": cannot remove a protected node";
                log.error(msg);
                throw new ConstraintViolationException(msg);
            }
        } else {
            PropertyImpl prop = (PropertyImpl) this;
            PropertyDef def = prop.getDefinition();
            // check protected flag
            if (def.isProtected()) {
                String msg = safeGetJCRPath() + ": cannot remove a protected property";
                log.error(msg);
                throw new ConstraintViolationException(msg);
            }
        }

        NodeImpl parentNode = (NodeImpl) getParent();

        // check if versioning allows write
        if (!parentNode.safeIsCheckedOut()) {
            String msg = parentNode.safeGetJCRPath() + ": cannot remove a child of a checked-in node";
            log.error(msg);
            throw new VersionException(msg);
        }

        // check protected flag of parent node
        if (parentNode.getDefinition().isProtected()) {
            String msg = parentNode.safeGetJCRPath() + ": cannot remove a child of a protected node";
            log.error(msg);
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
            throws AccessDeniedException, LockException,
            ConstraintViolationException, InvalidItemStateException,
            ReferentialIntegrityException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // synchronize on this session
        synchronized(session) {
            try {
                /**
                 * turn on temporary path caching for better performance
                 * (assuming that the paths won't change during this save() call)
                 */
                itemStateMgr.enablePathCaching(true);

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
                            NodeState parentState = null;
                            try {
                                parentState = (NodeState) itemStateMgr.getTransientItemState(id);
                            } catch (ItemStateException ise) {
                                // should never get here...
                                String msg = "inconsistency: failed to retrieve transient state for " + itemMgr.safeGetJCRPath(id);
                                log.error(msg);
                                throw new RepositoryException(msg);
                            }
                            // check if parent is also going to be saved
                            if (!dirty.contains(parentState)) {
                                // need to save the parent too
                                String msg = itemMgr.safeGetJCRPath(id) + " needs to be saved also.";
                                log.error(msg);
                                throw new RepositoryException(msg);
                            }
                        }
                    }
                }

                /**
                 * validate access and node type constraints
                 * (this will also validate child removals)
                 */
                validateTransientItems(dirty.iterator());

                WorkspaceImpl wsp = (WorkspaceImpl) session.getWorkspace();

                // list of events that are generated by saved changes
                ObservationManagerFactory obsFactory = rep.getObservationManagerFactory(wsp.getName());
                EventStateCollection events = obsFactory.createEventStateCollection(session,
                        session.getItemStateManager(), session.getHierarchyManager());

                /**
                 * we need to make sure that we are not interrupted while
                 * verifying/persisting node references
                 */
                ReferenceManager refMgr = wsp.getReferenceManager();
                synchronized (refMgr) {
                    /**
                     * build list of transient descendents in the attic
                     * (i.e. those marked as 'removed')
                     */
                    Collection removed = getRemovedStates();

                    /**
                     * referential integrity checks:
                     * make sure that a referenced node cannot be removed and
                     * that all references are updated and persisted
                     */
                    checkReferences(dirty.iterator(), removed.iterator(), refMgr);

                    /**
                     * create event states for the affected item states and
                     * prepare them for event dispatch (this step is necessary in order
                     * to check access rights on items that will be removed)
                     *
                     * todo consolidate event generating and dispatching code (ideally one method call after save has succeeded)
                     */
                    events.createEventStates(dirty);
                    events.createEventStates(removed);
                    events.prepare();

                    // definitively remove transient items marked as 'removed'
                    removeTransientItems(removed.iterator());

                    // dispose the transient states marked 'removed'
                    iter = removed.iterator();
                    while (iter.hasNext()) {
                        transientState = (ItemState) iter.next();
                        /**
                         * dispose the transient state, it is no longer used
                         * this will indirectly (through stateDiscarded listener method)
                         * permanently invalidate the wrapping Item instance
                         */
                        itemStateMgr.disposeTransientItemStateInAttic(transientState);
                    }

                    // initialize version histories for new nodes (might generate new transient state)
                    if (initVersionHistories(dirty.iterator())) {
                        /**
                         * re-build the list of transient states because the previous call
                         * generated new transient state
                         */
                        dirty = getTransientStates();
                    }

                    // persist 'new' or 'modified' transient states
                    persistTransientItems(dirty.iterator());
                } // synchronized(refMgr)

                // now it is safe to dispose the transient states
                iter = dirty.iterator();
                while (iter.hasNext()) {
                    transientState = (ItemState) iter.next();
                    // dispose the transient state, it is no longer used
                    itemStateMgr.disposeTransientItemState(transientState);
                }

                /**
                 * all changes are persisted, now dispatch events;
                 * forward this to the session to let it decide on the right
                 * time for those events to be dispatched in case of
                 * transactional support
                 */
                session.dispatch(events);
            } finally {
                // turn off temporary path caching
                itemStateMgr.enablePathCaching(false);
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
                itemStateMgr.disposeAllTransientItemStates();
                return;
            }
        }

        // list of transient items that should be discarded
        ArrayList list = new ArrayList();
        ItemState transientState;

        // check status of this item's state
        if (isTransient()) {
            transientState = (ItemState) state;
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
                        log.error(msg);
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
            Iterator iter = itemStateMgr.getDescendantTransientItemStates(id);
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
            itemStateMgr.disposeTransientItemState(transientState);
        }

        // discard all transient descendents in the attic (i.e. those marked
        // as 'removed'); this will resurrect the removed items
        iter = itemStateMgr.getDescendantTransientItemStatesInAttic(id);
        while (iter.hasNext()) {
            transientState = (ItemState) iter.next();
            // dispose the transient state; this will indirectly (through
            // stateDiscarded listener method) resurrect the wrapping Item instances
            itemStateMgr.disposeTransientItemStateInAttic(transientState);
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
            log.error(msg, npde);
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
