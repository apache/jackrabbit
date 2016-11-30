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

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.header.DepthHeader;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.PropfindInfo;

/**
 * <code>PropFindMethod</code>, as specified in
 * <a href="http://www.webdav.org/specs/rfc4918.html#rfc.section.9.1">RFC 4918, Section 9.1</a>
 * <p>
 * Supported types:
 * <ul>
 *   <li>{@link DavConstants#PROPFIND_ALL_PROP}: all custom properties, 
 *   plus the live properties defined in RFC2518/RFC4918
 *   <li>{@link DavConstants#PROPFIND_ALL_PROP_INCLUDE}: same as 
 *   {@link DavConstants#PROPFIND_ALL_PROP} plus the properties specified
 *   in <code>propNameSet</code>
 *   <li>{@link DavConstants#PROPFIND_BY_PROPERTY}: just the properties
 *   specified in <code>propNameSet</code>
 *   <li>{@link DavConstants#PROPFIND_PROPERTY_NAMES}: just the property names
 * </ul>
 * <p>
 * Note: only WebDAV level 3 servers support {@link DavConstants#PROPFIND_ALL_PROP_INCLUDE},
 * older servers will ignore the extension and act as if {@link DavConstants#PROPFIND_ALL_PROP}
 * was used.
 */
public class PropFindMethod extends DavMethodBase {

    public PropFindMethod(String uri) throws IOException {
        this(uri, PROPFIND_ALL_PROP, new DavPropertyNameSet(), DEPTH_INFINITY);
    }

    public PropFindMethod(String uri, DavPropertyNameSet propNameSet, int depth)
        throws IOException {
        this(uri, PROPFIND_BY_PROPERTY, propNameSet, depth);
    }

    public PropFindMethod(String uri, int propfindType, int depth)
        throws IOException {
        this(uri, propfindType, new DavPropertyNameSet(), depth);
    }

    public PropFindMethod(String uri, int propfindType, DavPropertyNameSet propNameSet,
                           int depth) throws IOException {
        super(uri);

        DepthHeader dh = new DepthHeader(depth);
        setRequestHeader(dh.getHeaderName(), dh.getHeaderValue());

        PropfindInfo info = new PropfindInfo(propfindType, propNameSet);
        setRequestBody(info);
    }

    //---------------------------------------------------------< HttpMethod >---
    /**
     * @see org.apache.commons.httpclient.HttpMethod#getName()
     */
    @Override
    public String getName() {
        return DavMethods.METHOD_PROPFIND;
    }

    //------------------------------------------------------< DavMethodBase >---
    /**
     * @param statusCode
     * @return true if status code is {@link DavServletResponse#SC_MULTI_STATUS 207 (Multi-Status)}.
     */
    @Override
    protected boolean isSuccess(int statusCode) {
        return statusCode == DavServletResponse.SC_MULTI_STATUS;
    }
}
