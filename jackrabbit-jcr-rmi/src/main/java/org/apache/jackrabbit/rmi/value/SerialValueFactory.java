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
package org.apache.jackrabbit.rmi.value;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

/**
 * The <code>SerialValueFactory</code> class is used in the RMI infrastructure
 * to create serializable <code>Value</code> instances on the client side.
 * <p>
 * This class works in conjunction with the implementations of the
 * <code>javax.jcr.Value</code> interface found in this package.
 * <p>
 * This class may be extended to overwrite any of the
 * <code>createXXXValue</code> methods to create instances of the respective
 * type of {@link Value} implementation. The
 * methods of the <code>ValueFactory</code> interface are declared final to
 * guard against breaking the rules.
 */
public class SerialValueFactory implements ValueFactory {

    /** The singleton value factory instance */
    private static final SerialValueFactory INSTANCE = new SerialValueFactory();

    /**
     * Returns the <code>ValueFactory</code> instance, which currently is a
     * singleton instance of this class.
     * <p>
     * Future revisions will support some kind of configuration to specify
     * which concrete class should be used.
     */
    public static final SerialValueFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Utility method for decorating an array of values. The returned array will
     * contain serializable value decorators for all the given values. Note that
     * the contents of the original values will only be copied when the
     * decorators are serialized.
     * <p>
     * If the given array is <code>null</code>, then an empty array is
     * returned.
     *
     * @param values the values to be decorated
     * @return array of decorated values
     * @throws RepositoryException if the values can not be serialized
     */
    public static Value[] makeSerialValueArray(Value[] values)
            throws RepositoryException {
        List<Value> serials = new ArrayList<Value>();
        if (values != null) {
            for (Value value : values) {
                if (value != null) {
                    serials.add(makeSerialValue(value));
                }
            }
        }
        return serials.toArray(new Value[serials.size()]);
    }

    /**
     * Utility method for decorating a value. Note that the contents of the
     * original values will only be copied when the decorators are serialized.
     * Null referenced and already serializable values are passed as-is.
     *
     * @param value the value to be decorated, or <code>null</code>
     * @return the decorated value, or <code>null</code>
     * @throws RepositoryException if the value can not be serialized
     */
    public static Value makeSerialValue(Value value) throws RepositoryException {
        // if the value is null or already serializable, just return it
        if (value == null || value instanceof Serializable) {
            return value;
        } else {
            return INSTANCE.createValue(value);
        }
    }

    /**
     * Utility method for converting an array of strings to serializable
     * string values.
     * <p>
     * If the given array is <code>null</code>, then an empty array is
     * returned.
     *
     * @param values the string array
     * @return array of string values
     */
    public static Value[] makeSerialValueArray(String[] values) {
        List<Value> serials = new ArrayList<Value>();
        if (values != null) {
            for (String value : values) {
                if (value != null) {
                    serials.add(INSTANCE.createValue(value));
                }
            }
        }
        return serials.toArray(new Value[serials.size()]);
    }

    /**
     * Default constructor only visible to extensions of this class. See
     * class comments for details.
     */
    protected SerialValueFactory() {
    }

    /** {@inheritDoc} */
    public Value createValue(String value) {
        return new StringValue(value);
    }

    /** {@inheritDoc} */
    public final Value createValue(String value, int type)
            throws ValueFormatException {
        try {
            return createValue(new StringValue(value), type);
        } catch (ValueFormatException e) {
            throw e;
        } catch (RepositoryException e) {
            throw new ValueFormatException(
                    "Unexpected error when creating value: " + value, e);
        }
    }

    private Value createValue(Value value) throws RepositoryException {
        return createValue(value, value.getType());
    }

    private Value createValue(Value value, int type)
            throws RepositoryException {
        switch (type) {
        case PropertyType.BINARY:
            Binary binary = value.getBinary();
            try {
                return createValue(binary.getStream());
            } finally {
                binary.dispose();
            }
        case PropertyType.BOOLEAN:
            return createValue(value.getBoolean());
        case PropertyType.DATE:
            return createValue(value.getDate());
        case PropertyType.DECIMAL:
            return createValue(value.getDecimal());
        case PropertyType.DOUBLE:
            return createValue(value.getDouble());
        case PropertyType.LONG:
            return createValue(value.getLong());
        case PropertyType.NAME:
            return new NameValue(value.getString());
        case PropertyType.PATH:
            return new PathValue(value.getString());
        case PropertyType.REFERENCE:
            return new ReferenceValue(value.getString());
        case PropertyType.STRING:
            return createValue(value.getString());
        default:
            throw new ValueFormatException("Unknown value type " + type);
        }
    }

    /** {@inheritDoc} */
    public final Value createValue(long value) {
        return new LongValue(value);
    }

    /** {@inheritDoc} */
    public final Value createValue(double value) {
        return new DoubleValue(value);
    }

    /** {@inheritDoc} */
    public final Value createValue(boolean value) {
        return new BooleanValue(value);
    }

    /** {@inheritDoc} */
    public Value createValue(BigDecimal value) {
        return new DecimalValue(value);
    }

    /** {@inheritDoc} */
    public final Value createValue(Calendar value) {
        return new DateValue(value);
    }

    /** {@inheritDoc} */
    public final Value createValue(InputStream value) {
        try {
            return createValue(createBinary(value));
        } catch (RepositoryException e) {
            throw new RuntimeException("Unable to create a binary value", e);
        }
    }

    /** {@inheritDoc} */
    public final Value createValue(Node value) throws RepositoryException {
        return createValue(value.getUUID(), PropertyType.REFERENCE);
    }

    public Binary createBinary(InputStream stream) throws RepositoryException {
        try {
            try {
                return new SerializableBinary(stream);
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            throw new RepositoryException("Unable to read binary stream", e);
        }
    }

    public Value createValue(Binary value) {
        return new BinaryValue(value);
    }

    public Value createValue(Node value, boolean weak)
            throws RepositoryException {
        return new ReferenceValue(value.getUUID());
    }

}
