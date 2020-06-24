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
package org.apache.jackrabbit.webdav.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import junit.framework.TestCase;

/**
 * <code>CSRFUtilTest</code>...
 */
public class CSRFUtilTest extends TestCase {

    private static final String SERVER_NAME = "localhost";

    private static final String GET = "GET";
    private static final String POST = "POST";

    private static final List<String> validURLs = new ArrayList<String>();
    private static final List<String> invalidURLs = new ArrayList<String>();

    static {
        validURLs.add("http://localhost:4503/jackrabbit/server");
        validURLs.add("https://localhost:4503/jackrabbit/server");
        validURLs.add("https://localhost/jackrabbit/server");
        validURLs.add("//localhost/jackrabbit/server");
        validURLs.add("/jackrabbit/server");

        invalidURLs.add("http://invalidHost/test");
        invalidURLs.add("http://host1:8080/test");
        invalidURLs.add("http://user:pw@host2/test");
    }

    static String[] noContentType = new String[0];

    private static void testValid(CSRFUtil util, Collection<String> validURLs, String method, Set<String> contentTypes) {
        if (null == contentTypes) {
            for (String url : validURLs) {
                assertTrue(url, util.isValidRequest(createRequest(url, method, noContentType)));
            }
        } else {
            for (String contentType : contentTypes) {
                for (String url : validURLs) {
                    assertTrue(url, util.isValidRequest(createRequest(url, method, contentType)));
                }
            }
        }
    }

    private static void testInvalid(CSRFUtil util, Collection<String> invalidURLs, String method, Set<String> contentTypes) {
        if (null == contentTypes) {
            for (String url : validURLs) {
                assertFalse(url, util.isValidRequest(createRequest(url, method, noContentType)));
            }
        } else {
            for (String contentType : contentTypes) {
                for (String url : invalidURLs) {
                    assertFalse(url, util.isValidRequest(createRequest(url, method, contentType)));
                }
            }
        }
    }

    private static HttpServletRequest createRequest(String url, String method, String[] contentTypes) {
        return new DummyRequest(url, SERVER_NAME, method, contentTypes);
    }

    private static HttpServletRequest createRequest(String url, String method, String contentType) {
        return new DummyRequest(url, SERVER_NAME, method, new String[] { contentType });
    }

    public void testNullConfig() throws Exception {
        CSRFUtil util = new CSRFUtil(null);
        testValid(util, validURLs, POST, CSRFUtil.CONTENT_TYPES);
        testInvalid(util, invalidURLs, POST, CSRFUtil.CONTENT_TYPES);
    }

    public void testEmptyConfig() throws Exception {
        CSRFUtil util = new CSRFUtil("");
        testValid(util, validURLs, POST, CSRFUtil.CONTENT_TYPES);
        testInvalid(util, invalidURLs, POST, CSRFUtil.CONTENT_TYPES);
    }

    public void testNoReferrer() throws Exception {
        CSRFUtil util = new CSRFUtil("");
        testValid(util, validURLs, POST, CSRFUtil.CONTENT_TYPES);
        assertFalse("no referrer", util.isValidRequest(createRequest(null, POST, "text/plain")));
        assertFalse("no referrer", util.isValidRequest(createRequest(null, POST, noContentType)));
        assertFalse("no referrer", util.isValidRequest(createRequest(null, POST, "TEXT/PLAIN; foo=bar")));
        assertTrue("no referrer", util.isValidRequest(createRequest(null, POST, "application/json")));
        assertFalse("no referrer", util.isValidRequest(createRequest(null, POST, new String[] { "application/json", "foo/bar" })));
    }

    public void testDisabledConfig() throws Exception {
        CSRFUtil util = new CSRFUtil(CSRFUtil.DISABLED);
        testValid(util, validURLs, POST, CSRFUtil.CONTENT_TYPES);
        // since test is disabled any other referer host must be allowed
        testValid(util, invalidURLs, POST, CSRFUtil.CONTENT_TYPES);
    }

    public void testConfig() throws Exception {
        List<String> configs = new ArrayList<String>();
        configs.add("host1,host2");
        configs.add(" host1 , host2 ");
        configs.add("\rhost1,\rhost2\r");

        // hosts listed in the config must be valid
        List<String> otherHosts = new ArrayList<String>();
        otherHosts.add("http://host1:80/test");
        otherHosts.add("http://host1/test");
        otherHosts.add("https://user:pw@host2/test");

        List<String> invalidURLs = new ArrayList<String>();
        invalidURLs.add("http://invalidHost/test");
        invalidURLs.add("http://host3:8080/test");
        invalidURLs.add("https://user:pw@host4/test");

        for (String config : configs) {
            CSRFUtil util = new CSRFUtil(config);
            testValid(util, validURLs, POST, CSRFUtil.CONTENT_TYPES);
            testValid(util, otherHosts, POST, CSRFUtil.CONTENT_TYPES);
            testInvalid(util, invalidURLs, POST, CSRFUtil.CONTENT_TYPES);
        }
    }

