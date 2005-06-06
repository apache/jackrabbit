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

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavRequestImpl;
import org.apache.jackrabbit.webdav.WebdavResponse;
import org.apache.jackrabbit.webdav.WebdavResponseImpl;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.observation.EventDiscovery;
import org.apache.jackrabbit.webdav.observation.ObservationResource;
import org.apache.jackrabbit.webdav.observation.Subscription;
import org.apache.jackrabbit.webdav.observation.SubscriptionInfo;
import org.apache.jackrabbit.webdav.ordering.OrderPatch;
import org.apache.jackrabbit.webdav.ordering.OrderingResource;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyIterator;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.search.SearchConstants;
import org.apache.jackrabbit.webdav.search.SearchInfo;
import org.apache.jackrabbit.webdav.search.SearchResource;
import org.apache.jackrabbit.webdav.transaction.TransactionInfo;
import org.apache.jackrabbit.webdav.transaction.TransactionResource;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.apache.jackrabbit.webdav.version.DeltaVResource;
import org.apache.jackrabbit.webdav.version.LabelInfo;
import org.apache.jackrabbit.webdav.version.MergeInfo;
import org.apache.jackrabbit.webdav.version.OptionsInfo;
import org.apache.jackrabbit.webdav.version.OptionsResponse;
import org.apache.jackrabbit.webdav.version.UpdateInfo;
import org.apache.jackrabbit.webdav.version.VersionControlledResource;
import org.apache.jackrabbit.webdav.version.VersionResource;
import org.apache.jackrabbit.webdav.version.VersionableResource;
import org.apache.jackrabbit.webdav.version.report.Report;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.log4j.Logger;
import org.jdom.Document;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <code>AbstractWebdavServlet</code>
 * <p/>
 * todo respect Position header
 */
abstract public class AbstractWebdavServlet extends HttpServlet implements DavConstants {

    /**
     * default logger
     */
    private static Logger log = Logger.getLogger(AbstractWebdavServlet.class);

    /**
     * Default value for the 'WWW-Authenticate' header, that is set, if request
     * results in a {@link DavServletResponse#SC_UNAUTHORIZED 401 (Unauthorized)}
     * error.
     *
     * @see #getAuthenticateHeaderValue()
     */
    public static final String DEFAULT_AUTHENTICATE_HEADER = "Basic Realm=Jackrabbit Webdav Server";

    /**
     * Checks if the precondition for this request and resource is valid.
     *
     * @param request
     * @param resource
     * @return
     */
    abstract protected boolean isPreconditionValid(WebdavRequest request, DavResource resource);

    /**
     * Returns the <code>DavSessionProvider</code>.
     *
     * @return the session provider
     */
    abstract public DavSessionProvider getSessionProvider();

    /**
     * Returns the <code>DavLocatorFactory</code>.
     *
     * @return the locator factory
     */
    abstract public DavLocatorFactory getLocatorFactory();

    /**
     * Returns the <code>DavResourceFactory</code>.
     *
     * @return the resource factory
     */
    abstract public DavResourceFactory getResourceFactory();

    /**
     * Returns the value of the 'WWW-Authenticate' header, that is returned in
     * case of 401 error.
     *
     * @return value of the 'WWW-Authenticate' header
     */
    abstract public String getAuthenticateHeaderValue();

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

