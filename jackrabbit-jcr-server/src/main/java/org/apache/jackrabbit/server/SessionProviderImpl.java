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
package org.apache.jackrabbit.server;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * This Class implements a default session provider uses a credentials provider.
 */
public class SessionProviderImpl implements SessionProvider {

    /**
     * the credentials provider
     */
    private CredentialsProvider cp;

    /**
     * Creates a new SessionProvider
     * @param cp
     */
    public SessionProviderImpl(CredentialsProvider cp) {
        this.cp = cp;
    }

    /**
     * {@inheritDoc }
     */
    public Session getSession(HttpServletRequest request, Repository repository,
                              String workspace)
        throws LoginException, RepositoryException, ServletException {
        Credentials creds = cp.getCredentials(request);
        if (creds == null) {
            return repository.login(workspace);
        } else {
            return repository.login(creds, workspace);
        }
    }

    /**
     * {@inheritDoc }
     */
    public void releaseSession(Session session) {
        session.logout();
    }
}
