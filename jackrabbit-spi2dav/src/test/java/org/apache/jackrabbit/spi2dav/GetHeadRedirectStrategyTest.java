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
package org.apache.jackrabbit.spi2dav;

import java.net.URI;

import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.jackrabbit.webdav.DavMethods;
import org.junit.Assert;
import org.junit.Test;

/**
 * Checks that automatic redirects stay restricted to the safe methods, as they were
 * under HttpClient 4. HttpClient 5 would otherwise follow redirects for every method.
 */
public class GetHeadRedirectStrategyTest {

    private boolean isRedirected(String method, int status, boolean withLocation) throws Exception {
        BasicClassicHttpRequest request = new BasicClassicHttpRequest(method, URI.create("http://localhost/a"));
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(status);
        if (withLocation) {
            response.setHeader(HttpHeaders.LOCATION, "http://localhost/b");
        }
        return GetHeadRedirectStrategy.INSTANCE.isRedirected(request, response, new BasicHttpContext());
    }

    @Test
    public void testSafeMethodsAreRedirected() throws Exception {
        for (int status : new int[] { HttpStatus.SC_MOVED_PERMANENTLY, HttpStatus.SC_MOVED_TEMPORARILY,
                HttpStatus.SC_TEMPORARY_REDIRECT, HttpStatus.SC_PERMANENT_REDIRECT }) {
            Assert.assertTrue("GET should follow " + status, isRedirected("GET", status, true));
            Assert.assertTrue("HEAD should follow " + status, isRedirected("HEAD", status, true));
        }
    }

    @Test
    public void testWriteMethodsAreNotRedirected() throws Exception {
        String[] methods = { "PUT", "DELETE", "POST", DavMethods.METHOD_MOVE, DavMethods.METHOD_COPY,
                DavMethods.METHOD_PROPPATCH, DavMethods.METHOD_MKCOL, DavMethods.METHOD_LOCK };
        for (String method : methods) {
            for (int status : new int[] { HttpStatus.SC_MOVED_PERMANENTLY, HttpStatus.SC_MOVED_TEMPORARILY,
                    HttpStatus.SC_TEMPORARY_REDIRECT, HttpStatus.SC_PERMANENT_REDIRECT }) {
                Assert.assertFalse(method + " must not follow " + status, isRedirected(method, status, true));
            }
        }
    }

    /**
     * PROPFIND is read-only but was not redirected by HttpClient 4 either, since only
     * GET and HEAD were on the list.
     */
    @Test
    public void testPropfindIsNotRedirected() throws Exception {
        Assert.assertFalse(isRedirected(DavMethods.METHOD_PROPFIND, HttpStatus.SC_MOVED_PERMANENTLY, true));
    }

    @Test
    public void testSeeOtherIsAlwaysRedirected() throws Exception {
        // 303 directs the client to fetch a different resource with GET
        Assert.assertTrue(isRedirected("GET", HttpStatus.SC_SEE_OTHER, true));
        Assert.assertTrue(isRedirected(DavMethods.METHOD_MOVE, HttpStatus.SC_SEE_OTHER, true));
    }

    @Test
    public void testFoundRequiresLocationHeader() throws Exception {
        Assert.assertTrue(isRedirected("GET", HttpStatus.SC_MOVED_TEMPORARILY, true));
        Assert.assertFalse(isRedirected("GET", HttpStatus.SC_MOVED_TEMPORARILY, false));
    }

    @Test
    public void testNonRedirectStatusIsNotRedirected() throws Exception {
        for (int status : new int[] { HttpStatus.SC_OK, HttpStatus.SC_NOT_MODIFIED, HttpStatus.SC_USE_PROXY,
                HttpStatus.SC_MULTI_STATUS, HttpStatus.SC_NOT_FOUND }) {
            Assert.assertFalse("must not follow " + status, isRedirected("GET", status, true));
        }
    }
}
