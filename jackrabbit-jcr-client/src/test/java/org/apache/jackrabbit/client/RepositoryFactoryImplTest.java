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

import junit.framework.TestCase;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Subscription;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.logging.Slf4jLogWriterProvider;
import org.apache.jackrabbit.client.spilogger.RepositoryConfigImpl;

import javax.jcr.RepositoryException;
import javax.jcr.Repository;
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
import javax.jcr.RepositoryFactory;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NodeTypeExistsException;
import java.util.Map;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;

/**
 * <code>RepositoryFactoryImplTest</code>...
 */
public class RepositoryFactoryImplTest extends TestCase {

    private final RepositoryFactory factory = new RepositoryFactoryImpl();
    private final RepositoryService service = new RepositoryServiceImpl();

    public void testGetDefaultRepository() throws RepositoryException {
        try {
            Repository repo = factory.getRepository(null);
            assertNotNull(repo);
        } catch (RepositoryException e) {
            // repository on top of spi2davex can only be initialized if the
            // server is running. ok.
        }

        try {
            System.setProperty(org.apache.jackrabbit.client.spi2davex.RepositoryConfigImpl.REPOSITORY_SPI2DAVEX_URI, org.apache.jackrabbit.client.spi2davex.RepositoryConfigImpl.DEFAULT_URI);
            Repository repo = factory.getRepository(null);
            assertNotNull(repo);
        } catch (RepositoryException e) {
            // repository on top of spi2davex can only be initialized if the
            // server is running. ok.
        }
    }

    public void testGetRepository() throws RepositoryException {
        RepositoryConfig config = new AbstractRepositoryConfig() {
            public RepositoryService getRepositoryService() throws RepositoryException {
                return service;
            }
        };

        Repository repo = factory.getRepository(Collections.singletonMap(RepositoryFactoryImpl.REPOSITORY_CONFIG, config));
        assertNotNull(repo);
    }

    public void testGetRepositoryWithLogger() throws RepositoryException {
        RepositoryConfig config = new AbstractRepositoryConfig() {
            public RepositoryService getRepositoryService() throws RepositoryException {
                return service;
            }
        };

        List lwprovider = new ArrayList();
        lwprovider.add(null);
        lwprovider.add(new Boolean(true));
        lwprovider.add(new Slf4jLogWriterProvider());

        Map params = new HashMap();
        params.put(RepositoryFactoryImpl.REPOSITORY_CONFIG, config);

        for (int i = 0; i < lwprovider.size(); i++) {
            params.put(RepositoryConfigImpl.PARAM_LOG_WRITER_PROVIDER, lwprovider.get(i));
            Repository repo = factory.getRepository(params);
            assertNotNull(repo);
        }        
    }

    public void testGetRepositoryUnknownParams() throws RepositoryException {
        Repository repo = factory.getRepository(Collections.EMPTY_MAP);
        assertNull(repo);
    }

    //--------------------------------------------------------------------------
    /**
     * Dummy RepositoryService
     */
    private static final class RepositoryServiceImpl implements RepositoryService {

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

        public Map getRepositoryDescriptors() throws RepositoryException {
            return null;
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
        }

        public String[] getWorkspaceNames(SessionInfo sessionInfo) throws RepositoryException {
            return new String[0];
        }

        public boolean isGranted(SessionInfo sessionInfo, ItemId itemId, String[] actions) throws RepositoryException {
            return false;
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

        public Iterator getItemInfos(SessionInfo sessionInfo, NodeId nodeId) throws ItemNotFoundException, RepositoryException {
            return null;
        }

        public Iterator getChildInfos(SessionInfo sessionInfo, NodeId parentId) throws ItemNotFoundException, RepositoryException {
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
        }

        public void importXml(SessionInfo sessionInfo, NodeId parentId, InputStream xmlStream, int uuidBehaviour) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        }

        public void move(SessionInfo sessionInfo, NodeId srcNodeId, NodeId destParentNodeId, Name destName) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        }

