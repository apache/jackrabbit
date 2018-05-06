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
package org.apache.jackrabbit.core.version;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.MergeException;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemValidator;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JCR Version Manager implementation is split in several classes in order to
 * group related methods together.
 * <p>
 * This class provides methods for the merge operations.
 */
abstract public class VersionManagerImplMerge extends VersionManagerImplRestore {

    /**
     * default logger
     */
    private static final Logger log = LoggerFactory.getLogger(VersionManagerImplMerge.class);

    /**
     * Creates a new version manager for the given session
     *
     * @param context component context of the current session
     * @param stateMgr the underlying state manager
     * @param hierMgr local hierarchy manager
     */
    protected VersionManagerImplMerge(
            SessionContext context, UpdatableItemStateManager stateMgr,
            HierarchyManager hierMgr) {
        super(context, stateMgr, hierMgr);
    }

    /**
     * Merges/Updates this node with its corresponding ones
     *
     * @param state state to merge or update
     * @param srcRoot src workspace root node
     * @param failedIds list of failed ids
     * @param bestEffort best effort flag
     * @param shallow is shallow flag
     * @throws RepositoryException if an error occurs
     * @throws ItemStateException if an error occurs
     */
    protected void merge(NodeStateEx state, NodeStateEx srcRoot,
                       List<ItemId> failedIds,
                       boolean bestEffort, boolean shallow)
            throws RepositoryException, ItemStateException {

        if (shallow) {
            // If <code>isShallow</code> is <code>true</code> and this node is not
            // versionable, then this method returns and no changes are made.
            if (!state.getEffectiveNodeType().includesNodeType(
                    NameConstants.MIX_SIMPLE_VERSIONABLE)) {
                return;
            }
        }
        NodeStateEx srcNode = getCorrespondingNode(state, srcRoot);
        if (srcNode == null) {
            // If this node (the one on which merge is called) does not have a corresponding
            // node in the indicated workspace, then the merge method returns quietly and no
            // changes are made.
            return;
        }
        WriteOperation ops = startWriteOperation();
        try {
            internalMerge(state, srcRoot, failedIds, bestEffort, shallow);
            state.store();
            ops.save();
        } finally {
            ops.close();
        }
    }

    /**
     * Merges/Updates this node with its corresponding ones
     *
     * @param state state to merge or update
     * @param srcRoot src workspace root node
     * @param failedIds list of failed ids
     * @param bestEffort best effort flag
     * @param shallow is shallow flag
     * @throws RepositoryException if an error occurs
     * @throws ItemStateException if an error occurs
     */
    private void internalMerge(NodeStateEx state, NodeStateEx srcRoot,
                       List<ItemId> failedIds,
                       boolean bestEffort, boolean shallow)
            throws RepositoryException, ItemStateException {

        NodeStateEx srcNode = doMergeTest(state, srcRoot, failedIds, bestEffort);
        if (srcNode == null) {
            if (!shallow) {
                // leave, iterate over children, but ignore non-versionable child
                // nodes (see JCR-1046)
                for (NodeStateEx n: state.getChildNodes()) {
                    if (n.getEffectiveNodeType().includesNodeType(NameConstants.MIX_VERSIONABLE)) {
                        internalMerge(n, srcRoot, failedIds, bestEffort, shallow);
                    }
                }
            }
            return;
        }

        // check lock and hold status if node exists
        checkModify(state, ItemValidator.CHECK_LOCK | ItemValidator.CHECK_HOLD, Permission.NONE);

        // remove all properties that are not present in srcNode
        for (PropertyState prop: state.getProperties()) {
            if (!srcNode.hasProperty(prop.getName())) {
                state.removeProperty(prop.getName());
            }
        }

        // update all properties from the src node
        for (PropertyState prop: srcNode.getProperties()) {
            Name propName = prop.getName();
            // ignore system types
            if (propName.equals(NameConstants.JCR_PRIMARYTYPE)
                    || propName.equals(NameConstants.JCR_MIXINTYPES)
                    || propName.equals(NameConstants.JCR_UUID)) {
                continue;
            }
            state.copyFrom(prop);
        }

        // update the mixin types
        state.setMixins(srcNode.getState().getMixinTypeNames());

        // remove the child nodes in N but not in N'
        LinkedList<ChildNodeEntry> toDelete = new LinkedList<ChildNodeEntry>();
        for (ChildNodeEntry entry: state.getState().getChildNodeEntries()) {
            if (!srcNode.getState().hasChildNodeEntry(entry.getName(), entry.getIndex())) {
                toDelete.add(entry);
            }
        }
        for (ChildNodeEntry entry: toDelete) {
            state.removeNode(entry.getName(), entry.getIndex());
        }
        state.store();

        // add source ones
        for (ChildNodeEntry entry: srcNode.getState().getChildNodeEntries()) {
            NodeStateEx child = state.getNode(entry.getName(), entry.getIndex());
            if (child == null) {
                // if destination workspace already has such an node, remove it
                if (state.hasNode(entry.getId())) {
                    child = state.getNode(entry.getId());
                    NodeStateEx parent = child.getParent();
                    parent.removeNode(child);
                    parent.store();
                }
                // create new child
                NodeStateEx srcChild = srcNode.getNode(entry.getId());
                child = state.addNode(entry.getName(), srcChild.getState().getNodeTypeName(), srcChild.getNodeId());
                child.setMixins(srcChild.getState().getMixinTypeNames());
                // copy src child
                state.store();
                internalMerge(child, srcRoot, null, bestEffort, false);
            } else if (!shallow) {
                // recursively merge
                internalMerge(child, srcRoot, failedIds, bestEffort, false);

            }
        }
    }

