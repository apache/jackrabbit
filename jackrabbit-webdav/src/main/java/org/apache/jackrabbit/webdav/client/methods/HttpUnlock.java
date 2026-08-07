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
package org.apache.jackrabbit.webdav.client.methods;

import java.net.URI;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.header.CodedUrlHeader;

/**
 * Represents an HTTP UNLOCK request.
 * 
 * @see <a href="http://webdav.org/specs/rfc4918.html#rfc.section.9.11">RFC 4918, Section 9.11</a>
 * @since 2.13.6
 */
public class HttpUnlock extends BaseDavRequest {

    public HttpUnlock(URI uri, String lockToken) {
        super(DavMethods.METHOD_UNLOCK, uri);
        CodedUrlHeader lth = new CodedUrlHeader(DavConstants.HEADER_LOCK_TOKEN, lockToken);
        super.setHeader(lth.getHeaderName(), lth.getHeaderValue());
    }

    public HttpUnlock(String uri, String lockToken) {
        this(URI.create(uri), lockToken);
    }

    @Override
    public boolean succeeded(ClassicHttpResponse response) {
        int statusCode = response.getCode();
        return statusCode == DavServletResponse.SC_OK || statusCode == DavServletResponse.SC_NO_CONTENT;
    }
}