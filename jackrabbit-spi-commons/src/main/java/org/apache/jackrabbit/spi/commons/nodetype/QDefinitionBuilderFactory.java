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

import org.apache.jackrabbit.commons.cnd.DefinitionBuilderFactory;
import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.util.ISO9075;

/**
 * This implementation of {@link DefinitionBuilderFactory} can be used with
 * the {@link CompactNodeTypeDefReader} to produce node type definitions of type
 * {@link QNodeTypeDefinition} and a namespace map of type {@link NamespaceMapping}.
 * It uses {@link QNodeTypeDefinitionBuilderImpl} for building node type definitions,
 * {@link QPropertyDefinitionBuilderImpl} for building property definitions, and
 * {@link QNodeDefinitionBuilderImpl} for building node definitions. It further uses
 * {@link NameFactoryImpl} for creating <code>Name</code>s and {@link QValueFactoryImpl} for
 * creating <code>QValue</code>s.
 */
public class QDefinitionBuilderFactory extends DefinitionBuilderFactory<QNodeTypeDefinition, NamespaceMapping> {

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
        return new QNodeTypeDefinitionBuilderImpl();
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

    private class QNodeTypeDefinitionBuilderImpl extends AbstractNodeTypeDefinitionBuilder<QNodeTypeDefinition> {
        private Name name;
        private final List<Name> supertypes = new ArrayList<Name>();
        private Name primaryItem;
        private final List<QPropertyDefinition> propertyDefs = new ArrayList<QPropertyDefinition>();
        private final List<QNodeDefinition> childNodeDefs = new ArrayList<QNodeDefinition>();

        @Override
        public AbstractNodeDefinitionBuilder<QNodeTypeDefinition> newNodeDefinitionBuilder() {
            return new QNodeDefinitionBuilderImpl(this);
        }

        @Override
        public AbstractPropertyDefinitionBuilder<QNodeTypeDefinition> newPropertyDefinitionBuilder() {
            return new QPropertyDefinitionBuilderImpl(this);
        }

