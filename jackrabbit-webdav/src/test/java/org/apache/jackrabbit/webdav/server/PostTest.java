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

import junit.framework.TestCase;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.net.URI;

/**
 * Test cases for the WebDAV servlet not executing the POST method (CSRF protection).
 * <p>
 * Required system properties:
 * <ul>
 * <li>webdav.test.url</li>
 * <li>webdav.test.username</li>
 * <li>webdav.test.password</li>
 * </ul>
 */

public class PostTest extends TestCase {

    private String root;
    private URI uri;
    private String username, password;
    private HttpClient client;

    protected void setUp() throws Exception {
        this.uri = URI.create(System.getProperty("webdav.test.url"));
        this.root = this.uri.toASCIIString();
        if (!this.root.endsWith("/")) {
            this.root += "/";
        }
        this.username = System.getProperty(("webdav.test.username"), "");
        this.password = System.getProperty(("webdav.test.password"), "");
        this.client = new HttpClient();
        this.client.getState().setCredentials(
                new AuthScope(this.uri.getHost(), this.uri.getPort()),
                new UsernamePasswordCredentials(this.username, this.password));
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPostDenied() throws HttpException, IOException {
        PostMethod options = new PostMethod(this.uri.toASCIIString());
        int status = this.client.executeMethod(options);
        assertEquals(405, status);
    }
}
