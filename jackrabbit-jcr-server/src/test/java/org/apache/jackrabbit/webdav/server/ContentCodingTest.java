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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.client.methods.HttpPropfind;

public class ContentCodingTest extends WebDAVTestBase {

    public void testPutNoContentCoding() throws IOException {
        String testUri = this.uri.toString() + (this.uri.toString().endsWith("/") ? "" : "/") + "testPutNoContentCoding";
        try {
            HttpPut put = new HttpPut(testUri);
            put.setEntity(new StringEntity("foobar"));
            int status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
        } finally {
            delete(testUri);
        }
    }

    public void testPutUnknownContentCoding() throws IOException {
        String testUri = this.uri.toString() + (this.uri.toString().endsWith("/") ? "" : "/") + "testPutUnkownContentCoding";
        int status = -1;
        try {
            HttpPut put = new HttpPut(testUri);
            StringEntity entity = new StringEntity("foobarfoobarfoobar");
            entity.setContentEncoding(new BasicHeader("Content-Encoding", "qux"));
            put.setEntity(entity);
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertTrue("server must signal error for unknown content coding", status == 415);
        } finally {
            if (status / 2 == 100) {
                delete(testUri);
            }
        }
    }

    public void testPutGzipContentCoding() throws IOException {
        String testUri = this.uri.toString() + (this.uri.toString().endsWith("/") ? "" : "/") + "testPutGzipContentCoding";
        int status = -1;
        try {
            byte bytes[] = "foobarfoobarfoobar".getBytes("UTF-8");
            HttpPut put = new HttpPut(testUri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            OutputStream gos = new GZIPOutputStream(bos);
            gos.write(bytes);
            gos.flush();
            assertTrue(bos.toByteArray().length != bytes.length);
            InputStreamEntity entity = new InputStreamEntity(new ByteArrayInputStream(bos.toByteArray()));
            entity.setContentEncoding(new BasicHeader("Content-Encoding", "gzip"));
            put.setEntity(entity);
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertTrue("server create or signal error", status == 201 || status == 415);
            if (status / 2 == 100) {
                // check length
                HttpHead head = new HttpHead(testUri);
                HttpResponse response = this.client.execute(head, this.context);
                assertEquals(200, response.getStatusLine().getStatusCode());
                assertEquals(bytes.length, response.getFirstHeader("Content-Length").getValue());
            }
        } finally {
            if (status / 2 == 100) {
                delete(testUri);
            }
        }
    }

    public void testPropfindNoContentCoding() throws IOException {
        HttpPropfind propfind = new HttpPropfind(uri, DavConstants.PROPFIND_BY_PROPERTY, 0);
        int status = this.client.execute(propfind, this.context).getStatusLine().getStatusCode();
        assertEquals(207, status);
    }

    public void testPropfindUnknownContentCoding() throws IOException {
        HttpPropfind propfind = new HttpPropfind(uri, DavConstants.PROPFIND_BY_PROPERTY, 0);
        StringEntity entity = new StringEntity(
                "<D:propfind xmlns:D=\"DAV:\"><D:prop xmlns:R=\"http://ns.example.com/boxschema/\"><R:bigbox/></D:prop></D:propfind>");
        entity.setContentEncoding(new BasicHeader("Content-Encoding", "qux"));
        propfind.setEntity(entity);
        int status = this.client.execute(propfind, this.context).getStatusLine().getStatusCode();
        assertTrue("server must signal error for unknown content coding", status == 415);
    }
}
