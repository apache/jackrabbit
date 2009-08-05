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
package org.apache.jackrabbit.jcr2spi;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import junit.framework.TestCase;

import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.Subscription;

/**
 * Abstract base class for jcr2spi tests. This class implements {@link RepositoryService}
 * by delegation to {@link AbstractRepositoryService}. Implementors can overrid individual
 * methods as needed.
 */
public abstract class AbstractJCR2SPITest extends TestCase implements RepositoryService {
    protected RepositoryService repositoryService;
    protected RepositoryConfig config;
    protected Repository repository;

    public void setUp() throws Exception {
        super.setUp();
        repositoryService = getRepositoryService();
        config = getRepositoryConfig();
        repository = getRepository();
    }

    protected RepositoryService getRepositoryService() throws RepositoryException {
        return new AbstractRepositoryService() {
            public Iterator getChildInfos(SessionInfo sessionInfo, NodeId parentId)
                    throws RepositoryException {

                return AbstractJCR2SPITest.this.getChildInfos(sessionInfo, parentId);
            }

            public Iterator getItemInfos(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
                return AbstractJCR2SPITest.this.getItemInfos(sessionInfo, nodeId);
            }

            public NodeInfo getNodeInfo(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
                return AbstractJCR2SPITest.this.getNodeInfo(sessionInfo, nodeId);
            }

            public PropertyInfo getPropertyInfo(SessionInfo sessionInfo, PropertyId propertyId) {
                return AbstractJCR2SPITest.this.getPropertyInfo(sessionInfo, propertyId);
            }
        };
    }

    protected RepositoryConfig getRepositoryConfig() {
        return new AbstractRepositoryConfig() {
            public RepositoryService getRepositoryService() throws RepositoryException {
                return repositoryService;
            }
        };
    }

    protected Repository getRepository() throws RepositoryException {
        return RepositoryImpl.create(config);
    }

    // -----------------------------------------------------< RepositoryService >---

    public IdFactory getIdFactory() throws RepositoryException {
        return repositoryService.getIdFactory();
    }

    public NameFactory getNameFactory() throws RepositoryException {
        return repositoryService.getNameFactory();
    }

    public PathFactory getPathFactory() throws RepositoryException {
        return repositoryService.getPathFactory();
    }

    public QValueFactory getQValueFactory() throws RepositoryException {
        return repositoryService.getQValueFactory();
    }

    public Map getRepositoryDescriptors() throws RepositoryException {
        return repositoryService.getRepositoryDescriptors();
    }


    //-----------------------------------< SessionInfo creation and release >---

    public SessionInfo obtain(Credentials credentials, String workspaceName) throws RepositoryException {
        return repositoryService.obtain(credentials, workspaceName);
    }

    public SessionInfo obtain(SessionInfo sessionInfo, String workspaceName) throws RepositoryException {
        return repositoryService.obtain(sessionInfo, workspaceName);
    }

    public SessionInfo impersonate(SessionInfo sessionInfo, Credentials credentials)
            throws RepositoryException {

        return repositoryService.impersonate(sessionInfo, credentials);
    }

    public void dispose(SessionInfo sessionInfo) throws RepositoryException {
        repositoryService.dispose(sessionInfo);
    }

    public String[] getWorkspaceNames(SessionInfo sessionInfo) throws RepositoryException {
        return repositoryService.getWorkspaceNames(sessionInfo);
    }


    //-----------------------------------------------------< Access Control >---

    public boolean isGranted(SessionInfo sessionInfo, ItemId itemId, String[] actions)
            throws RepositoryException {

        return repositoryService.isGranted(sessionInfo, itemId, actions);
    }


    //------------------------------------------------------< Reading items >---

