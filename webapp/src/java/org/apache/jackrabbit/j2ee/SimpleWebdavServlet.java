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
package org.apache.jackrabbit.j2ee;

import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.config.ConfigParser;
import org.apache.commons.chain.impl.CatalogFactoryBase;
import org.apache.jackrabbit.server.CredentialsProvider;
import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.server.SessionProviderImpl;
import org.apache.jackrabbit.server.AbstractWebdavServlet;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavResponse;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.SimpleLockManager;
import org.apache.jackrabbit.webdav.simple.DavSessionProviderImpl;
import org.apache.jackrabbit.webdav.simple.LocatorFactoryImpl;
import org.apache.jackrabbit.webdav.simple.ResourceFactoryImpl;
import org.apache.log4j.Logger;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;

/**
 * WebdavServlet provides webdav support (level 1 and 2 complient) for repository
 * resources.
 */
public class SimpleWebdavServlet extends AbstractWebdavServlet {

    /**
     * the default logger
     */
    private static final Logger log = Logger.getLogger(SimpleWebdavServlet.class);

    /**
     * init param name of the repository prefix
     */
    public static final String INIT_PARAM_RESOURCE_PATH_PREFIX = "resource-path-prefix";

    /**
     * init param file of the commons chain catalog
     */
    public static final String INIT_PARAM_CHAIN_CATALOG = "chain-catalog";

    /**
     * Name of the optional init parameter that defines the value of the
     * 'WWW-Authenticate' header.<p/>
     * If the parameter is omitted the default value
     * {@link #DEFAULT_AUTHENTICATE_HEADER "Basic Realm=Jackrabbit Webdav Server"}
     * is used.
     *
     * @see #getAuthenticateHeaderValue()
     */
    public static final String INIT_PARAM_AUTHENTICATE_HEADER = "authenticate-header";

    public static final String CTX_ATTR_RESOURCE_PATH_PREFIX = "jcr.webdav.resourcepath";

    /**
     * the resource path prefix
     */
    private String resourcePathPrefix;

    /**
     * the chain catalog for i/o operations
     */
    private Catalog chainCatalog;

    /**
     * Header value as specified in the {@link #INIT_PARAM_AUTHENTICATE_HEADER} parameter.
     */
    private String authenticate_header;

    /**
     * Map used to remember any webdav lock created without being reflected
     * in the underlaying repository.
     * This is needed because some clients rely on a successful locking
     * mechanism in order to perform properly (e.g. mac OSX built-in dav client)
     */
    private LockManager lockManager;

    /**
     * the resource factory
     */
    private DavResourceFactory resourceFactory;

    /**
     * the locator factory
     */
    private DavLocatorFactory locatorFactory;

    /**
     * the jcr repository
     */
    private Repository repository;

    /**
     * the webdav session provider
     */
    private DavSessionProvider davSessionProvider;

    /**
     * the repository session provider
     */
    private SessionProvider sessionProvider;

    /**
     * Init this servlet
     *
     * @throws ServletException
     */
    public void init() throws ServletException {
        super.init();

        resourcePathPrefix = getInitParameter(INIT_PARAM_RESOURCE_PATH_PREFIX);
        if (resourcePathPrefix == null) {
            log.debug("Missing path prefix > setting to empty string.");
            resourcePathPrefix = "";
        } else if (resourcePathPrefix.endsWith("/")) {
            log.debug("Path prefix ends with '/' > removing trailing slash.");
            resourcePathPrefix = resourcePathPrefix.substring(0, resourcePathPrefix.length() - 1);
        }
        getServletContext().setAttribute(CTX_ATTR_RESOURCE_PATH_PREFIX, resourcePathPrefix);
        log.info(INIT_PARAM_RESOURCE_PATH_PREFIX + " = '" + resourcePathPrefix + "'");

        try {
            String chain = getInitParameter(INIT_PARAM_CHAIN_CATALOG);
            URL chainUrl = getServletContext().getResource(chain);
            ConfigParser parser = new ConfigParser();
            parser.parse(chainUrl);
            chainCatalog = CatalogFactoryBase.getInstance().getCatalog();
        } catch (Exception e) {
            throw new ServletException(e);
        }
        log.info(INIT_PARAM_CHAIN_CATALOG + " = '" + chainCatalog + "'");

        authenticate_header = getInitParameter(INIT_PARAM_AUTHENTICATE_HEADER);
        if (authenticate_header == null) {
            authenticate_header = DEFAULT_AUTHENTICATE_HEADER;
        }
        log.info("WWW-Authenticate header = '" + authenticate_header + "'");
    }

