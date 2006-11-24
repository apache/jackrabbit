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
package org.apache.jackrabbit.core.security;

import javax.jcr.Credentials;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.util.Collections;
import java.util.Map;

/**
 * An authentication context used to authenticate users. It is similar to JAAS' <code>LoginContext</code>
 * but can work in a non-JAAS environment.
 * <p>
 * This class is abstract and has two implementations:
 * <ul>
 *   <li>{@link AuthContext.JAAS} which delegates to a regular JAAS <code>LoginContext</code></li>
 *   <li>{@link AuthContext.Local} which implements authentication using a locally-defined
 *       JAAS <code>LoginModule</code></li>
 * </ul>
 *
 */
public abstract class AuthContext {

    /**
     * Perform the authentication and, if successful, associate Principals and Credentials
     * with the authenticated<code>Subject</code>.
     *
     * @see LoginContext#login()
     * @throws LoginException if the authentication fails.
     */
    public abstract void login() throws LoginException;

    /**
     * Return the authenticated Subject.
     *
     * @see LoginContext#getSubject()
     * @return the authenticated Subject or <code>null</code> if authentication failed.
     */
    public abstract Subject getSubject();

    /**
     * Logout the <code>Subject</code>.
     *
     * @see LoginContext#logout()
     * @exception LoginException if the logout fails.
     */
    public abstract void logout() throws LoginException;

    /**
     * An {@link AuthContext} implemented using a regular JAAS <code>LoginContext</code>.
     */
    public static class JAAS extends AuthContext {

        private final LoginContext ctx;

        /**
         * Creates an authentication context given a JAAS configuration name and some credentials.
         *
         * @param name the JAAS configuration index
         * @param creds the credentials
         * @throws LoginException if the JAAS context couldn't be created
         */
        public JAAS(String name, Credentials creds) throws LoginException {
            this.ctx = new LoginContext(name, new CredentialsCallbackHandler(creds));
        }

        /**
         * {@inheritDoc}
         */
        public void login() throws LoginException {
            ctx.login();
        }

        /**
         * {@inheritDoc}
         */
        public Subject getSubject() {
            return ctx.getSubject();
        }

        /**
         * {@inheritDoc}
         */
        public void logout() throws LoginException {
            ctx.logout();
        }

    }

    /**
     * An {@link AuthContext} implemented using a particular <code>LoginModule</code>.
     */
    public static class Local extends AuthContext {
        private final LoginModule module;
        private final Map options;
        private Subject subject;
        private Credentials creds;

        /**
         * Creates an authentication context given a login module and some credentials.
         *
         * @param module the login module
         * @param options login module options
         * @param config the login module configuration
         * @param creds the credentials
         */
        public Local(LoginModule module, Map options, Credentials creds) {
            this.module = module;
            this.options = options;
            this.creds = creds;
        }

        /**
         * {@inheritDoc}
         */
        public void login() throws LoginException {
            this.subject = new Subject();
            this.module.initialize(
                    this.subject,
                    new CredentialsCallbackHandler(this.creds),
                    Collections.EMPTY_MAP,
                    this.options);

            LoginException failure = null;
            try {
                if (!module.login()) {
                    failure = new LoginException("Login not confirmed");
                }
            } catch (LoginException e) {
                failure = e;
            }
            if (failure == null) {
                module.commit();
            } else {
                module.abort();
                throw failure;
            }
        }

        /**
         * {@inheritDoc}
         */
        public Subject getSubject() {
            return this.subject;
        }

        /**
         * {@inheritDoc}
         */
        public void logout() throws LoginException {
            if (this.subject == null) {
                throw new LoginException("Logout called before login");
            }
            this.module.logout();
        }
    }
}
