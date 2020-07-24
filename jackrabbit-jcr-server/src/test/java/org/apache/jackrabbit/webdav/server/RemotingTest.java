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
package org.apache.jackrabbit.webdav.server;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.HttpPropfind;
import org.apache.jackrabbit.webdav.observation.ObservationConstants;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.w3c.dom.Element;

public class RemotingTest extends WebDAVTestBase {

    // simple test the verifies that we are indeed talking to the remoting
    // servlet
    public void testRoot() throws IOException, DavException {
        String testuri = this.remotingUri.toASCIIString() + "default/jcr:root";
        DavPropertyName pntn = DavPropertyName.create("primarynodetype", ObservationConstants.NAMESPACE);
        DavPropertyNameSet names = new DavPropertyNameSet();
        names.add(pntn);

        HttpPropfind propfind = new HttpPropfind(testuri, DavConstants.PROPFIND_BY_PROPERTY, names, 0);
        HttpResponse resp = this.client.execute(propfind, this.context);
        int status = resp.getStatusLine().getStatusCode();
        assertEquals(207, status);

        MultiStatus multistatus = propfind.getResponseBodyAsMultiStatus(resp);
        MultiStatusResponse[] responses = multistatus.getResponses();
        assertEquals(1, responses.length);

        MultiStatusResponse response = responses[0];
        DavPropertySet found = response.getProperties(200);
        DavProperty<?> pnt = found.get(pntn);
        Element el = (Element) pnt.getValue();
        assertEquals("rep:root", DomUtil.getText((Element) (el.getFirstChild())));
    }
}
