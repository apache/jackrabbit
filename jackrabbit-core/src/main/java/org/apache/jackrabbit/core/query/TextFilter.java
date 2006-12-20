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
package org.apache.jackrabbit.core.query;

import org.apache.jackrabbit.core.state.PropertyState;

import javax.jcr.RepositoryException;
import java.util.Map;

/**
 * Defines an interface for extracting text out of binary properties according
 * to their mime-type.
 * </p>
 * {@link TextFilter} implementations are asked if they can handle a certain
 * mime type ({@link #canFilter(String)} and if one of them returns
 * <code>true</code> the text representation is created with
 * {@link #doFilter(PropertyState, String)}
 *
 * @deprecated use the {@link org.apache.jackrabbit.extractor.TextExtractor}
 *             interface
 */
public interface TextFilter {

    /**
     * Returns <code>true</code> if this <code>TextFilter</code> can index
     * content of <code>mimeType</code>; <code>false</code> otherwise.
     *
     * @param mimeType the mime type of the content to index.
     * @return whether this <code>TextFilter</code> can index content of
     *         <code>mimeType</code>.
     */
    boolean canFilter(String mimeType);

    /**
     * Creates an text representation of a binary property <code>data</code>.
     * The returned map contains {@link java.io.Reader} values. Keys to the
     * reader values are <code>String</code>s that serve as field names.
     * <p/>
     * E.g. a TextFilter for a html document may extract multiple fields: one
     * for the title and one for the whole content.
     *
     * @param data     the data property that contains the binary content.
     * @param encoding the encoding of the content or <code>null</code> if
     *                 <code>data</code> does not use encoding.
     * @return the extracted text.
     * @throws RepositoryException if an error occurs while reading from the
     *                             node or if the data is malformed.
     */
    Map doFilter(PropertyState data, String encoding)
            throws RepositoryException;
}
