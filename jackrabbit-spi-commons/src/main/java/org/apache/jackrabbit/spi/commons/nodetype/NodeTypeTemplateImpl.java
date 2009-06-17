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

import java.util.LinkedList;
import java.util.List;

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.PropertyDefinitionTemplate;

/**
 * A <code>NodeTypeTemplateImpl</code> ...
 */
public class NodeTypeTemplateImpl implements NodeTypeTemplate {

    private String name;
    private String[] superTypeNames;
    private String primaryItemName;
    private boolean abstractStatus;
    private boolean queryable;
    private boolean mixin;
    private boolean orderableChildNodes;
    private List<NodeDefinitionTemplate> nodeDefinitionTemplates;
    private List<PropertyDefinitionTemplate> propertyDefinitionTemplates;

    /**
     * Package private constructor
     */
    NodeTypeTemplateImpl() {
        queryable = true;
    }

    /**
     * Package private constructor
     *
     * @param def
     */
    NodeTypeTemplateImpl(NodeTypeDefinition def) {
        name = def.getName();
        superTypeNames = def.getDeclaredSupertypeNames();
        primaryItemName = def.getPrimaryItemName();
        abstractStatus = def.isAbstract();
        mixin = def.isMixin();
        queryable = def.isQueryable();
        orderableChildNodes = def.hasOrderableChildNodes();
        NodeDefinition[] nodeDefs = def.getDeclaredChildNodeDefinitions();
        if (nodeDefs != null) {
            List list = getNodeDefinitionTemplates();
            for (NodeDefinition nodeDef : nodeDefs) {
                list.add(new NodeDefinitionTemplateImpl(nodeDef));
            }
        }
        PropertyDefinition[] propDefs = def.getDeclaredPropertyDefinitions();
        if (propDefs != null) {
            List list = getPropertyDefinitionTemplates();
            for (PropertyDefinition propDef : propDefs) {
                list.add(new PropertyDefinitionTemplateImpl(propDef));
            }
        }
    }

    //-----------------------------------------------------< NodeTypeTemplate >
    /**
     * {@inheritDoc}
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public void setDeclaredSuperTypeNames(String[] names) {
        superTypeNames = names;
    }

    /**
     * {@inheritDoc}
     */
    public void setAbstract(boolean abstractStatus) {
        this.abstractStatus = abstractStatus;
    }

    /**
     * {@inheritDoc}
     */
    public void setMixin(boolean mixin) {
        this.mixin = mixin;
    }

    /**
     * {@inheritDoc}
     */
    public void setOrderableChildNodes(boolean orderable) {
        orderableChildNodes = orderable;
    }

    /**
     * {@inheritDoc}
     */
    public void setPrimaryItemName(String name) {
        primaryItemName = name;
    }

    /**
     * {@inheritDoc}
     */
    public List getPropertyDefinitionTemplates() {
        if (propertyDefinitionTemplates == null) {
            propertyDefinitionTemplates = new LinkedList<PropertyDefinitionTemplate>();
        }
        return propertyDefinitionTemplates;
    }

    /**
     * {@inheritDoc}
     */
    public List getNodeDefinitionTemplates() {
        if (nodeDefinitionTemplates == null) {
            nodeDefinitionTemplates = new LinkedList<NodeDefinitionTemplate>();
        }
        return nodeDefinitionTemplates;
    }

    /**
     * {@inheritDoc}
     */
    public void setQueryable(boolean queryable) {
        this.queryable = queryable;
    }

    //---------------------------------------------------< NodeTypeDefinition >
    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getDeclaredSupertypeNames() {
        return superTypeNames;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAbstract() {
        return abstractStatus;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMixin() {
        return mixin;
    }

    public boolean isQueryable() {
        return queryable;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasOrderableChildNodes() {
        return orderableChildNodes;
    }

    /**
     * {@inheritDoc}
     */
    public String getPrimaryItemName() {
        return primaryItemName;
    }

    /**
     * {@inheritDoc}
     */
    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        if (propertyDefinitionTemplates == null) {
            return null;
        } else {
            return propertyDefinitionTemplates.toArray(
                    new PropertyDefinition[propertyDefinitionTemplates.size()]);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        if (nodeDefinitionTemplates == null) {
            return null;
        } else {
            return nodeDefinitionTemplates.toArray(
                    new NodeDefinition[nodeDefinitionTemplates.size()]);
        }
    }

}
