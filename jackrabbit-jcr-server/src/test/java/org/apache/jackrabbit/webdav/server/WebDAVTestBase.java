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

import javax.jcr.Repository;
import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
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
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.server.remoting.davex.JcrRemotingServlet;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.RepositoryStubException;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import junit.framework.TestResult;

/**
 * Base class for WebDAV tests.
 */
public class WebDAVTestBase extends AbstractJCRTest {

    private static final String SIMPLE_WEBDAV_SERVLET_PATH_MAPPING = "/*";
    private static final String REMOTING_PREFIX = "/remoting";
    private static final String REMOTING_WEBDAV_SERVLET_PATH_MAPPING = REMOTING_PREFIX + "/*";

    private static ServerConnector httpConnector;
    private static ServerConnector httpsConnector;
    private static Server server;
    private static RepositoryContext repoContext;

    public URI uri;
    public URI httpsUri;
    public String root;

    // URI for remoting servlet, does not include workspace name
    public URI remotingUri;

    public HttpClient client;
    public HttpClientContext context;

    private static final String KEYSTORE = "keystore";
    private static final String KEYSTOREPW = "geheimer";

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

        File keystore = new File(home, KEYSTORE);
        if (!keystore.exists()) {
            createKeystore(keystore);
        }

        if (repoContext == null) {
            repoContext = RepositoryContext.create(RepositoryConfig.create(config.toURI(), home.getPath()));
        }

        if (server == null) {
            server = new Server();

            ServletHolder simple = new ServletHolder(new SimpleWebdavServlet() {
                private static final long serialVersionUID = 8638589328461138178L;

                public Repository getRepository() {
                    return repoContext.getRepository();
                }
            });
            simple.setInitParameter(SimpleWebdavServlet.INIT_PARAM_RESOURCE_CONFIG, "/config.xml");

            ServletHolder remoting = new ServletHolder(new JcrRemotingServlet() {
                private static final long serialVersionUID = -2969534124090379387L;

                public Repository getRepository() {
                    return repoContext.getRepository();
                }
            });
            remoting.setInitParameter(JcrRemotingServlet.INIT_PARAM_RESOURCE_PATH_PREFIX, "/remoting");

            ServletContextHandler schandler = new ServletContextHandler(server, "/");
            schandler.addServlet(simple, SIMPLE_WEBDAV_SERVLET_PATH_MAPPING);
            schandler.addServlet(remoting, REMOTING_WEBDAV_SERVLET_PATH_MAPPING);
            schandler.setBaseResource(Resource.newClassPathResource("/"));

            server.setHandler(schandler);
        }

        if (httpConnector == null) {
            httpConnector = new ServerConnector(server);
            httpConnector.setHost("localhost");
            httpConnector.setPort(0);
            server.addConnector(httpConnector);
        }

        if (httpsConnector == null) {
            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(keystore.getPath());
            sslContextFactory.setKeyStorePassword(KEYSTOREPW);
            sslContextFactory.setKeyManagerPassword(KEYSTOREPW);
            sslContextFactory.setTrustStorePath(keystore.getPath());
            sslContextFactory.setTrustStorePassword(KEYSTOREPW);
            SslConnectionFactory cfac = new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString());
            httpsConnector = new ServerConnector(server, cfac, new HttpConnectionFactory(new HttpConfiguration()));
            httpsConnector.setHost("localhost");
            httpsConnector.setPort(0);
            server.addConnector(httpsConnector);
        }

        if (!server.isStarted()) {
            try {
                server.start();
            } catch (Exception e) {
                throw new RepositoryStubException(e);
            }
        }

        this.uri = new URI("http", null, "localhost", httpConnector.getLocalPort(), "/default/", null, null);
        this.remotingUri = new URI("http", null, "localhost", httpConnector.getLocalPort(), REMOTING_PREFIX + "/", null, null);
        this.httpsUri = new URI("https", null, "localhost", httpsConnector.getLocalPort(), "/default/", null, null);
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
    private void createDefaultConfiguration(File config) throws ServletException {
        try (OutputStream output = new FileOutputStream(config);
                InputStream input = WebDAVTestBase.class.getResourceAsStream("/repository.xml")) {
            IOUtils.copy(input, output);
        } catch (IOException e) {
            throw new ServletException("Failed to copy default configuration: " + config, e);
        }
    }

    private void createKeystore(File keystore) throws ServletException {
        try (OutputStream output = new FileOutputStream(keystore);
                InputStream input = WebDAVTestBase.class.getResourceAsStream("/" + KEYSTORE)) {
            IOUtils.copy(input, output);
        } catch (IOException e) {
            throw new ServletException("Failed to copy keystore: " + keystore, e);
        }
    }

    @Override
    public void run(TestResult testResult) {
        if (Boolean.getBoolean("jackrabbit.test.integration")) {
            super.run(testResult);
        }
    }
}
