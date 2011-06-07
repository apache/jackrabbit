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
package org.apache.jackrabbit.spi;

import java.io.InputStream;
import java.util.Calendar;
import java.math.BigDecimal;
import java.net.URI;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Binary;

/**
 * <code>QValue</code> is the SPI representation of a
 * {@link javax.jcr.Value jcr value}. It therefore refers to <code>Name</code>s
 * and <code>Path</code>s only and is thus isolated from session-specific
 * namespace mappings.
 */
public interface QValue {

    public static final QValue[] EMPTY_ARRAY = new QValue[0];

    /**
     * Returns the <code>PropertyType</code> of this <code>QValue</code> object.
     * It may be either of the value property types defined by the JSR 283:
     * <ul>
     * <li>{@link PropertyType#STRING}</li>
     * <li>{@link PropertyType#BINARY}</li>
     * <li>{@link PropertyType#BOOLEAN}</li>
     * <li>{@link PropertyType#DATE}</li>
     * <li>{@link PropertyType#DECIMAL}</li>
     * <li>{@link PropertyType#DOUBLE}</li>
     * <li>{@link PropertyType#LONG}</li>
     * <li>{@link PropertyType#NAME}</li>
     * <li>{@link PropertyType#PATH}</li>
     * <li>{@link PropertyType#REFERENCE}</li>
     * <li>{@link PropertyType#URI}</li>
     * <li>{@link PropertyType#WEAKREFERENCE}</li>
     * </ul>
     *
     * @return the <code>PropertyType</code> of this <code>QValue</code> object.
     */
    public int getType();

    /**
     * Returns the length of the internal value or -1 if the implementation
     * cannot determine the length at this time.<br>
     * NOTE: for {@link PropertyType#NAME} and {@link PropertyType#PATH} the
     * length of the internal value must not be used for indicating the length
     * of a property such as retrieved by calling {@link Property#getLength()}
     * and {@link Property#getLengths()}.
     *
     * @return length of this <code>QValue</code> object.
     * @throws RepositoryException
     */
    public long getLength() throws RepositoryException;

    /**
     * Returns a <code>String</code> representation of this <code>QValue</code>
     * object.
     *
     * @return A <code>String</code> representation of this <code>QValue</code>
     * object.
     * @throws RepositoryException
     */
    public String getString() throws RepositoryException;

    /**
     * Returns an <code>InputStream</code> representation of this <code>QValue</code>
     * object. This method always returns a new stream.
     *
     * @return A stream representation of this value.
     * @throws RepositoryException
     */
    public InputStream getStream() throws RepositoryException;

    /**
     * Returns a <code>Binary</code> representation of this <code>QValue</code>
     * object.
     *
     * @return A <code>Binary</code> representation of this value.
     * @throws RepositoryException
     */
    public Binary getBinary() throws RepositoryException;

    /**
     * Returns a <code>Calendar</code> representation of this value.
     *
     * @return A <code>Calendar</code> representation of this value.
     * @throws RepositoryException if an error occurs.
     */
    public Calendar getCalendar() throws RepositoryException;

    /**
     * Returns a <code>BigDecimal</code> representation of this value.
     *
     * @return A <code>BigDecimal</code> representation of this value.
     * @throws RepositoryException if an error occurs.
     */
    public BigDecimal getDecimal() throws RepositoryException;

    /**
     * Returns a <code>double</code> representation of this value.
     *
     * @return A <code>double</code> representation of this value.
     * @throws RepositoryException if an error occurs.
     */
    public double getDouble() throws RepositoryException;

    /**
     * Returns a <code>long</code> representation of this value.
     *
     * @return A <code>long</code> representation of this value.
     * @throws RepositoryException if an error occurs.
     */
    public long getLong() throws RepositoryException;

    /**
     * Returns a <code>boolean</code> representation of this value.
     *
     * @return A <code>boolean</code> representation of this value.
     * @throws RepositoryException if an error occurs.
     */
    public boolean getBoolean() throws RepositoryException;


    /**
     * Returns a <code>Name</code> representation of this value.
     *
     * @return A <code>Name</code> representation of this value.
     * @throws RepositoryException if an error occurs.
     */
    public Name getName() throws RepositoryException;

    /**
     * Returns a <code>Path</code> representation of this value.
     *
     * @return A <code>Path</code> representation of this value.
     * @throws RepositoryException if an error occurs.
     */
    public Path getPath() throws RepositoryException;

    /**
     * Returns an <code>URI</code> representation of this value.
     *
     * @return A <code>URI</code> representation of this value.
     * @throws RepositoryException if an error occurs.
     */
    public URI getURI() throws RepositoryException;

    /**
     * Frees temporarily allocated resources such as temporary file, buffer, etc.
     */
    public void discard();
}
