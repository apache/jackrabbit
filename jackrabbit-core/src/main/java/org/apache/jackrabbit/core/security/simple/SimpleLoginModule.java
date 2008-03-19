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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.core.security.authentication.Authentication;
import org.apache.jackrabbit.core.security.authentication.AbstractLoginModule;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.Subject;
import javax.jcr.Session;
import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import java.util.Map;
import java.security.Principal;
import java.security.acl.Group;

/**
 * <code>SimpleLoginModule</code>...
 */
public class SimpleLoginModule extends AbstractLoginModule {

    private static Logger log = LoggerFactory.getLogger(SimpleLoginModule.class);

    protected void doInit(CallbackHandler callbackHandler, Session session, Map options) throws LoginException {
        // nothing to do
        log.debug("init: SimpleLoginModule. Done.");
    }

    protected boolean impersonate(Principal principal, Credentials credentials) throws RepositoryException, LoginException {
        if (principal instanceof Group) {
            return false;
        }
        Subject impersSubject = getImpersonatorSubject(credentials);
        return impersSubject != null;
    }

    protected Authentication getAuthentication(Principal principal, Credentials creds) throws RepositoryException {
        if (principal instanceof Group) {
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
}