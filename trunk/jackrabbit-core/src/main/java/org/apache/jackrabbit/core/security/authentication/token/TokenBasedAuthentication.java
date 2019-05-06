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

import java.util.Date;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.authentication.token.TokenCredentials;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authentication.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication implementation that compares the tokens stored with a
 * given user node to the token present in the SimpleCredentials attributes.
 * Authentication succeeds if the login token refers to a non-expired
 * token node and if all other credential attributes are equal to the
 * corresponding properties.
 */
public class TokenBasedAuthentication implements Authentication {

    private static final Logger log = LoggerFactory.getLogger(TokenBasedAuthentication.class);

    /**
     * Default expiration time for login tokens is 2 hours.
     */
    public static final long TOKEN_EXPIRATION = 2 * 3600 * 1000;

    /**
     * The name of the login token attribute.
     */
    public static final String TOKEN_ATTRIBUTE = ".token";

    /**
     * @deprecated This system parameter allows to enable backwards compatible
     * behavior of the {@code TokenBasedAuthentication}. Note that as of OAK 1.0
     * this flag will no be supported.
     */
    public static final String PARAM_COMPAT = "TokenCompatMode";

    private final TokenInfo tokenInfo;

    public TokenBasedAuthentication(String token, long tokenExpiration, Session session) throws RepositoryException {
        if (compatMode()) {
            this.tokenInfo = new CompatTokenProvider((SessionImpl) session, tokenExpiration).getTokenInfo(token);
        } else {
            this.tokenInfo = new TokenProvider((SessionImpl) session, tokenExpiration).getTokenInfo(token);
        }

    }

    /**
     * @see Authentication#canHandle(javax.jcr.Credentials)
     */
    public boolean canHandle(Credentials credentials) {
        return tokenInfo != null && isTokenBasedLogin(credentials);
    }

    /**
     * @see Authentication#authenticate(javax.jcr.Credentials)
     */
    public boolean authenticate(Credentials credentials) throws RepositoryException {
        if (!(credentials instanceof TokenCredentials)) {
            throw new RepositoryException("TokenCredentials expected. Cannot handle " + credentials.getClass().getName());
        }
        TokenCredentials tokenCredentials = (TokenCredentials) credentials;
        return validateCredentials(tokenCredentials);
    }

    private boolean validateCredentials(TokenCredentials tokenCredentials) throws RepositoryException {
        if (tokenInfo == null) {
            log.debug("No valid TokenInfo for token.");
            return false;
        }

        long loginTime = new Date().getTime();
        if (tokenInfo.isExpired(loginTime)) {
            // token is expired
            log.debug("Token is expired");
            tokenInfo.remove();
            return false;
        }

        if (tokenInfo.matches(tokenCredentials)) {
            tokenInfo.resetExpiration(loginTime);
            return true;
        }

        return false;
    }

    //--------------------------------------------------------------------------
    /**
     * Returns <code>true</code> if the given <code>credentials</code> object
     * is an instance of <code>TokenCredentials</code>.
     *
     * @param credentials
     * @return <code>true</code> if the given <code>credentials</code> object
     * is an instance of <code>TokenCredentials</code>; <code>false</code> otherwise.
     */
    public static boolean isTokenBasedLogin(Credentials credentials) {
        return credentials instanceof TokenCredentials;
    }

    /**
     * Returns <code>true</code> if the specified <code>attributeName</code>
     * starts with or equals {@link #TOKEN_ATTRIBUTE}.
     *
     * @param attributeName
     * @return <code>true</code> if the specified <code>attributeName</code>
     * starts with or equals {@link #TOKEN_ATTRIBUTE}.
     */
    public static boolean isMandatoryAttribute(String attributeName) {
        if (compatMode()) {
            return CompatTokenProvider.isMandatoryAttribute(attributeName);
        } else {
            return TokenProvider.isMandatoryAttribute(attributeName);
        }
    }

    /**
     * Returns <code>true</code> if the specified <code>credentials</code>
     * should be used to create a new login token.
     *
     * @param credentials
     * @return <code>true</code> if upon successful authentication a new
     * login token should be created; <code>false</code> otherwise.
     */
    public static boolean doCreateToken(Credentials credentials) {
        if (credentials instanceof SimpleCredentials) {
            Object attr = ((SimpleCredentials) credentials).getAttribute(TOKEN_ATTRIBUTE);
            return (attr != null && "".equals(attr.toString()));
        }
        return false;
    }

    /**
     * Create a new token node for the specified user.
     *
     * @param user
     * @param credentials
     * @param tokenExpiration
     * @param session
     * @return A new instance of <code>TokenCredentials</code> to be used for
     * further login actions against this Authentication implementation.
     * @throws RepositoryException If there is no node corresponding to the
     * specified user in the current workspace or if an error occurs while
     * creating the token node.
     */
    public static Credentials createToken(User user, SimpleCredentials credentials,
                                          long tokenExpiration, Session session) throws RepositoryException {
        String workspaceName = session.getWorkspace().getName();
        if (user == null) {
            throw new RepositoryException("Cannot create login token: No corresponding node for 'null' user in workspace '" + workspaceName + "'.");
        }

        TokenInfo ti;
        if (compatMode()) {
            ti = new CompatTokenProvider((SessionImpl) session, tokenExpiration).createToken(user, credentials);
        } else {
            ti = new TokenProvider((SessionImpl) session, tokenExpiration).createToken(user, credentials);
        }

        if (ti != null) {
            return ti.getCredentials();
        } else {
            throw new RepositoryException("Cannot create login token.");
        }
    }

    public static Node getTokenNode(TokenCredentials credentials, Session session) throws RepositoryException {
        if (compatMode()) {
            return CompatTokenProvider.getTokenNode(credentials.getToken(), session);
        } else {
            return TokenProvider.getTokenNode(credentials.getToken(), session);
        }
    }


    public static String getUserId(TokenCredentials tokenCredentials, Session session) throws RepositoryException {
        if (compatMode()) {
            return CompatTokenProvider.getUserId(tokenCredentials, session);
        } else {
            if (!(session instanceof JackrabbitSession)) {
                throw new RepositoryException("JackrabbitSession expected");
            }
            NodeImpl n = (NodeImpl) getTokenNode(tokenCredentials, session);
            return TokenProvider.getUserId(n, ((JackrabbitSession) session).getUserManager());
        }
    }

    private static boolean compatMode() {
        return Boolean.parseBoolean(System.getProperty(PARAM_COMPAT));
    }
}