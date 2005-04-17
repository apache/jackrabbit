/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.client.RepositoryAccessServlet;

import javax.jcr.*;
import javax.servlet.ServletException;
import java.util.HashMap;
import java.util.HashSet;
/**
 * <code>JCRWebdavServer</code>...
 */
public class JCRWebdavServer implements DavSessionProvider {

    /** the default logger */
    private static Logger log = Logger.getLogger(JCRWebdavServer.class);

    /** the session cache */
    private final SessionCache cache = new SessionCache();

    /** the jcr repository */
    private final Repository repository;

    /**
     * Creates a new JCRWebdavServer that operates on the given repository.
     *
     * @param repository
     */
    public JCRWebdavServer(Repository repository) {
	this.repository = repository;
    }

    //---------------------------------------< DavSessionProvider interface >---
    /**
     * Acquires a DavSession either from the session cache or creates a new
     * one by login to the repository.
     * Upon success, the WebdavRequest will reference that session.
     *
     * @param request
     * @throws DavException if no session could be obtained.
     * @see DavSessionProvider#acquireSession(org.apache.jackrabbit.webdav.WebdavRequest)
     */
    public void acquireSession(WebdavRequest request)
            throws DavException {
        DavSession session = cache.get(request);
	request.setDavSession(session);
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
    private class DavSessionImpl implements DavSession {

	/** the underlaying jcr session */
        private final Session session;

        /**
         * Private constructor.
         *
         * @param request
         * @throws DavException in case a {@link javax.jcr.LoginException} or {@link javax.jcr.RepositoryException} occurs.
         */
        private DavSessionImpl(DavServletRequest request) throws DavException {
            try {
                String workspaceName = request.getRequestLocator().getWorkspaceName();
		Credentials creds = RepositoryAccessServlet.getCredentialsFromHeader(request.getHeader(DavConstants.HEADER_AUTHORIZATION));
                session = repository.login(creds, workspaceName);
            } catch (LoginException e) {
                // LoginException results in UNAUTHORIZED,
                throw new JcrDavException(e);
            } catch (RepositoryException e) {
                // RepositoryException results in FORBIDDEN
                throw new JcrDavException(e);
            } catch (ServletException e) {
		throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	    }
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
         * from the internal cache and the underlaying session is released by calling
         * {@link javax.jcr.Session#logout()}.
         *
         * @see DavSession#removeReference(Object)
         */
        public void removeReference(Object reference) {
            cache.removeReference(this, reference);
        }

        /**
         * @see DavSession#getRepositorySession()
         */
        public Session getRepositorySession() {
            return session;
        }

	/**
	 * @see DavSession#addLockToken(String)
	 */
	public void addLockToken(String token) {
	    session.addLockToken(token);
	}

	/**
	 * @see DavSession#getLockTokens()
	 */
	public String[] getLockTokens() {
	    return session.getLockTokens();
	}

	/**
	 * @see DavSession#removeLockToken(String)
	 */
	public void removeLockToken(String token) {
	    session.removeLockToken(token);
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

            if ((lockToken != null || txId != null) && subscriptionId != null) {
                throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "Ambiguous headers: either TransactionId/Lock-Token or SubscriptionId can be present, not both.");
            }

            DavSession session = null;
            // try to retrieve a cached session
            if (lockToken != null && containsReference(lockToken)) {
                session = getSessionByReference(lockToken);
            } else if (txId != null && containsReference(txId)) {
                session = getSessionByReference(txId);
            } else if (subscriptionId != null && containsReference(subscriptionId)) {
                session = getSessionByReference(subscriptionId);
            }
            // no cached session present -> create new one.
            if (session == null) {
                session = new DavSessionImpl(request);
                sessionMap.put(session, new HashSet());
                log.info("login: User '" + session.getRepositorySession().getUserID() + "' logged in.");
            } else {
                log.info("login: Retrieved cached session for user '" + session.getRepositorySession().getUserID() + "'");
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
                    log.info("Removed reference " + reference + " to session " + session);
                    referenceToSessionMap.remove(reference);
                } else {
                    log.warn("Failed to remove reference " + reference + " to session " + session);
                }
                if (referenceSet.isEmpty()) {
                    log.info("No more references present on webdav session -> clean up.");
                    sessionMap.remove(session);
                    log.info("Login: User '" + session.getRepositorySession().getUserID() + "' logged out");
                    session.getRepositorySession().logout();
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