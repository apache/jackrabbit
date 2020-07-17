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
package org.apache.jackrabbit.spi2dav;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertPathBuilderException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi2davex.Spi2davexRepositoryServiceFactory;
import org.apache.jackrabbit.webdav.server.WebDAVTestBase;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.ProxyAuthenticator;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

public class ConnectionTest extends WebDAVTestBase {

    private Spi2davexRepositoryServiceFactory repositoryServiceFactory;

    protected void setUp() throws Exception {
        super.setUp();
        repositoryServiceFactory = new Spi2davexRepositoryServiceFactory();
    }

    RepositoryService createRepositoryService(boolean isViaHttps, final Map<String,String> additionalParameters) throws URISyntaxException, RepositoryException {
        Map<String, String> parameters = new HashMap<>();
        if (isViaHttps) {
            parameters.put(Spi2davexRepositoryServiceFactory.PARAM_REPOSITORY_URI,
                    new URI("https", null, remotingUri.getHost(), httpsUri.getPort(), remotingUri.getPath(), null, null).toString());
        } else {
            parameters.put(Spi2davexRepositoryServiceFactory.PARAM_REPOSITORY_URI, remotingUri.toString());
        }
        if (additionalParameters != null) {
            parameters.putAll(additionalParameters);
        }
        return repositoryServiceFactory.createRepositoryService(parameters);
    }

    public void testObtainWithoutTLS() throws RepositoryException, URISyntaxException {
        RepositoryService repositoryService = createRepositoryService(false, null);
        repositoryService.obtain(new SimpleCredentials("admin", "admin".toCharArray()), null);
    }

    public void testObtainWithTLSSelfSignedCertNotAllowed() throws RepositoryException, URISyntaxException {
        RepositoryService repositoryService = createRepositoryService(true, null);
        try {
            repositoryService.obtain(new SimpleCredentials("admin", "admin".toCharArray()), null);
            fail("should have failed with CertPathBuilderException!");
        } catch (RepositoryException e) {
            Throwable cause = ExceptionUtils.getRootCause(e);
            if (!(cause instanceof CertPathBuilderException)) {
                fail("should have failed with CertPathBuilderException but got " + e.getCause());
            }
        }
    }

    public void testObtainWithTLSSelfSignedCertAllowed() throws RepositoryException, URISyntaxException {
        Map<String, String> parameters = new HashMap<>();
        ConnectionOptions.Builder builder = ConnectionOptions.builder();
        builder.allowSelfSignedCertificates(true);
        parameters.putAll(builder.build().toServiceFactoryParameters());
        RepositoryService repositoryService = createRepositoryService(true, parameters);
        try {
            repositoryService.obtain(new SimpleCredentials("admin", "admin".toCharArray()), null);
        } catch (RepositoryException e) {
            Throwable cause = ExceptionUtils.getRootCause(e);
            if (!(cause instanceof SSLPeerUnverifiedException)) {
                fail("should have failed with SSLPeerUnverifiedException but got " + e.getCause());
            }
        }
    }

    public void testObtainWithTLSSelfSignedCertAllowedAndHostnameVerificationDisabled() throws RepositoryException, URISyntaxException {
        Map<String, String> parameters = new HashMap<>();
        ConnectionOptions.Builder builder = ConnectionOptions.builder();
        builder.allowSelfSignedCertificates(true);
        builder.disableHostnameVerification(true);
        parameters.putAll(builder.build().toServiceFactoryParameters());
        RepositoryService repositoryService = createRepositoryService(true, parameters);
        repositoryService.obtain(new SimpleCredentials("admin", "admin".toCharArray()), null);
    }

    public void testObtainViaProxy() throws URISyntaxException, RepositoryException {
        HttpProxyServer proxyServer = DefaultHttpProxyServer.bootstrap()
                .withPort(0) // use arbitrary port
                .start();
        try {
            Map<String, String> parameters = new HashMap<>();
            ConnectionOptions.Builder builder = ConnectionOptions.builder();
            builder.proxyHost("127.0.0.1");
            builder.proxyPort(proxyServer.getListenAddress().getPort());
            parameters.putAll(builder.build().toServiceFactoryParameters());
            RepositoryService repositoryService = createRepositoryService(false, parameters);
            repositoryService.obtain(new SimpleCredentials("admin", "admin".toCharArray()), null);
        } finally {
            proxyServer.stop();
        }
    }

