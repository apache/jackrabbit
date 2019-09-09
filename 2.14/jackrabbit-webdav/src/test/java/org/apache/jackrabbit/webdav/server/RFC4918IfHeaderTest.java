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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.client.methods.HttpLock;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;

/**
 * Test cases for RFC 4918 If header functionality
 * (see <a href="http://www.webdav.org/specs/rfc4918.html#rfc.section.10.4">RFC 4918, Section 10.4</a>
 */

public class RFC4918IfHeaderTest extends WebDAVTest {

    public void testPutIfEtag() throws IOException, DavException, URISyntaxException {

        String testuri = this.root + "iftest";

        try {
            HttpPut put = new HttpPut(testuri);
            String condition = "<" + testuri + "> ([" + "\"an-etag-this-testcase-invented\"" + "])";
            put.setEntity(new StringEntity("1"));
            put.setHeader("If", condition);
            int status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertEquals("status: " + status, 412, status);
        }
        finally {
            HttpDelete delete = new HttpDelete(testuri);
            int status = this.client.execute(delete, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 200 || status == 204 || status == 404);
        }
    }

    public void testPutIfLockToken() throws IOException, DavException, URISyntaxException {

        String testuri = this.root + "iflocktest";
        String locktoken = null;

        try {
            HttpPut put = new HttpPut(testuri);
            put.setEntity(new StringEntity("1"));
            int status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 200 || status == 201 || status == 204);

            HttpLock lock = new HttpLock(testuri, new LockInfo(
                    Scope.EXCLUSIVE, Type.WRITE, "testcase", 10000, true));
            HttpResponse response = this.client.execute(lock, this.context);
            status = response.getStatusLine().getStatusCode();
            assertEquals("status", 200, status);
            locktoken = lock.getLockToken(response);
            assertNotNull(locktoken);
            System.out.println(locktoken);
            System.out.println(response.getFirstHeader("lock-token").getValue());

            // try to overwrite without lock token
            put = new HttpPut(testuri);
            put.setEntity(new StringEntity("2"));
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertEquals("status: " + status, 423, status);

            // try to overwrite using bad lock token
            put = new HttpPut(testuri);
            put.setEntity(new StringEntity("2"));
            put.setHeader("If", "(<" + "DAV:foobar" + ">)");
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertEquals("status: " + status, 412, status);

            // try to overwrite using correct lock token, using  No-Tag-list format
            put = new HttpPut(testuri);
            put.setEntity(new StringEntity("2"));
            put.setHeader("If", "(<" + locktoken + ">)");
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 200 || status == 204);

            // try to overwrite using correct lock token, using Tagged-list format
            // and full URI
            put = new HttpPut(testuri);
            put.setEntity(new StringEntity("3"));
            put.setHeader("If", "<" + testuri + ">" + "(<" + locktoken + ">)");
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 200 || status == 204);

            // try to overwrite using correct lock token, using Tagged-list format
            // and absolute path only
            put = new HttpPut(testuri);
            put.setEntity(new StringEntity("4"));
            put.setHeader("If", "<" + new URI(testuri).getRawPath() + ">" + "(<" + locktoken + ">)");
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 200 || status == 204);

            // try to overwrite using correct lock token, using Tagged-list format
            // and bad path
            put = new HttpPut(testuri);
            put.setEntity(new StringEntity("5"));
            put.setHeader("If", "</foobar>" + "(<" + locktoken + ">)");
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 404 || status == 412);
        } finally {
            HttpDelete delete = new HttpDelete(testuri);
            if (locktoken != null) {
                delete.setHeader("If", "(<" + locktoken + ">)");
            }
            int status = this.client.execute(delete, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 200 || status == 204 || status == 404);
        }
    }
}
