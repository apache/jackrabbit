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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.jackrabbit.spi.commons.name.JcrPath;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;

import javax.jcr.RepositoryException;

/**
 * PathParserTest
 */
public class PathParserTest extends TestCase {

    private final PathFactory factory = PathFactoryImpl.getInstance();
    private final NameResolver resolver;
    private final PathResolver pathResolver;

    private JcrPath[] tests = JcrPath.getTests();

    private static int NUM_TESTS = 1;

    public PathParserTest() {
        resolver = new ParsingNameResolver(NameFactoryImpl.getInstance(), new DummyNamespaceResolver());
        pathResolver = new ParsingPathResolver(factory, resolver);
    }

    public void testParse() throws Exception {
        for (int i=0; i<tests.length; i++) {
            JcrPath t = tests[i];
            long t1 = System.currentTimeMillis();
            for (int j=0; j<NUM_TESTS; j++) {
                try {
                    Path p = PathParser.parse(t.path, resolver, factory);
                    if (t.normalizedPath==null) {
                        if (!t.isValid()) {
                            fail("Should throw IllegalArgumentException: " + t.path);
                        }
                        assertEquals("\"" + t.path + "\".create(false)", t.path,  pathResolver.getJCRPath(p));
                        assertEquals("\"" + t.path + "\".isNormalized()", t.isNormalized(), p.isNormalized());
                        assertEquals("\"" + t.path + "\".isAbsolute()", t.isAbsolute(), p.isAbsolute());
                    } else {
                        // check with normalization
                        p = p.getNormalizedPath();
                        if (!t.isValid()) {
                            fail("Should throw IllegalArgumentException: " + t.path);
                        }
                        assertEquals("\"" + t.path + "\".create(true)", t.normalizedPath, pathResolver.getJCRPath(p));
                        assertEquals("\"" + t.path + "\".isAbsolute()", t.isAbsolute(), p.isAbsolute());
                    }
                } catch (Exception e) {
                    if (t.isValid()) {
                        System.out.println(t.path);
                        throw e;
                    }
                }
            }
            long t2 = System.currentTimeMillis();
            if (NUM_TESTS>1) {
                System.out.println("testCreate():\t" + t + "\t" + (t2-t1) + "\tms");
            }
        }
    }

    public void testCheckFormat() throws Exception {
        for (int i=0; i<tests.length; i++) {
            JcrPath t = tests[i];
            long t1 = System.currentTimeMillis();
            for (int j=0; j<NUM_TESTS; j++) {
                if (t.normalizedPath==null) {
                    // check just creation
                    boolean isValid = true;
                    try {
                        PathParser.checkFormat(t.path);
                    } catch (MalformedPathException e) {
                        isValid = false;
                    }
                    assertEquals("\"" + t.path + "\".checkFormat()", t.isValid(),  isValid);
                }
            }
            long t2 = System.currentTimeMillis();
            if (NUM_TESTS>1) {
                System.out.println("testCheckFormat():\t" + t + "\t" + (t2-t1) + "\tms");
            }
        }
    }

    public void testNormalizedPaths() throws Exception {
        List paths = new ArrayList();
        // normalized paths
        paths.add(PathParser.parse("/", resolver, factory));
        paths.add(PathParser.parse("/foo", resolver, factory));
        paths.add(PathParser.parse("/foo/bar", resolver, factory));
        paths.add(PathParser.parse("foo/bar", resolver, factory));
        paths.add(PathParser.parse("foo", resolver, factory));
        paths.add(PathParser.parse("../../foo/bar", resolver, factory));
        paths.add(PathParser.parse("..", resolver, factory));
        paths.add(PathParser.parse(".", resolver, factory));

        for (Iterator it = paths.iterator(); it.hasNext(); ) {
            Path path = (Path) it.next();
            assertTrue("path is not normalized: " + path, path.isNormalized());
        }

        paths.clear();

        // not normalized paths
        paths.add(PathParser.parse("/foo/..", resolver, factory));
        paths.add(PathParser.parse("/foo/.", resolver, factory));
        paths.add(PathParser.parse("/foo/../bar", resolver, factory));
        paths.add(PathParser.parse("/foo/./bar", resolver, factory));
        paths.add(PathParser.parse("./foo", resolver, factory));
        paths.add(PathParser.parse("foo/..", resolver, factory));
        paths.add(PathParser.parse("../foo/..", resolver, factory));
        paths.add(PathParser.parse("../foo/.", resolver, factory));

        for (Iterator it = paths.iterator(); it.hasNext(); ) {
            Path path = (Path) it.next();
            assertFalse("path is normalized: " + path, path.isNormalized());
        }
    }

