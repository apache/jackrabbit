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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.jcr.AccessDeniedException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.api.security.authentication.token.TokenCredentials;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.ProtectedItemModifier;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.user.PasswordUtility;
import org.apache.jackrabbit.core.security.user.UserImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backport of the TokenProvider implementation present with OAK adjusted to
 * match some subtle differences in jackrabbit token login.
 */
public class TokenProvider extends ProtectedItemModifier {

    private static final Logger log = LoggerFactory.getLogger(TokenProvider.class);

    private static final String TOKEN_ATTRIBUTE = ".token";
    private static final String TOKEN_ATTRIBUTE_EXPIRY = "rep:token.exp";
    private static final String TOKEN_ATTRIBUTE_KEY = "rep:token.key";
    private static final String TOKENS_NODE_NAME = ".tokens";
    private static final String TOKEN_NT_NAME = "rep:Token";
    private static final Name TOKENS_NT_NAME = NameConstants.NT_UNSTRUCTURED;

    private static final char DELIM = '_';

    private static final Set<String> RESERVED_ATTRIBUTES = new HashSet(3);
    static {
        RESERVED_ATTRIBUTES.add(TOKEN_ATTRIBUTE);
        RESERVED_ATTRIBUTES.add(TOKEN_ATTRIBUTE_EXPIRY);
        RESERVED_ATTRIBUTES.add(TOKEN_ATTRIBUTE_KEY);
    }

    private static final Collection<String> RESERVED_PREFIXES = Collections.unmodifiableList(Arrays.asList(
            NamespaceRegistry.PREFIX_XML,
            NamespaceRegistry.PREFIX_JCR,
            NamespaceRegistry.PREFIX_NT,
            NamespaceRegistry.PREFIX_MIX,
            Name.NS_XMLNS_PREFIX,
            Name.NS_REP_PREFIX,
            Name.NS_SV_PREFIX
    ));

    private final SessionImpl session;
    private final UserManager userManager;
    private final long tokenExpiration;

    TokenProvider(SessionImpl session, long tokenExpiration) throws RepositoryException {
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
        TokenInfo tokenInfo = null;
        if (sc != null && user != null && user.getID().equalsIgnoreCase(sc.getUserID())) {
            String[] attrNames = sc.getAttributeNames();
            Map<String, String> attributes = new HashMap<String, String>(attrNames.length);
            for (String attrName : sc.getAttributeNames()) {
                attributes.put(attrName, sc.getAttribute(attrName).toString());
            }
            tokenInfo = createToken(user, attributes);
            if (tokenInfo != null) {
                // also set the new token to the simple credentials.
                sc.setAttribute(TOKEN_ATTRIBUTE, tokenInfo.getToken());
            }
        }

        return tokenInfo;
    }

    /**
     * Create a separate token node underneath a dedicated token store within
     * the user home node. That token node contains the hashed token, the
     * expiration time and additional mandatory attributes that will be verified
     * during login.
     *
     * @param userId     The identifier of the user for which a new token should
     *                   be created.
     * @param attributes The attributes associated with the new token.
     * @return A new {@code TokenInfo} or {@code null} if the token could not
     *         be created.
     */
    private TokenInfo createToken(User user, Map<String, ?> attributes) throws RepositoryException {
        String error = "Failed to create login token. ";
        NodeImpl tokenParent = getTokenParent(user);
        if (tokenParent != null) {
            try {
                ValueFactory vf = session.getValueFactory();
                long creationTime = new Date().getTime();
                Calendar creation = GregorianCalendar.getInstance();
                creation.setTimeInMillis(creationTime);

                Name tokenName = session.getQName(Text.replace(ISO8601.format(creation), ":", "."));
                NodeImpl tokenNode = super.addNode(tokenParent, tokenName, session.getQName(TOKEN_NT_NAME), NodeId.randomId());

                String key = generateKey(8);
                String token = new StringBuilder(tokenNode.getId().toString()).append(DELIM).append(key).toString();

                String keyHash = PasswordUtility.buildPasswordHash(getKeyValue(key, user.getID()));
                setProperty(tokenNode, session.getQName(TOKEN_ATTRIBUTE_KEY), vf.createValue(keyHash));
                setProperty(tokenNode, session.getQName(TOKEN_ATTRIBUTE_EXPIRY), createExpirationValue(creationTime, session));

                for (String name : attributes.keySet()) {
                    if (!RESERVED_ATTRIBUTES.contains(name)) {
                        String attr = attributes.get(name).toString();
                        setProperty(tokenNode, session.getQName(name), vf.createValue(attr));
                    }
                }
                session.save();
                return new TokenInfoImpl(tokenNode, token, user.getID());
            } catch (NoSuchAlgorithmException e) {
                // error while generating login token
                log.error(error, e);
            } catch (AccessDeniedException e) {
                log.warn(error, e);
            }
        } else {
            log.warn("Unable to get/create token store for user {}", user.getID());
        }
        return null;
    }

