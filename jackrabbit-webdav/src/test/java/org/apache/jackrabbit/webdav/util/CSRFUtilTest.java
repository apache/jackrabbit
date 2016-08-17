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

import junit.framework.TestCase;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * <code>CSRFUtilTest</code>...
 */
public class CSRFUtilTest extends TestCase {

    private static final String SERVER_NAME = "localhost";

    private static final List<String> validURLs = new ArrayList<String>();

    static {
        validURLs.add(null);
        validURLs.add("http://localhost:4503/jackrabbit/server");
        validURLs.add("https://localhost:4503/jackrabbit/server");
        validURLs.add("https://localhost/jackrabbit/server");
    }

    private static void testValid(CSRFUtil util, Collection<String> validURLs) throws MalformedURLException {
        for (String url : validURLs) {
            assertTrue(url, util.isValidRequest(createRequest(url)));
        }
    }

    private static void testInvalid(CSRFUtil util, Collection<String> invalidURLs) throws MalformedURLException {
        for (String url : invalidURLs) {
            assertFalse(url, util.isValidRequest(createRequest(url)));
        }
    }

    private static HttpServletRequest createRequest(String url) {
        return new DummyRequest(url, SERVER_NAME);
    }

    public void testNullConfig() throws Exception {
        CSRFUtil util = new CSRFUtil(null);

        testValid(util, validURLs);

        List<String> invalidURLs = new ArrayList<String>();
        invalidURLs.add("http://invalidHost/test");
        invalidURLs.add("http://host1:8080/test");
        invalidURLs.add("http://user:pw@host2/test");
        testInvalid(util, invalidURLs);
    }

    public void testEmptyConfig() throws Exception {
        CSRFUtil util = new CSRFUtil("");
        testValid(util, validURLs);

        List<String> invalidURLs = new ArrayList<String>();
        invalidURLs.add("http://invalidHost/test");
        invalidURLs.add("http://host1:8080/test");
        invalidURLs.add("http://user:pw@host2/test");
        testInvalid(util, invalidURLs);
    }

    public void testDisabledConfig() throws Exception {
        CSRFUtil util = new CSRFUtil(CSRFUtil.DISABLED);
        testValid(util, validURLs);

        // since test is disabled any other referer host must be allowed
        List<String> otherHosts = new ArrayList<String>();
        otherHosts.add("http://validHost:80/test");
        otherHosts.add("http://host1/test");
        otherHosts.add("https://user:pw@host2/test");
        testValid(util, otherHosts);
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
            testValid(util, validURLs);
            testValid(util, otherHosts);
            testInvalid(util, invalidURLs);
        }
    }

    private static final class DummyRequest implements HttpServletRequest {

        private final String referer;
        private final String serverName;

        private DummyRequest(String referer, String serverName) {
            this.referer = referer;
            this.serverName = serverName;
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
        public Enumeration getHeaders(String name) {
            return null;
        }
        public Enumeration getHeaderNames() {
            return null;
        }
        public int getIntHeader(String name) {
            return 0;
        }
        public String getMethod() {
            return null;
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
        public String getContentType() {
            return null;
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
    }
}