    public void testAbsolutePaths() throws Exception {
        List paths = new ArrayList();

        // absolute paths
        paths.add(PathParser.parse("/", resolver, factory));
        paths.add(PathParser.parse("/foo", resolver, factory));
        paths.add(PathParser.parse("/foo/bar", resolver, factory));
        paths.add(PathParser.parse("/foo/../bar", resolver, factory));
        paths.add(PathParser.parse("/foo/..", resolver, factory));
        paths.add(PathParser.parse("/foo/./bar", resolver, factory));
        paths.add(PathParser.parse("/foo/.././bar/./foo", resolver, factory));

        for (Iterator it = paths.iterator(); it.hasNext(); ) {
            Path path = (Path) it.next();
            assertTrue("path is not absolute: " + path, path.isAbsolute());
        }

        paths.clear();

        // not absoulute paths
        paths.add(PathParser.parse("foo/..", resolver, factory));
        paths.add(PathParser.parse("foo/.", resolver, factory));
        paths.add(PathParser.parse("foo/../bar", resolver, factory));
        paths.add(PathParser.parse("foo/./bar", resolver, factory));
        paths.add(PathParser.parse("./foo", resolver, factory));
        paths.add(PathParser.parse(".", resolver, factory));
        paths.add(PathParser.parse("foo/..", resolver, factory));
        paths.add(PathParser.parse("../foo/..", resolver, factory));
        paths.add(PathParser.parse("../foo/.", resolver, factory));

        for (Iterator it = paths.iterator(); it.hasNext(); ) {
            Path path = (Path) it.next();
            assertFalse("path is absolute: " + path, path.isAbsolute());
        }
    }

    public void testCanonicalPaths() throws Exception {
        List paths = new ArrayList();

        // canonical paths
        paths.add(PathParser.parse("/", resolver, factory));
        paths.add(PathParser.parse("/foo", resolver, factory));
        paths.add(PathParser.parse("/foo/bar", resolver, factory));

        for (Iterator it = paths.iterator(); it.hasNext(); ) {
            Path path = (Path) it.next();
            assertTrue("path is not canonical: " + path, path.isCanonical());
        }

        paths.clear();

        // not canonical paths
        paths.add(PathParser.parse("/foo/..", resolver, factory));
        paths.add(PathParser.parse("/foo/.", resolver, factory));
        paths.add(PathParser.parse("/foo/../bar", resolver, factory));
        paths.add(PathParser.parse("/foo/./bar", resolver, factory));
        paths.add(PathParser.parse("./foo", resolver, factory));
        paths.add(PathParser.parse(".", resolver, factory));
        paths.add(PathParser.parse("/foo/..", resolver, factory));
        paths.add(PathParser.parse("/../foo/.", resolver, factory));

        for (Iterator it = paths.iterator(); it.hasNext(); ) {
            Path path = (Path) it.next();
            assertFalse("path is canonical: " + path, path.isCanonical());
        }
    }

    public void testIdentifierParse() throws RepositoryException {
        DummyIdentifierResolver idResolver = new DummyIdentifierResolver();
        List valid = idResolver.getValidIdentifiers();
        for (Iterator it = valid.iterator(); it.hasNext();) {
            String jcrPath = "[" + it.next() + "]";
            try {                
                PathParser.parse(jcrPath, resolver, factory);
                fail("Parsing an identifier-based jcr path needs a IdentifierResolver");
            } catch (MalformedPathException e) {
                // success: cannot parse identifier path if idResolver is missing.
            }
            try {
                PathParser.parse(factory.getRootPath(), jcrPath, resolver, factory);
                fail("Parsing an identifier-based jcr path needs a IdentifierResolver");
            } catch (MalformedPathException e) {
                // success: cannot parse identifier path if idResolver is missing.
            }

            Path p = PathParser.parse(jcrPath, resolver, idResolver, factory, true);
            assertFalse(p.denotesIdentifier());

            p = PathParser.parse(jcrPath, resolver, idResolver, factory, false);
            assertTrue(p.denotesIdentifier());

            try {
                PathParser.parse(factory.getRootPath(), jcrPath, resolver, idResolver, factory);
                fail("Cannot parser an identifier-based path to a relative path.");
            } catch (MalformedPathException e) {
                // success: invalid argument parent-path if the jcr-path is an identifier-based path.
            }

            try {
                PathParser.parse(jcrPath, resolver, factory);
                fail("Parsing an identifier-based jcr path needs a IdentifierResolver");
            } catch (MalformedPathException e) {
                // success: cannot parse identifier path if idResolver is missing.
            }
        }
    }

    public void testInvalidIdentifierParse() throws RepositoryException {
        DummyIdentifierResolver idResolver = new DummyIdentifierResolver();

        List invalid = idResolver.getInvalidIdentifierPaths();
        for (Iterator it = invalid.iterator(); it.hasNext();) {
            String jcrPath = it.next().toString();
            try {
                Path p = PathParser.parse(jcrPath, resolver, idResolver, factory, true);
                fail("Invalid identifier based path");
            } catch (MalformedPathException e) {
                // ok
            }
            try {
                Path p = PathParser.parse(jcrPath, resolver, idResolver, factory, false);
                fail("Invalid identifier based path");
            } catch (MalformedPathException e) {
                // ok
            }
        }       
    }
    
    public void testIdentifierCheckFormat() throws RepositoryException {
        DummyIdentifierResolver idResolver = new DummyIdentifierResolver();
        List valid = idResolver.getValidIdentifiers();
        for (Iterator it = valid.iterator(); it.hasNext();) {
            String jcrPath = "[" + it.next() + "]";
            PathParser.checkFormat(jcrPath);
        }

        List invalid = idResolver.getInvalidIdentifierFormats();
        for (Iterator it = invalid.iterator(); it.hasNext();) {
            String jcrPath = it.next().toString();
            try {
                // passing null-nameResolver -> executes check-format only
                PathParser.checkFormat(jcrPath);
                fail(jcrPath);
            } catch (MalformedPathException e) {
                // success
            }
        }
    }
}
