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
package org.apache.jackrabbit.webdav;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.util.Text;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.property.*;
import org.apache.jackrabbit.webdav.transaction.*;
import org.apache.jackrabbit.webdav.observation.*;
import org.apache.jackrabbit.webdav.version.*;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.ordering.*;
import org.jdom.input.SAXBuilder;
import org.jdom.JDOMException;
import org.jdom.Document;
import org.jdom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletInputStream;
import javax.servlet.RequestDispatcher;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.BufferedReader;
import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;

/**
 * <code>WebdavRequestImpl</code>...
 */
public class WebdavRequestImpl implements WebdavRequest {

    private static Logger log = Logger.getLogger(WebdavRequestImpl.class);

    private final HttpServletRequest httpRequest;
    private final DavLocatorFactory factory;
    private final IfHeader ifHeader;
    private final String hrefPrefix;

    private DavSession session;

    private int propfindType = DavConstants.PROPFIND_ALL_PROP;
    private DavPropertyNameSet propfindProps;
    private DavPropertySet proppatchSet;
    private DavPropertyNameSet proppatchRemove;

    /**
     * Creates a new <code>DavServletRequest</code> with the given parameters.
     *
     * @param httpRequest
     * @param factory
     */
    public WebdavRequestImpl(HttpServletRequest httpRequest, DavLocatorFactory factory) {
        this.httpRequest = httpRequest;
        this.factory = factory;
        this.ifHeader = new IfHeader(httpRequest);

        String host = getHeader("Host");
	String scheme = getScheme();
	hrefPrefix = scheme + "://" + host + getContextPath();
    }

    /**
     * Sets the session field and adds all lock tokens present with either the
     * Lock-Token header or the If header to the given session object.
     *
     * @param session
     * @see DavServletRequest#setDavSession(DavSession)
     */
    public void setDavSession(DavSession session) {
        this.session = session;
        // set lock-tokens from header to the current session
        if (session != null && session.getRepositorySession() != null) {
            String lt = getLockToken();
            if (lt != null) {
                session.addLockToken(lt);
            }
            // add all token present in the the If header to the session as well.
            Iterator it = ifHeader.getAllTokens();
            while (it.hasNext()) {
                String ifHeaderToken = (String)it.next();
                session.addLockToken(ifHeaderToken);
            }
        }
    }

    /**
     * @see DavServletRequest#getDavSession()
     */
    public DavSession getDavSession() {
        return session;
    }

    /**
     * Return a <code>DavResourceLocator</code> representing the request handle.
     *
     * @return locator of the requested resource
     * @see DavServletRequest#getRequestLocator()
     */
    public DavResourceLocator getRequestLocator() {
        String path = getPathInfo();
        if (path == null) {
            path = getServletPath();
        }
        return factory.createResourceLocator(hrefPrefix, path);
    }

    /**
     * Parse the destination header field and return the path of the destination
     * resource.
     *
     * @return path of the destination resource.
     * @see DavConstants#HEADER_DESTINATION
     * @see DavServletRequest#getDestinationLocator
     */
    public DavResourceLocator getDestinationLocator() {
        String destination = httpRequest.getHeader(DavConstants.HEADER_DESTINATION);
        if (destination != null) {
	    try {
		URI uri = new URI(destination);
		if (uri.getAuthority().equals(httpRequest.getHeader("Host"))) {
		    destination = Text.unescape(uri.getPath());
		}
	    } catch (URISyntaxException e) {
		log.debug("Destination is path is not a valid URI ("+e.getMessage()+".");
		int pos = destination.lastIndexOf(":");
		if (pos > 0) {
		    destination = destination.substring(destination.indexOf("/",pos));
		    log.debug("Tried to retrieve resource destination path from invalid URI: "+destination);
		}
	    }

	    // cut off the context path
	    String contextPath = httpRequest.getContextPath();
	    if (destination.startsWith(contextPath)) {
		destination = destination.substring(contextPath.length());
	    }
	}
	return factory.createResourceLocator(hrefPrefix, destination);
    }

