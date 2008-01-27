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
import org.apache.jackrabbit.util.Text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This Class implements a test case for the 'Path' class.
 *
 * Actually, this should be below the {@link org.apache.jackrabbit.test} package,
 * but it needs package protected methods of that class.
 */
public class PathTest extends TestCase {

    private final NamespaceResolver resolver;

    private Test[] tests;

    private static final int ABS = 1;
    private static final int NOR = 2;
    private static final int VAL = 4;

    private static int NUM_TESTS = 1;

    public PathTest() {

        // create dummy namespace resolver
        resolver = new AbstractNamespaceResolver(){
            public String getURI(String prefix) {
                return prefix;
            }

            public String getPrefix(String uri) {
                return uri;
            }
        };

        // create tests
        ArrayList list = new ArrayList();
        // absolute paths
        list.add(new Test("/", NOR|VAL));
        list.add(new Test("/", NOR|VAL));
        list.add(new Test("/", NOR|VAL));
        list.add(new Test("/a/b/c", NOR|VAL));
        list.add(new Test("/prefix:name/prefix:name", NOR|VAL));
        list.add(new Test("/name[2]/name[2]", NOR|VAL));
        list.add(new Test("/prefix:name[2]/prefix:name[2]", NOR|VAL));
        list.add(new Test("a/b/c/", "a/b/c", NOR|VAL));
        list.add(new Test("/a/b/c/", "/a/b/c", NOR|VAL));

        // relative paths
        list.add(new Test("a/b/c", NOR|VAL));
        list.add(new Test("prefix:name/prefix:name", NOR|VAL));
        list.add(new Test("name[2]/name[2]", NOR|VAL));
        list.add(new Test("prefix:name[2]/prefix:name[2]", NOR|VAL));

        // invalid paths
        list.add(new Test(""));
        list.add(new Test(" /a/b/c/"));
        list.add(new Test("/a/b/c/ "));
        list.add(new Test("/:name/prefix:name"));
        list.add(new Test("/prefix:name "));
        list.add(new Test("/prefix: name"));
        list.add(new Test("/ prefix:name"));
        list.add(new Test("/prefix : name"));
        list.add(new Test("/name[0]/name[2]"));
        list.add(new Test("/prefix:name[2]foo/prefix:name[2]"));
        list.add(new Test(":name/prefix:name"));
        list.add(new Test("name[0]/name[2]"));
        list.add(new Test("prefix:name[2]foo/prefix:name[2]"));

        // not normalized paths
        list.add(new Test("/a/../b", "/b", VAL));
        list.add(new Test("./../.", "..", VAL));
        list.add(new Test("/a/./b", "/a/b", VAL));
        list.add(new Test("/a/b/../..", "/", VAL));
        list.add(new Test("/a/b/c/../d/..././f", "/a/b/d/.../f", VAL));
        list.add(new Test("../a/b/../../../../f", "../../../f", VAL));
        list.add(new Test("a/../..", "..", VAL));
        list.add(new Test("../../a/.", "../../a", VAL));

        // invalid normalized paths
        list.add(new Test("/..", "/..", 0));
        list.add(new Test("/a/b/../../..", "/a/b/../../..", 0));

        tests = (Test[]) list.toArray(new Test[list.size()]);
    }

