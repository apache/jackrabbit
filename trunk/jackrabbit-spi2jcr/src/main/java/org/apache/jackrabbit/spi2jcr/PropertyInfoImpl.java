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

import javax.jcr.RepositoryException;
import javax.jcr.Property;
import javax.jcr.Value;

/**
 * <code>PropertyInfoImpl</code> implements a <code>PropertyInfo</code> on top
 * of a JCR repository.
 */
class PropertyInfoImpl
        extends org.apache.jackrabbit.spi.commons.PropertyInfoImpl {

    /**
     * Creates a new property info for the given <code>property</code>.
     *
     * @param property      the JCR property.
     * @param idFactory     the id factory.
     * @param resolver
     * @param qValueFactory the QValue factory.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>property</code>.
     */
    public PropertyInfoImpl(Property property,
                            IdFactoryImpl idFactory,
                            NamePathResolver resolver,
                            QValueFactory qValueFactory) throws RepositoryException, NameException {
        super(resolver.getQPath(property.getPath()),
                idFactory.createPropertyId(property, resolver),
                property.getType(), property.isMultiple(),
                getValues(property, resolver, qValueFactory));
    }

    /**
     * Returns the QValues for the <code>property</code>.
     *
     * @param property   the property.
     * @param resolver   the name and path resolver.
     * @param factory    the value factory.
     * @return the values of the property.
     * @throws RepositoryException if an error occurs while reading the values.
     */
    private static QValue[] getValues(Property property,
                                      NamePathResolver resolver,
                                      QValueFactory factory) throws RepositoryException {
        boolean isMultiValued = property.isMultiple();
        QValue[] values;
        if (isMultiValued) {
            Value[] jcrValues = property.getValues();
            values = new QValue[jcrValues.length];
            for (int i = 0; i < jcrValues.length; i++) {
                values[i] = ValueFormat.getQValue(jcrValues[i], resolver, factory);
            }
        } else {
            values = new QValue[]{
                    ValueFormat.getQValue(property.getValue(), resolver, factory)
            };
        }
        return values;
    }
}
