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
package org.apache.jackrabbit.client;

import java.io.InputStream;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.MergeException;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.version.VersionException;

import junit.framework.TestCase;

import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
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
import org.apache.jackrabbit.spi.RepositoryServiceFactory;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.Subscription;
import org.apache.jackrabbit.spi.Tree;
import org.apache.jackrabbit.spi.commons.logging.Slf4jLogWriterProvider;
import org.apache.jackrabbit.webdav.DavException;

public class RepositoryFactoryImplTest extends TestCase {
    private final RepositoryFactory factory = new RepositoryFactoryImpl();

    public void testGetRepositoryFromServiceFactory() throws RepositoryException {
        Map<String, RepositoryServiceFactory> parameters = Collections.singletonMap(
                "org.apache.jackrabbit.spi.RepositoryServiceFactory",
                RepositoryServiceFactoryImpl.INSTANCE);

        Repository repo = factory.getRepository(parameters);
        assertNotNull(repo);
    }

    public void testGetRepositoryFromRepositoryConfig() throws RepositoryException {
        Map<String, RepositoryConfig> parameters = Collections.singletonMap(
                "org.apache.jackrabbit.jcr2spi.RepositoryConfig",
                RepositoryConfigImpl.INSTANCE);

        Repository repo = factory.getRepository(parameters);
        assertNotNull(repo);
    }

    public void testGetRepositoryWithLogger() throws RepositoryException {
        List<Object> lwprovider = new ArrayList<Object>();
        lwprovider.add(null);
        lwprovider.add(true);
        lwprovider.add(new Slf4jLogWriterProvider());

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("org.apache.jackrabbit.jcr2spi.RepositoryConfig", RepositoryConfigImpl.INSTANCE);

        for (Object aLwprovider : lwprovider) {
            params.put("org.apache.jackrabbit.spi.commons.logging.LogWriterProvider", aLwprovider);
            Repository repo = factory.getRepository(params);
            assertNotNull(repo);
        }
    }

    public void testGetDefaultRepository() throws RepositoryException {
        Repository repo = factory.getRepository(null);
        assertNotNull(repo);
        assertEquals("Jackrabbit", repo.getDescriptor(Repository.REP_NAME_DESC));
    }

    public void testGetSpi2jcrRepository() throws RepositoryException {
        Repository coreRepo = factory.getRepository(null);

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("org.apache.jackrabbit.spi.RepositoryServiceFactory",
                       "org.apache.jackrabbit.spi2jcr.Spi2jcrRepositoryServiceFactory");
        parameters.put("org.apache.jackrabbit.spi2jcr.Repository", coreRepo);

        Repository jcr2spiRepo = factory.getRepository(parameters);
        assertNotNull(jcr2spiRepo);
        assertEquals("Jackrabbit", jcr2spiRepo.getDescriptor(Repository.REP_NAME_DESC));
    }

    public void testGetSpi2davRepository() throws RepositoryException {
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("org.apache.jackrabbit.spi.RepositoryServiceFactory",
                       "org.apache.jackrabbit.spi2dav.Spi2davRepositoryServiceFactory");
        parameters.put("org.apache.jackrabbit.spi2dav.uri",
                       "http://localhost/");

        try {
            Repository repo = factory.getRepository(parameters);
            assertNotNull(repo);
        } catch (RepositoryException e) {
            // If there is no jcr server on localhost, one of the below
            // exceptions will be thrown. Since this indicates that the
            // factory is working correctly, it is safe to ignore them.
            if (!(ConnectException.class.isInstance(e.getCause()) ||
                  DavException.class.isInstance(e.getCause()))) {
                throw e;
            }
        }
    }

    public void testGetSpi2davexRepository() throws RepositoryException {
        Map<String, String> parameters = Collections.singletonMap(
                "org.apache.jackrabbit.spi.RepositoryServiceFactory",
                "org.apache.jackrabbit.spi2davex.Spi2davexRepositoryServiceFactory");

        try {
            Repository repo = factory.getRepository(parameters);
            assertNotNull(repo);
        } catch (RepositoryException e) {
            // If there is no jcr server on localhost, one of the below
            // exceptions will be thrown. Since this indicates that the
            // factory is working correctly, it is safe to ignore them.
            if (!(ConnectException.class.isInstance(e.getCause()) ||
                    DavException.class.isInstance(e.getCause()))) {
                  throw e;
              }
        }
    }

    public void testGetRepositoryUnknownParams() throws RepositoryException {
        Repository repo = factory.getRepository(Collections.emptyMap());
        assertNull(repo);
    }

    // -----------------------------------------------------< private >---

    /**
     * Dummy RepositoryServiceFactory
     */
    private static final class RepositoryServiceFactoryImpl implements RepositoryServiceFactory {
        public static final RepositoryServiceFactory INSTANCE = new RepositoryServiceFactoryImpl();

        private RepositoryServiceFactoryImpl() {
            super();
        }

        public RepositoryService createRepositoryService(Map<?, ?> parameters) throws RepositoryException {
            return RepositoryServiceImpl.INSTANCE;
        }
    }

