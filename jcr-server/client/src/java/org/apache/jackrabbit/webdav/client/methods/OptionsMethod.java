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
import org.apache.jackrabbit.webdav.version.OptionsInfo;
import org.apache.jackrabbit.webdav.version.OptionsResponse;
import org.w3c.dom.Element;

import java.io.IOException;

/**
 * <code>OptionsMethod</code>...
 */
public class OptionsMethod extends DavMethodBase {

    private static Logger log = Logger.getLogger(OptionsMethod.class);

    public OptionsMethod(String uri) {
	super(uri);
    }

    public OptionsMethod(String uri, String[] optionsEntries) throws IOException {
        this(uri, new OptionsInfo(optionsEntries));
    }

    public OptionsMethod(String uri, OptionsInfo optionsInfo) throws IOException {
        super(uri);
        if (optionsInfo != null) {
            setRequestHeader(DavConstants.HEADER_CONTENT_TYPE, "text/xml; charset=UTF-8");
            setRequestBody(optionsInfo);
        }
    }

    /**
     * @see org.apache.commons.httpclient.HttpMethod#getName()
     */
    public String getName() {
	return DavMethods.METHOD_OPTIONS;
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public OptionsResponse getResponseAsOptionsResponse() throws IOException {
        checkUsed();
        OptionsResponse or = null;
        Element rBody = getRootElement();
        if (rBody != null) {
            or = OptionsResponse.createFromXml(rBody);
        }
        return or;
    }
}