    public void testCreate() throws Exception {
        for (int i=0; i<tests.length; i++) {
            Test t = tests[i];
            long t1 = System.currentTimeMillis();
            for (int j=0; j<NUM_TESTS; j++) {
                try {
                    if (t.normalizedPath==null) {
                        // check just creation
                        Path p = PathFormat.parse(t.path, resolver);
                        if (!t.isValid()) {
                            fail("Should throw MalformedPathException: " + t.path);
                        }
                        assertEquals("\"" + t.path + "\".create(false)", t.path,  PathFormat.format(p, resolver));
                        assertEquals("\"" + t.path + "\".isNormalized()", t.isNormalized(), p.isNormalized());
                        assertEquals("\"" + t.path + "\".isAbsolute()", t.isAbsolute(), p.isAbsolute());
                    } else {
                        // check with normalization
                        Path p = PathFormat.parse(t.path, resolver).getNormalizedPath();
                        if (!t.isValid()) {
                            fail("Should throw MalformedPathException: " + t.path);
                        }
                        assertEquals("\"" + t.path + "\".create(true)", t.normalizedPath, PathFormat.format(p, resolver));
                        assertEquals("\"" + t.path + "\".isAbsolute()", t.isAbsolute(), p.isAbsolute());
                    }
                } catch (MalformedPathException e) {
                    if (t.isValid()) {
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
            Test t = tests[i];
            long t1 = System.currentTimeMillis();
            for (int j=0; j<NUM_TESTS; j++) {
                if (t.normalizedPath==null) {
                    // check just creation
                    boolean isValid = true;
                    try {
                        PathFormat.checkFormat(t.path);
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

    public void testBuilder() throws Exception {
        for (int i=0; i<tests.length; i++) {
            Test t = tests[i];
            if (t.isValid()) {
                if (t.normalizedPath==null) {
                    // check just creation
                    Path p = build(t.path, resolver, false);
                    assertEquals("\"" + t.path + "\".create(false)", t.path,  PathFormat.format(p, resolver));
                    assertEquals("\"" + t.path + "\".isNormalized()", t.isNormalized(), p.isNormalized());
                    assertEquals("\"" + t.path + "\".isAbsolute()", t.isAbsolute(), p.isAbsolute());
                } else {
                    // check with normalization
                    Path p = build(t.path, resolver, true);
                    assertEquals("\"" + t.path + "\".create(true)", t.normalizedPath, PathFormat.format(p, resolver));
                    assertEquals("\"" + t.path + "\".isAbsolute()", t.isAbsolute(), p.isAbsolute());
                }
            }
        }
    }

    public void testBuilderReverse() throws Exception {
        for (int i=0; i<tests.length; i++) {
            Test t = tests[i];
            if (t.isValid()) {
                if (t.normalizedPath==null) {
                    // check just creation
                    Path p = buildReverse(t.path, resolver, false);
                    assertEquals("\"" + t.path + "\".create(false)", t.path,  PathFormat.format(p, resolver));
                    assertEquals("\"" + t.path + "\".isNormalized()", t.isNormalized(), p.isNormalized());
                    assertEquals("\"" + t.path + "\".isAbsolute()", t.isAbsolute(), p.isAbsolute());
                } else {
                    // check with normalization
                    Path p = buildReverse(t.path, resolver, true);
                    assertEquals("\"" + t.path + "\".create(true)", t.normalizedPath, PathFormat.format(p, resolver));
                    assertEquals("\"" + t.path + "\".isAbsolute()", t.isAbsolute(), p.isAbsolute());
                }
            }
        }
    }

    private Path build(String path, NamespaceResolver resolver, boolean normalize)
            throws Exception {
        Path.PathBuilder builder = new Path.PathBuilder();
        String[] elems = Text.explode(path, '/', false);
        if (path.startsWith("/")) {
            builder.addRoot();
        }
        for (int i=0; i<elems.length; i++) {
            int pos = elems[i].indexOf('[');
            String elem;
            QName name;
            int index;
            if (pos<0) {
                elem = elems[i];
                index = -1;
            } else {
                index = Integer.parseInt(elems[i].substring(pos+1, elems[i].length()-1));
                elem = elems[i].substring(0, pos);
            }
            if (".".equals(elem)) {
                name = new QName("", ".");
            } else if ("..".equals(elems[i])) {
                name = new QName("", "..");
            } else {
                name = NameFormat.parse(elem, resolver);
            }
            if (index < 0) {
                builder.addLast(name);
            } else {
                builder.addLast(name, index);
            }
        }
        return normalize ? builder.getPath().getNormalizedPath() : builder.getPath();
    }

    private Path buildReverse(String path, NamespaceResolver resolver, boolean normalize)
            throws Exception {
        Path.PathBuilder builder = new Path.PathBuilder();
        String[] elems = Text.explode(path, '/', false);
        for (int i=elems.length-1; i>=0; i--) {
            int pos = elems[i].indexOf('[');
            String elem;
            QName name;
            int index;
            if (pos<0) {
                elem = elems[i];
                index = -1;
            } else {
                index = Integer.parseInt(elems[i].substring(pos+1, elems[i].length()-1));
                elem = elems[i].substring(0, pos);
            }
            if (".".equals(elem)) {
                name = new QName("", ".");
            } else if ("..".equals(elems[i])) {
                name = new QName("", "..");
            } else {
                name = NameFormat.parse(elem, resolver);
            }
            if (index < 0) {
                builder.addFirst(name);
            } else {
                builder.addFirst(name, index);
            }
        }
        if (path.startsWith("/")) {
            builder.addRoot();
        }
        return normalize ? builder.getPath().getNormalizedPath() : builder.getPath();
    }

   public void testNormalizedPaths() throws Exception {
        List paths = new ArrayList();

        // normalized paths
        paths.add(PathFormat.parse("/", resolver));
        paths.add(PathFormat.parse("/foo", resolver));
        paths.add(PathFormat.parse("/foo/bar", resolver));
        paths.add(PathFormat.parse("foo/bar", resolver));
        paths.add(PathFormat.parse("foo", resolver));
        paths.add(PathFormat.parse("../../foo/bar", resolver));
        paths.add(PathFormat.parse("..", resolver));

        for (Iterator it = paths.iterator(); it.hasNext(); ) {
            Path path = (Path) it.next();
            assertTrue("path is not normalized: " + PathFormat.format(path, resolver), path.isNormalized());
        }

        paths.clear();

        // not normalized paths
        paths.add(PathFormat.parse("/foo/..", resolver));
        paths.add(PathFormat.parse("/foo/.", resolver));
        paths.add(PathFormat.parse("/foo/../bar", resolver));
        paths.add(PathFormat.parse("/foo/./bar", resolver));
        paths.add(PathFormat.parse("./foo", resolver));
        paths.add(PathFormat.parse(".", resolver));
        paths.add(PathFormat.parse("foo/..", resolver));
        paths.add(PathFormat.parse("../foo/..", resolver));
        paths.add(PathFormat.parse("../foo/.", resolver));

        for (Iterator it = paths.iterator(); it.hasNext(); ) {
            Path path = (Path) it.next();
            assertFalse("path is normalized: " + PathFormat.format(path, resolver), path.isNormalized());
        }
    }

    public void testAbsolutePaths() throws Exception {
        List paths = new ArrayList();

        // absolute paths
        paths.add(PathFormat.parse("/", resolver));
        paths.add(PathFormat.parse("/foo", resolver));
        paths.add(PathFormat.parse("/foo/bar", resolver));
        paths.add(PathFormat.parse("/foo/../bar", resolver));
        paths.add(PathFormat.parse("/foo/..", resolver));
        paths.add(PathFormat.parse("/foo/./bar", resolver));
        paths.add(PathFormat.parse("/foo/.././bar/./foo", resolver));

        for (Iterator it = paths.iterator(); it.hasNext(); ) {
            Path path = (Path) it.next();
            assertTrue("path is not absolute: " + PathFormat.format(path, resolver), path.isAbsolute());
        }

        paths.clear();

        // not absoulute paths
        paths.add(PathFormat.parse("foo/..", resolver));
        paths.add(PathFormat.parse("foo/.", resolver));
        paths.add(PathFormat.parse("foo/../bar", resolver));
        paths.add(PathFormat.parse("foo/./bar", resolver));
        paths.add(PathFormat.parse("./foo", resolver));
        paths.add(PathFormat.parse(".", resolver));
        paths.add(PathFormat.parse("foo/..", resolver));
        paths.add(PathFormat.parse("../foo/..", resolver));
        paths.add(PathFormat.parse("../foo/.", resolver));

        for (Iterator it = paths.iterator(); it.hasNext(); ) {
            Path path = (Path) it.next();
            assertFalse("path is absolute: " + PathFormat.format(path, resolver), path.isAbsolute());
        }
    }

    public void testCanonicalPaths() throws Exception {
        List paths = new ArrayList();

        // canonical paths
        paths.add(PathFormat.parse("/", resolver));
        paths.add(PathFormat.parse("/foo", resolver));
        paths.add(PathFormat.parse("/foo/bar", resolver));

        for (Iterator it = paths.iterator(); it.hasNext(); ) {
            Path path = (Path) it.next();
            assertTrue("path is not canonical: " + PathFormat.format(path, resolver), path.isCanonical());
        }

        paths.clear();

        // not canonical paths
        paths.add(PathFormat.parse("/foo/..", resolver));
        paths.add(PathFormat.parse("/foo/.", resolver));
        paths.add(PathFormat.parse("/foo/../bar", resolver));
        paths.add(PathFormat.parse("/foo/./bar", resolver));
        paths.add(PathFormat.parse("./foo", resolver));
        paths.add(PathFormat.parse(".", resolver));
        paths.add(PathFormat.parse("/foo/..", resolver));
        paths.add(PathFormat.parse("/../foo/..", resolver));
        paths.add(PathFormat.parse("/../foo/.", resolver));

        for (Iterator it = paths.iterator(); it.hasNext(); ) {
            Path path = (Path) it.next();
            assertFalse("path is canonical: " + PathFormat.format(path, resolver), path.isCanonical());
        }
    }

    private static class Test {

        private final String path;

        private final String normalizedPath;

        private final int flags;

        /**
         * creates an invalid path test
         * @param path
         */
        public Test(String path) {
            this(path, null, 0);
        }

        /**
         * @param path
         * @param flags
         */
        public Test(String path, int flags) {
            this(path, null, flags);
        }

        public Test(String path, String normalizedPath, int flags) {
            this.path = path;
            this.normalizedPath = normalizedPath;
            this.flags = flags | ((path.length()>0 && path.charAt(0)=='/') ? ABS : 0);
        }

        public boolean isAbsolute() {
            return (flags&ABS) > 0;
        }

        public boolean isNormalized() {
            return (flags&NOR) > 0;
        }

        public boolean isValid() {
            return (flags&VAL) > 0;
        }

        public String toString() {
            StringBuffer b = new StringBuffer(path);
            if (normalizedPath!=null) {
                b.append(" -> ").append(normalizedPath);
            }
            if (isAbsolute()) {
                b.append(",ABS");
            }
            if (isNormalized()) {
                b.append(",NOR");
            }
            if (isValid()) {
                b.append(",VAL");
            }
            return b.toString();
        }
    }
}