    /**
     * Dummy RepositoryConfig
     */
    private static final class RepositoryConfigImpl implements RepositoryConfig {
        public static final RepositoryConfig INSTANCE = new RepositoryConfigImpl();

        private RepositoryConfigImpl() {
            super();
        }

        public CacheBehaviour getCacheBehaviour() {
            return CacheBehaviour.INVALIDATE;
        }

        public int getItemCacheSize() {
            return 1234;
        }

        public int getPollTimeout() {
            return 1234;
        }

        @Override
        public <T> T getConfiguration(String name, T defaultValue) {
            return null;
        }

        public RepositoryService getRepositoryService() throws RepositoryException {
            return RepositoryServiceImpl.INSTANCE;
        }

    }

    /**
     * Dummy RepositoryService
     */
    private static final class RepositoryServiceImpl implements RepositoryService {

        public static final RepositoryService INSTANCE = new RepositoryServiceImpl();

        private RepositoryServiceImpl() {
            super();
        }

        public IdFactory getIdFactory() throws RepositoryException {
            return null;
        }

        public NameFactory getNameFactory() throws RepositoryException {
            return null;
        }

        public PathFactory getPathFactory() throws RepositoryException {
            return null;
        }

        public QValueFactory getQValueFactory() throws RepositoryException {
            return null;
        }

        public ItemInfoCache getItemInfoCache(SessionInfo sessionInfo) throws RepositoryException {
            return null;
        }

        public Map<String, QValue[]> getRepositoryDescriptors() throws RepositoryException {
            return Collections.emptyMap();
        }

        public SessionInfo obtain(Credentials credentials, String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
            return null;
        }

        public SessionInfo obtain(SessionInfo sessionInfo, String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
            return null;
        }

        public SessionInfo impersonate(SessionInfo sessionInfo, Credentials credentials) throws LoginException, RepositoryException {
            return null;
        }

        public void dispose(SessionInfo sessionInfo) throws RepositoryException {
            // empty
        }

        public String[] getWorkspaceNames(SessionInfo sessionInfo) throws RepositoryException {
            return new String[0];
        }

        public boolean isGranted(SessionInfo sessionInfo, ItemId itemId, String[] actions) throws RepositoryException {
            return false;
        }

        @Override
        public PrivilegeDefinition[] getPrivilegeDefinitions(SessionInfo sessionInfo) throws RepositoryException {
            return new PrivilegeDefinition[0];
        }

        @Override
        public Name[] getPrivilegeNames(SessionInfo sessionInfo, NodeId id) throws RepositoryException {
            return new Name[0];
        }

        @Override
        public PrivilegeDefinition[] getSupportedPrivileges(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
            return new PrivilegeDefinition[0];
        }

        public QNodeDefinition getNodeDefinition(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
            return null;
        }

        public QPropertyDefinition getPropertyDefinition(SessionInfo sessionInfo, PropertyId propertyId) throws RepositoryException {
            return null;
        }

        public NodeInfo getNodeInfo(SessionInfo sessionInfo, NodeId nodeId) throws ItemNotFoundException, RepositoryException {
            return null;
        }

        public Iterator<? extends ItemInfo> getItemInfos(SessionInfo sessionInfo, ItemId itemId) throws ItemNotFoundException, RepositoryException {
            return null;
        }

        public Iterator<ChildInfo> getChildInfos(SessionInfo sessionInfo, NodeId parentId) throws ItemNotFoundException, RepositoryException {
            return null;
        }

        public Iterator<PropertyId> getReferences(SessionInfo sessionInfo, NodeId nodeId, Name propertyName, boolean weakReferences) throws ItemNotFoundException, RepositoryException {
            return null;
        }

        public PropertyInfo getPropertyInfo(SessionInfo sessionInfo, PropertyId propertyId) throws ItemNotFoundException, RepositoryException {
            return null;
        }

        public Batch createBatch(SessionInfo sessionInfo, ItemId itemId) throws RepositoryException {
            return null;
        }

        public void submit(Batch batch) throws PathNotFoundException, ItemNotFoundException, NoSuchNodeTypeException, ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            // empty
        }

        @Override
        public Tree createTree(SessionInfo sessionInfo, Batch batch, Name nodeName, Name primaryTypeName, String uniqueId) throws RepositoryException {
            return null;
        }

