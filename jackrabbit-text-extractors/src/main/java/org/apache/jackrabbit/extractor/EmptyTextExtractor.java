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

/**
 * Dummy text extractor that always returns and empty reader for all documents.
 * Useful as a dummy handler for unsupported content types.
 */
public class EmptyTextExtractor implements TextExtractor {

    /**
     * Supported content types.
     */
    private final String[] types;

    /**
     * Creates a dummy text extractor for the given content types.
     * The given array must not be modified after it has been passed
     * to this constructor.
     *
     * @param types supported content types
     */
    public EmptyTextExtractor(String[] types) {
        this.types = types;
    }

    /**
     * Creates a dummy text extractor for the given content type.
     *
     * @param type supported content type
     */
    public EmptyTextExtractor(String type) {
        this(new String[] { type });
    }

    //-------------------------------------------------------< TextExtractor >

    /**
     * Returns the supported content types.
     *
     * @return supported content types
     */
    public String[] getContentTypes() {
        return types;
    }

    /**
     * Closes the given stream and returns an empty reader.
     *
     * @param stream binary stream that simply gets closed
     * @param type ignored
     * @param encoding ignored
     * @return empty reader
     * @throws IOException if the binary stream can not be closed
     */
    public Reader extractText(InputStream stream, String type, String encoding)
            throws IOException {
        stream.close();
        return new StringReader("");
    }

}
