/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.webdav.client.methods;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.ordering.OrderPatch;
import org.apache.jackrabbit.webdav.ordering.Position;
import org.apache.jackrabbit.webdav.ordering.OrderingConstants;

import java.io.IOException;

/**
 * <code>OrderPatchMethod</code>...
 */
public class OrderPatchMethod extends DavMethodBase {

    private static Logger log = Logger.getLogger(OrderPatchMethod.class);

    /**
     * Create a new <code>OrderPatchMethod</code> with the given order patch
     * object.
     *
     * @param uri
     * @param orderPatch
     */
    public OrderPatchMethod(String uri, OrderPatch orderPatch) throws IOException {
        super(uri);
        setRequestHeader(DavConstants.HEADER_CONTENT_TYPE, "text/xml; charset=UTF-8");
        setRequestBody(orderPatch);
    }

    /**
     * Create a new <code>OrderPatchMethod</code> that reorders the members
     * of the resource identified by 'uri': the member identified by 'memberSegment'
     * is moved to the first or to the last position, respectively.<br>
     * See the constructor taking an <code>OrderPatch</code> object for a ORDERPATCH call
     * that reorders multiple members at once.
     *
     * @param uri
     * @param orderingHref
     * @param memberSegment
     * @param first
     */
    public OrderPatchMethod(String uri, String orderingHref, String memberSegment, boolean first) throws IOException {
        super(uri);
        String orderType = (first) ? OrderingConstants.XML_FIRST : OrderingConstants.XML_LAST;
        Position p = new Position(orderType);
        OrderPatch op = new OrderPatch(orderingHref, new OrderPatch.Member(memberSegment, p));
        setRequestHeader(DavConstants.HEADER_CONTENT_TYPE, "text/xml; charset=UTF-8");
        setRequestBody(op);
    }

    /**
     * Create a new <code>OrderPatchMethod</code> that reorders the members
     * of the resource identified by 'uri': the member identified by 'memberSegment'
     * is reordered before or after the member identified by 'targetMemberSegmet'.<br>
     * See the constructor taking an <code>OrderPatch</code> object for a ORDERPATCH call
     * that reorders multiple members at once.
     *
     * @param uri
     * @param orderingHref
     * @param memberSegment
     * @param targetMemberSegmet
     * @param above
     */
    public OrderPatchMethod(String uri, String orderingHref, String memberSegment, String targetMemberSegmet, boolean above) throws IOException {
        super(uri);
        String orderType = (above) ? OrderingConstants.XML_AFTER : OrderingConstants.XML_BEFORE;
        Position p = new Position(orderType, targetMemberSegmet);
        OrderPatch op = new OrderPatch(orderingHref, new OrderPatch.Member(memberSegment, p));
        setRequestHeader(DavConstants.HEADER_CONTENT_TYPE, "text/xml; charset=UTF-8");
        setRequestBody(op);
    }

    /**
     * @see org.apache.commons.httpclient.HttpMethod#getName()
     */
    public String getName() {
        return DavMethods.METHOD_ORDERPATCH;
    }
}