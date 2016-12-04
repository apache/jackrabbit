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
import org.apache.jackrabbit.spi.Path.Element;

/**
 * Test cases for various kinds of path elements.
 */
public class ElementTest extends TestCase {

    private Name createName(String name) {
        return NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, name);
    }

    public void testCurrentElement() {
        Element element = CurrentPath.CURRENT_PATH;
        assertTrue(element.denotesCurrent());
        assertFalse(element.denotesIdentifier());
        assertFalse(element.denotesName());
        assertFalse(element.denotesParent());
        assertFalse(element.denotesRoot());
        assertEquals(createName("."), element.getName());
        assertEquals(Path.INDEX_UNDEFINED, element.getIndex());
        assertEquals(Path.INDEX_DEFAULT, element.getNormalizedIndex());
        assertEquals(".", element.getString());
    }

    public void testIdentifierElement() {
        Element element = new IdentifierPath("test");
        assertFalse(element.denotesCurrent());
        assertTrue(element.denotesIdentifier());
        assertFalse(element.denotesName());
        assertFalse(element.denotesParent());
        assertFalse(element.denotesRoot());
        assertNull(element.getName());
        assertEquals(Path.INDEX_UNDEFINED, element.getIndex());
        assertEquals(Path.INDEX_DEFAULT, element.getNormalizedIndex());
        assertEquals("[test]", element.getString());
    }

    public void testNameElement() {
        Element element =
            new NamePath(null, createName("test"), Path.INDEX_UNDEFINED);
        assertFalse(element.denotesCurrent());
        assertFalse(element.denotesIdentifier());
        assertTrue(element.denotesName());
        assertFalse(element.denotesParent());
        assertFalse(element.denotesRoot());
        assertEquals(createName("test"), element.getName());
        assertEquals(Path.INDEX_UNDEFINED, element.getIndex());
        assertEquals(Path.INDEX_DEFAULT, element.getNormalizedIndex());
        assertEquals("{}test", element.getString());
    }

    public void testIndexedNameElement() {
        Element element = new NamePath(null, createName("test"), 123);
        assertFalse(element.denotesCurrent());
        assertFalse(element.denotesIdentifier());
        assertTrue(element.denotesName());
        assertFalse(element.denotesParent());
        assertFalse(element.denotesRoot());
        assertEquals(createName("test"), element.getName());
        assertEquals(123, element.getIndex());
        assertEquals(123, element.getNormalizedIndex());
        assertEquals("{}test[123]", element.getString());
    }

    public void testParentElement() {
        Element element = ParentPath.PARENT_PATH;
        assertFalse(element.denotesCurrent());
        assertFalse(element.denotesIdentifier());
        assertFalse(element.denotesName());
        assertTrue(element.denotesParent());
        assertFalse(element.denotesRoot());
        assertEquals(createName(".."), element.getName());
        assertEquals(Path.INDEX_UNDEFINED, element.getIndex());
        assertEquals(Path.INDEX_DEFAULT, element.getNormalizedIndex());
        assertEquals("..", element.getString());
    }

    public void testRootElement() {
        Element element = RootPath.ROOT_PATH;
        assertFalse(element.denotesCurrent());
        assertFalse(element.denotesIdentifier());
        assertFalse(element.denotesName());
        assertFalse(element.denotesParent());
        assertTrue(element.denotesRoot());
        assertEquals(createName(""), element.getName());
        assertEquals(Path.INDEX_UNDEFINED, element.getIndex());
        assertEquals(Path.INDEX_DEFAULT, element.getNormalizedIndex());
        assertEquals("{}", element.getString());
    }

}
