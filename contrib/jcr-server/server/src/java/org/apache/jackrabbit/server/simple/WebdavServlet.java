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
package org.apache.jackrabbit.server.simple;

import org.apache.jackrabbit.server.simple.dav.lock.SimpleLockManager;
import org.apache.jackrabbit.server.simple.dav.ResourceFactoryImpl;
import org.apache.jackrabbit.server.simple.dav.LocatorFactoryImpl;

import javax.servlet.http.*;
import javax.servlet.*;
import javax.jcr.*;
import java.io.*;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.server.AbstractWebdavServlet;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.spi.JcrDavException;
import org.apache.jackrabbit.client.RepositoryAccessServlet;

/**
 * WebdavServlet provides webdav support (level 1 and 2 complient) for repository
 * resources.
 */
public class WebdavServlet extends AbstractWebdavServlet {

    /** the default logger */
    private static final Logger log = Logger.getLogger(WebdavServlet.class);

    /** init param name of the repository prefix */
    public static final String INIT_PARAM_RESOURCE_PATH_PREFIX = "resource-path-prefix";

    /**
     * Map used to remember any webdav lock created without being reflected
     * in the underlaying repository.
     * This is needed because some clients rely on a successful locking
     * mechanism in order to perform properly (e.g. mac OSX built-in dav client)
     */
    private SimpleLockManager lockManager;

    /** the resource factory */
    private DavResourceFactory resourceFactory;

    /** the locator factory */
    private DavLocatorFactory locatorFactory;

    /** the repository prefix retrieved from config */
    private static String resourcePathPrefix;

    /**
     * Init this servlet
     *
     * @throws ServletException
     */
    public void init() throws ServletException {
        super.init();

	resourcePathPrefix = getInitParameter(INIT_PARAM_RESOURCE_PATH_PREFIX);
	log.info(INIT_PARAM_RESOURCE_PATH_PREFIX + " = " + resourcePathPrefix);

	lockManager = new SimpleLockManager();
        resourceFactory = new ResourceFactoryImpl(lockManager);
        locatorFactory = new LocatorFactoryImpl(resourcePathPrefix);
    }

    /**
     * Returns the configured path prefix
     *
     * @return resourcePathPrefix
     * @see #INIT_PARAM_RESOURCE_PATH_PREFIX
     */
    public static String getPathPrefix() {
	return resourcePathPrefix;
    }

    /**
     * Service the given request.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void service(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {

	try {
            WebdavRequest webdavRequest = new WebdavRequestImpl(request, locatorFactory);
            WebdavResponse webdavResponse = new WebdavResponseImpl(response);

            // make sure there is a authenticated user
	    DavSession session = getSession(webdavRequest);
	    if (session == null) {
		return;
	    }

	    // check matching if=header for lock-token relevant operations
	    DavResource resource = createResource(webdavRequest.getRequestLocator(), webdavRequest, webdavResponse);
	    if (resource.exists() && !webdavRequest.matchesIfHeader(resource)) {
		webdavResponse.sendError(DavServletResponse.SC_PRECONDITION_FAILED);
		return;
	    }

	    /* set cache control headers in order to deal with non-dav complient
	     * http1.1 or http1.0 proxies. >> see RFC2518 9.4.5 */
	    webdavResponse.addHeader("Pragma", "No-cache");  // http1.0
	    webdavResponse.addHeader("Cache-Control", "no-cache"); // http1.1

