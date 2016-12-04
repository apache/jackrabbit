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
package org.apache.jackrabbit.webdav.xml;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * <code>NamespaceTest</code>...
 */
public class NamespaceTest extends TestCase {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(NamespaceTest.class);

    public void testGetNamespace() {
        assertEquals(Namespace.EMPTY_NAMESPACE, Namespace.getNamespace("", ""));
        assertEquals(Namespace.EMPTY_NAMESPACE, Namespace.getNamespace(null, null));
        assertEquals(Namespace.XML_NAMESPACE, Namespace.getNamespace(Namespace.XML_NAMESPACE.getPrefix(), Namespace.XML_NAMESPACE.getURI()));
        assertEquals(Namespace.XMLNS_NAMESPACE, Namespace.getNamespace(Namespace.XMLNS_NAMESPACE.getPrefix(), Namespace.XMLNS_NAMESPACE.getURI()));

        Map<String, String> m = new HashMap<String, String>();
        m.put("foo", "http://foo.org/ns/foo");
        m.put("", "http://foo.org/ns/foo");
        m.put("off", "http://foo.org/ns/foo");
        m.put("off", "");

        for (String prefix: m.keySet()) {
            String uri = m.get(prefix);
            Namespace ns = Namespace.getNamespace(prefix, uri);
            assertEquals(prefix, ns.getPrefix());
            assertEquals(uri, ns.getURI());
            assertEquals(ns, Namespace.getNamespace(prefix, uri));
        }
    }

    public void testIsSame() {
        Map<String, Namespace> same = new HashMap<String, Namespace>();
        same.put("http://foo.org/ns/foo", Namespace.getNamespace("foo", "http://foo.org/ns/foo"));
        same.put("http://foo.org/ns/foo", Namespace.getNamespace("abc", "http://foo.org/ns/foo"));
        same.put("http://foo.org/ns/foo", Namespace.getNamespace("", "http://foo.org/ns/foo"));
        same.put("http://foo.org/ns/foo", Namespace.getNamespace(null, "http://foo.org/ns/foo"));
        same.put("", Namespace.EMPTY_NAMESPACE);
        same.put(null, Namespace.EMPTY_NAMESPACE);
        same.put(Namespace.XML_NAMESPACE.getURI(), Namespace.XML_NAMESPACE);
        same.put(Namespace.XMLNS_NAMESPACE.getURI(), Namespace.XMLNS_NAMESPACE);

        for (String nsURI : same.keySet()) {
            assertTrue(nsURI, same.get(nsURI).isSame(nsURI));
        }

        Map<String, Namespace> notSame = new HashMap<String, Namespace>();
        notSame.put("http://foo.org/ns/abc", Namespace.getNamespace("foo", "http://foo.org/ns/foo"));
        notSame.put("", Namespace.getNamespace("abc", "http://foo.org/ns/foo"));
        notSame.put(null, Namespace.getNamespace("", "http://foo.org/ns/foo"));
        notSame.put("http://foo.org/ns/abc", Namespace.EMPTY_NAMESPACE);
        notSame.put(Namespace.XML_NAMESPACE.getURI(), Namespace.EMPTY_NAMESPACE);
        notSame.put(Namespace.EMPTY_NAMESPACE.getURI(), Namespace.XML_NAMESPACE);
        notSame.put(Namespace.XMLNS_NAMESPACE.getURI(), Namespace.XML_NAMESPACE);
        notSame.put(Namespace.XML_NAMESPACE.getURI(), Namespace.XMLNS_NAMESPACE);

        for (String nsURI : notSame.keySet()) {
            assertFalse(notSame.get(nsURI).isSame(nsURI));
        }
    }

    public void testEquals() {
        assertEquals(Namespace.EMPTY_NAMESPACE, Namespace.getNamespace("prefix", ""));
        assertEquals(Namespace.EMPTY_NAMESPACE, Namespace.getNamespace("prefix", null));

        assertFalse(Namespace.EMPTY_NAMESPACE.equals(Namespace.getNamespace("", "something")));
        assertFalse(Namespace.EMPTY_NAMESPACE.equals(Namespace.getNamespace(null, "something")));
    }
}