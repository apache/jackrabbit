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
package org.apache.jackrabbit.api.security.user;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.TestPrincipal;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import java.security.Principal;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.io.UnsupportedEncodingException;

/**
 * <code>AbstractUserTest</code>...
 */
public abstract class AbstractUserTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(AbstractUserTest.class);

    protected UserManager userMgr;

    protected void setUp() throws Exception {
        super.setUp();

        userMgr = getUserManager(superuser);
    }

    protected static UserManager getUserManager(Session session) throws RepositoryException, NotExecutableException {
        if (!(session instanceof JackrabbitSession)) {
            throw new NotExecutableException();
        }
        try {
            return ((JackrabbitSession) session).getUserManager();
        } catch (UnsupportedRepositoryOperationException e) {
            throw new NotExecutableException(e.getMessage());
        } catch (UnsupportedOperationException e) {
            throw new NotExecutableException(e.getMessage());
        }
    }

    protected static Subject buildSubject(Principal p) {
        return new Subject(true, Collections.singleton(p), Collections.EMPTY_SET, Collections.EMPTY_SET);
    }

    protected Principal getTestPrincipal() throws RepositoryException {
        String pn = "any_principal" + UUID.randomUUID();
        Principal p = new TestPrincipal(pn);
        return p;
    }

    protected String buildPassword(String uid, boolean createDigest) throws IllegalArgumentException {
        if (createDigest) {
            try {
                StringBuffer password = new StringBuffer();
                password.append("{").append(SecurityConstants.DEFAULT_DIGEST).append("}");
                password.append(Text.digest(SecurityConstants.DEFAULT_DIGEST, uid.getBytes("UTF-8")));
                return password.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalArgumentException(e.toString());
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(e.toString());
            }
        } else {
            return uid;
        }
    }

    protected String buildPassword(Principal p) {
        return buildPassword(p.getName(), false);
    }

    protected Credentials buildCredentials(String uID, String pw) {
        return new SimpleCredentials(uID, pw.toCharArray());
    }

    protected static Set getPrincipalSetFromSession(Session session) throws NotExecutableException {
        if (session instanceof SessionImpl) {
            return ((SessionImpl) session).getSubject().getPrincipals();
        } else {
            // TODO add fallback
            throw new NotExecutableException();
        }
    }

    protected User getTestUser(Session session) throws NotExecutableException, RepositoryException {
        Set principals = getPrincipalSetFromSession(session);
        for (Iterator it = principals.iterator(); it.hasNext();) {
            try {
                Authorizable auth = userMgr.getAuthorizable((Principal) it.next());
                if (auth != null && !auth.isGroup()) {
                    return (User) auth;
                }
            } catch (RepositoryException e) {
                // ignore
            }
        }
        // should never happen. An Session should always have a corresponding User.
        throw new RepositoryException("Unable to retrieve a User.");
    }

    protected Group getTestGroup(Session session) throws NotExecutableException, RepositoryException {
        Set principals = getPrincipalSetFromSession(session);
        for (Iterator it = principals.iterator(); it.hasNext();) {
            try {
                Authorizable auth = userMgr.getAuthorizable((Principal) it.next());
                if (auth != null && auth.isGroup()) {
                    return (Group) auth;
                }
            } catch (RepositoryException e) {
                // ignore
            }
        }
        // may happen -> don't throw RepositoryException
        throw new NotExecutableException("No Group found.");
    }
}