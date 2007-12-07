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

import javax.jcr.Session;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import javax.jcr.Property;
import javax.jcr.nodetype.NodeType;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * <code>NamespaceRemappingTest</code> tests transient namespace remapping.
 *
 * @test
 * @sources NamespaceRemappingTest.java
 * @executeClass org.apache.jackrabbit.test.api.NamespaceRemappingTest
 * @keywords level1
 */
public class NamespaceRemappingTest extends AbstractJCRTest {

    /**
     * The read only session for the test cases
     */
    private Session session;

    /**
     * The namespace registry of the current session
     */
    private NamespaceRegistry nsr;

    /**
     * Sets up the fixture for the tests.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
        session = helper.getReadOnlySession();
        nsr = session.getWorkspace().getNamespaceRegistry();
    }

    protected void tearDown() throws Exception {
        try {
            if (session != null) {
                session.logout();
                session = null;
            }
        } finally {
            nsr = null;
            super.tearDown();
        }
    }

    /**
     * Tests if the remapping of jcr:primaryType to a different prefix works and
     * returns the property with the correct primaryType value.
     */
    public void testNamespaceRemapping() throws RepositoryException {
        Property primaryTypeProp = session.getRootNode().getProperty(jcrPrimaryType);
        NodeType ntBaseType = session.getWorkspace().getNodeTypeManager().getNodeType(ntBase);

        // find an unused prefix
        String jcrPrefix = getUnusedPrefix();
        // remap jcr prefix
        session.setNamespacePrefix(jcrPrefix, NS_JCR_URI);
        // find an unused prefix
        String ntPrefix = getUnusedPrefix();
        // remap nt prefix
        session.setNamespacePrefix(ntPrefix, NS_NT_URI);

        assertTrue("Unable to retrieve property with new namespace prefix.",
                session.getRootNode().getProperty(jcrPrefix + ":primaryType").isSame(primaryTypeProp));

        assertEquals("NodeType name does not use new namespace prefix.",
                ntBaseType.getName(), ntPrefix + ":base");

        String propval = session.getRootNode().getProperty(jcrPrefix + ":primaryType").getString();
        String primaryType = session.getRootNode().getPrimaryNodeType().getName();
        assertEquals("Remapping of jcr prefix failed", primaryType, propval);
    }

    /**
     * Tests if the remapping is cleared in a new session object
     */
    public void testRemapClearing() throws RepositoryException {
        // find an unused prefix
        String testPrefix = getUnusedPrefix();
        // remap jcr prefix
        session.setNamespacePrefix(testPrefix, NS_JCR_URI);
        session.logout();

        session = helper.getReadOnlySession();
        try {
            session.getNamespaceURI(testPrefix);
            fail("Must throw a NamespaceException on unknown prefix.");
        } catch (NamespaceException nse) {
            // correct
        }
    }

    /**
     * Tests if a remapping to "xml" fails correctly
     */
    public void testXmlRemapping() throws RepositoryException {
        try {
            session.setNamespacePrefix("xml", NS_JCR_URI);
            fail("Remapping a namespace uri to 'xml' must not be possible");
        } catch (NamespaceException nse) {
            // correct
        }
        try {
            session.setNamespacePrefix("xmlfoo", NS_JCR_URI);
            fail("Remapping a namespace uri to 'xmlfoo' must not be possible");
        } catch (NamespaceException nse) {
            // correct
        }
    }

    /**
     * tests that when a prefix which is mapped to a URI yet globally registered
     * this prefix cannot be remapped to another URI with
     * session.setNamespacePrefix()
     */
    public void testNamespaceException() throws RepositoryException {
        String testURI = getUnusedURI();
        String prefix = session.getNamespacePrefix(NS_JCR_URI);
        try {
            session.setNamespacePrefix(prefix, testURI);
            fail("NamespaceRegistry must not register a URI with an already assign prefix");
        } catch (NamespaceException nse) {
            // ok
        }
    }

    /**
     * Tests that Session.getNamespaceURI() returns according the session scoped
     * mapping
     */
    public void testGetNamespaceURI() throws RepositoryException {
        String testPrefix = getUnusedPrefix();
        // remap the jcr uri
        session.setNamespacePrefix(testPrefix, NS_JCR_URI);
        String uri = session.getNamespaceURI(testPrefix);
        assertEquals("Session.getNamespaceURI does not return the correct value.", NS_JCR_URI, uri);
    }

    /**
     * Tests that Session.getNamespacePrefix returns the session scoped
     * mapping.
     */
    public void testGetNamespacePrefix() throws RepositoryException {
        String testPrefix = getUnusedPrefix();
        // remap the jcr uri
        session.setNamespacePrefix(testPrefix, NS_JCR_URI);
        String prefix = session.getNamespacePrefix(NS_JCR_URI);
        assertEquals("Session.getNamespacePrefix does not return the correct value.", testPrefix, prefix);
    }

    /**
     * Tests if {@link javax.jcr.Session#getNamespacePrefixes()} returns
     * all prefixes currently set for this session, including all those
     * registered in the NamespaceRegistry but not over-ridden by a
     * Session.setNamespacePrefix, plus those currently set locally by
     * Session.setNamespacePrefix.
     */
    public void testGetNamespacePrefixes() throws RepositoryException {
        String testPrefix = getUnusedPrefix();

        // remap the jcr uri
        session.setNamespacePrefix(testPrefix, NS_JCR_URI);

        String prefixes[] = session.getNamespacePrefixes();

        assertEquals("Session.getNamespacePrefixes() must return all prefixes " +
                "currently set for this session.",
                nsr.getPrefixes().length,
                session.getNamespacePrefixes().length);

        // the prefix of the jcr uri as set in the namespace registry
        String prefixNSR = nsr.getPrefix(NS_JCR_URI);

        // test if the "NSR prefix" (and over-ridden by the session) is not
        // returned by Session.getNamespacePrefixes()
        for (int i = 0; i < prefixes.length; i++) {
            if (prefixes[i].equals(prefixNSR)) {
                fail("Session.getNamespacePrefixes() must not return the " +
                     "prefixes over-ridden by Session.setNamespacePrefix");
            }
        }
    }


    /**
     * Returns a namespace prefix that is not in use.
     *
     * @return a namespace prefix that is not in use.
     */
    private String getUnusedPrefix() throws RepositoryException {
        Set prefixes = new HashSet();
        prefixes.addAll(Arrays.asList(session.getNamespacePrefixes()));
        String prefix = "myapp";
        int count = 0;
        while (prefixes.contains(prefix + count)) {
            count++;
        }
        return prefix + count;
    }

    /**
     * Returns a namespace uri that is not in use.
     *
     * @return a namespace uri that is not in use.
     */
    private String getUnusedURI() throws RepositoryException {
        Set uris = new HashSet();
        uris.addAll(Arrays.asList(nsr.getURIs()));
        String uri = "http://www.unknown-company.com/namespace";
        int count = 0;
        while (uris.contains(uri + count)) {
            count++;
        }
        return uri + count;
    }
}
