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
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid() && tests[i].isAbsolute()) {
                Path p = resolver.getQPath(tests[i].path);
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
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        int degree = 5;
        for (int i = 0; i < tests.length; i++) {
            JcrPath test = tests[i];
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
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (int i = 0; i < tests.length; i++) {
            JcrPath test = tests[i];
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
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid() && tests[i].isAbsolute()) {
                Path p = resolver.getQPath(tests[i].path);
                while (!p.getNormalizedPath().denotesRoot()) {
                    Path ancestor = p.getAncestor(1);
                    assertTrue(ancestor.isAncestorOf(p));
                    p = ancestor;
                }
            }
        }
    }

    public void testGetAncestorOfRelativePath2() throws RepositoryException {
        for (Iterator it = JcrPathAndAncestor.list.iterator(); it.hasNext();) {
            JcrPathAndAncestor tp = (JcrPathAndAncestor) it.next();

            Path ancestor = resolver.getQPath(tp.ancestor).getNormalizedPath();
            Path p = resolver.getQPath(tp.path);
            assertEquals("Expected ancestor " + tp.ancestor + " was " + tp.path + ".",
                    ancestor, p.getAncestor(tp.degree).getNormalizedPath());
        }
    }

    public void testGetAncestorReturnsNormalized() throws RepositoryException {
        List tests = JcrPathAndAncestor.list;
        for (Iterator it = tests.iterator(); it.hasNext();) {
            JcrPathAndAncestor test = (JcrPathAndAncestor) it.next();

            Path p = resolver.getQPath(test.path);
            assertTrue(p.getAncestor(test.degree).isNormalized());
        }
    }

    public void testIsAncestorOfRelativePath() throws RepositoryException {
        for (Iterator it = JcrPathAndAncestor.list.iterator(); it.hasNext();) {
            JcrPathAndAncestor tp = (JcrPathAndAncestor) it.next();

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
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid() && tests[i].isAbsolute()) {
                Path p = resolver.getQPath(tests[i].path).getNormalizedPath();
                if (!p.equals(root)) {
                    assertTrue(tests[i].path + " must be decendant of the root path.",p.isDescendantOf(root));
                }
            }
        }
    }

    public void testRootIsAncestorOfAbsolutePath() throws RepositoryException {
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid() && tests[i].isAbsolute()) {
                Path p = resolver.getQPath(tests[i].path).getNormalizedPath();
                if (!p.equals(root)) {
                    assertFalse(p.isAncestorOf(root));
                }
            }
        }
    }

    public void testIsEquivalentToSelf() throws RepositoryException {
        JcrPath[] tests = JcrPath.getTests();
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid()) {
                Path p = resolver.getQPath(tests[i].path);
                assertTrue(p.isEquivalentTo(p));
            }
        }
    }

    public void testIsEquivalentTo() throws IllegalArgumentException, RepositoryException {
        for (Iterator it = Equivalent.list.iterator(); it.hasNext();) {
            Equivalent tp = (Equivalent) it.next();

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
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid()) {
                Path p = resolver.getQPath(tests[i].path).getNormalizedPath();
                if (tests[i].isAbsolute()) {
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
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid() && tests[i].isAbsolute()) {
                Path p = resolver.getQPath(tests[i].path).getNormalizedPath();
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
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid() && tests[i].isAbsolute()) {
                Path p = resolver.getQPath(tests[i].path).getNormalizedPath();
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
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid() && tests[i].isAbsolute()) {
                Path p = resolver.getQPath(tests[i].path);
                assertTrue("Path must be absolute " + tests[i].path, p.isAbsolute());
            }
        }
    }

    public void testNotAbsolutePaths() throws RepositoryException {
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid() && !tests[i].isAbsolute()) {
                Path p = resolver.getQPath(tests[i].path);
                assertFalse("Path must not be absolute " + tests[i].path, p.isAbsolute());
            }
        }
    }

    public void testCanonicalPaths() throws Exception {
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid() && tests[i].isAbsolute()) {
                Path p = resolver.getQPath(tests[i].path);;
                if (!tests[i].isNormalized()) {
                    p = p.getNormalizedPath();
                }
                assertTrue("Path must be canonical " + tests[i].path, p.isCanonical());
            }
        }
    }

    public void testNotCanonicalPaths() throws Exception {
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid() && (!tests[i].isNormalized() || !tests[i].isAbsolute())) {
                Path p = resolver.getQPath(tests[i].path);
                assertFalse("Path must not be canonical " + tests[i].path, p.isCanonical());
            }
        }
    }

    public void testIsNotAncestor() throws RepositoryException {
        for (Iterator it = NotAncestor.list.iterator(); it.hasNext();) {
            NotAncestor test = (NotAncestor) it.next();
            Path p = resolver.getQPath(test.path);
            Path ancestor = resolver.getQPath(test.notAncestor);
            assertFalse(test.notAncestor + " isn't an ancestor of " + test.path,
                    ancestor.isAncestorOf(p));
        }
    }

    public void testDepth() throws RepositoryException {
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid() && tests[i].isAbsolute()) {
                Path p = resolver.getQPath(tests[i].path);
                String normJcrPath = (tests[i].normalizedPath == null) ? tests[i].path : tests[i].normalizedPath;
                int depth = Text.explode(normJcrPath, '/').length;
                assertTrue("Depth of " + tests[i].path + " must be " + depth, depth == p.getDepth());
            }
        }
    }

    public void testDepthOfRelativePath() throws RepositoryException {
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid() && !tests[i].isAbsolute()) {
                Path p = resolver.getQPath(tests[i].path);
                int depth = Path.ROOT_DEPTH;
                Path.Element[] elements = p.getNormalizedPath().getElements();
                for (int j = 0; j < elements.length; j++) {
                    if (elements[j].denotesParent()) {
                        depth--;
                    } else if (elements[j].denotesName()) {
                        depth++;
                    }
                }
                //System.out.println("Depth of " + tests[i].path + " = " + depth);
                assertTrue("Depth of " + tests[i].path + " must be " + depth, depth == p.getDepth());
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
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid() && tests[i].isAbsolute()) {
                Path p = resolver.getQPath(tests[i].path);
                assertTrue("Ancestor count must be same a depth", p.getDepth() == p.getAncestorCount());
            }
        }
    }

    public void testAncestorCountOfRelativePath() throws RepositoryException {
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid() && !tests[i].isAbsolute()) {
                Path p = resolver.getQPath(tests[i].path);
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
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid()) {
                int length = Text.explode(tests[i].path, '/').length;
                if (tests[i].isAbsolute()) {
                    length++;
                }
                Path p = resolver.getQPath(tests[i].path);
                //System.out.println("Length of " + tests[i].path + " = " + length);
                assertEquals("Length of " + tests[i].path + " must reflect " +
                        "number of elements.", new Integer(length), new Integer(p.getLength()));
            }
        }
    }

    public void testIsNormalized() throws RepositoryException {
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid()) {
                Path p = resolver.getQPath(tests[i].path);
                if (tests[i].isNormalized()) {
                    assertTrue("Path " + tests[i].path + " must be normalized.", p.isNormalized());
                } else {
                    assertFalse("Path " + tests[i].path + " must not be normalized.", p.isNormalized());
                }
            }
        }
    }

    public void testGetNameElement() throws RepositoryException {
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid()) {
                Path p = resolver.getQPath(tests[i].path);
                Path.Element nameEl = p.getNameElement();
                Path.Element[] all = p.getElements();
                assertEquals(all[all.length-1], nameEl);
            }
        }
    }

    public void testSubPath() throws RepositoryException {
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].isValid() && tests[i].isNormalized()) {
                Path p = resolver.getQPath(tests[i].path);

                // subpath between 0 and length -> equal path
                assertEquals(p, p.subPath(0, p.getLength()));

                // subpath a single element
                if (p.getLength() > 2) {
                    Path expected = factory.create(new Path.Element[] {p.getElements()[1]});
                    assertEquals(expected, p.subPath(1,2));
                }
                // subpath name element
                if (p.getLength() > 2) {
                    Path expected = p.getLastElement();
                    assertEquals(expected, p.subPath(p.getLength()-1, p.getLength()));
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

        private static List list = new ArrayList();
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

        private static List list = new ArrayList();
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

        private static List list = new ArrayList();
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
