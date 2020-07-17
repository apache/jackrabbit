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

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.jackrabbit.spi2davex.Spi2davexRepositoryServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Advanced connection options to use for connections to a remote repository.
 *
 */
public final class ConnectionOptions {

    private static Logger log = LoggerFactory.getLogger(ConnectionOptions.class);

    private final boolean isUseSystemPropertes;
    private final int maxConnections;
    private final boolean isAllowSelfSignedCertificates;
    private final boolean isDisableHostnameVerification;
    private final String proxyHost;
    private final int proxyPort;
    private final String proxyProtocol;
    private final String proxyUsername;
    private final String proxyPassword;
    private final int connectionTimeoutMs;
    private final int requestTimeoutMs;
    private final int socketTimeoutMs;

    /**
     * Boolean flag whether to use the default Java system properties for setting proxy, TLS and further options as defined by {@link HttpClientBuilder}.
     * Default = {@code false}.
     */
    public static final String PARAM_USE_SYSTEM_PROPERTIES = "org.apache.jackrabbit.spi2dav.connection.useSystemProperties";

    /**
     * Boolean flag whether to allow self-signed certificates of remote repositories.
     * Default = {@code false}.
     */
    public static final String PARAM_ALLOW_SELF_SIGNED_CERTIFICATES = "org.apache.jackrabbit.spi2dav.connection.allowSelfSignedCertificates";
    
    /**
     * Boolean flag whether to disable the host name verification against the common name of the server's certificate.
     * Default = {@code false}.
     */
    public static final String PARAM_DISABLE_HOSTNAME_VERIFICATION = "org.apache.jackrabbit.spi2dav.connection.disableHostnameVerification";
    
    /**
     * The host of a proxy server.
     */
    public static final String PARAM_PROXY_HOST = "org.apache.jackrabbit.spi2dav.connection.proxyHost";
    
    /**
     * Integer value for the proxy's port. Only effective if {@link #PARAM_PROXY_HOST} is used as well. If -1 or not set the default for the scheme will be used.
     */
    public static final String PARAM_PROXY_PORT = "org.apache.jackrabbit.spi2dav.connection.proxyPort";
    
    /**
     * The protocol for which to use the proxy. Only effective if {@link #PARAM_PROXY_HOST} is used as well.
     */
    public static final String PARAM_PROXY_PROTOCOL = "org.apache.jackrabbit.spi2dav.connection.proxyProtocol";

    /**
     * The user name to authenticate at the proxy. Only effective if {@link #PARAM_PROXY_HOST} is used as well.
     */
    public static final String PARAM_PROXY_USERNAME = "org.apache.jackrabbit.spi2dav.connection.proxyUsername";

    /**
     * The password to authenticate at the proxy. Only effective if {@link #PARAM_PROXY_HOST} and {@link #PARAM_PROXY_USERNAME} are used as well.
     */
    public static final String PARAM_PROXY_PASSWORD = "org.apache.jackrabbit.spi2dav.connection.proxyPassword";

    /**
     * The connection timeout in milliseconds as Integer. -1 for default, 0 for infinite.
     */
    public static final String PARAM_CONNECTION_TIMEOUT_MS = "org.apache.jackrabbit.spi2dav.connection.connectionTimeoutMs";

    /**
     * The request timeout in milliseconds as Integer. -1 for default, 0 for infinite.
     */
    public static final String PARAM_REQUEST_TIMEOUT_MS = "org.apache.jackrabbit.spi2dav.connection.requestTimeoutMs";
    
    /**
     * The request timeout in milliseconds as Integer. -1 for default, 0 for infinite.
     */
    public static final String PARAM_SOCKET_TIMEOUT_MS = "org.apache.jackrabbit.spi2dav.connection.socketTimeoutMs";

    /**
     * Optional configuration parameter: Its value defines the
     * maximumConnectionsPerHost value on the HttpClient configuration and
     * must be an int greater than zero.
     */
    public static final String PARAM_MAX_CONNECTIONS = "connection.maxConnections";


    /**
     * Default value for the maximum number of connections per host such as
     * configured with {@link PoolingHttpClientConnectionManager#setDefaultMaxPerRoute(int)}.
     */
    public static final int MAX_CONNECTIONS_DEFAULT = 20;

    /**
     * The default connection options with regular TLS settings, without proxy and not leveraging system properties
     */
    public static final ConnectionOptions DEFAULT = new ConnectionOptions.Builder().build();

