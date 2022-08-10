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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.lock.LockException;
import javax.net.ssl.SSLContext;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.jackrabbit.commons.webdav.AtomFeedConstants;
import org.apache.jackrabbit.commons.webdav.EventUtil;
import org.apache.jackrabbit.commons.webdav.JcrRemotingConstants;
import org.apache.jackrabbit.commons.webdav.JcrValueType;
import org.apache.jackrabbit.commons.webdav.NodeTypeConstants;
import org.apache.jackrabbit.commons.webdav.NodeTypeUtil;
import org.apache.jackrabbit.commons.webdav.ValueUtil;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.ItemInfoCache;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.Subscription;
import org.apache.jackrabbit.spi.Tree;
import org.apache.jackrabbit.spi.commons.ChildInfoImpl;
import org.apache.jackrabbit.spi.commons.EventBundleImpl;
import org.apache.jackrabbit.spi.commons.EventFilterImpl;
import org.apache.jackrabbit.spi.commons.ItemInfoCacheImpl;
import org.apache.jackrabbit.spi.commons.conversion.IdentifierResolver;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingNameResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingPathResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.iterator.Iterators;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.namespace.AbstractNamespaceResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.nodetype.compact.CompactNodeTypeDefWriter;
import org.apache.jackrabbit.spi.commons.privilege.PrivilegeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.value.QValueValue;
import org.apache.jackrabbit.spi.commons.value.ValueFactoryQImpl;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.BaseDavRequest;
import org.apache.jackrabbit.webdav.client.methods.HttpCheckin;
import org.apache.jackrabbit.webdav.client.methods.HttpCheckout;
import org.apache.jackrabbit.webdav.client.methods.HttpCopy;
import org.apache.jackrabbit.webdav.client.methods.HttpDelete;
import org.apache.jackrabbit.webdav.client.methods.HttpLabel;
import org.apache.jackrabbit.webdav.client.methods.HttpLock;
import org.apache.jackrabbit.webdav.client.methods.HttpMerge;
import org.apache.jackrabbit.webdav.client.methods.HttpMkcol;
import org.apache.jackrabbit.webdav.client.methods.HttpMkworkspace;
import org.apache.jackrabbit.webdav.client.methods.HttpMove;
import org.apache.jackrabbit.webdav.client.methods.HttpOptions;
import org.apache.jackrabbit.webdav.client.methods.HttpOrderpatch;
import org.apache.jackrabbit.webdav.client.methods.HttpPoll;
import org.apache.jackrabbit.webdav.client.methods.HttpPropfind;
import org.apache.jackrabbit.webdav.client.methods.HttpProppatch;
import org.apache.jackrabbit.webdav.client.methods.HttpReport;
import org.apache.jackrabbit.webdav.client.methods.HttpSearch;
import org.apache.jackrabbit.webdav.client.methods.HttpSubscribe;
import org.apache.jackrabbit.webdav.client.methods.HttpUnlock;
import org.apache.jackrabbit.webdav.client.methods.HttpUnsubscribe;
import org.apache.jackrabbit.webdav.client.methods.HttpUpdate;
import org.apache.jackrabbit.webdav.client.methods.XmlEntity;
import org.apache.jackrabbit.webdav.header.CodedUrlHeader;
import org.apache.jackrabbit.webdav.header.IfHeader;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockDiscovery;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.observation.DefaultEventType;
import org.apache.jackrabbit.webdav.observation.EventDiscovery;
import org.apache.jackrabbit.webdav.observation.EventType;
import org.apache.jackrabbit.webdav.observation.ObservationConstants;
import org.apache.jackrabbit.webdav.observation.SubscriptionInfo;
import org.apache.jackrabbit.webdav.ordering.OrderPatch;
import org.apache.jackrabbit.webdav.ordering.OrderingConstants;
import org.apache.jackrabbit.webdav.ordering.Position;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.HrefProperty;
import org.apache.jackrabbit.webdav.search.SearchInfo;
import org.apache.jackrabbit.webdav.security.CurrentUserPrivilegeSetProperty;
import org.apache.jackrabbit.webdav.security.Privilege;
import org.apache.jackrabbit.webdav.security.SecurityConstants;
import org.apache.jackrabbit.webdav.security.SupportedPrivilege;
import org.apache.jackrabbit.webdav.security.SupportedPrivilegeSetProperty;
import org.apache.jackrabbit.webdav.transaction.TransactionConstants;
import org.apache.jackrabbit.webdav.transaction.TransactionInfo;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.apache.jackrabbit.webdav.version.LabelInfo;
import org.apache.jackrabbit.webdav.version.MergeInfo;
import org.apache.jackrabbit.webdav.version.UpdateInfo;
import org.apache.jackrabbit.webdav.version.VersionControlledResource;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.ElementIterator;
import org.apache.jackrabbit.webdav.xml.XmlSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * <code>RepositoryServiceImpl</code>...
 */
// TODO: encapsulate URI building, escaping, unescaping...
// TODO: TO-BE-FIXED. caches don't get adjusted upon removal/move of items
public class RepositoryServiceImpl implements RepositoryService, DavConstants {

    private static Logger log = LoggerFactory.getLogger(RepositoryServiceImpl.class);

    private static final SubscriptionInfo S_INFO = new SubscriptionInfo(DefaultEventType.create(EventUtil.EVENT_ALL, ItemResourceConstants.NAMESPACE), true, INFINITE_TIMEOUT);

    /**
     * Key for the client map during repo creation (no sessionInfo present)
     */
    private static final String CLIENT_KEY = "repoCreation";

    /**
     * Default value for the maximum number of connections per host such as
     * configured with {@link PoolingHttpClientConnectionManager#setDefaultMaxPerRoute(int)}.
     */
    public static final int MAX_CONNECTIONS_DEFAULT = 20;

    private final IdFactory idFactory;
    private final NameFactory nameFactory;
    private final PathFactory pathFactory;
    private final QValueFactory qValueFactory;
    private final ValueFactory valueFactory;

    private final int itemInfoCacheSize;

    private final NamespaceCache nsCache;
    private final URIResolverImpl uriResolver;

    private final HttpHost httpHost;
    private final ConcurrentMap<Object, HttpClient> clients;
    private final HttpClientBuilder httpClientBuilder;
    private final Map<AuthScope, org.apache.http.auth.Credentials> commonCredentials;

    private final Map<Name, QNodeTypeDefinition> nodeTypeDefinitions = new HashMap<Name, QNodeTypeDefinition>();

    /** Repository descriptors. */
    private final Map<String, QValue[]> descriptors =
            new HashMap<String, QValue[]>();

    /** Observation features. */
    private boolean remoteServerProvidesNodeTypes = false;
    private boolean remoteServerProvidesNoLocalFlag = false;

    /* DAV conformance levels */
    private Set<String> remoteDavComplianceClasses = null;

    /**
     * Same as {@link #RepositoryServiceImpl(String, IdFactory, NameFactory, PathFactory, QValueFactory, int, ConnectionOptions)}
     * using {@link ItemInfoCacheImpl#DEFAULT_CACHE_SIZE} as size for the item
     * cache and {@link #MAX_CONNECTIONS_DEFAULT} for the maximum number of
     * connections on the client and {@link ConnectionOptions#DEFAULT}.
     *
     * @param uri The server uri.
     * @param idFactory The id factory.
     * @param nameFactory The name factory.
     * @param pathFactory The path factory.
     * @param qValueFactory The value factory.
     * @throws RepositoryException If an error occurs.
     */
    public RepositoryServiceImpl(String uri, IdFactory idFactory,
                                 NameFactory nameFactory, PathFactory pathFactory,
                                 QValueFactory qValueFactory) throws RepositoryException {
        this(uri, idFactory, nameFactory, pathFactory, qValueFactory, ItemInfoCacheImpl.DEFAULT_CACHE_SIZE);
    }

    /**
     * Same as {@link #RepositoryServiceImpl(String, IdFactory, NameFactory, PathFactory, QValueFactory, int, ConnectionOptions)}
     * using {@link #MAX_CONNECTIONS_DEFAULT} for the maximum number of
     * connections on the client and {@link ConnectionOptions#DEFAULT}.
     *
     * @param uri The server uri.
     * @param idFactory The id factory.
     * @param nameFactory The name factory.
     * @param pathFactory The path factory.
     * @param qValueFactory The value factory.
     * @param itemInfoCacheSize The size of the item info cache.
     * @throws RepositoryException If an error occurs.
     */
    public RepositoryServiceImpl(String uri, IdFactory idFactory,
                                 NameFactory nameFactory, PathFactory pathFactory,
                                 QValueFactory qValueFactory, int itemInfoCacheSize) throws RepositoryException {
        this(uri, idFactory, nameFactory, pathFactory, qValueFactory, itemInfoCacheSize, ConnectionOptions.DEFAULT);
    }

    /**
     * Creates a new instance of this repository service.
     *
     * @param uri The server uri.
     * @param idFactory The id factory.
     * @param nameFactory The name factory.
     * @param pathFactory The path factory.
     * @param qValueFactory The value factory.
     * @param itemInfoCacheSize The size of the item info cache.
     * @param connectionOptions The advanced connection options.
     * @throws RepositoryException If an error occurs.
     */
    public RepositoryServiceImpl(String uri, IdFactory idFactory,
                                 NameFactory nameFactory, PathFactory pathFactory,
                                 QValueFactory qValueFactory, int itemInfoCacheSize,
                                 ConnectionOptions connectionOptions) throws RepositoryException {
        if (uri == null || "".equals(uri)) {
            throw new RepositoryException("Invalid repository uri '" + uri + "'.");
        }

        if (idFactory == null || qValueFactory == null) {
            throw new RepositoryException("IdFactory and QValueFactory may not be null.");
        }
        this.idFactory = idFactory;
        this.nameFactory = nameFactory;
        this.pathFactory = pathFactory;
        this.qValueFactory = qValueFactory;
        this.itemInfoCacheSize = itemInfoCacheSize;
        this.commonCredentials = new HashMap<>();

        try {
            URI repositoryUri = computeRepositoryUri(uri);
            httpHost = new HttpHost(repositoryUri.getHost(), repositoryUri.getPort(), repositoryUri.getScheme());

            nsCache = new NamespaceCache();
            uriResolver = new URIResolverImpl(repositoryUri, this, DomUtil.createDocument());
            NamePathResolver resolver = new NamePathResolverImpl(nsCache);
            valueFactory = new ValueFactoryQImpl(qValueFactory, resolver);

        } catch (URISyntaxException e) {
            throw new RepositoryException(e);
        } catch (ParserConfigurationException e) {
            throw new RepositoryException(e);
        }

        HttpClientBuilder hcb = HttpClients.custom();

        final SSLConnectionSocketFactory sslSocketFactory;

        // request config
        RequestConfig requestConfig = RequestConfig.custom().
                setConnectTimeout(connectionOptions.getConnectionTimeoutMs()).
                setConnectionRequestTimeout(connectionOptions.getRequestTimeoutMs()).
                setSocketTimeout(connectionOptions.getSocketTimeoutMs()).build();
        hcb.setDefaultRequestConfig(requestConfig);
        if (Boolean.getBoolean("jackrabbit.client.useSystemProperties") || connectionOptions.isUseSystemPropertes()) {
            log.debug("Using system properties for establishing connection!");

            if (connectionOptions.isAllowSelfSignedCertificates()) {
                throw new RepositoryException(ConnectionOptions.PARAM_ALLOW_SELF_SIGNED_CERTIFICATES
                        + " is not allowed when system properties (jackrabbit.client.useSystemProperties) have been specified.");
            }
            if (connectionOptions.isDisableHostnameVerification()) {
                throw new RepositoryException(ConnectionOptions.PARAM_DISABLE_HOSTNAME_VERIFICATION
                        + " is not allowed when system properties (jackrabbit.client.useSystemProperties) have been specified.");
            }

            // support Java system proxy? (JCR-3211)
            hcb.useSystemProperties();

            sslSocketFactory = SSLConnectionSocketFactory.getSystemSocketFactory();
        } else {
            // TLS settings (via connection manager)
            final SSLContext sslContext;
            try {
                if (connectionOptions.isAllowSelfSignedCertificates()) {
                    log.warn("Nonsecure TLS setting: Accepting self-signed certificates!");
                        sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustSelfSignedStrategy()).build();
                        hcb.setSSLContext(sslContext);
                } else {
                    sslContext = SSLContextBuilder.create().build();
                }
            } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
                throw new RepositoryException(e);
            }

