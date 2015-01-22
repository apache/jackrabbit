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
package org.apache.jackrabbit.spi.commons.logging;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeExistsException;

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
import org.apache.jackrabbit.spi.Tree;

/**
 * Log wrapper for a {@link RepositoryService}.
 */
public class RepositoryServiceLogger extends AbstractLogger implements RepositoryService {
    private final RepositoryService service;

    /**
     * Create a new instance for the given <code>service</code> which uses
     * <code>writer</code> for persisting log messages.
     * @param service
     * @param writer
     */
    public RepositoryServiceLogger(RepositoryService service, LogWriter writer) {
        super(writer);
        this.service = service;
    }

    /**
     * @return  the wrapped RepositoryService
     */
    public RepositoryService getRepositoryService() {
        return service;
    }

    public NameFactory getNameFactory() throws RepositoryException {
        return (NameFactory) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getNameFactory();
            }
        }, "getNameFactory()", new Object[]{});
    }

    public PathFactory getPathFactory() throws RepositoryException {
        return (PathFactory) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getPathFactory();
            }
        }, "getPathFactory()", new Object[]{});
    }

    public IdFactory getIdFactory() throws RepositoryException {
        return (IdFactory) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getIdFactory();
            }
        }, "getIdFactory()", new Object[]{});
    }

    public QValueFactory getQValueFactory() throws RepositoryException {
        return (QValueFactory) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getQValueFactory();
            }
        }, "getQValueFactory()", new Object[]{});
    }

    public Map<String, QValue[]> getRepositoryDescriptors() throws RepositoryException {
        return (Map<String, QValue[]>) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getRepositoryDescriptors();
            }
        }, "getRepositoryDescriptors()", new Object[]{});
    }

    public ItemInfoCache getItemInfoCache(final SessionInfo sessionInfo) throws RepositoryException {
        return (ItemInfoCache) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getItemInfoCache(sessionInfo);
            }
        }, "getItemInfoCache(SessionInfo)", new Object[]{sessionInfo});
    }

    public SessionInfo obtain(final Credentials credentials, final String workspaceName)
            throws RepositoryException {

        return (SessionInfo) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.obtain(credentials, workspaceName);
            }
        }, "obtain(Credentials, String)", new Object[]{credentials, workspaceName});
    }

    public SessionInfo obtain(final SessionInfo sessionInfo, final String workspaceName)
            throws RepositoryException {

        return (SessionInfo) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.obtain(unwrap(sessionInfo), workspaceName);
            }
        }, "obtain(SessionInfo, String)", new Object[]{unwrap(sessionInfo), workspaceName});
    }

    public SessionInfo impersonate(final SessionInfo sessionInfo, final Credentials credentials)
            throws RepositoryException {

        return (SessionInfo) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.impersonate(unwrap(sessionInfo), credentials);
            }
        }, "impersonate(SessionInfo, Credentials)", new Object[]{unwrap(sessionInfo), credentials});
    }

    public void dispose(final SessionInfo sessionInfo) throws RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.dispose(unwrap(sessionInfo));
                return null;
            }
        }, "dispose(SessionInfo)", new Object[]{unwrap(sessionInfo)});
    }

    public String[] getWorkspaceNames(final SessionInfo sessionInfo) throws RepositoryException {
        return (String[]) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getWorkspaceNames(unwrap(sessionInfo));
            }
        }, "getWorkspaceNames(SessionInfo)", new Object[]{unwrap(sessionInfo)});
    }

    public boolean isGranted(final SessionInfo sessionInfo, final ItemId itemId, final String[] actions)
            throws RepositoryException {

        return (Boolean) execute(new Callable() {
            public Object call() throws RepositoryException {
                return Boolean.valueOf(service.isGranted(unwrap(sessionInfo), itemId, actions));
            }
        }, "isGranted(SessionInfo, ItemId, String[])", new Object[] { unwrap(sessionInfo), itemId, actions });
    }

    @Override
    public PrivilegeDefinition[] getPrivilegeDefinitions(final SessionInfo sessionInfo) throws RepositoryException {
        return (PrivilegeDefinition[]) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getPrivilegeDefinitions(unwrap(sessionInfo));
            }
        }, "getSupportedPrivileges(SessionInfo)", new Object[]{unwrap(sessionInfo)});
    }

    public PrivilegeDefinition[] getSupportedPrivileges(final SessionInfo sessionInfo, final NodeId nodeId) throws RepositoryException {
        return (PrivilegeDefinition[]) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getSupportedPrivileges(unwrap(sessionInfo), nodeId);
            }
        }, "getSupportedPrivileges(SessionInfo, NodeId)", new Object[]{unwrap(sessionInfo), nodeId});
    }

    public Name[] getPrivilegeNames(final SessionInfo sessionInfo, final NodeId nodeId) throws RepositoryException {
        return (Name[]) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getPrivilegeNames(unwrap(sessionInfo), nodeId);
            }
        }, "getPrivileges(SessionInfo, NodeId)", new Object[]{unwrap(sessionInfo), nodeId});
    }

    public QNodeDefinition getNodeDefinition(final SessionInfo sessionInfo, final NodeId nodeId)
            throws RepositoryException {

        return (QNodeDefinition) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getNodeDefinition(unwrap(sessionInfo), nodeId);
            }
        }, "getNodeDefinition(SessionInfo, NodeId)", new Object[]{unwrap(sessionInfo), nodeId});
    }

    public QPropertyDefinition getPropertyDefinition(final SessionInfo sessionInfo,
            final PropertyId propertyId) throws RepositoryException {

        return (QPropertyDefinition) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getPropertyDefinition(unwrap(sessionInfo), propertyId);
            }
        }, "getPropertyDefinition(SessionInfo, PropertyId)", new Object[]{unwrap(sessionInfo), propertyId});
    }

    public NodeInfo getNodeInfo(final SessionInfo sessionInfo, final NodeId nodeId)
            throws RepositoryException {

        return (NodeInfo) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getNodeInfo(unwrap(sessionInfo), nodeId);
            }
        }, "getNodeInfo(SessionInfo, NodeId)", new Object[]{unwrap(sessionInfo), nodeId});
    }

    public Iterator<? extends ItemInfo> getItemInfos(final SessionInfo sessionInfo, final ItemId itemId)
            throws RepositoryException {

        return (Iterator<? extends ItemInfo>) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getItemInfos(unwrap(sessionInfo), itemId);
            }
        }, "getItemInfos(SessionInfo, NodeId)", new Object[]{unwrap(sessionInfo), itemId});
    }

    public Iterator<ChildInfo> getChildInfos(final SessionInfo sessionInfo, final NodeId parentId)
            throws RepositoryException {

        return (Iterator<ChildInfo>) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getChildInfos(unwrap(sessionInfo), parentId);
            }
        }, "getChildInfos(SessionInfo, NodeId)", new Object[]{unwrap(sessionInfo), parentId});
    }

    public Iterator<PropertyId> getReferences(final SessionInfo sessionInfo, final NodeId nodeId, final Name propertyName, final boolean weakReferences) throws RepositoryException {
        return (Iterator<PropertyId>) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getReferences(unwrap(sessionInfo), nodeId, propertyName, weakReferences);
            }
        }, "getReferences(SessionInfo, NodeId, Name, boolean)", new Object[]{unwrap(sessionInfo), nodeId, propertyName, weakReferences});
    }

    public PropertyInfo getPropertyInfo(final SessionInfo sessionInfo, final PropertyId propertyId)
            throws RepositoryException {

        return (PropertyInfo) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getPropertyInfo(unwrap(sessionInfo), propertyId);
            }
        }, "getPropertyInfo(SessionInfo,PropertyId)", new Object[]{unwrap(sessionInfo), propertyId});
    }

    public Batch createBatch(final SessionInfo sessionInfo, final ItemId itemId) throws RepositoryException {
        return (Batch) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.createBatch(unwrap(sessionInfo), itemId);
            }
        }, "createBatch(SessionInfo, ItemId)", new Object[]{unwrap(sessionInfo), itemId});
    }

    public void submit(final Batch batch) throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.submit(unwrap(batch));
                return null;
            }
        }, "submit(Batch)", new Object[]{unwrap(batch)});
    }

    @Override
    public Tree createTree(final SessionInfo sessionInfo, final Batch batch, final Name nodeName, final Name primaryTypeName, final String uniqueId) throws RepositoryException {
            return (Tree) execute(new Callable() {
                public Object call() throws RepositoryException {
                    return service.createTree(sessionInfo, batch, nodeName, primaryTypeName, uniqueId);
                }}, "createTree(SessionInfo, Batch, Name, Name, String)", new Object[]{sessionInfo, batch, nodeName, primaryTypeName, uniqueId});
    }

    public void importXml(final SessionInfo sessionInfo, final NodeId parentId, final InputStream xmlStream,
            final int uuidBehaviour) throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.importXml(unwrap(sessionInfo), parentId, xmlStream, uuidBehaviour);
                return null;
            }
        }, "importXml(SessionInfo, NodeId, InputStream, int)",
                new Object[]{unwrap(sessionInfo), parentId, xmlStream, uuidBehaviour});
    }

    public void move(final SessionInfo sessionInfo, final NodeId srcNodeId, final NodeId destParentNodeId,
            final Name destName) throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.move(unwrap(sessionInfo), srcNodeId, destParentNodeId, destName);
                return null;
            }
        }, "move(SessionInfo, NodeId, NodeId, Name)",
                new Object[]{unwrap(sessionInfo), srcNodeId, destParentNodeId, destName});
    }

    public void copy(final SessionInfo sessionInfo, final String srcWorkspaceName, final NodeId srcNodeId,
            final NodeId destParentNodeId, final Name destName) throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.copy(unwrap(sessionInfo), srcWorkspaceName, srcNodeId, destParentNodeId, destName);
                return null;
            }
        }, "copy(SessionInfo, String, NodeId, NodeId, Name)",
                new Object[]{unwrap(sessionInfo), srcWorkspaceName, srcNodeId, destParentNodeId, destName});
    }

    public void update(final SessionInfo sessionInfo, final NodeId nodeId, final String srcWorkspaceName)
            throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.update(unwrap(sessionInfo), nodeId, srcWorkspaceName);
                return null;
            }
        }, "update(SessionInfo, NodeId, String)", new Object[]{unwrap(sessionInfo), nodeId, srcWorkspaceName});
    }

    public void clone(final SessionInfo sessionInfo, final String srcWorkspaceName, final NodeId srcNodeId,
            final NodeId destParentNodeId, final Name destName, final boolean removeExisting)
            throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.clone(unwrap(sessionInfo), srcWorkspaceName, srcNodeId, destParentNodeId, destName,
                        removeExisting);
                return null;
            }
        }, "clone(SessionInfo, String, NodeId, NodeId, Name, boolean)",
                new Object[] { unwrap(sessionInfo), srcWorkspaceName, srcNodeId, destParentNodeId, destName, removeExisting});
    }

    public LockInfo getLockInfo(final SessionInfo sessionInfo, final NodeId nodeId)
            throws RepositoryException {

        return (LockInfo) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getLockInfo(unwrap(sessionInfo), nodeId);
            }
        }, "getLockInfo(SessionInfo, NodeId)", new Object[]{unwrap(sessionInfo), nodeId});
    }

    public LockInfo lock(final SessionInfo sessionInfo, final NodeId nodeId, final boolean deep,
            final boolean sessionScoped) throws RepositoryException {

        return (LockInfo) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.lock(unwrap(sessionInfo), nodeId, deep, sessionScoped);
            }
        }, "lock(SessionInfo, NodeId, boolean, boolean)",
                new Object[]{unwrap(sessionInfo), nodeId, deep, sessionScoped});
    }

    public LockInfo lock(final SessionInfo sessionInfo, final NodeId nodeId, final boolean deep,
            final boolean sessionScoped, final long timeoutHint, final String ownerHint)
            throws RepositoryException {

        return (LockInfo) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.lock(unwrap(sessionInfo), nodeId, deep, sessionScoped, timeoutHint, ownerHint);
            }
        }, "lock(SessionInfo, NodeId, boolean, boolean, long, String)",
                new Object[] { unwrap(sessionInfo),
                nodeId, deep, sessionScoped, timeoutHint,
                ownerHint });
    }

    public void refreshLock(final SessionInfo sessionInfo, final NodeId nodeId)
            throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.refreshLock(unwrap(sessionInfo), nodeId);
                return null;
            }
        }, "refreshLock(SessionInfo, NodeId)", new Object[]{unwrap(sessionInfo), nodeId});
    }

    public void unlock(final SessionInfo sessionInfo, final NodeId nodeId)
            throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.unlock(unwrap(sessionInfo), nodeId);
                return null;
            }
        }, "unlock(SessionInfo, NodeId)", new Object[]{unwrap(sessionInfo), nodeId});
    }

    public NodeId checkin(final SessionInfo sessionInfo, final NodeId nodeId) throws RepositoryException {

        return (NodeId) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.checkin(unwrap(sessionInfo), nodeId);
            }
        }, "checkin(SessionInfo, NodeId)", new Object[]{unwrap(sessionInfo), nodeId});
    }

    public void checkout(final SessionInfo sessionInfo, final NodeId nodeId)
            throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.checkout(unwrap(sessionInfo), nodeId);
                return null;
            }
        }, "checkout(SessionInfo, NodeId)", new Object[]{unwrap(sessionInfo), nodeId});
    }

    public void checkout(final SessionInfo sessionInfo, final NodeId nodeId, final NodeId activityId)
            throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.checkout(unwrap(sessionInfo), nodeId, activityId);
                return null;
            }
        }, "checkout(SessionInfo, NodeId, NodeId)", new Object[]{unwrap(sessionInfo), nodeId, activityId});
    }

    public NodeId checkpoint(final SessionInfo sessionInfo, final NodeId nodeId) throws UnsupportedRepositoryOperationException, RepositoryException {
        return (NodeId) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.checkpoint(unwrap(sessionInfo), nodeId);
            }
        }, "checkpoint(SessionInfo, NodeId)", new Object[]{unwrap(sessionInfo), nodeId});
    }

    public NodeId checkpoint(final SessionInfo sessionInfo, final NodeId nodeId, final NodeId activityId) throws UnsupportedRepositoryOperationException, RepositoryException {
        return (NodeId) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.checkpoint(unwrap(sessionInfo), nodeId, activityId);
            }
        }, "checkpoint(SessionInfo, NodeId, NodeId)", new Object[]{unwrap(sessionInfo), nodeId, activityId});
    }

    public void removeVersion(final SessionInfo sessionInfo, final NodeId versionHistoryId,
            final NodeId versionId) throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.removeVersion(unwrap(sessionInfo), versionHistoryId, versionId);
                return null;
            }
        }, "removeVersion(SessionInfo, NodeId, NodeId)",
                new Object[]{unwrap(sessionInfo), versionHistoryId, versionId});
    }

    public void restore(final SessionInfo sessionInfo, final NodeId nodeId, final NodeId versionId,
            final boolean removeExisting) throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.restore(unwrap(sessionInfo), nodeId, versionId, removeExisting);
                return null;
            }
        }, "restore(SessionInfo, NodeId, NodeId, boolean)",
                new Object[]{unwrap(sessionInfo), nodeId, versionId, removeExisting});
    }

    public void restore(final SessionInfo sessionInfo, final NodeId[] nodeIds, final boolean removeExisting)
            throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.restore(unwrap(sessionInfo), nodeIds, removeExisting);
                return null;
            }
        }, "restore(SessionInfo, NodeId[], boolean)",
                new Object[]{unwrap(sessionInfo), nodeIds, removeExisting});
    }

    public Iterator<NodeId> merge(final SessionInfo sessionInfo, final NodeId nodeId, final String srcWorkspaceName,
            final boolean bestEffort) throws RepositoryException {

        return (Iterator<NodeId>) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.merge(unwrap(sessionInfo), nodeId, srcWorkspaceName, bestEffort);
            }
        }, "merge(SessionInfo, NodeId, String, boolean)",
                new Object[]{unwrap(sessionInfo), nodeId, srcWorkspaceName, bestEffort});
    }

    public Iterator<NodeId> merge(final SessionInfo sessionInfo, final NodeId nodeId, final String srcWorkspaceName,
            final boolean bestEffort, final boolean isShallow) throws RepositoryException {

        return (Iterator<NodeId>) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.merge(unwrap(sessionInfo), nodeId, srcWorkspaceName, bestEffort, isShallow);
            }
        }, "merge(SessionInfo, NodeId, String, boolean, boolean)",
                new Object[]{unwrap(sessionInfo), nodeId, srcWorkspaceName, bestEffort});
    }

    public void resolveMergeConflict(final SessionInfo sessionInfo, final NodeId nodeId,
            final NodeId[] mergeFailedIds, final NodeId[] predecessorIds) throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.resolveMergeConflict(unwrap(sessionInfo), nodeId, mergeFailedIds, predecessorIds);
                return null;
            }
        }, "resolveMergeConflict(SessionInfo, NodeId, NodeId[], NodeId[])",
                new Object[]{unwrap(sessionInfo), nodeId, mergeFailedIds, predecessorIds});
    }

    public void addVersionLabel(final SessionInfo sessionInfo, final NodeId versionHistoryId,
            final NodeId versionId, final Name label, final boolean moveLabel) throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.addVersionLabel(unwrap(sessionInfo), versionHistoryId, versionId, label, moveLabel);
                return null;
            }
        }, "addVersionLabel(SessionInfo, NodeId, NodeId, Name, boolean)",
                new Object[]{unwrap(sessionInfo), versionHistoryId, versionId, label, moveLabel});
    }

    public void removeVersionLabel(final SessionInfo sessionInfo, final NodeId versionHistoryId,
            final NodeId versionId, final Name label) throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.removeVersionLabel(unwrap(sessionInfo), versionHistoryId, versionId, label);
                return null;
            }
        }, "removeVersionLabel(SessionInfo, NodeId, NodeId, Name)",
                new Object[]{unwrap(sessionInfo), versionHistoryId, versionId, label});
    }

    public NodeId createActivity(final SessionInfo sessionInfo, final String title) throws UnsupportedRepositoryOperationException, RepositoryException {
        return (NodeId) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.createActivity(unwrap(sessionInfo), title);
            }
        }, "createActivity(SessionInfo, String)", new Object[]{unwrap(sessionInfo), title});
    }

    public void removeActivity(final SessionInfo sessionInfo, final NodeId activityId) throws UnsupportedRepositoryOperationException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.removeActivity(unwrap(sessionInfo), activityId);
                return null;
            }
        }, "removeActivity(SessionInfo, NodeId)",
                new Object[]{unwrap(sessionInfo), activityId});
    }

    public Iterator<NodeId> mergeActivity(final SessionInfo sessionInfo, final NodeId activityId) throws UnsupportedRepositoryOperationException, RepositoryException {
        return (Iterator<NodeId>) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.mergeActivity(unwrap(sessionInfo), activityId);
            }
        }, "mergeActivity(SessionInfo, NodeId)", new Object[]{unwrap(sessionInfo), activityId});
    }

    public NodeId createConfiguration(final SessionInfo sessionInfo, final NodeId nodeId) throws UnsupportedRepositoryOperationException, RepositoryException {
        return (NodeId) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.createConfiguration(unwrap(sessionInfo), nodeId);
            }
        }, "createConfiguration(SessionInfo, NodeId, NodeId)", new Object[]{unwrap(sessionInfo), nodeId});
    }

    public String[] getSupportedQueryLanguages(final SessionInfo sessionInfo) throws RepositoryException {
        return (String[]) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getSupportedQueryLanguages(unwrap(sessionInfo));
            }
        }, "getSupportedQueryLanguages(SessionInfo)", new Object[]{unwrap(sessionInfo)});
    }

    public String[] checkQueryStatement(final SessionInfo sessionInfo, final String statement,
            final String language, final Map<String, String> namespaces) throws RepositoryException {

        return (String[]) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.checkQueryStatement(unwrap(sessionInfo), statement, language, namespaces);
            }
        }, "checkQueryStatement(SessionInfo, String, String, Map)",
                new Object[]{unwrap(sessionInfo), statement, language, namespaces});
    }

    public QueryInfo executeQuery(final SessionInfo sessionInfo, final String statement,
                                  final String language, final Map<String, String> namespaces, final long limit, final long offset, final Map<String, QValue> values) throws RepositoryException {

        return (QueryInfo) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.executeQuery(unwrap(sessionInfo), statement, language, namespaces, limit, offset, values);
            }
        }, "executeQuery(SessionInfo, String, String, Map, long, long, Map)",
                new Object[]{unwrap(sessionInfo), statement, language, namespaces, limit, offset, values});
    }

    public EventFilter createEventFilter(final SessionInfo sessionInfo, final int eventTypes,
            final Path absPath, final boolean isDeep, final String[] uuid, final Name[] qnodeTypeName,
            final boolean noLocal) throws RepositoryException {

        return (EventFilter) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.createEventFilter(unwrap(sessionInfo), eventTypes, absPath, isDeep, uuid,
                       qnodeTypeName, noLocal);
            }
        }, "createEventFilter(SessionInfo, int, Path, boolean, String[], Name[], boolean)",
                new Object[]{unwrap(sessionInfo), eventTypes, absPath, isDeep, uuid,
                qnodeTypeName, noLocal});
    }

    public Subscription createSubscription(final SessionInfo sessionInfo, final EventFilter[] filters)
            throws RepositoryException {

        return (Subscription) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.createSubscription(unwrap(sessionInfo), filters);
            }
        }, "createSubscription(SessionInfo, EventFilter[])",
                new Object[]{unwrap(sessionInfo), filters});
    }

    public EventBundle[] getEvents(final Subscription subscription, final long timeout)
            throws RepositoryException, InterruptedException {

        final String methodName = "getEvents(Subscription, long)";
        final Object[] args = new Object[]{subscription, timeout};
        final InterruptedException[] ex = new InterruptedException[1];

        EventBundle[] result = (EventBundle[]) execute(new Callable() {
            public Object call() throws RepositoryException {
                try {
                    return service.getEvents(subscription, timeout);
                } catch (InterruptedException e) {
                    writer.error(methodName, args, e);
                    ex[0] = e;
                    return null;
                }
            }
        }, methodName, args);

        if (ex[0] != null) {
            throw ex[0];
        }

        return result;
    }

    public EventBundle getEvents(final SessionInfo sessionInfo,
                                 final EventFilter filter,
                                 final long after) throws RepositoryException,
            UnsupportedRepositoryOperationException {
        return (EventBundle) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getEvents(sessionInfo, filter, after);
            }
        }, "getEvents(SessionInfo, EventFilter, long)",
                new Object[]{unwrap(sessionInfo), filter, after});
    }

    public void updateEventFilters(final Subscription subscription, final EventFilter[] eventFilters)
            throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.updateEventFilters(subscription, eventFilters);
                return null;
            }
        }, "updateEventFilters(Subscription, EventFilter[])",
                new Object[]{subscription, eventFilters});
    }

    public void dispose(final Subscription subscription) throws RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.dispose(subscription);
                return null;
            }
        }, "dispose(Subscription)", new Object[]{});
    }

    public Map<String, String> getRegisteredNamespaces(final SessionInfo sessionInfo) throws RepositoryException {
        return (Map<String, String>) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getRegisteredNamespaces(unwrap(sessionInfo));
            }
        }, "getRegisteredNamespaces(SessionInfo)", new Object[]{unwrap(sessionInfo)});
    }

    public String getNamespaceURI(final SessionInfo sessionInfo, final String prefix)
            throws RepositoryException {
        return (String) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getNamespaceURI(unwrap(sessionInfo), prefix);
            }
        }, "getNamespaceURI(SessionInfo, String)", new Object[]{unwrap(sessionInfo), prefix});
    }

    public String getNamespacePrefix(final SessionInfo sessionInfo, final String uri)
            throws RepositoryException {

        return (String) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getNamespacePrefix(unwrap(sessionInfo), uri);
            }
        }, "getNamespacePrefix(SessionInfo, String)", new Object[]{unwrap(sessionInfo), uri});
    }

    public void registerNamespace(final SessionInfo sessionInfo, final String prefix, final String uri)
            throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.registerNamespace(unwrap(sessionInfo), prefix, uri);
                return null;
            }
        }, "registerNamespace(SessionInfo, String, String)", new Object[]{unwrap(sessionInfo), prefix, uri});
    }

    public void unregisterNamespace(final SessionInfo sessionInfo, final String uri)
            throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.unregisterNamespace(unwrap(sessionInfo), uri);
                return null;
            }
        }, "unregisterNamespace(SessionInfo, String)", new Object[]{unwrap(sessionInfo), uri});
    }

    public Iterator<QNodeTypeDefinition> getQNodeTypeDefinitions(final SessionInfo sessionInfo) throws RepositoryException {
        return (Iterator<QNodeTypeDefinition>) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getQNodeTypeDefinitions(unwrap(sessionInfo));
            }
        }, "getQNodeTypeDefinitions(SessionInfo)", new Object[]{unwrap(sessionInfo)});
    }

    public Iterator<QNodeTypeDefinition> getQNodeTypeDefinitions(final SessionInfo sessionInfo, final Name[] nodetypeNames)
            throws RepositoryException {

        return (Iterator) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getQNodeTypeDefinitions(unwrap(sessionInfo), nodetypeNames);
            }
        }, "getQNodeTypeDefinitions(SessionInfo, Name[])", new Object[]{unwrap(sessionInfo), nodetypeNames});
    }

    public void registerNodeTypes(final SessionInfo sessionInfo, final QNodeTypeDefinition[] nodeTypeDefinitions, final boolean allowUpdate) throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.registerNodeTypes(unwrap(sessionInfo), nodeTypeDefinitions, allowUpdate);
                return null;
            }
        }, "registerNodeTypes(SessionInfo, QNodeTypeDefinition[], boolean)", new Object[]{unwrap(sessionInfo), nodeTypeDefinitions, allowUpdate});
    }

    public void unregisterNodeTypes(final SessionInfo sessionInfo, final Name[] nodeTypeNames) throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.unregisterNodeTypes(unwrap(sessionInfo), nodeTypeNames);
                return null;
            }
        }, "unregisterNodeTypes(SessionInfo, Name[])", new Object[]{unwrap(sessionInfo), nodeTypeNames});
    }

    public void createWorkspace(final SessionInfo sessionInfo, final String name, final String srcWorkspaceName) throws RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.createWorkspace(unwrap(sessionInfo), name, srcWorkspaceName);
                return null;
            }
        }, "createWorkspace(SessionInfo, String, String)", new Object[]{unwrap(sessionInfo), name, srcWorkspaceName});
    }

    public void deleteWorkspace(final SessionInfo sessionInfo, final String name) throws RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.deleteWorkspace(unwrap(sessionInfo), name);
                return null;
            }
        }, "deleteWorkspace(SessionInfo, String, String)", new Object[]{unwrap(sessionInfo), name});

    }

    // -----------------------------------------------------< private  >---

    private static SessionInfo unwrap(SessionInfo sessionInfo) {
        if (sessionInfo instanceof SessionInfoLogger) {
            return ((SessionInfoLogger) sessionInfo).getSessionInfo();
        }
        else {
            return sessionInfo;
        }
    }

    private static Batch unwrap(Batch batch) {
        if (batch instanceof BatchLogger) {
            return ((BatchLogger) batch).getBatch();
        }
        else {
            return batch;
        }
    }

}
