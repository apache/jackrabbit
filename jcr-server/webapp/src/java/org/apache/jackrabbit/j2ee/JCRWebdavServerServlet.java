/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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

import org.apache.jackrabbit.server.AbstractWebdavServlet;
import org.apache.jackrabbit.server.BasicCredentialsProvider;
import org.apache.jackrabbit.server.SessionProviderImpl;
import org.apache.jackrabbit.webdav.jcr.JcrDavSession;
import org.apache.jackrabbit.server.jcr.JCRWebdavServer;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.jcr.DavLocatorFactoryImpl;
import org.apache.jackrabbit.webdav.jcr.DavResourceFactoryImpl;
import org.apache.jackrabbit.webdav.jcr.observation.SubscriptionManagerImpl;
import org.apache.jackrabbit.webdav.jcr.transaction.TxLockManagerImpl;
import org.apache.jackrabbit.webdav.observation.SubscriptionManager;
import org.apache.log4j.Logger;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;

/**
 * JCRWebdavServerServlet provides request/response handling for the JCRWebdavServer.
 */
public class JCRWebdavServerServlet extends AbstractWebdavServlet implements DavConstants {

    /**
     * the default logger
     */
    private static Logger log = Logger.getLogger(JCRWebdavServerServlet.class);

    /**
     * Init parameter specifying the prefix used with the resource path.
     */
    public static final String INIT_PARAM_RESOURCE_PATH_PREFIX = "resource-path-prefix";

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

    /** the 'missing-auth-mapping' init parameter */
    public final static String INIT_PARAM_MISSING_AUTH_MAPPING = "missing-auth-mapping";

    /**
     * Servlet context attribute used to store the path prefix instead of
     * having a static field with this servlet. The latter causes problems
     * when running multiple
     */
    public static final String CTX_ATTR_RESOURCE_PATH_PREFIX = "jackrabbit.webdav.jcr.resourcepath";

    private String pathPrefix;
    private String authenticate_header;

    private JCRWebdavServer server;
    private DavResourceFactory resourceFactory;
    private DavLocatorFactory locatorFactory;
    private TxLockManagerImpl txMgr;
    private SubscriptionManager subscriptionMgr;

    /**
     * Initializes the servlet set reads the following parameter from the
     * servlet configuration:
     * <ul>
     * <li>resource-path-prefix: optional prefix for all resources.</li>
     * </ul>
     *
     * @throws ServletException
     */
    public void init() throws ServletException {
        super.init();

        // set resource path prefix
        pathPrefix = getInitParameter(INIT_PARAM_RESOURCE_PATH_PREFIX);
        getServletContext().setAttribute(CTX_ATTR_RESOURCE_PATH_PREFIX, pathPrefix);
        log.debug(INIT_PARAM_RESOURCE_PATH_PREFIX + " = " + pathPrefix);

        authenticate_header = getInitParameter(INIT_PARAM_AUTHENTICATE_HEADER);
        if (authenticate_header == null) {
            authenticate_header = DEFAULT_AUTHENTICATE_HEADER;
        }
        log.debug(INIT_PARAM_AUTHENTICATE_HEADER + " = " + authenticate_header);

        txMgr = new TxLockManagerImpl();
        subscriptionMgr = new SubscriptionManagerImpl();

        // todo: ev. make configurable
        resourceFactory = new DavResourceFactoryImpl(txMgr, subscriptionMgr);
        locatorFactory = new DavLocatorFactoryImpl(pathPrefix);
    }

    /**
     * Returns true if the preconditions are met. This includes validation of
     * {@link WebdavRequest#matchesIfHeader(DavResource) If header} and validation
     * of {@link org.apache.jackrabbit.webdav.transaction.TransactionConstants#HEADER_TRANSACTIONID
     * TransactionId header}. This method will also return false if the requested
     * resource resides within a differenct workspace as is assigned to the repository
     * session attached to the given request.
     *
     * @see AbstractWebdavServlet#isPreconditionValid(WebdavRequest, DavResource)
     */
    protected boolean isPreconditionValid(WebdavRequest request, DavResource resource) {
        // first check matching If header
        if (!request.matchesIfHeader(resource)) {
            return false;
        }

        // test if the requested path matches to the existing session
        // this may occur if the session was retrieved from the cache.
        try {
            Session repositorySesssion = JcrDavSession.getRepositorySession(request.getDavSession());
            String wsName = repositorySesssion.getWorkspace().getName();
            if (!resource.getLocator().isSameWorkspace(wsName)) {
                return false;
            }
        } catch (DavException e) {
            log.error(e);
            return false;
        }


        // make sure, the TransactionId header is valid
        String txId = request.getTransactionId();
        return txId == null || txMgr.hasLock(txId, resource);
    }

    /**
     * Returns the <code>DavSessionProvider</code>
     *
     * @return server
     * @see AbstractWebdavServlet#getDavSessionProvider()
     */
    public DavSessionProvider getDavSessionProvider() {
        if (server == null) {
            Repository repository = RepositoryAccessServlet.getRepository(getServletContext());
            server = new JCRWebdavServer(repository, new SessionProviderImpl(
                    new BasicCredentialsProvider(
                            getInitParameter(INIT_PARAM_MISSING_AUTH_MAPPING)))
            );
        }
        return server;
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     *
     * @see AbstractWebdavServlet#setDavSessionProvider(DavSessionProvider)
     */
    public void setDavSessionProvider(DavSessionProvider davSessionProvider) {
        throw new UnsupportedOperationException("Not implemented. DavSession(s) are provided by the 'JCRWebdavServer'");
    }

    /**
     * Returns the <code>DavLocatorFactory</code>
     *
     * @see AbstractWebdavServlet#getLocatorFactory()
     */
    public DavLocatorFactory getLocatorFactory() {
        if (locatorFactory == null) {
            locatorFactory = new DavLocatorFactoryImpl(pathPrefix);
        }
        return locatorFactory;
    }

    /**
     * Sets the <code>DavLocatorFactory</code>
     *
     * @see AbstractWebdavServlet#setLocatorFactory(DavLocatorFactory)
     */
    public void setLocatorFactory(DavLocatorFactory locatorFactory) {
        this.locatorFactory = locatorFactory;
    }

    /**
     * Returns the <code>DavResourceFactory</code>.
     *
     * @see AbstractWebdavServlet#getResourceFactory()
     */
    public DavResourceFactory getResourceFactory() {
        if (resourceFactory == null) {
            resourceFactory = new DavResourceFactoryImpl(txMgr, subscriptionMgr);
        }
        return resourceFactory;
    }

    /**
     * Sets the <code>DavResourceFactory</code>.
     *
     * @see AbstractWebdavServlet#setResourceFactory(org.apache.jackrabbit.webdav.DavResourceFactory)
     */
    public void setResourceFactory(DavResourceFactory resourceFactory) {
        this.resourceFactory = resourceFactory;
    }

    /**
     * Returns the init param of the servlet configuration or
     * {@link #DEFAULT_AUTHENTICATE_HEADER} as default value.
     *
     * @return corresponding init parameter or {@link #DEFAULT_AUTHENTICATE_HEADER}.
     * @see #INIT_PARAM_AUTHENTICATE_HEADER
     */
    public String getAuthenticateHeaderValue() {
        return authenticate_header;
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
}
