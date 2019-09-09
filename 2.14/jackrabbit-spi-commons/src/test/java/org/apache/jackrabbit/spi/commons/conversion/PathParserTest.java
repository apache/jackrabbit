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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.JcrPath;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

import javax.jcr.NamespaceException;
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
        for (JcrPath t : tests) {
            long t1 = System.currentTimeMillis();
            for (int j = 0; j < NUM_TESTS; j++) {
                try {
                    Path p = PathParser.parse(t.path, resolver, factory);
                    if (t.normalizedPath == null) {
                        if (!t.isValid()) {
                            fail("Should throw IllegalArgumentException: " + t.path);
                        }
                        assertEquals("\"" + t.path + "\".create(false)", t.path, pathResolver.getJCRPath(p));
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
            if (NUM_TESTS > 1) {
                System.out.println("testCreate():\t" + t + "\t" + (t2 - t1) + "\tms");
            }
        }
    }

    public void testCheckFormat() throws Exception {
        for (JcrPath t : tests) {
            long t1 = System.currentTimeMillis();
            for (int j = 0; j < NUM_TESTS; j++) {
                if (t.normalizedPath == null) {
                    // check just creation
                    boolean isValid = true;
                    try {
                        PathParser.checkFormat(t.path);
                    } catch (MalformedPathException e) {
                        isValid = false;
                    }
                    assertEquals("\"" + t.path + "\".checkFormat()", t.isValid(), isValid);
                }
            }
            long t2 = System.currentTimeMillis();
            if (NUM_TESTS > 1) {
                System.out.println("testCheckFormat():\t" + t + "\t" + (t2 - t1) + "\tms");
            }
        }
    }

    public void testNormalizedPaths() throws Exception {
        List<Path> paths = new ArrayList<Path>();
        // normalized paths
        paths.add(PathParser.parse("/", resolver, factory));
        paths.add(PathParser.parse("/foo", resolver, factory));
        paths.add(PathParser.parse("/foo/bar", resolver, factory));
        paths.add(PathParser.parse("foo/bar", resolver, factory));
        paths.add(PathParser.parse("foo", resolver, factory));
        paths.add(PathParser.parse("../../foo/bar", resolver, factory));
        paths.add(PathParser.parse("..", resolver, factory));
        paths.add(PathParser.parse(".", resolver, factory));

        for (Path path : paths) {
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

        for (Path path : paths) {
            assertFalse("path is normalized: " + path, path.isNormalized());
        }
    }

    public void testAbsolutePaths() throws Exception {
        List<Path> paths = new ArrayList<Path>();

        // absolute paths
        paths.add(PathParser.parse("/", resolver, factory));
        paths.add(PathParser.parse("/foo", resolver, factory));
        paths.add(PathParser.parse("/foo/bar", resolver, factory));
        paths.add(PathParser.parse("/foo/../bar", resolver, factory));
        paths.add(PathParser.parse("/foo/..", resolver, factory));
        paths.add(PathParser.parse("/foo/./bar", resolver, factory));
        paths.add(PathParser.parse("/foo/.././bar/./foo", resolver, factory));

        for (Path path : paths) {
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

        for (Path path : paths) {
            assertFalse("path is absolute: " + path, path.isAbsolute());
        }
    }

    public void testExpandedPaths() throws Exception {
        Map<String,Path> paths = new HashMap<String,Path>();

        NameResolver ns = new ParsingNameResolver(NameFactoryImpl.getInstance(), new TestNamespaceResolver());
        PathResolver ps = new ParsingPathResolver(factory, ns);

        // expanded paths
        paths.put("/",PathParser.parse("/", ns, factory));
        paths.put("/foo",PathParser.parse("/{}foo", ns, factory));
        paths.put("/a:foo/a:bar",PathParser.parse("/{http://jackrabbit.apache.org}foo/{http://jackrabbit.apache.org}bar", ns, factory));
        paths.put("/a:foo/../a:bar",PathParser.parse("/{http://jackrabbit.apache.org}foo/../{http://jackrabbit.apache.org}bar", ns, factory));
        paths.put("/foo/..",PathParser.parse("/{}foo/..", ns, factory));
        paths.put("/a:foo/./a:bar",PathParser.parse("/{http://jackrabbit.apache.org}foo/./{http://jackrabbit.apache.org}bar", ns, factory));
        paths.put("/a:foo/.././a:bar/./a:foo",PathParser.parse("/{http://jackrabbit.apache.org}foo/.././{http://jackrabbit.apache.org}bar/./{http://jackrabbit.apache.org}foo", resolver, factory));
        paths.put("/foo/.{a}/a:b",PathParser.parse("/{}foo/{}.{a}/{http://jackrabbit.apache.org}b", ns, factory));
        paths.put("/foo/.{.}/a:c",PathParser.parse("/{}foo/{}.{.}/{http://jackrabbit.apache.org}c", ns, factory));
        paths.put("foo/.{.}/a:c",PathParser.parse("{}foo/{}.{.}/{http://jackrabbit.apache.org}c", ns, factory));

        for (String key : paths.keySet()) {
            Path path = paths.get(key);
            assertEquals(key, ps.getJCRPath(path));
        }
    }

    public void testJCRPaths() throws Exception {
        Map<String,Path> paths = new HashMap<String,Path>();
        paths.put("/",PathParser.parse("/", resolver, factory));
        paths.put("/foo",PathParser.parse("/foo", resolver, factory));
        paths.put("/a:foo/a:bar",PathParser.parse("/a:foo/a:bar", resolver, factory));
        paths.put("/a:foo/../a:bar",PathParser.parse("/a:foo/../a:bar", resolver, factory));
        paths.put("/a:foo/..",PathParser.parse("/a:foo/..", resolver, factory));
        paths.put("/a:foo/./a:bar",PathParser.parse("/a:foo/./a:bar", resolver, factory));
        paths.put("/a:foo/.././a:bar/./a:foo",PathParser.parse("/a:foo/.././a:bar/./a:foo", resolver, factory));
        paths.put("foo/..",PathParser.parse("foo/..", resolver, factory));
        paths.put("foo/.",PathParser.parse("foo/.", resolver, factory));
        paths.put("foo/../bar",PathParser.parse("foo/../bar", resolver, factory));
        paths.put("foo/./bar",PathParser.parse("foo/./bar", resolver, factory));
        paths.put("./foo",PathParser.parse("./foo", resolver, factory));
        paths.put(".",PathParser.parse(".", resolver, factory));
        paths.put("foo/..",PathParser.parse("foo/..", resolver, factory));
        paths.put("../foo/..",PathParser.parse("../foo/..", resolver, factory));
        paths.put("../foo/.",PathParser.parse("../foo/.", resolver, factory));
        paths.put("/foo/.{a}/a:b",PathParser.parse("/foo/.{a}/a:b", resolver, factory));
        paths.put("/a:foo/.{.}/a:c",PathParser.parse("/a:foo/.{.}/a:c", resolver, factory));
        paths.put("/a:foo/{.}/a:c",PathParser.parse("/a:foo/{.}/a:c", resolver, factory));
        paths.put("/a:foo/{..}/a:c",PathParser.parse("/a:foo/{..}/a:c", resolver, factory));
        paths.put("/a:foo/{...}/a:c",PathParser.parse("/a:foo/{...}/a:c", resolver, factory));
        paths.put("/a:foo/.{.}/a:c",PathParser.parse("/a:foo/.{.}/a:c", resolver, factory));
        paths.put("/a:foo/.{.}/a:c",PathParser.parse("/a:foo/.{.}/a:c", resolver, factory));
        paths.put(".{a}",PathParser.parse(".{a}", resolver, factory));
        paths.put(".{.}",PathParser.parse(".{.}", resolver, factory));
        paths.put("..{.}",PathParser.parse("..{.}", resolver, factory));
        paths.put("..{..}",PathParser.parse("..{..}", resolver, factory));
        paths.put(".{...}",PathParser.parse(".{...}", resolver, factory));
        paths.put("{...}",PathParser.parse("{...}", resolver, factory));
        paths.put("...",PathParser.parse("...", resolver, factory));
        paths.put("a:.{.}",PathParser.parse("a:.{.}", resolver, factory));
        paths.put("..{a}",PathParser.parse("..{a}", resolver, factory));
        paths.put(".{..}",PathParser.parse(".{..}", resolver, factory));
        paths.put("a:..{.}",PathParser.parse("a:..{.}", resolver, factory));
        paths.put("a:.{..}",PathParser.parse("a:.{..}", resolver, factory));
        paths.put("a:..{..}",PathParser.parse("a:..{..}", resolver, factory));
        paths.put(".a",PathParser.parse(".a", resolver, factory));
        paths.put("..a",PathParser.parse("..a", resolver, factory));

        for (String key : paths.keySet()) {
            Path path = paths.get(key);
            assertEquals(key, pathResolver.getJCRPath(path));
        }
    }

    public void testInvalidJCRPaths() throws Exception {
        List<String> paths = new ArrayList<String>();
        paths.add("/a:..");
        paths.add("/a:.");
        paths.add("/a::");
        paths.add("/a:{:a}");
        paths.add("/.{:a}");
        paths.add("/.{a:a}");
        paths.add("/:");
        paths.add("/*");
        paths.add("//");
        paths.add("foo\u3000bar"); // non-ASCII whitespace

        for (String jcrPath : paths) {
            try {
                PathParser.parse(jcrPath, resolver, factory);
                fail(jcrPath + " isn't a valid jcr path");
            } catch (MalformedPathException e) {
                // ok.
            }
        }
    }

    public void testCanonicalPaths() throws Exception {
        List<Path> paths = new ArrayList<Path>();

        // canonical paths
        paths.add(PathParser.parse("/", resolver, factory));
        paths.add(PathParser.parse("/foo", resolver, factory));
        paths.add(PathParser.parse("/foo/bar", resolver, factory));

        for (Path path : paths) {
            assertTrue("path is not canonical: " + path, path.isCanonical());
        }

        paths.clear();

        // non-canonical paths
        paths.add(PathParser.parse("/foo/..", resolver, factory));
        paths.add(PathParser.parse("/foo/.", resolver, factory));
        paths.add(PathParser.parse("/foo/../bar", resolver, factory));
        paths.add(PathParser.parse("/foo/./bar", resolver, factory));
        paths.add(PathParser.parse("./foo", resolver, factory));
        paths.add(PathParser.parse(".", resolver, factory));
        paths.add(PathParser.parse("/foo/..", resolver, factory));

        for (Path path : paths) {
            assertFalse("path is canonical: " + path, path.isCanonical());
        }
    }

    public void testIdentifierParse() throws RepositoryException {
        DummyIdentifierResolver idResolver = new DummyIdentifierResolver();
        List<String> valid = idResolver.getValidIdentifiers();
        for (String id : valid) {
            String jcrPath = "[" + id + "]";
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
            assertFalse(p.isIdentifierBased());

            p = PathParser.parse(jcrPath, resolver, idResolver, factory, false);
            assertTrue(p.isIdentifierBased());

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

    public void testIdentifierParseWithTrailingString() throws RepositoryException {
        List<String> suffix = new ArrayList<String>();
        suffix.add("/");          // additional path delimiter
        suffix.add("/property");  // additional path segment
        suffix.add("suffix");     // trailing string
        suffix.add("[1]");        // an index

        DummyIdentifierResolver idResolver = new DummyIdentifierResolver();
        List<String> valid = idResolver.getValidIdentifiers();
        for (String id : valid) {
            for (String s : suffix) {
                String jcrPath = "[" + id + "]" + s;
                try {
                    PathParser.parse(jcrPath, resolver, idResolver, factory, true);
                } catch (MalformedPathException e) {
                    // success
                }

                try {
                    PathParser.parse(jcrPath, resolver, idResolver, factory, false);
                } catch (MalformedPathException e) {
                    // success
                }
            }
        }
    }

    public void testInvalidIdentifierParse() throws RepositoryException {
        DummyIdentifierResolver idResolver = new DummyIdentifierResolver();

        List<String> invalid = idResolver.getInvalidIdentifierPaths();
        for (String jcrPath : invalid) {
            try {
                PathParser.parse(jcrPath, resolver, idResolver, factory, true);
                fail("Invalid identifier based path");
            } catch (MalformedPathException e) {
                // ok
            }
            try {
                PathParser.parse(jcrPath, resolver, idResolver, factory, false);
                fail("Invalid identifier based path");
            } catch (MalformedPathException e) {
                // ok
            }
        }
    }

    public void testIdentifierCheckFormat() throws RepositoryException {
        DummyIdentifierResolver idResolver = new DummyIdentifierResolver();
        List<String> valid = idResolver.getValidIdentifiers();
        for (String id : valid) {
            String jcrPath = "[" + id + "]";
            PathParser.checkFormat(jcrPath);
        }

        List<String> invalid = idResolver.getInvalidIdentifierFormats();
        for (String jcrPath : invalid) {
            try {
                // passing null-nameResolver -> executes check-format only
                PathParser.checkFormat(jcrPath);
                fail(jcrPath);
            } catch (MalformedPathException e) {
                // success
            }
        }
    }


    /**
     * Dummy NamespaceResolver that only knows the empty namespace and
     * namespaces containing either 'jackrabbit' or 'abc'. Used to test
     * the parsing of the expanded jcr names, which should treat a jcr name with
     * unknown namespace uri qualified jcr names.
     */
    private class TestNamespaceResolver implements NamespaceResolver {

        public String getURI(String prefix) throws NamespaceException {
            if (Name.NS_EMPTY_PREFIX.equals(prefix)) {
                return Name.NS_DEFAULT_URI;
            } else if ("a".equals(prefix)) {
                return "http://jackrabbit.apache.org";
            } else {
                throw new NamespaceException("Unknown namespace prefix " + prefix);
            }
        }

        public String getPrefix(String uri) throws NamespaceException {
            if (Name.NS_DEFAULT_URI.equals(uri)) {
                return Name.NS_EMPTY_PREFIX;
            } else if ("http://jackrabbit.apache.org".equals(uri)) {
                return "a";
            } else {
                throw new NamespaceException("Unknown namespace prefix " + uri);
            }
        }
    }
}
