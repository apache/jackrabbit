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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import junit.framework.TestResult;
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

import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

import org.apache.jackrabbit.test.RepositoryStubException;

import javax.jcr.Repository;
import javax.servlet.ServletException;

import static org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet.INIT_PARAM_RESOURCE_CONFIG;

/**
 * Base class for WebDAV tests.
 */
public class WebDAVTestBase extends AbstractJCRTest {

    private static final String WEBDAV_SERVLET_PATH_MAPPING = "/*";

    private static ServerConnector connector;
    private static Server server;
    private static RepositoryContext repoContext;

    public URI uri;
    public String root;

    public HttpClient client;
    public HttpClientContext context;

    protected void setUp() throws Exception {
        super.setUp();

        File home = new File("target/jackrabbit-repository");
        if (!home.exists()) {
            home.mkdirs();
        }

        File config = new File(home, "repository.xml");
        if (!config.exists()) {
            createDefaultConfiguration(config);
        }

        if (repoContext == null) {
            repoContext = RepositoryContext.create(RepositoryConfig.create(config.toURI(), home.getPath()));
        }

        if (server == null) {
            server = new Server();

            ServletHolder holder = new ServletHolder(new SimpleWebdavServlet() {
                public Repository getRepository() {
                    return repoContext.getRepository();
                }
            });
            holder.setInitParameter(INIT_PARAM_RESOURCE_CONFIG, "/config.xml");

            ServletContextHandler schandler = new ServletContextHandler(server, "/");
            schandler.addServlet(holder, WEBDAV_SERVLET_PATH_MAPPING);
            schandler.setBaseResource(Resource.newClassPathResource("/"));

            server.setHandler(schandler);
        }

        if (connector == null) {
            connector = new ServerConnector(server);
            connector.setHost("localhost");
            connector.setPort(0);
            server.addConnector(connector);

            try {
                server.start();
            } catch (Exception e) {
                throw new RepositoryStubException(e);
            }
        }

        this.uri = new URI("http", null, "localhost", connector.getLocalPort(), "/default/", null, null);
        this.root = this.uri.toASCIIString();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        //cm.setMaxTotal(100);
        HttpHost targetHost = new HttpHost(uri.getHost(), uri.getPort());

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(targetHost.getHostName(), targetHost.getPort()),
                new UsernamePasswordCredentials("admin", "admin"));

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

    public static Server getServer() {
        return server;
    }

    /**
     * Copies the default repository configuration file to the given location.
     *
     * @param config path of the configuration file
     * @throws ServletException if the configuration file could not be copied
     */
    private void createDefaultConfiguration(File config)
            throws ServletException {
        try {
            OutputStream output = new FileOutputStream(config);
            try {
                InputStream input =
                        RepositoryImpl.class.getResourceAsStream("repository.xml");
                try {
                    byte[] buffer = new byte[8192];
                    int n = input.read(buffer);
                    while (n != -1) {
                        output.write(buffer, 0, n);
                        n = input.read(buffer);
                    }
                } finally {
                    input.close();
                }
            } finally {
                output.close();
            }
        } catch (IOException e) {
            throw new ServletException(
                    "Failed to copy default configuration: " + config, e);
        }
    }

    @Override
    public void run(TestResult testResult) {
        if (Boolean.getBoolean("jackrabbit.test.integration")) {
            super.run(testResult);
        }
    }
}
