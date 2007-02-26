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
package org.apache.jackrabbit.spi2jcr;

import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.NameException;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Collections;

/**
 * <code>QNodeTypeDefinitionImpl</code> implements a qualified node type
 * definition based on a JCR {@link NodeType}.
 */
class QNodeTypeDefinitionImpl implements QNodeTypeDefinition {

    /**
     * The name of the node definition.
     */
    private final QName name;

    /**
     * The names of the declared super types of this node type definition.
     */
    private final QName[] supertypes;

    /**
     * Indicates whether this is a mixin node type definition.
     */
    private final boolean isMixin;

    /**
     * Indicates whether this node type definition has orderable child nodes.
     */
    private final boolean hasOrderableChildNodes;

    /**
     * The name of the primary item or <code>null</code> if none is defined.
     */
    private final QName primaryItemName;

    /**
     * The list of property definitions.
     */
    private final QPropertyDefinition[] propertyDefs;

    /**
     * The list of child node definitions.
     */
    private final QNodeDefinition[] childNodeDefs;

    /**
     * Unmodifiable collection of dependent node type <code>QName</code>s.
     * @see #getDependencies()
     */
    private Collection dependencies;

    /**
     * Creates a new qualified node type definition based on a JCR
     * <code>NodeType</code>.
     *
     * @param nt            the JCR node type.
     * @param nsResolver    the namespace resolver in use.
     * @param qValueFactory the QValue factory.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>nt</code>.
     */
    public QNodeTypeDefinitionImpl(NodeType nt,
                                   NamespaceResolver nsResolver,
                                   QValueFactory qValueFactory)
            throws RepositoryException {
        try {
            this.name = NameFormat.parse(nt.getName(), nsResolver);
            NodeType[] superNts = nt.getDeclaredSupertypes();
            this.supertypes = new QName[superNts.length];
            for (int i = 0; i < superNts.length; i++) {
                supertypes[i] = NameFormat.parse(superNts[i].getName(), nsResolver);
            }
            this.isMixin = nt.isMixin();
            this.hasOrderableChildNodes = nt.hasOrderableChildNodes();
            String primaryItemJcrName = nt.getPrimaryItemName();
            if (primaryItemJcrName == null) {
                this.primaryItemName = null;
            } else {
                this.primaryItemName = NameFormat.parse(primaryItemJcrName, nsResolver);
            }
        } catch (NameException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
        PropertyDefinition[] propDefs = nt.getDeclaredPropertyDefinitions();
        this.propertyDefs = new QPropertyDefinition[propDefs.length];
        for (int i = 0; i < propDefs.length; i++) {
            this.propertyDefs[i] = new QPropertyDefinitionImpl(
                    propDefs[i], nsResolver, qValueFactory);
        }
        NodeDefinition[] nodeDefs = nt.getDeclaredChildNodeDefinitions();
        this.childNodeDefs = new QNodeDefinition[nodeDefs.length];
        for (int i = 0; i < nodeDefs.length; i++) {
            this.childNodeDefs[i] = new QNodeDefinitionImpl(nodeDefs[i], nsResolver);
        }
    }

    /**
     * {@inheritDoc}
     */
    public QName getQName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public QName[] getSupertypes() {
        QName[] sTypes = new QName[supertypes.length];
        System.arraycopy(supertypes, 0, sTypes, 0, supertypes.length);
        return sTypes;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMixin() {
        return isMixin;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasOrderableChildNodes() {
        return hasOrderableChildNodes;
    }

    /**
     * {@inheritDoc}
     */
    public QName getPrimaryItemName() {
        return primaryItemName;
    }

    /**
     * {@inheritDoc}
     */
    public QPropertyDefinition[] getPropertyDefs() {
        QPropertyDefinition[] pDefs = new QPropertyDefinition[propertyDefs.length];
        System.arraycopy(propertyDefs, 0, pDefs, 0, propertyDefs.length);
        return pDefs;
    }

    /**
     * {@inheritDoc}
     */
    public QNodeDefinition[] getChildNodeDefs() {
        QNodeDefinition[] cnDefs = new QNodeDefinition[childNodeDefs.length];
        System.arraycopy(childNodeDefs, 0, cnDefs, 0, childNodeDefs.length);
        return cnDefs;
    }

    /**
     * TODO: generalize (this method is copied from spi2dav)
     */
    public Collection getDependencies() {
        if (dependencies == null) {
            Collection deps = new HashSet();
            // supertypes
            for (int i = 0; i < supertypes.length; i++) {
                deps.add(supertypes[i]);
            }
            // child node definitions
            for (int i = 0; i < childNodeDefs.length; i++) {
                // default primary type
                QName ntName = childNodeDefs[i].getDefaultPrimaryType();
                if (ntName != null && !name.equals(ntName)) {
                    deps.add(ntName);
                }
                // required primary type
                QName[] ntNames = childNodeDefs[i].getRequiredPrimaryTypes();
                for (int j = 0; j < ntNames.length; j++) {
                    if (ntNames[j] != null && !name.equals(ntNames[j])) {
                        deps.add(ntNames[j]);
                    }
                }
            }
            // property definitions
            for (int i = 0; i < propertyDefs.length; i++) {
                // REFERENCE value constraints
                if (propertyDefs[i].getRequiredType() == PropertyType.REFERENCE) {
                    String[] ca = propertyDefs[i].getValueConstraints();
                    if (ca != null) {
                        for (int j = 0; j < ca.length; j++) {
                            QName ntName = QName.valueOf(ca[j]);
                            if (!name.equals(ntName)) {
                                deps.add(ntName);
                            }
                        }
                    }
                }
            }
            dependencies = Collections.unmodifiableCollection(deps);
        }
        return dependencies;
    }
}
