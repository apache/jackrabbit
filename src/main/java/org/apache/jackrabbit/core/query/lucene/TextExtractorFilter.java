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

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.query.TextFilter;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.extractor.TextExtractor;

/**
 * Utility base class for migrating functionality from existing implementations
 * of the deprecated {@link TextFilter} interface to the new
 * {@link TextExtractor} interface. Once the functionality of an existing
 * TextFilter has been copied to a new TextExtractor, the original class can
 * be replaced with the following template to keep backwards compatibility
 * while avoiding the burden of maintaining duplicate code:
 * <pre>
 * <b>public class</b> SomeTextFilter <b>extends</b> TextExtractorFilter {
 *     <b>public</b> SomeTextFilter() {
 *         <b>super</b>(<b>new</b> SomeTextExtractor());
 *     }
 * }
 * </pre>
 */
public class TextExtractorFilter implements TextFilter {

    /**
     * The adapted text extractor.
     */
    private final TextExtractor extractor;

    /**
     * Creates a text filter adapter for the given text extractor.
     *
     * @param extractor adapted text extractor
     */
    public TextExtractorFilter(TextExtractor extractor) {
        this.extractor = extractor;
    }

    /**
     * Returns true if the adapted text extractor supports the given
     * content type.
     *
     * @param mimeType content type
     * @return <code>true</code> if the content type is supported,
     *         <code>false</code> otherwise
     */
    public boolean canFilter(String mimeType) {
        mimeType = mimeType.toLowerCase();
        String[] types = extractor.getContentTypes();
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(mimeType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts text content of the given binary property using the adapted
     * text extractor.
     *
     * @param data binary property
     * @param encoding character encoding, or <code>null</code>
     * @return map that contains a reader for the extracted text as
     *         the {@link FieldNames#FULLTEXT} entry
     * @throws RepositoryException if the binary property can not be read
     */
    public Map doFilter(PropertyState data, String encoding)
            throws RepositoryException {
        InternalValue[] values = data.getValues();
        if (values.length == 1) {
            try {
                String type = "application/octet-stream";
                String[] types = extractor.getContentTypes();
                if (types.length > 0) {
                    type = types[0];
                }

                BLOBFileValue blob = values[0].getBLOBFileValue();
                Reader reader =
                    extractor.extractText(blob.getStream(), type, encoding);

                Map result = new HashMap();
                result.put(FieldNames.FULLTEXT, reader);
                return result;
            } catch (IOException e) {
                throw new RepositoryException("Text extraction error", e);
            }
        } else {
            // multi value not supported
            throw new RepositoryException(
                    "Multi-valued binary properties not supported.");
        }
    }

}
