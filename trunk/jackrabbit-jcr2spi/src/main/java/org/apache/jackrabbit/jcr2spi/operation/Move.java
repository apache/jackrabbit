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
package org.apache.jackrabbit.jcr2spi.operation;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.util.LogUtil;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>Move</code>...
 */
public class Move extends TransientOperation {

    private static Logger log = LoggerFactory.getLogger(Move.class);

    private static final int MOVE_OPTIONS = ItemStateValidator.CHECK_ACCESS
            | ItemStateValidator.CHECK_LOCK
            | ItemStateValidator.CHECK_VERSIONING
            | ItemStateValidator.CHECK_CONSTRAINTS;

    private final NodeId srcId;
    private final NodeId destParentId;
    private final Name destName;

    private final NodeState srcState;
    private final NodeState srcParentState;
    private final NodeState destParentState;

    private final boolean sessionMove;

    private Move(NodeState srcNodeState, NodeState srcParentState, NodeState destParentState, Name destName, boolean sessionMove)
            throws RepositoryException {
        super(sessionMove ? MOVE_OPTIONS : ItemStateValidator.CHECK_NONE);

        this.srcId = (NodeId) srcNodeState.getId();
        this.destParentId = destParentState.getNodeId();
        this.destName = destName;

        this.srcState = srcNodeState;
        this.srcParentState = srcParentState;
        this.destParentState = destParentState;

        this.sessionMove = sessionMove;

        addAffectedItemState(srcNodeState);
        addAffectedItemState(srcParentState);
        addAffectedItemState(destParentState);
    }

    //----------------------------------------------------------< Operation >---
    /**
     *
     * @param visitor
     */
    public void accept(OperationVisitor visitor) throws LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        assert status == STATUS_PENDING;
        visitor.visit(this);
    }

    /**
     * Throws UnsupportedOperationException if this Move Operation is a transient
     * modification. Otherwise, the moved state as well as both parent states
     * are invalidated.
     *
     * @see Operation#persisted()
     */
    public void persisted() throws RepositoryException {
        assert status == STATUS_PENDING;
        status = STATUS_PERSISTED;
        if (sessionMove) {
            srcState.getNodeEntry().complete(this);
        } else {
            // non-recursive invalidation
            try {
                srcState.getNodeEntry().move(destName, destParentState.getNodeEntry(), false);
                // TODO: TOBEFIXED. moved state ev. got a new definition.
            } catch (RepositoryException e) {
                // should not occur
                log.error("Internal error", e);
                srcParentState.getHierarchyEntry().invalidate(false);
                destParentState.getHierarchyEntry().invalidate(false);
                srcState.getHierarchyEntry().invalidate(false);
            }
        }
    }

    /**
     * @see Operation#undo()
     */
    @Override
    public void undo() throws RepositoryException {
        assert status == STATUS_PENDING;
        if (sessionMove) {
            status = STATUS_UNDO;
            srcState.getHierarchyEntry().complete(this);
        } else {
            super.undo();
        }
    }

    //----------------------------------------< Access Operation Parameters >---
    public NodeId getSourceId() {
        return srcId;
    }

    public NodeId getDestinationParentId() {
        return destParentId;
    }

    public NodeState getSourceState() {
        return srcState;
    }

    public NodeState getSourceParentState() {
        return srcParentState;
    }

    public NodeState getDestinationParentState() {
        return destParentState;
    }

    public Name getDestinationName() {
        return destName;
    }

    //------------------------------------------------------------< Factory >---
    public static Operation create(Path srcPath, Path destPath,
                                   HierarchyManager hierMgr,
                                                    PathResolver resolver,
                                                    boolean sessionMove)
        throws ItemExistsException, NoSuchNodeTypeException, RepositoryException {
        // src must not be ancestor of destination
        if (srcPath.isAncestorOf(destPath)) {
            String msg = "Invalid destination path: cannot be descendant of source path (" + LogUtil.safeGetJCRPath(destPath, resolver) + "," + LogUtil.safeGetJCRPath(srcPath, resolver) + ")";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // destination must not contain an index
        int index = destPath.getIndex();
        if (index != Path.INDEX_UNDEFINED) {
            // subscript in name element
            String msg = "Invalid destination path: subscript in name element is not allowed (" + LogUtil.safeGetJCRPath(destPath, resolver) + ")";
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        // root node cannot be moved:
        if (srcPath.denotesRoot() || destPath.denotesRoot()) {
            String msg = "Cannot move the root node.";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        NodeState srcState = getNodeState(srcPath, hierMgr);
        NodeState srcParentState = getNodeState(srcPath.getAncestor(1), hierMgr);
        NodeState destParentState = getNodeState(destPath.getAncestor(1), hierMgr);
        Name destName = destPath.getName();

        if (sessionMove) {
            NodeEntry destEntry = (NodeEntry) destParentState.getHierarchyEntry();

            // force child node entries list to be present before the move is executed
            // on the hierarchy entry.
            assertChildNodeEntries(srcParentState);
            assertChildNodeEntries(destParentState);

            if (destEntry.hasNodeEntry(destName)) {
                NodeEntry existing = destEntry.getNodeEntry(destName, Path.INDEX_DEFAULT);
                if (existing != null && sessionMove) {
                    try {
                        if (!existing.getNodeState().getDefinition().allowsSameNameSiblings()) {
                            throw new ItemExistsException("Node existing at move destination does not allow same name siblings.");
                        }
                    } catch (ItemNotFoundException e) {
                        // existing apparent not valid any more -> probably no conflict
                    }
                }
            }
        }

        Move move = new Move(srcState, srcParentState, destParentState, destName, sessionMove);
        return move;
    }
}