        public void copy(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, Name destName) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        }

        public void update(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        }

        public void clone(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, Name destName, boolean removeExisting) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
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
        }

        public void unlock(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        }

        public NodeId checkin(SessionInfo sessionInfo, NodeId nodeId) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
            return null;
        }

        public void checkout(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        }

        public NodeId checkpoint(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, RepositoryException {
            return null;
        }

        public void removeVersion(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId) throws ReferentialIntegrityException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        }

        public void restore(SessionInfo sessionInfo, NodeId nodeId, NodeId versionId, boolean removeExisting) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        }

        public void restore(SessionInfo sessionInfo, NodeId[] versionIds, boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException {
        }

        public Iterator merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
            return null;
        }

        public Iterator merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName, boolean bestEffort, boolean isShallow) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
            return null;
        }

        public void resolveMergeConflict(SessionInfo sessionInfo, NodeId nodeId, NodeId[] mergeFailedIds, NodeId[] predecessorIds) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        }

        public void addVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, Name label, boolean moveLabel) throws VersionException, RepositoryException {
        }

        public void removeVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, Name label) throws VersionException, RepositoryException {
        }

        public NodeId createActivity(SessionInfo sessionInfo, String title) throws UnsupportedRepositoryOperationException, RepositoryException {
            return null;
        }

        public void removeActivity(SessionInfo sessionInfo, NodeId activityId) throws UnsupportedRepositoryOperationException, RepositoryException {

        }

        public Iterator mergeActivity(SessionInfo sessionInfo, NodeId activityId) throws UnsupportedRepositoryOperationException, RepositoryException {
            return null;
        }

        public NodeId createConfiguration(SessionInfo sessionInfo, NodeId nodeId, NodeId baselineId) throws UnsupportedRepositoryOperationException, RepositoryException {
            return null;
        }

        public String[] getSupportedQueryLanguages(SessionInfo sessionInfo) throws RepositoryException {
            return new String[0];
        }

        public void checkQueryStatement(SessionInfo sessionInfo, String statement, String language, Map namespaces) throws InvalidQueryException, RepositoryException {
        }

        public QueryInfo executeQuery(SessionInfo sessionInfo, String statement, String language, Map namespaces) throws RepositoryException {
            return null;
        }

        public QueryInfo executeQuery(SessionInfo sessionInfo, String statement, String language, Map namespaces, long limit, long offset, Map<String, QValue> values) throws RepositoryException {
            return null;
        }

        public EventFilter createEventFilter(SessionInfo sessionInfo, int eventTypes, Path absPath, boolean isDeep, String[] uuid, Name[] nodeTypeName, boolean noLocal) throws UnsupportedRepositoryOperationException, RepositoryException {
            return null;
        }

        public Subscription createSubscription(SessionInfo sessionInfo, EventFilter[] filters) throws UnsupportedRepositoryOperationException, RepositoryException {
            return null;
        }

        public void updateEventFilters(Subscription subscription, EventFilter[] filters) throws RepositoryException {
        }

        public EventBundle[] getEvents(Subscription subscription, long timeout) throws RepositoryException, InterruptedException {
            return new EventBundle[0];
        }

        public void dispose(Subscription subscription) throws RepositoryException {
        }

        public Map getRegisteredNamespaces(SessionInfo sessionInfo) throws RepositoryException {
            return null;
        }

        public String getNamespaceURI(SessionInfo sessionInfo, String prefix) throws NamespaceException, RepositoryException {
            return null;
        }

        public String getNamespacePrefix(SessionInfo sessionInfo, String uri) throws NamespaceException, RepositoryException {
            return null;
        }

        public void registerNamespace(SessionInfo sessionInfo, String prefix, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        }

        public void unregisterNamespace(SessionInfo sessionInfo, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        }

        public Iterator getQNodeTypeDefinitions(SessionInfo sessionInfo) throws RepositoryException {
            return null;
        }

        public Iterator getQNodeTypeDefinitions(SessionInfo sessionInfo, Name[] nodetypeNames) throws RepositoryException {
            return null;
        }

        public void registerNodeTypes(SessionInfo sessionInfo, QNodeTypeDefinition[] nodeTypeDefinitions, boolean allowUpdate) throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException, RepositoryException {

        }

        public void unregisterNodeTypes(SessionInfo sessionInfo, Name[] nodeTypeNames) throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException, RepositoryException {

        }

        public void createWorkspace(SessionInfo sessionInfo, String name, String srcWorkspaceName) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {

        }

        public void deleteWorkspace(SessionInfo sessionInfo, String name) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {

        }
    }
}