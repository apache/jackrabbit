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
package org.apache.jackrabbit.spi.commons.nodetype.compact;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.QNodeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.QPropertyDefinitionImpl;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.nodetype.InvalidConstraintException;
import org.apache.jackrabbit.spi.commons.nodetype.ValueConstraint;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;

/**
 * Default implementations of a {@link QNodeTypeDefinitionsBuilder}. This implementations uses
 * {@link QNodeTypeDefinitionBuilderImpl} for building node type definitions,
 * {@link QPropertyDefinitionBuilderImpl} for building property definitions, and
 * {@link QNodeDefinitionBuilderImpl} for building node definitions. It further uses
 * {@link NameFactoryImpl} for creating <code>Name</code>s and {@link QValueFactoryImpl} for
 * creating <code>QValue</code>s.
 */
public class QNodeTypeDefinitionsBuilderImpl extends QNodeTypeDefinitionsBuilder {

    private static final NameFactory NAME_FACTORY = NameFactoryImpl.getInstance();

    public QNodeTypeDefinitionBuilder newQNodeTypeDefinition() {
        return new QNodeTypeDefinitionBuilderImpl();
    }

    public Name createName(String namespaceURI, String localName) {
        return NAME_FACTORY.create(namespaceURI, localName);
    }

    /**
     * Default implementation of a {@link QNodeTypeDefinitionBuilder}.
     */
    public class QNodeTypeDefinitionBuilderImpl extends QNodeTypeDefinitionBuilder {

        public QNodeDefinitionBuilder newQNodeDefinitionBuilder() {
            return new QNodeDefinitionBuilderImpl();
        }

        public QPropertyDefinitionBuilder newQPropertyDefinition() {
            return new QPropertyDefinitionBuilderImpl();
        }

        public QNodeTypeDefinition build() {
            return new QNodeTypeDefinitionImpl(
                    this.getName(),
                    this.getSuperTypes(),
                    this.getMixin(),
                    this.getOrderableChildNodes(),
                    this.getPrimaryItemName(),
                    this.getPropertyDefs(),
                    this.getChildNodeDefs());
        }

    }

    /**
     * Default implementation of a {@link QPropertyDefinitionBuilder}.
     */
    public class QPropertyDefinitionBuilderImpl extends QPropertyDefinitionBuilder {

        public QValue createValue(String value, NamePathResolver resolver)
                throws ValueFormatException, RepositoryException {

            return ValueFormat.getQValue(value, getRequiredType(), resolver, QValueFactoryImpl
                    .getInstance());
        }

        public String createValueConstraint(String constraint, NamePathResolver resolver)
                throws InvalidConstraintException {

            return ValueConstraint.create(getRequiredType(), constraint, resolver).getQualifiedDefinition();
        }

        public QPropertyDefinition build() {
            return new QPropertyDefinitionImpl(
                    this.getName(),
                    this.getDeclaringNodeType(),
                    this.getAutoCreated(),
                    this.getMandatory(),
                    this.getOnParentVersion(),
                    this.getProtected(),
                    this.getDefaultValues(),
                    this.getMultiple(),
                    this.getRequiredType(),
                    this.getValueConstraints());
        }

    }

    /**
     * Default implementation of a {@link QNodeDefinitionBuilder}.
     */
    public class QNodeDefinitionBuilderImpl extends QNodeDefinitionBuilder {

        public QNodeDefinition build() {
            return new QNodeDefinitionImpl(
                    this.getName(),
                    this.getDeclaringNodeType(),
                    this.getAutoCreated(),
                    this.getMandatory(),
                    this.getOnParentVersion(),
                    this.getProtected(),
                    this.getDefaultPrimaryType(),
                    this.getRequiredPrimaryTypes(),
                    this.getAllowsSameNameSiblings());
        }

    }

}
