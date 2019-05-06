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
package org.apache.jackrabbit.spi.commons.conversion;

import junit.framework.TestCase;

import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;

import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.Path;

import java.util.List;
import java.util.Iterator;

/**
 * Test cases for the {@link ParsingPathResolver} class.
 */
public class ParsingPathResolverTest extends TestCase {

    private static NameResolver nameResolver = new ParsingNameResolver(NameFactoryImpl.getInstance(), new DummyNamespaceResolver());
    private DummyIdentifierResolver idResolver;

    /**
     * Path resolver being tested.
     */
    private PathResolver resolver = new ParsingPathResolver(PathFactoryImpl.getInstance(), nameResolver);
    private PathResolver resolverV2;


    protected void setUp() throws Exception {
        super.setUp();
        idResolver = new DummyIdentifierResolver();
        resolverV2 = new ParsingPathResolver(PathFactoryImpl.getInstance(), nameResolver, idResolver);
    }

    /**
     * Checks that the given path resolves properly.
     *
     * @param path JCR path
     */
    private void assertValidPath(String path) {
        assertValidPath(path, path);
    }

    private void assertValidPath(String path, String expectedResult) {
        try {
            Path qpath = resolver.getQPath(path);
            assertEquals(path, expectedResult, resolver.getJCRPath(qpath));
        } catch (RepositoryException e) {
            fail(path);
        }

        try {
            Path qpath = resolverV2.getQPath(path);
            assertEquals(path, expectedResult, resolver.getJCRPath(qpath));
        } catch (RepositoryException e) {
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
        } catch (RepositoryException e) {
            // success
        }
        try {
            resolverV2.getQPath(path);
            fail(path);
        } catch (RepositoryException e) {
            // success
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

        // trailing slash is valid
        assertValidPath("a/", "a");
        assertValidPath("prefix:name/", "prefix:name");
        assertValidPath("/prefix:name[1]/", "/prefix:name");
        assertValidPath("/a/../b/", "/a/../b");

        assertValidPath("/a/../b");
        assertValidPath("./../.");
        assertValidPath("/a/./b");
        assertValidPath("/a/b/../..");
        assertValidPath("/a/b/c/../d/..././f");
        assertValidPath("../a/b/../../../../f");
        assertValidPath("a/../..");
        assertValidPath("../../a/.");
    }

    /**
     * Tests that resolution of invalid paths fails.
     */
    public void testInvalidPaths() {
        assertInvalidPath("");
        assertInvalidPath("//");
        assertInvalidPath("x:");
        assertInvalidPath("x:/");
        assertInvalidPath("x[]");
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

    public void testValidIdentifierPaths() throws MalformedPathException, IllegalNameException, NamespaceException {
        for (Iterator it = idResolver.getValidIdentifiers().iterator(); it.hasNext();) {
            String jcrPath = "[" + it.next().toString() + "]";

            Path p = resolverV2.getQPath(jcrPath, true);
            assertFalse(p.isIdentifierBased());
            assertTrue(p.isAbsolute());
            assertTrue(p.isNormalized());
            assertTrue(p.isCanonical());
            assertEquals(DummyIdentifierResolver.JCR_PATH, resolverV2.getJCRPath(p));

            p = resolverV2.getQPath(jcrPath, false);
            assertTrue(p.isIdentifierBased());
            assertEquals(1, p.getLength());
            assertTrue(p.isAbsolute());
            assertFalse(p.isNormalized());
            assertTrue(p.isCanonical());
            assertEquals(jcrPath, resolverV2.getJCRPath(p));
        }
    }

    public void testInvalidIdentifierPaths() throws MalformedPathException, IllegalNameException, NamespaceException {
        List l = idResolver.getInvalidIdentifierPaths();

        for (Iterator it = l.iterator(); it.hasNext();) {
            String path = it.next().toString();
            try {
                resolverV2.getQPath(path);
                fail(path);
            } catch (MalformedPathException e) {
                // success
            }
        }
    }
}
