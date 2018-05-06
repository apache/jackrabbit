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

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import java.util.Calendar;
import java.util.UUID;
import java.math.BigDecimal;

/**
 * A <code>ReferenceValue</code> provides an implementation
 * of the <code>Value</code> interface representing a <code>REFERENCE</code> value
 * (a UUID of an existing node).
 */
public class ReferenceValue extends BaseValue {

    public static final int TYPE = PropertyType.REFERENCE;

    private final String uuid;

    /**
     * Constructs a <code>ReferenceValue</code> object representing the UUID of
     * an existing node.
     *
     * @param target the node to be referenced
     * @throws IllegalArgumentException If <code>target</code> is nonreferenceable.
     * @throws javax.jcr.RepositoryException      If another error occurs.
     */
    public ReferenceValue(Node target) throws RepositoryException {
        super(TYPE);
        try {
            this.uuid = target.getUUID();
        } catch (UnsupportedRepositoryOperationException ure) {
            throw new IllegalArgumentException("target is nonreferenceable.");
        }
    }

    /**
     * Returns a new <code>ReferenceValue</code> initialized to the value
     * represented by the specified <code>String</code>.
     * <p>
     * The specified <code>String</code> must denote the UUID of an existing
     * node.
     *
     * @param s the string to be parsed.
     * @return a newly constructed <code>ReferenceValue</code> representing the
     *         the specified value.
     * @throws javax.jcr.ValueFormatException If the <code>String</code> is not a valid
     *                              not a valid UUID format.
     */
    public static ReferenceValue valueOf(String s) throws ValueFormatException {
        if (s != null) {
            try {
                UUID.fromString(s);
            } catch (IllegalArgumentException iae) {
                throw new ValueFormatException("not a valid UUID format: " + s);
            }
            return new ReferenceValue(s);
        } else {
            throw new ValueFormatException("not a valid UUID format: " + s);
        }
    }

    /**
     * Protected constructor creating a <code>ReferenceValue</code> object
     * without validating the UUID format.
     *
     * @param uuid the UUID of the node to be referenced
     * @see #valueOf
     */
    protected ReferenceValue(String uuid) {
        super(TYPE);
        this.uuid = uuid;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>ReferenceValue</code> object that
     * represents the same value as this object.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ReferenceValue) {
            ReferenceValue other = (ReferenceValue) obj;
            if (uuid == other.uuid) {
                return true;
            } else if (uuid != null && other.uuid != null) {
                return uuid.equals(other.uuid);
            }
        }
        return false;
    }

    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }

    //------------------------------------------------------------< BaseValue >
    /**
     * {@inheritDoc}
     */
    protected String getInternalString() throws ValueFormatException {
        if (uuid != null) {
            return uuid;
        } else {
            throw new ValueFormatException("empty value");
        }
    }

    //----------------------------------------------------------------< Value >
    /**
     * {@inheritDoc}
     */
    public Calendar getDate()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        throw new ValueFormatException("conversion to date failed: inconvertible types");
    }

    /**
     * {@inheritDoc}
     */
    public long getLong()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        throw new ValueFormatException("conversion to long failed: inconvertible types");
    }

    /**
     * {@inheritDoc}
     */
    public boolean getBoolean()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        throw new ValueFormatException("conversion to boolean failed: inconvertible types");
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        throw new ValueFormatException("conversion to double failed: inconvertible types");
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal getDecimal()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        throw new ValueFormatException("conversion to Decimal failed: inconvertible types");
    }
}