    /**
     * Return true if the overwrite header does not inhibit overwriting.
     *
     * @return true if the overwrite header requests 'overwriting'
     * @see DavConstants#HEADER_OVERWRITE
     * @see DavServletRequest#isOverwrite()
     */
    public boolean isOverwrite() {
	boolean doOverwrite = true;
	String overwriteHeader = httpRequest.getHeader(DavConstants.HEADER_OVERWRITE);
	if (overwriteHeader != null && !overwriteHeader.equalsIgnoreCase(DavConstants.NO_OVERWRITE)){
	    doOverwrite = false;
	}
	return doOverwrite;
    }

    /**
     * @see DavServletRequest#getDepth(int)
     */
    public int getDepth(int defaultValue) {
	String dHeader = httpRequest.getHeader(DavConstants.HEADER_DEPTH);
	int depth = depthToInt(dHeader, defaultValue);
	return depth;
    }

    /**
     * @see DavServletRequest#getDepth()
     */
    public int getDepth() {
        return getDepth(DavConstants.DEPTH_INFINITY);
    }

    /**
     * Parse the request timeout header and convert the timeout value
     * into a long indicating the number of milliseconds until expiration time
     * is reached.<br>
     * NOTE: If the requested timeout is 'infinite' {@link Long.MAX_VALUE}
     * is returned.
     *
     * @return milliseconds the lock is requested to live.
     * @see DavServletRequest#getTimeout()
     */
    public long getTimeout() {
	String timeoutStr = httpRequest.getHeader(DavConstants.HEADER_TIMEOUT);
	long timeout = DavConstants.UNDEFINED_TIMEOUT;
	if (timeoutStr != null && timeoutStr.length() > 0) {
	    int secondsInd = timeoutStr.indexOf("Second-");
	    if (secondsInd >= 0) {
		secondsInd += 7; // read over "Second-"
		int i = secondsInd;
                while (i < timeoutStr.length() && Character.isDigit(timeoutStr.charAt(i))) {
                    i++;
                }
		try {
		    timeout = 1000L * Long.parseLong(timeoutStr.substring(secondsInd, i));
		} catch (NumberFormatException ignore) {
		    // ignore an let the lock define the default timeout
                    log.error("Invalid timeout format: "+timeoutStr);
		}
	    } else if (timeoutStr.equalsIgnoreCase(DavConstants.TIMEOUT_INFINITE)) {
		timeout = DavConstants.INFINITE_TIMEOUT;
	    }
	}
	return timeout;
    }

    /**
     * Retrive the lock token from the 'Lock-Token' header.
     *
     * @return String representing the lock token sent in the Lock-Token header.
     * @throws IllegalArgumentException If the value has not the correct format.
     * @see DavConstants#HEADER_LOCK_TOKEN
     * @see DavServletRequest#getLockToken()
     */
    public String getLockToken() {
	return getCodedURLHeader(DavConstants.HEADER_LOCK_TOKEN);
    }

    /**
     * @return Xml document
     * @see DavServletRequest#getRequestDocument()
     */
    public Document getRequestDocument() {
        Document requestDocument = null;
        // try to parse the request body
        try {
            SAXBuilder builder = new SAXBuilder(false);
            InputStream in = httpRequest.getInputStream();
            if (in != null) {
                requestDocument = builder.build(in);
            }
        } catch (IOException e) {
            log.warn("Error while reading the request body: " + e.getMessage());
        } catch (JDOMException e) {
            log.warn("Error while building xml document from request body: " + e.getMessage());
        }
        return requestDocument;
    }

    /**
     * Returns the type of PROPFIND as indicated by the request body.
     *
     * @return type of the PROPFIND request. Default value is
     * {@link DavConstants#PROPFIND_ALL_PROP allprops}
     * @see DavServletRequest#getPropFindType()
     */
    public int getPropFindType() {
        if (propfindProps == null) {
            parsePropFindRequest();
        }
        return propfindType;
    }

