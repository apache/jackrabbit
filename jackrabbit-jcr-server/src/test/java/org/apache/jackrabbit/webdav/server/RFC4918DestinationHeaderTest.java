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
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.jackrabbit.webdav.client.methods.HttpMove;

/**
 * Test cases for RFC 4918 Destination header functionality
 * (see <a href="http://www.webdav.org/specs/rfc4918.html#rfc.section.10.3">RFC 4918, Section 10.3</a>
 */
public class RFC4918DestinationHeaderTest extends WebDAVTestBase {

    public void testMove() throws IOException, URISyntaxException {

        String testuri = this.root + "movetest";
        String destinationuri = testuri + "2";
        String destinationpath = new URI(destinationuri).getRawPath();
        // make sure the scheme is removed
        assertFalse(destinationpath.contains(":"));

        HttpRequestBase requestBase = null;
        try {
            requestBase = new HttpPut(testuri);
            int status = this.client.execute(requestBase, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 200 || status == 201 || status == 204);
            requestBase.releaseConnection();

            // try to move outside the servlet's name space
            requestBase = new HttpMove(testuri, "/foobar", true);
            status = this.client.execute(requestBase, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 502);
            requestBase.releaseConnection();

            // try a relative path
            requestBase = new HttpMove(testuri, "foobar", true);
            status = this.client.execute(requestBase, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 400);
            requestBase.releaseConnection();

            requestBase = new HttpMove(testuri, destinationpath, true);
            status = this.client.execute(requestBase, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 200 || status == 201 || status == 204);
            requestBase.releaseConnection();

            requestBase = new HttpHead(destinationuri);
            status = this.client.execute(requestBase, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 200);
            requestBase.releaseConnection();

            requestBase = new HttpHead(testuri);
            status = this.client.execute(requestBase, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 404);
        } finally {
            requestBase.releaseConnection();
            requestBase = new HttpDelete(testuri);
            int status = this.client.execute(requestBase, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 200 || status == 204 || status == 404);
            requestBase.releaseConnection();
            requestBase = new HttpDelete(destinationuri);
            status = this.client.execute(requestBase, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 200 || status == 204 || status == 404);
            requestBase.releaseConnection();
        }
    }
}