        public void importXml(SessionInfo sessionInfo, NodeId parentId, InputStream xmlStream, int uuidBehaviour) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            // empty
        }

        public void move(SessionInfo sessionInfo, NodeId srcNodeId, NodeId destParentNodeId, Name destName) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            // empty
        }

        public void copy(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, Name destName) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
            // empty
        }

        public void update(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
            // empty
        }

        public void clone(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, Name destName, boolean removeExisting) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
            // empty
        }

        public LockInfo getLockInfo(SessionInfo sessionInfo, NodeId nodeId) throws AccessDeniedException, RepositoryException {
            return null;
        }

        public LockInfo lock(SessionInfo sessionInfo, NodeId nodeId, boolean deep, boolean sessionScoped) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
            return null;
        }

        public LockInfo lock(SessionInfo sessionInfo, NodeId nodeId, boolean deep, boolean sessionScoped, long timeoutHint, String ownerHint) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
            return null;
        }

        public void refreshLock(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
            // empty
        }

        public void unlock(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
            // empty
        }

        public NodeId checkin(SessionInfo sessionInfo, NodeId nodeId) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
            return null;
        }

        public void checkout(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
            // empty
        }

        public void checkout(SessionInfo sessionInfo, NodeId nodeId, NodeId activityId) throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
            // empty
        }

        public NodeId checkpoint(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, RepositoryException {
            return null;
        }

        public NodeId checkpoint(SessionInfo sessionInfo, NodeId nodeId, NodeId activityId) throws UnsupportedRepositoryOperationException, RepositoryException {
            return null;
        }

        public void removeVersion(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId) throws ReferentialIntegrityException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
            // empty
        }

        public void restore(SessionInfo sessionInfo, NodeId nodeId, NodeId versionId, boolean removeExisting) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
            // empty
        }

        public void restore(SessionInfo sessionInfo, NodeId[] versionIds, boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException {
            // empty
        }

        public Iterator<NodeId> merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
            return null;
        }

        public Iterator<NodeId> merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName, boolean bestEffort, boolean isShallow) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
            return null;
        }

        public void resolveMergeConflict(SessionInfo sessionInfo, NodeId nodeId, NodeId[] mergeFailedIds, NodeId[] predecessorIds) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
            // empty
        }

        public void addVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, Name label, boolean moveLabel) throws VersionException, RepositoryException {
            // empty
        }

        public void removeVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, Name label) throws VersionException, RepositoryException {
            // empty
        }

        public NodeId createActivity(SessionInfo sessionInfo, String title) throws UnsupportedRepositoryOperationException, RepositoryException {
            return null;
        }

        public void removeActivity(SessionInfo sessionInfo, NodeId activityId) throws UnsupportedRepositoryOperationException, RepositoryException {
            // empty
        }

        public Iterator<NodeId> mergeActivity(SessionInfo sessionInfo, NodeId activityId) throws UnsupportedRepositoryOperationException, RepositoryException {
            return null;
        }

        public NodeId createConfiguration(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, RepositoryException {
            return null;
        }

        public String[] getSupportedQueryLanguages(SessionInfo sessionInfo) throws RepositoryException {
            return new String[0];
        }

        public String[] checkQueryStatement(SessionInfo sessionInfo, String statement, String language, Map<String, String> namespaces) throws InvalidQueryException, RepositoryException {
            return new String[0];
        }

        public QueryInfo executeQuery(SessionInfo sessionInfo, String statement, String language, Map<String, String> namespaces, long limit, long offset, Map<String, QValue> values) throws RepositoryException {
            return null;
        }

        public EventFilter createEventFilter(SessionInfo sessionInfo, int eventTypes, Path absPath, boolean isDeep, String[] uuid, Name[] nodeTypeName, boolean noLocal) throws UnsupportedRepositoryOperationException, RepositoryException {
            return null;
        }

        public Subscription createSubscription(SessionInfo sessionInfo, EventFilter[] filters) throws UnsupportedRepositoryOperationException, RepositoryException {
            return null;
        }

        public void updateEventFilters(Subscription subscription, EventFilter[] filters) throws RepositoryException {
            // empty
        }

        public EventBundle[] getEvents(Subscription subscription, long timeout) throws RepositoryException, InterruptedException {
            return new EventBundle[0];
        }

        public EventBundle getEvents(SessionInfo sessionInfo, EventFilter filter, long after) throws RepositoryException, UnsupportedRepositoryOperationException {
            return null;
        }

        public void dispose(Subscription subscription) throws RepositoryException {
            // empty
        }

        public Map<String, String> getRegisteredNamespaces(SessionInfo sessionInfo) throws RepositoryException {
            return null;
        }

        public String getNamespaceURI(SessionInfo sessionInfo, String prefix) throws NamespaceException, RepositoryException {
            return null;
        }

        public String getNamespacePrefix(SessionInfo sessionInfo, String uri) throws NamespaceException, RepositoryException {
            return null;
        }

        public void registerNamespace(SessionInfo sessionInfo, String prefix, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
            // empty
        }

        public void unregisterNamespace(SessionInfo sessionInfo, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
            // empty
        }

        public Iterator<QNodeTypeDefinition> getQNodeTypeDefinitions(SessionInfo sessionInfo) throws RepositoryException {
            return null;
        }

        public Iterator<QNodeTypeDefinition> getQNodeTypeDefinitions(SessionInfo sessionInfo, Name[] nodetypeNames) throws RepositoryException {
            return null;
        }

        public void registerNodeTypes(SessionInfo sessionInfo, QNodeTypeDefinition[] nodeTypeDefinitions, boolean allowUpdate) throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException, RepositoryException {
            // empty
        }

        public void unregisterNodeTypes(SessionInfo sessionInfo, Name[] nodeTypeNames) throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException, RepositoryException {
            // empty
        }

        public void createWorkspace(SessionInfo sessionInfo, String name, String srcWorkspaceName) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
            // empty
        }

        public void deleteWorkspace(SessionInfo sessionInfo, String name) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
            // empty
        }
    }

}