            if (connectionOptions.isDisableHostnameVerification()) {
                log.warn("Nonsecure TLS setting: Host name verification of TLS certificates disabled!");
                // we can optionally disable hostname verification.
                sslSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
            } else {
                sslSocketFactory = new SSLConnectionSocketFactory(sslContext);
            }
        }

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .register("https", sslSocketFactory)
            .build();

        PoolingHttpClientConnectionManager cmgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        int maxConnections = connectionOptions.getMaxConnections();
        if (maxConnections > 0) {
            cmgr.setDefaultMaxPerRoute(connectionOptions.getMaxConnections());
            cmgr.setMaxTotal(connectionOptions.getMaxConnections());
        } else {
            maxConnections = ConnectionOptions.MAX_CONNECTIONS_DEFAULT;
        }
        hcb.setConnectionManager(cmgr);

        if (connectionOptions.getProxyHost() != null) {
            // https://hc.apache.org/httpcomponents-client-4.5.x/tutorial/html/connmgmt.html#d5e485
            HttpHost proxy = new HttpHost(connectionOptions.getProxyHost(), connectionOptions.getProxyPort(), connectionOptions.getProxyProtocol());
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            hcb.setRoutePlanner(routePlanner);
            log.debug("Connection via proxy {}", proxy);
            if (connectionOptions.getProxyUsername() != null) {
                log.debug("Proxy connection with credentials {}", proxy);
                commonCredentials.put(
                        new AuthScope(proxy),
                        new UsernamePasswordCredentials(connectionOptions.getProxyUsername(), connectionOptions.getProxyPassword()));
                hcb.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
            }
        }
        httpClientBuilder = hcb;

        // This configuration of the clients cache assumes that the level of
        // concurrency on this map will be equal to the default number of maximum
        // connections allowed on the httpClient level.
        // TODO: review again
        clients = new ConcurrentHashMap<Object, HttpClient>(maxConnections, .75f, maxConnections);
    }

    private static void checkSessionInfo(SessionInfo sessionInfo) throws RepositoryException {
        if (!(sessionInfo instanceof SessionInfoImpl)) {
            throw new RepositoryException("Unknown SessionInfo implementation.");
        }
    }

    /**
     * Resolve the given URI against a base URI (usually the request URI of an HTTP request)
     */
    private static String resolve(String baseUri, String relUri) throws RepositoryException {
        try {
            java.net.URI base = new java.net.URI(baseUri);
            java.net.URI rel = new java.net.URI(relUri);
            return base.resolve(rel).toString();
        } catch (URISyntaxException ex) {
            throw new RepositoryException(ex);
        }
    }

    private static void checkSubscription(Subscription subscription) throws RepositoryException {
        if (!(subscription instanceof EventSubscriptionImpl)) {
            throw new RepositoryException("Unknown Subscription implementation.");
        }
    }

    private static boolean isUnLockMethod(HttpUriRequest request) {
        int code = DavMethods.getMethodCode(request.getMethod());
        return DavMethods.DAV_UNLOCK == code;
    }

    protected static void initMethod(HttpUriRequest request, SessionInfo sessionInfo, boolean addIfHeader) throws RepositoryException {
        if (addIfHeader) {
            checkSessionInfo(sessionInfo);
            Set<String> allLockTokens = ((SessionInfoImpl) sessionInfo).getAllLockTokens();
            // TODO: ev. build tagged if header
            if (!allLockTokens.isEmpty()) {
                String[] locktokens = allLockTokens.toArray(new String[allLockTokens.size()]);
                IfHeader ifH = new IfHeader(locktokens);
                request.setHeader(ifH.getHeaderName(), ifH.getHeaderValue());
            }
        }

        initMethod(request, sessionInfo);
    }

    // set of HTTP methods that will not change the remote state
    private static final Set<String> readMethods;
    static {
        Set<String> tmp = new HashSet<String>();
        tmp.add("GET");
        tmp.add("HEAD");
        tmp.add("PROPFIND");
        tmp.add("POLL");
        tmp.add("REPORT");
        tmp.add("SEARCH");
        readMethods = Collections.unmodifiableSet(tmp);
    }

    // set headers for user data and session identification
    protected static void initMethod(HttpUriRequest request, SessionInfo sessionInfo) throws RepositoryException {

        boolean isReadAccess = readMethods.contains(request.getMethod());
        boolean needsSessionId = !isReadAccess || "POLL".equals(request.getMethod());

        if (sessionInfo instanceof SessionInfoImpl && needsSessionId) {
            request.addHeader("Link", generateLinkHeaderFieldValue(sessionInfo, isReadAccess));
        }
    }

    private static String generateLinkHeaderFieldValue(SessionInfo sessionInfo, boolean isReadAccess) {
        StringBuilder linkHeaderField = new StringBuilder();

        String sessionIdentifier = ((SessionInfoImpl) sessionInfo).getSessionIdentifier();
        linkHeaderField.append("<").append(sessionIdentifier).append(">; rel=\"")
                .append(JcrRemotingConstants.RELATION_REMOTE_SESSION_ID).append("\"");

        String userdata = ((SessionInfoImpl) sessionInfo).getUserData();
        if (userdata != null && !isReadAccess) {
            String escaped = Text.escape(userdata);
            linkHeaderField.append(", <data:,").append(escaped).append(">; rel=\"").append(JcrRemotingConstants.RELATION_USER_DATA)
                    .append("\"");
        }
        return linkHeaderField.toString();
    }

    private static void initMethod(HttpUriRequest request, BatchImpl batchImpl, boolean addIfHeader) throws RepositoryException {
        initMethod(request, batchImpl.sessionInfo, addIfHeader);

        // add batchId as separate header, TODO: could probably re-use session id Link relation
        CodedUrlHeader ch = new CodedUrlHeader(TransactionConstants.HEADER_TRANSACTIONID, batchImpl.batchId);
        request.setHeader(ch.getHeaderName(), ch.getHeaderValue());
    }

    private static boolean isSameResource(String requestURI, MultiStatusResponse response) {
        try {
            String href = resolve(requestURI, response.getHref());
            if (href.endsWith("/") && !requestURI.endsWith("/")) {
                href = href.substring(0, href.length() - 1);
            }
            return requestURI.equals(href);
        } catch (RepositoryException e) {
            return false;
        }
    }

    private String saveGetIdString(ItemId id, SessionInfo sessionInfo) {
        NamePathResolver resolver = null;
        try {
            resolver = getNamePathResolver(sessionInfo);
        } catch (RepositoryException e) {
            // ignore.
        }
        return saveGetIdString(id, resolver);
    }

    private String saveGetIdString(ItemId id, NamePathResolver resolver) {
        StringBuffer bf = new StringBuffer();
        String uid = id.getUniqueID();
        if (uid != null) {
            bf.append(uid);
        }
        Path p = id.getPath();
        if (p != null) {
            if (resolver == null) {
                bf.append(p.toString());
            } else {
                try {
                    bf.append(resolver.getJCRPath(p));
                } catch (NamespaceException e) {
                    bf.append(p.toString());
                }
            }
        }
        return bf.toString();
    }

    protected NamePathResolver getNamePathResolver(SessionInfo sessionInfo) throws RepositoryException {
        checkSessionInfo(sessionInfo);
        return getNamePathResolver(((SessionInfoImpl) sessionInfo));
    }

    private NamePathResolver getNamePathResolver(SessionInfoImpl sessionInfo) {
        NamePathResolver resolver = sessionInfo.getNamePathResolver();
        if (resolver == null) {
            resolver = new NamePathResolverImpl(sessionInfo);
            sessionInfo.setNamePathResolver(resolver);
        }
        return resolver;
    }

    /**
     * Returns a key for the httpClient hash. The key is either the specified
     * SessionInfo or a marker if the session info is null (used during
     * repository instantiation).
     *
     * @param sessionInfo
     * @return Key for the client map.
     */
    private static Object getClientKey(SessionInfo sessionInfo) {
        return (sessionInfo == null) ? CLIENT_KEY : sessionInfo;
    }

    protected HttpClient getClient(SessionInfo sessionInfo) throws RepositoryException {
        Object clientKey = getClientKey(sessionInfo);
        HttpClient client = clients.get(clientKey);
        if (client == null) {
            client = httpClientBuilder.build();
            if (sessionInfo != null) {
                checkSessionInfo(sessionInfo);
                clients.put(clientKey, client);
                log.debug("Created Client " + client + " for SessionInfo " + sessionInfo);
            }
        }
        return client;
    }

    protected HttpContext getContext(SessionInfo sessionInfo) throws RepositoryException {
        HttpClientContext result = HttpClientContext.create();
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        result.setCredentialsProvider(credsProvider);
        // take over default credentials (e.g. for proxy)
        for (Map.Entry<AuthScope, org.apache.http.auth.Credentials> entry : commonCredentials.entrySet()) {
            credsProvider.setCredentials(entry.getKey(), entry.getValue());
        }
        if (sessionInfo != null) {
            checkSessionInfo(sessionInfo);
            org.apache.http.auth.Credentials creds = ((SessionInfoImpl) sessionInfo).getCredentials().getHttpCredentials();
            if (creds != null) {
                credsProvider.setCredentials(new org.apache.http.auth.AuthScope(httpHost.getHostName(), httpHost.getPort()), creds);
                BasicScheme basicAuth = new BasicScheme();
                AuthCache authCache = new BasicAuthCache();
                authCache.put(httpHost, basicAuth);
                result.setAuthCache(authCache);
            }
        }
        return result;
    }

    private void removeClient(SessionInfo sessionInfo) {
        HttpClient cl = clients.remove(getClientKey(sessionInfo));
        log.debug("Removed Client " + cl + " for SessionInfo " + sessionInfo);
    }

    protected String getItemUri(ItemId itemId, SessionInfo sessionInfo) throws RepositoryException {
        return getItemUri(itemId, sessionInfo, sessionInfo.getWorkspaceName());
    }

    protected String getItemUri(ItemId itemId, SessionInfo sessionInfo, String workspaceName) throws RepositoryException {
        return uriResolver.getItemUri(itemId, workspaceName, sessionInfo);
    }

    /**
     * Clear all URI mappings. This is required after hierarchy operations such
     * as e.g. MOVE.
     *
     * @param sessionInfo
     */
    protected void clearItemUriCache(SessionInfo sessionInfo) {
        uriResolver.clearCacheEntries(sessionInfo);
    }

    private String getItemUri(NodeId parentId, Name childName,
                              SessionInfo sessionInfo) throws RepositoryException {
        String parentUri = uriResolver.getItemUri(parentId, sessionInfo.getWorkspaceName(), sessionInfo);
        NamePathResolver resolver = getNamePathResolver(sessionInfo);
        // JCR-2920: don't append '/' to a trailing '/'
        if (!parentUri.endsWith("/")) {
            parentUri += "/";
        }
        return parentUri + Text.escape(resolver.getJCRName(childName));
    }

    private NodeId getParentId(String baseUri, DavPropertySet propSet, SessionInfo sessionInfo)
        throws RepositoryException {
        NodeId parentId = null;
        DavProperty<?> p = propSet.get(JcrRemotingConstants.JCR_PARENT_LN, ItemResourceConstants.NAMESPACE);
        if (p != null) {
            HrefProperty parentProp = new HrefProperty(p);
            String parentHref = parentProp.getHrefs().get(0);
            if (parentHref != null && parentHref.length() > 0) {
                parentId = uriResolver.getNodeId(resolve(baseUri, parentHref), sessionInfo);
            }
        }
        return parentId;
    }

    String getUniqueID(DavPropertySet propSet) {
        DavProperty<?> prop = propSet.get(JcrRemotingConstants.JCR_UUID_LN, ItemResourceConstants.NAMESPACE);
        if (prop != null) {
            return prop.getValue().toString();
        } else {
            return null;
        }
    }

    Name getQName(DavPropertySet propSet, NamePathResolver resolver) throws RepositoryException {
        DavProperty<?> nameProp = propSet.get(JcrRemotingConstants.JCR_NAME_LN, ItemResourceConstants.NAMESPACE);
        if (nameProp != null && nameProp.getValue() != null) {
            // not root node. Note that 'unespacing' is not required since
            // the jcr:name property does not provide the value in escaped form.
            String jcrName = nameProp.getValue().toString();
            try {
                return resolver.getQName(jcrName);
            } catch (NameException e) {
                throw new RepositoryException(e);
            }
        } else {
            return NameConstants.ROOT;
        }
    }

    int getIndex(DavPropertySet propSet) {
        int index = Path.INDEX_UNDEFINED;
        DavProperty<?> indexProp = propSet.get(JcrRemotingConstants.JCR_INDEX_LN, ItemResourceConstants.NAMESPACE);
        if (indexProp != null && indexProp.getValue() != null) {
            index = Integer.parseInt(indexProp.getValue().toString());
        }
        return index;
    }

    //--------------------------------------------------------------------------
    /**
     * Execute a 'Workspace' operation.
     */
    private HttpResponse execute(BaseDavRequest request, SessionInfo sessionInfo) throws RepositoryException {
        try {
            initMethod(request, sessionInfo, !isUnLockMethod(request));

            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);
            return response;
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e, request);
        }
    }

    //--------------------------------------------------< RepositoryService >---

    @Override
    public IdFactory getIdFactory() {
        return idFactory;
    }

    @Override
    public NameFactory getNameFactory() {
        return nameFactory;
    }

    @Override
    public PathFactory getPathFactory() {
        return pathFactory;
    }

    @Override
    public QValueFactory getQValueFactory() {
        return qValueFactory;
    }

    @Override
    public ItemInfoCache getItemInfoCache(SessionInfo sessionInfo) throws RepositoryException {
        return new ItemInfoCacheImpl(itemInfoCacheSize);
    }

    @Override
    public Map<String, QValue[]> getRepositoryDescriptors() throws RepositoryException {
        if (descriptors.isEmpty()) {
            ReportInfo info = new ReportInfo(JcrRemotingConstants.REPORT_REPOSITORY_DESCRIPTORS, ItemResourceConstants.NAMESPACE);
            HttpReport request = null;
            try {
                request = new HttpReport(uriResolver.getRepositoryUri(), info);
                HttpResponse response = executeRequest(null, request);
                int sc = response.getStatusLine().getStatusCode();
                if (sc == HttpStatus.SC_UNAUTHORIZED
                        || sc == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
                    // JCR-3076: Mandatory authentication prevents us from
                    // accessing the descriptors on the server, so instead
                    // of failing with an exception we simply return an empty
                    // set of descriptors
                    log.warn("Authentication required to access repository descriptors");
                    return descriptors;
                }

                request.checkSuccess(response);
                Document doc = request.getResponseBodyAsDocument(response.getEntity());

                if (doc != null) {
                    Element rootElement = doc.getDocumentElement();
                    ElementIterator nsElems = DomUtil.getChildren(rootElement, JcrRemotingConstants.XML_DESCRIPTOR, ItemResourceConstants.NAMESPACE);
                    while (nsElems.hasNext()) {
                        Element elem = nsElems.nextElement();
                        String key = DomUtil.getChildText(elem, JcrRemotingConstants.XML_DESCRIPTORKEY, ItemResourceConstants.NAMESPACE);
                        ElementIterator it = DomUtil.getChildren(elem, JcrRemotingConstants.XML_DESCRIPTORVALUE, ItemResourceConstants.NAMESPACE);
                        List<QValue> vs = new ArrayList<QValue>();
                        while (it.hasNext()) {
                            Element dv = it.nextElement();
                            String descriptor = DomUtil.getText(dv);
                            if (key != null && descriptor != null) {
                                String typeStr = (DomUtil.getAttribute(dv, JcrRemotingConstants.ATTR_VALUE_TYPE, null));
                                int type = (typeStr == null) ? PropertyType.STRING : PropertyType.valueFromName(typeStr);
                                vs.add(getQValueFactory().create(descriptor, type));
                            } else {
                                log.error("Invalid descriptor key / value pair: " + key + " -> " + descriptor);
                            }

                        }
                        descriptors.put(key, vs.toArray(new QValue[vs.size()]));
                    }
                }
            } catch (IOException e) {
                throw new RepositoryException(e);
            } catch (DavException e) {
                throw ExceptionConverter.generate(e);
            } finally {
                if (request != null) {
                    request.releaseConnection();
                }
            }
        }
        return descriptors;
    }

    @Override
    public SessionInfo obtain(Credentials credentials, String workspaceName)
        throws RepositoryException {
        CredentialsWrapper dc = new CredentialsWrapper(credentials);
        return obtain(dc, workspaceName);
    }

    @Override
    public SessionInfo obtain(SessionInfo sessionInfo, String workspaceName)
        throws RepositoryException {
        checkSessionInfo(sessionInfo);
        return obtain(((SessionInfoImpl) sessionInfo).getCredentials(), workspaceName);
    }

    @Override
    public SessionInfo impersonate(SessionInfo sessionInfo, Credentials credentials) throws RepositoryException {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    private SessionInfo obtain(CredentialsWrapper credentials, String workspaceName)
        throws RepositoryException {
        // check if the workspace with the given name is accessible
        HttpPropfind request = null;
        SessionInfoImpl sessionInfo = new SessionInfoImpl(credentials, workspaceName);
        try {
            DavPropertyNameSet nameSet = new DavPropertyNameSet();
            // for backwards compat. -> retrieve DAV:workspace if the newly
            // added property (workspaceName) is not supported by the server.
            nameSet.add(DeltaVConstants.WORKSPACE);
            nameSet.add(JcrRemotingConstants.JCR_WORKSPACE_NAME_LN, ItemResourceConstants.NAMESPACE);

            request = new HttpPropfind(uriResolver.getWorkspaceUri(workspaceName), nameSet, DEPTH_0);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);

            MultiStatusResponse[] responses = request.getResponseBodyAsMultiStatus(response).getResponses();
            if (responses.length != 1) {
                throw new LoginException("Login failed: Unknown workspace '" + workspaceName + "'.");
            }

            DavPropertySet props = responses[0].getProperties(DavServletResponse.SC_OK);
            DavProperty<?> prop = props.get(JcrRemotingConstants.JCR_WORKSPACE_NAME_LN, ItemResourceConstants.NAMESPACE);
            if (prop != null) {
                String wspName = prop.getValue().toString();
                if (workspaceName == null) {
                    // login with 'null' workspace name -> retrieve the effective
                    // workspace name from the property and recreate the SessionInfo.
                    sessionInfo = new SessionInfoImpl(credentials, wspName);
                } else if (!wspName.equals(workspaceName)) {
                    throw new LoginException("Login failed: Invalid workspace name '" + workspaceName + "'.");
                }
            } else if (props.contains(DeltaVConstants.WORKSPACE)) {
                String wspHref = new HrefProperty(props.get(DeltaVConstants.WORKSPACE)).getHrefs().get(0);
                String wspName = Text.unescape(Text.getName(wspHref, true));
                if (!wspName.equals(workspaceName)) {
                    throw new LoginException("Login failed: Invalid workspace name " + workspaceName);
                }
            } else {
                throw new LoginException("Login failed: Unknown workspace '" + workspaceName + "'.");
            }
        } catch (IOException e) {
            throw new RepositoryException(e.getMessage(), e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }

        // make sure the general namespace mappings have been loaded once
        // before additional requests are executed that rely on the namespace
        // mappings.
        if (nsCache.prefixToURI.isEmpty()) {
            try {
                getRegisteredNamespaces(sessionInfo);
            } catch (RepositoryException e) {
                // ignore
            }
        }

        // return the sessionInfo
        return sessionInfo;
    }

    @Override
    public void dispose(SessionInfo sessionInfo) throws RepositoryException {
        checkSessionInfo(sessionInfo);
        removeClient(sessionInfo);
    }

    @Override
    public String[] getWorkspaceNames(SessionInfo sessionInfo) throws RepositoryException {
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(DeltaVConstants.WORKSPACE);
        HttpPropfind request = null;
        try {
            request = new HttpPropfind(uriResolver.getRepositoryUri(), nameSet, DEPTH_1);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);
            MultiStatusResponse[] mresponses = request.getResponseBodyAsMultiStatus(response).getResponses();
            Set<String> wspNames = new HashSet<String>();
            for (MultiStatusResponse mresponse : mresponses) {
                DavPropertySet props = mresponse.getProperties(DavServletResponse.SC_OK);
                if (props.contains(DeltaVConstants.WORKSPACE)) {
                    HrefProperty hp = new HrefProperty(props.get(DeltaVConstants.WORKSPACE));
                    String wspHref = hp.getHrefs().get(0);
                    String name = Text.unescape(Text.getName(wspHref, true));
                    wspNames.add(name);
                }
            }
            return wspNames.toArray(new String[wspNames.size()]);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public boolean isGranted(SessionInfo sessionInfo, ItemId itemId, String[] actions) throws RepositoryException {
        HttpReport request = null;
        try {
            String uri = obtainAbsolutePathFromUri(getItemUri(itemId, sessionInfo));
            ReportInfo reportInfo = new ReportInfo(JcrRemotingConstants.REPORT_PRIVILEGES, ItemResourceConstants.NAMESPACE);
            reportInfo.setContentElement(DomUtil.hrefToXml(uri, DomUtil.createDocument()));

            request = new HttpReport(uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName()), reportInfo);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);

            MultiStatusResponse[] responses = request.getResponseBodyAsMultiStatus(response).getResponses();
            if (responses.length < 1) {
                throw new ItemNotFoundException("Unable to retrieve permissions for item " + saveGetIdString(itemId, sessionInfo));
            }
            DavProperty<?> p = responses[0].getProperties(DavServletResponse.SC_OK).get(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
            if (p == null) {
                return false;
            }
            // build set of privileges from given actions. NOTE: since the actions
            // have no qualifying namespace, the {@link ItemResourceConstants#NAMESPACE}
            // is used.
            Set<Privilege> requiredPrivileges = new HashSet<Privilege>();
            for (String action : actions) {
                requiredPrivileges.add(Privilege.getPrivilege(action, ItemResourceConstants.NAMESPACE));
            }
            // build set of privileges granted to the current user.
            CurrentUserPrivilegeSetProperty privSet = new CurrentUserPrivilegeSetProperty(p);
            Collection<Privilege> privileges = privSet.getValue();

            // check privileges present against required privileges.
            return privileges.containsAll(requiredPrivileges);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (ParserConfigurationException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public PrivilegeDefinition[] getPrivilegeDefinitions(SessionInfo sessionInfo) throws RepositoryException {
        return internalGetPrivilegeDefinitions(sessionInfo, uriResolver.getRepositoryUri());
    }

    @Override
    public PrivilegeDefinition[] getSupportedPrivileges(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        String uri = (nodeId == null) ? uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName()) : getItemUri(nodeId, sessionInfo);
        return internalGetPrivilegeDefinitions(sessionInfo, uri);
    }

    @Override
    public Name[] getPrivilegeNames(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        String uri = (nodeId == null) ? uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName()) : getItemUri(nodeId, sessionInfo);
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);

        HttpPropfind propfindRequest = null;
        try {
            propfindRequest = new HttpPropfind(uri, nameSet, DEPTH_0);
            HttpResponse response = execute(propfindRequest, sessionInfo);
            propfindRequest.checkSuccess(response);

            MultiStatusResponse[] mresponses = propfindRequest.getResponseBodyAsMultiStatus(response).getResponses();
            if (mresponses.length < 1) {
                throw new PathNotFoundException("Unable to retrieve privileges definitions.");
            }

            DavPropertyName displayName = SecurityConstants.CURRENT_USER_PRIVILEGE_SET;
            DavProperty<?> p = mresponses[0].getProperties(DavServletResponse.SC_OK).get(displayName);
            if (p == null) {
                return new Name[0];
            } else {
                Collection<Privilege> privs = new CurrentUserPrivilegeSetProperty(p).getValue();
                Set<Name> privNames = new HashSet<Name>(privs.size());
                for (Privilege priv : privs) {
                    privNames.add(nameFactory.create(priv.getNamespace().getURI(), priv.getName()));
                }
                return privNames.toArray(new Name[privNames.size()]);
            }
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (propfindRequest != null) {
                propfindRequest.releaseConnection();
            }
        }
    }

    private PrivilegeDefinition[] internalGetPrivilegeDefinitions(SessionInfo sessionInfo, String uri) throws RepositoryException {
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(SecurityConstants.SUPPORTED_PRIVILEGE_SET);
        HttpPropfind request = null;
        try {
            request = new HttpPropfind(uri, nameSet, DEPTH_0);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);

            MultiStatusResponse[] mresponses = request.getResponseBodyAsMultiStatus(response).getResponses();
            if (mresponses.length < 1) {
                throw new PathNotFoundException("Unable to retrieve privileges definitions.");
            }

            DavPropertyName displayName = SecurityConstants.SUPPORTED_PRIVILEGE_SET;
            DavProperty<?> p = mresponses[0].getProperties(DavServletResponse.SC_OK).get(displayName);
            if (p == null) {
                return new PrivilegeDefinition[0];
            } else {
                // build PrivilegeDefinition(s) from the supported-privileges dav property
                Map<Name, SupportedPrivilege> spMap = new HashMap<Name, SupportedPrivilege>();
                fillSupportedPrivilegeMap(new SupportedPrivilegeSetProperty(p).getValue(), spMap, getNameFactory());

                List<PrivilegeDefinition> pDefs = new ArrayList<PrivilegeDefinition>();
                for (Name privilegeName : spMap.keySet()) {
                    SupportedPrivilege sp = spMap.get(privilegeName);
                    Set<Name> aggrnames = null;
                    SupportedPrivilege[] aggregates = sp.getSupportedPrivileges();
                    if (aggregates != null && aggregates.length > 0) {
                        aggrnames = new HashSet<Name>();
                        for (SupportedPrivilege aggregate : aggregates) {
                            Name aggregateName = nameFactory.create(aggregate.getPrivilege().getNamespace().getURI(),
                                                                    aggregate.getPrivilege().getName());
                            aggrnames.add(aggregateName);
                        }
                    }
                    PrivilegeDefinition def = new PrivilegeDefinitionImpl(privilegeName, sp.isAbstract(), aggrnames);
                    pDefs.add(def);
                }
                return pDefs.toArray(new PrivilegeDefinition[pDefs.size()]);
            }
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    private static void fillSupportedPrivilegeMap(List<SupportedPrivilege> sps, Map<Name, SupportedPrivilege> spMap, NameFactory nameFactory) throws NamespaceException, IllegalNameException {
        for (SupportedPrivilege sp : sps) {
            Privilege p = sp.getPrivilege();
            Name privName = nameFactory.create(p.getNamespace().getURI(), p.getName());
            spMap.put(privName, sp);
            List<SupportedPrivilege> agg = Arrays.asList(sp.getSupportedPrivileges());
            if (!agg.isEmpty()) {
                fillSupportedPrivilegeMap(agg, spMap, nameFactory);
            }
        }
    }

    @Override
    public QNodeDefinition getNodeDefinition(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        return (QNodeDefinition) getItemDefinition(sessionInfo, nodeId);
    }

    @Override
    public QPropertyDefinition getPropertyDefinition(SessionInfo sessionInfo, PropertyId propertyId) throws RepositoryException {
        return (QPropertyDefinition) getItemDefinition(sessionInfo, propertyId);
    }

    /**
     *
     * @param sessionInfo
     * @param itemId
     * @return
     * @throws RepositoryException
     */
    private QItemDefinition getItemDefinition(SessionInfo sessionInfo, ItemId itemId) throws RepositoryException {
        // set of properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(JcrRemotingConstants.JCR_DEFINITION_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(DavPropertyName.RESOURCETYPE);

        HttpPropfind request = null;
        try {
            String uri = getItemUri(itemId, sessionInfo);
            request = new HttpPropfind(uri, nameSet, DEPTH_0);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);

            MultiStatusResponse[] mresponses = request.getResponseBodyAsMultiStatus(response).getResponses();
            if (mresponses.length < 1) {
                throw new ItemNotFoundException(
                        "Unable to retrieve the item definition for " + saveGetIdString(itemId, sessionInfo));
            }
            if (mresponses.length > 1) {
                throw new RepositoryException(
                        "Internal error: ambigous item definition found '" + saveGetIdString(itemId, sessionInfo) + "'.");
            }
            DavPropertySet propertySet = mresponses[0].getProperties(DavServletResponse.SC_OK);

            // check if definition matches the type of the id
            DavProperty<?> rType = propertySet.get(DavPropertyName.RESOURCETYPE);
            if (rType.getValue() == null && itemId.denotesNode()) {
                throw new RepositoryException("Internal error: requested node definition and got property definition.");
            }

            NamePathResolver resolver = getNamePathResolver(sessionInfo);

            // build the definition
            QItemDefinition definition = null;
            DavProperty<?> prop = propertySet.get(JcrRemotingConstants.JCR_DEFINITION_LN, ItemResourceConstants.NAMESPACE);
            if (prop != null) {
                Object value = prop.getValue();
                if (value != null && value instanceof Element) {
                    Element idfElem = (Element) value;
                    if (itemId.denotesNode()) {
                        definition = DefinitionUtil.createQNodeDefinition(null, idfElem, resolver);
                    } else {
                        definition = DefinitionUtil.createQPropertyDefinition(null, idfElem, resolver, getQValueFactory());
                    }
                }
            }
            if (definition == null) {
                throw new RepositoryException("Unable to retrieve definition for item with id '" + saveGetIdString(itemId, resolver) + "'.");
            }
            return definition;
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public NodeInfo getNodeInfo(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        // set of properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(JcrRemotingConstants.JCR_INDEX_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(JcrRemotingConstants.JCR_PARENT_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(JcrRemotingConstants.JCR_NAME_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(JcrRemotingConstants.JCR_PRIMARYNODETYPE_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(JcrRemotingConstants.JCR_MIXINNODETYPES_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(JcrRemotingConstants.JCR_REFERENCES_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(JcrRemotingConstants.JCR_UUID_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(JcrRemotingConstants.JCR_PATH_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(DavPropertyName.RESOURCETYPE);

        HttpPropfind request = null;
        try {
            String uri = getItemUri(nodeId, sessionInfo);
            request = new HttpPropfind(uri, nameSet, DEPTH_1);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);

            MultiStatusResponse[] mresponses = request.getResponseBodyAsMultiStatus(response).getResponses();
            if (mresponses.length < 1) {
                throw new ItemNotFoundException("Unable to retrieve the node with id " + saveGetIdString(nodeId, sessionInfo));
            }

            MultiStatusResponse nodeResponse = null;
            List<MultiStatusResponse> childResponses = new ArrayList<MultiStatusResponse>();
            for (MultiStatusResponse mresponse : mresponses) {
                if (isSameResource(uri, mresponse)) {
                    nodeResponse = mresponse;
                } else {
                    childResponses.add(mresponse);
                }
            }

            if (nodeResponse == null) {
                throw new ItemNotFoundException("Unable to retrieve the node " + saveGetIdString(nodeId, sessionInfo));
            }

            DavPropertySet propSet = nodeResponse.getProperties(DavServletResponse.SC_OK);
            Object type = propSet.get(DavPropertyName.RESOURCETYPE).getValue();
            if (type == null) {
                // the given id points to a Property instead of a Node
                throw new ItemNotFoundException("No node for id " + saveGetIdString(nodeId, sessionInfo));
            }

            NamePathResolver resolver = getNamePathResolver(sessionInfo);
            NodeId parentId = getParentId(uri, propSet, sessionInfo);

            NodeInfoImpl nInfo = buildNodeInfo(uri, nodeResponse, parentId, propSet, sessionInfo, resolver);

            for (MultiStatusResponse resp : childResponses) {
                DavPropertySet childProps = resp.getProperties(DavServletResponse.SC_OK);
                if (childProps.contains(DavPropertyName.RESOURCETYPE) &&
                        childProps.get(DavPropertyName.RESOURCETYPE).getValue() != null) {
                    // any other resource type than default (empty) is represented by a node item
                    // --> build child info object
                    nInfo.addChildInfo(buildChildInfo(childProps, sessionInfo));
                } else {
                    PropertyId childId = uriResolver.buildPropertyId(nInfo.getId(), resp, sessionInfo.getWorkspaceName(), getNamePathResolver(sessionInfo));
                    nInfo.addPropertyId(childId);
                }
            }
            return nInfo;
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } catch (NameException e) {
            throw new RepositoryException(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public Iterator<? extends ItemInfo> getItemInfos(SessionInfo sessionInfo, ItemId itemId) throws RepositoryException {
        // TODO: implement batch read properly:
        // currently: missing 'value/values' property PropertyInfo cannot be built
        // currently: missing prop-names with child-NodeInfo
        if (itemId.denotesNode()) {
            List<ItemInfo> l = new ArrayList<ItemInfo>();
            NodeInfo nInfo = getNodeInfo(sessionInfo, (NodeId) itemId);
            l.add(nInfo);
            // at least add propertyInfos for the meta-props already known from the
            // nodeInfo.
            l.addAll(buildPropertyInfos(nInfo));
            return l.iterator();
        } else {
            PropertyInfo pInfo = getPropertyInfo(sessionInfo, (PropertyId) itemId);
            return Iterators.singleton(pInfo);
        }
    }

    private NodeInfoImpl buildNodeInfo(String baseUri, MultiStatusResponse nodeResponse,
                                       NodeId parentId, DavPropertySet propSet,
                                       SessionInfo sessionInfo,
                                       NamePathResolver resolver) throws RepositoryException {
        NodeId id = uriResolver.buildNodeId(parentId, baseUri, nodeResponse, sessionInfo.getWorkspaceName(), getNamePathResolver(sessionInfo));
        NodeInfoImpl nInfo = new NodeInfoImpl(id, propSet, resolver);
        DavProperty p = propSet.get(JcrRemotingConstants.JCR_REFERENCES_LN, ItemResourceConstants.NAMESPACE);
        if (p != null) {
            HrefProperty refProp = new HrefProperty(p);
            for (String propertyHref : refProp.getHrefs()) {
                PropertyId propertyId = uriResolver.getPropertyId(propertyHref, sessionInfo);
                nInfo.addReference(propertyId);
            }
        }
        return nInfo;
    }

    private List<PropertyInfo> buildPropertyInfos(NodeInfo nInfo) throws RepositoryException {
        List<PropertyInfo> l = new ArrayList<PropertyInfo>(3);
        NodeId nid = nInfo.getId();
        Path nPath = nInfo.getPath();

        if (nid.getPath() == null) {
            PropertyId id = getIdFactory().createPropertyId(nid, NameConstants.JCR_UUID);
            QValue[] vs = new QValue[] { getQValueFactory().create(nid.getUniqueID(), PropertyType.STRING) };
            Path p = getPathFactory().create(nPath, NameConstants.JCR_UUID, true);
            PropertyInfo pi = new PropertyInfoImpl(id, p, PropertyType.STRING, false, vs);
            l.add(pi);
        }

        Name pName = NameConstants.JCR_PRIMARYTYPE;
        QValue[] vs = new QValue[] { getQValueFactory().create(nInfo.getNodetype()) };
        PropertyInfo pi = new PropertyInfoImpl(getIdFactory().createPropertyId(nid, pName),
                getPathFactory().create(nPath, pName, true), PropertyType.NAME, false, vs);
        l.add(pi);

        Name[] mixins = nInfo.getMixins();
        if (mixins.length > 0) {
            pName = NameConstants.JCR_MIXINTYPES;
            vs = new QValue[mixins.length];
            for (int i = 0; i < mixins.length; i++) {
                vs[i] = getQValueFactory().create(mixins[i]);
            }
            pi = new PropertyInfoImpl(getIdFactory().createPropertyId(nid, pName),
                    getPathFactory().create(nPath, pName, true), PropertyType.NAME,
                    true, vs);
            l.add(pi);
        }

        return l;
    }

    @Override
    public Iterator<ChildInfo> getChildInfos(SessionInfo sessionInfo, NodeId parentId) throws RepositoryException {
        // set of properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(JcrRemotingConstants.JCR_NAME_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(JcrRemotingConstants.JCR_INDEX_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(JcrRemotingConstants.JCR_PARENT_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(JcrRemotingConstants.JCR_UUID_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(DavPropertyName.RESOURCETYPE);

        HttpPropfind request = null;
        try {
            String uri = getItemUri(parentId, sessionInfo);
            request = new HttpPropfind(uri, nameSet, DEPTH_1);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);

            List<ChildInfo> childEntries;
            MultiStatusResponse[] mresponses = request.getResponseBodyAsMultiStatus(response).getResponses();
            if (mresponses.length < 1) {
                throw new ItemNotFoundException("Unable to retrieve the node with id " + saveGetIdString(parentId, sessionInfo));
            } else if (mresponses.length == 1) {
                // no child nodes nor properties
                childEntries = Collections.emptyList();
                return childEntries.iterator();
            }

            childEntries = new ArrayList<ChildInfo>();
            for (MultiStatusResponse mresponse : mresponses) {
                if (!isSameResource(uri, mresponse)) {
                    DavPropertySet childProps = mresponse.getProperties(DavServletResponse.SC_OK);
                    if (childProps.contains(DavPropertyName.RESOURCETYPE) &&
                        childProps.get(DavPropertyName.RESOURCETYPE).getValue() != null) {
                        childEntries.add(buildChildInfo(childProps, sessionInfo));
                    } // else: property -> ignore
                } // else: ignore the response related to the parent
            }
            return childEntries.iterator();
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    private ChildInfo buildChildInfo(DavPropertySet properties, SessionInfo sessionInfo) throws RepositoryException {
        Name qName = getQName(properties, getNamePathResolver(sessionInfo));
        int index = getIndex(properties);
        String uuid = getUniqueID(properties);

        return new ChildInfoImpl(qName, uuid, index);
    }

    @Override
    public Iterator<PropertyId> getReferences(SessionInfo sessionInfo, NodeId nodeId, Name propertyName, boolean weakReferences) throws RepositoryException {
        // set of properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        String refType = weakReferences ? JcrRemotingConstants.JCR_WEAK_REFERENCES_LN : JcrRemotingConstants.JCR_REFERENCES_LN;
        nameSet.add(refType, ItemResourceConstants.NAMESPACE);

        HttpPropfind request = null;
        try {
            String uri = getItemUri(nodeId, sessionInfo);
            request = new HttpPropfind(uri, nameSet, DEPTH_0);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);

            MultiStatusResponse[] mresponses = request.getResponseBodyAsMultiStatus(response).getResponses();
            if (mresponses.length < 1) {
                throw new ItemNotFoundException("Unable to retrieve the node with id " + saveGetIdString(nodeId, sessionInfo));
            }

            List<PropertyId> refIds = Collections.emptyList();
            for (MultiStatusResponse mresponse : mresponses) {
                if (isSameResource(uri, mresponse)) {
                    DavPropertySet props = mresponse.getProperties(DavServletResponse.SC_OK);
                    DavProperty<?> p = props.get(refType, ItemResourceConstants.NAMESPACE);

                    if (p != null) {
                        refIds = new ArrayList<PropertyId>();
                        HrefProperty hp = new HrefProperty(p);
                        for (String propHref : hp.getHrefs()) {
                            PropertyId propId = uriResolver.getPropertyId(resolve(uri, propHref), sessionInfo);
                            if (propertyName == null || propertyName.equals(propId.getName())) {
                                refIds.add(propId);
                            }
                        }
                    }
                }
            }
            return refIds.iterator();
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public PropertyInfo getPropertyInfo(SessionInfo sessionInfo, PropertyId propertyId) throws RepositoryException {
        HttpGet request = null;
        try {
            String uri = getItemUri(propertyId, sessionInfo);
            request = new HttpGet(uri);
            HttpResponse response = executeRequest(sessionInfo, request);

            int status = response.getStatusLine().getStatusCode();
            if (status != DavServletResponse.SC_OK) {
                throw ExceptionConverter.generate(new DavException(status, response.getStatusLine().getReasonPhrase()));
            }

            Path path = uriResolver.getQPath(uri, sessionInfo);

            HttpEntity entity = response.getEntity();
            ContentType ct = ContentType.get(entity);

            boolean isMultiValued;
            QValue[] values;
            int type;

            NamePathResolver resolver = getNamePathResolver(sessionInfo);

            if (ct != null && ct.getMimeType().startsWith("jcr-value")) {
                type = JcrValueType.typeFromContentType(ct.getMimeType());
                QValue v;
                if (type == PropertyType.BINARY) {
                    v = getQValueFactory().create(entity.getContent());
                } else {
                    Reader reader = new InputStreamReader(entity.getContent(), ct.getCharset());
                    StringBuffer sb = new StringBuffer();
                    int c;
                    while ((c = reader.read()) > -1) {
                        sb.append((char) c);
                    }
                    Value jcrValue = valueFactory.createValue(sb.toString(), type);
                    if (jcrValue instanceof QValueValue) {
                        v = ((QValueValue) jcrValue).getQValue();
                    } else {
                        v = ValueFormat.getQValue(jcrValue, resolver, getQValueFactory());
                    }
                }
                values = new QValue[] { v };
                isMultiValued = false;
            } else if (ct != null && ct.getMimeType().equals("text/xml")) {
                // jcr:values property spooled
                values = getValues(entity.getContent(), resolver, propertyId);
                type = (values.length > 0) ? values[0].getType() : loadType(uri, getClient(sessionInfo), propertyId, sessionInfo, resolver);
                isMultiValued = true;
            } else {
                throw new ItemNotFoundException("Unable to retrieve the property with id " + saveGetIdString(propertyId, resolver));
            }

            return new PropertyInfoImpl(propertyId, path, type, isMultiValued, values);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } catch (NameException e) {
            throw new RepositoryException(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    private QValue[] getValues(InputStream response, NamePathResolver resolver, ItemId id) throws RepositoryException {
        try {
            Document doc = DomUtil.parseDocument(response);
            Element prop = DomUtil.getChildElement(doc, JcrRemotingConstants.JCR_VALUES_LN, ItemResourceConstants.NAMESPACE);
            if (prop == null) {
                // no jcr-values present in the response body -> apparently
                // not representation of a jcr-property
                throw new ItemNotFoundException("No property found at " + saveGetIdString(id, resolver));
            } else {
                DavProperty<?> p = DefaultDavProperty.createFromXml(prop);
                Value[] jcrVs = ValueUtil.valuesFromXml(p.getValue(), PropertyType.STRING, valueFactory);
                QValue[] qvs = new QValue[jcrVs.length];
                int type = (jcrVs.length > 0) ? jcrVs[0].getType() : PropertyType.STRING;

                for (int i = 0; i < jcrVs.length; i++) {
                    if (jcrVs[i] instanceof QValueValue) {
                        qvs[i] = ((QValueValue) jcrVs[i]).getQValue();
                    } else if (type == PropertyType.BINARY) {
                        qvs[i] = qValueFactory.create(jcrVs[i].getStream());
                    } else {
                        qvs[i] = ValueFormat.getQValue(jcrVs[i], resolver, qValueFactory);
                    }
                }
                return qvs;
            }
        } catch (SAXException e) {
            log.warn("Internal error: {}", e.getMessage());
            throw new RepositoryException(e);
        } catch (IOException e) {
            log.warn("Internal error: {}", e.getMessage());
            throw new RepositoryException(e);
        } catch (ParserConfigurationException e) {
            log.warn("Internal error: {}", e.getMessage());
            throw new RepositoryException(e);
        }
    }

    private int loadType(String propertyURI, HttpClient client, PropertyId propertyId, SessionInfo sessionInfo, NamePathResolver resolver) throws IOException, DavException, RepositoryException {
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(JcrRemotingConstants.JCR_TYPE_LN, ItemResourceConstants.NAMESPACE);

        HttpPropfind request = null;
        try {
            request = new HttpPropfind(propertyURI, nameSet, DEPTH_0);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);

            MultiStatusResponse[] mresponses = request.getResponseBodyAsMultiStatus(response).getResponses();
            if (mresponses.length == 1) {
                DavPropertySet props = mresponses[0].getProperties(DavServletResponse.SC_OK);
                DavProperty<?> type = props.get(JcrRemotingConstants.JCR_TYPE_LN, ItemResourceConstants.NAMESPACE);
                if (type != null) {
                    return PropertyType.valueFromName(type.getValue().toString());
                } else {
                    throw new RepositoryException("Internal error. Cannot retrieve property type at " + saveGetIdString(propertyId, resolver));
                }
            } else {
                throw new ItemNotFoundException("Internal error. Cannot retrieve property type at " + saveGetIdString(propertyId, resolver));
            }
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public Batch createBatch(SessionInfo sessionInfo, ItemId itemId) throws RepositoryException {
        checkSessionInfo(sessionInfo);
        return new BatchImpl(itemId, sessionInfo);
    }

    @Override
    public void submit(Batch batch) throws RepositoryException {
        if (!(batch instanceof BatchImpl)) {
            throw new RepositoryException("Unknown Batch implementation.");
        }
        BatchImpl batchImpl = (BatchImpl) batch;
        if (batchImpl.isEmpty()) {
            batchImpl.dispose();
            return;
        }

        HttpRequestBase request = null;
        try {
            HttpClient client = batchImpl.start();
            boolean success = false;

            try {
                Iterator<HttpRequestBase> it = batchImpl.requests();
                while (it.hasNext()) {
                    request = it.next();
                    initMethod(request, batchImpl, true);

                    HttpResponse response = client.execute(request);
                    if (request instanceof BaseDavRequest) {
                        ((BaseDavRequest) request).checkSuccess(response);
                    } else {
                        // use generic HTTP status code checking
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode < 200 || statusCode >= 300) {
                            throw new DavException(statusCode, "Unexpected status code " + statusCode + " in response to "
                                    + request.getMethod() + " request.");
                        }
                    }
                    request.releaseConnection();
                }
                success = true;
            } finally {
                // make sure the lock is removed. if any of the methods
                // failed the unlock is used to abort any pending changes
                // on the server.
                batchImpl.end(client, success);
            }
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e, request);
        } finally {
            batchImpl.dispose();
        }
    }

    @Override
    public Tree createTree(SessionInfo sessionInfo, Batch batch, Name nodeName, Name primaryTypeName, String uniqueId) throws RepositoryException {
        return new DocumentTree(nodeName, primaryTypeName, uniqueId, getNamePathResolver(sessionInfo));
    }

    @Override
    public void importXml(SessionInfo sessionInfo, NodeId parentId, InputStream xmlStream, int uuidBehaviour) throws RepositoryException {
        // TODO: improve. currently random name is built instead of retrieving name of new resource from top-level xml element within stream
        Name nodeName = getNameFactory().create(Name.NS_DEFAULT_URI, UUID.randomUUID().toString());
        String uri = getItemUri(parentId, nodeName, sessionInfo);
        HttpMkcol mkcolRequest = new HttpMkcol(uri);
        mkcolRequest.addHeader(JcrRemotingConstants.IMPORT_UUID_BEHAVIOR, Integer.toString(uuidBehaviour));
        mkcolRequest.setEntity(new InputStreamEntity(xmlStream, ContentType.create("text/xml")));
        execute(mkcolRequest, sessionInfo);
    }

    @Override
    public void move(SessionInfo sessionInfo, NodeId srcNodeId, NodeId destParentNodeId, Name destName) throws RepositoryException {
        String uri = getItemUri(srcNodeId, sessionInfo);
        String destUri = getItemUri(destParentNodeId, destName, sessionInfo);
        if (isDavClass3(sessionInfo)) {
            destUri = obtainAbsolutePathFromUri(destUri);
        }
        HttpMove request = new HttpMove(uri, destUri, false);
        try {
            initMethod(request, sessionInfo);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);
            // need to clear the cache as the move may have affected nodes with
            // uuid.
            clearItemUriCache(sessionInfo);
        } catch (IOException ex) {
            throw new RepositoryException(ex);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e, request);
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public void copy(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, Name destName) throws RepositoryException {
        String uri = uriResolver.getItemUri(srcNodeId, srcWorkspaceName, sessionInfo);
        String destUri = getItemUri(destParentNodeId, destName, sessionInfo);
        if (isDavClass3(sessionInfo)) {
            destUri = obtainAbsolutePathFromUri(destUri);
        }
        HttpCopy request = new HttpCopy(uri, destUri, false, false);
        try {
            initMethod(request, sessionInfo);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);
        } catch (IOException ex) {
            throw new RepositoryException(ex);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e, request);
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public void update(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName) throws RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        String workspUri = uriResolver.getWorkspaceUri(srcWorkspaceName);

        update(uri, null, new String[] { workspUri }, UpdateInfo.UPDATE_BY_WORKSPACE, false, sessionInfo);
    }

    @Override
    public void clone(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, Name destName, boolean removeExisting) throws RepositoryException {
        // TODO: missing implementation
        throw new UnsupportedOperationException("Missing implementation");
    }

    @Override
    public LockInfo getLockInfo(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        // set of Dav-properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(DavPropertyName.LOCKDISCOVERY);
        nameSet.add(JcrRemotingConstants.JCR_PARENT_LN, ItemResourceConstants.NAMESPACE);

        HttpPropfind request = null;
        try {
            String uri = getItemUri(nodeId, sessionInfo);
            request = new HttpPropfind(uri, nameSet, DEPTH_0);
            initMethod(request, sessionInfo, false);

            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);

            MultiStatusResponse[] mresponses = request.getResponseBodyAsMultiStatus(response).getResponses();
            if (mresponses.length != 1) {
                throw new ItemNotFoundException(
                        "Unable to retrieve the LockInfo. No such node " + saveGetIdString(nodeId, sessionInfo));
            }

            DavPropertySet ps = mresponses[0].getProperties(DavServletResponse.SC_OK);
            if (ps.contains(DavPropertyName.LOCKDISCOVERY)) {
                DavProperty<?> p = ps.get(DavPropertyName.LOCKDISCOVERY);
                LockDiscovery ld = LockDiscovery.createFromXml(p.toXml(DomUtil.createDocument()));
                NodeId parentId = getParentId(uri, ps, sessionInfo);
                return retrieveLockInfo(ld, sessionInfo, nodeId, parentId);
            } else {
                // no lock present
                log.debug("No Lock present on node with id " + saveGetIdString(nodeId, sessionInfo));
                return null;
            }
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (ParserConfigurationException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public LockInfo lock(SessionInfo sessionInfo, NodeId nodeId, boolean deep,
                         boolean sessionScoped) throws RepositoryException {
        return lock(sessionInfo, nodeId, deep, sessionScoped, Long.MAX_VALUE, null);
    }

    @Override
    public LockInfo lock(SessionInfo sessionInfo, NodeId nodeId, boolean deep, boolean sessionScoped, long timeoutHint, String ownerHint) throws RepositoryException {
        HttpLock request = null;
        try {
            checkSessionInfo(sessionInfo);
            long davTimeout = (timeoutHint == Long.MAX_VALUE) ? INFINITE_TIMEOUT : timeoutHint * 1000;
            String ownerInfo = (ownerHint == null) ? sessionInfo.getUserID() : ownerHint;

            String uri = getItemUri(nodeId, sessionInfo);
            Scope scope = (sessionScoped) ? ItemResourceConstants.EXCLUSIVE_SESSION : Scope.EXCLUSIVE;
            request  = new HttpLock(uri,
                    new org.apache.jackrabbit.webdav.lock.LockInfo(scope, Type.WRITE, ownerInfo, davTimeout, deep));
            HttpResponse response = execute(request, sessionInfo);

            String lockToken = request.getLockToken(response);
            ((SessionInfoImpl) sessionInfo).addLockToken(lockToken, sessionScoped);

            LockDiscovery disc = request.getResponseBodyAsLockDiscovery(response);
            return retrieveLockInfo(disc, sessionInfo, nodeId, null);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public void refreshLock(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        checkSessionInfo(sessionInfo);
        String uri = getItemUri(nodeId, sessionInfo);
        // since sessionInfo does not allow to retrieve token by NodeId,
        // pass all available lock tokens to the LOCK method (TODO: correct?)
        Set<String> allLockTokens = ((SessionInfoImpl) sessionInfo).getAllLockTokens();
        String[] locktokens = allLockTokens.toArray(new String[allLockTokens.size()]);
        HttpLock httpLock = null;
        try {
            httpLock = new HttpLock(uri, INFINITE_TIMEOUT, locktokens);
            execute(httpLock, sessionInfo);
        } finally {
            if (httpLock != null) {
                httpLock.releaseConnection();
            }
        }
    }

    @Override
    public void unlock(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        checkSessionInfo(sessionInfo);
        String uri = getItemUri(nodeId, sessionInfo);
        // Note: since sessionInfo does not allow to identify the id of the
        // lock holding node, we need to access the token via lockInfo
        // TODO: review this.
        LockInfoImpl lInfo = (LockInfoImpl) getLockInfo(sessionInfo, nodeId);
        if (lInfo == null) {
            throw new LockException("No Lock present on Node with id " + saveGetIdString(nodeId, sessionInfo));
        }

        String lockToken = lInfo.getActiveLock().getToken();
        boolean isSessionScoped = lInfo.isSessionScoped();

        if (!((SessionInfoImpl) sessionInfo).getAllLockTokens().contains(lockToken)) {
            throw new LockException("Lock " + lockToken + " not owned by this session");
        }

        HttpUnlock unlockRequest = new HttpUnlock(uri, lockToken);
        try {
            execute(unlockRequest, sessionInfo);
            ((SessionInfoImpl) sessionInfo).removeLockToken(lockToken, isSessionScoped);
        } finally {
            unlockRequest.releaseConnection();
        }
    }

    private LockInfo retrieveLockInfo(LockDiscovery lockDiscovery, SessionInfo sessionInfo,
                                      NodeId nodeId, NodeId parentId) throws RepositoryException {
        checkSessionInfo(sessionInfo);
        List<ActiveLock> activeLocks = lockDiscovery.getValue();
        ActiveLock activeLock = null;
        for (ActiveLock l : activeLocks) {
            Scope sc = l.getScope();
            if (l.getType() == Type.WRITE && (Scope.EXCLUSIVE.equals(sc) || sc == ItemResourceConstants.EXCLUSIVE_SESSION)) {
                if (activeLock != null) {
                    throw new RepositoryException("Node " + saveGetIdString(nodeId, sessionInfo) + " contains multiple exclusive write locks.");
                } else {
                    activeLock = l;
                }
            }
        }
        if (activeLock == null) {
            log.debug("No lock present on node " + saveGetIdString(nodeId, sessionInfo));
            return null;
        }

        NodeId holder = null;
        String lockroot = activeLock.getLockroot();
        if (activeLock.getLockroot() != null) {
            holder = uriResolver.getNodeId(lockroot, sessionInfo);
        }

        if (activeLock.isDeep() && holder == null && parentId != null) {
            // deep lock, parent known, but holder is not
            LockInfo pLockInfo = getLockInfo(sessionInfo, parentId);
            if (pLockInfo != null) {
                return pLockInfo;
            }
        }
        return new LockInfoImpl(activeLock, holder == null ? nodeId : holder, ((SessionInfoImpl) sessionInfo).getAllLockTokens());
    }

    @Override
    public NodeId checkin(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        HttpCheckin request = new HttpCheckin(uri);
        try {
            initMethod(request, sessionInfo, !isUnLockMethod(request));
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);
            org.apache.http.Header rh = response.getFirstHeader(DeltaVConstants.HEADER_LOCATION);
            return uriResolver.getNodeId(resolve(uri, rh.getValue()), sessionInfo);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException ex) {
            throw ExceptionConverter.generate(ex);
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public void checkout(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        HttpCheckout request = new HttpCheckout(uri);
        try {
            initMethod(request, sessionInfo, !isUnLockMethod(request));
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException ex) {
            throw ExceptionConverter.generate(ex);
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public void checkout(SessionInfo sessionInfo, NodeId nodeId, NodeId activityId) throws RepositoryException {
        if (activityId == null) {
            checkout(sessionInfo, nodeId);
        } else {
            // TODO
            throw new UnsupportedOperationException("JCR-2104: JSR 283 Versioning. Implementation missing");
        }
    }

    @Override
    public NodeId checkpoint(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        // TODO review again.
        NodeId vID = checkin(sessionInfo, nodeId);
        checkout(sessionInfo, nodeId);
        return vID;
    }

    @Override
    public NodeId checkpoint(SessionInfo sessionInfo, NodeId nodeId, NodeId activityId) throws RepositoryException {
        if (activityId == null) {
            return checkpoint(sessionInfo, nodeId);
        } else {
            // TODO
            throw new UnsupportedOperationException("JCR-2104: JSR 283 Versioning. Implementation missing");
        }
    }

    @Override
    public void removeVersion(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId) throws RepositoryException {
        String uri = getItemUri(versionId, sessionInfo);
        HttpDelete request = new HttpDelete(uri);
        try {
            initMethod(request, sessionInfo, !isUnLockMethod(request));
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);
        } catch (IOException ex) {
            throw new RepositoryException(ex);
        } catch (DavException ex) {
            throw ExceptionConverter.generate(ex);
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public void restore(SessionInfo sessionInfo, NodeId nodeId, NodeId versionId, boolean removeExisting) throws RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        String vUri = getItemUri(versionId, sessionInfo);

        Path relPath = null;
        if (!exists(sessionInfo, uri)) {
            // restore with rel-Path part
            Path path = nodeId.getPath();
            if (nodeId.getUniqueID() != null) {
                uri = getItemUri(idFactory.createNodeId(nodeId.getUniqueID(), null), sessionInfo);
                relPath = (path.isAbsolute()) ? getPathFactory().getRootPath().computeRelativePath(path) : path;
            } else {
                int degree = 0;
                while (degree < path.getLength()) {
                    Path ancestorPath = path.getAncestor(degree);
                    NodeId parentId = idFactory.createNodeId(nodeId.getUniqueID(), ancestorPath);
                    if (exists(sessionInfo, getItemUri(parentId, sessionInfo))) {
                        uri = getItemUri(parentId, sessionInfo);
                        relPath = ancestorPath.computeRelativePath(path);
                        break;
                    }
                    degree++;
                }
            }
        }

        update(uri, relPath, new String[] { vUri }, UpdateInfo.UPDATE_BY_VERSION, removeExisting, sessionInfo);
    }

    private boolean exists(SessionInfo sInfo, String uri) {
        HttpHead request = new HttpHead(uri);
        try {
            int statusCode = executeRequest(sInfo, request).getStatusLine().getStatusCode();
            return (statusCode == DavServletResponse.SC_OK);
        } catch (IOException e) {
            log.error("Unexpected error while testing existence of item.", e);
            return false;
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            return false;
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public void restore(SessionInfo sessionInfo, NodeId[] versionIds, boolean removeExisting) throws RepositoryException {
        String uri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());
        String[] vUris = new String[versionIds.length];
        for (int i = 0; i < versionIds.length; i++) {
            vUris[i] = getItemUri(versionIds[i], sessionInfo);
        }

        update(uri, null, vUris, UpdateInfo.UPDATE_BY_VERSION, removeExisting, sessionInfo);
    }

    private void update(String uri, Path relPath, String[] updateSource, int updateType, boolean removeExisting, SessionInfo sessionInfo) throws RepositoryException {
        HttpUpdate request = null;
        try {
            UpdateInfo uInfo;
            String tmpUpdateSource[] = obtainAbsolutePathsFromUris(updateSource);
            if (removeExisting || relPath != null) {
                Element uElem = UpdateInfo.createUpdateElement(tmpUpdateSource, updateType, DomUtil.createDocument());
                if (removeExisting) {
                    DomUtil.addChildElement(uElem, JcrRemotingConstants.XML_REMOVEEXISTING, ItemResourceConstants.NAMESPACE);
                }
                if (relPath != null) {
                    DomUtil.addChildElement(uElem, JcrRemotingConstants.XML_RELPATH, ItemResourceConstants.NAMESPACE, getNamePathResolver(sessionInfo).getJCRPath(relPath));
                }

                uInfo = new UpdateInfo(uElem);
            } else {
                uInfo = new UpdateInfo(tmpUpdateSource, updateType, new DavPropertyNameSet());
            }

            request = new HttpUpdate(uri, uInfo);
            initMethod(request, sessionInfo, !isUnLockMethod(request));
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (ParserConfigurationException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public Iterator<NodeId> merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName, boolean bestEffort) throws RepositoryException {
        return merge(sessionInfo, nodeId, srcWorkspaceName, bestEffort, false);
    }

    @Override
    public Iterator<NodeId> merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName, boolean bestEffort, boolean isShallow) throws RepositoryException {
        HttpMerge request = null;
        try {
            Document doc = DomUtil.createDocument();
            String wspHref = obtainAbsolutePathFromUri(uriResolver.getWorkspaceUri(srcWorkspaceName));
            Element mElem = MergeInfo.createMergeElement(new String[] { wspHref }, !bestEffort, false, doc);
            if (isShallow) {
                mElem.appendChild(DomUtil.depthToXml(false, doc));
            }
            MergeInfo mInfo = new MergeInfo(mElem);

            String uri = getItemUri(nodeId, sessionInfo);
            request = new HttpMerge(uri, mInfo);
            initMethod(request, sessionInfo, !isUnLockMethod(request));
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);

            MultiStatusResponse[] resps = request.getResponseBodyAsMultiStatus(response).getResponses();
            List<NodeId> failedIds = new ArrayList<NodeId>(resps.length);
            for (MultiStatusResponse resp : resps) {
                String href = resolve(uri, resp.getHref());
                failedIds.add(uriResolver.getNodeId(href, sessionInfo));
            }
            return failedIds.iterator();
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (ParserConfigurationException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public void resolveMergeConflict(SessionInfo sessionInfo, NodeId nodeId, NodeId[] mergeFailedIds, NodeId[] predecessorIds) throws RepositoryException {
        HttpProppatch request = null;
        try {
            List<HrefProperty> changeList = new ArrayList<HrefProperty>();
            String[] mergeFailedHref = new String[mergeFailedIds.length];
            for (int i = 0; i < mergeFailedIds.length; i++) {
                mergeFailedHref[i] = getItemUri(mergeFailedIds[i], sessionInfo);
            }
            changeList.add(new HrefProperty(VersionControlledResource.AUTO_MERGE_SET, mergeFailedHref, false));

            if (predecessorIds != null && predecessorIds.length > 0) {
                String[] pdcHrefs = new String[predecessorIds.length];
                for (int i = 0; i < predecessorIds.length; i++) {
                    pdcHrefs[i] = getItemUri(predecessorIds[i], sessionInfo);
                }
                changeList.add(new HrefProperty(VersionControlledResource.PREDECESSOR_SET, pdcHrefs, false));
            }

            request = new HttpProppatch(getItemUri(nodeId, sessionInfo), changeList);
            initMethod(request, sessionInfo, !isUnLockMethod(request));
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public void addVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, Name label, boolean moveLabel) throws RepositoryException {
        HttpLabel request = null;
        try {
            String uri = getItemUri(versionId, sessionInfo);
            String strLabel = getNamePathResolver(sessionInfo).getJCRName(label);
            request = new HttpLabel(uri, new LabelInfo(strLabel, moveLabel ? LabelInfo.TYPE_SET : LabelInfo.TYPE_ADD));
            initMethod(request, sessionInfo, !isUnLockMethod(request));
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException ex) {
            throw ExceptionConverter.generate(ex);
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public void removeVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, Name label) throws RepositoryException {
        HttpLabel request = null;
        try {
            String uri = getItemUri(versionId, sessionInfo);
            String strLabel = getNamePathResolver(sessionInfo).getJCRName(label);
            request = new HttpLabel(uri, new LabelInfo(strLabel, LabelInfo.TYPE_REMOVE));
            initMethod(request, sessionInfo, !isUnLockMethod(request));
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException ex) {
            throw ExceptionConverter.generate(ex);
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public NodeId createActivity(SessionInfo sessionInfo, String title) throws RepositoryException {
        // TODO
        throw new UnsupportedOperationException("JCR-2104: JSR 283 Versioning. Implementation missing");
    }

    @Override
    public void removeActivity(SessionInfo sessionInfo, NodeId activityId) throws RepositoryException {
        // TODO
        throw new UnsupportedOperationException("JCR-2104: JSR 283 Versioning. Implementation missing");
    }

    @Override
    public Iterator<NodeId> mergeActivity(SessionInfo sessionInfo, NodeId activityId) throws RepositoryException {
        // TODO
        throw new UnsupportedOperationException("JCR-2104: JSR 283 Versioning. Implementation missing");
    }

    @Override
    public NodeId createConfiguration(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        // TODO
        throw new UnsupportedOperationException("JCR-2104: JSR 283 Versioning. Implementation missing");
    }

    @Override
    public String[] getSupportedQueryLanguages(SessionInfo sessionInfo) throws RepositoryException {
        HttpOptions request = new HttpOptions(uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName()));
        try {
            HttpResponse response = executeRequest(sessionInfo, request);
            int status = response.getStatusLine().getStatusCode();
            if (status != DavServletResponse.SC_OK) {
                throw new DavException(status);
            }
            return request.getSearchGrammars(response).toArray(new String[0]);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public String[] checkQueryStatement(SessionInfo sessionInfo,
                                    String statement,
                                    String language,
                                    Map<String, String> namespaces)
            throws RepositoryException {
        // TODO implement
        return new String[0];
    }

    @Override
    public QueryInfo executeQuery(SessionInfo sessionInfo, String statement, String language, Map<String, String> namespaces, long limit, long offset, Map<String, QValue> values) throws RepositoryException {
        HttpSearch request = null;
        try {
            String uri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());
            SearchInfo sInfo = new SearchInfo(
                    language, ItemResourceConstants.NAMESPACE,
                    statement, namespaces);

            if (limit != -1) {
                sInfo.setNumberResults(limit);
            }
            if (offset != -1) {
                sInfo.setOffset(offset);
            }

            if (!(values == null || values.isEmpty())) {
                throw new UnsupportedOperationException("Implementation missing:  JCR-2107");
            }

            request = new HttpSearch(uri, sInfo);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);

            MultiStatus ms = request.getResponseBodyAsMultiStatus(response);
            NamePathResolver resolver = getNamePathResolver(sessionInfo);
            return new QueryInfoImpl(ms, idFactory, resolver, valueFactory, getQValueFactory());
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public EventFilter createEventFilter(SessionInfo sessionInfo,
                                         int eventTypes,
                                         Path absPath,
                                         boolean isDeep,
                                         String[] uuids,
                                         Name[] nodeTypeNames,
                                         boolean noLocal)
            throws RepositoryException {
        // resolve node type names
        // todo what if new node types become available while event filter is still in use?
        Set<Name> resolvedTypeNames = null;
        if (nodeTypeNames != null) {
            resolvedTypeNames = new HashSet<Name>();
            // make sure node type definitions are available
            if (nodeTypeDefinitions.size() == 0) {
                getQNodeTypeDefinitions(sessionInfo);
            }
            synchronized (nodeTypeDefinitions) {
                for (Name nodeTypeName : nodeTypeNames) {
                    resolveNodeType(resolvedTypeNames, nodeTypeName);
                }
            }
        }
        return new EventFilterImpl(eventTypes, absPath, isDeep, uuids,
                resolvedTypeNames, noLocal);
    }

    @Override
    public EventBundle[] getEvents(Subscription subscription, long timeout)
            throws RepositoryException {
        checkSubscription(subscription);

        EventSubscriptionImpl subscr = (EventSubscriptionImpl) subscription;
        String rootUri = uriResolver.getRootItemUri(subscr.getSessionInfo().getWorkspaceName());

        return poll(rootUri, subscr.getId(), timeout, subscr.getSessionInfo());
    }

    @Override
    public EventBundle getEvents(SessionInfo sessionInfo, EventFilter filter, long after) throws RepositoryException {
        // TODO: use filters remotely (JCR-3179)

        HttpGet request = null;
        String rootUri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());
        rootUri += "?type=journal"; // TODO should have a way to discover URI template

        try {
            request = new HttpGet(rootUri);
            request.addHeader("If-None-Match", "\"" + Long.toHexString(after) + "\""); // TODO
            initMethod(request, sessionInfo);

            HttpResponse response = executeRequest(sessionInfo, request);
            int status = response.getStatusLine().getStatusCode();
            if (status != 200) {
                throw new RepositoryException("getEvents to " + rootUri + " failed with " + response.getStatusLine());
            }

            HttpEntity entity = response.getEntity();
            InputStream in = entity.getContent();
            Document doc = null;
            if (in != null) {
                // read response and try to build a xml document
                try {
                    doc = DomUtil.parseDocument(in);
                } catch (ParserConfigurationException e) {
                    throw new IOException("XML parser configuration error", e);
                } catch (SAXException e) {
                    throw new IOException("XML parsing error", e);
                } finally {
                    in.close();
                }
            }

            List<Event> events = new ArrayList<Event>();

            ElementIterator entries = DomUtil.getChildren(doc.getDocumentElement(), AtomFeedConstants.N_ENTRY);
            while (entries.hasNext()) {
                Element entryElem = entries.next();

                Element contentElem = DomUtil.getChildElement(entryElem, AtomFeedConstants.N_CONTENT);
                if (contentElem != null
                        && "application/vnd.apache.jackrabbit.event+xml".equals(contentElem.getAttribute("type"))) {
                    List<Event> el = buildEventList(contentElem, (SessionInfoImpl) sessionInfo, rootUri);
                    for (Event e : el) {
                        if (e.getDate() > after && (filter == null || filter.accept(e, false))) {
                            events.add(e);
                        }
                    }
                }
            }

            return new EventBundleImpl(events, false);
        } catch (Exception ex) {
            log.error("extracting events from journal feed", ex);
            throw new RepositoryException("extracting events from journal feed: " + ex.getMessage(), ex);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public Subscription createSubscription(SessionInfo sessionInfo,
                                           EventFilter[] filters)
            throws RepositoryException {

        checkSessionInfo(sessionInfo);
        String rootUri = uriResolver.getRootItemUri(sessionInfo.getWorkspaceName());
        String subscriptionId = subscribe(rootUri, S_INFO, null, sessionInfo, null);
        log.debug("Subscribed on server for session info " + sessionInfo);

        try {
            checkEventFilterSupport(filters);
        } catch (UnsupportedRepositoryOperationException ex) {
            unsubscribe(rootUri, subscriptionId, sessionInfo);
            throw (ex);
        }
        return new EventSubscriptionImpl(subscriptionId, (SessionInfoImpl) sessionInfo);
    }

    @Override
    public void updateEventFilters(Subscription subscription,
                                   EventFilter[] filters)
            throws RepositoryException {
        // do nothing ...
        // this is actually not correct because we listen for everything and
        // rely on the client of the repository service to filter the events
        checkEventFilterSupport(filters);
    }

    private void checkEventFilterSupport(EventFilter[] filters)
            throws UnsupportedRepositoryOperationException {
        for (EventFilter ef : filters) {
            if (ef instanceof EventFilterImpl) {
                EventFilterImpl efi = (EventFilterImpl) ef;
                if (efi.getNodeTypeNames() != null
                        && !remoteServerProvidesNodeTypes) {
                    throw new UnsupportedRepositoryOperationException(
                            "Remote server does not provide node type information in events");
                }
                if (efi.getNoLocal() && !remoteServerProvidesNoLocalFlag) {
                    throw new UnsupportedRepositoryOperationException(
                            "Remote server does not provide local flag in events");
                }
            }
        }
    }

    @Override
    public void dispose(Subscription subscription) throws RepositoryException {
        checkSubscription(subscription);
        EventSubscriptionImpl subscr = (EventSubscriptionImpl) subscription;
        String rootUri = uriResolver.getRootItemUri(
                subscr.getSessionInfo().getWorkspaceName());
        unsubscribe(rootUri, subscr.getId(), subscr.getSessionInfo());
    }

    private String subscribe(String uri, SubscriptionInfo subscriptionInfo,
                             String subscriptionId, SessionInfo sessionInfo,
                             String batchId) throws RepositoryException {
        HttpSubscribe request = null;
        try {
            request = new HttpSubscribe(uri, subscriptionInfo, subscriptionId);
            initMethod(request, sessionInfo);

            if (batchId != null) {
                // add batchId as separate header
                CodedUrlHeader ch = new CodedUrlHeader(TransactionConstants.HEADER_TRANSACTIONID, batchId);
                request.setHeader(ch.getHeaderName(), ch.getHeaderValue());
            }

            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);

            org.apache.jackrabbit.webdav.observation.Subscription[] subs = request.getResponseBodyAsSubscriptionDiscovery(response)
                    .getValue();
            if (subs.length == 1) {
                this.remoteServerProvidesNodeTypes = subs[0].eventsProvideNodeTypeInformation();
                this.remoteServerProvidesNoLocalFlag = subs[0].eventsProvideNoLocalFlag();
            }

            return request.getSubscriptionId(response);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    private void unsubscribe(String uri, String subscriptionId, SessionInfo sessionInfo) throws RepositoryException {
        HttpUnsubscribe request = null;
        try {
            request = new HttpUnsubscribe(uri, subscriptionId);
            initMethod(request, sessionInfo);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    private void resolveNodeType(Set<Name> resolved, Name ntName) {
        if (!resolved.add(ntName)) {
            return;
        }
        QNodeTypeDefinition def = nodeTypeDefinitions.get(ntName);
        if (def != null) {
            for (Name supertype : def.getSupertypes()) {
                resolveNodeType(resolved, supertype);
            }
        }
    }

    private EventBundle[] poll(String uri, String subscriptionId, long timeout, SessionInfoImpl sessionInfo) throws RepositoryException {
        HttpPoll request = null;
        try {
            request = new HttpPoll(uri, subscriptionId, timeout);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);

            EventDiscovery disc = request.getResponseBodyAsEventDiscovery(response);
            EventBundle[] events;
            if (disc.isEmpty()) {
                events = new EventBundle[0];
            } else {
                Element discEl = disc.toXml(DomUtil.createDocument());
                ElementIterator it = DomUtil.getChildren(discEl,
                        ObservationConstants.N_EVENTBUNDLE);
                List<EventBundle> bundles = new ArrayList<EventBundle>();
                while (it.hasNext()) {
                    Element bundleElement = it.nextElement();
                    String value = DomUtil.getAttribute(bundleElement,
                            ObservationConstants.XML_EVENT_LOCAL, null);
                    // check if it matches a batch id recently submitted
                    boolean isLocal = false;
                    if (value != null) {
                        isLocal = Boolean.parseBoolean(value);
                    }
                    bundles.add(new EventBundleImpl(
                            buildEventList(bundleElement, sessionInfo, uri),
                            isLocal));
                }
                events = bundles.toArray(new EventBundle[bundles.size()]);
            }
            return events;
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (ParserConfigurationException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    private List<Event> buildEventList(Element bundleElement, SessionInfoImpl sessionInfo, String baseUri)
            throws RepositoryException {
        List<Event> events = new ArrayList<Event>();
        ElementIterator eventElementIterator = DomUtil.getChildren(bundleElement, ObservationConstants.N_EVENT);

        String userId = null;

        // get user id from enclosing Atom entry element in case this was a feed
        if (DomUtil.matches(bundleElement, AtomFeedConstants.N_ENTRY)) {
            Element authorEl = DomUtil.getChildElement(bundleElement, AtomFeedConstants.N_AUTHOR);
            Element nameEl = authorEl != null ? DomUtil.getChildElement(authorEl, AtomFeedConstants.N_NAME) : null;
            if (nameEl != null) {
                userId = DomUtil.getTextTrim(nameEl);
            }
        }

        while (eventElementIterator.hasNext()) {
            Element evElem = eventElementIterator.nextElement();
            Element typeEl = DomUtil.getChildElement(evElem, ObservationConstants.N_EVENTTYPE);
            EventType[] et = DefaultEventType.createFromXml(typeEl);
            if (et.length == 0 || et.length > 1) {
                // should not occur.
                log.error("Ambiguous event type definition: expected one single event type.");
                continue;
            }

            String href = DomUtil.getChildTextTrim(evElem, XML_HREF, NAMESPACE);

            int type = EventUtil.getJcrEventType(et[0].getName());
            Path eventPath = null;
            ItemId eventId = null;
            NodeId parentId = null;

            if (href != null) {
                href = resolve(baseUri, href);
                try {
                    eventPath = uriResolver.getQPath(href, sessionInfo);
                } catch (RepositoryException e) {
                    // should not occur
                    log.error("Internal error while building Event: ()", e.getMessage());
                    continue;
                }

                boolean isForNode = (type == Event.NODE_ADDED
                        || type == Event.NODE_REMOVED || type == Event.NODE_MOVED);

                try {
                    if (isForNode) {
                        eventId = uriResolver.getNodeIdAfterEvent(href,
                                sessionInfo, type == Event.NODE_REMOVED);
                    } else {
                        eventId = uriResolver.getPropertyId(href, sessionInfo);
                    }
                } catch (RepositoryException e) {
                    if (isForNode) {
                        eventId = idFactory.createNodeId((String) null, eventPath);
                    } else {
                        try {
                            eventId = idFactory.createPropertyId(
                                    idFactory.createNodeId((String) null,
                                            eventPath.getAncestor(1)),
                                    eventPath.getName());
                        } catch (RepositoryException e1) {
                            log.warn("Unable to build event itemId: {}",
                                    e.getMessage());
                        }
                    }
                }

                String parentHref = Text.getRelativeParent(href, 1, true);
                try {
                    parentId = uriResolver.getNodeId(parentHref, sessionInfo);
                } catch (RepositoryException e) {
                    log.warn("Unable to build event parentId: {}", e.getMessage());
                }
            }

            if (userId == null) {
                // user id not retrieved from container
                userId = DomUtil.getChildTextTrim(evElem, ObservationConstants.N_EVENTUSERID);
            }

            events.add(new EventImpl(eventId, eventPath, parentId, type, userId, evElem,
                    getNamePathResolver(sessionInfo), getQValueFactory()));
        }

        return events;
    }

    @Override
    public Map<String, String> getRegisteredNamespaces(SessionInfo sessionInfo) throws RepositoryException {
        ReportInfo info = new ReportInfo(JcrRemotingConstants.REPORT_REGISTERED_NAMESPACES, ItemResourceConstants.NAMESPACE);
        HttpReport request = null;
        try {
            request = new HttpReport(uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName()), info);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);

            Document doc = request.getResponseBodyAsDocument(response.getEntity());
            Map<String, String> namespaces = new HashMap<String, String>();
            if (doc != null) {
                Element rootElement = doc.getDocumentElement();
                ElementIterator nsElems = DomUtil.getChildren(rootElement, JcrRemotingConstants.XML_NAMESPACE, ItemResourceConstants.NAMESPACE);
                while (nsElems.hasNext()) {
                    Element elem = nsElems.nextElement();
                    String prefix = DomUtil.getChildText(elem, JcrRemotingConstants.XML_PREFIX, ItemResourceConstants.NAMESPACE);
                    String uri = DomUtil.getChildText(elem, JcrRemotingConstants.XML_URI, ItemResourceConstants.NAMESPACE);
                    // default namespace
                    if (prefix == null && uri == null) {
                        prefix = uri = "";
                    }
                    // any other uri must not be null
                    if (uri != null) {
                        namespaces.put(prefix, uri);
                        // TODO: not correct since nsRegistry is retrieved from each session
                        nsCache.add(prefix, uri);
                    } else {
                        log.error("Invalid prefix / uri pair: " + prefix + " -> " + uri);
                    }
                }
            }
            return namespaces;
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public String getNamespaceURI(SessionInfo sessionInfo, String prefix)
            throws RepositoryException {
        try {
            return nsCache.getURI(prefix);
        } catch (NamespaceException e) {
            // refresh namespaces and try again
            getRegisteredNamespaces(sessionInfo);
            return nsCache.getURI(prefix);
        }
    }

    @Override
    public String getNamespacePrefix(SessionInfo sessionInfo, String uri)
            throws RepositoryException {
        try {
            return nsCache.getPrefix(uri);
        } catch (NamespaceException e) {
            // refresh namespaces and try again
            getRegisteredNamespaces(sessionInfo);
            return nsCache.getPrefix(uri);
        }
    }

    @Override
    public void registerNamespace(SessionInfo sessionInfo, String prefix, String uri) throws RepositoryException {
        // make sure we have them all
        getRegisteredNamespaces(sessionInfo);

        Map<String, String> namespaces = new HashMap<String, String>(nsCache.getNamespaces());
        // add new pair that needs to be registered.
        namespaces.put(prefix, uri);

        internalSetNamespaces(sessionInfo, namespaces);
        // adjust internal mappings:
        // TODO: not correct since nsRegistry is retrieved from each session
        nsCache.add(prefix, uri);
    }

    @Override
    public void unregisterNamespace(SessionInfo sessionInfo, String uri) throws RepositoryException {
        // make sure we have them all
        getRegisteredNamespaces(sessionInfo);

        String prefix = nsCache.getPrefix(uri);
        Map<String, String> namespaces = new HashMap<String, String>(nsCache.getNamespaces());
        // remove pair that needs to be unregistered
        namespaces.remove(prefix);

        internalSetNamespaces(sessionInfo, namespaces);
        // adjust internal mappings:
        nsCache.remove(prefix, uri);
    }

    /**
     *
     * @param sessionInfo
     * @param namespaces
     * @throws NamespaceException
     * @throws UnsupportedRepositoryOperationException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    private void internalSetNamespaces(SessionInfo sessionInfo, Map<String, String> namespaces) throws RepositoryException {
        DavPropertySet setProperties = new DavPropertySet();
        setProperties.add(createNamespaceProperty(namespaces));

        HttpProppatch request = null;
        try {
            String uri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());

            request = new HttpProppatch(uri, setProperties, new DavPropertyNameSet());
            initMethod(request, sessionInfo, true);

            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public Iterator<QNodeTypeDefinition> getQNodeTypeDefinitions(SessionInfo sessionInfo) throws RepositoryException {
        HttpReport request = null;
        try {
            ReportInfo info = new ReportInfo(JcrRemotingConstants.REPORT_NODETYPES, ItemResourceConstants.NAMESPACE);
            info.setContentElement(DomUtil.createElement(DomUtil.createDocument(), NodeTypeConstants.XML_REPORT_ALLNODETYPES, ItemResourceConstants.NAMESPACE));

            String workspaceUri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());
            request = new HttpReport(workspaceUri, info);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);

            Document reportDoc = request.getResponseBodyAsDocument(response.getEntity());
            return retrieveQNodeTypeDefinitions(sessionInfo, reportDoc);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (ParserConfigurationException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public Iterator<QNodeTypeDefinition> getQNodeTypeDefinitions(SessionInfo sessionInfo, Name[] nodetypeNames) throws RepositoryException {
        // in order to avoid individual calls for every nodetype, retrieve
        // the complete set from the server (again).
        return getQNodeTypeDefinitions(sessionInfo);
    }

    @Override
    public void registerNodeTypes(SessionInfo sessionInfo, QNodeTypeDefinition[] nodeTypeDefinitions, boolean allowUpdate) throws RepositoryException {
        HttpProppatch request = null;
        try {
            DavPropertySet setProperties = new DavPropertySet();
            setProperties.add(createRegisterNodeTypesProperty(sessionInfo, nodeTypeDefinitions, allowUpdate));
            String uri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());
            request = new HttpProppatch(uri, setProperties, new DavPropertyNameSet());
            initMethod(request, sessionInfo, true);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public void unregisterNodeTypes(SessionInfo sessionInfo, Name[] nodeTypeNames) throws RepositoryException {
        HttpProppatch request = null;
        try {
            DavPropertySet setProperties = new DavPropertySet();
            setProperties.add(createUnRegisterNodeTypesProperty(sessionInfo, nodeTypeNames));
            String uri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());
            request = new HttpProppatch(uri, setProperties, new DavPropertyNameSet());
            initMethod(request, sessionInfo, true);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public void createWorkspace(SessionInfo sessionInfo, String name, String srcWorkspaceName) throws RepositoryException {
        if (srcWorkspaceName != null) {
            throw new UnsupportedOperationException("JCR-2003. Implementation missing");
        }

        HttpMkworkspace request = null;
        try {
            request = new HttpMkworkspace(uriResolver.getWorkspaceUri(name));
            initMethod(request, sessionInfo, true);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    @Override
    public void deleteWorkspace(SessionInfo sessionInfo, String name) throws RepositoryException {
        HttpDelete request = null;
        try {
            request = new HttpDelete(uriResolver.getWorkspaceUri(name));
            initMethod(request, sessionInfo, true);
            HttpResponse response = executeRequest(sessionInfo, request);
            request.checkSuccess(response);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    /**
     * Compute the repository URI (while dealing with trailing / and port number
     * defaulting)
     */
    public static URI computeRepositoryUri(String uri) throws URISyntaxException {
        URI repositoryUri = URI.create((uri.endsWith("/")) ? uri : uri + "/");
        // workaround for JCR-3228: normalize default port numbers because of
        // the weak URI matching code elsewhere (the remote server is unlikely
        // to include the port number in URIs when it's the default for the
        // protocol)
        boolean useDefaultPort = ("http".equalsIgnoreCase(repositoryUri.getScheme()) && repositoryUri.getPort() == 80)
                || (("https".equalsIgnoreCase(repositoryUri.getScheme()) && repositoryUri.getPort() == 443));
        if (useDefaultPort) {
            repositoryUri = new URI(repositoryUri.getScheme(), repositoryUri.getUserInfo(), repositoryUri.getHost(), -1,
                    repositoryUri.getPath(), repositoryUri.getQuery(), repositoryUri.getFragment());
        }

        return repositoryUri;
    }

    public HttpResponse executeRequest(SessionInfo sessionInfo, HttpUriRequest request) throws IOException, RepositoryException {
        return getClient(sessionInfo).execute(request, getContext(sessionInfo));
    }

    /**
     *
     * @param sessionInfo
     * @param reportDoc
     * @return
     * @throws RepositoryException
     */
    private Iterator<QNodeTypeDefinition> retrieveQNodeTypeDefinitions(SessionInfo sessionInfo, Document reportDoc) throws RepositoryException {
        ElementIterator it = DomUtil.getChildren(reportDoc.getDocumentElement(), NodeTypeConstants.NODETYPE_ELEMENT, null);
        List<QNodeTypeDefinition> ntDefs = new ArrayList<QNodeTypeDefinition>();
        NamePathResolver resolver = getNamePathResolver(sessionInfo);
        while (it.hasNext()) {
            ntDefs.add(DefinitionUtil.createQNodeTypeDefinition(it.nextElement(), resolver, getQValueFactory()));
        }
        // refresh node type definitions map
        synchronized (nodeTypeDefinitions) {
            nodeTypeDefinitions.clear();
            for (Object ntDef : ntDefs) {
                QNodeTypeDefinition def = (QNodeTypeDefinition) ntDef;
                nodeTypeDefinitions.put(def.getName(), def);
            }
        }
        return ntDefs.iterator();
    }

    private DavProperty<List<XmlSerializable>> createRegisterNodeTypesProperty(SessionInfo sessionInfo, QNodeTypeDefinition[] nodeTypeDefinitions, final boolean allowUpdate) throws IOException {
        // create xml elements for both cnd and allow update value.
        List<XmlSerializable> val = new ArrayList<XmlSerializable>();

        StringWriter sw = new StringWriter();
        CompactNodeTypeDefWriter writer = new CompactNodeTypeDefWriter(sw, new NamespaceResolverImpl(sessionInfo), true);
        writer.write(Arrays.asList(nodeTypeDefinitions));
        writer.close();

        final String cnd = sw.toString();
        val.add(new XmlSerializable() {
            public Element toXml(Document document) {
                Element cndElem = document.createElementNS(JcrRemotingConstants.NS_URI, JcrRemotingConstants.NS_PREFIX +  ":" + JcrRemotingConstants.XML_CND);
                DomUtil.setText(cndElem, cnd);
                return cndElem;
            }
        });
        val.add(new XmlSerializable() {
            public Element toXml(Document document) {
                Element allowElem = document.createElementNS(JcrRemotingConstants.NS_URI, JcrRemotingConstants.NS_PREFIX  + ":" + JcrRemotingConstants.XML_ALLOWUPDATE);
                DomUtil.setText(allowElem, Boolean.toString(allowUpdate));
                return allowElem;
            }
        });

        return new DefaultDavProperty<List<XmlSerializable>>(JcrRemotingConstants.JCR_NODETYPES_CND_LN, val, ItemResourceConstants.NAMESPACE, false);
    }

    private DavProperty<List<XmlSerializable>> createUnRegisterNodeTypesProperty(SessionInfo sessionInfo, Name[] nodeTypeNames) throws IOException, RepositoryException {
        NamePathResolver resolver = getNamePathResolver(sessionInfo);
        List<XmlSerializable> val = new ArrayList<XmlSerializable>();
        for (Name ntName : nodeTypeNames) {
            final String jcrName = resolver.getJCRName(ntName);
            val.add(new XmlSerializable() {
                public Element toXml(Document document) {
                    Element ntNameElem = document.createElementNS(JcrRemotingConstants.NS_URI, JcrRemotingConstants.NS_PREFIX + ":" + JcrRemotingConstants.XML_NODETYPENAME);
                    org.w3c.dom.Text txt = document.createTextNode(jcrName);
                    ntNameElem.appendChild(txt);
                    return ntNameElem;
                }
            });
        }
        return new DefaultDavProperty<List<XmlSerializable>>(JcrRemotingConstants.JCR_NODETYPES_CND_LN, val, ItemResourceConstants.NAMESPACE, false);
    }

    private static DavProperty<List<XmlSerializable>> createValuesProperty(Value[] jcrValues) {
        // convert the specified jcr values to a xml-serializable value
        List<XmlSerializable> val = new ArrayList<XmlSerializable>();
        for (final Value jcrValue : jcrValues) {
            val.add(new XmlSerializable() {
                public Element toXml(Document document) {
                    try {
                        return ValueUtil.valueToXml(jcrValue, document);
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        return new DefaultDavProperty<List<XmlSerializable>>(JcrRemotingConstants.JCR_VALUES_LN, val, ItemResourceConstants.NAMESPACE, false);
    }

    private static DavProperty<List<XmlSerializable>> createNamespaceProperty(final Map<String, String> namespaces) {
        // convert the specified namespace to a xml-serializable value
        List<XmlSerializable> val = new ArrayList<XmlSerializable>();
        for (final String prefix : namespaces.keySet()) {
            val.add(new XmlSerializable() {

                public Element toXml(Document document) {
                    Element nsElem = document.createElementNS(JcrRemotingConstants.NS_URI, JcrRemotingConstants.NS_PREFIX + ":" + JcrRemotingConstants.XML_NAMESPACE);
                    Element prefixElem = document.createElementNS(JcrRemotingConstants.NS_URI, JcrRemotingConstants.NS_PREFIX + ":" + JcrRemotingConstants.XML_PREFIX);
                    org.w3c.dom.Text txt = document.createTextNode(prefix);
                    prefixElem.appendChild(txt);

                    final String uri = namespaces.get(prefix);
                    Element uriElem = document.createElementNS(JcrRemotingConstants.NS_URI, JcrRemotingConstants.NS_PREFIX + ":" + JcrRemotingConstants.XML_URI);
                    org.w3c.dom.Text txt2 = document.createTextNode(uri);
                    uriElem.appendChild(txt2);

                    nsElem.appendChild(prefixElem);
                    nsElem.appendChild(uriElem);

                    return nsElem;
                }
            });
        }
        return new DefaultDavProperty<List<XmlSerializable>>(JcrRemotingConstants.JCR_NAMESPACES_LN, val, ItemResourceConstants.NAMESPACE, false);
    }

    private static DavProperty<List<XmlSerializable>> createNodeTypeProperty(String localName, String[] ntNames) {
        // convert the specified node type names to a xml-serializable value
        List<XmlSerializable> val = new ArrayList<XmlSerializable>();
        for (final String ntName : ntNames) {
            val.add(new XmlSerializable() {
                public Element toXml(Document document) {
                    return NodeTypeUtil.ntNameToXml(ntName, document);
                }
            });
        }
        return new DefaultDavProperty<List<XmlSerializable>>(localName, val, ItemResourceConstants.NAMESPACE, false);
    }

    private Set<String> getDavComplianceClasses(SessionInfo sessionInfo) throws RepositoryException {
        if (this.remoteDavComplianceClasses == null) {
            HttpOptions request = new HttpOptions(uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName()));
            try {
                HttpResponse response = executeRequest(sessionInfo, request);
                int status = response.getStatusLine().getStatusCode();
                if (status != DavServletResponse.SC_OK) {
                    throw new DavException(status);
                }
                this.remoteDavComplianceClasses = request.getDavComplianceClasses(response);
            } catch (IOException e) {
                throw new RepositoryException(e);
            } catch (DavException e) {
                throw ExceptionConverter.generate(e);
            } finally {
                request.releaseConnection();
            }
        }
        return this.remoteDavComplianceClasses;
    }

    private boolean isDavClass3(SessionInfo sessionInfo) {
        try {
            return getDavComplianceClasses(sessionInfo).contains("3");
        } catch (RepositoryException ex) {
            log.warn("failure to obtain OPTIONS response", ex);
            return false;
        }
    }

    private static String obtainAbsolutePathFromUri(String uri) {
        try {
            java.net.URI u = new java.net.URI(uri);
            StringBuilder sb = new StringBuilder();
            sb.append(u.getRawPath());
            if (u.getRawQuery() != null) {
                sb.append("?").append(u.getRawQuery());
            }
            return sb.toString();
        } catch (java.net.URISyntaxException ex) {
            log.warn("parsing " + uri, ex);
            return uri;
        }
    }

    private static String[] obtainAbsolutePathsFromUris(String[] uris) {
        if (uris == null) {
            return null;
        } else {
            String result[] = new String[uris.length];

            for (int i = 0; i < result.length; i++) {
                result[i] = obtainAbsolutePathFromUri(uris[i]);
            }
            return result;
        }
    }

    //------------------------------------------------< Inner Class 'Batch' >---
    private class BatchImpl implements Batch {

        private final SessionInfo sessionInfo;
        private final ItemId targetId;
        private final List<HttpRequestBase> requests = new ArrayList<HttpRequestBase>();
        private final NamePathResolver resolver;

        private String batchId;

        private boolean isConsumed = false;
        private boolean clear = false;

        private BatchImpl(ItemId targetId, SessionInfo sessionInfo) throws RepositoryException {
            this.targetId = targetId;
            this.sessionInfo = sessionInfo;
            this.resolver = getNamePathResolver(sessionInfo);
        }

        private HttpClient start() throws RepositoryException {
            checkConsumed();
            String uri = getItemUri(targetId, sessionInfo);
            HttpLock request = null;
            try {
                // start special 'lock'
                request = new HttpLock(uri, new org.apache.jackrabbit.webdav.lock.LockInfo(TransactionConstants.LOCAL, TransactionConstants.TRANSACTION, null,
                        INFINITE_TIMEOUT, true));
                initMethod(request, sessionInfo, true);

                HttpClient client = getClient(sessionInfo);
                HttpResponse response = client.execute(request,getContext(sessionInfo));
                if (response.getStatusLine().getStatusCode() == DavServletResponse.SC_PRECONDITION_FAILED) {
                    throw new InvalidItemStateException("Unable to persist transient changes.");
                }
                request.checkSuccess(response);

                batchId = request.getLockToken(response);

                return client;
            } catch (IOException e) {
                throw new RepositoryException(e);
            } catch (DavException e) {
                throw ExceptionConverter.generate(e);
            } finally {
                if (request != null) {
                    request.releaseConnection();
                }
            }
        }

        private void end(HttpClient client, boolean commit) throws RepositoryException {
            checkConsumed();

            String uri = getItemUri(targetId, sessionInfo);
            HttpUnlock request = null;
            try {
                // make sure the lock initially created is removed again on the
                // server, asking the server to persist the modifications
                request = new HttpUnlock(uri, batchId);
                initMethod(request, sessionInfo, true);

                // in contrast to standard UNLOCK, the tx-unlock provides a
                // request body.
                request.setEntity(XmlEntity.create(new TransactionInfo(commit)));
                HttpResponse response = client.execute(request, getContext(sessionInfo));
                request.checkSuccess(response);
                if (sessionInfo instanceof SessionInfoImpl) {
                    ((SessionInfoImpl) sessionInfo).setLastBatchId(batchId);
                }
                if (clear) {
                    clearItemUriCache(sessionInfo);
                }
            } catch (IOException e) {
                throw new RepositoryException(e);
            } catch (DavException e) {
                throw ExceptionConverter.generate(e);
            } finally {
                if (request != null) {
                    // release UNLOCK method
                    request.releaseConnection();
                }
            }
        }

        private void dispose() {
            requests.clear();
            isConsumed = true;
        }

        private void checkConsumed() {
            if (isConsumed) {
                throw new IllegalStateException("Batch has already been consumed.");
            }
        }

        private boolean isEmpty() {
            return requests.isEmpty();
        }

        private Iterator<HttpRequestBase> requests() {
            return requests.iterator();
        }

        //----------------------------------------------------------< Batch >---
        @Override
        public void addNode(NodeId parentId, Name nodeName, Name nodetypeName, String uuid) throws RepositoryException {
            checkConsumed();
            try {
                // TODO: TOBEFIXED. WebDAV does not allow MKCOL for existing resource -> problem with SNS
                // use fake name instead (see also #importXML)
                Name fakeName = getNameFactory().create(Name.NS_DEFAULT_URI, UUID.randomUUID().toString());
                String uri = getItemUri(parentId, fakeName, sessionInfo);
                HttpMkcol request = new HttpMkcol(uri);

                // build 'sys-view' for the node to create and append it as request body
                Document body = DomUtil.createDocument();
                BatchUtils.createNodeElement(body, nodeName, nodetypeName, uuid, resolver);
                request.setEntity(XmlEntity.create(body));

                requests.add(request);
            } catch (IOException e) {
                throw new RepositoryException(e);
            } catch (ParserConfigurationException e) {
                throw new RepositoryException(e);
            }
        }

        @Override
        public void addProperty(NodeId parentId, Name propertyName, QValue value) throws RepositoryException {
            checkConsumed();
            String uri = getItemUri(parentId, propertyName, sessionInfo);

            HttpPut request = new HttpPut(uri);
            request.setHeader(HEADER_CONTENT_TYPE, JcrValueType.contentTypeFromType(value.getType()));
            request.setEntity(getEntity(value));
            requests.add(request);
        }

        @Override
        public void addProperty(NodeId parentId, Name propertyName, QValue[] values) throws RepositoryException {
            checkConsumed();
            // TODO: avoid usage of the ValuesProperty. specially for binary props.
            // TODO: replace by a multipart-POST
            try {
                String uri = getItemUri(parentId, propertyName, sessionInfo);
                Value[] jcrValues = new Value[values.length];
                for (int i = 0; i < values.length; i++) {
                    jcrValues[i] = ValueFormat.getJCRValue(values[i], resolver, valueFactory);
                }
                DavProperty<List<XmlSerializable>> vp = createValuesProperty(jcrValues);
                HttpPut request = new HttpPut(uri);
                request.setEntity(XmlEntity.create(vp));

                requests.add(request);
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }

        @Override
        public void setValue(PropertyId propertyId, QValue value) throws RepositoryException {
            checkConsumed();
            if (value == null) {
                // setting property value to 'null' is identical to a removal
                remove(propertyId);
            } else {
                HttpEntity ent = getEntity(value);
                String uri = getItemUri(propertyId, sessionInfo);
                // TODO: use PUT in order to avoid the ValuesProperty-PROPPATCH call.
                // TODO: actually not quite correct for PROPPATCH assert that prop really exists.
                HttpPut request = new HttpPut(uri);
                request.setHeader(HEADER_CONTENT_TYPE, JcrValueType.contentTypeFromType(value.getType()));
                request.setEntity(ent);
                requests.add(request);
            }
        }

        @Override
        public void setValue(PropertyId propertyId, QValue[] values) throws RepositoryException {
            checkConsumed();
            if (values == null) {
                // setting property value to 'null' is identical to a removal
                remove(propertyId);
            } else {
                // TODO: use multipart-POST instead of ValuesProperty
                DavPropertySet setProperties = new DavPropertySet();
                // SPI values must be converted to jcr values
                Value[] jcrValues = new Value[values.length];
                for (int i = 0; i < values.length; i++) {
                    jcrValues[i] = ValueFormat.getJCRValue(values[i], resolver, valueFactory);
                }
                setProperties.add(createValuesProperty(jcrValues));
                try {
                    String uri = getItemUri(propertyId, sessionInfo);
                    HttpProppatch request = new HttpProppatch(uri, setProperties, new DavPropertyNameSet());
                    requests.add(request);
                } catch (IOException e) {
                    throw new RepositoryException(e);
                }
            }
        }

        private HttpEntity getEntity(QValue value) throws RepositoryException {
            // SPI value must be converted to jcr value
            int type = value.getType();
            String contentType = JcrValueType.contentTypeFromType(type);
            HttpEntity ent;
            switch (type) {
                case PropertyType.NAME:
                case PropertyType.PATH:
                    String str = ValueFormat.getJCRString(value, resolver);
                    ent = new StringEntity(str, ContentType.create(contentType, "UTF-8"));
                    break;
                case PropertyType.BINARY:
                    InputStream in = value.getStream();
                    ent = new InputStreamEntity(in, ContentType.create(contentType));
                    break;
                default:
                    str = value.getString();
                    ent = new StringEntity(str, ContentType.create(contentType, "UTF-8"));
                    break;
            }
            return ent;
        }

        @Override
        public void remove(ItemId itemId) throws RepositoryException {
            checkConsumed();
            String uri = getItemUri(itemId, sessionInfo);
            HttpDelete request = new HttpDelete(uri);

            requests.add(request);
            if (itemId.getPath() == null) {
                clear = true;
            }
        }

        @Override
        public void reorderNodes(NodeId parentId, NodeId srcNodeId, NodeId beforeNodeId) throws RepositoryException {
            checkConsumed();
            try {
                String uri = getItemUri(parentId, sessionInfo);
                String srcUri = getItemUri(srcNodeId, sessionInfo);
                String srcSegment = Text.getName(srcUri, true);

                Position p;
                if (beforeNodeId == null) {
                    // move src to the end
                    p = new Position(OrderingConstants.XML_LAST);
                } else {
                    // insert src before the targetSegment
                    String beforeUri = getItemUri(beforeNodeId, sessionInfo);
                    String targetSegment = Text.getName(beforeUri, true);
                    p = new Position(OrderingConstants.XML_BEFORE, targetSegment);
                }
                OrderPatch op = new OrderPatch(OrderingConstants.ORDERING_TYPE_CUSTOM, new OrderPatch.Member(srcSegment, p));
                HttpOrderpatch request = new HttpOrderpatch(uri, op);
                requests.add(request);
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }

        @Override
        public void setMixins(NodeId nodeId, Name[] mixinNodeTypeIds) throws RepositoryException {
            checkConsumed();
            try {
                DavPropertySet setProperties;
                DavPropertyNameSet removeProperties;
                if (mixinNodeTypeIds == null || mixinNodeTypeIds.length == 0) {
                    setProperties = new DavPropertySet();
                    removeProperties = new DavPropertyNameSet();
                    removeProperties.add(JcrRemotingConstants.JCR_MIXINNODETYPES_LN, ItemResourceConstants.NAMESPACE);
                } else {
                    String[] ntNames = new String[mixinNodeTypeIds.length];
                    for (int i = 0; i < mixinNodeTypeIds.length; i++) {
                        ntNames[i] = resolver.getJCRName(mixinNodeTypeIds[i]);
                    }
                    setProperties = new DavPropertySet();
                    setProperties.add(createNodeTypeProperty(JcrRemotingConstants.JCR_MIXINNODETYPES_LN, ntNames));
                    removeProperties = new DavPropertyNameSet();
                }

                String uri = getItemUri(nodeId, sessionInfo);
                HttpProppatch request = new HttpProppatch(uri, setProperties, removeProperties);

                requests.add(request);
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }

        @Override
        public void setPrimaryType(NodeId nodeId, Name primaryNodeTypeName) throws RepositoryException {
            checkConsumed();
            try {
                DavPropertySet setProperties = new DavPropertySet();
                setProperties.add(createNodeTypeProperty(JcrRemotingConstants.JCR_PRIMARYNODETYPE_LN, new String[] {resolver.getJCRName(primaryNodeTypeName)}));

                String uri = getItemUri(nodeId, sessionInfo);
                HttpProppatch request = new HttpProppatch(uri, setProperties, new DavPropertyNameSet());

                requests.add(request);
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }

        @Override
        public void move(NodeId srcNodeId, NodeId destParentNodeId, Name destName) throws RepositoryException {
            checkConsumed();
            String uri = getItemUri(srcNodeId, sessionInfo);
            String destUri = getItemUri(destParentNodeId, destName, sessionInfo);
            if (isDavClass3(sessionInfo)) {
                destUri = obtainAbsolutePathFromUri(destUri);
            }
            HttpMove request = new HttpMove(uri, destUri, false);

            requests.add(request);
            clear = true;
        }

        @Override
        public void setTree(NodeId parentId, Tree tree) throws RepositoryException {
            checkConsumed();

            if (!(tree instanceof DocumentTree)) {
                throw new RepositoryException("Invalid tree implementation " + tree.getClass().getName());
            }
            try {
                // TODO: TOBEFIXED. WebDAV does not allow MKCOL for existing resource -> problem with SNS
                // use fake name instead (see also #importXML)
                Name fakeName = getNameFactory().create(Name.NS_DEFAULT_URI, UUID.randomUUID().toString());
                String uri = getItemUri(parentId, fakeName, sessionInfo);
                HttpMkcol request = new HttpMkcol(uri);

                request.setEntity(XmlEntity.create(((DocumentTree) tree).toDocument()));

                requests.add(request);
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }
    }

    //----------------------------------------------< NamespaceResolverImpl >---
    /**
     * NamespaceResolver implementation that uses a sessionInfo to determine
     * namespace mappings either from cache or from the server.
     */
    private class NamespaceResolverImpl implements NamespaceResolver {

        private final SessionInfo sessionInfo;

        /**
         * Creates a new namespace resolver using the given session info.
         *
         * @param sessionInfo the session info to contact the repository.
         */
        private NamespaceResolverImpl(SessionInfo sessionInfo) {
            this.sessionInfo = sessionInfo;
        }

        @Override
        public String getURI(String prefix) throws NamespaceException {
            try {
                return getNamespaceURI(sessionInfo, prefix);
            } catch (RepositoryException e) {
                String msg = "Error retrieving namespace uri";
                throw new NamespaceException(msg, e);
            }
        }

        @Override
        public String getPrefix(String uri) throws NamespaceException {
            try {
                return getNamespacePrefix(sessionInfo, uri);
            } catch (RepositoryException e) {
                String msg = "Error retrieving namespace prefix";
                throw new NamespaceException(msg, e);
            }
        }
    }

    //---------------------------------------------< IdentifierResolverImpl >---
    private class IdentifierResolverImpl implements IdentifierResolver {

        private final SessionInfo sessionInfo;

        private IdentifierResolverImpl(SessionInfo sessionInfo) {
            this.sessionInfo = sessionInfo;
        }

        private Path buildPath(String uniqueID) throws RepositoryException {
            String uri = uriResolver.getItemUri(getIdFactory().createNodeId(uniqueID), sessionInfo.getWorkspaceName(), sessionInfo);
            return uriResolver.getQPath(uri, sessionInfo);
        }

        private Path resolvePath(String jcrPath) throws RepositoryException {
            return ((SessionInfoImpl) sessionInfo).getNamePathResolver().getQPath(jcrPath);
        }

        @Override
        public Path getPath(String identifier) throws MalformedPathException {
            try {
                int pos = identifier.indexOf('/');
                if (pos == -1) {
                    // unique id identifier
                    return buildPath(identifier);
                } else if (pos == 0) {
                    // jcr-path identifier
                    return resolvePath(identifier);
                } else {
                    Path p1 = buildPath(identifier.substring(0, pos));
                    Path p2 = resolvePath(identifier.substring(pos));
                    return getPathFactory().create(p1, p2, true);
                }
            } catch (RepositoryException e) {
                throw new MalformedPathException(identifier);
            }
        }

        @Override
        public void checkFormat(String identifier) throws MalformedPathException {
            // cannot be determined. assume ok.
        }
    }
    //-----------------------------------------------< NamePathResolverImpl >---
    /**
     * Implements a namespace resolver based on a session info.
     */
    private class NamePathResolverImpl implements NamePathResolver {

        private final NameResolver nResolver;
        private final PathResolver pResolver;

        private NamePathResolverImpl(SessionInfo sessionInfo) {
            NamespaceResolver nsResolver = new NamespaceResolverImpl(sessionInfo);
            nResolver = new ParsingNameResolver(getNameFactory(), nsResolver);
            IdentifierResolver idResolver = new IdentifierResolverImpl(sessionInfo);
            pResolver = new ParsingPathResolver(getPathFactory(), nResolver, idResolver);
        }

        private NamePathResolverImpl(NamespaceResolver nsResolver) {
            nResolver = new ParsingNameResolver(getNameFactory(), nsResolver);
            pResolver = new ParsingPathResolver(getPathFactory(), nResolver);
        }

        @Override
        public Name getQName(String jcrName) throws IllegalNameException, NamespaceException {
            return nResolver.getQName(jcrName);
        }

        @Override
        public String getJCRName(Name qName) throws NamespaceException {
            return nResolver.getJCRName(qName);
        }

        @Override
        public Path getQPath(String path) throws MalformedPathException, IllegalNameException, NamespaceException {
            return pResolver.getQPath(path);
        }

        @Override
        public Path getQPath(String path, boolean normalizeIdentifier) throws MalformedPathException, IllegalNameException, NamespaceException {
            return pResolver.getQPath(path, normalizeIdentifier);
        }

        @Override
        public String getJCRPath(Path path) throws NamespaceException {
            return pResolver.getJCRPath(path);
        }
    }

    /**
     * Namespace Cache
     */
    private static class NamespaceCache extends AbstractNamespaceResolver {

        private final HashMap<String, String> prefixToURI = new HashMap<String, String>();
        private final HashMap<String, String> uriToPrefix = new HashMap<String, String>();

        public Map<String, String> getNamespaces() {
            return new HashMap<String, String>(prefixToURI);
        }

        public void add(String prefix, String uri) {
            prefixToURI.put(prefix, uri);
            uriToPrefix.put(uri, prefix);
        }

        public void remove(String prefix, String uri) {
            prefixToURI.remove(prefix);
            uriToPrefix.remove(uri);
        }

        //----------------------------------------------< NamespaceResolver >---

        @Override
        public String getURI(String prefix) throws NamespaceException {
            String uri = prefixToURI.get(prefix);
            if (uri != null) {
                return uri;
            } else {
                throw new NamespaceException(prefix + ": is not a registered namespace prefix.");
            }
        }

        @Override
        public String getPrefix(String uri) throws NamespaceException {
            String prefix = uriToPrefix.get(uri);
            if (prefix != null) {
                return prefix;
            } else {
                throw new NamespaceException(uri + ": is not a registered namespace uri.");
            }
        }
    }
}
