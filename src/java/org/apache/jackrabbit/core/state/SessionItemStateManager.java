/*
 * Copyright 2004 The Apache Software Foundation.
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

import org.apache.log4j.Logger;
import org.apache.jackrabbit.core.*;

import javax.jcr.RepositoryException;
import java.io.PrintStream;
import java.util.*;

/**
 * <code>SessionItemStateManager</code> ...
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.2 $, $Date: 2004/09/07 14:39:44 $
 */
public class SessionItemStateManager implements ItemStateProvider {

    private static Logger log = Logger.getLogger(SessionItemStateManager.class);

    private final PersistentItemStateManager persistentStateMgr;
    private final TransientItemStateManager transientStateMgr;

    private final HierarchyManager hierMgr;

    /**
     * Creates a new <code>SessionItemStateManager</code> instance.
     *
     * @param rootNodeUUID
     * @param persistentStateMgr
     * @param nsResolver
     */
    public SessionItemStateManager(String rootNodeUUID, PersistentItemStateManager persistentStateMgr, NamespaceResolver nsResolver) {
	this.persistentStateMgr = persistentStateMgr;
	// create transient item state manager
	transientStateMgr = new TransientItemStateManager();
	// create hierarchy manager that uses both transient and persistent state
	hierMgr = new HierarchyManagerImpl(rootNodeUUID, this, nsResolver);
    }

    private synchronized void collectDescendantItemStates(ItemId id, List descendents) {
	// XXX very inefficient implementation, especially if # of transient states
	// is relatively small compared to the total # of persistent states
	if (descendents.size() == transientStateMgr.getEntriesCount()) {
	    return;
	}
	try {
	    if (id.denotesNode()) {
		NodeId parentId = (NodeId) id;
		ItemId[] childIds = hierMgr.listChildren(parentId);
		for (int i = 0; i < childIds.length; i++) {
		    ItemId childId = childIds[i];
		    if (transientStateMgr.hasItemState(childId)) {
			ItemState state = transientStateMgr.getItemState(childId);
			if (!descendents.contains(state)) {
			    descendents.add(state);
			}
		    }
		    if (childId.denotesNode()) {
			// recurse
			collectDescendantItemStates(childId, descendents);
		    }
		}
		// also add transient child nodes that have been unlinked from
		// the specified parent node but are not orphaned yet (i.e.
		// they are still linked to at least one other parent node)
		if (transientStateMgr.hasItemState(parentId)) {
		    NodeState parentState = (NodeState) transientStateMgr.getItemState(parentId);
		    Iterator iter = parentState.getRemovedChildNodeEntries().iterator();
		    while (iter.hasNext()) {
			// removed child nodes
			NodeState.ChildNodeEntry cne = (NodeState.ChildNodeEntry) iter.next();
			NodeId removedChildId = new NodeId(cne.getUUID());
			if (transientStateMgr.hasItemState(removedChildId)) {
			    ItemState state = transientStateMgr.getItemState(removedChildId);
			    if (!descendents.contains(state)) {
				descendents.add(state);
			    }
			}
		    }
		}
	    }
	} catch (ItemStateException ise) {
	    log.warn("inconsistent hierarchy state", ise);
	} catch (RepositoryException re) {
	    log.warn("inconsistent hierarchy state", re);
	}
    }

    private synchronized void collectDescendantItemStatesInAttic(ItemId id, List descendents) {
	// XXX very inefficient implementation, especially if # of transient states
	// is relatively small compared to the total # of persistent states
	if (descendents.size() == transientStateMgr.getEntriesInAtticCount()) {
	    return;
	}
	try {
	    if (id.denotesNode()) {
		NodeId parentId = (NodeId) id;

		// traverse zombie children (i.e. children marked as removed)
		ItemId[] childIds = hierMgr.listZombieChildren(parentId);
		for (int i = 0; i < childIds.length; i++) {
		    ItemId childId = childIds[i];
		    // check attic
		    if (transientStateMgr.hasItemStateInAttic(childId)) {
			// found on attic, add to descendents list
			ItemState state = transientStateMgr.getItemStateInAttic(childId);
			if (!descendents.contains(state)) {
			    descendents.add(state);
			}
		    }
		    if (childId.denotesNode()) {
			// recurse
			collectDescendantItemStatesInAttic(childId, descendents);
		    }
		}

		// traverse existing children (because they might have zombie children)
		childIds = hierMgr.listChildren(parentId);
		for (int i = 0; i < childIds.length; i++) {
		    ItemId childId = childIds[i];
		    if (childId.denotesNode()) {
			// recurse
			collectDescendantItemStatesInAttic(childId, descendents);
		    }
		}
	    }
	} catch (ItemStateException ise) {
	    log.warn("inconsistent hierarchy state", ise);
	} catch (RepositoryException re) {
	    log.warn("inconsistent hierarchy state", re);
	}
    }

