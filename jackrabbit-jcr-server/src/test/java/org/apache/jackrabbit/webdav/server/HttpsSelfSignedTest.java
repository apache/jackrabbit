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

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;

public class HttpsSelfSignedTest extends WebDAVTestBase {

    // check that by default, the client will fail connecting to HTTPS due to
    // self-signed certificate
    public void testPutCheckLastModified() throws IOException, ParseException {
        try {
            String testUri = this.httpsUri.toString();
            HttpPut put = new HttpPut(testUri);
            put.setEntity(new StringEntity("foobar"));
            HttpResponse response = this.client.execute(put, this.context);
            fail("should failt with SSLHandshakeException, but got: " + response);
        } catch (SSLHandshakeException expected) {
        }
    }
}
