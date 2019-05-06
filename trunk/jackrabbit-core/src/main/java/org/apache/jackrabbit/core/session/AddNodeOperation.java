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
package org.apache.jackrabbit.core.session;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;

/**
 * Session operation for adding a new node.
 */
public class AddNodeOperation implements SessionWriteOperation<Node> {

    private final NodeImpl node;

    private final String relPath;

    private final String nodeTypeName;

    private final String uuid;

    public AddNodeOperation(
            NodeImpl node, String relPath,
            String nodeTypeName, String uuid) {
        this.node = node;
        this.relPath = relPath;
        this.nodeTypeName = nodeTypeName;
        this.uuid = uuid;
    }

    public Node perform(SessionContext context) throws RepositoryException {
        ItemManager itemMgr = context.getItemManager();

        // Get the canonical path of the new node
        Path path;
        try {
            path = PathFactoryImpl.getInstance().create(
                    node.getPrimaryPath(), context.getQPath(relPath), true);
        } catch (NameException e) {
            throw new RepositoryException(
                    "Failed to resolve path " + relPath
                    + " relative to " + node, e);
        }

        // Check that the last path element is a simple name
        if (!path.denotesName() || path.getIndex() != Path.INDEX_UNDEFINED) {
            throw new RepositoryException(
                    "Invalid last path element for adding node "
                    + relPath + " relative to " + node);
        }

        // Get the parent node instance
        NodeImpl parentNode;
        Path parentPath = path.getAncestor(1);
        try {
            parentNode = itemMgr.getNode(parentPath);
        } catch (PathNotFoundException e) {
            if (itemMgr.propertyExists(parentPath)) {
                throw new ConstraintViolationException(
                        "Unable to add a child node to property "
                        + context.getJCRPath(parentPath));
            }
            throw e;
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(
                    "Failed to resolve path " + relPath
                    + " relative to " + node);
        }

        // Resolve node type name (if any)
        Name typeName = null;
        if (nodeTypeName != null) {
            typeName = context.getQName(nodeTypeName);
        }

        // Check that the given UUID (if any) does not already exist
        NodeId id = null;
        if (uuid != null) {
            id = new NodeId(uuid);
            if (itemMgr.itemExists(id)) {
                throw new ItemExistsException(
                        "A node with this UUID already exists: " + uuid);
            }
        }

        return parentNode.addNode(path.getName(), typeName, id);
    }


    //--------------------------------------------------------------< Object >

    /**
     * Returns a string representation of this operation.
     */
    public String toString() {
        return "node.addNode(" + relPath + ", " + nodeTypeName + ", " + uuid  + ")";
    }

}