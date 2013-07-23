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
package org.apache.jackrabbit.spi2davex;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.jcr.Credentials;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.PartBase;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.json.JsonParser;
import org.apache.jackrabbit.commons.json.JsonUtil;
import org.apache.jackrabbit.commons.webdav.JcrRemotingConstants;
import org.apache.jackrabbit.commons.webdav.JcrValueType;
import org.apache.jackrabbit.commons.webdav.ValueUtil;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.commons.ItemInfoCacheImpl;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.identifier.IdFactoryImpl;
import org.apache.jackrabbit.spi.commons.iterator.Iterators;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.spi2dav.ExceptionConverter;
import org.apache.jackrabbit.spi2dav.ItemResourceConstants;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.header.IfHeader;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>RepositoryServiceImpl</code>...
 */
public class RepositoryServiceImpl extends org.apache.jackrabbit.spi2dav.RepositoryServiceImpl {

    private static Logger log = LoggerFactory.getLogger(RepositoryServiceImpl.class);

    private static final String PARAM_DIFF = ":diff";
    private static final String PARAM_COPY = ":copy";
    private static final String PARAM_CLONE = ":clone";

    private static final char SYMBOL_ADD_NODE = '+';
    private static final char SYMBOL_MOVE = '>';
    private static final char SYMBOL_REMOVE = '-';
    private static final char SYMBOL_SET_PROPERTY = '^';

    private static final String ORDER_POSITION_LAST = "#last";
    private static final String ORDER_POSITION_BEFORE = "#before";

    private static final String DEFAULT_CHARSET = "UTF-8";

    private static final DavPropertyName JCR_TYPE =
            DavPropertyName.create(ItemResourceConstants.JCR_TYPE_LN, ItemResourceConstants.NAMESPACE);

    private static final DavPropertyName JCR_LENGTH =
            DavPropertyName.create(ItemResourceConstants.JCR_LENGTH_LN, ItemResourceConstants.NAMESPACE);

    private static final DavPropertyName JCR_LENGTHS =
            DavPropertyName.create(ItemResourceConstants.JCR_LENGTHS_LN, ItemResourceConstants.NAMESPACE);

    private static final DavPropertyName JCR_GET_STRING =
            DavPropertyName.create(ItemResourceConstants.JCR_GET_STRING_LN, ItemResourceConstants.NAMESPACE);

    private static final DavPropertyNameSet LAZY_PROPERTY_NAME_SET = new DavPropertyNameSet(){{
        add(JCR_TYPE);
        add(JCR_LENGTH);
        add(JCR_LENGTHS);
        add(JCR_GET_STRING);
    }};

    /**
     * base uri to the extended jcr-server that can handle the GET and POST
     * (or PATCH) requests sent by this service implementation.
     */
    private final String jcrServerURI;

    /**
     * the name of the default workspace or <code>null</code>.
     * NOTE: with JCR-1842 the RepositoryConfiguration doesn't provide the
     * default workspace name any more. In order to provide backwards
     * compatibility with jcr-server &lt; 1.5.0 the workspace name can be
     * passed to the RepositoryService implementation.
     */
    private final String defaultWorkspaceName;

    /**
     * The configuration map used to determine the maximal depth of child
     * items to be accessed upon a call to {@link #getNodeInfo(SessionInfo, NodeId)}.
     */
    private final BatchReadConfig batchReadConfig;

    private final Map<SessionInfo, QValueFactoryImpl> qvFactories = new HashMap<SessionInfo, QValueFactoryImpl>();

    /**
     * Same as {@link #RepositoryServiceImpl(String, String, BatchReadConfig, int, int))}
     * using <code>null</code> workspace name, {@link ItemInfoCacheImpl#DEFAULT_CACHE_SIZE)}
     * as size for the item cache and {@link #MAX_CONNECTIONS_DEFAULT} for the
     * maximum number of connections on the client.
     *
     * @param jcrServerURI The server uri.
     * @param batchReadConfig The batch read configuration.
     * @throws RepositoryException If an exception occurs.
     */
    public RepositoryServiceImpl(String jcrServerURI, BatchReadConfig batchReadConfig) throws RepositoryException {
        this(jcrServerURI, null, batchReadConfig, ItemInfoCacheImpl.DEFAULT_CACHE_SIZE);
    }

