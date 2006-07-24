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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.HrefProperty;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.apache.jackrabbit.webdav.version.LabelInfo;
import org.apache.jackrabbit.webdav.version.UpdateInfo;
import org.apache.jackrabbit.webdav.version.MergeInfo;
import org.apache.jackrabbit.webdav.version.VersionControlledResource;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.ReportMethod;
import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;
import org.apache.jackrabbit.webdav.client.methods.OptionsMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.PropPatchMethod;
import org.apache.jackrabbit.webdav.client.methods.OrderPatchMethod;
import org.apache.jackrabbit.webdav.client.methods.CheckinMethod;
import org.apache.jackrabbit.webdav.client.methods.CheckoutMethod;
import org.apache.jackrabbit.webdav.client.methods.LockMethod;
import org.apache.jackrabbit.webdav.client.methods.UnLockMethod;
import org.apache.jackrabbit.webdav.client.methods.LabelMethod;
import org.apache.jackrabbit.webdav.client.methods.MoveMethod;
import org.apache.jackrabbit.webdav.client.methods.CopyMethod;
import org.apache.jackrabbit.webdav.client.methods.UpdateMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.client.methods.SearchMethod;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.MergeMethod;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.observation.SubscriptionInfo;
import org.apache.jackrabbit.webdav.observation.EventType;
import org.apache.jackrabbit.webdav.observation.Filter;
import org.apache.jackrabbit.webdav.observation.ObservationConstants;
import org.apache.jackrabbit.webdav.security.SecurityConstants;
import org.apache.jackrabbit.webdav.security.CurrentUserPrivilegeSetProperty;
import org.apache.jackrabbit.webdav.security.Privilege;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.lock.LockDiscovery;
import org.apache.jackrabbit.webdav.transaction.TransactionConstants;
import org.apache.jackrabbit.webdav.transaction.TransactionInfo;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.ElementIterator;
import org.apache.jackrabbit.webdav.xml.Namespace;
import org.apache.jackrabbit.webdav.header.CodedUrlHeader;
import org.apache.jackrabbit.webdav.header.IfHeader;
import org.apache.jackrabbit.webdav.header.DepthHeader;
import org.apache.jackrabbit.webdav.search.SearchConstants;
import org.apache.jackrabbit.webdav.jcr.version.report.RepositoryDescriptorsReport;
import org.apache.jackrabbit.webdav.jcr.version.report.RegisteredNamespacesReport;
import org.apache.jackrabbit.webdav.jcr.version.report.NodeTypesReport;
import org.apache.jackrabbit.webdav.jcr.version.report.LocateByUuidReport;
import org.apache.jackrabbit.webdav.jcr.nodetype.NodeTypeConstants;
import org.apache.jackrabbit.webdav.jcr.nodetype.NodeTypeProperty;
import org.apache.jackrabbit.webdav.jcr.property.ValuesProperty;
import org.apache.jackrabbit.webdav.jcr.property.NamespacesProperty;
import org.apache.jackrabbit.webdav.jcr.observation.SubscriptionImpl;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.name.AbstractNamespaceResolver;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.name.PathFormat;
import org.apache.jackrabbit.BaseException;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.QNodeTypeDefinitionIterator;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.EventListener;
import org.apache.jackrabbit.spi.EventIterator;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.value.ValueFormat;
import org.apache.jackrabbit.value.QValue;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import javax.jcr.RepositoryException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ItemExistsException;
import javax.jcr.AccessDeniedException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.MergeException;
import javax.jcr.NamespaceException;
import javax.jcr.Credentials;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.Collection;
import java.io.InputStream;
import java.io.IOException;

/**
 * <code>RepositoryServiceImpl</code>...
 */
// TODO: encapsulate URI building, escaping, unescaping...
// TODO: cache info objects
public class RepositoryServiceImpl extends AbstractNamespaceResolver implements URIResolver, RepositoryService, DavConstants {

    private static Logger log = LoggerFactory.getLogger(RepositoryServiceImpl.class);

    private final Document domFactory;

    private final HostConfiguration hostConfig;
    private final URI repositoryUri;

    // URI/ID resolution
    private final Map uriCache = new HashMap();
    private final Map idCache = new HashMap();

    private final IdFactory idFactory;
    private final ValueFactory valueFactory;

    // namespace resolver
    private Properties prefixToURI = new Properties();
    private Properties uriToPrefix = new Properties();

    public RepositoryServiceImpl(String uri, IdFactory idFactory, ValueFactory valueFactory) throws RepositoryException {
        if (uri == null || "".equals(uri)) {
            throw new RepositoryException("Invalid repository uri.");
        }
        if (idFactory == null) {
            throw new RepositoryException("IdFactory must not be null.");
        }
        this.idFactory = idFactory;
        this.valueFactory = valueFactory;

        try {
            hostConfig = new HostConfiguration();
            if (!uri.endsWith("/")) {
                uri += "/";
            }
            repositoryUri = new URI(uri);
            hostConfig.setHost(repositoryUri);

            domFactory = DomUtil.BUILDER_FACTORY.newDocumentBuilder().newDocument();
        } catch (URIException e) {
            throw new RepositoryException(e);
        } catch (ParserConfigurationException e) {
            throw new RepositoryException(e);
        }
    }

    private HttpClient getClient(Credentials credentials) {
        HttpClient client = new HttpClient();
        client.setHostConfiguration(hostConfig);
        UsernamePasswordCredentials creds;
        if (credentials == null) {
            creds = new UsernamePasswordCredentials(null, null);
        } else {
            // TODO properly build http client credentials
            creds = new UsernamePasswordCredentials(credentials.toString());
        }
        HttpState httpState = client.getState();
        httpState.setCredentials(null, hostConfig.getHost(), creds);
        return client;
    }

    private HttpClient getClient(SessionInfo sessionInfo) {
        Iterator allCreds = sessionInfo.getSubject().getPublicCredentials(Credentials.class).iterator();
        Credentials credentials = null;
        if (allCreds.hasNext()) {
            credentials = (Credentials) allCreds.next();
        }
        return getClient(credentials);
    }

    private String getRepositoryUri() {
        return repositoryUri.getEscapedURI();
    }

    private String getWorkspaceUri(String workspaceName) {
        String workspaceUri = getRepositoryUri();
        if (workspaceName != null) {
            workspaceUri += Text.escape(workspaceName);
        }
        return workspaceUri;
    }

    private String getRootItemUri(SessionInfo sessionInfo) {
        String rootUri = getWorkspaceUri(sessionInfo.getWorkspaceName()) + "/";
        return rootUri;
    }

    private String getItemUri(String workspaceId, ItemId itemId, SessionInfo sessionInfo) throws RepositoryException {
        String uri = getWorkspaceUri(workspaceId);
        String uuid = itemId.getUUID();
        if (uuid != null) {
            if (uriCache.containsKey(uuid)) {
                uri = (String) uriCache.get(uuid);
            } else {
                // retrieve info related to uuid using the locate-by-uuid report
                ReportInfo rInfo = new ReportInfo(LocateByUuidReport.LOCATE_BY_UUID_REPORT);
                rInfo.setContentElement(DomUtil.hrefToXml(uuid, domFactory));
                try {
                    ReportMethod rm = new ReportMethod(uri, rInfo);
                    getClient(sessionInfo).executeMethod(rm);
                    MultiStatus ms = rm.getResponseBodyAsMultiStatus();
                    uri = ms.getResponses()[0].getHref();
                    uriCache.put(uuid, uri);
                } catch (IOException e) {
                    throw new RepositoryException(e.getMessage());
                } catch (DavException e) {
                    throw ExceptionConverter.generate(e);
                }
            }
        }
        Path relativePath = itemId.getRelativePath();
        if (relativePath != null) {
            try {
                String jcrPath = PathFormat.format(relativePath, this);
                uri += Text.escapePath(jcrPath);
            } catch (NoPrefixDeclaredException e) {
                throw new RepositoryException(e);
            }
        }
        if (itemId.denotesNode() && !uri.endsWith("/")) {
            uri += "/";
        }
        return uri;
    }

