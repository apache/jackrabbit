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
import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.HierarchyManagerImpl;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.MalformedPathException;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.Path;
import org.apache.jackrabbit.core.QName;
import org.apache.log4j.Logger;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;
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
    private HierarchyManager hierMgr;

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
        hierMgr = new HierarchyManagerImpl(rootNodeUUID, this, nsResolver,
                transientStateMgr.getAttic());
    }

    /**
     * En-/Disable chaching of path values.
     * <p/>
     * Please keep in mind that the cache of <code>Path</code>s is not automatically
     * updated when the underlying hierarchy is changing. Therefore it should only be
     * turned on with caution and in special situations (usually only locally
     * within a narrow scope) where the underlying hierarchy is not expected to
     * change.
     *
     * @param enable
     */
    public void enablePathCaching(boolean enable) {
        if (enable) {
            if (!(hierMgr instanceof CachingHierarchyManager)) {
                hierMgr = new CachingHierarchyManager(hierMgr);
            }
        } else {
            if (hierMgr instanceof CachingHierarchyManager) {
                CachingHierarchyManager chm = (CachingHierarchyManager) hierMgr;
                chm.clearCache();
                hierMgr = chm.unwrap();
            }
        }
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
     * @see ItemStateManager#getItemState(ItemId)
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
     * @see ItemStateManager#hasItemState(ItemId)
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
     * @see ItemStateManager#getNodeReferences
     */
    public NodeReferences getNodeReferences(NodeReferencesId id)
            throws NoSuchItemStateException, ItemStateException {

        return persistentStateMgr.getNodeReferences(id);
    }

    /**
     * @see UpdatableItemStateManager#edit
     */
    public void edit() throws ItemStateException {
        persistentStateMgr.edit();
    }

    /**
     * @see UpdatableItemStateManager#createNew
     */
    public NodeState createNew(String uuid, QName nodeTypeName, String parentUUID) {
        return persistentStateMgr.createNew(uuid, nodeTypeName, parentUUID);
    }

    /**
     * @see UpdatableItemStateManager#createNew
     */
    public PropertyState createNew(QName propName, String parentUUID) {
        return persistentStateMgr.createNew(propName, parentUUID);
    }

    /**
     * @see UpdatableItemStateManager#store
     */
    public void store(ItemState state) {
        persistentStateMgr.store(state);
    }

    /**
     * @see UpdatableItemStateManager#store
     */
    public void store(NodeReferences refs) {
        persistentStateMgr.store(refs);
    }

    /**
     * @see UpdatableItemStateManager#destroy
     */
    public void destroy(ItemState state) {
        persistentStateMgr.destroy(state);
    }

    /**
     * @see UpdatableItemStateManager#cancel
     */
    public void cancel() {
        persistentStateMgr.cancel();
    }

    /**
     * @see UpdatableItemStateManager#update
     */
    public void update() throws ItemStateException {
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
    public Iterator getDescendantTransientItemStates(ItemId parentId)
            throws InvalidItemStateException, RepositoryException {
        // @todo need a more efficient way to find descendents in cache (e.g. using hierarchical index)
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
                }
            }
            return descendants.values().iterator();
        }

        Path[] parentPaths;
        try {
            parentPaths = hierMgr.getAllPaths(parentId);
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
                Path[] paths;
                try {
                    paths = hierMgr.getAllPaths(id);
                } catch (ItemNotFoundException infe) {
                    /**
                     * one of the parents of the specified item has been
                     * removed externally; as we don't know its path,
                     * we can't determine if it is a descendant;
                     * ItemNotFoundException should only be thrown if
                     * a descendant is affected;
                     * => log warning and ignore for now
                     * todo FIXME
                     */
                    log.warn(id + ": inconsistent hierarchy state", infe);
                    continue;
                }
                boolean isDescendant = false;
                /**
                 * check if any of the paths to the transient state
                 * is a descendant of any of the specified parentId's paths
                 */
                for (int i = 0; i < paths.length; i++) {
                    Path p0 = paths[i]; // path to transient state
                    // walk through array of the specified parentId's paths
                    for (int j = 0; j < parentPaths.length; j++) {
                        Path p1 = parentPaths[j]; // path to specified parentId
                        if (p0.isDescendantOf(p1)) {
                            // this is a descendant, add it to the list and
                            // continue with next transient state
                            descendants.put(p0, state);
                            isDescendant = true;
                            break;
                        }
                    }
                    if (isDescendant) {
                        break;
                    }
                }
                if (!isDescendant && id.denotesNode()) {
                    /**
                     * finally check if transient state has been unlinked
                     * from a parent node (but is not orphaned yet, i.e. is
                     * still linked to at least one other parent node);
                     * if that's the case, check if that parent is a
                     * descendant of/identical with any of the specified
                     * parentId's paths.
                     */
                    NodeState nodeState = (NodeState) state;
                    Iterator iterUUIDs = nodeState.getRemovedParentUUIDs().iterator();
                    while (iterUUIDs.hasNext()) {
                        /**
                         * check if any of the paths to the removed parent
                         * is a descendant of/identical with any of the
                         * specified parentId's paths.
                         */
                        String uuid = (String) iterUUIDs.next();
                        Path[] pa;
                        try {
                            pa = hierMgr.getAllPaths(new NodeId(uuid));
                        } catch (ItemNotFoundException infe) {
                            /**
                             * one of the parents of the specified item has been
                             * removed externally; as we don't know its path,
                             * we can't determine if it is a descendant;
                             * ItemNotFoundException should only be thrown if
                             * a descendant is affected;
                             * => log warning and ignore for now
                             * todo FIXME
                             */
                            log.warn(id + ": inconsistent hierarchy state", infe);
                            continue;
                        }

                        for (int k = 0; k < pa.length; k++) {
                            Path p0 = pa[k];   // path to removed parent
                            // walk through array of the specified parentId's paths
                            for (int j = 0; j < parentPaths.length; j++) {
                                Path p1 = parentPaths[j]; // path to specified parentId
                                if (p0.equals(p1) || p0.isDescendantOf(p1)) {
                                    // this is a descendant, add it to the list and
                                    // continue with next transient state

                                    /**
                                     * FIXME need to create dummy path by
                                     * appending a random integer in order to
                                     * avoid potential conflicts
                                     */
                                    Path dummy = Path.create(p0,
                                            Path.create(new QName(Constants.NS_DEFAULT_URI, Integer.toString(new Random().nextInt())), 0),
                                            true);
                                    descendants.put(dummy, state);
                                    isDescendant = true;
                                    break;
                                }
                            }
                            if (isDescendant) {
                                break;
                            }
                        }
                        if (isDescendant) {
                            break;
                        }
                    }
                }
                // continue with next transient state
            }
        } catch (MalformedPathException mpe) {
            String msg = "inconsistent hierarchy state";
            log.warn(msg, mpe);
            throw new RepositoryException(msg, mpe);
        }

        return descendants.values().iterator();
    }

    /**
     * Same as <code>{@link #getDescendantTransientItemStates(ItemId)}</code>
     * except that item state instances in the attic are returned.
     *
     * @param parentId the id of the common parent of the transient item state
     *                 instances to be returned.
     * @return an iterator over descendant transient item state instances in the attic
     */
    public Iterator getDescendantTransientItemStatesInAttic(ItemId parentId) {
        // @todo need a more efficient way to find descendents in attic (e.g. using hierarchical index)
        if (!transientStateMgr.hasAnyItemStatesInAttic()) {
            return Collections.EMPTY_LIST.iterator();
        }
        // collection of descendant transient states in attic:
        // the path serves as key and sort criteria
        TreeMap descendants = new TreeMap(new PathComparator());
        try {
            Path[] parentPaths = hierMgr.getAllPaths(parentId, true);
            /**
             * walk through list of transient states in attic and check if
             * they are descendants of the specified parent
             */
            Iterator iter = transientStateMgr.getEntriesInAttic();
            while (iter.hasNext()) {
                ItemState state = (ItemState) iter.next();
                ItemId id = state.getId();
                Path[] paths = hierMgr.getAllPaths(id, true);
                boolean isDescendant = false;
                /**
                 * check if any of the paths to the transient state
                 * is a descendant of any of the specified parentId's paths
                 */
                for (int i = 0; i < paths.length; i++) {
                    Path p0 = paths[i]; // path to transient state in attic
                    // walk through array of the specified parentId's paths
                    for (int j = 0; j < parentPaths.length; j++) {
                        Path p1 = parentPaths[j]; // path to specified parentId
                        if (p0.isDescendantOf(p1)) {
                            // this is a descendant, add it to the list and
                            // continue with next transient state
                            descendants.put(p0, state);
                            isDescendant = true;
                            break;
                        }
                    }
                    if (isDescendant) {
                        break;
                    }
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
        return transientStateMgr.createNodeState(overlayedState, initialStatus);
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
        return transientStateMgr.createPropertyState(overlayedState, initialStatus);
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
