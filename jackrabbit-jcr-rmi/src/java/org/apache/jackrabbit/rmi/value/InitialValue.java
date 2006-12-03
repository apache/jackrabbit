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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * Initial value state. This class implements the non-committed
 * value state as a part of the State design pattern (GoF) used
 * by this package. The value getters of this class perform the
 * stream/non-stream state transition by changing the state
 * reference of the containing {@link SerialValue SerialValue}
 * instance. Once the state change is complete (and the InitialValue
 * state is no longer referenced by the SerialValue instance), the
 * calling SerialValue getter method is restarted to get the actual
 * underlying value.
 *
 * @see SerialValue
 */
final class InitialValue implements Serializable, StatefulValue {

    /** The serial version UID */
    private static final long serialVersionUID = -3277781963593015976L;

    /** The containing general value instance. */
    private final SerialValue general;

    /** The underlying concrete value instance. */
    private final StatefulValue value;

    /**
     * Creates an initial value state instance.
     *
     * @param general containing general value
     * @param value   underlying concrete value
     */
    InitialValue(SerialValue general, StatefulValue value) {
        this.general = general;
        this.value = value;
    }

    /**
     * Converts the given binary stream to a string. This utility method
     * is used to convert stream values to non-stream values.
     * <p>
     * Note that a RepositoryException is thrown instead of a
     * ValueFormatException if the stream can not be converted to a string.
     * This is because the string constructor used does not report encoding
     * problems.
     *
     * @param input binary stream
     * @return string value
     * @throws ValueFormatException if a stream decoding problem occurs
     */
    private static String toString(InputStream input) throws ValueFormatException, RepositoryException {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            for (int n = input.read(buffer); n != -1; n = input.read(buffer)) {
                output.write(buffer, 0, n);
            }
            return new String(output.toByteArray(), "UTF-8");
        } catch (IOException e) {
            // Throwing a RepositoryException instead of a
            // ValueFormatException because the problem is probably
            // caused by some IO problem or another similar issue.
            // The String(byte[], String) constructor does not report
            // encoding problems. (TODO use a more detailed decoding mechanism)
            throw new ValueFormatException(
                    "Failed to convert from binary to string value", e);
        }
    }

    /**
     * Commits the value into the stream state and returns the stream
     * representation of the value. Implemented by changing the state
     * reference of the containing general value and restarting the
     * general value getter method.
     *
     * @return stream value
     */
    public InputStream getStream() throws ValueFormatException, RepositoryException {
        StatefulValue realValue;
        if (getType() != PropertyType.BINARY) {
            realValue = SerialValueFactory.getInstance().createBinaryValue(value.getString());
        } else {
            realValue = value;
        }

        general.setValue(realValue);
        return realValue.getStream();
    }

    /**
     * Commits the value into the non-stream state and returns the string
     * representation of the value. Implemented by changing the state
     * reference of the containing general value and restarting the
     * general value getter method.
     *
     * @return string value
     *
     * @throws ValueFormatException if conversion to string is not possible
     * @throws IllegalStateException not thrown by proper instances
     * @throws RepositoryException if another error occurs
     * @see Value#getString()
     */
    public String getString() throws ValueFormatException, RepositoryException {
        StatefulValue realValue;
        if (getType() == PropertyType.BINARY) {
            realValue = new StringValue(toString(value.getStream()));
        } else {
            realValue = value;
        }

        general.setValue(realValue);
        return realValue.getString();
    }

    /**
     * Commits the value into the non-stream state and returns the long
     * representation of the value. Implemented by changing the state
     * reference of the containing general value and restarting the
     * general value getter method.
     *
     * @return long value
     * @throws ValueFormatException if conversion to long is not possible
     * @throws IllegalStateException not thrown by proper instances
     * @throws RepositoryException if another error occurs
     * @see Value#getLong()
     */
    public long getLong() throws ValueFormatException, RepositoryException {
        StatefulValue realValue;
        if (getType() == PropertyType.BINARY) {
            realValue = SerialValueFactory.getInstance().createLongValue(toString(value.getStream()));
        } else {
            realValue = value;
        }

        general.setValue(realValue);
        return realValue.getLong();
    }

    /**
     * Commits the value into the non-stream state and returns the double
     * representation of the value. Implemented by changing the state
     * reference of the containing general value and restarting the
     * general value getter method.
     *
     * @return double value
     * @throws ValueFormatException if conversion to double is not possible
     * @throws IllegalStateException not thrown by proper instances
     * @throws RepositoryException if another error occurs
     * @see Value#getDouble()
     */
    public double getDouble() throws ValueFormatException, RepositoryException {
        StatefulValue realValue;
        if (getType() == PropertyType.BINARY) {
            realValue = SerialValueFactory.getInstance().createDoubleValue(toString(value.getStream()));
        } else {
            realValue = value;
        }

        general.setValue(realValue);
        return realValue.getDouble();
    }

    /**
     * Commits the value into the non-stream state and returns the date
     * representation of the value. Implemented by changing the state
     * reference of the containing general value and restarting the
     * general value getter method.
     *
     * @return date value
     * @throws ValueFormatException if conversion to date is not possible
     * @throws IllegalStateException not thrown by proper instances
     * @throws RepositoryException if another error occurs
     * @see Value#getDate()
     */
    public Calendar getDate() throws  ValueFormatException, RepositoryException {
        StatefulValue realValue;
        if (getType() == PropertyType.BINARY) {
            realValue = SerialValueFactory.getInstance().createDateValue(toString(value.getStream()));
        } else {
            realValue = value;
        }

        general.setValue(realValue);
        return realValue.getDate();
    }

    /**
     * Commits the value into the non-stream state and returns the long
     * representation of the value. Implemented by changing the state
     * reference of the containing general value and restarting the
     * general value getter method.
     *
     * @return boolean value
     * @throws ValueFormatException if conversion to boolean is not possible
     * @throws IllegalStateException not thrown by proper instances
     * @throws RepositoryException if another error occurs
     * @see Value#getBoolean()
     */
    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        StatefulValue realValue;
        if (getType() == PropertyType.BINARY) {
            realValue = SerialValueFactory.getInstance().createBooleanValue(toString(value.getStream()));
        } else {
            realValue = value;
        }

        general.setValue(realValue);
        return realValue.getBoolean();
    }

    /**
     * Returns the type of the underlying concrete value instance.
     *
     * @return value type
     * @see PropertyType
     * @see Value#getType()
     */
    public int getType() {
        return value.getType();
    }
}