    private String getItemUri(ItemId itemId, SessionInfo sessionInfo) throws RepositoryException {
        return getItemUri(sessionInfo.getWorkspaceName(), itemId, sessionInfo);
    }

    private String getItemUri(NodeId parentId, QName childName, SessionInfo sessionInfo) throws RepositoryException {
        String parentUri = getItemUri(parentId, sessionInfo);
        try {
            return parentUri + getJCRName(childName);
        } catch (NoPrefixDeclaredException e) {
            throw new RepositoryException(e);
        }
    }

    private static void initMethod(DavMethod method, SessionInfo sessionInfo, boolean addIfHeader) {
        if (addIfHeader) {
            String[] locktokens = sessionInfo.getLockTokens();
            // TODO: ev. build tagged if header
            if (locktokens != null && locktokens.length > 0) {
                IfHeader ifH = new IfHeader(locktokens);
                method.setRequestHeader(ifH.getHeaderName(), ifH.getHeaderValue());
            }
        }
        if (sessionInfo instanceof SessionInfoImpl) {
            String txId = ((SessionInfoImpl) sessionInfo).getBatchId();
            if (txId != null) {
                CodedUrlHeader ch = new CodedUrlHeader(TransactionConstants.HEADER_TRANSACTIONID, txId);
                method.setRequestHeader(ch.getHeaderName(), ch.getHeaderValue());
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
     * @see RepositoryService#getRepositoryDescriptors()
     */
    public Properties getRepositoryDescriptors() throws RepositoryException {
        ReportInfo info = new ReportInfo(RepositoryDescriptorsReport.REPOSITORY_DESCRIPTORS_REPORT, DavConstants.DEPTH_0);
        ReportMethod method = null;
        try {
            method = new ReportMethod(repositoryUri.getEscapedPath(), info);
            getClient((Credentials) null).executeMethod(method);
            method.checkSuccess();
            Document doc = method.getResponseBodyAsDocument();
            Properties descriptors = new Properties();
            if (doc != null) {
                Element rootElement = doc.getDocumentElement();
                ElementIterator nsElems = DomUtil.getChildren(rootElement, ItemResourceConstants.XML_DESCRIPTOR, ItemResourceConstants.NAMESPACE);
                while (nsElems.hasNext()) {
                    Element elem = nsElems.nextElement();
                    String key = DomUtil.getChildText(elem, ItemResourceConstants.XML_DESCRIPTORKEY, ItemResourceConstants.NAMESPACE);
                    String descriptor = DomUtil.getChildText(elem, ItemResourceConstants.XML_DESCRIPTORVALUE, ItemResourceConstants.NAMESPACE);
                    if (key != null && descriptor != null) {
                        descriptors.setProperty(key, descriptor);
                    } else {
                        log.error("Invalid descriptor key / descriptor pair: " + key + " -> " + descriptor);
                    }
                }
            }
            return descriptors;
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
     * @see RepositoryService#login(Credentials, String)
     */
    public SessionInfo login(Credentials credentials, String workspaceName) throws RepositoryException {
        // interested in workspace href property only, which allows to retrieve the
        // name of the workspace in case 'workspaceName' is 'null'.
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(DeltaVConstants.WORKSPACE);
        PropFindMethod method = null;
        try {
            method = new PropFindMethod(getWorkspaceUri(workspaceName), nameSet, DavConstants.DEPTH_0);
            getClient(credentials).executeMethod(method);
            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length != 1) {
                throw new RepositoryException("Unable to retrieve default workspace name.");
            }
            DavPropertySet props = responses[0].getProperties(DavServletResponse.SC_OK);
            if (props.contains(DeltaVConstants.WORKSPACE)) {
                String wspHref = new HrefProperty(props.get(DeltaVConstants.WORKSPACE)).getHrefs().get(0).toString();
                String wspName = Text.getName(wspHref, true);
                return new SessionInfoImpl(credentials, wspName);
            } else {
                throw new RepositoryException("Unable to retrieve default workspace name.");
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
    }

    /**
     * @see RepositoryService#getWorkspaceNames(SessionInfo)
     */
    public String[] getWorkspaceNames(SessionInfo sessionInfo) throws RepositoryException {
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(DeltaVConstants.WORKSPACE);
        PropFindMethod method = null;
        try {
            method = new PropFindMethod(getRepositoryUri(), nameSet, DEPTH_1);
            getClient(sessionInfo).executeMethod(method);
            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            Set ids = new HashSet();
            for (int i = 0; i < responses.length; i++) {
                DavPropertySet props = responses[i].getProperties(DavServletResponse.SC_OK);
                if (props.contains(DeltaVConstants.WORKSPACE)) {
                    HrefProperty hp = new HrefProperty(props.get(DeltaVConstants.WORKSPACE));
                    String wspHref = hp.getHrefs().get(0).toString();
                    String id = Text.getName(wspHref, true);
                    ids.add(id);
                }
            }
            return (String[]) ids.toArray(new String[ids.size()]);
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
        PropFindMethod method = null;
        try {
            DavPropertyNameSet propNameSet = new DavPropertyNameSet();
            propNameSet.add(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);

            String uri = getItemUri(itemId, sessionInfo);
            method = new PropFindMethod(uri, propNameSet, DepthHeader.DEPTH_0);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length < 1) {
                throw new ItemNotFoundException("Unable to retrieve permissions for item " + itemId);
            }
            DavProperty p = responses[0].getProperties(DavServletResponse.SC_OK).get(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
            if (p == null) {
                return false;
            }
            // build set of privileges from given actions. NOTE: since the actions
            // have no qualifying namespace, the {@link ItemResourceConstants#NAMESPACE}
            // is used. // TODO check if correct.
            Set requiredPrivileges = new HashSet();
            for (int i = 0; i < actions.length; i++) {
               requiredPrivileges.add(Privilege.getPrivilege(actions[i], ItemResourceConstants.NAMESPACE));
            }
            // build set of privileges granted to the current user.
            CurrentUserPrivilegeSetProperty privSet = new CurrentUserPrivilegeSetProperty(p);
            Collection privileges = (Collection) privSet.getValue();

            // check privileges present against required privileges.
            return privileges.containsAll(requiredPrivileges);
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
     * @see RepositoryService#getRootId(SessionInfo)
     */
    public NodeId getRootId(SessionInfo sessionInfo) throws RepositoryException {
        String rootUri = getRootItemUri(sessionInfo);
        if (idCache.containsKey(rootUri)) {
            return (NodeId) idCache.get(rootUri);
        } else {
            NodeId rootId = retrieveNodeId(rootUri, null, sessionInfo);
            return rootId;
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
        nameSet.add(ItemResourceConstants.JCR_DEFINITION);
        nameSet.add(DavPropertyName.RESOURCETYPE);

        DavMethodBase method = null;
        try {
            String uri = getItemUri(itemId, sessionInfo);
            method = new PropFindMethod(uri, nameSet, DEPTH_0);
            getClient(sessionInfo).executeMethod(method);

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length < 1) {
                throw new ItemNotFoundException("Unable to retrieve the item with id " + itemId);
            }
            // todo: make sure the server only sent a single response...
            // todo: make sure the resourcetype matches the itemId type
            QItemDefinition definition = null;
            DavPropertySet propertySet = responses[0].getProperties(DavServletResponse.SC_OK);
            if (propertySet.contains(ItemResourceConstants.JCR_DEFINITION)) {
                DavProperty prop = propertySet.get(ItemResourceConstants.JCR_DEFINITION);
                Object value = prop.getValue();
                if (value != null && value instanceof Element) {
                    Element idfElem = (Element) value;
                    if (itemId.denotesNode()) {
                        definition = new QNodeDefinitionImpl(null, idfElem, getNamespaceResolver());
                    } else {
                        definition = new QPropertyDefinitionImpl(null, idfElem, getNamespaceResolver());
                    }
                }
            }
            if (definition == null) {
                throw new RepositoryException("Unable to retrieve definition for item with id '" + itemId + "'.");
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
     * @see RepositoryService#exists(SessionInfo, ItemId)
     */
    public boolean exists(SessionInfo sessionInfo, ItemId itemId) throws RepositoryException {
        HeadMethod method = new HeadMethod(getItemUri(itemId, sessionInfo));
        try {
            int statusCode = getClient(sessionInfo).executeMethod(method);
            if (statusCode == DavServletResponse.SC_OK) {
                return true;
            } else if (statusCode == DavServletResponse.SC_NOT_FOUND) {
                return false;
            } else {
                String msg = "Unexpected status code ("+ statusCode +") while testing existence of item with id " + itemId;
                log.error(msg);
                throw new RepositoryException(msg);
            }
        } catch (IOException e) {
            log.error("Unexpected error while testing existence of item.",e);
            throw new RepositoryException(e);
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * @see RepositoryService#getNodeInfo(SessionInfo, NodeId)
     */
    public NodeInfo getNodeInfo(SessionInfo sessionInfo, NodeId nodeId) throws PathNotFoundException, RepositoryException {
        // set of properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(ItemResourceConstants.JCR_NAME);
        nameSet.add(ItemResourceConstants.JCR_INDEX);
        nameSet.add(ItemResourceConstants.JCR_PARENT);
        nameSet.add(ItemResourceConstants.JCR_PRIMARYNODETYPE);
        nameSet.add(ItemResourceConstants.JCR_MIXINNODETYPES);
        nameSet.add(ItemResourceConstants.JCR_REFERENCES);
        nameSet.add(ItemResourceConstants.JCR_UUID);
        nameSet.add(DavPropertyName.RESOURCETYPE);

        DavMethodBase method = null;
        try {
            String uri = getItemUri(nodeId, sessionInfo);
            method = new PropFindMethod(uri, nameSet, DEPTH_1);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length < 1) {
                throw new ItemNotFoundException("Unable to retrieve the node with id " + nodeId);
            }

            MultiStatusResponse nodeResponse = null;
            List childResponses = new ArrayList();

            for (int i = 0; i < responses.length; i++) {
                String respHref = responses[i].getHref();
                if (uri.equals(respHref)) {
                    nodeResponse = responses[i];
                } else {
                    childResponses.add(responses[i]);
                }
            }
            if (nodeResponse == null) {
                throw new ItemNotFoundException("Unable to retrieve the node with id " + nodeId);
            }
            NodeInfo nInfo = new NodeInfoImpl(nodeResponse, childResponses, getURIResolver(), sessionInfo);
            return nInfo;
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
    public PropertyInfo getPropertyInfo(SessionInfo sessionInfo, PropertyId propertyId) throws PathNotFoundException, RepositoryException {
        // set of Dav-properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(ItemResourceConstants.JCR_NAME);
        nameSet.add(ItemResourceConstants.JCR_PARENT);
        nameSet.add(ItemResourceConstants.JCR_TYPE);
        nameSet.add(ItemResourceConstants.JCR_VALUE);
        nameSet.add(ItemResourceConstants.JCR_VALUES);
        nameSet.add(DavPropertyName.RESOURCETYPE);

        PropFindMethod method = null;
        try {
            String uri = getItemUri(propertyId, sessionInfo);
            method = new PropFindMethod(uri, nameSet, DEPTH_1);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length < 1) {
                throw new ItemNotFoundException("Unable to retrieve the property with id " + propertyId);
            }

            PropertyInfo pInfo = new PropertyInfoImpl(responses[0], getURIResolver(), getNamespaceResolver(), sessionInfo);
            return pInfo;
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
     * @see RepositoryService#createBatch(SessionInfo)
     */
    public Batch createBatch(SessionInfo sessionInfo) throws RepositoryException {
        if (sessionInfo instanceof SessionInfoImpl) {
            return new BatchImpl((SessionInfoImpl)sessionInfo);
        } else {
            throw new RepositoryException("Unknown SessionInfo implementation.");
        }
    }

    /**
     * @see RepositoryService#submit(Batch)
     */
    public EventIterator submit(Batch batch) throws RepositoryException {
        if (!(batch instanceof BatchImpl)) {
            throw new RepositoryException("Unknown Batch implementation.");
        }
        BatchImpl batchImpl = (BatchImpl) batch;
        if (batchImpl.isEmpty()) {
            batchImpl.dispose();
            // TODO build empty eventIterator
            return null;
        }
        // send batched information
        try {
            HttpClient client = batchImpl.start();
            boolean success = false;
            try {
                Iterator it = batchImpl.methods();
                while (it.hasNext()) {
                    DavMethod method = (DavMethod) it.next();
                    client.executeMethod(method);
                    if (!(success = method.succeeded())) {
                        throw method.getResponseException();
                    }
                }
            } finally {
                // make sure the lock is removed. if any of the methods
                // failed the unlock is used to abort any pending changes
                // on the server.
                batchImpl.end(client, success);
            }
            // TODO retrieve events.
            return null;
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            batchImpl.dispose();
        }
    }

    /**
     * @see RepositoryService#importXml(SessionInfo, NodeId, InputStream, int)
     */
    public EventIterator importXml(SessionInfo sessionInfo, NodeId parentId, InputStream xmlStream, int uuidBehaviour) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        MkColMethod method = null;
        try {
            // TODO: improve. currently random name is built instead of retrieving name of new resource from top-level xml element within stream
            QName nodeName = new QName(QName.NS_DEFAULT_URI, UUID.randomUUID().toString());
            String uri = getItemUri(parentId, nodeName, sessionInfo);
            method = new MkColMethod(uri);
            initMethod(method, sessionInfo, true);
            method.setRequestBody(xmlStream);

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            // TODO: retrieve events
            return null;
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
     * @see RepositoryService#move(SessionInfo, NodeId, NodeId, QName)
     */
    public EventIterator move(SessionInfo sessionInfo, NodeId srcNodeId, NodeId destParentNodeId, QName destName) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        MoveMethod method = null;
        try {
            String uri = getItemUri(srcNodeId, sessionInfo);
            String destUri = getItemUri(destParentNodeId, destName, sessionInfo);
            method = new MoveMethod(uri, destUri, true);
            initMethod(method, sessionInfo, true);

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            // TODO: retrieve events
            return null;
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
     * @see RepositoryService#copy(SessionInfo, String, NodeId, NodeId, QName)
     */
    public EventIterator copy(SessionInfo sessionInfo, String srcWorkspaceId, NodeId srcNodeId, NodeId destParentNodeId, QName destName) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        CopyMethod method = null;
        try {
            String uri = getItemUri(srcWorkspaceId, srcNodeId, sessionInfo);
            String destUri = getItemUri(destParentNodeId, destName, sessionInfo);
            method = new CopyMethod(uri, destUri, true, false);
            initMethod(method, sessionInfo, true);

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            // TODO: retrieve events
            return null;
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
     * @see RepositoryService#update(SessionInfo, NodeId, String)
     */
    public EventIterator update(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceId) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        String workspUri = getWorkspaceUri(srcWorkspaceId);

        return update(uri, new String[] {workspUri}, UpdateInfo.UPDATE_BY_WORKSPACE, false, sessionInfo);
    }

    /**
     * @see RepositoryService#clone(SessionInfo, String, NodeId, NodeId, QName, boolean)
     */
    public EventIterator clone(SessionInfo sessionInfo, String srcWorkspaceId, NodeId srcNodeId, NodeId destParentNodeId, QName destName, boolean removeExisting) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        // TODO: missing implementation
        return null;
    }

    /**
     * @see RepositoryService#getLockInfo(SessionInfo, NodeId)
     */
    public LockInfo getLockInfo(SessionInfo sessionInfo, NodeId nodeId) throws LockException, RepositoryException {
        // set of Dav-properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(DavPropertyName.LOCKDISCOVERY);

        PropFindMethod method = null;
        try {
            String uri = getItemUri(nodeId, sessionInfo);
            method = new PropFindMethod(uri, nameSet, DEPTH_0);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length != 1) {
                throw new ItemNotFoundException("Unable to retrieve the property with id " + nodeId);
            }

            DavPropertySet ps = responses[0].getProperties(DavServletResponse.SC_OK);
            if (ps.contains(DavPropertyName.LOCKDISCOVERY)) {
                DavProperty p = ps.get(DavPropertyName.LOCKDISCOVERY);
                return new LockInfoImpl(LockDiscovery.createFromXml(p.toXml(domFactory)), nodeId);
            } else {
                throw new LockException("No Lock present on node with id " + nodeId);
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

    /**
     * @see RepositoryService#lock(SessionInfo, NodeId, boolean)
     */
    public EventIterator lock(SessionInfo sessionInfo, NodeId nodeId, boolean deep) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        LockMethod method = null;
        try {
            String uri = getItemUri(nodeId, sessionInfo);
            method = new LockMethod(uri, Scope.EXCLUSIVE, Type.WRITE, null, DavConstants.INFINITE_TIMEOUT, true);
            initMethod(method, sessionInfo, false);

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            String lockToken = method.getLockToken();
            sessionInfo.addLockToken(lockToken);

            // TODO: ev. need to take care of 'timeout' ?
            // TODO: ev. evaluate lock response, if depth and type is according to request?

            // TODO: retrieve events
            return null;
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
     * @see RepositoryService#refreshLock(SessionInfo, NodeId)
     */
    public EventIterator refreshLock(SessionInfo sessionInfo, NodeId nodeId) throws LockException, RepositoryException {
        LockMethod method = null;
        try {
            String uri = getItemUri(nodeId, sessionInfo);
            // since sessionInfo does not allow to retrieve token by NodeId,
            // pass all available lock tokens to the LOCK method (TODO: correct?)
            method = new LockMethod(uri, DavConstants.INFINITE_TIMEOUT, sessionInfo.getLockTokens());
            initMethod(method, sessionInfo, false);

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            // TODO: retrieve events
            return null;
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
     * @see RepositoryService#unlock(SessionInfo, NodeId)
     */
    public EventIterator unlock(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        UnLockMethod method = null;
        try {
            String uri = getItemUri(nodeId, sessionInfo);
            // Note: since sessionInfo does not allow to identify the id of the
            // lock holding node, we need to access the token via lockInfo
            // TODO: review this.
            LockInfo lInfo = getLockInfo(sessionInfo, nodeId);
            String lockToken = lInfo.getLockToken();

            // TODO: ev. additional check if lt is present on the sessionInfo?

            method = new UnLockMethod(uri, lockToken);
            initMethod(method, sessionInfo, false);

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            sessionInfo.removeLockToken(lockToken);

            // TODO: retrieve events
            return null;
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
     * @see RepositoryService#checkin(SessionInfo, NodeId)
     */
    public EventIterator checkin(SessionInfo sessionInfo, NodeId nodeId) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        CheckinMethod method = null;
        try {
            String uri = getItemUri(nodeId, sessionInfo);
            method = new CheckinMethod(uri);
            initMethod(method, sessionInfo, true);

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            // String vUri = method.getVersionUri();
            // TODO: retrieve events
            return null;
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
     * @see RepositoryService#checkout(SessionInfo, NodeId)
     */
    public EventIterator checkout(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        CheckoutMethod method = null;
        try {
            String uri = getItemUri(nodeId, sessionInfo);
            method = new CheckoutMethod(uri);
            initMethod(method, sessionInfo, true);

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            // TODO: retrieve events
            return null;
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
     * @see RepositoryService#restore(SessionInfo, NodeId, NodeId, boolean)
     */
    public EventIterator restore(SessionInfo sessionInfo, NodeId nodeId, NodeId versionId, boolean removeExisting) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        String vUri = getItemUri(versionId, sessionInfo);

        return update(uri, new String[] {vUri}, UpdateInfo.UPDATE_BY_VERSION, removeExisting, sessionInfo);
    }

    /**
     * @see RepositoryService#restore(SessionInfo, NodeId[], boolean)
     */
    public EventIterator restore(SessionInfo sessionInfo, NodeId[] versionIds, boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException {
        String uri = getWorkspaceUri(sessionInfo.getWorkspaceName());
        String[] vUris = new String[versionIds.length];
        for (int i = 0; i < versionIds.length; i++) {
            vUris[i] = getItemUri(versionIds[i], sessionInfo);
        }

        return update(uri, vUris, UpdateInfo.UPDATE_BY_VERSION, removeExisting, sessionInfo);
    }

    private EventIterator update(String uri, String[] updateSource, int updateType, boolean removeExisting, SessionInfo sessionInfo) throws RepositoryException {
        UpdateMethod method = null;
        try {
            UpdateInfo uInfo;
            if (removeExisting) {
                Element uElem = UpdateInfo.createUpdateElement(updateSource, updateType, domFactory);
                DomUtil.addChildElement(uElem, ItemResourceConstants.XML_REMOVEEXISTING, ItemResourceConstants.NAMESPACE);
                uInfo = new UpdateInfo(uElem);
            } else {
                uInfo = new UpdateInfo(updateSource, updateType, new DavPropertyNameSet());
            }

            method = new UpdateMethod(uri, uInfo);
            initMethod(method, sessionInfo, true);

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            // TODO: retrieve events
            return null;
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
     * @see RepositoryService#merge(SessionInfo, NodeId, String, boolean)
     */
    public EventIterator merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceId, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        MergeMethod method = null;
        try {
            String wspHref = getWorkspaceUri(srcWorkspaceId);
            Element mElem = MergeInfo.createMergeElement(new String[] {wspHref}, bestEffort, false, domFactory);
            MergeInfo mInfo = new MergeInfo(mElem);

            method = new MergeMethod(getItemUri(nodeId, sessionInfo), mInfo);
            initMethod(method, sessionInfo, true);

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            // TODO: need to evaluate response?
            // TODO: retrieve events
            return null;
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
     * @see RepositoryService#resolveMergeConflict(SessionInfo, NodeId, NodeId[], NodeId[])
     */
    public EventIterator resolveMergeConflict(SessionInfo sessionInfo, NodeId nodeId, NodeId[] mergeFailedIds, NodeId[] predecessorIds) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        PropPatchMethod method = null;
        try {
            List changeList = new ArrayList();
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

            method = new PropPatchMethod(getItemUri(nodeId, sessionInfo), changeList);
            initMethod(method, sessionInfo, true);

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            // TODO: need to evaluate response?
            // TODO: retrieve events
            return null;
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
     * @see RepositoryService#addVersionLabel(SessionInfo,NodeId,NodeId,QName,boolean)
     */
    public EventIterator addVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, QName label, boolean moveLabel) throws VersionException, RepositoryException {
        LabelMethod method = null;
        try {
            String uri = getItemUri(versionId, sessionInfo);
            method = new LabelMethod(uri, getJCRName(label), (moveLabel) ? LabelInfo.TYPE_SET : LabelInfo.TYPE_ADD);
            initMethod(method, sessionInfo, true);

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            // TODO: retrieve events
            return null;
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } catch (NoPrefixDeclaredException e) {
            throw new RepositoryException(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#removeVersionLabel(SessionInfo,NodeId,NodeId,QName)
     */
    public EventIterator removeVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, QName label) throws VersionException, RepositoryException {
        LabelMethod method = null;
        try {
            String uri = getItemUri(versionId, sessionInfo);
            method = new LabelMethod(uri, getJCRName(label), LabelInfo.TYPE_REMOVE);
            initMethod(method, sessionInfo, true);

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            // TODO: retrieve events
            return null;
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } catch (NoPrefixDeclaredException e) {
            throw new RepositoryException(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#getSupportedQueryLanguages(SessionInfo)
     */
    public String[] getSupportedQueryLanguages(SessionInfo sessionInfo) throws RepositoryException {
        OptionsMethod method = new OptionsMethod(getWorkspaceUri(sessionInfo.getWorkspaceName()));
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
        }
    }

    /**
     * @see RepositoryService#executeQuery(SessionInfo, String, String)
     */
    public QueryInfo executeQuery(SessionInfo sessionInfo, String statement, String language) throws RepositoryException {
        try {
            SearchMethod method = new SearchMethod(getWorkspaceUri(sessionInfo.getWorkspaceName()), statement, language);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            MultiStatus ms = method.getResponseBodyAsMultiStatus();
            return new QueryInfoImpl(ms, sessionInfo, getURIResolver(), getNamespaceResolver());
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        }
    }

    /**
     * @see RepositoryService#addEventListener(SessionInfo, NodeId, EventListener, int, boolean, String[], QName[])
     */
    public void addEventListener(SessionInfo sessionInfo, NodeId nodeId, EventListener listener, int eventTypes, boolean isDeep, String[] uuids, QName[] nodeTypeIds) throws RepositoryException {
        // build event types
        // TODO: server expected JCR-event types... currently spi types are used
        List eTypes = new ArrayList();
        if ((eventTypes & Event.NODE_ADDED) == Event.NODE_ADDED) {
            eTypes.add(SubscriptionImpl.getEventType(Event.NODE_ADDED));
        }
        if ((eventTypes & Event.NODE_REMOVED) == Event.NODE_REMOVED) {
            eTypes.add(SubscriptionImpl.getEventType(Event.NODE_REMOVED));
        }
        if ((eventTypes & Event.PROPERTY_ADDED) == Event.PROPERTY_ADDED) {
            eTypes.add(SubscriptionImpl.getEventType(Event.PROPERTY_ADDED));
        }
        if ((eventTypes & Event.PROPERTY_REMOVED) == Event.PROPERTY_REMOVED) {
            eTypes.add(SubscriptionImpl.getEventType(Event.PROPERTY_REMOVED));
        }
        if ((eventTypes & Event.PROPERTY_CHANGED) == Event.PROPERTY_CHANGED) {
            eTypes.add(SubscriptionImpl.getEventType(Event.PROPERTY_CHANGED));
        }
        EventType[] etArr = (EventType[]) eTypes.toArray(new EventType[eTypes.size()]);

        // build filters from params
        List filters = new ArrayList();
        for (int i = 0; i < uuids.length; i++) {
            filters.add(new Filter(ObservationConstants.XML_UUID, ObservationConstants.NAMESPACE, uuids[i]));
        }
        for (int i = 0; i < nodeTypeIds.length; i++) {
            try {
                String ntName = getJCRName(nodeTypeIds[i]);
                filters.add(new Filter(ObservationConstants.XML_NODETYPE_NAME, ObservationConstants.NAMESPACE, ntName));
            } catch (NoPrefixDeclaredException e) {
                throw new RepositoryException(e);
            }
        }
        Filter[] ftArr = (Filter[]) filters.toArray(new Filter[filters.size()]);
        // always 'noLocal' since local changes are reported by return values
        boolean noLocal = true;

        SubscriptionInfo subscriptionInfo = new SubscriptionInfo(etArr, ftArr, noLocal, isDeep, DavConstants.UNDEFINED_TIMEOUT);

        // TODO: missing implementation
    }

    /**
     * @see RepositoryService#removeEventListener(SessionInfo, NodeId, EventListener)
     */
    public void removeEventListener(SessionInfo sessionInfo, NodeId nodeId, EventListener listener) throws RepositoryException {
        // TODO: missing implementation
    }

    /**
     * @see RepositoryService#getRegisteredNamespaces(SessionInfo)
     */
    public Properties getRegisteredNamespaces(SessionInfo sessionInfo) throws RepositoryException {
        ReportInfo info = new ReportInfo(RegisteredNamespacesReport.REGISTERED_NAMESPACES_REPORT, DEPTH_0);
        ReportMethod method = null;
        try {
            method = new ReportMethod(getWorkspaceUri(sessionInfo.getWorkspaceName()), info);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            Document doc = method.getResponseBodyAsDocument();
            Properties namespaces = new Properties();
            if (doc != null) {
                Element rootElement = doc.getDocumentElement();
                ElementIterator nsElems = DomUtil.getChildren(rootElement, ItemResourceConstants.XML_NAMESPACE, ItemResourceConstants.NAMESPACE);
                while (nsElems.hasNext()) {
                    Element elem = nsElems.nextElement();
                    String prefix = DomUtil.getChildText(elem, ItemResourceConstants.XML_PREFIX, ItemResourceConstants.NAMESPACE);
                    String uri = DomUtil.getChildText(elem, ItemResourceConstants.XML_URI, ItemResourceConstants.NAMESPACE);
                    // default namespace
                    if (prefix == null && uri == null) {
                        prefix = uri = "";
                    }
                    // any other uri must not be null
                    if (uri != null) {
                        namespaces.setProperty(prefix, uri);
                        // TODO: not correct since nsRegistry is retrieved from each session
                        prefixToURI.setProperty(prefix, uri);
                        uriToPrefix.setProperty(uri, prefix);
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
     * @see RepositoryService#registerNamespace(SessionInfo, String, String)
     */
    public void registerNamespace(SessionInfo sessionInfo, String prefix, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        Properties namespaces = new Properties();
        namespaces.putAll(prefixToURI);
        namespaces.setProperty(prefix, uri);
        internalSetNamespaces(sessionInfo, namespaces);
        // adjust internal mappings:
        // TODO: not correct since nsRegistry is retrieved from each session
        prefixToURI.setProperty(prefix, uri);
        uriToPrefix.setProperty(uri, prefix);
    }

    /**
     * @see RepositoryService#unregisterNamespace(SessionInfo, String)
     */
    public void unregisterNamespace(SessionInfo sessionInfo, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        String prefix = uriToPrefix.getProperty(uri);
        Properties namespaces = new Properties();
        namespaces.putAll(prefixToURI);
        namespaces.remove(prefix);
        internalSetNamespaces(sessionInfo, namespaces);
        // adjust internal mappings:
        // TODO: not correct since nsRegistry is retrieved from each session
        prefixToURI.remove(prefix);
        uriToPrefix.remove(uri);
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
    private void internalSetNamespaces(SessionInfo sessionInfo, Properties namespaces) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        DavPropertySet setProperties = new DavPropertySet();
        setProperties.add(new NamespacesProperty(namespaces));

        PropPatchMethod method = null;
        try {
            String uri = getWorkspaceUri(sessionInfo.getWorkspaceName());

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
     * @see RepositoryService#getNodeTypeDefinitions(SessionInfo)
     */
    public QNodeTypeDefinitionIterator getNodeTypeDefinitions(SessionInfo sessionInfo) throws RepositoryException {
        ReportInfo info = new ReportInfo(NodeTypesReport.NODETYPES_REPORT, DEPTH_0);
        info.setContentElement(DomUtil.createElement(domFactory, NodeTypeConstants.XML_REPORT_ALLNODETYPES, NodeTypeConstants.NAMESPACE));

        ReportMethod method = null;
        try {
            method = new ReportMethod(getWorkspaceUri(sessionInfo.getWorkspaceName()), info);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            Document reportDoc = method.getResponseBodyAsDocument();
            ElementIterator it = DomUtil.getChildren(reportDoc.getDocumentElement(), NodeTypeConstants.NODETYPE_ELEMENT, null);
            List ntDefs = new ArrayList();
            while (it.hasNext()) {
                ntDefs.add(new QNodeTypeDefinitionImpl(it.nextElement(), getNamespaceResolver()));
            }
            return new IteratorHelper(ntDefs);
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
     * @see RepositoryService#registerNodeTypes(SessionInfo, QNodeTypeDefinition[])
     */
    public void registerNodeTypes(SessionInfo sessionInfo, QNodeTypeDefinition[] nodetypeDefs) throws NoSuchNodeTypeException, UnsupportedRepositoryOperationException, RepositoryException {
        // TODO: missing implementation
    }

    /**
     * @see RepositoryService#reregisterNodeTypes(SessionInfo, QNodeTypeDefinition[])
     */
    public void reregisterNodeTypes(SessionInfo sessionInfo, QNodeTypeDefinition[] nodetypeDefs) throws NoSuchNodeTypeException, UnsupportedRepositoryOperationException, RepositoryException {
        // TODO: missing implementation
    }

    /**
     * @see RepositoryService#unregisterNodeTypes(SessionInfo, QName[])
     */
    public void unregisterNodeTypes(SessionInfo sessionInfo, QName[] nodetypeNames) throws NoSuchNodeTypeException, UnsupportedRepositoryOperationException, RepositoryException {
        // TODO: missing implementation
    }

    //--------------------------------------------------< NamespaceResolver >---
    private NamespaceResolver getNamespaceResolver() {
        return this;
    }

    public String getURI(String prefix) throws NamespaceException {
        String uri = (String) prefixToURI.get(prefix);
        if (uri == null) {
            throw new NamespaceException(prefix + ": is not a registered namespace prefix.");
        }
        return uri;
    }

    public String getPrefix(String uri) throws NamespaceException {
        String prefix = (String) uriToPrefix.get(uri);
        if (prefix == null) {
            throw new NamespaceException(uri
                + ": is not a registered namespace uri.");
        }
        return prefix;
    }

    //-------------------------------------------------------< URI resolver >---

    private URIResolver getURIResolver() {
        return this;
    }

    /**
     * @inheritDoc
     */
    public Path getQPath(String uri, SessionInfo sessionInfo) throws RepositoryException {
        String repoUri = getRepositoryUri();
        String wspUri = getWorkspaceUri(sessionInfo.getWorkspaceName());
        String jcrPath;
        if (uri.startsWith(wspUri)) {
            jcrPath = uri.substring(wspUri.length());
        } else if (uri.startsWith(repoUri)) {
            jcrPath = uri.substring(repoUri.length());
            // then cut workspace name
            jcrPath = jcrPath.substring(jcrPath.indexOf('/', 1));
        } else {
            // todo: probably rather an error?
            jcrPath = uri;
        }
        try {
            return PathFormat.parse(jcrPath, this);
        } catch (MalformedPathException e) {
            throw new RepositoryException();
        }
    }

    /**
     * @inheritDoc
     */
    public NodeId getNodeId(NodeId parentId, MultiStatusResponse response) throws RepositoryException {
        // build the id
        NodeId nodeId;
        DavPropertySet propSet = response.getProperties(DavServletResponse.SC_OK);
        String nodeURI = response.getHref();
        if (propSet.contains(ItemResourceConstants.JCR_UUID)) {
            String uuid = propSet.get(ItemResourceConstants.JCR_UUID).getValue().toString();
            // make sure the mapping uuid -> uri is cached
            uriCache.put(uuid, nodeURI);
            nodeId = idFactory.createNodeId(uuid);
        } else {
            DavProperty nameProp = propSet.get(ItemResourceConstants.JCR_NAME);
            if (nameProp != null && nameProp.getValue() != null) {
                // not root node. Note that 'unespacing' is not required since
                // the jcr:name property does not provide the value in escaped form.
                String jcrName = nameProp.getValue().toString();
                int index = org.apache.jackrabbit.name.Path.INDEX_UNDEFINED;
                DavProperty indexProp = propSet.get(ItemResourceConstants.JCR_INDEX);
                if (indexProp != null && indexProp.getValue() != null) {
                    index = Integer.parseInt(indexProp.getValue().toString());
                }
                try {
                    QName qName = getQName(jcrName);
                    nodeId = idFactory.createNodeId(parentId, Path.create(qName, index));
                } catch (NameException e) {
                    throw new RepositoryException(e);
                }
            } else {
                // special case: root node
                nodeId = idFactory.createNodeId((String)null, Path.ROOT);
            }
        }

        // mapping href to nodeId
        idCache.put(nodeURI, nodeId);
        return nodeId;
    }

    /**
     * @inheritDoc
     */
    public NodeId getNodeId(String uri, SessionInfo sessionInfo) throws RepositoryException {
        if (idCache.containsKey(uri)) {
            // id has been accessed before and
            return (NodeId) idCache.get(uri);
        } else {
            // retrieve parentId from cache or by recursive calls
            NodeId parentId;
            if (getRootItemUri(sessionInfo).equals(uri)) {
                parentId = null;
            } else {
                String parentUri = Text.getRelativeParent(uri, 1, true);
                if (!parentUri.endsWith("/")) {
                    parentUri += "/";
                }
                parentId = getNodeId(parentUri, sessionInfo);
            }
            NodeId id = retrieveNodeId(uri, parentId, sessionInfo);
            idCache.put(uri, id);
            return id;
        }
    }

    /**
     * @inheritDoc
     */
    public PropertyId getPropertyId(NodeId parentId, MultiStatusResponse response) throws RepositoryException {
        try {
            DavPropertySet propSet = response.getProperties(DavServletResponse.SC_OK);
            QName name = getQName(propSet.get(ItemResourceConstants.JCR_NAME).getValue().toString());
            PropertyId propertyId = idFactory.createPropertyId(parentId, name);
            return propertyId;
        } catch (BaseException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @inheritDoc
     */
    public PropertyId getPropertyId(String uri, SessionInfo sessionInfo) throws RepositoryException {
        String propertyUri = uri;
        // separate parent uri and property JCRName
        String parentUri = Text.getRelativeParent(propertyUri, 1, true);
        // make sure propName is unescaped
        String propName = Text.unescape(Text.getName(propertyUri));
        // retrieve parent node id
        NodeId parentId = getNodeId(parentUri, sessionInfo);
        // build property id
        try {
            PropertyId propertyId = idFactory.createPropertyId(parentId, getQName(propName));
            return propertyId;
        } catch (NameException e) {
            throw new RepositoryException(e);
        }
    }

    private NodeId retrieveNodeId(String uri, NodeId parentId, SessionInfo sessionInfo) throws RepositoryException {
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(ItemResourceConstants.JCR_UUID);
        nameSet.add(ItemResourceConstants.JCR_NAME);
        nameSet.add(ItemResourceConstants.JCR_INDEX);
        DavMethodBase method = null;
        try {
            method = new PropFindMethod(uri, nameSet, DEPTH_0);
            initMethod(method, sessionInfo, false);
            getClient(sessionInfo).executeMethod(method);
            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length != 1) {
                throw new ItemNotFoundException("Unable to retrieve the node with id " + uri);
            }
            return getNodeId(parentId, responses[0]);
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

    //------------------------------------------------< Inner Class 'Batch' >---
    private class BatchImpl implements Batch {

        private final SessionInfoImpl sessionInfo;
        private final List methods = new ArrayList();

        private boolean isConsumed = false;

        private BatchImpl(SessionInfoImpl sessionInfo) {
            this.sessionInfo = sessionInfo;
        }

        private HttpClient start() throws RepositoryException {
            checkConsumed();
            try {
                String uri = getRootItemUri(sessionInfo);
                LockMethod method = new LockMethod(uri, TransactionConstants.LOCAL, TransactionConstants.TRANSACTION, null, DavConstants.INFINITE_TIMEOUT, true);
                initMethod(method, sessionInfo, false);

                HttpClient client = getClient(sessionInfo);
                client.executeMethod(method);
                method.checkSuccess();

                String batchId = method.getLockToken();
                sessionInfo.setBatchId(batchId);
                return client;
            } catch (IOException e) {
                throw new RepositoryException(e);
            } catch (DavException e) {
                throw ExceptionConverter.generate(e);
            }
        }

        private void end(HttpClient client, boolean discard) throws RepositoryException {
            checkConsumed();
            try {
                String uri = getRootItemUri(sessionInfo);
                UnLockMethod method = new UnLockMethod(uri, sessionInfo.getBatchId());
                // todo: check if 'initmethod' would work (ev. conflict with TxId header).
                String[] locktokens = sessionInfo.getLockTokens();
                if (locktokens != null && locktokens.length > 0) {
                    IfHeader ifH = new IfHeader(locktokens);
                    method.setRequestHeader(ifH.getHeaderName(), ifH.getHeaderValue());
                }
                // in contrast to standard UNLOCK, the tx-unlock provides a
                // request body.
                method.setRequestBody(new TransactionInfo(!discard));

                client.executeMethod(method);
                method.checkSuccess();
                // make sure the batchId on the sessionInfo is reset.
                sessionInfo.setBatchId(null);
            } catch (IOException e) {
                throw new RepositoryException(e);
            } catch (DavException e) {
                throw ExceptionConverter.generate(e);
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

        private Iterator methods() {
            return methods.iterator();
        }

        //----------------------------------------------------------< Batch >---
        /**
         * The XML elements and attributes used in serialization
         */
        private final Namespace SV_NAMESPACE = Namespace.getNamespace(QName.NS_SV_PREFIX, QName.NS_SV_URI);
        private final String NODE_ELEMENT = "node";
        private final String PROPERTY_ELEMENT = "property";
        private final String VALUE_ELEMENT = "value";
        private final String NAME_ATTRIBUTE = "name";
        private final String TYPE_ATTRIBUTE = "type";

        /**
         * @see Batch#addNode(NodeId, QName, QName, String)
         */
        public void addNode(NodeId parentId, QName nodeName, QName nodetypeName, String uuid) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, NoSuchNodeTypeException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            try {
                String uri = getItemUri(parentId, nodeName, sessionInfo);
                MkColMethod method = new MkColMethod(uri);
                initMethod(method, sessionInfo, true);
                // build 'sys-view' for the node to create and append it as request body
                if (nodetypeName != null || uuid != null) {
                    Document body = DomUtil.BUILDER_FACTORY.newDocumentBuilder().newDocument();
                    Element nodeElement = DomUtil.addChildElement(body, NODE_ELEMENT, SV_NAMESPACE);
                    DomUtil.setAttribute(nodeElement, NAME_ATTRIBUTE, SV_NAMESPACE, Text.getName(uri, true));

                    if (nodetypeName != null) {
                        Element propElement = DomUtil.addChildElement(nodeElement, PROPERTY_ELEMENT, SV_NAMESPACE);
                        DomUtil.setAttribute(propElement, NAME_ATTRIBUTE, SV_NAMESPACE, getJCRName(QName.JCR_PRIMARYTYPE));
                        DomUtil.setAttribute(propElement, TYPE_ATTRIBUTE, SV_NAMESPACE, PropertyType.nameFromValue(PropertyType.NAME));
                        DomUtil.addChildElement(propElement, VALUE_ELEMENT, SV_NAMESPACE, getJCRName(nodetypeName));
                    }
                    if (uuid != null) {
                        Element propElement = DomUtil.addChildElement(nodeElement, PROPERTY_ELEMENT, SV_NAMESPACE);
                        DomUtil.setAttribute(propElement, NAME_ATTRIBUTE, SV_NAMESPACE, getJCRName(QName.JCR_UUID));
                        DomUtil.setAttribute(propElement, TYPE_ATTRIBUTE, SV_NAMESPACE, PropertyType.nameFromValue(PropertyType.STRING));
                        DomUtil.addChildElement(propElement, VALUE_ELEMENT, SV_NAMESPACE, uuid);
                    }
                    method.setRequestBody(body);
                }

                methods.add(method);

                // TODO: retrieve location header (in order to detect index) and update id-mapping
            } catch (IOException e) {
                throw new RepositoryException(e);
            } catch (ParserConfigurationException e) {
                throw new RepositoryException(e);
            } catch (NoPrefixDeclaredException e) {
                throw new RepositoryException(e);
            }
        }

        /**
         * @see Batch#addProperty(NodeId, QName, String, int)
         */
        public void addProperty(NodeId parentId, QName propertyName, String value, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            QValue qV = QValue.create(value, propertyType);
            Value jcrValue = ValueFormat.getJCRValue(qV, getNamespaceResolver(), valueFactory);
            ValuesProperty vp = new ValuesProperty(jcrValue);
            internalAddProperty(parentId, propertyName, vp);
        }

        /**
         * @see Batch#addProperty(NodeId, QName, String[], int)
         */
        public void addProperty(NodeId parentId, QName propertyName, String[] values, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            Value[] jcrValues = new Value[values.length];
            for (int i = 0; i < values.length; i++) {
                QValue v = QValue.create(values[i], propertyType);
                jcrValues[i] = ValueFormat.getJCRValue(v, getNamespaceResolver(), valueFactory);
            }
            ValuesProperty vp = new ValuesProperty(jcrValues);
            internalAddProperty(parentId, propertyName, vp);
        }

        /**
         * @see Batch#addProperty(NodeId, QName, InputStream, int)
         */
        public void addProperty(NodeId parentId, QName propertyName, InputStream value, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            QValue qV = null;
            try {
                qV = QValue.create(value, propertyType);
                Value jcrValue = ValueFormat.getJCRValue(qV, getNamespaceResolver(), valueFactory);
                ValuesProperty vp = new ValuesProperty(jcrValue);
                internalAddProperty(parentId, propertyName, vp);
            } catch (IOException e) {
                throw new ValueFormatException(e);
            }
        }

        /**
         * @see Batch#addProperty(NodeId, QName, InputStream[], int)
         */
        public void addProperty(NodeId parentId, QName propertyName, InputStream[] values, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            try {
                Value[] jcrValues = new Value[values.length];
                for (int i = 0; i < values.length; i++) {
                    QValue qV = QValue.create(values[i], propertyType);
                    jcrValues[i] = ValueFormat.getJCRValue(qV, getNamespaceResolver(), valueFactory);
                }
                ValuesProperty vp = new ValuesProperty(jcrValues);
                internalAddProperty(parentId, propertyName, vp);
            } catch (IOException e) {
                throw new ValueFormatException(e);
            }
        }

        private void internalAddProperty(NodeId parentId, QName propertyName, ValuesProperty vp) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            try {
                String uri = getItemUri(parentId, propertyName, sessionInfo);
                PutMethod method = new PutMethod(uri);
                initMethod(method, sessionInfo, true);
                method.setRequestBody(vp);

                methods.add(method);

            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }

        /**
         * @see Batch#setValue(PropertyId, String, int)
         */
        public void setValue(PropertyId propertyId, String value, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            DavPropertySet setProperties = new DavPropertySet();
            if (value == null) {
                // setting property value to 'null' is identical to a removal
                remove(propertyId);
            } else {
                // qualified value must be converted to jcr value
                QValue qV = QValue.create(value, propertyType);
                Value jcrValue = ValueFormat.getJCRValue(qV, getNamespaceResolver(), valueFactory);
                ValuesProperty vp = new ValuesProperty(jcrValue);
                setProperties.add(vp);
            }
            internalSetValue(propertyId, setProperties);
        }

        /**
         * @see Batch#setValue(PropertyId, String[], int)
         */
        public void setValue(PropertyId propertyId, String[] values, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            DavPropertySet setProperties = new DavPropertySet();
            if (values == null) {
                // setting property value to 'null' is identical to a removal
                remove(propertyId);
            } else {
                // qualified values must be converted to jcr values
                Value[] jcrValues = new Value[values.length];
                for (int i = 0; i < values.length; i++) {
                    QValue qV = QValue.create(values[i], propertyType);
                    jcrValues[i] = ValueFormat.getJCRValue(qV, getNamespaceResolver(), valueFactory);
                }
                setProperties.add(new ValuesProperty(jcrValues));
            }
            internalSetValue(propertyId, setProperties);
        }

        /**
         * @see Batch#setValue(PropertyId, InputStream, int)
         */
        public void setValue(PropertyId propertyId, InputStream value, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            try {
                DavPropertySet setProperties = new DavPropertySet();
                if (value == null) {
                    // setting property value to 'null' is identical to a removal
                    remove(propertyId);
                } else {
                    // qualified value must be converted to jcr value
                    QValue qV = QValue.create(value, propertyType);
                    Value jcrValue = ValueFormat.getJCRValue(qV, getNamespaceResolver(), valueFactory);
                    ValuesProperty vp = new ValuesProperty(jcrValue);
                    setProperties.add(vp);
                }
                internalSetValue(propertyId, setProperties);
            } catch (IOException e) {
                throw new ValueFormatException(e);
            }
        }

        /**
         * @see Batch#setValue(PropertyId, InputStream[], int)
         */
        public void setValue(PropertyId propertyId, InputStream[] values, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            try {
                DavPropertySet setProperties = new DavPropertySet();
                if (values == null) {
                    // setting property value to 'null' is identical to a removal
                    remove(propertyId);
                } else {
                    // qualified values must be converted to jcr values
                    Value[] jcrValues = new Value[values.length];
                    for (int i = 0; i < values.length; i++) {
                        QValue qV = QValue.create(values[i], propertyType);
                        jcrValues[i] = ValueFormat.getJCRValue(qV, getNamespaceResolver(), valueFactory);
                    }
                    setProperties.add(new ValuesProperty(jcrValues));
                }
                internalSetValue(propertyId, setProperties);
            }   catch (IOException e) {
                throw new ValueFormatException(e);
            }
        }

        /**
         *
         * @param propertyId
         * @param setProperties
         * @throws ValueFormatException
         * @throws VersionException
         * @throws LockException
         * @throws ConstraintViolationException
         * @throws AccessDeniedException
         * @throws UnsupportedRepositoryOperationException
         * @throws RepositoryException
         */
        private void internalSetValue(PropertyId propertyId, DavPropertySet setProperties) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            try {
                String uri = getItemUri(propertyId, sessionInfo);
                PropPatchMethod method = new PropPatchMethod(uri, setProperties, new DavPropertyNameSet());
                initMethod(method, sessionInfo, true);

                methods.add(method);
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }

        /**
         * @see Batch#remove(ItemId)
         */
        public void remove(ItemId itemId) throws VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            String uri = getItemUri(itemId, sessionInfo);
            DeleteMethod method = new DeleteMethod(uri);
            initMethod(method, sessionInfo, true);

            methods.add(method);
        }

        /**
         * @see Batch#reorderNodes(NodeId, NodeId, NodeId)
         */
        public void reorderNodes(NodeId parentId, NodeId srcNodeId, NodeId beforeNodeId) throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException, ItemNotFoundException, LockException, AccessDeniedException, RepositoryException {
            checkConsumed();
            try {
                String srcUri = getItemUri(srcNodeId, sessionInfo);
                String srcSegment = Text.getName(srcUri, true);
                String targetSegment = Text.getName(getItemUri(beforeNodeId, sessionInfo), true);

                String uri = getItemUri(parentId, sessionInfo);
                OrderPatchMethod method = new OrderPatchMethod(uri, srcSegment, targetSegment, true);
                initMethod(method, sessionInfo, true);

                methods.add(method);
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }

        /**
         * @see Batch#setMixins(NodeId, QName[])
         */
        public void setMixins(NodeId nodeId, QName[] mixinNodeTypeIds) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            try {
                DavPropertySet setProperties;
                DavPropertyNameSet removeProperties;
                if (mixinNodeTypeIds == null || mixinNodeTypeIds.length == 0) {
                    setProperties = new DavPropertySet();
                    removeProperties = new DavPropertyNameSet();
                    removeProperties.add(ItemResourceConstants.JCR_MIXINNODETYPES);
                } else {
                    String[] ntNames = new String[mixinNodeTypeIds.length];
                    for (int i = 0; i < mixinNodeTypeIds.length; i++) {
                        ntNames[i] = getJCRName(mixinNodeTypeIds[i]);
                    }
                    setProperties = new DavPropertySet();
                    setProperties.add(new NodeTypeProperty(ItemResourceConstants.JCR_MIXINNODETYPES, ntNames, false));
                    removeProperties = new DavPropertyNameSet();
                }

                String uri = getItemUri(nodeId, sessionInfo);
                PropPatchMethod method = new PropPatchMethod(uri, setProperties, removeProperties);
                initMethod(method, sessionInfo, true);

                methods.add(method);
            } catch (IOException e) {
                throw new RepositoryException(e);
            } catch (NoPrefixDeclaredException e) {
                // should not occur.
                throw new RepositoryException(e);
            }
        }

        /**
         * @see Batch#move(NodeId, NodeId, QName)
         */
        public void move(NodeId srcNodeId, NodeId destParentNodeId, QName destName) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            String uri = getItemUri(srcNodeId, sessionInfo);
            String destUri = getItemUri(destParentNodeId, destName, sessionInfo);
            MoveMethod method = new MoveMethod(uri, destUri, true);
            initMethod(method, sessionInfo, true);

            methods.add(method);
        }
    }
}
