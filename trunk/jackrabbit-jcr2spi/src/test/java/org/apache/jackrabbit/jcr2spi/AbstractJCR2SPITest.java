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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import junit.framework.TestCase;

import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.ChildInfo;
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
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.Subscription;
import org.apache.jackrabbit.spi.Path.Element;
import org.apache.jackrabbit.spi.Tree;
import org.apache.jackrabbit.spi.commons.AbstractReadableRepositoryService;
import org.apache.jackrabbit.spi.commons.ItemInfoBuilder;
import org.apache.jackrabbit.spi.commons.ItemInfoBuilder.NodeInfoBuilder;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;

/**
 * Abstract base class for jcr2spi tests. This class implements {@link RepositoryService}
 * by delegation to {@link AbstractReadableRepositoryService}. Implementors can override
 * individual methods as needed.
 */
public abstract class AbstractJCR2SPITest extends TestCase implements RepositoryService {
    private static final String DEFAULT_WSP = "default";

    private RepositoryService repositoryService;

    protected ItemInfoStore itemInfoStore;
    protected RepositoryConfig config;
    protected Repository repository;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        itemInfoStore = new ItemInfoStore();
        ItemInfoBuilder.Listener listener = new ItemInfoBuilder.Listener() {
            public void createPropertyInfo(PropertyInfo propertyInfo) {
                itemInfoStore.addItemInfo(propertyInfo);
            }

            public void createNodeInfo(NodeInfo nodeInfo) {
                itemInfoStore.addItemInfo(nodeInfo);
            }

            public void createChildInfos(NodeId id, Iterator<ChildInfo> childInfos) {
                itemInfoStore.setChildInfos(id, childInfos);
            }
        };