    /**
     * Returns the set of properties requested by the PROPFIND body or an
     * empty set if the {@link #getPropFindType type} is either 'allprop' or
     * 'propname'.
     *
     * @return set of properties requested by the PROPFIND body or an empty set.
     * @see DavServletRequest#getPropFindProperties()
     */
    public DavPropertyNameSet getPropFindProperties() {
        if (propfindProps == null) {
            parsePropFindRequest();
        }
        return propfindProps;
    }

    /**
     * Parse the propfind request body in order to determine the type of the propfind
     * and the set of requested property.
     * NOTE: An empty 'propfind' request body will be treated as request for all
     * property according to the specification.
     */
    private void parsePropFindRequest() {

        propfindProps = new DavPropertyNameSet();
        Document requestDocument = getRequestDocument();

	// propfind httpRequest with empty body or invalid Xml >> retrieve all property
	// TODO: spec requires a 'BAD REQUEST' error code
	if (requestDocument == null) {
	    return;
	}

	// propfind httpRequest with invalid body >> treat as if empty body
	Element root = requestDocument.getRootElement();
	if (!root.getName().equals(DavConstants.XML_PROPFIND)) {
	    log.info("PropFind-Request has no <profind> tag.");
	    return;
	}

	List childList = root.getChildren();
	for (int i = 0; i < childList.size(); i++) {
	    Element child = (Element) childList.get(i);
	    String nodeName = child.getName();
	    if (DavConstants.XML_PROP.equals(nodeName)) {
		propfindType = DavConstants.PROPFIND_BY_PROPERTY;
		propfindProps = new DavPropertyNameSet(child);
                break;
	    } else if (DavConstants.XML_PROPNAME.equals(nodeName)) {
		propfindType = DavConstants.PROPFIND_PROPERTY_NAMES;
                break;
	    } else if (DavConstants.XML_ALLPROP.equals(nodeName)) {
		propfindType = DavConstants.PROPFIND_ALL_PROP;
                break;
	    }
	}
    }

    /**
     * Return the list of 'set' entries in the PROPPATCH request body. The list
     * is empty if the request body could not be parsed or if the request body did
     * not contain any 'set' elements.
     *
     * @return the list of 'set' entries in the PROPPATCH request body
     * @see DavServletRequest#getPropPatchSetProperties()
     */
    public DavPropertySet getPropPatchSetProperties() {
        if (proppatchSet == null) {
            parsePropPatchRequest();
        }
        return proppatchSet;
    }

    /**
     * Return the list of 'remove' entries in the PROPPATCH request body. The list
     * is empty if the request body could not be parsed or if the request body did
     * not contain any 'remove' elements.
     *
     * @return the list of 'remove' entries in the PROPPATCH request body
     * @see DavServletRequest#getPropPatchRemoveProperties()
     */
    public DavPropertyNameSet getPropPatchRemoveProperties() {
        if (proppatchRemove == null) {
            parsePropPatchRequest();
        }
        return proppatchRemove;
    }

    /**
     * Parse the PROPPATCH request body.
     */
    private void parsePropPatchRequest() {

        proppatchSet = new DavPropertySet();
        proppatchRemove = new DavPropertyNameSet();
        Document requestDocument = getRequestDocument();

        if (requestDocument == null) {
            return;
        }

        Element root = requestDocument.getRootElement();
        if (!root.getName().equals(DavConstants.XML_PROPERTYUPDATE)) {
            // we should also check for correct namespace
            log.warn("PropPatch-Request has no <propertyupdate> tag.");
            return;
        }

        List setList = root.getChildren(DavConstants.XML_SET, DavConstants.NAMESPACE);
        if (!setList.isEmpty()) {
            Iterator setIter = setList.iterator();
            while (setIter.hasNext()) {
                Element propElem = ((Element) setIter.next()).getChild(DavConstants.XML_PROP, DavConstants.NAMESPACE);
                List properties = propElem.getChildren();
                for (int i = 0; i < properties.size(); i++) {
                    Element property = (Element) properties.get(i);
                    proppatchSet.add(new DefaultDavProperty(property.getName(), property.getContent(), property.getNamespace()));
                }
            }
        }

        // get <remove> properties
        List removeList = root.getChildren(DavConstants.XML_REMOVE, DavConstants.NAMESPACE);
        if (!removeList.isEmpty()) {
            Iterator removeIter = removeList.iterator();
            while (removeIter.hasNext()) {
                Element propElem = ((Element) removeIter.next()).getChild(DavConstants.XML_PROP, DavConstants.NAMESPACE);
                Iterator propIter = propElem.getChildren().iterator();
                while (propIter.hasNext()) {
                    Element property = (Element) propIter.next();
                    proppatchRemove.add(DavPropertyName.create(property.getName(), property.getNamespace()));
                }
            }
        }
    }

