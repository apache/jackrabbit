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
import java.text.ParseException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

public class ConditionalsTest extends WebDAVTestBase {

    // private DateFormat HTTPDATEFORMAT = new SimpleDateFormat("EEE, dd MMM
    // yyyy HH:mm:ss ZZZ", Locale.ENGLISH);

    public void testPutCheckLastModified() throws IOException, ParseException {
        String testUri = this.uri.toString() + (this.uri.toString().endsWith("/") ? "" : "/") + "testPutCheckLastModified";
        try {
            // create test resource
            {
                HttpPut put = new HttpPut(testUri);
                put.setEntity(new StringEntity("foobar"));
                HttpResponse response = this.client.execute(put, this.context);
                int status = response.getStatusLine().getStatusCode();
                assertEquals(201, status);
            }

            long created = System.currentTimeMillis();

            // get last modified date
            Header etag = null;
            Header lm = null;
            {
                HttpHead head = new HttpHead(testUri);
                HttpResponse response = this.client.execute(head, this.context);
                int status = response.getStatusLine().getStatusCode();
                assertEquals(200, status);
                lm = response.getFirstHeader("last-modified");
                assertNotNull(lm);
                etag = response.getFirstHeader("etag");
            }

            // Date created = HTTPDATEFORMAT.parse(lm.getValue());

            // conditional GET
            {
                HttpGet get = new HttpGet(testUri);
                get.setHeader("If-Modified-Since", lm.getValue());
                HttpResponse response = this.client.execute(get, this.context);
                int status = response.getStatusLine().getStatusCode();
                assertEquals(304, status);
                if (etag != null) {
                    Header newetag = response.getFirstHeader("etag");
                    assertNotNull(newetag);
                    assertEquals(etag.getValue(), newetag.getValue());
                }
            }

            // conditional HEAD
            {
                HttpHead head = new HttpHead(testUri);
                head.setHeader("If-Modified-Since", lm.getValue());
                HttpResponse response = this.client.execute(head, this.context);
                int status = response.getStatusLine().getStatusCode();
                assertEquals(304, status);
                if (etag != null) {
                    Header newetag = response.getFirstHeader("etag");
                    assertNotNull(newetag);
                    assertEquals(etag.getValue(), newetag.getValue());
                }
            }

            // conditional HEAD with broken date (MUST ignore header field)
            {
                HttpHead head = new HttpHead(testUri);
                head.setHeader("If-Modified-Since", "broken");
                HttpResponse response = this.client.execute(head, this.context);
                int status = response.getStatusLine().getStatusCode();
                assertEquals(200, status);
            }

            // conditional GET with broken date (MUST ignore header field)
            {
                HttpGet req = new HttpGet(testUri);
                req.addHeader("If-Modified-Since", lm.getValue());
                req.addHeader("If-Modified-Since", "foo");
                HttpResponse response = this.client.execute(req, this.context);
                int status = response.getStatusLine().getStatusCode();
                assertEquals(200, status);
                EntityUtils.consume(response.getEntity());
            }

            // let one sec elapse
            while (System.currentTimeMillis() < created + 1000) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }

            // verify last modified did not change
            {
                HttpHead head = new HttpHead(testUri);
                HttpResponse response = this.client.execute(head, this.context);
                int status = response.getStatusLine().getStatusCode();
                assertEquals(200, status);
                Header newlm = response.getFirstHeader("last-modified");
                assertNotNull(newlm);
                assertEquals(lm.getValue(), newlm.getValue());
            }

            // conditional PUT
            {
                HttpPut put = new HttpPut(testUri);
                put.setHeader("If-Unmodified-Since", lm.getValue());
                put.setEntity(new StringEntity("qux"));
                HttpResponse response = this.client.execute(put, this.context);
                int status = response.getStatusLine().getStatusCode();
                assertEquals(204, status);
            }

            // conditional PUT once more should fail
            {
                HttpPut put = new HttpPut(testUri);
                put.setHeader("If-Unmodified-Since", lm.getValue());
                put.setEntity(new StringEntity("lazydog"));
                HttpResponse response = this.client.execute(put, this.context);
                int status = response.getStatusLine().getStatusCode();
                assertEquals(412, status);
            }

            // conditional PUT with broken If-Unmodified-Since should pass
            {
                HttpPut put = new HttpPut(testUri);
                put.addHeader("If-Unmodified-Since", lm.getValue());
                put.addHeader("If-Unmodified-Since", "foo");
                put.setEntity(new StringEntity("qux"));
                HttpResponse response = this.client.execute(put, this.context);
                int status = response.getStatusLine().getStatusCode();
                assertEquals(204, status);
            }
        } finally {
            delete(testUri);
        }
    }

    public void testGetCollectionEtag() throws IOException, ParseException {
        String testUri = this.uri.toString() + (this.uri.toString().endsWith("/") ? "" : "/");
        HttpGet get = new HttpGet(testUri);
        HttpResponse response = this.client.execute(get, this.context);
        int status = response.getStatusLine().getStatusCode();
        assertEquals(200, status);
        Header etag = response.getFirstHeader("etag");
        if (etag != null) {
            assertFalse("etag must not be empty", "".equals(etag.getValue()));
        }
    }
}
