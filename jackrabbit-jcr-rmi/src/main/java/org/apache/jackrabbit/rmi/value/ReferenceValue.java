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
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * The <code>ReferenceValue</code> class implements the committed value state
 * for Reference values as a part of the State design pattern (Gof) used by
 * this package.
 *
 * @since 0.16.4.1
 */
public class ReferenceValue extends BaseNonStreamValue
        implements Serializable, StatefulValue {

    /** The serial version UID */
    private static final long serialVersionUID = -3160494922729580458L;

    /** The reference value */
    private final String value;

    /**
     * Creates an instance for the given reference <code>value</code>.
     */
    protected ReferenceValue(String value) throws ValueFormatException {
        this.value = toReference(value);
    }

    /**
     * Checks whether the string value adheres to the reference syntax.
     *
     * @param value The string to check for synthactical compliance with a
     *      reference value.
     *
     * @return The input value.
     *
     * @throws ValueFormatException if the string <code>value</code> is not a
     *      synthactically correct reference.
     */
    protected static String toReference(String value) throws ValueFormatException {
        // TODO: check syntax
        return value;
    }

    /**
     * Returns <code>PropertyType.REFERENCE</code>.
     */
    public int getType() {
        return PropertyType.REFERENCE;
    }

    /**
     * Returns the string representation of the reference value.
     */
    public String getString() throws ValueFormatException, RepositoryException {
        return value;
    }
}
