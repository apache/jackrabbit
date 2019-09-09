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

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.authentication.token.TokenCredentials;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.NodeIdFactory;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.user.UserImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backport of the TokenProvider implementation present with OAK adjusted to
 * match some subtle differences in jackrabbit token login.
 */
class CompatTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(CompatTokenProvider.class);

    private static final String TOKEN_ATTRIBUTE = ".token";
    private static final String TOKEN_ATTRIBUTE_EXPIRY = TOKEN_ATTRIBUTE + ".exp";
    private static final String TOKEN_ATTRIBUTE_KEY = TOKEN_ATTRIBUTE + ".key";
    private static final String TOKENS_NODE_NAME = ".tokens";
    private static final String TOKENS_NT_NAME = "nt:unstructured"; // TODO: configurable

    private static final char DELIM = '_';

    private final SessionImpl session;
    private final UserManager userManager;
    private final long tokenExpiration;

    CompatTokenProvider(SessionImpl session, long tokenExpiration) throws RepositoryException {
        this.session = session;
        this.userManager = session.getUserManager();
        this.tokenExpiration = tokenExpiration;
    }

    /**
     * Create a separate token node underneath a dedicated token store within
     * the user home node. That token node contains the hashed token, the
     * expiration time and additional mandatory attributes that will be verified
     * during login.
     *
     * @param user
     * @param sc The current simple credentials.
     * @return A new {@code TokenInfo} or {@code null} if the token could not
     *         be created.
     */
    public TokenInfo createToken(User user, SimpleCredentials sc) throws RepositoryException {
        String userPath = null;
        Principal pr = user.getPrincipal();
        if (pr instanceof ItemBasedPrincipal) {
            userPath = ((ItemBasedPrincipal) pr).getPath();
        }

        TokenCredentials tokenCredentials;
        if (userPath != null && session.nodeExists(userPath)) {
            Node userNode = session.getNode(userPath);
            Node tokenParent;
            if (!userNode.hasNode(TOKENS_NODE_NAME)) {
                userNode.addNode(TOKENS_NODE_NAME, TOKENS_NT_NAME);
                try {
                    session.save();
                } catch (RepositoryException e) {
                    // may happen when .tokens node is created concurrently
                    session.refresh(false);
                }
            }
            tokenParent = userNode.getNode(TOKENS_NODE_NAME);

            long creationTime = new Date().getTime();
            long expirationTime = creationTime + tokenExpiration;

            Calendar cal = GregorianCalendar.getInstance();
            cal.setTimeInMillis(creationTime);

            // generate key part of the login token
            String key = generateKey(8);

            // create the token node
            String tokenName = Text.replace(ISO8601.format(cal), ":", ".");
            Node tokenNode;
            // avoid usage of sequential nodeIDs
            if (System.getProperty(NodeIdFactory.SEQUENTIAL_NODE_ID) == null) {
                tokenNode = tokenParent.addNode(tokenName);
            } else {
                tokenNode = ((NodeImpl) tokenParent).addNodeWithUuid(tokenName, NodeId.randomId().toString());
            }

            StringBuilder sb = new StringBuilder(tokenNode.getIdentifier());
            sb.append(DELIM).append(key);

            String token = sb.toString();
            tokenCredentials = new TokenCredentials(token);
            sc.setAttribute(TOKEN_ATTRIBUTE, token);

            // add key property
            tokenNode.setProperty(TOKEN_ATTRIBUTE_KEY, getDigestedKey(key));

            // add expiration time property
            cal.setTimeInMillis(expirationTime);
            tokenNode.setProperty(TOKEN_ATTRIBUTE_EXPIRY, session.getValueFactory().createValue(cal));

            // add additional attributes passed in by the credentials.
            for (String name : sc.getAttributeNames()) {
                if (!TOKEN_ATTRIBUTE.equals(name)) {
                    String value = sc.getAttribute(name).toString();
                    tokenNode.setProperty(name, value);
                    tokenCredentials.setAttribute(name, value);
                }
            }
            session.save();
            return new CompatModeInfo(token, tokenNode);
        } else {
            throw new RepositoryException("Cannot create login token: No corresponding node for User " + user.getID() +" in workspace '" + session.getWorkspace().getName() + "'.");
        }
    }

    /**
     * Retrieves the token information associated with the specified login
     * token. If no accessible {@code Tree} exists for the given token or if
     * the token is not associated with a valid user this method returns {@code null}.
     *
     * @param token A valid login token.
     * @return The {@code TokenInfo} associated with the specified token or
     *         {@code null} of the corresponding information does not exist or is not
     *         associated with a valid user.
     */
    public TokenInfo getTokenInfo(String token) throws RepositoryException {
        if (token == null) {
            return null;
        }
        NodeImpl tokenNode = (NodeImpl) getTokenNode(token, session);
        String userId = getUserId(tokenNode, userManager);
        if (userId == null || !isValidTokenTree(tokenNode)) {
            return null;
        } else {
            return new CompatModeInfo(token);
        }
    }

    static Node getTokenNode(String token, Session session) throws RepositoryException {
        int pos = token.indexOf(DELIM);
        String id = (pos == -1) ? token : token.substring(0, pos);
        return session.getNodeByIdentifier(id);
    }

    public static String getUserId(TokenCredentials tokenCredentials, Session session) throws RepositoryException {
        if (!(session instanceof JackrabbitSession)) {
            throw new RepositoryException("JackrabbitSession expected");
        }
        NodeImpl n = (NodeImpl) getTokenNode(tokenCredentials.getToken(), session);
        return getUserId(n, ((JackrabbitSession) session).getUserManager());
    }

    private static String getUserId(NodeImpl tokenNode, UserManager userManager) throws RepositoryException {
        if (tokenNode != null) {
            final NodeImpl userNode = (NodeImpl) tokenNode.getParent().getParent();
            final String principalName = userNode.getProperty(UserImpl.P_PRINCIPAL_NAME).getString();
            if (userNode.isNodeType(UserImpl.NT_REP_USER)) {
                Authorizable a = userManager.getAuthorizable(new ItemBasedPrincipal() {
                    public String getPath() throws RepositoryException {
                        return userNode.getPath();
                    }

                    public String getName() {
                        return principalName;
                    }
                });
                if (a != null && !a.isGroup() && !((User)a).isDisabled()) {
                    return a.getID();
                }
            } else {
                throw new RepositoryException("Failed to calculate userId from token credentials");
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if the specified {@code attributeName}
     * starts with or equals {@link #TOKEN_ATTRIBUTE}.
     *
     * @param attributeName The attribute name.
     * @return {@code true} if the specified {@code attributeName}
     *         starts with or equals {@link #TOKEN_ATTRIBUTE}.
     */
    static boolean isMandatoryAttribute(String attributeName) {
        return attributeName != null && attributeName.startsWith(TOKEN_ATTRIBUTE);
    }

    /**
     * Returns <code>false</code> if the specified attribute name doesn't have
     * a 'jcr' or 'rep' namespace prefix; <code>true</code> otherwise. This is
     * a lazy evaluation in order to avoid testing the defining node type of
     * the associated jcr property.
     *
     * @param propertyName
     * @return <code>true</code> if the specified property name doesn't seem
     * to represent repository internal information.
     */
    private static boolean isInfoAttribute(String propertyName) {
        String prefix = Text.getNamespacePrefix(propertyName);
        return !Name.NS_JCR_PREFIX.equals(prefix) && !Name.NS_REP_PREFIX.equals(prefix);
    }

    private static boolean isValidTokenTree(NodeImpl tokenNode) throws RepositoryException {
        if (tokenNode == null) {
            return false;
        } else {
            return TOKENS_NODE_NAME.equals(tokenNode.getParent().getName());
        }
    }

    private static String generateKey(int size) {
        SecureRandom random = new SecureRandom();
        byte key[] = new byte[size];
        random.nextBytes(key);

        StringBuffer res = new StringBuffer(key.length * 2);
        for (byte b : key) {
            res.append(Text.hexTable[(b >> 4) & 15]);
            res.append(Text.hexTable[b & 15]);
        }
        return res.toString();
    }

    private static String getDigestedKey(TokenCredentials tc) throws RepositoryException {
        String tk = tc.getToken();
        int pos = tk.indexOf(DELIM);
        if (pos > -1) {
            return getDigestedKey(tk.substring(pos+1));
        }
        return null;
    }

    private static String getDigestedKey(String key) throws RepositoryException {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{").append(SecurityConstants.DEFAULT_DIGEST).append("}");
            sb.append(Text.digest(SecurityConstants.DEFAULT_DIGEST, key, "UTF-8"));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RepositoryException("Failed to generate login token.");
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryException("Failed to generate login token.");
        }
    }

    private final class CompatModeInfo implements TokenInfo {

        private final String token;

        private final Map<String, String> attributes;
        private final Map<String, String> info;
        private final long expiry;
        private final String key;

        private CompatModeInfo(String token) throws RepositoryException {
            this(token, getTokenNode(token, session));
        }

        private CompatModeInfo(String token, Node n) throws RepositoryException {
            this.token = token;
            long expTime = Long.MAX_VALUE;
            String keyV = null;
            if (token != null) {
                attributes = new HashMap<String, String>();
                info = new HashMap<String, String>();

                PropertyIterator it = n.getProperties();
                while (it.hasNext()) {
                    Property p = it.nextProperty();
                    String name = p.getName();
                    if (TOKEN_ATTRIBUTE_EXPIRY.equals(name)) {
                        expTime = p.getLong();
                    } else if (TOKEN_ATTRIBUTE_KEY.equals(name)) {
                        keyV = p.getString();
                    } else if (isMandatoryAttribute(name)) {
                        attributes.put(name, p.getString());
                    } else if (isInfoAttribute(name)) {
                        info.put(name, p.getString());
                    } // else: jcr property -> ignore
                }
            } else {
                attributes = Collections.emptyMap();
                info = Collections.emptyMap();
            }
            expiry = expTime;
            key = keyV;
        }

        public String getToken() {
            return token;
        }

        public boolean isExpired(long loginTime) {
            return expiry < loginTime;
        }

        public boolean remove() {
            Session s = null;
            try {
                s = ((SessionImpl) session).createSession(session.getWorkspace().getName());
                Node tokenNode = getTokenNode(token, s);

                tokenNode.remove();
                s.save();
                return true;
            } catch (RepositoryException e) {
                log.warn("Internal error while removing token node.", e);
            } finally {
                if (s != null) {
                    s.logout();
                }
            }
            return false;
        }

        public boolean matches(TokenCredentials tokenCredentials) throws RepositoryException {
            // test for matching key
            if (key != null && !key.equals(getDigestedKey(tokenCredentials))) {
                return false;
            }

            // check if all other required attributes match
            for (String name : attributes.keySet()) {
                if (!attributes.get(name).equals(tokenCredentials.getAttribute(name))) {
                    // no match -> login fails.
                    return false;
                }
            }

            // update set of informative attributes on the credentials
            // based on the properties present on the token node.
            Collection<String> attrNames = Arrays.asList(tokenCredentials.getAttributeNames());
            for (String key : info.keySet()) {
                if (!attrNames.contains(key)) {
                    tokenCredentials.setAttribute(key, info.get(key));
                }
            }

            return true;
        }

        public boolean resetExpiration(long loginTime) throws RepositoryException {
            Node tokenNode;
            Session s = null;
            try {
                // expiry...
                if (expiry - loginTime <= tokenExpiration/2) {
                    long expirationTime = loginTime + tokenExpiration;
                    Calendar cal = GregorianCalendar.getInstance();
                    cal.setTimeInMillis(expirationTime);

                    s = ((SessionImpl) session).createSession(session.getWorkspace().getName());
                    tokenNode = getTokenNode(token, s);
                    tokenNode.setProperty(TOKEN_ATTRIBUTE_EXPIRY, s.getValueFactory().createValue(cal));
                    s.save();
                    return true;
                }
            } catch (RepositoryException e) {
                log.warn("Failed to update expiry or informative attributes of token node.", e);
            } finally {
                if (s != null) {
                    s.logout();
                }
            }
            return false;
        }

        public TokenCredentials getCredentials() {
            TokenCredentials tc = new TokenCredentials(token);
            for (String name : attributes.keySet()) {
                tc.setAttribute(name, attributes.get(name));
            }
            for (String name : info.keySet()) {
                tc.setAttribute(name, info.get(name));
            }
            return tc;
        }
    }
}