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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

public class CompatTokenProviderTest extends AbstractJCRTest {

    private User testuser;
    private String userId;

    private SessionImpl session;
    private CompatTokenProvider tokenProvider;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (superuser instanceof SessionImpl) {
            UserManager umgr = ((SessionImpl) superuser).getUserManager();
            if (!umgr.isAutoSave()) {
                umgr.autoSave(true);
            }
            String uid = "test";
            while (umgr.getAuthorizable(uid) != null) {
                uid += "_";
            }

            testuser = umgr.createUser(uid, uid);
            userId = testuser.getID();
        } else {
            throw new NotExecutableException();
        }

        if (superuser.nodeExists(((ItemBasedPrincipal) testuser.getPrincipal()).getPath())) {
            session = (SessionImpl) superuser;
        } else {
            session = (SessionImpl) getHelper().getSuperuserSession("security");
        }
        tokenProvider = new CompatTokenProvider((SessionImpl) session, TokenBasedAuthentication.TOKEN_EXPIRATION);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            testuser.remove();
            session.logout();
        } finally {
            super.tearDown();
        }
    }

    public void testCreateTokenFromCredentials() throws Exception {
        TokenInfo info = tokenProvider.createToken(testuser, new SimpleCredentials(userId, new char[0]));
        assertTokenInfo(info);
    }

    public void testCreateTokenIsCaseInsensitive() throws Exception {
        String upperCaseUserId = userId.toUpperCase();
        TokenInfo info = tokenProvider.createToken(testuser, new SimpleCredentials(upperCaseUserId, new char[0]));
        assertTokenInfo(info);
    }

    public void testTokenNode() throws Exception {
        Map<String, String> privateAttributes = new HashMap<String, String>();
        privateAttributes.put(".token_exp", "value");
        privateAttributes.put(".tokenTest", "value");
        privateAttributes.put(".token_something", "value");

        Map<String, String> publicAttributes = new HashMap<String, String>();
        publicAttributes.put("any", "value");
        publicAttributes.put("another", "value");

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.putAll(publicAttributes);
        attributes.putAll(privateAttributes);

        SimpleCredentials sc = new SimpleCredentials(userId, userId.toCharArray());
        for (String s : attributes.keySet()) {
            sc.setAttribute(s, attributes.get(s));
        }

        TokenInfo info = tokenProvider.createToken(testuser, sc);
        Node tokenNode = getTokenNode(info);
        Property prop = tokenNode.getProperty(".token.key");
        assertNotNull(prop);
        assertEquals(PropertyType.STRING, prop.getType());
        assertFalse(prop.getDefinition().isProtected());

        prop = tokenNode.getProperty(".token.exp");
        assertNotNull(prop);
        assertEquals(PropertyType.DATE, prop.getType());
        assertFalse(prop.getDefinition().isProtected());

        for (String key : privateAttributes.keySet()) {
            assertEquals(privateAttributes.get(key), tokenNode.getProperty(key).getString());
        }

        for (String key : publicAttributes.keySet()) {
            assertEquals(publicAttributes.get(key), tokenNode.getProperty(key).getString());
        }
    }

    public void testGetTokenInfoFromInvalidToken() throws Exception {
        List<String> invalid = new ArrayList<String>();
        invalid.add("/invalid");
        invalid.add(UUID.randomUUID().toString());

        try {
            for (String token : invalid) {
                TokenInfo info = tokenProvider.getTokenInfo(token);
                assertNull(info);
            }
        } catch (Exception e) {
            // success
        }
    }

    public void testGetTokenInfo() throws Exception {
        String token = tokenProvider.createToken(testuser, new SimpleCredentials(userId, userId.toCharArray())).getToken();
        TokenInfo info = tokenProvider.getTokenInfo(token);
        assertTokenInfo(info);
    }

    public void testIsExpired() throws Exception {
        TokenInfo info = tokenProvider.createToken(testuser, new SimpleCredentials(userId, userId.toCharArray()));

        long loginTime = waitForSystemTimeIncrement(System.currentTimeMillis());
        assertFalse(info.isExpired(loginTime));
        assertTrue(info.isExpired(loginTime + TokenBasedAuthentication.TOKEN_EXPIRATION));
    }

    public void testReset() throws Exception {
        TokenInfo info = tokenProvider.createToken(testuser, new SimpleCredentials(userId, userId.toCharArray()));
        long expTime = getTokenNode(info).getProperty(".token.exp").getLong();

        long loginTime = System.currentTimeMillis();
        assertFalse(info.resetExpiration(loginTime));
        assertTrue(info.resetExpiration(loginTime + TokenBasedAuthentication.TOKEN_EXPIRATION / 2));
        long expTime2 = getTokenNode(info).getProperty(".token.exp").getLong();
        assertFalse(expTime == expTime2);
    }

    //--------------------------------------------------------------------------
    private static void assertTokenInfo(TokenInfo info) {
        assertNotNull(info);
        assertNotNull(info.getToken());
        assertFalse(info.isExpired(new Date().getTime()));
    }

    private Node getTokenNode(TokenInfo info) throws RepositoryException {
        return CompatTokenProvider.getTokenNode(info.getToken(), session);
    }

    private static long waitForSystemTimeIncrement(long old){
        while (old == System.currentTimeMillis()) {
            // wait for system timer to move
        }
        return System.currentTimeMillis();
    }
}