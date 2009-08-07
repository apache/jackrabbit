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
package org.apache.jackrabbit.api.jsr283;

import java.io.InputStream;
import java.io.IOException;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

/**
 * A <code>Binary</code> object holds a JCR property value of type
 * <code>BINARY</code>. The <code>Binary</code> interface and the related
 * methods in {@link Property}, {@link Value} and {@link ValueFactory} replace
 * the deprecated {@link Value#getStream} and {@link Property#getStream}
 * methods.
 *
 * @since JCR 2.0
 */
public interface Binary {

    /**
     * Returns an {@link InputStream} representation of this value. Each call to
     * <code>getStream()</code> returns a new stream. The API consumer is
     * responsible for calling <code>close()</code> on the returned stream.
     *
     * @return A stream representation of this value.
     *
     * @throws RepositoryException if an error occurs.
     */
    InputStream getStream() throws RepositoryException;

    /**
     * Reads successive bytes from the specified <code>position</code> in this
     * <code>Binary</code> into the passed byte array until either the byte
     * array is full or the end of the <code>Binary</code> is encountered.
     *
     * @param b the buffer into which the data is read.
     * @param position the position in this Binary from which to start reading
     * bytes.
     *
     * @return the number of bytes read into the buffer, or -1 if there is no
     *         more data because the end of the Binary has been reached.
     *
     * @throws IOException if an I/O error occurs.
     * @throws NullPointerException if b is null.
     * @throws IllegalArgumentException if offset is negative.
     * @throws RepositoryException if another error occurs.
     */
    int read(byte[] b, long position) throws IOException, RepositoryException;

    /**
     * Returns the size of this <code>Binary</code> value in bytes.
     *
     * @return the size of this value in bytes.
     *
     * @throws RepositoryException if an error occurs.
     */
    long getSize() throws RepositoryException;
}