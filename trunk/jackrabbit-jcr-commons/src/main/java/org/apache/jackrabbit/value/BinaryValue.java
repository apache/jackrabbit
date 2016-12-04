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

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.Binary;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * A <code>BinaryValue</code> provides an implementation
 * of the <code>Value</code> interface representing a binary value.
 */
public class BinaryValue extends BaseValue {

    public static final int TYPE = PropertyType.BINARY;

    private Binary bin = null;
    private String text = null;

    /**
     * Constructs a <code>BinaryValue</code> object based on a string.
     *
     * @param text the string this <code>BinaryValue</code> should represent
     */
    public BinaryValue(String text) {
        super(TYPE);
        this.text = text;
    }

    /**
     * Constructs a <code>BinaryValue</code> object based on a <code>Binary</code>.
     *
     * @param bin the bytes this <code>BinaryValue</code> should represent
     */
    public BinaryValue(Binary bin) {
        super(TYPE);
        this.bin = bin;
    }

    /**
     * Constructs a <code>BinaryValue</code> object based on a stream.
     *
     * @param stream the stream this <code>BinaryValue</code> should represent
     */
    public BinaryValue(InputStream stream) {
        super(TYPE);
        try {
            bin = new BinaryImpl(stream);
        } catch (IOException e) {
            throw new IllegalArgumentException("specified stream cannot be read", e);
        }
    }

    /**
     * Constructs a <code>BinaryValue</code> object based on a byte array.
     *
     * @param data the bytes this <code>BinaryValue</code> should represent
     */
    public BinaryValue(byte[] data) {
        super(TYPE);
        bin = new BinaryImpl(data);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>BinaryValue</code> object that
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
        if (obj instanceof BinaryValue) {
            BinaryValue other = (BinaryValue) obj;
            if (text == other.text && stream == other.stream
                    && bin == other.bin) {
                return true;
            }
            return (text != null && text.equals(other.text))
                    || (bin != null && bin.equals(other.bin));
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
     * Gets the string representation of this binary value.
     *
     * @return string representation of this binary value.
     *
     * @throws javax.jcr.ValueFormatException
     * @throws javax.jcr.RepositoryException  if another error occurs
     */
    public String getInternalString()
            throws ValueFormatException, RepositoryException {
        // build text value if necessary
        if (text == null) {
            try {
                byte[] bytes = new byte[(int) bin.getSize()];
                bin.read(bytes, 0);
                text = new String(bytes, DEFAULT_ENCODING);
            } catch (UnsupportedEncodingException e) {
                throw new RepositoryException(DEFAULT_ENCODING
                        + " not supported on this platform", e);
            } catch (IOException e) {
                throw new RepositoryException("failed to retrieve binary data", e);
            }
        }

        return text;
    }

    //----------------------------------------------------------------< Value >
    /**
     * {@inheritDoc}
     */
    public InputStream getStream()
            throws IllegalStateException, RepositoryException {
        if (stream == null) {
            if (bin != null) {
                stream = bin.getStream();
            } else {
                try {
                    stream = new ByteArrayInputStream(text.getBytes(DEFAULT_ENCODING));
                } catch (UnsupportedEncodingException e) {
                    throw new RepositoryException(DEFAULT_ENCODING
                            + " not supported on this platform", e);
                }
            }
        }

        return stream;
    }

    /**
     * {@inheritDoc}
     */
    public Binary getBinary()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {

        if (bin == null) {
            try {
                bin = new BinaryImpl(new ByteArrayInputStream(text.getBytes(DEFAULT_ENCODING)));
            } catch (UnsupportedEncodingException e) {
                throw new RepositoryException(DEFAULT_ENCODING
                        + " not supported on this platform", e);
            } catch (IOException e) {
                throw new RepositoryException("failed to retrieve binary data", e);
            }
        }

        return bin;
    }
}
