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
package org.apache.jackrabbit.core.value;

import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.AbstractQValueFactory;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.uuid.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.FileInputStream;
import java.util.Calendar;
import java.math.BigDecimal;
import java.net.URI;

/**
 * <code>InternalValueFactory</code>...
 */
public final class InternalValueFactory extends AbstractQValueFactory {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(InternalValueFactory.class);

    private static final QValueFactory INSTANCE = new InternalValueFactory(null);

    private final DataStore store;

    InternalValueFactory(DataStore store) {
        this.store = store;
    }
    
    public static QValueFactory getInstance() {
        return INSTANCE;
    }

    public QValue create(String value, int type) throws ValueFormatException, RepositoryException {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        try {
            switch (type) {
                case PropertyType.BOOLEAN:
                    return InternalValue.create(Boolean.valueOf(value).booleanValue());
                case PropertyType.DATE: {
                    Calendar cal = ISO8601.parse(value);
                    if (cal == null) {
                        throw new ValueFormatException("not a valid date: " + value);
                    }
                    return InternalValue.create(cal);
                }
                case PropertyType.DOUBLE:
                    return InternalValue.create(Double.parseDouble(value));
                case PropertyType.LONG:
                    return InternalValue.create(Long.parseLong(value));
                case PropertyType.DECIMAL:
                    return InternalValue.create(new BigDecimal(value));
                case PropertyType.PATH:
                    return InternalValue.create(PathFactoryImpl.getInstance().create(value));
                case PropertyType.NAME:
                    return InternalValue.create(NameFactoryImpl.getInstance().create(value));
                case PropertyType.STRING:
                    return InternalValue.create(value);
                case PropertyType.URI:
                    return InternalValue.create(URI.create(value));
                case PropertyType.REFERENCE:
                    return InternalValue.create(new UUID(value));
                case PropertyType.WEAKREFERENCE:
                    return InternalValue.create(new UUID(value), true);
                case PropertyType.BINARY:
                    return InternalValue.create(value.getBytes("UTF-8"));
                // default: invalid type specified -> see below.
            }
        } catch (NumberFormatException ex) {
            // given String value cannot be converted to Decimal
            throw new ValueFormatException(ex);
        } catch (IllegalArgumentException ex) {
            // given String value cannot be converted to Long/Double/Path/Name
            throw new ValueFormatException(ex);
        } catch (UnsupportedEncodingException ex) {
            throw new RepositoryException(ex);
        }

        // invalid type specified:
        throw new IllegalArgumentException("illegal type " + type);    }

    public QValue create(Calendar value) throws RepositoryException {
        return InternalValue.create(value);
    }

    public QValue create(double value) throws RepositoryException {
        return InternalValue.create(value);
    }

    public QValue create(long value) throws RepositoryException {
        return InternalValue.create(value);
    }

    public QValue create(boolean value) throws RepositoryException {
        return InternalValue.create(value);
    }

    public QValue create(Name value) throws RepositoryException {
        return InternalValue.create(value);
    }

    public QValue create(Path value) throws RepositoryException {
        return InternalValue.create(value);
    }

    public QValue create(URI value) throws RepositoryException {
        return InternalValue.create(value);
    }

    public QValue create(BigDecimal value) throws RepositoryException {
        return InternalValue.create(value);
    }

    public QValue create(byte[] value) throws RepositoryException {
        return InternalValue.create(value);
    }

    public QValue create(InputStream value) throws RepositoryException, IOException {
        return InternalValue.create(value, store);
    }

    public QValue create(File value) throws RepositoryException, IOException {
        if (InternalValue.USE_DATA_STORE) {
            InputStream in = new FileInputStream(value);
            return InternalValue.createTemporary(in);
        } else {
            return InternalValue.create(value);
        }
    }
}