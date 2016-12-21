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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.retention.RetentionRegistry;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.jackrabbit.core.util.ReferenceChangeTracker;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.version.VersionHistoryInfo;
import org.apache.jackrabbit.core.version.InternalVersionManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>BatchedItemOperations</code> is an <i>internal</i> helper class that
 * provides both high- and low-level operations directly on the
 * <code>ItemState</code> level.
 */
public class BatchedItemOperations extends ItemValidator {

    private static Logger log = LoggerFactory.getLogger(BatchedItemOperations.class);

    // flags used by the copy(...) methods
    protected static final int COPY = 0;
    protected static final int CLONE = 1;
    protected static final int CLONE_REMOVE_EXISTING = 2;

    /**
     * wrapped item state manager
     */
    protected final UpdatableItemStateManager stateMgr;
    /**
     * current session used for checking access rights
     */
    protected final SessionImpl session;

    private final HierarchyManager hierMgr;

    /**
     * Creates a new <code>BatchedItemOperations</code> instance.
     *
     * @param stateMgr   item state manager
     * @param sessionContext the session context
     * @throws RepositoryException
     */
    public BatchedItemOperations(
            UpdatableItemStateManager stateMgr, SessionContext sessionContext)
            throws RepositoryException {
        super(sessionContext);
        this.stateMgr = stateMgr;
        this.session = sessionContext.getSessionImpl();
        this.hierMgr = sessionContext.getHierarchyManager();
    }

    //-----------------------------------------< controlling batch operations >
    /**
     * Starts an edit operation on the wrapped state manager.
     * At the end of this operation, either {@link #update} or {@link #cancel}
     * must be invoked.
     *
     * @throws IllegalStateException if the state manager is already in edit mode
     */
    public void edit() throws IllegalStateException {
        stateMgr.edit();
    }

    /**
     * Store an item state.
     *
     * @param state item state that should be stored
     * @throws IllegalStateException if the manager is not in edit mode.
     */
    public void store(ItemState state) throws IllegalStateException {
        stateMgr.store(state);
    }

    /**
     * Destroy an item state.
     *
     * @param state item state that should be destroyed
     * @throws IllegalStateException if the manager is not in edit mode.
     */
    public void destroy(ItemState state) throws IllegalStateException {
        stateMgr.destroy(state);
    }

    /**
     * End an update operation. This will save all changes made since
     * the last invocation of {@link #edit()}. If this operation fails,
     * no item will have been saved.
     *
     * @throws RepositoryException   if the update operation failed
     * @throws IllegalStateException if the state manager is not in edit mode
     */
    public void update() throws RepositoryException, IllegalStateException {
        try {
            stateMgr.update();
        } catch (ItemStateException ise) {
            String msg = "update operation failed";
            log.debug(msg, ise);
            throw new RepositoryException(msg, ise);
        }
    }

    /**
     * Cancel an update operation. This will undo all changes made since
     * the last invocation of {@link #edit()}.
     *
     * @throws IllegalStateException if the state manager is not in edit mode
     */
    public void cancel() throws IllegalStateException {
        stateMgr.cancel();
    }

    //-------------------------------------------< high-level item operations >

