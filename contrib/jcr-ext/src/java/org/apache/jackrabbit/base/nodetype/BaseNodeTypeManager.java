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
package org.apache.jackrabbit.base.nodetype;

import java.util.Vector;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.iterator.ArrayNodeTypeIterator;

/**
 * TODO
 */
public class BaseNodeTypeManager implements NodeTypeManager {

    /**
     * Static empty node type array instance. Used to avoid
     * instantiating the empty array more than once.
     */
    private static final NodeType[] EMPTY_NODETYPE_ARRAY = new NodeType[0];

    /**
     * Returns an empty iterator. Subclasses should override this method
     * to return all the available node types.
     *
     * @return all node types (empty array)
     * @throws RepositoryException on repository errors (not thrown)
     */
    public NodeTypeIterator getAllNodeTypes() throws RepositoryException {
        return new ArrayNodeTypeIterator(EMPTY_NODETYPE_ARRAY);
    }

    /**
     * Returns the named node type. Implemented by searching through
     * the iterator returned by <code>getAllNodeTypes()</code> looking
     * for a match for <code>nodeTypeName.equals(type.getName())</code>.
     * <p>
     * Note that names are compared using normal string comparison, which
     * means that both the given name and the internal node type names must
     * use the same namespace mappings.
     *
     * @param nodeTypeName node type name
     * @return named node type
     * @throws NoSuchNodeTypeException if the named node type does not exist
     * @throws RepositoryException on repository errors
     */
    public NodeType getNodeType(String nodeTypeName)
            throws NoSuchNodeTypeException, RepositoryException {
        NodeTypeIterator types = getAllNodeTypes();
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            if (nodeTypeName.equals(type.getName())) {
                return type;
            }
        }

        throw new NoSuchNodeTypeException(
                "Node type " + nodeTypeName + " not found");
    }

    /**
     * Returns all primary node types. Implemented by filtering the iterator
     * returned by <code>getAllNodeTypes()</code> with
     * <code>!type.isMixin()</code>.
     *
     * @return primary node types
     * @throws RepositoryException on repository errors
     */
    public NodeTypeIterator getPrimaryNodeTypes() throws RepositoryException {
        Vector primaryTypes = new Vector();

        NodeTypeIterator types = getAllNodeTypes();
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            if (!type.isMixin()) {
                primaryTypes.add(type);
            }
        }

        return new ArrayNodeTypeIterator(
                (NodeType[]) primaryTypes.toArray(EMPTY_NODETYPE_ARRAY));
    }

    /**
     * Returns all mixin node types. Implemented by filtering the iterator
     * returned by <code>getAllNodeTypes()</code> with
     * <code>type.isMixin()</code>.
     *
     * @return mixin node types
     * @throws RepositoryException on repository errors
     */
    public NodeTypeIterator getMixinNodeTypes() throws RepositoryException {
        Vector mixinTypes = new Vector();

        NodeTypeIterator types = getAllNodeTypes();
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            if (!type.isMixin()) {
                mixinTypes.add(type);
            }
        }

        return new ArrayNodeTypeIterator(
                (NodeType[]) mixinTypes.toArray(EMPTY_NODETYPE_ARRAY));
    }

}
