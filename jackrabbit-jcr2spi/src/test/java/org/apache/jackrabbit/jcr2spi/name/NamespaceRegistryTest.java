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
package org.apache.jackrabbit.jcr2spi.name;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>NamespaceRegistryTest</code>...
 */
public class NamespaceRegistryTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(NamespaceRegistryTest.class);

    /** Default value of test prefix */
    private static final String TEST_PREFIX = "test";

    /** Default value of test namespace uri */
    private static final String TEST_URI = "http://www.apache.org/jackrabbit/test/namespaceRegistryTest";

    private NamespaceRegistry nsRegistry;
    private String testPrefix;
    private String testURI;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        nsRegistry = superuser.getWorkspace().getNamespaceRegistry();

        testPrefix = getUnusedPrefix();
        testURI = getUnusedURI();

        boolean level2 = Boolean.valueOf(superuser.getRepository().getDescriptor(Repository.LEVEL_2_SUPPORTED)).booleanValue();
        if (!level2) {
            throw new NotExecutableException("Cannot test namespace registration/unregistration. Repository is a Level 1 only.");
        }
    }

    @Override
    protected void tearDown() throws Exception {
        nsRegistry = null;
        super.tearDown();
    }

    /**
     * Test if a new registered namespace is immediately visible through another
     * session object.
     *
     * @throws RepositoryException
     */
    public void testRegisteredNamespaceVisibility() throws RepositoryException {
        Session otherSession = getHelper().getReadOnlySession();
        try {
            NamespaceRegistry other = otherSession.getWorkspace().getNamespaceRegistry();

            nsRegistry.registerNamespace(testPrefix, testURI);
            String otherUri = other.getURI(testPrefix);
            String otherPrefix = other.getPrefix(testURI);
            assertTrue("Namespace registered must be immediately visible to any other session.", testURI.equals(otherUri) && testPrefix.equals(otherPrefix));
        } finally {
            otherSession.logout();
        }
    }

    /**
     * Test if a replace namespace prefix cannot be used as key any more to
     * retrieve the uri.
     *
     * @throws RepositoryException
     */
    public void testReRegisteredNamespace() throws RepositoryException {
        nsRegistry.registerNamespace(testPrefix, testURI);
        String replacePrefix = getUnusedPrefix();
        nsRegistry.registerNamespace(replacePrefix, testURI);
        try {
            nsRegistry.getURI(testPrefix);
            fail("Namespace with prefix " + testPrefix + " has been reregistered with new prefix " + replacePrefix);
        } catch (NamespaceException e) {
            // OK
        }
    }

    /**
     * Test if a replace namespace prefix cannot be used as key any more to
     * retrieve the uri in the <code>NamespaceRegistry</code> retrieved by
     * another Session object.
     *
     * @throws RepositoryException
     */
    public void testReRegisteredNamespace2() throws RepositoryException {
        Session otherSession = getHelper().getReadOnlySession();
        try {
            NamespaceRegistry other = otherSession.getWorkspace().getNamespaceRegistry();

            nsRegistry.registerNamespace(testPrefix, testURI);
            other.getPrefix(testURI);

            String replacePrefix = getUnusedPrefix();
            nsRegistry.registerNamespace(replacePrefix, testURI);

            String otherPrefix = other.getPrefix(testURI);
            assertEquals("Namespace with prefix " + testPrefix + " has been reregistered with new prefix " + replacePrefix, replacePrefix, otherPrefix);
        } finally {
            otherSession.logout();
        }
    }

    /**
     * Test if a replaced namespace prefix is immediately visible in the
     * NamespaceRegistry obtained from another session object.
     *
     * @throws RepositoryException
     */
    public void testReRegisteredNamespaceVisibility() throws RepositoryException {
        Session otherSession = getHelper().getReadOnlySession();
        try {
            NamespaceRegistry other = otherSession.getWorkspace().getNamespaceRegistry();

            nsRegistry.registerNamespace(testPrefix, testURI);
            other.getPrefix(testURI);

            String replacePrefix = getUnusedPrefix();
            nsRegistry.registerNamespace(replacePrefix, testURI);

            String otherUri = other.getURI(replacePrefix);
            String otherPrefix = other.getPrefix(testURI);
            assertTrue("Namespace registered must be immediately visible to any other session.", testURI.equals(otherUri) && replacePrefix.equals(otherPrefix));

            try {
                other.getURI(testPrefix);
                fail("Namespace with prefix " + testPrefix + " has been reregistered with new prefix " + replacePrefix);
            } catch (NamespaceException e) {
                // OK
            }
        } finally {
            otherSession.logout();
        }
    }

    /**
     * Test if unregistering a namespace is propagated to all other sessions.
     *
     * @throws RepositoryException
     */
    public void testUnregisteredNamespaceVisibility() throws RepositoryException, NotExecutableException {
        String prefix = getUnusedPrefix();
        String uri = getUnusedURI();

        Session otherSession = getHelper().getReadOnlySession();
        try {
            NamespaceRegistry other = otherSession.getWorkspace().getNamespaceRegistry();

            nsRegistry.registerNamespace(prefix, uri);
            try {
                nsRegistry.unregisterNamespace(prefix);
            } catch (NamespaceException e) {
                throw new NotExecutableException("Repository does not support unregistration of namespaces.");
            }

            String otherUri = other.getURI(prefix);
            String otherPrefix = other.getPrefix(uri);
            assertTrue("Namespace registered must be immediately visible to any other session.", uri.equals(otherUri) && prefix.equals(otherPrefix));
        } finally {
            otherSession.logout();
        }
    }

    /**
     * Returns a namespace prefix that currently not used in the namespace
     * registry.
     * @return an unused namespace prefix.
     */
    private String getUnusedPrefix() throws RepositoryException {
        Set<String> prefixes = new HashSet<String>(Arrays.asList(nsRegistry.getPrefixes()));
        String prefix = TEST_PREFIX;
        int i = 0;
        while (prefixes.contains(prefix)) {
            prefix = TEST_PREFIX + i++;
        }
        return prefix;
    }

    /**
     * Returns a namespace URI that currently not used in the namespace
     * registry.
     * @return an unused namespace URI.
     */
    private String getUnusedURI() throws RepositoryException {
        Set<String> uris = new HashSet<String>(Arrays.asList(nsRegistry.getURIs()));
        String uri = TEST_URI;
        int i = 0;
        while (uris.contains(uri)) {
            uri = TEST_URI + i++;
        }
        return uri;
    }
}