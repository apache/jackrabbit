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

import javax.jcr.Item;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>NamespaceRegistryTest</code> tests whether the repository registers and
 * unregisters namespaces correctly. This is a level 2 feature.
 * <p/>
 * NOTE: Implementations are free to not support unregistering. In other words:
 * Even a repository that supports namespaces may always legally throw an
 * exception when you try to unregister.
 *
 * @test
 * @sources NamespaceRegistryTest.java
 * @executeClass org.apache.jackrabbit.test.api.NamespaceRegistryTest
 * @keywords level2
 */
public class NamespaceRegistryTest extends AbstractJCRTest {

    /** The system namespace prefixes */
    private static final String[] SYSTEM_PREFIXES = {"jcr", "nt", "mix", "sv"};

    /** Default value of test prefix */
    private static final String TEST_PREFIX = "tst";

    /** Default value of test namespace uri */
    private static final String TEST_URI = "http://www.apache.org/jackrabbit/test/namespaceRegistryTest";

    /** The namespace registry of the superuser session */
    private NamespaceRegistry nsp;

    /** The namespace prefix we use for the tests */
    private String namespacePrefix;

    /** The namespace uri we use for the tests */
    private String namespaceUri;

    protected void setUp() throws Exception {
        super.setUp();
        nsp = superuser.getWorkspace().getNamespaceRegistry();

        namespacePrefix = getUnusedPrefix();
        namespaceUri = getUnusedURI();
    }

    protected void tearDown() throws Exception {
        try {
            if (Arrays.asList(nsp.getPrefixes()).contains(namespacePrefix)) {
                nsp.unregisterNamespace(namespacePrefix);
            }
        } catch (NamespaceException e) {
            log.println("Unable to unregister name space with prefix " + namespacePrefix + ": " + e.toString());
        } finally {
            nsp = null;
            super.tearDown();
        }
    }

    /**
     * Trying to register a system namespace must throw a NamespaceException
     */
    public void testRegisterNamespaceExceptions() throws RepositoryException {
        try {
            nsp.registerNamespace("jcr", namespaceUri);
            fail("Trying to register the namespace 'jcr' must throw a NamespaceException.");
        } catch (NamespaceException e) {
            // expected behaviour
        }
        try {
            nsp.registerNamespace("nt", namespaceUri);
            fail("Trying to register the namespace 'nt' must throw a NamespaceException.");
        } catch (NamespaceException e) {
            // expected behaviour
        }
        try {
            nsp.registerNamespace("mix", namespaceUri);
            fail("Trying to register the namespace 'mix' must throw a NamespaceException.");
        } catch (NamespaceException e) {
            // expected behaviour
        }
        try {
            nsp.registerNamespace("sv", namespaceUri);
            fail("Trying to register the namespace 'sv' must throw a NamespaceException.");
        } catch (NamespaceException e) {
            // expected behaviour
        }
    }

    /**
     * Trying to register "xml" or anything that starts with "xml" as a
     * namespace must throw a repository exception
     */
    public void testRegisterNamespaceXmlExceptions() throws RepositoryException {
        try {
            nsp.registerNamespace("xml", namespaceUri);
            fail("Trying to register the namespace 'xml' must throw a NamespaceException.");
        } catch (NamespaceException e) {
            // expected behaviour
        }
        try {
            nsp.registerNamespace("xml" + Math.floor(Math.random() * 999999), namespaceUri);
            fail("Trying to register a namespace that starts with 'xml' must throw a NamespaceException.");
        } catch (NamespaceException e) {
            // expected behaviour
        }
    }

    /**
     * Tries to register a namespace.
     */
    public void testRegisterNamespace() throws RepositoryException {
        nsp.registerNamespace(namespacePrefix, namespaceUri);

        assertEquals("Namespace prefix was not registered.", namespacePrefix, nsp.getPrefix(namespaceUri));
        assertEquals("Namespace URI was not registered.", namespaceUri, nsp.getURI(namespacePrefix));

        Item created;
        
        try {
            created = testRootNode.addNode(namespacePrefix + ":root");
            testRootNode.save();
        }
        catch (RepositoryException ex) {
            // that didn't work; maybe the repository allows a property here?
            testRootNode.getSession().refresh(false);
            created = testRootNode.setProperty(namespacePrefix + ":root", "test");
            testRootNode.save();
        }

        // Need to remove it here, otherwise teardown can't unregister the NS.
        testRootNode.getSession().getItem(created.getPath()).remove();
        testRootNode.save();
    }

    /**
     * Tests whether unregistering a system namespace or an undefined namespace
     * throws the expected exception.
     */
    public void testUnregisterNamespaceExceptions() throws RepositoryException {
        // Attempting to unregister a built-in namespace
        // must throw a NamespaceException.
        for (int t = 0; t < SYSTEM_PREFIXES.length; t++) {
            try {
                nsp.unregisterNamespace(SYSTEM_PREFIXES[t]);
                fail("Trying to unregister " + SYSTEM_PREFIXES[t] + " must fail");
            } catch (NamespaceException e) {
                // expected behaviour
            }
        }

        // An attempt to unregister a namespace that is not currently registered
        // must throw a NamespaceException.
        try {
            nsp.unregisterNamespace("ThisNamespaceIsNotCurrentlyRegistered");
            fail("Trying to unregister an unused prefix must fail");
        } catch (NamespaceException e) {
            // expected behaviour
        }
    }

    //------------------------< internal >--------------------------------------

    /**
     * Returns a namespace prefix that currently not used in the namespace
     * registry.
     * @return an unused namespace prefix.
     */
    private String getUnusedPrefix() throws RepositoryException {
        Set prefixes = new HashSet(Arrays.asList(nsp.getPrefixes()));
        String prefix = TEST_PREFIX;
        int i = 0;
        while (prefixes.contains(prefix)) {
            prefix += i++;
        }
        return prefix;
    }

    /**
     * Returns a namespace URI that currently not used in the namespace
     * registry.
     * @return an unused namespace URI.
     */
    private String getUnusedURI() throws RepositoryException {
        Set uris = new HashSet(Arrays.asList(nsp.getURIs()));
        String uri = TEST_URI;
        int i = 0;
        while (uris.contains(uri)) {
            uri += i++;
        }
        return uri;
    }
}