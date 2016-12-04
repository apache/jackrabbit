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

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * Implements the common {@link AuthContext} interface for the JAAS environment.
 *
 * @see AuthContext
 */
public class JAASAuthContext implements AuthContext {

    private LoginContext context;

    /**
     * @param appName   application name in JAAS Login-Configuration to use
     * @param cbHandler CallbackHandler for login-modules
     * @param subject   to extend authentication
     */
    protected JAASAuthContext(String appName, CallbackHandler cbHandler,
                              Subject subject) {

        // make sure we are using our own context class loader when we
        // instantiate a LoginContext. See bug# 14329.
        Thread current = Thread.currentThread();
        ClassLoader orig = current.getContextClassLoader();
        try {
            current.setContextClassLoader(JAASAuthContext.class.getClassLoader());
            if (null == subject) {
                context = new LoginContext(appName, cbHandler);
            } else {
                context = new LoginContext(appName, subject, cbHandler);
            }
        } catch (LoginException e) {
            //all cases it is thrown are checked -> ignore
        } finally {
            current.setContextClassLoader(orig);
        }
    }

    public void login() throws LoginException {
        context.login();
    }

    public Subject getSubject() {
        return context.getSubject();
    }

    public void logout() throws LoginException {
        context.logout();
    }
}