    public void testMethodsAndMediaType() throws Exception {
        CSRFUtil util = new CSRFUtil("");
        testValid(util, invalidURLs, GET, CSRFUtil.CONTENT_TYPES);
        testValid(util, invalidURLs, POST, new HashSet<String>(Arrays.asList(new String[] {"application/json"})));
        testInvalid(util, invalidURLs, POST, CSRFUtil.CONTENT_TYPES);
    }

    private static final class DummyRequest implements HttpServletRequest {

        private final String referer;
        private final String serverName;
        private final String method;
        private final String[] contentTypes;

        private DummyRequest(String referer, String serverName, String method, String[] contentTypes) {
            this.referer = referer;
            this.serverName = serverName;
            this.method = method;
            this.contentTypes = contentTypes;
        }

        //---------------------------------------------< HttpServletRequest >---

        public String getHeader(String name) {
            if ("Referer".equalsIgnoreCase(name)) {
                return referer;
            } else {
                return null;
            }
        }

        public String getServerName() {
            return serverName;
        }

        public String getContentType() {
            return contentTypes.length == 0 ? null : contentTypes[0];
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public Enumeration getHeaders(String name) {
            if (name != null && contentTypes.length > 0 && name.toLowerCase(Locale.ENGLISH).equals("content-type")) {
                return new Vector(Arrays.asList(contentTypes)).elements();
            } else {
                return null;
            }
        }

        //---------------------------------------------------------< unused >---
        public String getAuthType() {
            return null;
        }
        public Cookie[] getCookies() {
            return new Cookie[0];
        }
        public long getDateHeader(String name) {
            return 0;
        }
        public Enumeration getHeaderNames() {
            return null;
        }
        public int getIntHeader(String name) {
            return 0;
        }
        public String getMethod() {
            return method;
        }
        public String getPathInfo() {
            return null;
        }
        public String getPathTranslated() {
            return null;
        }
        public String getContextPath() {
            return null;
        }
        public String getQueryString() {
            return null;
        }
        public String getRemoteUser() {
            return null;
        }
        public boolean isUserInRole(String role) {
            return false;
        }
        public Principal getUserPrincipal() {
            return null;
        }
        public String getRequestedSessionId() {
            return null;
        }
        public String getRequestURI() {
            return null;
        }
        public StringBuffer getRequestURL() {
            return null;
        }
        public String getServletPath() {
            return null;
        }
        public HttpSession getSession(boolean create) {
            return null;
        }
        public HttpSession getSession() {
            return null;
        }
        public boolean isRequestedSessionIdValid() {
            return false;
        }
        public boolean isRequestedSessionIdFromCookie() {
            return false;
        }
        public boolean isRequestedSessionIdFromURL() {
            return false;
        }
        public boolean isRequestedSessionIdFromUrl() {
            return false;
        }
        public Object getAttribute(String name) {
            return null;
        }
        public Enumeration getAttributeNames() {
            return null;
        }
        public String getCharacterEncoding() {
            return null;
        }
        public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

        }
        public int getContentLength() {
            return 0;
        }
        public ServletInputStream getInputStream() throws IOException {
            return null;
        }
        public String getParameter(String name) {
            return null;
        }
        public Enumeration getParameterNames() {
            return null;
        }
        public String[] getParameterValues(String name) {
            return new String[0];
        }
        public Map getParameterMap() {
            return null;
        }
        public String getProtocol() {
            return null;
        }
        public String getScheme() {
            return null;
        }
        public int getServerPort() {
            return 0;
        }
        public BufferedReader getReader() throws IOException {
            return null;
        }
        public String getRemoteAddr() {
            return null;
        }
        public String getRemoteHost() {
            return null;
        }
        public void setAttribute(String name, Object o) {

        }
        public void removeAttribute(String name) {

        }
        public Locale getLocale() {
            return null;
        }
        public Enumeration getLocales() {
            return null;
        }
        public boolean isSecure() {
            return false;
        }
        public RequestDispatcher getRequestDispatcher(String path) {
            return null;
        }
        public String getRealPath(String path) {
            return null;
        }
        public int getRemotePort() {
            return 0;
        }
        public String getLocalName() {
            return null;
        }
        public String getLocalAddr() {
            return null;
        }
        public int getLocalPort() {
            return 0;
        }
        public long getContentLengthLong() {
            return 0;
        }
        public ServletContext getServletContext() {
            return null;
        }
        public AsyncContext startAsync() throws IllegalStateException {
            return null;
        }
        public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
                throws IllegalStateException {
            return null;
        }
        public boolean isAsyncStarted() {
            return false;
        }
        public boolean isAsyncSupported() {
            return false;
        }
        public AsyncContext getAsyncContext() {
            return null;
        }
        public DispatcherType getDispatcherType() {
            return null;
        }
        public String changeSessionId() {
            return null;
        }
        public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
            return false;
        }
        public void login(String username, String password) throws ServletException {
        }
        public void logout() throws ServletException {
        }
        public Collection<Part> getParts() throws IOException, ServletException {
            return null;
        }
        public Part getPart(String name) throws IOException, ServletException {
            return null;
        }
        public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
            return null;
        }
    }
}