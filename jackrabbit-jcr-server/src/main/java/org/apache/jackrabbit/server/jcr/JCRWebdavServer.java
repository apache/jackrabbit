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
package org.apache.jackrabbit.server.jcr;

import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.header.IfHeader;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.jcr.JcrDavSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * <code>JCRWebdavServer</code>...
 */
public class JCRWebdavServer implements DavSessionProvider {

    /** the default logger */
    private static Logger log = LoggerFactory.getLogger(JCRWebdavServer.class);

    /** the session cache */
    private final SessionCache cache = new SessionCache();

    /** the jcr repository */
    private final Repository repository;

    /** the provider for the credentials */
    private final SessionProvider sessionProvider;

    /**
     * Creates a new JCRWebdavServer that operates on the given repository.
     *
     * @param repository
     */
    public JCRWebdavServer(Repository repository, SessionProvider sessionProvider) {
        this.repository = repository;
        this.sessionProvider = sessionProvider;
    }

    //---------------------------------------< DavSessionProvider interface >---
    /**
     * Acquires a DavSession either from the session cache or creates a new
     * one by login to the repository.
     * Upon success, the WebdavRequest will reference that session.
     *
     * @param request
     * @throws DavException if no session could be obtained.
     * @see DavSessionProvider#attachSession(org.apache.jackrabbit.webdav.WebdavRequest)
     */
    public boolean attachSession(WebdavRequest request)
        throws DavException {
        DavSession session = cache.get(request);
        request.setDavSession(session);
        return true;
    }

    /**
     * Releases the reference from the request to the session. If no further
     * references to the session exist, the session will be removed from the
     * cache.
     *
     * @param request
     * @see DavSessionProvider#releaseSession(org.apache.jackrabbit.webdav.WebdavRequest)
     */
    public void releaseSession(WebdavRequest request) {
        DavSession session = request.getDavSession();
        if (session != null) {
            session.removeReference(request);
        }
        // remove the session from the request
        request.setDavSession(null);
    }

    //--------------------------------------------------------------------------
    /**
     * Private inner class implementing the <code>DavSession</code> interface.
     */
    private class DavSessionImpl extends JcrDavSession {

        /**
         * Private constructor.
         *
         * @param session
         */
        private DavSessionImpl(Session session) {
            super(session);
        }

        /**
         * Add a reference to this <code>DavSession</code>.
         *
         * @see DavSession#addReference(Object)
         */
        public void addReference(Object reference) {
            cache.addReference(this, reference);
        }

        /**
         * Removes the reference from this <code>DavSession</code>. If no
         * more references are present, this <code>DavSession</code> is removed
         * from the internal cache and the underlying session is released by
         * calling {@link SessionProvider#releaseSession(javax.jcr.Session)}
         *
         * @see DavSession#removeReference(Object)
         */
        public void removeReference(Object reference) {
            cache.removeReference(this, reference);
        }
    }

    /**
     * Private inner class providing a cache for referenced session objects.
     */
    private class SessionCache {

        private SessionMap sessionMap = new SessionMap();
        private HashMap referenceToSessionMap = new HashMap();

        /**
         * Try to retrieve <code>DavSession</code> if a TransactionId or
         * SubscriptionId is present in the request header. If no cached session
         * was found <code>null</code> is returned.
         *
         * @param request
         * @return a cached <code>DavSession</code> or <code>null</code>.
         * @throws DavException
         */
        private DavSession get(WebdavRequest request)
            throws DavException {
            String txId = request.getTransactionId();
            String subscriptionId = request.getSubscriptionId();
            String lockToken = request.getLockToken();

            DavSession session = null;
            // try to retrieve a cached session
            if (lockToken != null && containsReference(lockToken)) {
                session = getSessionByReference(lockToken);
            } else if (txId != null && containsReference(txId)) {
                session = getSessionByReference(txId);
            } else if (subscriptionId != null && containsReference(subscriptionId)) {
                session = getSessionByReference(subscriptionId);
            }

            if (session == null) {
                // try tokens present in the if-header
                IfHeader ifHeader = new IfHeader(request);
                for (Iterator it = ifHeader.getAllTokens(); it.hasNext();) {
                    String token = it.next().toString();
                    if (containsReference(token)) {
                        session = getSessionByReference(token);
                        break;
                    }
                }
            }

            // no cached session present -> create new one.
            if (session == null) {
                Session repSession = getRepositorySession(request);
                session = new DavSessionImpl(repSession);
                sessionMap.put(session, new HashSet());
                log.debug("login: User '" + repSession.getUserID() + "' logged in.");
            } else {
                log.debug("login: Retrieved cached session for user '" + getUserID(session) + "'");
            }
            addReference(session, request);
            return session;
        }

