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

import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

import javax.swing.text.DefaultStyledDocument;

import junit.framework.TestCase;

/**
 * <code>RTFTextExtractorTest</code> implements test cases for
 * {@link RTFTextExtractor}.
 */
public class RTFTextExtractorTest extends TestCase {

    public void testExtractor() throws IOException {
        // JCR-1881: Only run the test if the underlying libraries work
        try {
            new DefaultStyledDocument();
        } catch (Throwable t) {
            return;
        }

        TextExtractor extractor = new RTFTextExtractor();
        InputStream in = getClass().getResourceAsStream("test.rtf");
        Reader r = extractor.extractText(in, "application/rtf", null);
        StringWriter w = new StringWriter();
        try {
            int c;
            while ((c = r.read()) != -1) {
                w.write(c);
            }
        } finally {
            r.close();
        }
        assertTrue(w.toString().indexOf("The quick brown fox jumps over the lazy dog.") != -1);
    }
}
