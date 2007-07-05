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

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.IllegalNameException;
import org.apache.jackrabbit.name.UnknownPrefixException;

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

/**
 * <code>QNodeDefinitionImpl</code> implements a <code>QNodeDefinition</code>.
 */
class QNodeDefinitionImpl
        extends org.apache.jackrabbit.spi.commons.QNodeDefinitionImpl {

    /**
     * Creates a new qualified node definition based on a JCR NodeDefinition.
     *
     * @param nodeDef    the node definition.
     * @param nsResolver the namespace resolver in use.
     * @throws IllegalNameException   if <code>nodeDef</code> contains an
     *                                illegal name.
     * @throws UnknownPrefixException if <code>nodeDef</code> contains a name
     *                                with an namespace prefix that is unknown
     *                                to <code>nsResolver</code>.
     */
    QNodeDefinitionImpl(NodeDefinition nodeDef,
                        NamespaceResolver nsResolver)
            throws IllegalNameException, UnknownPrefixException {
        super(nodeDef.getName().equals(ANY_NAME.getLocalName()) ? ANY_NAME : NameFormat.parse(nodeDef.getName(), nsResolver),
                nodeDef.getDeclaringNodeType() != null ? NameFormat.parse(nodeDef.getDeclaringNodeType().getName(), nsResolver) : null,
                nodeDef.isAutoCreated(), nodeDef.isMandatory(),
                nodeDef.getOnParentVersion(), nodeDef.isProtected(),
                nodeDef.getDefaultPrimaryType() != null ? NameFormat.parse(nodeDef.getDefaultPrimaryType().getName(), nsResolver) : null,
                getNodeTypeNames(nodeDef.getRequiredPrimaryTypes(), nsResolver),
                nodeDef.allowsSameNameSiblings());
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
}
