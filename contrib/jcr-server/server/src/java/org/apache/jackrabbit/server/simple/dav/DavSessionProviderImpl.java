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
package org.apache.jackrabbit.server.simple.dav;

import javax.jcr.*;
import javax.servlet.ServletException;

import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.spi.JcrDavException;
import org.apache.jackrabbit.client.RepositoryAccessServlet;

/**
 * Simple implementation of the {@link DavSessionProvider}
 * interface that uses the {@link RepositoryAccessServlet} to locate
 * credentials in the request, log into the respository, and provide
 * a {@link DavSession} to the request.
 */
public class DavSessionProviderImpl implements DavSessionProvider {

    /**
     * Acquires a DavSession. Upon success, the WebdavRequest will
     * reference that session.
     *
     * A session will not be available if credentials can not be found
     * in the request (meaning that the  request has not been
     * authenticated).
     *
     * @param request
     * @throws DavException if a problem occurred while obtaining the
     * session
     * @see DavSessionProvider#acquireSession(org.apache.jackrabbit.webdav.WebdavRequest)
     */
    public void acquireSession(WebdavRequest request) throws DavException {
        try {
            Credentials creds = RepositoryAccessServlet.getCredentialsFromHeader(request.getHeader(DavConstants.HEADER_AUTHORIZATION));
            if (creds == null) {
                // generate anonymous login to gain write access
                creds = new SimpleCredentials("anonymous", "anonymous".toCharArray());
            }
            Session repSession = RepositoryAccessServlet.getRepository().login(creds);
            DavSession ds = new DavSessionImpl(repSession);
            request.setDavSession(ds);
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
