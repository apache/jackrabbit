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
package org.apache.jackrabbit.webdav.jcr;

import org.apache.jackrabbit.server.BasicCredentialsProvider;
import org.apache.jackrabbit.server.CredentialsProvider;
import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.server.SessionProviderImpl;
import org.apache.jackrabbit.server.jcr.JCRWebdavServer;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.jcr.observation.SubscriptionManagerImpl;
import org.apache.jackrabbit.webdav.jcr.transaction.TxLockManagerImpl;
import org.apache.jackrabbit.webdav.observation.SubscriptionManager;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * JCRWebdavServerServlet provides request/response handling for the
 * JCRWebdavServer.
 * <p>
 * Implementations of this abstract class must implement the
 * {@link #getRepository()} method to access the repository.
 */
public abstract class JCRWebdavServerServlet extends AbstractWebdavServlet {

    /**
     * the default logger
     */
    private static Logger log = LoggerFactory.getLogger(JCRWebdavServerServlet.class);

    /**
     * Init parameter specifying the prefix used with the resource path.
     */
    public static final String INIT_PARAM_RESOURCE_PATH_PREFIX = "resource-path-prefix";

    /**
     * Optional 'concurrency-level' parameter defining the concurrency level
     * within the jcr-server. If the parameter is omitted the internal default
     * value (50) is used.
     */
    public final static String INIT_PARAM_CONCURRENCY_LEVEL = "concurrency-level";

    /**
     * Servlet context attribute used to store the path prefix instead of
     * having a static field with this servlet. The latter causes problems
     * when running multiple
     */
    public static final String CTX_ATTR_RESOURCE_PATH_PREFIX = "jackrabbit.webdav.jcr.resourcepath";

    private String pathPrefix;

    private JCRWebdavServer server;
    private DavResourceFactory resourceFactory;
    private DavLocatorFactory locatorFactory;
    protected TxLockManagerImpl txMgr;
    protected SubscriptionManager subscriptionMgr;

    /**
     * Initializes the servlet set reads the following parameter from the
     * servlet configuration:
     * <ul>
     * <li>resource-path-prefix: optional prefix for all resources.</li>
     * </ul>
     *
     * @throws ServletException
     */
    @Override
    public void init() throws ServletException {
        super.init();

        // set resource path prefix
        pathPrefix = getInitParameter(INIT_PARAM_RESOURCE_PATH_PREFIX);
        getServletContext().setAttribute(CTX_ATTR_RESOURCE_PATH_PREFIX, pathPrefix);
        log.debug(INIT_PARAM_RESOURCE_PATH_PREFIX + " = " + pathPrefix);

        txMgr = new TxLockManagerImpl();
        subscriptionMgr = new SubscriptionManagerImpl();
        txMgr.addTransactionListener((SubscriptionManagerImpl) subscriptionMgr);

        // todo: eventually make configurable
        resourceFactory = new DavResourceFactoryImpl(txMgr, subscriptionMgr);
        locatorFactory = new DavLocatorFactoryImpl(pathPrefix);
    }

    /**
     * Returns true if the preconditions are met. This includes validation of
     * {@link WebdavRequest#matchesIfHeader(DavResource) If header} and validation
     * of {@link org.apache.jackrabbit.webdav.transaction.TransactionConstants#HEADER_TRANSACTIONID
     * TransactionId header}. This method will also return false if the requested
     * resource resides within a different workspace as is assigned to the repository
     * session attached to the given request.
     *
     * @see AbstractWebdavServlet#isPreconditionValid(WebdavRequest, DavResource)
     */
    @Override
    protected boolean isPreconditionValid(WebdavRequest request, DavResource resource) {
        // first check matching If header
        if (!request.matchesIfHeader(resource)) {
            return false;
        }

        // test if the requested path matches to the existing session
        // this may occur if the session was retrieved from the cache.
        try {
            Session repositorySesssion = JcrDavSession.getRepositorySession(request.getDavSession());
            String reqWspName = resource.getLocator().getWorkspaceName();
            String wsName = repositorySesssion.getWorkspace().getName();
            // compare workspace names if the requested resource isn't the
            // root-collection and the request not MKWORKSPACE.
            if (DavMethods.DAV_MKWORKSPACE != DavMethods.getMethodCode(request.getMethod()) &&
                    reqWspName != null && !reqWspName.equals(wsName)) {
                return false;
            }
        } catch (DavException e) {
            log.error("Internal error: " + e.toString());
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
    @Override
    public DavSessionProvider getDavSessionProvider() {
        if (server == null) {
            Repository repository = getRepository();
            String cl = getInitParameter(INIT_PARAM_CONCURRENCY_LEVEL);
            if (cl != null) {
                try {
                    server = new JCRWebdavServer(repository, getSessionProvider(), Integer.parseInt(cl));
                } catch (NumberFormatException e) {
                    log.debug("Invalid value '" + cl+ "' for init-param 'concurrency-level'. Using default instead.");
                    server = new JCRWebdavServer(repository, getSessionProvider());
                }
            } else {
                server = new JCRWebdavServer(repository, getSessionProvider());
            }
        }
        return server;
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     *
     * @see AbstractWebdavServlet#setDavSessionProvider(DavSessionProvider)
     */
    @Override
    public void setDavSessionProvider(DavSessionProvider davSessionProvider) {
        throw new UnsupportedOperationException("Not implemented. DavSession(s) are provided by the 'JCRWebdavServer'");
    }

    /**
     * Returns the <code>DavLocatorFactory</code>
     *
     * @see AbstractWebdavServlet#getLocatorFactory()
     */
    @Override
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
    @Override
    public void setLocatorFactory(DavLocatorFactory locatorFactory) {
        this.locatorFactory = locatorFactory;
    }

    /**
     * Returns the <code>DavResourceFactory</code>.
     *
     * @see AbstractWebdavServlet#getResourceFactory()
     */
    @Override
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
    @Override
    public void setResourceFactory(DavResourceFactory resourceFactory) {
        this.resourceFactory = resourceFactory;
    }

    /**
     * Modified variant needed for JCR move and copy that isn't compliant to
     * WebDAV. The latter requires both methods to fail if the destination already
     * exists and Overwrite is set to F (false); in JCR however this depends on
     * the node type characteristics of the parent (SNSiblings allowed or not).
     *
     * @param destResource destination resource to be validated.
     * @param request The webdav request
     * @param checkHeader flag indicating if the destination header must be present.
     * @return status code indicating whether the destination is valid.
     */
    @Override
    protected int validateDestination(DavResource destResource, WebdavRequest request, boolean checkHeader)
            throws DavException {

        if (checkHeader) {
            String destHeader = request.getHeader(HEADER_DESTINATION);
            if (destHeader == null || "".equals(destHeader)) {
                return DavServletResponse.SC_BAD_REQUEST;
            }
        }
        if (destResource.getLocator().equals(request.getRequestLocator())) {
            return DavServletResponse.SC_FORBIDDEN;
        }

        int status;
        if (destResource.exists()) {
            if (request.isOverwrite()) {
                // matching if-header required for existing resources
                if (!request.matchesIfHeader(destResource)) {
                    return DavServletResponse.SC_PRECONDITION_FAILED;
                } else {
                    // overwrite existing resource
                    destResource.getCollection().removeMember(destResource);
                    status = DavServletResponse.SC_NO_CONTENT;
                }
            } else {
              /* NO overwrite header:

                 but, instead of return the 412 Precondition-Failed code required
                 by the WebDAV specification(s) leave the validation to the
                 JCR repository.
               */
                status = DavServletResponse.SC_CREATED;
            }

        } else {
            // destination does not exist >> copy/move can be performed
            status = DavServletResponse.SC_CREATED;
        }
        return status;
    }

    /**
     * Returns the configured path prefix
     *
     * @param ctx The servlet context.
     * @return resourcePathPrefix
     * @see #INIT_PARAM_RESOURCE_PATH_PREFIX
     */
    public static String getPathPrefix(ServletContext ctx) {
        return (String) ctx.getAttribute(CTX_ATTR_RESOURCE_PATH_PREFIX);
    }

    /**
     * Returns the repository to be used by this servlet.
     *
     * @return the JCR repository to be used by this servlet
     */
    protected abstract Repository getRepository();

    /**
     * Returns a new instanceof <code>BasicCredentialsProvider</code>.
     *
     * @return a new credentials provider
     */
    protected CredentialsProvider getCredentialsProvider() {
        return new BasicCredentialsProvider(getInitParameter(INIT_PARAM_MISSING_AUTH_MAPPING));
    }

    /**
     * Returns a new instanceof <code>SessionProviderImpl</code>.
     *
     * @return a new session provider
     */
    protected SessionProvider getSessionProvider() {
        return new SessionProviderImpl(getCredentialsProvider());
    }
}
