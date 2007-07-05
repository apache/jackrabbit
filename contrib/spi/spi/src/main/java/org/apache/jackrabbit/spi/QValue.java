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

import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.Property;
import java.io.InputStream;

/**
 * <code>QValue</code>...
 */
public interface QValue {

    public static final QValue[] EMPTY_ARRAY = new QValue[0];

    /**
     * Returns the <code>PropertyType</code> of this <code>QValue</code> object.
     * It may be either of the value property types defined by the JSR 170:
     * <ul>
     * <li>{@link PropertyType#BINARY}</li>
     * <li>{@link PropertyType#BOOLEAN}</li>
     * <li>{@link PropertyType#DATE}</li>
     * <li>{@link PropertyType#DOUBLE}</li>
     * <li>{@link PropertyType#LONG}</li>
     * <li>{@link PropertyType#NAME}</li>
     * <li>{@link PropertyType#PATH}</li>
     * <li>{@link PropertyType#REFERENCE}</li>
     * <li>{@link PropertyType#STRING}</li>
     * </ul>
     *
     * @return the <code>PropertyType</code> of this <code>QValue</code> object.
     */
    public int getType();

    /**
     * Returns the length of the internal value.<br>
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
     * Returns a <code>InputStream</code> representation of this <code>QValue</code>
     * object.
     *
     * @return A stream representation of this value.
     * @throws RepositoryException
     */
    public InputStream getStream() throws RepositoryException;

    /**
     * Returns a <code>QName</code> representation of this value.
     *
     * @return A <code>QName</code> representation of this value.
     * @throws RepositoryException if an error occurs.
     */
    public QName getQName() throws RepositoryException;

    /**
     * Returns a <code>Path</code> representation of this value.
     *
     * @return A <code>Path</code> representation of this value.
     * @throws RepositoryException if an error occurs.
     */
    public Path getPath() throws RepositoryException;

    /**
     * Frees temporarily allocated resources such as temporary file, buffer, etc.
     */
    public void discard();
}
