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

import java.util.Map;

/**
 * Advanced connection options to use for connections to a remote repository.
 *
 */
public final class ConnectionOptions {

    private final boolean isUseSystemPropertes;
    private final boolean isAllowSelfSignedCertificates;
    private final boolean isDisableHostnameVerification;
    private final String proxyHost;
    private final int proxyPort;
    private final String proxyProtocol;
    private final String proxyUsername;
    private final String proxyPassword;

    /**
     * Boolean flag whether to use the default Java system properties for setting proxy, TLS and further options.
     * Default = {@code false}.
     * 
     * @see <a href="https://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/impl/client/HttpClientBuilder.html">HttpClientBuilder</a> 
     */
    public static final String PARAM_USE_SYSTEM_PROPERTIES = "connection.useSystemProperties";

    /**
     * Boolean flag whether to allow self-signed certificates of remote repositories.
     * Default = {@code false}.
     */
    public static final String PARAM_ALLOW_SELF_SIGNED_CERTIFICATS = "connection.allowSelfSignedCertificates";
    
    /**
     * Boolean flag whether to disable the host name verification against the common name of the server's certificate.
     * Default = {@code false}.
     */
    public static final String PARAM_DISABLE_HOSTNAME_VERIFICATION = "connection.disableHostnameVerification";
    
    /**
     * The host of a proxy server.
     */
    public static final String PARAM_PROXY_HOST = "connection.proxyHost";
    
    /**
     * Integer value for the proxy's port. Only effective if {@link #PARAM_PROXY_HOST} is used as well.
     */
    public static final String PARAM_PROXY_PORT = "connection.proxyPort";
    
    /**
     * The protocol for which to use the proxy. Only effective if {@link #PARAM_PROXY_HOST} is used as well.
     */
    public static final String PARAM_PROXY_PROTOCOL = "connection.proxyProtocol";

    /**
     * The user name to authenticate at the proxy. Only effective if {@link #PARAM_PROXY_HOST} is used as well.
     */
    public static final String PARAM_PROXY_USERNAME = "connection.proxyUsername";

    /**
     * The password to authenticate at the proxy. Only effective if {@link #PARAM_PROXY_HOST} and {@link #PARAM_PROXY_USERNAME} are used as well.
     */
    public static final String PARAM_PROXY_PASSWORD = "connection.proxyPassword";

    
    
    /**
     * The default connection options with regular TLS settings, without proxy and not leveraging system properties
     */
    public static final ConnectionOptions DEFAULT = new ConnectionOptions(false, false, false, null, 0, null, null, null);

    private ConnectionOptions(boolean isUseSystemPropertes, boolean isAllowSelfSignedCertificates, boolean isDisableHostnameVerification, String proxyHost, int proxyPort, String proxyProtocol, String proxyUsername, String proxyPassword) {
        super();
        this.isUseSystemPropertes = isUseSystemPropertes;
        this.isAllowSelfSignedCertificates = isAllowSelfSignedCertificates;
        this.isDisableHostnameVerification = isDisableHostnameVerification;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyProtocol = proxyProtocol;
        this.proxyUsername = proxyUsername;
        this.proxyPassword = proxyPassword;
    }

    public boolean isUseSystemPropertes() {
        return isUseSystemPropertes;
    }

    public boolean isAllowSelfSignedCertificates() {
        return isAllowSelfSignedCertificates;
    }

    public boolean isDisableHostnameVerification() {
        return isDisableHostnameVerification;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getProxyProtocol() {
        return proxyProtocol;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    @Override
    public String toString() {
        return "ConnectionOptions [isUseSystemPropertes=" + isUseSystemPropertes + ", isAllowSelfSignedCertificates="
                + isAllowSelfSignedCertificates + ", isDisableHostnameVerification=" + isDisableHostnameVerification + ", proxyHost="
                + proxyHost + ", proxyPort=" + proxyPort + ", proxyProtocol=" + proxyProtocol + ", proxyUsername=" + proxyUsername
                + ", proxyPassword=" + proxyPassword + "]";
    }

    public static ConnectionOptions fromServiceFactoryParameters(String parameterPrefix, Map<?, ?> parameters) {
        return new ConnectionOptions(
                Boolean.parseBoolean(parameters.get(parameterPrefix+PARAM_USE_SYSTEM_PROPERTIES).toString()), 
                Boolean.parseBoolean(parameters.get(parameterPrefix+PARAM_ALLOW_SELF_SIGNED_CERTIFICATS).toString()), 
                Boolean.parseBoolean(parameters.get(parameterPrefix+PARAM_DISABLE_HOSTNAME_VERIFICATION).toString()), 
                parameters.get(parameterPrefix+PARAM_PROXY_HOST).toString(),
                Integer.parseInt(parameters.get(parameterPrefix+PARAM_PROXY_PORT).toString()),
                parameters.get(parameterPrefix+PARAM_PROXY_PROTOCOL).toString(),
                parameters.get(parameterPrefix+PARAM_PROXY_USERNAME).toString(),
                parameters.get(parameterPrefix+PARAM_PROXY_PASSWORD).toString());
    }
}
