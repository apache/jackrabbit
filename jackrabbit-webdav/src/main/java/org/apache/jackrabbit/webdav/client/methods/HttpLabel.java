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

import java.io.IOException;
import java.net.URI;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.header.DepthHeader;
import org.apache.jackrabbit.webdav.version.LabelInfo;

/**
 * Represents an HTTP LABEL request.
 * 
 * @see <a href="http://webdav.org/specs/rfc3253.html#rfc.section.8.2">RFC 3253, Section 8.2</a>
 * @since 2.13.6
 */
public class HttpLabel extends BaseDavRequest {

    public HttpLabel(URI uri, LabelInfo labelInfo) throws IOException {
        super(DavMethods.METHOD_LABEL, uri);
        DepthHeader dh = new DepthHeader(labelInfo.getDepth());
        super.setHeader(dh.getHeaderName(), dh.getHeaderValue());
        super.setEntity(XmlEntity.create(labelInfo));
    }

    public HttpLabel(String uri, LabelInfo labelInfo) throws IOException {
        this(URI.create(uri), labelInfo);
    }

    @Override
    public boolean succeeded(ClassicHttpResponse response) {
        int statusCode = response.getCode();
        return statusCode == DavServletResponse.SC_OK;
    }
}
