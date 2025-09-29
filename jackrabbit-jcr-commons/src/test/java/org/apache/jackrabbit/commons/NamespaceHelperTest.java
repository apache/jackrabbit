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

    public void testRegisterNamespaceDeriving() throws RepositoryException {
        Workspace workspace = Mockito.mock(Workspace.class);
        NamespaceRegistry nsReg = mockNamespaceRegistry();
        Session session = mockSessionWithoutSessionMappings();

        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getNamespaceRegistry()).thenReturn(nsReg);
        NamespaceHelper nsHelper = new NamespaceHelper(session);

        // check deriving from namespace name
        assertEquals("example.com-foo", nsHelper.registerNamespace(null, "https://ns.example.com/foo"));

        // check deriving from namespace name with prefix already taken, hash used instead
        assertEquals("s-12a490e", nsHelper.registerNamespace(null, "http://www.example.com/foo"));

        // nasty: computed hash already used as prefix
        assertEquals("example.com-bar", nsHelper.registerNamespace(null, "http://www.example.com/bar"));
        assertEquals("s-b2e675c", nsHelper.registerNamespace("s-b2e675c", "ouch:"));
        // returned prefix uses the SHA, but one more character
        assertEquals("s-b2e675c8", nsHelper.registerNamespace(null, "example.com/bar"));
    }

    public void testRegisterNamespaceKnownMapping() throws RepositoryException {
        Workspace workspace = Mockito.mock(Workspace.class);
        NamespaceRegistry nsReg = mockNamespaceRegistry();
        Session session = mockSessionWithoutSessionMappings();

        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getNamespaceRegistry()).thenReturn(nsReg);
        NamespaceHelper nsHelper = new NamespaceHelper(session);

        // check deriving from namespace name
        assertEquals("example.com-foo", nsHelper.registerNamespace(null, "https://ns.example.com/foo"));
    }

    public void testRegisterNamespaceSimple() throws RepositoryException {
        Workspace workspace = Mockito.mock(Workspace.class);
        NamespaceRegistry nsReg = mockNamespaceRegistry();
        Session session = mockSessionWithoutSessionMappings();

        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getNamespaceRegistry()).thenReturn(nsReg);
        NamespaceHelper nsHelper = new NamespaceHelper(session);

        // simple cases
        assertEquals("foo", nsHelper.registerNamespace("", "foo:"));
        assertEquals("foo2", nsHelper.registerNamespace("", "foo2:"));

        // suggested prefix will be ignored because it is illegal, so a new prefix is generated
        assertEquals("foo3", nsHelper.registerNamespace("xmlxxx", "foo3:"));

        // suggested prefix will be ignored because it is illegal, so a new prefix is generated
        assertEquals("foo4", nsHelper.registerNamespace("123", "foo4:"));

        // suggested prefix will be ignored because it is illegal, however the namespace name already is mapped
        assertEquals("foo3", nsHelper.registerNamespace("xmlxxx", "foo3:"));

        // null prefix, handled like illegal prefix, namespace name is new thus new prefix
        assertEquals("foo6", nsHelper.registerNamespace(null, "foo6:"));

        // simple case: new namespace name, new prefix
        assertEquals("bar", nsHelper.registerNamespace("bar", "bar:"));

        // suggested prefix already is mapped to a different namespace, derive new one based on namespace name
        assertEquals("bar2", nsHelper.registerNamespace("bar", "bar2:"));

        // repeat
        assertEquals("bar3", nsHelper.registerNamespace("bar", "bar3:"));

        // check invocation count for getNamespaceRegistry (JCR-5161)
        Mockito.verify(workspace, Mockito.times(1)).getNamespaceRegistry();
    }

    public void testRegisterNamespacePreferredPrefixTaken() throws RepositoryException {
        Workspace workspace = Mockito.mock(Workspace.class);
        NamespaceRegistry nsReg = mockNamespaceRegistry();
        Session session = mockSessionWithoutSessionMappings();

        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getNamespaceRegistry()).thenReturn(nsReg);
        NamespaceHelper nsHelper = new NamespaceHelper(session);

        // nasty: registering a preferred namespace prefix, fall back to namespace name based prefix generation
        assertEquals("xmp", nsHelper.registerNamespace("xmp", "xmp-ouch:"));
        assertEquals("adobe.com-xap-1.0", nsHelper.registerNamespace(null, "http://ns.adobe.com/xap/1.0/"));
    }

    public void testRegisterNamespaceImmutableMappings() throws RepositoryException {
        Workspace workspace = Mockito.mock(Workspace.class);
        NamespaceRegistry nsReg = mockNamespaceRegistry();
        Session session = mockSessionWithoutSessionMappings();

        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getNamespaceRegistry()).thenReturn(nsReg);
        NamespaceHelper nsHelper = new NamespaceHelper(session);

        // attempt to register over immutable mappings returns hardwired prefix
        assertEquals("jcr", nsHelper.registerNamespace("wtf", NamespaceRegistry.NAMESPACE_JCR));
        assertEquals("xml", nsHelper.registerNamespace("", NamespaceRegistry.NAMESPACE_XML));
    }

    public void testRegisterNamespaceProblematicNames() throws RepositoryException {
        Workspace workspace = Mockito.mock(Workspace.class);
        NamespaceRegistry nsReg = mockNamespaceRegistry();
        Session session = mockSessionWithoutSessionMappings();

        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getNamespaceRegistry()).thenReturn(nsReg);
        NamespaceHelper nsHelper = new NamespaceHelper(session);

        // problematic trailing chars
        assertEquals("urn-xyz", nsHelper.registerNamespace("", "urn:xyz::"));
        // would be same prefix after removing trailing problems, thus falling back to hash
        assertEquals("s-98cf7c3", nsHelper.registerNamespace("", "urn:xyz:::"));
    }

    public void testRegisterMultipleNamespaces() throws RepositoryException {
        Workspace workspace = Mockito.mock(Workspace.class);
        NamespaceRegistry nsReg = mockNamespaceRegistry();
        Session session = mockSessionWithoutSessionMappings();

        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getNamespaceRegistry()).thenReturn(nsReg);
        NamespaceHelper nsHelper = new NamespaceHelper(session);

        Map<String, String> input = Map.of("test1", "test1:", "test2", "test2", "", "test3:");
        nsHelper.registerNamespaces(input);

        // registered prefixes as suggested
        assertEquals("test1", nsReg.getPrefix("test1:"));

        // test2 is invalid namespace name, but reluctantly accepted
        assertEquals("test2", nsReg.getPrefix("test2"));

        // no prefix suggested, prefix derived from namespace name
        assertEquals("test3", nsReg.getPrefix("test3:"));
    }

    // poor man's namespace registry

    private Map<String, String> pref2uri = new HashMap<>();
    private Map<String, String> uri2pref = new HashMap<>();

    private NamespaceRegistry mockNamespaceRegistry() throws RepositoryException {
        NamespaceRegistry result = Mockito.mock(NamespaceRegistry.class);

        // defaults (incomplete)
        pref2uri.put("xml", NamespaceRegistry.NAMESPACE_XML);
        uri2pref.put(NamespaceRegistry.NAMESPACE_XML, "xml");
        pref2uri.put("jcr", NamespaceRegistry.NAMESPACE_JCR);
        uri2pref.put(NamespaceRegistry.NAMESPACE_JCR, "jcr");

        Mockito.doAnswer(invocation -> {
            String rpref = invocation.getArgument(0);
            String rname = invocation.getArgument(1);
            if (null != uri2pref.get(rname)) {
                throw new NamespaceException("namespace name '" + rname + "' already registered");
            } else if (null != pref2uri.get(rpref)) {
                throw new NamespaceException("namespace prefix '" + rpref + "' already registered");
            } else {
                pref2uri.put(rpref, rname);
                uri2pref.put(rname, rpref);
                return null;
            }
        }).when(result).registerNamespace(any(), any());

        when(result.getPrefix(any())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            String found = uri2pref.get(name);
            if (found != null) {
                return found;
            } else {
                throw new NamespaceException("namespace name '" + name + "' not registered");
            }
        });

        when(result.getURI(any())).thenAnswer(invocation -> {
            String prefix = invocation.getArgument(0);
            String found = pref2uri.get(prefix);
            if (found != null) {
                return found;
            } else {
                throw new NamespaceException("namespace prefix '" + prefix + "' not registered");
            }
        });

        return result;
    }

    private Session mockSessionWithoutSessionMappings() throws RepositoryException {
        Session result = Mockito.mock(Session.class);

        // no session-local mappings

        when(result.getNamespacePrefix(any())).thenAnswer(invocation -> {
            String namespace = invocation.getArgument(0);
            String found = uri2pref.get(namespace);
            if (found != null) {
                return found;
            } else {
                throw new NamespaceException("no mapping found for namespace '" + namespace + "'");
            }
        });

        when(result.getNamespaceURI(any())).thenAnswer(invocation -> {
            String prefix = invocation.getArgument(0);
            String found = pref2uri.get(prefix);
            if (found != null) {
                return found;
            } else {
                throw new NamespaceException("no mapping found for prefix '" + prefix + "'");
            }
        });

        return result;
    }
}