    /**
     * {@link LockInfo} object encapsulating the information passed with a LOCK
     * request if the LOCK request body was valid. If the request body is
     * missing a 'refresh lock' request is assumed. The {@link LockInfo}
     * then only provides timeout and isDeep property and returns true on
     * {@link org.apache.jackrabbit.webdav.lock.LockInfo#isRefreshLock()}
     *
     * @return lock info object or <code>null</code> if an error occured while
     * parsing the request body.
     * @see DavServletRequest#getLockInfo()
     */
    public LockInfo getLockInfo() {
        LockInfo lockInfo = null;
        boolean isDeep = getDepth(DavConstants.DEPTH_INFINITY) == DavConstants.DEPTH_INFINITY;
	Document requestDocument = getRequestDocument();
        // check if XML request body is present. It SHOULD have one for
	// 'create Lock' request and missing for a 'refresh Lock' request
	if (requestDocument != null) {
	    Element root = requestDocument.getRootElement();
	    if (root.getName().equals(DavConstants.XML_LOCKINFO)) {
                lockInfo = new LockInfo(root, getTimeout(), isDeep);
	    } else {
		log.debug("Lock-Request has no <lockinfo> tag.");
	    }
	} else {
            lockInfo = new LockInfo(null, getTimeout(), isDeep);
        }
        return lockInfo;
    }

    /**
     * MTest if the if header matches the given resource. The comparison is
     * made with the {@link DavResource#getHref()
     * resource href} and the token returned from an exclusive write lock present on
     * the resource. An empty strong ETag is currently assumed.<br>
     * NOTE: If either the If header or the resource is <code>null</code> or if
     * the resource has not applied an exclusive write lock the preconditions are met.
     * If in contrast the lock applied to the given resource returns a
     * <code>null</code> lock token (e.g. for security reasons) or a lock token
     * that does not match, the method will return false.
     *
     * @param resource Webdav resources being operated on
     * @return true if the test is successful and the preconditions for the
     * request processing are fulfilled.
     * @param resource
     * @return
     * @see DavServletRequest#matchesIfHeader(DavResource)
     * @see IfHeader#matches(String, String, String)
     * @see DavResource#hasLock(org.apache.jackrabbit.webdav.lock.Type, org.apache.jackrabbit.webdav.lock.Scope)
     * @see org.apache.jackrabbit.webdav.lock.ActiveLock#getToken()
     */
    public boolean matchesIfHeader(DavResource resource) {
        // no ifheader, no resource or no write lock on resource
        // >> preconditions ok so far
        if (ifHeader == null || resource == null || !resource.hasLock(Type.WRITE, Scope.EXCLUSIVE)) {
            return true;
        }

        boolean isMatching = false;
        String lockToken = resource.getLock(Type.WRITE, Scope.EXCLUSIVE).getToken();
        if (lockToken != null) {
            // TODO: strongETag is missing
            isMatching = matchesIfHeader(resource.getHref(), lockToken, "");
        } // else: lockToken is null >> the if-header will not match.

        return isMatching;
    }

    /**
     * @see DavServletRequest#matchesIfHeader(String, String, String)
     * @see IfHeader#matches(String, String, String)
     */
    public boolean matchesIfHeader(String href, String token, String eTag) {
        return ifHeader.matches(href, token, eTag);
    }

