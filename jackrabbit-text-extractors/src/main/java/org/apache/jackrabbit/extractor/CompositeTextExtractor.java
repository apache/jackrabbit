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
package org.apache.jackrabbit.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Composite text extractor. This class presents a unified interface
 * for a set of {@link TextExtractor} instances. The composite extractor
 * supports all the content types supported by the component extractors,
 * and delegates text extraction calls to the appropriate components.
 */
public class CompositeTextExtractor implements TextExtractor {

    /**
     * Configured {@link TextExtractor} instances, keyed by content types.
     */
    private final Map extractors = new HashMap();

    /**
     * Adds a component text extractor. The given extractor is registered
     * to process all the content types it claims to support.
     *
     * @param extractor component extractor
     */
    public void addTextExtractor(TextExtractor extractor) {
        String[] types = extractor.getContentTypes();
        for (int i = 0; i < types.length; i++) {
            extractors.put(types[i], extractor);
        }
    }

    //-------------------------------------------------------< TextExtractor >

    /**
     * Returns all the content types supported by the component extractors.
     *
     * @return supported content types
     */
    public String[] getContentTypes() {
        Set types = extractors.keySet();
        return (String[]) types.toArray(new String[types.size()]);
    }

    /**
     * Extracts text content using one of the component extractors. If an
     * extractor for the given content type does not exist, then the binary
     * stream is just closed and an empty reader is returned.
     *
     * @param stream binary stream
     * @param type content type
     * @param encoding optional character encoding
     * @return reader for the text content of the binary stream
     * @throws IOException if the binary stream can not be read
     */
    public Reader extractText(InputStream stream, String type, String encoding)
            throws IOException {
        TextExtractor extractor = (TextExtractor) extractors.get(type);
        if (extractor != null) {
            return extractor.extractText(stream, type, encoding);
        } else {
            stream.close();
            return new StringReader("");
        }
    }

}
