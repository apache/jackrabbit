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
package org.apache.jackrabbit.core.integration;

import java.security.AccessControlException;

import javax.jcr.Credentials;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * Integration tests for the Session implementation in Jackrabbit core.
 */
public class SessionImplTest extends AbstractJCRTest {

    /**
     * <a href="https://issues.apache.org/jira/browse/JCR-1731">JCR-1731</a>:
     * Session.checkPermission("/", "add_node") throws PathNotFoundException
     * instead of AccessControlException
     */
    public void testCheckAddNodePermissionOnRoot() throws RepositoryException {
        Session session = getHelper().getReadOnlySession();
        try {
            session.checkPermission("/", "add_node");
        } catch (PathNotFoundException e) {
            fail("JCR-1731: Session.checkPermission(\"/\", \"add_node\")"
                    + " throws PathNotFoundException instead of"
                    + " AccessControlException");
        } catch (AccessControlException e) {
            // expected
        } finally {
            session.logout();
        }
    }

    /**
     * JCR-1932: Session.getAttributes( ) call always returns an empty array
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1932">JCR-1932</a>
     */
    public void testSessionAttributes() throws RepositoryException {
        SimpleCredentials credentials =
            new SimpleCredentials("admin", "admin".toCharArray());
        credentials.setAttribute("test", "attribute");
        Session session = getHelper().getRepository().login(credentials);
        try {
            String[] names = session.getAttributeNames();
            assertEquals(1, names.length);
            assertEquals("test", names[0]);
            assertEquals("attribute", session.getAttribute("test"));
        } finally {
            session.logout();
        }
    }

    /**
     * JCR-2595: SessionImpl.createSession uses same Subject/LoginContext
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-2595">JCR-2595</a>
     */
    public void testCreateSession() throws RepositoryException, NotExecutableException {
        if (!(superuser instanceof SessionImpl)) {
            throw new NotExecutableException();
        }

        String currentWsp = superuser.getWorkspace().getName();
        String otherWsp = null;
        for (String wsp : superuser.getWorkspace().getAccessibleWorkspaceNames()) {
            if (!wsp.equals(currentWsp)) {
                otherWsp = wsp;
                break;
            }
        }

        SessionImpl sImpl = (SessionImpl) superuser;
        Subject subject = sImpl.getSubject();

        Session s1 = sImpl.createSession(currentWsp);
        try {
            assertFalse(s1 == sImpl);
            assertFalse(subject == ((SessionImpl) s1).getSubject());
            assertEquals(subject, ((SessionImpl) s1).getSubject());
            assertEquals(currentWsp, s1.getWorkspace().getName());
        } finally {
            s1.logout();
            assertFalse(subject.getPrincipals().isEmpty());
            assertFalse(subject.getPublicCredentials().isEmpty());
        }


        Session s2 = sImpl.createSession(otherWsp);
        try {
            assertFalse(s2 == sImpl);
            assertFalse(subject == ((SessionImpl) s2).getSubject());
            assertEquals(subject, ((SessionImpl) s2).getSubject());
            assertEquals(otherWsp, s2.getWorkspace().getName());
        } finally {
            s2.logout();
            assertFalse(subject.getPrincipals().isEmpty());
            assertFalse(subject.getPublicCredentials().isEmpty());
        }

        Session s3 = sImpl.createSession(null);
        try {
            assertFalse(s3 == sImpl);
            assertFalse(subject == ((SessionImpl) s3).getSubject());
            assertEquals(subject, ((SessionImpl) s3).getSubject());
            assertEquals(((RepositoryImpl) sImpl.getRepository()).getConfig().getDefaultWorkspaceName(), s3.getWorkspace().getName());
        } finally {
            s3.logout();
            assertFalse(subject.getPrincipals().isEmpty());
            assertFalse(subject.getPublicCredentials().isEmpty());
        }
    }

    /**
     * JCR-2895 : SessionImpl#getSubject() should return an unmodifiable subject
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-2895">JCR-2895</a>
     */
    public void testGetSubject() {
        Subject subject = ((SessionImpl) superuser).getSubject();

        assertFalse(subject.getPublicCredentials().isEmpty());
        assertFalse(subject.getPublicCredentials(Credentials.class).isEmpty());
        assertFalse(subject.getPrincipals().isEmpty());

        assertTrue(subject.isReadOnly());
        try {
            subject.getPublicCredentials().add(new SimpleCredentials("test", new char[0]));
            fail("Subject expected to be readonly");
        } catch (IllegalStateException e) {
            // success
        }
        try {
            subject.getPrincipals().add(new PrincipalImpl("test"));
            fail("Subject expected to be readonly");
        } catch (IllegalStateException e) {
            // success
        }
    }

    /**
     * JCR-3014 Identifier paths for inexistent items throw exception
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-3014">JCR-3014</a>
     */
    public void testCheckNonExistingItem() throws Exception {
        String dummyPath = "[" + NodeId.randomId() + "]";
        assertFalse(superuser.itemExists(dummyPath));
        assertFalse(superuser.nodeExists(dummyPath));
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/JCR-3885">JCR-3885</a>
     */
    public void testSessionHasPermission() throws Exception {
        JackrabbitSession js = (JackrabbitSession) superuser;

        assertEquals(superuser.hasPermission("/", Session.ACTION_READ), js.hasPermission("/", new String[] {Session.ACTION_READ}));
        assertEquals(superuser.hasPermission("/", Session.ACTION_READ + "," + Session.ACTION_ADD_NODE) , js.hasPermission("/", Session.ACTION_READ, Session.ACTION_ADD_NODE));

        try {
            js.hasPermission("/", new String[0]);
            fail();
        } catch (IllegalArgumentException e) {
            // success
        }

        try {
            js.hasPermission("/", new String[] {""});
            fail();
        } catch (IllegalArgumentException e) {
            // success
        }

        // note: that's a bit unexpected
        assertEquals(superuser.hasPermission("/", ",,"), js.hasPermission("/", "", "", ""));
    }
}