    /**
     * Same as {@link #RepositoryServiceImpl(String, String, BatchReadConfig, int, int))}
     * using {@link #MAX_CONNECTIONS_DEFAULT} for the maximum number of
     * connections on the client.
     *
     * @param jcrServerURI The server uri.
     * @param defaultWorkspaceName The default workspace name.
     * @param batchReadConfig The batch read configuration.
     * @param itemInfoCacheSize The size of the item info cache.
     * @throws RepositoryException If an exception occurs.
     */
    public RepositoryServiceImpl(String jcrServerURI, String defaultWorkspaceName,
                                 BatchReadConfig batchReadConfig, int itemInfoCacheSize) throws RepositoryException {
        this(jcrServerURI, defaultWorkspaceName, batchReadConfig, itemInfoCacheSize, MAX_CONNECTIONS_DEFAULT);
    }

    /**
     * Creates a new instance of this repository service.
     *
     * @param jcrServerURI The server uri.
     * @param defaultWorkspaceName The default workspace name.
     * @param batchReadConfig The batch read configuration.
     * @param itemInfoCacheSize The size of the item info cache.
     * @param maximumHttpConnections maximumHttpConnections A int &gt;0 defining
     * the maximum number of connections per host to be configured on
     * {@link org.apache.commons.httpclient.params.HttpConnectionManagerParams#setDefaultMaxConnectionsPerHost(int)}.
     * @throws RepositoryException If an exception occurs.
     */
    public RepositoryServiceImpl(String jcrServerURI, String defaultWorkspaceName,
                                 BatchReadConfig batchReadConfig, int itemInfoCacheSize,
                                 int maximumHttpConnections) throws RepositoryException {

        super(jcrServerURI, IdFactoryImpl.getInstance(), NameFactoryImpl.getInstance(),
                PathFactoryImpl.getInstance(), new QValueFactoryImpl(), itemInfoCacheSize, maximumHttpConnections);

        try {
            URI repositoryUri = computeRepositoryUri(jcrServerURI);
            this.jcrServerURI = repositoryUri.toString();
        } catch (URIException e) {
            throw new RepositoryException(e);
        }

        this.defaultWorkspaceName = defaultWorkspaceName;
        if (batchReadConfig == null) {
            this.batchReadConfig = new BatchReadConfig() {
                public int getDepth(Path path, PathResolver resolver) {
                    return 0;
                }
            };
        } else {
            this.batchReadConfig = batchReadConfig;
        }
    }

    private Path getPath(ItemId itemId, SessionInfo sessionInfo) throws RepositoryException {
        return getPath(itemId, sessionInfo, sessionInfo.getWorkspaceName());
    }

    private Path getPath(ItemId itemId, SessionInfo sessionInfo, String workspaceName) throws RepositoryException {
        if (itemId.denotesNode()) {
            Path p = itemId.getPath();
            String uid = itemId.getUniqueID();
            if (uid == null) {
                return p;
            } else {
                NamePathResolver resolver = getNamePathResolver(sessionInfo);
                String uri = super.getItemUri(itemId, sessionInfo, workspaceName);
                String rootUri = getRootURI(sessionInfo);
                String jcrPath;
                if (uri.startsWith(rootUri)) {
                    jcrPath = uri.substring(rootUri.length());
                } else {
                    log.warn("ItemURI " + uri + " doesn't start with rootURI (" + rootUri + ").");
                    // fallback:
                    // calculated uri does not start with the rootURI
                    // -> search /jcr:root and start sub-string behind.
                    String rootSegment = Text.escapePath(JcrRemotingConstants.ROOT_ITEM_RESOURCEPATH);
                    jcrPath = uri.substring(uri.indexOf(rootSegment) + rootSegment.length());
                }
                jcrPath = Text.unescape(jcrPath);
                return resolver.getQPath(jcrPath);
            }
        } else {
            PropertyId pId = (PropertyId) itemId;
            Path parentPath = getPath(pId.getParentId(), sessionInfo, workspaceName);
            return getPathFactory().create(parentPath, pId.getName(), true);
        }
    }

    private String getURI(Path path, SessionInfo sessionInfo) throws RepositoryException {
        StringBuilder sb = new StringBuilder(getRootURI(sessionInfo));
        String jcrPath = getNamePathResolver(sessionInfo).getJCRPath(path);
        sb.append(Text.escapePath(jcrPath));
        return sb.toString();
    }

