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

import org.apache.http.HttpResponse;
import org.apache.jackrabbit.commons.webdav.JcrRemotingConstants;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.HttpPropfind;
import org.apache.jackrabbit.webdav.client.methods.HttpReport;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.w3c.dom.Document;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>URIResolverImpl</code>...
 */
class URIResolverImpl implements URIResolver {

    private final URI repositoryUri;
    private final RepositoryServiceImpl service;
    private final Document domFactory;

    // TODO: to-be-fixed. uri/id-caches don't get updated
    // for each workspace a separate idUri-cache is created
    private final Map<String, IdURICache> idURICaches = new HashMap<String, IdURICache>();

    URIResolverImpl(URI repositoryUri, RepositoryServiceImpl service, Document domFactory) {
        this.repositoryUri = repositoryUri;
        this.service = service;
        this.domFactory = domFactory;
    }

    private IdURICache getCache(String workspaceName) {
        if (idURICaches.containsKey(workspaceName)) {
            return idURICaches.get(workspaceName);
        } else {
            IdURICache c = new IdURICache(getWorkspaceUri(workspaceName));
            idURICaches.put(workspaceName, c);
            return c;
        }
    }

    String getRepositoryUri() {
        return repositoryUri.toASCIIString();
    }

    String getWorkspaceUri(String workspaceName) {
        String workspaceUri = getRepositoryUri();
        if (workspaceName != null) {
            workspaceUri += Text.escape(workspaceName);
        }
        return workspaceUri;
    }

    String getRootItemUri(String workspaceName) {
        return getWorkspaceUri(workspaceName) + Text.escapePath(JcrRemotingConstants.ROOT_ITEM_RESOURCEPATH);
    }

    String getItemUri(ItemId itemId, String workspaceName, SessionInfo sessionInfo)
            throws RepositoryException {
        IdURICache cache = getCache(workspaceName);
        // check if uri is available from cache
        if (cache.containsItemId(itemId)) {
            return cache.getUri(itemId);
        } else {
            StringBuffer uriBuffer = new StringBuffer();

            Path path = itemId.getPath();
            String uniqueID = itemId.getUniqueID();

            // resolver uuid part
            if (uniqueID != null) {
                ItemId uuidId = (path == null) ? itemId : service.getIdFactory().createNodeId(uniqueID);
                if (path != null && cache.containsItemId(uuidId)) {
                    // append uri of parent node, that is already cached
                    uriBuffer.append(cache.getUri(uuidId));
                } else {
                    // try to request locate-by-uuid report to build the uri
                    ReportInfo rInfo = new ReportInfo(JcrRemotingConstants.REPORT_LOCATE_BY_UUID, ItemResourceConstants.NAMESPACE);
                    rInfo.setContentElement(DomUtil.hrefToXml(uniqueID, domFactory));

                    HttpReport request = null;
                    try {
                        String wspUri = getWorkspaceUri(workspaceName);
                        request = new HttpReport(wspUri, rInfo);

                        HttpResponse response = service.executeRequest(sessionInfo, request);
                        request.checkSuccess(response);

                        MultiStatus ms = request.getResponseBodyAsMultiStatus(response);

                        if (ms.getResponses().length == 1) {
                            String absoluteUri = resolve(wspUri, ms.getResponses()[0].getHref());
                            uriBuffer.append(absoluteUri);
                            cache.add(absoluteUri, uuidId);
                        } else {
                            throw new ItemNotFoundException("Cannot identify item with uniqueID " + uniqueID);
                        }

                    } catch (IOException e) {
                        throw new RepositoryException(e.getMessage());
                    } catch (DavException e) {
                        throw ExceptionConverter.generate(e);
                    } finally {
                        if (request != null) {
                            request.releaseConnection();
                        }
                    }
                }
            } else {
                // start build uri from root-item
                uriBuffer.append(getRootItemUri(workspaceName));
            }
            // resolve relative-path part unless it denotes the root-item
            if (path != null && !path.denotesRoot()) {
                String jcrPath = service.getNamePathResolver(sessionInfo).getJCRPath(path);
                if (!path.isAbsolute() && !uriBuffer.toString().endsWith("/")) {
                    uriBuffer.append("/");
                }
                uriBuffer.append(Text.escapePath(jcrPath));
            }
            String itemUri = uriBuffer.toString();
            if (!cache.containsItemId(itemId)) {
                cache.add(itemUri, itemId);
            }
            return itemUri;
        }
    }

    /**
     * Resolve the given href obtained from multistatus against base URI
     */
    private static String resolve(String wspUri, String href) throws RepositoryException {
        try {
            java.net.URI base = new java.net.URI(wspUri);
            java.net.URI rel = new java.net.URI(href);
            return base.resolve(rel).toString();
        }
        catch (URISyntaxException ex) {
            throw new RepositoryException(ex);
        }
    }

    protected NodeId buildNodeId(NodeId parentId, String baseUri, MultiStatusResponse response,
                       String workspaceName, NamePathResolver resolver) throws RepositoryException {
        IdURICache cache = getCache(workspaceName);

        NodeId nodeId;
        DavPropertySet propSet = response.getProperties(DavServletResponse.SC_OK);

        String uniqueID = service.getUniqueID(propSet);
        if (uniqueID != null) {
            nodeId = service.getIdFactory().createNodeId(uniqueID);
        } else {
            Name qName = service.getQName(propSet, resolver);
            if (NameConstants.ROOT.equals(qName)) {
                nodeId = service.getIdFactory().createNodeId((String) null, service.getPathFactory().getRootPath());
            } else {
                int index = service.getIndex(propSet);
                nodeId = service.getIdFactory().createNodeId(parentId, service.getPathFactory().create(qName, index));
            }
        }
        // cache
        cache.add(resolve(baseUri, response.getHref()), nodeId);
        return nodeId;
    }

