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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.math.BigDecimal;
import java.net.URI;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * <code>QValueFactory</code> defines methods to create <code>QValue</code>
 * instances.
 */
public interface QValueFactory {

    /**
     * Create a new <code>QValue</code> using the given String representation
     * of the value and its {@link javax.jcr.PropertyType type}.
     *
     * @param value String representation of the new <code>QValue</code>. Note,
     * that the given String must never be <code>null</code>.
     * @param type A valid {@link javax.jcr.PropertyType type}.
     * @return a new <code>QValue</code>.
     * @throws ValueFormatException If the given <code>value</code> cannot be
     * converted to the specified <code>type</code>.
     * @throws RepositoryException If another error occurs.
     * @see QValue#getType()
     */
    public QValue create(String value, int type) throws ValueFormatException, RepositoryException;

    /**
     * Create a new <code>QValue</code> with type {@link javax.jcr.PropertyType#DATE}.
     *
     * @param value A non-null <code>Calendar</code> object acting as value
     * of the new <code>QValue</code>.
     * @return a new <code>QValue</code>.
     */
    public QValue create(Calendar value) throws RepositoryException;

    /**
     * Create a new <code>QValue</code> with type {@link javax.jcr.PropertyType#DOUBLE}.
     *
     * @param value A <code>double</code> containing the value
     * of the new <code>QValue</code>.
     * @return a new <code>QValue</code>.
     */
    public QValue create(double value) throws RepositoryException;

    /**
     * Create a new <code>QValue</code> with type {@link javax.jcr.PropertyType#LONG}.
     *
     * @param value A <code>long</code> containing the value
     * of the new <code>QValue</code>.
     * @return a new <code>QValue</code>.
     */
    public QValue create(long value) throws RepositoryException;

    /**
     * Create a new <code>QValue</code> with type {@link javax.jcr.PropertyType#BOOLEAN}.
     *
     * @param value A <code>boolean</code> containing the value
     * of the new <code>QValue</code>.
     * @return a new <code>QValue</code>.
     */
    public QValue create(boolean value) throws RepositoryException;

    /**
     * Create a new <code>QValue</code> with type {@link javax.jcr.PropertyType#NAME}.
     *
     * @param value A non-null <code>Name</code>.
     * @return a new <code>QValue</code>.
     */
    public QValue create(Name value) throws RepositoryException;

    /**
     * Create a new <code>QValue</code> with type {@link javax.jcr.PropertyType#PATH}.
     *
     * @param value A non-null <code>Path</code>.
     * @return a new <code>QValue</code>.
     */
    public QValue create(Path value) throws RepositoryException;

    /**
     * Create a new <code>QValue</code> with type {@link javax.jcr.PropertyType#DECIMAL}.
     *
     * @param value A non-null <code>BigDecimal</code>.
     * @return a new <code>QValue</code>.
     */
    public QValue create(BigDecimal value) throws RepositoryException;

    /**
     * Create a new <code>QValue</code> with type {@link javax.jcr.PropertyType#URI}.
     *
     * @param value A non-null <code>URI</code>.
     * @return a new <code>QValue</code>.
     */
    public QValue create(URI value) throws RepositoryException;

    /**
     * Create a new <code>QValue</code> with type {@link javax.jcr.PropertyType#BINARY}.
     *
     * @param value
     * @return a new <code>QValue</code>.
     */
    public QValue create(byte[] value) throws RepositoryException;

    /**
     * Creates a QValue that contains the given binary stream.
     * The given stream is consumed and closed by this method. The type of the
     * resulting QValue will be {@link javax.jcr.PropertyType#BINARY}.
     *
     * @param value binary stream
     * @return a new binary <code>QValue</code>.
     * @throws RepositoryException if the value could not be created
     * @throws IOException if the stream can not be consumed
     */
    public QValue create(InputStream value) throws RepositoryException, IOException;

    /**
     * Create a new <code>QValue</code> with type {@link javax.jcr.PropertyType#BINARY}.
     *
     * @param value
     * @return a new binary <code>QValue</code>.
     * @throws IOException
     */
    public QValue create(File value) throws RepositoryException, IOException;

    /**
     * Given the <code>QPropertyDefinition</code> of an <em>autocreated</em>
     * property, compute suitable values to be used in transient space until
     * the newly created node gets saved.
     *
     * @param propertyDefinition definition of property for which values should be created
     * @return computed value
     * @throws RepositoryException
     */
    public QValue[] computeAutoValues(QPropertyDefinition propertyDefinition) throws RepositoryException;
}