    /**
     * Returns the corresponding node in the workspace of the given session.
     * <p>
     * Given a node N1 in workspace W1, its corresponding node N2 in workspace
     * W2 is defined as follows:
     * <ul>
     * <li>If N1 is the root node of W1 then N2 is the root node of W2.
     * <li>If N1 is referenceable (has a UUID) then N2 is the node in W2 with
     * the same UUID.
     * <li>If N1 is not referenceable (does not have a UUID) then there is some
     * node M1 which is either the nearest ancestor of N1 that is
     * referenceable, or is the root node of W1. If the corresponding node
     * of M1 is M2 in W2, then N2 is the node with the same relative path
     * from M2 as N1 has from M1.
     * </ul>
     *
     * @param state N1
     * @param srcRoot the root node state of W2
     * @return the corresponding node or <code>null</code> if no corresponding
     *         node exists.
     * @throws RepositoryException If another error occurs.
     */
    private NodeStateEx getCorrespondingNode(NodeStateEx state, NodeStateEx srcRoot)
            throws RepositoryException {

        // search nearest ancestor that is referenceable
        NodeStateEx m1 = state;
        LinkedList<ChildNodeEntry> elements = new LinkedList<ChildNodeEntry>();
        while (m1.getParentId() != null &&
                !m1.getEffectiveNodeType().includesNodeType(NameConstants.MIX_REFERENCEABLE)) {
            NodeStateEx parent = m1.getParent();
            elements.addFirst(parent.getState().getChildNodeEntry(m1.getNodeId()));
            m1 = parent;
        }

        // check if corresponding ancestor exists
        if (srcRoot.hasNode(m1.getNodeId())) {
            NodeStateEx m2 = srcRoot.getNode(m1.getNodeId());
            Iterator<ChildNodeEntry> iter = elements.iterator();
            while (iter.hasNext() && m2 != null) {
                ChildNodeEntry e = iter.next();
                m2 = m2.getNode(e.getName(), e.getIndex());
            }
            return m2;
        } else {
            return null;
        }
    }

