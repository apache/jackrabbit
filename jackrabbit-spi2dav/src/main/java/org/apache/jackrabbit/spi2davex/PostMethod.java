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

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.util.EncodingUtil;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>PostMethod</code>...*
 */
class PostMethod extends DavMethodBase {

    private static Logger log = LoggerFactory.getLogger(PostMethod.class);

    /** The Content-Type for www-form-urlencoded. */
    public static final String FORM_URL_ENCODED_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8";

    /**
     * The buffered request body consisting of <code>NameValuePair</code>s.
     */
    private List params = new ArrayList();

    public PostMethod(String uri) {
        super(uri);
        HttpMethodParams params = getParams();
        params.setContentCharset("UTF-8");
    }

    // -----------------------------------------------------< DavMethodBase >---
    protected boolean isSuccess(int statusCode) {
        return statusCode == DavServletResponse.SC_OK ||
                statusCode == DavServletResponse.SC_NO_CONTENT ||
                statusCode == DavServletResponse.SC_CREATED;
    }

    public String getName() {
        return DavMethods.METHOD_POST;
    }

    // COPIED FROM httpclient PostMethod.
    // ---------------------------------------------< EntityEnclosingMethod >---
    protected boolean hasRequestContent() {
        if (!params.isEmpty()) {
            return true;
        } else {
            return super.hasRequestContent();
        }
    }

    protected void clearRequestBody() {
        log.debug("enter PostMethod.clearRequestBody()");
        this.params.clear();
        super.clearRequestBody();
    }

    protected RequestEntity generateRequestEntity() {
        if (!this.params.isEmpty()) {
            // Use a ByteArrayRequestEntity instead of a StringRequestEntity.
            // This is to avoid potential encoding issues.  Form url encoded strings
            // are ASCII by definition but the content type may not be.  Treating the content
            // as bytes allows us to keep the current charset without worrying about how
            // this charset will effect the encoding of the form url encoded string.
            NameValuePair[] mvps = (NameValuePair[]) params.toArray(new NameValuePair[params.size()]);
            String content = EncodingUtil.formUrlEncode(mvps, getRequestCharSet());
            ByteArrayRequestEntity entity = new ByteArrayRequestEntity(
                    EncodingUtil.getAsciiBytes(content),
                    FORM_URL_ENCODED_CONTENT_TYPE
            );
            return entity;
        } else {
            return super.generateRequestEntity();
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Adds a new parameter to be used in the POST request body.
     *
     * @param paramName The parameter name to add.
     * @param paramValue The parameter value to add.
     * @throws IllegalArgumentException if either argument is null
     */
    public void addParameter(String paramName, String paramValue)
            throws IllegalArgumentException {
        log.debug("enter PostMethod.addParameter(String, String)");

        if ((paramName == null) || (paramValue == null)) {
            throw new IllegalArgumentException(
                    "Arguments to addParameter(String, String) cannot be null");
        }
        super.clearRequestBody();
        params.add(new NameValuePair(paramName, paramValue));
    }
    // COPIED FROM httpclient PostMethod.
}