    public void testObtainViaProxyWithInvalidCredentials() throws URISyntaxException, RepositoryException {
        ProxyAuthenticator authenticator = new ProxyAuthenticator() {

            @Override
            public String getRealm() {
                return null;
            }

            @Override
            public boolean authenticate(String userName, String password) {
                return false;
            }
        };
        HttpProxyServer proxyServer = DefaultHttpProxyServer.bootstrap()
                .withPort(0) // use arbitrary port
                .withProxyAuthenticator(authenticator)
                .start();
        try {
            Map<String, String> parameters = new HashMap<>();
            ConnectionOptions.Builder builder = ConnectionOptions.builder();
            builder.proxyHost("127.0.0.1");
            builder.proxyPort(proxyServer.getListenAddress().getPort());
            builder.proxyUsername("test");
            builder.proxyPassword("invalid");
            parameters.putAll(builder.build().toServiceFactoryParameters());
            RepositoryService repositoryService = createRepositoryService(false, parameters);
            try {
                repositoryService.obtain(new SimpleCredentials("admin", "admin".toCharArray()), null);
                fail("should have failed with proxy authentication failure!");
            } catch (RepositoryException expected) {
            }
        } finally {
            proxyServer.stop();
        }
    }

    public void testObtainViaProxyWithValidCredentials() throws URISyntaxException, RepositoryException {
        ProxyAuthenticator authenticator = new ProxyAuthenticator() {

            @Override
            public String getRealm() {
                return null;
            }

            @Override
            public boolean authenticate(String userName, String password) {
                if (userName.equals("test") && password.equals("valid")) {
                    return true;
                }
                return false;
            }
        };
        HttpProxyServer proxyServer = DefaultHttpProxyServer.bootstrap()
                .withPort(0) // use arbitrary port
                .withProxyAuthenticator(authenticator)
                .start();
        try {
            Map<String, String> parameters = new HashMap<>();
            ConnectionOptions.Builder builder = ConnectionOptions.builder();
            builder.proxyHost("127.0.0.1");
            builder.proxyPort(proxyServer.getListenAddress().getPort());
            builder.proxyUsername("test");
            builder.proxyPassword("valid");
            parameters.putAll(builder.build().toServiceFactoryParameters());
            RepositoryService repositoryService = createRepositoryService(false, parameters);
            repositoryService.obtain(new SimpleCredentials("admin", "admin".toCharArray()), null);
        } finally {
            proxyServer.stop();
        }
    }

    public void testObtainInvalidURIWithConnectionTimeout() throws URISyntaxException, RepositoryException {
        // make sure it takes long enough before a timeout is happening
        Map<String, String> parameters = new HashMap<>();
        ConnectionOptions.Builder builder = ConnectionOptions.builder();
        int connectionTimeoutMs = 1000;
        builder.connectionTimeoutMs(connectionTimeoutMs);
        parameters.putAll(builder.build().toServiceFactoryParameters());
        // overwrite URI (use non-routable IP to run into connection timeout)
        parameters.put(Spi2davexRepositoryServiceFactory.PARAM_REPOSITORY_URI, "http://10.0.0.0");
        RepositoryService repositoryService = createRepositoryService(true, parameters);

        long beforeTimeMs = System.currentTimeMillis();
        try {
            repositoryService.obtain(new SimpleCredentials("admin", "admin".toCharArray()), null);
            fail("Should have run into a connect exception");
        } catch (RepositoryException e) {
            if (!(e.getCause() instanceof ConnectTimeoutException)) {
                fail("Expected a connect timeout but received " + e);
            }
        }
        long afterTimeMs = System.currentTimeMillis();
        assertTrue("Have not waited long enough!", afterTimeMs - beforeTimeMs >= connectionTimeoutMs);
    }
}