    private String getURI(ItemId itemId, SessionInfo sessionInfo) throws RepositoryException {
        Path p = getPath(itemId, sessionInfo);
        if (p == null) {
            return super.getItemUri(itemId, sessionInfo);
        } else {
            return getURI(p, sessionInfo);
        }
    }

    private String getRootURI(SessionInfo sessionInfo) {
        StringBuilder sb = new StringBuilder(getWorkspaceURI(sessionInfo));
        sb.append(Text.escapePath(JcrRemotingConstants.ROOT_ITEM_RESOURCEPATH));
        return sb.toString();
    }

    private String getWorkspaceURI(SessionInfo sessionInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(jcrServerURI);
        sb.append(Text.escape(sessionInfo.getWorkspaceName()));
        return sb.toString();
    }

    /**
     * @see RepositoryService#getQValueFactory()
     */
    public QValueFactoryImpl getQValueFactory(SessionInfo sessionInfo) throws RepositoryException {
        QValueFactoryImpl qv;
        if (qvFactories.containsKey(sessionInfo)) {
            qv = qvFactories.get(sessionInfo);
        } else {
            ValueLoader loader = new ValueLoader(getClient(sessionInfo));
            qv = new QValueFactoryImpl(getNamePathResolver(sessionInfo), loader);
            qvFactories.put(sessionInfo, qv);
        }
        return qv;
    }

    //--------------------------------------------------< RepositoryService >---

    // exists && getPropertyInfo -> to be done
    // getNodeInfo: omitted for requires list of 'references'

    /**
     * If the specified <code>workspaceName</code> the default workspace name
     * is used for backwards compatibility with jackrabbit-jcr-server &lt; 1.5.0
     *
     * @see RepositoryService#obtain(Credentials, String)
     */
    @Override
    public SessionInfo obtain(Credentials credentials, String workspaceName)
            throws  RepositoryException {
        // for backwards compatibility with jcr-server < 1.5.0
        String wspName = (workspaceName == null) ? defaultWorkspaceName : workspaceName;
        return super.obtain(credentials, wspName);
    }

    /**
     * If the specified <code>workspaceName</code> the default workspace name
     * is used for backwards compatibility with jackrabbit-jcr-server &lt; 1.5.0
     *
     * @see RepositoryService#obtain(SessionInfo, String)
     */
    @Override
    public SessionInfo obtain(SessionInfo sessionInfo, String workspaceName)
            throws RepositoryException {
        // for backwards compatibility with jcr-server < 1.5.0
        String wspName = (workspaceName == null) ? defaultWorkspaceName : workspaceName;
        return super.obtain(sessionInfo, wspName);
    }

    /**
     * @see RepositoryService#dispose(SessionInfo)
     */
    @Override
    public void dispose(SessionInfo sessionInfo) throws RepositoryException {
        super.dispose(sessionInfo);
        // remove the qvalue factory created for the given SessionInfo from the
        // map of valuefactories.
        qvFactories.remove(sessionInfo);
    }

