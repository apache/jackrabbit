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
import java.io.UnsupportedEncodingException;
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
import javax.jcr.MergeException;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.version.VersionException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
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
import org.apache.jackrabbit.webdav.client.methods.CheckinMethod;
import org.apache.jackrabbit.webdav.client.methods.CheckoutMethod;
import org.apache.jackrabbit.webdav.client.methods.CopyMethod;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.LabelMethod;
import org.apache.jackrabbit.webdav.client.methods.LockMethod;
import org.apache.jackrabbit.webdav.client.methods.MergeMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.MkWorkspaceMethod;
import org.apache.jackrabbit.webdav.client.methods.MoveMethod;
import org.apache.jackrabbit.webdav.client.methods.OptionsMethod;
import org.apache.jackrabbit.webdav.client.methods.OrderPatchMethod;
import org.apache.jackrabbit.webdav.client.methods.PollMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PropPatchMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.client.methods.ReportMethod;
import org.apache.jackrabbit.webdav.client.methods.SearchMethod;
import org.apache.jackrabbit.webdav.client.methods.SubscribeMethod;
import org.apache.jackrabbit.webdav.client.methods.UnLockMethod;
import org.apache.jackrabbit.webdav.client.methods.UnSubscribeMethod;
import org.apache.jackrabbit.webdav.client.methods.UpdateMethod;
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
import org.apache.jackrabbit.webdav.ordering.OrderingConstants;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.HrefProperty;
import org.apache.jackrabbit.webdav.search.SearchConstants;
import org.apache.jackrabbit.webdav.search.SearchInfo;
import org.apache.jackrabbit.webdav.security.CurrentUserPrivilegeSetProperty;
import org.apache.jackrabbit.webdav.security.Privilege;
import org.apache.jackrabbit.webdav.security.SecurityConstants;
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
import org.apache.jackrabbit.webdav.xml.Namespace;
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
     * configured with {@link HttpConnectionManagerParams#setDefaultMaxConnectionsPerHost(int)}.
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

    private final HostConfiguration hostConfig;
    private final ConcurrentMap<Object, HttpClient> clients;
    private final HttpConnectionManager connectionManager;

    private final Map<Name, QNodeTypeDefinition> nodeTypeDefinitions = new HashMap<Name, QNodeTypeDefinition>();

    /** Repository descriptors. */
    private final Map<String, QValue[]> descriptors =
            new HashMap<String, QValue[]>();

    /** Observation features. */
    private boolean remoteServerProvidesNodeTypes = false;
    private boolean remoteServerProvidesNoLocalFlag = false;

    /**
     * Same as {@link #RepositoryServiceImpl(String, IdFactory, NameFactory, PathFactory, QValueFactory, int, int)}
     * using {@link ItemInfoCacheImpl#DEFAULT_CACHE_SIZE)} as size for the item
     * cache and {@link #MAX_CONNECTIONS_DEFAULT} for the maximum number of
     * connections on the client.
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
     * Same as {@link #RepositoryServiceImpl(String, IdFactory, NameFactory, PathFactory, QValueFactory, int, int)}
     * using {@link #MAX_CONNECTIONS_DEFAULT} for the maximum number of
     * connections on the client.
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
        this(uri, idFactory, nameFactory, pathFactory, qValueFactory, itemInfoCacheSize, MAX_CONNECTIONS_DEFAULT);
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
     * @param maximumHttpConnections A int &gt;0 defining the maximum number of
     * connections per host to be configured on
     * {@link HttpConnectionManagerParams#setDefaultMaxConnectionsPerHost(int)}.
     * @throws RepositoryException If an error occurs.
     */
    public RepositoryServiceImpl(String uri, IdFactory idFactory,
                                 NameFactory nameFactory, PathFactory pathFactory,
                                 QValueFactory qValueFactory, int itemInfoCacheSize,
                                 int maximumHttpConnections ) throws RepositoryException {
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

        try {
            URI repositoryUri = new URI((uri.endsWith("/")) ? uri : uri+"/", true);
            hostConfig = new HostConfiguration();
            hostConfig.setHost(repositoryUri);

            nsCache = new NamespaceCache();
            uriResolver = new URIResolverImpl(repositoryUri, this, DomUtil.createDocument());
            NamePathResolver resolver = new NamePathResolverImpl(nsCache);
            valueFactory = new ValueFactoryQImpl(qValueFactory, resolver);

        } catch (URIException e) {
            throw new RepositoryException(e);
        } catch (ParserConfigurationException e) {
            throw new RepositoryException(e);
        }

        connectionManager = new MultiThreadedHttpConnectionManager();
        if (maximumHttpConnections > 0) {
            HttpConnectionManagerParams connectionParams = connectionManager.getParams();
            connectionParams.setDefaultMaxConnectionsPerHost(maximumHttpConnections);
        }

        // This configuration of the clients cache assumes that the level of
        // concurrency on this map will be equal to the default number of maximum
        // connections allowed on the httpClient level.
        // TODO: review again
        int concurrencyLevel = MAX_CONNECTIONS_DEFAULT;
        int initialCapacity = MAX_CONNECTIONS_DEFAULT;
        if (maximumHttpConnections > 0) {
            concurrencyLevel = maximumHttpConnections;
            initialCapacity = maximumHttpConnections;
        }
        clients = new ConcurrentHashMap<Object, HttpClient>(concurrencyLevel, .75f, initialCapacity);
    }

    private static void checkSessionInfo(SessionInfo sessionInfo) throws RepositoryException {
        if (!(sessionInfo instanceof SessionInfoImpl)) {
            throw new RepositoryException("Unknown SessionInfo implementation.");
        }
    }

    private static void checkSubscription(Subscription subscription) throws RepositoryException {
        if (!(subscription instanceof EventSubscriptionImpl)) {
            throw new RepositoryException("Unknown Subscription implementation.");
        }
    }

    private static boolean isUnLockMethod(DavMethod method) {
        int code = DavMethods.getMethodCode(method.getName());
        return DavMethods.DAV_UNLOCK == code;
    }

    protected static void initMethod(HttpMethod method, SessionInfo sessionInfo, boolean addIfHeader) throws RepositoryException {
        if (addIfHeader) {
            checkSessionInfo(sessionInfo);
            Set<String> allLockTokens = ((SessionInfoImpl) sessionInfo).getAllLockTokens();
            // TODO: ev. build tagged if header
            if (!allLockTokens.isEmpty()) {
                String[] locktokens = allLockTokens.toArray(new String[allLockTokens.size()]);
                IfHeader ifH = new IfHeader(locktokens);
                method.setRequestHeader(ifH.getHeaderName(), ifH.getHeaderValue());
            }
        }
        
        initMethod(method, sessionInfo);
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
    protected static void initMethod(HttpMethod method, SessionInfo sessionInfo) throws RepositoryException {

        boolean isReadAccess = readMethods.contains(method.getName());
        boolean needsSessionId = !isReadAccess || "POLL".equals(method.getName());

        if (sessionInfo instanceof SessionInfoImpl && needsSessionId) {
            StringBuilder linkHeaderField = new StringBuilder();

            String sessionIdentifier = ((SessionInfoImpl) sessionInfo)
                    .getSessionIdentifier();
            linkHeaderField.append("<" + sessionIdentifier + ">; rel=\""
                    + JcrRemotingConstants.RELATION_REMOTE_SESSION_ID + "\"");

            String userdata = ((SessionInfoImpl) sessionInfo).getUserData();
            if (userdata != null && ! isReadAccess) {
                String escaped = Text.escape(userdata);
                linkHeaderField.append((", <data:," + escaped + ">; rel=\""
                        + JcrRemotingConstants.RELATION_USER_DATA + "\""));
            }

            method.addRequestHeader("Link", linkHeaderField.toString());
        }
    }

    private static void initMethod(DavMethod method, BatchImpl batchImpl, boolean addIfHeader) throws RepositoryException {
        initMethod(method, batchImpl.sessionInfo,  addIfHeader);

        // add batchId as separate header, TODO: could probably re-use session id Link relation
        CodedUrlHeader ch = new CodedUrlHeader(TransactionConstants.HEADER_TRANSACTIONID, batchImpl.batchId);
        method.setRequestHeader(ch.getHeaderName(), ch.getHeaderValue());
    }

    private static boolean isSameResource(String requestURI, MultiStatusResponse response) {
        String href = response.getHref();
        if (href.endsWith("/") && !requestURI.endsWith("/")) {
            href = href.substring(0, href.length() - 1);
        }
        return requestURI.equals(href);
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
            client = new HttpClient(connectionManager);
            client.setHostConfiguration(hostConfig);
            // NOTE: null credentials only work if 'missing-auth-mapping' param is
            // set on the server
            org.apache.commons.httpclient.Credentials creds = null;
            if (sessionInfo != null) {
                checkSessionInfo(sessionInfo);
                creds = ((SessionInfoImpl) sessionInfo).getCredentials().getCredentials();
                // always send authentication not waiting for 401
                client.getParams().setAuthenticationPreemptive(true);
            }
            client.getState().setCredentials(AuthScope.ANY, creds);
            clients.put(clientKey, client);
            log.debug("Created Client " + client + " for SessionInfo " + sessionInfo);
        }
        return client;
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

    private NodeId getParentId(DavPropertySet propSet, SessionInfo sessionInfo)
        throws RepositoryException {
        NodeId parentId = null;
        DavProperty<?> p = propSet.get(JcrRemotingConstants.JCR_PARENT_LN, ItemResourceConstants.NAMESPACE);
        if (p != null) {
            HrefProperty parentProp = new HrefProperty(p);
            String parentHref = parentProp.getHrefs().get(0);
            if (parentHref != null && parentHref.length() > 0) {
                parentId = uriResolver.getNodeId(parentHref, sessionInfo);
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
     *
     * @param method
     * @param sessionInfo
     * @throws RepositoryException
     */
    private void execute(DavMethod method, SessionInfo sessionInfo) throws RepositoryException {
        try {
            initMethod(method, sessionInfo, !isUnLockMethod(method));

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e, method);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    //--------------------------------------------------< RepositoryService >---
    /**
     * @see RepositoryService#getIdFactory()
     */
    public IdFactory getIdFactory() {
        return idFactory;
    }

    /**
     * @see RepositoryService#getNameFactory()
     */
    public NameFactory getNameFactory() {
        return nameFactory;
    }

    /**
     * @see RepositoryService#getPathFactory()
     */
    public PathFactory getPathFactory() {
        return pathFactory;
    }

    public QValueFactory getQValueFactory() {
        return qValueFactory;
    }

    public ItemInfoCache getItemInfoCache(SessionInfo sessionInfo) throws RepositoryException {
        return new ItemInfoCacheImpl(itemInfoCacheSize);
    }

    /**
     * @see RepositoryService#getRepositoryDescriptors()
     */
    public Map<String, QValue[]> getRepositoryDescriptors() throws RepositoryException {
        if (descriptors.isEmpty()) {
            ReportInfo info = new ReportInfo(JcrRemotingConstants.REPORT_REPOSITORY_DESCRIPTORS, ItemResourceConstants.NAMESPACE);
            ReportMethod method = null;
            try {
                method = new ReportMethod(uriResolver.getRepositoryUri(), info);
                int sc = getClient(null).executeMethod(method);
                if (sc == HttpStatus.SC_UNAUTHORIZED
                        || sc == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
                    // JCR-3076: Mandatory authentication prevents us from
                    // accessing the descriptors on the server, so instead
                    // of failing with an exception we simply return an empty
                    // set of descriptors
                    log.warn("Authentication required to access repository descriptors");
                    return descriptors;
                }

                method.checkSuccess();
                Document doc = method.getResponseBodyAsDocument();

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
                if (method != null) {
                    method.releaseConnection();
                }
            }
        }
        return descriptors;
    }

    /**
     * @see RepositoryService#obtain(Credentials, String)
     */
    public SessionInfo obtain(Credentials credentials, String workspaceName)
        throws LoginException, NoSuchWorkspaceException, RepositoryException {
        CredentialsWrapper dc = new CredentialsWrapper(credentials);
        return obtain(dc, workspaceName);
    }

    public SessionInfo obtain(SessionInfo sessionInfo, String workspaceName)
        throws LoginException, NoSuchWorkspaceException, RepositoryException {
        checkSessionInfo(sessionInfo);
        return obtain(((SessionInfoImpl)sessionInfo).getCredentials(), workspaceName);
    }

    public SessionInfo impersonate(SessionInfo sessionInfo, Credentials credentials) throws LoginException, RepositoryException {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    private SessionInfo obtain(CredentialsWrapper credentials, String workspaceName)
        throws LoginException, NoSuchWorkspaceException, RepositoryException {
        // check if the workspace with the given name is accessible
        PropFindMethod method = null;
        SessionInfoImpl sessionInfo = new SessionInfoImpl(credentials, workspaceName);
        try {
            DavPropertyNameSet nameSet = new DavPropertyNameSet();
            // for backwards compat. -> retrieve DAV:workspace if the newly
            // added property (workspaceName) is not supported by the server.
            nameSet.add(DeltaVConstants.WORKSPACE);
            nameSet.add(JcrRemotingConstants.JCR_WORKSPACE_NAME_LN, ItemResourceConstants.NAMESPACE);

            method = new PropFindMethod(uriResolver.getWorkspaceUri(workspaceName), nameSet, DEPTH_0);
            getClient(sessionInfo).executeMethod(method);

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length != 1) {
                throw new LoginException("Login failed: Unknown workspace '" + workspaceName+ "'.");
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
                throw new LoginException("Login failed: Unknown workspace '" + workspaceName+ "'.");
            }
        } catch (IOException e) {
            throw new RepositoryException(e.getMessage());
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
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

    public void dispose(SessionInfo sessionInfo) throws RepositoryException {
        checkSessionInfo(sessionInfo);
        removeClient(sessionInfo);
    }

    /**
     * @see RepositoryService#getWorkspaceNames(SessionInfo)
     */
    public String[] getWorkspaceNames(SessionInfo sessionInfo) throws RepositoryException {
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(DeltaVConstants.WORKSPACE);
        PropFindMethod method = null;
        try {
            method = new PropFindMethod(uriResolver.getRepositoryUri(), nameSet, DEPTH_1);
            getClient(sessionInfo).executeMethod(method);
            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            Set<String> wspNames = new HashSet<String>();
            for (MultiStatusResponse response : responses) {
                DavPropertySet props = response.getProperties(DavServletResponse.SC_OK);
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
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#isGranted(SessionInfo, ItemId, String[] actions)
     */
    public boolean isGranted(SessionInfo sessionInfo, ItemId itemId, String[] actions) throws RepositoryException {
        ReportMethod method = null;
        try {
            String uri = getItemUri(itemId, sessionInfo);
            ReportInfo reportInfo = new ReportInfo(JcrRemotingConstants.REPORT_PRIVILEGES, ItemResourceConstants.NAMESPACE);
            reportInfo.setContentElement(DomUtil.hrefToXml(uri, DomUtil.createDocument()));

            method = new ReportMethod(uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName()), reportInfo);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
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
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#getNodeDefinition(SessionInfo, NodeId)
     */
    public QNodeDefinition getNodeDefinition(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        return (QNodeDefinition) getItemDefinition(sessionInfo, nodeId);
    }

    /**
     * @see RepositoryService#getPropertyDefinition(SessionInfo, PropertyId)
     */
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

        DavMethodBase method = null;
        try {
            String uri = getItemUri(itemId, sessionInfo);
            method = new PropFindMethod(uri, nameSet, DEPTH_0);
            getClient(sessionInfo).executeMethod(method);

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length < 1) {
                throw new ItemNotFoundException("Unable to retrieve the item definition for " + saveGetIdString(itemId, sessionInfo));
            }
            if (responses.length > 1) {
                throw new RepositoryException("Internal error: ambigous item definition found '" + saveGetIdString(itemId, sessionInfo) + "'.");
            }
            DavPropertySet propertySet = responses[0].getProperties(DavServletResponse.SC_OK);

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
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#getNodeInfo(SessionInfo, NodeId)
     */
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

        DavMethodBase method = null;
        try {
            String uri = getItemUri(nodeId, sessionInfo);
            method = new PropFindMethod(uri, nameSet, DEPTH_1);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length < 1) {
                throw new ItemNotFoundException("Unable to retrieve the node with id " + saveGetIdString(nodeId, sessionInfo));
            }

            MultiStatusResponse nodeResponse = null;
            List<MultiStatusResponse> childResponses = new ArrayList<MultiStatusResponse>();
            for (MultiStatusResponse response : responses) {
                if (isSameResource(uri, response)) {
                    nodeResponse = response;
                } else {
                    childResponses.add(response);
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
            NodeId parentId = getParentId(propSet, sessionInfo);

            NodeInfoImpl nInfo = buildNodeInfo(nodeResponse, parentId, propSet, sessionInfo, resolver);

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
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#getItemInfos(SessionInfo, ItemId)
     */
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
        }
        else {
            PropertyInfo pInfo = getPropertyInfo(sessionInfo, (PropertyId) itemId);
            return Iterators.singleton(pInfo);
        }
    }

    private NodeInfoImpl buildNodeInfo(MultiStatusResponse nodeResponse,
                                       NodeId parentId, DavPropertySet propSet,
                                       SessionInfo sessionInfo,
                                       NamePathResolver resolver) throws NameException, RepositoryException {
        NodeId id = uriResolver.buildNodeId(parentId, nodeResponse, sessionInfo.getWorkspaceName(), getNamePathResolver(sessionInfo));
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
            QValue[] vs = new QValue[] {getQValueFactory().create(nid.getUniqueID(), PropertyType.STRING)};
            Path p = getPathFactory().create(nPath, NameConstants.JCR_UUID, true);
            PropertyInfo pi = new PropertyInfoImpl(id, p, PropertyType.STRING, false, vs);
            l.add(pi);
        }

        Name pName = NameConstants.JCR_PRIMARYTYPE;
        QValue[] vs = new QValue[] {getQValueFactory().create(nInfo.getNodetype())};
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

    /**
     * @see RepositoryService#getChildInfos(SessionInfo, NodeId)
     */
    public Iterator<ChildInfo> getChildInfos(SessionInfo sessionInfo, NodeId parentId) throws RepositoryException {
        // set of properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(JcrRemotingConstants.JCR_NAME_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(JcrRemotingConstants.JCR_INDEX_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(JcrRemotingConstants.JCR_PARENT_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(JcrRemotingConstants.JCR_UUID_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(DavPropertyName.RESOURCETYPE);

        DavMethodBase method = null;
        try {
            String uri = getItemUri(parentId, sessionInfo);
            method = new PropFindMethod(uri, nameSet, DEPTH_1);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            List<ChildInfo> childEntries;
            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length < 1) {
                throw new ItemNotFoundException("Unable to retrieve the node with id " + saveGetIdString(parentId, sessionInfo));
            } else if (responses.length == 1) {
                // no child nodes nor properties
                childEntries = Collections.emptyList();
                return childEntries.iterator();
            }

            childEntries = new ArrayList<ChildInfo>();
            for (MultiStatusResponse resp : responses) {
                if (!isSameResource(uri, resp)) {
                    DavPropertySet childProps = resp.getProperties(DavServletResponse.SC_OK);
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
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    private ChildInfo buildChildInfo(DavPropertySet properties, SessionInfo sessionInfo) throws RepositoryException {
        Name qName = getQName(properties, getNamePathResolver(sessionInfo));
        int index = getIndex(properties);
        String uuid = getUniqueID(properties);

        return new ChildInfoImpl(qName, uuid, index);
    }

    /**
     * @see RepositoryService#getReferences(SessionInfo, NodeId, Name, boolean)
     */
    public Iterator<PropertyId> getReferences(SessionInfo sessionInfo, NodeId nodeId, Name propertyName, boolean weakReferences) throws ItemNotFoundException, RepositoryException {
        // set of properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        if (weakReferences) {
            nameSet.add(JcrRemotingConstants.JCR_WEAK_REFERENCES_LN, ItemResourceConstants.NAMESPACE);
        } else {
            nameSet.add(JcrRemotingConstants.JCR_REFERENCES_LN, ItemResourceConstants.NAMESPACE);
        }

        DavMethodBase method = null;
        try {
            String uri = getItemUri(nodeId, sessionInfo);
            method = new PropFindMethod(uri, nameSet, DEPTH_0);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length < 1) {
                throw new ItemNotFoundException("Unable to retrieve the node with id " + saveGetIdString(nodeId, sessionInfo));
            }

            List<PropertyId> refIds = Collections.emptyList();
            for (MultiStatusResponse resp : responses) {
                if (isSameResource(uri, resp)) {
                    DavPropertySet props = resp.getProperties(DavServletResponse.SC_OK);
                    DavProperty<?> p;
                    if (weakReferences) {
                        p = props.get(JcrRemotingConstants.JCR_WEAK_REFERENCES_LN, ItemResourceConstants.NAMESPACE);
                    } else {
                        p = props.get(JcrRemotingConstants.JCR_REFERENCES_LN, ItemResourceConstants.NAMESPACE);
                    }

                    if (p != null) {
                        refIds = new ArrayList<PropertyId>();
                        HrefProperty hp = new HrefProperty(p);
                        for (String propHref : hp.getHrefs()) {
                            PropertyId propId = uriResolver.getPropertyId(propHref, sessionInfo);
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
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#getPropertyInfo(SessionInfo, PropertyId)
     */
    public PropertyInfo getPropertyInfo(SessionInfo sessionInfo, PropertyId propertyId) throws RepositoryException {
        GetMethod method = null;
        try {
            String uri = getItemUri(propertyId, sessionInfo);
            method = new GetMethod(uri);
            HttpClient client = getClient(sessionInfo);
            client.executeMethod(method);

            int status = method.getStatusCode();
            if (status != DavServletResponse.SC_OK) {
                throw ExceptionConverter.generate(new DavException(status, method.getStatusText()));
            }

            Path path = uriResolver.getQPath(uri, sessionInfo);

            String ct = null;
            Header hd = method.getResponseHeader(HEADER_CONTENT_TYPE);
            if (hd != null) {
                ct = hd.getValue();
            }

            boolean isMultiValued;
            QValue[] values;
            int type;

            NamePathResolver resolver = getNamePathResolver(sessionInfo);
            if (ct.startsWith("jcr-value")) {
                type = JcrValueType.typeFromContentType(ct);                
                QValue v;
                if (type == PropertyType.BINARY) {
                    v = getQValueFactory().create(method.getResponseBodyAsStream());
                } else {
                    Reader reader = new InputStreamReader(method.getResponseBodyAsStream(), method.getResponseCharSet());
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
                values = new QValue[] {v};
                isMultiValued = false;
            } else if (ct.startsWith("text/xml")) {
                // jcr:values property spooled
                values = getValues(method.getResponseBodyAsStream(), resolver, propertyId);
                type = (values.length > 0) ? values[0].getType() : loadType(uri, client, propertyId, sessionInfo, resolver);
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
            if (method != null) {
                method.releaseConnection();
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
            log.warn("Internal error: ", e.getMessage());
            throw new RepositoryException(e);
        } catch (IOException e) {
            log.warn("Internal error: ", e.getMessage());
            throw new RepositoryException(e);
        } catch (ParserConfigurationException e) {
            log.warn("Internal error: ", e.getMessage());
            throw new RepositoryException(e);
        }
    }

    private int loadType(String propertyURI, HttpClient client, PropertyId propertyId, SessionInfo sessionInfo, NamePathResolver resolver) throws IOException, DavException, RepositoryException {
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(JcrRemotingConstants.JCR_TYPE_LN, ItemResourceConstants.NAMESPACE);

        DavMethodBase method = null;
        try {
            method = new PropFindMethod(propertyURI, nameSet, DEPTH_0);
            client.executeMethod(method);
            method.checkSuccess();

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length == 1) {
                DavPropertySet props = responses[0].getProperties(DavServletResponse.SC_OK);
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
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#createBatch(SessionInfo,ItemId)
     */
    public Batch createBatch(SessionInfo sessionInfo, ItemId itemId) throws RepositoryException {
        checkSessionInfo(sessionInfo);
        return new BatchImpl(itemId, sessionInfo);
    }

    /**
     * @see RepositoryService#submit(Batch)
     */
    public void submit(Batch batch) throws RepositoryException {
        if (!(batch instanceof BatchImpl)) {
            throw new RepositoryException("Unknown Batch implementation.");
        }
        BatchImpl batchImpl = (BatchImpl) batch;
        if (batchImpl.isEmpty()) {
            batchImpl.dispose();
            return;
        }

        DavMethod method = null;
        try {
            HttpClient client = batchImpl.start();
            boolean success = false;

            try {
                Iterator<DavMethod> it = batchImpl.methods();
                while (it.hasNext()) {
                    method = it.next();
                    initMethod(method, batchImpl, true);

                    client.executeMethod(method);
                    method.checkSuccess();
                    method.releaseConnection();
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
            throw ExceptionConverter.generate(e, method);
        } finally {
            batchImpl.dispose();
        }
    }

    /**
     * @see RepositoryService#importXml(SessionInfo, NodeId, InputStream, int)
     */
    public void importXml(SessionInfo sessionInfo, NodeId parentId, InputStream xmlStream, int uuidBehaviour) throws RepositoryException {
        // TODO: improve. currently random name is built instead of retrieving name of new resource from top-level xml element within stream
        Name nodeName = getNameFactory().create(Name.NS_DEFAULT_URI, UUID.randomUUID().toString());
        String uri = getItemUri(parentId, nodeName, sessionInfo);
        MkColMethod method = new MkColMethod(uri);
        method.addRequestHeader(JcrRemotingConstants.IMPORT_UUID_BEHAVIOR, Integer.toString(uuidBehaviour));
        method.setRequestEntity(new InputStreamRequestEntity(xmlStream, "text/xml"));
        execute(method, sessionInfo);
    }

    /**
     * @see RepositoryService#move(SessionInfo, NodeId, NodeId, Name)
     */
    public void move(SessionInfo sessionInfo, NodeId srcNodeId, NodeId destParentNodeId, Name destName) throws RepositoryException {
        String uri = getItemUri(srcNodeId, sessionInfo);
        String destUri = getItemUri(destParentNodeId, destName, sessionInfo);
        MoveMethod method = new MoveMethod(uri, destUri, false);
        execute(method, sessionInfo);
        // need to clear the cache as the move may have affected nodes with uuid.
        clearItemUriCache(sessionInfo);
    }

    /**
     * @see RepositoryService#copy(SessionInfo, String, NodeId, NodeId, Name)
     */
    public void copy(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, Name destName) throws RepositoryException {
        String uri = uriResolver.getItemUri(srcNodeId, srcWorkspaceName, sessionInfo);
        String destUri = getItemUri(destParentNodeId, destName, sessionInfo);
        CopyMethod method = new CopyMethod(uri, destUri, false, false);
        execute(method, sessionInfo);
    }

    /**
     * @see RepositoryService#update(SessionInfo, NodeId, String)
     */
    public void update(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName) throws RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        String workspUri = uriResolver.getWorkspaceUri(srcWorkspaceName);

        update(uri, null, new String[] {workspUri}, UpdateInfo.UPDATE_BY_WORKSPACE, false, sessionInfo);
    }

    /**
     * @see RepositoryService#clone(SessionInfo, String, NodeId, NodeId, Name, boolean)
     */
    public void clone(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, Name destName, boolean removeExisting) throws RepositoryException {
        // TODO: missing implementation
        throw new UnsupportedOperationException("Missing implementation");
    }

    /**
     * @see RepositoryService#getLockInfo(SessionInfo, NodeId)
     */
    public LockInfo getLockInfo(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        // set of Dav-properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(DavPropertyName.LOCKDISCOVERY);
        nameSet.add(JcrRemotingConstants.JCR_PARENT_LN, ItemResourceConstants.NAMESPACE);

        PropFindMethod method = null;
        try {
            String uri = getItemUri(nodeId, sessionInfo);
            method = new PropFindMethod(uri, nameSet, DEPTH_0);
            // TODO: not correct. pass tokens in order avoid new session to be created TOBEFIXED
            initMethod(method, sessionInfo, true);

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length != 1) {
                throw new ItemNotFoundException("Unable to retrieve the LockInfo. No such node " + saveGetIdString(nodeId, sessionInfo));
            }

            DavPropertySet ps = responses[0].getProperties(DavServletResponse.SC_OK);
            if (ps.contains(DavPropertyName.LOCKDISCOVERY)) {
                DavProperty<?> p = ps.get(DavPropertyName.LOCKDISCOVERY);
                LockDiscovery ld = LockDiscovery.createFromXml(p.toXml(DomUtil.createDocument()));
                NodeId parentId = getParentId(ps, sessionInfo);
                return retrieveLockInfo(ld, sessionInfo, nodeId, parentId);
            }  else {
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
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#lock(SessionInfo, NodeId, boolean, boolean)
     */
    public LockInfo lock(SessionInfo sessionInfo, NodeId nodeId, boolean deep,
                         boolean sessionScoped) throws RepositoryException {
        return lock(sessionInfo, nodeId, deep, sessionScoped, Long.MAX_VALUE, null);
    }

    /**
     * @see RepositoryService#lock(SessionInfo, NodeId, boolean, boolean, long, String)
     */
    public LockInfo lock(SessionInfo sessionInfo, NodeId nodeId, boolean deep, boolean sessionScoped, long timeoutHint, String ownerHint) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        try {
            checkSessionInfo(sessionInfo);
            long davTimeout = (timeoutHint == Long.MAX_VALUE) ? INFINITE_TIMEOUT : timeoutHint*1000;
            String ownerInfo = (ownerHint == null) ? sessionInfo.getUserID() : ownerHint;

            String uri = getItemUri(nodeId, sessionInfo);
            Scope scope = (sessionScoped) ? ItemResourceConstants.EXCLUSIVE_SESSION : Scope.EXCLUSIVE;
            LockMethod method = new LockMethod(uri, scope, Type.WRITE, ownerInfo, davTimeout , deep);
            execute(method, sessionInfo);

            String lockToken = method.getLockToken();
            ((SessionInfoImpl) sessionInfo).addLockToken(lockToken, sessionScoped);

            LockDiscovery disc = method.getResponseAsLockDiscovery();
            return retrieveLockInfo(disc, sessionInfo, nodeId, null);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        }
    }

    /**
     * @see RepositoryService#refreshLock(SessionInfo, NodeId)
     */
    public void refreshLock(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        checkSessionInfo(sessionInfo);
        String uri = getItemUri(nodeId, sessionInfo);
        // since sessionInfo does not allow to retrieve token by NodeId,
        // pass all available lock tokens to the LOCK method (TODO: correct?)
        Set<String> allLockTokens = ((SessionInfoImpl) sessionInfo).getAllLockTokens();
        String[] locktokens = allLockTokens.toArray(new String[allLockTokens.size()]);
        LockMethod method = new LockMethod(uri, INFINITE_TIMEOUT, locktokens);
        execute(method, sessionInfo);
    }

    /**
     * @see RepositoryService#unlock(SessionInfo, NodeId)
     */
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

        UnLockMethod method = new UnLockMethod(uri, lockToken);
        execute(method, sessionInfo);

        ((SessionInfoImpl) sessionInfo).removeLockToken(lockToken, isSessionScoped);
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
        if (activeLock.isDeep() && parentId != null) {
            // try if lock is inherited
            LockInfo pLockInfo = getLockInfo(sessionInfo, parentId);
            if (pLockInfo != null) {
                return pLockInfo;
            }
        }
        // no deep lock or parentID == null or lock is not present on parent
        // -> nodeID is lockHolding Id.
        return new LockInfoImpl(activeLock, nodeId, ((SessionInfoImpl)sessionInfo).getAllLockTokens());
    }

    /**
     * @see RepositoryService#checkin(SessionInfo, NodeId)
     */
    public NodeId checkin(SessionInfo sessionInfo, NodeId nodeId) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        CheckinMethod method = new CheckinMethod(uri);
        execute(method, sessionInfo);
        Header rh = method.getResponseHeader(DeltaVConstants.HEADER_LOCATION);
        return uriResolver.getNodeId(rh.getValue(), sessionInfo);
    }

    /**
     * @see RepositoryService#checkout(SessionInfo, NodeId)
     */
    public void checkout(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        CheckoutMethod method = new CheckoutMethod(uri);
        execute(method, sessionInfo);
    }

    /**
     * @see RepositoryService#checkout(SessionInfo, NodeId, NodeId)
     */
    public void checkout(SessionInfo sessionInfo, NodeId nodeId, NodeId activityId) throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        if (activityId == null) {
            checkout(sessionInfo, nodeId);
        } else {
            // TODO
            throw new UnsupportedOperationException("JCR-2104: JSR 283 Versioning. Implementation missing");
        }
    }

    /**
     * @see RepositoryService#checkpoint(SessionInfo, NodeId)
     */
    public NodeId checkpoint(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, RepositoryException {
        // TODO review again.
        NodeId vID = checkin(sessionInfo, nodeId);
        checkout(sessionInfo, nodeId);
        return vID;
    }

    /**
     * @see RepositoryService#checkpoint(SessionInfo, NodeId, NodeId)
     */
    public NodeId checkpoint(SessionInfo sessionInfo, NodeId nodeId, NodeId activityId) throws UnsupportedRepositoryOperationException, RepositoryException {
        if (activityId == null) {
            return checkpoint(sessionInfo, nodeId);
        } else {
            // TODO
            throw new UnsupportedOperationException("JCR-2104: JSR 283 Versioning. Implementation missing");
        }
    }

    /**
     * @see RepositoryService#removeVersion(SessionInfo, NodeId, NodeId)
     */
    public void removeVersion(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId) throws RepositoryException {
        String uri = getItemUri(versionId, sessionInfo);
        DeleteMethod method = new DeleteMethod(uri);
        execute(method, sessionInfo);
    }

    /**
     * @see RepositoryService#restore(SessionInfo, NodeId, NodeId, boolean)
     */
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

        update(uri, relPath, new String[] {vUri}, UpdateInfo.UPDATE_BY_VERSION, removeExisting, sessionInfo);
    }

    private boolean exists(SessionInfo sInfo, String uri) {
        HeadMethod method = new HeadMethod(uri);
        try {
            int statusCode = getClient(sInfo).executeMethod(method);
            if (statusCode == DavServletResponse.SC_OK) {
                return true;
            }
        } catch (IOException e) {
            log.error("Unexpected error while testing existence of item.",e);
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        } finally {
            method.releaseConnection();
        }
        return false;
    }


    /**
     * @see RepositoryService#restore(SessionInfo, NodeId[], boolean)
     */
    public void restore(SessionInfo sessionInfo, NodeId[] versionIds, boolean removeExisting) throws RepositoryException {
        String uri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());
        String[] vUris = new String[versionIds.length];
        for (int i = 0; i < versionIds.length; i++) {
            vUris[i] = getItemUri(versionIds[i], sessionInfo);
        }

        update(uri, null, vUris, UpdateInfo.UPDATE_BY_VERSION, removeExisting, sessionInfo);
    }

    private void update(String uri, Path relPath, String[] updateSource, int updateType, boolean removeExisting, SessionInfo sessionInfo) throws RepositoryException {
        try {
            UpdateInfo uInfo;
            if (removeExisting || relPath != null) {
                Element uElem = UpdateInfo.createUpdateElement(updateSource, updateType, DomUtil.createDocument());
                if (removeExisting) {
                    DomUtil.addChildElement(uElem, JcrRemotingConstants.XML_REMOVEEXISTING, ItemResourceConstants.NAMESPACE);
                }
                if (relPath != null) {
                    DomUtil.addChildElement(uElem, JcrRemotingConstants.XML_RELPATH, ItemResourceConstants.NAMESPACE, getNamePathResolver(sessionInfo).getJCRPath(relPath));
                }

                uInfo = new UpdateInfo(uElem);
            } else {
                uInfo = new UpdateInfo(updateSource, updateType, new DavPropertyNameSet());
            }

            UpdateMethod method = new UpdateMethod(uri, uInfo);
            execute(method, sessionInfo);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (ParserConfigurationException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        }
    }

    /**
     * @see RepositoryService#merge(SessionInfo, NodeId, String, boolean)
     */
    public Iterator<NodeId> merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        return merge(sessionInfo, nodeId, srcWorkspaceName, bestEffort, false);
    }

    /**
     * @see RepositoryService#merge(SessionInfo, NodeId, String, boolean, boolean)
     */
    public Iterator<NodeId> merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName, boolean bestEffort, boolean isShallow) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        try {
            Document doc = DomUtil.createDocument();
            String wspHref = uriResolver.getWorkspaceUri(srcWorkspaceName);
            Element mElem = MergeInfo.createMergeElement(new String[] {wspHref}, !bestEffort, false, doc);
            if (isShallow) {
                mElem.appendChild(DomUtil.depthToXml(false, doc));
            }
            MergeInfo mInfo = new MergeInfo(mElem);

            MergeMethod method = new MergeMethod(getItemUri(nodeId, sessionInfo), mInfo);
            execute(method, sessionInfo);

            MultiStatusResponse[] resps = method.getResponseBodyAsMultiStatus().getResponses();
            List<NodeId> failedIds = new ArrayList<NodeId>(resps.length);
            for (MultiStatusResponse resp : resps) {
                String href = resp.getHref();
                failedIds.add(uriResolver.getNodeId(href, sessionInfo));
            }
            return failedIds.iterator();
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (ParserConfigurationException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        }
    }

    /**
     * @see RepositoryService#resolveMergeConflict(SessionInfo, NodeId, NodeId[], NodeId[])
     */
    public void resolveMergeConflict(SessionInfo sessionInfo, NodeId nodeId, NodeId[] mergeFailedIds, NodeId[] predecessorIds) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
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

            PropPatchMethod method = new PropPatchMethod(getItemUri(nodeId, sessionInfo), changeList);
            execute(method, sessionInfo);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see RepositoryService#addVersionLabel(SessionInfo,NodeId,NodeId,Name,boolean)
     */
    public void addVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, Name label, boolean moveLabel) throws VersionException, RepositoryException {
        try {
            String uri = getItemUri(versionId, sessionInfo);
            String strLabel = getNamePathResolver(sessionInfo).getJCRName(label);
            LabelMethod method = new LabelMethod(uri, strLabel, (moveLabel) ? LabelInfo.TYPE_SET : LabelInfo.TYPE_ADD);
            execute(method, sessionInfo);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see RepositoryService#removeVersionLabel(SessionInfo,NodeId,NodeId,Name)
     */
    public void removeVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, Name label) throws VersionException, RepositoryException {
        try {
            String uri = getItemUri(versionId, sessionInfo);
            String strLabel = getNamePathResolver(sessionInfo).getJCRName(label);
            LabelMethod method = new LabelMethod(uri, strLabel, LabelInfo.TYPE_REMOVE);
            execute(method, sessionInfo);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see RepositoryService#createActivity(SessionInfo, String)
     */
    public NodeId createActivity(SessionInfo sessionInfo, String title) throws UnsupportedRepositoryOperationException, RepositoryException {
        // TODO
        throw new UnsupportedOperationException("JCR-2104: JSR 283 Versioning. Implementation missing");
    }

    /**
     * @see RepositoryService#removeActivity(SessionInfo, NodeId)
     */
    public void removeActivity(SessionInfo sessionInfo, NodeId activityId) throws UnsupportedRepositoryOperationException, RepositoryException {
        // TODO
        throw new UnsupportedOperationException("JCR-2104: JSR 283 Versioning. Implementation missing");
    }

    /**
     * @see RepositoryService#mergeActivity(SessionInfo, NodeId)
     */
    public Iterator<NodeId> mergeActivity(SessionInfo sessionInfo, NodeId activityId) throws UnsupportedRepositoryOperationException, RepositoryException {
        // TODO
        throw new UnsupportedOperationException("JCR-2104: JSR 283 Versioning. Implementation missing");
    }

    /**
     * @see RepositoryService#createConfiguration(SessionInfo, NodeId)
     */
    public NodeId createConfiguration(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, RepositoryException {
        // TODO
        throw new UnsupportedOperationException("JCR-2104: JSR 283 Versioning. Implementation missing");
    }

    /**
     * @see RepositoryService#getSupportedQueryLanguages(SessionInfo)
     */
    public String[] getSupportedQueryLanguages(SessionInfo sessionInfo) throws RepositoryException {
        OptionsMethod method = new OptionsMethod(uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName()));
        try {
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            Header daslHeader = method.getResponseHeader(SearchConstants.HEADER_DASL);
            CodedUrlHeader h = new CodedUrlHeader(daslHeader.getName(), daslHeader.getValue());
            return h.getCodedUrls();
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            method.releaseConnection();
        }
    }

    public String[] checkQueryStatement(SessionInfo sessionInfo,
                                    String statement,
                                    String language,
                                    Map<String, String> namespaces)
            throws InvalidQueryException, RepositoryException {
        // TODO implement
        return new String[0];
    }

    /**
     * @see RepositoryService#executeQuery(SessionInfo, String, String,java.util.Map,long,long,java.util.Map
     */
    public QueryInfo executeQuery(SessionInfo sessionInfo, String statement, String language, Map<String, String> namespaces, long limit, long offset, Map<String, QValue> values) throws RepositoryException {
        SearchMethod method = null;
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

            method = new SearchMethod(uri, sInfo);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            MultiStatus ms = method.getResponseBodyAsMultiStatus();
            NamePathResolver resolver = getNamePathResolver(sessionInfo);
            return new QueryInfoImpl(ms, idFactory, resolver, valueFactory, getQValueFactory());
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        }  finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#createEventFilter(SessionInfo, int, Path, boolean, String[], Name[], boolean)
     */
    public EventFilter createEventFilter(SessionInfo sessionInfo,
                                         int eventTypes,
                                         Path absPath,
                                         boolean isDeep,
                                         String[] uuids,
                                         Name[] nodeTypeNames,
                                         boolean noLocal)
            throws UnsupportedRepositoryOperationException, RepositoryException {
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

    /**
     * @see RepositoryService#getEvents(Subscription, long)
     */
    public EventBundle[] getEvents(Subscription subscription, long timeout)
            throws RepositoryException, UnsupportedRepositoryOperationException {
        checkSubscription(subscription);

        EventSubscriptionImpl subscr = (EventSubscriptionImpl) subscription;
        String rootUri = uriResolver.getRootItemUri(subscr.getSessionInfo().getWorkspaceName());

        return poll(rootUri, subscr.getId(), timeout, subscr.getSessionInfo());
    }

    /**
     * @see RepositoryService#getEvents(SessionInfo, EventFilter, long)
     */
    public EventBundle getEvents(SessionInfo sessionInfo, EventFilter filter, long after) throws RepositoryException,
            UnsupportedRepositoryOperationException {
        // TODO: use filters remotely (JCR-3179)

        GetMethod method = null;
        String rootUri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());
        rootUri += "?type=journal"; // TODO should have a way to discover URI template

        try {
            method = new GetMethod(rootUri);
            method.addRequestHeader("If-None-Match", "\"" + Long.toHexString(after) + "\""); // TODO
            initMethod(method, sessionInfo);

            getClient(sessionInfo).executeMethod(method);
            assert method.getStatusCode() == 200;

            InputStream in = method.getResponseBodyAsStream();
            Document doc = null;
            if (in != null) {
                // read response and try to build a xml document
                try {
                    doc = DomUtil.parseDocument(in);
                } catch (ParserConfigurationException e) {
                    IOException exception = new IOException("XML parser configuration error");
                    exception.initCause(e);
                    throw exception;
                } catch (SAXException e) {
                    IOException exception = new IOException("XML parsing error");
                    exception.initCause(e);
                    throw exception;
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
                    List<Event> el = buildEventList(contentElem, (SessionInfoImpl) sessionInfo);
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
            throw new RepositoryException(ex);
        }
    }

    /**
     * @see RepositoryService#createSubscription(SessionInfo, EventFilter[])
     */
    public Subscription createSubscription(SessionInfo sessionInfo,
                                           EventFilter[] filters)
            throws UnsupportedRepositoryOperationException, RepositoryException {

        checkSessionInfo(sessionInfo);
        String rootUri = uriResolver.getRootItemUri(sessionInfo.getWorkspaceName());
        String subscriptionId = subscribe(rootUri, S_INFO, null, sessionInfo, null);
        log.debug("Subscribed on server for session info " + sessionInfo);

        try {
            checkEventFilterSupport(filters);
        }
        catch (UnsupportedRepositoryOperationException ex) {
            unsubscribe(rootUri, subscriptionId, sessionInfo);
            throw (ex);
        }
        return new EventSubscriptionImpl(subscriptionId, (SessionInfoImpl) sessionInfo);
    }

    /**
     * @see RepositoryService#updateEventFilters(Subscription, EventFilter[])
     */
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
        SubscribeMethod method = null;
        try {
            if (subscriptionId != null) {
                method = new SubscribeMethod(uri, subscriptionInfo, subscriptionId);
            } else {
                method = new SubscribeMethod(uri, subscriptionInfo);
            }
            initMethod(method, sessionInfo);

            if (batchId != null) {
                // add batchId as separate header
                CodedUrlHeader ch = new CodedUrlHeader(TransactionConstants.HEADER_TRANSACTIONID, batchId);
                method.setRequestHeader(ch.getHeaderName(), ch.getHeaderValue());
            }

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            org.apache.jackrabbit.webdav.observation.Subscription[] subs = method.getResponseAsSubscriptionDiscovery().getValue();
            if (subs.length == 1) {
                this.remoteServerProvidesNodeTypes = subs[0].eventsProvideNodeTypeInformation();
                this.remoteServerProvidesNoLocalFlag = subs[0].eventsProvideNoLocalFlag();
            }

            return method.getSubscriptionId();
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        }  finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    private void unsubscribe(String uri, String subscriptionId, SessionInfo sessionInfo) throws RepositoryException {
        UnSubscribeMethod method = null;
        try {
            method = new UnSubscribeMethod(uri, subscriptionId);
            initMethod(method, sessionInfo);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        }  finally {
            if (method != null) {
                method.releaseConnection();
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
        PollMethod method = null;
        try {
            method = new PollMethod(uri, subscriptionId, timeout);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            EventDiscovery disc = method.getResponseAsEventDiscovery();
            EventBundle[] events;
            if (disc.isEmpty()) {
                events = new EventBundle[0];
            } else {
                Element discEl = disc.toXml(DomUtil.createDocument());
                ElementIterator it = DomUtil.getChildren(discEl,
                        ObservationConstants.XML_EVENTBUNDLE,
                        ObservationConstants.NAMESPACE);
                List<EventBundle> bundles = new ArrayList<EventBundle>();
                while (it.hasNext()) {
                    Element bundleElement = it.nextElement();
                    String value = DomUtil.getAttribute(bundleElement,
                            ObservationConstants.XML_EVENT_LOCAL,
                            ObservationConstants.NAMESPACE);
                    // check if it matches a batch id recently submitted
                    boolean isLocal = false;
                    if (value != null) {
                        isLocal = Boolean.parseBoolean(value);
                    }
                    bundles.add(new EventBundleImpl(
                            buildEventList(bundleElement, sessionInfo),
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
        }  finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    private List<Event> buildEventList(Element bundleElement, SessionInfoImpl sessionInfo) throws IllegalNameException, NamespaceException {
        List<Event> events = new ArrayList<Event>();
        ElementIterator eventElementIterator = DomUtil.getChildren(bundleElement, ObservationConstants.XML_EVENT, ObservationConstants.NAMESPACE);

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
            Element typeEl = DomUtil.getChildElement(evElem, ObservationConstants.XML_EVENTTYPE, ObservationConstants.NAMESPACE);
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
                try {
                    eventPath = uriResolver.getQPath(href, sessionInfo);
                } catch (RepositoryException e) {
                    // should not occur
                    log.error("Internal error while building Event", e.getMessage());
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
                            log.warn("Unable to build event itemId: ",
                                    e.getMessage());
                        }
                    }
                }

                String parentHref = Text.getRelativeParent(href, 1, true);
                try {
                    parentId = uriResolver.getNodeId(parentHref, sessionInfo);
                } catch (RepositoryException e) {
                    log.warn("Unable to build event parentId: ", e.getMessage());
                }
                
            }

            if (userId == null) {
                // user id not retrieved from container
                userId = DomUtil.getChildTextTrim(evElem, ObservationConstants.XML_EVENTUSERID, ObservationConstants.NAMESPACE);
            }

            events.add(new EventImpl(eventId, eventPath, parentId, type, userId, evElem,
                    getNamePathResolver(sessionInfo), getQValueFactory()));
        }

        return events;
    }

    /**
     * @see RepositoryService#getRegisteredNamespaces(SessionInfo)
     */
    public Map<String, String> getRegisteredNamespaces(SessionInfo sessionInfo) throws RepositoryException {
        ReportInfo info = new ReportInfo(JcrRemotingConstants.REPORT_REGISTERED_NAMESPACES, ItemResourceConstants.NAMESPACE);
        ReportMethod method = null;
        try {
            method = new ReportMethod(uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName()), info);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            Document doc = method.getResponseBodyAsDocument();
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
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#getNamespaceURI(SessionInfo, String)
     */
    public String getNamespaceURI(SessionInfo sessionInfo, String prefix)
            throws NamespaceException, RepositoryException {
        try {
            return nsCache.getURI(prefix);
        } catch (NamespaceException e) {
            // refresh namespaces and try again
            getRegisteredNamespaces(sessionInfo);
            return nsCache.getURI(prefix);
        }
    }

    /**
     * @see RepositoryService#getNamespacePrefix(SessionInfo, String)
     */
    public String getNamespacePrefix(SessionInfo sessionInfo, String uri)
            throws NamespaceException, RepositoryException {
        try {
            return nsCache.getPrefix(uri);
        } catch (NamespaceException e) {
            // refresh namespaces and try again
            getRegisteredNamespaces(sessionInfo);
            return nsCache.getPrefix(uri);
        }
    }

    /**
     * @see RepositoryService#registerNamespace(SessionInfo, String, String)
     */
    public void registerNamespace(SessionInfo sessionInfo, String prefix, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
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

    /**
     * @see RepositoryService#unregisterNamespace(SessionInfo, String)
     */
    public void unregisterNamespace(SessionInfo sessionInfo, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
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
    private void internalSetNamespaces(SessionInfo sessionInfo, Map<String, String> namespaces) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        DavPropertySet setProperties = new DavPropertySet();
        setProperties.add(createNamespaceProperty(namespaces));

        PropPatchMethod method = null;
        try {
            String uri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());

            method = new PropPatchMethod(uri, setProperties, new DavPropertyNameSet());
            initMethod(method, sessionInfo, true);

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#getQNodeTypeDefinitions(SessionInfo)
     */
    public Iterator<QNodeTypeDefinition> getQNodeTypeDefinitions(SessionInfo sessionInfo) throws RepositoryException {       
        ReportMethod method = null;
        try {
            ReportInfo info = new ReportInfo(JcrRemotingConstants.REPORT_NODETYPES, ItemResourceConstants.NAMESPACE);
            info.setContentElement(DomUtil.createElement(DomUtil.createDocument(), NodeTypeConstants.XML_REPORT_ALLNODETYPES, ItemResourceConstants.NAMESPACE));

            String workspaceUri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());
            method = new ReportMethod(workspaceUri, info);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            Document reportDoc = method.getResponseBodyAsDocument();
            return retrieveQNodeTypeDefinitions(sessionInfo, reportDoc);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (ParserConfigurationException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<QNodeTypeDefinition> getQNodeTypeDefinitions(SessionInfo sessionInfo, Name[] nodetypeNames) throws RepositoryException {
        // in order to avoid individual calls for every nodetype, retrieve
        // the complete set from the server (again).
        return getQNodeTypeDefinitions(sessionInfo);
    }

    /**
     * {@inheritDoc}
     */
    public void registerNodeTypes(SessionInfo sessionInfo, QNodeTypeDefinition[] nodeTypeDefinitions, boolean allowUpdate) throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException, RepositoryException {
        PropPatchMethod method = null;
     	try {
             DavPropertySet setProperties = new DavPropertySet();
             setProperties.add(createRegisterNodeTypesProperty(sessionInfo, nodeTypeDefinitions, allowUpdate));
             String uri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());
             method = new PropPatchMethod(uri, setProperties, new DavPropertyNameSet());
             initMethod(method, sessionInfo, true);
             getClient(sessionInfo).executeMethod(method);
             method.checkSuccess();
         } catch (IOException e) {
             throw new RepositoryException(e);
         } catch (DavException e) {
             throw ExceptionConverter.generate(e);
         } finally {
             if (method != null) {
                 method.releaseConnection();
             }
         }
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterNodeTypes(SessionInfo sessionInfo, Name[] nodeTypeNames) throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException, RepositoryException {
        PropPatchMethod method = null;
     	try {
             DavPropertySet setProperties = new DavPropertySet();
             setProperties.add(createUnRegisterNodeTypesProperty(sessionInfo, nodeTypeNames));
             String uri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());
             method = new PropPatchMethod(uri, setProperties, new DavPropertyNameSet());
             initMethod(method, sessionInfo, true);
             getClient(sessionInfo).executeMethod(method);
             method.checkSuccess();
         } catch (IOException e) {
             throw new RepositoryException(e);
         } catch (DavException e) {
             throw ExceptionConverter.generate(e);
         } finally {
             if (method != null) {
                 method.releaseConnection();
             }
         }
    }

    /**
     * {@inheritDoc}
     */
    public void createWorkspace(SessionInfo sessionInfo, String name, String srcWorkspaceName) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        if (srcWorkspaceName != null) {
            throw new UnsupportedOperationException("JCR-2003. Implementation missing");
        }

        MkWorkspaceMethod method = null;
     	try {
             method = new MkWorkspaceMethod(uriResolver.getWorkspaceUri(name));
             initMethod(method, sessionInfo, true);
             getClient(sessionInfo).executeMethod(method);
             method.checkSuccess();
         } catch (IOException e) {
             throw new RepositoryException(e);
         } catch (DavException e) {
             throw ExceptionConverter.generate(e);
         } finally {
             if (method != null) {
                 method.releaseConnection();
             }
         }
    }

    /**
     * {@inheritDoc}
     */
    public void deleteWorkspace(SessionInfo sessionInfo, String name) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        DeleteMethod method = null;
     	try {
             method = new DeleteMethod(uriResolver.getWorkspaceUri(name));
             initMethod(method, sessionInfo, true);
             getClient(sessionInfo).executeMethod(method);
             method.checkSuccess();
         } catch (IOException e) {
             throw new RepositoryException(e);
         } catch (DavException e) {
             throw ExceptionConverter.generate(e);
         } finally {
             if (method != null) {
                 method.releaseConnection();
             }
         }
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

    /**
     * The XML elements and attributes used in serialization
     */
    private static final Namespace SV_NAMESPACE = Namespace.getNamespace(Name.NS_SV_PREFIX, Name.NS_SV_URI);
    private static final String NODE_ELEMENT = "node";
    private static final String PROPERTY_ELEMENT = "property";
    private static final String VALUE_ELEMENT = "value";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String TYPE_ATTRIBUTE = "type";

    //------------------------------------------------< Inner Class 'Batch' >---
    private class BatchImpl implements Batch {

        private final SessionInfo sessionInfo;
        private final ItemId targetId;
        private final List<DavMethod> methods = new ArrayList<DavMethod>();
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
            try {
                // start special 'lock'
                LockMethod method = new LockMethod(uri, TransactionConstants.LOCAL, TransactionConstants.TRANSACTION, null, INFINITE_TIMEOUT, true);
                initMethod(method, sessionInfo, true);

                HttpClient client = getClient(sessionInfo);
                client.executeMethod(method);
                if (method.getStatusCode() == DavServletResponse.SC_PRECONDITION_FAILED) {
                    throw new InvalidItemStateException("Unable to persist transient changes.");
                }
                method.checkSuccess();

                batchId = method.getLockToken();

                return client;
            } catch (IOException e) {
                throw new RepositoryException(e);
            } catch (DavException e) {
                throw ExceptionConverter.generate(e);
            }
        }

        private void end(HttpClient client, boolean commit) throws RepositoryException {
            checkConsumed();

            String uri = getItemUri(targetId, sessionInfo);
            UnLockMethod method = null;
            try {
                // make sure the lock initially created is removed again on the
                // server, asking the server to persist the modifications
                method = new UnLockMethod(uri, batchId);
                initMethod(method, sessionInfo, true);

                // in contrast to standard UNLOCK, the tx-unlock provides a
                // request body.
                method.setRequestBody(new TransactionInfo(commit));
                client.executeMethod(method);
                method.checkSuccess();
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
                if (method != null) {
                    // release UNLOCK method
                    method.releaseConnection();
                }
            }
        }

        private void dispose() {
            methods.clear();
            isConsumed = true;
        }

        private void checkConsumed() {
            if (isConsumed) {
                throw new IllegalStateException("Batch has already been consumed.");
            }
        }

        private boolean isEmpty() {
            return methods.isEmpty();
        }

        private Iterator<DavMethod> methods() {
            return methods.iterator();
        }

        //----------------------------------------------------------< Batch >---
        /**
         * @see Batch#addNode(NodeId, Name, Name, String)
         */
        public void addNode(NodeId parentId, Name nodeName, Name nodetypeName, String uuid) throws RepositoryException {
            checkConsumed();
            try {
                // TODO: TOBEFIXED. WebDAV does not allow MKCOL for existing resource -> problem with SNS
                // use fake name instead (see also #importXML)
                Name fakeName = getNameFactory().create(Name.NS_DEFAULT_URI, UUID.randomUUID().toString());
                String uri = getItemUri(parentId, fakeName, sessionInfo);
                MkColMethod method = new MkColMethod(uri);

                // build 'sys-view' for the node to create and append it as request body
                Document body = DomUtil.createDocument();
                Element nodeElement = DomUtil.addChildElement(body, NODE_ELEMENT, SV_NAMESPACE);
                String nameAttr = resolver.getJCRName(nodeName);
                DomUtil.setAttribute(nodeElement, NAME_ATTRIBUTE, SV_NAMESPACE, nameAttr);

                // nodetype must never be null
                Element propElement = DomUtil.addChildElement(nodeElement, PROPERTY_ELEMENT, SV_NAMESPACE);
                String name = resolver.getJCRName(NameConstants.JCR_PRIMARYTYPE);
                DomUtil.setAttribute(propElement, NAME_ATTRIBUTE, SV_NAMESPACE, name);
                DomUtil.setAttribute(propElement, TYPE_ATTRIBUTE, SV_NAMESPACE, PropertyType.nameFromValue(PropertyType.NAME));
                name = resolver.getJCRName(nodetypeName);
                DomUtil.addChildElement(propElement, VALUE_ELEMENT, SV_NAMESPACE, name);
                // optional uuid
                if (uuid != null) {
                    propElement = DomUtil.addChildElement(nodeElement, PROPERTY_ELEMENT, SV_NAMESPACE);
                    name = resolver.getJCRName(NameConstants.JCR_UUID);
                    DomUtil.setAttribute(propElement, NAME_ATTRIBUTE, SV_NAMESPACE, name);
                    DomUtil.setAttribute(propElement, TYPE_ATTRIBUTE, SV_NAMESPACE, PropertyType.nameFromValue(PropertyType.STRING));
                    DomUtil.addChildElement(propElement, VALUE_ELEMENT, SV_NAMESPACE, uuid);
                }
                method.setRequestBody(body);

                methods.add(method);
            } catch (IOException e) {
                throw new RepositoryException(e);
            } catch (ParserConfigurationException e) {
                throw new RepositoryException(e);
            }
        }

        /**
         * @see Batch#addProperty(NodeId, Name, QValue)
         */
        public void addProperty(NodeId parentId, Name propertyName, QValue value) throws RepositoryException {
            checkConsumed();
            String uri = getItemUri(parentId, propertyName, sessionInfo);

            PutMethod method = new PutMethod(uri);
            method.setRequestHeader(HEADER_CONTENT_TYPE, JcrValueType.contentTypeFromType(value.getType()));
            method.setRequestEntity(getEntity(value));
            methods.add(method);
        }

        /**
         * @see Batch#addProperty(NodeId, Name, QValue[])
         */
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
                PutMethod method = new PutMethod(uri);
                method.setRequestBody(vp);

                methods.add(method);
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }

        /**
         * @see Batch#setValue(PropertyId, QValue)
         */
        public void setValue(PropertyId propertyId, QValue value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            if (value == null) {
                // setting property value to 'null' is identical to a removal
                remove(propertyId);
            } else {
                RequestEntity ent = getEntity(value);
                String uri = getItemUri(propertyId, sessionInfo);
                // TODO: use PUT in order to avoid the ValuesProperty-PROPPATCH call.
                // TODO: actually not quite correct for PROPPATCH assert that prop really exists.
                PutMethod method = new PutMethod(uri);
                method.setRequestHeader(HEADER_CONTENT_TYPE, JcrValueType.contentTypeFromType(value.getType()));
                method.setRequestEntity(ent);
                methods.add(method);
            }
        }

        /**
         * @see Batch#setValue(PropertyId, QValue[])
         */
        public void setValue(PropertyId propertyId, QValue[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
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
                    PropPatchMethod method = new PropPatchMethod(uri, setProperties, new DavPropertyNameSet());
                    methods.add(method);
                } catch (IOException e) {
                    throw new RepositoryException(e);
                }
            }
        }

        private RequestEntity getEntity(QValue value) throws RepositoryException {
            // SPI value must be converted to jcr value
            InputStream in;
            int type = value.getType();
            String contentType = JcrValueType.contentTypeFromType(type);
            RequestEntity ent;
            try {
                switch (type) {
                    case PropertyType.NAME:
                    case PropertyType.PATH:
                        String str = ValueFormat.getJCRString(value, resolver);
                        ent = new StringRequestEntity(str, contentType, "UTF-8");
                        break;
                    case PropertyType.BINARY:
                        in = value.getStream();
                        ent = new InputStreamRequestEntity(in, contentType);
                        break;
                    default:
                        str = value.getString();
                        ent = new StringRequestEntity(str, contentType, "UTF-8");
                        break;
                }
            } catch (UnsupportedEncodingException e) {
                // should never get here
                throw new RepositoryException(e.getMessage());
            }
            return ent;
        }

        /**
         * @see Batch#remove(ItemId)
         */
        public void remove(ItemId itemId) throws RepositoryException {
            checkConsumed();
            String uri = getItemUri(itemId, sessionInfo);
            DeleteMethod method = new DeleteMethod(uri);

            methods.add(method);
            if (itemId.getPath() == null) {
                clear = true;
            }
        }

        /**
         * @see Batch#reorderNodes(NodeId, NodeId, NodeId)
         */
        public void reorderNodes(NodeId parentId, NodeId srcNodeId, NodeId beforeNodeId) throws RepositoryException {
            checkConsumed();
            try {
                String uri = getItemUri(parentId, sessionInfo);
                String srcUri = getItemUri(srcNodeId, sessionInfo);
                String srcSegment = Text.getName(srcUri, true);

                OrderPatchMethod method;
                if (beforeNodeId == null) {
                    // move src to the end
                    method = new OrderPatchMethod(uri, OrderingConstants.ORDERING_TYPE_CUSTOM, srcSegment, false);
                } else {
                    // insert src before the targetSegment
                    String beforeUri = getItemUri(beforeNodeId, sessionInfo);
                    String targetSegment = Text.getName(beforeUri, true);
                    method = new OrderPatchMethod(uri, OrderingConstants.ORDERING_TYPE_CUSTOM, srcSegment, targetSegment, true);
                }
                methods.add(method);
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }

        /**
         * @see Batch#setMixins(NodeId, Name[])
         */
        public void setMixins(NodeId nodeId, Name[] mixinNodeTypeIds) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
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
                PropPatchMethod method = new PropPatchMethod(uri, setProperties, removeProperties);

                methods.add(method);
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }

        /**
         * @see Batch#setPrimaryType(NodeId, Name)
         */
        public void setPrimaryType(NodeId nodeId, Name primaryNodeTypeName) throws RepositoryException {
            checkConsumed();
            try {
                DavPropertySet setProperties = new DavPropertySet();
                setProperties.add(createNodeTypeProperty(JcrRemotingConstants.JCR_PRIMARYNODETYPE_LN, new String[] {resolver.getJCRName(primaryNodeTypeName)}));

                String uri = getItemUri(nodeId, sessionInfo);
                PropPatchMethod method = new PropPatchMethod(uri, setProperties, new DavPropertyNameSet());

                methods.add(method);
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }

        /**
         * @see Batch#move(NodeId, NodeId, Name)
         */
        public void move(NodeId srcNodeId, NodeId destParentNodeId, Name destName) throws RepositoryException {
            checkConsumed();
            String uri = getItemUri(srcNodeId, sessionInfo);
            String destUri = getItemUri(destParentNodeId, destName, sessionInfo);
            MoveMethod method = new MoveMethod(uri, destUri, false);

            methods.add(method);
            clear = true;
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

        /**
         * @inheritDoc
         */
        public String getURI(String prefix) throws NamespaceException {
            try {
                return getNamespaceURI(sessionInfo, prefix);
            } catch (RepositoryException e) {
                String msg = "Error retrieving namespace uri";
                throw new NamespaceException(msg, e);
            }
        }

        /**
         * @inheritDoc
         */
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

        /**
         * @inheritDoc
         */
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

        /**
         * @inheritDoc
         */
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

        /**
         * @inheritDoc
         */
        public Name getQName(String jcrName) throws IllegalNameException, NamespaceException {
            return nResolver.getQName(jcrName);
        }

        /**
         * @inheritDoc
         */
        public String getJCRName(Name qName) throws NamespaceException {
            return nResolver.getJCRName(qName);
        }

        /**
         * @inheritDoc
         */
        public Path getQPath(String path) throws MalformedPathException, IllegalNameException, NamespaceException {
            return pResolver.getQPath(path);
        }

        /**
         * @inheritDoc
         */
        public Path getQPath(String path, boolean normalizeIdentifier) throws MalformedPathException, IllegalNameException, NamespaceException {
            return pResolver.getQPath(path, normalizeIdentifier);
        }

        /**
         * @inheritDoc
         */
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

        public String getURI(String prefix) throws NamespaceException {
            String uri = prefixToURI.get(prefix);
            if (uri != null) {
                return uri;
            } else {
                throw new NamespaceException(prefix + ": is not a registered namespace prefix.");
            }
        }

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
