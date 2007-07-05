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

import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.IllegalNameException;
import org.apache.jackrabbit.name.UnknownPrefixException;
import org.apache.jackrabbit.name.PathFormat;
import org.apache.jackrabbit.name.MalformedPathException;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.nodetype.NodeType;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

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
     * @param nsResolver the namespace resolver in use.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>node</code>.
     */
    public NodeInfoImpl(Node node,
                        IdFactoryImpl idFactory,
                        NamespaceResolver nsResolver)
            throws RepositoryException, IllegalNameException, UnknownPrefixException, MalformedPathException {
        super(node.getName().length() == 0 ? null : idFactory.createNodeId(node.getParent(), nsResolver),
                node.getName().length() == 0 ? QName.ROOT : NameFormat.parse(node.getName(), nsResolver),
                PathFormat.parse(node.getPath(), nsResolver),
                idFactory.createNodeId(node, nsResolver), node.getIndex(),
                NameFormat.parse(node.getPrimaryNodeType().getName(), nsResolver),
                getNodeTypeNames(node.getMixinNodeTypes(), nsResolver),
                getPropertyIds(node.getReferences(), nsResolver, idFactory),
                getPropertyIds(node.getProperties(), nsResolver, idFactory));
    }

    /**
     * Returns the qualified names of the passed node types using the namespace
     * resolver to parse the names.
     *
     * @param nt         the node types
     * @param nsResolver the namespace resolver.
     * @return the qualified names of the node types.
     * @throws IllegalNameException   if a node type returns an illegal name.
     * @throws UnknownPrefixException if the nameo of a node type contains a
     *                                prefix that is not known to <code>nsResolver</code>.
     */
    private static QName[] getNodeTypeNames(NodeType[] nt,
                                     NamespaceResolver nsResolver)
            throws IllegalNameException, UnknownPrefixException {
        QName[] names = new QName[nt.length];
        for (int i = 0; i < nt.length; i++) {
            QName ntName = NameFormat.parse(nt[i].getName(), nsResolver);
            names[i] = ntName;
        }
        return names;
    }

    /**
     * Returns property ids for the passed JCR properties.
     *
     * @param props      the JCR properties.
     * @param nsResolver the namespace resolver.
     * @param idFactory  the id factory.
     * @return the property ids for the passed JCR properties.
     * @throws RepositoryException if an error occurs while reading from the
     *                             properties.
     */
    private static Iterator getPropertyIds(PropertyIterator props,
                                              NamespaceResolver nsResolver,
                                              IdFactoryImpl idFactory)
            throws RepositoryException {
        List references = new ArrayList();
        while (props.hasNext()) {
            references.add(idFactory.createPropertyId(props.nextProperty(), nsResolver));
        }
        return references.iterator();
    }
}
