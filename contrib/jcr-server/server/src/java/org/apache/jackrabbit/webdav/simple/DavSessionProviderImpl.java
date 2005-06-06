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
package org.apache.jackrabbit.webdav.simple;

import javax.jcr.*;
import javax.servlet.ServletException;

import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.server.CredentialsProvider;
import org.apache.jackrabbit.server.SessionProvider;

/**
 * Simple implementation of the {@link DavSessionProvider}
 * interface that uses a {@link CredentialsProvider} to locate
 * credentials in the request, log into the respository, and provide
 * a {@link DavSession} to the request.
 */
public class DavSessionProviderImpl implements DavSessionProvider {

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
     * @throws DavException if a problem occurred while obtaining the session
     * @see DavSessionProvider#attachSession(org.apache.jackrabbit.webdav.WebdavRequest)
     */
    public boolean attachSession(WebdavRequest request) throws DavException {
        try {
            // login to repository
            Session repSession = sesProvider.getSession(request, repository, null);
            if (repSession == null) {
                return false;
            }
            DavSession ds = new DavSessionImpl(repSession);
            request.setDavSession(ds);
            return true;
        } catch (LoginException e) {
	    throw new JcrDavException(e);
        } catch (RepositoryException e) {
	    throw new JcrDavException(e);
	} catch (ServletException e) {
	    throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
    }

    /**
     * Only removes the <code>DavSession</code> object from the given request object.
     * No further actions required, since <code>DavSessionImpl</code> does not
     * allow to keep track of references to it.
     *
     * @param request
     * @see DavSessionProvider#releaseSession(org.apache.jackrabbit.webdav.WebdavRequest)
     */
    public void releaseSession(WebdavRequest request) {
        request.setDavSession(null);
    }
}
