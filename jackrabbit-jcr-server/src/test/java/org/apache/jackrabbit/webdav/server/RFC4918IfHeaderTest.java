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

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.jackrabbit.webdav.client.methods.HttpLock;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;

/**
 * Test cases for RFC 4918 If header functionality
 * (see <a href="http://www.webdav.org/specs/rfc4918.html#rfc.section.10.4">RFC 4918, Section 10.4</a>
 */

public class RFC4918IfHeaderTest extends WebDAVTestBase {

    public void testPutIfEtag() throws IOException {

        String testuri = this.root + "iftest";
        HttpPut put = new HttpPut(testuri);
        try {
            put = new HttpPut(testuri);
            String condition = "<" + testuri + "> ([" + "\"an-etag-this-testcase-invented\"" + "])";
            put.setEntity(new StringEntity("1"));
            put.setHeader("If", condition);
            int status = this.client.executeOpen(null, put, this.context).getCode();
            assertEquals("status: " + status, 412, status);
            put.reset();

        }
        finally {
            put.reset();
            HttpDelete delete = new HttpDelete(testuri);
            int status = this.client.executeOpen(null, delete, this.context).getCode();
            assertTrue("status: " + status, status == 200 || status == 204 || status == 404);
            delete.reset();
        }
    }

    public void testPutIfLockToken() throws IOException, URISyntaxException {

        String testuri = this.root + "iflocktest";
        String locktoken = null;

        HttpUriRequestBase requestBase = null;
        try {
            requestBase = new HttpPut(testuri);
            ((HttpPut)requestBase).setEntity(new StringEntity("1"));
            int status = this.client.executeOpen(null, requestBase, this.context).getCode();
            assertTrue("status: " + status, status == 200 || status == 201 || status == 204);
            requestBase.reset();

            requestBase = new HttpLock(testuri, new LockInfo(
                    Scope.EXCLUSIVE, Type.WRITE, "testcase", 10000, true));
            ClassicHttpResponse response = this.client.executeOpen(null, requestBase, this.context);
            status = response.getCode();
            assertEquals("status", 200, status);
            locktoken = ((HttpLock)requestBase).getLockToken(response);
            assertNotNull(locktoken);
            requestBase.reset();

            // try to overwrite without lock token
            requestBase = new HttpPut(testuri);
            ((HttpPut)requestBase).setEntity(new StringEntity("2"));
            status = this.client.executeOpen(null, requestBase, this.context).getCode();
            assertEquals("status: " + status, 423, status);
            requestBase.reset();

            // try to overwrite using bad lock token
            requestBase = new HttpPut(testuri);
            ((HttpPut)requestBase).setEntity(new StringEntity("2"));
            requestBase.setHeader("If", "(<" + "DAV:foobar" + ">)");
            status = this.client.executeOpen(null, requestBase, this.context).getCode();
            assertEquals("status: " + status, 412, status);
            requestBase.reset();

            // try to overwrite using correct lock token, using  No-Tag-list format
            requestBase = new HttpPut(testuri);
            ((HttpPut)requestBase).setEntity(new StringEntity("2"));
            requestBase.setHeader("If", "(<" + locktoken + ">)");
            status = this.client.executeOpen(null, requestBase, this.context).getCode();
            assertTrue("status: " + status, status == 200 || status == 204);
            requestBase.reset();

            // try to overwrite using correct lock token, using Tagged-list format
            // and full URI
            requestBase = new HttpPut(testuri);
            ((HttpPut)requestBase).setEntity(new StringEntity("3"));
            requestBase.setHeader("If", "<" + testuri + ">" + "(<" + locktoken + ">)");
            status = this.client.executeOpen(null, requestBase, this.context).getCode();
            assertTrue("status: " + status, status == 200 || status == 204);
            requestBase.reset();

            // try to overwrite using correct lock token, using Tagged-list format
            // and absolute path only
            requestBase = new HttpPut(testuri);
            ((HttpPut)requestBase).setEntity(new StringEntity("4"));
            requestBase.setHeader("If", "<" + new URI(testuri).getRawPath() + ">" + "(<" + locktoken + ">)");
            status = this.client.executeOpen(null, requestBase, this.context).getCode();
            assertTrue("status: " + status, status == 200 || status == 204);
            requestBase.reset();

            // try to overwrite using correct lock token, using Tagged-list format
            // and bad path
            requestBase = new HttpPut(testuri);
            ((HttpPut)requestBase).setEntity(new StringEntity("5"));
            requestBase.setHeader("If", "</foobar>" + "(<" + locktoken + ">)");
            status = this.client.executeOpen(null, requestBase, this.context).getCode();
            assertTrue("status: " + status, status == 404 || status == 412);
        } finally {
            requestBase.reset();
            requestBase = new HttpDelete(testuri);
            if (locktoken != null) {
                requestBase.setHeader("If", "(<" + locktoken + ">)");
            }
            int status = this.client.executeOpen(null, requestBase, this.context).getCode();
            assertTrue("status: " + status, status == 200 || status == 204 || status == 404);
        }
    }
}
