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
package org.apache.jackrabbit.spi.commons.nodetype;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.NamespaceException;

/**
 * This class implements the <code>NodeDefinition</code> interface.
 * All method calls are delegated to the wrapped {@link QNodeDefinition},
 * performing the translation from <code>Name</code>s to JCR names
 * where necessary.
 */
public class NodeDefinitionImpl extends ItemDefinitionImpl implements NodeDefinition {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(NodeDefinitionImpl.class);

    /**
     * Constructor to create a definition that is based on a template.
     *
     * @param itemDef  item definition
     * @param resolver
     */
    public NodeDefinitionImpl(QItemDefinition itemDef, NamePathResolver resolver) {
        super(itemDef, resolver);
    }

    /**
     * Constructor to create a definition that is based on an
     * existing node type.
     *
     * @param itemDef  item definition
     * @param resolver
     */
    public NodeDefinitionImpl(QItemDefinition itemDef, AbstractNodeTypeManager ntMgr, NamePathResolver resolver) {
        super(itemDef, ntMgr, resolver);
    }

    /**
     * Returns the wrapped node definition.
     *
     * @return the wrapped node definition.
     */
    public QNodeDefinition unwrap() {
        return (QNodeDefinition) itemDef;
    }

    //-------------------------------------------------------< NodeDefinition >

    /**
     * {@inheritDoc}
     */
    public boolean allowsSameNameSiblings() {
        return ((QNodeDefinition) itemDef).allowsSameNameSiblings();
    }

    /**
     * @see NodeDefinition#getDefaultPrimaryTypeName()
     * @since JCR 2.0
     */
    public String getDefaultPrimaryTypeName() {
        Name ntName = ((QNodeDefinition) itemDef).getDefaultPrimaryType();
        if (ntName == null) {
            return null;
        }
        try {
            return resolver.getJCRName(ntName);
        } catch (NamespaceException e) {
            // should never get here
            log.error("invalid default node type " + ntName, e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeType getDefaultPrimaryType() {
        if (ntMgr == null) {
            // not attached to an existing node type
            return null;
        }
        Name ntName = ((QNodeDefinition) itemDef).getDefaultPrimaryType();
        if (ntName == null) {
            return null;
        }
        try {
            return ntMgr.getNodeType(ntName);
        } catch (NoSuchNodeTypeException e) {
            // should never get here
            log.error("invalid default node type " + ntName, e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeType[] getRequiredPrimaryTypes() {
        if (ntMgr == null) {
            // not attached to an existing node type
            return null;
        }
        Name[] ntNames = ((QNodeDefinition) itemDef).getRequiredPrimaryTypes();
        try {
            if (ntNames == null || ntNames.length == 0) {
                // return "nt:base"
                return new NodeType[] { ntMgr.getNodeType(NameConstants.NT_BASE) };
            } else {
                NodeType[] nodeTypes = new NodeType[ntNames.length];
                for (int i = 0; i < ntNames.length; i++) {
                    nodeTypes[i] = ntMgr.getNodeType(ntNames[i]);
                }
                return nodeTypes;
            }
        } catch (NoSuchNodeTypeException e) {
            // should never get here
            log.error("required node type does not exist", e);
            return new NodeType[0];
        }
    }

    /**
     * @see NodeDefinition#getRequiredPrimaryTypeNames()
     * @since JCR 2.0
     */
    public String[] getRequiredPrimaryTypeNames() {
        Name[] ntNames = ((QNodeDefinition) itemDef).getRequiredPrimaryTypes();
        try {
            if (ntNames == null || ntNames.length == 0) {
                // return "nt:base"
                return new String[] { resolver.getJCRName(NameConstants.NT_BASE) };
            } else {
                String[] jcrNames = new String[ntNames.length];
                for (int i = 0; i < ntNames.length; i++) {
                    jcrNames[i] = resolver.getJCRName(ntNames[i]);
                }
                return jcrNames;
            }
        } catch (NamespaceException e) {
            // should never get here
            log.error("required node type does not exist", e);
            return new String[0];
        }
    }
}