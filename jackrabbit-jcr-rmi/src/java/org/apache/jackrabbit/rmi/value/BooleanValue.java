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

import java.io.Serializable;

import javax.jcr.PropertyType;

/**
 * The <code>BooleanValue</code> class implements the committed value state for
 * Boolean values as a part of the State design pattern (Gof) used by this
 * package.
 *
 * @since 0.16.4.1
 * @see org.apache.jackrabbit.rmi.value.SerialValue
 */
public class BooleanValue extends BaseNonStreamValue
        implements Serializable, StatefulValue {

    /** The serial version UID */
    private static final long serialVersionUID = 8212168298890947089L;

    /** The boolean value */
    private final boolean value;

    /**
     * Creates an instance for the given boolean <code>value</code>.
     */
    protected BooleanValue(boolean value) {
        this.value = value;
    }

    /**
     * Creates an instance for the given string representation of a boolean.
     * <p>
     * Calls {@link #toBoolean(String)} to convert the string to a boolean.
     */
    protected BooleanValue(String value) {
        this(toBoolean(value));
    }

    /**
     * Returns the boolean value represented by the string <code>value</code>.
     * <p>
     * This implementation uses the <code>Boolean.valueOf(String)</code> method
     * to convert the string to a boolean.
     */
    protected static boolean toBoolean(String value) {
        return Boolean.valueOf(value).booleanValue();
    }

    /**
     * Returns <code>PropertyType.BOOLEAN</code>.
     */
    public int getType() {
        return PropertyType.BOOLEAN;
    }

    /**
     * Returns the boolean value.
     */
    public boolean getBoolean() {
        return value;
    }

    /**
     * Returns the boolean as a string converted by the
     * <code>Boolean.toString(boolean)</code>.
     */
    public String getString() {
        return Boolean.toString(value);
    }
}
