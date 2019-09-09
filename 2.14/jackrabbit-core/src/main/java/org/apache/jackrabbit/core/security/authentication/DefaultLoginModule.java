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
package org.apache.jackrabbit.core.security.authentication;

import java.security.Principal;
import java.util.Map;
import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.jackrabbit.api.security.authentication.token.TokenCredentials;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authentication.token.TokenBasedAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>DefaultLoginModule</code> authenticates Credentials related to
 * a {@link User} of the Repository<br>
 * In any other case it is marked to be ignored.<p>
 * This Module can deal with the following credentials
 * <ul>
 * <li><code>SimpleCredentials</code> -&gt; handled by {@link SimpleCredentialsAuthentication}.</li>
 * <li><code>TokenCredentials</code> -&gt; handled by {@link TokenBasedAuthentication}.</li>
 * </ul>
 * In both cases the login is successful if the system contains a non-disabled,
 * valid user that matches the given credentials.
 * <p>
 * Correspondingly impersonation is delegated to the <code>User</code>'s
 * {@link User#getImpersonation() Impersonation} object.
 *
 * @see AbstractLoginModule
 */
public class DefaultLoginModule extends AbstractLoginModule {

    private static final Logger log = LoggerFactory.getLogger(DefaultLoginModule.class);

    /**
     * Optional configuration parameter to disable token based authentication.
     */
    private static final String PARAM_DISABLE_TOKEN_AUTH = "disableTokenAuth";

    /**
     * Optional configuration parameter to disable token based authentication.
     */
    private static final String PARAM_TOKEN_EXPIRATION = "tokenExpiration";

    /**
     * Flag indicating if Token-based authentication is disabled by the
     * LoginModule configuration.
     */
    private boolean disableTokenAuth;

    /**
     * The expiration time for login tokens as set by the LoginModule configuration.
     */
    private long tokenExpiration = TokenBasedAuthentication.TOKEN_EXPIRATION;

    /**
     * The user object retrieved during the authentication process.
     */
    protected User user;
    private SessionImpl session;
    private UserManager userManager;

    /**
     * The TokenCredentials or null in case of another credentials.
     */
    private TokenCredentials tokenCredentials;

    //--------------------------------------------------------< LoginModule >---
    /**
     * @see javax.security.auth.spi.LoginModule#commit()
     */
    @Override
    public boolean commit() throws LoginException {
        boolean success = super.commit();
        if (success && !disableTokenAuth) {
            if (TokenBasedAuthentication.doCreateToken(credentials)) {
                Session s = null;
                try {
                    /*
                    use a different session instance to create the token
                    node in order to prevent concurrent modifications with
                    the shared system session.
                    */
                    s = session.createSession(session.getWorkspace().getName());
                    Credentials tc = TokenBasedAuthentication.createToken(user, credentials, tokenExpiration, s);
                    if (tc != null) {
                        subject.getPublicCredentials().add(tc);
                    }
                } catch (RepositoryException e) {
                    LoginException le = new LoginException("Failed to commit: " + e.getMessage());
                    le.initCause(e);
                    throw le;
                } finally {
                    if (s != null) {
                        s.logout();
                    }
                }
            } else if (tokenCredentials != null) {
                subject.getPublicCredentials().add(tokenCredentials);
            }
        }
        return success;
    }

    //------------------------------------------------< AbstractLoginModule >---
    /**
     * Retrieves the user manager from the specified session. If this fails
     * this login modules initialization must fail.
     *
     * @see AbstractLoginModule#doInit(CallbackHandler, Session, Map)
     */
    @Override
    protected void doInit(CallbackHandler callbackHandler, Session session, Map options) throws LoginException {
        if (!(session instanceof SessionImpl)) {
            throw new LoginException("Unable to initialize LoginModule: SessionImpl expected.");
        }
        try {
            this.session = (SessionImpl) session;
            userManager = this.session.getUserManager();
            log.debug("- UserManager -> '" + userManager.getClass().getName() + "'");
        } catch (RepositoryException e) {
            throw new LoginException("Unable to initialize LoginModule: " + e.getMessage());
        }

        // configuration options related to token based authentication
        if (options.containsKey(PARAM_DISABLE_TOKEN_AUTH)) {
            disableTokenAuth = Boolean.parseBoolean(options.get(PARAM_DISABLE_TOKEN_AUTH).toString());
            log.debug("- Token authentication disabled -> '" + disableTokenAuth + "'");
        }
        if (options.containsKey(PARAM_TOKEN_EXPIRATION)) {
            try {
                tokenExpiration = Long.parseLong(options.get(PARAM_TOKEN_EXPIRATION).toString());
                log.debug("- Token expiration -> '" + tokenExpiration + "'");
            } catch (NumberFormatException e) {
                log.warn("Unabled to parse token expiration: {}", e.getMessage());
            }
        }
    }

    /**
     * Resolves the userID from the given credentials and obtains the
     * principal from the User object associated with the given userID.
     * If the the userID cannot be resolved to a User or if obtaining the
     * principal fail, <code>null</code> is returned.
     *
     * @param credentials Credentials to retrieve the principal for.
     * @return a user principal or <code>null</code>.
     * @see AbstractLoginModule#getPrincipal(Credentials)
     */
    @Override
    protected Principal getPrincipal(Credentials credentials) {
        Principal principal = null;
        String userId = getUserID(credentials);
        try {
            Authorizable authrz = userManager.getAuthorizable(userId);
            if (authrz != null && !authrz.isGroup()) {
                user = (User) authrz;
                if (user.isDisabled()) {
                    // log message and return null -> login module returns false.
                    log.debug("User " + userId + " has been disabled.");
                } else {
                    principal = user.getPrincipal();
                }
            }
        } catch (RepositoryException e) {
            // should not get here
            log.warn("Error while retrieving principal. {}", e.getMessage());
        }
        return principal;
    }

    /**
     * @see AbstractLoginModule#supportsCredentials(javax.jcr.Credentials)
     */
    @Override
    protected boolean supportsCredentials(Credentials creds) {
        if (creds instanceof TokenCredentials) {
            return !disableTokenAuth;
        } else {
            return super.supportsCredentials(creds);
        }
    }

    /**
     * @see AbstractLoginModule#getUserID(javax.jcr.Credentials)
     */
    @Override
    protected String getUserID(Credentials credentials) {
        // shortcut to avoid duplicate evaluation.
        if (user != null) {
            try {
                return user.getID();
            } catch (RepositoryException e) {
                log.warn("Failed to retrieve userID from user", e);
                // ignore and re-evaluate credentials.
            }
        }

        // handle TokenCredentials
        if (!disableTokenAuth && TokenBasedAuthentication.isTokenBasedLogin(credentials)) {
            // special token based login
            tokenCredentials = ((TokenCredentials) credentials);
            try {
                return TokenBasedAuthentication.getUserId(tokenCredentials, session);
            } catch (RepositoryException e) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to retrieve UserID from token-based credentials", e);
                } else {
                    log.warn("Failed to retrieve UserID from token-based credentials: {}", e.toString());
                }
            }
            // failed to retrieve the user from loginToken.
            return null;
        } else {
            // regular login -> extraction of userID is handled by the super class.
            return super.getUserID(credentials);
        }
    }

    /**
     * @see AbstractLoginModule#getAuthentication(Principal, Credentials)
     */
    @Override
    protected Authentication getAuthentication(Principal principal, Credentials creds) throws RepositoryException {
        if (!disableTokenAuth && tokenCredentials != null) {
            Authentication authentication = new TokenBasedAuthentication(tokenCredentials.getToken(), tokenExpiration, session);
            if (authentication.canHandle(creds)) {
                return authentication;
            }
        }

        if (user != null) {
            Authentication authentication = new SimpleCredentialsAuthentication(user);
            if (authentication.canHandle(creds)) {
                return authentication;
            }
        }
        // no valid user or authentication could not handle the given credentials
        return null;
    }

    /**
     * Handles the impersonation of given Credentials.
     * <p>
     * Current implementation takes {@link User} for the given Principal and
     * delegates the check to
     * {@link org.apache.jackrabbit.api.security.user.Impersonation#allows(javax.security.auth.Subject)}
     *
     * @param principal Principal to impersonate.
     * @param credentials Credentials used to create the impersonation subject.
     * @return false, if there is no User to impersonate,
     *         true if impersonation is allowed
     * @throws javax.jcr.RepositoryException
     * @throws javax.security.auth.login.FailedLoginException
     *                                       if credentials don't allow to impersonate to principal
     * @see AbstractLoginModule#impersonate(Principal, Credentials)
     */
    @Override
    protected boolean impersonate(Principal principal, Credentials credentials)
            throws RepositoryException, FailedLoginException {
        if (user != null) {
            Subject impersSubject = getImpersonatorSubject(credentials);
            if (user.getImpersonation().allows(impersSubject)) {
                return true;
            } else {
                throw new FailedLoginException("attempt to impersonate denied for " + principal.getName());
            }
        } else {
            log.debug("Failed to retrieve user to impersonate for principal name " + principal.getName());
            return false;
        }
    }

    //--------------------------------------------------------------------------
    // methods used for token based login
    //--------------------------------------------------------------------------
    /**
     * Return a flag indicating if token based authentication is disabled.
     *
     * @return <code>true</code> if token based authentication is disabled;
     * <code>false</code> otherwise.
     */
    public boolean isDisableTokenAuth() {
        return disableTokenAuth;
    }

    /**
     * Set a flag indicating if token based authentication is disabled.
     *
     * @param disableTokenAuth <code>true</code> to disable token based
     * authentication; <code>false</code> otherwise
     */
    public void setDisableTokenAuth(boolean disableTokenAuth) {
        this.disableTokenAuth = disableTokenAuth;
    }

    /**
     * @return The configured expiration time for login tokens in milliseconds.
     */
    public long getTokenExpiration() {
        return tokenExpiration;
    }

    /**
     * @param tokenExpiration Sets the configured expiration time (in milliseconds)
     * of login tokens.
     */
    public void setTokenExpiration(long tokenExpiration) {
        this.tokenExpiration = tokenExpiration;
    }
}
