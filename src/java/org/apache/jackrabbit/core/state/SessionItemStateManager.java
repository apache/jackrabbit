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

import org.apache.jackrabbit.core.CachingHierarchyManager;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.MalformedPathException;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.Path;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.ZombieHierarchyManager;
import org.apache.log4j.Logger;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * <code>SessionItemStateManager</code> ...
 */
public class SessionItemStateManager implements UpdatableItemStateManager {

    private static Logger log = Logger.getLogger(SessionItemStateManager.class);

    /**
     * Root node id
     */
    private final NodeId rootNodeId;

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
     * @param rootNodeUUID
     * @param persistentStateMgr
     * @param nsResolver
     */
    public SessionItemStateManager(String rootNodeUUID,
                                   UpdatableItemStateManager persistentStateMgr,
                                   NamespaceResolver nsResolver) {

        rootNodeId = new NodeId(rootNodeUUID);
        this.persistentStateMgr = persistentStateMgr;
        // create transient item state manager
        transientStateMgr = new TransientItemStateManager();
        // create hierarchy manager that uses both transient and persistent state
        hierMgr = new CachingHierarchyManager(rootNodeUUID, this, nsResolver);
    }

    /**
     * Dumps the state of this <code>SessionItemStateManager</code> instance
     * (used for diagnostic purposes).
     *
     * @param ps
     */
    public void dump(PrintStream ps) {
        ps.println("SessionItemStateManager (" + this + ")");
        ps.println();
        // FIXME hack!
        if (persistentStateMgr instanceof ItemStateCache) {
            ((ItemStateCache) persistentStateMgr).dump(ps);
            ps.println();
        }
        transientStateMgr.dump(ps);
        ps.println();
    }