        initInfosStore(ItemInfoBuilder.nodeInfoBuilder(listener));
        repositoryService = getRepositoryService();
        config = getRepositoryConfig();
        repository = getRepository();
    }

    /**
     * Convert the given <code>path</code> to a JCR path.
     * @param path
     * @return
     */
    public static final String toJCRPath(Path path) {
        Element[] elems = path.getElements();
        StringBuffer jcrPath = new StringBuffer();

        for (int k = 0; k < elems.length; k++) {
            jcrPath.append(elems[k].getName().getLocalName());
            if (k + 1 < elems.length || elems.length == 1) {
                jcrPath.append('/');
            }
        }

        return jcrPath.toString();
    }

    /**
     * Initialize the mock repository using the <code>builder</code>.
     * @param builder
     * @throws RepositoryException
     */
    protected abstract void initInfosStore(NodeInfoBuilder builder) throws RepositoryException;

    protected RepositoryService getRepositoryService() throws RepositoryException, ParseException {
        return new AbstractReadableRepositoryService(getDescriptors(), getNameSpaces(), getCndReader(),
                getWspNames(), DEFAULT_WSP) {

            @Override
            protected void checkCredentials(Credentials credentials, String workspaceName)
                    throws LoginException {

                AbstractJCR2SPITest.this.checkCredentials(credentials, workspaceName);
            }

            @Override
            protected QNodeDefinition createRootNodeDefinition(SessionInfo sessionInfo)
                    throws RepositoryException {

                return AbstractJCR2SPITest.this.createRootNodeDefinition();
            }

            public Iterator<? extends ItemInfo> getItemInfos(SessionInfo sessionInfo, ItemId itemId)
                    throws ItemNotFoundException, RepositoryException {

                return AbstractJCR2SPITest.this.getItemInfos(sessionInfo, itemId);
            }

            public Iterator<ChildInfo> getChildInfos(SessionInfo sessionInfo, NodeId parentId)
                    throws ItemNotFoundException, RepositoryException {

                return AbstractJCR2SPITest.this.getChildInfos(sessionInfo, parentId);
            }

            @Override
            public PrivilegeDefinition[] getPrivilegeDefinitions(SessionInfo sessionInfo) throws RepositoryException {
                return AbstractJCR2SPITest.this.getPrivilegeDefinitions(sessionInfo);
            }

            @Override
            public PrivilegeDefinition[] getSupportedPrivileges(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
                return AbstractJCR2SPITest.this.getSupportedPrivileges(sessionInfo, nodeId);
            }

            @Override
            public Name[] getPrivilegeNames(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
                return AbstractJCR2SPITest.this.getPrivilegeNames(sessionInfo, nodeId);
            }
            
            @Override
            public NodeInfo getNodeInfo(SessionInfo sessionInfo, NodeId nodeId) throws ItemNotFoundException,
                    RepositoryException {

                return AbstractJCR2SPITest.this.getNodeInfo(sessionInfo, nodeId);
            }

            public PropertyInfo getPropertyInfo(SessionInfo sessionInfo, PropertyId propertyId)
                    throws ItemNotFoundException, RepositoryException {

                return AbstractJCR2SPITest.this.getPropertyInfo(sessionInfo, propertyId);
            }

            public Iterator<PropertyId> getReferences(SessionInfo sessionInfo, NodeId nodeId,
                    Name propertyName, boolean weakReferences) throws ItemNotFoundException,
                    RepositoryException {

                return AbstractJCR2SPITest.this.getReferences(sessionInfo, nodeId, propertyName, weakReferences);
            }

        };
    }

    protected Reader getCndReader() throws RepositoryException {
        String resourceName = "default-nodetypes.cnd";
        InputStream is = AbstractJCR2SPITest.class.getResourceAsStream(resourceName);
        if (is == null) {
            throw new RepositoryException(("Resource not found: " + resourceName));
        }

        return new InputStreamReader(new BufferedInputStream(is));
    }

    protected Map<String, String> getNameSpaces() {
        return Collections.emptyMap();
    }

    protected Map<String, QValue[]> getDescriptors() throws RepositoryException {
        Map<String, QValue[]> descriptorKeys = new HashMap<String, QValue[]>();

        QValueFactory qvf = QValueFactoryImpl.getInstance();

        descriptorKeys.put(Repository.REP_NAME_DESC, new QValue[] {qvf.create("Mock Repository", PropertyType.STRING)});
        descriptorKeys.put(Repository.REP_VENDOR_DESC, new QValue[] {qvf.create("Apache Software Foundation", PropertyType.STRING)});
        descriptorKeys.put(Repository.REP_VENDOR_URL_DESC, new QValue[] {qvf.create("http://www.apache.org/", PropertyType.STRING)});
        descriptorKeys.put(Repository.REP_VERSION_DESC, new QValue[] {qvf.create("2.0", PropertyType.STRING)});
        descriptorKeys.put(Repository.SPEC_NAME_DESC, new QValue[] {qvf.create("Content Repository API for Java(TM) Technology Specification", PropertyType.STRING)});
        descriptorKeys.put(Repository.SPEC_VERSION_DESC, new QValue[] {qvf.create("2.0", PropertyType.STRING)});

        return descriptorKeys;
    }

    protected List<String> getWspNames() {
        return Collections.singletonList(DEFAULT_WSP);
    }

    protected RepositoryConfig getRepositoryConfig() {
        return new AbstractRepositoryConfig() {
            public RepositoryService getRepositoryService() throws RepositoryException {
                return AbstractJCR2SPITest.this;
            }
        };
    }

    protected Repository getRepository() throws RepositoryException {
        return RepositoryImpl.create(config);
    }

    protected void checkCredentials(Credentials credentials, String workspaceName) {
        // empty -> all credentials are valid by default
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

    public Map<String, QValue[]> getRepositoryDescriptors() throws RepositoryException {
        return repositoryService.getRepositoryDescriptors();
    }

    public ItemInfoCache getItemInfoCache(SessionInfo sessionInfo) throws RepositoryException {
        return repositoryService.getItemInfoCache(sessionInfo);
    }

    public PrivilegeDefinition[] getPrivilegeDefinitions(
            SessionInfo sessionInfo) throws RepositoryException {
        return repositoryService.getPrivilegeDefinitions(sessionInfo);
    }

    public PrivilegeDefinition[] getSupportedPrivileges(
            SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        return repositoryService.getSupportedPrivileges(sessionInfo, nodeId);
    }
    
    public Name[] getPrivilegeNames(
            SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        return repositoryService.getPrivilegeNames(sessionInfo, nodeId);
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

        return repositoryService.getPropertyDefinition(sessionInfo, propertyId);
    }

    protected abstract QNodeDefinition createRootNodeDefinition();

    public abstract NodeInfo getNodeInfo(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException;

    public abstract Iterator<? extends ItemInfo> getItemInfos(SessionInfo sessionInfo, ItemId itemId) throws ItemNotFoundException, RepositoryException;

    public abstract Iterator<ChildInfo> getChildInfos(SessionInfo sessionInfo, NodeId parentId) throws ItemNotFoundException, RepositoryException;

    public abstract PropertyInfo getPropertyInfo(SessionInfo sessionInfo, PropertyId propertyId) throws ItemNotFoundException, RepositoryException;

    public Iterator<PropertyId> getReferences(SessionInfo sessionInfo, NodeId nodeId, Name propertyName,
            boolean weakReferences) throws RepositoryException {

        return repositoryService.getReferences(sessionInfo, nodeId, propertyName, weakReferences);
    }

    //-----------------------------------------------< general modification >---

    public Batch createBatch(SessionInfo sessionInfo, ItemId itemId) throws RepositoryException {
        return repositoryService.createBatch(sessionInfo, itemId);
    }

    public void submit(Batch batch) throws RepositoryException {
        repositoryService.submit(batch);
    }

    @Override
    public Tree createTree(SessionInfo sessionInfo, Batch batch, Name nodeName, Name primaryTypeName, String uniqueId) throws RepositoryException {
        return repositoryService.createTree(sessionInfo, batch, nodeName, primaryTypeName, uniqueId);
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

    public LockInfo lock(SessionInfo sessionInfo, NodeId nodeId, boolean deep, boolean sessionScoped,
            long timeoutHint, String ownerHint) throws RepositoryException {

        return repositoryService.lock(sessionInfo, nodeId, deep, sessionScoped, timeoutHint, ownerHint);
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

    public void checkout(SessionInfo sessionInfo, NodeId nodeId, NodeId activityId) throws RepositoryException {
        repositoryService.checkout(sessionInfo, nodeId, activityId);
    }

    public NodeId checkpoint(SessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException {
        return repositoryService.checkpoint(sessionInfo, nodeId);
    }

    public NodeId checkpoint(SessionInfo sessionInfo, NodeId nodeId, NodeId activityId)
            throws RepositoryException {
        return repositoryService.checkpoint(sessionInfo, nodeId, activityId);
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

    public Iterator<NodeId> merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName,
            boolean bestEffort) throws RepositoryException {

        return repositoryService.merge(sessionInfo, nodeId, srcWorkspaceName, bestEffort);
    }

    public Iterator<NodeId> merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName,
            boolean bestEffort, boolean isShallow) throws RepositoryException {

        return repositoryService.merge(sessionInfo, nodeId, srcWorkspaceName, bestEffort, isShallow);
    }

    public void resolveMergeConflict(SessionInfo sessionInfo, NodeId nodeId, NodeId[] mergeFailedIds,
            NodeId[] predecessorIds) throws RepositoryException {

        repositoryService.resolveMergeConflict(sessionInfo, nodeId, mergeFailedIds, predecessorIds);
    }

    public void addVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId,
            Name label, boolean moveLabel) throws RepositoryException {

        repositoryService.addVersionLabel(sessionInfo, versionHistoryId, versionId, label, moveLabel);
    }

    public void removeVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId,
            Name label) throws RepositoryException {

        repositoryService.removeVersionLabel(sessionInfo, versionHistoryId, versionId, label);
    }

    public NodeId createActivity(SessionInfo sessionInfo, String title)
            throws RepositoryException {
        return repositoryService.createActivity(sessionInfo, title);
    }

    public void removeActivity(SessionInfo sessionInfo, NodeId activityId)
            throws RepositoryException {

        repositoryService.removeActivity(sessionInfo, activityId);
    }

    public Iterator<NodeId> mergeActivity(SessionInfo sessionInfo, NodeId activityId)
            throws RepositoryException {

        return repositoryService.mergeActivity(sessionInfo, activityId);
    }

    public NodeId createConfiguration(SessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException {
        return repositoryService.createConfiguration(sessionInfo, nodeId);
    }

    //----------------------------------------------------------< Searching >---

    public String[] getSupportedQueryLanguages(SessionInfo sessionInfo) throws RepositoryException {
        return repositoryService.getSupportedQueryLanguages(sessionInfo);
    }


    public String[] checkQueryStatement(SessionInfo sessionInfo, String statement, String language,
            Map<String, String> namespaces) throws RepositoryException {

        return repositoryService.checkQueryStatement(sessionInfo, statement, language, namespaces);
    }

    public QueryInfo executeQuery(SessionInfo sessionInfo, String statement, String language,
            Map<String, String> namespaces, long limit, long offset, Map<String, QValue> values)
            throws RepositoryException {

        return repositoryService.executeQuery(sessionInfo, statement, language, namespaces, limit, offset,
                values);
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

    public EventBundle getEvents(SessionInfo sessionInfo, EventFilter filter, long after)
            throws RepositoryException {

        return repositoryService.getEvents(sessionInfo, filter, after);
    }

    public void dispose(Subscription subscription) throws RepositoryException {
        repositoryService.dispose(subscription);
    }


    //---------------------------------------------------------< Namespaces >---

    public Map<String, String> getRegisteredNamespaces(SessionInfo sessionInfo) throws RepositoryException {
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

    public Iterator<QNodeTypeDefinition> getQNodeTypeDefinitions(SessionInfo sessionInfo)
            throws RepositoryException {

        return repositoryService.getQNodeTypeDefinitions(sessionInfo);
    }

    public Iterator<QNodeTypeDefinition> getQNodeTypeDefinitions(SessionInfo sessionInfo, Name[] nodeTypeNames)
            throws RepositoryException {

        return repositoryService.getQNodeTypeDefinitions(sessionInfo, nodeTypeNames);
    }

    public void registerNodeTypes(SessionInfo sessionInfo, QNodeTypeDefinition[] nodeTypeDefinitions,
            boolean allowUpdate) throws RepositoryException {

        repositoryService.registerNodeTypes(sessionInfo, nodeTypeDefinitions, allowUpdate);
    }

    public void unregisterNodeTypes(SessionInfo sessionInfo, Name[] nodeTypeNames)
            throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException, RepositoryException {

        repositoryService.unregisterNodeTypes(sessionInfo, nodeTypeNames);
    }

    //-----------------------------------------------< Workspace Management >---

    public void createWorkspace(SessionInfo sessionInfo, String name, String srcWorkspaceName)
            throws RepositoryException {

        repositoryService.createWorkspace(sessionInfo, name, srcWorkspaceName);
    }

    public void deleteWorkspace(SessionInfo sessionInfo, String name) throws RepositoryException {
        repositoryService.deleteWorkspace(sessionInfo, name);
    }
}