	    int methodCode = DavMethods.getMethodCode(webdavRequest.getMethod());
	    switch (methodCode) {
		case DavMethods.DAV_HEAD:
		case DavMethods.DAV_GET:
		    doGet(webdavRequest, webdavResponse, resource);
                case DavMethods.DAV_OPTIONS:
                    doOptions(webdavRequest, webdavResponse, resource);
                    break;
		case DavMethods.DAV_PROPFIND:
		    doPropFind(webdavRequest, webdavResponse, resource);
		    break;
		case DavMethods.DAV_PROPPATCH:
		    doPropPatch(webdavRequest, webdavResponse, resource);
		    break;
		case DavMethods.DAV_PUT:
		case DavMethods.DAV_POST:
		    doPut(webdavRequest, webdavResponse, resource);
		    break;
		case DavMethods.DAV_DELETE:
		    doDelete(webdavRequest, webdavResponse, resource);
		    break;
		case DavMethods.DAV_COPY:
		    doCopy(webdavRequest, webdavResponse, resource);
		    break;
		case DavMethods.DAV_MOVE:
		    doMove(webdavRequest, webdavResponse, resource);
		    break;
		case DavMethods.DAV_MKCOL:
		    doMkCol(webdavRequest, webdavResponse, resource);
		    break;
		case DavMethods.DAV_LOCK:
		    doLock(webdavRequest, webdavResponse, resource);
		    break;
		case DavMethods.DAV_UNLOCK:
		    doUnlock(webdavRequest, webdavResponse, resource);
		    break;
		default:
		    // GET, HEAD, TRACE......
		    super.service(request, response);
	    }
	} catch (DavException e) {
	    response.sendError(e.getErrorCode());
	}
    }

    /**
     * The MKCOL method
     *
     * @throws IOException
     */
    protected void doMkCol(WebdavRequest request, WebdavResponse response,
                           DavResource resource) throws IOException, DavException {
        // mkcol request with request.body is not supported.
        if (request.getContentLength()>0 || request.getHeader("Transfer-Encoding") != null) {
            response.sendError(DavServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }
        super.doMkCol(request, response, resource);
    }

    /**
     * Build a <code>DavResource</code> from the given path.<br>
     * Please note, that the resource may not have a corresponding element in
     * the repository in which case, {@link DavResource#exists()} will return
     * false.
     *
     * @see AbstractWebdavServlet#createResource(org.apache.jackrabbit.webdav.DavResourceLocator, org.apache.jackrabbit.webdav.WebdavRequest, org.apache.jackrabbit.webdav.WebdavResponse)
     */
    protected DavResource createResource(DavResourceLocator locator, WebdavRequest request, WebdavResponse response)
            throws DavException {
        return resourceFactory.createResource(locator, request, response);
    }

    /**
     * Retrieve the repository session for the given request object and force a header
     * authentication if necessary.
     *
     * @param request
     * @return a repository session for the given request or <code>null</code> if the
     * authentication is missing. In the latter case the authentication is
     * forces by the response code.
     * @throws DavException
     */
    private DavSession getSession(WebdavRequest request) throws DavException {
        try {
	    Credentials creds = RepositoryAccessServlet.getCredentialsFromHeader(request.getHeader(DavConstants.HEADER_AUTHORIZATION));
	    if (creds == null) {
		// generate anonymous login to gain write access
		creds = new SimpleCredentials("anonymous", "anonymous".toCharArray());
	    }
            Session repSession = RepositoryAccessServlet.getRepository().login(creds);
	    DavSession ds = new DavSessionImpl(repSession);
	    request.setDavSession(ds);
	    return ds;
        } catch (RepositoryException e) {
	    throw new JcrDavException(e);
	} catch (ServletException e) {
	    throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
    }

    /**
     * Inner class implementing the DavSession interface
     */
    private class DavSessionImpl implements DavSession {

	/** the underlaying jcr session */
        private final Session session;

	/** the lock tokens of this session */
	private final HashSet lockTokens = new HashSet();

	/**
	 * Creates a new DavSession based on a jcr session
	 * @param session
	 */
        private DavSessionImpl(Session session) {
            this.session = session;
        }

	/**
	 * @see DavSession#addReference(Object)
	 */
        public void addReference(Object reference) {
            throw new UnsupportedOperationException("No yet implemented.");
        }

	/**
	 * @see DavSession#removeReference(Object)
	 */
        public void removeReference(Object reference) {
            throw new UnsupportedOperationException("No yet implemented.");
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
	    lockTokens.add(token);
	}

	/**
	 * @see DavSession#getLockTokens()
	 */
	public String[] getLockTokens() {
	    return (String[]) lockTokens.toArray(new String[lockTokens.size()]);
	}

	/**
	 * @see DavSession#removeLockToken(String)
	 */
	public void removeLockToken(String token) {
	    lockTokens.remove(token);
	}
    }
}
