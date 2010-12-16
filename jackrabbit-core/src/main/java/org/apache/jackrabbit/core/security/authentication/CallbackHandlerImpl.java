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

import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.principal.PrincipalProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

/**
 * CallbackHandler that deals with the following callbacks:
 * <ul>
 * <li>{@link NameCallback}
 * <li>{@link PasswordCallback}
 * <li>{@link CredentialsCallback}
 * <li>{@link ImpersonationCallback}
 * <li>{@link RepositoryCallback}
 * </ul>
 */
public class CallbackHandlerImpl implements CallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(CallbackHandlerImpl.class);

    private final Session session;
    private final Credentials credentials;
    private final PrincipalProviderRegistry principalProviderRegistry;
    private final String adminId;
    private final String anonymousId;

    /**
     * Instantiate with the data needed to handle callbacks
     *
     * @param credentials
     * @param session
     */
    public CallbackHandlerImpl(Credentials credentials, Session session,
                               PrincipalProviderRegistry principalProviderRegistry,
                               String adminId, String anonymousId) {
        this.credentials = credentials;
        this.session = session;
        this.principalProviderRegistry = principalProviderRegistry;
        this.adminId = adminId;
        this.anonymousId = anonymousId;

        if (session == null) {
            log.debug("Session is null -> CallbackHandler won't be able to handle RepositoryCallback.");
        }
        if (principalProviderRegistry == null) {
            log.debug("PrincipalProviderRegistry is null -> CallbackHandler won't be able to handle RepositoryCallback.");            
        }
    }

    /**
     * @param callbacks
     * @throws IOException
     * @throws UnsupportedCallbackException
     * @see CallbackHandler#handle(Callback[])
     */
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

        for (Callback callback : callbacks) {
            if (callback instanceof CredentialsCallback) {
                ((CredentialsCallback) callback).setCredentials(credentials);
            } else if (callback instanceof RepositoryCallback) {
                /*
                if callback handler has been created with null session or
                null principalProviderRegistry this handler cannot properly
                deal with RepositoryCallback
                */
                if (session == null || principalProviderRegistry == null) {
                    throw new UnsupportedCallbackException(callback);
                }
                RepositoryCallback rcb = (RepositoryCallback) callback;
                rcb.setSession(session);
                rcb.setPrincipalProviderRegistry(principalProviderRegistry);
                rcb.setAdminId(adminId);
                rcb.setAnonymousId(anonymousId);
            } else if (credentials != null && credentials instanceof SimpleCredentials) {
                SimpleCredentials simpleCreds = (SimpleCredentials) credentials;
                if (callback instanceof NameCallback) {
                    String userId = simpleCreds.getUserID();
                    ((NameCallback) callback).setName(userId);
                } else if (callback instanceof PasswordCallback) {
                    char[] pw = simpleCreds.getPassword();
                    ((PasswordCallback) callback).setPassword(pw);
                } else if (callback instanceof ImpersonationCallback) {
                    Object impersAttr = simpleCreds.getAttribute(SecurityConstants.IMPERSONATOR_ATTRIBUTE);
                    ((ImpersonationCallback) callback).setImpersonator(impersAttr);
                } else {
                    throw new UnsupportedCallbackException(callback);
                }
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }
}
