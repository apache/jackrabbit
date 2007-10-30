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
package org.apache.jackrabbit.core;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.spi.Name;

import junit.framework.TestCase;

/**
 * Test case for the {@link NamespaceRegistryImpl} class. See also the
 * {@link org.apache.jackrabbit.test.api.NamespaceRegistryTest} and
 * {@link org.apache.jackrabbit.test.api.NamespaceRegistryReadMethodsTest}
 * test cases in the JCR TCK test suite.
 */
public class NamespaceRegistryImplTest extends TestCase {

    private static final String PREFIX = "test_safe_register_namespace";

    private static final String URI1 = "test-safe-register-namespace1";

    private static final String URI2 = "test-safe-register-namespace2";

    private static final String URI3 = "test-safe-register-namespace3";

    private Session session;

    private NamespaceRegistryImpl registry;

    /**
     * Removes the registered test namespaces.
     *
     * @throws RepositoryException on repository errors
     */
    private void clean() throws RepositoryException {
        try {
            registry.unregisterNamespace(registry.getPrefix(URI1));
        } catch (NamespaceException e) {
            // URI1 not yet registered
        }
        try {
            registry.unregisterNamespace(registry.getPrefix(URI2));
        } catch (NamespaceException e) {
            // URI2 not yet registered
        }
        try {
            registry.unregisterNamespace(registry.getPrefix(URI3));
        } catch (NamespaceException e) {
            // URI3 not yet registered
        }
    }

    protected void setUp() throws RepositoryException {
        session = TestRepository.getInstance().login();
        registry = (NamespaceRegistryImpl)
            session.getWorkspace().getNamespaceRegistry();
        clean();
    }

    protected void tearDown() throws RepositoryException {
        clean();
        session.logout();
    }

    public void testSafeRegisterNamespace() throws RepositoryException {
        try {
            registry.safeRegisterNamespace(PREFIX, URI1);
        } catch (NamespaceException e) {
            fail("safeRegisterNamespace() fails to register a namespace");
        }
        try {
            registry.safeRegisterNamespace(PREFIX, URI2);
        } catch (NamespaceException e) {
            fail("safeRegisterNamespace() fails to generate a unique prefix");
        }
        try {
            registry.safeRegisterNamespace(PREFIX, URI1);
        } catch (NamespaceException e) {
            fail("safeRegisterNamespace() fails to reregister a namespace");
        }
        try {
            registry.safeRegisterNamespace(Name.NS_XML_PREFIX, URI3);
        } catch (NamespaceException e) {
            fail("safeRegisterNamespace() fails to handle a reserved prefix");
        }
    }

}
