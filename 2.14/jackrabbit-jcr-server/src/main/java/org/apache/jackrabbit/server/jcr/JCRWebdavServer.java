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

import org.apache.jackrabbit.commons.webdav.JcrRemotingConstants;
import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.spi.commons.SessionExtensions;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.header.IfHeader;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.jcr.JcrDavSession;
import org.apache.jackrabbit.webdav.util.LinkHeaderFieldParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <code>JCRWebdavServer</code>...
 */
public class JCRWebdavServer implements DavSessionProvider {

    /** the default logger */
    private static Logger log = LoggerFactory.getLogger(JCRWebdavServer.class);

    /** the session cache */
    private final SessionCache cache;

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
        cache = new SessionCache();
    }

    /**
     * Creates a new JCRWebdavServer that operates on the given repository.
     *
     * @param repository
     * @param concurrencyLevel 
     */
    public JCRWebdavServer(Repository repository, SessionProvider sessionProvider, int concurrencyLevel) {
        this.repository = repository;
        this.sessionProvider = sessionProvider;
        cache = new SessionCache(concurrencyLevel);
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

        private static final int CONCURRENCY_LEVEL_DEFAULT = 50;
        private static final int INITIAL_CAPACITY = 50;
    	private static final int INITIAL_CAPACITY_REF_TO_SESSION = 3 * INITIAL_CAPACITY;
    	
        private ConcurrentMap<DavSession, Set<Object>> sessionMap;
        private ConcurrentMap<Object, DavSession> referenceToSessionMap;

        /**
         * Create a new session cache with the {@link #CONCURRENCY_LEVEL_DEFAULT default concurrency level}.
         */
        private SessionCache() {
            this(CONCURRENCY_LEVEL_DEFAULT);
        }

        /**
         * Create a new session cache with the specified the level of concurrency
         * for this server.
         * 
         * @param cacheConcurrencyLevel A positive int value specifying the
         * concurrency level of the server.
         */
        private SessionCache(int cacheConcurrencyLevel) {
        	sessionMap = new ConcurrentHashMap<DavSession, Set<Object>>(INITIAL_CAPACITY, .75f, cacheConcurrencyLevel);
        	referenceToSessionMap = new ConcurrentHashMap<Object, DavSession>(INITIAL_CAPACITY_REF_TO_SESSION, .75f, cacheConcurrencyLevel);
        }
        
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
                for (Iterator<String> it = ifHeader.getAllTokens(); it.hasNext();) {
                    String token = it.next();
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
                
                // TODO: review again if using ConcurrentMap#putIfAbsent() was more appropriate.
                sessionMap.put(session, new HashSet<Object>());
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
            Set<Object> referenceSet = sessionMap.get(session);
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
            Set<Object> referenceSet = sessionMap.get(session);
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
                        // should not occur, since we originally built a
                        // DavSessionImpl that wraps a repository session.
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
            return referenceToSessionMap.get(reference);
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
                String workspaceName = null;
                if (DavMethods.DAV_MKWORKSPACE != DavMethods.getMethodCode(request.getMethod())) {
                    workspaceName = request.getRequestLocator().getWorkspaceName();
                }

                Session session = sessionProvider.getSession(
                        request, repository, workspaceName);

                // extract information from Link header fields
                LinkHeaderFieldParser lhfp =
                        new LinkHeaderFieldParser(request.getHeaders("Link"));
                setJcrUserData(session, lhfp);
                setSessionIdentifier(session, lhfp);

                return session;
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

        /**
         * Find first link relation for JCR user data and set it as
         * the user data of the observation manager of the given session.
         */
        private void setJcrUserData(
                Session session, LinkHeaderFieldParser lhfp)
                throws RepositoryException {
            String data = null;

            // extract User Data string from RFC 2397 "data" URI
            // only supports the simple case of "data:,..." for now
            String target = lhfp.getFirstTargetForRelation(
                    JcrRemotingConstants.RELATION_USER_DATA);
            if (target != null) {
                try {
                    URI uri = new URI(target);
                    // Poor Man's data: URI parsing
                    if ("data".equalsIgnoreCase(uri.getScheme())) {
                        String sspart = uri.getRawSchemeSpecificPart();
                        if (sspart.startsWith(",")) {
                            data = Text.unescape(sspart.substring(1));
                        }
                    }
                } catch (URISyntaxException ex) {
                    // not a URI, skip
                }
            }

            try {
                session.getWorkspace().getObservationManager().setUserData(data);
            } catch (UnsupportedRepositoryOperationException ignore) {
            }
        }

        /**
         * Find first link relation for remote session identifier and set
         * it as an attribute of the given session.
         */
        private void setSessionIdentifier(
                Session session, LinkHeaderFieldParser lhfp) {
            if (session instanceof SessionExtensions) {
                String name = JcrRemotingConstants.RELATION_REMOTE_SESSION_ID;
                String id = lhfp.getFirstTargetForRelation(name);
                ((SessionExtensions) session).setAttribute(name, id);
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
}
