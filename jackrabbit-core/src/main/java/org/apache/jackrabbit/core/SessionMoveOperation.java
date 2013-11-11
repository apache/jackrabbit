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

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.session.SessionWriteOperation;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionMoveOperation implements SessionWriteOperation<Object> {

    private final Logger log =
        LoggerFactory.getLogger(SessionMoveOperation.class);

    private final String srcAbsPath;

    private final Path srcPath;

    private final String destAbsPath;

    private final Path destPath;

    public SessionMoveOperation(
            PathResolver resolver, String srcAbsPath, String destAbsPath)
            throws RepositoryException {
        this.srcAbsPath = srcAbsPath;
        this.srcPath = getAbsolutePath(resolver, srcAbsPath);

        this.destAbsPath = destAbsPath;
        this.destPath = getAbsolutePath(resolver, destAbsPath);
        if (destPath.getIndex() != Path.INDEX_UNDEFINED) {
            // subscript in name element
            String msg = destAbsPath + ": invalid destination path (subscript in name element is not allowed)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }


        if (srcPath.isAncestorOf(destPath)) {
            throw new RepositoryException(
                    "Destination path " + destAbsPath
                    + " cannot be descendant of source path " + srcAbsPath
                    + " in a move operation.");
        }
    }

    private Path getAbsolutePath(PathResolver resolver, String path)
            throws RepositoryException {
        try {
            Path qpath = resolver.getQPath(path).getNormalizedPath();
            if (!qpath.isAbsolute()) {
                throw new RepositoryException("Path is not absolute: " + path);
            }
            return qpath;
        } catch (NameException e) {
            throw new RepositoryException("Path is invalid: " + path, e);
        }
    }

    private NodeImpl getNode(
            SessionContext context, Path path, String absPath)
            throws RepositoryException {
        try {
            return context.getItemManager().getNode(path);
        } catch (AccessDeniedException e) {
            throw new PathNotFoundException("Path not found: " + absPath);
        }
    }

    public Object perform(SessionContext context) throws RepositoryException {
        // Get node instances
        NodeImpl targetNode = getNode(context, srcPath, srcAbsPath);
        NodeImpl srcParentNode =
            getNode(context, srcPath.getAncestor(1), srcAbsPath);
        NodeImpl destParentNode =
            getNode(context, destPath.getAncestor(1), destAbsPath);

        if (context.getHierarchyManager().isShareAncestor(
                targetNode.getNodeId(), destParentNode.getNodeId())) {
            throw new RepositoryException(
                    "Move not possible because of a share cycle between "
                    + srcAbsPath + " and " + destAbsPath);
        }

        // check for name collisions
        NodeImpl existing = null;
        try {
            existing = context.getItemManager().getNode(destPath);
            // there's already a node with that name:
            // check same-name sibling setting of existing node
            if (!existing.getDefinition().allowsSameNameSiblings()) {
                throw new ItemExistsException(
                        "Same name siblings are not allowed: " + existing);
            }
        } catch (AccessDeniedException ade) {
            // FIXME by throwing ItemExistsException we're disclosing too much information
            throw new ItemExistsException(destAbsPath);
        } catch (PathNotFoundException pnfe) {
            // no name collision, fall through
        }

        // verify that the targetNode can be removed
        int options = ItemValidator.CHECK_HOLD | ItemValidator.CHECK_RETENTION;
        context.getItemValidator().checkRemove(targetNode, options, Permission.NONE);

        // verify for both source and destination parent nodes that
        // - they are checked-out
        // - are not protected neither by node type constraints nor by retention/hold
        options = ItemValidator.CHECK_CHECKED_OUT | ItemValidator.CHECK_LOCK |
        ItemValidator.CHECK_CONSTRAINTS | ItemValidator.CHECK_HOLD | ItemValidator.CHECK_RETENTION;
        context.getItemValidator().checkModify(srcParentNode, options, Permission.NONE);
        context.getItemValidator().checkModify(destParentNode, options, Permission.NONE);

        // check constraints
        // get applicable definition of target node at new location
        NodeTypeImpl nt = (NodeTypeImpl) targetNode.getPrimaryNodeType();
        org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl newTargetDef;
        try {
            newTargetDef = destParentNode.getApplicableChildNodeDefinition(destPath.getName(), nt.getQName());
        } catch (RepositoryException re) {
            String msg = destAbsPath + ": no definition found in parent node's node type for new node";
            log.debug(msg);
            throw new ConstraintViolationException(msg, re);
        }
        // if there's already a node with that name also check same-name sibling
        // setting of new node; just checking same-name sibling setting on
        // existing node is not sufficient since same-name sibling nodes don't
        // necessarily have identical definitions
        if (existing != null && !newTargetDef.allowsSameNameSiblings()) {
            throw new ItemExistsException(
                    "Same name siblings not allowed: " + existing);
        }

        NodeId targetId = targetNode.getNodeId();

        // check permissions
        AccessManager acMgr = context.getAccessManager();
        if (!(acMgr.isGranted(srcPath, Permission.REMOVE_NODE) &&
                acMgr.isGranted(destPath, Permission.ADD_NODE | Permission.NODE_TYPE_MNGMT))) {
            String msg = "Not allowed to move node " + srcAbsPath + " to " + destAbsPath;
            log.debug(msg);
            throw new AccessDeniedException(msg);
        }

        if (srcParentNode.isSame(destParentNode)) {
            // change definition of target
            targetNode.onRedefine(newTargetDef.unwrap());
            // do rename
            destParentNode.renameChildNode(targetId, destPath.getName(), false);
        } else {
            // check shareable case
            if (targetNode.getNodeState().isShareable()) {
                String msg = "Moving a shareable node is not supported.";
                log.debug(msg);
                throw new UnsupportedRepositoryOperationException(msg);
            }
            // change definition of target
            targetNode.onRedefine(newTargetDef.unwrap());

            // Get the transient states
            NodeState srcParentState =
                (NodeState) srcParentNode.getOrCreateTransientItemState();
            NodeState targetState =
                (NodeState) targetNode.getOrCreateTransientItemState();
            NodeState destParentState =
                (NodeState) destParentNode.getOrCreateTransientItemState();

            // do move:
            // 1. remove child node entry from old parent
            if (srcParentState.removeChildNodeEntry(targetId)) {
                // 2. re-parent target node
                targetState.setParentId(destParentNode.getNodeId());
                // 3. add child node entry to new parent
                destParentState.addChildNodeEntry(destPath.getName(), targetId);
            }
        }

        return this;
    }


    //--------------------------------------------------------------< Object >

    /**
     * Returns a string representation of this operation.
     */
    public String toString() {
        return "session.move(" + srcAbsPath + ", " + destAbsPath + ")";
    }

}
