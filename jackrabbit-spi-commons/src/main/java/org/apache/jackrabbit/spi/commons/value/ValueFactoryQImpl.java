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

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.util.ISO8601;

/**
 * This class implements the <code>ValueFactory</code> interface,
 * wrapping an existing SPI <code>QValueFactory</code> and a
 * <code>NamePathResolver</code>.
 *
 * @see ValueFactory
 * @see QValueFactory
 */
public class ValueFactoryQImpl implements ValueFactory {

    private final QValueFactory qfactory;
    private final NamePathResolver resolver;

    /**
     * Constructs a new <code>ValueFactoryQImpl</code> based
     * on an existing SPI <code>QValueFactory</code> and a
     * <code>NamePathResolver</code>.
     * @param qfactory wrapped <code>QValueFactory</code>
     * @param resolver wrapped <code>NamePathResolver</code>
     */
    public ValueFactoryQImpl(QValueFactory qfactory, NamePathResolver resolver) {
        this.qfactory = qfactory;
        this.resolver = resolver;
    }

    /**
     * The <code>QValueFactory</code> that is wrapped by this <code>ValueFactory</code>
     * instance.
     *
     * @return qfactory The <code>QValueFactory</code> wrapped by this instance.
     */
    public QValueFactory getQValueFactory() {
        return qfactory;
    }

    /**
     * Create a new <code>Value</code> based on an existing
     * <code>QValue</code>
     * @param qvalue existing <code>QValue</code>
     * @return a <code>Value</code> representing the <code>QValue</code>
     */
    public Value createValue(QValue qvalue) {
        return new QValueValue(qvalue, resolver);
    }

    //---------------------------------------------------------< ValueFactory >

    /**
     * {@inheritDoc}
     */
    public Value createValue(String value) {
        try {
            QValue qvalue = qfactory.create(value, PropertyType.STRING);
            return new QValueValue(qvalue, resolver);
        } catch (RepositoryException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(long value) {
        try {
            QValue qvalue = qfactory.create(value);
            return new QValueValue(qvalue, resolver);
        } catch (RepositoryException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(double value) {
        try {
            QValue qvalue = qfactory.create(value);
            return new QValueValue(qvalue, resolver);
        } catch (RepositoryException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(boolean value) {
        try {
            QValue qvalue = qfactory.create(value);
            return new QValueValue(qvalue, resolver);
        } catch (RepositoryException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(Calendar value) {
        try {
            ISO8601.getYear(value);
            QValue qvalue = qfactory.create(value);
            return new QValueValue(qvalue, resolver);
        } catch (RepositoryException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(InputStream value) {
        try {
            try {
                QValue qvalue = qfactory.create(value);
                return new QValueValue(qvalue, resolver);
            } finally {
                value.close(); // JCR-2903
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (RepositoryException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(Node value) throws RepositoryException {
        return createValue(value, false);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(String value, int type) throws ValueFormatException {
        try {
            QValue qvalue;

            if (type == PropertyType.NAME) {
                Name name = resolver.getQName(value);
                qvalue = qfactory.create(name);
            } else if (type == PropertyType.PATH) {
                Path path = resolver.getQPath(value, false);
                qvalue = qfactory.create(path);
            } else {
                qvalue = qfactory.create(value, type);
            }

            return new QValueValue(qvalue, resolver);
        } catch (IllegalNameException ex) {
            throw new ValueFormatException(ex);
        } catch (MalformedPathException ex) {
            throw new ValueFormatException(ex);
        } catch (NamespaceException ex) {
            throw new ValueFormatException(ex);
        } catch (ValueFormatException ex) {
            throw ex;
        } catch (RepositoryException ex) {
            throw new ValueFormatException(ex);
        }
    }

    public Binary createBinary(InputStream stream) throws RepositoryException {
        // TODO review/optimize/refactor
        try {
            try {
                QValue qvalue = qfactory.create(stream);
                return qvalue.getBinary();
            } finally {
                stream.close(); // JCR-2903
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (RepositoryException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Value createValue(Binary value) {
        // TODO review/optimize/refactor
        try {
            return createValue(value.getStream());
        } catch (RepositoryException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Value createValue(BigDecimal value) {
        try {
            QValue qvalue = qfactory.create(value);
            return new QValueValue(qvalue, resolver);
        } catch (RepositoryException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Value createValue(Node value, boolean weak) throws RepositoryException {
        QValue qvalue = qfactory.create(value.getUUID(), weak ? PropertyType.WEAKREFERENCE : PropertyType.REFERENCE);
        return new QValueValue(qvalue, resolver);
    }

}
