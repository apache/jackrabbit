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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.jackrabbit.core.security.TestPrincipal;
import org.apache.jackrabbit.core.security.principal.FallbackPrincipalProvider;
import org.apache.jackrabbit.core.security.principal.ProviderRegistryImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * <code>LoginModuleTest</code> checks if multiple login modules are properly
 * handled. More specifically, this test case sets up a configuration with
 * two login modules:
 * <ul>
 * <li>module 1: required. This module will always authenticate successfully</li>
 * <li>module 2: sufficient. This module will always indicate that it should be ignored.</li>
 * </ul>
 * See also JCR-2671.
 */
public class LoginModuleTest extends AbstractJCRTest {

    private static final String APP_NAME = LoginModuleTest.class.getName();

    public void testMultipleModules() throws Exception {

        CallbackHandler ch = new CallbackHandlerImpl(new SimpleCredentials("user", "pass".toCharArray()), 
                superuser, new ProviderRegistryImpl(new FallbackPrincipalProvider()),
                "admin", "anonymous");
        LoginContext context = new LoginContext(
                APP_NAME, new Subject(), ch, new TestConfiguration());
        context.login();
        assertFalse("no principal set", context.getSubject().getPrincipals().isEmpty());
    }

    static class TestConfiguration extends Configuration {

        @Override
        public void refresh() {
        }

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            return new AppConfigurationEntry[] {
                    new TestAppConfigurationEntry(AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, false),
                    new TestAppConfigurationEntry(AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT, true)
            };
        }
    }

    static class TestAppConfigurationEntry extends AppConfigurationEntry {

        private static final Map<String, Object> IGNORE = new HashMap<String, Object>();

        private static final Map<String, Object> EMPTY = Collections.emptyMap();

        static {
            IGNORE.put("ignore", "true");
        }

        public TestAppConfigurationEntry(LoginModuleControlFlag controlFlag,
                                         boolean ignore) {
            super(TestLoginModule.class.getName(), controlFlag, ignore ? IGNORE : EMPTY);
        }
    }

    public static class TestLoginModule extends AbstractLoginModule {

        private boolean ignore = false;

        @Override
        protected void doInit(CallbackHandler callbackHandler,
                              Session session,
                              Map options) throws LoginException {
            if (options.containsKey("ignore")) {
                ignore = true;
            }
        }

        @Override
        protected boolean impersonate(Principal principal,
                                      Credentials credentials)
                throws RepositoryException, LoginException {
            return false;
        }

        @Override
        protected Authentication getAuthentication(Principal principal,
                                                   Credentials creds)
                throws RepositoryException {
            if (ignore) {
                return null;
            } else {
                return new Authentication() {
                    public boolean canHandle(Credentials credentials) {
                        return true;
                    }

                    public boolean authenticate(Credentials credentials)
                            throws RepositoryException {
                        return true;
                    }
                };
            }
        }

        @Override
        protected Principal getPrincipal(Credentials credentials) {
            if (ignore) {
                return null;
            } else {
                return new TestPrincipal(((SimpleCredentials) credentials).getUserID());
            }
        }
    }
}
