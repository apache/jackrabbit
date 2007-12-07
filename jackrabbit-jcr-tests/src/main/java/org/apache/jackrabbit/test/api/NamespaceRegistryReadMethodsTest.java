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
package org.apache.jackrabbit.test.api;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * <code>NamespaceRegistryReadMethodsTest</code> This class tests read method of the
 * NamespaceRegistry class and also the correct Exception throwing for methods
 * not supported in level 1.
 *
 * @test
 * @sources NamespaceRegistryReadMethodsTest.java
 * @executeClass org.apache.jackrabbit.test.api.NamespaceRegistryReadMethodsTest
 * @keywords level1
 */
public class NamespaceRegistryReadMethodsTest extends AbstractJCRTest {

    /** The built in namespace prefixes */
    private static final String[] BUILTIN_PREFIXES = {"jcr", "nt", "mix", "sv", ""};

    /** The built in namespace uris */
    private static final String[] BUILTIN_URIS = {NS_JCR_URI, NS_NT_URI, NS_MIX_URI, NS_SV_URI, ""};

    /** The NamespaceRegistry of the repository */
    private NamespaceRegistry nsr;

    /** A read-only session */
    private Session session;

    public void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
        session = helper.getReadOnlySession();
        Workspace ws = session.getWorkspace();
        nsr = ws.getNamespaceRegistry();
    }

    /**
     * Releases the session aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
            session = null;
        }
        nsr = null;
        super.tearDown();
    }

    /**
     * Tests if {@link javax.jcr.NamespaceRegistry#getPrefixes()} returns the
     * required namespace prefixes and if they are mapped to the correct URIs.
     */
    public void testGetNamespacePrefixes() throws RepositoryException {
        Set prefixes = new HashSet();
        prefixes.addAll(Arrays.asList(nsr.getPrefixes()));
        for (int i = 0; i < BUILTIN_PREFIXES.length; i++) {
            String prefix = BUILTIN_PREFIXES[i];
            assertTrue("NamespaceRegistry does not contain built in prefix: " + prefix, prefixes.contains(prefix));
            String uri = nsr.getURI(prefix);
            assertEquals("Wrong namespace mapping for prefix: " + prefix, BUILTIN_URIS[i], uri);
        }
    }

    /**
     * Tests if {@link javax.jcr.NamespaceRegistry#getURIs()} returns the
     * required namespace URIs and if they are mapped to the correct prefixes.
     */
    public void testGetNamespaceURIs() throws RepositoryException {
        Set uris = new HashSet();
        uris.addAll(Arrays.asList(nsr.getURIs()));
        for (int i = 0; i < BUILTIN_URIS.length; i++) {
            String uri = BUILTIN_URIS[i];
            assertTrue("NamespaceRegistry does not contain built in uri: " + uri, uris.contains(uri));
            String prefix = nsr.getPrefix(uri);
            assertEquals("Wrong namespace mapping for uri: " + uri, BUILTIN_PREFIXES[i], prefix);
        }
    }

    /**
     * Tests if a {@link javax.jcr.NamespaceException} is thrown when
     * {@link javax.jcr.NamespaceRegistry#getURI(String)} is called for an
     * unknown prefix.
     */
    public void testGetURINamespaceException() throws RepositoryException, NotExecutableException {
        Set prefixes = new HashSet();
        prefixes.addAll(Arrays.asList(nsr.getPrefixes()));
        String prefix = "myapp";
        int count = 0;
        while (prefixes.contains(prefix + count)) {
            count++;
        }
        String testPrefix = prefix + count;
        try {
            nsr.getURI(testPrefix);
            fail("NamespaceRegistry.getURI should throw a NamespaceException " +
                    "in case of an unmapped prefix.");
        } catch (NamespaceException nse) {
            //ok
        }
    }

    /**
     * Tests if a {@link javax.jcr.NamespaceException} is thrown when
     * {@link javax.jcr.NamespaceRegistry#getPrefix(String)} is called for an
     * unknown URI.
     */
    public void testGetPrefixNamespaceException() throws RepositoryException, NotExecutableException {
        Set uris = new HashSet();
        uris.addAll(Arrays.asList(nsr.getURIs()));
        String uri = "http://www.unknown-company.com/namespace";
        int count = 0;
        while (uris.contains(uri + count)) {
            count++;
        }
        String testURI = uri + count;
        try {
            nsr.getPrefix(testURI);
            fail("NamespaceRegistry.getPrefix should throw a NamespaceException " +
                    "in case of an unregistered URI.");
        } catch (NamespaceException nse) {
            //ok
        }
    }

}