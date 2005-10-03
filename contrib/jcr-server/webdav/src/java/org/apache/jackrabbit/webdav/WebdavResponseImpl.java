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

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.lock.*;
import org.apache.jackrabbit.webdav.observation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * WebdavResponseImpl implements the <code>WebdavResponse</code> interface.
 */
public class WebdavResponseImpl implements WebdavResponse {

    private static Logger log = Logger.getLogger(WebdavResponseImpl.class);

    private HttpServletResponse httpResponse;

    /**
     * Create a new <code>WebdavResponse</code>
     * 
     * @param httpResponse
     */
    public WebdavResponseImpl(HttpServletResponse httpResponse) {
        this.httpResponse = httpResponse;

        /* set cache control headers in order to deal with non-webdav complient
        * http1.1 or http1.0 proxies. >> see RFC2518 9.4.5 */
        addHeader("Pragma", "No-cache");  // http1.0
        addHeader("Cache-Control", "no-cache"); // http1.1
    }

    /**
     *
     * @param exception
     * @throws IOException
     * @see DavServletResponse#sendError(org.apache.jackrabbit.webdav.DavException)
     */
    public void sendError(DavException exception) throws IOException {
        Element errorElem = exception.getError();
        if (errorElem == null || errorElem.getChildren().size() == 0) {
            httpResponse.sendError(exception.getErrorCode(), exception.getStatusPhrase());
        } else {
            sendXmlResponse(new Document(exception.getError()), exception.getErrorCode());
        }
    }

    /**
     * Send a multistatus response.
     *
     * @param multistatus
     * @throws IOException
     * @see DavServletResponse#sendMultiStatus(org.apache.jackrabbit.webdav.MultiStatus)
     */
    public void sendMultiStatus(MultiStatus multistatus) throws IOException {
        sendXmlResponse(multistatus.toXml(), SC_MULTI_STATUS);
    }

    /**
     * Send response body for a lock request intended to create a new lock.
     *
     * @param lock
     * @throws java.io.IOException
     * @see DavServletResponse#sendLockResponse(org.apache.jackrabbit.webdav.lock.ActiveLock)
     */
    public void sendLockResponse(ActiveLock lock) throws IOException {
        httpResponse.setHeader(DavConstants.HEADER_LOCK_TOKEN, "<" + lock.getToken() + ">");

        Element propElem = new Element(DavConstants.XML_PROP, DavConstants.NAMESPACE);
	propElem.addContent(new LockDiscovery(lock).toXml());
	sendXmlResponse(new Document(propElem), SC_OK);
    }

    /**
     * Send response body for a lock request that was intended to refresh one
     * or several locks.
     *
     * @param locks
     * @throws java.io.IOException
     * @see DavServletResponse#sendRefreshLockResponse(org.apache.jackrabbit.webdav.lock.ActiveLock[])
     */
    public void sendRefreshLockResponse(ActiveLock[] locks) throws IOException {
        Element propElem = new Element(DavConstants.XML_PROP, DavConstants.NAMESPACE);
        propElem.addContent(new LockDiscovery(locks).toXml());
        sendXmlResponse(new Document(propElem), SC_OK);
    }

    /**
     * Send Xml response body.
     *
     * @param xmlDoc
     * @param status
     * @throws IOException
     * @see DavServletResponse#sendXmlResponse(Document, int);
     */
    public void sendXmlResponse(Document xmlDoc, int status) throws IOException {
        httpResponse.setStatus(status);
        if (xmlDoc != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Write dom tree into byte array output stream
            XMLOutputter xmli = new XMLOutputter(Format.getRawFormat());
            xmli.output(xmlDoc, out);
            byte[] bytes = out.toByteArray();
            httpResponse.setContentType("text/xml; charset=UTF-8");
            httpResponse.setContentLength(bytes.length);
            httpResponse.getOutputStream().write(bytes);
            out.close();
            out.flush();
        }
    }

    //----------------------------< ObservationDavServletResponse Interface >---
    /**
     *
     * @param subscription
     * @throws IOException
     * @see org.apache.jackrabbit.webdav.observation.ObservationDavServletResponse#sendSubscriptionResponse(org.apache.jackrabbit.webdav.observation.Subscription)
     */
    public void sendSubscriptionResponse(Subscription subscription) throws IOException {
        Element propElem = new Element(DavConstants.XML_PROP, DavConstants.NAMESPACE);
	propElem.addContent(new SubscriptionDiscovery(subscription).toXml());
	Document doc = new Document(propElem);
	sendXmlResponse(doc, SC_OK);
    }

    /**
     *
     * @param eventDiscovery
     * @throws IOException
     * @see org.apache.jackrabbit.webdav.observation.ObservationDavServletResponse#sendPollResponse(org.apache.jackrabbit.webdav.observation.EventDiscovery)
     */
    public void sendPollResponse(EventDiscovery eventDiscovery) throws IOException {
        Document pollDoc = new Document(eventDiscovery.toXml());
        sendXmlResponse(pollDoc, SC_OK);
    }

    //--------------------------------------< HttpServletResponse interface >---
    public void addCookie(Cookie cookie) {
        httpResponse.addCookie(cookie);
    }

    public boolean containsHeader(String s) {
        return httpResponse.containsHeader(s);
    }

    public String encodeURL(String s) {
        return httpResponse.encodeRedirectURL(s);
    }

    public String encodeRedirectURL(String s) {
        return httpResponse.encodeRedirectURL(s);
    }

    public String encodeUrl(String s) {
        return httpResponse.encodeUrl(s);
    }

    public String encodeRedirectUrl(String s) {
        return httpResponse.encodeRedirectURL(s);
    }

    public void sendError(int i, String s) throws IOException {
        httpResponse.sendError(i, s);
    }

    public void sendError(int i) throws IOException {
        httpResponse.sendError(i);
    }

    public void sendRedirect(String s) throws IOException {
        httpResponse.sendRedirect(s);
    }

    public void setDateHeader(String s, long l) {
        httpResponse.setDateHeader(s, l);
    }

    public void addDateHeader(String s, long l) {
        httpResponse.addDateHeader(s, l);
    }

    public void setHeader(String s, String s1) {
        httpResponse.setHeader(s, s1);
    }

    public void addHeader(String s, String s1) {
        httpResponse.addHeader(s, s1);
    }

    public void setIntHeader(String s, int i) {
        httpResponse.setIntHeader(s, i);
    }

    public void addIntHeader(String s, int i) {
        httpResponse.addIntHeader(s, i);
    }

    public void setStatus(int i) {
        httpResponse.setStatus(i);
    }

    public void setStatus(int i, String s) {
        httpResponse.setStatus(i, s);
    }

    public String getCharacterEncoding() {
        return httpResponse.getCharacterEncoding();
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return httpResponse.getOutputStream();
    }

    public PrintWriter getWriter() throws IOException {
        return httpResponse.getWriter();
    }

    public void setContentLength(int i) {

    }

    public void setContentType(String s) {
        httpResponse.setContentType(s);
    }

    public void setBufferSize(int i) {
        httpResponse.setBufferSize(i);
    }

    public int getBufferSize() {
        return httpResponse.getBufferSize();
    }

    public void flushBuffer() throws IOException {
        httpResponse.flushBuffer();
    }

    public void resetBuffer() {
        httpResponse.resetBuffer();
    }

    public boolean isCommitted() {
        return httpResponse.isCommitted();
    }

    public void reset() {
        httpResponse.reset();
    }

    public void setLocale(Locale locale) {
        httpResponse.setLocale(locale);
    }

    public Locale getLocale() {
        return httpResponse.getLocale();
    }
}