    /**
     * Clones the subtree at the node <code>srcAbsPath</code> in to the new
     * location at <code>destAbsPath</code>. This operation is only supported:
     * <ul>
     * <li>If the source element has the mixin <code>mix:shareable</code> (or some
     * derived node type)</li>
     * <li>If the parent node of <code>destAbsPath</code> has not already a shareable
     * node in the same shared set as the node at <code>srcPath</code>.</li>
     * </ul>
     *
     * @param srcPath source path
     * @param destPath destination path
     * @return the node id of the destination's parent
     *
     * @throws ConstraintViolationException if the operation would violate a
     * node-type or other implementation-specific constraint.
     * @throws VersionException if the parent node of <code>destAbsPath</code> is
     * versionable and checked-in, or is non-versionable but its nearest versionable ancestor is
     * checked-in. This exception will also be thrown if <code>removeExisting</code> is <code>true</code>,
     * and a UUID conflict occurs that would require the moving and/or altering of a node that is checked-in.
     * @throws AccessDeniedException if the current session does not have
     * sufficient access rights to complete the operation.
     * @throws PathNotFoundException if the node at <code>srcAbsPath</code> in
     * <code>srcWorkspace</code> or the parent of <code>destAbsPath</code> in this workspace does not exist.
     * @throws ItemExistsException if a property already exists at
     * <code>destAbsPath</code> or a node already exist there, and same name
     * siblings are not allowed or if <code>removeExisting</code> is false and a
     * UUID conflict occurs.
     * @throws LockException if a lock prevents the clone.
     * @throws RepositoryException if the last element of <code>destAbsPath</code>
     * has an index or if another error occurs.
     */
    public NodeId clone(Path srcPath, Path destPath)
            throws ConstraintViolationException, AccessDeniedException,
                   VersionException, PathNotFoundException, ItemExistsException,
                   LockException, RepositoryException, IllegalStateException {

        // check precondition
        checkInEditMode();

        // 1. check paths & retrieve state
        NodeState srcState = getNodeState(srcPath);

        Path destParentPath = destPath.getAncestor(1);
        NodeState destParentState = getNodeState(destParentPath);
        int ind = destPath.getIndex();
        if (ind > 0) {
            // subscript in name element
            String msg =
                "invalid destination path: " + safeGetJCRPath(destPath)
                + " (subscript in name element is not allowed)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        return clone(srcState, destParentState, destPath.getName());
    }

    /**
     * Implementation of {@link #clone(Path, Path)} that has already determined
     * the affected <code>NodeState</code>s.
     *
     * @param srcState source state
     * @param destParentState destination parent state
     * @param destName destination name
     * @return the node id of the destination's parent
     *
     * @throws ConstraintViolationException if the operation would violate a
     * node-type or other implementation-specific constraint.
     * @throws VersionException if the parent node of <code>destAbsPath</code> is
     * versionable and checked-in, or is non-versionable but its nearest versionable ancestor is
     * checked-in. This exception will also be thrown if <code>removeExisting</code> is <code>true</code>,
     * and a UUID conflict occurs that would require the moving and/or altering of a node that is checked-in.
     * @throws AccessDeniedException if the current session does not have
     * sufficient access rights to complete the operation.
     * @throws PathNotFoundException if the node at <code>srcAbsPath</code> in
     * <code>srcWorkspace</code> or the parent of <code>destAbsPath</code> in this workspace does not exist.
     * @throws ItemExistsException if a property already exists at
     * <code>destAbsPath</code> or a node already exist there, and same name
     * siblings are not allowed or if <code>removeExisting</code> is false and a
     * UUID conflict occurs.
     * @throws LockException if a lock prevents the clone.
     * @throws RepositoryException if the last element of <code>destAbsPath</code>
     * has an index or if another error occurs.
     * @see #clone(Path, Path)
     */
    public NodeId clone(NodeState srcState, NodeState destParentState, Name destName)
            throws ConstraintViolationException, AccessDeniedException,
                   VersionException, PathNotFoundException, ItemExistsException,
                   LockException, RepositoryException, IllegalStateException {


        // 2. check access rights, lock status, node type constraints, etc.
        checkAddNode(destParentState, destName,
                srcState.getNodeTypeName(), CHECK_ACCESS | CHECK_LOCK
                | CHECK_CHECKED_OUT | CHECK_CONSTRAINTS | CHECK_HOLD | CHECK_RETENTION);

        // 3. verify that source has mixin mix:shareable
        if (!isShareable(srcState)) {
            String msg =
                "Cloning inside a workspace is only allowed for shareable"
                + " nodes. Node with type " + srcState.getNodeTypeName()
                + " is not shareable.";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // 4. detect share cycle
        NodeId srcId = srcState.getNodeId();
        NodeId destParentId = destParentState.getNodeId();
        if (destParentId.equals(srcId) || hierMgr.isAncestor(srcId, destParentId)) {
            String msg =
                "Cloning Node with id " + srcId
                + " to parent with id " + destParentId
                + " would create a share cycle.";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // 5. do clone operation (modify and store affected states)
        if (!srcState.addShare(destParentState.getNodeId())) {
            String msg =
                "Adding a shareable node with id ("
                + destParentState.getNodeId()
                + ") twice to the same parent is not supported.";
            log.debug(msg);
            throw new UnsupportedRepositoryOperationException(msg);
        }
        destParentState.addChildNodeEntry(destName, srcState.getNodeId());

        // store states
        stateMgr.store(srcState);
        stateMgr.store(destParentState);
        return destParentState.getNodeId();
    }


    /**
     * Copies the tree at <code>srcPath</code> to the new location at
     * <code>destPath</code>. Returns the id of the node at its new position.
     * <p>
     * <b>Precondition:</b> the state manager needs to be in edit mode.
     *
     * @param srcPath
     * @param destPath
     * @param flag     one of
     *                 <ul>
     *                 <li><code>COPY</code></li>
     *                 <li><code>CLONE</code></li>
     *                 <li><code>CLONE_REMOVE_EXISTING</code></li>
     *                 </ul>
     * @return the id of the node at its new position
     * @throws RepositoryException if the copy operation fails
     */
    public NodeId copy(Path srcPath, Path destPath, int flag)
            throws RepositoryException {
        return copy(
                srcPath, stateMgr, hierMgr, context.getAccessManager(),
                destPath, flag);
    }

    /**
     * Copies the tree at <code>srcPath</code> retrieved using the specified
     * <code>srcStateMgr</code> to the new location at <code>destPath</code>.
     * Returns the id of the node at its new position.
     * <p>
     * <b>Precondition:</b> the state manager needs to be in edit mode.
     *
     * @param srcPath
     * @param srcStateMgr
     * @param srcHierMgr
     * @param srcAccessMgr
     * @param destPath
     * @param flag         one of
     *                     <ul>
     *                     <li><code>COPY</code></li>
     *                     <li><code>CLONE</code></li>
     *                     <li><code>CLONE_REMOVE_EXISTING</code></li>
     *                     </ul>
     * @return the id of the node at its new position
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws PathNotFoundException
     * @throws ItemExistsException
     * @throws LockException
     * @throws RepositoryException
     * @throws IllegalStateException if the state manager is not in edit mode.
     */
    public NodeId copy(Path srcPath,
                       ItemStateManager srcStateMgr,
                       HierarchyManager srcHierMgr,
                       AccessManager srcAccessMgr,
                       Path destPath,
                       int flag)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException, IllegalStateException {

        // check precondition
        checkInEditMode();

        // 1. check paths & retrieve state

        NodeState srcState = getNodeState(srcStateMgr, srcHierMgr, srcPath);

        Path destParentPath = destPath.getAncestor(1);
        NodeState destParentState = getNodeState(destParentPath);
        int ind = destPath.getIndex();
        if (ind > 0) {
            // subscript in name element
            String msg =
                "invalid copy destination path: " + safeGetJCRPath(destPath)
                + " (subscript in name element is not allowed)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // 2. check access rights, lock status, node type constraints, etc.

        // JCR-2269: store target node state in changelog early as a
        // precautionary measure in order to isolate it from concurrent
        // underlying changes while checking preconditions
        stateMgr.store(destParentState);
        checkAddNode(destParentState, destPath.getName(),
                srcState.getNodeTypeName(), CHECK_ACCESS | CHECK_LOCK
                | CHECK_CHECKED_OUT | CHECK_CONSTRAINTS | CHECK_HOLD | CHECK_RETENTION);
        // check read access right on source node using source access manager
        try {
            if (!srcAccessMgr.isGranted(srcPath, Permission.READ)) {
                throw new PathNotFoundException(safeGetJCRPath(srcPath));
            }
        } catch (ItemNotFoundException infe) {
            String msg =
                "internal error: failed to check access rights for "
                + safeGetJCRPath(srcPath);
            log.debug(msg);
            throw new RepositoryException(msg, infe);
        }

        // 3. do copy operation (modify and store affected states)

        ReferenceChangeTracker refTracker = new ReferenceChangeTracker();

        // create deep copy of source node state
        NodeState newState = copyNodeState(srcState, srcPath, srcStateMgr, srcAccessMgr,
                destParentState.getNodeId(), flag, refTracker);

        // add to new parent
        destParentState.addChildNodeEntry(destPath.getName(), newState.getNodeId());

        // adjust references that refer to uuid's which have been mapped to
        // newly generated uuid's on copy/clone
        Iterator<Object> iter = refTracker.getProcessedReferences();
        while (iter.hasNext()) {
            PropertyState prop = (PropertyState) iter.next();
            // being paranoid...
            if (prop.getType() != PropertyType.REFERENCE
                    && prop.getType() != PropertyType.WEAKREFERENCE) {
                continue;
            }
            boolean modified = false;
            InternalValue[] values = prop.getValues();
            InternalValue[] newVals = new InternalValue[values.length];
            for (int i = 0; i < values.length; i++) {
                NodeId adjusted = refTracker.getMappedId(values[i].getNodeId());
                if (adjusted != null) {
                    boolean weak = prop.getType() == PropertyType.WEAKREFERENCE;
                    newVals[i] = InternalValue.create(adjusted, weak);
                    modified = true;
                } else {
                    // reference doesn't need adjusting, just copy old value
                    newVals[i] = values[i];
                }
            }
            if (modified) {
                prop.setValues(newVals);
                stateMgr.store(prop);
            }
        }
        refTracker.clear();

        // store states
        stateMgr.store(newState);
        stateMgr.store(destParentState);
        return newState.getNodeId();
    }

    /**
     * Moves the tree at <code>srcPath</code> to the new location at
     * <code>destPath</code>. Returns the id of the moved node.
     * <p>
     * <b>Precondition:</b> the state manager needs to be in edit mode.
     *
     * @param srcPath
     * @param destPath
     * @return the id of the moved node
     * @throws ConstraintViolationException
     * @throws VersionException
     * @throws AccessDeniedException
     * @throws PathNotFoundException
     * @throws ItemExistsException
     * @throws LockException
     * @throws RepositoryException
     * @throws IllegalStateException        if the state manager is not in edit mode
     */
    public NodeId move(Path srcPath, Path destPath)
            throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException, IllegalStateException {

        // check precondition
        if (!stateMgr.inEditMode()) {
            throw new IllegalStateException(
                    "cannot move path " + safeGetJCRPath(srcPath)
                    + " because manager is not in edit mode");
        }

        // 1. check paths & retrieve state

        try {
            if (srcPath.isAncestorOf(destPath)) {
                String msg =
                    safeGetJCRPath(destPath) + ": invalid destination path"
                    + " (cannot be descendant of source path)";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
        } catch (MalformedPathException mpe) {
            String msg = "invalid path for move: " + safeGetJCRPath(destPath);
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }

        Path srcParentPath = srcPath.getAncestor(1);
        NodeState target = getNodeState(srcPath);
        NodeState srcParent = getNodeState(srcParentPath);

        Path destParentPath = destPath.getAncestor(1);
        NodeState destParent = getNodeState(destParentPath);

        int ind = destPath.getIndex();
        if (ind > 0) {
            // subscript in name element
            String msg =
                safeGetJCRPath(destPath) + ": invalid destination path"
                + " (subscript in name element is not allowed)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        HierarchyManagerImpl hierMgr = (HierarchyManagerImpl) this.hierMgr;
        if (hierMgr.isShareAncestor(target.getNodeId(), destParent.getNodeId())) {
            String msg =
                safeGetJCRPath(destPath) + ": invalid destination path"
                + " (share cycle detected)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // 2. check if target state can be removed from old/added to new parent

        checkRemoveNode(target, srcParent.getNodeId(),
                CHECK_ACCESS | CHECK_LOCK | CHECK_CHECKED_OUT | CHECK_CONSTRAINTS
                | CHECK_HOLD | CHECK_RETENTION);
        checkAddNode(destParent, destPath.getName(),
                target.getNodeTypeName(), CHECK_ACCESS | CHECK_LOCK
                | CHECK_CHECKED_OUT | CHECK_CONSTRAINTS | CHECK_HOLD | CHECK_RETENTION);

        // 3. do move operation (modify and store affected states)
        boolean renameOnly = srcParent.getNodeId().equals(destParent.getNodeId());

        int srcNameIndex = srcPath.getIndex();
        if (srcNameIndex == 0) {
            srcNameIndex = 1;
        }

        stateMgr.store(target);
        if (renameOnly) {
            stateMgr.store(srcParent);
            // change child node entry
            destParent.renameChildNodeEntry(srcPath.getName(), srcNameIndex,
                    destPath.getName());
        } else {
            // check shareable case
            if (target.isShareable()) {
                String msg =
                    "Moving a shareable node (" + safeGetJCRPath(srcPath)
                    + ") is not supported.";
                log.debug(msg);
                throw new UnsupportedRepositoryOperationException(msg);
            }

            stateMgr.store(srcParent);
            stateMgr.store(destParent);

            // do move:
            // 1. remove child node entry from old parent
            if (srcParent.removeChildNodeEntry(target.getNodeId())) {
                // 2. re-parent target node
                target.setParentId(destParent.getNodeId());
                // 3. add child node entry to new parent
                destParent.addChildNodeEntry(destPath.getName(), target.getNodeId());
            }
        }

        return target.getNodeId();
    }

    /**
     * Removes the specified node, recursively removing its properties and
     * child nodes.
     * <p>
     * <b>Precondition:</b> the state manager needs to be in edit mode.
     *
     * @param nodePath
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws LockException
     * @throws ItemNotFoundException
     * @throws ReferentialIntegrityException
     * @throws RepositoryException
     * @throws IllegalStateException
     */
    public void removeNode(Path nodePath)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, LockException, ItemNotFoundException,
            ReferentialIntegrityException, RepositoryException,
            IllegalStateException {

        // check precondition
        if (!stateMgr.inEditMode()) {
            throw new IllegalStateException(
                    "cannot remove node (" + safeGetJCRPath(nodePath)
                    + ") because manager is not in edit mode");
        }

        // 1. retrieve affected state
        NodeState target = getNodeState(nodePath);
        NodeId parentId = target.getParentId();

        // 2. check if target state can be removed from parent
        checkRemoveNode(target, parentId,
                CHECK_ACCESS | CHECK_LOCK | CHECK_CHECKED_OUT
                | CHECK_CONSTRAINTS | CHECK_REFERENCES | CHECK_HOLD | CHECK_RETENTION);

        // 3. do remove operation
        removeNodeState(target);
    }

    //--------------------------------------< misc. high-level helper methods >
    /**
     * Checks if adding a child node called <code>nodeName</code> of node type
     * <code>nodeTypeName</code> to the given parent node is allowed in the
     * current context.
     *
     * @param parentState
     * @param nodeName
     * @param nodeTypeName
     * @param options      bit-wise OR'ed flags specifying the checks that should be
     *                     performed; any combination of the following constants:
     *                     <ul>
     *                     <li><code>{@link #CHECK_ACCESS}</code>: make sure
     *                     current session is granted read &amp; write access on
     *                     parent node</li>
     *                     <li><code>{@link #CHECK_LOCK}</code>: make sure
     *                     there's no foreign lock on parent node</li>
     *                     <li><code>{@link #CHECK_CHECKED_OUT}</code>: make sure
     *                     parent node is checked-out</li>
     *                     <li><code>{@link #CHECK_CONSTRAINTS}</code>:
     *                     make sure no node type constraints would be violated</li>
     *                     <li><code>{@link #CHECK_HOLD}</code>: check for effective holds preventing the add operation</li>
     *                     <li><code>{@link #CHECK_RETENTION}</code>: check for effective retention policy preventing the add operation</li>
     *                     </ul>
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws LockException
     * @throws ItemNotFoundException
     * @throws ItemExistsException
     * @throws RepositoryException
     */
    public void checkAddNode(NodeState parentState, Name nodeName,
                             Name nodeTypeName, int options)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, LockException, ItemNotFoundException,
            ItemExistsException, RepositoryException {

        Path parentPath = hierMgr.getPath(parentState.getNodeId());

        // 1. locking status

        if ((options & CHECK_LOCK) == CHECK_LOCK) {
            // make sure there's no foreign lock on parent node
            verifyUnlocked(parentPath);
        }

        // 2. versioning status

        if ((options & CHECK_CHECKED_OUT) == CHECK_CHECKED_OUT) {
            // make sure parent node is checked-out
            verifyCheckedOut(parentPath);
        }

        // 3. access rights

        if ((options & CHECK_ACCESS) == CHECK_ACCESS) {
            AccessManager accessMgr = context.getAccessManager();
            // make sure current session is granted read access on parent node
            if (!accessMgr.isGranted(parentPath, Permission.READ)) {
                throw new ItemNotFoundException(safeGetJCRPath(parentState.getNodeId()));
            }
            // make sure current session is granted write access on parent node
            if (!accessMgr.isGranted(parentPath, nodeName, Permission.ADD_NODE)) {
                throw new AccessDeniedException(safeGetJCRPath(parentState.getNodeId())
                        + ": not allowed to add child node");
            }
            // make sure the editing session is allowed create nodes with a
            // specified node type (and ev. mixins)
            if (!accessMgr.isGranted(parentPath, nodeName, Permission.NODE_TYPE_MNGMT)) {
                throw new AccessDeniedException(safeGetJCRPath(parentState.getNodeId())
                        + ": not allowed to add child node");
            }
        }

        // 4. node type constraints

        if ((options & CHECK_CONSTRAINTS) == CHECK_CONSTRAINTS) {
            QItemDefinition parentDef =
                context.getItemManager().getDefinition(parentState).unwrap();
            // make sure parent node is not protected
            if (parentDef.isProtected()) {
                throw new ConstraintViolationException(
                        safeGetJCRPath(parentState.getNodeId())
                        + ": cannot add child node to protected parent node");
            }
            // make sure there's an applicable definition for new child node
            EffectiveNodeType entParent = getEffectiveNodeType(parentState);
            entParent.checkAddNodeConstraints(
                    nodeName, nodeTypeName, context.getNodeTypeRegistry());
            QNodeDefinition newNodeDef =
                    findApplicableNodeDefinition(nodeName, nodeTypeName,
                            parentState);

            // check for name collisions
            if (parentState.hasChildNodeEntry(nodeName)) {
                // there's already a node with that name...

                // get definition of existing conflicting node
                ChildNodeEntry entry = parentState.getChildNodeEntry(nodeName, 1);
                NodeState conflictingState;
                NodeId conflictingId = entry.getId();
                try {
                    conflictingState = (NodeState) stateMgr.getItemState(conflictingId);
                } catch (ItemStateException ise) {
                    String msg =
                        "internal error: failed to retrieve state of "
                        + safeGetJCRPath(conflictingId);
                    log.debug(msg);
                    throw new RepositoryException(msg, ise);
                }
                QNodeDefinition conflictingTargetDef =
                    context.getItemManager().getDefinition(conflictingState).unwrap();
                // check same-name sibling setting of both target and existing node
                if (!conflictingTargetDef.allowsSameNameSiblings()
                        || !newNodeDef.allowsSameNameSiblings()) {
                    throw new ItemExistsException(
                            "cannot add child node '" + nodeName.getLocalName()
                            + "' to " + safeGetJCRPath(parentState.getNodeId())
                            + ": colliding with same-named existing node");
                }
            }
        }

        RetentionRegistry retentionReg =
            context.getSessionImpl().getRetentionRegistry();
        if ((options & CHECK_HOLD) == CHECK_HOLD) {
            if (retentionReg.hasEffectiveHold(parentPath, false)) {
                throw new RepositoryException("Unable to add node. Parent is affected by a hold.");
            }
        }
        if ((options & CHECK_RETENTION) == CHECK_RETENTION) {
            if (retentionReg.hasEffectiveRetention(parentPath, false)) {
                throw new RepositoryException("Unable to add node. Parent is affected by a retention.");
            }
        }
    }

    /**
     * Checks if removing the given target node is allowed in the current context.
     *
     * @param targetState
     * @param options     bit-wise OR'ed flags specifying the checks that should be
     *                    performed; any combination of the following constants:
     *                    <ul>
     *                    <li><code>{@link #CHECK_ACCESS}</code>: make sure
     *                    current session is granted read access on parent
     *                    and remove privilege on target node</li>
     *                    <li><code>{@link #CHECK_LOCK}</code>: make sure
     *                    there's no foreign lock on parent node</li>
     *                    <li><code>{@link #CHECK_CHECKED_OUT}</code>: make sure
     *                    parent node is checked-out</li>
     *                    <li><code>{@link #CHECK_CONSTRAINTS}</code>:
     *                    make sure no node type constraints would be violated</li>
     *                    <li><code>{@link #CHECK_REFERENCES}</code>:
     *                    make sure no references exist on target node</li>
     *                    <li><code>{@link #CHECK_HOLD}</code>: check for effective holds preventing the add operation</li>
     *                    <li><code>{@link #CHECK_RETENTION}</code>: check for effective retention policy preventing the add operation</li>
     *                    </ul>
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws LockException
     * @throws ItemNotFoundException
     * @throws ReferentialIntegrityException
     * @throws RepositoryException
     */
    public void checkRemoveNode(NodeState targetState, int options)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, LockException, ItemNotFoundException,
            ReferentialIntegrityException, RepositoryException {
        checkRemoveNode(targetState, targetState.getParentId(), options);
    }

    /**
     * Checks if removing the given target node from the specifed parent
     * is allowed in the current context.
     *
     * @param targetState
     * @param parentId
     * @param options     bit-wise OR'ed flags specifying the checks that should be
     *                    performed; any combination of the following constants:
     *                    <ul>
     *                    <li><code>{@link #CHECK_ACCESS}</code>: make sure
     *                    current session is granted read access on parent
     *                    and remove privilege on target node</li>
     *                    <li><code>{@link #CHECK_LOCK}</code>: make sure
     *                    there's no foreign lock on parent node</li>
     *                    <li><code>{@link #CHECK_CHECKED_OUT}</code>: make sure
     *                    parent node is checked-out</li>
     *                    <li><code>{@link #CHECK_CONSTRAINTS}</code>:
     *                    make sure no node type constraints would be violated</li>
     *                    <li><code>{@link #CHECK_REFERENCES}</code>:
     *                    make sure no references exist on target node</li>
     *                    <li><code>{@link #CHECK_HOLD}</code>: check for effective holds preventing the add operation</li>
     *                    <li><code>{@link #CHECK_RETENTION}</code>: check for effective retention policy preventing the add operation</li>
     *                    </ul>
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws LockException
     * @throws ItemNotFoundException
     * @throws ReferentialIntegrityException
     * @throws RepositoryException
     */
    public void checkRemoveNode(NodeState targetState, NodeId parentId,
                                int options)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, LockException, ItemNotFoundException,
            ReferentialIntegrityException, RepositoryException {

        if (targetState.getParentId() == null) {
            // root or orphaned node
            throw new ConstraintViolationException("cannot remove root node");
        }
        Path targetPath = hierMgr.getPath(targetState.getNodeId());
        NodeState parentState = getNodeState(parentId);
        Path parentPath = hierMgr.getPath(parentId);

        // 1. locking status

        if ((options & CHECK_LOCK) == CHECK_LOCK) {
            // make sure there's no foreign lock on parent node
            verifyUnlocked(parentPath);
        }

        // 2. versioning status

        if ((options & CHECK_CHECKED_OUT) == CHECK_CHECKED_OUT) {
            // make sure parent node is checked-out
            verifyCheckedOut(parentPath);
        }

        // 3. access rights

        if ((options & CHECK_ACCESS) == CHECK_ACCESS) {
            try {
                AccessManager accessMgr = context.getAccessManager();
                // make sure current session is granted read access on parent node
                if (!accessMgr.isGranted(targetPath, Permission.READ)) {
                    throw new PathNotFoundException(safeGetJCRPath(targetPath));
                }
                // make sure current session is allowed to remove target node
                if (!accessMgr.isGranted(targetPath, Permission.REMOVE_NODE)) {
                    throw new AccessDeniedException(safeGetJCRPath(targetPath)
                            + ": not allowed to remove node");
                }
            } catch (ItemNotFoundException infe) {
                String msg = "internal error: failed to check access rights for "
                        + safeGetJCRPath(targetPath);
                log.debug(msg);
                throw new RepositoryException(msg, infe);
            }
        }

        // 4. node type constraints

        if ((options & CHECK_CONSTRAINTS) == CHECK_CONSTRAINTS) {
            QItemDefinition parentDef =
                context.getItemManager().getDefinition(parentState).unwrap();
            if (parentDef.isProtected()) {
                throw new ConstraintViolationException(safeGetJCRPath(parentId)
                        + ": cannot remove child node of protected parent node");
            }
            QItemDefinition targetDef =
                context.getItemManager().getDefinition(targetState).unwrap();
            if (targetDef.isMandatory()) {
                throw new ConstraintViolationException(safeGetJCRPath(targetPath)
                        + ": cannot remove mandatory node");
            }
            if (targetDef.isProtected()) {
                throw new ConstraintViolationException(safeGetJCRPath(targetPath)
                        + ": cannot remove protected node");
            }
        }

        // 5. referential integrity

        if ((options & CHECK_REFERENCES) == CHECK_REFERENCES) {
            EffectiveNodeType ent = getEffectiveNodeType(targetState);
            if (ent.includesNodeType(NameConstants.MIX_REFERENCEABLE)) {
                NodeId targetId = targetState.getNodeId();
                if (stateMgr.hasNodeReferences(targetId)) {
                    try {
                        NodeReferences refs = stateMgr.getNodeReferences(targetId);
                        if (refs.hasReferences()) {
                            throw new ReferentialIntegrityException(safeGetJCRPath(targetPath)
                                    + ": cannot remove node with references");
                        }
                    } catch (ItemStateException ise) {
                        String msg = "internal error: failed to check references on "
                                + safeGetJCRPath(targetPath);
                        log.error(msg, ise);
                        throw new RepositoryException(msg, ise);
                    }
                }
            }
        }

        RetentionRegistry retentionReg =
            context.getSessionImpl().getRetentionRegistry();
        if ((options & CHECK_HOLD) == CHECK_HOLD) {
            if (retentionReg.hasEffectiveHold(targetPath, true)) {
                throw new RepositoryException("Unable to perform removal. Node is affected by a hold.");
            }
        }
        if ((options & CHECK_RETENTION) == CHECK_RETENTION) {
            if (retentionReg.hasEffectiveRetention(targetPath, true)) {
                throw new RepositoryException("Unable to perform removal. Node is affected by a retention.");
            }
        }
    }

    /**
     * Verifies that the node at <code>nodePath</code> is writable. The
     * following conditions must hold true:
     * <ul>
     * <li>the node must exist</li>
     * <li>the current session must be granted read &amp; write access on it</li>
     * <li>the node must not be locked by another session</li>
     * <li>the node must not be checked-in</li>
     * <li>the node must not be protected</li>
     * <li>the node must not be affected by a hold or a retention policy</li>
     * </ul>
     *
     * @param nodePath path of node to check
     * @throws PathNotFoundException        if no node exists at
     *                                      <code>nodePath</code> of the current
     *                                      session is not granted read access
     *                                      to the specified path
     * @throws AccessDeniedException        if write access to the specified
     *                                      path is not allowed
     * @throws ConstraintViolationException if the node at <code>nodePath</code>
     *                                      is protected
     * @throws VersionException             if the node at <code>nodePath</code>
     *                                      is checked-in
     * @throws LockException                if the node at <code>nodePath</code>
     *                                      is locked by another session
     * @throws RepositoryException          if another error occurs
     */
    public void verifyCanWrite(Path nodePath)
            throws PathNotFoundException, AccessDeniedException,
            ConstraintViolationException, VersionException, LockException,
            RepositoryException {

        NodeState node = getNodeState(nodePath);

        // access rights
        // make sure current session is granted read access on node
        AccessManager accessMgr = context.getAccessManager();
        if (!accessMgr.isGranted(nodePath, Permission.READ)) {
            throw new PathNotFoundException(safeGetJCRPath(node.getNodeId()));
        }
        // TODO: removed check for 'WRITE' permission on node due to the fact,
        // TODO: that add_node and set_property permission are granted on the
        // TODO: items to be create/modified and not on their parent.
        // in any case, the ability to add child-nodes and properties is checked
        // while executing the corresponding operation.

        // locking status
        verifyUnlocked(nodePath);

        // node type constraints
        verifyNotProtected(nodePath);

        // versioning status
        verifyCheckedOut(nodePath);

        RetentionRegistry retentionReg =
            context.getSessionImpl().getRetentionRegistry();
        if (retentionReg.hasEffectiveHold(nodePath, false)) {
            throw new RepositoryException("Unable to write. Node is affected by a hold.");
        }
        if (retentionReg.hasEffectiveRetention(nodePath, false)) {
            throw new RepositoryException("Unable to write. Node is affected by a retention.");
        }
    }

    /**
     * Verifies that the node at <code>nodePath</code> can be read. The
     * following conditions must hold true:
     * <ul>
     * <li>the node must exist</li>
     * <li>the current session must be granted read access on it</li>
     * </ul>
     *
     * @param nodePath path of node to check
     * @throws PathNotFoundException if no node exists at
     *                               <code>nodePath</code> of the current
     *                               session is not granted read access
     *                               to the specified path
     * @throws RepositoryException   if another error occurs
     */
    public void verifyCanRead(Path nodePath)
            throws PathNotFoundException, RepositoryException {
        // access rights
        // make sure current session is granted read access on node
        AccessManager accessMgr = context.getAccessManager();
        if (!accessMgr.isGranted(nodePath, Permission.READ)) {
            throw new PathNotFoundException(safeGetJCRPath(nodePath));
        }
    }

    //--------------------------------------------< low-level item operations >
    /**
     * Creates a new node.
     * <p>
     * Note that access rights are <b><i>not</i></b> enforced!
     * <p>
     * <b>Precondition:</b> the state manager needs to be in edit mode.
     *
     * @param parent
     * @param nodeName
     * @param nodeTypeName
     * @param mixinNames
     * @param id
     * @return
     * @throws ItemExistsException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     * @throws IllegalStateException if the state manager is not in edit mode.
     */
    public NodeState createNodeState(NodeState parent,
                                     Name nodeName,
                                     Name nodeTypeName,
                                     Name[] mixinNames,
                                     NodeId id)
            throws ItemExistsException, ConstraintViolationException,
            RepositoryException, IllegalStateException {

        // check precondition
        if (!stateMgr.inEditMode()) {
            throw new IllegalStateException(
                    "cannot create node state for " + nodeName
                    + " because manager is not in edit mode");
        }

        QNodeDefinition def = findApplicableNodeDefinition(nodeName, nodeTypeName, parent);
        return createNodeState(parent, nodeName, nodeTypeName, mixinNames, id, def);
    }

    /**
     * Creates a new node based on the given definition.
     * <p>
     * Note that access rights are <b><i>not</i></b> enforced!
     * <p>
     * <b>Precondition:</b> the state manager needs to be in edit mode.
     *
     * @param parent
     * @param nodeName
     * @param nodeTypeName
     * @param mixinNames
     * @param id
     * @param def
     * @return
     * @throws ItemExistsException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     * @throws IllegalStateException
     */
    public NodeState createNodeState(NodeState parent,
                                     Name nodeName,
                                     Name nodeTypeName,
                                     Name[] mixinNames,
                                     NodeId id,
                                     QNodeDefinition def)
            throws ItemExistsException, ConstraintViolationException,
            RepositoryException, IllegalStateException {

        // check for name collisions with existing nodes
        if (!def.allowsSameNameSiblings() && parent.hasChildNodeEntry(nodeName)) {
            NodeId errorId = parent.getChildNodeEntry(nodeName, 1).getId();
            throw new ItemExistsException(safeGetJCRPath(errorId));
        }
        if (nodeTypeName == null) {
            // no primary node type specified,
            // try default primary type from definition
            nodeTypeName = def.getDefaultPrimaryType();
            if (nodeTypeName == null) {
                String msg =
                    "an applicable node type could not be determined for "
                    + nodeName;
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
        NodeState node = stateMgr.createNew(id, nodeTypeName, parent.getNodeId());
        if (mixinNames != null && mixinNames.length > 0) {
            node.setMixinTypeNames(new HashSet<Name>(Arrays.asList(mixinNames)));
        }

        // now add new child node entry to parent
        parent.addChildNodeEntry(nodeName, node.getNodeId());

        EffectiveNodeType ent = getEffectiveNodeType(node);

        // check shareable
        if (ent.includesNodeType(NameConstants.MIX_SHAREABLE)) {
            node.addShare(parent.getNodeId());
        }

        if (!node.getMixinTypeNames().isEmpty()) {
            // create jcr:mixinTypes property
            QPropertyDefinition pd = ent.getApplicablePropertyDef(NameConstants.JCR_MIXINTYPES,
                    PropertyType.NAME, true);
            createPropertyState(node, pd.getName(), pd.getRequiredType(), pd);
        }

        // add 'auto-create' properties defined in node type
        for (QPropertyDefinition pd : ent.getAutoCreatePropDefs()) {
            createPropertyState(node, pd.getName(), pd.getRequiredType(), pd);
        }

        // recursively add 'auto-create' child nodes defined in node type
        for (QNodeDefinition nd : ent.getAutoCreateNodeDefs()) {
            createNodeState(node, nd.getName(), nd.getDefaultPrimaryType(),
                    null, null, nd);
        }

        // store node
        stateMgr.store(node);
        // store parent
        stateMgr.store(parent);

        return node;
    }

    /**
     * Creates a new property.
     * <p>
     * Note that access rights are <b><i>not</i></b> enforced!
     * <p>
     * <b>Precondition:</b> the state manager needs to be in edit mode.
     *
     * @param parent
     * @param propName
     * @param type
     * @param numValues
     * @return
     * @throws ItemExistsException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     * @throws IllegalStateException if the state manager is not in edit mode
     */
    public PropertyState createPropertyState(NodeState parent,
                                             Name propName,
                                             int type,
                                             int numValues)
            throws ItemExistsException, ConstraintViolationException,
            RepositoryException, IllegalStateException {

        // check precondition
        if (!stateMgr.inEditMode()) {
            throw new IllegalStateException(
                    "cannot create property state for " + propName
                    + " because manager is not in edit mode");
        }

        // find applicable definition
        QPropertyDefinition def;
        // multi- or single-valued property?
        if (numValues == 1) {
            // could be single- or multi-valued (n == 1)
            try {
                // try single-valued
                def = findApplicablePropertyDefinition(propName,
                        type, false, parent);
            } catch (ConstraintViolationException cve) {
                // try multi-valued
                def = findApplicablePropertyDefinition(propName,
                        type, true, parent);
            }
        } else {
            // can only be multi-valued (n == 0 || n > 1)
            def = findApplicablePropertyDefinition(propName,
                    type, true, parent);
        }
        return createPropertyState(parent, propName, type, def);
    }

    /**
     * Creates a new property based on the given definition.
     * <p>
     * Note that access rights are <b><i>not</i></b> enforced!
     * <p>
     * <b>Precondition:</b> the state manager needs to be in edit mode.
     *
     * @param parent
     * @param propName
     * @param type
     * @param def
     * @return
     * @throws ItemExistsException
     * @throws RepositoryException
     */
    public PropertyState createPropertyState(NodeState parent,
                                             Name propName,
                                             int type,
                                             QPropertyDefinition def)
            throws ItemExistsException, RepositoryException {

        // check for name collisions with existing properties
        if (parent.hasPropertyName(propName)) {
            PropertyId errorId = new PropertyId(parent.getNodeId(), propName);
            throw new ItemExistsException(safeGetJCRPath(errorId));
        }

        // create property
        PropertyState prop = stateMgr.createNew(propName, parent.getNodeId());

        if (def.getRequiredType() != PropertyType.UNDEFINED) {
            prop.setType(def.getRequiredType());
        } else if (type != PropertyType.UNDEFINED) {
            prop.setType(type);
        } else {
            prop.setType(PropertyType.STRING);
        }
        prop.setMultiValued(def.isMultiple());

        // compute system generated values if necessary
        new NodeTypeInstanceHandler(session.getUserID()).setDefaultValues(
                prop, parent, def);

        // now add new property entry to parent
        parent.addPropertyName(propName);
        // store parent
        stateMgr.store(parent);

        return prop;
    }

    /**
     * Unlinks the specified node state from its parent and recursively
     * removes it including its properties and child nodes.
     * <p>
     * Note that no checks (access rights etc.) are performed on the specified
     * target node state. Those checks have to be performed beforehand by the
     * caller. However, the (recursive) removal of target node's child nodes are
     * subject to the following checks: access rights, locking, versioning.
     *
     * @param target
     * @throws RepositoryException if an error occurs
     */
    public void removeNodeState(NodeState target)
            throws RepositoryException {

        NodeId parentId = target.getParentId();
        if (parentId == null) {
            String msg = "root node cannot be removed";
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        // remove target
        recursiveRemoveNodeState(target);
        // remove child node entry from parent
        NodeState parent = getNodeState(parentId);
        parent.removeChildNodeEntry(target.getNodeId());
        // store parent
        stateMgr.store(parent);
    }

    /**
     * Retrieves the state of the node at the given path.
     * <p>
     * Note that access rights are <b><i>not</i></b> enforced!
     *
     * @param nodePath
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public NodeState getNodeState(Path nodePath)
            throws PathNotFoundException, RepositoryException {
        return getNodeState(stateMgr, hierMgr, nodePath);
    }

    /**
     * Retrieves the state of the node with the given id.
     * <p>
     * Note that access rights are <b><i>not</i></b> enforced!
     *
     * @param id
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public NodeState getNodeState(NodeId id)
            throws ItemNotFoundException, RepositoryException {
        return (NodeState) getItemState(stateMgr, id);
    }

    /**
     * Retrieves the state of the property with the given id.
     * <p>
     * Note that access rights are <b><i>not</i></b> enforced!
     *
     * @param id
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public PropertyState getPropertyState(PropertyId id)
            throws ItemNotFoundException, RepositoryException {
        return (PropertyState) getItemState(stateMgr, id);
    }

    /**
     * Retrieves the state of the item with the given id.
     * <p>
     * Note that access rights are <b><i>not</i></b> enforced!
     *
     * @param id
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public ItemState getItemState(ItemId id)
            throws ItemNotFoundException, RepositoryException {
        return getItemState(stateMgr, id);
    }

    //----------------------------------------------------< protected methods >
    /**
     * Verifies that the node at <code>nodePath</code> is checked-out; throws a
     * <code>VersionException</code> if that's not the case.
     * <p>
     * A node is considered <i>checked-out</i> if it is versionable and
     * checked-out, or is non-versionable but its nearest versionable ancestor
     * is checked-out, or is non-versionable and there are no versionable
     * ancestors.
     *
     * @param nodePath
     * @throws PathNotFoundException
     * @throws VersionException
     * @throws RepositoryException
     */
    protected void verifyCheckedOut(Path nodePath)
            throws PathNotFoundException, VersionException, RepositoryException {
        // search nearest ancestor that is versionable, start with node at nodePath
        /**
         * FIXME should not only rely on existence of jcr:isCheckedOut property
         * but also verify that node.isNodeType("mix:versionable")==true;
         * this would have a negative impact on performance though...
         */
        NodeState nodeState = getNodeState(nodePath);
        while (!nodeState.hasPropertyName(NameConstants.JCR_ISCHECKEDOUT)) {
            if (nodePath.denotesRoot()) {
                return;
            }
            nodePath = nodePath.getAncestor(1);
            nodeState = getNodeState(nodePath);
        }
        PropertyId propId =
                new PropertyId(nodeState.getNodeId(), NameConstants.JCR_ISCHECKEDOUT);
        PropertyState propState;
        try {
            propState = (PropertyState) stateMgr.getItemState(propId);
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to retrieve state of "
                    + safeGetJCRPath(propId);
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
        boolean checkedOut = propState.getValues()[0].getBoolean();
        if (!checkedOut) {
            throw new VersionException(safeGetJCRPath(nodePath) + " is checked-in");
        }
    }

    /**
     * Verifies that the node at <code>nodePath</code> is not locked by
     * somebody else than the current session.
     *
     * @param nodePath path of node to check
     * @throws PathNotFoundException
     * @throws LockException         if write access to the specified path is not allowed
     * @throws RepositoryException   if another error occurs
     */
    protected void verifyUnlocked(Path nodePath)
            throws LockException, RepositoryException {
        // make sure there's no foreign lock on node at nodePath
        context.getWorkspace().getInternalLockManager().checkLock(
                nodePath, session);
    }

    /**
     * Verifies that the node at <code>nodePath</code> is not protected.
     *
     * @param nodePath path of node to check
     * @throws PathNotFoundException        if no node exists at <code>nodePath</code>
     * @throws ConstraintViolationException if write access to the specified
     *                                      path is not allowed
     * @throws RepositoryException          if another error occurs
     */
    protected void verifyNotProtected(Path nodePath)
            throws PathNotFoundException, ConstraintViolationException,
            RepositoryException {
        NodeState node = getNodeState(nodePath);
        if (context.getItemManager().getDefinition(node).isProtected()) {
            throw new ConstraintViolationException(safeGetJCRPath(nodePath)
                    + ": node is protected");
        }
    }

    /**
     * Retrieves the state of the node at <code>nodePath</code> using the given
     * item state manager.
     * <p>
     * Note that access rights are <b><i>not</i></b> enforced!
     *
     * @param srcStateMgr
     * @param srcHierMgr
     * @param nodePath
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    protected NodeState getNodeState(ItemStateManager srcStateMgr,
                                     HierarchyManager srcHierMgr,
                                     Path nodePath)
            throws PathNotFoundException, RepositoryException {
        try {
            NodeId id = srcHierMgr.resolveNodePath(nodePath);
            if (id == null) {
                throw new PathNotFoundException(safeGetJCRPath(nodePath));
            }
            return (NodeState) getItemState(srcStateMgr, id);
        } catch (ItemNotFoundException infe) {
            throw new PathNotFoundException(safeGetJCRPath(nodePath));
        }
    }

    /**
     * Retrieves the state of the item with the specified id using the given
     * item state manager.
     * <p>
     * Note that access rights are <b><i>not</i></b> enforced!
     *
     * @param srcStateMgr
     * @param id
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    protected ItemState getItemState(ItemStateManager srcStateMgr, ItemId id)
            throws ItemNotFoundException, RepositoryException {
        try {
            return srcStateMgr.getItemState(id);
        } catch (NoSuchItemStateException nsise) {
            throw new ItemNotFoundException(safeGetJCRPath(id));
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to retrieve state of "
                    + safeGetJCRPath(id);
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    //------------------------------------------------------< private methods >

    /**
     * Recursively removes the given node state including its properties and
     * child nodes.
     * <p>
     * The removal of child nodes is subject to the following checks:
     * access rights, locking & versioning status. Referential integrity
     * (references) is checked on commit.
     * <p>
     * Note that the child node entry refering to <code>targetState</code> is
     * <b><i>not</i></b> automatically removed from <code>targetState</code>'s
     * parent.
     *
     * @param targetState
     * @throws RepositoryException if an error occurs
     */
    private void recursiveRemoveNodeState(NodeState targetState)
            throws RepositoryException {

        if (targetState.hasChildNodeEntries()) {
            // remove child nodes
            // use temp array to avoid ConcurrentModificationException
            ArrayList<ChildNodeEntry> tmp = new ArrayList<ChildNodeEntry>(targetState.getChildNodeEntries());
            // remove from tail to avoid problems with same-name siblings
            for (int i = tmp.size() - 1; i >= 0; i--) {
                ChildNodeEntry entry = tmp.get(i);
                NodeId nodeId = entry.getId();
                try {
                    NodeState nodeState = (NodeState) stateMgr.getItemState(nodeId);
                    // check if child node can be removed
                    // (access rights, locking & versioning status as well
                    //  as retention and hold);
                    // referential integrity (references) is checked
                    // on commit
                    checkRemoveNode(nodeState, targetState.getNodeId(),
                            CHECK_ACCESS
                            | CHECK_LOCK
                            | CHECK_CHECKED_OUT
                            | CHECK_HOLD
                            | CHECK_RETENTION
                    );
                    // remove child node
                    recursiveRemoveNodeState(nodeState);
                } catch (ItemStateException ise) {
                    String msg = "internal error: failed to retrieve state of "
                            + nodeId;
                    log.debug(msg);
                    throw new RepositoryException(msg, ise);
                }
                // remove child node entry
                targetState.removeChildNodeEntry(entry.getName(), entry.getIndex());
            }
        }

        // remove properties
        // use temp set to avoid ConcurrentModificationException
        HashSet<Name> tmp = new HashSet<Name>(targetState.getPropertyNames());
        for (Name propName : tmp) {
            PropertyId propId =
                    new PropertyId(targetState.getNodeId(), propName);
            try {
                PropertyState propState =
                        (PropertyState) stateMgr.getItemState(propId);
                // remove property entry
                targetState.removePropertyName(propId.getName());
                // destroy property state
                stateMgr.destroy(propState);
            } catch (ItemStateException ise) {
                String msg = "internal error: failed to retrieve state of "
                        + propId;
                log.debug(msg);
                throw new RepositoryException(msg, ise);
            }
        }

        // now actually do unlink target state
        targetState.setParentId(null);
        // destroy target state (pass overlayed state since target state
        // might have been modified during unlinking)
        stateMgr.destroy(targetState.getOverlayedState());
    }

    /**
     * Recursively copies the specified node state including its properties and
     * child nodes.
     *
     * @param srcState
     * @param srcPath
     * @param srcStateMgr
     * @param srcAccessMgr
     * @param destParentId
     * @param flag           one of
     *                       <ul>
     *                       <li><code>COPY</code></li>
     *                       <li><code>CLONE</code></li>
     *                       <li><code>CLONE_REMOVE_EXISTING</code></li>
     *                       </ul>
     * @param refTracker     tracks uuid mappings and processed reference properties
     * @return a deep copy of the given node state and its children
     * @throws RepositoryException if an error occurs
     */
    private NodeState copyNodeState(NodeState srcState,
                                    Path srcPath,
                                    ItemStateManager srcStateMgr,
                                    AccessManager srcAccessMgr,
                                    NodeId destParentId,
                                    int flag,
                                    ReferenceChangeTracker refTracker)
            throws RepositoryException {

        NodeState newState;
        try {
            NodeId id = null;
            EffectiveNodeType ent = getEffectiveNodeType(srcState);
            boolean referenceable = ent.includesNodeType(NameConstants.MIX_REFERENCEABLE);
            boolean versionable = ent.includesNodeType(NameConstants.MIX_SIMPLE_VERSIONABLE);
            boolean fullVersionable = ent.includesNodeType(NameConstants.MIX_VERSIONABLE);
            boolean shareable = ent.includesNodeType(NameConstants.MIX_SHAREABLE);
            switch (flag) {
                case COPY:
                    /* if this node is shareable and another node in the same shared set
                     * has been already been copied and given a new uuid, use this one
                     * (see section 14.5 of the specification)
                     */
                    if (shareable && refTracker.getMappedId(srcState.getNodeId()) != null) {
                        NodeId newId = refTracker.getMappedId(srcState.getNodeId());
                        NodeState sharedState = (NodeState) stateMgr.getItemState(newId);
                        sharedState.addShare(destParentId);
                        return sharedState;
                    }
                    break;
                case CLONE:
                    if (!referenceable) {
                        // non-referenceable node: always create new node id
                        break;
                    }
                    // use same uuid as source node
                    id = srcState.getNodeId();

                    if (stateMgr.hasItemState(id)) {
                        if (shareable) {
                            NodeState sharedState = (NodeState) stateMgr.getItemState(id);
                            sharedState.addShare(destParentId);
                            return sharedState;
                        }
                        // node with this uuid already exists
                        throw new ItemExistsException(safeGetJCRPath(id));
                    }
                    break;
                case CLONE_REMOVE_EXISTING:
                    if (!referenceable) {
                        // non-referenceable node: always create new node id
                        break;
                    }
                    // use same uuid as source node
                    id = srcState.getNodeId();
                    if (stateMgr.hasItemState(id)) {
                        NodeState existingState = (NodeState) stateMgr.getItemState(id);
                        // make sure existing node is not the parent
                        // or an ancestor thereof
                        if (id.equals(destParentId)
                                || hierMgr.isAncestor(id, destParentId)) {
                            String msg =
                                "cannot remove node " + safeGetJCRPath(srcPath)
                                + " because it is an ancestor of the destination";
                            log.debug(msg);
                            throw new RepositoryException(msg);
                        }

                        // check if existing can be removed
                        // (access rights, locking & versioning status,
                        // node type constraints and retention/hold)
                        checkRemoveNode(existingState,
                                CHECK_ACCESS
                                | CHECK_LOCK
                                | CHECK_CHECKED_OUT
                                | CHECK_CONSTRAINTS
                                | CHECK_HOLD
                                | CHECK_RETENTION);
                        // do remove existing
                        removeNodeState(existingState);
                    }
                    break;
                default:
                    throw new IllegalArgumentException(
                            "unknown flag for copying node state: " + flag);
            }
            newState = stateMgr.createNew(id, srcState.getNodeTypeName(), destParentId);
            id = newState.getNodeId();
            if (flag == COPY && referenceable) {
                // remember uuid mapping
                refTracker.mappedId(srcState.getNodeId(), id);
            }
            // copy node state
            newState.setMixinTypeNames(srcState.getMixinTypeNames());
            if (shareable) {
                // initialize shared set
                newState.addShare(destParentId);
            }
            // copy child nodes
            for (ChildNodeEntry entry : srcState.getChildNodeEntries()) {
                Path srcChildPath = PathFactoryImpl.getInstance().create(srcPath, entry.getName(), true);
                if (!srcAccessMgr.isGranted(srcChildPath, Permission.READ)) {
                    continue;
                }
                NodeId nodeId = entry.getId();
                NodeState srcChildState = (NodeState) srcStateMgr.getItemState(nodeId);

                /**
                 * special handling required for child nodes with special semantics
                 * (e.g. those defined by nt:version,  et.al.)
                 *
                 * todo FIXME delegate to 'node type instance handler'
                 */

                /**
                 * If child is shareble and its UUID has already been remapped,
                 * then simply add a reference to the state with that remapped
                 * UUID instead of copying the whole subtree.
                 */
                if (srcChildState.isShareable()) {
                    NodeId mappedId = refTracker.getMappedId(srcChildState.getNodeId());
                    if (mappedId != null) {
                        if (stateMgr.hasItemState(mappedId)) {
                            NodeState destState = (NodeState) stateMgr.getItemState(mappedId);
                            if (!destState.isShareable()) {
                                String msg =
                                    "Remapped child (" + safeGetJCRPath(srcPath)
                                    + ") is not shareable.";
                                throw new ItemStateException(msg);
                            }
                            if (!destState.addShare(id)) {
                                String msg = "Unable to add share to node: " + id;
                                throw new ItemStateException(msg);
                            }
                            stateMgr.store(destState);
                            newState.addChildNodeEntry(entry.getName(), mappedId);
                            continue;
                        }
                    }
                }

                // recursive copying of child node
                NodeState newChildState = copyNodeState(srcChildState, srcChildPath,
                        srcStateMgr, srcAccessMgr, id, flag, refTracker);
                // store new child node
                stateMgr.store(newChildState);
                // add new child node entry to new node
                newState.addChildNodeEntry(entry.getName(), newChildState.getNodeId());
            }
            // init version history if needed
            VersionHistoryInfo history = null;
            if (versionable && flag == COPY) {
                NodeId copiedFrom = null;
                if (fullVersionable) {
                    // base version of copied versionable node is reference value of
                    // the histories jcr:copiedFrom property
                    PropertyId propId = new PropertyId(srcState.getNodeId(), NameConstants.JCR_BASEVERSION);
                    PropertyState prop = (PropertyState) srcStateMgr.getItemState(propId);
                    copiedFrom = prop.getValues()[0].getNodeId();
                }
                InternalVersionManager manager = session.getInternalVersionManager();
                history = manager.getVersionHistory(session, newState, copiedFrom);
            }
            // copy properties
            for (Name propName : srcState.getPropertyNames()) {
                Path propPath = PathFactoryImpl.getInstance().create(srcPath, propName, true);
                PropertyId propId = new PropertyId(srcState.getNodeId(), propName);
                if (!srcAccessMgr.canRead(propPath, propId)) {
                    continue;
                }
                PropertyState srcChildState =
                        (PropertyState) srcStateMgr.getItemState(propId);

                /**
                 * special handling required for properties with special semantics
                 * (e.g. those defined by mix:referenceable, mix:versionable,
                 * mix:lockable, et.al.)
                 *
                 * todo FIXME delegate to 'node type instance handler'
                 */
                QPropertyDefinition def = ent.getApplicablePropertyDef(
                        srcChildState.getName(), srcChildState.getType(),
                        srcChildState.isMultiValued());
                if (NameConstants.MIX_LOCKABLE.equals(def.getDeclaringNodeType())) {
                    // skip properties defined by mix:lockable
                    continue;
                }

                PropertyState newChildState =
                        copyPropertyState(srcChildState, id, propName, def);

                if (history != null) {
                    if (fullVersionable) {
                        if (propName.equals(NameConstants.JCR_VERSIONHISTORY)) {
                            // jcr:versionHistory
                            InternalValue value = InternalValue.create(
                                    history.getVersionHistoryId());
                            newChildState.setValues(new InternalValue[] { value });
                        } else if (propName.equals(NameConstants.JCR_BASEVERSION)
                                || propName.equals(NameConstants.JCR_PREDECESSORS)) {
                            // jcr:baseVersion or jcr:predecessors
                            InternalValue value = InternalValue.create(
                                    history.getRootVersionId());
                            newChildState.setValues(new InternalValue[] { value });
                        } else if (propName.equals(NameConstants.JCR_ISCHECKEDOUT)) {
                            // jcr:isCheckedOut
                            newChildState.setValues(new InternalValue[]{InternalValue.create(true)});
                        }
                    } else {
                        // for simple versionable, we just initialize the
                        // version history when we see the jcr:isCheckedOut
                        if (propName.equals(NameConstants.JCR_ISCHECKEDOUT)) {
                            // jcr:isCheckedOut
                            newChildState.setValues(new InternalValue[]{InternalValue.create(true)});
                        }
                    }
                }

                if (newChildState.getType() == PropertyType.REFERENCE
                        || newChildState.getType() == PropertyType.WEAKREFERENCE) {
                    refTracker.processedReference(newChildState);
                }
                // store new property
                stateMgr.store(newChildState);
                // add new property entry to new node
                newState.addPropertyName(propName);
            }
            return newState;
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to copy state of " + srcState.getNodeId();
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    /**
     * Copies the specified property state.
     *
     * @param srcState the property state to copy.
     * @param parentId the id of the parent node.
     * @param propName the name of the property.
     * @param def      the definition of the property.
     * @return a copy of the property state.
     * @throws RepositoryException if an error occurs while copying.
     */
    private PropertyState copyPropertyState(PropertyState srcState,
                                            NodeId parentId,
                                            Name propName,
                                            QPropertyDefinition def)
            throws RepositoryException {

        PropertyState newState = stateMgr.createNew(propName, parentId);

        newState.setType(srcState.getType());
        newState.setMultiValued(srcState.isMultiValued());
        InternalValue[] values = srcState.getValues();
        if (values != null) {
            /**
             * special handling required for properties with special semantics
             * (e.g. those defined by mix:referenceable, mix:versionable,
             * mix:lockable, et.al.)
             *
             * todo FIXME delegate to 'node type instance handler'
             */
            if (propName.equals(NameConstants.JCR_UUID)
                    && def.getDeclaringNodeType().equals(NameConstants.MIX_REFERENCEABLE)) {
                // set correct value of jcr:uuid property
                newState.setValues(new InternalValue[]{InternalValue.create(parentId.toString())});
            } else {
                InternalValue[] newValues = new InternalValue[values.length];
                for (int i = 0; i < values.length; i++) {
                    newValues[i] = values[i].createCopy();
                }
                newState.setValues(newValues);
            }
        }
        return newState;
    }

    /**
     * Check that the updatable item state manager is in edit mode.
     *
     * @throws IllegalStateException if it isn't
     */
    private void checkInEditMode() throws IllegalStateException {
        if (!stateMgr.inEditMode()) {
            throw new IllegalStateException("not in edit mode");
        }
    }

    /**
     * Determines whether the specified node is <i>shareable</i>, i.e.
     * whether the mixin type <code>mix:shareable</code> is either
     * directly assigned or indirectly inherited.
     *
     * @param state node state to check
     * @return true if the specified node is <i>shareable</i>, false otherwise.
     * @throws RepositoryException if an error occurs
     */
    private boolean isShareable(NodeState state) throws RepositoryException {
        // shortcut: check some wellknown built-in types first
        Name primary = state.getNodeTypeName();
        Set<Name> mixins = state.getMixinTypeNames();
        if (mixins.contains(NameConstants.MIX_SHAREABLE)) {
            return true;
        }

        try {
            NodeTypeRegistry registry = context.getNodeTypeRegistry();
            EffectiveNodeType type =
                registry.getEffectiveNodeType(primary, mixins);
            return type.includesNodeType(NameConstants.MIX_REFERENCEABLE);
        } catch (NodeTypeConflictException ntce) {
            String msg = "internal error: failed to build effective node type for node "
                    + state.getNodeId();
            log.debug(msg);
            throw new RepositoryException(msg, ntce);
        }
    }
}
