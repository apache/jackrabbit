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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

/**
 * Unit tests for the {@link CompositeTextExtractor} class.
 */
public class CompositeTextExtractorTest extends TestCase {

    /**
     * Text extractor being tested.
     */
    private CompositeTextExtractor extractor;

    /**
     * Creates the text extractor to be tested.
     */
    protected void setUp() throws Exception {
        super.setUp();
        extractor = new CompositeTextExtractor();
        extractor.addTextExtractor(new PlainTextExtractor());
        extractor.addTextExtractor(new XMLTextExtractor());
    }

    /**
     * Tests that the extractor supports all the content types of the
     * component extractors.
     */
    public void testContentTypes() {
        Set types = new HashSet();
        types.addAll(Arrays.asList(extractor.getContentTypes()));
        assertTrue(
                "CompositeTextExtractor does not support component types",
                types.contains("text/plain"));
        assertTrue(
                "CompositeTextExtractor does not support component types",
                types.contains("text/xml"));
        assertTrue(
                "CompositeTextExtractor does not support component types",
                types.contains("application/xml"));
        assertEquals(
                "CompositeTextExtractor supports unknown content types",
                3, types.size());
    }

    /**
     * Tests that the extractor correctly handles an empty stream.
     *
     * @throws IOException on IO errors
     */
    public void testEmptyStream() throws IOException {
        Reader reader = extractor.extractText(
                new ByteArrayInputStream(new byte[0]), "text/plain", null);
        assertEquals("", ExtractorHelper.read(reader));
    }

    /**
     * Tests that the extractor correctly handles a normal stream.
     *
     * @throws IOException on IO errors
     */
    public void testNormalStream() throws IOException {
        String text = "some test content";
        Reader reader = extractor.extractText(
                new ByteArrayInputStream(text.getBytes()), "text/plain", null);
        assertEquals(text, ExtractorHelper.read(reader));
    }

    /**
     * Tests that the extractor correctly handles unsupported content types.
     *
     * @throws IOException on IO errors
     */
    public void testUnsupportedEncoding() throws IOException {
        String text = "some test content";
        Reader reader = extractor.extractText(
                new ByteArrayInputStream(text.getBytes()),
                "unsupported", null);
        assertEquals("", ExtractorHelper.read(reader));
    }

}
