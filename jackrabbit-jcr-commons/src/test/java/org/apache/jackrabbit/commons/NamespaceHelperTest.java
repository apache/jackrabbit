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
package org.apache.jackrabbit.commons;

import junit.framework.TestCase;
import org.mockito.Mockito;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class NamespaceHelperTest extends TestCase {

    private Session session;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        session = Mockito.mock(Session.class);
    }

    public void testGetNamespaces() throws RepositoryException {
        NamespaceHelper nsHelper = new NamespaceHelper(session);

        when(session.getNamespacePrefixes()).thenReturn(new String[0]);
        Map<String, String> map = nsHelper.getNamespaces();
        assertEquals(0, map.size());

        when(session.getNamespacePrefixes()).thenReturn(new String[]{"foo"});
        map = nsHelper.getNamespaces();
        assertEquals(1, map.size());
        assertNull(map.get("foo"));

        when(session.getNamespacePrefixes()).thenReturn(new String[]{"foo", "bar"});
        when(session.getNamespaceURI("bar")).thenReturn("urn:test");
        map = nsHelper.getNamespaces();
        assertEquals(2, map.size());
        assertNull(map.get("foo"));
        assertEquals("urn:test", map.get("bar"));
    }

    public void testGetPrefix() throws RepositoryException {
        NamespaceHelper nsHelper = new NamespaceHelper(session);

        String uri1 = "urn:test1";

        when(session.getNamespacePrefix(uri1)).thenThrow(NamespaceException.class);
        String prefix = nsHelper.getPrefix(uri1);
        assertNull(prefix);

        String uri2 = "urn:test2";

        when(session.getNamespacePrefix(uri2)).thenReturn("foo");
        prefix = nsHelper.getPrefix(uri2);
        assertEquals("foo", prefix);
    }

    public void testGetURI() throws RepositoryException {
        NamespaceHelper nsHelper = new NamespaceHelper(session);

        String prefix1 = "prefix1";

        when(session.getNamespaceURI(prefix1)).thenThrow(NamespaceException.class);
        String uri = nsHelper.getURI(prefix1);
        assertNull(uri);

        String prefix2 = "prefix2";

        when(session.getNamespaceURI(prefix2)).thenReturn("foo:bar");
        uri = nsHelper.getURI(prefix2);
        assertEquals("foo:bar", uri);
    }

    public void testGetJcrName() throws RepositoryException {
        NamespaceHelper nsHelper = new NamespaceHelper(session);

        when(session.getNamespacePrefix(NamespaceHelper.JCR)).thenReturn("jcr");
        assertEquals("jcr:xyz", nsHelper.getJcrName("jcr:xyz"));

        when(session.getNamespacePrefix(NamespaceHelper.MIX)).thenReturn("foo");
        assertEquals("foo:xyz", nsHelper.getJcrName("mix:xyz"));

        try {
            when(session.getNamespacePrefix("urn:foo")).thenReturn("unknown");
            String shouldFail = nsHelper.getJcrName("foo:xyz");
            fail("getJcrName should fail for unknown prefix foo, but got: " + shouldFail);
        } catch (IllegalArgumentException expected) {
            // all good
        }

        try {
            when(session.getNamespacePrefix(NamespaceHelper.NT)).thenThrow(NamespaceException.class);
            String shouldFail = nsHelper.getJcrName(NamespaceHelper.NT);
            fail("getJcrName should fail for unknown prefix nt, but got: " + shouldFail);
        } catch (IllegalArgumentException expected) {
            // all good
        }
    }

    public void testGetJcrName2() throws RepositoryException {
        NamespaceHelper nsHelper = new NamespaceHelper(session);

        assertEquals("foo", nsHelper.getJcrName(null, "foo"));
        assertEquals("foo", nsHelper.getJcrName("", "foo"));

        when(session.getNamespacePrefix("urn:bar")).thenReturn("bar");
        assertEquals("bar:foo", nsHelper.getJcrName("urn:bar", "foo"));
    }

    public void testRegisterNamespaceAlreadyRegistered() throws RepositoryException {
        NamespaceHelper nsHelper = new NamespaceHelper(session);

        Workspace workspace = Mockito.mock(Workspace.class);
        NamespaceRegistry nsReg = Mockito.mock(NamespaceRegistry.class);

        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getNamespaceRegistry()).thenReturn(nsReg);

        when(nsReg.getPrefix("foo:")).thenReturn("xyz");
        when(session.getNamespacePrefix("foo:")).thenReturn("xyz");
        String prefix = nsHelper.registerNamespace("bar", "foo:");
        assertEquals("xyz", prefix);
    }

    public void testRegisterNamespace() throws RepositoryException {
        Workspace workspace = Mockito.mock(Workspace.class);
        NamespaceRegistry nsReg = Mockito.mock(NamespaceRegistry.class);

        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getNamespaceRegistry()).thenReturn(nsReg);

        // poor man's namespace registry

        final HashMap<String, String> pref2uri = new HashMap<>();
        final HashMap<String, String> uri2pref = new HashMap<>();

        // defaults (incomplete)
        pref2uri.put("xml", NamespaceRegistry.NAMESPACE_XML);
        uri2pref.put(NamespaceRegistry.NAMESPACE_XML, "xml");
        pref2uri.put("jcr", NamespaceRegistry.NAMESPACE_JCR);
        uri2pref.put(NamespaceRegistry.NAMESPACE_JCR, "jcr");

        Mockito.doAnswer(invocation -> {
            String rpref = invocation.getArgument(0);
            String ruri = invocation.getArgument(1);
            if (null != uri2pref.get(ruri) || null != pref2uri.get(rpref)) {
                throw new NamespaceException();
            } else {
                pref2uri.put(rpref, ruri);
                uri2pref.put(ruri, rpref);
                return null;
            }
        }).when(nsReg).registerNamespace(any(), any());

        when(nsReg.getPrefix(any())).thenAnswer(invocation -> {
            String found = uri2pref.get(invocation.getArgument(0));
            if (found != null) {
                return found;
            } else {
                throw new NamespaceException();
            }
        });

        when(nsReg.getURI(any())).thenAnswer(invocation -> {
            String found = pref2uri.get(invocation.getArgument(0));
            if (found != null) {
                return found;
            } else {
                throw new NamespaceException();
            }
        });

        when(session.getNamespacePrefix(any())).thenAnswer(invocation -> {
            String found = uri2pref.get(invocation.getArgument(0));
            if (found != null) {
                return found;
            } else {
                throw new NamespaceException();
            }
        });

        when(session.getNamespaceURI(any())).thenAnswer(invocation -> {
            String found = pref2uri.get(invocation.getArgument(0));
            if (found != null) {
                return found;
            } else {
                throw new NamespaceException();
            }
        });

        NamespaceHelper nsHelper = new NamespaceHelper(session);

        // register namespace (test makes assumptions about implementation)

        assertEquals("ns", nsHelper.registerNamespace("", "foo:"));
        assertEquals("ns2", nsHelper.registerNamespace("", "foo2:"));
        assertEquals("ns3", nsHelper.registerNamespace("xmlxxx", "foo3:"));
        assertEquals("ns4", nsHelper.registerNamespace("123", "foo4:"));
        assertEquals("ns3", nsHelper.registerNamespace("xmlxxx", "foo3:"));
        assertEquals("ns5", nsHelper.registerNamespace(null, "foo6:"));

        assertEquals("bar", nsHelper.registerNamespace("bar", "bar:"));
        assertEquals("bar2", nsHelper.registerNamespace("bar", "bar2:"));
        assertEquals("bar3", nsHelper.registerNamespace("bar", "bar3:"));

        assertEquals("jcr", nsHelper.registerNamespace("wtf", NamespaceRegistry.NAMESPACE_JCR));
        assertEquals("xml", nsHelper.registerNamespace("", NamespaceRegistry.NAMESPACE_XML));

        // register namespaces
        Map<String, String> input = Map.of("test1", "test1:", "test2", "test2", "", "test3:");
        nsHelper.registerNamespaces(input);
        assertEquals("test1", nsReg.getPrefix("test1:"));
        assertEquals("test2", nsReg.getPrefix("test2"));
        assertEquals("ns6", nsReg.getPrefix("test3:"));

        // check invocation count for getNamespaceRegistry (JCR-5161)
        Mockito.verify(workspace, Mockito.times(1)).getNamespaceRegistry();
    }
}
