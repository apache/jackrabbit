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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Binary value.
 */
class BinaryValue implements Value, Serializable {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1719020811685971215L;

    /**
     * The binary value
     */
    private final Binary value;

    /**
     * The stream instance returned by {@link #getStream()}. Note that
     * the stream is not included when serializing the value.
     */
    private transient InputStream stream = null;

    /**
     * Creates a binary value.
     */
    public BinaryValue(Binary value) {
        this.value = value;
    }

    /**
     * Returns {@link PropertyType#BINARY}.
     */
    public int getType() {
        return PropertyType.BINARY;
    }

    public Binary getBinary() {
        return value;
    }

    public String getString() throws RepositoryException {
        try {
            InputStream stream = value.getStream();
            try {
                Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                StringBuilder builder = new StringBuilder();
                char[] buffer = new char[1024];
                int n = reader.read(buffer);
                while (n != -1) {
                    builder.append(buffer, 0, n);
                    n = reader.read(buffer);
                }
                return builder.toString();
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            throw new RepositoryException("Unable to read the binary value", e);
        }
    }

    public synchronized InputStream getStream() throws RepositoryException {
        if (stream == null) {
            stream = value.getStream();
        }
        return stream;
    }

    public boolean getBoolean() throws RepositoryException {
        return new StringValue(getString()).getBoolean();
    }

    public Calendar getDate() throws RepositoryException {
        return new StringValue(getString()).getDate();
    }

    public BigDecimal getDecimal() throws RepositoryException {
        return new StringValue(getString()).getDecimal();
    }

    public double getDouble() throws RepositoryException {
        return new StringValue(getString()).getDouble();
    }

    public long getLong() throws RepositoryException {
        return new StringValue(getString()).getLong();
    }

}
