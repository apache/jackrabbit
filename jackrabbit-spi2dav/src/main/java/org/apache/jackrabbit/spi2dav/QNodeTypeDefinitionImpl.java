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
package org.apache.jackrabbit.spi2dav;

import org.w3c.dom.Element;
import org.apache.jackrabbit.webdav.jcr.nodetype.NodeTypeConstants;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.ElementIterator;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A <code>QNodeTypeDefinitionImpl</code> holds the definition of a node type.
 */
public class QNodeTypeDefinitionImpl implements QNodeTypeDefinition, NodeTypeConstants {

    private static Logger log = LoggerFactory.getLogger(QNodeTypeDefinitionImpl.class);

    private final Name name;
    private final Name[] supertypes;
    private final boolean mixin;
    private final boolean orderableChildNodes;
    private final Name primaryItemName;
    private final QPropertyDefinition[] propDefs;
    private final QNodeDefinition[] nodeDefs;
    private Set dependencies;

    private final boolean isAbstract;
    private final boolean isQueryable;

    /**
     * Default constructor.
     */
    public QNodeTypeDefinitionImpl(Element ntdElement, NamePathResolver resolver,
                                   QValueFactory qValueFactory)
        throws RepositoryException {
        // TODO: webdav-server currently sends jcr-names -> conversion needed
        // NOTE: the server should send the namespace-mappings as addition ns-defininitions
        try {
        if (ntdElement.hasAttribute(NAME_ATTRIBUTE)) {
            name = resolver.getQName(ntdElement.getAttribute(NAME_ATTRIBUTE));
        } else {
            name = null;
        }

        if (ntdElement.hasAttribute(PRIMARYITEMNAME_ATTRIBUTE)) {
            primaryItemName = resolver.getQName(ntdElement.getAttribute(PRIMARYITEMNAME_ATTRIBUTE));
        } else {
            primaryItemName = null;
        }

        Element child = DomUtil.getChildElement(ntdElement, SUPERTYPES_ELEMENT, null);
        if (child != null) {
            ElementIterator stIter = DomUtil.getChildren(child, SUPERTYPE_ELEMENT, null);
            List qNames = new ArrayList();
            while (stIter.hasNext()) {
                Name st = resolver.getQName(DomUtil.getTextTrim(stIter.nextElement()));
                qNames.add(st);
            }
            supertypes = (Name[]) qNames.toArray(new Name[qNames.size()]);
        } else {
            supertypes = Name.EMPTY_ARRAY;
        }
        if (ntdElement.hasAttribute(ISMIXIN_ATTRIBUTE)) {
            mixin = Boolean.valueOf(ntdElement.getAttribute(ISMIXIN_ATTRIBUTE)).booleanValue();
        } else {
            mixin = false;
        }
        if (ntdElement.hasAttribute(HASORDERABLECHILDNODES_ATTRIBUTE)) {
            orderableChildNodes = Boolean.valueOf(ntdElement.getAttribute(HASORDERABLECHILDNODES_ATTRIBUTE)).booleanValue();
        } else {
            orderableChildNodes = false;
        }
        if (ntdElement.hasAttribute(ISABSTRACT_ATTRIBUTE)) {
            isAbstract = Boolean.valueOf(ntdElement.getAttribute(ISABSTRACT_ATTRIBUTE)).booleanValue();
        } else {
            isAbstract = false;
        }
        if (ntdElement.hasAttribute(ISQUERYABLE_ATTRIBUTE)) {
            isQueryable = Boolean.valueOf(ntdElement.getAttribute(ISQUERYABLE_ATTRIBUTE)).booleanValue();
        } else {
            isQueryable = false;
        }

        // nodeDefinitions
        ElementIterator it = DomUtil.getChildren(ntdElement, CHILDNODEDEFINITION_ELEMENT, null);
        List itemDefs = new ArrayList();
        while (it.hasNext()) {
            itemDefs.add(new QNodeDefinitionImpl(name, it.nextElement(), resolver));
        }
        nodeDefs = (QNodeDefinition[]) itemDefs.toArray(new QNodeDefinition[itemDefs.size()]);


        // propertyDefinitions
        it = DomUtil.getChildren(ntdElement, PROPERTYDEFINITION_ELEMENT, null);
        itemDefs = new ArrayList();
        while (it.hasNext()) {
            itemDefs.add(new QPropertyDefinitionImpl(name, it.nextElement(), resolver, qValueFactory));
        }
        propDefs = (QPropertyDefinition[]) itemDefs.toArray(new QPropertyDefinition[itemDefs.size()]);
        } catch (NameException e) {
            log.error(e.getMessage());
            throw new RepositoryException(e);
        }
    }