    /**
     * @see RepositoryService#getItemInfos(SessionInfo, ItemId)
     */
    @Override
    public Iterator<? extends ItemInfo> getItemInfos(SessionInfo sessionInfo, ItemId itemId) throws ItemNotFoundException, RepositoryException {
        if (!itemId.denotesNode()) {
            PropertyInfo propertyInfo = getPropertyInfo(sessionInfo, (PropertyId) itemId);
            return Iterators.singleton(propertyInfo);
        } else {
            NodeId nodeId = (NodeId) itemId;
            Path path = getPath(itemId, sessionInfo);
            String uri = getURI(path, sessionInfo);
            int depth = batchReadConfig.getDepth(path, this.getNamePathResolver(sessionInfo));

            GetMethod method = new GetMethod(uri + "." + depth + ".json");
            try {
                int statusCode = getClient(sessionInfo).executeMethod(method);
                if (statusCode == DavServletResponse.SC_OK) {
                    if (method.getResponseContentLength() == 0) {
                        // no JSON response -> no such node on the server
                        throw new ItemNotFoundException("No such item " + nodeId);
                    }

                    NamePathResolver resolver = getNamePathResolver(sessionInfo);
                    NodeInfoImpl nInfo = new NodeInfoImpl(nodeId, path);

                    ItemInfoJsonHandler handler = new ItemInfoJsonHandler(resolver, nInfo, getRootURI(sessionInfo), getQValueFactory(sessionInfo), getPathFactory(), getIdFactory());
                    JsonParser ps = new JsonParser(handler);
                    ps.parse(method.getResponseBodyAsStream(), method.getResponseCharSet());

                    Iterator<? extends ItemInfo> it = handler.getItemInfos();
                    if (!it.hasNext()) {
                        throw new ItemNotFoundException("No such node " + uri);
                    }
                    return handler.getItemInfos();
                } else {
                    throw ExceptionConverter.generate(new DavException(statusCode, "Unable to retrieve NodeInfo for " + uri), method);
                }
            } catch (HttpException e) {
                throw ExceptionConverter.generate(new DavException(method.getStatusCode(), "Unable to retrieve NodeInfo for " + uri));
            } catch (IOException e) {
                log.error("Internal error while retrieving NodeInfo.",e);
                throw new RepositoryException(e.getMessage());
            } finally {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#getPropertyInfo(SessionInfo, PropertyId)
     */
    @Override
    public PropertyInfo getPropertyInfo(SessionInfo sessionInfo, PropertyId propertyId) throws RepositoryException {
        Path p = getPath(propertyId, sessionInfo);
        String uri = getURI(p, sessionInfo);
        PropFindMethod method = null;
        try {
            method = new PropFindMethod(uri, LAZY_PROPERTY_NAME_SET, DavConstants.DEPTH_0);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length != 1) {
                throw new ItemNotFoundException("Unable to retrieve the PropertyInfo. No such property " + uri);
            }

            MultiStatusResponse response = responses[0];
            DavPropertySet props = response.getProperties(DavServletResponse.SC_OK);
            int propertyType = PropertyType.valueFromName(props.get(JCR_TYPE).getValue().toString());

            if (propertyType == PropertyType.BINARY) {
                DavProperty<?> lengthsProp = props.get(JCR_LENGTHS);
                if (lengthsProp != null) {
                    // multivalued binary property
                    long[] lengths = ValueUtil.lengthsFromXml(lengthsProp.getValue());
                    QValue[] qValues = new QValue[lengths.length];
                    for (int i = 0 ; i < lengths.length ; i ++) {
                        qValues[i] = getQValueFactory(sessionInfo).create(lengths[i], uri, i);
                    }
                    return new PropertyInfoImpl(propertyId, p, propertyType, qValues);
                } else {
                    // single valued binary property
                    long length = Long.parseLong(props.get(JCR_LENGTH).getValue().toString());
                    QValue qValue = getQValueFactory(sessionInfo).create(length, uri, QValueFactoryImpl.NO_INDEX) ;
                    return new PropertyInfoImpl(propertyId, p, propertyType, qValue);
                }
            } else if (props.contains(JCR_GET_STRING)) {
                // single valued non-binary property
                Object v = props.get(JCR_GET_STRING).getValue();
                String str = (v == null) ? "" : v.toString();
                QValue qValue = ValueFormat.getQValue(str, propertyType, getNamePathResolver(sessionInfo), getQValueFactory(sessionInfo));
                return new PropertyInfoImpl(propertyId, p, propertyType, qValue);
            } else {
                // multivalued non-binary property or some other property that
                // didn't expose the JCR_GET_STRING dav property.
                return super.getPropertyInfo(sessionInfo, propertyId);
            }
        } catch (IOException e) {
            log.error("Internal error while retrieving ItemInfo.",e);
            throw new RepositoryException(e.getMessage());
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    @Override
    public Batch createBatch(SessionInfo sessionInfo, ItemId itemId) throws RepositoryException {
        return new BatchImpl(itemId, sessionInfo);
    }

    @Override
    public void submit(Batch batch) throws RepositoryException {
        if (!(batch instanceof BatchImpl)) {
            throw new RepositoryException("Unknown Batch implementation.");
        }
        BatchImpl batchImpl = (BatchImpl) batch;
        try {
            if (!batchImpl.isEmpty()) {
                batchImpl.start();
            }
        } finally {
            batchImpl.dispose();
        }
    }

    @Override
    public void copy(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, Name destName) throws RepositoryException {
        if (srcWorkspaceName.equals(sessionInfo.getWorkspaceName())) {
            super.copy(sessionInfo, srcWorkspaceName, srcNodeId, destParentNodeId, destName);
            return;
        }
        PostMethod method = null;
        try {
            method = new PostMethod(getWorkspaceURI(sessionInfo));
            NamePathResolver resolver = getNamePathResolver(sessionInfo);

            StringBuilder args = new StringBuilder();
            args.append(srcWorkspaceName);
            args.append(",");
            args.append(resolver.getJCRPath(getPath(srcNodeId, sessionInfo, srcWorkspaceName)));
            args.append(",");
            String destParentPath = resolver.getJCRPath(getPath(destParentNodeId, sessionInfo));
            String destPath = (destParentPath.endsWith("/") ?
                    destParentPath + resolver.getJCRName(destName) :
                    destParentPath + "/" + resolver.getJCRName(destName));
            args.append(destPath);

            method.addParameter(PARAM_COPY, args.toString());
            addIfHeader(sessionInfo, method);
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

    @Override
    public void clone(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, Name destName, boolean removeExisting) throws RepositoryException {
        PostMethod method = null;
        try {
            method = new PostMethod(getWorkspaceURI(sessionInfo));

            NamePathResolver resolver = getNamePathResolver(sessionInfo);
            StringBuilder args = new StringBuilder();
            args.append(srcWorkspaceName);
            args.append(",");
            args.append(resolver.getJCRPath(getPath(srcNodeId, sessionInfo, srcWorkspaceName)));
            args.append(",");
            String destParentPath = resolver.getJCRPath(getPath(destParentNodeId, sessionInfo));
            String destPath = (destParentPath.endsWith("/") ?
                    destParentPath + resolver.getJCRName(destName) :
                    destParentPath + "/" + resolver.getJCRName(destName));
            args.append(destPath);
            args.append(",");
            args.append(Boolean.toString(removeExisting));

            method.addParameter(PARAM_CLONE, args.toString());
            addIfHeader(sessionInfo, method);
            getClient(sessionInfo).executeMethod(method);

            method.checkSuccess();
            if (removeExisting) {
                clearItemUriCache(sessionInfo);
            }
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

    private static void addIfHeader(SessionInfo sInfo, HttpMethod method) {
        try {
            initMethod(method, sInfo, true);
        } catch (RepositoryException e) {
            // should never get here
            log.error("Unable to retrieve lock tokens: omitted from request header.");
        }
    }

    //--------------------------------------------------------------------------

    private class BatchImpl implements Batch {

        private final ItemId targetId;
        private final SessionInfo sessionInfo;
        private final List<Part> parts;
        private final List<String> diff;
        /*
          If this batch needs to remove multiple same-name-siblings starting
          from lower index, the index of the following siblings must be reset
          in order to avoid PathNotFoundException.
        */
        private final Map<Path, Path> removed = new HashMap<Path, Path>();

        private PostMethod method; // TODO: use PATCH request instead.
        private boolean isConsumed;
        // flag to determine if the uri-lookup needs to be cleared... e.g.
        // after a move operation.
        private boolean clear;

        private BatchImpl(ItemId targetId, SessionInfo sessionInfo) {
            this.targetId = targetId;
            this.sessionInfo = sessionInfo;
            parts = new ArrayList<Part>();
            diff = new ArrayList<String>();
        }

        private void start() throws RepositoryException {
            checkConsumed();

            // add lock tokens
            addIfHeader(sessionInfo, method);

            // insert the content of 'batchMap' part containing the ordered list
            // of methods to be executed:
            StringBuilder buf = new StringBuilder();
            for (Iterator<String> it = diff.iterator(); it.hasNext();) {
                buf.append(it.next());
                if (it.hasNext()) {
                    buf.append("\r");
                }
            }

            if (parts.isEmpty()) {
                // only a diff part. no multipart required.
                method.addParameter(PARAM_DIFF, buf.toString());
            } else {
                // other parts are present -> add the diff part
                addPart(PARAM_DIFF, buf.toString());
                // ... and create multipart-entity (and set it to method)
                Part[] partArr = parts.toArray(new Part[parts.size()]);
                RequestEntity entity = new MultipartRequestEntity(partArr, method.getParams());
                method.setRequestEntity(entity);
            }

            HttpClient client = getClient(sessionInfo);
            try {
                client.executeMethod(method);
                method.checkSuccess();
                if (clear) {
                    RepositoryServiceImpl.super.clearItemUriCache(sessionInfo);
                }
            }  catch (IOException e) {
                throw new RepositoryException(e);
            } catch (DavException e) {
                throw ExceptionConverter.generate(e, method);
            } finally {
                method.releaseConnection();
            }
        }

        private void dispose() {
            method = null;
            isConsumed = true;
            // discard binary parts (JCR-2582)
            if (parts != null) {
                for (Part part : parts) {
                    if (part instanceof BinaryPart) {
                        ((BinaryPart) part).dispose();
                    }
                }
            }
        }

        private void checkConsumed() {
            if (isConsumed) {
                throw new IllegalStateException("Batch has already been consumed.");
            }
        }

        private boolean isEmpty() {
            return method == null;
        }

        private void assertMethod() throws RepositoryException {
            if (method == null) {
                String uri = getURI(targetId, sessionInfo);
                method = new PostMethod(uri);
                // ship lock-tokens as if-header to circumvent problems with
                // locks created by this session.
                String[] locktokens = sessionInfo.getLockTokens();
                if (locktokens != null && locktokens.length > 0) {
                    IfHeader ifH = new IfHeader(locktokens);
                    method.setRequestHeader(ifH.getHeaderName(), ifH.getHeaderValue());
                }
            }
        }

        //----------------------------------------------------------< Batch >---
        /**
         * @inheritDoc
         */
        public void addNode(NodeId parentId, Name nodeName, Name nodetypeName,
                            String uuid) throws RepositoryException {
            assertMethod();

            NamePathResolver resolver = getNamePathResolver(sessionInfo);
            Path p = getPathFactory().create(getPath(parentId, sessionInfo), nodeName, true);
            String jcrPath = resolver.getJCRPath(p);

            StringWriter wr = new StringWriter();
            wr.write('{');
            wr.write(getJsonKey(JcrConstants.JCR_PRIMARYTYPE));
            wr.write(JsonUtil.getJsonString(getNamePathResolver(sessionInfo).getJCRName(nodetypeName)));
            if (uuid != null) {
                wr.write(',');
                wr.write(getJsonKey(JcrConstants.JCR_UUID));
                wr.write(JsonUtil.getJsonString(uuid));
            }
            wr.write('}');
            appendDiff(SYMBOL_ADD_NODE, jcrPath, wr.toString());
        }

        /**
         * @inheritDoc
         */
        public void addProperty(NodeId parentId, Name propertyName, QValue value) throws RepositoryException {
            assertMethod();
            Path p = getPathFactory().create(getPath(parentId, sessionInfo), propertyName, true);
            setProperty(p, value, false);
        }

        /**
         * @inheritDoc
         */
        public void addProperty(NodeId parentId, Name propertyName, QValue[] values) throws RepositoryException {
            assertMethod();
            Path p = getPathFactory().create(getPath(parentId, sessionInfo), propertyName, true);
            setProperty(p, values, false);
        }

        /**
         * @inheritDoc
         */
        public void setValue(PropertyId propertyId, QValue value) throws RepositoryException {
            assertMethod();
            Path p = getPath(propertyId, sessionInfo);
            setProperty(p, value, true);
        }

        /**
         * @inheritDoc
         */
        public void setValue(PropertyId propertyId, QValue[] values) throws RepositoryException {
            assertMethod();
            Path p = getPath(propertyId, sessionInfo);
            setProperty(p, values, true);
        }

        /**
         * @inheritDoc
         */
        public void remove(ItemId itemId) throws RepositoryException {
            assertMethod();

            Path rmPath = getPath(itemId, sessionInfo);
            if (itemId.denotesNode()) {
                rmPath = calcRemovePath(rmPath);
            }
            String rmJcrPath = getNamePathResolver(sessionInfo).getJCRPath(rmPath);
            appendDiff(SYMBOL_REMOVE, rmJcrPath, null);

            // clear the uri-lookup in case the itemID contains a uniqueID part.
            if (itemId.getPath() == null) {
                clear = true;
            }
        }

        /**
         * @inheritDoc
         */
        public void reorderNodes(NodeId parentId, NodeId srcNodeId, NodeId beforeNodeId) throws RepositoryException {
            assertMethod();

            // TODO: multiple reorder of SNS nodes requires readjustment of path -> see remove()
            String srcPath = getNamePathResolver(sessionInfo).getJCRPath(getPath(srcNodeId, sessionInfo));

            StringBuilder val = new StringBuilder();
            if (beforeNodeId != null) {
                Path beforePath = getPath(beforeNodeId, sessionInfo);
                String beforeJcrPath = getNamePathResolver(sessionInfo).getJCRPath(beforePath);
                val.append(Text.getName(beforeJcrPath));
                val.append(ORDER_POSITION_BEFORE);
            } else {
                val.append(ORDER_POSITION_LAST);
            }
            appendDiff(SYMBOL_MOVE, srcPath, val.toString());

            // clear the uri-lookup in case the itemID contains a uniqueID part.
            if (srcNodeId.getPath() == null || (beforeNodeId != null && beforeNodeId.getPath() == null)) {
                clear = true;
            }
        }

        /**
         * @inheritDoc
         */
        public void setMixins(NodeId nodeId, Name[] mixinNodeTypeNames) throws RepositoryException {
            assertMethod();

            QValue[] vs = new QValue[mixinNodeTypeNames.length];
            for (int i = 0; i < mixinNodeTypeNames.length; i++) {
                vs[i] = getQValueFactory(sessionInfo).create(mixinNodeTypeNames[i]);
            }
            Path p = getPathFactory().create(getPath(nodeId, sessionInfo), NameConstants.JCR_MIXINTYPES, true);
            // register the diff entry including clearing previous calls to
            // setMixins for the same node.
            setProperty(p, vs, true);
        }

        /**
         * @inheritDoc
         */
        public void setPrimaryType(NodeId nodeId, Name primaryNodeTypeName) throws RepositoryException {
            assertMethod();

            QValue v = getQValueFactory(sessionInfo).create(primaryNodeTypeName);
            Path p = getPathFactory().create(getPath(nodeId, sessionInfo), NameConstants.JCR_PRIMARYTYPE, true);
            // register the diff entry including clearing previous calls to
            // setPrimaryType for the same node.
            setProperty(p, v, true);
        }

        /**
         * @inheritDoc
         */
        public void move(NodeId srcNodeId, NodeId destParentNodeId, Name destName) throws RepositoryException {
            assertMethod();

            String srcPath = getNamePathResolver(sessionInfo).getJCRPath(getPath(srcNodeId, sessionInfo));
            Path destPath = getPathFactory().create(getPath(destParentNodeId, sessionInfo), destName, true);
            String destJcrPath = getNamePathResolver(sessionInfo).getJCRPath(destPath);

            appendDiff(SYMBOL_MOVE, srcPath, destJcrPath);

            clear = true;
        }

        /**
         *
         * @param symbol
         * @param targetPath
         * @param value
         */
        private void appendDiff(char symbol, String targetPath, String value) {
            StringBuilder bf = new StringBuilder();
            bf.append(symbol).append(targetPath).append(" : ");
            if (value != null) {
                bf.append(value);
            }
            diff.add(bf.toString());
        }

        /**
         *
         * @param propPath
         * @param value
         * @throws RepositoryException
         */
        private void setProperty(Path propPath, QValue value, boolean clearPrevious) throws RepositoryException {
            NamePathResolver resolver = getNamePathResolver(sessionInfo);
            String jcrPropPath = resolver.getJCRPath(propPath);
            if (clearPrevious) {
                clearPreviousSetProperty(jcrPropPath);
            }

            String strValue = getJsonString(value);
            appendDiff(SYMBOL_SET_PROPERTY, jcrPropPath, strValue);
            if (strValue == null) {
                addPart(jcrPropPath, value, resolver);
            }
        }

        private void setProperty(Path propPath, QValue[] values, boolean clearPrevious) throws RepositoryException {
            NamePathResolver resolver = getNamePathResolver(sessionInfo);
            String jcrPropPath = resolver.getJCRPath(propPath);
            if (clearPrevious) {
                clearPreviousSetProperty(jcrPropPath);
            }

            StringBuilder strVal = new StringBuilder("[");
            for (int i = 0; i < values.length; i++) {
                String str = getJsonString(values[i]);
                if (str == null) {
                    addPart(jcrPropPath, values[i], resolver);
                } else {
                    String delim = (i == 0) ? "" : ",";
                    strVal.append(delim).append(str);
                }
            }
            strVal.append("]");
            appendDiff(SYMBOL_SET_PROPERTY, jcrPropPath, strVal.toString());
        }

        private void clearPreviousSetProperty(String jcrPropPath) {
            String key = SYMBOL_SET_PROPERTY + jcrPropPath + " : ";
            // make sure that multiple calls to setProperty for a given path
            // are only reflected once in the multipart, otherwise this will
            // cause consistency problems as the various calls cannot be separated
            // (missing unique identifier for the parts).
            for (Iterator<String> it = diff.iterator(); it.hasNext();) {
                String entry = it.next();
                if (entry.startsWith(key)) {
                    it.remove();
                    removeParts(jcrPropPath);
                    return;
                }
            }
        }

        /**
         *
         * @param paramName
         * @param value
         */
        private void addPart(String paramName, String value) {
            parts.add(new StringPart(paramName, value, DEFAULT_CHARSET));
        }

        /**
         *
         * @param paramName
         * @param value
         * @param resolver
         * @throws RepositoryException
         */
        private void addPart(String paramName, QValue value, NamePathResolver resolver) throws RepositoryException {
            Part part;
            switch (value.getType()) {
                case PropertyType.BINARY:
                    part = new BinaryPart(paramName, new BinaryPartSource(value), JcrValueType.contentTypeFromType(PropertyType.BINARY), DEFAULT_CHARSET);
                    break;
                case PropertyType.NAME:
                    part = new StringPart(paramName, resolver.getJCRName(value.getName()), DEFAULT_CHARSET);
                    break;
                case PropertyType.PATH:
                    part = new StringPart(paramName, resolver.getJCRPath(value.getPath()), DEFAULT_CHARSET);
                    break;
                default:
                    part = new StringPart(paramName, value.getString(), DEFAULT_CHARSET);
            }
            String ctype = JcrValueType.contentTypeFromType(value.getType());
            ((PartBase) part).setContentType(ctype);

            parts.add(part);
        }

        private void removeParts(String paramName) {
            for (Iterator<Part> it = parts.iterator(); it.hasNext();) {
                Part part = it.next();
                if (part.getName().equals(paramName)) {
                    it.remove();
                }
            }
        }

        private String getJsonKey(String str) {
            return JsonUtil.getJsonString(str) + ":";
        }

        private String getJsonString(QValue value) throws RepositoryException {
            String str;
            switch (value.getType()) {
                case PropertyType.STRING:
                    str = JsonUtil.getJsonString(value.getString());
                    break;
                case PropertyType.BOOLEAN:
                case PropertyType.LONG:
                    str = value.getString();
                    break;
                case PropertyType.DOUBLE:
                    double d = value.getDouble();
                    if (Double.isNaN(d) || Double.isInfinite(d)) {
                    // JSON cannot specifically handle this property type...
                        str = null;
                    } else {
                        str = value.getString();
                        if (str.indexOf('.') == -1) {
                            str += ".0";
                        }
                    }
                    break;
                default:
                    // JSON cannot specifically handle this property type...
                    str = null;
            }
            return str;
        }

        private Path calcRemovePath(Path removedNodePath) throws RepositoryException {
            removed.put(removedNodePath, removedNodePath);
            Name name = removedNodePath.getName();
            int index = removedNodePath.getNormalizedIndex();
            if (index > Path.INDEX_DEFAULT) {
                Path.Element[] elems = removedNodePath.getElements();
                PathBuilder pb = new PathBuilder();
                for (int i = 0; i <= elems.length - 2; i++) {
                    pb.addLast(elems[i]);
                }
                Path parent = pb.getPath();
                while (index > Path.INDEX_UNDEFINED) {
                    Path siblingP = getPathFactory().create(parent, name, --index, true);
                    if (removed.containsKey(siblingP)) {
                        // as the previous sibling has been remove -> the same index
                        // must be used to remove the node at removedNodePath.
                        siblingP = removed.get(siblingP);
                        removed.put(removedNodePath, siblingP);
                        return siblingP;
                    }
                }
            }
            // first of siblings or no sibling at all
            return removedNodePath;
        }
    }
}
