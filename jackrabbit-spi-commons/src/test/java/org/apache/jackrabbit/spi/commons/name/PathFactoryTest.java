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
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

import javax.jcr.NamespaceException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <code>PathFactoryTest</code>...
 */
public class PathFactoryTest extends TestCase {

    private PathFactory factory;
    private PathResolver resolver;

    protected void setUp() throws Exception {
        super.setUp();
        factory = PathFactoryImpl.getInstance();

        NamespaceResolver nsresolver = new NamespaceResolver() {
            public String getURI(String prefix) throws NamespaceException {
                throw new UnsupportedOperationException();
            }
            public String getPrefix(String uri) throws NamespaceException {
                if (uri.equals(Name.NS_JCR_URI)) {
                    return Name.NS_JCR_PREFIX;
                } else {
                    return uri;
                }
            }
        };
        resolver = new DefaultNamePathResolver(nsresolver);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private String getString(Path p) throws NamespaceException {
        return resolver.getJCRPath(p);
    }

    public void testCreateNullName() {
        try {
            factory.create((Name) null);
            fail("Creating with null name is invalid");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testCreateNullNameIndex() {
        try {
            factory.create(null, 1);
            fail("Creating with null name is invalid");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testCreateElementNullName() {
        try {
            factory.createElement(null);
            fail("Creating element with null name is invalid");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testCreateElementNullNameIndex() {
        try {
            factory.createElement(null, 1);
            fail("Creating element with null name is invalid");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testCreateWithInvalidIndex() {
        try {
            factory.create(NameConstants.JCR_NAME, -1);
            fail("-1 is an invalid index");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testCreateElementWithInvalidIndex() {
        try {
            factory.createElement(NameConstants.JCR_NAME, -1);
            fail("-1 is an invalid index");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testRoot() {
        assertTrue(factory.getRootPath().isAbsolute());
        assertTrue(factory.getRootPath().isNormalized());
    }

    public void testCreateRoot() {
        Path root = factory.getRootPath();
        Path.Element rootElement = factory.getRootElement();
        Name rootName = rootElement.getName();

        assertEquals(root, factory.create(rootName));
        assertEquals(root, factory.create(new Path.Element[] {rootElement}));
        assertEquals(root, factory.create(root.toString()));
        assertEquals(root, factory.create(new Path.Element[] {factory.createElement(rootName)}));

        try {
            factory.create(rootName, 1);
            fail("Cannot create path from root name with a specific index.");
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            factory.createElement(rootName, 1);
            fail("Cannot create element from root name with a specific index.");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testCurrent() {
        Path.Element currElem = factory.getCurrentElement();
        Name currName = currElem.getName();

        assertEquals(currElem, factory.createElement(currName));

        Path current = factory.create(new Path.Element[] {currElem});
        assertEquals(current, factory.create(currName));
        assertFalse(current.isAbsolute());
        assertTrue(current.isNormalized());

        try {
            factory.createElement(currName, 1);
            fail("Cannot create current element with an index.");
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            factory.create(currName, 1);
            fail("Cannot create current path with an index.");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testParent() {
        Path.Element parentElem = factory.getParentElement();
        Name parentName = parentElem.getName();

        assertEquals(parentElem, factory.createElement(parentName));

        Path parent = factory.create(new Path.Element[] {parentElem});
        assertEquals(parent, factory.create(parentName));
        assertFalse(parent.isAbsolute());
        assertTrue(parent.isNormalized());

        try {
            factory.createElement(parentName, 1);
            fail("Cannot create parent element with an index.");
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            factory.create(parentName, 1);
            fail("Cannot create parent path with an index.");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testCreateInvalidPath() throws NamespaceException {

        Path.Element rootEl = factory.getRootElement();
        Path.Element pe = factory.getParentElement();
        Path.Element ce = factory.getCurrentElement();
        Path.Element element = factory.createElement(NameConstants.JCR_NAME, 3);
        Path.Element element2 = factory.createElement(NameConstants.JCR_DATA, 3);

        List elementArrays = new ArrayList();
        elementArrays.add(new Path.Element[]{rootEl, rootEl});
        elementArrays.add(new Path.Element[] {element, rootEl, pe});
        elementArrays.add(new Path.Element[] {pe, rootEl, element});
        elementArrays.add(new Path.Element[] {pe, rootEl, element});
        elementArrays.add(new Path.Element[] {rootEl, pe});
        elementArrays.add(new Path.Element[] {rootEl, element, element2, pe, pe, pe});

        for (Iterator it = elementArrays.iterator(); it.hasNext(); ) {
            try {
                Path p = factory.create((Path.Element[]) it.next());
                fail("Invalid path " + getString(p));
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
    }

    public void testCreateInvalidPath2() {
        Path root = factory.getRootPath();
        Name rootName = factory.getRootElement().getName();
        Name parentName = factory.getParentElement().getName();

        List list = new ArrayList();
        list.add(new ParentPathNameIndexDoNormalize(root, rootName, -1, true));
        list.add(new ParentPathNameIndexDoNormalize(root, rootName, -1, false));
        list.add(new ParentPathNameIndexDoNormalize(root, rootName, 3, false));
        list.add(new ParentPathNameIndexDoNormalize(factory.create(parentName), rootName, 3, true));

        for (Iterator it = list.iterator(); it.hasNext();) {
            ParentPathNameIndexDoNormalize test = (ParentPathNameIndexDoNormalize) it.next();
            try {
                if (test.index == -1) {
                    Path p = factory.create(test.parentPath, test.name, test.doNormalize);
                } else {
                    Path p = factory.create(test.parentPath, test.name, test.index, test.doNormalize);
                }
                fail("Invalid path " + test.parentPath + " + " + test.name);
            } catch (Exception e) {
                // ok
            }
        }
    }

    public void testCreateInvalidPath3() {
        Path root = factory.getRootPath();
        JcrPath[] tests = JcrPath.getTests();
        for (int i = 0; i < tests.length; i++) {
            if (!tests[i].isValid()) {
                try {
                    Path p = resolver.getQPath(tests[i].path);
                    fail("Invalid path " + getString(p));
                } catch (Exception e) {
                    // ok
                }
            }
        }
    }

    //--------------------------------------------------------------------------
    private static class ParentPathNameIndexDoNormalize {

        private final Path parentPath;
        private final Name name;
        private final int index;
        private final boolean doNormalize;

        private ParentPathNameIndexDoNormalize(Path parentPath, Name name,
                                               int index, boolean doNormalize) {
            this.parentPath = parentPath;
            this.name = name;
            this.index = index;
            this.doNormalize = doNormalize;
        }
    }
}