        @Override
        public QNodeTypeDefinition build() {
            if (!isMixin && !NameConstants.NT_BASE.equals(name)) {
                supertypes.add(NameConstants.NT_BASE);
            }

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

    private class QPropertyDefinitionBuilderImpl extends AbstractPropertyDefinitionBuilder<QNodeTypeDefinition> {

        private final QNodeTypeDefinitionBuilderImpl ntd;
        private final QPropertyDefinitionBuilder builder = new QPropertyDefinitionBuilder();

        public QPropertyDefinitionBuilderImpl(QNodeTypeDefinitionBuilderImpl ntd) {
            super();
            this.ntd = ntd;
        }

        @Override
        public void setName(String name) throws RepositoryException {
            super.setName(name);
            if ("*".equals(name)) {
                builder.setName(NameConstants.ANY_NAME);
            }
            else {
                builder.setName(toName(name));
            }
        }

        @Override
        public void setRequiredType(int type) throws RepositoryException {
            super.setRequiredType(type);
            builder.setRequiredType(type);
        }

        @Override
        public void setMultiple(boolean isMultiple) throws RepositoryException {
            super.setMultiple(isMultiple);
            builder.setMultiple(isMultiple);
        }

        @Override
        public void setFullTextSearchable(boolean fullTextSearchable)
                throws RepositoryException {
            super.setFullTextSearchable(fullTextSearchable);
            builder.setFullTextSearchable(fullTextSearchable);
        }

        @Override
        public void setQueryOrderable(boolean queryOrderable)
                throws RepositoryException {
            super.setQueryOrderable(queryOrderable);
            builder.setQueryOrderable(queryOrderable);
        }

        @Override
        public void setAvailableQueryOperators(String[] queryOperators)
                throws RepositoryException {
            super.setAvailableQueryOperators(queryOperators);
            builder.setAvailableQueryOperators(queryOperators);
        }

        @Override
        public void setAutoCreated(boolean autocreate)
                throws RepositoryException {
            super.setAutoCreated(autocreate);
            builder.setAutoCreated(autocreate);
        }

        @Override
        public void setOnParentVersion(int onParent)
                throws RepositoryException {
            super.setOnParentVersion(onParent);
            builder.setOnParentVersion(onParent);
        }

        @Override
        public void setProtected(boolean isProtected)
                throws RepositoryException {
            super.setProtected(isProtected);
            builder.setProtected(isProtected);
        }

        @Override
        public void setMandatory(boolean isMandatory)
                throws RepositoryException {
            super.setMandatory(isMandatory);
            builder.setMandatory(isMandatory);
        }

        @Override
        public void addDefaultValues(String value) throws RepositoryException {
            builder.addDefaultValue(ValueFormat.getQValue(value, getRequiredType(), resolver, QValueFactoryImpl.getInstance()));
        }

        @Override
        public void addValueConstraint(String constraint) throws InvalidConstraintException {
            builder.addValueConstraint(ValueConstraint.create(getRequiredType(), constraint, resolver));
        }

        @Override
        public void setDeclaringNodeType(String name) throws IllegalNameException, NamespaceException {
            builder.setDeclaringNodeType(toName(name));
        }

        @Override
        public void build() throws IllegalStateException {
            ntd.propertyDefs.add(builder.build());
        }
    }

    private class QNodeDefinitionBuilderImpl extends AbstractNodeDefinitionBuilder<QNodeTypeDefinition> {
        private final QNodeTypeDefinitionBuilderImpl ntd;

        private final QNodeDefinitionBuilder builder = new QNodeDefinitionBuilder();

        public QNodeDefinitionBuilderImpl(QNodeTypeDefinitionBuilderImpl ntd) {
            super();
            this.ntd = ntd;
        }

        @Override
        public void setName(String name) throws RepositoryException {
            super.setName(name);
            if ("*".equals(name)) {
                builder.setName(NameConstants.ANY_NAME);
            }
            else {
                builder.setName(toName(name));
            }
        }

        @Override
        public void setAllowsSameNameSiblings(boolean allowSns)
                throws RepositoryException {
            super.setAllowsSameNameSiblings(allowSns);
            builder.setAllowsSameNameSiblings(allowSns);
        }

        @Override
        public void setAutoCreated(boolean autocreate)
                throws RepositoryException {
            super.setAutoCreated(autocreate);
            builder.setAutoCreated(autocreate);
        }

        @Override
        public void setOnParentVersion(int onParent)
                throws RepositoryException {
            super.setOnParentVersion(onParent);
            builder.setOnParentVersion(onParent);
        }

        @Override
        public void setProtected(boolean isProtected)
                throws RepositoryException {
            super.setProtected(isProtected);
            builder.setProtected(isProtected);
        }

        @Override
        public void setMandatory(boolean isMandatory)
                throws RepositoryException {
            super.setMandatory(isMandatory);
            builder.setMandatory(isMandatory);
        }

        @Override
        public void addRequiredPrimaryType(String name) throws IllegalNameException, NamespaceException {
            builder.addRequiredPrimaryType(toName(name));
        }

        @Override
        public void setDefaultPrimaryType(String name) throws IllegalNameException, NamespaceException {
            builder.setDefaultPrimaryType(toName(name));
        }

        @Override
        public void setDeclaringNodeType(String name) throws IllegalNameException, NamespaceException {
            builder.setDeclaringNodeType(toName(name));
        }

        @Override
        public void build() {
            ntd.childNodeDefs.add(builder.build());
        }
    }


    private Name toName(String name) throws IllegalNameException, NamespaceException {
        Name n = resolver.getQName(name);
        String decodedLocalName = ISO9075.decode(n.getLocalName());
        return NAME_FACTORY.create(n.getNamespaceURI(), decodedLocalName);
    }

}
