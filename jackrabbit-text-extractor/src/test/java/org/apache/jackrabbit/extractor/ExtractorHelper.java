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

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;

/**
 * Helper class for text extractor unit tests.
 */
class ExtractorHelper {

    /**
     * Private constructor to prevent instantiation.
     */
    private ExtractorHelper() {
    };

    /**
     * Returns the entire content of the given reader as a string.
     *
     * @param reader reader to be read and closed
     * @return entire content of the reader
     * @throws IOException on IO errors
     */
    public static String read(Reader reader) throws IOException {
        try {
            CharArrayWriter writer = new CharArrayWriter();
            try {
                char[] buffer = new char[4096];
                int n = reader.read(buffer);
                while (n > 0) {
                    writer.write(buffer, 0, n);
                    n = reader.read(buffer);
                }
            } finally {
                writer.close();
            }
            return new String(writer.toCharArray());
        } finally {
            reader.close();
        }
    }

}
