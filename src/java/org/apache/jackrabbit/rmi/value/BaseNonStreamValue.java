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

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.ValueFormatException;

/**
 * The <code>BaseNonStreamValue</code> class implements the basic committed
 * value state for non-stream values as a part of the State design pattern (Gof)
 * used by this package.
 * <p>
 * This class implements all methods of the
 * {@link org.apache.jackrabbit.rmi.value.StatefullValue} except
 * <code>getString</code> and <code>getType</code> interface by always
 * throwing an appropriate exception. Extensions of this class should overwrite
 * methods as appropriate except for the {@link #getStream()} which must throw
 * an <code>IllegalStateException</code> for this line of committed non-stream
 * states.
 *
 * @author Felix Meschberger
 */
public abstract class BaseNonStreamValue implements StatefulValue {

    /**
     * Default constructor with no special tasks.
     */
    protected BaseNonStreamValue() {
    }

    /**
     * Always throws <code>IllegalStateException</code> because only non-stream
     * getters are available from this implementation.
     * <p>
     * This method is declared final to mark that this line of implementations
     * does not provide access to <code>InputStream</code>s.
     *
     * @return nothing
     * @throws IllegalStateException as defined above.
     */
    public final InputStream getStream() throws IllegalStateException {
        throw new IllegalStateException("Stream not available");
    }

    /**
     * Always throws a <code>ValueFormatException</code>. Implementations should
     * overwrite if conversion to boolean is supported.
     *
     * @return nothing
     * @throws ValueFormatException If the value cannot be converted to a
     *      boolean.
     */
    public boolean getBoolean() throws ValueFormatException {
        throw getValueFormatException(PropertyType.TYPENAME_BOOLEAN);
    }

    /**
     * Always throws a <code>ValueFormatException</code>. Implementations should
     * overwrite if conversion to <code>Calender</code> is supported.
     *
     * @return nothing
     * @throws ValueFormatException If the value cannot be converted to a
     *      <code>Calendar</code> instance.
     */
    public Calendar getDate() throws ValueFormatException {
        throw getValueFormatException(PropertyType.TYPENAME_DATE);
    }

    /**
     * Always throws a <code>ValueFormatException</code>. Implementations should
     * overwrite if conversion to double is supported.
     *
     * @return nothing
     * @throws ValueFormatException If the value cannot be converted to a
     *      double.
     */
    public double getDouble() throws ValueFormatException {
        throw getValueFormatException(PropertyType.TYPENAME_DOUBLE);
    }

    /**
     * Always throws a <code>ValueFormatException</code>. Implementations should
     * overwrite if conversion to long is supported.
     *
     * @return nothing
     * @throws ValueFormatException If the value cannot be converted to a
     *      long.
     */
    public long getLong() throws ValueFormatException {
        throw getValueFormatException(PropertyType.TYPENAME_LONG);
    }

    /**
     * Returns a <code>ValueFormatException</code> with a message indicating
     * what kind of type conversion is not supported.
     *
     * @return nothing
     * @param destType The name of the value type to which this value cannot
     *      be converted.
     */
    protected ValueFormatException getValueFormatException(String destType) {
        return new ValueFormatException("Cannot convert value of type "
            + PropertyType.nameFromValue(getType()) + " to " + destType);
    }
}
