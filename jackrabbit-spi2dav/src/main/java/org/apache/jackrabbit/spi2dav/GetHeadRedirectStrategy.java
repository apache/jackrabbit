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

import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.protocol.HttpContext;

/**
 * Redirect strategy that only follows redirects for safe methods.
 * <p>
 * HttpClient 5 follows redirects for every method, whereas HttpClient 4 restricted
 * automatic redirects to GET and HEAD. Following a redirect automatically for a
 * WebDAV method such as MOVE, COPY, PUT or DELETE would silently retarget a write
 * at a different resource instead of surfacing the redirect to the caller, so this
 * strategy reproduces the HttpClient 4 rules.
 *
 * @see org.apache.hc.client5.http.impl.DefaultRedirectStrategy
 */
final class GetHeadRedirectStrategy extends DefaultRedirectStrategy {

    static final GetHeadRedirectStrategy INSTANCE = new GetHeadRedirectStrategy();

    @Override
    public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) {
        switch (response.getCode()) {
            case HttpStatus.SC_MOVED_TEMPORARILY:
                // 302 additionally required a Location header under HttpClient 4
                return isRedirectable(request.getMethod())
                        && response.getFirstHeader(HttpHeaders.LOCATION) != null;
            case HttpStatus.SC_MOVED_PERMANENTLY:
            case HttpStatus.SC_TEMPORARY_REDIRECT:
            case HttpStatus.SC_PERMANENT_REDIRECT:
                return isRedirectable(request.getMethod());
            case HttpStatus.SC_SEE_OTHER:
                // 303 tells the client to fetch a different resource with GET,
                // which is safe regardless of the original method
                return true;
            default:
                return false;
        }
    }

    private static boolean isRedirectable(String method) {
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
    }
}