    /**
     * Dumps the state of this <code>TransientItemStateManager</code> instance
     * (used for diagnostic purposes).
     *
     * @param ps
     */
    public void dump(PrintStream ps) {
	ps.println("SessionItemStateManager (" + this + ")");
	ps.println();
	persistentStateMgr.dump(ps);
	ps.println();
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

    //----------------------------------------------------< ItemStateProvider >
    /**
     * @see ItemStateProvider#getItemState(ItemId)
     */
    public ItemState getItemState(ItemId id)
	    throws NoSuchItemStateException, ItemStateException {
	// first check if the specified item has been transiently removed
	if (transientStateMgr.hasItemStateInAttic(id)) {
	    /**
	     * check if there's new transient state for the specified item
	     * (e.g. if a property with name 'x' has been removed and a new
	     * property with same name has been created);
	     * this will throw a NoSuchItemStateException if there's no new
	     * transient state
	     */
	    return transientStateMgr.getItemState(id);
	}
	try {
	    // check if there's transient state for the specified item
	    return transientStateMgr.getItemState(id);
	} catch (NoSuchItemStateException nsise) {
	    // check if there's persistent state for the specified item
	    return persistentStateMgr.getItemState(id);
	}
    }

    /**
     * @see ItemStateProvider#hasItemState(ItemId)
     */
    public boolean hasItemState(ItemId id) {
	try {
	    getItemState(id);
	    return true;
	} catch (ItemStateException ise) {
	    return false;
	}
    }

    /**
     * @see ItemStateProvider#getItemStateInAttic(ItemId)
     */
    public ItemState getItemStateInAttic(ItemId id)
	    throws NoSuchItemStateException, ItemStateException {
	return transientStateMgr.getItemStateInAttic(id);
    }

    /**
     * @see ItemStateProvider#hasItemStateInAttic(ItemId)
     */
    public boolean hasItemStateInAttic(ItemId id) {
	try {
	    getItemStateInAttic(id);
	    return true;
	} catch (ItemStateException ise) {
	    return false;
	}
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
     */
    public Iterator getDescendantTransientItemStates(ItemId parentId) {
	// @todo need a more efficient way to find descendents in cache (e.g. using hierarchical index)
	if (!transientStateMgr.hasAnyItemStates()) {
	    return Collections.EMPTY_LIST.iterator();
	}
	// collection of descendant transient states:
	// the path serves as key and sort criteria
	TreeMap descendants = new TreeMap(new PathComparator());
	try {
	    Path[] parentPaths = hierMgr.getAllPaths(parentId);
	    /**
	     * walk through list of transient states and check if
	     * they are descendants of the specified parent
	     */
	    Iterator iter = transientStateMgr.getEntries();
	    while (iter.hasNext()) {
		ItemState state = (ItemState) iter.next();
		ItemId id = state.getId();
		Path[] paths = hierMgr.getAllPaths(id);
		boolean isDescendant = false;
		/**
		 * check if any of the paths to the transient state
		 * is a descendant of any of the specified parentId's paths
		 */
		for (int i = 0; i < paths.length; i++) {
		    Path p0 = paths[i];	// path to transient state
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
			Path[] pa = hierMgr.getAllPaths(new NodeId((String) iterUUIDs.next()));
			for (int k = 0; k < pa.length; k++) {
			    Path p0 = pa[k];	// path to removed parent
			    // walk through array of the specified parentId's paths
			    for (int j = 0; j < parentPaths.length; j++) {
				Path p1 = parentPaths[j]; // path to specified parentId
				if (p0.equals(p1) || p0.isDescendantOf(p1)) {
				    // this is a descendant, add it to the list and
				    // continue with next transient state

				    // FIXME need to create dummy path in order
				    // to avoid conflicts
				    Path.PathBuilder pb = new Path.PathBuilder(p0.getElements());
				    pb.addFirst(NamespaceRegistryImpl.NS_DEFAULT_URI, Integer.toString(new Random().nextInt()));
				    descendants.put(pb.getPath(), state);
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
	    log.warn("inconsistent hierarchy state", mpe);
	} catch (RepositoryException re) {
	    log.warn("inconsistent hierarchy state", re);
	}

	return descendants.values().iterator();
/*
	ArrayList descendents = new ArrayList();
	collectDescendantItemStates(parentId, descendents);
	return Collections.unmodifiableList(descendents).iterator();
*/
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
		    Path p0 = paths[i];	// path to transient state in attic
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
/*
	ArrayList descendents = new ArrayList();
	collectDescendantItemStatesInAttic(parentId, descendents);
	return Collections.unmodifiableList(descendents).iterator();
*/
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

    //------------------< methods for creating persistent ItemState instances >
    /**
     * Creates a <code>PersistentNodeState</code> instance representing new,
     * i.e. not yet existing state. Call <code>{@link PersistentNodeState#store()}</code>
     * on the returned object to make it persistent.
     *
     * @param uuid
     * @param nodeTypeName
     * @param parentUUID
     * @return
     * @throws ItemStateException
     */
    public PersistentNodeState createPersistentNodeState(String uuid, QName nodeTypeName, String parentUUID)
	    throws ItemStateException {
	return persistentStateMgr.createNodeState(uuid, nodeTypeName, parentUUID);
    }

    /**
     * Creates a <code>PersistentPropertyState</code> instance representing new,
     * i.e. not yet existing state. Call <code>{@link PersistentPropertyState#store()}</code>
     * on the returned object to make it persistent.
     *
     * @param parentUUID
     * @param propName
     * @return
     * @throws ItemStateException
     */
    public PersistentPropertyState createPersistentPropertyState(String parentUUID, QName propName)
	    throws ItemStateException {
	return persistentStateMgr.createPropertyState(parentUUID, propName);
    }

    //--------------------------------------------------------< inner classes >
    /**
     * Comparator used to sort canonical <code>Path</code> objects
     * hierarchically (in depth-first tree traversal order).
     */
    static class PathComparator implements Comparator {
	/**
	 *
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
