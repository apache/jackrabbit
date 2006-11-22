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
import org.apache.jackrabbit.jcr2spi.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.entry.ChildNodeEntry;
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
    private final NodeState srcState;
    private final NodeState srcParentState;
    private final NodeState destParentState;
    private final QName destName;

    private Move(NodeState srcNodeState, NodeState srcParentState, NodeState destParentState, QName destName) {
        this.srcId = (NodeId) srcNodeState.getId();
        this.srcState = srcNodeState;
        this.srcParentState = srcParentState;
        this.destParentState = destParentState;
        this.destName = destName;
        
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
        if (srcState.isWorkspaceState()) {
            srcParentState.invalidate(false);
            destParentState.invalidate(false);
            srcState.invalidate(false);
        } else {
            throw new UnsupportedOperationException("persisted() not implemented for transient modification.");
        }
    }
    //----------------------------------------< Access Operation Parameters >---
    public NodeId getSourceId() {
        return srcId;
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
                                   HierarchyManager hierMgr, NamespaceResolver nsResolver)
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

        if (destParentState.hasPropertyName(destName)) {
            throw new ItemExistsException("Move destination already exists (Property).");
        } else if (destParentState.hasChildNodeEntry(destName)) {
            ChildNodeEntry existing = destParentState.getChildNodeEntry(destName, Path.INDEX_DEFAULT);
            try {
                if (!existing.getNodeState().getDefinition().allowsSameNameSiblings()) {
                    throw new ItemExistsException("Node existing at move destination does not allow same name siblings.");
                }
            } catch (ItemStateException e) {
                throw new RepositoryException(e);
            }
        }

        Move move = new Move(srcState, srcParentState, destParentState, destName);
        return move;
    }
}