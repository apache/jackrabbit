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
package org.apache.jackrabbit.core.security.authentication.token;

import java.util.UUID;
import javax.jcr.Credentials;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.api.security.authentication.token.TokenCredentials;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * <code>TokenBasedAuthenticationOakTest</code>...
 */
public class TokenBasedAuthenticationTest extends AbstractJCRTest {

    private SessionImpl adminSession;
    private User testUser;

    private String token;
    private Node tokenNode;
    private TokenCredentials tokenCreds;

    private String expiredToken;
    private Node expiredNode;
    private TokenCredentials expiredCreds;


    private TokenBasedAuthentication nullTokenAuth;
    private TokenBasedAuthentication validTokenAuth;

    private Credentials simpleCreds = new SimpleCredentials("uid", "pw".toCharArray());
    private Credentials creds = new Credentials() {};

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        adminSession = (SessionImpl) getHelper().getSuperuserSession("security");
        testUser = adminSession.getUserManager().createUser(UUID.randomUUID().toString(), "pw");
        adminSession.save();

        SimpleCredentials sc = new SimpleCredentials(testUser.getID(), "pw".toCharArray());
        sc.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE, "");
        sc.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE+".any", "correct");
        sc.setAttribute("informative", "value");

        TokenProvider tp = new TokenProvider(adminSession, TokenBasedAuthentication.TOKEN_EXPIRATION);
        TokenInfo ti = tp.createToken(testUser, sc);
        tokenCreds = ti.getCredentials();
        token = tokenCreds.getToken();
        tokenNode = TokenProvider.getTokenNode(token, adminSession);

        long ttl = 1; // 1ms expiration
        tp = new TokenProvider(adminSession, ttl);
        TokenInfo expired = tp.createToken(testUser, sc);
        expiredCreds = expired.getCredentials();
        expiredToken = expiredCreds.getToken();
        long tokenWillExpireAfter = System.currentTimeMillis() + ttl;
        expiredNode = TokenProvider.getTokenNode(expiredToken, adminSession);

        nullTokenAuth = new TokenBasedAuthentication(null, -1, adminSession);
        validTokenAuth = new TokenBasedAuthentication(token, 7200, adminSession);

        while (System.currentTimeMillis() <= tokenWillExpireAfter) {
            // wait until the token is actually expired
        }
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            testUser.remove();
            adminSession.save();
            adminSession.logout();
        } finally {
            super.tearDown();
        }
    }

    private TokenBasedAuthentication createAuthenticationForExpiredToken() throws RepositoryException, LockException, ConstraintViolationException, VersionException {
        return new TokenBasedAuthentication(expiredToken, TokenBasedAuthentication.TOKEN_EXPIRATION, adminSession);
    }

    private TokenBasedAuthentication createAuthentication() throws RepositoryException {
        return new TokenBasedAuthentication(token, TokenBasedAuthentication.TOKEN_EXPIRATION, adminSession);
    }

    public void testCanHandle() throws RepositoryException {
        assertTrue(validTokenAuth.canHandle(tokenCreds));
        assertFalse(nullTokenAuth.canHandle(tokenCreds));

        assertFalse(validTokenAuth.canHandle(simpleCreds));
        assertFalse(nullTokenAuth.canHandle(simpleCreds));

        assertFalse(validTokenAuth.canHandle(creds));
        assertFalse(nullTokenAuth.canHandle(creds));
    }

    public void testCanHandleExpiredToken() throws RepositoryException {
        TokenBasedAuthentication expiredToken = createAuthenticationForExpiredToken();
        assertTrue(expiredToken.canHandle(expiredCreds));
    }

    public void testExpiry() throws RepositoryException {
        TokenBasedAuthentication expiredToken = createAuthenticationForExpiredToken();
        assertFalse(expiredToken.authenticate(expiredCreds));
    }

    public void testRemoval() throws RepositoryException {
        String identifier = expiredNode.getIdentifier();

        TokenBasedAuthentication expiredToken = createAuthenticationForExpiredToken();
        assertFalse(expiredToken.authenticate(expiredCreds));

        try {
            superuser.getNodeByIdentifier(identifier);
            fail("expired token node should be removed.");
        } catch (ItemNotFoundException e) {
            // success
        }
    }

    public void testInvalidCredentials() throws RepositoryException {
        try {
            validTokenAuth.authenticate(creds);
            fail("RepositoryException expected");
        } catch (RepositoryException e) {
            // success
        }

        try {
            assertFalse(validTokenAuth.authenticate(simpleCreds));
            fail("RepositoryException expected");            
        } catch (RepositoryException e) {
            // success
        }
    }

    public void testAttributes() throws RepositoryException {
        TokenBasedAuthentication auth = createAuthentication();
        assertFalse(auth.authenticate(new TokenCredentials(token)));

        TokenCredentials tc = new TokenCredentials(token);
        tc.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE +".any", "wrong");
        assertFalse(auth.authenticate(tc));

        tc = new TokenCredentials(token);
        tc.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE +".any", "correct");
        assertTrue(auth.authenticate(tokenCreds));
    }

    public void testUpdateAttributes() throws RepositoryException {
        // token credentials must be updated to contain the additional attribute
        // present on the token node.
        TokenBasedAuthentication auth = createAuthentication();

        TokenCredentials tc = new TokenCredentials(token);
        tc.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE +".any", "correct");

        assertTrue(auth.authenticate(tc));
        assertEquals("value", tc.getAttribute("informative"));

        // additional informative property present on credentials upon subsequent
        // authentication -> the node must not be updated
        auth = createAuthentication();
        tc.setAttribute("informative2", "value2");
        assertTrue(auth.authenticate(tc));
        assertFalse(tokenNode.hasProperty("informative2"));

        // modified informative property present on credentials upon subsequent
        // authentication -> the node must not be updated
        auth = createAuthentication();
        tc.setAttribute("informative", "otherValue");
        assertTrue(auth.authenticate(tc));
        assertTrue(tokenNode.hasProperty("informative"));
        assertEquals("value", tokenNode.getProperty("informative").getString());

        // additional mandatory property on the credentials upon subsequent
        // authentication -> must be ignored
        auth = createAuthentication();
        tc.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE +".toIgnore", "ignore");
        assertTrue(auth.authenticate(tokenCreds));
        assertFalse(tokenNode.hasProperty(TokenBasedAuthentication.TOKEN_ATTRIBUTE +".toIgnore"));
    }

    public void testIsTokenBasedLogin() {
        assertFalse(TokenBasedAuthentication.isTokenBasedLogin(simpleCreds));
        assertFalse(TokenBasedAuthentication.isTokenBasedLogin(creds));

        assertTrue(TokenBasedAuthentication.isTokenBasedLogin(tokenCreds));
    }

    public void testIsMandatoryAttribute() {
        assertFalse(TokenBasedAuthentication.isMandatoryAttribute("noMatchRequired"));

        assertTrue(TokenBasedAuthentication.isMandatoryAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE + ".exp"));
        assertTrue(TokenBasedAuthentication.isMandatoryAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE + ".custom"));
        assertTrue(TokenBasedAuthentication.isMandatoryAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE + "_custom"));
        assertTrue(TokenBasedAuthentication.isMandatoryAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE + "custom"));
    }

    public void testDoCreateToken() {
        assertFalse(TokenBasedAuthentication.doCreateToken(creds));
        assertFalse(TokenBasedAuthentication.doCreateToken(simpleCreds));
        assertFalse(TokenBasedAuthentication.doCreateToken(tokenCreds));

        SimpleCredentials sc = new SimpleCredentials("uid", "pw".toCharArray());
        sc.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE, null);

        assertFalse(TokenBasedAuthentication.doCreateToken(sc));

        sc.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE, "somevalue");
        assertFalse(TokenBasedAuthentication.doCreateToken(sc));

        sc.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE, "");
        assertTrue(TokenBasedAuthentication.doCreateToken(sc));
    }
}