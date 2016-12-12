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

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import junit.framework.TestCase;

/**
 * Base class for WebDAV tests.
 * <p>
 * Required system properties:
 * <ul>
 *   <li>webdav.test.url</li>
 *   <li>webdav.test.username</li>
 *   <li>webdav.test.password</li>
 * </ul>
 */
public class WebDAVTest extends TestCase {

    private String username, password;
    public URI uri;
    public String root;

    public HttpClient client;
    public HttpClientContext context;

    protected void setUp() throws Exception {
        this.uri = URI.create(System.getProperty("webdav.test.url", "http://localhost:8080/repository/default/"));
        this.root = this.uri.toASCIIString();
        if (!this.root.endsWith("/")) {
            this.root += "/";
        }
        this.username = System.getProperty("webdav.test.username", "admin");
        this.password = System.getProperty("webdav.test.password", "admin");

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        HttpHost targetHost = new HttpHost(uri.getHost(), uri.getPort());

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(targetHost.getHostName(), targetHost.getPort()),
                new UsernamePasswordCredentials(this.username, this.password));

        AuthCache authCache = new BasicAuthCache();
        // Generate BASIC scheme object and add it to the local auth cache
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(targetHost, basicAuth);

        // Add AuthCache to the execution context
        this.context = HttpClientContext.create();
        this.context.setCredentialsProvider(credsProvider);
        this.context.setAuthCache(authCache);

        this.client = HttpClients.custom().setConnectionManager(cm).build();

        super.setUp();
    }

    protected void delete(String uri) throws IOException {
        HttpDelete delete = new HttpDelete(uri);
        int status = this.client.execute(delete, this.context).getStatusLine().getStatusCode();
        assertTrue("status: " + status, status == 200 || status == 204);
    }
}
