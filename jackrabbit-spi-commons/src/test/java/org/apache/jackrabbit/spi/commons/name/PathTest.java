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

import junit.framework.TestCase;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.conversion.ParsingNameResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingPathResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

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
    private static final PathResolver resolver = new ParsingPathResolver(factory, new ParsingNameResolver(NameFactoryImpl.getInstance(), nsResolver));

    public void testRootIsDescendantOfRoot() throws RepositoryException {
        Path root = factory.getRootPath();
        assertFalse(root.isDescendantOf(root));
    }
    public void testRootIsAncestorOfRoot() throws RepositoryException {
        Path root = factory.getRootPath();
        assertFalse(root.isAncestorOf(root));
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
     * Test if IllegalArgumentException is thrown as expected.
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
     * Test if RepositoryException is thrown as expected.
     */
    public void testIsDescendantOfThrowsRepositoryException() throws RepositoryException {
        Path abs = factory.create(NameConstants.JCR_DATA);
        Path rel = factory.create(new Path.Element[] {factory.getCurrentElement()});
        try {
            abs.isDescendantOf(rel);
            fail("Path.isDescendantOf(Path) must throw RepositoryException if either path cannot be normalized.");
        } catch (RepositoryException e) {
            // ok.
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

    /**
     * Test if RepositoryException is thrown as expected.
     */
    public void testIsAncestorOfThrowsRepositoryException() throws RepositoryException {
        Path abs = factory.create(NameConstants.JCR_DATA);
        Path rel = factory.create(new Path.Element[] {factory.getCurrentElement()});
        try {
            abs.isAncestorOf(rel);
            fail("Path.isAncestorOf(Path) must throw RepositoryException if either path cannot be normalized.");
        } catch (RepositoryException e) {
            // ok.
        }
    }
}