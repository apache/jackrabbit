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
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

/**
 * A <code>NodeTypeTemplateImpl</code> ...
 */
public class NodeTypeTemplateImpl implements NodeTypeTemplate {

    private static final Logger log = LoggerFactory.getLogger(NodeTypeTemplateImpl.class);

    private Name name;
    private Name[] superTypeNames;
    private Name primaryItemName;
    private boolean abstractStatus;
    private boolean queryable;
    private boolean mixin;
    private boolean orderableChildNodes;
    private List<NodeDefinitionTemplate> nodeDefinitionTemplates;
    private List<PropertyDefinitionTemplate> propertyDefinitionTemplates;

    private final NamePathResolver resolver;

    /**
     * Package private constructor
     *
     * @param resolver
     */
    NodeTypeTemplateImpl(NamePathResolver resolver) {
        // TODO: see https://jsr-283.dev.java.net/issues/show_bug.cgi?id=798
        queryable = true;
        // TODO see https://jsr-283.dev.java.net/issues/show_bug.cgi?id=797
        superTypeNames = Name.EMPTY_ARRAY;
        this.resolver = resolver;
    }

    /**
     * Package private constructor
     *
     * @param def
     * @param resolver
     */
    NodeTypeTemplateImpl(NodeTypeDefinition def, NamePathResolver resolver) throws RepositoryException {
        this.resolver = resolver;
        
        if (def instanceof NodeTypeDefinitionImpl) {
            QNodeTypeDefinition qDef = ((NodeTypeDefinitionImpl) def).ntd;
            name = qDef.getName();
            superTypeNames = qDef.getSupertypes();
            primaryItemName = qDef.getPrimaryItemName();
        } else {
            setName(def.getName());
            setDeclaredSuperTypeNames(def.getDeclaredSupertypeNames());
            setPrimaryItemName(def.getPrimaryItemName());
        }

        abstractStatus = def.isAbstract();
        mixin = def.isMixin();
        queryable = def.isQueryable();
        orderableChildNodes = def.hasOrderableChildNodes();

        NodeDefinition[] nodeDefs = def.getDeclaredChildNodeDefinitions();
        if (nodeDefs != null) {
            List list = getNodeDefinitionTemplates();
            for (NodeDefinition nodeDef : nodeDefs) {
                list.add(new NodeDefinitionTemplateImpl(nodeDef, resolver));
            }
        }
        PropertyDefinition[] propDefs = def.getDeclaredPropertyDefinitions();
        if (propDefs != null) {
            List list = getPropertyDefinitionTemplates();
            for (PropertyDefinition propDef : propDefs) {
                list.add(new PropertyDefinitionTemplateImpl(propDef, resolver));
            }
        }
    }

    //-----------------------------------------------------< NodeTypeTemplate >
    /**
     * {@inheritDoc}
     */
    public void setName(String name) throws ConstraintViolationException {
        try {
            this.name = resolver.getQName(name);
        } catch (RepositoryException e) {
            throw new ConstraintViolationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setDeclaredSuperTypeNames(String[] names) throws ConstraintViolationException {
        // TODO see https://jsr-283.dev.java.net/issues/show_bug.cgi?id=797
        if (names == null) {
            throw new ConstraintViolationException("null isn't a valid array of JCR names.");            
        } else {
            superTypeNames = new Name[names.length];
            for (int i = 0; i < names.length; i++) {
                try {
                    superTypeNames[i] = resolver.getQName(names[i]);
                } catch (RepositoryException e) {
                    throw new ConstraintViolationException(e);
                }
            }
        }
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
    public void setPrimaryItemName(String name) throws ConstraintViolationException {
        if (name == null) {
            primaryItemName = null;
        } else {
            try {
                primaryItemName = resolver.getQName(name);
            } catch (RepositoryException e) {
                throw new ConstraintViolationException(e);
            }
        }
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
        if (name == null) {
            return null;
        } else {
            try {
                return resolver.getJCRName(name);
            } catch (NamespaceException e) {
                // should never get here
                log.error("encountered unregistered namespace in node type name", e);
                return name.toString();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getDeclaredSupertypeNames() {
        String[] names = new String[superTypeNames.length];
        for (int i = 0; i < superTypeNames.length; i++) {
            try {
                names[i] = resolver.getJCRName(superTypeNames[i]);
            } catch (NamespaceException e) {
                // should never get here
                log.error("encountered unregistered namespace in super type name", e);
                names[i] = superTypeNames[i].toString();
            }
        }
        return names;
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
        if (primaryItemName == null) {
            return null;
        } else {
            try {
                return resolver.getJCRName(primaryItemName);
            } catch (NamespaceException e) {
                // should never get here
                log.error("encountered unregistered namespace in primary type name", e);
                return primaryItemName.toString();
            }
        }
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
