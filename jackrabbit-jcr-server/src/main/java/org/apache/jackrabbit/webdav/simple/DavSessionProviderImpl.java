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
package org.apache.jackrabbit.webdav.simple;

import org.apache.jackrabbit.server.CredentialsProvider;
import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

/**
 * Simple implementation of the {@link org.apache.jackrabbit.webdav.DavSessionProvider}
 * interface that uses a {@link CredentialsProvider} to locate
 * credentials in the request, log into the respository, and provide
 * a {@link org.apache.jackrabbit.webdav.DavSession} to the request.
 */
public class DavSessionProviderImpl implements DavSessionProvider {

    private static Logger log = LoggerFactory.getLogger(DavSessionProviderImpl.class);

    /**
     * the repository
     */
    private final Repository repository;

    /**
     * the credentials provider
     */
    private final SessionProvider sesProvider;

    /**
     * Creates a new DavSessionProviderImpl
     * @param rep
     * @param sesProvider
     */
    public DavSessionProviderImpl(Repository rep, SessionProvider sesProvider) {
        this.repository = rep;
        this.sesProvider = sesProvider;
    }

    /**
     * Acquires a DavSession. Upon success, the WebdavRequest will
     * reference that session.
     *
     * A session will not be available if an exception is thrown.
     *
     * @param request
     * @throws org.apache.jackrabbit.webdav.DavException if a problem occurred while obtaining the session
     * @see DavSessionProvider#attachSession(org.apache.jackrabbit.webdav.WebdavRequest)
     */
    public boolean attachSession(WebdavRequest request) throws DavException {
        try {
            // retrieve the workspace name
            String workspaceName = request.getRequestLocator().getWorkspaceName();
            // empty workspaceName rather means default -> must be 'null'
            if (workspaceName != null && "".equals(workspaceName)) {
                workspaceName = null;
            }
            // login to repository
            Session repSession = sesProvider.getSession(request, repository, workspaceName);
            if (repSession == null) {
                log.debug("Could not to retrieve a repository session.");
                return false;
            }
            DavSession ds = new DavSessionImpl(repSession);
            log.debug("Attaching session '"+ ds + "' to request '" + request + "'");
            request.setDavSession(ds);
            return true;
        } catch (NoSuchWorkspaceException e) {
            // the default error-code for NoSuchWorkspaceException is 409 conflict
            // which seems not appropriate here
            throw new JcrDavException(e, DavServletResponse.SC_NOT_FOUND);
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        } catch (ServletException e) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Only removes the <code>DavSession</code> object from the given request object.
     * and remove all the lock tokens from the underlying repository session
     * in order make sure they can be reset when attaching a session to the
     * next request. Finally the session provider is informed, that the
     * session is no longer used.
     *
     * @param request
     * @see DavSessionProvider#releaseSession(org.apache.jackrabbit.webdav.WebdavRequest)
     */
    public void releaseSession(WebdavRequest request) {
        DavSession ds = request.getDavSession();
        if (ds != null && ds instanceof DavSessionImpl) {
            Session repSession = ((DavSessionImpl)ds).getRepositorySession();
            for (String lockToken : repSession.getLockTokens()) {
                repSession.removeLockToken(lockToken);
            }
            sesProvider.releaseSession(repSession);
            log.debug("Releasing session '"+ ds + "' from request '" + request + "'");
        } // else : session is null. nothing to be done.
        request.setDavSession(null);
    }
}
