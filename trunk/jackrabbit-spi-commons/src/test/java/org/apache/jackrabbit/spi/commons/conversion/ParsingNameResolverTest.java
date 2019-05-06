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

import javax.jcr.NamespaceException;

import junit.framework.TestCase;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

/**
 * Test cases for the {@link org.apache.jackrabbit.conversion.ParsingNameResolver} class.
 */
public class ParsingNameResolverTest extends TestCase {

    /**
     * Name resolver being tested.
     */
    private NameResolver resolver =
        new ParsingNameResolver(NameFactoryImpl.getInstance(), new DummyNamespaceResolver());

    /**
     * Checks that the given name resolves to the given namespace URI and
     * local part.
     *
     * @param name JCR name
     * @param uri namespace URI
     * @param local local part
     */
    private void assertValidName(String name, String uri, String local) {
        try {
            Name qname = resolver.getQName(name);
            assertEquals(name, uri, qname.getNamespaceURI());
            assertEquals(name, local, qname.getLocalName());
            assertEquals(name, name, resolver.getJCRName(qname));
        } catch (NameException e) {
            fail(name);
        } catch (NamespaceException e) {
            fail(name);
        }
    }

    /**
     * Tests that valid names are properly resolved.
     */
    public void testValidNames() {
        assertValidName("x", "", "x");
        assertValidName("name", "", "name");
        assertValidName("space name", "", "space name");
        assertValidName("x:y", "x", "y");
        assertValidName("prefix:name", "prefix", "name");
        assertValidName("prefix:space name", "prefix", "space name");
    }

    /**
     * Checks that the given name fails to resolve.
     *
     * @param name JCR name
     */
    private void assertInvalidName(String name) {
        try {
            resolver.getQName(name);
            fail(name);
        } catch (NameException e) {
        } catch (NamespaceException e) {
        }
    }

    /**
     * Tests that resolution of invalid names fails.
     */
    public void testInvalidNames() {
        assertInvalidName("");
        assertInvalidName(":name");
        assertInvalidName(".");
        assertInvalidName("..");
        assertInvalidName("pre:");
        assertInvalidName(" name");
        assertInvalidName(" prefix: name");
        assertInvalidName("prefix: name");
        assertInvalidName("prefix:name ");
        assertInvalidName("pre fix:name");
        assertInvalidName("prefix :name");
        assertInvalidName("name/name");
        assertInvalidName("name[]");
        assertInvalidName("name[1]");
        assertInvalidName("name[name");
        assertInvalidName("name]name");
        assertInvalidName("name*name");
        assertInvalidName("prefix:name:name");
    }

}
