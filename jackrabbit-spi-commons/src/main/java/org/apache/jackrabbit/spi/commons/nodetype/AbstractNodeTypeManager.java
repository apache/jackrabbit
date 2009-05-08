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

import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.RepositoryException;

/**
 * <code>AbstractNodeTypeManager</code> covers creation of node type templates
 * and definition templates.
 */
public abstract class AbstractNodeTypeManager implements NodeTypeManager {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(AbstractNodeTypeManager.class);

    //----------------------------------------------------< NodeTypeManager >---
    /**
     * @see javax.jcr.nodetype.NodeTypeManager#createNodeTypeTemplate()
     */
    public NodeTypeTemplate createNodeTypeTemplate()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return new NodeTypeTemplateImpl();
    }

    /**
     * @see javax.jcr.nodetype.NodeTypeManager#createNodeTypeTemplate(NodeTypeDefinition)
     */
    public NodeTypeTemplate createNodeTypeTemplate(NodeTypeDefinition ntd)
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return new NodeTypeTemplateImpl(ntd);
    }

    /**
     * @see javax.jcr.nodetype.NodeTypeManager#createNodeDefinitionTemplate()
     */
    public NodeDefinitionTemplate createNodeDefinitionTemplate()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return new NodeDefinitionTemplateImpl(getNodeType(NodeType.NT_BASE));
    }

    /**
     * @see javax.jcr.nodetype.NodeTypeManager#createPropertyDefinitionTemplate()
     */
    public PropertyDefinitionTemplate createPropertyDefinitionTemplate()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return new PropertyDefinitionTemplateImpl();
    }
}