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
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
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

    private final QName name;
    private final QName[] supertypes;
    private final boolean mixin;
    private final boolean orderableChildNodes;
    private final QName primaryItemName;
    private final QPropertyDefinition[] propDefs;
    private final QNodeDefinition[] nodeDefs;
    private Set dependencies;

    /**
     * Default constructor.
     */
    public QNodeTypeDefinitionImpl(Element ntdElement, NamespaceResolver nsResolver)
        throws RepositoryException {
        // TODO: webdav-server currently sends jcr-names -> conversion needed
        // NOTE: the server should send the namespace-mappings as addition ns-defininitions
        try {
        if (ntdElement.hasAttribute(NAME_ATTRIBUTE)) {
            name = nsResolver.getQName(ntdElement.getAttribute(NAME_ATTRIBUTE));
        } else {
            name = null;
        }

        if (ntdElement.hasAttribute(PRIMARYITEMNAME_ATTRIBUTE)) {
            primaryItemName = nsResolver.getQName(ntdElement.getAttribute(PRIMARYITEMNAME_ATTRIBUTE));
        } else {
            primaryItemName = null;
        }

        Element child = DomUtil.getChildElement(ntdElement, SUPERTYPES_ELEMENT, null);
        if (child != null) {
            ElementIterator stIter = DomUtil.getChildren(child, SUPERTYPE_ELEMENT, null);
            List qNames = new ArrayList();
            while (stIter.hasNext()) {
                QName st = nsResolver.getQName(DomUtil.getTextTrim(stIter.nextElement()));
                qNames.add(st);
            }
            supertypes = (QName[]) qNames.toArray(new QName[qNames.size()]);
        } else {
            supertypes = QName.EMPTY_ARRAY;
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

        // nodeDefinitions
        ElementIterator it = DomUtil.getChildren(ntdElement, CHILDNODEDEFINITION_ELEMENT, null);
        List itemDefs = new ArrayList();
        while (it.hasNext()) {
            itemDefs.add(new QNodeDefinitionImpl(name, it.nextElement(), nsResolver));
        }
        nodeDefs = (QNodeDefinition[]) itemDefs.toArray(new QNodeDefinition[itemDefs.size()]);


        // propertyDefinitions
        it = DomUtil.getChildren(ntdElement, PROPERTYDEFINITION_ELEMENT, null);
        itemDefs = new ArrayList();
        while (it.hasNext()) {
            itemDefs.add(new QPropertyDefinitionImpl(name, it.nextElement(), nsResolver));
        }
        propDefs = (QPropertyDefinition[]) itemDefs.toArray(new QPropertyDefinition[itemDefs.size()]);
        } catch (NameException e) {
            log.error(e.getMessage());
            throw new RepositoryException(e);
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof QNodeTypeDefinition) {
            QNodeTypeDefinition other = (QNodeTypeDefinition) obj;
            return (name == null ? other.getQName() == null : name.equals(other.getQName()))
                    && (primaryItemName == null ? other.getPrimaryItemName() == null : primaryItemName.equals(other.getPrimaryItemName()))
                    && Arrays.equals(supertypes, other.getSupertypes())
                    && mixin == other.isMixin()
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

    /**
     * Returns the name of the node type being defined or
     * <code>null</code> if not set.
     *
     * @return the name of the node type or <code>null</code> if not set.
     */
    public QName getQName() {
        return name;
    }

    /**
     * Returns an array containing the names of the supertypes or
     * <code>null</code> if not set.
     *
     * @return an array listing the names of the supertypes or
     *         <code>null</code> if not set.
     */
    public QName[] getSupertypes() {
        return supertypes;
    }

    /**
     * Returns the value of the mixin flag.
     *
     * @return true if this is a mixin node type; false otherwise.
     */
    public boolean isMixin() {
        return mixin;
    }

    /**
     * Returns the value of the orderableChildNodes flag.
     *
     * @return true if nodes of this node type can have orderable child nodes; false otherwise.
     */
    public boolean hasOrderableChildNodes() {
        return orderableChildNodes;
    }

    /**
     * Returns the name of the primary item (one of the child items of the
     * node's of this node type) or <code>null</code> if not set.
     *
     * @return the name of the primary item or <code>null</code> if not set.
     */
    public QName getPrimaryItemName() {
        return primaryItemName;
    }

    /**
     * Returns an array containing the property definitions or
     * <code>null</code> if not set.
     *
     * @return an array containing the property definitions or
     *         <code>null</code> if not set.
     */
    public QPropertyDefinition[] getPropertyDefs() {
        return propDefs;
    }

    /**
     * Returns an array containing the child node definitions or
     * <code>null</code> if not set.
     *
     * @return an array containing the child node definitions or
     *         <code>null</code> if not set.
     */
    public QNodeDefinition[] getChildNodeDefs() {
        return nodeDefs;
    }

    /**
     * @inheritDoc
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
                QName ntName = nodeDefs[i].getDefaultPrimaryType();
                if (ntName != null && !name.equals(ntName)) {
                    dependencies.add(ntName);
                }
                // required primary type
                QName[] ntNames = nodeDefs[i].getRequiredPrimaryTypes();
                for (int j = 0; j < ntNames.length; j++) {
                    if (ntNames[j] != null && !name.equals(ntNames[j])) {
                        dependencies.add(ntNames[j]);
                    }
                }
            }
            // property definitions
            for (int i = 0; i < propDefs.length; i++) {
                // REFERENCE value constraints
                if (propDefs[i].getRequiredType() == PropertyType.REFERENCE) {
                    String[] ca = propDefs[i].getValueConstraints();
                    if (ca != null) {
                        for (int j = 0; j < ca.length; j++) {
                            QName ntName = QName.valueOf(ca[j]);
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
}
