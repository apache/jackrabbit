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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.commons.cnd.AbstractItemTypeDefinitionsBuilder;
import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.commons.QNodeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.QPropertyDefinitionImpl;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
import org.apache.jackrabbit.spi.commons.query.qom.Operator;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.util.ISO9075;

/**
 * This implementation of {@link AbstractItemTypeDefinitionsBuilder} can be used with
 * the {@link CompactNodeTypeDefReader} to produce node type definitions of type
 * {@link QNodeTypeDefinition} and a namespace map of type {@link NamespaceMapping}.
 * It uses {@link QNodeTypeDefinitionBuilder} for building node type definitions,
 * {@link QPropertyDefinitionBuilder} for building property definitions, and
 * {@link QNodeDefinitionBuilder} for building node definitions. It further uses
 * {@link NameFactoryImpl} for creating <code>Name</code>s and {@link QValueFactoryImpl} for
 * creating <code>QValue</code>s.
 */
public class QItemDefinitionsBuilder extends
        AbstractItemTypeDefinitionsBuilder<QNodeTypeDefinition, NamespaceMapping> {

    private static final NameFactory NAME_FACTORY = NameFactoryImpl.getInstance();

    /**
     * Default namespace mappings
     */
    public static final NamespaceMapping NS_DEFAULTS;
    static {
        try {
            NS_DEFAULTS = new NamespaceMapping();
            NS_DEFAULTS.setMapping(Name.NS_EMPTY_PREFIX, Name.NS_DEFAULT_URI);
            NS_DEFAULTS.setMapping(Name.NS_JCR_PREFIX, Name.NS_JCR_URI);
            NS_DEFAULTS.setMapping(Name.NS_MIX_PREFIX, Name.NS_MIX_URI);
            NS_DEFAULTS.setMapping(Name.NS_NT_PREFIX, Name.NS_NT_URI);
            NS_DEFAULTS.setMapping(Name.NS_REP_PREFIX, Name.NS_REP_URI);
        } catch (NamespaceException e) {
            throw new InternalError(e.toString());
        }
    }

    private NamespaceMapping nsMappings = new NamespaceMapping(NS_DEFAULTS);
    private NamePathResolver resolver = new DefaultNamePathResolver(nsMappings);

    @Override
    public AbstractNodeTypeDefinitionBuilder<QNodeTypeDefinition> newNodeTypeDefinitionBuilder() {
        return new QNodeTypeDefinitionBuilder();
    }

    @Override
    public void setNamespaceMapping(NamespaceMapping nsMapping) {
        this.nsMappings = nsMapping;
        this.resolver = new DefaultNamePathResolver(nsMapping);
    }

    @Override
    public NamespaceMapping getNamespaceMapping() {
        return nsMappings;
    }

    @Override
    public void setNamespace(String prefix, String uri) {
        try {
            nsMappings.setMapping(prefix, uri);
        }
        catch (NamespaceException e) {
            // ignore
        }
    }

    public class QNodeTypeDefinitionBuilder extends AbstractNodeTypeDefinitionBuilder<QNodeTypeDefinition> {
        private Name name;
        private final List<Name> supertypes = new ArrayList<Name>();
        private Name primaryItem;
        private final List<QPropertyDefinition> propertyDefs = new ArrayList<QPropertyDefinition>();
        private final List<QNodeDefinition> childNodeDefs = new ArrayList<QNodeDefinition>();

        @Override
        public AbstractNodeDefinitionBuilder<QNodeTypeDefinition> newNodeDefinitionBuilder() {
            return new QNodeDefinitionBuilder(this);
        }

        @Override
        public AbstractPropertyDefinitionBuilder<QNodeTypeDefinition> newPropertyDefinitionBuilder() {
            return new QPropertyDefinitionBuilder(this);
        }

        @Override
        public QNodeTypeDefinition build() {
            return new QNodeTypeDefinitionImpl(
                    name,
                    supertypes.toArray(new Name[supertypes.size()]),
                    null,
                    super.isMixin,
                    super.isAbstract,
                    super.queryable,
                    super.isOrderable,
                    primaryItem,
                    propertyDefs.toArray(new QPropertyDefinition[propertyDefs.size()]),
                    childNodeDefs.toArray(new QNodeDefinition[childNodeDefs.size()]));
        }

        @Override
        public void setName(String name) throws RepositoryException {
            super.setName(name);
            this.name = toName(name);
        }

        @Override
        public void addSupertype(String name) throws IllegalNameException, NamespaceException {
            supertypes.add(toName(name));
        }

        @Override
        public void setPrimaryItemName(String name) throws IllegalNameException, NamespaceException {
            primaryItem = toName(name);
        }

    }

    public class QPropertyDefinitionBuilder extends AbstractPropertyDefinitionBuilder<QNodeTypeDefinition> {
        private Name name;
        private final QNodeTypeDefinitionBuilder ntd;
        private final List<QValue> values = new ArrayList<QValue>();
        private final List<QValueConstraint> constraints = new ArrayList<QValueConstraint>();
        private Name declaringType;

        public QPropertyDefinitionBuilder(QNodeTypeDefinitionBuilder ntd) {
            super();
            this.ntd = ntd;
        }

        @Override
        public void setName(String name) throws RepositoryException {
            super.setName(name);
            if ("*".equals(name)) {
                this.name = NameConstants.ANY_NAME;
            }
            else {
                this.name = toName(name);
            }
        }

        @Override
        public void addDefaultValues(String value) throws RepositoryException {
            values.add(ValueFormat.getQValue(value, getRequiredType(), resolver, QValueFactoryImpl.getInstance()));
        }

        @Override
        public void addValueConstraint(String constraint) throws InvalidConstraintException {
            constraints.add(ValueConstraint.create(getRequiredType(), constraint, resolver));
        }

        @Override
        public void setDeclaringNodeType(String name) throws IllegalNameException, NamespaceException {
            this.declaringType = toName(name);
        }

        @Override
        public void build() throws IllegalStateException {
            if (queryOperators == null) {
                queryOperators = Operator.getAllQueryOperators();
            }

            ntd.propertyDefs.add(new QPropertyDefinitionImpl(
                    name,
                    declaringType,
                    super.autocreate,
                    super.isMandatory,
                    super.onParent,
                    super.isProtected,
                    values.toArray(new QValue[values.size()]),
                    super.isMultiple,
                    super.requiredType,
                    constraints.toArray(new QValueConstraint[constraints.size()]),
                    super.queryOperators,
                    super.fullTextSearchable,
                    super.queryOrderable));
        }

    }

    public class QNodeDefinitionBuilder extends AbstractNodeDefinitionBuilder<QNodeTypeDefinition> {
        private final QNodeTypeDefinitionBuilder ntd;
        private Name name;
        private final List<Name> requiredPrimaryTypes = new ArrayList<Name>();
        private Name defaultPrimaryType;
        private Name declaringNodeType;

        public QNodeDefinitionBuilder(QNodeTypeDefinitionBuilder ntd) {
            super();
            this.ntd = ntd;
        }

        @Override
        public void setName(String name) throws RepositoryException {
            super.setName(name);
            if ("*".equals(name)) {
                this.name = NameConstants.ANY_NAME;
            }
            else {
                this.name = toName(name);
            }
        }

        @Override
        public void addRequiredPrimaryType(String name) throws IllegalNameException, NamespaceException {
            requiredPrimaryTypes.add(toName(name));
        }

        @Override
        public void setDefaultPrimaryType(String name) throws IllegalNameException, NamespaceException {
            defaultPrimaryType = toName(name);
        }

        @Override
        public void setDeclaringNodeType(String name) throws IllegalNameException, NamespaceException {
            declaringNodeType = toName(name);
        }

        @Override
        public void build() {
            if (requiredPrimaryTypes.isEmpty()) {
                requiredPrimaryTypes.add(NameConstants.NT_BASE);
            }

            ntd.childNodeDefs.add(new QNodeDefinitionImpl(
                    name,
                    declaringNodeType,
                    super.autocreate,
                    super.isMandatory,
                    super.onParent,
                    super.isProtected,
                    defaultPrimaryType,
                    requiredPrimaryTypes.toArray(new Name[requiredPrimaryTypes.size()]),
                    super.allowSns));
        }

    }


    private Name toName(String name) throws IllegalNameException, NamespaceException {
        Name n = resolver.getQName(name);
        String decodedLocalName = ISO9075.decode(n.getLocalName());
        return NAME_FACTORY.create(n.getNamespaceURI(), decodedLocalName);
    }

}
