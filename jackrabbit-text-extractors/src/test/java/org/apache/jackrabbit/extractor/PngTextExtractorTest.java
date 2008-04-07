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

public class PngTextExtractorTest extends TestCase {

    /**
     * Text extractor being tested.
     */
    private TextExtractor extractor;

    /**
     * Creates the text extractor to be tested.
     */
    protected void setUp() throws Exception {
        super.setUp();
        extractor = new PngTextExtractor();
    }

    /**
     * Tests that the extractor supportes <code>image/png</code>,
     * <code>image/apng</code> and <code>image/mng</code>.
     */
    public void testContentTypes() {
        Set types = new HashSet();
        types.addAll(Arrays.asList(extractor.getContentTypes()));
        assertTrue("PngTextExtractor does not support image/png",
                types.contains("image/png"));
        assertTrue("PngTextExtractor does not support image/apng",
                types.contains("image/apng"));
        assertTrue("PngTextExtractor does not support image/mng",
                types.contains("image/mng"));
        assertEquals("PngTextExtractor supports unknown content types",
                3, types.size());
    }

    /**
     * Tests that the extractor correctly handles an empty stream.
     */
    public void testEmptyStream() {
        try {
            Reader reader = extractor.extractText(new ByteArrayInputStream(new byte[0]), "image/png", null);
            assertEquals("", ExtractorHelper.read(reader));
        } catch (IOException e) {
            fail("PngTextExtractor does not handle empty streams");
        }
    }

    /**
     * Tests that the extractor correctly handles a normal stream.
     *
     * @throws IOException on IO errors
     */
    public void testNormalStream() throws IOException {
        byte[] png = {-119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82,
                      0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0, 31, 21, -60,
                      -119, 0, 0, 0, 6, 98, 75, 71, 68, 0, -1, 0, -1, 0, -1, -96,
                      -67, -89, -109, 0, 0, 0, 9, 112, 72, 89, 115, 0, 0, 11, 19, 0,
                      0, 11, 19, 1, 0, -102, -100, 24, 0, 0, 0, 7, 116, 73, 77, 69,
                      7, -40, 4, 6, 5, 59, 15, 72, -108, -3, -68, 0, 0, 0, 52, 116,
                      69, 88, 116, 67, 111, 109, 109, 101, 110, 116, 0, 84, 104, 101, 32, 113,
                      117, 105, 99, 107, 32, 98, 114, 111, 119, 110, 32, 102, 111, 120, 32, 106,
                      117, 109, 112, 115, 32, 111, 118, 101, 114, 32, 116, 104, 101, 32, 108, 97,
                      122, 121, 32, 100, 111, 103, 46, 55, 79, -28, -66, 0, 0, 0, 13, 73,
                      68, 65, 84, 8, -41, 99, -8, -33, -64, -16, 31, 0, 6, -128, 2, 127,
                      -21, 73, 116, -101, 0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126};

        Reader reader = extractor.extractText(new ByteArrayInputStream(png), "image/png", null);
        assertEquals("Comment: The quick brown fox jumps over the lazy dog.", ExtractorHelper.read(reader));
    }

}
