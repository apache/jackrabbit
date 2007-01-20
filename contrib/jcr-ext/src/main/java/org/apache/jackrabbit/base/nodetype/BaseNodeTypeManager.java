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
package org.apache.jackrabbit.base.nodetype;

import java.util.LinkedList;
import java.util.List;
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

    /**
     * Returns an empty node type iterator. Subclasses should override this
     * method to return the available node types.
     *
     * @return empty node type iterator
     * @see NodeTypeManager#getAllNodeTypes()
     */
    public NodeTypeIterator getAllNodeTypes() throws RepositoryException {
        return new ArrayNodeTypeIterator(new NodeType[0]);
    }

    /**
     * Iterates through the node types returned by the
     * {@link #getAllNodeTypes() getAllNodeTypes()} method and returns the
     * node type with the given name. If a matching node type is not found,
     * then a {@link NoSuchNodeTypeException} is thrown. Subclasses may
     * want to override this method for better performance.
     *
     * @param node type name
     * @return named node type
     * @see NodeTypeManager#getNodeType(String)
     */
    public NodeType getNodeType(String nodeTypeName) throws RepositoryException {
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
     * Iterates through the node types returned by the
     * {@link #getAllNodeTypes() getAllNodeTypes()} method and returns an
     * {@link ArrayNodeTypeIterator} containing all the primary node types.
     * Subclasses may want to override this method for better performance.
     *
     * @return primary node types
     * @see NodeTypeManager#getPrimaryNodeTypes()
     */
    public NodeTypeIterator getPrimaryNodeTypes() throws RepositoryException {
        List primaryTypes = new LinkedList();

        NodeTypeIterator types = getAllNodeTypes();
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            if (!type.isMixin()) {
                primaryTypes.add(type);
            }
        }

        return new ArrayNodeTypeIterator(primaryTypes);
    }

    /**
     * Iterates through the node types returned by the
     * {@link #getAllNodeTypes() getAllNodeTypes()} method and returns an
     * {@link ArrayNodeTypeIterator} containing all the mixin node types.
     * Subclasses may want to override this method for better performance.
     *
     * @return mixin node types
     * @see NodeTypeManager#getMixinNodeTypes()
     */
    public NodeTypeIterator getMixinNodeTypes() throws RepositoryException {
        List mixinTypes = new LinkedList();

        NodeTypeIterator types = getAllNodeTypes();
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            if (type.isMixin()) {
                mixinTypes.add(type);
            }
        }

        return new ArrayNodeTypeIterator(mixinTypes);
    }

}
