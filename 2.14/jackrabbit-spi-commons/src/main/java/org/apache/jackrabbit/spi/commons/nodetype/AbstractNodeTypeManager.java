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

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.RepositoryException;

/**
 * <code>AbstractNodeTypeManager</code> covers creation of node type templates
 * and definition templates.
 */
public abstract class AbstractNodeTypeManager implements NodeTypeManager {

    /**
     * Return the node type with the specified <code>ntName</code>.
     *
     * @param ntName Name of the node type to be returned.
     * @return the node type with the specified <code>ntName</code>.
     * @throws NoSuchNodeTypeException If no such node type exists.
     */
    public abstract NodeType getNodeType(Name ntName) throws NoSuchNodeTypeException;

    /**
     * Retrieve the <code>NodeDefinition</code> for the given
     * <code>QNodeDefinition</code>.
     *
     * @param nd the QNodeDefinition.
     * @return the node definition.
     */
    public abstract NodeDefinition getNodeDefinition(QNodeDefinition nd);

    /**
     * Retrieve the <code>PropertyDefinition</code> for the given
     * <code>QPropertyDefinition</code>.
     *
     * @param pd the QPropertyDefinition.
     * @return the property definition.
     */
    public abstract PropertyDefinition getPropertyDefinition(QPropertyDefinition pd);

    /**
     * Returns the NamePathResolver used to validate JCR names.
     *
     * @return the NamePathResolver used to convert JCR names/paths to internal
     * onces and vice versa. The resolver may also be used to validate names
     * passed to the various templates.
     */
    public abstract NamePathResolver getNamePathResolver();

    //----------------------------------------------------< NodeTypeManager >---
    /**
     * @see javax.jcr.nodetype.NodeTypeManager#createNodeTypeTemplate()
     */
    public NodeTypeTemplate createNodeTypeTemplate()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return new NodeTypeTemplateImpl(getNamePathResolver());
    }

    /**
     * @see javax.jcr.nodetype.NodeTypeManager#createNodeTypeTemplate(NodeTypeDefinition)
     */
    public NodeTypeTemplate createNodeTypeTemplate(NodeTypeDefinition ntd)
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return new NodeTypeTemplateImpl(ntd, getNamePathResolver());
    }

    /**
     * @see javax.jcr.nodetype.NodeTypeManager#createNodeDefinitionTemplate()
     */
    public NodeDefinitionTemplate createNodeDefinitionTemplate()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return new NodeDefinitionTemplateImpl(getNamePathResolver());
    }

    /**
     * @see javax.jcr.nodetype.NodeTypeManager#createPropertyDefinitionTemplate()
     */
    public PropertyDefinitionTemplate createPropertyDefinitionTemplate()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return new PropertyDefinitionTemplateImpl(getNamePathResolver());
    }

    /**
     * @see javax.jcr.nodetype.NodeTypeManager#registerNodeType(NodeTypeDefinition, boolean)
     */
    public NodeType registerNodeType(NodeTypeDefinition ntd, boolean allowUpdate)
            throws RepositoryException {
        NodeTypeDefinition[] ntds = new NodeTypeDefinition[] { ntd };
        return registerNodeTypes(ntds, allowUpdate).nextNodeType();
    }

    /**
     * @see javax.jcr.nodetype.NodeTypeManager#unregisterNodeType(String)
     */
    public void unregisterNodeType(String name)
            throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException, RepositoryException {
        unregisterNodeTypes(new String[] {name});
    }
}