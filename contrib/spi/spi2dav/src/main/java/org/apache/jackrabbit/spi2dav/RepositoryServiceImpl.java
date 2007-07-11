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
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
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
import org.apache.jackrabbit.webdav.client.methods.SubscribeMethod;
import org.apache.jackrabbit.webdav.client.methods.UnSubscribeMethod;
import org.apache.jackrabbit.webdav.client.methods.PollMethod;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.ordering.OrderingConstants;
import org.apache.jackrabbit.webdav.observation.SubscriptionInfo;
import org.apache.jackrabbit.webdav.observation.EventType;
import org.apache.jackrabbit.webdav.observation.EventDiscovery;
import org.apache.jackrabbit.webdav.observation.ObservationConstants;
import org.apache.jackrabbit.webdav.observation.DefaultEventType;
import org.apache.jackrabbit.webdav.security.SecurityConstants;
import org.apache.jackrabbit.webdav.security.CurrentUserPrivilegeSetProperty;
import org.apache.jackrabbit.webdav.security.Privilege;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.lock.LockDiscovery;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.transaction.TransactionConstants;
import org.apache.jackrabbit.webdav.transaction.TransactionInfo;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.ElementIterator;
import org.apache.jackrabbit.webdav.xml.Namespace;
import org.apache.jackrabbit.webdav.header.CodedUrlHeader;
import org.apache.jackrabbit.webdav.header.IfHeader;
import org.apache.jackrabbit.webdav.search.SearchConstants;
import org.apache.jackrabbit.webdav.search.SearchInfo;
import org.apache.jackrabbit.webdav.jcr.version.report.RepositoryDescriptorsReport;
import org.apache.jackrabbit.webdav.jcr.version.report.RegisteredNamespacesReport;
import org.apache.jackrabbit.webdav.jcr.version.report.NodeTypesReport;
import org.apache.jackrabbit.webdav.jcr.version.report.JcrPrivilegeReport;
import org.apache.jackrabbit.webdav.jcr.nodetype.NodeTypeConstants;
import org.apache.jackrabbit.webdav.jcr.nodetype.NodeTypeProperty;
import org.apache.jackrabbit.webdav.jcr.property.ValuesProperty;
import org.apache.jackrabbit.webdav.jcr.property.NamespacesProperty;
import org.apache.jackrabbit.webdav.jcr.observation.SubscriptionImpl;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.IllegalNameException;
import org.apache.jackrabbit.name.UnknownPrefixException;
import org.apache.jackrabbit.name.AbstractNamespaceResolver;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.commons.EventFilterImpl;
import org.apache.jackrabbit.spi.commons.EventBundleImpl;
import org.apache.jackrabbit.spi.commons.ChildInfoImpl;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.value.ValueFormat;
import org.apache.jackrabbit.value.QValueFactoryImpl;
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
import javax.jcr.LoginException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.xml.parsers.ParserConfigurationException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.io.InputStream;
import java.io.IOException;

/**
 * <code>RepositoryServiceImpl</code>...
 */
// TODO: encapsulate URI building, escaping, unescaping...
// TODO: TO-BE-FIXED. caches don't get adjusted upon removal/move of items
public class RepositoryServiceImpl implements RepositoryService, DavConstants {

    private static Logger log = LoggerFactory.getLogger(RepositoryServiceImpl.class);


    private static final EventType[] ALL_EVENTS = new EventType[5];
    static {
        ALL_EVENTS[0] = SubscriptionImpl.getEventType(javax.jcr.observation.Event.NODE_ADDED);
        ALL_EVENTS[1] = SubscriptionImpl.getEventType(javax.jcr.observation.Event.NODE_REMOVED);
        ALL_EVENTS[2] = SubscriptionImpl.getEventType(javax.jcr.observation.Event.PROPERTY_ADDED);
        ALL_EVENTS[3] = SubscriptionImpl.getEventType(javax.jcr.observation.Event.PROPERTY_CHANGED);
        ALL_EVENTS[4] = SubscriptionImpl.getEventType(javax.jcr.observation.Event.PROPERTY_REMOVED);
    }
    private static final SubscriptionInfo S_INFO = new SubscriptionInfo(ALL_EVENTS, true, DavConstants.INFINITE_TIMEOUT);

    private final IdFactory idFactory;
    private final ValueFactory valueFactory;

    private final Document domFactory;
    private final NamespaceCache nsCache;
    private final URIResolverImpl uriResolver;

    private final HostConfiguration hostConfig;
    private final HashMap clients = new HashMap();
    private final HttpConnectionManager connectionManager;

    private final Map nodeTypeDefinitions = new HashMap();

    private Map descriptors;

    public RepositoryServiceImpl(String uri, IdFactory idFactory, ValueFactory valueFactory) throws RepositoryException {
        if (uri == null || "".equals(uri)) {
            throw new RepositoryException("Invalid repository uri '" + uri + "'.");
        }
        if (idFactory == null || valueFactory == null) {
            throw new RepositoryException("IdFactory and ValueFactory may not be null.");
        }
        this.idFactory = idFactory;
        this.valueFactory = valueFactory;

        try {
            domFactory = DomUtil.BUILDER_FACTORY.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new RepositoryException(e);
        }

        try {
            URI repositoryUri = new URI((uri.endsWith("/")) ? uri : uri+"/", true);
            hostConfig = new HostConfiguration();
            hostConfig.setHost(repositoryUri);

            nsCache = new NamespaceCache();
            uriResolver = new URIResolverImpl(repositoryUri, this, nsCache, domFactory);

        } catch (URIException e) {
            throw new RepositoryException(e);
        }

        this.connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setMaxConnectionsPerHost(hostConfig, 20);
        this.connectionManager.setParams(params);
    }

    private static void checkSessionInfo(SessionInfo sessionInfo) throws RepositoryException {
        if (!(sessionInfo instanceof SessionInfoImpl)) {
            throw new RepositoryException("Unknown SessionInfo implementation.");
        }
    }

    private static boolean isUnLockMethod(DavMethod method) {
        int code = DavMethods.getMethodCode(method.getName());
        return DavMethods.DAV_UNLOCK == code;
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
    }

