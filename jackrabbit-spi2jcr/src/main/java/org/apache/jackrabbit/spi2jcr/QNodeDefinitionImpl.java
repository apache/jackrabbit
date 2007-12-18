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

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.Name;

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.NamespaceException;

/**
 * <code>QNodeDefinitionImpl</code> implements a <code>QNodeDefinition</code>.
 */
class QNodeDefinitionImpl
        extends org.apache.jackrabbit.spi.commons.QNodeDefinitionImpl {

    /**
     * Creates a new qualified node definition based on a JCR NodeDefinition.
     *
     * @param nodeDef    the node definition.
     * @param resolver
     * @throws NameException   if <code>nodeDef</code> contains an
     *                                illegal name.
     * @throws NamespaceException if <code>nodeDef</code> contains a name
     *                                with an namespace prefix that is unknown
     *                                to <code>resolver</code>.
     */
    QNodeDefinitionImpl(NodeDefinition nodeDef,
                        NamePathResolver resolver)
            throws NameException, NamespaceException {
        super(nodeDef.getName().equals(ANY_NAME.getLocalName()) ? ANY_NAME : resolver.getQName(nodeDef.getName()),
                nodeDef.getDeclaringNodeType() != null ? resolver.getQName(nodeDef.getDeclaringNodeType().getName()) : null,
                nodeDef.isAutoCreated(), nodeDef.isMandatory(),
                nodeDef.getOnParentVersion(), nodeDef.isProtected(),
                nodeDef.getDefaultPrimaryType() != null ? resolver.getQName(nodeDef.getDefaultPrimaryType().getName()) : null,
                getNodeTypeNames(nodeDef.getRequiredPrimaryTypes(), resolver),
                nodeDef.allowsSameNameSiblings());
    }

    /**
     * Returns the qualified names of the passed node types using the namespace
     * resolver to parse the names.
     *
     * @param nt         the node types
     * @param resolver
     * @return the qualified names of the node types.
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
}