    //------------------------------------------------< QNodeTypeDefinition >---
    /**
     * @see QNodeTypeDefinition#getName()
     */
    public Name getName() {
        return name;
    }

    /**
     * @see QNodeTypeDefinition#getSupertypes()
     */
    public Name[] getSupertypes() {
        if (supertypes.length > 0
                || isMixin() || NameConstants.NT_BASE.equals(getName())) {
            return supertypes;
        } else {
            return new Name[] { NameConstants.NT_BASE };
        }
    }

    /**
     * @return <code>null</code> since no restrictions are known.
     * @see QNodeTypeDefinition#getSupportedMixinTypes()
     */
    public Name[] getSupportedMixinTypes() {
        return null;
    }

    /**
     * @see QNodeTypeDefinition#isMixin()
     */
    public boolean isMixin() {
        return mixin;
    }

    /**
     * @see QNodeTypeDefinition#isAbstract()
     */
    public boolean isAbstract() {
        return isAbstract;
    }

    /**
     * @see QNodeTypeDefinition#isQueryable()
     */
    public boolean isQueryable() {
        return isQueryable;
    }

    /**
     * @see QNodeTypeDefinition#hasOrderableChildNodes()
     */
    public boolean hasOrderableChildNodes() {
        return orderableChildNodes;
    }

    /**
     * @see QNodeTypeDefinition#getPrimaryItemName()
     */
    public Name getPrimaryItemName() {
        return primaryItemName;
    }

    /**
     * @see QNodeTypeDefinition#getPropertyDefs() 
     */
    public QPropertyDefinition[] getPropertyDefs() {
        return propDefs;
    }

    /**
     * @see QNodeTypeDefinition#getChildNodeDefs() 
     */
    public QNodeDefinition[] getChildNodeDefs() {
        return nodeDefs;
    }

    /**
     * @see QNodeTypeDefinition#getDependencies() 
     */
    public Collection getDependencies() {
        if (dependencies == null) {
            dependencies = new HashSet();
            // supertypes
            for (int i = 0; i < supertypes.length; i++) {
                dependencies.add(supertypes[i]);
            }
            // child node definitions
            for (int i = 0; i < nodeDefs.length; i++) {
                // default primary type
                Name ntName = nodeDefs[i].getDefaultPrimaryType();
                if (ntName != null && !name.equals(ntName)) {
                    dependencies.add(ntName);
                }
                // required primary type
                Name[] ntNames = nodeDefs[i].getRequiredPrimaryTypes();
                for (int j = 0; j < ntNames.length; j++) {
                    if (ntNames[j] != null && !name.equals(ntNames[j])) {
                        dependencies.add(ntNames[j]);
                    }
                }
            }
            // property definitions
            for (int i = 0; i < propDefs.length; i++) {
                // [WEAK]REFERENCE value constraints
                if (propDefs[i].getRequiredType() == PropertyType.REFERENCE
                        || propDefs[i].getRequiredType() == PropertyType.WEAKREFERENCE) {
                    QValueConstraint[] ca = propDefs[i].getValueConstraints();
                    if (ca != null) {
                        for (int j = 0; j < ca.length; j++) {
                            // TODO: don't rely on a specific factory
                            Name ntName = NameFactoryImpl.getInstance().create(ca[j].getString());
                            if (!name.equals(ntName)) {
                                dependencies.add(ntName);
                            }
                        }
                    }
                }
            }
        }
        return dependencies;
    }

    //-------------------------------------------------------------< Object >---
    /**
     * @see Object#equals(Object) 
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof QNodeTypeDefinition) {
            QNodeTypeDefinition other = (QNodeTypeDefinition) obj;
            return (name == null ? other.getName() == null : name.equals(other.getName()))
                && (primaryItemName == null ? other.getPrimaryItemName() == null : primaryItemName.equals(other.getPrimaryItemName()))
                && Arrays.equals(supertypes, other.getSupertypes())
                && mixin == other.isMixin()
                && isAbstract == other.isAbstract()
                && isQueryable == other.isQueryable()
                && orderableChildNodes == other.hasOrderableChildNodes()
                && Arrays.equals(propDefs, other.getPropertyDefs())
                && Arrays.equals(nodeDefs, other.getChildNodeDefs());
        }
        return false;
    }

    /**
     * Always returns 0
     *
     * @see Object#hashCode()
     */
    public int hashCode() {
        // TODO: can be calculated for the definition is immutable
        return 0;
    }
}
