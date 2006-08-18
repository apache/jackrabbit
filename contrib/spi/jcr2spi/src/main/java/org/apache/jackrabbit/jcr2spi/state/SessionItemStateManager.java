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
import org.apache.jackrabbit.jcr2spi.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.ZombieHierarchyManager;
import org.apache.jackrabbit.jcr2spi.HierarchyManagerImpl;
import org.apache.jackrabbit.jcr2spi.util.ReferenceChangeTracker;
import org.apache.jackrabbit.jcr2spi.util.LogUtil;
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
import org.apache.jackrabbit.name.NamespaceResolver;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.value.QValue;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.io.InputStream;
import java.io.IOException;

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
    // DIFF JACKRABBIT: private final TransientItemStateManager transientStateMgr;
    private final TransientItemStateManager transientStateMgr;

    /**
     * Hierarchy manager
     */
    private final HierarchyManager hierMgr;
    private final NamespaceResolver nsResolver;
    private final ItemStateValidator validator;

    /**
     * Creates a new <code>SessionItemStateManager</code> instance.
     *
     * @param workspaceItemStateMgr
     * @param nsResolver
     */
    public SessionItemStateManager(UpdatableItemStateManager workspaceItemStateMgr,
                                   IdFactory idFactory,
                                   ItemStateValidator validator,
                                   NamespaceResolver nsResolver) {
        this.workspaceItemStateMgr = workspaceItemStateMgr;
        // DIFF JACKRABBIT: this.transientStateMgr = new TransientItemStateManager();
        this.transientStateMgr = new TransientItemStateManager(idFactory, workspaceItemStateMgr);
        // DIFF JR: validator added
        this.validator = validator;

        this.nsResolver = nsResolver;

        // create hierarchy manager
        hierMgr = new HierarchyManagerImpl(this, nsResolver);

    }

    /**
     *
     * @return
     */
    public HierarchyManager getHierarchyManager() {
        return hierMgr;
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
        return workspaceItemStateMgr.getReferingStates(nodeState);
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
        return workspaceItemStateMgr.hasReferingStates(nodeState);
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
        transientStateMgr.disposeAllItemStates();
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
        ChangeLog changeLog = getChangeLog(state);
        if (!changeLog.isEmpty()) {
            // only pass changelog if there are transient modifications available
            // for the specified item and its decendants.
            workspaceItemStateMgr.execute(changeLog);
        }

        // dispose the transient states marked 'new' or 'modified'
        Iterator it = new IteratorChain(changeLog.addedStates(), changeLog.modifiedStates());
        while (it.hasNext()) {
            ItemState transientState = (ItemState) it.next();
            // dispose the transient state, it is no longer used
            transientStateMgr.disposeItemState(transientState);
        }

        // dispose the transient states marked 'removed'.
        // item states in attic are removed after store, because
        // the observation mechanism needs to build paths of removed
        // items in update().
        it = changeLog.deletedStates();
        while (it.hasNext()) {
            ItemState transientState = (ItemState) it.next();
            // dispose the transient state, it is no longer used
            transientStateMgr.disposeItemStateInAttic(transientState);
        }

        // remove operations just processed
        transientStateMgr.disposeOperations(changeLog.getOperations());
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
    public void undo(ItemState itemState) throws ItemStateException {
        if (itemState.getParent() == null) {
            // optimization for root
            transientStateMgr.disposeAllItemStates();
            return;
        }

        // list of transient items that should be discarded
        ChangeLog changeLog = new ChangeLog();

        // check status of current item's state
        if (itemState.isTransient()) {
            switch (itemState.getStatus()) {
                // safety... should have been checked befoe
                case ItemState.STATUS_NEW:
                    throw new ItemStateException("Unexpected state: cannot start undo from a new item state.");

                case ItemState.STATUS_STALE_MODIFIED:
                case ItemState.STATUS_STALE_DESTROYED:
                case ItemState.STATUS_EXISTING_MODIFIED:
                    // add this item's state to the list
                    changeLog.modified(itemState);
                    break;
                default:
                    log.debug("unexpected state status (" + itemState.getStatus() + ")");
                    // ignore
                    break;
            }
        }

        if (itemState.isNode()) {
            NodeState nodeState = (NodeState)itemState;
            // build list of 'new', 'modified' or 'stale' descendants
            Iterator iter = getDescendantTransientItemStates(nodeState);
            while (iter.hasNext()) {
                ItemState childState = (ItemState) iter.next();
                switch (childState.getStatus()) {
                    case ItemState.STATUS_STALE_MODIFIED:
                    case ItemState.STATUS_STALE_DESTROYED:
                    case ItemState.STATUS_NEW:
                    case ItemState.STATUS_EXISTING_MODIFIED:
                        // add new or modified state to the list
                        changeLog.modified(childState);
                        break;

                    default:
                        log.debug("unexpected state status (" + childState.getStatus() + ")");
                        // ignore
                        break;
                }
            }

            // build list of deleted states
            Iterator atticIter = getDescendantTransientItemStatesInAttic(nodeState);
            while (atticIter.hasNext()) {
                ItemState transientState = (ItemState) atticIter.next();
                changeLog.deleted(transientState);
            }
        }

        /**
         * build set of item id's which are within the scope of
         * (i.e. affected by) this cancel operation
         */
        Set affectedStates = new HashSet();
        Iterator it = new IteratorChain(changeLog.modifiedStates(), changeLog.deletedStates());
        while (it.hasNext()) {
            affectedStates.add(it.next());
        }
        collectOperations(affectedStates, changeLog);

        // process list of 'new', 'modified' or 'stale' transient states
        Iterator transIter = changeLog.modifiedStates();
        while (transIter.hasNext()) {
            // dispose the transient state, it is no longer used;
            // this will indirectly (through stateDiscarded listener method)
            // either restore or permanently invalidate the wrapping Item instances
            ItemState transientState = (ItemState) transIter.next();
            transientStateMgr.disposeItemState(transientState);
        }
        // process list of deleted states
        Iterator remIter = changeLog.deletedStates();
        while (remIter.hasNext()) {
            ItemState rmState = (ItemState) remIter.next();
            // dispose the transient state; this will indirectly (through
            // stateDiscarded listener method) resurrect the wrapping Item instances
            transientStateMgr.disposeItemStateInAttic(rmState);
        }

        // remove all canceled operations
        Iterator opIter = changeLog.getOperations();
        while (opIter.hasNext()) {
            transientStateMgr.removeOperation((Operation) opIter.next());
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
            // DIFF JR: remove check (already asserted on processReference)
            boolean modified = false;
            QValue[] values = propState.getValues();
            QValue[] newVals = new QValue[values.length];
            for (int i = 0; i < values.length; i++) {
                QValue val = values[i];
                QValue adjusted = refTracker.getMappedReference(val);
                if (adjusted != null) {
                    newVals[i] = adjusted;
                    modified = true;
                } else {
                    // reference doesn't need adjusting, just copy old value
                    newVals[i] = val;
                }
            }
            if (modified) {
                setPropertyStateValue(propState, newVals, PropertyType.REFERENCE);
            }
        }
        // make sure all entries are removed
        refTracker.clear();
    }
    //-------------------------------------------< Transient state handling >---
    /**
     * Returns an iterator over those transient item state instances that are
     * direct or indirect descendents of the item state with the given
     * <code>parentId</code>. The transient item state instance with the given
     * <code>parentId</code> itself (if there is such) will not be included.
     * <p/>
     * The instances are returned in depth-first tree traversal order.
     *
     * @param parent the common parent state of the transient item state
     * instances to be returned.
     * @return an iterator over descendant transient item state instances
     */
    private Iterator getDescendantTransientItemStates(NodeState parent) {
        if (!transientStateMgr.hasPendingChanges()) {
            return Collections.EMPTY_LIST.iterator();
        }

        // build ordered collection of descendant transient states
        // sorted by decreasing relative depth

        // use an array of lists to group the descendants by relative depth;
        // the depth is used as array index
        List[] la = new List[10];
        try {
            Iterator iter = transientStateMgr.getModifiedOrAddedItemStates();
            while (iter.hasNext()) {
                ItemState state = (ItemState) iter.next();
                // determine relative depth: > 0 means it's a descendant
                int depth;
                try {
                    depth = hierMgr.getRelativeDepth(parent, state);
                } catch (ItemNotFoundException infe) {
                    /**
                     * one of the parents of the specified item has been
                     * removed externally; as we don't know its path,
                     * we can't determine if it is a descendant;
                     * InvalidItemStateException should only be thrown if
                     * a descendant is affected;
                     * => throw InvalidItemStateException for now
                     * todo FIXME
                     */
                    // unable to determine relative depth, assume that the item
                    // (or any of its ancestors) has been removed externally
                    String msg = state.getId()
                            + ": the item seems to have been removed externally.";
                    log.debug(msg);
                    throw new InvalidItemStateException(msg);
                }

                if (depth < 1) {
                    // not a descendant
                    continue;
                }

                // ensure capacity
                if (depth > la.length) {
                    List old[] = la;
                    la = new List[depth + 10];
                    System.arraycopy(old, 0, la, 0, old.length);
                }

                List list = la[depth - 1];
                if (list == null) {
                    list = new ArrayList();
                    la[depth - 1] = list;
                }
                list.add(state);
            }
        } catch (RepositoryException re) {
            log.warn("inconsistent hierarchy state", re);
        }
        // create an iterator over the collected descendants
        // in decreasing depth order
        IteratorChain resultIter = new IteratorChain();
        for (int i = la.length - 1; i >= 0; i--) {
            List list = la[i];
            if (list != null) {
                resultIter.addIterator(list.iterator());
            }
        }
        /**
         * if the resulting iterator chain is empty return
         * EMPTY_LIST.iterator() instead because older versions
         * of IteratorChain (pre Commons Collections 3.1)
         * would throw UnsupportedOperationException in this
         * situation
         */
        if (resultIter.getIterators().isEmpty()) {
            return Collections.EMPTY_LIST.iterator();
        }
        return resultIter;
    }

    /**
     * Same as <code>{@link #getDescendantTransientItemStates(NodeState)}</code>
     * except that item state instances in the attic are returned.
     *
     * @param parent the common parent of the transient item state
     * instances to be returned.
     * @return an iterator over descendant transient item state instances in the attic
     */
    private Iterator getDescendantTransientItemStatesInAttic(NodeState parent) {
        // DIFF JACKRABBIT: if (!transientStateMgr.hasAnyItemStatesInAttic()) {
        if (!transientStateMgr.hasDeletedItemStates()) {
            return Collections.EMPTY_LIST.iterator();
        }

        // build ordered collection of descendant transient states in attic
        // sorted by decreasing relative depth

        // use a special attic-aware hierarchy manager
        ZombieHierarchyManager zombieHierMgr =
                new ZombieHierarchyManager(this, transientStateMgr.getAttic(), nsResolver);

        // use an array of lists to group the descendants by relative depth;
        // the depth is used as array index
        List[] la = new List[10];
        try {
            Iterator iter = transientStateMgr.getDeletedItemStates();
            while (iter.hasNext()) {
                ItemState state = (ItemState) iter.next();
                // determine relative depth: > 0 means it's a descendant
                int depth = zombieHierMgr.getRelativeDepth(parent, state);
                if (depth < 1) {
                    // not a descendant
                    continue;
                }

                // ensure capacity
                if (depth > la.length) {
                    List old[] = la;
                    la = new List[depth + 10];
                    System.arraycopy(old, 0, la, 0, old.length);
                }

                List list = la[depth - 1];
                if (list == null) {
                    list = new ArrayList();
                    la[depth - 1] = list;
                }
                list.add(state);
            }
        } catch (RepositoryException re) {
            log.warn("inconsistent hierarchy state", re);
        }
        // create an iterator over the collected descendants
        // in decreasing depth order
        IteratorChain resultIter = new IteratorChain();
        for (int i = la.length - 1; i >= 0; i--) {
            List list = la[i];
            if (list != null) {
                resultIter.addIterator(list.iterator());
            }
        }
        /**
         * if the resulting iterator chain is empty return
         * EMPTY_LIST.iterator() instead because older versions
         * of IteratorChain (pre Commons Collections 3.1)
         * would throw UnsupportedOperationException in this
         * situation
         */
        if (resultIter.getIterators().isEmpty()) {
            return Collections.EMPTY_LIST.iterator();
        }
        return resultIter;
    }

    /**
     *
     * @param itemState
     * @return
     * @throws StaleItemStateException
     * @throws ItemStateException
     */
    private ChangeLog getChangeLog(ItemState itemState) throws StaleItemStateException, ItemStateException {

        ChangeLog changeLog = new ChangeLog();
        if (itemState.getParent() == null) {
            // root state -> get all item states
            for (Iterator it = transientStateMgr.addedStates(); it.hasNext(); ) {
                changeLog.added((ItemState) it.next());
            }
            for (Iterator it = transientStateMgr.modifiedStates(); it.hasNext(); ) {
                changeLog.modified((ItemState) it.next());
            }
            for (Iterator it = transientStateMgr.deletedStates(); it.hasNext(); ) {
                changeLog.deleted((ItemState) it.next());
            }
            for (Iterator it = transientStateMgr.getOperations(); it.hasNext(); ) {
                changeLog.addOperation((Operation) it.next());
            }
        } else {
            // build changelog for affected and decendant states only
            collectTransientStates(itemState, changeLog);
            collectRemovedStates(itemState, changeLog);

            /**
             * build set of item id's which are within the scope of
             * (i.e. affected by) this save operation
             */
            Iterator it = new IteratorChain(changeLog.modifiedStates(), changeLog.deletedStates());
            Set affectedStates = new HashSet();
            while (it.hasNext()) {
                affectedStates.add(it.next());
            }

            checkIsSelfContained(affectedStates, changeLog);
            collectOperations(affectedStates, changeLog);
        }
        return changeLog;
    }

    /**
     * DIFF JACKRABBIT: copied and adapted from ItemImpl.getRemovedStates()
     * <p/>
     * Builds a list of transient descendant item states in the attic
     * (i.e. those marked as 'removed') that are within the scope of
     * <code>root</code>.
     *
     * @throws StaleItemStateException
     */
    private void collectRemovedStates(ItemState root, ChangeLog changeLog)
        throws StaleItemStateException {
        ItemState transientState;
        if (root.isNode()) {
            Iterator iter = getDescendantTransientItemStatesInAttic((NodeState)root);
            while (iter.hasNext()) {
                transientState = (ItemState) iter.next();
                // check if stale
                if (transientState.getStatus() == ItemState.STATUS_STALE_MODIFIED) {
                    String msg = transientState.getId()
                        + ": the item cannot be removed because it has been modified externally.";
                    log.debug(msg);
                    throw new StaleItemStateException(msg);
                }
                if (transientState.getStatus() == ItemState.STATUS_STALE_DESTROYED) {
                    String msg = transientState.getId()
                        + ": the item cannot be removed because it has already been deleted externally.";
                    log.debug(msg);
                    throw new StaleItemStateException(msg);
                }
                changeLog.deleted(transientState);
            }
        }
    }

    /**
     * DIFF JACKRABBIT: copied and adapted from ItemImpl.getTransientStates()
     * <p/>
     * Builds a list of transient (i.e. new or modified) item states that are
     * within the scope of <code>state</code>.
     *
     * @throws StaleItemStateException
     * @throws ItemStateException
     */
    private void collectTransientStates(ItemState state, ChangeLog changeLog)
            throws StaleItemStateException, ItemStateException {
        // list of transient states that should be persisted
        ItemState transientState;

        // fail-fast test: check status of this item's state
        if (state.isTransient()) {
            switch (state.getStatus()) {
                case ItemState.STATUS_EXISTING_MODIFIED:
                    // add this item's state to the list
                    changeLog.modified(state);
                    break;

                case ItemState.STATUS_NEW:
                    {
                        String msg = LogUtil.safeGetJCRPath(state, nsResolver, hierMgr) + ": cannot save a new item.";
                        log.debug(msg);
                        throw new ItemStateException(msg);
                    }

                case ItemState.STATUS_STALE_MODIFIED:
                    {
                        String msg = LogUtil.safeGetJCRPath(state, nsResolver, hierMgr) + ": the item cannot be saved because it has been modified externally.";
                        log.debug(msg);
                        throw new StaleItemStateException(msg);
                    }

                case ItemState.STATUS_STALE_DESTROYED:
                    {
                        String msg = LogUtil.safeGetJCRPath(state, nsResolver, hierMgr) + ": the item cannot be saved because it has been deleted externally.";
                        log.debug(msg);
                        throw new StaleItemStateException(msg);
                    }

                case ItemState.STATUS_UNDEFINED:
                    {
                        String msg = LogUtil.safeGetJCRPath(state, nsResolver, hierMgr) + ": the item cannot be saved; it seems to have been removed externally.";
                        log.debug(msg);
                        throw new StaleItemStateException(msg);
                    }

                default:
                    log.debug("unexpected state status (" + state.getStatus() + ")");
                    // ignore
                    break;
            }
        }

        if (state.isNode()) {
            // build list of 'new' or 'modified' descendants
            Iterator iter = getDescendantTransientItemStates((NodeState) state);
            while (iter.hasNext()) {
                transientState = (ItemState) iter.next();
                // fail-fast test: check status of transient state
                switch (transientState.getStatus()) {
                    case ItemState.STATUS_NEW:
                    case ItemState.STATUS_EXISTING_MODIFIED:
                        // add modified state to the list
                        changeLog.modified(transientState);
                        break;

                    case ItemState.STATUS_STALE_MODIFIED:
                        {
                            String msg = transientState.getId() + ": the item cannot be saved because it has been modified externally.";
                            log.debug(msg);
                            throw new StaleItemStateException(msg);
                        }

                    case ItemState.STATUS_STALE_DESTROYED:
                        {
                            String msg = transientState.getId() + ": the item cannot be saved because it has been deleted externally.";
                            log.debug(msg);
                            throw new StaleItemStateException(msg);
                        }

                    case ItemState.STATUS_UNDEFINED:
                        {
                            String msg = transientState.getId() + ": the item cannot be saved; it seems to have been removed externally.";
                            log.debug(msg);
                            throw new StaleItemStateException(msg);
                        }

                    default:
                        log.debug("unexpected state status (" + transientState.getStatus() + ")");
                        // ignore
                        break;
                }
            }
        }
    }

    /**
     * Retuns a list of operations that are in the scope the the change set
     * defined by the affected <code>ItemState</code>s.
     *
     * @param affectedStates
     * @param changeLog
     */
    private void collectOperations(Set affectedStates, ChangeLog changeLog) {
        Iterator opsIter = transientStateMgr.getOperations();
        while (opsIter.hasNext()) {
            Operation op = (Operation) opsIter.next();
            Iterator states = op.getAffectedItemStates().iterator();
            while (states.hasNext()) {
                ItemState state = (ItemState) states.next();
                if (affectedStates.contains(state)) {
                    changeLog.addOperation(op);
                    break;
                }
            }
        }
    }

    /**
     * Make sure that this save operation is totally 'self-contained'
     * and independant; items within the scope of this update operation
     * must not have 'external' dependencies;
     * (e.g. moving a node requires that the target node including both
     * old and new parents are saved)
     *
     * @param affectedStates
     * @param changeLog
     */
    private void checkIsSelfContained(Set affectedStates, ChangeLog changeLog) throws ItemStateException {
        Iterator it = new IteratorChain(changeLog.modifiedStates(), changeLog.deletedStates());
        while (it.hasNext()) {
            ItemState transientState = (ItemState) it.next();
            if (transientState.isNode()) {
                NodeState nodeState = (NodeState) transientState;
                Set dependentStates = new HashSet();
                if (nodeState.hasOverlayedState()) {
                    NodeState oldParentState = nodeState.getOverlayedState().getParent();
                    NodeState newParentState = nodeState.getParent();
                    if (oldParentState != null) {
                        if (newParentState == null) {
                            // node has been removed, add old parent
                            // to dependencies
                            dependentStates.add(oldParentState);
                        } else {
                            if (!oldParentState.equals(newParentState)) {
                                // node has been moved, add old and new parent
                                // to dependencies
                                dependentStates.add(oldParentState);
                                dependentStates.add(newParentState);
                            }
                        }
                    }
                }
                // removed child node entries
                Iterator cneIt = nodeState.getRemovedChildNodeEntries().iterator();
                while (cneIt.hasNext()) {
                    ChildNodeEntry cne = (ChildNodeEntry) cneIt.next();
                    dependentStates.add(cne.getNodeState());
                }
                // added child node entries
                cneIt = nodeState.getAddedChildNodeEntries().iterator();
                while (cneIt.hasNext()) {
                    ChildNodeEntry cne = (ChildNodeEntry) cneIt.next();
                    dependentStates.add(cne.getNodeState());
                }

                // now walk through dependencies and check whether they
                // are within the scope of this save operation
                Iterator depIt = dependentStates.iterator();
                while (depIt.hasNext()) {
                    NodeState dependantState = (NodeState) depIt.next();
                    if (!affectedStates.contains(dependantState)) {
                        // need to save the parent as well
                        String msg = LogUtil.safeGetJCRPath(dependantState, nsResolver, hierMgr) + " needs to be saved as well.";
                        log.debug(msg);
                        throw new ItemStateException(msg);
                    }
                }
            }
        }
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
    public void visit(Clone operation) throws NoSuchWorkspaceException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        workspaceItemStateMgr.execute(operation);
    }

    /**
     * @inheritDoc
     */
    public void visit(Copy operation) throws NoSuchWorkspaceException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        workspaceItemStateMgr.execute(operation);
    }

    /**
     * @inheritDoc
     */
    public void visit(Move operation) throws LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {

        // retrieve states and assert they are modifiable
        NodeState srcState = operation.getNodeState();
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
        // TODO: improve. definition has already retrieve within the checkAddNode...
        QNodeDefinition newDefinition = validator.getApplicableNodeDefinition(operation.getDestinationName(), srcState.getNodeTypeName(), destParent);

        // perform the move (modifying states)
        srcParent.moveChildNodeEntry(destParent, srcState, operation.getDestinationName());

        // change definition of target node
        srcState.setDefinition(newDefinition);

        // remember operation
        transientStateMgr.addOperation(operation);
    }

    public void visit(Update operation) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        workspaceItemStateMgr.execute(operation);
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
        // remember operation
        transientStateMgr.addOperation(operation);
    }

    /**
     * @inheritDoc
     */
    public void visit(SetMixin operation) throws ConstraintViolationException, AccessDeniedException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        // remember if an existing mixin is being removed.
        boolean anyRemoved;

        QName[] mixinNames = operation.getMixinNames();
        NodeState nState = operation.getNodeState();

        // mixin-names to be execute on the nodestate (and corresponding property state)
        if (mixinNames != null && mixinNames.length > 0) {
            // find out if any of the existing mixins is removed
            List originalMixins = Arrays.asList(nState.getMixinTypeNames());
            originalMixins.removeAll(Arrays.asList(mixinNames));
            anyRemoved = originalMixins.size() > 0;

            // update nodestate
            nState.setMixinTypeNames(mixinNames);

            // update/create corresponding property state
            if (nState.hasPropertyName(QName.JCR_MIXINTYPES)) {
                // execute value of existing property
                try {
                    PropertyState pState = nState.getPropertyState(QName.JCR_MIXINTYPES);
                    setPropertyStateValue(pState, QValue.create(mixinNames), PropertyType.NAME);
                } catch (ItemStateException e) {
                    // should not occur, since existance has been asserted before
                    throw new RepositoryException(e);
                }
            } else {
                // create new jcr:mixinTypes property
                EffectiveNodeType ent = validator.getEffectiveNodeType(nState);
                QPropertyDefinition pd = ent.getApplicablePropertyDefinition(QName.JCR_MIXINTYPES, PropertyType.NAME, true);
                QValue[] mixinValue = QValue.create(nState.getMixinTypeNames());
                int options = 0; // nothing to check
                addPropertyState(nState, pd.getQName(), pd.getRequiredType(), mixinValue, pd, options);
            }
        } else {
            anyRemoved = nState.getMixinTypeNames().length > 0;
            // remove all mixins
            nState.setMixinTypeNames(null);

            // remove the jcr:mixinTypes property state if already present
            if (nState.hasPropertyName(QName.JCR_MIXINTYPES)) {
                try {
                    PropertyState pState = nState.getPropertyState(QName.JCR_MIXINTYPES);
                    int options = 0; // no checks required
                    removeItemState(pState, options);
                } catch (ItemStateException e) {
                    // should not occur, since existance has been asserted before
                    throw new RepositoryException(e);
                }
            }
        }

        // make sure, the modification of the mixin set did not left child-item
        // states defined by the removed mixin type(s)
        // TODO: the following block should be delegated to 'server' - side.
        if (anyRemoved) {
            EffectiveNodeType ent = validator.getEffectiveNodeType(nState);
            // use temp set to avoid ConcurrentModificationException
            Iterator childProps = new HashSet(nState.getPropertyEntries()).iterator();
            while (childProps.hasNext()) {
                try {
                    ChildPropertyEntry entry = (ChildPropertyEntry) childProps.next();
                    PropertyState childState = entry.getPropertyState();
                    QName declNtName = childState.getDefinition().getDeclaringNodeType();
                    // check if property has been defined by mixin type (or one of its supertypes)
                    if (!ent.includesNodeType(declNtName)) {
                        // the remaining effective node type doesn't include the
                        // node type that declared this property, it is thus safe
                        // to remove it
                        int options = 0; // no checks required
                        removeItemState(childState, options);
                    }
                } catch (ItemStateException e) {
                    // ignore. cleanup will occure upon save anyway
                    log.error("Error while removing child node defined by removed mixin: {0}", e.getMessage());
                }
            }
            // use temp array to avoid ConcurrentModificationException
            Iterator childNodes = new ArrayList(nState.getChildNodeEntries()).iterator();
            while (childNodes.hasNext()) {
                try {
                    ChildNodeEntry entry = (ChildNodeEntry) childNodes.next();
                    NodeState childState = entry.getNodeState();
                    // check if node has been defined by mixin type (or one of its supertypes)
                    QName declNtName = childState.getDefinition().getDeclaringNodeType();
                    if (!ent.includesNodeType(declNtName)) {
                        // the remaining effective node type doesn't include the
                        // node type that declared this child node, it is thus safe
                        // to remove it.
                        int options = 0; // NOTE: referencial intergrity checked upon save.
                        removeItemState(childState, options);
                    }
                } catch (ItemStateException e) {
                    // ignore. cleanup will occure upon save anyway
                    log.error("Error while removing child property defined by removed mixin: {0}", e.getMessage());
                }
            }
        }

        transientStateMgr.addOperation(operation);
    }

    /**
     * @inheritDoc
     */
    public void visit(SetPropertyValue operation) throws ValueFormatException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        PropertyState pState = operation.getPropertyState();
        setPropertyStateValue(pState, operation.getValues(), operation.getPropertyType());
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

    public void visit(Checkout operation) throws RepositoryException, UnsupportedRepositoryOperationException {
        workspaceItemStateMgr.execute(operation);
    }

    public void visit(Checkin operation) throws UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        workspaceItemStateMgr.execute(operation);
    }

    public void visit(Restore operation) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        workspaceItemStateMgr.execute(operation);
    }

    public void visit(Merge operation) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        workspaceItemStateMgr.execute(operation);
    }

    public void visit(ResolveMergeConflict operation) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        workspaceItemStateMgr.execute(operation);
    }

    public void visit(LockOperation operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        workspaceItemStateMgr.execute(operation);
    }

    public void visit(LockRefresh operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        workspaceItemStateMgr.execute(operation);
    }

    public void visit(LockRelease operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        workspaceItemStateMgr.execute(operation);
    }

    public void visit(AddLabel operation) throws VersionException, RepositoryException {
        workspaceItemStateMgr.execute(operation);
    }

    public void visit(RemoveLabel operation) throws VersionException, RepositoryException {
        workspaceItemStateMgr.execute(operation);
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

        // make sure the arguements are consistent and do not violate the
        // given property definition.
        validator.validate(propertyType, values, pDef);

        // create property state
        PropertyState propState = transientStateMgr.createPropertyState(parent, propertyName);
        propState.setDefinition(pDef);

        // NOTE: callers must make sure, the property type is not 'undefined'
        propState.setType(propertyType);
        propState.setMultiValued(pDef.isMultiple());
        propState.setValues(values);
    }

    private void addNodeState(NodeState parent, QName nodeName, QName nodeTypeName, String uuid, QNodeDefinition definition, int options) throws RepositoryException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchNodeTypeException, ItemExistsException, VersionException {

        // TODO: improve...
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

        NodeState nodeState = transientStateMgr.createNodeState(nodeName, uuid, nodeTypeName, parent);
        nodeState.setDefinition(definition);

        EffectiveNodeType ent = validator.getEffectiveNodeType(nodeState);
        // add 'auto-create' properties defined in node type
        QPropertyDefinition[] pda = ent.getAutoCreatePropDefs();
        for (int i = 0; i < pda.length; i++) {
            QPropertyDefinition pd = pda[i];
            QValue[] autoValue = computeSystemGeneratedPropertyValues(nodeState, pd);
            if (autoValue != null) {
                int propOptions = 0; // nothing to check
                // execute 'addProperty' without adding operation.
                addPropertyState(nodeState, pd.getQName(), pd.getRequiredType(), autoValue, pd, propOptions);
            }
        }

        // recursively add 'auto-create' child nodes defined in node type
        QNodeDefinition[] nda = ent.getAutoCreateNodeDefs();
        for (int i = 0; i < nda.length; i++) {
            QNodeDefinition nd = nda[i];
            // execute 'addNode' without validation and adding operation.
            int opt = ItemStateValidator.CHECK_LOCK | ItemStateValidator.CHECK_COLLISION;
            addNodeState(nodeState, nd.getQName(), nd.getDefaultPrimaryType(), null, nd, opt);
        }
    }

    private void removeItemState(ItemState itemState, int options) throws RepositoryException {
        // DIFF JR: check for both, node- and propertyState
        validator.checkRemoveItem(itemState, options);

        // recursively remove the complete tree including the given node state.
        boolean success = false;
        try {
            itemState.remove();
            success = true;
        } catch (ItemStateException e) {
            throw new RepositoryException("Cannot remove item: " + e.getMessage(), e);
        } finally {
            if (!success) {
                // TODO: undo state modifications
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
    private void setPropertyStateValue(PropertyState propState, QValue[] iva, int valueType) throws ValueFormatException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        // assert that the property can be modified.
        // TODO improve
        int options = ItemStateValidator.CHECK_LOCK //| ItemStateValidator.CHECK_COLLISION
            | ItemStateValidator.CHECK_VERSIONING | ItemStateValidator.CHECK_CONSTRAINTS;
        validator.checkSetProperty(propState, options);

        // make sure property is valid according to its definition
        validator.validate(valueType, iva, propState.getDefinition());

        // free old values as necessary
        QValue[] oldValues = propState.getValues();
        if (oldValues != null) {
            for (int i = 0; i < oldValues.length; i++) {
                QValue old = oldValues[i];
                if (old != null) {
                    // make sure temporarily allocated data is discarded
                    // before overwriting it (see QValue#discard())
                    old.discard();
                }
            }
        }

        propState.setValues(iva);
        propState.setType(valueType);
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
        /**
         * todo: need to come up with some callback mechanism for applying system generated values
         * (e.g. using a NodeTypeInstanceHandler interface)
         */
        String[] qDefaultValues = def.getDefaultValues();
        if (qDefaultValues != null && qDefaultValues.length > 0) {
            if (def.getRequiredType() == PropertyType.BINARY) {
                try {
                    genValues = QValue.create(def.getDefaultValuesAsStream(), def.getRequiredType());
                } catch (IOException e) {
                    log.error("Internal error while build QValue from property definition: ", e.getMessage());
                    return null;
                }
            } else {
               genValues = QValue.create(qDefaultValues, def.getRequiredType());
            }
        } else {
            // some predefined nodetypes declare auto-created properties without
            // default values
            QName declaringNT = def.getDeclaringNodeType();
            QName name = def.getQName();
            if (QName.MIX_REFERENCEABLE.equals(declaringNT) && QName.JCR_UUID.equals(name)) {
                // mix:referenceable node type defines jcr:uuid
                genValues = new QValue[]{QValue.create(parent.getNodeId().getUUID().toString())};
            } else if (QName.NT_BASE.equals(declaringNT)) {
                // nt:base node type
                if (QName.JCR_PRIMARYTYPE.equals(name)) {
                    // jcr:primaryType property
                    genValues = new QValue[]{QValue.create(parent.getNodeTypeName())};
                } else if (QName.JCR_MIXINTYPES.equals(name)) {
                    // jcr:mixinTypes property
                    QName[] mixins = parent.getMixinTypeNames();
                    genValues = new QValue[mixins.length];
                    for (int i = 0; i < mixins.length; i++) {
                        genValues[i] = QValue.create(mixins[i]);
                    }
                }
            } else if (QName.NT_HIERARCHYNODE.equals(declaringNT) && QName.JCR_CREATED.equals(name)) {
                // nt:hierarchyNode node type defines jcr:created property
                genValues = new QValue[]{QValue.create(Calendar.getInstance())};
            } else if (QName.NT_RESOURCE.equals(declaringNT) && QName.JCR_LASTMODIFIED.equals(name)) {
                // nt:resource node type defines jcr:lastModified property
                genValues = new QValue[]{QValue.create(Calendar.getInstance())};
            } else if (QName.NT_VERSION.equals(declaringNT) && QName.JCR_CREATED.equals(name)) {
                // nt:version node type defines jcr:created property
                genValues = new QValue[]{QValue.create(Calendar.getInstance())};
            }
        }
        return genValues;
    }
}
