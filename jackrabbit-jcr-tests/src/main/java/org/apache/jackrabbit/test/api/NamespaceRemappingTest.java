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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Test cases for JSR 283 sections 3.5.2 Session-Local Mappings and
 * 5.11 Namespace Mapping and the related namespace mapping methods
 * in {@link Session}.
 *
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
        session = getHelper().getReadOnlySession();
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
     * Test case for the automatic generation of a new local prefix for a
     * registered namespace URI that doesn't have a local mapping, as specified
     * in section 3.5.2 Session-Local Mappings:
     *
     * <blockquote>
     * If a JCR method returns a name from the repository with a namespace URI
     * for which no local mapping exists, a prefix is created automatically
     * and a mapping between that prefix and the namespace URI in question is
     * added to the set of local mappings. The new prefix must differ from
     * those already present among the set of local mappings.
     * </blockquote>
     */
    public void testAutomaticNewLocalPrefix() throws RepositoryException {
        Set<String> prefixes =
            new HashSet<String>(Arrays.asList(session.getNamespacePrefixes()));
        prefixes.remove(session.getNamespacePrefix(NS_JCR_URI));
        prefixes.remove(session.getNamespacePrefix(NS_NT_URI));

        // Remove the local mapping of NS_JCR_URI
        // NOTE: JCR 1.0 repositories will throw an exception on this
        String before = session.getNamespacePrefix(NS_JCR_URI);
        session.setNamespacePrefix(before, NS_NT_URI);

        // Have the repository come up with a new prefix for this namespace
        String name =
            session.getProperty("/{" + NS_JCR_URI + "}primaryType").getName();
        int colon = name.indexOf(':');
        String after = name.substring(0, colon);

        assertFalse(
                "Automatically created new local prefix of a namespace"
                + " must be different from the prefix that removed the mapping",
                after.equals(before));

        assertFalse(
                "Automatically created new local prefix of a namespace"
                + " must be different from those already present",
                prefixes.contains(after));

        try {
            assertEquals(
                    "The local namespace mappings must match the"
                    + " automatically created new prefix of a namespace",
                    after, session.getNamespacePrefix(NS_JCR_URI));
        } catch (NamespaceException e) {
            fail("Automatically created new prefix must be included in"
                    + " the set of local namespace mappings");
        }
    }

    /**
     * Test case for the unknown prefix behaviour specified in
     * section 3.5.2 Session-Local Mappings:
     * 
     * <blockquote>
     * If a JCR method is passed a name or path containing a prefix which
     * does not exist in the local mapping an exception is thrown.
     * </blockquote>
     */
    public void testExceptionOnUnknownPrefix() throws RepositoryException {
        // Change the local prefix of of NS_JCR_URI
        // NOTE: JCR 1.0 repositories will throw an exception on this
        String before = session.getNamespacePrefix(NS_JCR_URI);
        String after = before + "-changed";
        session.setNamespacePrefix(after, NS_JCR_URI);

        // Try to use the changed prefix
        try {
            session.propertyExists("/" + before + ":primaryType");
            fail("A path with an unknown prefix must cause"
                    + " an exception to be thrown");
        } catch (RepositoryException expected) {
        }
    }

    /**
     * Test case for the initial set of local namespace mappings as specified
     * in section 3.5.2 Session-Local Mappings:
     *
     * <blockquote> 
     * When a new session is acquired, the mappings present in the persistent
     * namespace registry are copied to the local namespace mappings of that
     * session.
     * </blockquote>
     */
    public void testInitialLocalNamespaceMappings() throws RepositoryException {
        String[] uris = nsr.getURIs();
        for (int i = 0; i < uris.length; i++) {
            assertEquals(
                    "The initial local namespace prefix of \""
                    + uris[i] + "\" must match the persistent registry mapping",
                    nsr.getPrefix(uris[i]),
                    session.getNamespacePrefix(uris[i]));
            
        }
    }

    /**
     * Test case for the scope of the local namespace mappings as specified
     * in section 3.5.2 Session-Local Mappings:
     *
     * <blockquote> 
     * The resulting mapping table applies only within the scope of that session
     * </blockquote>
     *
     * <p>
     * Also specified in the javadoc of
     * {@link Session#setNamespacePrefix(String, String)}:
     *
     * <blockquote>
     * The remapping only affects operations done through this
     * <code>Session</code>. To clear all remappings, the client must
     * acquire a new <code>Session</code>.
     * </blockquote>
     */
    public void testScopeOfLocalNamepaceMappings() throws RepositoryException {
        String before = session.getNamespacePrefix(NS_JCR_URI);
        String after = before + "-changed";

        // Change the prefix of a well-known namespace
        session.setNamespacePrefix(after, NS_JCR_URI);
        assertEquals(after, session.getNamespacePrefix(NS_JCR_URI));

        // Check whether the mapping affects another session
        Session another = getHelper().getReadOnlySession();
        try {
            assertEquals(
                    "Local namespace changes must not affect other sessions",
                    before, another.getNamespacePrefix(NS_JCR_URI));
        } finally {
            another.logout();
        }
    }

    /**
     * Test case for the exception clauses of section 5.11 Namespace Mapping:
     *
     * <blockquote>
     * However, the method will throw an exception if
     * <ul>
     * <li>the specified prefix begins with the characters "xml"
     *     (in any combination of case) or,</li>
     * <li>the specified prefix is the empty string or,</li>
     * <li>the specified namespace URI is the empty string.</li>
     * </ul>
     * </blockquote>
     *
     * <p>
     * Also specified in the javadoc for throwing a {@link NamespaceException}
     * from {@link Session#setNamespacePrefix(String, String)}:
     *
     * <blockquote>
     * if an attempt is made to map a namespace URI to a prefix beginning
     * with the characters "<code>xml</code>" (in any combination of case)
     * or if an attempt is made to map either the empty prefix or the empty
     * namespace (i.e., if either <code>prefix</code> or  <code>uri</code>
     * are the empty string).
     * </blockquote>
     *
     * <p>
     * Section 3.2 Names also contains extra constraints on the prefix and
     * namespace URI syntax:
     *
     * <blockquote><pre>
     * Namespace   ::= EmptyString | Uri
     * EmptyString ::= The empty string
     * Uri         ::= A URI, as defined in Section 3 in
     *                 http://tools.ietf.org/html/rfc3986#section-3
     * Prefix      ::= Any string that matches the NCName production in
     *                 http://www.w3.org/TR/REC-xml-names
     * </pre></blockquote>
     *
     * <p>
     * It is unspecified whether an implementation should actually enforce
     * these constraints, so for now this test case <em>does not</em>
     * check this behaviour.
     */
    public void testExceptionsFromRemapping() throws RepositoryException {
        // Remapping to "xml..." in any combination of case must fail
        assertSetNamespacePrefixFails("xml",    NS_JCR_URI);
        assertSetNamespacePrefixFails("xmlfoo", NS_JCR_URI);
        assertSetNamespacePrefixFails("XML",    NS_JCR_URI);
        assertSetNamespacePrefixFails("XMLFOO", NS_JCR_URI);
        assertSetNamespacePrefixFails("Xml",    NS_JCR_URI);
        assertSetNamespacePrefixFails("XmlFoo", NS_JCR_URI);

        // Remapping the empty prefix or empty URI must fail
        assertSetNamespacePrefixFails("", NS_JCR_URI);
        assertSetNamespacePrefixFails("prefix", "");
    }

    /**
     * Checks that a {@link Session#setNamespacePrefix(String, String)}
     * call with the given arguments throws a {@link NamespaceException}.
     *
     * @param prefix namespace prefix
     * @param uri namespace URI
     * @throws RepositoryException if another error occurs
     */
    private void assertSetNamespacePrefixFails(String prefix, String uri)
            throws RepositoryException {
        try {
            session.setNamespacePrefix(prefix, uri);
            fail("Setting a local namespace mapping from \""
                    + prefix + "\" to \"" + uri + "\" must fail");
        } catch (NamespaceException expected) {
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
        Set<String> prefixes = new HashSet<String>();
        prefixes.addAll(Arrays.asList(session.getNamespacePrefixes()));
        String prefix = "myapp";
        int count = 0;
        while (prefixes.contains(prefix + count)) {
            count++;
        }
        return prefix + count;
    }

}