    private static void initMethod(DavMethod method, BatchImpl batchImpl, boolean addIfHeader) {
        initMethod(method, batchImpl.sessionInfo,  addIfHeader);

        // add batchId as separate header
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

    HttpClient getClient(SessionInfo sessionInfo) throws RepositoryException {
        HttpClient client = (HttpClient) clients.get(sessionInfo);
        if (client == null) {
            client = new HttpClient(connectionManager);
            client.setHostConfiguration(hostConfig);
            // always send authentication not waiting for 401
            client.getParams().setAuthenticationPreemptive(true);
            // NOTE: null credentials only work if 'missing-auth-mapping' param is
            // set on the server
            org.apache.commons.httpclient.Credentials creds = null;
            if (sessionInfo != null) {
                checkSessionInfo(sessionInfo);
                creds = ((SessionInfoImpl) sessionInfo).getCredentials().getCredentials();
            }
            client.getState().setCredentials(AuthScope.ANY, creds);
            clients.put(sessionInfo, client);
            log.debug("Created Client " + client + " for SessionInfo " + sessionInfo);
        }
        return client;
    }

    private void removeClient(SessionInfo sessionInfo) {
        HttpClient cl = (HttpClient) clients.remove(sessionInfo);
        log.debug("Removed Client " + cl + " for SessionInfo " + sessionInfo);
    }

    private String getItemUri(ItemId itemId, SessionInfo sessionInfo) throws RepositoryException {
        return uriResolver.getItemUri(itemId, sessionInfo.getWorkspaceName(), sessionInfo);
    }

    private String getItemUri(NodeId parentId, QName childName, SessionInfo sessionInfo) throws RepositoryException {
        String parentUri = uriResolver.getItemUri(parentId, sessionInfo.getWorkspaceName(), sessionInfo);
        try {
            NamespaceResolver resolver = new NamespaceResolverImpl(sessionInfo);
            return parentUri + "/" + Text.escape(NameFormat.format(childName, resolver));
        } catch (NoPrefixDeclaredException e) {
            throw new RepositoryException(e);
        }
    }

    private NodeId getParentId(DavPropertySet propSet, SessionInfo sessionInfo)
        throws RepositoryException {
        NodeId parentId = null;
        if (propSet.contains(ItemResourceConstants.JCR_PARENT)) {
            HrefProperty parentProp = new HrefProperty(propSet.get(ItemResourceConstants.JCR_PARENT));
            String parentHref = parentProp.getHrefs().get(0).toString();
            if (parentHref != null && parentHref.length() > 0) {
                parentId = uriResolver.getNodeId(parentHref, sessionInfo);
            }
        }
        return parentId;
    }

    String getUniqueID(DavPropertySet propSet) {
        if (propSet.contains(ItemResourceConstants.JCR_UUID)) {
            return propSet.get(ItemResourceConstants.JCR_UUID).getValue().toString();
        } else {
            return null;
        }
    }

    QName getQName(DavPropertySet propSet, NamespaceResolver nsResolver) throws RepositoryException {
        DavProperty nameProp = propSet.get(ItemResourceConstants.JCR_NAME);
        if (nameProp != null && nameProp.getValue() != null) {
            // not root node. Note that 'unespacing' is not required since
            // the jcr:name property does not provide the value in escaped form.
            String jcrName = nameProp.getValue().toString();
            try {
                return NameFormat.parse(jcrName, nsResolver);
            } catch (NameException e) {
                throw new RepositoryException(e);
            }
        } else {
            return QName.ROOT;
        }
    }

    int getIndex(DavPropertySet propSet) {
        int index = Path.INDEX_UNDEFINED;
        DavProperty indexProp = propSet.get(ItemResourceConstants.JCR_INDEX);
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
            throw ExceptionConverter.generate(e);
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

    public QValueFactory getQValueFactory() {
        return QValueFactoryImpl.getInstance();
    }

    /**
     * @see RepositoryService#getRepositoryDescriptors()
     */
    public Map getRepositoryDescriptors() throws RepositoryException {
        if (descriptors == null) {
            ReportInfo info = new ReportInfo(RepositoryDescriptorsReport.REPOSITORY_DESCRIPTORS_REPORT, DavConstants.DEPTH_0);
            ReportMethod method = null;
            try {
                method = new ReportMethod(uriResolver.getRepositoryUri(), info);
                getClient(null).executeMethod(method);
                method.checkSuccess();
                Document doc = method.getResponseBodyAsDocument();

                descriptors = new HashMap();
                if (doc != null) {
                    Element rootElement = doc.getDocumentElement();
                    ElementIterator nsElems = DomUtil.getChildren(rootElement, ItemResourceConstants.XML_DESCRIPTOR, ItemResourceConstants.NAMESPACE);
                    while (nsElems.hasNext()) {
                        Element elem = nsElems.nextElement();
                        String key = DomUtil.getChildText(elem, ItemResourceConstants.XML_DESCRIPTORKEY, ItemResourceConstants.NAMESPACE);
                        String descriptor = DomUtil.getChildText(elem, ItemResourceConstants.XML_DESCRIPTORVALUE, ItemResourceConstants.NAMESPACE);
                        if (key != null && descriptor != null) {
                            descriptors.put(key, descriptor);
                        } else {
                            log.error("Invalid descriptor key / value pair: " + key + " -> " + descriptor);
                        }
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
            nameSet.add(DeltaVConstants.WORKSPACE);
            method = new PropFindMethod(uriResolver.getWorkspaceUri(workspaceName), nameSet, DavConstants.DEPTH_0);
            getClient(sessionInfo).executeMethod(method);

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length != 1) {
                throw new LoginException("Login failed: Unknown workspace '" + workspaceName+ " '.");
            }

            DavPropertySet props = responses[0].getProperties(DavServletResponse.SC_OK);
            if (props.contains(DeltaVConstants.WORKSPACE)) {
                String wspHref = new HrefProperty(props.get(DeltaVConstants.WORKSPACE)).getHrefs().get(0).toString();
                String wspName = Text.unescape(Text.getName(wspHref, true));
                if (!wspName.equals(workspaceName)) {
                    throw new LoginException("Login failed: Invalid workspace name " + workspaceName);
                }
            } else {
                throw new LoginException("Login failed: Unknown workspace '" + workspaceName+ " '.");
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

        // create a subscription on the server
        String rootUri = uriResolver.getRootItemUri(workspaceName);
        String subscriptionId = subscribe(rootUri, S_INFO, null, sessionInfo, null);
        log.debug("Subscribed on server for session info " + sessionInfo);
        sessionInfo.setSubscriptionId(subscriptionId);
        return sessionInfo;
    }

    public void dispose(SessionInfo sessionInfo) throws RepositoryException {
        checkSessionInfo(sessionInfo);
        String subscriptionId = ((SessionInfoImpl)sessionInfo).getSubscriptionId();
        if (subscriptionId != null) {
            String rootUri = uriResolver.getRootItemUri(sessionInfo.getWorkspaceName());
            unsubscribe(rootUri, subscriptionId, sessionInfo);
        }
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
        ReportMethod method = null;
        try {
            String uri = getItemUri(itemId, sessionInfo);
            ReportInfo reportInfo = new ReportInfo(JcrPrivilegeReport.PRIVILEGES_REPORT);
            reportInfo.setContentElement(DomUtil.hrefToXml(uri, domFactory));

            method = new ReportMethod(uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName()), reportInfo);
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
            // is used.
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
        String rootUri = uriResolver.getRootItemUri(sessionInfo.getWorkspaceName());
        return uriResolver.getNodeId(rootUri, sessionInfo);
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
                throw new ItemNotFoundException("Unable to retrieve the item definition for " + itemId);
            }
            if (responses.length > 1) {
                throw new RepositoryException("Internal error: ambigous item definition found '" + itemId + "'.");
            }
            DavPropertySet propertySet = responses[0].getProperties(DavServletResponse.SC_OK);

            // check if definition matches the type of the id
            DavProperty rType = propertySet.get(DavPropertyName.RESOURCETYPE);
            if (rType.getValue() == null && itemId.denotesNode()) {
                throw new RepositoryException("Internal error: requested node definition and got property definition.");
            }

            NamespaceResolver resolver = new NamespaceResolverImpl(sessionInfo);

            // build the definition
            QItemDefinition definition = null;
            if (propertySet.contains(ItemResourceConstants.JCR_DEFINITION)) {
                DavProperty prop = propertySet.get(ItemResourceConstants.JCR_DEFINITION);
                Object value = prop.getValue();
                if (value != null && value instanceof Element) {
                    Element idfElem = (Element) value;
                    if (itemId.denotesNode()) {
                        definition = new QNodeDefinitionImpl(null, idfElem, resolver);
                    } else {
                        definition = new QPropertyDefinitionImpl(null, idfElem, resolver, getQValueFactory());
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
    public NodeInfo getNodeInfo(SessionInfo sessionInfo, NodeId nodeId) throws ItemNotFoundException, RepositoryException {
        // set of properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(ItemResourceConstants.JCR_NAME);
        nameSet.add(ItemResourceConstants.JCR_INDEX);
        nameSet.add(ItemResourceConstants.JCR_PARENT);
        nameSet.add(ItemResourceConstants.JCR_PRIMARYNODETYPE);
        nameSet.add(ItemResourceConstants.JCR_MIXINNODETYPES);
        nameSet.add(ItemResourceConstants.JCR_REFERENCES);
        nameSet.add(ItemResourceConstants.JCR_UUID);
        nameSet.add(ItemResourceConstants.JCR_PATH);
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
                if (isSameResource(uri, responses[i])) {
                    nodeResponse = responses[i];
                } else {
                    childResponses.add(responses[i]);
                }
            }

            if (nodeResponse == null) {
                throw new ItemNotFoundException("Unable to retrieve the node " + nodeId);
            }

            DavPropertySet propSet = nodeResponse.getProperties(DavServletResponse.SC_OK);
            Object type = propSet.get(DavPropertyName.RESOURCETYPE).getValue();
            if (type == null) {
                // the given id points to a Property instead of a Node
                throw new ItemNotFoundException("No node for id " + nodeId);
            }

            NodeId parentId = getParentId(propSet, sessionInfo);
            NodeId id = uriResolver.buildNodeId(parentId, nodeResponse, sessionInfo.getWorkspaceName());

            NamespaceResolver resolver = new NamespaceResolverImpl(sessionInfo);
            NodeInfoImpl nInfo = new NodeInfoImpl(id, parentId, propSet, resolver);
            if (propSet.contains(ItemResourceConstants.JCR_REFERENCES)) {
                HrefProperty refProp = new HrefProperty(propSet.get(ItemResourceConstants.JCR_REFERENCES));
                Iterator hrefIter = refProp.getHrefs().iterator();
                while(hrefIter.hasNext()) {
                    String propertyHref = hrefIter.next().toString();
                    PropertyId propertyId = uriResolver.getPropertyId(propertyHref, sessionInfo);
                    nInfo.addReference(propertyId);
                }
            }
            for (Iterator it = childResponses.iterator(); it.hasNext();) {
                MultiStatusResponse resp = (MultiStatusResponse) it.next();
                DavPropertySet childProps = resp.getProperties(DavServletResponse.SC_OK);
                if (childProps.contains(DavPropertyName.RESOURCETYPE) &&
                    childProps.get(DavPropertyName.RESOURCETYPE).getValue() != null) {
                    // any other resource type than default (empty) is represented by a node item
                    // --> ignore
                } else {
                    PropertyId childId = uriResolver.buildPropertyId(nInfo.getId(), resp, sessionInfo.getWorkspaceName());
                    nInfo.addPropertyId(childId);
                }
            }
            return nInfo;
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } catch (MalformedPathException e) {
            throw new RepositoryException(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#getItemInfos(SessionInfo, NodeId)
     */
    public Iterator getItemInfos(SessionInfo sessionInfo, NodeId nodeId) throws ItemNotFoundException, RepositoryException {
        // set of properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(ItemResourceConstants.JCR_NAME);
        nameSet.add(ItemResourceConstants.JCR_INDEX);
        nameSet.add(ItemResourceConstants.JCR_PARENT);
        nameSet.add(ItemResourceConstants.JCR_PRIMARYNODETYPE);
        nameSet.add(ItemResourceConstants.JCR_MIXINNODETYPES);
        nameSet.add(ItemResourceConstants.JCR_REFERENCES);
        nameSet.add(ItemResourceConstants.JCR_UUID);
        nameSet.add(ItemResourceConstants.JCR_PATH);
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
                if (isSameResource(uri, responses[i])) {
                    nodeResponse = responses[i];
                } else {
                    childResponses.add(responses[i]);
                }
            }

            if (nodeResponse == null) {
                throw new ItemNotFoundException("Unable to retrieve the node " + nodeId);
            }

            DavPropertySet propSet = nodeResponse.getProperties(DavServletResponse.SC_OK);
            Object type = propSet.get(DavPropertyName.RESOURCETYPE).getValue();
            if (type == null) {
                // the given id points to a Property instead of a Node
                throw new ItemNotFoundException("No node for id " + nodeId);
            }

            NamespaceResolver resolver = new NamespaceResolverImpl(sessionInfo);

            NodeId parentId = getParentId(propSet, sessionInfo);
            NodeId id = uriResolver.buildNodeId(parentId, nodeResponse, sessionInfo.getWorkspaceName());
            NodeInfoImpl nInfo = new NodeInfoImpl(id, parentId, propSet, resolver);
            if (propSet.contains(ItemResourceConstants.JCR_REFERENCES)) {
                HrefProperty refProp = new HrefProperty(propSet.get(ItemResourceConstants.JCR_REFERENCES));
                Iterator hrefIter = refProp.getHrefs().iterator();
                while(hrefIter.hasNext()) {
                    String propertyHref = hrefIter.next().toString();
                    PropertyId propertyId = uriResolver.getPropertyId(propertyHref, sessionInfo);
                    nInfo.addReference(propertyId);
                }
            }

            List infos = new ArrayList(responses.length);
            infos.add(nInfo);

            for (Iterator it = childResponses.iterator(); it.hasNext();) {
                MultiStatusResponse resp = (MultiStatusResponse) it.next();
                DavPropertySet childProps = resp.getProperties(DavServletResponse.SC_OK);
                if (childProps.contains(DavPropertyName.RESOURCETYPE) &&
                    childProps.get(DavPropertyName.RESOURCETYPE).getValue() != null) {
                    // any other resource type than default (empty) is represented by a node item
                    parentId = getParentId(childProps, sessionInfo);
                    id = uriResolver.buildNodeId(parentId, resp, sessionInfo.getWorkspaceName());
                    nInfo = new NodeInfoImpl(id, parentId, childProps, resolver);
                    if (childProps.contains(ItemResourceConstants.JCR_REFERENCES)) {
                        HrefProperty refProp = new HrefProperty(childProps.get(ItemResourceConstants.JCR_REFERENCES));
                        Iterator hrefIter = refProp.getHrefs().iterator();
                        while(hrefIter.hasNext()) {
                            String propertyHref = hrefIter.next().toString();
                            PropertyId propertyId = uriResolver.getPropertyId(propertyHref, sessionInfo);
                            nInfo.addReference(propertyId);
                        }
                    }
                    infos.add(nInfo);
                } else {
                    PropertyId childId = uriResolver.buildPropertyId(nInfo.getId(), resp, sessionInfo.getWorkspaceName());
                    nInfo.addPropertyId(childId);
                    // TODO: due to missing 'value/values' property PropertyInfo cannot be built
                }
            }
            return infos.iterator();
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } catch (MalformedPathException e) {
            throw new RepositoryException(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#getChildInfos(SessionInfo, NodeId)
     */
    public Iterator getChildInfos(SessionInfo sessionInfo, NodeId parentId) throws ItemNotFoundException, RepositoryException {
        // set of properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(ItemResourceConstants.JCR_NAME);
        nameSet.add(ItemResourceConstants.JCR_INDEX);
        nameSet.add(ItemResourceConstants.JCR_PARENT);
        nameSet.add(ItemResourceConstants.JCR_PRIMARYNODETYPE);
        nameSet.add(ItemResourceConstants.JCR_MIXINNODETYPES);
        nameSet.add(ItemResourceConstants.JCR_REFERENCES);
        nameSet.add(ItemResourceConstants.JCR_UUID);
        nameSet.add(ItemResourceConstants.JCR_PATH);
        nameSet.add(DavPropertyName.RESOURCETYPE);

        DavMethodBase method = null;
        try {
            String uri = getItemUri(parentId, sessionInfo);
            method = new PropFindMethod(uri, nameSet, DEPTH_1);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length < 1) {
                throw new ItemNotFoundException("Unable to retrieve the node with id " + parentId);
            } else if (responses.length == 1) {
                // no child nodes nor properties
                return Collections.EMPTY_LIST.iterator();
            }

            NamespaceResolver resolver = new NamespaceResolverImpl(sessionInfo);

            List childEntries = new ArrayList();
            for (int i = 0; i < responses.length; i++) {
                if (!isSameResource(uri, responses[i])) {
                    MultiStatusResponse resp = responses[i];
                    DavPropertySet childProps = resp.getProperties(DavServletResponse.SC_OK);
                    if (childProps.contains(DavPropertyName.RESOURCETYPE) &&
                        childProps.get(DavPropertyName.RESOURCETYPE).getValue() != null) {

                        QName qName = getQName(childProps, resolver);
                        int index = getIndex(childProps);
                        String uuid = getUniqueID(childProps);

                        ChildInfo childInfo = new ChildInfoImpl(qName, uuid, index);
                        childEntries.add(childInfo);
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

    /**
     * @see RepositoryService#getPropertyInfo(SessionInfo, PropertyId)
     */
    public PropertyInfo getPropertyInfo(SessionInfo sessionInfo, PropertyId propertyId) throws ItemNotFoundException, RepositoryException {
        // set of Dav-properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(ItemResourceConstants.JCR_NAME);
        nameSet.add(ItemResourceConstants.JCR_PARENT);
        nameSet.add(ItemResourceConstants.JCR_TYPE);
        nameSet.add(ItemResourceConstants.JCR_VALUE);
        nameSet.add(ItemResourceConstants.JCR_VALUES);
        nameSet.add(ItemResourceConstants.JCR_PATH);
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

            DavPropertySet propSet = responses[0].getProperties(DavServletResponse.SC_OK);
            NodeId parentId = getParentId(propSet, sessionInfo);
            PropertyId id = uriResolver.buildPropertyId(parentId, responses[0], sessionInfo.getWorkspaceName());

            NamespaceResolver resolver = new NamespaceResolverImpl(sessionInfo);
            PropertyInfo pInfo = new PropertyInfoImpl(id, parentId, propSet,
                    resolver, valueFactory, getQValueFactory());
            return pInfo;
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } catch (MalformedPathException e) {
            throw new RepositoryException(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#createBatch(ItemId, SessionInfo)
     */
    public Batch createBatch(ItemId itemId, SessionInfo sessionInfo) throws RepositoryException {
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
                Iterator it = batchImpl.methods();
                while (it.hasNext()) {
                    method = (DavMethod) it.next();
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
    public void importXml(SessionInfo sessionInfo, NodeId parentId, InputStream xmlStream, int uuidBehaviour) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        // TODO: improve. currently random name is built instead of retrieving name of new resource from top-level xml element within stream
        QName nodeName = new QName(QName.NS_DEFAULT_URI, UUID.randomUUID().toString());
        String uri = getItemUri(parentId, nodeName, sessionInfo);
        MkColMethod method = new MkColMethod(uri);
        method.addRequestHeader(ItemResourceConstants.IMPORT_UUID_BEHAVIOR, new Integer(uuidBehaviour).toString());
        method.setRequestEntity(new InputStreamRequestEntity(xmlStream, "text/xml"));
        execute(method, sessionInfo);
    }

    /**
     * @see RepositoryService#move(SessionInfo, NodeId, NodeId, QName)
     */
    public void move(SessionInfo sessionInfo, NodeId srcNodeId, NodeId destParentNodeId, QName destName) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        String uri = getItemUri(srcNodeId, sessionInfo);
        String destUri = getItemUri(destParentNodeId, destName, sessionInfo);
        MoveMethod method = new MoveMethod(uri, destUri, true);
        execute(method, sessionInfo);
    }

    /**
     * @see RepositoryService#copy(SessionInfo, String, NodeId, NodeId, QName)
     */
    public void copy(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, QName destName) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        String uri = uriResolver.getItemUri(srcNodeId, srcWorkspaceName, sessionInfo);
        String destUri = getItemUri(destParentNodeId, destName, sessionInfo);
        CopyMethod method = new CopyMethod(uri, destUri, true, false);
        execute(method, sessionInfo);
    }

    /**
     * @see RepositoryService#update(SessionInfo, NodeId, String)
     */
    public void update(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        String workspUri = uriResolver.getWorkspaceUri(srcWorkspaceName);

        update(uri, new String[] {workspUri}, UpdateInfo.UPDATE_BY_WORKSPACE, false, sessionInfo);
    }

    /**
     * @see RepositoryService#clone(SessionInfo, String, NodeId, NodeId, QName, boolean)
     */
    public void clone(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, QName destName, boolean removeExisting) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        // TODO: missing implementation
        throw new UnsupportedOperationException("Missing implementation");
    }

    /**
     * @see RepositoryService#getLockInfo(SessionInfo, NodeId)
     */
    public LockInfo getLockInfo(SessionInfo sessionInfo, NodeId nodeId) throws LockException, RepositoryException {
        // set of Dav-properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(DavPropertyName.LOCKDISCOVERY);
        nameSet.add(ItemResourceConstants.JCR_PARENT);

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
                throw new ItemNotFoundException("Unable to retrieve the property with id " + nodeId);
            }

            DavPropertySet ps = responses[0].getProperties(DavServletResponse.SC_OK);
            if (ps.contains(DavPropertyName.LOCKDISCOVERY)) {
                DavProperty p = ps.get(DavPropertyName.LOCKDISCOVERY);
                LockDiscovery ld = LockDiscovery.createFromXml(p.toXml(domFactory));
                NodeId parentId = getParentId(ps, sessionInfo);
                return retrieveLockInfo(ld, sessionInfo, nodeId, parentId);
            }  else {
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
     * @see RepositoryService#lock(SessionInfo, NodeId, boolean, boolean)
     */
    public LockInfo lock(SessionInfo sessionInfo, NodeId nodeId, boolean deep, boolean sessionScoped) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        try {
            String uri = getItemUri(nodeId, sessionInfo);
            Scope scope = (sessionScoped) ? ItemResourceConstants.EXCLUSIVE_SESSION : Scope.EXCLUSIVE;
            LockMethod method = new LockMethod(uri, scope, Type.WRITE,
                sessionInfo.getUserID(), DavConstants.INFINITE_TIMEOUT, deep);
            execute(method, sessionInfo);

            String lockToken = method.getLockToken();
            sessionInfo.addLockToken(lockToken);

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
    public void refreshLock(SessionInfo sessionInfo, NodeId nodeId) throws LockException, RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        // since sessionInfo does not allow to retrieve token by NodeId,
        // pass all available lock tokens to the LOCK method (TODO: correct?)
        LockMethod method = new LockMethod(uri, DavConstants.INFINITE_TIMEOUT, sessionInfo.getLockTokens());
        execute(method, sessionInfo);
    }

    /**
     * @see RepositoryService#unlock(SessionInfo, NodeId)
     */
    public void unlock(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        // Note: since sessionInfo does not allow to identify the id of the
        // lock holding node, we need to access the token via lockInfo
        // TODO: review this.
        LockInfo lInfo = getLockInfo(sessionInfo, nodeId);
        String lockToken = lInfo.getLockToken();

        // TODO: ev. additional check if lt is present on the sessionInfo?

        UnLockMethod method = new UnLockMethod(uri, lockToken);
        execute(method, sessionInfo);

        sessionInfo.removeLockToken(lockToken);
    }

    private LockInfo retrieveLockInfo(LockDiscovery lockDiscovery, SessionInfo sessionInfo,
                                      NodeId nodeId, NodeId parentId)
        throws LockException, RepositoryException {
        List activeLocks = (List) lockDiscovery.getValue();
        Iterator it = activeLocks.iterator();
        ActiveLock activeLock = null;
        while (it.hasNext()) {
            ActiveLock l = (ActiveLock) it.next();
            Scope sc = l.getScope();
            if (l.getType() == Type.WRITE && (sc == Scope.EXCLUSIVE || sc == ItemResourceConstants.EXCLUSIVE_SESSION)) {
                if (activeLock != null) {
                    throw new RepositoryException("Node " + nodeId + " contains multiple exclusive write locks.");
                } else {
                    activeLock = l;
                }
            }
        }
        if (activeLock == null) {
            throw new LockException("No lock present on node " + nodeId);
        }
        if (activeLock.isDeep() && parentId != null) {
            // try if lock is inherited
            try {
                return getLockInfo(sessionInfo, parentId);
            } catch (LockException e) {
                // no lock on parent
                return new LockInfoImpl(activeLock, nodeId);
            }
        }
        // no deep lock or parentID == null or lock is not present on parent
        // -> nodeID is lockHolding Id.
        return new LockInfoImpl(activeLock, nodeId);
    }

    /**
     * @see RepositoryService#checkin(SessionInfo, NodeId)
     */
    public void checkin(SessionInfo sessionInfo, NodeId nodeId) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        CheckinMethod method = new CheckinMethod(uri);
        execute(method, sessionInfo);
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
     * @see RepositoryService#removeVersion(SessionInfo, NodeId, NodeId)
     */
    public void removeVersion(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId) throws ReferentialIntegrityException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        String uri = getItemUri(versionId, sessionInfo);
        DeleteMethod method = new DeleteMethod(uri);
        execute(method, sessionInfo);
    }

    /**
     * @see RepositoryService#restore(SessionInfo, NodeId, NodeId, boolean)
     */
    public void restore(SessionInfo sessionInfo, NodeId nodeId, NodeId versionId, boolean removeExisting) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        String vUri = getItemUri(versionId, sessionInfo);

        update(uri, new String[] {vUri}, UpdateInfo.UPDATE_BY_VERSION, removeExisting, sessionInfo);
    }

    /**
     * @see RepositoryService#restore(SessionInfo, NodeId[], boolean)
     */
    public void restore(SessionInfo sessionInfo, NodeId[] versionIds, boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException {
        String uri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());
        String[] vUris = new String[versionIds.length];
        for (int i = 0; i < versionIds.length; i++) {
            vUris[i] = getItemUri(versionIds[i], sessionInfo);
        }

        update(uri, vUris, UpdateInfo.UPDATE_BY_VERSION, removeExisting, sessionInfo);
    }

    private void update(String uri, String[] updateSource, int updateType, boolean removeExisting, SessionInfo sessionInfo) throws RepositoryException {
        try {
            UpdateInfo uInfo;
            if (removeExisting) {
                Element uElem = UpdateInfo.createUpdateElement(updateSource, updateType, domFactory);
                DomUtil.addChildElement(uElem, ItemResourceConstants.XML_REMOVEEXISTING, ItemResourceConstants.NAMESPACE);
                uInfo = new UpdateInfo(uElem);
            } else {
                uInfo = new UpdateInfo(updateSource, updateType, new DavPropertyNameSet());
            }

            UpdateMethod method = new UpdateMethod(uri, uInfo);
            execute(method, sessionInfo);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        }
    }
    /**
     * @see RepositoryService#merge(SessionInfo, NodeId, String, boolean)
     */
    public Iterator merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        try {
            String wspHref = uriResolver.getWorkspaceUri(srcWorkspaceName);
            Element mElem = MergeInfo.createMergeElement(new String[] {wspHref}, bestEffort, false, domFactory);
            MergeInfo mInfo = new MergeInfo(mElem);

            MergeMethod method = new MergeMethod(getItemUri(nodeId, sessionInfo), mInfo);
            execute(method, sessionInfo);

            MultiStatusResponse[] resps = method.getResponseBodyAsMultiStatus().getResponses();
            List failedIds = new ArrayList(resps.length);
            for (int i = 0; i < resps.length; i++) {
                String href = resps[i].getHref();
                failedIds.add(uriResolver.getNodeId(href, sessionInfo));
            }
            return failedIds.iterator();
        } catch (IOException e) {
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

            PropPatchMethod method = new PropPatchMethod(getItemUri(nodeId, sessionInfo), changeList);
            execute(method, sessionInfo);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see RepositoryService#addVersionLabel(SessionInfo,NodeId,NodeId,QName,boolean)
     */
    public void addVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, QName label, boolean moveLabel) throws VersionException, RepositoryException {
        try {
            String uri = getItemUri(versionId, sessionInfo);
            String strLabel = NameFormat.format(label, new NamespaceResolverImpl(sessionInfo));
            LabelMethod method = new LabelMethod(uri, strLabel, (moveLabel) ? LabelInfo.TYPE_SET : LabelInfo.TYPE_ADD);
            execute(method, sessionInfo);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (NoPrefixDeclaredException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see RepositoryService#removeVersionLabel(SessionInfo,NodeId,NodeId,QName)
     */
    public void removeVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, QName label) throws VersionException, RepositoryException {
        try {
            String uri = getItemUri(versionId, sessionInfo);
            String strLabel = NameFormat.format(label, new NamespaceResolverImpl(sessionInfo));
            LabelMethod method = new LabelMethod(uri, strLabel, LabelInfo.TYPE_REMOVE);
            execute(method, sessionInfo);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (NoPrefixDeclaredException e) {
            throw new RepositoryException(e);
        }
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

    public void checkQueryStatement(SessionInfo sessionInfo,
                                    String statement,
                                    String language,
                                    Map namespaces)
            throws InvalidQueryException, RepositoryException {
        // TODO implement
    }

    /**
     * @see RepositoryService#executeQuery(SessionInfo, String, String, Map)
     */
    public QueryInfo executeQuery(SessionInfo sessionInfo, String statement, String language, Map namespaces) throws RepositoryException {
        SearchMethod method = null;
        try {
            String uri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());
            SearchInfo sInfo = new SearchInfo(language,
                    Namespace.EMPTY_NAMESPACE, statement, namespaces);
            method = new SearchMethod(uri, sInfo);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            MultiStatus ms = method.getResponseBodyAsMultiStatus();
            NamespaceResolver resolver = new NamespaceResolverImpl(sessionInfo);
            return new QueryInfoImpl(ms, sessionInfo, uriResolver,
                resolver, valueFactory, getQValueFactory());
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
     * @see RepositoryService#createEventFilter(SessionInfo, int, org.apache.jackrabbit.name.Path, boolean, String[], org.apache.jackrabbit.name.QName[], boolean)
     */
    public EventFilter createEventFilter(SessionInfo sessionInfo,
                                         int eventTypes,
                                         Path absPath,
                                         boolean isDeep,
                                         String[] uuids,
                                         QName[] nodeTypeNames,
                                         boolean noLocal)
            throws UnsupportedRepositoryOperationException, RepositoryException {
        // resolve node type names
        // todo what if new node types become available while event filter is still in use?
        Set resolvedTypeNames = null;
        if (nodeTypeNames != null) {
            resolvedTypeNames = new HashSet();
            // make sure node type definitions are available
            if (nodeTypeDefinitions.size() == 0) {
                getQNodeTypeDefinitions(sessionInfo);
            }
            synchronized (nodeTypeDefinitions) {
                for (int i = 0; i < nodeTypeNames.length; i++) {
                    resolveNodeType(resolvedTypeNames, nodeTypeNames[i]);
                }
            }
        }
        return new EventFilterImpl(eventTypes, absPath, isDeep, uuids,
                resolvedTypeNames, noLocal);
    }

    /**
     * @see RepositoryService#getEvents(SessionInfo, long, EventFilter[])
     */
    public EventBundle[] getEvents(SessionInfo sessionInfo, long timeout, EventFilter[] filters)
            throws RepositoryException, UnsupportedRepositoryOperationException {
        checkSessionInfo(sessionInfo);

        SessionInfoImpl sessionInfoImpl = (SessionInfoImpl)sessionInfo;
        String rootUri = uriResolver.getRootItemUri(sessionInfo.getWorkspaceName());

        return poll(rootUri, sessionInfoImpl.getSubscriptionId(), timeout, sessionInfoImpl);
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

            if (batchId != null) {
                // add batchId as separate header
                CodedUrlHeader ch = new CodedUrlHeader(TransactionConstants.HEADER_TRANSACTIONID, batchId);
                method.setRequestHeader(ch.getHeaderName(), ch.getHeaderValue());
            }

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();
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

    private void resolveNodeType(Set resolved, QName ntName) {
        if (!resolved.add(ntName)) {
            return;
        }
        QNodeTypeDefinition def = (QNodeTypeDefinition) nodeTypeDefinitions.get(ntName);
        if (def != null) {
            QName[] supertypes = def.getSupertypes();
            for (int i = 0; i < supertypes.length; i++) {
                resolveNodeType(resolved, supertypes[i]);
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
                Element discEl = disc.toXml(domFactory);
                ElementIterator it = DomUtil.getChildren(discEl,
                        ObservationConstants.XML_EVENTBUNDLE,
                        ObservationConstants.NAMESPACE);
                List bundles = new ArrayList();
                while (it.hasNext()) {
                    Element bundleElement = it.nextElement();
                    String value = DomUtil.getAttribute(bundleElement,
                            ObservationConstants.XML_EVENT_TRANSACTION_ID,
                            ObservationConstants.NAMESPACE);
                    // check if it matches a batch id recently submitted
                    boolean isLocal = false;
                    if (value != null) {
                        isLocal = value.equals(sessionInfo.getLastBatchId());
                    }
                    bundles.add(new EventBundleImpl(
                            buildEventList(bundleElement, sessionInfo),
                            isLocal,
                            null)); // TODO: bundle id is missing
                }
                events = (EventBundle[]) bundles.toArray(new EventBundle[bundles.size()]);
            }
            return events;
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

    private List buildEventList(Element bundleElement, SessionInfo sessionInfo) {
        List events = new ArrayList();
        ElementIterator eventElementIterator = DomUtil.getChildren(bundleElement, ObservationConstants.XML_EVENT, ObservationConstants.NAMESPACE);
        while (eventElementIterator.hasNext()) {
            Element evElem = eventElementIterator.nextElement();
            Element typeEl = DomUtil.getChildElement(evElem, ObservationConstants.XML_EVENTTYPE, ObservationConstants.NAMESPACE);
            EventType[] et = DefaultEventType.createFromXml(typeEl);
            if (et.length == 0 || et.length > 1) {
                // should not occur.
                log.error("Ambigous event type definition: expected one single eventtype.");
                continue;
            }

            String href = DomUtil.getChildTextTrim(evElem, DavConstants.XML_HREF, DavConstants.NAMESPACE);

            int type;
            Path eventPath;
            try {
                type = SubscriptionImpl.getJcrEventType(et[0]);
                eventPath = uriResolver.getQPath(href, sessionInfo);
            } catch (DavException e) {
                // should not occur
                log.error("Internal error while building Event", e);
                continue;
            } catch (RepositoryException e) {
                // should not occur
                log.error("Internal error while building Event", e);
                continue;
            }

            ItemId eventId = null;
            try {
                if (type == Event.NODE_ADDED || type == Event.NODE_REMOVED) {
                    eventId = uriResolver.getNodeId(href, sessionInfo);
                } else {
                    eventId = uriResolver.getPropertyId(href, sessionInfo);
                }
            } catch (RepositoryException e) {
                log.warn("Unable to build event itemId: ", e);
            }
            String parentHref = Text.getRelativeParent(href, 1, true);
            NodeId parentId = null;
            try {
                parentId = uriResolver.getNodeId(parentHref, sessionInfo);
            } catch (RepositoryException e) {
                log.warn("Unable to build event parentId: ", e);
            }

            events.add(new EventImpl(eventId, eventPath, parentId, type, evElem));
        }

        return events;
    }

    /**
     * @see RepositoryService#getRegisteredNamespaces(SessionInfo)
     */
    public Map getRegisteredNamespaces(SessionInfo sessionInfo) throws RepositoryException {
        ReportInfo info = new ReportInfo(RegisteredNamespacesReport.REGISTERED_NAMESPACES_REPORT, DEPTH_0);
        ReportMethod method = null;
        try {
            method = new ReportMethod(uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName()), info);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            Document doc = method.getResponseBodyAsDocument();
            Map namespaces = new HashMap();
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

        Map namespaces = new HashMap(nsCache.getNamespaces());
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
        Map namespaces = new HashMap(nsCache.getNamespaces());
        // remove pair that needs to be unregistered
        namespaces.remove(prefix);

        internalSetNamespaces(sessionInfo, namespaces);
        // adjust internal mappings:
        // TODO: not correct since nsRegistry is retrieved from each session
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
    private void internalSetNamespaces(SessionInfo sessionInfo, Map namespaces) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        DavPropertySet setProperties = new DavPropertySet();
        setProperties.add(new NamespacesProperty(namespaces));

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
    public Iterator getQNodeTypeDefinitions(SessionInfo sessionInfo) throws RepositoryException {
        ReportInfo info = new ReportInfo(NodeTypesReport.NODETYPES_REPORT, DEPTH_0);
        info.setContentElement(DomUtil.createElement(domFactory, NodeTypeConstants.XML_REPORT_ALLNODETYPES, NodeTypeConstants.NAMESPACE));

        ReportMethod method = null;
        try {
            String workspaceUri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());
            method = new ReportMethod(workspaceUri, info);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            Document reportDoc = method.getResponseBodyAsDocument();
            return retrieveQNodeTypeDefinitions(sessionInfo, reportDoc);
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
    public Iterator getQNodeTypeDefinitions(SessionInfo sessionInfo, QName[] nodetypeNames) throws RepositoryException {
        ReportMethod method = null;
        try {
            NamespaceResolver resolver = new NamespaceResolverImpl(sessionInfo);

            ReportInfo info = new ReportInfo(NodeTypesReport.NODETYPES_REPORT, DEPTH_0);
            for (int i = 0; i < nodetypeNames.length; i++) {
                Element el = DomUtil.createElement(domFactory, NodeTypeConstants.XML_NODETYPE, NodeTypeConstants.NAMESPACE);
                String jcrName = NameFormat.format(nodetypeNames[i], resolver);
                DomUtil.addChildElement(el, NodeTypeConstants.XML_NODETYPENAME, NodeTypeConstants.NAMESPACE, jcrName);
                info.setContentElement(el);
            }

            String workspaceUri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());
            method = new ReportMethod(workspaceUri, info);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            Document reportDoc = method.getResponseBodyAsDocument();
            return retrieveQNodeTypeDefinitions(sessionInfo, reportDoc);
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
     * 
     * @param sessionInfo
     * @param reportDoc
     * @return
     * @throws RepositoryException
     */
    private Iterator retrieveQNodeTypeDefinitions(SessionInfo sessionInfo, Document reportDoc) throws RepositoryException {
        ElementIterator it = DomUtil.getChildren(reportDoc.getDocumentElement(), NodeTypeConstants.NODETYPE_ELEMENT, null);
            List ntDefs = new ArrayList();
            NamespaceResolver resolver = new NamespaceResolverImpl(sessionInfo);
            while (it.hasNext()) {
                ntDefs.add(new QNodeTypeDefinitionImpl(it.nextElement(), resolver, getQValueFactory()));
            }
            // refresh node type definitions map
            synchronized (nodeTypeDefinitions) {
                nodeTypeDefinitions.clear();
                for (Iterator defIt = ntDefs.iterator(); defIt.hasNext(); ) {
                    QNodeTypeDefinition def = (QNodeTypeDefinition) defIt.next();
                    nodeTypeDefinitions.put(def.getQName(), def);
                }
            }
            return ntDefs.iterator();
    }

    /**
     * The XML elements and attributes used in serialization
     */
    private static final Namespace SV_NAMESPACE = Namespace.getNamespace(QName.NS_SV_PREFIX, QName.NS_SV_URI);
    private static final String NODE_ELEMENT = "node";
    private static final String PROPERTY_ELEMENT = "property";
    private static final String VALUE_ELEMENT = "value";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String TYPE_ATTRIBUTE = "type";

    //------------------------------------------------< Inner Class 'Batch' >---
    private class BatchImpl implements Batch {

        private final SessionInfo sessionInfo;
        private final ItemId targetId;
        private final List methods = new ArrayList();
        private final NamespaceResolver nsResolver;

        private String batchId;

        private boolean isConsumed = false;

        private BatchImpl(ItemId targetId, SessionInfo sessionInfo) {
            this.targetId = targetId;
            this.sessionInfo = sessionInfo;
            this.nsResolver = new NamespaceResolverImpl(sessionInfo);
        }

        private HttpClient start() throws RepositoryException {
            checkConsumed();
            String uri = getItemUri(targetId, sessionInfo);
            try {
                // start special 'lock'
                LockMethod method = new LockMethod(uri, TransactionConstants.LOCAL, TransactionConstants.TRANSACTION, null, DavConstants.INFINITE_TIMEOUT, true);
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

        private Iterator methods() {
            return methods.iterator();
        }

        //----------------------------------------------------------< Batch >---
        /**
         * @see Batch#addNode(NodeId, QName, QName, String)
         */
        public void addNode(NodeId parentId, QName nodeName, QName nodetypeName, String uuid) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, NoSuchNodeTypeException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            try {
                // TODO: TOBEFIXED. WebDAV does not allow MKCOL for existing resource -> problem with SNS
                // use fake name instead (see also #importXML)
                QName fakeName = new QName(QName.NS_DEFAULT_URI, UUID.randomUUID().toString());
                String uri = getItemUri(parentId, fakeName, sessionInfo);
                MkColMethod method = new MkColMethod(uri);

                // build 'sys-view' for the node to create and append it as request body
                Document body = DomUtil.BUILDER_FACTORY.newDocumentBuilder().newDocument();
                Element nodeElement = DomUtil.addChildElement(body, NODE_ELEMENT, SV_NAMESPACE);
                String nameAttr = NameFormat.format(nodeName, nsResolver);
                DomUtil.setAttribute(nodeElement, NAME_ATTRIBUTE, SV_NAMESPACE, nameAttr);

                // nodetype must never be null
                Element propElement = DomUtil.addChildElement(nodeElement, PROPERTY_ELEMENT, SV_NAMESPACE);
                DomUtil.setAttribute(propElement, NAME_ATTRIBUTE, SV_NAMESPACE, NameFormat.format(QName.JCR_PRIMARYTYPE, nsResolver));
                DomUtil.setAttribute(propElement, TYPE_ATTRIBUTE, SV_NAMESPACE, PropertyType.nameFromValue(PropertyType.NAME));
                DomUtil.addChildElement(propElement, VALUE_ELEMENT, SV_NAMESPACE, NameFormat.format(nodetypeName, nsResolver));
                // optional uuid
                if (uuid != null) {
                    propElement = DomUtil.addChildElement(nodeElement, PROPERTY_ELEMENT, SV_NAMESPACE);
                    DomUtil.setAttribute(propElement, NAME_ATTRIBUTE, SV_NAMESPACE, NameFormat.format(QName.JCR_UUID, nsResolver));
                    DomUtil.setAttribute(propElement, TYPE_ATTRIBUTE, SV_NAMESPACE, PropertyType.nameFromValue(PropertyType.STRING));
                    DomUtil.addChildElement(propElement, VALUE_ELEMENT, SV_NAMESPACE, uuid);
                }
                method.setRequestBody(body);

                methods.add(method);
            } catch (IOException e) {
                throw new RepositoryException(e);
            } catch (ParserConfigurationException e) {
                throw new RepositoryException(e);
            } catch (NoPrefixDeclaredException e) {
                throw new RepositoryException(e);
            }
        }

        /**
         * @see Batch#addProperty(NodeId, QName, QValue)
         */
        public void addProperty(NodeId parentId, QName propertyName, QValue value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            Value jcrValue = ValueFormat.getJCRValue(value, nsResolver, valueFactory);
            ValuesProperty vp = new ValuesProperty(jcrValue);
            internalAddProperty(parentId, propertyName, vp);
        }

        /**
         * @see Batch#addProperty(NodeId, QName, QValue[])
         */
        public void addProperty(NodeId parentId, QName propertyName, QValue[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            Value[] jcrValues = new Value[values.length];
            for (int i = 0; i < values.length; i++) {
                jcrValues[i] = ValueFormat.getJCRValue(values[i], nsResolver, valueFactory);
            }
            ValuesProperty vp = new ValuesProperty(jcrValues);
            internalAddProperty(parentId, propertyName, vp);
        }

        private void internalAddProperty(NodeId parentId, QName propertyName, ValuesProperty vp) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            try {
                String uri = getItemUri(parentId, propertyName, sessionInfo);
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
            DavPropertySet setProperties = new DavPropertySet();
            if (value == null) {
                // setting property value to 'null' is identical to a removal
                remove(propertyId);
            } else {
                // qualified value must be converted to jcr value
                Value jcrValue = ValueFormat.getJCRValue(value, nsResolver, valueFactory);
                ValuesProperty vp = new ValuesProperty(jcrValue);
                setProperties.add(vp);
            }
            internalSetValue(propertyId, setProperties);
        }

        /**
         * @see Batch#setValue(PropertyId, QValue[])
         */
        public void setValue(PropertyId propertyId, QValue[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            DavPropertySet setProperties = new DavPropertySet();
            if (values == null) {
                // setting property value to 'null' is identical to a removal
                remove(propertyId);
            } else {
                // qualified values must be converted to jcr values
                Value[] jcrValues = new Value[values.length];
                for (int i = 0; i < values.length; i++) {
                    jcrValues[i] = ValueFormat.getJCRValue(values[i], nsResolver, valueFactory);
                }
                setProperties.add(new ValuesProperty(jcrValues));
            }
            internalSetValue(propertyId, setProperties);
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

            methods.add(method);
        }

        /**
         * @see Batch#reorderNodes(NodeId, NodeId, NodeId)
         */
        public void reorderNodes(NodeId parentId, NodeId srcNodeId, NodeId beforeNodeId) throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException, ItemNotFoundException, LockException, AccessDeniedException, RepositoryException {
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
                        ntNames[i] = NameFormat.format(mixinNodeTypeIds[i], nsResolver);
                    }
                    setProperties = new DavPropertySet();
                    setProperties.add(new NodeTypeProperty(ItemResourceConstants.JCR_MIXINNODETYPES, ntNames, false));
                    removeProperties = new DavPropertyNameSet();
                }

                String uri = getItemUri(nodeId, sessionInfo);
                PropPatchMethod method = new PropPatchMethod(uri, setProperties, removeProperties);

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

            methods.add(method);
        }
    }

    //----------------------------------------------< NamespaceResolverImpl >---

    /**
     * Implements a namespace resolver based on a session info.
     */
    private class NamespaceResolverImpl implements NamespaceResolver {

        private final SessionInfo sessionInfo;

        /**
         * Creates a new namespace resolver using the given session info.
         *
         * @param sessionInfo the session info to contact the repository.
         */
        NamespaceResolverImpl(SessionInfo sessionInfo) {
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

        /**
         * @inheritDoc
         */
        public QName getQName(String jcrName) throws IllegalNameException, UnknownPrefixException {
            return NameFormat.parse(jcrName, this);
        }

        /**
         * @inheritDoc
         */
        public String getJCRName(QName qName) throws NoPrefixDeclaredException {
            return NameFormat.format(qName, this);
        }
    }

    private static class NamespaceCache extends AbstractNamespaceResolver {

        private final HashMap prefixToURI = new HashMap();
        private final HashMap uriToPrefix = new HashMap();

        public Map getNamespaces() {
            return new HashMap(prefixToURI);
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
            String uri = (String) prefixToURI.get(prefix);
            if (uri != null) {
                return uri;
            } else {
                throw new NamespaceException(prefix + ": is not a registered namespace prefix.");
            }
        }

        public String getPrefix(String uri) throws NamespaceException {
            String prefix = (String) uriToPrefix.get(uri);
            if (prefix != null) {
                return prefix;
            } else {
                throw new NamespaceException(uri + ": is not a registered namespace uri.");
            }
        }
    }
}
