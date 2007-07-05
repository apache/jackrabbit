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
package org.apache.jackrabbit.spi.xml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.Arrays;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.LoginException;
import javax.jcr.MergeException;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.core.nodetype.ItemDef;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class XMLRepositoryService implements RepositoryService {

    private final Document document;

    private final QNodeTypeDefinition[] types;

    private final QNodeDefinition nodeDefinition;

    private final QPropertyDefinition primaryTypeDefinition;

    private final QPropertyDefinition propertyDefinition;
    
    public XMLRepositoryService(Document document, QNodeTypeDefinition[] types)
            throws RepositoryException {
        this.document = document;
        this.types = types;
        this.nodeDefinition =
            getNodeDefinition(QName.NT_UNSTRUCTURED, ItemDef.ANY_NAME);
        this.primaryTypeDefinition =
            getPropertyDefinition(QName.NT_BASE, QName.JCR_PRIMARYTYPE);
        this.propertyDefinition =
            getPropertyDefinition(QName.NT_UNSTRUCTURED, ItemDef.ANY_NAME);
    }

    private QNodeTypeDefinition getNodeTypeDefinition(QName name)
            throws RepositoryException {
        for (int i = 0; i < types.length; i++) {
            if (name.equals(types[i].getQName())) {
                return types[i];
            }
        }
        throw new IllegalStateException("Node type not found: " + name);
    }

    private QNodeDefinition getNodeDefinition(QName type, QName name)
            throws RepositoryException {
        QNodeDefinition[] nodes =
            getNodeTypeDefinition(type).getChildNodeDefs();
        for (int i = 0; i < nodes.length; i++) {
            if (name.equals(nodes[i].getQName())) {
                return nodes[i];
            }
        }
        throw new RepositoryException("Node definition not found: " + name);
    }

    private QPropertyDefinition getPropertyDefinition(QName type, QName name)
            throws RepositoryException {
        QPropertyDefinition[] properties =
            getNodeTypeDefinition(type).getPropertyDefs();
        for (int i = 0; i < properties.length; i++) {
            if (name.equals(properties[i].getQName())
                    && !properties[i].isMultiple()) {
                return properties[i];
            }
        }
        throw new RepositoryException("Property definition not found: " + name);
    }

    //---------------------------------------------------< RepositoryService >

    /**
     * Returns the descriptors of the XML repository.
     *
     * @return repository descriptors
     */
    public Map getRepositoryDescriptors() {
        Map descriptors = new HashMap();
        descriptors.put(Repository.LEVEL_1_SUPPORTED, Boolean.TRUE.toString());
        descriptors.put(Repository.LEVEL_2_SUPPORTED, Boolean.FALSE.toString());
        descriptors.put(Repository.OPTION_LOCKING_SUPPORTED, Boolean.FALSE.toString());
        descriptors.put(Repository.OPTION_OBSERVATION_SUPPORTED, Boolean.FALSE.toString());
        descriptors.put(Repository.OPTION_QUERY_SQL_SUPPORTED, Boolean.FALSE.toString());
        descriptors.put(Repository.OPTION_TRANSACTIONS_SUPPORTED, Boolean.FALSE.toString());
        descriptors.put(Repository.OPTION_VERSIONING_SUPPORTED, Boolean.FALSE.toString());
        descriptors.put(Repository.QUERY_XPATH_DOC_ORDER, Boolean.FALSE.toString());
        descriptors.put(Repository.QUERY_XPATH_POS_INDEX, Boolean.FALSE.toString());
        descriptors.put(Repository.REP_NAME_DESC, "Apache Jackrabbit SPI XML");
        descriptors.put(Repository.REP_VENDOR_DESC, "Apache Software Foundation");
        descriptors.put(Repository.REP_VENDOR_URL_DESC, "http://jackrabbit.apache.org/");
        descriptors.put(Repository.REP_VERSION_DESC, "1.3-SNAPSHOT");
        descriptors.put(Repository.SPEC_NAME_DESC, "Content Repository for Java Technology API");
        descriptors.put(Repository.SPEC_VERSION_DESC, "1.0");
        return descriptors;
    }

    /**
     * Obtains session info for the XML workspace.
     *
     * @param credentials ignored
     * @param workspace workspace name, should be <code>null</code> or "xml"
     * @return session info
     * @throws NoSuchWorkspaceException if the workspace name is not known
     */
    public SessionInfo obtain(Credentials credentials, String workspace)
            throws NoSuchWorkspaceException {
        if (workspace == null || workspace.equals("xml")) {
            if (credentials instanceof SimpleCredentials) {
                return new XMLSessionInfo(
                        ((SimpleCredentials) credentials).getUserID());
            } else {
                return new XMLSessionInfo("");
            }
        } else {
            throw new NoSuchWorkspaceException(
                    "Unknown workspace: " + workspace);
        }
    }

    /**
     * Returns the default namespace mappings and any extra mappings
     * available as xmlns:prefix="uri" attributes on the document element.
     *
     * @param session ignored
     * @return namespace mappings
     */
    public Map getRegisteredNamespaces(SessionInfo session) {
        Map namespaces = new HashMap();
        namespaces.put(QName.NS_EMPTY_PREFIX, QName.NS_DEFAULT_URI);
        namespaces.put(QName.NS_JCR_PREFIX, QName.NS_JCR_URI);
        namespaces.put(QName.NS_MIX_PREFIX, QName.NS_MIX_URI);
        namespaces.put(QName.NS_NT_PREFIX, QName.NS_NT_URI);
        namespaces.put(QName.NS_SV_PREFIX, QName.NS_SV_URI);
        namespaces.put(QName.NS_XML_PREFIX, QName.NS_XML_URI);
        Set uris = new HashSet(namespaces.values());

        Element root = document.getDocumentElement();
        NamedNodeMap attributes = root.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            if (QName.NS_XMLNS_URI.equals(attribute.getNamespaceURI())
                    && !uris.contains(attribute.getNodeValue())) {
                String prefix = attribute.getLocalName();
                for (int j = 2; namespaces.containsKey(prefix); j++) {
                    prefix = attribute.getLocalName() + j;
                }
                namespaces.put(prefix, attribute.getNodeValue());
                uris.add(attribute.getNodeValue());
            }
        }

        return namespaces;
    }

    public String getNamespaceURI(SessionInfo sessionInfo, String string) throws NamespaceException, RepositoryException {
        // TODO
        return null;
    }

    public String getNamespacePrefix(SessionInfo sessionInfo, String string) throws NamespaceException, RepositoryException {
        // TODO
        return null;
    }

    public Iterator getNodeTypeDefinitions(
            SessionInfo session) {
        return Arrays.asList(types).iterator();
    }

    public NodeId getRootId(SessionInfo session) {
        return new XMLNodeId(document);
    }

    public QNodeDefinition getNodeDefinition(SessionInfo session, NodeId id) {
        return nodeDefinition;
    }

    public NodeInfo getNodeInfo(SessionInfo session, NodeId id)
            throws RepositoryException {
        try {
            return new XMLNodeInfo((XMLNodeId) id);
        } catch (ClassCastException e) {
            throw new RepositoryException("Invalid node identifier: " + id);
        }
    }

    public Iterator getItemInfos(SessionInfo sessionInfo, NodeId nodeId) throws ItemNotFoundException, RepositoryException {
        try {
            return Collections.singletonList(new XMLNodeInfo((XMLNodeId) nodeId)).iterator();
        } catch (ClassCastException e) {
            throw new RepositoryException("Invalid node identifier: " + nodeId);
        }
    }

    public Iterator getChildInfos(SessionInfo session, NodeId id)
            throws RepositoryException {
        try {
            return ((XMLNodeId) id).getChildInfos();
        } catch (ClassCastException e) {
            throw new RepositoryException("Invalid node identifier: " + id);
        }
    }

    public IdFactory getIdFactory() {
        return new XMLIdFactory(new XMLNodeId(document));
    }

    public PropertyInfo getPropertyInfo(SessionInfo session, PropertyId id)
            throws RepositoryException {
        try {
            if (id instanceof XMLPrimaryTypeId) {
                return new XMLPrimaryTypeInfo(id);
            } else {
                return new XMLPropertyInfo((XMLNodeId) id);
            }
        } catch (ClassCastException e) {
            throw new RepositoryException("Invalid property identifier: " + id);
        }
    }

    public QPropertyDefinition getPropertyDefinition(
            SessionInfo session, PropertyId id) throws RepositoryException {
        if (id instanceof XMLPrimaryTypeId) {
            return primaryTypeDefinition;
        } else {
            return propertyDefinition;
        }
    }

    public boolean isGranted(SessionInfo session, ItemId id, String[] actions) {
        return true;
    }

    public String[] getWorkspaceNames(SessionInfo session) {
        return new String[] { "xml" };
    }

    public Batch createBatch(ItemId itemId, SessionInfo sessionInfo)
            throws RepositoryException {
        return null;
    }

    public void submit(Batch batch) throws RepositoryException {
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#addVersionLabel(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.name.QName, boolean)
     */
    public void addVersionLabel(SessionInfo sessionInfo,
            NodeId versionHistoryId, NodeId versionId, QName label,
            boolean moveLabel) throws VersionException, RepositoryException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#checkQueryStatement(org.apache.jackrabbit.spi.SessionInfo, java.lang.String, java.lang.String, java.util.Map)
     */
    public void checkQueryStatement(SessionInfo sessionInfo, String statement,
            String language, Map namespaces) throws InvalidQueryException,
            RepositoryException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#checkin(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId)
     */
    public void checkin(SessionInfo sessionInfo, NodeId nodeId)
            throws VersionException, UnsupportedRepositoryOperationException,
            InvalidItemStateException, LockException, RepositoryException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#checkout(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId)
     */
    public void checkout(SessionInfo sessionInfo, NodeId nodeId)
            throws UnsupportedRepositoryOperationException, LockException,
            RepositoryException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#clone(org.apache.jackrabbit.spi.SessionInfo, java.lang.String, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.name.QName, boolean)
     */
    public void clone(SessionInfo sessionInfo, String srcWorkspaceName,
            NodeId srcNodeId, NodeId destParentNodeId, QName destName,
            boolean removeExisting) throws NoSuchWorkspaceException,
            ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            LockException, UnsupportedRepositoryOperationException,
            RepositoryException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#copy(org.apache.jackrabbit.spi.SessionInfo, java.lang.String, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.name.QName)
     */
    public void copy(SessionInfo sessionInfo, String srcWorkspaceName,
            NodeId srcNodeId, NodeId destParentNodeId, QName destName)
            throws NoSuchWorkspaceException, ConstraintViolationException,
            VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException,
            UnsupportedRepositoryOperationException, RepositoryException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#createEventFilter(org.apache.jackrabbit.spi.SessionInfo, int, org.apache.jackrabbit.name.Path, boolean, java.lang.String[], org.apache.jackrabbit.name.QName[], boolean)
     */
    public EventFilter createEventFilter(SessionInfo sessionInfo,
            int eventTypes, Path absPath, boolean isDeep, String[] uuid,
            QName[] nodeTypeName, boolean noLocal)
            throws UnsupportedRepositoryOperationException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#dispose(org.apache.jackrabbit.spi.SessionInfo)
     */
    public void dispose(SessionInfo sessionInfo) throws RepositoryException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#executeQuery(org.apache.jackrabbit.spi.SessionInfo, java.lang.String, java.lang.String, java.util.Map)
     */
    public QueryInfo executeQuery(SessionInfo sessionInfo, String statement,
            String language, Map namespaces) throws RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#exists(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.ItemId)
     */
    public boolean exists(SessionInfo sessionInfo, ItemId itemId)
            throws RepositoryException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#getEvents(org.apache.jackrabbit.spi.SessionInfo, long, org.apache.jackrabbit.spi.EventFilter[])
     */
    public EventBundle[] getEvents(SessionInfo sessionInfo, long timeout,
            EventFilter[] filters) throws RepositoryException,
            UnsupportedRepositoryOperationException, InterruptedException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#getLockInfo(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId)
     */
    public LockInfo getLockInfo(SessionInfo sessionInfo, NodeId nodeId)
            throws LockException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#getQValueFactory()
     */
    public QValueFactory getQValueFactory() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#getSupportedQueryLanguages(org.apache.jackrabbit.spi.SessionInfo)
     */
    public String[] getSupportedQueryLanguages(SessionInfo sessionInfo)
            throws RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#importXml(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, java.io.InputStream, int)
     */
    public void importXml(SessionInfo sessionInfo, NodeId parentId,
            InputStream xmlStream, int uuidBehaviour)
            throws ItemExistsException, PathNotFoundException,
            VersionException, ConstraintViolationException, LockException,
            AccessDeniedException, UnsupportedRepositoryOperationException,
            RepositoryException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#lock(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, boolean, boolean)
     */
    public LockInfo lock(SessionInfo sessionInfo, NodeId nodeId, boolean deep,
            boolean sessionScoped)
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, InvalidItemStateException,
            RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#merge(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, java.lang.String, boolean)
     */
    public Iterator merge(SessionInfo sessionInfo, NodeId nodeId,
            String srcWorkspaceName, boolean bestEffort)
            throws NoSuchWorkspaceException, AccessDeniedException,
            MergeException, LockException, InvalidItemStateException,
            RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#move(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.name.QName)
     */
    public void move(SessionInfo sessionInfo, NodeId srcNodeId,
            NodeId destParentNodeId, QName destName)
            throws ItemExistsException, PathNotFoundException,
            VersionException, ConstraintViolationException, LockException,
            AccessDeniedException, UnsupportedRepositoryOperationException,
            RepositoryException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#obtain(org.apache.jackrabbit.spi.SessionInfo, java.lang.String)
     */
    public SessionInfo obtain(SessionInfo sessionInfo, String workspaceName)
            throws LoginException, NoSuchWorkspaceException,
            RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public SessionInfo impersonate(SessionInfo sessionInfo, Credentials credentials) throws LoginException, RepositoryException {
        // TODO
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#refreshLock(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId)
     */
    public void refreshLock(SessionInfo sessionInfo, NodeId nodeId)
            throws LockException, RepositoryException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#registerNamespace(org.apache.jackrabbit.spi.SessionInfo, java.lang.String, java.lang.String)
     */
    public void registerNamespace(SessionInfo sessionInfo, String prefix,
            String uri) throws NamespaceException,
            UnsupportedRepositoryOperationException, AccessDeniedException,
            RepositoryException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#removeVersion(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.spi.NodeId)
     */
    public void removeVersion(SessionInfo sessionInfo, NodeId versionHistoryId,
            NodeId versionId) throws ReferentialIntegrityException,
            AccessDeniedException, UnsupportedRepositoryOperationException,
            VersionException, RepositoryException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#removeVersionLabel(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.name.QName)
     */
    public void removeVersionLabel(SessionInfo sessionInfo,
            NodeId versionHistoryId, NodeId versionId, QName label)
            throws VersionException, RepositoryException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#resolveMergeConflict(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.spi.NodeId[], org.apache.jackrabbit.spi.NodeId[])
     */
    public void resolveMergeConflict(SessionInfo sessionInfo, NodeId nodeId,
            NodeId[] mergeFailedIds, NodeId[] predecessorIds)
            throws VersionException, InvalidItemStateException,
            UnsupportedRepositoryOperationException, RepositoryException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#restore(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId[], boolean)
     */
    public void restore(SessionInfo sessionInfo, NodeId[] versionIds,
            boolean removeExisting) throws ItemExistsException,
            UnsupportedRepositoryOperationException, VersionException,
            LockException, InvalidItemStateException, RepositoryException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#restore(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.spi.NodeId, boolean)
     */
    public void restore(SessionInfo sessionInfo, NodeId nodeId,
            NodeId versionId, boolean removeExisting) throws VersionException,
            PathNotFoundException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#unlock(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId)
     */
    public void unlock(SessionInfo sessionInfo, NodeId nodeId)
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, InvalidItemStateException,
            RepositoryException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#unregisterNamespace(org.apache.jackrabbit.spi.SessionInfo, java.lang.String)
     */
    public void unregisterNamespace(SessionInfo sessionInfo, String uri)
            throws NamespaceException, UnsupportedRepositoryOperationException,
            AccessDeniedException, RepositoryException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.spi.RepositoryService#update(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, java.lang.String)
     */
    public void update(SessionInfo sessionInfo, NodeId nodeId,
            String srcWorkspaceName) throws NoSuchWorkspaceException,
            AccessDeniedException, LockException, InvalidItemStateException,
            RepositoryException {
        // TODO Auto-generated method stub

    }

}