    public QNodeDefinition getNodeDefinition(SessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException {

        return repositoryService.getNodeDefinition(sessionInfo, nodeId);
    }

    public QPropertyDefinition getPropertyDefinition(SessionInfo sessionInfo, PropertyId propertyId)
            throws RepositoryException {

        return getPropertyDefinition(sessionInfo, propertyId);
    }

    public abstract NodeInfo getNodeInfo(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException;

    public abstract Iterator getItemInfos(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException;

    public abstract Iterator getChildInfos(SessionInfo sessionInfo, NodeId parentId) throws RepositoryException;

    public abstract PropertyInfo getPropertyInfo(SessionInfo sessionInfo, PropertyId propertyId);

    //-----------------------------------------------< general modification >---

    public Batch createBatch(SessionInfo sessionInfo, ItemId itemId) throws RepositoryException {
        return repositoryService.createBatch(sessionInfo, itemId);
    }

    public void submit(Batch batch) throws RepositoryException {
        repositoryService.submit(batch);
    }


    //-------------------------------------------------------------< Import >---

    public void importXml(SessionInfo sessionInfo, NodeId parentId, InputStream xmlStream, int uuidBehaviour)
            throws RepositoryException {

        repositoryService.importXml(sessionInfo, parentId, xmlStream, uuidBehaviour);
    }


    //---------------------------------------------------------< Copy, Move >---

    public void move(SessionInfo sessionInfo, NodeId srcNodeId, NodeId destParentNodeId, Name destName)
            throws RepositoryException {

        repositoryService.move(sessionInfo, srcNodeId, destParentNodeId, destName);
    }

    public void copy(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId,
            NodeId destParentNodeId, Name destName) throws RepositoryException {

        repositoryService.copy(sessionInfo, srcWorkspaceName, srcNodeId, destParentNodeId, destName);
    }


    //------------------------------------------------------< Update, Clone >---

    public void update(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName)
            throws RepositoryException {

        repositoryService.update(sessionInfo, nodeId, srcWorkspaceName);
    }

    public void clone(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId,
            NodeId destParentNodeId, Name destName, boolean removeExisting) throws RepositoryException {

        repositoryService.clone(sessionInfo, srcWorkspaceName, srcNodeId, destParentNodeId, destName, removeExisting);
    }


    //------------------------------------------------------------< Locking >---

    public LockInfo getLockInfo(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        return repositoryService.getLockInfo(sessionInfo, nodeId);
    }

    public LockInfo lock(SessionInfo sessionInfo, NodeId nodeId, boolean deep, boolean sessionScoped)
            throws RepositoryException {

        return repositoryService.lock(sessionInfo, nodeId, deep, sessionScoped);
    }

    public LockInfo lock(
            SessionInfo sessionInfo, NodeId nodeId,
            boolean deep, boolean sessionScoped,
            long timeoutHint, String ownerHint)
            throws RepositoryException {
        return repositoryService.lock(
            sessionInfo, nodeId, deep, sessionScoped, timeoutHint, ownerHint);
    }

    public void refreshLock(SessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException {

        repositoryService.refreshLock(sessionInfo, nodeId);
    }

    public void unlock(SessionInfo sessionInfo, NodeId nodeId)
            throws  RepositoryException {

        repositoryService.unlock(sessionInfo, nodeId);
    }


    //---------------------------------------------------------< Versioning >---

    public NodeId checkin(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        return repositoryService.checkin(sessionInfo, nodeId);
    }

    public void checkout(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        repositoryService.checkout(sessionInfo, nodeId);
    }

    public void removeVersion(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId)
            throws RepositoryException {

        repositoryService.removeVersion(sessionInfo, versionHistoryId, versionId);
    }

    public void restore(SessionInfo sessionInfo, NodeId[] versionIds, boolean removeExisting)
            throws RepositoryException {

        repositoryService.restore(sessionInfo, versionIds, removeExisting);
    }

    public void restore(SessionInfo sessionInfo, NodeId nodeId, NodeId versionId, boolean removeExisting)
            throws RepositoryException {

        repositoryService.restore(sessionInfo, nodeId, versionId, removeExisting);
    }

    public Iterator merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName, boolean bestEffort)
            throws RepositoryException {

        return repositoryService.merge(sessionInfo, nodeId, srcWorkspaceName, bestEffort);
    }

    public void resolveMergeConflict(SessionInfo sessionInfo, NodeId nodeId, NodeId[] mergeFailedIds,
            NodeId[] predecessorIds) throws RepositoryException {

        repositoryService.resolveMergeConflict(sessionInfo, nodeId, mergeFailedIds, predecessorIds);
    }

    public void addVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId,
            Name label, boolean moveLabel) throws RepositoryException {

        addVersionLabel(sessionInfo, versionHistoryId, versionId, label, moveLabel);
    }

    public void removeVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId,
            Name label) throws RepositoryException {

        repositoryService.removeVersionLabel(sessionInfo, versionHistoryId, versionId, label);
    }


    //----------------------------------------------------------< Searching >---

    public String[] getSupportedQueryLanguages(SessionInfo sessionInfo) throws RepositoryException {
        return repositoryService.getSupportedQueryLanguages(sessionInfo);
    }


    public void checkQueryStatement(SessionInfo sessionInfo, String statement, String language,
            Map namespaces) throws RepositoryException {

        repositoryService.checkQueryStatement(sessionInfo, statement, language, namespaces);
    }

    public QueryInfo executeQuery(SessionInfo sessionInfo, String statement, String language,
            Map namespaces) throws RepositoryException {

        return repositoryService.executeQuery(sessionInfo, statement, language, namespaces);
    }


    //--------------------------------------------------------< Observation >---

    public EventFilter createEventFilter(SessionInfo sessionInfo, int eventTypes, Path absPath,
            boolean isDeep, String[] uuid, Name[] nodeTypeName, boolean noLocal) throws RepositoryException {

        return repositoryService.createEventFilter(sessionInfo, eventTypes, absPath, isDeep, uuid, nodeTypeName, noLocal);
    }

    public Subscription createSubscription(SessionInfo sessionInfo, EventFilter[] filters)
            throws RepositoryException {

        return repositoryService.createSubscription(sessionInfo, filters);
    }

    public void updateEventFilters(Subscription subscription, EventFilter[] filters)
            throws RepositoryException {

        repositoryService.updateEventFilters(subscription, filters);
    }

    public EventBundle[] getEvents(Subscription subscription, long timeout) throws RepositoryException,
            InterruptedException {

        return repositoryService.getEvents(subscription, timeout);
    }

    public void dispose(Subscription subscription) throws RepositoryException {
        repositoryService.dispose(subscription);
    }


    //---------------------------------------------------------< Namespaces >---

    public Map getRegisteredNamespaces(SessionInfo sessionInfo) throws RepositoryException {
        return repositoryService.getRegisteredNamespaces(sessionInfo);
    }

    public String getNamespaceURI(SessionInfo sessionInfo, String prefix)
            throws RepositoryException {

        return repositoryService.getNamespaceURI(sessionInfo, prefix);
    }

    public String getNamespacePrefix(SessionInfo sessionInfo, String uri)
            throws RepositoryException {

        return repositoryService.getNamespacePrefix(sessionInfo, uri);
    }

    public void registerNamespace(SessionInfo sessionInfo, String prefix, String uri)
            throws RepositoryException {

        repositoryService.registerNamespace(sessionInfo, prefix, uri);
    }

    public void unregisterNamespace(SessionInfo sessionInfo, String uri) throws RepositoryException {
        repositoryService.unregisterNamespace(sessionInfo, uri);
    }


    //----------------------------------------------------------< NodeTypes >---

    public Iterator getQNodeTypeDefinitions(SessionInfo sessionInfo) throws RepositoryException {
        return repositoryService.getQNodeTypeDefinitions(sessionInfo);
    }

    public Iterator getQNodeTypeDefinitions(SessionInfo sessionInfo, Name[] nodeTypeNames)
            throws RepositoryException {

        return repositoryService.getQNodeTypeDefinitions(sessionInfo, nodeTypeNames);
    }

}
