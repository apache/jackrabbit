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

    public Map getRepositoryDescriptors() throws RepositoryException {
        return (Map) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getRepositoryDescriptors();
            }
        }, "getRepositoryDescriptors()", new Object[]{});
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

        return ((Boolean) execute(new Callable() {
            public Object call() throws RepositoryException {
                return Boolean.valueOf(service.isGranted(unwrap(sessionInfo), itemId, actions));
            }
        }, "isGranted(SessionInfo, ItemId, String[])", new Object[] { unwrap(sessionInfo), itemId, actions }))
                .booleanValue();
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

    public Iterator getItemInfos(final SessionInfo sessionInfo, final NodeId nodeId)
            throws RepositoryException {

        return (Iterator) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getItemInfos(unwrap(sessionInfo), nodeId);
            }
        }, "getItemInfos(SessionInfo, NodeId)", new Object[]{unwrap(sessionInfo), nodeId});
    }

    public Iterator getChildInfos(final SessionInfo sessionInfo, final NodeId parentId)
            throws RepositoryException {

        return (Iterator) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getChildInfos(unwrap(sessionInfo), parentId);
            }
        }, "getChildInfos(SessionInfo, NodeId)", new Object[]{unwrap(sessionInfo), parentId});
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

    public void importXml(final SessionInfo sessionInfo, final NodeId parentId, final InputStream xmlStream,
            final int uuidBehaviour) throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.importXml(unwrap(sessionInfo), parentId, xmlStream, uuidBehaviour);
                return null;
            }
        }, "importXml(SessionInfo, NodeId, InputStream, int)",
                new Object[]{unwrap(sessionInfo), parentId, xmlStream, new Integer(uuidBehaviour)});
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
                new Object[] { unwrap(sessionInfo), srcWorkspaceName, srcNodeId, destParentNodeId, destName,
                Boolean.valueOf(removeExisting) });
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
                new Object[]{unwrap(sessionInfo), nodeId, Boolean.valueOf(deep), Boolean.valueOf(sessionScoped)});
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
                nodeId, Boolean.valueOf(deep), Boolean.valueOf(sessionScoped), new Long(timeoutHint),
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
                new Object[]{unwrap(sessionInfo), nodeId, versionId, Boolean.valueOf(removeExisting)});
    }

    public void restore(final SessionInfo sessionInfo, final NodeId[] nodeIds, final boolean removeExisting)
            throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.restore(unwrap(sessionInfo), nodeIds, removeExisting);
                return null;
            }
        }, "restore(SessionInfo, NodeId[], boolean)",
                new Object[]{unwrap(sessionInfo), nodeIds, Boolean.valueOf(removeExisting)});
    }

    public Iterator merge(final SessionInfo sessionInfo, final NodeId nodeId, final String srcWorkspaceName,
            final boolean bestEffort) throws RepositoryException {

        return (Iterator) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.merge(unwrap(sessionInfo), nodeId, srcWorkspaceName, bestEffort);
            }
        }, "merge(SessionInfo, NodeId, String, boolean)",
                new Object[]{unwrap(sessionInfo), nodeId, srcWorkspaceName, Boolean.valueOf(bestEffort)});
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
                new Object[]{unwrap(sessionInfo), versionHistoryId, versionId, label, Boolean.valueOf(moveLabel)});
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

    public String[] getSupportedQueryLanguages(final SessionInfo sessionInfo) throws RepositoryException {
        return (String[]) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getSupportedQueryLanguages(unwrap(sessionInfo));
            }
        }, "getSupportedQueryLanguages(SessionInfo)", new Object[]{unwrap(sessionInfo)});
    }

    public void checkQueryStatement(final SessionInfo sessionInfo, final String statement,
            final String language, final Map namespaces) throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.checkQueryStatement(unwrap(sessionInfo), statement, language, namespaces);
                return null;
            }
        }, "checkQueryStatement(SessionInfo, String, String, Map)",
                new Object[]{unwrap(sessionInfo), statement, language, namespaces});
    }

    public QueryInfo executeQuery(final SessionInfo sessionInfo, final String statement,
            final String language, final Map namespaces) throws RepositoryException {

        return (QueryInfo) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.executeQuery(unwrap(sessionInfo), statement, language, namespaces);
            }
        }, "executeQuery(SessionInfo, String, String, Map)",
                new Object[]{unwrap(sessionInfo), statement, language, namespaces});
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
                new Object[]{unwrap(sessionInfo), new Integer(eventTypes), absPath, Boolean.valueOf(isDeep), uuid,
                qnodeTypeName, Boolean.valueOf(noLocal)});
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
        final Object[] args = new Object[]{subscription, new Long(timeout)};
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

    public Map getRegisteredNamespaces(final SessionInfo sessionInfo) throws RepositoryException {
        return (Map) execute(new Callable() {
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

    public Iterator getQNodeTypeDefinitions(final SessionInfo sessionInfo) throws RepositoryException {
        return (Iterator) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getQNodeTypeDefinitions(unwrap(sessionInfo));
            }
        }, "getQNodeTypeDefinitions(SessionInfo)", new Object[]{unwrap(sessionInfo)});
    }

    public Iterator getQNodeTypeDefinitions(final SessionInfo sessionInfo, final Name[] nodetypeNames)
            throws RepositoryException {

        return (Iterator) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getQNodeTypeDefinitions(unwrap(sessionInfo), nodetypeNames);
            }
        }, "getQNodeTypeDefinitions(SessionInfo, Name[])", new Object[]{unwrap(sessionInfo), nodetypeNames});
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
