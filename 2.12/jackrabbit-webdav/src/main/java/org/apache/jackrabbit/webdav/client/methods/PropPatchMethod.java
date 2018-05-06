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
import java.util.List;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpState;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.Status;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameIterator;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.PropEntry;
import org.apache.jackrabbit.webdav.property.ProppatchInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>PropPatchMethod</code>...
 */
public class PropPatchMethod extends DavMethodBase implements DavConstants {

    private static Logger log = LoggerFactory.getLogger(PropPatchMethod.class);

    private DavException responseException;
    private DavPropertyNameSet propertyNames;

    /**
     *
     * @param uri
     * @param changeList list of DavProperty (for 'set') and DavPropertyName
     * (for 'remove') entries.
     * @throws IOException
     */
    public PropPatchMethod(String uri, List<? extends PropEntry> changeList) throws IOException {
        super(uri);
        ProppatchInfo info = new ProppatchInfo(changeList);
        setRequestBody(info);
        this.propertyNames = info.getAffectedProperties();
    }

    public PropPatchMethod(String uri, DavPropertySet setProperties,
                           DavPropertyNameSet removeProperties) throws IOException {
        super(uri);
        ProppatchInfo info = new ProppatchInfo(setProperties, removeProperties);
        setRequestBody(info);
        this.propertyNames = info.getAffectedProperties();
    }

    //---------------------------------------------------------< HttpMethod >---
    /**
     * @see org.apache.commons.httpclient.HttpMethod#getName()
     */
    @Override
    public String getName() {
        return DavMethods.METHOD_PROPPATCH;
    }

    //------------------------------------------------------< DavMethodBase >---
    /**
     *
     * @param statusCode
     * @return true if status code is {@link DavServletResponse#SC_MULTI_STATUS 207 (Multi-Status)}.
     * For compliance reason {@link DavServletResponse#SC_OK 200 (OK)} is
     * interpreted as successful response as well.
     */
    @Override
    protected boolean isSuccess(int statusCode) {
        return statusCode == DavServletResponse.SC_MULTI_STATUS || statusCode == DavServletResponse.SC_OK;
    }

    /**
     *
     * @param multiStatus
     * @param httpState
     * @param httpConnection
     */
    @Override
    protected void processMultiStatusBody(MultiStatus multiStatus, HttpState httpState, HttpConnection httpConnection) {
        // check of OK response contains all set/remove properties
        MultiStatusResponse[] resp = multiStatus.getResponses();
        if (resp.length != 1) {
            log.warn("Expected a single multi-status response in PROPPATCH, but got " + resp.length + " elements.");
        }
        boolean success = true;

        // only check the first ms-response
        if (resp.length == 1) {
            MultiStatusResponse r = resp[0];

            if (r.isPropStat()) {
                DavPropertyNameSet okSet = r.getPropertyNames(DavServletResponse.SC_OK);
                if (okSet.isEmpty()) {
                    log.debug("PROPPATCH failed: No 'OK' response found for resource " + r.getHref());
                    success = false;
                } else {
                    DavPropertyNameIterator it = propertyNames.iterator();
                    while (it.hasNext()) {
                        DavPropertyName pn = it.nextPropertyName();
                        success = okSet.remove(pn);
                    }
                }
                if (!okSet.isEmpty()) {
                    StringBuffer b = new StringBuffer("The following properties outside of the original request where set or removed: ");
                    DavPropertyNameIterator it = okSet.iterator();
                    while (it.hasNext()) {
                        b.append(it.nextPropertyName().toString()).append("; ");
                    }
                    log.warn(b.toString());
                }
            }
            else {
                int status = r.getStatus()[0].getStatusCode();
                success = status == DavServletResponse.SC_OK;
                if (!success) {
                    log.warn("PROPPATCH failed: overall status of " + status);
                }
            }
        }
        // if  build the error message
        if (!success) {
            // TODO: array might be empty, no?
            Status[] st = resp[0].getStatus();
            // TODO: respect multiple error reasons (not only the first one)
            for (int i = 0; i < st.length && responseException == null; i ++) {
                switch (st[i].getStatusCode()) {
                    case DavServletResponse.SC_FAILED_DEPENDENCY:
                        // ignore
                        break;
                    default:
                        responseException = new DavException(st[i].getStatusCode());
                }
            }
        }
    }

    /**
     *
     * @return
     * @throws IOException
     * @see DavMethod#getResponseException()
     */
    @Override
    public DavException getResponseException() throws IOException {
        checkUsed();
        if (getSuccess()) {
            String msg = "Cannot retrieve exception from successful response.";
            log.warn(msg);
            throw new IllegalStateException(msg);
        }
        if (responseException != null) {
            return responseException;
        } else {
            return super.getResponseException();
        }
    }
}
