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
package org.apache.jackrabbit.value;

import java.io.InputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

/**
 * This class implements the <code>ValueFactory</code> interface.
 *
 * @see javax.jcr.Session#getValueFactory()
 */
public abstract class AbstractValueFactory implements ValueFactory {

    /**
     * Constructs a <code>ValueFactory</code> object.
     */
    protected AbstractValueFactory() {
    }

    /**
     * Checks the format of the given string representing a path value.
     * Implementations must assert that the given value is a valid jcr path.
     *
     * @param pathValue
     * @throws javax.jcr.ValueFormatException If the given <code>pathValue</code>
     * isn't a valid jcr path.
     */
    protected abstract void checkPathFormat(String pathValue) throws ValueFormatException;

    /**
     * Checks the format of the given string representing a name value.
     * Implementations must assert that the given value is a valid jcr name.
     *
     * @param nameValue
     * @throws javax.jcr.ValueFormatException  If the given <code>pathValue</code>
     * isn't a valid jcr name.
     */
    protected abstract void checkNameFormat(String nameValue) throws ValueFormatException;

    //---------------------------------------------------------< ValueFactory >
    /**
     * {@inheritDoc}
     */
    public Value createValue(boolean value) {
        return new BooleanValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(Calendar value) {
        return new DateValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(double value) {
        return new DoubleValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(InputStream value) {
        try {
            return new BinaryValue(value);
        } finally {
            // JCR-2903
            try { value.close(); } catch (IOException ignore) {}
        }
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(long value) {
        return new LongValue(value);
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
    public Value createValue(String value) {
        return new StringValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(String value, int type)
            throws ValueFormatException {
        Value val;
        switch (type) {
            case PropertyType.STRING:
                val = new StringValue(value);
                break;
            case PropertyType.BOOLEAN:
                val = BooleanValue.valueOf(value);
                break;
            case PropertyType.DOUBLE:
                val = DoubleValue.valueOf(value);
                break;
            case PropertyType.LONG:
                val = LongValue.valueOf(value);
                break;
            case PropertyType.DECIMAL:
                val = DecimalValue.valueOf(value);
                break;
            case PropertyType.DATE:
                val = DateValue.valueOf(value);
                break;
            case PropertyType.NAME:
                checkNameFormat(value);
                val = NameValue.valueOf(value);
                break;
            case PropertyType.PATH:
                checkPathFormat(value);
                val = PathValue.valueOf(value);
                break;
            case PropertyType.URI:
                val = URIValue.valueOf(value);
                break;
            case PropertyType.REFERENCE:
                val = ReferenceValue.valueOf(value);
                break;
            case PropertyType.WEAKREFERENCE:
                val = WeakReferenceValue.valueOf(value);
                break;
            case PropertyType.BINARY:
                val = new BinaryValue(value);
                break;
            default:
                throw new IllegalArgumentException("Invalid type constant: " + type);
        }
        return val;
    }

    /**
     * {@inheritDoc}
     */
    public Binary createBinary(InputStream stream) throws RepositoryException {
        try {
            return new BinaryImpl(stream);
        } catch (IOException e) {
            throw new RepositoryException("failed to create Binary instance", e);
        } finally {
            // JCR-2903
            try { stream.close(); } catch (IOException ignore) {}
        }
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(Binary value) {
        return new BinaryValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(BigDecimal value) {
        return new DecimalValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(Node node, boolean weak)
            throws RepositoryException {
        if (weak) {
            return new WeakReferenceValue(node);
        } else {
            return new ReferenceValue(node);
        }
    }

}