    PropertyId buildPropertyId(NodeId parentId, MultiStatusResponse response,
                               String workspaceName, NamePathResolver resolver) throws RepositoryException {
        IdURICache cache = getCache(workspaceName);
        if (cache.containsUri(response.getHref())) {
            ItemId id = cache.getItemId(response.getHref());
            if (!id.denotesNode()) {
                return (PropertyId) id;
            }
        }

        try {
            DavPropertySet propSet = response.getProperties(DavServletResponse.SC_OK);
            Name name = resolver.getQName(propSet.get(JcrRemotingConstants.JCR_NAME_LN, ItemResourceConstants.NAMESPACE).getValue().toString());
            PropertyId propertyId = service.getIdFactory().createPropertyId(parentId, name);

            cache.add(response.getHref(), propertyId);
            return propertyId;
        } catch (NameException e) {
            throw new RepositoryException(e);
        }
    }

    void clearCacheEntries(ItemId itemId, SessionInfo sessionInfo) {
        IdURICache cache = getCache(sessionInfo.getWorkspaceName());
        if (cache.containsItemId(itemId)) {
            cache.remove(itemId);
        }
    }

    void clearCacheEntries(SessionInfo sessionInfo) {
        IdURICache cache = getCache(sessionInfo.getWorkspaceName());
        cache.clear();
    }

    private static boolean isSameURI(String uri1, String uri2) {
        return getCleanURI(uri1).equals(getCleanURI(uri2));

    }

    private static String getCleanURI(String uri) {
        if (uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        } else {
            return uri;
        }
    }

    private NodeId getNodeId(String uri, SessionInfo sessionInfo, boolean nodeIsGone) throws RepositoryException {

        IdURICache cache = getCache(sessionInfo.getWorkspaceName());
        if (cache.containsUri(uri)) {
            // id has been accessed before and is cached
            ItemId id = cache.getItemId(uri);
            if (id.denotesNode()) {
                return (NodeId) id;
            }
        }

        if (nodeIsGone) {
            throw new RepositoryException("Can't reconstruct nodeId from URI when the remote node is gone.");
        }

        // retrieve parentId from cache or by recursive calls
        NodeId parentId;
        if (isSameURI(uri, getRootItemUri(sessionInfo.getWorkspaceName()))) {
            parentId = null;
        } else {
            String parentUri = Text.getRelativeParent(uri, 1, true);
            parentId = getNodeId(parentUri, sessionInfo, false);
        }

        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(JcrRemotingConstants.JCR_UUID_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(JcrRemotingConstants.JCR_NAME_LN, ItemResourceConstants.NAMESPACE);
        nameSet.add(JcrRemotingConstants.JCR_INDEX_LN, ItemResourceConstants.NAMESPACE);
        HttpPropfind request = null;
        try {
            request = new HttpPropfind(uri, nameSet, DavConstants.DEPTH_0);

            HttpResponse response = service.executeRequest(sessionInfo, request);
            if (response.getStatusLine().getStatusCode() != DavServletResponse.SC_MULTI_STATUS) {
                throw new ItemNotFoundException("Unable to retrieve the node with id " + uri + ", response status was: "
                        + response.getStatusLine().getStatusCode());
            }
            MultiStatusResponse[] responses = request.getResponseBodyAsMultiStatus(response).getResponses();
            if (responses.length != 1) {
                throw new ItemNotFoundException("Unable to retrieve the node with id " + uri);
            }
            return buildNodeId(parentId, uri, responses[0], sessionInfo.getWorkspaceName(), service.getNamePathResolver(sessionInfo));
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

    //-------------------------------------------------------< URI resolver >---
    /**
     * @inheritDoc
     */
    public Path getQPath(String uri, SessionInfo sessionInfo) throws RepositoryException {
        String rootUri = getRootItemUri(sessionInfo.getWorkspaceName());
        String jcrPath;
        if (uri.startsWith(rootUri)) {
            jcrPath = uri.substring(rootUri.length());
        } else {
            // todo: probably rather an error?
            jcrPath = uri;
        }
        try {
            return service.getNamePathResolver(sessionInfo).getQPath(Text.unescape(jcrPath));
        } catch (NameException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @inheritDoc
     */
    public NodeId getNodeId(String uri, SessionInfo sessionInfo) throws RepositoryException {
        return getNodeId(uri, sessionInfo, false);
    }

    /**
     * @inheritDoc
     */
    public NodeId getNodeIdAfterEvent(String uri, SessionInfo sessionInfo, boolean nodeIsGone) throws RepositoryException {
        return getNodeId(uri, sessionInfo, nodeIsGone);
    }

    /**
     * @inheritDoc
     */
    public PropertyId getPropertyId(String uri, SessionInfo sessionInfo) throws RepositoryException {
        IdURICache cache = getCache(sessionInfo.getWorkspaceName());
        ItemId id = cache.getItemId(uri);
        if (id != null) {
            if (!id.denotesNode()) {
                return (PropertyId) id;
            }
        }

        // separate parent uri and property JCRName
        String parentUri = Text.getRelativeParent(uri, 1, true);
        // make sure propName is unescaped
        String propName = Text.unescape(Text.getName(uri, true));
        // retrieve parent node id
        NodeId parentId = getNodeId(parentUri, sessionInfo, false);
        // build property id
        try {
            Name name = service.getNamePathResolver(sessionInfo).getQName(propName);
            PropertyId propertyId = service.getIdFactory().createPropertyId(parentId, name);
            cache.add(uri, propertyId);

            return propertyId;
        } catch (NameException e) {
            throw new RepositoryException(e);
        }
    }
}
