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
package org.apache.jackrabbit.spi2davex;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.protocol.HttpContext;
import org.apache.jackrabbit.commons.webdav.JcrRemotingConstants;
import org.apache.jackrabbit.spi2dav.ExceptionConverter;
import org.apache.jackrabbit.spi2dav.ItemResourceConstants;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.HttpPropfind;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;

/**
 * <code>ValueLoader</code>...
 */
class ValueLoader {

    private final HttpClient client;
    private final HttpContext context;

    ValueLoader(HttpClient client, HttpContext context) {
        this.client = client;
        this.context = context;
    }

    void loadBinary(String uri, int index, Target target) throws RepositoryException, IOException {
        HttpGet request = new HttpGet(uri);
        try {
            HttpResponse response = client.execute(request, context);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == DavServletResponse.SC_OK) {
                target.setStream(response.getEntity().getContent());
            } else {
                throw ExceptionConverter.generate(new DavException(statusCode, ("Unable to load binary at " + uri + " - Status line = " + response.getStatusLine())));
            }
        } finally {
            request.releaseConnection();
        }
    }

    public Map<String, String> loadHeaders(String uri, String[] headerNames) throws IOException,
            RepositoryException {
        HttpHead request = new HttpHead(uri);
        try {
            HttpResponse response = client.execute(request, context);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == DavServletResponse.SC_OK) {
                Map<String, String> headers = new HashMap<String, String>();
                for (String name : headerNames) {
                    Header hdr = response.getFirstHeader(name);
                    if (hdr != null) {
                        headers.put(name, hdr.getValue());
                    }
                }
                return headers;
            } else {
                throw ExceptionConverter.generate(new DavException(statusCode, ("Unable to load headers at " + uri + " - Status line = " + response.getStatusLine().toString())));
            }
        } finally {
            request.releaseConnection();
        }
    }

    int loadType(String uri) throws RepositoryException, IOException {
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(JcrRemotingConstants.JCR_TYPE_LN, ItemResourceConstants.NAMESPACE);

        HttpPropfind request = null;
        try {
            request = new HttpPropfind(uri, nameSet, DavConstants.DEPTH_0);
            HttpResponse response = client.execute(request, context);
            request.checkSuccess(response);

            MultiStatusResponse[] responses = request.getResponseBodyAsMultiStatus(response).getResponses();
            if (responses.length == 1) {
                DavPropertySet props = responses[0].getProperties(DavServletResponse.SC_OK);
                DavProperty<?> type = props.get(JcrRemotingConstants.JCR_TYPE_LN, ItemResourceConstants.NAMESPACE);
                if (type != null) {
                    return PropertyType.valueFromName(type.getValue().toString());
                } else {
                    throw new RepositoryException("Internal error. Cannot retrieve property type at " + uri);
                }
            } else {
                throw new ItemNotFoundException("Internal error. Cannot retrieve property type at " + uri);
            }
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Internal interface
     */
    interface Target {
        /**
         * @param in
         * @throws IOException
         */
        void setStream(InputStream in) throws IOException;

        /**
         * Resets (clears) the state and this target is in the state as
         * prior to setStream()
         */
        void reset();
    }

}