        /**
         * Add a references to the specified <code>DavSession</code>.
         *
         * @param session
         * @param reference
         */
        private void addReference(DavSession session, Object reference) {
            HashSet referenceSet = sessionMap.get(session);
            if (referenceSet != null) {
                referenceSet.add(reference);
                referenceToSessionMap.put(reference, session);
            } else {
                log.error("Failed to add reference to session. No entry in cache found.");
            }
        }

        /**
         * Remove the given reference from the specified <code>DavSession</code>.
         *
         * @param session
         * @param reference
         */
        private void removeReference(DavSession session, Object reference) {
            HashSet referenceSet = sessionMap.get(session);
            if (referenceSet != null) {
                if (referenceSet.remove(reference)) {
                    log.debug("Removed reference " + reference + " to session " + session);
                    referenceToSessionMap.remove(reference);
                } else {
                    log.warn("Failed to remove reference " + reference + " to session " + session);
                }
                if (referenceSet.isEmpty()) {
                    log.debug("No more references present on webdav session -> clean up.");
                    sessionMap.remove(session);
                    try {
                        Session repSession = DavSessionImpl.getRepositorySession(session);
                        String usr = getUserID(session) ;
                        sessionProvider.releaseSession(repSession);
                        log.debug("Login: User '" + usr + "' logged out");
                    } catch (DavException e) {
                        // should not occure, since we original built a DavSessionImpl
                        // that wraps a repository session.
                        log.error("Unexpected error: " + e.getMessage(), e.getCause());
                    }
                } else {
                    log.debug(referenceSet.size() + " references remaining on webdav session " + session);
                }
            } else {
                log.error("Failed to remove reference from session. No entry in cache found.");
            }
        }

        /**
         * Returns true, if there exists a <code>DavSession</code> in the cache
         * that is referenced by the specified object.
         *
         * @param reference
         * @return true if a <code>DavSession</code> is referenced by the given
         * object.
         */
        private boolean containsReference(Object reference) {
            return referenceToSessionMap.containsKey(reference);
        }

        /**
         * Returns the <code>DavSession</code> that is referenced by the
         * specified reference object.
         *
         * @param reference
         * @return <code>DavSession</code> that is referenced by this reference
         * object.
         * @see #containsReference(Object)
         */
        private DavSession getSessionByReference(Object reference) {
            return (DavSession) referenceToSessionMap.get(reference);
        }

        /**
         * Retrieve the {@link Session} object for the given request.
         *
         * @param request
         * @return JCR session object used to build the <code>DavSession</code>
         * @throws DavException
         * @throws DavException in case a {@link javax.jcr.LoginException} or {@link javax.jcr.RepositoryException} occurs.
         */
        private Session getRepositorySession(WebdavRequest request) throws DavException {
            try {
                String workspaceName = request.getRequestLocator().getWorkspaceName();
                return sessionProvider.getSession(request, repository, workspaceName);
            } catch (LoginException e) {
                // LoginException results in UNAUTHORIZED,
                throw new JcrDavException(e);
            } catch (RepositoryException e) {
                // RepositoryException results in FORBIDDEN
                throw new JcrDavException(e);
            } catch (ServletException e) {
                throw new DavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        private String getUserID(DavSession session) {
            try {
                Session s = DavSessionImpl.getRepositorySession(session);
                if (s != null) {
                    return s.getUserID();
                }
            } catch (DavException e) {
                log.error(e.toString());
            }
            // fallback
            return session.toString();
        }
    }

    /**
     * Simple inner class extending the {@link HashMap}.
     */
    private static class SessionMap extends HashMap {

        public HashSet get(DavSession key) {
            return (HashSet) super.get(key);
        }

        public HashSet put(DavSession key, HashSet value) {
            return (HashSet) super.put(key, value);
        }

        public HashSet remove(DavSession key) {
            return (HashSet) super.remove(key);
        }
    }
}
