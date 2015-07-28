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
package org.apache.jackrabbit.core.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.CachingHierarchyManager;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ZombieHierarchyManager;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.spi.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Item state manager that handles both transient and persistent items.
 */
public class SessionItemStateManager
        implements UpdatableItemStateManager, NodeStateListener {

    private static Logger log = LoggerFactory.getLogger(SessionItemStateManager.class);

    /**
     * State manager that allows updates
     */
    private final LocalItemStateManager stateMgr;

    /**
     * Hierarchy manager
     */
    private CachingHierarchyManager hierMgr;

    /**
     * map of those states that have been removed transiently
     */
    private final Map<ItemId, ItemState> atticStore =
            Collections.synchronizedMap(new HashMap<ItemId, ItemState>());

    /**
     * map of new or modified transient states
     */
    private final Map<ItemId, ItemState> transientStore =
            Collections.synchronizedMap(new HashMap<ItemId, ItemState>());

    /**
     * ItemStateManager view of the states in the attic; lazily instantiated
     * in {@link #getAttic()}
     */
    private AtticItemStateManager attic;

    /**
     * State change dispatcher.
     */
    private final transient StateChangeDispatcher dispatcher = new StateChangeDispatcher();

    /**
     * Creates a new <code>SessionItemStateManager</code> instance.
     *
     * @param rootNodeId the root node id
     * @param stateMgr the local item state manager
     */
    public SessionItemStateManager(
            NodeId rootNodeId, LocalItemStateManager stateMgr) {
        this.stateMgr = stateMgr;

        // create hierarchy manager that uses both transient and persistent state
        hierMgr = new CachingHierarchyManager(rootNodeId, this);
        addListener(hierMgr);
    }

    /**
     * Returns the hierarchy manager
     *
     * @return the hierarchy manager
     */
    public HierarchyManager getHierarchyMgr() {
        return hierMgr;
    }

    /**
     * Returns an attic-aware hierarchy manager, i.e. an hierarchy manager that
     * is also able to build/resolve paths of those items that have been moved
     * or removed (i.e. moved to the attic).
     *
     * @return an attic-aware hierarchy manager
     */
    public HierarchyManager getAtticAwareHierarchyMgr() {
        return new ZombieHierarchyManager(hierMgr, this, getAttic());
    }

    //--------------------------------------------------------------< Object >

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SessionItemStateManager (" + super.toString() + ")\n");
        builder.append("[transient]\n");
        builder.append(transientStore);
        builder.append("[attic]\n");
        builder.append(atticStore);
        return builder.toString();
    }

    //-----------------------------------------------------< ItemStateManager >

    /**
     * {@inheritDoc}
     */
    public ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {

        // first check if the specified item has been transiently removed
        if (atticStore.containsKey(id)) {
            /**
             * check if there's new transient state for the specified item
             * (e.g. if a property with name 'x' has been removed and a new
             * property with same name has been created);
             * this will throw a NoSuchItemStateException if there's no new
             * transient state
             */
            return getTransientItemState(id);
        }

        // check if there's transient state for the specified item
        if (transientStore.containsKey(id)) {
            return getTransientItemState(id);
        }

        return stateMgr.getItemState(id);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasItemState(ItemId id) {
        // first check if the specified item has been transiently removed
        if (atticStore.containsKey(id)) {
            /**
             * check if there's new transient state for the specified item
             * (e.g. if a property with name 'x' has been removed and a new
             * property with same name has been created);
             */
            return transientStore.containsKey(id);
        }
        // check if there's transient state for the specified item
        if (transientStore.containsKey(id)) {
            return true;
        }
        // check if there's persistent state for the specified item
        return stateMgr.hasItemState(id);
    }

    /**
     * {@inheritDoc}
     */
    public NodeReferences getNodeReferences(NodeId id)
            throws NoSuchItemStateException, ItemStateException {

        return stateMgr.getNodeReferences(id);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNodeReferences(NodeId id) {
        return stateMgr.hasNodeReferences(id);
    }

    //--------------------------------------------< UpdatableItemStateManager >

    /**
     * {@inheritDoc}
     */
    public void edit() throws IllegalStateException {
        stateMgr.edit();
    }

    /**
     * {@inheritDoc}
     */
    public boolean inEditMode() {
        return stateMgr.inEditMode();
    }

    /**
     * {@inheritDoc}
     */
    public NodeState createNew(
            NodeId id, Name nodeTypeName, NodeId parentId)
            throws RepositoryException {
        return stateMgr.createNew(id, nodeTypeName, parentId);
    }

    /**
     * {@inheritDoc}
     */
    public PropertyState createNew(Name propName, NodeId parentId)
            throws IllegalStateException {
        return stateMgr.createNew(propName, parentId);
    }

    /**
     * Customized variant of {@link #createNew(Name, NodeId)} that
     * connects the newly created persistent state with the transient state.
     */
    public PropertyState createNew(PropertyState transientState)
            throws ItemStateException {
        PropertyState persistentState = createNew(transientState.getName(),
                transientState.getParentId());
        transientState.connect(persistentState);
        return persistentState;
    }


    /**
     * {@inheritDoc}
     */
    public void store(ItemState state) throws IllegalStateException {
        stateMgr.store(state);
    }

    /**
     * {@inheritDoc}
     */
    public void destroy(ItemState state) throws IllegalStateException {
        assert state != null;
        stateMgr.destroy(state);
    }

    /**
     * {@inheritDoc}
     */
    public void cancel() throws IllegalStateException {
        stateMgr.cancel();
    }

    /**
     * {@inheritDoc}
     */
    public void update()
            throws ReferentialIntegrityException, StaleItemStateException,
            ItemStateException, IllegalStateException {
        stateMgr.update();
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        // remove hierarchy manager as listener to avoid
        // unnecessary work during stateMgr.dispose()
        removeListener(hierMgr);
        // discard all transient changes
        disposeAllTransientItemStates();
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

        ItemState state = transientStore.get(id);
        if (state != null) {
            return state;
        } else {
            throw new NoSuchItemStateException(id.toString());
        }
    }

    /**
     *
     * @param id
     * @return
     */
    public boolean hasTransientItemState(ItemId id) {
        return transientStore.containsKey(id);
    }

    /**
     *
     * @param id
     * @return
     */
    public boolean hasTransientItemStateInAttic(ItemId id) {
        return atticStore.containsKey(id);
    }

    /**
     * @return <code>true</code> if this manager has any transient state;
     *         <code>false</code> otherwise.
     */
    public boolean hasAnyTransientItemStates() {
        return !transientStore.isEmpty();
    }

    /**
     * Returns a collection of those transient item state instances that are
     * direct or indirect descendants of the item state with the given parent.
     * The transient item state instance with the given identifier itself
     * (if there is such) will not be included.
     * <p>
     * The instances are returned in depth-first tree traversal order.
     *
     * @param id identifier of the common parent of the transient item state
     *           instances to be returned
     * @return collection of descendant transient item state instances
     * @throws InvalidItemStateException if any descendant item state has been
     *                                   deleted externally
     * @throws RepositoryException       if another error occurs
     */
    public Collection<ItemState> getDescendantTransientItemStates(ItemId id)
            throws InvalidItemStateException, RepositoryException {
        try {
            return getDescendantItemStates(
                    id, transientStore, getAtticAwareHierarchyMgr());
        } catch (ItemNotFoundException e) {
            // one of the parents of the specified item has been
            // removed externally; as we don't know its path,
            // we can't determine if it is a descendant;
            // InvalidItemStateException should only be thrown if
            // a descendant is affected;
            // => throw InvalidItemStateException for now (FIXME)
            // unable to determine relative depth, assume that the item
            // (or any of its ancestors) has been removed externally
            throw new InvalidItemStateException(
                    "Item seems to have been removed externally", e);
        }
    }

    /**
     * Same as <code>{@link #getDescendantTransientItemStates(ItemId)}</code>
     * except that item state instances in the attic are returned.
     *
     * @param id identifier of the common parent of the transient item state
     *           instances to be returned
     * @return collection of descendant transient item state instances
     *         in the attic
     */
    public Iterable<ItemState> getDescendantTransientItemStatesInAttic(
            ItemId id) throws RepositoryException {
        return getDescendantItemStates(
                id, atticStore,
                new ZombieHierarchyManager(hierMgr, this, getAttic()));
    }

    /**
     * Utility method used by the
     * {@link #getDescendantTransientItemStates(ItemId)} and
     * {@link #getDescendantTransientItemStatesInAttic(ItemId)} methods
     * to collect descendant item states from the given item state store.
     *
     * @param id identifier of the parent item
     * @param store item state store
     * @param hierarchyManager hierarchy manager associated with the store
     * @return descendants of the identified item
     * @throws RepositoryException if the descendants could not be accessed
     */
    private List<ItemState> getDescendantItemStates(
            ItemId id, Map<ItemId, ItemState> store,
            HierarchyManager hierarchyManager) throws RepositoryException {
        if (id.denotesNode() && !store.isEmpty()) {
            // Group the descendants by reverse relative depth
            SortedMap<Integer, Collection<ItemState>> statesByReverseDepth =
                new TreeMap<Integer, Collection<ItemState>>();
            ItemState[] states = store.values().toArray(new ItemState[0]);
            for (ItemState state : states) {
                // determine relative depth: > 0 means it's a descendant
                int depth = hierarchyManager.getShareRelativeDepth(
                        (NodeId) id, state.getId());
                if (depth > 0) {
                    Collection<ItemState> statesAtDepth =
                        statesByReverseDepth.get(-depth);
                    if (statesAtDepth == null) {
                        statesAtDepth = new ArrayList<ItemState>();
                        statesByReverseDepth.put(-depth, statesAtDepth);
                    }
                    statesAtDepth.add(state);
                }
            }

            // Collect the descendants in decreasing depth order
            List<ItemState> descendants = new ArrayList<ItemState>();
            for (Collection<ItemState> statesAtDepth
                    : statesByReverseDepth.values()) {
                descendants.addAll(statesAtDepth);
            }
            return descendants;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Returns the id of the root of the minimal subtree including all
     * transient states.
     *
     * @return id of nearest common ancestor of all transient states or null
     *         if there's no transient state.
     * @throws RepositoryException if an error occurs
     */
    public NodeId getIdOfRootTransientNodeState() throws RepositoryException {
        if (transientStore.isEmpty()) {
            return null;
        }

        // short cut
        if (transientStore.containsKey(hierMgr.getRootNodeId())) {
            return hierMgr.getRootNodeId();
        }

        // the nearest common ancestor of all transient states
        // must be either
        // a) a node state with STATUS_EXISTING_MODIFIED or STATUS_STALE_DESTROYED, or
        // b) the parent node of a property state with STATUS_EXISTING_MODIFIED or STATUS_STALE_DESTROYED

        // collect all candidates based on above criteria
        Collection<NodeId> candidateIds = new LinkedList<NodeId>();
        try {
            HierarchyManager hierMgr = getHierarchyMgr();
            ItemState[] states =
                    transientStore.values().toArray(new ItemState[0]);
            for (ItemState state : states) {
                if (state.getStatus() == ItemState.STATUS_EXISTING_MODIFIED
                        || state.getStatus() == ItemState.STATUS_STALE_DESTROYED) {
                    NodeId nodeId;
                    if (state.isNode()) {
                        nodeId = (NodeId) state.getId();
                    } else {
                        nodeId = state.getParentId();
                    }
                    // remove any descendant candidates
                    boolean skip = false;
                    for (Iterator<NodeId> it = candidateIds.iterator(); it.hasNext();) {
                        NodeId id = it.next();
                        if (nodeId.equals(id) || hierMgr.isAncestor(id, nodeId)) {
                            // already a candidate or a descendant thereof
                            // => skip
                            skip = true;
                            break;
                        }
                        if (hierMgr.isAncestor(nodeId, id)) {
                            // candidate is a descendant => remove
                            it.remove();
                        }
                    }
                    if (!skip) {
                        // add to candidates
                        candidateIds.add(nodeId);
                    }
                }
            }

            if (candidateIds.size() == 1) {
                return candidateIds.iterator().next();
            }

            // pick (any) candidate with shortest path to start with
            NodeId candidateId = null;
            for (NodeId id : candidateIds) {
                if (candidateId == null) {
                    candidateId = id;
                } else {
                    if (hierMgr.getDepth(id) < hierMgr.getDepth(candidateId)) {
                        candidateId = id;
                    }
                }
            }

            // starting with this candidate closest to root, find first parent
            // which is an ancestor of all candidates
            NodeState state = (NodeState) getItemState(candidateId);
            NodeId parentId = state.getParentId();
            boolean continueWithParent = false;
            while (parentId != null) {
                for (NodeId id : candidateIds) {
                    if (hierMgr.getRelativeDepth(parentId, id) == -1) {
                        continueWithParent = true;
                        break;
                    }
                }
                if (continueWithParent) {
                    state = (NodeState) getItemState(parentId);
                    parentId = state.getParentId();
                    continueWithParent = false;
                } else {
                    break;
                }
            }
            return parentId;
        } catch (ItemStateException e) {
            throw new RepositoryException("failed to determine common root of transient changes", e);
        }
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
        return atticStore.containsKey(id);
    }

    //------< methods for creating & discarding transient ItemState instances >

    /**
     * @param id
     * @param nodeTypeName
     * @param parentId
     * @param initialStatus
     * @return
     * @throws RepositoryException
     */
    public NodeState createTransientNodeState(NodeId id, Name nodeTypeName, NodeId parentId, int initialStatus)
            throws RepositoryException {
        if (initialStatus == ItemState.STATUS_NEW && id != null
                && hasItemState(id)) {
            throw new InvalidItemStateException(
                    "Node " + id + " already exists");
        }

        // check map; synchronized to ensure an entry is not created twice.
        synchronized (transientStore) {
            if (id == null) {
                id = stateMgr.getNodeIdFactory().newNodeId();
            } else if (transientStore.containsKey(id)) {
                throw new RepositoryException(
                        "There is already a transient state for node " + id);
            }

            NodeState state = new NodeState(
                    id, nodeTypeName, parentId, initialStatus, true);
            // put transient state in the map
            transientStore.put(state.getId(), state);
            state.setContainer(this);
            return state;
        }
    }

    /**
     * @param overlayedState
     * @param initialStatus
     * @return
     * @throws ItemStateException
     */
    public NodeState createTransientNodeState(NodeState overlayedState, int initialStatus)
            throws ItemStateException {

        ItemId id = overlayedState.getNodeId();

        // check map; synchronized to ensure an entry is not created twice.
        synchronized (transientStore) {
            if (transientStore.containsKey(id)) {
                String msg = "there's already a node state instance with id " + id;
                log.debug(msg);
                throw new ItemStateException(msg);
            }

            NodeState state = new NodeState(overlayedState, initialStatus, true);
            // put transient state in the map
            transientStore.put(id, state);
            state.setContainer(this);
            return state;
        }
    }

    /**
     * @param parentId
     * @param propName
     * @param initialStatus
     * @return
     * @throws ItemStateException
     */
    public PropertyState createTransientPropertyState(NodeId parentId, Name propName, int initialStatus)
            throws ItemStateException {

        PropertyId id = new PropertyId(parentId, propName);

        // check map; synchronized to ensure an entry is not created twice.
        synchronized (transientStore) {
            if (transientStore.containsKey(id)) {
                String msg = "there's already a property state instance with id " + id;
                log.debug(msg);
                throw new ItemStateException(msg);
            }

            PropertyState state = new PropertyState(id, initialStatus, true);
            // put transient state in the map
            transientStore.put(id, state);
            state.setContainer(this);
            return state;
        }
    }

    /**
     * @param overlayedState
     * @param initialStatus
     * @return
     * @throws ItemStateException
     */
    public PropertyState createTransientPropertyState(PropertyState overlayedState, int initialStatus)
            throws ItemStateException {

        PropertyId id = overlayedState.getPropertyId();

        // check map; synchronized to ensure an entry is not created twice.
        synchronized (transientStore) {
            if (transientStore.containsKey(id)) {
                String msg = "there's already a property state instance with id " + id;
                log.debug(msg);
                throw new ItemStateException(msg);
            }

            PropertyState state = new PropertyState(overlayedState, initialStatus, true);
            // put transient state in the map
            transientStore.put(id, state);
            state.setContainer(this);
            return state;
        }
    }

    /**
     * Disconnect a transient item state from its underlying persistent state.
     * Notifies the <code>HierarchyManager</code> about the changed identity.
     *
     * @param state the transient <code>ItemState</code> instance that should
     *              be disconnected
     */
    public void disconnectTransientItemState(ItemState state) {
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
        // discard item state, this will invalidate the wrapping Item
        // instance of the transient state
        state.discard();
        // remove from map
        transientStore.remove(state.getId());
        // give the instance a chance to prepare to get gc'ed
        state.onDisposed();
    }

    /**
     * Transfers the specified transient item state instance from the 'active'
     * cache to the attic.
     *
     * @param state the transient <code>ItemState</code> instance that should
     *              be moved to the attic
     */
    public void moveTransientItemStateToAttic(ItemState state) {
        // remove from map
        transientStore.remove(state.getId());
        // add to attic
        atticStore.put(state.getId(), state);
    }

    /**
     * Disposes the specified transient item state instance in the attic, i.e.
     * discards it and removes it from the attic.
     *
     * @param state the transient <code>ItemState</code> instance that should
     *              be disposed @see ItemState#discard()
     */
    public void disposeTransientItemStateInAttic(ItemState state) {
        // discard item state, this will invalidate the wrapping Item
        // instance of the transient state
        state.discard();
        // remove from attic
        atticStore.remove(state.getId());
        // give the instance a chance to prepare to get gc'ed
        state.onDisposed();
    }

    /**
     * Disposes all transient item states in the cache and in the attic.
     */
    public void disposeAllTransientItemStates() {
        // dispose item states in transient map & attic
        // (use temp collection to avoid ConcurrentModificationException)
        ItemState[] tmp;
        tmp = transientStore.values().toArray(new ItemState[0]);
        for (ItemState state : tmp) {
            disposeTransientItemState(state);
        }
        tmp = atticStore.values().toArray(new ItemState[0]);
        for (ItemState state : tmp) {
            disposeTransientItemStateInAttic(state);
        }
    }

    /**
     * Add an <code>ItemStateListener</code>
     *
     * @param listener the new listener to be informed on modifications
     */
    public void addListener(ItemStateListener listener) {
        dispatcher.addListener(listener);
    }

    /**
     * Remove an <code>ItemStateListener</code>
     *
     * @param listener an existing listener
     */
    public void removeListener(ItemStateListener listener) {
        dispatcher.removeListener(listener);
    }

    /**
     * Return the attic item state provider that holds all items
     * moved into the attic.
     *
     * @return attic
     */
    public ItemStateManager getAttic() {
        if (attic == null) {
            attic = new AtticItemStateManager();
        }
        return attic;
    }

    //----------------------------------------------------< ItemStateListener >

    /**
     * {@inheritDoc}
     * <p>
     * Notification handler gets called for both transient states that this state manager
     * has created, as well as states that were created by the local state manager
     * we're listening to.
     */
    public void stateCreated(ItemState created) {
        ItemState visibleState = created;
        if (created.getContainer() != this) {
            // local state was created
            ItemState transientState = transientStore.get(created.getId());
            if (transientState != null) {
                if (transientState.hasOverlayedState()) {
                    // underlying state has been permanently created
                    transientState.pull();
                    transientState.setStatus(ItemState.STATUS_EXISTING);
                } else {
                    // this is a notification from another session
                    try {
                        ItemState local = stateMgr.getItemState(created.getId());
                        transientState.connect(local);
                        // update mod count
                        transientState.setModCount(local.getModCount());
                        transientState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
                    } catch (ItemStateException e) {
                        // something went wrong
                        transientState.setStatus(ItemState.STATUS_UNDEFINED);
                    }
                }
                visibleState = transientState;
            }
        }
        boolean notifyTransientSpace;
        if (visibleState instanceof NodeState) {
            // No need to push "node created" to transient space:
            // either the transient already knows about this state, or it doesn't.
            // If we notify in this case, this can lead to a deadlock,
            // see JCR-3226.
            notifyTransientSpace = false;
        } else {
            notifyTransientSpace = true;
        }
        if (notifyTransientSpace) {
            dispatcher.notifyStateCreated(visibleState);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Notification handler gets called for both transient states that this state manager
     * has created, as well as states that were created by the local state manager
     * we're listening to.
     */
    public void stateModified(ItemState modified) {
        ItemState visibleState = modified;
        // JCR-2650: ignore external changes, they will be considered/merged on save().
        dispatcher.notifyStateModified(visibleState);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Notification handler gets called for both transient states that this state manager
     * has created, as well as states that were created by the local state manager
     * we're listening to.
     */
    public void stateDestroyed(ItemState destroyed) {
        ItemState visibleState = destroyed;
        if (destroyed.getContainer() != this) {
            // local state was destroyed
            ItemState transientState = transientStore.get(destroyed.getId());
            if (transientState != null) {
                transientState.setStatus(ItemState.STATUS_STALE_DESTROYED);
                visibleState = transientState;
            } else {
                // check attic
                transientState = atticStore.remove(destroyed.getId());
                if (transientState != null) {
                    transientState.onDisposed();
                }
            }
        }
        dispatcher.notifyStateDestroyed(visibleState);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Notification handler gets called for both transient states that this state manager
     * has created, as well as states that were created by the local state manager
     * we're listening to.
     */
    public void stateDiscarded(ItemState discarded) {
        ItemState visibleState = discarded;
        if (discarded.getContainer() != this) {
            // local state was discarded
            ItemState transientState = transientStore.get(discarded.getId());
            if (transientState != null) {
                transientState.setStatus(ItemState.STATUS_UNDEFINED);
                visibleState = transientState;
            }
        }
        dispatcher.notifyStateDiscarded(visibleState);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Pass notification to listeners if a transient state was modified
     * or if the local state is not overlayed.
     */
    public void nodeAdded(NodeState state, Name name, int index, NodeId id) {
        if (state.getContainer() == this
                || !transientStore.containsKey(state.getId())) {
            dispatcher.notifyNodeAdded(state, name, index, id);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Pass notification to listeners if a transient state was modified
     * or if the local state is not overlayed.
     */
    public void nodesReplaced(NodeState state) {
        if (state.getContainer() == this
                || !transientStore.containsKey(state.getId())) {
            dispatcher.notifyNodesReplaced(state);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Pass notification to listeners if a transient state was modified
     * or if the local state is not overlayed.
     */
    public void nodeModified(NodeState state) {
        if (state.getContainer() == this
                || !transientStore.containsKey(state.getId())) {
            dispatcher.notifyNodeModified(state);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Pass notification to listeners if a transient state was modified
     * or if the local state is not overlayed.
     */
    public void nodeRemoved(NodeState state, Name name, int index, NodeId id) {
        if (state.getContainer() == this
                || !transientStore.containsKey(state.getId())) {
            dispatcher.notifyNodeRemoved(state, name, index, id);
        }
    }

    //--------------------------------------------------------< inner classes >

    /**
     * ItemStateManager view of the states in the attic
     *
     * @see SessionItemStateManager#getAttic
     */
    private class AtticItemStateManager implements ItemStateManager {

        /**
         * {@inheritDoc}
         */
        public ItemState getItemState(ItemId id)
                throws NoSuchItemStateException, ItemStateException {

            ItemState state = atticStore.get(id);
            if (state != null) {
                return state;
            } else {
                throw new NoSuchItemStateException(id.toString());
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasItemState(ItemId id) {
            return atticStore.containsKey(id);
        }

        /**
         * {@inheritDoc}
         */
        public NodeReferences getNodeReferences(NodeId id)
                throws NoSuchItemStateException, ItemStateException {
            // n/a
            throw new ItemStateException("getNodeReferences() not implemented");
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNodeReferences(NodeId id) {
            // n/a
            return false;
        }
    }

    /**
     * Pushes the given transient state to the change log so it'll be
     * persisted when the change log is committed. The transient state
     * is replaced with the local state that has been pushed to the
     * change log.
     *
     * @param transientState transient state
     * @return the local state to be persisted
     * @throws RepositoryException if the transiet state can not be persisted
     */
    public NodeState makePersistent(NodeState transientState)
            throws RepositoryException {
        NodeState localState = stateMgr.getOrCreateLocalState(transientState);

        synchronized (localState) {
            // copy state from transient state:
            localState.copy(transientState, true);
            // make state persistent
            store(localState);
        }

        // disconnect the transient item state
        disconnectTransientItemState(transientState);

        return localState;
    }

}