    /**
     * Performs the merge test. If the result is 'update', then the corresponding
     * source node is returned. if the result is 'leave' or 'besteffort-fail'
     * then <code>null</code> is returned. If the result of the merge test is
     * 'fail' with bestEffort set to <code>false</code> a MergeException is
     * thrown.
     * <p>
     * jsr170 - 8.2.10 Merge:
     * [...]
     * <ul>
     * <li>If N is currently checked-in then:</li>
     * <ul>
     * <li>If V' is a successor (to any degree) of V, then the merge result
     *     for N is update.
     * </li>
     * <li>If V' is a predecessor (to any degree) of V or if V and
     *     V' are identical (i.e., are actually the same version),
     *     then the merge result for N is leave.
     * </li>
     * <li>If V is neither a successor of, predecessor of, nor
     *     identical with V', then the merge result for N is failed.
     *     This is the case where N and N' represent divergent
     *     branches of the version graph, thus determining the
     *     result of a merge is non-trivial.
     * </li>
     * </ul>
     * <li>If N is currently checked-out then:</li>
     * <ul>
     * <li>If V' is a predecessor (to any degree) of V or if V and
     *     V' are identical (i.e., are actually the same version),
     *     then the merge result for N is leave.
     * </li>
     * <li>If any other relationship holds between V and V',
     *     then the merge result for N is fail.
     * </li>
     * </ul>
     * </ul>
     *
     * @param state state to test
     * @param srcRoot root node state of the source workspace
     * @param failedIds the list to store the failed node ids.
     * @param bestEffort the best effort flag
     * @return the corresponding source node or <code>null</code>
     * @throws RepositoryException if an error occurs.
     * @throws AccessDeniedException if access is denied
     */
    private NodeStateEx doMergeTest(NodeStateEx state, NodeStateEx srcRoot, List<ItemId> failedIds, boolean bestEffort)
            throws RepositoryException, AccessDeniedException {

        // If N does not have a corresponding node then the merge result for N is leave.
        NodeStateEx srcNode = getCorrespondingNode(state, srcRoot);
        if (srcNode == null) {
            return null;
        }

        // if not versionable, update
        if (!state.getEffectiveNodeType().includesNodeType(NameConstants.MIX_VERSIONABLE) || failedIds == null) {
            return srcNode;
        }
        // if source node is not versionable, leave
        if (!srcNode.getEffectiveNodeType().includesNodeType(NameConstants.MIX_VERSIONABLE)) {
            return null;
        }
        // test versions. the following code could be simplified but is
        // intentionally expanded for follow the spec.
        InternalVersion v = getBaseVersion(state);
        InternalVersion vp = getBaseVersion(srcNode);
        if (!isCheckedOut(state)) {
            // If N is currently checked-in then:
            if (vp.isMoreRecent(v)) {
                // - If V' is an eventual successor of V, then the merge result for N is update.
                return srcNode;
            } else if (v.equals(vp) || v.isMoreRecent(vp)) {
                // - If V' is an eventual predecessor of V or if V and V' are identical (i.e., are
                // actually the same version), then the merge result for N is leave.
                return null;
            }
            // - If V is neither an eventual successor of, eventual predecessor of, nor
            // identical with V', then the merge result for N is failed. This is the case
            // where N and N' represent divergent branches of the version graph.

            // failed is covered below
        } else {
            // If N is currently checked-out then:
            if (v.equals(vp) || v.isMoreRecent(vp)) {
                // - If V' is an eventual predecessor of V or if V and V' are identical (i.e., are
                //   actually the same version), then the merge result for
                //   N is leave.
                return null;
            }
            // - If any other relationship holds between V and V', then the merge result
            //   for N is fail.

            // failed is covered below
        }


        if (vp.isMoreRecent(v) && !isCheckedOut(state)) {
            // I f V' is a successor (to any degree) of V, then the merge result for
            // N is update. This case can be thought of as the case where N' is
            // "newer" than N and therefore N should be updated to reflect N'.
            return srcNode;
        } else if (v.equals(vp) || v.isMoreRecent(vp)) {
            // If V' is a predecessor (to any degree) of V or if V and V' are
            // identical (i.e., are actually the same version), then the merge
            // result for N is leave. This case can be thought of as the case where
            // N' is "older" or the "same age" as N and therefore N should be left alone.
            return null;
        } else {
            // If V is neither a successor of, predecessor of, nor identical
            // with V', then the merge result for N is failed. This is the case
            // where N and N' represent divergent branches of the version graph,
            // thus determining the result of a merge is non-trivial.
            if (bestEffort) {
                // add 'offending' version to jcr:mergeFailed property
                Set<NodeId> set = getMergeFailed(state);
                set.add(vp.getId());
                setMergeFailed(state, set);
                failedIds.add(state.getNodeId());
                state.store();
                return null;
            } else {
                String msg =
                    "Unable to merge nodes. Violating versions. " + safeGetJCRPath(state);
                log.debug(msg);
                throw new MergeException(msg);
            }
        }
    }

