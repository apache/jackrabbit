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
package org.apache.jackrabbit.webdav.io;

import junit.framework.TestCase;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

/**
 * <code>OutputContextImplTest</code>...
 */
public class OutputContextImplTest extends TestCase {

    public void testSetContentLength() {
        HttpServletResponse response = new DummyResponse() {
            @Override
            public void setContentLength(int len) {
                assertTrue(len >= 0);
            }
            @Override
            public void setHeader(String name, String value) {
                assertTrue(Long.parseLong(value) > Integer.MAX_VALUE);
            }
            @Override
            public String getContentType() {
                return null;
            }
            @Override
            public void setCharacterEncoding(String charset) {
            }
            @Override
            public int getStatus() {
                return 0;
            }
            @Override
            public String getHeader(String name) {
                return null;
            }
            @Override
            public Collection<String> getHeaders(String name) {
                return null;
            }
            @Override
            public Collection<String> getHeaderNames() {
                return null;
            }
            @Override
            public void setContentLengthLong(long len) {
            }
        };

        OutputContext ctx = new OutputContextImpl(response, null);

        ctx.setContentLength(Long.MAX_VALUE);
        ctx.setContentLength(Long.MIN_VALUE);
        ctx.setContentLength(Integer.MAX_VALUE);
        ctx.setContentLength((long) Integer.MAX_VALUE + 1);
        ctx.setContentLength(0);
        ctx.setContentLength(-1);
        ctx.setContentLength(12345);
    }

    private abstract class DummyResponse implements HttpServletResponse {

        public void addCookie(Cookie cookie) {
        }

        public boolean containsHeader(String name) {
            return false;
        }

        public String encodeURL(String url) {
            return null;
        }

        public String encodeRedirectURL(String url) {
            return null;
        }

        public String encodeUrl(String url) {
            return null;
        }

        public String encodeRedirectUrl(String url) {
            return null;
        }

        public void sendError(int sc, String msg) throws IOException {
        }

        public void sendError(int sc) throws IOException {
        }

        public void sendRedirect(String location) throws IOException {
        }

        public void setDateHeader(String name, long date) {
        }

        public void addDateHeader(String name, long date) {
        }

        public void setHeader(String name, String value) {
        }

        public void addHeader(String name, String value) {
        }

        public void setIntHeader(String name, int value) {
        }

        public void addIntHeader(String name, int value) {
        }

        public void setStatus(int sc) {
        }

        public void setStatus(int sc, String sm) {
        }

        public String getCharacterEncoding() {
            return null;
        }

        public ServletOutputStream getOutputStream() throws IOException {
            return null;
        }

        public PrintWriter getWriter() throws IOException {
            return null;
        }

        public void setContentLength(int len) {
        }

        public void setContentType(String type) {
        }

        public void setBufferSize(int size) {
        }

        public int getBufferSize() {
            return 0;
        }

        public void flushBuffer() throws IOException {
        }

        public void resetBuffer() {
        }

        public boolean isCommitted() {
            return false;
        }

        public void reset() {
        }

        public void setLocale(Locale loc) {
        }

        public Locale getLocale() {
            return null;
        }
    }
}
