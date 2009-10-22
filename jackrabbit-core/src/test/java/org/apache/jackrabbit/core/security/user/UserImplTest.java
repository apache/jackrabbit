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
package org.apache.jackrabbit.core.security.user;

import org.apache.jackrabbit.api.security.user.AbstractUserTest;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.security.authentication.CryptedSimpleCredentials;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.value.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;

/**
 * <code>UserImplTest</code>...
 */
public class UserImplTest extends AbstractUserTest {

    private static Logger log = LoggerFactory.getLogger(UserImplTest.class);

    private String uID;
    private Credentials creds;
    private Session uSession;
    private UserManager uMgr;

    protected void setUp() throws Exception {
        super.setUp();

        Principal p = getTestPrincipal();
        String pw = buildPassword(p);
        creds = new SimpleCredentials(p.getName(), pw.toCharArray());

        User u = userMgr.createUser(p.getName(), pw);
        save(superuser);

        uID = u.getID();
        uSession = getHelper().getRepository().login(creds);
        uMgr = getUserManager(uSession);
    }

    protected void tearDown() throws Exception {
        try {
            userMgr.getAuthorizable(uID).remove();
            save(superuser);
        } finally {
            uSession.logout();
        }
        super.tearDown();
    }

    public void testUserImplHasCryptedSimplCredentials() throws RepositoryException, NotExecutableException {
        User user = getTestUser(superuser);
        Credentials creds = user.getCredentials();
        assertNotNull(creds);

        assertTrue(creds instanceof CryptedSimpleCredentials);
        assertEquals(((CryptedSimpleCredentials) creds).getUserID(), user.getID());
    }

    public void testIsUser() throws RepositoryException {
        Authorizable auth = uMgr.getAuthorizable(uID);
        assertFalse(auth.isGroup());
    }

    public void testUserCanModifyItsOwnProperties() throws RepositoryException, NotExecutableException {
        User u = (User) uMgr.getAuthorizable(uID);
        if (u == null) {
            fail("User " +uID+ "hast not been removed and must be visible to the Session created with its credentials.");
        }

        if (!uSession.hasPermission(((UserImpl) u).getNode().getPath(), "set_property")) {
            throw new NotExecutableException("Users should be able to modify their properties -> Check repository config.");
        }

        // single valued properties
        u.setProperty("Email", new StringValue("tu@security.test"));
        save(uSession);

        assertNotNull(u.getProperty("Email"));
        assertEquals("tu@security.test", u.getProperty("Email")[0].getString());

        u.removeProperty("Email");
        save(uSession);

        assertNull(u.getProperty("Email"));

        // multivalued properties
        u.setProperty(propertyName1, new Value[] {uSession.getValueFactory().createValue("anyValue")});
        save(uSession);

        assertNotNull(u.getProperty(propertyName1));

        u.removeProperty(propertyName1);
        save(uSession);
        
        assertNull(u.getProperty(propertyName1));
    }

    public void testChangePassword() throws RepositoryException, NotExecutableException, NoSuchAlgorithmException, UnsupportedEncodingException {
        String oldPw = getHelper().getProperty("javax.jcr.tck.superuser.pwd");
        if (oldPw == null) {
            // missing property
            throw new NotExecutableException();
        }

        User user = getTestUser(superuser);
        try {
            user.changePassword("pw");
            save(superuser);

            SimpleCredentials creds = new SimpleCredentials(user.getID(), "pw".toCharArray());
            assertTrue(((CryptedSimpleCredentials) user.getCredentials()).matches(creds));
        } finally {
            user.changePassword(oldPw);
            save(superuser);
        }
    }
}