    /**
     * Perform {@link Node#cancelMerge(Version)} or {@link Node#doneMerge(Version)}
     * depending on the value of <code>cancel</code>.
     * @param state state to finish
     * @param version version
     * @param cancel flag inidicates if this is a cancel operation
     * @throws RepositoryException if an error occurs
     */
    protected void finishMerge(NodeStateEx state, Version version, boolean cancel)
            throws RepositoryException {
        // check versionable
        if (!checkVersionable(state)) {
            throw new UnsupportedRepositoryOperationException("Node not full versionable: " + safeGetJCRPath(state));
        }

        // check if version is in mergeFailed list
        Set<NodeId> failed = getMergeFailed(state);
        NodeId versionId = ((VersionImpl) version).getNodeId();
        if (!failed.remove(versionId)) {
            String msg =
                "Unable to finish merge. Specified version is not in"
                + " jcr:mergeFailed property: " + safeGetJCRPath(state);
            log.error(msg);
            throw new VersionException(msg);
        }

        WriteOperation ops = startWriteOperation();
        try {
            // remove version from mergeFailed list
            setMergeFailed(state, failed);

            if (!cancel) {
                // add version to jcr:predecessors list
                InternalValue[] vals = state.getPropertyValues(NameConstants.JCR_PREDECESSORS);
                InternalValue[] v = new InternalValue[vals.length + 1];
                for (int i = 0; i < vals.length; i++) {
                    v[i] = InternalValue.create(vals[i].getNodeId());
                }
                v[vals.length] = InternalValue.create(versionId);
                state.setPropertyValues(NameConstants.JCR_PREDECESSORS, PropertyType.REFERENCE, v, true);
            }
            state.store();
            ops.save();
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            ops.close();
        }
    }

    /**
     * Returns a set of nodeid from the jcr:mergeFailed property of the given state
     * @param state the state
     * @return set of node ids
     */
    private Set<NodeId> getMergeFailed(NodeStateEx state) {
        Set<NodeId> set = new HashSet<NodeId>();
        if (state.hasProperty(NameConstants.JCR_MERGEFAILED)) {
            InternalValue[] vals = state.getPropertyValues(NameConstants.JCR_MERGEFAILED);
            for (InternalValue val : vals) {
                set.add(val.getNodeId());
            }
        }
        return set;
    }

    /**
     * Updates the merge failed property of the given state/
     * @param state the state to update
     * @param set the set of ids
     * @throws RepositoryException if an error occurs
     */
    private void setMergeFailed(NodeStateEx state, Set<NodeId> set)
            throws RepositoryException {
        if (set.isEmpty()) {
            state.removeProperty(NameConstants.JCR_MERGEFAILED);
        } else {
            InternalValue[] vals = new InternalValue[set.size()];
            Iterator<NodeId> iter = set.iterator();
            int i = 0;
            while (iter.hasNext()) {
                NodeId id = iter.next();
                vals[i++] = InternalValue.create(id);
            }
            state.setPropertyValues(NameConstants.JCR_MERGEFAILED, PropertyType.REFERENCE, vals, true);
        }
    }

    /**
     * Merge the given activity to this workspace
     *
     * @param activity internal activity
     * @param failedIds list of failed ids
     * @throws RepositoryException if an error occurs
     */
    protected void merge(InternalActivity activity, List<ItemId> failedIds)
            throws RepositoryException {

        VersionSet changeSet = activity.getChangeSet();
        WriteOperation ops = startWriteOperation();
        try {
            Iterator<NodeId> iter = changeSet.versions().keySet().iterator();
            while (iter.hasNext()) {
                InternalVersion v = changeSet.versions().remove(iter.next());
                NodeStateEx state = getNodeStateEx(v.getFrozenNode().getFrozenId());
                if (state != null) {
                    InternalVersion base = getBaseVersion(state);
                    // if base version is newer than version, add to failed list
                    // but merge it anyways
                    if (base.isMoreRecent(v)) {
                        failedIds.add(state.getNodeId());
                        // add it to the jcr:mergeFailed property
                        Set<NodeId> set = getMergeFailed(state);
                        set.add(base.getId());
                        setMergeFailed(state, set);
                        state.store();
                    } else {
                        for (InternalVersion restored: internalRestore(state, v, changeSet, true)) {
                            changeSet.versions().remove(restored.getVersionHistory().getId());
                        }
                    }
                }

                // reset iterator
                iter = changeSet.versions().keySet().iterator();
            }
            ops.save();
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            ops.close();
        }
    }

}