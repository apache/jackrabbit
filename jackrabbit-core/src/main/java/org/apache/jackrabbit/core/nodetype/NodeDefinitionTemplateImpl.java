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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.api.jsr283.nodetype.NodeDefinitionTemplate;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

/**
 * A <code>NodeDefinitionTemplateImpl</code> ...
 */
class NodeDefinitionTemplateImpl
        extends AbstractItemDefinitionTemplate
        implements NodeDefinitionTemplate {

    private NodeType[] requiredPrimaryTypes;
    private String[] requiredPrimaryTypeNames;
    private String defaultPrimaryTypeName;
    private boolean allowSameNameSiblings;

    /**
     * Package private constructor
     *
     * @param ntMgr
     * @throws RepositoryException
     */
    NodeDefinitionTemplateImpl(NodeTypeManagerImpl ntMgr) throws RepositoryException {
        requiredPrimaryTypes = new NodeType[] {ntMgr.getNodeType(NameConstants.NT_BASE)};
        requiredPrimaryTypeNames = new String[] {requiredPrimaryTypes[0].getName()};
    }

    /**
     * Package private constructor
     *
     * @param def
     */
    NodeDefinitionTemplateImpl(NodeDefinition def) {
        super(def);
        requiredPrimaryTypes = def.getRequiredPrimaryTypes();
        // FIXME temporary workaround until JSR 283 has been finalized
        requiredPrimaryTypeNames = new String[requiredPrimaryTypes.length];
        for (int i = 0; i < requiredPrimaryTypes.length; i++) {
            requiredPrimaryTypeNames[i] = requiredPrimaryTypes[i].getName();
        }
        defaultPrimaryTypeName =
                def.getDefaultPrimaryType() == null ? null : def.getDefaultPrimaryType().getName();
        allowSameNameSiblings = def.allowsSameNameSiblings();
    }

    //-----------------------------------------------< NodeDefinitionTemplate >
    /**
     * {@inheritDoc}
     */
    public void setRequiredPrimaryTypeNames(String[] requiredPrimaryTypeNames) {
        this.requiredPrimaryTypeNames = requiredPrimaryTypeNames;
    }

    /**
     * {@inheritDoc}
     */
    public void setDefaultPrimaryTypeName(String defaultPrimaryType) {
        this.defaultPrimaryTypeName = defaultPrimaryType;
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
        return requiredPrimaryTypeNames;
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
        return defaultPrimaryTypeName;
    }

    /**
     * {@inheritDoc}
     */

    public boolean allowsSameNameSiblings() {
        return allowSameNameSiblings;
    }
}
