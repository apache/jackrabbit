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

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class ConnectionOptionsTest {

    @Test
    public void testFromServiceParameterToServiceParameters() {
        Map<String, String> serviceParameters = new HashMap<>();
        serviceParameters.put(ConnectionOptions.PARAM_USE_SYSTEM_PROPERTIES, "true");
        serviceParameters.put(ConnectionOptions.PARAM_MAX_CONNECTIONS, "10");
        serviceParameters.put(ConnectionOptions.PARAM_ALLOW_SELF_SIGNED_CERTIFICATES, "true");
        serviceParameters.put(ConnectionOptions.PARAM_DISABLE_HOSTNAME_VERIFICATION, "true");
        serviceParameters.put(ConnectionOptions.PARAM_CONNECTION_TIMEOUT_MS, "100");
        serviceParameters.put(ConnectionOptions.PARAM_REQUEST_TIMEOUT_MS, "200");
        serviceParameters.put(ConnectionOptions.PARAM_SOCKET_TIMEOUT_MS, "300");
        serviceParameters.put(ConnectionOptions.PARAM_PROXY_HOST, "somehost");
        serviceParameters.put(ConnectionOptions.PARAM_PROXY_PORT, "111");
        serviceParameters.put(ConnectionOptions.PARAM_PROXY_USERNAME, "user");
        serviceParameters.put(ConnectionOptions.PARAM_PROXY_PASSWORD, "password");
        Assert.assertEquals(serviceParameters, ConnectionOptions.fromServiceFactoryParameters(serviceParameters).toServiceFactoryParameters());
    }

    @Test
    public void testLegacyMaxConnectionsParameter() {
        Map<String, String> serviceParameters = new HashMap<>();
        serviceParameters.put("org.apache.jackrabbit.spi2davex.MaxConnections", "30");
        ConnectionOptions connectionOptions = ConnectionOptions.builder().maxConnections(30).build();
        Assert.assertEquals(connectionOptions, ConnectionOptions.fromServiceFactoryParameters(serviceParameters));
    }

    @Test
    public void testBuilder() {
        ConnectionOptions.Builder builder = ConnectionOptions.builder();
        builder.allowSelfSignedCertificates(true);
        builder.disableHostnameVerification(false);
        builder.maxConnections(10);
        builder.connectionTimeoutMs(100);
        builder.requestTimeoutMs(200);
        builder.socketTimeoutMs(300);
        builder.proxyHost("proxyHost");
        builder.proxyPort(1234);
        builder.proxyUsername("proxyUser");
        builder.proxyPassword("proxyPassword");
        builder.proxyProtocol("https:");
        ConnectionOptions options = builder.build();
        Assert.assertEquals(true, options.isAllowSelfSignedCertificates());
        Assert.assertEquals(false, options.isDisableHostnameVerification());
        Assert.assertEquals(10, options.getMaxConnections());
        Assert.assertEquals(100, options.getConnectionTimeoutMs());
        Assert.assertEquals(200, options.getRequestTimeoutMs());
        Assert.assertEquals(300, options.getSocketTimeoutMs());
        Assert.assertEquals("proxyHost", options.getProxyHost());
        Assert.assertEquals(1234, options.getProxyPort());
        Assert.assertEquals("proxyUser", options.getProxyUsername());
        Assert.assertEquals("proxyPassword", options.getProxyPassword());
        Assert.assertEquals("https:", options.getProxyProtocol());
    }
}
