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

import org.apache.jackrabbit.api.security.authentication.token.TokenCredentials;
import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.Credentials;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import java.util.Calendar;
import java.util.Date;

/**
 * <code>TokenBasedAuthenticationTest</code>...
 */
public class TokenBasedAuthenticationTest extends AbstractJCRTest {

    Node tokenNode;

    TokenBasedAuthentication nullTokenAuth;
    TokenBasedAuthentication validTokenAuth;

    TokenCredentials tokenCreds;
    Credentials simpleCreds = new SimpleCredentials("uid", "pw".toCharArray());
    Credentials creds = new Credentials() {};

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        tokenNode = testRootNode.addNode(nodeName1, "nt:unstructured");
        tokenNode.setProperty(TokenBasedAuthentication.TOKEN_ATTRIBUTE +".exp", new Date().getTime()+TokenBasedAuthentication.TOKEN_EXPIRATION);
        superuser.save();

        String token = tokenNode.getIdentifier();

        nullTokenAuth = new TokenBasedAuthentication(null, -1, superuser);
        validTokenAuth = new TokenBasedAuthentication(token, 7200, superuser);

        tokenCreds = new TokenCredentials(token);
    }

    private TokenBasedAuthentication expiredToken() throws RepositoryException, LockException, ConstraintViolationException, VersionException {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(new Date().getTime()-100);
        tokenNode.setProperty(".token.exp", cal);
        superuser.save();
        return new TokenBasedAuthentication(tokenNode.getIdentifier(), TokenBasedAuthentication.TOKEN_EXPIRATION, superuser);
    }

    public void testCanHandle() throws RepositoryException {
        assertTrue(validTokenAuth.canHandle(tokenCreds));
        assertFalse(nullTokenAuth.canHandle(tokenCreds));

        assertFalse(validTokenAuth.canHandle(simpleCreds));
        assertFalse(nullTokenAuth.canHandle(simpleCreds));

        assertFalse(validTokenAuth.canHandle(creds));
        assertFalse(nullTokenAuth.canHandle(creds));

        TokenBasedAuthentication expiredToken = expiredToken();
        assertTrue(expiredToken.canHandle(tokenCreds));
    }

    public void testExpiry() throws RepositoryException {
        assertTrue(validTokenAuth.authenticate(tokenCreds));

        TokenBasedAuthentication expiredToken = expiredToken();
        assertFalse(expiredToken.authenticate(tokenCreds));
    }

    public void testRemoval() throws RepositoryException {
        String identifier = tokenNode.getIdentifier();

        TokenBasedAuthentication expiredToken = expiredToken();
        assertFalse(expiredToken.authenticate(tokenCreds));

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
        tokenNode.setProperty(TokenBasedAuthentication.TOKEN_ATTRIBUTE +".any", "correct");
        superuser.save();
        TokenBasedAuthentication auth = new TokenBasedAuthentication(tokenNode.getIdentifier(), TokenBasedAuthentication.TOKEN_EXPIRATION, superuser);

        assertFalse(auth.authenticate(tokenCreds));

        tokenCreds.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE +".any", "wrong");
        assertFalse(auth.authenticate(tokenCreds));

        tokenCreds.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE +".any", "correct");
        assertTrue(auth.authenticate(tokenCreds));

        // add informative property
        tokenNode.setProperty("noMatchRequired", "abc");
        superuser.save();
        auth = new TokenBasedAuthentication(tokenNode.getIdentifier(), TokenBasedAuthentication.TOKEN_EXPIRATION, superuser);

        assertTrue(auth.authenticate(tokenCreds));
    }

    public void testUpdateAttributes() throws RepositoryException {
        tokenNode.setProperty(TokenBasedAuthentication.TOKEN_ATTRIBUTE +".any", "correct");
        tokenNode.setProperty("informative","value");
        superuser.save();

        // token credentials must be updated to contain the additional attribute
        // present on the token node.
        TokenBasedAuthentication auth = new TokenBasedAuthentication(tokenNode.getIdentifier(), TokenBasedAuthentication.TOKEN_EXPIRATION, superuser);
        tokenCreds.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE +".any", "correct");
        assertTrue(auth.authenticate(tokenCreds));               
        assertEquals("value", tokenCreds.getAttribute("informative"));

        // additional informative property present on credentials upon subsequent
        // authentication -> the node must not be updated
        auth = new TokenBasedAuthentication(tokenNode.getIdentifier(), TokenBasedAuthentication.TOKEN_EXPIRATION, superuser);
        tokenCreds.setAttribute("informative2", "value2");
        assertTrue(auth.authenticate(tokenCreds));
        assertFalse(tokenNode.hasProperty("informative2"));

        // modified informative property present on credentials upon subsequent
        // authentication -> the node must not be updated
        auth = new TokenBasedAuthentication(tokenNode.getIdentifier(), TokenBasedAuthentication.TOKEN_EXPIRATION, superuser);
        tokenCreds.setAttribute("informative", "otherValue");
        assertTrue(auth.authenticate(tokenCreds));
        assertTrue(tokenNode.hasProperty("informative"));
        assertEquals("value", tokenNode.getProperty("informative").getString());

        // additional mandatory property on the credentials upon subsequent
        // authentication -> must be ignored
        auth = new TokenBasedAuthentication(tokenNode.getIdentifier(), TokenBasedAuthentication.TOKEN_EXPIRATION, superuser);        
        tokenCreds.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE +".toIgnore", "ignore");
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