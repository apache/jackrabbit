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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Text extractor for plain text.
 */
public class PlainTextExtractor extends AbstractTextExtractor {

    /**
     * Logger instance.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(PlainTextExtractor.class);

    /**
     * Creates a new <code>PlainTextExtractor</code> instance.
     */
    public PlainTextExtractor() {
        super(new String[]{"text/plain"});
    }

    //-------------------------------------------------------< TextExtractor >

    /**
     * Wraps the given input stream to an {@link InputStreamReader} using
     * the given encoding, or the platform default encoding if the encoding
     * is not given or is unsupported. Closes the stream and returns an empty
     * reader if the given encoding is not supported.
     *
     * @param stream binary stream
     * @param type ignored
     * @param encoding character encoding, optional
     * @return reader for the plain text content
     * @throws IOException if the binary stream can not be closed in case
     *                     of an encoding issue
     */
    public Reader extractText(InputStream stream, String type, String encoding)
            throws IOException {
        try {
            if (encoding != null) {
                return new InputStreamReader(stream, encoding);
            }
        } catch (UnsupportedEncodingException e) {
            logger.warn("Unsupported encoding '{}', using default ({}) instead.",
                    new Object[]{encoding, System.getProperty("file.encoding")});
        }
        return new InputStreamReader(stream);
    }

}