    private ConnectionOptions(boolean isUseSystemPropertes, int maxConnections, boolean isAllowSelfSignedCertificates, boolean isDisableHostnameVerification, int connectionTimeoutMs,  int requestTimeoutMs, int socketTimeoutMs, String proxyHost, int proxyPort, String proxyProtocol, String proxyUsername, String proxyPassword) {
        super();
        this.isUseSystemPropertes = isUseSystemPropertes;
        this.maxConnections = maxConnections;
        this.isAllowSelfSignedCertificates = isAllowSelfSignedCertificates;
        this.isDisableHostnameVerification = isDisableHostnameVerification;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.requestTimeoutMs = requestTimeoutMs;
        this.socketTimeoutMs = socketTimeoutMs;
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

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public int getSocketTimeoutMs() {
        return socketTimeoutMs;
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + connectionTimeoutMs;
        result = prime * result + (isAllowSelfSignedCertificates ? 1231 : 1237);
        result = prime * result + (isDisableHostnameVerification ? 1231 : 1237);
        result = prime * result + (isUseSystemPropertes ? 1231 : 1237);
        result = prime * result + maxConnections;
        result = prime * result + ((proxyHost == null) ? 0 : proxyHost.hashCode());
        result = prime * result + ((proxyPassword == null) ? 0 : proxyPassword.hashCode());
        result = prime * result + proxyPort;
        result = prime * result + ((proxyProtocol == null) ? 0 : proxyProtocol.hashCode());
        result = prime * result + ((proxyUsername == null) ? 0 : proxyUsername.hashCode());
        result = prime * result + requestTimeoutMs;
        result = prime * result + socketTimeoutMs;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConnectionOptions other = (ConnectionOptions) obj;
        if (connectionTimeoutMs != other.connectionTimeoutMs)
            return false;
        if (isAllowSelfSignedCertificates != other.isAllowSelfSignedCertificates)
            return false;
        if (isDisableHostnameVerification != other.isDisableHostnameVerification)
            return false;
        if (isUseSystemPropertes != other.isUseSystemPropertes)
            return false;
        if (maxConnections != other.maxConnections)
            return false;
        if (proxyHost == null) {
            if (other.proxyHost != null)
                return false;
        } else if (!proxyHost.equals(other.proxyHost))
            return false;
        if (proxyPassword == null) {
            if (other.proxyPassword != null)
                return false;
        } else if (!proxyPassword.equals(other.proxyPassword))
            return false;
        if (proxyPort != other.proxyPort)
            return false;
        if (proxyProtocol == null) {
            if (other.proxyProtocol != null)
                return false;
        } else if (!proxyProtocol.equals(other.proxyProtocol))
            return false;
        if (proxyUsername == null) {
            if (other.proxyUsername != null)
                return false;
        } else if (!proxyUsername.equals(other.proxyUsername))
            return false;
        if (requestTimeoutMs != other.requestTimeoutMs)
            return false;
        if (socketTimeoutMs != other.socketTimeoutMs)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ConnectionOptions [isUseSystemPropertes=" + isUseSystemPropertes + ", maxConnections=" + maxConnections
                + ", isAllowSelfSignedCertificates=" + isAllowSelfSignedCertificates + ", isDisableHostnameVerification="
                + isDisableHostnameVerification + ", proxyHost=" + proxyHost + ", proxyPort=" + proxyPort + ", proxyProtocol="
                + proxyProtocol + ", proxyUsername=" + proxyUsername + ", proxyPassword=" + proxyPassword + ", connectionTimeoutMs="
                + connectionTimeoutMs + ", requestTimeoutMs=" + requestTimeoutMs + ", socketTimeoutMs=" + socketTimeoutMs + "]";
    }

    public Map<String, String> toServiceFactoryParameters() {
        Map<String, String> parameters = new HashMap<>();
        if (isUseSystemPropertes) {
            parameters.put(PARAM_USE_SYSTEM_PROPERTIES, Boolean.toString(isUseSystemPropertes));
        }
        if (maxConnections != MAX_CONNECTIONS_DEFAULT) {
            parameters.put(PARAM_MAX_CONNECTIONS, Integer.toString(maxConnections));
        }
        if (isAllowSelfSignedCertificates) {
            parameters.put(PARAM_ALLOW_SELF_SIGNED_CERTIFICATES, Boolean.toString(isAllowSelfSignedCertificates));
        }
        if (isDisableHostnameVerification) {
            parameters.put(PARAM_DISABLE_HOSTNAME_VERIFICATION, Boolean.toString(isDisableHostnameVerification));
        }
        if (connectionTimeoutMs != -1) {
            parameters.put(PARAM_CONNECTION_TIMEOUT_MS, Integer.toString(connectionTimeoutMs));
        }
        if (requestTimeoutMs != -1) {
            parameters.put(PARAM_REQUEST_TIMEOUT_MS, Integer.toString(requestTimeoutMs));
        }
        if (socketTimeoutMs != -1) {
            parameters.put(PARAM_SOCKET_TIMEOUT_MS, Integer.toString(socketTimeoutMs));
        }
        if (proxyHost != null) {
            parameters.put(PARAM_PROXY_HOST, proxyHost);
        }
        if (proxyPort != -1) {
            parameters.put(PARAM_PROXY_PORT, Integer.toString(proxyPort));
        }
        if (proxyProtocol != null) {
            parameters.put(PARAM_PROXY_PROTOCOL, proxyProtocol);
        }
        if (proxyUsername != null) {
            parameters.put(PARAM_PROXY_USERNAME, proxyUsername);
        }
        if (proxyPassword != null) {
            parameters.put(PARAM_PROXY_PASSWORD, proxyPassword);
        }
        return parameters;
    }

    public static ConnectionOptions fromServiceFactoryParameters(Map<?, ?> parameters) {
        return new ConnectionOptions(
                getBooleanValueFromParameter(parameters, false, PARAM_USE_SYSTEM_PROPERTIES),
                getIntegerValueFromParameter(parameters, MAX_CONNECTIONS_DEFAULT, PARAM_MAX_CONNECTIONS, Spi2davRepositoryServiceFactory.PARAM_MAX_CONNECTIONS, Spi2davexRepositoryServiceFactory.PARAM_MAX_CONNECTIONS),
                getBooleanValueFromParameter(parameters, false, PARAM_ALLOW_SELF_SIGNED_CERTIFICATES),
                getBooleanValueFromParameter(parameters, false, PARAM_DISABLE_HOSTNAME_VERIFICATION),
                getIntegerValueFromParameter(parameters, -1, PARAM_CONNECTION_TIMEOUT_MS),
                getIntegerValueFromParameter(parameters, -1, PARAM_REQUEST_TIMEOUT_MS),
                getIntegerValueFromParameter(parameters, -1, PARAM_SOCKET_TIMEOUT_MS),
                getStringValueFromParameter(parameters, null, PARAM_PROXY_HOST),
                getIntegerValueFromParameter(parameters, -1, PARAM_PROXY_PORT),
                getStringValueFromParameter(parameters, null, PARAM_PROXY_PROTOCOL),
                getStringValueFromParameter(parameters, null, PARAM_PROXY_USERNAME),
                getStringValueFromParameter(parameters, null, PARAM_PROXY_PASSWORD));
    }

    private static int getIntegerValueFromParameter(Map<?, ?> parameters, int defaultValue, String... parameterKeys) {
        for (String key : parameterKeys) {
            Object value = parameters.get(key);
            if (value != null) {
                try {
                    return Integer.parseInt(value.toString());
                } catch (NumberFormatException e) {
                    log.warn("Invalid integer value '{}' given for parameter '{}'. Using default '{}' instead.",  value, key, defaultValue);
                    // using default
                }
            }
        }
        return defaultValue;
    }

    private static boolean getBooleanValueFromParameter(Map<?, ?> parameters, boolean defaultValue, String... parameterKeys) {
        for (String key : parameterKeys) {
            Object value = parameters.get(key);
            if (value != null) {
                return Boolean.parseBoolean(value.toString());
            }
        }
        return defaultValue;
    }

    private static String getStringValueFromParameter(Map<?, ?> parameters, String defaultValue, String... parameterKeys) {
        for (String key : parameterKeys) {
            Object value = parameters.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return defaultValue;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean isUseSystemPropertes;
        private int maxConnections = MAX_CONNECTIONS_DEFAULT;
        private boolean isAllowSelfSignedCertificates;
        private boolean isDisableHostnameVerification;
        private String proxyHost;
        private int proxyPort = -1;
        private String proxyProtocol;
        private String proxyUsername;
        private String proxyPassword;
        private int connectionTimeoutMs = -1;
        private int requestTimeoutMs = -1;
        private int socketTimeoutMs = -1;

        public Builder useSystemProperties(boolean isUseSystemPropertes) {
            this.isUseSystemPropertes = isUseSystemPropertes;
            return this;
        }

        public Builder maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public Builder allowSelfSignedCertificates(boolean isAllowSelfSignedCertificates) {
            this.isAllowSelfSignedCertificates = isAllowSelfSignedCertificates;
            return this;
        }

        public Builder disableHostnameVerification(boolean isDisableHostnameVerification) {
            this.isDisableHostnameVerification = isDisableHostnameVerification;
            return this;
        }

        public Builder connectionTimeoutMs(int connectionTimeoutMs) {
            this.connectionTimeoutMs = connectionTimeoutMs;
            return this;
        }

        public Builder requestTimeoutMs(int requestTimeoutMs) {
            this.requestTimeoutMs = requestTimeoutMs;
            return this;
        }

        public Builder socketTimeoutMs(int socketTimeoutMs) {
            this.socketTimeoutMs = socketTimeoutMs;
            return this;
        }

        public Builder proxyHost(String proxyHost) {
            this.proxyHost = proxyHost;
            return this;
        }

        public Builder proxyPort(int proxyPort) {
            this.proxyPort = proxyPort;
            return this;
        }

        public Builder proxyProtocol(String proxyProtocol) {
            this.proxyProtocol = proxyProtocol;
            return this;
        }

        public Builder proxyUsername(String proxyUsername) {
            this.proxyUsername = proxyUsername;
            return this;
        }

        public Builder proxyPassword(String proxyPassword) {
            this.proxyPassword = proxyPassword;
            return this;
        }

        public ConnectionOptions build() {
            return new ConnectionOptions(isUseSystemPropertes, maxConnections, 
                    isAllowSelfSignedCertificates, isDisableHostnameVerification,
                    connectionTimeoutMs, requestTimeoutMs, socketTimeoutMs,
                    proxyHost, proxyPort, proxyProtocol, proxyUsername, proxyPassword);
        }
    }
}
