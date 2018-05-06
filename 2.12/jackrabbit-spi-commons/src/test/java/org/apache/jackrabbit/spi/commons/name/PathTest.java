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
package org.apache.jackrabbit.spi.commons.name;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

import junit.framework.TestCase;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingNameResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingPathResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.util.Text;

/**
 * <code>PathTest</code>...
 */
public class PathTest extends TestCase {

    private static final PathFactory factory = PathFactoryImpl.getInstance();
    private static final NamespaceResolver nsResolver = new NamespaceResolver() {
        public String getURI(String prefix) throws NamespaceException {
            return prefix;
        }
        public String getPrefix(String uri) throws NamespaceException {
            return uri;
        }
    };
    private static final NameResolver nameResolver = new ParsingNameResolver(NameFactoryImpl.getInstance(), nsResolver);
    private static final PathResolver resolver = new ParsingPathResolver(factory, nameResolver);

    public void testRootIsDescendantOfRoot() throws RepositoryException {
        Path root = factory.getRootPath();
        assertFalse(root.isDescendantOf(root));
    }
    public void testRootIsAncestorOfRoot() throws RepositoryException {
        Path root = factory.getRootPath();
        assertFalse(root.isAncestorOf(root));
    }

    public void testGetAncestor() throws RepositoryException {
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid() && test.isAbsolute()) {
                Path p = resolver.getQPath(test.path);
                if (p.getNormalizedPath().denotesRoot()) {
                    continue;
                }

                String jcrAncestor = Text.getRelativeParent(resolver.getJCRPath(p.getNormalizedPath()), 1);
                Path ancestor = resolver.getQPath(jcrAncestor);
                assertEquals(ancestor, p.getAncestor(1));
            }
        }
    }

    public void testGetAncestorOfRelativePath() throws RepositoryException {
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid() && !test.isAbsolute()) {
                Path p = resolver.getQPath(test.path);

                StringBuffer expJcrAnc = new StringBuffer(test.path);
                expJcrAnc.append((test.path.endsWith("/") ? "" : "/"));
                expJcrAnc.append("../../../../..");

                Path ancestor = resolver.getQPath(expJcrAnc.toString()).getNormalizedPath();
                assertEquals(ancestor, p.getAncestor(5).getNormalizedPath());
            }
        }
    }

    public void testGetAncestorAtDegreeDepth() throws RepositoryException {
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid() && test.isAbsolute()) {
                Path p = resolver.getQPath(test.path);

                int degree = p.getDepth();
                if (degree > 0) {
                    assertTrue(p.getAncestor(degree).denotesRoot());
                }
            }
        }
    }

    public void testGetAncestorIsAncestor() throws RepositoryException {
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid() && test.isAbsolute()) {
                Path p = resolver.getQPath(test.path);
                while (!p.getNormalizedPath().denotesRoot()) {
                    Path ancestor = p.getAncestor(1);
                    assertTrue(ancestor.isAncestorOf(p));
                    p = ancestor;
                }
            }
        }
    }

    public void testGetAncestorOfRelativePath2() throws RepositoryException {
        for (Object aList : JcrPathAndAncestor.list) {
            JcrPathAndAncestor tp = (JcrPathAndAncestor) aList;

            Path ancestor = resolver.getQPath(tp.ancestor).getNormalizedPath();
            Path p = resolver.getQPath(tp.path);
            assertEquals("Expected ancestor " + tp.ancestor + " was " + tp.path + ".",
                    ancestor, p.getAncestor(tp.degree).getNormalizedPath());
        }
    }

    public void testGetAncestorReturnsNormalized() throws RepositoryException {
        List<JcrPathAndAncestor> tests = JcrPathAndAncestor.list;
        for (JcrPathAndAncestor test : tests) {
            Path p = resolver.getQPath(test.path);
            assertTrue(p.getAncestor(test.degree).isNormalized());
        }
    }

    public void testIsAncestorOfRelativePath() throws RepositoryException {
        for (JcrPathAndAncestor tp : JcrPathAndAncestor.list) {
            Path ancestor = resolver.getQPath(tp.ancestor);
            Path p = resolver.getQPath(tp.path);

            if (tp.degree == 0) {
                assertFalse(tp.ancestor + " should not be ancestor of " + tp.path,
                    ancestor.isAncestorOf(p));
            } else {
                assertTrue(tp.ancestor + " should be ancestor of " + tp.path,
                    ancestor.isAncestorOf(p));
            }
        }
    }

    public void testAbsolutePathIsDescendantOfRoot() throws RepositoryException {
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid() && test.isAbsolute()) {
                Path p = resolver.getQPath(test.path).getNormalizedPath();
                if (!p.equals(root)) {
                    assertTrue(test.path + " must be decendant of the root path.", p.isDescendantOf(root));
                }
            }
        }
    }

    public void testRootIsAncestorOfAbsolutePath() throws RepositoryException {
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid() && test.isAbsolute()) {
                Path p = resolver.getQPath(test.path).getNormalizedPath();
                if (!p.equals(root)) {
                    assertFalse(p.isAncestorOf(root));
                }
            }
        }
    }

    public void testIsEquivalentToSelf() throws RepositoryException {
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid()) {
                Path p = resolver.getQPath(test.path);
                assertTrue(p.isEquivalentTo(p));
            }
        }
    }

    public void testIsEquivalentTo() throws IllegalArgumentException, RepositoryException {
        for (Equivalent tp : Equivalent.list) {

            Path path = resolver.getQPath(tp.path);
            Path other = resolver.getQPath(tp.other);

            if (tp.isEquivalent) {
                assertTrue(tp.path + " should be equivalent to " + tp.other,
                    path.isEquivalentTo(other));
            } else {
                assertFalse(tp.path + " should not be equivalent to " + tp.other,
                        path.isEquivalentTo(other));
            }
        }
    }

    public void testIsAncestorIsDescendant() throws RepositoryException {
        Path absPath = factory.getRootPath();
        Path relPath = factory.create(NameConstants.JCR_DATA);
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid()) {
                Path p = resolver.getQPath(test.path).getNormalizedPath();
                if (test.isAbsolute()) {
                    if (absPath.isAncestorOf(p)) {
                        assertTrue(p.isDescendantOf(absPath));
                    } else {
                        assertFalse(p.isDescendantOf(absPath));
                    }
                    absPath = p;
                } else {
                    if (relPath.isAncestorOf(p)) {
                        assertTrue(p.isDescendantOf(relPath));
                    } else {
                        assertFalse(p.isDescendantOf(relPath));
                    }
                    relPath = p;
                }
            }
        }
    }

    /**
     * Test if IllegalArgumentException is thrown as expected.
     */
    public void testIsDescendantOfNull() throws RepositoryException {
        try {
            Path p = factory.getRootPath();
            p.isDescendantOf(null);
            fail("Path.isDescendantOf(null) must throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            // ok.
        }
    }

    /**
     * Testing Path.isDescendantOf with rel/abs path where the path is abs/rel.
     */
    public void testIsDescendantOfThrowsIllegalArgumentException() throws RepositoryException {
        Path abs = factory.create(factory.getRootPath(), NameConstants.JCR_DATA, true);
        Path rel = factory.create(NameConstants.JCR_DATA);

        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid() && test.isAbsolute()) {
                Path p = resolver.getQPath(test.path).getNormalizedPath();
                try {
                    if (p.isAbsolute()) {
                        p.isDescendantOf(rel);
                    } else {
                        p.isDescendantOf(abs);
                    }
                    fail("Path.isDescendantOf(Path) must throw IllegalArgumentException if Path.isAbsolute is not the same for both.");
                } catch (IllegalArgumentException e) {
                    // ok.
                }
            }
        }
    }

    /**
     * Test if IllegalArgumentException is thrown as expected.
     */
    public void testIsAncestorOfNull() throws RepositoryException {
        try {
            Path p = factory.getRootPath();
            p.isAncestorOf(null);
            fail("Path.isAncestorOf(null) must throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            // ok.
        }
    }

    /**
     * Test if IllegalArgumentException is thrown as expected.
     */
    public void testIsAncestorOfThrowsIllegalArgumentException() throws RepositoryException {
        Path abs = factory.create(factory.getRootPath(), NameConstants.JCR_DATA, true);
        Path rel = factory.create(NameConstants.JCR_DATA);

        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid() && test.isAbsolute()) {
                Path p = resolver.getQPath(test.path).getNormalizedPath();
                try {
                    if (p.isAbsolute()) {
                        p.isAncestorOf(rel);
                    } else {
                        p.isAncestorOf(abs);
                    }
                    fail("Path.isAncestorOf(Path) must throw IllegalArgumentException if Path.isAbsolute is not the same for both.");
                } catch (IllegalArgumentException e) {
                    // ok.
                }
            }
        }
    }

    public void testAbsolutePaths() throws RepositoryException {
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid() && test.isAbsolute()) {
                Path p = resolver.getQPath(test.path);
                assertTrue("Path must be absolute " + test.path, p.isAbsolute());
            }
        }
    }

    public void testNotAbsolutePaths() throws RepositoryException {
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid() && !test.isAbsolute()) {
                Path p = resolver.getQPath(test.path);
                assertFalse("Path must not be absolute " + test.path, p.isAbsolute());
            }
        }
    }

    public void testCanonicalPaths() throws Exception {
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid() && test.isAbsolute()) {
                Path p = resolver.getQPath(test.path);
                if (!test.isNormalized()) {
                    p = p.getNormalizedPath();
                }
                assertTrue("Path must be canonical " + test.path, p.isCanonical());
            }
        }
    }

    public void testNotCanonicalPaths() throws Exception {
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid() && (!test.isNormalized() || !test.isAbsolute())) {
                Path p = resolver.getQPath(test.path);
                assertFalse("Path must not be canonical " + test.path, p.isCanonical());
            }
        }
    }

    public void testIsNotAncestor() throws RepositoryException {
        for (NotAncestor test : NotAncestor.list) {
            Path p = resolver.getQPath(test.path);
            Path ancestor = resolver.getQPath(test.notAncestor);
            assertFalse(test.notAncestor + " isn't an ancestor of " + test.path,
                    ancestor.isAncestorOf(p));
        }
    }

    public void testDepth() throws RepositoryException {
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid() && test.isAbsolute()) {
                Path p = resolver.getQPath(test.path);
                String normJcrPath = (test.normalizedPath == null) ? test.path : test.normalizedPath;
                int depth = Text.explode(normJcrPath, '/').length;
                assertTrue("Depth of " + test.path + " must be " + depth, depth == p.getDepth());
            }
        }
    }

    public void testDepthOfRelativePath() throws RepositoryException {
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid() && !test.isAbsolute()) {
                Path p = resolver.getQPath(test.path);
                int depth = Path.ROOT_DEPTH;
                Path.Element[] elements = p.getNormalizedPath().getElements();
                for (Path.Element element : elements) {
                    if (element.denotesParent()) {
                        depth--;
                    } else if (element.denotesName()) {
                        depth++;
                    }
                }
                //System.out.println("Depth of " + tests[i].path + " = " + depth);
                assertTrue("Depth of " + test.path + " must be " + depth, depth == p.getDepth());
            }
        }
    }

    public void testDepthOfRoot() throws RepositoryException {
        assertTrue("Depth of root must be " + Path.ROOT_DEPTH,
                factory.getRootPath().getDepth() == Path.ROOT_DEPTH);
    }

    public void testDepthOfCurrent() throws RepositoryException {
        Path current = factory.create(factory.getCurrentElement().getName());
        assertTrue("Depth of current must be same as for root (" + Path.ROOT_DEPTH + ")",
                current.getDepth() == Path.ROOT_DEPTH);
    }

    public void testDepthOfParent() throws RepositoryException {
        Path parent = factory.create(factory.getParentElement().getName());
        int depth = Path.ROOT_DEPTH - 1;
        assertTrue("Depth of parent must be same as for root -1 (" + depth + ")",
                parent.getDepth() == depth);
    }

    public void testAncestorCount() throws RepositoryException {
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid() && test.isAbsolute()) {
                Path p = resolver.getQPath(test.path);
                assertTrue("Ancestor count must be same a depth", p.getDepth() == p.getAncestorCount());
            }
        }
    }

    public void testAncestorCountOfRelativePath() throws RepositoryException {
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid() && !test.isAbsolute()) {
                Path p = resolver.getQPath(test.path);
                assertTrue("Ancestor count or a relative path must be -1", -1 == p.getAncestorCount());
            }
        }
    }

    public void testAncestorCountOfRoot() throws RepositoryException {
        assertTrue("AncestorCount of root must be " + 0,
                factory.getRootPath().getAncestorCount() == 0);
    }

    public void testAncestorCountOfCurrent() throws RepositoryException {
        Path current = factory.create(factory.getCurrentElement().getName());
        assertTrue("AncestorCount of current must be -1",
                current.getAncestorCount() == -1);
    }

    public void testAncestorCountOfParent() throws RepositoryException {
        Path parent = factory.create(factory.getParentElement().getName());
        assertTrue("AncestorCount of parent must be same as for -1",
                parent.getAncestorCount() == - 1);
    }

    public void testLength() throws RepositoryException {
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid()) {
                int length = Text.explode(test.path, '/').length;
                if (test.isAbsolute()) {
                    length++;
                }
                Path p = resolver.getQPath(test.path);
                //System.out.println("Length of " + tests[i].path + " = " + length);
                assertEquals("Length of " + test.path + " must reflect " +
                        "number of elements.", new Integer(length), new Integer(p.getLength()));
            }
        }
    }

    public void testIsNormalized() throws RepositoryException {
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid()) {
                Path p = resolver.getQPath(test.path);
                if (test.isNormalized()) {
                    assertTrue("Path " + test.path + " must be normalized.", p.isNormalized());
                } else {
                    assertFalse("Path " + test.path + " must not be normalized.", p.isNormalized());
                }
            }
        }
    }

    public void testGetNameElement() throws RepositoryException {
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid()) {
                Path p = resolver.getQPath(test.path);
                Path.Element nameEl = p.getNameElement();
                Path.Element[] all = p.getElements();
                assertEquals(all[all.length - 1], nameEl);
            }
        }
    }

    public void testSubPath() throws RepositoryException {
        JcrPath[] tests = JcrPath.getTests();
        for (JcrPath test : tests) {
            if (test.isValid() && test.isNormalized()) {
                Path p = resolver.getQPath(test.path);

                // subpath between 0 and length -> equal path
                assertEquals(p, p.subPath(0, p.getLength()));

                // subpath a single element
                if (p.getLength() > 2) {
                    Path expected = factory.create(new Path.Element[]{p.getElements()[1]});
                    assertEquals(expected, p.subPath(1, 2));
                }
                // subpath name element
                if (p.getLength() > 2) {
                    Path expected = p.getLastElement();
                    assertEquals(expected, p.subPath(p.getLength() - 1, p.getLength()));
                }
            }
        }
    }

    public void testSubPathInvalid() throws RepositoryException {
        Path p = resolver.getQPath("/a/b/c/d/e");

        try {
            p.subPath(2,2);
            fail("Path.subPath with identical from/to must throw IllegalArumentException");
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            p.subPath(3,2);
            fail("Path.subPath with from > to must throw IllegalArumentException");
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            p.subPath(-1, 2);
            fail("Path.subPath with from == -1 to must throw IllegalArumentException");
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            p.subPath(1, p.getLength()+1);
            fail("Path.subPath with to > length to must throw IllegalArumentException");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    //--------------------------------------------------------------------------
    private static class JcrPathAndAncestor {

        private final String path;
        private final String ancestor;
        private final int degree;

        private JcrPathAndAncestor(String path, String ancestor, int degree) {
            this.path = path;
            this.ancestor = ancestor;
            this.degree = degree;
        }

        private static List<JcrPathAndAncestor> list = new ArrayList<JcrPathAndAncestor>();
        static {
            // normalized
            list.add(new JcrPathAndAncestor("abc/def", "abc", 1));
            list.add(new JcrPathAndAncestor("a/b/c/", "a", 2));
            list.add(new JcrPathAndAncestor("prefix:name[2]/prefix:name[2]", "prefix:name[2]/prefix:name[2]", 0));
            list.add(new JcrPathAndAncestor("../../a/b/c/d", "../../a/b/c", 1));
            list.add(new JcrPathAndAncestor("..", "../..", 1));
            list.add(new JcrPathAndAncestor("a/b", ".", 2));
            list.add(new JcrPathAndAncestor("a/b", "..", 3));
            list.add(new JcrPathAndAncestor("a/b", ".", 2));
            list.add(new JcrPathAndAncestor("..", "../..", 1));
            list.add(new JcrPathAndAncestor("../a", "../..", 2));
            list.add(new JcrPathAndAncestor(".", "..", 1));
            list.add(new JcrPathAndAncestor(".", "../..", 2));
            list.add(new JcrPathAndAncestor("../a/b", "../a", 1));
            list.add(new JcrPathAndAncestor("../a/b", "../a", 1));
            list.add(new JcrPathAndAncestor("a", "..", 2));
            list.add(new JcrPathAndAncestor("a", ".", 1));
            list.add(new JcrPathAndAncestor("a", ".", 1));
            list.add(new JcrPathAndAncestor("../a", "..", 1));

            // not normalized paths
            list.add(new JcrPathAndAncestor("a/./b", "a", 1));
            list.add(new JcrPathAndAncestor(".a./.b.", ".a.", 1));
            list.add(new JcrPathAndAncestor("./../.", "./../.", 0));
            list.add(new JcrPathAndAncestor("./../.", "../../..", 2));
            list.add(new JcrPathAndAncestor("a/b/c/../d/..././f", "a", 4));
            list.add(new JcrPathAndAncestor("../a/b/../../../../f", "../a/b/../../../../f", 0));
            list.add(new JcrPathAndAncestor("../a/b/../../../../f", "../../..", 1));

            list.add(new JcrPathAndAncestor("a/b/c/", "a/b/c/../../..", 3));
            list.add(new JcrPathAndAncestor("a/b/c/", "a/b/c/../../../..", 4));
            list.add(new JcrPathAndAncestor("a/../b", ".", 1));
            list.add(new JcrPathAndAncestor(".", "..", 1));
            list.add(new JcrPathAndAncestor("a/b/../..", "a/b/../..", 0));
            list.add(new JcrPathAndAncestor("a/b/../..", "..", 1));
            list.add(new JcrPathAndAncestor("a", "a", 0));
            list.add(new JcrPathAndAncestor(".../...", "..", 3));
            list.add(new JcrPathAndAncestor("../a/b/../../../../f", "../a/b/../../../../f/../..", 2));
        }
    }

    private static class NotAncestor {

        private final String path;
        private final String notAncestor;

        private NotAncestor(String path, String notAncestor) {
            this.path = path;
            this.notAncestor = notAncestor;
        }

        private static List<NotAncestor> list = new ArrayList<NotAncestor>();
        static {
            // false if same path
            list.add(new NotAncestor("/", "/"));
            list.add(new NotAncestor("/a/.", "/a"));

            // false if siblings or in sibling tree
            list.add(new NotAncestor("a", "b"));
            list.add(new NotAncestor("a/b", "b"));
            list.add(new NotAncestor("../../a/b/c", "../../d/a/b"));
            list.add(new NotAncestor("../../a/b/c", "../../d/e/f"));

            // false if path to test is ancestor
            list.add(new NotAncestor("/", "/a"));
            list.add(new NotAncestor("/", "/a/."));
            list.add(new NotAncestor("/", "/a/b/c"));
            list.add(new NotAncestor("a/b", "a/b/c"));
            list.add(new NotAncestor("../..", ".."));

            // undefined if ancestor -> false
            list.add(new NotAncestor("a", "../a"));
            list.add(new NotAncestor("b", "../a"));
            list.add(new NotAncestor("../../b", "../../../a"));
            list.add(new NotAncestor("../../a", "../../../a"));
            list.add(new NotAncestor(".", "../../a"));
            list.add(new NotAncestor(".", "../a"));
            list.add(new NotAncestor("../a", "../../../a/a"));
            list.add(new NotAncestor("../../a/b/c", "../../../a/b"));
            list.add(new NotAncestor("../../a/b/c", "../../../a"));
            list.add(new NotAncestor("../../d/b/c", "../../../a"));

            // misc relative paths
            list.add(new NotAncestor(".", "a/b"));
            list.add(new NotAncestor("../..", ".."));
            list.add(new NotAncestor("../../a", ".."));
            list.add(new NotAncestor("../..", "../a"));
            list.add(new NotAncestor(".", "."));
            list.add(new NotAncestor("..", "."));
            list.add(new NotAncestor("../..", "."));
            list.add(new NotAncestor("../../a", "b"));
            list.add(new NotAncestor("b", "../../a"));
            list.add(new NotAncestor("../../a", "."));
            list.add(new NotAncestor(".", "../../a"));
            list.add(new NotAncestor("../../a", "a/.."));
            list.add(new NotAncestor("a/..", "../../a"));
            list.add(new NotAncestor("../a", "."));
            list.add(new NotAncestor(".", "../a"));
            list.add(new NotAncestor("../a", "a/.."));
            list.add(new NotAncestor("a/..", "../a"));
            list.add(new NotAncestor("../a", "../a/b"));
            list.add(new NotAncestor("..", "a"));
            list.add(new NotAncestor(".", "a"));
            list.add(new NotAncestor("..", ".."));
            list.add(new NotAncestor(".", "."));
            list.add(new NotAncestor("..", "../a"));
            list.add(new NotAncestor("..", "../../a"));
            list.add(new NotAncestor("../../a", ".."));
        }
    }

    private static class Equivalent {

        private final String path;
        private final String other;
        private final boolean isEquivalent;

        private Equivalent(String path, String other, boolean isEquivalent) {
            this.path = path;
            this.other = other;
            this.isEquivalent = isEquivalent;
        }

        private static List<Equivalent> list = new ArrayList<Equivalent>();
        static {
            list.add(new Equivalent(".", "a/b", false));
            list.add(new Equivalent(".", "a/..", true));
            list.add(new Equivalent(".", ".", true));
            list.add(new Equivalent("..", "..", true));
            list.add(new Equivalent("../..", "../..", true));
            list.add(new Equivalent("../a", "../a/b/..", true));
            list.add(new Equivalent("../a/b/..", "..", false));
        }
    }

}
