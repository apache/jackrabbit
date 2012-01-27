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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * This class implements a default session provider based on a given
 * {@link CredentialsProvider credentials provider}. Additionally,
 * since Jackrabbit 2.4, if another session provider is available as
 * the "org.apache.jackrabbit.server.SessionProvider" request attribute,
 * then that provider is asked first for a session before the default
 * credential-based login mechanism is used.
 */
public class SessionProviderImpl implements SessionProvider {

    /**
     * the credentials provider
     */
    private CredentialsProvider cp;

    /**
     * Map of sessions acquired from custom session providers looked up
     * from request attributes. We need to keep track of such providers
     * so we can route the {@link #releaseSession(Session)} call to the
     * correct provider.
     */
    private final Map<Session, SessionProvider> externalSessions =
            Collections.synchronizedMap(new HashMap<Session, SessionProvider>());

    /**
     * Creates a new SessionProvider
     * 
     * @param cp
     */
    public SessionProviderImpl(CredentialsProvider cp) {
        this.cp = cp;
    }

    /**
     * {@inheritDoc }
     */
    public Session getSession(HttpServletRequest request,
            Repository repository, String workspace) throws LoginException,
            RepositoryException, ServletException {
        Session s = null;

        // JCR-3222: Check if a custom session provider is available as a
        // request attribute. If one is available, ask it first for a session.
        Object object = request.getAttribute(SessionProvider.class.getName());
        if (object instanceof SessionProvider) {
            SessionProvider provider = (SessionProvider) object;
            s = provider.getSession(request, repository, workspace);
            if (s != null) {
                externalSessions.put(s, provider);
            }
        }

        if (s == null) {
            Credentials creds = cp.getCredentials(request);
            if (creds == null) {
                s = repository.login(workspace);
            } else {
                s = repository.login(creds, workspace);
            }
        }

        return s;
    }

    /**
     * {@inheritDoc }
     */
    public void releaseSession(Session session) {
        // JCR-3222: If the session was acquired from a custom session
        // provider, we need to ask that provider to release the session.
        SessionProvider provider = externalSessions.remove(session);
        if (provider != null) {
            provider.releaseSession(session);
        } else {
            session.logout();
        }
    }

}
