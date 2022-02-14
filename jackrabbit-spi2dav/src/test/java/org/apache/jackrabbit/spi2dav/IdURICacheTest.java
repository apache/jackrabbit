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
package org.apache.jackrabbit.spi2dav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class IdURICacheTest {

    @Test
    public void testException() {

        String wspuri = "https://example.org/foo/";
        IdURICache cache = new IdURICache(wspuri);
        String test;

        // port number
        test = "https://example.org:443/foo/x";
        try {
            cache.add(test, null);
            fail("should throw");
        } catch (IllegalArgumentException ex) {
            assertEquals("Workspace mismatch: '" + test + "' not under workspace '" + wspuri
                    + "' (position 18: '{https://example.org}:443/foo/x', expected: '/foo/')", ex.getMessage());
        }

        // protocol
        test = "http://example.org/foo/x";
        try {
            cache.add(test, null);
            fail("should throw");
        } catch (IllegalArgumentException ex) {
            assertEquals("Workspace mismatch: '" + test + "' not under workspace '" + wspuri
                    + "' (position 3: '{http}://example.org/foo/x', expected: 's://example.org/foo/')", ex.getMessage());
        }

        // hostname
        test = "https://example.com/foo/x";
        try {
            cache.add(test, null);
            fail("should throw");
        } catch (IllegalArgumentException ex) {
            assertEquals("Workspace mismatch: '" + test + "' not under workspace '" + wspuri
                    + "' (position 15: '{https://example.}com/foo/x', expected: 'org/foo/')", ex.getMessage());
        }

        // root path
        test = "https://example.org/bar/x";
        try {
            cache.add(test, null);
            fail("should throw");
        } catch (IllegalArgumentException ex) {
            assertEquals("Workspace mismatch: '" + test + "' not under workspace '" + wspuri
                    + "' (position 19: '{https://example.org/}bar/x', expected: 'foo/')", ex.getMessage());
        }

        // too short
        test = "https://example.org/fo/x";
        try {
            cache.add(test, null);
            fail("should throw");
        } catch (IllegalArgumentException ex) {
            assertEquals("Workspace mismatch: '" + test + "' not under workspace '" + wspuri
                    + "' (position 21: '{https://example.org/fo}/x', expected: 'o/')", ex.getMessage());
        }

        // way too short
        test = "https://x.org/foo/x";
        try {
            cache.add(test, null);
            fail("should throw");
        } catch (IllegalArgumentException ex) {
            assertEquals("Workspace mismatch: '" + test + "' not under workspace '" + wspuri
                    + "' (position 7: '{https://}x.org/foo/x', expected: 'example.org/foo/')", ex.getMessage());
        }
    }
}
