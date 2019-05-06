/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.jackrabbit.server.remoting.davex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.apache.jackrabbit.webdav.util.CSRFUtil;

@Component(metatype = true, label = "%dav.name", description = "%dav.description")
@Service(Servlet.class)
@Properties({
    @Property(name = "service.description", value = "Apache Jackrabbit JcrRemoting Servlet"),
    @Property(name = JcrRemotingServlet.INIT_PARAM_AUTHENTICATE_HEADER, value = AbstractWebdavServlet.DEFAULT_AUTHENTICATE_HEADER),
    @Property(name = JcrRemotingServlet.INIT_PARAM_CSRF_PROTECTION, value = CSRFUtil.DISABLED),
    @Property(name = JcrRemotingServlet.INIT_PARAM_MISSING_AUTH_MAPPING, value = ""),
    @Property(name = "contextId", value = "") })
@Reference(
        name = "providers", referenceInterface = SessionProvider.class,
        policy = ReferencePolicy.DYNAMIC,
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        bind = "addSessionProvider", unbind = "removeSessionProvider")
public class DavexServletService extends JcrRemotingServlet
        implements SessionProvider {

    /** Serial version UID */
    private static final long serialVersionUID = -901601294536148635L;

    private static final String DEFAULT_ALIAS = "/server";

    @Property(value = DEFAULT_ALIAS)
    private static final String PARAM_ALIAS = "alias";

    @Reference
    private Repository repository;

    private String alias;

    /**
     * Currently available custom session providers. They're used
     * first before the default provider gets consulted. The associated
     * set of sessions is used to forcibly release all sessions acquired
     * from a provider when that provider is being removed.
     */
    private final Map<SessionProvider, Set<Session>> providers =
            new LinkedHashMap<SessionProvider, Set<Session>>();

    /**
     * Currently active sessions. Used to link a session to the correct
     * provider in the {@link #releaseSession(Session)} method.
     */
    private final Map<Session, SessionProvider> sessions =
            new HashMap<Session, SessionProvider>();

    @Override
    protected Repository getRepository() {
        return repository;
    }

    @Override
    protected String getResourcePathPrefix() {
        return alias;
    }

    @Activate
    public void activate(Map<String, ?> config) {
        Object object = config.get(PARAM_ALIAS);
        String string = "";
        if (object != null) {
            string = object.toString();
        }
        if (string.length() > 0) {
            this.alias = string;
        } else {
            this.alias = DEFAULT_ALIAS;
        }
    }

    @Override
    protected SessionProvider getSessionProvider() {
        return this;
    }

    /**
     * Adds a custom session provider service.
     *
     * @param provider session provider
     */
    public synchronized void addSessionProvider(SessionProvider provider) {
        providers.put(provider, new HashSet<Session>());
    }

    /**
     * Removes a custom session provider service. All active sessions
     * acquired from that provider are forcibly released.
     *
     * @param provider session provider
     */
    public synchronized void removeSessionProvider(SessionProvider provider) {
        Set<Session> sessions = providers.remove(provider);
        if (sessions != null) {
            for (Session session : sessions) {
                releaseSession(session);
            }
        }
    }

    //-----------------------------------------------------< SessionProvider >

    /**
     * Asks each available session provider in order for a session and
     * returns the first session given. The default provider is used
     * if no custom provider service is available or can provide a requested
     * session.
     */
    public synchronized Session getSession(
            HttpServletRequest request, Repository repository, String workspace)
            throws LoginException, ServletException, RepositoryException {
        SessionProvider provider = null;
        Session session = null;

        for (Map.Entry<SessionProvider, Set<Session>> entry : providers.entrySet()) {
            provider = entry.getKey();
            session = provider.getSession(request, repository, workspace);
            if (session != null) {
                entry.getValue().add(session);
                break;
            }
        }

        if (session == null) {
            provider = super.getSessionProvider();
            session = provider.getSession(request, repository, workspace);
        }

        if (session != null) {
            sessions.put(session, provider);
        }

        return session;
    }

    /**
     * Releases the given session using the provider from which it was acquired.
     */
    public synchronized void releaseSession(Session session) {
        SessionProvider provider = sessions.remove(session);
        if (provider != null) {
            provider.releaseSession(session);
        }
    }

}