    /**
     * Retrieve the header with the given header name and parse the CodedURL
     * value included.
     *
     * @param headerName
     * @return token present in the CodedURL header or <code>null</code> if
     * the header is not present.
     */
    private String getCodedURLHeader(String headerName) {
        String headerValue = null;
	String header = httpRequest.getHeader(headerName);
	if (header != null) {
	    int p1 = header.indexOf('<');
	    if (p1<0) {
		throw new IllegalArgumentException("Invalid CodedURL header value:"+header);
	    }
	    int p2 = header.indexOf('>', p1);
	    if (p2<0) {
		throw new IllegalArgumentException("Invalid CodedURL header value:"+header);
	    }
	    headerValue = header.substring(p1+1, p2);
	}
	return headerValue;
    }

    /**
     * Convert the String depth value to an integer.
     *
     * @param depth
     * @param defaultValue
     * @return integer representation of the given depth String or the given
     * defaultValue if depth is <code>null</code> or invalid.
     */
    private static int depthToInt(String depth, int defaultValue) {
        int d = defaultValue;
        if (depth != null) {
            if (depth.equalsIgnoreCase("infinity")) {
                d = DavConstants.DEPTH_INFINITY;
            } else if (depth.equals("0")) {
                d = DavConstants.DEPTH_0;
            } else if (depth.equals("1")) {
                d = DavConstants.DEPTH_1;
            }
        }
        return d;
    }

        //-----------------------------< TransactionDavServletRequest Interface >---
    /**
     *
     * @return
     * @see org.apache.jackrabbit.webdav.transaction.TransactionDavServletRequest#getTransactionId()
     */
    public String getTransactionId() {
        return getCodedURLHeader(TransactionConstants.HEADER_TRANSACTIONID);
    }

    /**
     *
     * @return
     * @see org.apache.jackrabbit.webdav.transaction.TransactionDavServletRequest#getTransactionInfo()
     */
    public TransactionInfo getTransactionInfo() {
        Document requestDocument = getRequestDocument();
        if (requestDocument != null) {
            try {
                return new TransactionInfo(requestDocument.getRootElement());
            } catch (IllegalArgumentException e) {
                log.error(e.getMessage());
            }
        }
        return null;
    }

    //-----------------------------< ObservationDavServletRequest Interface >---
    /**
     *
     * @return
     * @see org.apache.jackrabbit.webdav.observation.ObservationDavServletRequest#getSubscriptionId()
     */
    public String getSubscriptionId() {
        return getCodedURLHeader(ObservationConstants.HEADER_SUBSCRIPTIONID);
    }

    /**
     *
     * @return
     * @see org.apache.jackrabbit.webdav.observation.ObservationDavServletRequest#getSubscriptionInfo()
     */
    public SubscriptionInfo getSubscriptionInfo() {
        Document requestDocument = getRequestDocument();
        if (requestDocument != null) {
            Element root = requestDocument.getRootElement();
            if (ObservationConstants.XML_SUBSCRIPTIONINFO.equals(root.getName())) {
                int depth = getDepth(DavConstants.DEPTH_0);
                return new SubscriptionInfo(root, getTimeout(), depth == DavConstants.DEPTH_INFINITY);
            }
        }
        return null;
    }

    //--------------------------------< OrderingDavServletRequest Interface >---
    /**
     *
     * @return
     * @see org.apache.jackrabbit.webdav.ordering.OrderingDavServletRequest#getOrderingType()
     */
    public String getOrderingType() {
        return getHeader(OrderingConstants.HEADER_ORDERING_TYPE);
    }

    /**
     *
     * @return
     * @see org.apache.jackrabbit.webdav.ordering.OrderingDavServletRequest#getPosition()
     */
    public Position getPosition() {
        String h = getHeader(OrderingConstants.HEADER_POSITION);
        Position pos = null;
        if (h != null) {
            String[] typeNSegment = h.split("\\s");
            if (typeNSegment.length == 2) {
                try {
                    pos = new Position(typeNSegment[0], typeNSegment[1]);
                } catch (IllegalArgumentException e) {
                    log.error("Cannot parse Position header: "+e.getMessage());
                }
            }
        }
        return pos;
    }

