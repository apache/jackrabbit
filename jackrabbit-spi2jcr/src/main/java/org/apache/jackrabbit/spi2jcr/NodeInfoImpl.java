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

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <code>NodeInfoImpl</code> implements a <code>NodeInfo</code> on top of a JCR
 * repository.
 */
class NodeInfoImpl extends org.apache.jackrabbit.spi.commons.NodeInfoImpl {

    /**
     * Creates a new node info for the given <code>node</code>.
     *
     * @param node       the JCR node.
     * @param idFactory  the id factory.
     * @param resolver
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>node</code>.
     */
    public NodeInfoImpl(Node node,
                        IdFactoryImpl idFactory,
                        NamePathResolver resolver)
            throws RepositoryException, NameException {
        super(resolver.getQPath(node.getPath()),
                idFactory.createNodeId(node), node.getIndex(),
                resolver.getQName(node.getPrimaryNodeType().getName()),
                getNodeTypeNames(node.getMixinNodeTypes(), resolver),
                getPropertyIds(node.getReferences(), resolver, idFactory),
                getPropertyIds(node.getProperties(), resolver, idFactory),
                getChildInfos(node.getNodes(), resolver));
    }

    /**
     * Returns the names of the passed node types using the namespace
     * resolver to parse the names.
     *
     * @param nt the node types from which the names should be retrieved.
     * @param resolver The name and path resolver.
     * @return the names of the node types.
     * @throws NameException   if a node type returns an illegal name.
     * @throws NamespaceException if the name of a node type contains a
     *                            prefix that is not known to <code>resolver</code>.
     */
    private static Name[] getNodeTypeNames(NodeType[] nt,
                                           NamePathResolver resolver)
            throws NameException, NamespaceException {
        Name[] names = new Name[nt.length];
        for (int i = 0; i < nt.length; i++) {
            Name ntName = resolver.getQName(nt[i].getName());
            names[i] = ntName;
        }
        return names;
    }

    /**
     * Returns property ids for the passed JCR properties.
     *
     * @param props      the JCR properties.
     * @param resolver
     * @param idFactory  the id factory.
     * @return the property ids for the passed JCR properties.
     * @throws RepositoryException if an error occurs while reading from the
     *                             properties.
     */
    private static Iterator<PropertyId> getPropertyIds(PropertyIterator props,
                                                       NamePathResolver resolver,
                                                       IdFactoryImpl idFactory)
            throws RepositoryException {
        List<PropertyId> references = new ArrayList<PropertyId>();
        while (props.hasNext()) {
            references.add(idFactory.createPropertyId(props.nextProperty(), resolver));
        }
        return references.iterator();
    }

    private static Iterator<ChildInfo> getChildInfos(NodeIterator childNodes,
                                                     NamePathResolver resolver) throws RepositoryException {
        List<ChildInfo> childInfos = new ArrayList<ChildInfo>();
        while (childNodes.hasNext()) {
            childInfos.add(new ChildInfoImpl(childNodes.nextNode(), resolver));
        }
        return childInfos.iterator();
    }
}
