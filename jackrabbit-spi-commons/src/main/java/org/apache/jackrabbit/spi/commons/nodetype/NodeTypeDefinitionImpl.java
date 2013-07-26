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
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.NamespaceException;
import javax.jcr.ValueFactory;

/**
 * <code>AbstractNodeTypeDefinition</code>...
 */
public class NodeTypeDefinitionImpl implements NodeTypeDefinition {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(NodeTypeDefinitionImpl.class);

    protected final QNodeTypeDefinition ntd;
    private final NamePathResolver resolver;
    private final ValueFactory valueFactory;

    /**
     * 
     * @param ntd
     * @param resolver
     * @param valueFactory
     */
    public NodeTypeDefinitionImpl(QNodeTypeDefinition ntd, NamePathResolver resolver,
                                  ValueFactory valueFactory) {
        this.ntd = ntd;
        this.resolver = resolver;
        this.valueFactory = valueFactory;
    }

    //-------------------------------------------------< NodeTypeDefinition >---
    /**
     * @see javax.jcr.nodetype.NodeTypeDefinition#getName()
     */
    public String getName() {
        try {
            return resolver.getJCRName(ntd.getName());
        } catch (NamespaceException e) {
            // should never get here
            log.error("encountered unregistered namespace in node type name", e);
            return ntd.getName().toString();
        }
    }

    /**
     * @see javax.jcr.nodetype.NodeTypeDefinition#getPrimaryItemName()
     */
    public String getPrimaryItemName() {
        try {
            Name piName = ntd.getPrimaryItemName();
            if (piName != null) {
                return resolver.getJCRName(piName);
            } else {
                return null;
            }
        } catch (NamespaceException e) {
            // should never get here
            log.error("encountered unregistered namespace in name of primary item", e);
            return ntd.getName().toString();
        }
    }

    /**
     * @see javax.jcr.nodetype.NodeTypeDefinition#isMixin()
     */
    public boolean isMixin() {
        return ntd.isMixin();
    }

    /**
     * @see javax.jcr.nodetype.NodeTypeDefinition#hasOrderableChildNodes()
     */
    public boolean hasOrderableChildNodes() {
        return ntd.hasOrderableChildNodes();
    }

    /**
     * @see javax.jcr.nodetype.NodeTypeDefinition#isAbstract()
     */
    public boolean isAbstract() {
        return ntd.isAbstract();
    }

    /**
     * @see javax.jcr.nodetype.NodeTypeDefinition#isQueryable()
     */
    public boolean isQueryable() {
        return ntd.isQueryable();
    }

    /**
     * @see javax.jcr.nodetype.NodeTypeDefinition#getDeclaredPropertyDefinitions()
     */
    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        QPropertyDefinition[] pds = ntd.getPropertyDefs();
        PropertyDefinition[] propDefs = new PropertyDefinition[pds.length];
        for (int i = 0; i < pds.length; i++) {
            propDefs[i] = new PropertyDefinitionImpl(pds[i], resolver, valueFactory);
        }
        return propDefs;
    }


    /**
     * @see javax.jcr.nodetype.NodeTypeDefinition#getDeclaredChildNodeDefinitions()
     */
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        QNodeDefinition[] cnda = ntd.getChildNodeDefs();
        NodeDefinition[] nodeDefs = new NodeDefinition[cnda.length];
        for (int i = 0; i < cnda.length; i++) {
            nodeDefs[i] = new NodeDefinitionImpl(cnda[i], resolver);
        }
        return nodeDefs;
    }

    /**
     * @see javax.jcr.nodetype.NodeTypeDefinition#getDeclaredSupertypeNames()
     */
    public String[] getDeclaredSupertypeNames() {
        Name[] stNames = ntd.getSupertypes();
        String[] dstn = new String[stNames.length];
        for (int i = 0; i < stNames.length; i++) {
            try {
                dstn[i] = resolver.getJCRName(stNames[i]);
            } catch (NamespaceException e) {
                // should never get here
                log.error("invalid node type name: " + stNames[i], e);
                dstn[i] = stNames[i].toString();
            }
        }
        return dstn;
    }
}