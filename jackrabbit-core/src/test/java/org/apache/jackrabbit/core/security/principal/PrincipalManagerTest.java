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
package org.apache.jackrabbit.core.security.principal;

import java.security.Principal;
import java.util.Properties;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.principal.GroupPrincipal;
import org.apache.jackrabbit.api.security.principal.JackrabbitPrincipal;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.mockito.Mockito;


/**
 * <code>PrincipalManagerTest</code>...
 */
public class PrincipalManagerTest extends AbstractJCRTest {

    private static final String TESTGROUP_NAME = "org.apache.jackrabbit.core.security.principal.PrincipalManagerTest.testgroup";
    private static final GroupPrincipal TESTGROUP = Mockito.mock(GroupPrincipal.class);

    private static class CustomPrincipalProvider extends AbstractPrincipalProvider {

        protected Principal providePrincipal(String principalName) {
            return TESTGROUP_NAME.equals(principalName) ? TESTGROUP : null;
        }

        public PrincipalIterator findPrincipals(String simpleFilter) {
            throw new UnsupportedOperationException();
        }

        public PrincipalIterator findPrincipals(String simpleFilter, int searchType) {
            throw new UnsupportedOperationException();
        }

        public PrincipalIterator getPrincipals(int searchType) {
            throw new UnsupportedOperationException();
        }

        public PrincipalIterator getGroupMembership(Principal principal) {
            throw new UnsupportedOperationException();
        }

        public boolean canReadPrincipal(Session session, Principal principalToRead) {
            return true;
        }
    }

    /**
     * Test if a group which is not item based will be wrapped by a JackrabbitPrincipal implementation.
     * @throws NotExecutableException
     * @throws RepositoryException
     */
    public void testJackrabbitPrincipal() throws NotExecutableException, RepositoryException {

        final PrincipalProvider testProvider = new CustomPrincipalProvider();
        testProvider.init(new Properties());
        PrincipalManagerImpl principalManager = new PrincipalManagerImpl(superuser, new PrincipalProvider[] { testProvider });
        Principal principalFromManager = principalManager.getPrincipal(TESTGROUP_NAME);
        assertTrue(principalFromManager instanceof JackrabbitPrincipal);
    }
}