    /**
     *
     * @return <code>OrderPatch</code> object representing the orderpatch request
     * body or <code>null</code> if the
     * @see org.apache.jackrabbit.webdav.ordering.OrderingDavServletRequest#getOrderPatch()
     */
    public OrderPatch getOrderPatch() {
        OrderPatch op = null;
        Document requestDocument = getRequestDocument();
        if (requestDocument != null) {
            Element root = requestDocument.getRootElement();
            if (!OrderingConstants.XML_ORDERPATCH.equals(root.getName()) ||
                    root.getChild(OrderingConstants.XML_ORDERING_TYPE) == null) {
                log.error("ORDERPATH request body must start with an 'orderpatch' element, which must contain an 'ordering-type' child element.");
                return op;
            }

            try {
                op = new OrderPatch(root);
            } catch (IllegalArgumentException e) {
                log.error(e.getMessage());
            }
        } else {
            log.error("Error while building xml document from ORDERPATH request body.");
        }
        return op;
    }

    //-------------------------------------< DeltaVServletRequest interface >---
    /**
     * @see org.apache.jackrabbit.webdav.version.DeltaVServletRequest#getLabel()
     */
    public String getLabel() {
        String label = getHeader(DeltaVConstants.HEADER_LABEL);
        if (label != null) {
            label = Text.unescape(label);
        }
        return label;
    }

    /**
     * @see org.apache.jackrabbit.webdav.version.DeltaVServletRequest#getLabelInfo()
     */
    public LabelInfo getLabelInfo() {
        LabelInfo lInfo = null;
        Document requestDocument = getRequestDocument();
        if (requestDocument != null) {
            Element root = requestDocument.getRootElement();
            int depth = getDepth(DavConstants.DEPTH_0);
            try {
               lInfo = new LabelInfo(root, depth);
            } catch (IllegalArgumentException e) {
                log.error(e.getMessage());
            }
        }
        return lInfo;
    }

    /**
     * @see org.apache.jackrabbit.webdav.version.DeltaVServletRequest#getMergeInfo()
     */
    public MergeInfo getMergeInfo() {
        MergeInfo mInfo = null;
        Document requestDocument = getRequestDocument();
        if (requestDocument != null) {
            try {
               mInfo = new MergeInfo(requestDocument.getRootElement());
            } catch (IllegalArgumentException e) {
                log.error(e.getMessage());
            }
        }
        return mInfo;
    }

    /**
     * @see org.apache.jackrabbit.webdav.version.DeltaVServletRequest#getUpdateInfo()
     */
    public UpdateInfo getUpdateInfo() {
        UpdateInfo uInfo = null;
        Document requestDocument = getRequestDocument();
        if (requestDocument != null) {
            try {
               uInfo = new UpdateInfo(requestDocument.getRootElement());
            } catch (IllegalArgumentException e) {
                log.error(e.getMessage());
            }
        }
        return uInfo;
    }

    /**
     * @see org.apache.jackrabbit.webdav.version.DeltaVServletRequest#getReportInfo()
     */
    public ReportInfo getReportInfo() {
        ReportInfo rInfo = null;
        Document requestDocument = getRequestDocument();
        if (requestDocument != null) {
            rInfo = new ReportInfo(requestDocument.getRootElement(), getDepth(DavConstants.DEPTH_0));
        }
        return rInfo;
    }

    /**
     * @see org.apache.jackrabbit.webdav.version.DeltaVServletRequest#getOptionsInfo()
     */
    public OptionsInfo getOptionsInfo() {
        OptionsInfo info = null;
        Document requestDocument = getRequestDocument();
        if (requestDocument != null) {
            info = new OptionsInfo(requestDocument.getRootElement());
        }
        return info;
    }

    //---------------------------------------< HttpServletRequest interface >---
    public String getAuthType() {
        return httpRequest.getAuthType();
    }

    public Cookie[] getCookies() {
        return httpRequest.getCookies();
    }

    public long getDateHeader(String s) {
        return httpRequest.getDateHeader(s);
    }

