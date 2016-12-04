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
package org.apache.jackrabbit.jca.test;

import java.io.Serializable;
import java.util.HashSet;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.naming.Referenceable;
import javax.resource.spi.ManagedConnection;
import javax.transaction.xa.XAResource;

import org.apache.jackrabbit.jca.JCAConnectionRequestInfo;
import org.apache.jackrabbit.jca.JCARepositoryHandle;
import org.apache.jackrabbit.jca.JCASessionHandle;

/**
 * This case executes tests on the connection factory.
 */
public final class ConnectionFactoryTest
        extends AbstractTestCase {

    /**
     * Test the connection factory allocation.
     */
    public void testAllocation() throws Exception {

        // Create the connection factory
        Object cf = mcf.createConnectionFactory();
        assertTrue(cf instanceof JCARepositoryHandle);
        Repository repository = (Repository) cf;

        // Open a new session
        Session session = repository.login(JCR_SUPERUSER);
        assertTrue(session != null);
        assertTrue(session instanceof JCASessionHandle);

        // Logout session
        session.logout();
    }

    /**
     * Test the connection matching.
     */
    public void testMatching() throws Exception {

        // Create connection request infos
        JCAConnectionRequestInfo cri1 = new JCAConnectionRequestInfo(JCR_SUPERUSER, JCR_WORKSPACE);
        JCAConnectionRequestInfo cri2 = new JCAConnectionRequestInfo(JCR_ANONUSER, JCR_WORKSPACE);

        // Check if not same
        assertNotSame(cri1, cri2);

        // Create the connection factory
        mcf.createConnectionFactory();

        // Allocate connections
        ManagedConnection mc1 = mcf.createManagedConnection(null, cri1);
        ManagedConnection mc2 = mcf.createManagedConnection(null, cri2);

        // Check if not same
        assertTrue(mc1 != mc2);

        // Create a sef of connections
        HashSet connectionSet = new HashSet();
        connectionSet.add(mc1);
        connectionSet.add(mc2);

        // Match the first connection
        JCAConnectionRequestInfo cri3 = new JCAConnectionRequestInfo(cri1);
        assertTrue((cri1 != cri3) && cri1.equals(cri3));
        ManagedConnection mc3 = mcf.matchManagedConnections(connectionSet, null, cri3);
        assertTrue(mc1 == mc3);

        // Match the second connection
        JCAConnectionRequestInfo cri4 = new JCAConnectionRequestInfo(cri2);
        assertTrue((cri2 != cri4) && cri2.equals(cri4));
        ManagedConnection mc4 = mcf.matchManagedConnections(connectionSet, null, cri4);
        assertTrue(mc2 == mc4);
    }

    /**
     * Test if the connection factory is serializable.
     */
    public void testSerializable() throws Exception {

        // Create the connection factory
        Object cf = mcf.createConnectionFactory();

        // Check if serializable and referenceable
        assertTrue(cf != null);
        assertTrue(cf instanceof Serializable);
        assertTrue(cf instanceof Referenceable);
    }

    /**
     * Test if the session supports transactions
     */
    public void testTransactionSupport() throws Exception {
        // Create the connection factory
        Object cf = mcf.createConnectionFactory();
        assertTrue(cf instanceof JCARepositoryHandle);
        Repository repository = (Repository) cf;

        // Open a session
        Session session = repository.login(JCR_SUPERUSER);
        assertTrue(session instanceof XAResource);
        session.logout();
    }
    
    /**
     * Tests if a NoSuchWorkspaceException is thrown if a wrong workspace name is given to login
     */
    public void testExceptionHandling() throws Exception {
        Object cf = mcf.createConnectionFactory();
        Repository repository = (Repository) cf;
        try {
            repository.login(JCR_SUPERUSER, "xxx");
        } catch (Exception e) {
            assertTrue(e instanceof NoSuchWorkspaceException);
        }
    }

}
