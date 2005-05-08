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
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.iterator.ArrayNodeTypeIterator;

/**
 * Node type manager base class.
 */
public class BaseNodeTypeManager implements NodeTypeManager {

    /** Protected constructor. This class is only useful when extended. */
    protected BaseNodeTypeManager() {
    }

    /** Not implemented. {@inheritDoc} */
    public NodeTypeIterator getAllNodeTypes() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Implemented by calling <code>getAllNodeTypes()</code> and iterating
     * through the returned node types to find the named node type.
     * {@inheritDoc}
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
        throw new NoSuchNodeTypeException("Type not found: " + nodeTypeName);
    }

    /**
     * Implemented by calling <code>getAllNodeTypes()</code> and iterating
     * through the returned node types to select all primary node types.
     * {@inheritDoc}
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

        return new ArrayNodeTypeIterator((NodeType[])
                primaryTypes.toArray(new NodeType[primaryTypes.size()]));
    }

    /**
     * Implemented by calling <code>getAllNodeTypes()</code> and iterating
     * through the returned node types to select all mixin node types.
     * {@inheritDoc}
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

        return new ArrayNodeTypeIterator((NodeType[])
                mixinTypes.toArray(new NodeType[mixinTypes.size()]));
    }

}
