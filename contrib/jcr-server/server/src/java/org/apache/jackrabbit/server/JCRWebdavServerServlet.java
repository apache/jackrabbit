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
package org.apache.jackrabbit.server;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.WebdavRequestImpl;
import org.apache.jackrabbit.webdav.observation.*;
import org.apache.jackrabbit.webdav.spi.*;
import org.apache.jackrabbit.webdav.spi.observation.SubscriptionManagerImpl;
import org.apache.jackrabbit.webdav.spi.transaction.TxLockManagerImpl;
import org.apache.jackrabbit.client.RepositoryAccessServlet;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.jcr.Repository;
import java.io.IOException;

/**
 * JCRWebdavServerServlet provides request/response handling for the JCRWebdavServer.
 */
public class JCRWebdavServerServlet extends AbstractWebdavServlet implements DavConstants {

    /** the default logger */
    private static Logger log = Logger.getLogger(JCRWebdavServerServlet.class);

    /** Init parameter specifying the prefix used with the resource path. */
    public static final String INIT_PARAM_PREFIX = "resource-path-prefix";
    private static String pathPrefix;

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
	pathPrefix = getInitParameter(INIT_PARAM_PREFIX);
	log.debug(INIT_PARAM_PREFIX + " = " + pathPrefix);

	Repository repository = RepositoryAccessServlet.getRepository();
	if (repository == null) {
	    throw new ServletException("Repository could not be retrieved. Check config of 'RepositoryServlet'.");
	}
	server = new JCRWebdavServer(repository);
        txMgr = new TxLockManagerImpl();
        subscriptionMgr = new SubscriptionManagerImpl();

        // todo: ev. make configurable
        resourceFactory = new DavResourceFactoryImpl(txMgr, subscriptionMgr);
        locatorFactory = new DavLocatorFactoryImpl(pathPrefix);
    }

    /**
     * Returns the path prefix
     *
     * @return pathPrefix
     * @see #INIT_PARAM_PREFIX
     */
    public static String getPathPrefix() {
	return pathPrefix;
    }

    /**
     * Service the request.
     *
     * @param request
     * @param response
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     * @see HttpServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        WebdavRequest webdavRequest = new WebdavRequestImpl(request, locatorFactory);
        WebdavResponse webdavResponse = new WebdavResponseImpl(response);
        try {
            // login to the server
            server.acquireSession(webdavRequest);

            // create the resource and perform initial precondition tests
            DavResource resource = createResource(webdavRequest.getRequestLocator(), webdavRequest, webdavResponse);
            if (preconditionFailed(webdavRequest, resource)) {
                webdavResponse.sendError(DavServletResponse.SC_PRECONDITION_FAILED);
                return;
            }

            // execute the requested method
            int methodCode = DavMethods.getMethodCode(webdavRequest.getMethod());
            execute(webdavRequest, webdavResponse, methodCode, resource);
        } catch (DavException e) {
            webdavResponse.sendErrorResponse(e);
        } finally {
            // logout
	    server.releaseSession(webdavRequest);
        }
    }

    /**
     *
     * @param locator
     * @param request
     * @param response
     * @return
     */
    protected DavResource createResource(DavResourceLocator locator, WebdavRequest request,
                               WebdavResponse response) throws DavException {
        return resourceFactory.createResource(locator, request, response);
    }

    /**
     *
     * @param request
     * @param resource
     * @return
     */
    private boolean preconditionFailed(WebdavRequest request, DavResource resource) {
        // first check matching If header
        if (!request.matchesIfHeader(resource)) {
            return true;
        }

        // test if the requested path matches to the existing session
        // this may occur if the session was retrieved from the cache.
        String wsName = request.getDavSession().getRepositorySession().getWorkspace().getName();
        boolean failed = !resource.getLocator().isSameWorkspace(wsName);
        if (!failed) {
            // make sure, the TransactionId header is valid
            String txId = request.getTransactionId();
            if (txId != null && !txMgr.hasLock(txId, resource)) {
               failed = true;
            }
        }
        return failed;
    }

    /**
     * @param request
     * @param response
     * @param method
     * @param resource
     * @throws ServletException
     * @throws IOException
     * @throws DavException
     */
    private void execute(WebdavRequest request, WebdavResponse response,
                 int method, DavResource resource)
            throws ServletException, IOException, DavException {

        switch (method) {
            case DavMethods.DAV_GET:
                doGet(request, response, resource);
                break;
            case DavMethods.DAV_HEAD:
                doHead(request, response, resource);
                break;
            case DavMethods.DAV_PROPFIND:
                doPropFind(request, response, resource);
                break;
            case DavMethods.DAV_PROPPATCH:
                doPropPatch(request, response, resource);
                break;
            case DavMethods.DAV_POST:
            case DavMethods.DAV_PUT:
                doPut(request, response, resource);
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
            case DavMethods.DAV_OPTIONS:
                doOptions(request, response, resource);
                break;
            case DavMethods.DAV_LOCK:
                doLock(request, response, resource);
                break;
            case DavMethods.DAV_UNLOCK:
                doUnlock(request, response, resource);
                break;
            case DavMethods.DAV_ORDERPATCH:
                doOrderPatch(request, response, resource);
                break;
            case DavMethods.DAV_SUBSCRIBE:
                doSubscribe(request, response, resource);
                break;
            case DavMethods.DAV_UNSUBSCRIBE:
                doUnsubscribe(request, response, resource);
                break;
            case DavMethods.DAV_POLL:
                doPoll(request, response, resource);
                break;
            case DavMethods.DAV_SEARCH:
                doSearch(request, response, resource);
                break;
            case DavMethods.DAV_VERSION_CONTROL:
                doVersionControl(request, response, resource);
                break;
            case DavMethods.DAV_LABEL:
                doLabel(request, response, resource);
                break;
            case DavMethods.DAV_REPORT:
                doReport(request, response, resource);
                break;
            case DavMethods.DAV_CHECKIN:
                doCheckin(request, response, resource);
                break;
            case DavMethods.DAV_CHECKOUT:
                doCheckout(request, response, resource);
                break;
            case DavMethods.DAV_UNCHECKOUT:
                doUncheckout(request, response, resource);
                break;
            case DavMethods.DAV_MERGE:
                doMerge(request, response, resource);
                break;
            case DavMethods.DAV_UPDATE:
                doUpdate(request, response, resource);
                break;
            case DavMethods.DAV_MKWORKSPACE:
                doMkWorkspace(request, response, resource);
                break;
            default:
                // any other method
                super.service(request, response);
        }
    }
}