    public String getHeader(String s) {
        return httpRequest.getHeader(s);
    }

    public Enumeration getHeaders(String s) {
        return httpRequest.getHeaders(s);
    }

    public Enumeration getHeaderNames() {
        return httpRequest.getHeaderNames();
    }

    public int getIntHeader(String s) {
        return httpRequest.getIntHeader(s);
    }

    public String getMethod() {
        return httpRequest.getMethod();
    }

    public String getPathInfo() {
        return httpRequest.getPathInfo();
    }

    public String getPathTranslated() {
        return httpRequest.getPathTranslated();
    }

    public String getContextPath() {
        return httpRequest.getContextPath();
    }

    public String getQueryString() {
        return httpRequest.getQueryString();
    }

    public String getRemoteUser() {
        return httpRequest.getRemoteUser();
    }

    public boolean isUserInRole(String s) {
        return httpRequest.isUserInRole(s);
    }

    public Principal getUserPrincipal() {
        return httpRequest.getUserPrincipal();
    }

    public String getRequestedSessionId() {
        return httpRequest.getRequestedSessionId();
    }

    public String getRequestURI() {
        return httpRequest.getRequestURI();
    }

    public StringBuffer getRequestURL() {
        return httpRequest.getRequestURL();
    }

    public String getServletPath() {
        return httpRequest.getServletPath();
    }

    public HttpSession getSession(boolean b) {
        return httpRequest.getSession(b);
    }

    public HttpSession getSession() {
        return httpRequest.getSession();
    }

    public boolean isRequestedSessionIdValid() {
        return httpRequest.isRequestedSessionIdValid();
    }

    public boolean isRequestedSessionIdFromCookie() {
        return httpRequest.isRequestedSessionIdFromCookie();
    }

    public boolean isRequestedSessionIdFromURL() {
        return httpRequest.isRequestedSessionIdFromURL();
    }

    public boolean isRequestedSessionIdFromUrl() {
        return httpRequest.isRequestedSessionIdFromUrl();
    }

    public Object getAttribute(String s) {
        return httpRequest.getAttribute(s);
    }

    public Enumeration getAttributeNames() {
        return httpRequest.getAttributeNames();
    }

    public String getCharacterEncoding() {
        return httpRequest.getCharacterEncoding();
    }

    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
        httpRequest.setCharacterEncoding(s);
    }

    public int getContentLength() {
        return httpRequest.getContentLength();
    }

    public String getContentType() {
        return httpRequest.getContentType();
    }

    public ServletInputStream getInputStream() throws IOException {
        return httpRequest.getInputStream();
    }

    public String getParameter(String s) {
        return httpRequest.getParameter(s);
    }

    public Enumeration getParameterNames() {
        return httpRequest.getParameterNames();
    }

    public String[] getParameterValues(String s) {
        return httpRequest.getParameterValues(s);
    }

    public Map getParameterMap() {
        return httpRequest.getParameterMap();
    }

    public String getProtocol() {
        return httpRequest.getProtocol();
    }

    public String getScheme() {
        return httpRequest.getScheme();
    }

    public String getServerName() {
        return httpRequest.getServerName();
    }

    public int getServerPort() {
        return httpRequest.getServerPort();
    }

    public BufferedReader getReader() throws IOException {
        return httpRequest.getReader();
    }

    public String getRemoteAddr() {
        return httpRequest.getRemoteAddr();
    }

    public String getRemoteHost() {
        return httpRequest.getRemoteHost();
    }

    public void setAttribute(String s, Object o) {
        httpRequest.setAttribute(s, o);
    }

    public void removeAttribute(String s) {
       httpRequest.removeAttribute(s);
    }

    public Locale getLocale() {
        return httpRequest.getLocale();
    }

    public Enumeration getLocales() {
        return httpRequest.getLocales();
    }

    public boolean isSecure() {
        return httpRequest.isSecure();
    }

    public RequestDispatcher getRequestDispatcher(String s) {
        return httpRequest.getRequestDispatcher(s);
    }

    public String getRealPath(String s) {
        return httpRequest.getRealPath(s);
    }
}