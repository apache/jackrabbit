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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
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
            assertTrue("server must signal error for unknown content coding, got: " + status, status == 415);
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
            byte gzbytes[] = asGzipOctets(bytes);
            assertTrue(gzbytes.length != bytes.length);
            ByteArrayEntity entity = new ByteArrayEntity(gzbytes);
            entity.setContentEncoding(new BasicHeader("Content-Encoding", "gzip"));
            put.setEntity(entity);
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertTrue("server create or signal error, got: " + status, status == 201 || status == 415);
            if (status / 2 == 100) {
                // check length
                HttpHead head = new HttpHead(testUri);
                HttpResponse response = this.client.execute(head, this.context);
                assertEquals(200, response.getStatusLine().getStatusCode());
                assertEquals(bytes.length, Integer.parseInt(response.getFirstHeader("Content-Length").getValue()));
            }
        } finally {
            if (status / 2 == 100) {
                delete(testUri);
            }
        }
    }

    public void testPropfindNoContentCoding() throws IOException, DavException {
        HttpPropfind propfind = new HttpPropfind(uri, DavConstants.PROPFIND_BY_PROPERTY, 0);
        HttpResponse response = this.client.execute(propfind, this.context);
        int status = response.getStatusLine().getStatusCode();
        assertEquals(207, status);
        List<String> encodings = getContentCodings(response);
        assertTrue("Accept should list 'gzip' but did not: " + encodings, encodings.contains("gzip"));
        assertTrue("Accept should list 'deflate' but did not: " + encodings, encodings.contains("deflate"));
    }

    public void testPropfindAcceptReponseEncoding() throws IOException, DavException {
        HttpPropfind propfind = new HttpPropfind(uri, DavConstants.PROPFIND_BY_PROPERTY, 0);
        propfind.setHeader(new BasicHeader("Accept-Encoding", "gzip;q=0.555"));
        HttpResponse response = this.client.execute(propfind, this.context);
        int status = response.getStatusLine().getStatusCode();
        assertEquals(207, status);
        MultiStatusResponse[] responses = propfind.getResponseBodyAsMultiStatus(response).getResponses();
        assertEquals(1, responses.length);
    }

    private static String PF = "<D:propfind xmlns:D=\"DAV:\"><D:prop><D:resourcetype/></D:prop></D:propfind>";

    public void testPropfindUnknownContentCoding() throws IOException {
        HttpPropfind propfind = new HttpPropfind(uri, DavConstants.PROPFIND_BY_PROPERTY, 0);
        StringEntity entity = new StringEntity(PF);
        entity.setContentEncoding(new BasicHeader("Content-Encoding", "qux"));
        propfind.setEntity(entity);
        HttpResponse response = this.client.execute(propfind, this.context);
        int status = response.getStatusLine().getStatusCode();
        assertTrue("server must signal error for unknown content coding, got: " + status, status == 415);
        List<String> encodings = getContentCodings(response);
        assertTrue("Accept should list 'gzip' but did not: " + encodings, encodings.contains("gzip"));
        assertTrue("Accept should list 'deflate' but did not: " + encodings, encodings.contains("deflate"));
    }

    public void testPropfindGzipContentCoding() throws IOException {
        HttpPropfind propfind = new HttpPropfind(uri, DavConstants.PROPFIND_BY_PROPERTY, 0);
        ByteArrayEntity entity = new ByteArrayEntity(asGzipOctets(PF));
        entity.setContentEncoding(new BasicHeader("Content-Encoding", "gzip"));
        propfind.setEntity(entity);
        int status = this.client.execute(propfind, this.context).getStatusLine().getStatusCode();
        assertEquals(207, status);
    }

    // double encoded, empty list member in field value, mixed upper/lower in
    // coding name
    public void testPropfindGzipContentCodingTwice() throws IOException {
        HttpPropfind propfind = new HttpPropfind(uri, DavConstants.PROPFIND_BY_PROPERTY, 0);
        ByteArrayEntity entity = new ByteArrayEntity(asGzipOctets(asGzipOctets(PF)));
        entity.setContentEncoding(new BasicHeader("Content-Encoding", "gziP,, Gzip"));
        propfind.setEntity(entity);
        int status = this.client.execute(propfind, this.context).getStatusLine().getStatusCode();
        assertEquals(207, status);
    }

    // double encoded, but only when encoding in header field
    public void testPropfindGzipContentCodingBadSpec() throws IOException {
        HttpPropfind propfind = new HttpPropfind(uri, DavConstants.PROPFIND_BY_PROPERTY, 0);
        ByteArrayEntity entity = new ByteArrayEntity(asGzipOctets(asGzipOctets(PF)));
        entity.setContentEncoding(new BasicHeader("Content-Encoding", "gzip"));
        propfind.setEntity(entity);
        int status = this.client.execute(propfind, this.context).getStatusLine().getStatusCode();
        assertEquals(400, status);
    }

    public void testPropfindDeflateContentCoding() throws IOException {
        HttpPropfind propfind = new HttpPropfind(uri, DavConstants.PROPFIND_BY_PROPERTY, 0);
        ByteArrayEntity entity = new ByteArrayEntity(asDeflateOctets(PF));
        entity.setContentEncoding(new BasicHeader("Content-Encoding", "deflate"));
        propfind.setEntity(entity);
        int status = this.client.execute(propfind, this.context).getStatusLine().getStatusCode();
        assertEquals(207, status);
    }

    public void testPropfindGzipDeflateContentCoding() throws IOException {
        HttpPropfind propfind = new HttpPropfind(uri, DavConstants.PROPFIND_BY_PROPERTY, 0);
        ByteArrayEntity entity = new ByteArrayEntity(asDeflateOctets(asGzipOctets(PF)));
        entity.setContentEncoding(new BasicHeader("Content-Encoding", "gzip, deflate"));
        propfind.setEntity(entity);
        int status = this.client.execute(propfind, this.context).getStatusLine().getStatusCode();
        assertEquals(207, status);
    }

    public void testPropfindGzipDeflateContentCodingMislabeled() throws IOException {
        HttpPropfind propfind = new HttpPropfind(uri, DavConstants.PROPFIND_BY_PROPERTY, 0);
        ByteArrayEntity entity = new ByteArrayEntity(asDeflateOctets(asGzipOctets(PF)));
        entity.setContentEncoding(new BasicHeader("Content-Encoding", "deflate, gzip"));
        propfind.setEntity(entity);
        int status = this.client.execute(propfind, this.context).getStatusLine().getStatusCode();
        assertEquals(400, status);
    }

    private static byte[] asGzipOctets(String input) throws IOException {
        return asGzipOctets(input.getBytes("UTF-8"));
    }

    private static byte[] asGzipOctets(byte[] input) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStream gos = new GZIPOutputStream(bos);
        gos.write(input);
        gos.flush();
        gos.close();
        return bos.toByteArray();
    }

    private static byte[] asDeflateOctets(String input) throws IOException {
        return asDeflateOctets(input.getBytes("UTF-8"));
    }

    private static byte[] asDeflateOctets(byte[] input) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStream gos = new DeflaterOutputStream(bos);
        gos.write(input);
        gos.flush();
        gos.close();
        return bos.toByteArray();
    }

    private static List<String> getContentCodings(HttpResponse response) {
        List<String> result = Collections.emptyList();
        for (Header l : response.getHeaders("Accept-Encoding")) {
            for (String h : l.getValue().split(",")) {
                if (!h.trim().isEmpty()) {
                    if (result.isEmpty()) {
                        result = new ArrayList<String>();
                    }
                    result.add(h.trim().toLowerCase(Locale.ENGLISH));
                }
            }
        }

        return result;
    }
}
