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

import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.identifier.AbstractIdFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
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

    @Override
    protected PathFactory getPathFactory() {
        return PathFactoryImpl.getInstance();
    }
    /**
     * Creates a <code>NodeId</code> for the given <code>node</code>.
     *
     * @param node       the JCR Node.
     * @return the <code>NodeId</code> for <code>node</code>.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>node</code>.
     */
    public NodeId createNodeId(Node node) throws RepositoryException {
        String uniqueId = node.getIdentifier();
        return createNodeId(uniqueId);
    }

    /**
     * Creates a <code>PropertyId</code> for the given <code>property</code>.
     *
     * @param property   the JCR Property.
     * @param resolver
     * @return the <code>PropertyId</code> for <code>property</code>.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>property</code>.
     */
    public PropertyId createPropertyId(Property property,
                                       NamePathResolver resolver)
            throws RepositoryException {
        Node parent = property.getParent();
        NodeId nodeId = createNodeId(parent);
        String jcrName = property.getName();
        Name name;
        try {
            name = resolver.getQName(jcrName);
        } catch (NameException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
        return createPropertyId(nodeId, name);
    }
}