        WebdavRequest webdavRequest = new WebdavRequestImpl(request, getLocatorFactory());
        WebdavResponse webdavResponse = new WebdavResponseImpl(response);
        try {
            // make sure there is a authenticated user
            if (!getSessionProvider().attachSession(webdavRequest)) {
                return;
            }

            // check matching if=header for lock-token relevant operations
            DavResource resource = getResourceFactory().createResource(webdavRequest.getRequestLocator(), webdavRequest, webdavResponse);
            if (!isPreconditionValid(webdavRequest, resource)) {
                webdavResponse.sendError(DavServletResponse.SC_PRECONDITION_FAILED);
                return;
            }

            int methodCode = DavMethods.getMethodCode(webdavRequest.getMethod());
            if (!execute(webdavRequest, webdavResponse, methodCode, resource)) {
                super.service(request, response);
            }

        } catch (DavException e) {
            if (e.getErrorCode() == HttpServletResponse.SC_UNAUTHORIZED) {
                webdavResponse.setHeader("WWW-Authenticate", getAuthenticateHeaderValue());
                webdavResponse.sendError(e.getErrorCode(), e.getStatusPhrase());
            } else {
                webdavResponse.sendErrorResponse(e);
            }
        } finally {
            getSessionProvider().releaseSession(webdavRequest);
        }
    }

    /**
     * Executes the respective method in the given webdav context
     *
     * @param request
     * @param response
     * @param method
     * @param resource
     * @throws ServletException
     * @throws IOException
     * @throws DavException
     */
    protected boolean execute(WebdavRequest request, WebdavResponse response,
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
                doPost(request, response, resource);
                break;
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
                return false;
        }
        return true;
    }

    /**
     * The OPTION method
     *
     * @param request
     * @param response
     * @param resource
     */
    protected void doOptions(WebdavRequest request, WebdavResponse response,
                             DavResource resource) throws IOException {
        response.addHeader(DavConstants.HEADER_DAV, resource.getComplianceClass());
        response.addHeader("Allow", resource.getSupportedMethods());
        response.addHeader("MS-Author-Via", DavConstants.HEADER_DAV);
        if (resource instanceof SearchResource) {
            String[] langs = ((SearchResource) resource).getQueryGrammerSet().getQueryLanguages();
            for (int i = 0; i < langs.length; i++) {
                response.addHeader(SearchConstants.HEADER_DASL, "<" + langs[i] + ">");
            }
        }
        // with DeltaV the OPTIONS request may contain a Xml body.
        OptionsResponse oR = null;
        OptionsInfo oInfo = request.getOptionsInfo();
        if (oInfo != null && resource instanceof DeltaVResource) {
            oR = ((DeltaVResource) resource).getOptionResponse(oInfo);
        }
        if (oR == null) {
            response.setStatus(DavServletResponse.SC_OK);
        } else {
            response.sendXmlResponse(oR.toXml(), DavServletResponse.SC_OK);
        }
    }

    /**
     * The HEAD method
     *
     * @param request
     * @param response
     * @param resource
     * @throws java.io.IOException
     */
    protected void doHead(WebdavRequest request, WebdavResponse response,
                          DavResource resource) throws IOException {
        spoolResource(request, response, resource, false);
    }

    /**
     * The GET method
     *
     * @param request
     * @param response
     * @param resource
     * @throws IOException
     */
    protected void doGet(WebdavRequest request, WebdavResponse response,
                         DavResource resource) throws IOException {
        spoolResource(request, response, resource, true);
    }

    /**
     * @param request
     * @param response
     * @param resource
     * @param sendContent
     * @throws IOException
     */
    private void spoolResource(WebdavRequest request, WebdavResponse response,
                               DavResource resource, boolean sendContent)
            throws IOException {

        if (!resource.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        long modTime = resource.getModificationTime();
        if (modTime != DavResource.UNDEFINED_MODIFICATIONTIME && modTime <= request.getDateHeader("If-Modified-Since")) {
            // resource has not been modified since the time indicated in the
            // 'If-Modified-Since' header.
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        DavProperty lastMod = resource.getProperty(DavPropertyName.GETLASTMODIFIED);
        if (lastMod != null) {
            response.setHeader("Last-Modified", String.valueOf(lastMod.getValue()));
        }

        DavProperty etag = resource.getProperty(DavPropertyName.GETETAG);
        if (etag != null) {
            response.setHeader("ETag", String.valueOf(etag.getValue()));
        }

        DavProperty contentType = resource.getProperty(DavPropertyName.GETCONTENTTYPE);
        if (contentType != null) {
            response.setHeader("Content-Type", String.valueOf(contentType.getValue()));
        }

        DavProperty contentLength = resource.getProperty(DavPropertyName.GETCONTENTLENGTH);
        if (contentLength != null) {
            try {
                int length = Integer.parseInt(contentLength.getValue() + "");
                if (length > 0) {
                    response.setIntHeader("Content-Length", length);
                }
            } catch (NumberFormatException e) {
                log.error("Could not build content length from property value '" + contentLength.getValue() + "'");
            }
        }

        // spool content in case of 'GET' request
        InputStream in = resource.getStream();
        try {
            if (sendContent) {
                if (in != null) {
                    OutputStream out = response.getOutputStream();
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) >= 0) {
                        out.write(buffer, 0, read);
                    }
                }
            }
        } finally {
            // also close stream if not sending content
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        response.flushBuffer();
    }

    /**
     * The PROPFIND method
     *
     * @param request
     * @param response
     * @param resource
     * @throws IOException
     */
    protected void doPropFind(WebdavRequest request, WebdavResponse response,
                              DavResource resource) throws IOException {

        if (!resource.exists()) {
            response.sendError(DavServletResponse.SC_NOT_FOUND);
            return;
        }

        int depth = request.getDepth(DEPTH_INFINITY);
        DavPropertyNameSet requestProperties = request.getPropFindProperties();
        int propfindType = request.getPropFindType();

        MultiStatus mstatus = new MultiStatus();
        mstatus.addResourceProperties(resource, requestProperties, propfindType, depth);
        response.sendMultiStatusResponse(mstatus);
    }

    /**
     * The PROPPATCH method
     *
     * @param request
     * @param response
     * @param resource
     * @throws IOException
     */
    protected void doPropPatch(WebdavRequest request, WebdavResponse response,
                               DavResource resource)
            throws IOException, DavException {

        DavPropertySet setProperties = request.getPropPatchSetProperties();
        DavPropertyNameSet removeProperties = request.getPropPatchRemoveProperties();
        if (setProperties.isEmpty() && removeProperties.isEmpty()) {
            response.sendError(DavServletResponse.SC_BAD_REQUEST);
            return;
        }

        // first resolve merge conflicts
        // TODO: not correct resolution of merge conflicts are immediately perstisted
        // TODO: rfc 2518 requires, that no changes must only be persisted if the complete proppatch-req succeeds
        if (resource instanceof VersionControlledResource) {
            ((VersionControlledResource) resource).resolveMergeConflict(setProperties, removeProperties);
        }

        // complete any other property setting or removing
        DavPropertyIterator setIter = setProperties.iterator();
        while (setIter.hasNext()) {
            DavProperty prop = setIter.nextProperty();
            resource.setProperty(prop);
        }
        Iterator remNameIter = removeProperties.iterator();
        while (remNameIter.hasNext()) {
            DavPropertyName propName = (DavPropertyName) remNameIter.next();
            resource.removeProperty(propName);
        }
        response.setStatus(DavServletResponse.SC_OK);

        // todo return multistatus response in case of failure
    }

    /**
     * The POST method. Delegate to PUT
     *
     * @param request
     * @param response
     * @param resource
     * @throws IOException
     * @throws DavException
     */
    protected void doPost(WebdavRequest request, WebdavResponse response,
                          DavResource resource) throws IOException, DavException {
        doPut(request, response, resource);
    }

    /**
     * The PUT method
     *
     * @param request
     * @param response
     * @param resource
     * @throws IOException
     * @throws DavException
     */
    protected void doPut(WebdavRequest request, WebdavResponse response,
                         DavResource resource) throws IOException, DavException {

        DavResource parentResource = resource.getCollection();
        if (parentResource == null || !parentResource.exists()) {
            // parent does not exist
            response.sendError(DavServletResponse.SC_CONFLICT);
            return;
        }

        int status;
        // test if resource already exists
        if (resource.exists()) {
            status = DavServletResponse.SC_NO_CONTENT;
        } else {
            status = DavServletResponse.SC_CREATED;
        }

        parentResource.addMember(resource, request.getInputStream());
        response.setStatus(status);
    }

    /**
     * The MKCOL method
     *
     * @param request
     * @param response
     * @param resource
     * @throws IOException
     * @throws DavException
     */
    protected void doMkCol(WebdavRequest request, WebdavResponse response,
                           DavResource resource) throws IOException, DavException {

        DavResource parentResource = resource.getCollection();
        if (parentResource == null || !parentResource.exists() || !parentResource.isCollection()) {
            // parent does not exist or is not a collection
            response.sendError(DavServletResponse.SC_CONFLICT);
            return;
        }

        if (request.getContentLength() > 0 || request.getHeader("Transfer-Encoding") != null) {
            parentResource.addMember(resource, request.getInputStream());
        } else {
            parentResource.addMember(resource);
        }
        response.setStatus(DavServletResponse.SC_CREATED);
    }

    /**
     * The DELETE method
     *
     * @param request
     * @param response
     * @param resource
     * @throws IOException
     * @throws DavException
     */
    protected void doDelete(WebdavRequest request, WebdavResponse response,
                            DavResource resource) throws IOException, DavException {
        DavResource parent = resource.getCollection();
        if (parent != null) {
            parent.removeMember(resource);
            response.setStatus(DavServletResponse.SC_NO_CONTENT);
        } else {
            response.sendError(DavServletResponse.SC_FORBIDDEN, "Cannot remove the root resource.");
        }
    }

    /**
     * The COPY method
     *
     * @param request
     * @param response
     * @param resource
     * @throws IOException
     * @throws DavException
     */
    protected void doCopy(WebdavRequest request, WebdavResponse response,
                          DavResource resource) throws IOException, DavException {

        // only depth 0 and infinity is allowed
        int depth = request.getDepth(DEPTH_INFINITY);
        if (!(depth == DEPTH_0 || depth == DEPTH_INFINITY)) {
            response.sendError(DavServletResponse.SC_BAD_REQUEST);
            return;
        }

        DavResource destResource = getResourceFactory().createResource(request.getDestinationLocator(), request, response);
        int status = validateDestination(destResource, request);
        if (status > DavServletResponse.SC_NO_CONTENT) {
            response.sendError(status);
            return;
        }

        resource.copy(destResource, depth == DEPTH_0);
        response.setStatus(status);
    }

    /**
     * The MOVE method
     *
     * @param request
     * @param response
     * @param resource
     * @throws IOException
     * @throws DavException
     */
    protected void doMove(WebdavRequest request, WebdavResponse response,
                          DavResource resource) throws IOException, DavException {

        DavResource destResource = getResourceFactory().createResource(request.getDestinationLocator(), request, response);
        int status = validateDestination(destResource, request);
        if (status > DavServletResponse.SC_NO_CONTENT) {
            response.sendError(status);
            return;
        }

        resource.move(destResource);
        response.setStatus(status);
    }

    /**
     * Validate the given destination resource and return the proper status
     * code: Any return value greater/equal than {@link DavServletResponse#SC_NO_CONTENT}
     * indicates an error.
     *
     * @param destResource destination resource to be validated.
     * @param request
     * @return status code indicating whether the destination is valid.
     */
    private int validateDestination(DavResource destResource, WebdavRequest request)
            throws DavException {

        String destHeader = request.getHeader(HEADER_DESTINATION);
        if (destHeader == null || "".equals(destHeader)) {
            return DavServletResponse.SC_BAD_REQUEST;
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
                // cannot copy/move to an existing item, if overwrite is not forced
                return DavServletResponse.SC_PRECONDITION_FAILED;
            }
        } else {
            // destination does not exist >> copy/move can be performed
            status = DavServletResponse.SC_CREATED;
        }
        return status;
    }

    /**
     * The LOCK method
     *
     * @param request
     * @param response
     * @param resource
     * @throws IOException
     * @throws DavException
     */
    protected void doLock(WebdavRequest request, WebdavResponse response,
                          DavResource resource) throws IOException, DavException {

        LockInfo lockInfo = request.getLockInfo();
        if (lockInfo.isRefreshLock()) {
            // refresh any matching existing locks
            ActiveLock[] activeLocks = resource.getLocks();
            List lList = new ArrayList();
            for (int i = 0; i < activeLocks.length; i++) {
                // adjust lockinfo with type/scope retrieved from the lock.
                lockInfo.setType(activeLocks[i].getType());
                lockInfo.setScope(activeLocks[i].getScope());
                
                // todo: do not ignore etag
                if (request.matchesIfHeader(resource.getHref(), activeLocks[i].getToken(), "")) {
                    lList.add(resource.refreshLock(lockInfo, activeLocks[i].getToken()));
                }
            }
            if (lList.isEmpty()) {
                throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
            }
            ActiveLock[] refreshedLocks = (ActiveLock[]) lList.toArray(new ActiveLock[lList.size()]);
            response.sendRefreshLockResponse(refreshedLocks);
        } else {
            // create a new lock
            ActiveLock lock = resource.lock(lockInfo);
            response.sendLockResponse(lock);
        }

        // TODO multistatus in case of failure...
        // NOTE: spec says 409 status, but example says 207 (multistatus)
    }

    /**
     * The UNLOCK method
     *
     * @param request
     * @param response
     * @param resource
     * @throws DavException
     */
    protected void doUnlock(WebdavRequest request, WebdavResponse response,
                            DavResource resource) throws DavException {
        // get lock token from header
        String lockToken = request.getLockToken();
        TransactionInfo tInfo = request.getTransactionInfo();
        if (tInfo != null) {
            ((TransactionResource) resource).unlock(lockToken, tInfo);
        } else {
            resource.unlock(lockToken);
        }
        response.setStatus(DavServletResponse.SC_NO_CONTENT);
    }

    /**
     * The ORDERPATCH method
     *
     * @param request
     * @param response
     * @param resource
     * @throws java.io.IOException
     * @throws DavException
     */
    protected void doOrderPatch(WebdavRequest request,
                                WebdavResponse response,
                                DavResource resource)
            throws IOException, DavException {

        if (!(resource instanceof OrderingResource)) {
            response.sendError(DavServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        OrderPatch op = request.getOrderPatch();
        if (op == null) {
            response.sendError(DavServletResponse.SC_BAD_REQUEST);
            return;
        }
        // perform reordering of internal members
        ((OrderingResource) resource).orderMembers(op);
        response.setStatus(DavServletResponse.SC_OK);

        //TODO: in case of failure Multistatus is required...
    }

    /**
     * The SUBSCRIBE method
     *
     * @param request
     * @param response
     * @param resource
     * @throws IOException
     * @throws DavException
     */
    protected void doSubscribe(WebdavRequest request,
                               WebdavResponse response,
                               DavResource resource)
            throws IOException, DavException {

        if (!(resource instanceof ObservationResource)) {
            response.sendError(DavServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        SubscriptionInfo info = request.getSubscriptionInfo();
        if (info == null) {
            response.sendError(DavServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }
        Subscription subs = ((ObservationResource) resource).subscribe(info, request.getSubscriptionId());
        response.sendSubscriptionResponse(subs);
    }

    /**
     * The UNSUBSCRIBE method
     *
     * @param request
     * @param response
     * @param resource
     * @throws IOException
     * @throws DavException
     */
    protected void doUnsubscribe(WebdavRequest request,
                                 WebdavResponse response,
                                 DavResource resource)
            throws IOException, DavException {

        if (!(resource instanceof ObservationResource)) {
            response.sendError(DavServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        ((ObservationResource) resource).unsubscribe(request.getSubscriptionId());
        response.setStatus(DavServletResponse.SC_NO_CONTENT);
    }

    /**
     * The POLL method
     *
     * @param request
     * @param response
     * @param resource
     * @throws IOException
     * @throws DavException
     */
    protected void doPoll(WebdavRequest request,
                          WebdavResponse response,
                          DavResource resource)
            throws IOException, DavException {

        if (!(resource instanceof ObservationResource)) {
            response.sendError(DavServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        EventDiscovery ed = ((ObservationResource) resource).poll(request.getSubscriptionId());
        response.sendPollResponse(ed);
    }

    /**
     * The VERSION-CONTROL method
     *
     * @param request
     * @param response
     * @param resource
     * @throws DavException
     * @throws IOException
     */
    protected void doVersionControl(WebdavRequest request, WebdavResponse response,
                                    DavResource resource)
            throws DavException, IOException {
        if (!(resource instanceof VersionableResource)) {
            response.sendError(DavServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        ((VersionableResource) resource).addVersionControl();
    }

    /**
     * The LABEL method
     *
     * @param request
     * @param response
     * @param resource
     * @throws DavException
     * @throws IOException
     */
    protected void doLabel(WebdavRequest request, WebdavResponse response,
                           DavResource resource)
            throws DavException, IOException {

        LabelInfo labelInfo = request.getLabelInfo();
        if (resource instanceof VersionResource) {
            ((VersionResource) resource).label(labelInfo);
        } else if (resource instanceof VersionControlledResource) {
            ((VersionControlledResource) resource).label(labelInfo);
        } else {
            // any other resource type that does not support a LABEL request
            response.sendError(DavServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    /**
     * The REPORT method
     *
     * @param request
     * @param response
     * @param resource
     * @throws DavException
     * @throws IOException
     */
    protected void doReport(WebdavRequest request, WebdavResponse response,
                            DavResource resource)
            throws DavException, IOException {
        if (!(resource instanceof DeltaVResource)) {
            response.sendError(DavServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        ReportInfo info = request.getReportInfo();
        Report report = ((DeltaVResource) resource).getReport(info);
        response.sendXmlResponse(report.toXml(), DavServletResponse.SC_OK);
    }

    /**
     * The CHECKIN method
     *
     * @param request
     * @param response
     * @param resource
     * @throws DavException
     * @throws IOException
     */
    protected void doCheckin(WebdavRequest request, WebdavResponse response,
                             DavResource resource)
            throws DavException, IOException {

        if (!(resource instanceof VersionControlledResource)) {
            response.sendError(DavServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        String versionHref = ((VersionControlledResource) resource).checkin();
        response.setHeader(DeltaVConstants.HEADER_LOCATION, versionHref);
    }

    /**
     * The CHECKOUT method
     *
     * @param request
     * @param response
     * @param resource
     * @throws DavException
     * @throws IOException
     */
    protected void doCheckout(WebdavRequest request, WebdavResponse response,
                              DavResource resource)
            throws DavException, IOException {
        if (!(resource instanceof VersionControlledResource)) {
            response.sendError(DavServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        ((VersionControlledResource) resource).checkout();
    }

    /**
     * The UNCHECKOUT method
     *
     * @param request
     * @param response
     * @param resource
     * @throws DavException
     * @throws IOException
     */
    protected void doUncheckout(WebdavRequest request, WebdavResponse response,
                                DavResource resource)
            throws DavException, IOException {
        if (!(resource instanceof VersionControlledResource)) {
            response.sendError(DavServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        ((VersionControlledResource) resource).uncheckout();
    }

    /**
     * The MERGE method
     *
     * @param request
     * @param response
     * @param resource
     * @throws DavException
     * @throws IOException
     */
    protected void doMerge(WebdavRequest request, WebdavResponse response,
                           DavResource resource) throws DavException, IOException {

        if (!(resource instanceof VersionControlledResource)) {
            response.sendError(DavServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        MergeInfo info = request.getMergeInfo();
        MultiStatus ms = ((VersionControlledResource) resource).merge(info);
        response.sendMultiStatusResponse(ms);
    }

    /**
     * The UPDATE method
     *
     * @param request
     * @param response
     * @param resource
     * @throws DavException
     * @throws IOException
     */
    protected void doUpdate(WebdavRequest request, WebdavResponse response,
                            DavResource resource) throws DavException, IOException {

        if (!(resource instanceof VersionControlledResource)) {
            response.sendError(DavServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        UpdateInfo info = request.getUpdateInfo();
        MultiStatus ms = ((VersionControlledResource) resource).update(info);
        response.sendMultiStatusResponse(ms);
    }

    /**
     * The MKWORKSPACE method
     *
     * @param request
     * @param response
     * @param resource
     * @throws DavException
     * @throws IOException
     */
    protected void doMkWorkspace(WebdavRequest request, WebdavResponse response,
                                 DavResource resource) throws DavException, IOException {
        if (resource.exists()) {
            log.warn("Cannot create a new workspace. Resource already exists.");
            response.sendError(DavServletResponse.SC_FORBIDDEN);
            return;
        }

        DavResource parentResource = resource.getCollection();
        if (parentResource == null || !parentResource.exists() || !parentResource.isCollection()) {
            // parent does not exist or is not a collection
            response.sendError(DavServletResponse.SC_CONFLICT);
            return;
        }
        if (!(parentResource instanceof DeltaVResource)) {
            response.sendError(DavServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        ((DeltaVResource) parentResource).addWorkspace(resource);
        response.setStatus(DavServletResponse.SC_CREATED);
    }

    /**
     * The SEARCH method
     *
     * @param request
     * @param response
     * @param resource
     * @throws DavException
     * @throws IOException
     */
    protected void doSearch(WebdavRequest request, WebdavResponse response,
                            DavResource resource) throws DavException, IOException {

        if (!(resource instanceof SearchResource)) {
            response.sendError(DavServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        try {
            Document doc = request.getRequestDocument();
            if (doc != null) {
                SearchInfo sR = new SearchInfo(doc);
                response.sendMultiStatusResponse(((SearchResource) resource).search(sR));
            } else {
                // request without request body is valid if requested resource
                // is a 'query' resource.
                response.sendMultiStatusResponse(((SearchResource) resource).search(null));
            }
        } catch (IllegalArgumentException e) {
            response.sendError(DavServletResponse.SC_BAD_REQUEST);
            return;
        }
    }
}