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
package org.apache.jackrabbit.core.state;

import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.jackrabbit.core.CachingHierarchyManager;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.ZombieHierarchyManager;
import org.apache.jackrabbit.core.util.Dumpable;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;
import org.apache.log4j.Logger;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * <code>SessionItemStateManager</code> ...
 */
public class SessionItemStateManager
        implements UpdatableItemStateManager, Dumpable {

    private static Logger log = Logger.getLogger(SessionItemStateManager.class);

    /**
     * State manager that allows updates
     */
    private final UpdatableItemStateManager persistentStateMgr;

    /**
     * State manager for the transient items
     */
    private final TransientItemStateManager transientStateMgr;

    /**
     * Hierarchy manager
     */
    private CachingHierarchyManager hierMgr;

    /**
     * Creates a new <code>SessionItemStateManager</code> instance.
     *
     * @param rootNodeId
     * @param persistentStateMgr
     * @param nsResolver
     */
    public SessionItemStateManager(NodeId rootNodeId,
                                   UpdatableItemStateManager persistentStateMgr,
                                   NamespaceResolver nsResolver) {

        this.persistentStateMgr = persistentStateMgr;
        // create transient item state manager
        transientStateMgr = new TransientItemStateManager();
        // create hierarchy manager that uses both transient and persistent state
        hierMgr = new CachingHierarchyManager(rootNodeId, this, nsResolver);
    }

    /**
     * Returns the hierarchy manager
     *
     * @return the hierarchy manager
     */
    public HierarchyManager getHierarchyMgr() {
        return hierMgr;
    }

    //-------------------------------------------------------------< Dumpable >
    /**
     * {@inheritDoc}
     */
    public void dump(PrintStream ps) {
        ps.println("SessionItemStateManager (" + this + ")");
        ps.println();
        transientStateMgr.dump(ps);
        ps.println();
    }

    //-----------------------------------------------------< ItemStateManager >
    /**
     * {@inheritDoc}
     */
    public ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {

        // first check if the specified item has been transiently removed
        if (transientStateMgr.getAttic().hasItemState(id)) {
            /**
             * check if there's new transient state for the specified item
             * (e.g. if a property with name 'x' has been removed and a new
             * property with same name has been created);
             * this will throw a NoSuchItemStateException if there's no new
             * transient state
             */
            return transientStateMgr.getItemState(id);
        }

        // check if there's transient state for the specified item
        if (transientStateMgr.hasItemState(id)) {
            return transientStateMgr.getItemState(id);
        }

        // check if there's persistent state for the specified item
        if (persistentStateMgr.hasItemState(id)) {
            return persistentStateMgr.getItemState(id);
        }

        throw new NoSuchItemStateException(id.toString());
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasItemState(ItemId id) {
        // first check if the specified item has been transiently removed
        if (transientStateMgr.getAttic().hasItemState(id)) {
            /**
             * check if there's new transient state for the specified item
             * (e.g. if a property with name 'x' has been removed and a new
             * property with same name has been created);
             */
            return transientStateMgr.hasItemState(id);
        }
        // check if there's transient state for the specified item
        if (transientStateMgr.hasItemState(id)) {
            return true;
        }
        // check if there's persistent state for the specified item
        return persistentStateMgr.hasItemState(id);
    }

    /**
     * {@inheritDoc}
     */
    public NodeReferences getNodeReferences(NodeReferencesId id)
            throws NoSuchItemStateException, ItemStateException {

        return persistentStateMgr.getNodeReferences(id);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNodeReferences(NodeReferencesId id) {
        return persistentStateMgr.hasNodeReferences(id);
    }

    //--------------------------------------------< UpdatableItemStateManager >
    /**
     * {@inheritDoc}
     */
    public void edit() throws IllegalStateException {
        persistentStateMgr.edit();
    }

    /**
     * {@inheritDoc}
     */
    public boolean inEditMode() throws IllegalStateException {
        return persistentStateMgr.inEditMode();
    }

    /**
     * {@inheritDoc}
     */
    public NodeState createNew(NodeId id, QName nodeTypeName,
                               NodeId parentId)
            throws IllegalStateException {
        return persistentStateMgr.createNew(id, nodeTypeName, parentId);
    }

    /**
     * Customized variant of {@link #createNew(NodeId, QName, NodeId)} that
     * connects the newly created persistent state with the transient state.
     */
    public NodeState createNew(NodeState transientState)
            throws IllegalStateException {

        NodeState persistentState = createNew(transientState.getNodeId(),
                transientState.getNodeTypeName(),
                transientState.getParentId());
        transientState.connect(persistentState);
        return persistentState;
    }

    /**
     * {@inheritDoc}
     */
    public PropertyState createNew(QName propName, NodeId parentId)
            throws IllegalStateException {
        return persistentStateMgr.createNew(propName, parentId);
    }

    /**
     * Customized variant of {@link #createNew(QName, NodeId)} that
     * connects the newly created persistent state with the transient state.
     */
    public PropertyState createNew(PropertyState transientState)
            throws IllegalStateException {

        PropertyState persistentState = createNew(transientState.getName(),
                transientState.getParentId());
        transientState.connect(persistentState);
        return persistentState;
    }


    /**
     * {@inheritDoc}
     */
    public void store(ItemState state) throws IllegalStateException {
        persistentStateMgr.store(state);
    }

    /**
     * {@inheritDoc}
     */
    public void destroy(ItemState state) throws IllegalStateException {
        persistentStateMgr.destroy(state);
    }

    /**
     * {@inheritDoc}
     */
    public void cancel() throws IllegalStateException {
        persistentStateMgr.cancel();
    }

    /**
     * {@inheritDoc}
     */
    public void update()
            throws ReferentialIntegrityException, StaleItemStateException,
            ItemStateException, IllegalStateException {
        persistentStateMgr.update();
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        // discard all transient changes
        transientStateMgr.disposeAllItemStates();
        // dispose our (i.e. 'local') state manager
        persistentStateMgr.dispose();
    }

    //< more methods for listing and retrieving transient ItemState instances >

    /**
     * @param id
     * @return
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    public ItemState getTransientItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {
        return transientStateMgr.getItemState(id);
    }

    /**
     * @return <code>true</code> if this manager has any transient state;
     *         <code>false</code> otherwise.
     */
    public boolean hasAnyTransientItemStates() {
        return transientStateMgr.hasAnyItemStates();
    }

    /**
     * Returns an iterator over those transient item state instances that are
     * direct or indirect descendents of the item state with the given
     * <code>parentId</code>. The transient item state instance with the given
     * <code>parentId</code> itself (if there is such) will not be included.
     * <p/>
     * The instances are returned in depth-first tree traversal order.
     *
     * @param parentId the id of the common parent of the transient item state
     *                 instances to be returned.
     * @return an iterator over descendant transient item state instances
     * @throws InvalidItemStateException if any descendant item state has been
     *                                   deleted externally
     * @throws RepositoryException       if another error occurs
     */
    public Iterator getDescendantTransientItemStates(NodeId parentId)
            throws InvalidItemStateException, RepositoryException {
        if (!transientStateMgr.hasAnyItemStates()) {
            return Collections.EMPTY_LIST.iterator();
        }

        // build ordered collection of descendant transient states
        // sorted by decreasing relative depth

        // use an array of lists to group the descendants by relative depth;
        // the depth is used as array index
        List[] la = new List[10];
        try {
            Iterator iter = transientStateMgr.getEntries();
            while (iter.hasNext()) {
                ItemState state = (ItemState) iter.next();
                // determine relative depth: > 0 means it's a descendant
                int depth;
                try {
                    depth = hierMgr.getRelativeDepth(parentId, state.getId());
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
     * Same as <code>{@link #getDescendantTransientItemStates(NodeId)}</code>
     * except that item state instances in the attic are returned.
     *
     * @param parentId the id of the common parent of the transient item state
     *                 instances to be returned.
     * @return an iterator over descendant transient item state instances in the attic
     */
    public Iterator getDescendantTransientItemStatesInAttic(NodeId parentId) {
        if (!transientStateMgr.hasAnyItemStatesInAttic()) {
            return Collections.EMPTY_LIST.iterator();
        }

        // build ordered collection of descendant transient states in attic
        // sorted by decreasing relative depth

        // use a special attic-aware hierarchy manager
        ZombieHierarchyManager zombieHierMgr =
                new ZombieHierarchyManager(hierMgr.getRootNodeId(),
                        this,
                        transientStateMgr.getAttic(),
                        hierMgr.getNamespaceResolver());

        // use an array of lists to group the descendants by relative depth;
        // the depth is used as array index
        List[] la = new List[10];
        try {
            Iterator iter = transientStateMgr.getEntriesInAttic();
            while (iter.hasNext()) {
                ItemState state = (ItemState) iter.next();
                // determine relative depth: > 0 means it's a descendant
                int depth = zombieHierMgr.getRelativeDepth(parentId, state.getId());
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
     * Return a flag indicating whether the specified item is in the transient
     * item state manager's attic space.
     *
     * @param id item id
     * @return <code>true</code> if the item state is in the attic space;
     *         <code>false</code> otherwise
     */
    public boolean isItemStateInAttic(ItemId id) {
        return transientStateMgr.getAttic().hasItemState(id);
    }

    //------< methods for creating & discarding transient ItemState instances >
    /**
     * @param id
     * @param nodeTypeName
     * @param parentId
     * @param initialStatus
     * @return
     * @throws ItemStateException
     */
    public NodeState createTransientNodeState(NodeId id, QName nodeTypeName, NodeId parentId, int initialStatus)
            throws ItemStateException {
        return transientStateMgr.createNodeState(id, nodeTypeName, parentId, initialStatus);
    }

    /**
     * @param overlayedState
     * @param initialStatus
     * @return
     * @throws ItemStateException
     */
    public NodeState createTransientNodeState(NodeState overlayedState, int initialStatus)
            throws ItemStateException {

        NodeState state = transientStateMgr.createNodeState(overlayedState, initialStatus);
        hierMgr.stateOverlaid(state);
        return state;
    }

    /**
     * @param parentId
     * @param propName
     * @param initialStatus
     * @return
     * @throws ItemStateException
     */
    public PropertyState createTransientPropertyState(NodeId parentId, QName propName, int initialStatus)
            throws ItemStateException {
        return transientStateMgr.createPropertyState(parentId, propName, initialStatus);
    }

    /**
     * @param overlayedState
     * @param initialStatus
     * @return
     * @throws ItemStateException
     */
    public PropertyState createTransientPropertyState(PropertyState overlayedState, int initialStatus)
            throws ItemStateException {

        PropertyState state = transientStateMgr.createPropertyState(overlayedState, initialStatus);
        hierMgr.stateOverlaid(state);
        return state;
    }

    /**
     * Disconnect a transient item state from its underlying persistent state.
     * Notifies the <code>HierarchyManager</code> about the changed identity.
     *
     * @param state the transient <code>ItemState</code> instance that should
     *              be disconnected
     */
    public void disconnectTransientItemState(ItemState state) {
        hierMgr.stateUncovered(state);
        state.disconnect();
    }

    /**
     * Disposes the specified transient item state instance, i.e. discards it
     * and clears it from cache.
     *
     * @param state the transient <code>ItemState</code> instance that should
     *              be disposed
     * @see ItemState#discard()
     */
    public void disposeTransientItemState(ItemState state) {
        transientStateMgr.disposeItemState(state);
    }

    /**
     * Transfers the specified transient item state instance from the 'active'
     * cache to the attic.
     *
     * @param state the transient <code>ItemState</code> instance that should
     *              be moved to the attic
     */
    public void moveTransientItemStateToAttic(ItemState state) {
        transientStateMgr.moveItemStateToAttic(state);
    }

    /**
     * Disposes the specified transient item state instance in the attic, i.e.
     * discards it and removes it from the attic.
     *
     * @param state the transient <code>ItemState</code> instance that should
     *              be disposed @see ItemState#discard()
     */
    public void disposeTransientItemStateInAttic(ItemState state) {
        transientStateMgr.disposeItemStateInAttic(state);
    }

    /**
     * Disposes all transient item states in the cache and in the attic.
     */
    public void disposeAllTransientItemStates() {
        transientStateMgr.disposeAllItemStates();
    }
}
