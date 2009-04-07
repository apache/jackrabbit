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
import java.util.Set;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParsingReader;

/**
 * Default text extractor based on Apache Tika.
 */
public class DefaultTextExtractor implements TextExtractor {

    /**
     * Auto-detecting parser.
     */
    private static final Parser PARSER;

    /**
     * Supported content types.
     */
    private static final String[] TYPES;

    static {
        AutoDetectParser parser = new AutoDetectParser();
        PARSER = parser;
        Set types = parser.getParsers().keySet();
        TYPES = (String[]) types.toArray(new String[types.size()]);
    }

    public String[] getContentTypes() {
        return TYPES;
    }

    public Reader extractText(InputStream stream, String type, String encoding)
            throws IOException {
        Metadata metadata = new Metadata();
        if (type != null && type.trim().length() > 0) {
            metadata.set(Metadata.CONTENT_TYPE, type.trim());
        }
        // TODO: This creates a background thread. Is that a problem?
        return new ParsingReader(PARSER, stream, metadata);
    }

}
