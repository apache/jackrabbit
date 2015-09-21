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
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.authentication.CryptedSimpleCredentials;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.value.StringValue;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>UserImplTest</code>...
 */
public class UserImplTest extends AbstractUserTest {

    private String uID;
    private Credentials creds;
    private Session uSession;
    private UserManager uMgr;

    @Override
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

    @Override
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

    public void testCredentials() throws RepositoryException, NoSuchAlgorithmException, UnsupportedEncodingException {
        User u = (User) userMgr.getAuthorizable(uID);

        Credentials uc = u.getCredentials();
        assertTrue(uc instanceof CryptedSimpleCredentials);
        assertTrue(((CryptedSimpleCredentials) uc).matches((SimpleCredentials) creds));
    }

    public void testChangePassword() throws RepositoryException, NotExecutableException, NoSuchAlgorithmException, UnsupportedEncodingException {
        User u = (User) userMgr.getAuthorizable(uID);

        String sha1Hash = "{" +SecurityConstants.DEFAULT_DIGEST+ "}" + Text.digest(SecurityConstants.DEFAULT_DIGEST, "abc".getBytes());
        String md5Hash = "{md5}" + Text.digest("md5", "abc".getBytes());

        // valid passwords and the corresponding match
        Map<String,String> pwds = new HashMap<String, String>();
        // plain text passwords
        pwds.put("abc", "abc");
        pwds.put("{a}password", "{a}password");
        // passwords with hash-like char-sequence -> must still be hashed.
        pwds.put(sha1Hash, sha1Hash);
        pwds.put(md5Hash, md5Hash);
        pwds.put("{"+SecurityConstants.DEFAULT_DIGEST+"}any", "{"+SecurityConstants.DEFAULT_DIGEST+"}any");
        pwds.put("{"+SecurityConstants.DEFAULT_DIGEST+"}", "{"+SecurityConstants.DEFAULT_DIGEST+"}");

        for (String pw : pwds.keySet()) {
            u.changePassword(pw);

            String plain = pwds.get(pw);
            SimpleCredentials sc = new SimpleCredentials(u.getID(), plain.toCharArray());
            CryptedSimpleCredentials cc = (CryptedSimpleCredentials) u.getCredentials();

            assertTrue(cc.matches(sc));
        }

        // valid passwords, non-matching plain text
        Map<String, String>noMatch = new HashMap<String, String>();
        noMatch.put("{"+SecurityConstants.DEFAULT_DIGEST+"}", "");
        noMatch.put("{"+SecurityConstants.DEFAULT_DIGEST+"}any", "any");
        noMatch.put(sha1Hash, "abc");
        noMatch.put(md5Hash, "abc");

        for (String pw : noMatch.keySet()) {
            u.changePassword(pw);

            String plain = noMatch.get(pw);
            SimpleCredentials sc = new SimpleCredentials(u.getID(), plain.toCharArray());
            CryptedSimpleCredentials cc = (CryptedSimpleCredentials) u.getCredentials();

            assertFalse(pw, cc.matches(sc));
        }
    }

    public void testChangePasswordNull() throws RepositoryException {
        User u = (User) userMgr.getAuthorizable(uID);

        // invalid 'null' pw string
        try {
            u.changePassword(null);
            fail("invalid pw null");
        } catch (Exception e) {
            // success
        }
    }

    public void testLoginWithCryptedCredentials() throws RepositoryException {
        User u = (User) uMgr.getAuthorizable(uID);

        Credentials creds = u.getCredentials();
        assertTrue(creds instanceof CryptedSimpleCredentials);

        try {
            Session s = getHelper().getRepository().login(u.getCredentials());
            s.logout();
            fail("Login using CryptedSimpleCredentials must fail.");
        } catch (LoginException e) {
            // success
        }
    }
}
