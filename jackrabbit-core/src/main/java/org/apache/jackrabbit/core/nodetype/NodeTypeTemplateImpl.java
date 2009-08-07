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

import org.apache.commons.collections.list.TypedList;
import org.apache.jackrabbit.api.jsr283.nodetype.NodeTypeTemplate;
import org.apache.jackrabbit.api.jsr283.nodetype.NodeTypeDefinition;
import org.apache.jackrabbit.api.jsr283.nodetype.NodeDefinitionTemplate;
import org.apache.jackrabbit.api.jsr283.nodetype.PropertyDefinitionTemplate;

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import java.util.ArrayList;
import java.util.List;

/**
 * A <code>NodeTypeTemplateImpl</code> ...
 */
public class NodeTypeTemplateImpl implements NodeTypeTemplate {

    private String name;
    private String[] superTypeNames;
    private String primaryItemName;
    private boolean abstractStatus;
    private boolean mixin;
    private boolean orderableChildNodes;
    private List nodeDefinitionTemplates;
    private List propertyDefinitionTemplates;

    /**
     * Package private constructor
     */
    NodeTypeTemplateImpl() {
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
        orderableChildNodes = def.hasOrderableChildNodes();
        NodeDefinition[] nodeDefs = def.getDeclaredChildNodeDefinitions();
        if (nodeDefs != null) {
            List list = getNodeDefinitionTemplates();
            for (int i = 0; i < nodeDefs.length; i++) {
                list.add(new NodeDefinitionTemplateImpl(nodeDefs[i]));
            }
        }
        PropertyDefinition[] propDefs = def.getDeclaredPropertyDefinitions();
        if (propDefs != null) {
            List list = getPropertyDefinitionTemplates();
            for (int i = 0; i < propDefs.length; i++) {
                list.add(new PropertyDefinitionTemplateImpl(propDefs[i]));
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
            propertyDefinitionTemplates = TypedList.decorate(
                    new ArrayList(), PropertyDefinitionTemplate.class);
        }
        return propertyDefinitionTemplates;
    }

    /**
     * {@inheritDoc}
     */
    public List getNodeDefinitionTemplates() {
        if (nodeDefinitionTemplates == null) {
            nodeDefinitionTemplates = TypedList.decorate(
                    new ArrayList(), NodeDefinitionTemplate.class);
        }
        return nodeDefinitionTemplates;
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
            return (PropertyDefinition[]) propertyDefinitionTemplates.toArray(
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
            return (NodeDefinition[]) nodeDefinitionTemplates.toArray(
                    new NodeDefinition[nodeDefinitionTemplates.size()]);
        }
    }
}
