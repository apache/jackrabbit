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

/**
 * Interface for extracting text content from binary streams.
 */
public interface TextExtractor {

    /**
     * Returns the MIME types supported by this extractor. The returned
     * strings must be in lower case, and the returned array must not be empty.
     * <p>
     * The returned array must not be modified.
     *
     * @return supported MIME types, lower case
     */
    String[] getContentTypes();

    /**
     * Returns a reader for the text content of the given binary document.
     * The content type and character encoding (if available and applicable)
     * are given as arguments. The given content type is guaranteed to be
     * one of the types reported by {@link #getContentTypes()} unless the
     * implementation explicitly permits other content types.
     * <p>
     * The implementation can choose either to read and parse the given
     * document immediately or to return a reader that does it incrementally.
     * The only constraint is that the implementation must close the given
     * stream latest when the returned reader is closed. The caller on the
     * other hand is responsible for closing the returned reader.
     * <p>
     * The implemenation should only throw an exception on transient
     * errors, i.e. when it can expect to be able to successfully extract
     * the text content of the same binary at another time. An effort
     * should be made to recover from syntax errors and other similar problems.
     * <p>
     * This method should be thread-safe, i.e. it is possible that this
     * method is invoked simultaneously by different threads to extract the
     * text content of different documents. On the other hand the returned
     * reader does not need to be thread-safe.
     *
     * @param stream   binary document from which to extract text
     * @param type     MIME type of the given document, lower case
     * @param encoding the character encoding of the binary data,
     *                 or <code>null</code> if not available
     * @return reader for the extracted text content
     * @throws IOException on transient errors
     */
    Reader extractText(InputStream stream, String type, String encoding)
        throws IOException;

}
