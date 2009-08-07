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
package org.apache.jackrabbit.core.query.lucene;

import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.query.TextFilter;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.extractor.TextExtractor;

/**
 * Adapter class for achieving backwards compatibility with classes
 * implementing the deprectated {@link TextFilter} interface. This class
 * implements the {@link TextExtractor} interface through calls to an
 * underlying {@link TextFilter} instance.
 */
public class TextFilterExtractor implements TextExtractor {

    /**
     * Supported content types.
     */
    private final String[] types;

    /**
     * The adapted text filter.
     */
    private final TextFilter filter;

    /**
     * Creates a text extractor adapter that supports the given content
     * types using the given text filter.
     *
     * @param types supported content types
     * @param filter text filter to be adapted
     */
    public TextFilterExtractor(String[] types, TextFilter filter) {
        this.types = types;
        this.filter = filter;
    }

    /**
     * Creates a text extractor adapter that supports the given content
     * type using the given text filter.
     *
     * @param type supported content type
     * @param filter text filter to be adapted
     */
    public TextFilterExtractor(String type, TextFilter filter) {
        this(new String[] { type }, filter);
    }

    /**
     * Returns the supported content types.
     *
     * @return supported content types
     */
    public String[] getContentTypes() {
        return types;
    }

    /**
     * Extracts the text content of the given binary stream by calling the
     * underlying {@link TextFilter} instance. A dummy {@link PropertyState}
     * instance is created to comply with the
     * {@link TextFilter#doFilter(PropertyState, String)} method signature.
     *
     * @param stream binary stream
     * @param type content type
     * @param encoding character encoding, or <code>null</code>
     * @return reader reader for the extracted text content
     * @throws IOException if the adapted call fails
     */
    public Reader extractText(InputStream stream, String type, String encoding)
            throws IOException {
        InternalValue v = null;
        try {
            v = InternalValue.createTemporary(stream);
            final InternalValue value = v;
            PropertyState state = new PropertyState(
                    (PropertyId) null, ItemState.STATUS_EXISTING, true);
            state.setValues(new InternalValue[] { value });
            Map fields = filter.doFilter(state, encoding);
            Object fulltext = fields.get(FieldNames.FULLTEXT);
            if (fulltext instanceof Reader) {
                return new FilterReader((Reader) fulltext) {
                    public void close() throws IOException {
                        super.close();
                        value.getBLOBFileValue().discard();
                    }
                };
            } else {
                value.getBLOBFileValue().discard();
                return new StringReader("");
            }
        } catch (RepositoryException e) {
            if (v != null) {
                v.getBLOBFileValue().discard();
            }
            return new StringReader("");
        }
    }

}
