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

import org.apache.jackrabbit.jcr2spi.util.LogUtil;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.spi.NodeId;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.lock.LockException;

/**
 * <code>Move</code>...
 */
public class Move extends AbstractOperation {

    private static Logger log = LoggerFactory.getLogger(Move.class);

    private final NodeId srcId;
    private final NodeId destParentId;
    private final QName destName;

    private final NodeState srcState;
    private final NodeState srcParentState;
    private final NodeState destParentState;

    private Move(NodeState srcNodeState, NodeState srcParentState, NodeState destParentState, QName destName) {
        this.srcId = (NodeId) srcNodeState.getId();
        this.destParentId = destParentState.getNodeId();
        this.destName = destName;

        this.srcState = srcNodeState;
        this.srcParentState = srcParentState;
        this.destParentState = destParentState;
        
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
        visitor.visit(this);
    }

    /**
     * Throws UnsupportedOperationException if this Move Operation is a transient
     * modification. Otherwise, the moved state as well as both parent states
     * are invalidated.
     *
     * @see Operation#persisted()
     */
    public void persisted() {
        // non-recursive invalidation
        try {
            srcState.getNodeEntry().move(destName, destParentState.getNodeEntry(), false);
            // TODO: TOBEFIXED. moved state ev. got a new definition.
        } catch (RepositoryException e) {
            // should not occure
            log.error("Internal error", e);
            srcParentState.getHierarchyEntry().invalidate(false);
            destParentState.getHierarchyEntry().invalidate(false);
            srcState.getHierarchyEntry().invalidate(false);
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

    public QName getDestinationName() {
        return destName;
    }

    //------------------------------------------------------------< Factory >---
    public static Operation create(Path srcPath, Path destPath,
                                   HierarchyManager hierMgr,
                                   NamespaceResolver nsResolver,
                                   boolean sessionMove)
        throws ItemExistsException, NoSuchNodeTypeException, RepositoryException {
        // src must not be ancestor of destination
        try {
            if (srcPath.isAncestorOf(destPath)) {
                String msg = "Invalid destination path: cannot be descendant of source path (" + LogUtil.safeGetJCRPath(destPath, nsResolver) + "," + LogUtil.safeGetJCRPath(srcPath, nsResolver) + ")";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
        } catch (MalformedPathException e) {
            String msg = "Invalid destination path: cannot be descendant of source path (" +LogUtil.safeGetJCRPath(destPath, nsResolver) + "," + LogUtil.safeGetJCRPath(srcPath, nsResolver) + ")";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
        Path.PathElement destElement = destPath.getNameElement();
        // destination must not contain an index
        int index = destElement.getIndex();
        if (index > Path.INDEX_UNDEFINED) {
            // subscript in name element
            String msg = "Invalid destination path: subscript in name element is not allowed (" + LogUtil.safeGetJCRPath(destPath, nsResolver) + ")";
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        // root node cannot be moved:
        if (Path.ROOT.equals(srcPath) || Path.ROOT.equals(destPath)) {
            String msg = "Cannot move the root node.";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        NodeState srcState = getNodeState(srcPath, hierMgr, nsResolver);
        NodeState srcParentState = getNodeState(srcPath.getAncestor(1), hierMgr, nsResolver);
        NodeState destParentState = getNodeState(destPath.getAncestor(1), hierMgr, nsResolver);
        QName destName = destElement.getName();

        // for session-move perform a lazy check for existing items at destination.
        // since the hierarchy may not be complete it is possible that an conflict
        // is only detected upon saving the 'move'.
        NodeEntry destEntry = (NodeEntry) destParentState.getHierarchyEntry();
        if (sessionMove) {
            if (destEntry.hasPropertyEntry(destName)) {
                throw new ItemExistsException("Move destination already exists (Property).");
            }
            if (destEntry.hasNodeEntry(destName)) {
                NodeEntry existing = destEntry.getNodeEntry(destName, Path.INDEX_DEFAULT);
                if (existing != null) {
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

        Move move = new Move(srcState, srcParentState, destParentState, destName);
        return move;
    }
}