    /**
     * Returns the hierarchy manager
     *
     * @return the hierarchy manager
     */
    public HierarchyManager getHierarchyMgr() {
        return hierMgr;
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
    public NodeState createNew(String uuid, QName nodeTypeName,
                               String parentUUID)
            throws IllegalStateException {
        return persistentStateMgr.createNew(uuid, nodeTypeName, parentUUID);
    }

    /**
     * Customized variant of {@link #createNew(String, QName, String)} that
     * connects the newly created persistent state with the transient state.
     */
    public NodeState createNew(NodeState transientState)
            throws IllegalStateException {

        NodeState persistentState = createNew(transientState.getUUID(),
                transientState.getNodeTypeName(),
                transientState.getParentUUID());
        transientState.connect(persistentState);
        return persistentState;
    }

    /**
     * {@inheritDoc}
     */
    public PropertyState createNew(QName propName, String parentUUID)
            throws IllegalStateException {
        return persistentStateMgr.createNew(propName, parentUUID);
    }

    /**
     * Customized variant of {@link #createNew(String, QName, String)} that
     * connects the newly created persistent state with the transient state.
     */
    public PropertyState createNew(PropertyState transientState)
            throws IllegalStateException {

        PropertyState persistentState = createNew(transientState.getName(),
                transientState.getParentUUID());
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
    public void store(NodeReferences refs) throws IllegalStateException {
        persistentStateMgr.store(refs);
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
    public void update() throws ItemStateException, IllegalStateException {
        persistentStateMgr.update();
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
     * @return
     */
    public boolean hasAnyTransientItemStates() {
        return !transientStateMgr.isEmpty();
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
        // collection of descendant transient states:
        // the path serves as key and sort criteria
        TreeMap descendants = new TreeMap(new PathComparator());

        // use shortcut if root was specified as parent
        // (in which case all non-root states are descendents)
        if (parentId.equals(rootNodeId)) {
            Iterator iter = transientStateMgr.getEntries();
            while (iter.hasNext()) {
                ItemState state = (ItemState) iter.next();
                ItemId id = state.getId();
                if (id.equals(rootNodeId)) {
                    // skip root
                    continue;
                }
                try {
                    Path p = hierMgr.getPath(id);
                    descendants.put(p, state);
                } catch (ItemNotFoundException infe) {
                    String msg = id + ": the item has been removed externally.";
                    log.debug(msg);
                    throw new InvalidItemStateException(msg);
                } catch (RepositoryException re) {
                    // unable to build path, assume that it (or any of
                    // its ancestors) has been removed externally
                    String msg = id
                            + ": the item seems to have been removed externally.";
                    log.debug(msg);
                    throw new InvalidItemStateException(msg);
                }
            }
            return descendants.values().iterator();
        }

        Path parentPath;
        try {
            parentPath = hierMgr.getPath(parentId);
        } catch (ItemNotFoundException infe) {
            String msg = parentId + ": the item has been removed externally.";
            log.debug(msg);
            throw new InvalidItemStateException(msg);
        }

        /**
         * walk through list of transient states and check if
         * they are descendants of the specified parent
         */
        try {
            Iterator iter = transientStateMgr.getEntries();
            while (iter.hasNext()) {
                ItemState state = (ItemState) iter.next();
                ItemId id = state.getId();
                Path path;
                try {
                    path = hierMgr.getPath(id);
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
                    // unable to build path, assume that it (or any of
                    // its ancestors) has been removed externally
                    String msg = id
                            + ": the item seems to have been removed externally.";
                    log.debug(msg);
                    throw new InvalidItemStateException(msg);
                }

                if (path.isDescendantOf(parentPath)) {
                    // this is a descendant, add it to the list
                    descendants.put(path, state);
                }
            }
        } catch (MalformedPathException mpe) {
            String msg = "inconsistent hierarchy state";
            log.warn(msg, mpe);
            throw new RepositoryException(msg, mpe);
        }

        return descendants.values().iterator();
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
        // collection of descendant transient states in attic:
        // the path serves as key and sort criteria

        // we have to use a special attic-aware hierarchy manager
        ZombieHierarchyManager zombieHierMgr =
                new ZombieHierarchyManager(hierMgr.getRootNodeId().getUUID(),
                        this,
                        transientStateMgr.getAttic(),
                        hierMgr.getNamespaceResolver());

        TreeMap descendants = new TreeMap(new PathComparator());
        try {
            Path parentPath = zombieHierMgr.getPath(parentId);
            /**
             * walk through list of transient states in attic and check if
             * they are descendants of the specified parent
             */
            Iterator iter = transientStateMgr.getEntriesInAttic();
            while (iter.hasNext()) {
                ItemState state = (ItemState) iter.next();
                ItemId id = state.getId();
                Path path = zombieHierMgr.getPath(id);
                if (path.isDescendantOf(parentPath)) {
                    // this is a descendant, add it to the list
                    descendants.put(path, state);
                }
                // continue with next transient state
            }
        } catch (MalformedPathException mpe) {
            log.warn("inconsistent hierarchy state", mpe);
        } catch (RepositoryException re) {
            log.warn("inconsistent hierarchy state", re);
        }

        return descendants.values().iterator();
    }

    //------< methods for creating & discarding transient ItemState instances >
    /**
     * @param uuid
     * @param nodeTypeName
     * @param parentUUID
     * @param initialStatus
     * @return
     * @throws ItemStateException
     */
    public NodeState createTransientNodeState(String uuid, QName nodeTypeName, String parentUUID, int initialStatus)
            throws ItemStateException {
        return transientStateMgr.createNodeState(uuid, nodeTypeName, parentUUID, initialStatus);
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
     * @param parentUUID
     * @param propName
     * @param initialStatus
     * @return
     * @throws ItemStateException
     */
    public PropertyState createTransientPropertyState(String parentUUID, QName propName, int initialStatus)
            throws ItemStateException {
        return transientStateMgr.createPropertyState(parentUUID, propName, initialStatus);
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

    //--------------------------------------------------------< inner classes >
    /**
     * Comparator used to sort canonical <code>Path</code> objects
     * hierarchically (in depth-first tree traversal order).
     */
    static class PathComparator implements Comparator {
        /**
         * @param o1
         * @param o2
         * @return
         */
        public int compare(Object o1, Object o2) {
            Path p1 = (Path) o1;
            Path p2 = (Path) o2;
            if (p1.equals(p2)) {
                return 0;
            }
            try {
                if (p1.isAncestorOf(p2)) {
                    return -1;
                } else if (p1.isDescendantOf(p2)) {
                    return 1;
                }
            } catch (MalformedPathException mpe) {
                log.warn("unable to compare non-canonical (i.e. relative) paths", mpe);
            }
            // the 2 paths are not on the same graph;
            // do string comparison of individual path elements
            Path.PathElement[] pea1 = p1.getElements();
            Path.PathElement[] pea2 = p2.getElements();
            for (int i = 0; i < pea1.length; i++) {
                if (i >= pea2.length) {
                    return 1;
                }
                String s1 = pea1[i].toString();
                String s2 = pea2[i].toString();
                int result = s1.compareTo(s2);
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        }
    }
}
