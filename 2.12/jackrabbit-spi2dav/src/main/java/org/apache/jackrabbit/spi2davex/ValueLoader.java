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

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.jackrabbit.commons.webdav.JcrRemotingConstants;
import org.apache.jackrabbit.spi2dav.ExceptionConverter;
import org.apache.jackrabbit.spi2dav.ItemResourceConstants;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>ValueLoader</code>...
 */
class ValueLoader {

    private final HttpClient client;

    ValueLoader(HttpClient client) {
        this.client = client;
    }

    void loadBinary(String uri, int index, Target target) throws RepositoryException, IOException {
        GetMethod method = new GetMethod(uri);
        try {
            int statusCode = client.executeMethod(method);
            if (statusCode == DavServletResponse.SC_OK) {
                target.setStream(method.getResponseBodyAsStream());
            } else {
                throw ExceptionConverter.generate(new DavException(statusCode, ("Unable to load binary at " + uri + " - Status line = " + method.getStatusLine().toString())));
            }
        } finally {
            method.releaseConnection();
        }
    }

    public Map<String, String> loadHeaders(String uri, String[] headerNames) throws IOException,
            RepositoryException {
        HeadMethod method = new HeadMethod(uri);
        try {
            int statusCode = client.executeMethod(method);
            if (statusCode == DavServletResponse.SC_OK) {
                Map<String, String> headers = new HashMap<String, String>();
                for (String name : headerNames) {
                    Header hdr = method.getResponseHeader(name);
                    if (hdr != null) {
                        headers.put(name, hdr.getValue());
                    }
                }
                return headers;
            } else {
                throw ExceptionConverter.generate(new DavException(statusCode, ("Unable to load headers at " + uri + " - Status line = " + method.getStatusLine().toString())));
            }
        } finally {
            method.releaseConnection();
        }
    }

    int loadType(String uri) throws RepositoryException, IOException {
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(JcrRemotingConstants.JCR_TYPE_LN, ItemResourceConstants.NAMESPACE);

        DavMethodBase method = null;
        try {
            method = new PropFindMethod(uri, nameSet, DavConstants.DEPTH_0);
            client.executeMethod(method);
            method.checkSuccess();

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
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
            if (method != null) {
                method.releaseConnection();
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
