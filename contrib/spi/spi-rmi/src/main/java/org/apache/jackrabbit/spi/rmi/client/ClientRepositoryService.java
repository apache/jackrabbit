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
package org.apache.jackrabbit.spi.rmi.client;

import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.rmi.remote.RemoteRepositoryService;
import org.apache.jackrabbit.spi.rmi.remote.RemoteSessionInfo;
import org.apache.jackrabbit.spi.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.spi.rmi.remote.RemoteQueryInfo;
import org.apache.jackrabbit.spi.rmi.common.SerializableInputStream;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.value.QValueFactoryImpl;
import org.apache.jackrabbit.identifier.IdFactoryImpl;

import javax.jcr.RepositoryException;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ValueFormatException;
import javax.jcr.AccessDeniedException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ItemExistsException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.MergeException;
import javax.jcr.NamespaceException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.ConstraintViolationException;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Arrays;
import java.io.InputStream;
import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * <code>ClientRepositoryService</code> implements a SPI repository service
 * which is backed by a remote repository service.
 */
public class ClientRepositoryService implements RepositoryService {

    /**
     * The remote repository service.
     */
    private final RemoteRepositoryService remoteService;

    /**
     * The id factory.
     */
    private final IdFactory idFactory = IdFactoryImpl.getInstance();

    /**
     * The QValue factory.
     */
    private final QValueFactory qValueFactory = QValueFactoryImpl.getInstance();

    /**
     * Creates a new client repository service.
     *
     * @param remoteService the remote repository service to expose.
     */
    public ClientRepositoryService(RemoteRepositoryService remoteService) {
        this.remoteService = remoteService;
    }

    /**
     * {@inheritDoc}
     */
    public IdFactory getIdFactory() {
        return idFactory;
    }

    /**
     * {@inheritDoc}
     */
    public QValueFactory getQValueFactory() {
        return qValueFactory;
    }

