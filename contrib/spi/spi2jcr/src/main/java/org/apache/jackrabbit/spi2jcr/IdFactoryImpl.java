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
package org.apache.jackrabbit.spi2jcr;

import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.identifier.AbstractIdFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Property;

/**
 * <code>IdFactoryImpl</code>...
 */
class IdFactoryImpl extends AbstractIdFactory {

    private static final IdFactory INSTANCE = new IdFactoryImpl();

    private IdFactoryImpl() {}

    public static IdFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a <code>NodeId</code> for the given <code>node</code>.
     *
     * @param node       the JCR Node.
     * @param nsResolver the namespace resolver in use.
     * @return the <code>NodeId</code> for <code>node</code>.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>node</code>.
     */
    public NodeId createNodeId(Node node, NamespaceResolver nsResolver)
            throws RepositoryException {
        Path.PathBuilder builder = new Path.PathBuilder();
        int pathElements = 0;
        String uniqueId = null;
        while (uniqueId == null) {
            try {
                uniqueId = node.getUUID();
            } catch (UnsupportedRepositoryOperationException e) {
                // not referenceable
                pathElements++;
                String jcrName = node.getName();
                if (jcrName.equals("")) {
                    // root node
                    builder.addFirst(QName.ROOT);
                    break;
                } else {
                    QName name;
                    try {
                        name = NameFormat.parse(node.getName(), nsResolver);
                    } catch (NameException ex) {
                       throw new RepositoryException(ex.getMessage(), ex);
                    }
                    if (node.getIndex() == 1) {
                        builder.addFirst(name);
                    } else {
                        builder.addFirst(name, node.getIndex());
                    }
                }
                node = node.getParent();
            }
        }
        if (pathElements > 0) {
            try {
                return createNodeId(uniqueId, builder.getPath());
            } catch (MalformedPathException e) {
                throw new RepositoryException(e.getMessage(), e);
            }
        } else {
            return createNodeId(uniqueId);
        }
    }

    /**
     * Creates a <code>PropertyId</code> for the given <code>property</code>.
     *
     * @param property   the JCR Property.
     * @param nsResolver the namespace resolver in use.
     * @return the <code>PropertyId</code> for <code>property</code>.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>property</code>.
     */
    public PropertyId createPropertyId(Property property,
                                       NamespaceResolver nsResolver)
            throws RepositoryException {
        Node parent = property.getParent();
        NodeId nodeId = createNodeId(parent, nsResolver);
        String jcrName = property.getName();
        QName name;
        try {
            name = NameFormat.parse(jcrName, nsResolver);
        } catch (NameException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
        return createPropertyId(nodeId, name);
    }
}