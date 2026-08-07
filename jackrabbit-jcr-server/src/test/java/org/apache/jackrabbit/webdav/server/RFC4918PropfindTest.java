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

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.HttpOptions;
import org.apache.jackrabbit.webdav.client.methods.HttpPropfind;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;

/**
 * Test cases for RFC 4918 PROPFIND functionality
 * (see <a href="http://www.webdav.org/specs/rfc4918.html#rfc.section.9.1">RFC 4918, Section 9.1</a>
 */

public class RFC4918PropfindTest extends WebDAVTestBase {

    public void testOptions() throws IOException {
        HttpOptions options = new HttpOptions(this.root);
        ClassicHttpResponse response = this.client.executeOpen(null, options, this.context);
        assertTrue(options.getDavComplianceClasses(response).contains("3"));
    }

    public void testPropfindInclude() throws IOException, DavException {

        String testuri = this.root + "iftest";

        int status;
        try {
            HttpPut put = new HttpPut(testuri);
            put.setEntity(new StringEntity("1"));
            status = this.client.executeOpen(null, put, this.context).getCode();
            assertEquals("status: " + status, 201, status);

            DavPropertyNameSet names = new DavPropertyNameSet();
            names.add(DeltaVConstants.COMMENT);
            HttpPropfind propfind = new HttpPropfind(testuri, DavConstants.PROPFIND_ALL_PROP_INCLUDE, names, 0);
            ClassicHttpResponse resp = this.client.executeOpen(null, propfind, this.context);
            status = resp.getCode();
            assertEquals(207, status);

            MultiStatus multistatus = propfind.getResponseBodyAsMultiStatus(resp);
            MultiStatusResponse[] responses = multistatus.getResponses();
            assertEquals(1, responses.length);

            MultiStatusResponse response = responses[0];
            DavPropertySet found = response.getProperties(200);
            DavPropertySet notfound = response.getProperties(404);

            assertTrue(found.contains(DeltaVConstants.COMMENT) || notfound.contains(DeltaVConstants.COMMENT));
        } finally {
            delete(testuri);
        }
    }
}
