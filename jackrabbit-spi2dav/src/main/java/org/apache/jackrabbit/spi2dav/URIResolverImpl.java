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

import org.apache.commons.httpclient.URI;
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
import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.ReportMethod;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.jcr.version.report.LocateByUuidReport;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.w3c.dom.Document;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import java.io.IOException;
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
    private final Map idURICaches = new HashMap();

    URIResolverImpl(URI repositoryUri, RepositoryServiceImpl service, Document domFactory) {
        this.repositoryUri = repositoryUri;
        this.service = service;
        this.domFactory = domFactory;
    }

    private IdURICache getCache(String workspaceName) {
        if (idURICaches.containsKey(workspaceName)) {
            return (IdURICache) idURICaches.get(workspaceName);
        } else {
            IdURICache c = new IdURICache(getWorkspaceUri(workspaceName));
            idURICaches.put(workspaceName, c);
            return c;
        }
    }

    String getRepositoryUri() {
        return repositoryUri.getEscapedURI();
    }

    String getWorkspaceUri(String workspaceName) {
        String workspaceUri = getRepositoryUri();
        if (workspaceName != null) {
            workspaceUri += Text.escape(workspaceName);
        }
        return workspaceUri;
    }

    String getRootItemUri(String workspaceName) {
        return getWorkspaceUri(workspaceName) + Text.escapePath(ItemResourceConstants.ROOT_ITEM_RESOURCEPATH);
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
                if (path != null & cache.containsItemId(uuidId)) {
                    // append uri of parent node, that is already cached
                    uriBuffer.append(cache.getUri(uuidId));
                } else {
                    // try to request locate-by-uuid report to build the uri
                    ReportInfo rInfo = new ReportInfo(LocateByUuidReport.LOCATE_BY_UUID_REPORT);
                    rInfo.setContentElement(DomUtil.hrefToXml(uniqueID, domFactory));

                    ReportMethod rm = null;
                    try {
                        String wspUri = getWorkspaceUri(workspaceName);
                        rm = new ReportMethod(wspUri, rInfo);

                        service.getClient(sessionInfo).executeMethod(rm);

                        MultiStatus ms = rm.getResponseBodyAsMultiStatus();
                        if (ms.getResponses().length == 1) {
                            uriBuffer.append(ms.getResponses()[0].getHref());
                            cache.add(ms.getResponses()[0].getHref(), uuidId);
                        } else {
                            throw new ItemNotFoundException("Cannot identify item with uniqueID " + uniqueID);
                        }

                    } catch (IOException e) {
                        throw new RepositoryException(e.getMessage());
                    } catch (DavException e) {
                        throw ExceptionConverter.generate(e);
                    } finally {
                        if (rm != null) {
                            rm.releaseConnection();
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

    NodeId buildNodeId(NodeId parentId, MultiStatusResponse response,
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
        cache.add(response.getHref(), nodeId);
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
            Name name = resolver.getQName(propSet.get(ItemResourceConstants.JCR_NAME).getValue().toString());
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
        IdURICache cache = getCache(sessionInfo.getWorkspaceName());
        if (cache.containsUri(uri)) {
            // id has been accessed before and is cached
            ItemId id = cache.getItemId(uri);
            if (id.denotesNode()) {
                return (NodeId) id;
            }
        }

        // retrieve parentId from cache or by recursive calls
        NodeId parentId;
        if (isSameURI(uri, getRootItemUri(sessionInfo.getWorkspaceName()))) {
            parentId = null;
        } else {
            String parentUri = Text.getRelativeParent(uri, 1, true);
            parentId = getNodeId(parentUri, sessionInfo);
        }

        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(ItemResourceConstants.JCR_UUID);
        nameSet.add(ItemResourceConstants.JCR_NAME);
        nameSet.add(ItemResourceConstants.JCR_INDEX);
        DavMethodBase method = null;
        try {
            method = new PropFindMethod(uri, nameSet, DavConstants.DEPTH_0);

            service.getClient(sessionInfo).executeMethod(method);
            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length != 1) {
                throw new ItemNotFoundException("Unable to retrieve the node with id " + uri);
            }
            return buildNodeId(parentId, responses[0], sessionInfo.getWorkspaceName(), service.getNamePathResolver(sessionInfo));

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
     * @inheritDoc
     */
    public PropertyId getPropertyId(String uri, SessionInfo sessionInfo) throws RepositoryException {
        IdURICache cache = getCache(sessionInfo.getWorkspaceName());
        if (cache.containsUri(uri)) {
            ItemId id = cache.getItemId(uri);
            if (!id.denotesNode()) {
                return (PropertyId) id;
            }
        }

        // separate parent uri and property JCRName
        String parentUri = Text.getRelativeParent(uri, 1, true);
        // make sure propName is unescaped
        String propName = Text.unescape(Text.getName(uri, true));
        // retrieve parent node id
        NodeId parentId = getNodeId(parentUri, sessionInfo);
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