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
package org.apache.jackrabbit.core.security.authentication;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.Impersonation;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import java.security.Principal;
import java.util.Iterator;

/**
 * <code>SimpleCredentialsAuthenticationTest</code>...
 */
public class SimpleCredentialsAuthenticationTest extends AbstractJCRTest {

    private static final Credentials creds = new Credentials(){};
    private static final SimpleCredentials simpleAA = new SimpleCredentials("a", "a".toCharArray());
    private static final SimpleCredentials simpleBB = new SimpleCredentials("b", "b".toCharArray());
    private static final SimpleCredentials simpleAB = new SimpleCredentials("a", "b".toCharArray());
    private static final SimpleCredentials simpleNull = new SimpleCredentials(null, new char[0]);
    private static final SimpleCredentials simpleEmpty = new SimpleCredentials("", new char[0]);

    private User user;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (superuser instanceof JackrabbitSession) {
            String userID = superuser.getUserID();
            user = (User) ((JackrabbitSession) superuser).getUserManager().getAuthorizable(userID);
        } else {
            throw new NotExecutableException();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCanHandle() throws RepositoryException {
        Authentication a = new SimpleCredentialsAuthentication(user);
        assertFalse(a.canHandle(null));
        assertFalse(a.canHandle(creds));
        assertTrue(a.canHandle(simpleEmpty));
        assertTrue(a.canHandle(simpleNull));


        a = new SimpleCredentialsAuthentication(new DummyUserImpl(null));
        assertFalse(a.canHandle(null));
        assertFalse(a.canHandle(creds));
        assertFalse(a.canHandle(simpleEmpty));

        a = new SimpleCredentialsAuthentication(new DummyUserImpl(creds));
        assertFalse(a.canHandle(null));
        assertFalse(a.canHandle(creds));
        assertFalse(a.canHandle(simpleEmpty));

        a = new SimpleCredentialsAuthentication(new DummyUserImpl(simpleAA));
        assertFalse(a.canHandle(null));
        assertFalse(a.canHandle(creds));
        assertTrue(a.canHandle(simpleEmpty));
        assertTrue(a.canHandle(simpleNull));
        assertTrue(a.canHandle(simpleAB));
    }

    public void testAuthenticate() throws RepositoryException {
        Authentication a = new SimpleCredentialsAuthentication(user);
        assertFalse(a.authenticate(simpleEmpty));
        assertFalse(a.authenticate(simpleNull));
        assertFalse(a.authenticate(simpleAA));

        a = new SimpleCredentialsAuthentication(new DummyUserImpl(null));
        assertFalse(a.authenticate(simpleEmpty));
        assertFalse(a.authenticate(simpleAA));

        a = new SimpleCredentialsAuthentication(new DummyUserImpl(creds));
        assertFalse(a.authenticate(simpleEmpty));
        assertFalse(a.authenticate(simpleAA));

        a = new SimpleCredentialsAuthentication(new DummyUserImpl(simpleAA));
        assertFalse(a.authenticate(simpleEmpty));
        assertFalse(a.authenticate(simpleBB));
        assertFalse(a.authenticate(simpleAB));
        assertTrue(a.authenticate(simpleAA));
    }

    //--------------------------------------------------------------------------
    /**
     * Internal class used for tests.
     */
    private class DummyUserImpl implements User {

        private final Credentials creds;

        private DummyUserImpl(Credentials creds) {
            this.creds = creds;
        }

        public boolean isAdmin() {
            return false;
        }

        public boolean isSystemUser() {
            return false;
        }

        public Credentials getCredentials() throws RepositoryException {
            return creds;
        }

        public Impersonation getImpersonation() throws RepositoryException {
            return null;
        }

        public void changePassword(String password) throws RepositoryException {
        }

        public void changePassword(String password, String oldPassword) throws RepositoryException {
        }

        public void disable(String reason) throws RepositoryException {
        }

        public boolean isDisabled() throws RepositoryException {
            return false;
        }

        public String getDisabledReason() throws RepositoryException {
            return null;
        }

        public String getID() throws RepositoryException {
            return null;
        }

        public boolean isGroup() {
            return false;
        }

        public Principal getPrincipal() throws RepositoryException {
            return null;
        }

        public Iterator<Group> declaredMemberOf() throws RepositoryException {
            return null;
        }

        public Iterator<Group> memberOf() throws RepositoryException {
            return null;
        }

        public void remove() throws RepositoryException {
        }

        public Iterator<String> getPropertyNames() throws RepositoryException {
            return null;
        }

        public Iterator<String> getPropertyNames(String relPath) throws RepositoryException {
            return null;
        }

        public boolean hasProperty(String name) throws RepositoryException {
            return false;
        }

        public void setProperty(String name, Value value) throws RepositoryException {
        }

        public void setProperty(String name, Value[] value) throws RepositoryException {
        }

        public Value[] getProperty(String name) throws RepositoryException {
            return new Value[0];
        }

        public boolean removeProperty(String name) throws RepositoryException {
            return false;
        }

        public String getPath() {
            return null;
        }
    }
}

