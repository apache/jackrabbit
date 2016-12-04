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

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.nodetype.NodeDefinitionTemplate;

import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * A <code>NodeDefinitionTemplateImpl</code> ...
 */
class NodeDefinitionTemplateImpl
        extends AbstractItemDefinitionTemplate
        implements NodeDefinitionTemplate {

    private static final Logger log = LoggerFactory.getLogger(NodeDefinitionTemplateImpl.class);

    private NodeType[] requiredPrimaryTypes;
    private Name[] requiredPrimaryTypeNames;
    private Name defaultPrimaryTypeName;
    private boolean allowSameNameSiblings;

    /**
     * Package private constructor
     *
     * @param resolver
     * @throws RepositoryException
     */
    NodeDefinitionTemplateImpl(NamePathResolver resolver) throws RepositoryException {
        super(resolver);
        requiredPrimaryTypes = null;
        requiredPrimaryTypeNames = null;
    }

    /**
     * Package private constructor
     *
     * @param def
     * @param resolver
     * @throws javax.jcr.nodetype.ConstraintViolationException
     */
    NodeDefinitionTemplateImpl(NodeDefinition def, NamePathResolver resolver) throws ConstraintViolationException {
        super(def, resolver);
        requiredPrimaryTypes = def.getRequiredPrimaryTypes();
        allowSameNameSiblings = def.allowsSameNameSiblings();

        if (def instanceof NodeDefinitionImpl) {
            QNodeDefinition qDef = (QNodeDefinition) ((NodeDefinitionImpl) def).itemDef;
            requiredPrimaryTypeNames = qDef.getRequiredPrimaryTypes();
            defaultPrimaryTypeName = qDef.getDefaultPrimaryType();
        } else {
            setRequiredPrimaryTypeNames(def.getRequiredPrimaryTypeNames());
            setDefaultPrimaryTypeName(def.getDefaultPrimaryTypeName());
        }        
    }

    //-----------------------------------------------< NodeDefinitionTemplate >
    /**
     * {@inheritDoc}
     */
    public void setRequiredPrimaryTypeNames(String[] requiredPrimaryTypeNames) throws ConstraintViolationException {
        if (requiredPrimaryTypeNames == null) {
            throw new ConstraintViolationException("null isn't a valid array of JCR names.");
        } else {
            this.requiredPrimaryTypeNames = new Name[requiredPrimaryTypeNames.length];
            for (int i = 0; i < requiredPrimaryTypeNames.length; i++) {
                try {
                    this.requiredPrimaryTypeNames[i] = resolver.getQName(requiredPrimaryTypeNames[i]);
                } catch (RepositoryException e) {
                    throw new ConstraintViolationException(e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setDefaultPrimaryTypeName(String defaultPrimaryType) throws ConstraintViolationException {
        try {
            this.defaultPrimaryTypeName = defaultPrimaryType == null
                    ? null
                    : resolver.getQName(defaultPrimaryType);
        } catch (RepositoryException e) {
            throw new ConstraintViolationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setSameNameSiblings(boolean allowSameNameSiblings) {
        this.allowSameNameSiblings = allowSameNameSiblings;
    }

    //-------------------------------------------------------< NodeDefinition >
    /**
     * {@inheritDoc}
     */
    public NodeType[] getRequiredPrimaryTypes() {
        return requiredPrimaryTypes;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getRequiredPrimaryTypeNames() {
        if (requiredPrimaryTypeNames == null) {
            return null;
        } else {
            String[] rptNames = new String[requiredPrimaryTypeNames.length];
            for (int i = 0; i < requiredPrimaryTypeNames.length; i++) {
                try {
                    rptNames[i] = resolver.getJCRName(requiredPrimaryTypeNames[i]);
                } catch (NamespaceException e) {
                    // should never get here
                    log.error("invalid node type name: " + requiredPrimaryTypeNames[i], e);
                    rptNames[i] = requiredPrimaryTypeNames[i].toString();
                }
            }
            return rptNames;
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeType getDefaultPrimaryType() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getDefaultPrimaryTypeName() {
        if (defaultPrimaryTypeName == null) {
            return null;
        } else {
            try {
                return resolver.getJCRName(defaultPrimaryTypeName);
            } catch (NamespaceException e) {
                // should never get here
                log.error("encountered unregistered namespace in default primary type name", e);
                return defaultPrimaryTypeName.toString();
            }
        }
    }

    /**
     * {@inheritDoc}
     */

    public boolean allowsSameNameSiblings() {
        return allowSameNameSiblings;
    }
}