    private Value createExpirationValue(long creationTime, Session session) throws RepositoryException {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(createExpirationTime(creationTime, tokenExpiration));
        return session.getValueFactory().createValue(cal);
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
            return new TokenInfoImpl(tokenNode, token, userId);
        }
    }

    static Node getTokenNode(String token, Session session) throws RepositoryException {
        int pos = token.indexOf(DELIM);
        String id = (pos == -1) ? token : token.substring(0, pos);
        return session.getNodeByIdentifier(id);
    }

    static String getUserId(NodeImpl tokenNode, UserManager userManager) throws RepositoryException {
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
     * Returns {@code false} if the specified attribute name doesn't have
     * a 'jcr' or 'rep' namespace prefix; {@code true} otherwise. This is
     * a lazy evaluation in order to avoid testing the defining node type of
     * the associated jcr property.
     *
     * @param attributeName The attribute name.
     * @return {@code true} if the specified property name doesn't seem
     *         to represent repository internal information.
     */
    static  boolean isInfoAttribute(String attributeName) {
        String prefix = Text.getNamespacePrefix(attributeName);
        return !RESERVED_PREFIXES.contains(prefix);
    }

    private static long createExpirationTime(long creationTime, long tokenExpiration) {
        return creationTime + tokenExpiration;
    }

    private static long getExpirationTime(NodeImpl tokenNode, long defaultValue) throws RepositoryException {
        if (tokenNode.hasProperty(TOKEN_ATTRIBUTE_EXPIRY)) {
            return tokenNode.getProperty(TOKEN_ATTRIBUTE_EXPIRY).getLong();
        } else {
            return defaultValue;
        }
    }

    private static String generateKey(int size) {
        SecureRandom random = new SecureRandom();
        byte key[] = new byte[size];
        random.nextBytes(key);

        StringBuilder res = new StringBuilder(key.length * 2);
        for (byte b : key) {
            res.append(Text.hexTable[(b >> 4) & 15]);
            res.append(Text.hexTable[b & 15]);
        }
        return res.toString();
    }

    private static String getKeyValue(String key, String userId) {
        return key + userId;
    }

    private static boolean isValidTokenTree(NodeImpl tokenNode) throws RepositoryException {
        if (tokenNode == null) {
            return false;
        } else {
            return TOKENS_NODE_NAME.equals(tokenNode.getParent().getName()) &&
                    TOKEN_NT_NAME.equals(tokenNode.getPrimaryNodeType().getName());
        }
    }

    private NodeImpl getTokenParent(User user) throws RepositoryException {
        NodeImpl tokenParent = null;
        String parentPath = null;
        try {
            if (user != null) {
                Principal pr = user.getPrincipal();
                if (pr instanceof ItemBasedPrincipal) {
                    String userPath = ((ItemBasedPrincipal) pr).getPath();
                    NodeImpl userNode = (NodeImpl) session.getNode(userPath);
                    if (userNode.hasNode(TOKENS_NODE_NAME)) {
                        tokenParent = (NodeImpl) userNode.getNode(TOKENS_NODE_NAME);
                    } else {
                        tokenParent = userNode.addNode(session.getQName(TOKENS_NODE_NAME), TOKENS_NT_NAME, NodeId.randomId());
                        parentPath = userPath + '/' + TOKENS_NODE_NAME;
                        session.save();
                    }
                }
            } else {
                log.debug("Cannot create login token: No user specified. (null)");
            }
        } catch (RepositoryException e) {
            // conflict while creating token store for this user -> refresh and
            // try to get the tree from the updated root.
            log.debug("Conflict while creating token store -> retrying", e);
            session.refresh(false);
            if (parentPath != null && session.nodeExists(parentPath)) {
                tokenParent = (NodeImpl) session.getNode(parentPath);
            }
        }
        return tokenParent;
    }

    private class TokenInfoImpl implements TokenInfo {

        private final String token;
        private final String tokenPath;
        private final String userId;

        private final long expirationTime;
        private final String key;

        private final Map<String, String> mandatoryAttributes;
        private final Map<String, String> publicAttributes;


        private TokenInfoImpl(NodeImpl tokenNode, String token, String userId) throws RepositoryException {
            this.token = token;
            this.tokenPath = tokenNode.getPath();
            this.userId = userId;

            expirationTime = getExpirationTime(tokenNode, Long.MIN_VALUE);
            key = tokenNode.getProperty(TOKEN_ATTRIBUTE_KEY).getString();

            mandatoryAttributes = new HashMap<String, String>();
            publicAttributes = new HashMap<String, String>();
            PropertyIterator pit = tokenNode.getProperties();
            while (pit.hasNext()) {
                Property property = pit.nextProperty();
                String name = property.getName();
                String value = property.getString();
                if (RESERVED_ATTRIBUTES.contains(name)) {
                    continue;
                }
                if (isMandatoryAttribute(name)) {
                    mandatoryAttributes.put(name, value);
                } else if (isInfoAttribute(name)) {
                    // info attribute
                    publicAttributes.put(name, value);
                } // else: jcr specific property
            }
        }

        public String getToken() {
            return token;
        }

        public boolean isExpired(long loginTime) {
            return expirationTime < loginTime;
        }

        public boolean resetExpiration(long loginTime) throws RepositoryException {
            if (isExpired(loginTime)) {
                log.debug("Attempt to reset an expired token.");
                return false;
            }

            Session s = null;
            try {
                if (expirationTime - loginTime <= tokenExpiration / 2) {
                    s = session.createSession(session.getWorkspace().getName());
                    setProperty((NodeImpl) s.getNode(tokenPath), session.getQName(TOKEN_ATTRIBUTE_EXPIRY), createExpirationValue(loginTime, session));
                    s.save();
                    log.debug("Successfully reset token expiration time.");
                    return true;
                }
            } catch (RepositoryException e) {
                log.warn("Error while resetting token expiration", e);
            } finally {
                if (s != null) {
                    s.logout();
                }
            }
            return false;
        }

        public boolean matches(TokenCredentials tokenCredentials) {
            String tk = tokenCredentials.getToken();
            int pos = tk.lastIndexOf(DELIM);
            if (pos > -1) {
                tk = tk.substring(pos + 1);
            }
            if (key == null || !PasswordUtility.isSame(key, getKeyValue(tk, userId))) {
                return false;
            }

            for (String name : mandatoryAttributes.keySet()) {
                String expectedValue = mandatoryAttributes.get(name);
                if (!expectedValue.equals(tokenCredentials.getAttribute(name))) {
                    return false;
                }
            }

            // update set of informative attributes on the credentials
            // based on the properties present on the token node.
            Collection<String> attrNames = Arrays.asList(tokenCredentials.getAttributeNames());
            for (String name : publicAttributes.keySet()) {
                if (!attrNames.contains(name)) {
                    tokenCredentials.setAttribute(name, publicAttributes.get(name).toString());

                }
            }
            return true;
        }

        public boolean remove() {
            Session s = null;
            try {
                s = session.createSession(session.getWorkspace().getName());
                Node node = s.getNode(tokenPath);
                node.remove();
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

        public TokenCredentials getCredentials() {
            TokenCredentials tc = new TokenCredentials(token);
            for (String name : mandatoryAttributes.keySet()) {
                tc.setAttribute(name, mandatoryAttributes.get(name));
            }
            for (String name : publicAttributes.keySet()) {
                tc.setAttribute(name, publicAttributes.get(name));
            }
            return tc;
        }

    }
}
