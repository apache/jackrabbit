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
package org.apache.jackrabbit.name;

import junit.framework.TestCase;

import javax.jcr.NamespaceException;

/**
 * Test cases for the {@link ParsingPathResolver} class.
 */
public class ParsingPathResolverTest extends TestCase {

    /**
     * Path resolver being tested.
     */
    private PathResolver resolver = new ParsingPathResolver(
            new ParsingNameResolver(new DummyNamespaceResolver()));

    /**
     * Checks that the given path resolves properly.
     *
     * @param path JCR path
     */
    private void assertValidPath(String path) {
        try {
            Path qpath = resolver.getQPath(path);
            assertEquals(path, path, resolver.getJCRPath(qpath));
        } catch (NameException e) {
            fail(path);
        } catch (NamespaceException e) {
            fail(path);
        }
    }

    /**
     * Checks that the given path fails to resolve.
     *
     * @param path JCR path
     */
    private void assertInvalidPath(String path) {
        try {
            resolver.getQPath(path);
            fail(path);
        } catch (NameException e) {
        } catch (NamespaceException e) {
        }
    }

    /**
     * Tests that valid paths are properly resolved.
     */
    public void testValidPaths() {
        assertValidPath("/");
        assertValidPath(".");
        assertValidPath("..");
        assertValidPath("x");
        assertValidPath("x:y");
        assertValidPath("x[2]");
        assertValidPath("x:y[123]");

        assertValidPath("/a/b/c");
        assertValidPath("/prefix:name/prefix:name");
        assertValidPath("/name[2]/name[2]");
        assertValidPath("/prefix:name[2]/prefix:name[2]");

        assertValidPath("a/b/c");
        assertValidPath("prefix:name/prefix:name");
        assertValidPath("name[2]/name[2]");
        assertValidPath("prefix:name[2]/prefix:name[2]");

        assertValidPath("/a/../b");
        assertValidPath("./../.");
        assertValidPath("/a/./b");
        assertValidPath("/a/b/../..");
        assertValidPath("/a/b/c/../d/..././f");
        assertValidPath("../a/b/../../../../f");
        assertValidPath("a/../..");
        assertValidPath("../../a/.");

        // TODO: Should these paths be detected as invalid by the parser?
        assertValidPath("/..");
        assertValidPath("/a/b/../../..");
    }

    /**
     * Tests that resolution of invalid paths fails.
     */
    public void testInvalidPaths() {
        assertInvalidPath("");
        assertInvalidPath("//");
        assertInvalidPath("x/");
        assertInvalidPath("x:");
        assertInvalidPath("x:/");
        assertInvalidPath("x[]");
        assertInvalidPath("x:y/");
        assertInvalidPath("x:y[1]/");
        assertInvalidPath("x:y[");
        assertInvalidPath("x:y[]");
        assertInvalidPath("x:y[1");
        assertInvalidPath("x:y[1]2");
        assertInvalidPath("x:y[1]]");
        assertInvalidPath("x:y[[1]");
        assertInvalidPath(" /a/b/c/");
        assertInvalidPath("/a/b/c/ ");
        assertInvalidPath("/:name/prefix:name");
        assertInvalidPath("/prefix:name ");
        assertInvalidPath("/prefix: name");
        assertInvalidPath("/ prefix:name");
        assertInvalidPath("/prefix : name");
        assertInvalidPath("/name[0]/name[2]");
        assertInvalidPath("/prefix:name[2]foo/prefix:name[2]");
        assertInvalidPath(":name/prefix:name");
        assertInvalidPath("name[0]/name[2]");
        assertInvalidPath("prefix:name[2]foo/prefix:name[2]");
    }

}
