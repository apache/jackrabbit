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

import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;

import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.PropertyType;

/**
 * <code>QPropertyDefinitionImpl</code> implements a qualified property
 * definition based on a JCR {@link javax.jcr.nodetype.PropertyDefinition}.
 */
class QPropertyDefinitionImpl
        extends org.apache.jackrabbit.spi.commons.QPropertyDefinitionImpl {

    /**
     * Creates a new qualified property definition based on
     * <code>propDef</code>.
     *
     * @param propDef       the JCR property definition.
     * @param resolver
     * @param qValueFactory the QValue factory.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>propDef</code>.
     */
    QPropertyDefinitionImpl(PropertyDefinition propDef,
                            NamePathResolver resolver,
                            QValueFactory qValueFactory)
            throws RepositoryException, NameException {
        super(propDef.getName().equals(ANY_NAME.getLocalName()) ? ANY_NAME : resolver.getQName(propDef.getName()),
                resolver.getQName(propDef.getDeclaringNodeType().getName()),
                propDef.isAutoCreated(), propDef.isMandatory(),
                propDef.getOnParentVersion(), propDef.isProtected(),
                convertValues(propDef.getDefaultValues(), resolver, qValueFactory),
                propDef.isMultiple(), propDef.getRequiredType(),
                convertConstraints(propDef.getValueConstraints(), resolver, qValueFactory, propDef.getRequiredType()));
    }

    /**
     * Convers JCR {@link Value}s to {@link QValue}s.
     *
     * @param values     the JCR values.
     * @param resolver
     * @param factory    the QValue factory.
     * @return the converted values.
     * @throws RepositoryException if an error occurs while converting the
     *                             values.
     */
    private static QValue[] convertValues(Value[] values,
                                          NamePathResolver resolver,
                                          QValueFactory factory)
            throws RepositoryException {
        QValue[] defaultValues = null;
        if (values != null) {
            defaultValues = new QValue[values.length];
            for (int i = 0; i < values.length; i++) {
                defaultValues[i] = ValueFormat.getQValue(values[i], resolver, factory);
            }
        }
        return defaultValues;
    }

    /**
     * Makes sure name and path constraints are parsed correctly using the
     * namespace resolver.
     *
     * @param constraints  the constraint strings from the JCR property
     *                     definition.
     * @param resolver
     * @param factory      the QValueFactory.
     * @param requiredType the required type of the property definition.
     * @return SPI formatted constraint strings.
     * @throws RepositoryException if an error occurs while converting the
     *                             constraint strings.
     */
    private static String[] convertConstraints(String[] constraints,
                                               NamePathResolver resolver,
                                               QValueFactory factory,
                                               int requiredType)
            throws RepositoryException {
        if (requiredType == PropertyType.REFERENCE
                || requiredType == PropertyType.NAME
                || requiredType == PropertyType.PATH) {
            int type = requiredType == PropertyType.REFERENCE ? PropertyType.NAME : requiredType;
            for (int i = 0; i < constraints.length; i++) {
                constraints[i] = ValueFormat.getQValue(
                        constraints[i], type, resolver, factory).getString();
            }
        }
        return constraints;
    }
}
