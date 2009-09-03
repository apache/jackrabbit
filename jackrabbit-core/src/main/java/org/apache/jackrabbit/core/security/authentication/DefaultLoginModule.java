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

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import java.security.Principal;
import java.util.Map;

/**
 * The <code>DefaultLoginModule</code> authenticates Credentials related to
 * a {@link User} of the Repository<br>
 * In any other case it is marked to be ignored.<p>
 * This Module can deal only with <code>SimpleCredentials</code> since it
 * uses by default the {@link SimpleCredentialsAuthentication}. Impersonation is
 * delegated to the <code>User</code>'s {@link User#getImpersonation()
 * Impersonation} object
 *
 * @see AbstractLoginModule
 */
public class DefaultLoginModule extends AbstractLoginModule {

    private static final Logger log = LoggerFactory.getLogger(AbstractLoginModule.class);

    protected User user;
    private UserManager userManager;

    /**
     * Retrieves the user manager from the specified session. If this fails
     * this login modules initialization must fail.
     *
     * @see AbstractLoginModule#doInit(CallbackHandler, Session, Map)
     */
    protected void doInit(CallbackHandler callbackHandler, Session session, Map options) throws LoginException {
        if (!(session instanceof SessionImpl)) {
            throw new LoginException("Unable to initialize LoginModule: SessionImpl expected.");
        }
        try {
            userManager = ((SessionImpl) session).getUserManager();
            log.debug("- UserManager -> '" + userManager.getClass().getName() + "'");
        } catch (RepositoryException e) {
            throw new LoginException("Unable to initialize LoginModule: " + e.getMessage());
        }
    }

    /**
     * Resolves the userID from the given credentials and obtains the
     * principal from the User object associated with the given userID.
     * If the the userID cannot be resolved to a User or if obtaining the
     * principal fail, <code>null</code> is returned.
     *
     * @param credentials Credentions to retrieve the principal for.
     * @return a user principal or <code>null</code>.
     * @see AbstractLoginModule#getPrincipal(Credentials)
     */
    protected Principal getPrincipal(Credentials credentials) {
        Principal principal = null;
        String userId = getUserID(credentials);
        try {
            Authorizable authrz = userManager.getAuthorizable(userId);
            if (authrz != null && !authrz.isGroup()) {
                user = (User) authrz;
                principal = user.getPrincipal();
            }
        } catch (RepositoryException e) {
            // should not get here
            log.warn("Error while retrieving principal.", e.getMessage());
        }
        return principal;
    }

    /**
     * @see AbstractLoginModule#getAuthentication(Principal, Credentials)
     */
    protected Authentication getAuthentication(Principal principal, Credentials creds) throws RepositoryException {
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
     * Handles the impersonation of given Credentials.<p />
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
    protected boolean impersonate(Principal principal, Credentials credentials)
            throws RepositoryException, FailedLoginException {

        Authorizable authrz = userManager.getAuthorizable(principal);
        if (authrz == null || authrz.isGroup()) {
            return false;
        }
        Subject impersSubject = getImpersonatorSubject(credentials);
        User user = (User) authrz;
        if (user.getImpersonation().allows(impersSubject)) {
            return true;
        } else {
            throw new FailedLoginException("attempt to impersonate denied for " + principal.getName());
        }
    }
}