    /**
     * Executes the respective method in the given webdav context.
     * The method is overridden since not all webdav methods should be
     * supported by this servlet.
     *
     * @param request
     * @param response
     * @param method
     * @param resource
     * @return
     * @throws ServletException
     * @throws IOException
     * @throws DavException
     */
    protected boolean execute(WebdavRequest request, WebdavResponse response,
                              int method, DavResource resource)
            throws ServletException, IOException, DavException {
        /* set cache control headers in order to deal with non-dav complient
        * http1.1 or http1.0 proxies. >> see RFC2518 9.4.5 */
        response.addHeader("Pragma", "No-cache");  // http1.0
        response.addHeader("Cache-Control", "no-cache"); // http1.1

        switch (method) {
            case DavMethods.DAV_HEAD:
                doHead(request, response, resource);
                break;
            case DavMethods.DAV_GET:
                doGet(request, response, resource);
                break;
            case DavMethods.DAV_OPTIONS:
                doOptions(request, response, resource);
                break;
            case DavMethods.DAV_PROPFIND:
                doPropFind(request, response, resource);
                break;
            case DavMethods.DAV_PROPPATCH:
                doPropPatch(request, response, resource);
                break;
            case DavMethods.DAV_PUT:
                doPut(request, response, resource);
                break;
            case DavMethods.DAV_POST:
                doPost(request, response, resource);
                break;
            case DavMethods.DAV_DELETE:
                doDelete(request, response, resource);
                break;
            case DavMethods.DAV_COPY:
                doCopy(request, response, resource);
                break;
            case DavMethods.DAV_MOVE:
                doMove(request, response, resource);
                break;
            case DavMethods.DAV_MKCOL:
                doMkCol(request, response, resource);
                break;
            case DavMethods.DAV_LOCK:
                doLock(request, response, resource);
                break;
            case DavMethods.DAV_UNLOCK:
                doUnlock(request, response, resource);
                break;
            default:
                // any other method
                return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isPreconditionValid(WebdavRequest request,
                                          DavResource resource) {
        return !resource.exists() || request.matchesIfHeader(resource);
    }

    /**
     * The MKCOL method
     *
     * @throws IOException
     */
    protected void doMkCol(WebdavRequest request, WebdavResponse response,
                           DavResource resource) throws IOException, DavException {
        // mkcol request with request.body is not supported.
        if (request.getContentLength() > 0 || request.getHeader("Transfer-Encoding") != null) {
            response.sendError(DavServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }
        super.doMkCol(request, response, resource);
    }

    /**
     * Returns the configured path prefix
     *
     * @return resourcePathPrefix
     * @see #INIT_PARAM_RESOURCE_PATH_PREFIX
     */
    public String getPathPrefix() {
        return resourcePathPrefix;
    }

    /**
     * Returns the configured path prefix
     *
     * @return resourcePathPrefix
     * @see #INIT_PARAM_RESOURCE_PATH_PREFIX
     */
    public static String getPathPrefix(ServletContext ctx) {
        return (String) ctx.getAttribute(CTX_ATTR_RESOURCE_PATH_PREFIX);
    }

    /**
     * Returns the <code>DavLocatorFactory</code>. If no locator factory has
     * been set or created a new instance of {@link org.apache.jackrabbit.webdav.simple.LocatorFactoryImpl} is
     * returned.
     *
     * @return the locator factory
     * @see AbstractWebdavServlet#getLocatorFactory()
     */
    public DavLocatorFactory getLocatorFactory() {
        if (locatorFactory == null) {
            locatorFactory = new LocatorFactoryImpl(resourcePathPrefix);
        }
        return locatorFactory;
    }

    /**
     * Sets the <code>DavLocatorFactory</code>.
     *
     * @param locatorFactory
     * @see AbstractWebdavServlet#setLocatorFactory(DavLocatorFactory)
     */
    public void setLocatorFactory(DavLocatorFactory locatorFactory) {
        this.locatorFactory = locatorFactory;
    }

    /**
     * Returns the <code>LockManager</code>. If no lock manager has
     * been set or created a new instance of {@link SimpleLockManager} is
     * returned.
     *
     * @return the lock manager
     */
    public LockManager getLockManager() {
        if (lockManager == null) {
            lockManager = new SimpleLockManager();
        }
        return lockManager;
    }

    /**
     * Sets the <code>LockManager</code>.
     *
     * @param lockManager
     */
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    /**
     * Returns the <code>DavResourceFactory</code>. If no request factory has
     * been set or created a new instance of {@link ResourceFactoryImpl} is
     * returned.
     *
     * @return the resource factory
     * @see org.apache.jackrabbit.server.AbstractWebdavServlet#getResourceFactory()
     */
    public DavResourceFactory getResourceFactory() {
        if (resourceFactory == null) {
            resourceFactory = new ResourceFactoryImpl(getLockManager());
        }
        return resourceFactory;
    }

    /**
     * Sets the <code>DavResourceFactory</code>.
     *
     * @param resourceFactory
     * @see AbstractWebdavServlet#setResourceFactory(org.apache.jackrabbit.webdav.DavResourceFactory)
     */
    public void setResourceFactory(DavResourceFactory resourceFactory) {
        this.resourceFactory = resourceFactory;
    }

    /**
     * Returns the <code>SessionProvider</code>. If no session provider has been
     * set or created a new instance of {@link SessionProviderImpl} that extracts
     * credentials from the request's <code>Authorization</code> header is
     * returned.
     *
     * @return the session provider
     * @see RepositoryAccessServlet#getCredentialsFromHeader(ServletContext, String)
     */
    public synchronized SessionProvider getSessionProvider() {
        if (sessionProvider == null) {
            CredentialsProvider cp = new CredentialsProvider() {
                public Credentials getCredentials(HttpServletRequest request) throws LoginException, ServletException {
                    return RepositoryAccessServlet.getCredentialsFromHeader(getServletContext(), request.getHeader(DavConstants.HEADER_AUTHORIZATION));
                }
            };
            sessionProvider = new SessionProviderImpl(cp);
        }
        return sessionProvider;
    }

    /**
     * Sets the <code>SessionProvider</code>.
     *
     * @param sessionProvider
     */
    public synchronized void setSessionProvider(SessionProvider sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    /**
     * Returns the <code>DavSessionProvider</code>. If no session provider has
     * been set or created a new instance of {@link DavSessionProviderImpl}
     * is returned.
     *
     * @return the session provider
     * @see org.apache.jackrabbit.server.AbstractWebdavServlet#getDavSessionProvider()
     */
    public synchronized DavSessionProvider getDavSessionProvider() {
        if (davSessionProvider == null) {
            davSessionProvider =
                new DavSessionProviderImpl(getRepository(), getSessionProvider());
        }
        return davSessionProvider;
    }

    /**
     * Sets the <code>DavSessionProvider</code>.
     *
     * @param sessionProvider
     * @see AbstractWebdavServlet#setDavSessionProvider(org.apache.jackrabbit.webdav.DavSessionProvider)
     */
    public synchronized void setDavSessionProvider(DavSessionProvider sessionProvider) {
        this.davSessionProvider = sessionProvider;
    }

    /**
     * Returns the header value retrieved from the {@link #INIT_PARAM_AUTHENTICATE_HEADER}
     * init parameter. If the parameter is missing, the value defaults to
     * {@link #DEFAULT_AUTHENTICATE_HEADER}.
     *
     * @return the header value retrieved from the corresponding init parameter
     * or {@link #DEFAULT_AUTHENTICATE_HEADER}.
     * @see org.apache.jackrabbit.server.AbstractWebdavServlet#getAuthenticateHeaderValue()
     */
    public String getAuthenticateHeaderValue() {
        return authenticate_header;
    }

    /**
     * Returns the <code>Repository</code>. If no repository has been set or
     * created the repository initialized by <code>RepositoryAccessServlet</code>
     * is returned.
     *
     * @return repository
     * @see RepositoryAccessServlet#getRepository(ServletContext)
     */
    public Repository getRepository() {
        if (repository == null) {
            repository = RepositoryAccessServlet.getRepository(getServletContext());
            if (repository == null) {
                throw new IllegalStateException("Repository could not be retrieved. Check config of 'RepositoryAccessServlet'.");
            }
        }
        return repository;
    }

    /**
     * Sets the <code>Repository</code>.
     *
     * @param repository
     */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
}
