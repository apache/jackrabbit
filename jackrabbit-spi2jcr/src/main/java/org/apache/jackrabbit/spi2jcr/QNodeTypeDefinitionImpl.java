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

import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameException;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;

/**
 * <code>QNodeTypeDefinitionImpl</code> implements a qualified node type
 * definition based on a JCR {@link NodeType}.
 */
class QNodeTypeDefinitionImpl
        extends org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl {

    /**
     * Creates a new qualified node type definition based on a JCR
     * <code>NodeType</code>.
     *
     * @param nt            the JCR node type.
     * @param resolver
     * @param qValueFactory the QValue factory.
     *
     * @throws NameException   if <code>nt</code> contains an illegal
     *                                name.
     * @throws NamespaceException if <code>nt</code> contains a name with an
     *                                namespace prefix that is unknown to
     *                                <code>nsResolver</code>.
     * @throws RepositoryException    if an error occurs while reading from
     *                                <code>nt</code>.
     */
    public QNodeTypeDefinitionImpl(NodeType nt,
                                   NamePathResolver resolver,
                                   QValueFactory qValueFactory)
            throws NamespaceException, RepositoryException, NameException {
        super(resolver.getQName(nt.getName()),
                getNodeTypeNames(nt.getDeclaredSupertypes(), resolver),
                nt.isMixin(), nt.hasOrderableChildNodes(),
                nt.getPrimaryItemName() != null ? resolver.getQName(nt.getPrimaryItemName()) : null,
                getQPropertyDefinitions(nt.getDeclaredPropertyDefinitions(), resolver, qValueFactory),
                getQNodeDefinitions(nt.getDeclaredChildNodeDefinitions(), resolver));
    }

    /**
     * Returns the qualified names of the passed node types using the namespace
     * resolver to parse the names.
     *
     * @param nt         the node types
     * @param resolver
     * @return the qualified names of the node types.
     * @throws IllegalNameException   if a node type returns an illegal name.
     * @throws NamespaceException if the name of a node type contains a
     *                            prefix that is not known to <code>rResolver</code>.
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
     * Returns qualified property definitions for JCR property definitions.
     *
     * @param propDefs   the JCR property definitions.
     * @param resolver
     * @param factory    the value factory.
     * @return qualified property definitions.
     * @throws RepositoryException    if an error occurs while converting the
     *                                definitions.
     */
    private static QPropertyDefinition[] getQPropertyDefinitions(
            PropertyDefinition[] propDefs,
            NamePathResolver resolver,
            QValueFactory factory) throws RepositoryException, NameException {
        QPropertyDefinition[] propertyDefs = new QPropertyDefinition[propDefs.length];
        for (int i = 0; i < propDefs.length; i++) {
            propertyDefs[i] = new QPropertyDefinitionImpl(propDefs[i], resolver, factory);
        }
        return propertyDefs;
    }

    /**
     * Returns qualified node definitions for JCR node definitions.
     *
     * @param nodeDefs the JCR node definitions.
     * @param resolver the name and path resolver.
     * @return qualified node definitions.
     * @throws IllegalNameException   if the node definition contains an illegal
     *                                name.
     * @throws NamespaceException if the name of a node definition contains
     *                                a namespace prefix that is now known to
     *                                <code>nsResolver</code>.
     */
    private static QNodeDefinition[] getQNodeDefinitions (NodeDefinition[] nodeDefs,
                                                          NamePathResolver resolver)
            throws NameException, NamespaceException {
        QNodeDefinition[] childNodeDefs = new QNodeDefinition[nodeDefs.length];
        for (int i = 0; i < nodeDefs.length; i++) {
            childNodeDefs[i] = new QNodeDefinitionImpl(nodeDefs[i], resolver);
        }
        return childNodeDefs;
    }
}
