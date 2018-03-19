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
package org.apache.jackrabbit.core.security.simple;

import org.apache.jackrabbit.core.security.authentication.AbstractLoginModule;
import org.apache.jackrabbit.core.security.authentication.Authentication;
import org.apache.jackrabbit.core.security.principal.GroupPrincipals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import java.security.Principal;
import java.util.Map;

/**
 * <code>SimpleLoginModule</code>...
 */
public class SimpleLoginModule extends AbstractLoginModule {

    private static Logger log = LoggerFactory.getLogger(SimpleLoginModule.class);

    /**
     * @see AbstractLoginModule#doInit(javax.security.auth.callback.CallbackHandler, javax.jcr.Session, java.util.Map)
     */
    @Override
    protected void doInit(CallbackHandler callbackHandler, Session session, Map options) throws LoginException {
        // nothing to do
        log.debug("init: SimpleLoginModule. Done.");
    }

    /**
     * @see AbstractLoginModule#impersonate(java.security.Principal, javax.jcr.Credentials)
     */
    @Override
    protected boolean impersonate(Principal principal, Credentials credentials) throws RepositoryException, LoginException {
        if (GroupPrincipals.isGroup(principal)) {
            return false;
        }
        Subject impersSubject = getImpersonatorSubject(credentials);
        return impersSubject != null;
    }

    /**
     * @see AbstractLoginModule#getAuthentication(java.security.Principal, javax.jcr.Credentials)
     */
    @Override
    protected Authentication getAuthentication(Principal principal, Credentials creds) throws RepositoryException {
        if (GroupPrincipals.isGroup(principal)) {
            return null;
        }
        return new Authentication() {
            public boolean canHandle(Credentials credentials) {
                return true;
            }
            public boolean authenticate(Credentials credentials) throws RepositoryException {
                return true;
            }
        };
    }

    /**
     * Uses the configured {@link org.apache.jackrabbit.core.security.principal.PrincipalProvider} to retrieve the principal.
     * It takes the {@link org.apache.jackrabbit.core.security.principal.PrincipalProvider#getPrincipal(String)} for the User-ID
     * resolved by  {@link #getUserID(Credentials)}, assuming that
     * User-ID and the corresponding principal name are always identical.
     *
     * @param credentials Credentials for which the principal should be resolved.
     * @return principal or <code>null</code> if the principal provider does
     * not contain a user-principal with the given userID/principal name.
     *
     * @see AbstractLoginModule#getPrincipal(Credentials)
     */
    @Override
    protected Principal getPrincipal(Credentials credentials) {
        String userId = getUserID(credentials);
        Principal principal = principalProvider.getPrincipal(userId);
        if (principal == null || GroupPrincipals.isGroup(principal)) {
            // no matching user principal
            return null;
        } else {
            return principal;
        }
    }
}