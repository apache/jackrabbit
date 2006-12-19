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
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

/**
 * Unit tests for the {@link XMLTextExtractor} class.
 */
public class XMLTextExtractorTest extends TestCase {

    /**
     * Text extractor being tested.
     */
    private TextExtractor extractor;

    /**
     * Creates the text extractor to be tested.
     */
    protected void setUp() throws Exception {
        super.setUp();
        extractor = new XMLTextExtractor();
    }

    /**
     * Tests that the extractor supportes <code>text/xml</code> and
     * <code>application/xml</code>.
     */
    public void testContentTypes() {
        Set types = new HashSet();
        types.addAll(Arrays.asList(extractor.getContentTypes()));
        assertTrue(
                "XMLTextExtractor does not support text/xml",
                types.contains("text/xml"));
        assertTrue(
                "XMLTextExtractor does not support application/xml",
                types.contains("application/xml"));
        assertEquals(
                "XMLTextExtractor supports unknown content types",
                2, types.size());
    }

    /**
     * Tests that the extractor correctly handles an empty stream.
     */
    public void testEmptyStream() {
        try {
            Reader reader = extractor.extractText(
                    new ByteArrayInputStream(new byte[0]), "text/xml", null);
            assertEquals("", ExtractorHelper.read(reader));
        } catch (IOException e) {
            fail("XMLTextExtractor does not handle empty streams");
        }
    }

    /**
     * Tests that the extractor correctly handles a normal stream.
     *
     * @throws IOException on IO errors
     */
    public void testNormalStream() throws IOException {
        String xml = "<a b=\"attribute value\">text content</a>";
        Reader reader = extractor.extractText(
                new ByteArrayInputStream(xml.getBytes()), "text/xml", null);
        assertEquals("attribute value text content", ExtractorHelper.read(reader));
    }

    /**
     * Tests that the extractor correctly handles XML parse errors.
     */
    public void testInvalidStream() {
        try {
            String xml = "<a b=\"attribute value\">text content</c>";
            Reader reader = extractor.extractText(
                    new ByteArrayInputStream(xml.getBytes()), "text/xml", null);
            assertEquals("", ExtractorHelper.read(reader));
        } catch (IOException e) {
            fail("XMLTextExtractor does not handle XML parse errors");
        }
    }

    /**
     * Tests that the extractor correctly handles unsupported encodings.
     */
    public void testUnsupportedEncoding() {
        try {
            String xml = "<a b=\"attribute value\">text content</a>";
            Reader reader = extractor.extractText(
                    new ByteArrayInputStream(xml.getBytes()),
                    "text/xml", "unsupported");
            assertEquals("", ExtractorHelper.read(reader));
        } catch (UnsupportedEncodingException e) {
            fail("XMLTextExtractor does not handle unsupported encodings");
        } catch (IOException e) {
            fail("XMLTextExtractor does not handle unsupported encodings");
        }
    }

}
