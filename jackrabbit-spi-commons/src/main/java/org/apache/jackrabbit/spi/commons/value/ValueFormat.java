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
package org.apache.jackrabbit.spi.commons.value;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>ValueFormat</code>...
 */
public class ValueFormat {

    /**
     *
     * @param jcrValue
     * @param resolver
     * @param factory
     * @return
     * @throws RepositoryException
     */
    public static QValue getQValue(Value jcrValue, NamePathResolver resolver,
                                   QValueFactory factory) throws RepositoryException {
        if (jcrValue == null) {
            throw new IllegalArgumentException("null value");
        } else if (jcrValue instanceof QValueValue) {
            return ((QValueValue)jcrValue).getQValue();
        } else if (jcrValue.getType() == PropertyType.BINARY) {
            // TODO: jsr 283 binary property conversion
            try {
                //return factory.create(jcrValue.getBinary());
                return factory.create(jcrValue.getStream());
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        } else if (jcrValue.getType() == PropertyType.DATE) {
            return factory.create(jcrValue.getDate());
        } else if (jcrValue.getType() == PropertyType.DOUBLE) {
            return factory.create(jcrValue.getDouble());
        } else if (jcrValue.getType() == PropertyType.LONG) {
            return factory.create(jcrValue.getLong());
        } else if (jcrValue.getType() == PropertyType.DECIMAL) {
            return factory.create(jcrValue.getDecimal());
        } else {
            return getQValue(jcrValue.getString(), jcrValue.getType(), resolver, factory);
        }
    }

    /**
     *
     * @param jcrValues
     * @param resolver
     * @param factory
     * @return
     * @throws RepositoryException
     */
    public static QValue[] getQValues(Value[] jcrValues,
                                      NamePathResolver resolver,
                                      QValueFactory factory) throws RepositoryException {
        if (jcrValues == null) {
            throw new IllegalArgumentException("null value");
        }
        List<QValue> qValues = new ArrayList<QValue>();
        for (Value jcrValue : jcrValues) {
            if (jcrValue != null) {
                qValues.add(getQValue(jcrValue, resolver, factory));
            }
        }
        return qValues.toArray(new QValue[qValues.size()]);
    }

    /**
     *
     * @param jcrValue
     * @param propertyType
     * @param resolver
     * @param factory
     * @return
     * @throws RepositoryException
     */
    public static QValue getQValue(String jcrValue, int propertyType,
                                   NamePathResolver resolver,
                                   QValueFactory factory) throws RepositoryException {
        QValue qValue;
        switch (propertyType) {
            case PropertyType.STRING:
            case PropertyType.BOOLEAN:
            case PropertyType.DOUBLE:
            case PropertyType.LONG:
            case PropertyType.DECIMAL:
            case PropertyType.DATE:
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
            case PropertyType.URI:
                qValue = factory.create(jcrValue, propertyType);
                break;
            case PropertyType.BINARY:
                qValue = factory.create(jcrValue.getBytes());
                break;
            case PropertyType.NAME:
                Name qName = resolver.getQName(jcrValue);
                qValue = factory.create(qName);
                break;
            case PropertyType.PATH:
                Path qPath = resolver.getQPath(jcrValue, false);
                qValue = factory.create(qPath);
                break;
            default:
                throw new IllegalArgumentException("Invalid property type.");
        }
        return qValue;
    }

    /**
     * @param value
     * @param resolver
     * @param factory
     * @return the JCR value created from the given <code>QValue</code>.
     * @throws RepositoryException
     */
    public static Value getJCRValue(QValue value,
                                    NamePathResolver resolver,
                                    ValueFactory factory) throws RepositoryException {
        if (factory instanceof ValueFactoryQImpl) {
            return ((ValueFactoryQImpl)factory).createValue(value);
        } else {
            Value jcrValue;
            int propertyType = value.getType();
            switch (propertyType) {
                case PropertyType.STRING:
                case PropertyType.REFERENCE:
                case PropertyType.WEAKREFERENCE:
                case PropertyType.URI:
                    jcrValue = factory.createValue(value.getString(), propertyType);
                    break;
                case PropertyType.PATH:
                    Path qPath = value.getPath();
                    jcrValue = factory.createValue(resolver.getJCRPath(qPath), propertyType);
                    break;
                case PropertyType.NAME:
                    Name qName = value.getName();
                    jcrValue = factory.createValue(resolver.getJCRName(qName), propertyType);
                    break;
                case PropertyType.BOOLEAN:
                    jcrValue = factory.createValue(value.getBoolean());
                    break;
                case PropertyType.BINARY:
                    jcrValue = factory.createValue(value.getBinary());
                    break;
                case PropertyType.DATE:
                    jcrValue = factory.createValue(value.getCalendar());
                    break;
                case PropertyType.DOUBLE:
                    jcrValue = factory.createValue(value.getDouble());
                    break;
                case PropertyType.LONG:
                    jcrValue = factory.createValue(value.getLong());
                    break;
                case PropertyType.DECIMAL:
                    jcrValue = factory.createValue(value.getDecimal());
                    break;
                default:
                    throw new RepositoryException("illegal internal value type");
            }
            return jcrValue;
        }
    }

    /**
     * Returns the JCR string representation of the given <code>QValue</code>.
     * This method is a shortcut for
     * {@link #getJCRValue(QValue, NamePathResolver, ValueFactory)} followed by
     * {@link Value#getString()}.
     *
     * @param value
     * @param resolver
     * @return the JCR String representation for the given <code>QValue</code>.
     * @throws RepositoryException
     */
    public static String getJCRString(QValue value,
                                      NamePathResolver resolver) throws RepositoryException {
        String jcrString;
        int propertyType = value.getType();
        switch (propertyType) {
            case PropertyType.STRING:
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
            case PropertyType.URI:
            case PropertyType.BOOLEAN:
            case PropertyType.DATE:
            case PropertyType.DOUBLE:
            case PropertyType.LONG:
            case PropertyType.DECIMAL:
            case PropertyType.BINARY:
                jcrString = value.getString();
                break;
            case PropertyType.PATH:
                Path qPath = value.getPath();
                jcrString = resolver.getJCRPath(qPath);
                break;
            case PropertyType.NAME:
                Name qName = value.getName();
                jcrString = resolver.getJCRName(qName);
                break;
            default:
                throw new RepositoryException("illegal internal value type");
        }
        return jcrString;
    }
}
