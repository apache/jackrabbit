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

import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.LoginModuleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Provide AuthContext interface, for a JAAS-LoginModule not running in
 * a {@link javax.security.auth.login.LoginContext}
 *
 * @see AuthContext
 */
public class LocalAuthContext implements AuthContext {

    private static final Logger log = LoggerFactory.getLogger(LocalAuthContext.class);

    private Subject subject;

    private LoginModuleConfig config;

    private LoginModule module;

    private final CallbackHandler cbHandler;

    /**
     * Create Context and set Subject to extend its authentication
     *
     * @param config    Configuration to be used for the LoginModule
     * @param cbHandler CallbackHandler for the LoginModule
     * @param subject   Subject if a pre-authenticated exists
     */
    protected LocalAuthContext(LoginModuleConfig config,
                               CallbackHandler cbHandler,
                               Subject subject) {
        this.config = config;
        this.cbHandler = cbHandler;
        this.subject = (null == subject) ? new Subject() : subject;
    }

    public void login() throws LoginException {
        try {
            module = config.getLoginModule();
        } catch (ConfigurationException e) {
            throw new LoginException(e.getMessage());
        }

        Map<String, Object> state = new HashMap<String, Object>();
        Map<String, String> options = new HashMap<String, String>();
        Properties parameters = config.getParameters();
        Enumeration< ? > names = parameters.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            options.put(name, parameters.getProperty(name));
        }
        module.initialize(subject, cbHandler, state, options);

        try {
            if (!(module.login() && module.commit())) {
                throw new FailedLoginException("LoginModule ignored Credentials");
            }
        } catch (LoginException le) {
            module.abort();
            throw le;
        } catch (Exception e) {
            module.abort();
            LoginException le = new LoginException("LoginModule could not perform authentication: " +
                    e.getMessage());
            le.initCause(e);
            log.debug("Login failed to runtime-exception: ", e);
            throw le;
        }
    }

    public Subject getSubject() {
        return subject;
    }

    public void logout() throws LoginException {
        if (subject != null) {
            module.logout();
        }
    }
}
