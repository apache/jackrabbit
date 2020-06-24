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
package org.apache.jackrabbit.server;

import junit.framework.TestCase;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.jcr.LoginException;
import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.jcr.GuestCredentials;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Locale;
import java.util.HashMap;
import java.security.Principal;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.BufferedReader;

/**
 * <code>BasicCredentialsProviderTest</code>...
 */
public class BasicCredentialsProviderTest extends TestCase {

    public void testMissingDefaultHeader() throws ServletException {
        CredentialsProvider cb = new BasicCredentialsProvider(null);
        try {
            Credentials creds = cb.getCredentials(new RequestImpl(null));
            fail("LoginException expected");
        } catch (LoginException e) {
            // ok
        }
    }

    public void testGuestCredentialsDefaultHeader() throws ServletException, LoginException {
        CredentialsProvider cb = new BasicCredentialsProvider(BasicCredentialsProvider.GUEST_DEFAULT_HEADER_VALUE);
        Credentials creds = cb.getCredentials(new RequestImpl(null));

        assertTrue(creds instanceof GuestCredentials);
    }

    public void testEmptyDefaultHeader() throws ServletException, LoginException {
        CredentialsProvider cb = new BasicCredentialsProvider(BasicCredentialsProvider.EMPTY_DEFAULT_HEADER_VALUE);
        Credentials creds = cb.getCredentials(new RequestImpl(null));

        assertNull(creds);
    }

    public void testDefaultPassword() throws ServletException, LoginException {
        Map<String, char[]> m = new HashMap<String, char[]>();
        m.put("userId", new char[0]);
        m.put("userId:", new char[0]);
        m.put("userId:pw", "pw".toCharArray());

        for (String uid : m.keySet()) {
            char[] pw = m.get(uid);

            CredentialsProvider cb = new BasicCredentialsProvider(uid);
            Credentials creds = cb.getCredentials(new RequestImpl(null));

            assertNotNull(creds);
            assertTrue(creds instanceof SimpleCredentials);
            assertEquals("userId", ((SimpleCredentials) creds).getUserID());
            if (pw.length == 0) {
                assertEquals(0, ((SimpleCredentials) creds).getPassword().length);
            } else {
                assertEquals(new String(pw), new String(((SimpleCredentials) creds).getPassword()));
            }
        }
    }




    private class RequestImpl implements HttpServletRequest {

        private final String authHeader;

        private RequestImpl(String authHeader) {
            this.authHeader = authHeader;
        }

        public String getAuthType() {
            return null;
        }

        public Cookie[] getCookies() {
            return new Cookie[0];
        }

        public long getDateHeader(String name) {
            return 0;
        }

        public String getHeader(String name) {
            return authHeader;
        }

        public Enumeration<String> getHeaders(String name) {
            return null;
        }

        public Enumeration<String> getHeaderNames() {
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

        public Enumeration<String> getAttributeNames() {
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

        public Enumeration<String> getParameterNames() {
            return null;
        }

        public String[] getParameterValues(String name) {
            return new String[0];
        }

        public Map<String, String[]> getParameterMap() {
            return null;
        }

        public String getProtocol() {
            return null;
        }

        public String getScheme() {
            return null;
        }

        public String getServerName() {
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

        public Enumeration<Locale> getLocales() {
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