    /**
     * {@inheritDoc}
     */
    public Map getRepositoryDescriptors() throws RepositoryException {
        try {
            return remoteService.getRepositoryDescriptors();
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public SessionInfo obtain(Credentials credentials, String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        try {
            return new ClientSessionInfo(remoteService.obtain(credentials, workspaceName));
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public SessionInfo obtain(SessionInfo sessionInfo, String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        try {
            return new ClientSessionInfo(remoteService.obtain(
                    getRemoteSessionInfo(sessionInfo), workspaceName));
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    public SessionInfo impersonate(SessionInfo sessionInfo, Credentials credentials) throws LoginException, RepositoryException {
        try {
            return new ClientSessionInfo(remoteService.impersonate(
                    getRemoteSessionInfo(sessionInfo), credentials));
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dispose(SessionInfo sessionInfo) throws RepositoryException {
        try {
            remoteService.dispose(getRemoteSessionInfo(sessionInfo));
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getWorkspaceNames(SessionInfo sessionInfo)
            throws RepositoryException {
        try {
            return remoteService.getWorkspaceNames(
                    getRemoteSessionInfo(sessionInfo));
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isGranted(SessionInfo sessionInfo,
                             ItemId itemId,
                             String[] actions) throws RepositoryException {
        try {
            return remoteService.isGranted(
                    getRemoteSessionInfo(sessionInfo), itemId, actions);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getRootId(SessionInfo sessionInfo)
            throws RepositoryException {
        try {
            return remoteService.getRootId(getRemoteSessionInfo(sessionInfo));
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public QNodeDefinition getNodeDefinition(SessionInfo sessionInfo,
                                             NodeId nodeId)
            throws RepositoryException {
        try {
            return remoteService.getNodeDefinition(
                    getRemoteSessionInfo(sessionInfo), nodeId);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }
    /**
     * {@inheritDoc}
     */
    public QPropertyDefinition getPropertyDefinition(SessionInfo sessionInfo,
                                                     PropertyId propertyId)
            throws RepositoryException {
        try {
            return remoteService.getPropertyDefinition(
                    getRemoteSessionInfo(sessionInfo), propertyId);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(SessionInfo sessionInfo, ItemId itemId)
            throws RepositoryException {
        try {
            return remoteService.exists(
                    getRemoteSessionInfo(sessionInfo), itemId);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeInfo getNodeInfo(SessionInfo sessionInfo, NodeId nodeId)
            throws ItemNotFoundException, RepositoryException {
        try {
            return remoteService.getNodeInfo(getRemoteSessionInfo(sessionInfo), nodeId);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterator getItemInfos(SessionInfo sessionInfo, NodeId nodeId)
            throws ItemNotFoundException, RepositoryException {
        try {
            RemoteIterator it = remoteService.getItemInfos(getRemoteSessionInfo(sessionInfo), nodeId);
            return new ClientIterator(it);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterator getChildInfos(SessionInfo sessionInfo, NodeId parentId)
            throws ItemNotFoundException, RepositoryException {
        try {
            RemoteIterator it = remoteService.getChildInfos(
                    getRemoteSessionInfo(sessionInfo), parentId);
            return new ClientIterator(it);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public PropertyInfo getPropertyInfo(SessionInfo sessionInfo,
                                        PropertyId propertyId)
            throws ItemNotFoundException, RepositoryException {
        try {
            return remoteService.getPropertyInfo(getRemoteSessionInfo(sessionInfo), propertyId);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Batch createBatch(ItemId itemId, SessionInfo sessionInfo)
            throws RepositoryException {
        return new ClientBatch(itemId, getRemoteSessionInfo(sessionInfo));
    }

    /**
     * {@inheritDoc}
     */
    public void submit(Batch batch) throws PathNotFoundException, ItemNotFoundException, NoSuchNodeTypeException, ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        if (batch instanceof ClientBatch) {
            try {
                ClientBatch clientBatch = (ClientBatch) batch;
                remoteService.submit(clientBatch.getRemoteSessionInfo(),
                        clientBatch.getSerializableBatch());
            } catch (RemoteException e) {
                throw new RemoteRepositoryException(e);
            }
        } else {
            throw new RepositoryException("Unknown Batch implementation: " +
                    batch.getClass().getName());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void importXml(SessionInfo sessionInfo,
                          NodeId parentId,
                          InputStream xmlStream,
                          int uuidBehaviour) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        try {
            remoteService.importXml(getRemoteSessionInfo(sessionInfo), parentId,
                    new SerializableInputStream(xmlStream), uuidBehaviour);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void move(SessionInfo sessionInfo,
                     NodeId srcNodeId,
                     NodeId destParentNodeId,
                     QName destName) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        try {
            remoteService.move(getRemoteSessionInfo(sessionInfo), srcNodeId,
                    destParentNodeId, destName);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void copy(SessionInfo sessionInfo,
                     String srcWorkspaceName,
                     NodeId srcNodeId,
                     NodeId destParentNodeId,
                     QName destName) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        try {
            remoteService.copy(getRemoteSessionInfo(sessionInfo),
                    srcWorkspaceName, srcNodeId, destParentNodeId, destName);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void update(SessionInfo sessionInfo,
                       NodeId nodeId,
                       String srcWorkspaceName)
            throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        try {
            remoteService.update(getRemoteSessionInfo(sessionInfo),
                    nodeId, srcWorkspaceName);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void clone(SessionInfo sessionInfo,
                      String srcWorkspaceName,
                      NodeId srcNodeId,
                      NodeId destParentNodeId,
                      QName destName,
                      boolean removeExisting) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        try {
            remoteService.clone(getRemoteSessionInfo(sessionInfo),
                    srcWorkspaceName, srcNodeId, destParentNodeId,
                    destName, removeExisting);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public LockInfo getLockInfo(SessionInfo sessionInfo, NodeId nodeId)
            throws LockException, RepositoryException {
        try {
            return remoteService.getLockInfo(
                    getRemoteSessionInfo(sessionInfo), nodeId);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public LockInfo lock(SessionInfo sessionInfo,
                         NodeId nodeId,
                         boolean deep,
                         boolean sessionScoped)
            throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        try {
            return remoteService.lock(getRemoteSessionInfo(sessionInfo),
                    nodeId, deep, sessionScoped);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void refreshLock(SessionInfo sessionInfo, NodeId nodeId)
            throws LockException, RepositoryException {
        try {
            remoteService.refreshLock(getRemoteSessionInfo(sessionInfo), nodeId);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unlock(SessionInfo sessionInfo, NodeId nodeId)
            throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        try {
            remoteService.unlock(getRemoteSessionInfo(sessionInfo), nodeId);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkin(SessionInfo sessionInfo, NodeId nodeId)
            throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        try {
            remoteService.checkin(getRemoteSessionInfo(sessionInfo), nodeId);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkout(SessionInfo sessionInfo, NodeId nodeId)
            throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        try {
            remoteService.checkout(getRemoteSessionInfo(sessionInfo), nodeId);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeVersion(SessionInfo sessionInfo,
                              NodeId versionHistoryId,
                              NodeId versionId)
            throws ReferentialIntegrityException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        try {
            remoteService.removeVersion(getRemoteSessionInfo(sessionInfo), versionHistoryId, versionId);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void restore(SessionInfo sessionInfo,
                        NodeId nodeId,
                        NodeId versionId,
                        boolean removeExisting) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        try {
            remoteService.restore(getRemoteSessionInfo(sessionInfo), nodeId, versionId, removeExisting);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void restore(SessionInfo sessionInfo,
                        NodeId[] versionIds,
                        boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException {
        try {
            remoteService.restore(getRemoteSessionInfo(sessionInfo), versionIds, removeExisting);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterator merge(SessionInfo sessionInfo,
                            NodeId nodeId,
                            String srcWorkspaceName,
                            boolean bestEffort)
            throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        try {
            RemoteIterator it = remoteService.merge(
                    getRemoteSessionInfo(sessionInfo), nodeId,
                    srcWorkspaceName, bestEffort);
            return new ClientIterator(it);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void resolveMergeConflict(SessionInfo sessionInfo,
                                     NodeId nodeId,
                                     NodeId[] mergeFailedIds,
                                     NodeId[] predecessorIds)
            throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        try {
            remoteService.resolveMergeConflict(getRemoteSessionInfo(sessionInfo),
                    nodeId, mergeFailedIds, predecessorIds);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addVersionLabel(SessionInfo sessionInfo,
                                NodeId versionHistoryId,
                                NodeId versionId,
                                QName label,
                                boolean moveLabel) throws VersionException, RepositoryException {
        try {
            remoteService.addVersionLabel(getRemoteSessionInfo(sessionInfo),
                    versionHistoryId, versionId, label, moveLabel);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeVersionLabel(SessionInfo sessionInfo,
                                   NodeId versionHistoryId,
                                   NodeId versionId,
                                   QName label) throws VersionException, RepositoryException {
        try {
            remoteService.removeVersionLabel(getRemoteSessionInfo(sessionInfo), versionHistoryId, versionId, label);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getSupportedQueryLanguages(SessionInfo sessionInfo)
            throws RepositoryException {
        try {
            return remoteService.getSupportedQueryLanguages(getRemoteSessionInfo(sessionInfo));
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkQueryStatement(SessionInfo sessionInfo,
                                    String statement,
                                    String language,
                                    Map namespaces)
            throws InvalidQueryException, RepositoryException {
        if (!(namespaces instanceof Serializable)) {
            namespaces = new HashMap(namespaces);
        }
        try {
            remoteService.checkQueryStatement(getRemoteSessionInfo(sessionInfo),
                    statement, language, namespaces);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public QueryInfo executeQuery(SessionInfo sessionInfo,
                                  String statement,
                                  String language,
                                  Map namespaces) throws RepositoryException {
        if (!(namespaces instanceof Serializable)) {
            namespaces = new HashMap(namespaces);
        }
        try {
            RemoteQueryInfo remoteQueryInfo = remoteService.executeQuery(
                    getRemoteSessionInfo(sessionInfo), statement,
                    language, namespaces);
            return new ClientQueryInfo(remoteQueryInfo);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public EventFilter createEventFilter(SessionInfo sessionInfo,
                                         int eventTypes,
                                         Path absPath,
                                         boolean isDeep,
                                         String[] uuid,
                                         QName[] nodeTypeName,
                                         boolean noLocal)
            throws UnsupportedRepositoryOperationException, RepositoryException {
        try {
            return remoteService.createEventFilter(
                    getRemoteSessionInfo(sessionInfo), eventTypes, absPath,
                    isDeep, uuid, nodeTypeName, noLocal);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public EventBundle[] getEvents(SessionInfo sessionInfo,
                                   long timeout,
                                   EventFilter[] filters)
            throws RepositoryException, UnsupportedRepositoryOperationException, InterruptedException {
        try {
            return remoteService.getEvents(getRemoteSessionInfo(sessionInfo),
                    timeout, filters);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map getRegisteredNamespaces(SessionInfo sessionInfo)
            throws RepositoryException {
        try {
            return remoteService.getRegisteredNamespaces(
                    getRemoteSessionInfo(sessionInfo));
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespaceURI(SessionInfo sessionInfo, String prefix)
            throws NamespaceException, RepositoryException {
        try {
            return remoteService.getNamespaceURI(
                    getRemoteSessionInfo(sessionInfo), prefix);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespacePrefix(SessionInfo sessionInfo, String uri)
            throws NamespaceException, RepositoryException {
        try {
            return remoteService.getNamespacePrefix(
                    getRemoteSessionInfo(sessionInfo), uri);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerNamespace(SessionInfo sessionInfo,
                                  String prefix,
                                  String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        try {
            remoteService.registerNamespace(getRemoteSessionInfo(sessionInfo),
                    prefix, uri);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterNamespace(SessionInfo sessionInfo, String uri)
            throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        try {
            remoteService.unregisterNamespace(getRemoteSessionInfo(sessionInfo), uri);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterator getQNodeTypeDefinitions(
            SessionInfo sessionInfo) throws RepositoryException {
        try {
            QNodeTypeDefinition[] ntDefs = remoteService.getNodeTypeDefinitions(
                    getRemoteSessionInfo(sessionInfo));
            return Arrays.asList(ntDefs).iterator();
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public QNodeTypeDefinition getQNodeTypeDefinition(SessionInfo sessionInfo, QName nodetypeName) throws RepositoryException {
        // TODO: implement me
        throw new RuntimeException("implementation for getQNodeTypeDefinition missing");
    }

    //------------------------------< internal >--------------------------------

    private RemoteSessionInfo getRemoteSessionInfo(SessionInfo sessionInfo)
            throws RepositoryException {
        if (sessionInfo instanceof ClientSessionInfo) {
            return ((ClientSessionInfo) sessionInfo).getRemoteSessionInfo();
        } else {
            throw new RepositoryException("Unknown SessionInfo implementation: " +
                    sessionInfo.getClass().getName());
        }
    }
}
