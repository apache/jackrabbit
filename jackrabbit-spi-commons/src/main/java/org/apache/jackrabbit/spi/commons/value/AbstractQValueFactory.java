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

import java.util.Calendar;
import java.util.UUID;
import java.math.BigDecimal;
import java.net.URI;
import java.io.UnsupportedEncodingException;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.util.ISO8601;

/**
 * <code>AbstractQValueFactory</code>...
 */
public abstract class AbstractQValueFactory implements QValueFactory {

    /**
     * the default encoding
     */
    public static final String DEFAULT_ENCODING = "UTF-8";

    protected static final PathFactory PATH_FACTORY = PathFactoryImpl.getInstance();
    protected static final NameFactory NAME_FACTORY = NameFactoryImpl.getInstance();


    //------------------------------------------------------< QValueFactory >---
    /**
     * @see QValueFactory#computeAutoValues(org.apache.jackrabbit.spi.QPropertyDefinition)
     */
    public QValue[] computeAutoValues(QPropertyDefinition propertyDefinition) throws RepositoryException {
        Name declaringNT = propertyDefinition.getDeclaringNodeType();
        Name name = propertyDefinition.getName();

        if (NameConstants.JCR_UUID.equals(name)
                && NameConstants.MIX_REFERENCEABLE.equals(declaringNT)) {
            // jcr:uuid property of a mix:referenceable
            return new QValue[]{create(UUID.randomUUID().toString(), PropertyType.STRING)};

        } else {
            throw new RepositoryException("createFromDefinition not implemented for: " + name);
        }
    }

    /**
     * @see QValueFactory#create(String, int)
     */
    public QValue create(String value, int type) throws RepositoryException {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }

        try {
            switch (type) {
                case PropertyType.BOOLEAN:
                    return create(Boolean.valueOf(value));
                case PropertyType.DATE: {
                        Calendar cal = ISO8601.parse(value);
                        if (cal == null) {
                            throw new ValueFormatException("not a valid date: " + value);
                        }
                        return create(cal);
                    }
                case PropertyType.DOUBLE:
                    return create(Double.valueOf(value));
                case PropertyType.LONG:
                    return create(Long.valueOf(value));
                case PropertyType.DECIMAL:
                    return create(new BigDecimal(value));
                case PropertyType.URI:
                    return create(URI.create(value));
                case PropertyType.PATH:
                    return create(PATH_FACTORY.create(value));
                case PropertyType.NAME:
                    return create(NAME_FACTORY.create(value));
                case PropertyType.STRING:
                    return createString(value);
                case PropertyType.REFERENCE:
                    return createReference(value, false);
                case PropertyType.WEAKREFERENCE:
                    return createReference(value, true);
                case PropertyType.BINARY:
                    return create(value.getBytes(DEFAULT_ENCODING));
                // default: invalid type specified -> see below.
            }
        } catch (IllegalArgumentException ex) {
            // given String value cannot be converted to Long/Double/Path/Name
            throw new ValueFormatException(ex);
        } catch (UnsupportedEncodingException ex) {
            throw new RepositoryException(ex);
        }

        // invalid type specified:
        throw new IllegalArgumentException("illegal type " + type);
    }

    /**
     * @see QValueFactory#create(Calendar)
     */
    public QValue create(Calendar value) throws RepositoryException {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new DefaultQValue(value);
    }

    /**
     * @see QValueFactory#create(double)
     */
    public QValue create(double value) throws RepositoryException {
        return new DefaultQValue(value);
    }

    /**
     * @see QValueFactory#create(long)
     */
    public QValue create(long value) throws RepositoryException {
        return new DefaultQValue(value);
    }

    /**
     * @see QValueFactory#create(boolean)
     */
    public QValue create(boolean value) throws RepositoryException {
        if (value) {
            return DefaultQValue.TRUE;
        } else {
            return DefaultQValue.FALSE;
        }
    }

    /**
     * @see QValueFactory#create(Name)
     */
    public QValue create(Name value) throws RepositoryException {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new DefaultQValue(value);
    }

    /**
     * @see QValueFactory#create(Path)
     */
    public QValue create(Path value) throws RepositoryException {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new DefaultQValue(value);
    }

    /**
     * @see QValueFactory#create(URI)
     */
    public QValue create(URI value) throws RepositoryException {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new DefaultQValue(value);
    }

    /**
     * @see QValueFactory#create(URI)
     */
    public QValue create(BigDecimal value) throws RepositoryException {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new DefaultQValue(value);
    }

    /**
     * Creates a new QValue of type STRING.
     *
     * @param value the string value.
     * @return a new QValue.
     */
    protected QValue createString(String value) {
        return new DefaultQValue(value, PropertyType.STRING);
    }

    /**
     * Creates a new QValue of type REFERENCE or WEAKREFERENCE.
     *
     * @param ref the reference value.
     * @param weak whether the reference is weak.
     * @return a new QValue.
     */
    protected QValue createReference(String ref, boolean weak) {
        if (weak) {
            return new DefaultQValue(ref, PropertyType.WEAKREFERENCE);
        } else {
            return new DefaultQValue(ref, PropertyType.REFERENCE);
        }
    }
}