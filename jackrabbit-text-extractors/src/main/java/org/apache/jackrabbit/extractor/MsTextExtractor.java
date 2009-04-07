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

import org.apache.poi.extractor.ExtractorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;

/**
 * Text extractor for Microsoft Word documents.
 */
public class MsTextExtractor extends AbstractTextExtractor {

    /**
     * Logger instance.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(MsTextExtractor.class);

    /**
     * Force loading of dependent class.
     */
    static {
        ExtractorFactory.class.getName();
    }

    /**
     * Creates a new <code>MsWordTextExtractor</code> instance.
     */
    public MsTextExtractor() {
        super(new String[]{"application/vnd.ms-word", 
                           "application/msword",
                           "application/vnd.ms-powerpoint",
                           "application/mspowerpoint",
                           "application/vnd.ms-excel",
                           "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                           "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                           "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"});
    }

    //-------------------------------------------------------< TextExtractor >

    /**
     * {@inheritDoc}
     * Returns an empty reader if an error occured extracting text from
     * the word document.
     */
    public Reader extractText(InputStream stream,
                              String type,
                              String encoding) throws IOException {
        try {
            String text = ExtractorFactory.createExtractor(stream).getText();
            return new StringReader(text);
        } catch (Exception e) {
            logger.warn("Failed to extract Microsoft Document text content", e);
            return new StringReader("");
        } finally {
            stream.close();
        }
    }

}
