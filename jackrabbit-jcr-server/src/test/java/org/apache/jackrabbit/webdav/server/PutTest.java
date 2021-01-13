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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;

/**
 * Test cases for HTTP PUT method
 */
public class PutTest extends WebDAVTestBase {

    public void testPutWithContentRange() throws IOException, ParseException {
        String testUri = this.uri.toString() + (this.uri.toString().endsWith("/") ? "" : "/") + "testPutWithContentRange";
        HttpPut put = new HttpPut(testUri);
        put.addHeader("Content-Range", "bytes 0-5/6");
        put.setEntity(new StringEntity("foobar"));
        HttpResponse response = this.client.execute(put, this.context);
        int status = response.getStatusLine().getStatusCode();
        assertEquals(400, status);
    }
}
