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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.MergeException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEventListener;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManagerImpl;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.nodetype.EffectiveNodeTypeProvider;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProviderImpl;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeCache;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeRegistryImpl;
import org.apache.jackrabbit.jcr2spi.observation.InternalEventListener;
import org.apache.jackrabbit.jcr2spi.operation.AddLabel;
import org.apache.jackrabbit.jcr2spi.operation.AddNode;
import org.apache.jackrabbit.jcr2spi.operation.AddProperty;
import org.apache.jackrabbit.jcr2spi.operation.Checkin;
import org.apache.jackrabbit.jcr2spi.operation.Checkout;
import org.apache.jackrabbit.jcr2spi.operation.Checkpoint;
import org.apache.jackrabbit.jcr2spi.operation.Clone;
import org.apache.jackrabbit.jcr2spi.operation.Copy;
import org.apache.jackrabbit.jcr2spi.operation.CreateActivity;
import org.apache.jackrabbit.jcr2spi.operation.CreateConfiguration;
import org.apache.jackrabbit.jcr2spi.operation.LockOperation;
import org.apache.jackrabbit.jcr2spi.operation.LockRefresh;
import org.apache.jackrabbit.jcr2spi.operation.LockRelease;
import org.apache.jackrabbit.jcr2spi.operation.Merge;
import org.apache.jackrabbit.jcr2spi.operation.Move;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.operation.OperationVisitor;
import org.apache.jackrabbit.jcr2spi.operation.Remove;
import org.apache.jackrabbit.jcr2spi.operation.RemoveActivity;
import org.apache.jackrabbit.jcr2spi.operation.RemoveLabel;
import org.apache.jackrabbit.jcr2spi.operation.RemoveVersion;
import org.apache.jackrabbit.jcr2spi.operation.ReorderNodes;
import org.apache.jackrabbit.jcr2spi.operation.ResolveMergeConflict;
import org.apache.jackrabbit.jcr2spi.operation.Restore;
import org.apache.jackrabbit.jcr2spi.operation.SetMixin;
import org.apache.jackrabbit.jcr2spi.operation.SetTree;
import org.apache.jackrabbit.jcr2spi.operation.SetPrimaryType;
import org.apache.jackrabbit.jcr2spi.operation.SetPropertyValue;
import org.apache.jackrabbit.jcr2spi.operation.Update;
import org.apache.jackrabbit.jcr2spi.operation.WorkspaceImport;
import org.apache.jackrabbit.jcr2spi.security.AccessManager;
import org.apache.jackrabbit.jcr2spi.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.jcr2spi.security.authorization.AccessControlProviderStub;
import org.apache.jackrabbit.jcr2spi.state.ChangeLog;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateFactory;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.jcr2spi.state.TransientISFactory;
import org.apache.jackrabbit.jcr2spi.state.TransientItemStateFactory;
import org.apache.jackrabbit.jcr2spi.state.UpdatableItemStateManager;
import org.apache.jackrabbit.jcr2spi.state.WorkspaceItemStateFactory;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.ItemInfoCache;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.Subscription;
import org.apache.jackrabbit.spi.Tree;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.nodetype.NodeTypeStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>WorkspaceManager</code>...
 */
public class WorkspaceManager
        implements UpdatableItemStateManager, NamespaceStorage, AccessManager {

    private static Logger log = LoggerFactory.getLogger(WorkspaceManager.class);

    private final RepositoryConfig config;
    private final RepositoryService service;
    private final SessionInfo sessionInfo;
    private final NameFactory nameFactory;
    private final PathFactory pathFactory;

    private final ItemStateFactory isf;
    private final HierarchyManager hierarchyManager;

    private final boolean observationSupported;

    private final IdFactory idFactory;
    private final NamespaceRegistryImpl nsRegistry;
    private final NodeTypeRegistryImpl ntRegistry;
    private final ItemDefinitionProvider definitionProvider;

    private AccessControlProvider acProvider;

    /**
     * Semaphore to synchronize the feed thread with client
     * threads that call {@link #execute(Operation)} or {@link
     * #execute(ChangeLog)}.
     */
    private final Semaphore updateSync = new Semaphore(1);

    /**
     * This is the event polling for changes. If <code>null</code>
     * then the underlying repository service does not support observation.
     * It is also <code>null</code> if {@link CacheBehaviour#INVALIDATE} is
     * configured and no event listeners have been registered.
     */
    private Thread changeFeed;

    /**
     * Flag that indicates that the changeFeed thread should be disposed.
     */
    private volatile boolean disposeChangeFeed = false;

    /**
     * List of event listener that are set on this WorkspaceManager to get
     * notifications about local and external changes.
     */
    private final List<InternalEventListener> listeners = new LinkedList<InternalEventListener>();

    /**
     * The current subscription for change events if there are listeners.
     */
    private Subscription subscription;

    /**
     * A cache for item infos as supplied by {@link RepositoryService#getItemInfoCache(SessionInfo)}
     */
    private ItemInfoCache cache;

    public WorkspaceManager(RepositoryConfig config, SessionInfo sessionInfo, boolean observationSupported)
        throws RepositoryException {

        this.config = config;
        this.service = config.getRepositoryService();
        this.sessionInfo = sessionInfo;
        this.observationSupported = observationSupported;

        this.nameFactory = service.getNameFactory();
        this.pathFactory = service.getPathFactory();

        idFactory = service.getIdFactory();
        nsRegistry = new NamespaceRegistryImpl(this);
        ntRegistry = createNodeTypeRegistry(nsRegistry);
        definitionProvider = createDefinitionProvider(getEffectiveNodeTypeProvider());

        TransientItemStateFactory stateFactory = createItemStateFactory();
        this.isf = stateFactory;
        this.hierarchyManager = createHierarchyManager(stateFactory, idFactory);

        // If cache behavior is observation register a hierarchy listener which is
        // notified about all changes. Otherwise just add a hierarchy listener which
        // is only notified on changes for which client event listeners have been
        // installed. Note: this listener has to be the first one called in order
        // for the hierarchy to be consistent with the event (See JCR-2293).
        InternalEventListener listener = createHierarchyListener(hierarchyManager);
        CacheBehaviour cacheBehaviour = config.getCacheBehaviour();
        if (cacheBehaviour == CacheBehaviour.OBSERVATION) {
            addEventListener(listener);
        } else {
            listeners.add(listener);
        }
    }

    public NamespaceRegistryImpl getNamespaceRegistryImpl() {
        return nsRegistry;
    }

    public NodeTypeRegistry getNodeTypeRegistry() {
        return ntRegistry;
    }

    public ItemDefinitionProvider getItemDefinitionProvider() {
        return definitionProvider;
    }

    public EffectiveNodeTypeProvider getEffectiveNodeTypeProvider() {
        return ntRegistry;
    }

    public HierarchyManager getHierarchyManager() {
        return hierarchyManager;
    }

    public String[] getWorkspaceNames() throws RepositoryException {
        return service.getWorkspaceNames(sessionInfo);
    }

    public IdFactory getIdFactory() {
        return idFactory;
    }

    public NameFactory getNameFactory() {
        return nameFactory;
    }

    public PathFactory getPathFactory()  {
        return pathFactory;
    }

    public ItemStateFactory getItemStateFactory() {
        return isf;
    }

    /**
     * Locates and instantiates an AccessControlProvider implementation.
     * @return      an access control manager provider.  
     * @throws      RepositoryException
     */
    public AccessControlProvider getAccessControlProvider() throws RepositoryException {
        if (acProvider == null) {
            acProvider = AccessControlProviderStub.newInstance(config);
        }
        return acProvider;
    }

    public LockInfo getLockInfo(NodeId nodeId) throws RepositoryException {
        return service.getLockInfo(sessionInfo, nodeId);
    }

    /**
     * Returns the lock tokens present with the <code>SessionInfo</code>.
     *
     * @return lock tokens present with the <code>SessionInfo</code>.
     * @throws UnsupportedRepositoryOperationException If not supported.
     * @throws RepositoryException If another error occurs.
     * @see org.apache.jackrabbit.spi.SessionInfo#getLockTokens()
     */
    public String[] getLockTokens() throws UnsupportedRepositoryOperationException, RepositoryException {
        return sessionInfo.getLockTokens();
    }

    /**
     * This method succeeds if the lock tokens could be added to the
     * <code>SessionInfo</code>.
     *
     * @param lt The lock token to be added.
     * @throws UnsupportedRepositoryOperationException If not supported.
     * @throws LockException If a lock related exception occurs.
     * @throws RepositoryException If another exception occurs.
     * @see SessionInfo#addLockToken(String)
     */
    public void addLockToken(String lt) throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        sessionInfo.addLockToken(lt);
    }

    /**
     * Tries to remove the given token from the <code>SessionInfo</code>.
     *
     * @param lt The lock token to be removed.
     * @throws UnsupportedRepositoryOperationException If not supported.
     * @throws LockException If a lock related exception occurs, e.g if the
     * session info does not contain the specified lock token.
     * @throws RepositoryException If another exception occurs.
     * @see SessionInfo#removeLockToken(String)
     */
    public void removeLockToken(String lt) throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        for (String token : sessionInfo.getLockTokens()) {
            if (token.equals(lt)) {
                sessionInfo.removeLockToken(lt);
                return;
            }
        }
        // sessionInfo doesn't contain the given lock token and is therefore
        // not the lock holder
        throw new LockException("Unable to remove locktoken '" + lt + "' from Session.");
    }

    /**
     *
     * @return The names of the supported query languages.
     * @throws RepositoryException If an error occurs.
     */
    public String[] getSupportedQueryLanguages() throws RepositoryException {
        return service.getSupportedQueryLanguages(sessionInfo);
    }

    /**
     * Checks if the query statement is valid.
     *
     * @param statement  the query statement.
     * @param language   the query language.
     * @param namespaces the locally remapped namespaces which might be used in
     *                   the query statement.
     * @return the bind variable names.
     * @throws InvalidQueryException if the query statement is invalid.
     * @throws RepositoryException   if an error occurs while checking the query
     *                               statement.
     */
    public String[] checkQueryStatement(String statement,
                                        String language,
                                        Map<String, String> namespaces)
            throws InvalidQueryException, RepositoryException {
        return service.checkQueryStatement(sessionInfo, statement, language, namespaces);
    }

    /**
     * @param statement  the query statement.
     * @param language   the query language.
     * @param namespaces the locally remapped namespaces which might be used in
     *                   the query statement.
     * @param limit The result size limit as specified by {@link javax.jcr.query.Query#setLimit(long)}.
     * @param offset The result offset as specified by {@link javax.jcr.query.Query#setOffset(long)}
     * @param boundValues The bound values as specified by {@link javax.jcr.query.Query#bindValue(String, javax.jcr.Value)}
     * @return the QueryInfo created by {@link RepositoryService#executeQuery(org.apache.jackrabbit.spi.SessionInfo, String, String, java.util.Map, long, long, java.util.Map)}.
     * @throws RepositoryException If an error occurs.
     */
    public QueryInfo executeQuery(String statement, String language, Map<String, String> namespaces,
                                  long limit, long offset, Map<String, QValue> boundValues) throws RepositoryException {
        return service.executeQuery(sessionInfo, statement, language, namespaces, limit, offset, boundValues);
    }

    /**
     * Sets the <code>InternalEventListener</code> that gets notifications about
     * local and external changes.
     *
     * @param listener the new listener.
     * @throws RepositoryException if the listener cannot be registered.
     */
    public void addEventListener(InternalEventListener listener) throws RepositoryException {
        if (changeFeed == null) {
            changeFeed = createChangeFeed(config.getPollTimeout(), observationSupported);
        }

        synchronized (listeners) {
            listeners.add(listener);
            EventFilter[] filters = getEventFilters(listeners);
            if (subscription == null) {
                subscription = service.createSubscription(sessionInfo, filters);
            } else {
                service.updateEventFilters(subscription, filters);
            }
            listeners.notifyAll();
        }
    }

    /**
     * Updates the event filters on the subscription. The filters are retrieved
     * from the current list of internal event listeners.
     *
     * @throws RepositoryException If an error occurs.
     */
    public void updateEventFilters() throws RepositoryException {
        synchronized (listeners) {
            service.updateEventFilters(subscription, getEventFilters(listeners));
        }
    }

    /**
     *
     * @param listener The listener to be removed.
     * @throws RepositoryException If an error occurs.
     */
    public void removeEventListener(InternalEventListener listener)
            throws RepositoryException {
        synchronized (listeners) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                service.dispose(subscription);
                subscription = null;
            } else {
                service.updateEventFilters(subscription, getEventFilters(listeners));
            }
        }
    }

    /**
     * Creates an event filter based on the parameters available in {@link
     * javax.jcr.observation.ObservationManager#addEventListener}.
     *
     * @param eventTypes   A combination of one or more event type constants
     *                     encoded as a bitmask.
     * @param path         an absolute path.
     * @param isDeep       a <code>boolean</code>.
     * @param uuids        array of UUIDs.
     * @param nodeTypes    array of node type names.
     * @param noLocal      a <code>boolean</code>.
     * @return the event filter instance with the given parameters.
     * @throws UnsupportedRepositoryOperationException
     *          if this implementation does not support observation.
     */
    public EventFilter createEventFilter(int eventTypes, Path path, boolean isDeep,
                                         String[] uuids, Name[] nodeTypes,
                                         boolean noLocal)
        throws UnsupportedRepositoryOperationException, RepositoryException {
        return service.createEventFilter(sessionInfo, eventTypes, path, isDeep, uuids, nodeTypes, noLocal);
    }

    /**
     * Returns the events from the journal that occurred after a given date.
     *
     * @param filter the event filter to apply.
     * @param after  a date in milliseconds.
     * @return the events as a bundle.
     * @throws RepositoryException if an error occurs.
     * @throws UnsupportedRepositoryOperationException
     *                             if the implementation does not support
     *                             journaled observation.
     */
    public EventBundle getEvents(EventFilter filter, long after)
            throws RepositoryException, UnsupportedRepositoryOperationException {
        return service.getEvents(sessionInfo, filter, after);
    }

    /**
     *
     * @param userData The user data used for the event processing.
     * @throws RepositoryException If an error occurs.
     */
    public void setUserData(String userData) throws RepositoryException {
        sessionInfo.setUserData(userData);
    }
    //--------------------------------------------------------------------------

    /**
     * Gets the event filters from the passed listener list.
     *
     * @param listeners the internal event listeners.
     * @return Array of EventFilter
     */
    private static EventFilter[] getEventFilters(Collection<InternalEventListener> listeners) {
        List<EventFilter> filters = new ArrayList<EventFilter>();
        for (InternalEventListener listener : listeners) {
            filters.addAll(listener.getEventFilters());
        }
        return filters.toArray(new EventFilter[filters.size()]);
    }

    /**
     * @return a new instance of <code>TransientItemStateFactory</code>.
     * @throws RepositoryException If an error occurs.
     */
    private TransientItemStateFactory createItemStateFactory() throws RepositoryException {
        cache = service.getItemInfoCache(sessionInfo);
        WorkspaceItemStateFactory isf = new WorkspaceItemStateFactory(service, sessionInfo,
                getItemDefinitionProvider(), cache);

        TransientItemStateFactory tisf = new TransientISFactory(isf, getItemDefinitionProvider());
        return tisf;
    }

    /**
     * @param tisf The transient item state factory.
     * @param idFactory The id factory.
     * @return a new instance of <code>HierarchyManager</code>.
     * @throws javax.jcr.RepositoryException If an error occurs.
     */
    private HierarchyManager createHierarchyManager(TransientItemStateFactory tisf, IdFactory idFactory) throws RepositoryException {
        return new HierarchyManagerImpl(tisf, idFactory, getPathFactory());
    }

    /**
     * @param hierarchyMgr The hierarchy manager.
     * @return a new InternalEventListener
     */
    private InternalEventListener createHierarchyListener(HierarchyManager hierarchyMgr) {
        InternalEventListener listener = new HierarchyEventListener(this, hierarchyMgr, config.getCacheBehaviour());
        return listener;
    }

    /**
     * @param nsRegistry The namespace registry.
     * @return an instance of <code>NodeTypeRegistryImpl</code>.
     */
    private NodeTypeRegistryImpl createNodeTypeRegistry(NamespaceRegistry nsRegistry) {
        NodeTypeStorage ntst = new NodeTypeStorage() {
            public Iterator<QNodeTypeDefinition> getAllDefinitions() throws RepositoryException {
                return service.getQNodeTypeDefinitions(sessionInfo);
            }
            public Iterator<QNodeTypeDefinition> getDefinitions(Name[] nodeTypeNames) throws NoSuchNodeTypeException, RepositoryException {
                return service.getQNodeTypeDefinitions(sessionInfo, nodeTypeNames);
            }
            public void registerNodeTypes(QNodeTypeDefinition[] nodeTypeDefs, boolean allowUpdate) throws RepositoryException {
                service.registerNodeTypes(sessionInfo, nodeTypeDefs, allowUpdate);
            }
            public void unregisterNodeTypes(Name[] nodeTypeNames) throws NoSuchNodeTypeException, RepositoryException {
                service.unregisterNodeTypes(sessionInfo, nodeTypeNames);
            }
        };
        NodeTypeCache ntCache = NodeTypeCache.getInstance(service, sessionInfo.getUserID());
        ntst = ntCache.wrap(ntst);
        return NodeTypeRegistryImpl.create(ntst, nsRegistry);
    }

    /**
     * @param entProvider The effective node type provider.
     * @return  a new instance of <code>ItemDefinitionProvider</code>.
     */
    private ItemDefinitionProvider createDefinitionProvider(EffectiveNodeTypeProvider entProvider) {
        return new ItemDefinitionProviderImpl(entProvider, service, sessionInfo);
    }

    /**
     * Creates a background thread which polls for external changes on the
     * RepositoryService.
     *
     * @param pollTimeout the polling timeout in milliseconds.
     * @param enableObservation if observation should be enabled.
     * @return the background polling thread or <code>null</code> if the underlying
     *         <code>RepositoryService</code> does not support observation.
     */
    private Thread createChangeFeed(int pollTimeout, boolean enableObservation) {
        Thread t = null;
        if (enableObservation) {
            t = new Thread(new ChangePolling(pollTimeout));
            t.setName("Change Polling");
            t.setDaemon(true);
            t.start();
        }
        return t;
    }

    //-----------------------------------------------------< wsp management >---
    /**
     * Create a new workspace with the specified <code>name</code>. If
     * <code>srcWorkspaceName</code> isn't <code>null</code> the content of
     * that workspace is used as initial content, otherwise an empty workspace
     * will be created.
     *
     * @param name The name of the workspace to be created.
     * @param srcWorkspaceName The name of the workspace from which the initial
     * content of the new workspace will be 'cloned'.
     * @throws RepositoryException If an exception occurs.
     */
    void createWorkspace(String name, String srcWorkspaceName) throws RepositoryException {
        service.createWorkspace(sessionInfo, name, srcWorkspaceName);
    }

    /**
     * Deletes the workspace with the specified <code>name</code>.
     *
     * @param name The name of the workspace to be deleted.
     * @throws RepositoryException If the operation fails.
     */
    void deleteWorkspace(String name) throws RepositoryException {
        service.deleteWorkspace(sessionInfo, name);
    }

    //------------------------------------------< UpdatableItemStateManager >---
    /**
     * Creates a new batch from the single workspace operation and executes it.
     *
     * @see UpdatableItemStateManager#execute(Operation)
     */
    public void execute(Operation operation) throws RepositoryException {
        // block event delivery while changes are executed
        try {
            updateSync.acquire();
        } catch (InterruptedException e) {
            throw new RepositoryException(e);
        }
        try {
            /*
            Execute operation and delegate invalidation of affected item
            states to the operation.
            NOTE, that the invalidation is independent of the cache behaviour
            due to the fact, that local event bundles are not processed by
            the HierarchyEventListener.
            */
            new OperationVisitorImpl(sessionInfo).execute(operation);
            operation.persisted();
        } finally {
            updateSync.release();
        }
    }

    /**
     * Creates a new batch from the given <code>ChangeLog</code> and executes it.
     *
     * @param changes The set of transient changes to be executed.
     * @throws RepositoryException
     */
    public void execute(ChangeLog changes) throws RepositoryException {
        // block event delivery while changes are executed
        try {
            updateSync.acquire();
        } catch (InterruptedException e) {
            throw new RepositoryException(e);
        }
        try {
            new OperationVisitorImpl(sessionInfo).execute(changes);
            changes.persisted();
        } finally {
            updateSync.release();
        }
    }

    /**
     * Dispose this <code>WorkspaceManager</code>
     */
    public synchronized void dispose() {
        try {
            updateSync.acquire();
        } catch (InterruptedException e) {
            log.warn("Exception while disposing WorkspaceManager: " + e);
            return;
        }
        try {
            if (changeFeed != null) {
                disposeChangeFeed = true;
                changeFeed.interrupt();
                changeFeed.join();
            }
            hierarchyManager.dispose();
            if (subscription != null) {
                service.dispose(subscription);
            }
            service.dispose(sessionInfo);
            cache.dispose();
        } catch (Exception e) {
            log.warn("Exception while disposing WorkspaceManager: " + e);
        } finally {
            updateSync.release();
        }
        ntRegistry.dispose();
    }

    //------------------------------------------------------< AccessManager >---
    /**
     * @see AccessManager#isGranted(NodeState, Path, String[])
     */
    public boolean isGranted(NodeState parentState, Path relPath, String[] actions)
            throws ItemNotFoundException, RepositoryException {
        if (parentState.getStatus() == Status.NEW) {
            return true;
        }
        // TODO: check again.
        // build itemId from the given state and the relative path without
        // making an attempt to retrieve the proper id of the item possibly
        // identified by the resulting id.
        // the server must be able to deal with paths and with proper ids anyway.
        // TODO: 'createNodeId' is basically wrong since isGranted is unspecific for any item.
        ItemId id = idFactory.createNodeId((NodeId) parentState.getWorkspaceId(), relPath);
        return service.isGranted(sessionInfo, id, actions);
    }

    /**
     * @see AccessManager#isGranted(ItemState, String[])
     */
    public boolean isGranted(ItemState itemState, String[] actions) throws ItemNotFoundException, RepositoryException {
        // a 'new' state can always be read, written and removed
        if (itemState.getStatus() == Status.NEW) {
            return true;
        }
        return service.isGranted(sessionInfo, itemState.getWorkspaceId(), actions);
    }

    /**
     * @see AccessManager#canRead(ItemState)
     */
    public boolean canRead(ItemState itemState) throws ItemNotFoundException, RepositoryException {
        // a 'new' state can always be read
        if (itemState.getStatus() == Status.NEW) {
            return true;
        }
        return service.isGranted(sessionInfo, itemState.getWorkspaceId(), AccessManager.READ);
    }

    /**
     * @see AccessManager#canRemove(ItemState)
     */
    public boolean canRemove(ItemState itemState) throws ItemNotFoundException, RepositoryException {
        // a 'new' state can always be removed again
        if (itemState.getStatus() == Status.NEW) {
            return true;
        }
        return service.isGranted(sessionInfo, itemState.getWorkspaceId(), AccessManager.REMOVE);
    }

    /**
     * @see AccessManager#canAccess(String)
     */
    public boolean canAccess(String workspaceName) throws NoSuchWorkspaceException, RepositoryException {
        for (String wspName : getWorkspaceNames()) {
            if (wspName.equals(workspaceName)) {
                return true;
            }
        }
        return false;
    }

    //---------------------------------------------------< NamespaceStorage >---

    public Map<String, String> getRegisteredNamespaces() throws RepositoryException {
        return service.getRegisteredNamespaces(sessionInfo);
    }

    public String getPrefix(String uri) throws NamespaceException, RepositoryException {
        return service.getNamespacePrefix(sessionInfo, uri);
    }

    public String getURI(String prefix) throws NamespaceException, RepositoryException {
        return service.getNamespaceURI(sessionInfo, prefix);
    }

    public void registerNamespace(String prefix, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        service.registerNamespace(sessionInfo, prefix, uri);
    }

    public void unregisterNamespace(String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        service.unregisterNamespace(sessionInfo, uri);
    }

    //--------------------------------------------------------------------------
    /**
     * Called when local or external events occurred. This method is called after
     * changes have been applied to the repository.
     *
     * @param eventBundles the event bundles generated by the repository service
     *                     as the effect of an local or external change.
     * @param lstnrs Array of internal event listeners
     * @throws InterruptedException if this thread is interrupted while waiting
     *                              for the {@link #updateSync}.
     */
    private void onEventReceived(EventBundle[] eventBundles,
                                 InternalEventListener[] lstnrs)
            throws InterruptedException {
        if (log.isDebugEnabled()) {
            log.debug("received {} event bundles.", eventBundles.length);
            for (EventBundle eventBundle : eventBundles) {
                log.debug("IsLocal:  {}", eventBundle.isLocal());
                for (Iterator<Event> it = eventBundle.getEvents(); it.hasNext();) {
                    Event e = it.next();
                    String type;
                    switch (e.getType()) {
                        case Event.NODE_ADDED:
                            type = "NodeAdded";
                            break;
                        case Event.NODE_REMOVED:
                            type = "NodeRemoved";
                            break;
                        case Event.PROPERTY_ADDED:
                            type = "PropertyAdded";
                            break;
                        case Event.PROPERTY_CHANGED:
                            type = "PropertyChanged";
                            break;
                        case Event.PROPERTY_REMOVED:
                            type = "PropertyRemoved";
                            break;
                        case Event.NODE_MOVED:
                            type = "NodeMoved";
                            break;
                        case Event.PERSIST:
                            type = "Persist";
                            break;
                        default:
                            type = "Unknown";
                    }
                    log.debug("  {}; {}", e.getPath(), type);
                }
            }
        }

        // do not deliver events while an operation executes
        updateSync.acquire();
        try {
            // notify listener
            for (EventBundle eventBundle : eventBundles) {
                for (InternalEventListener lstnr : lstnrs) {
                    try {
                        lstnr.onEvent(eventBundle);
                    } catch (Exception e) {
                        log.warn("Exception in event polling thread: " + e);
                        log.debug("Dump:", e);
                    }
                }
            }
        } finally {
            updateSync.release();
        }
    }

    /**
     * Executes a sequence of operations on the repository service within
     * a given <code>SessionInfo</code>.
     */
    private final class OperationVisitorImpl implements OperationVisitor {

        /**
         * The session info for all operations in this batch.
         */
        private final SessionInfo sessionInfo;

        private Batch batch;

        private OperationVisitorImpl(SessionInfo sessionInfo) {
            this.sessionInfo = sessionInfo;
        }

        /**
         * Executes the operations on the repository service.
         *
         * @param changeLog The changelog to be executed
         * @throws javax.jcr.AccessDeniedException
         * @throws javax.jcr.ItemExistsException
         * @throws javax.jcr.UnsupportedRepositoryOperationException
         * @throws javax.jcr.nodetype.ConstraintViolationException
         * @throws javax.jcr.nodetype.NoSuchNodeTypeException
         * @throws javax.jcr.version.VersionException
         */
        private void execute(ChangeLog changeLog) throws RepositoryException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException {
            RepositoryException ex = null;
            try {
                ItemState target = changeLog.getTarget();
                batch = service.createBatch(sessionInfo, target.getId());
                for (Operation op : changeLog.getOperations()) {
                    log.debug("executing " + op.getName());
                    op.accept(this);
                }
            } catch (RepositoryException e) {
                ex = e;
            } finally {
                if (batch != null) {
                    try {
                        // submit must be called even in case there is an
                        // exception to give the service a chance to clean
                        // up the batch
                        service.submit(batch);
                    } catch (RepositoryException e) {
                        if (ex == null) {
                            ex = e;
                        } else {
                            log.warn("Exception submitting batch", e);
                        }
                    }
                    // reset batch field
                    batch = null;
                }
            }
            if (ex != null) {
                throw ex;
            }
        }

        /**
         * Executes the operations on the repository service.
         *
         * @param workspaceOperation The workspace operation to be executed.
         * @throws javax.jcr.AccessDeniedException
         * @throws javax.jcr.ItemExistsException
         * @throws javax.jcr.UnsupportedRepositoryOperationException
         * @throws javax.jcr.nodetype.ConstraintViolationException
         * @throws javax.jcr.nodetype.NoSuchNodeTypeException
         * @throws javax.jcr.version.VersionException
         */
        private void execute(Operation workspaceOperation) throws RepositoryException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException {
            log.debug("executing " + workspaceOperation.getName());
            workspaceOperation.accept(this);
        }

        //-----------------------------------------------< OperationVisitor >---
        /**
         * @see OperationVisitor#visit(AddNode)
         */
        public void visit(AddNode operation) throws RepositoryException {
            NodeId parentId = operation.getParentId();
            batch.addNode(parentId, operation.getNodeName(), operation.getNodeTypeName(), operation.getUuid());
        }

        /**
         * @see OperationVisitor#visit(AddProperty)
         */
        public void visit(AddProperty operation) throws RepositoryException {
            NodeId parentId = operation.getParentId();
            Name propertyName = operation.getPropertyName();
            if (operation.isMultiValued()) {
                batch.addProperty(parentId, propertyName, operation.getValues());
            } else {
                QValue value = operation.getValues()[0];
                batch.addProperty(parentId, propertyName, value);
            }
        }

        /**
         * @see OperationVisitor#visit(org.apache.jackrabbit.jcr2spi.operation.SetTree)
         */
        public void visit(SetTree operation) throws RepositoryException {
            NodeState treeState = operation.getTreeState();
            Tree tree = service.createTree(sessionInfo, batch, treeState.getName(), treeState.getNodeTypeName(), treeState.getUniqueID());
            populateTree(tree, treeState.getNodeEntry());
            batch.setTree(operation.getParentId(), tree);
        }

        private void populateTree(Tree tree, NodeEntry nodeEntry) throws RepositoryException {
            Iterator<PropertyEntry> pEntries = nodeEntry.getPropertyEntries();
            while (pEntries.hasNext()) {
                PropertyState ps = pEntries.next().getPropertyState();
                if (!NameConstants.JCR_PRIMARYTYPE.equals(ps.getName()) && !NameConstants.JCR_UUID.equals(ps.getName())) {
                    if (ps.isMultiValued()) {
                        tree.addProperty(ps.getParent().getNodeId(), ps.getName(), ps.getType(), ps.getValues());
                    } else {
                        tree.addProperty(ps.getParent().getNodeId(), ps.getName(), ps.getType(), ps.getValue());
                    }
                }
            }

            Iterator<NodeEntry> nEntries = nodeEntry.getNodeEntries();
            while (nEntries.hasNext()) {
                NodeEntry child = nEntries.next();
                NodeState childState = child.getNodeState();
                Tree childTree = tree.addChild(childState.getName(), childState.getNodeTypeName(), childState.getUniqueID());
                populateTree(childTree, child);
            }
        }

        /**
         * @see OperationVisitor#visit(Clone)
         */
        public void visit(Clone operation) throws NoSuchWorkspaceException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
            NodeId nId = operation.getNodeId();
            NodeId destParentId = operation.getDestinationParentId();
            service.clone(sessionInfo, operation.getWorkspaceName(), nId, destParentId, operation.getDestinationName(), operation.isRemoveExisting());
        }

        /**
         * @see OperationVisitor#visit(Copy)
         */
        public void visit(Copy operation) throws NoSuchWorkspaceException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
            NodeId nId = operation.getNodeId();
            NodeId destParentId = operation.getDestinationParentId();
            service.copy(sessionInfo, operation.getWorkspaceName(), nId, destParentId, operation.getDestinationName());
        }

        /**
         * @see OperationVisitor#visit(Move)
         */
        public void visit(Move operation) throws LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
            NodeId moveId = operation.getSourceId();
            NodeId destParentId = operation.getDestinationParentId();

            if (batch == null) {
                service.move(sessionInfo, moveId, destParentId, operation.getDestinationName());
            } else {
                batch.move(moveId, destParentId, operation.getDestinationName());
            }
        }

        /**
         * @see OperationVisitor#visit(Update)
         */
        public void visit(Update operation) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
            NodeId nId = operation.getNodeId();
            service.update(sessionInfo, nId, operation.getSourceWorkspaceName());
        }

        /**
         * @see OperationVisitor#visit(Remove)
         */
        public void visit(Remove operation) throws RepositoryException {
            batch.remove(operation.getRemoveId());
        }

        /**
         * @see OperationVisitor#visit(SetMixin)
         */
        public void visit(SetMixin operation) throws RepositoryException {
            batch.setMixins(operation.getNodeId(), operation.getMixinNames());
        }

        /**
         * @see OperationVisitor#visit(SetPrimaryType)
         */
        public void visit(SetPrimaryType operation) throws RepositoryException {
            batch.setPrimaryType(operation.getNodeId(), operation.getPrimaryTypeName());
        }

        /**
         * @see OperationVisitor#visit(SetPropertyValue)
         */
        public void visit(SetPropertyValue operation) throws RepositoryException {
            PropertyId id = operation.getPropertyId();
            if (operation.isMultiValued()) {
                batch.setValue(id, operation.getValues());
            } else {
                batch.setValue(id, operation.getValues()[0]);
            }
        }

        /**
         * @see OperationVisitor#visit(ReorderNodes)
         */
        public void visit(ReorderNodes operation) throws RepositoryException {
            NodeId parentId = operation.getParentId();
            NodeId insertId = operation.getInsertId();
            NodeId beforeId = operation.getBeforeId();
            batch.reorderNodes(parentId, insertId, beforeId);
        }

        /**
         * @see OperationVisitor#visit(Checkout)
         */
        public void visit(Checkout operation) throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
            if (operation.supportsActivity()) {
                service.checkout(sessionInfo, operation.getNodeId(), operation.getActivityId());
            } else {
                service.checkout(sessionInfo, operation.getNodeId());
            }
        }

        /**
         * @see OperationVisitor#visit(Checkin)
         */
        public void visit(Checkin operation) throws UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
            NodeId newId = service.checkin(sessionInfo, operation.getNodeId());
            operation.setNewVersionId(newId);
        }

        /**
         * @see OperationVisitor#visit(Checkpoint)
         */
        public void visit(Checkpoint operation) throws UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
            NodeId newId;
            if (operation.supportsActivity()) {
                newId = service.checkpoint(sessionInfo, operation.getNodeId(), operation.getActivityId());
            } else {
                newId = service.checkpoint(sessionInfo, operation.getNodeId());
            }
            operation.setNewVersionId(newId);
        }

        /**
         * @see OperationVisitor#visit(Restore)
         */
        public void visit(Restore operation) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
            NodeId nId = operation.getNodeId();
            if (nId == null) {
                service.restore(sessionInfo, operation.getVersionIds(), operation.removeExisting());
            } else {
                NodeId targetId;
                Path relPath = operation.getRelativePath();
                if (relPath != null) {
                    targetId = idFactory.createNodeId(nId, relPath);
                } else {
                    targetId = nId;
                }
                NodeId versionId = operation.getVersionIds()[0];
                service.restore(sessionInfo, targetId, versionId, operation.removeExisting());
            }
        }

        /**
         * @see OperationVisitor#visit(Merge)
         */
        public void visit(Merge operation) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
            NodeId nId = operation.getNodeId();
            Iterator<NodeId> failed;
            if (operation.isActivityMerge()) {
                failed = service.mergeActivity(sessionInfo, nId);
            } else {
                failed = service.merge(sessionInfo, nId, operation.getSourceWorkspaceName(), operation.bestEffort(), operation.isShallow());
            }
            operation.setFailedIds(failed);
        }

        /**
         * @see OperationVisitor#visit(ResolveMergeConflict)
         */
        public void visit(ResolveMergeConflict operation) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
            NodeId nId = operation.getNodeId();
            NodeId[] mergedFailedIds = operation.getMergeFailedIds();
            NodeId[] predecessorIds = operation.getPredecessorIds();
            service.resolveMergeConflict(sessionInfo, nId, mergedFailedIds, predecessorIds);
        }

        /**
         * @see OperationVisitor#visit(LockOperation)
         */
        public void visit(LockOperation operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
            LockInfo lInfo = service.lock(sessionInfo, operation.getNodeId(), operation.isDeep(), operation.isSessionScoped(), operation.getTimeoutHint(), operation.getOwnerHint());
            operation.setLockInfo(lInfo);
        }

        /**
         * @see OperationVisitor#visit(LockRefresh)
         */
        public void visit(LockRefresh operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
            service.refreshLock(sessionInfo, operation.getNodeId());
        }

        /**
         * @see OperationVisitor#visit(LockRelease)
         */
        public void visit(LockRelease operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
            service.unlock(sessionInfo, operation.getNodeId());
        }

        /**
         * @see OperationVisitor#visit(AddLabel)
         */
        public void visit(AddLabel operation) throws VersionException, RepositoryException {
            NodeId vhId = operation.getVersionHistoryId();
            NodeId vId = operation.getVersionId();
            service.addVersionLabel(sessionInfo, vhId, vId, operation.getLabel(), operation.moveLabel());
        }

        /**
         * @see OperationVisitor#visit(RemoveLabel)
         */
        public void visit(RemoveLabel operation) throws VersionException, RepositoryException {
            NodeId vhId = operation.getVersionHistoryId();
            NodeId vId = operation.getVersionId();
            service.removeVersionLabel(sessionInfo, vhId, vId, operation.getLabel());
        }

        /**
         * @see OperationVisitor#visit(RemoveVersion)
         */
        public void visit(RemoveVersion operation) throws VersionException, AccessDeniedException, ReferentialIntegrityException, RepositoryException {
            NodeId versionId = (NodeId) operation.getRemoveId();
            NodeState vhState = operation.getParentState();
            service.removeVersion(sessionInfo, (NodeId) vhState.getWorkspaceId(), versionId);
        }

        /**
         * @see OperationVisitor#visit(WorkspaceImport)
         */
        public void visit(WorkspaceImport operation) throws RepositoryException {
            service.importXml(sessionInfo, operation.getNodeId(), operation.getXmlStream(), operation.getUuidBehaviour());
        }

        /**
         * @see OperationVisitor#visit(CreateActivity)
         */
        public void visit(CreateActivity operation) throws RepositoryException {
            NodeId activityId = service.createActivity(sessionInfo, operation.getTitle());
            operation.setNewActivityId(activityId);
        }

        /**
         * @see OperationVisitor#visit(RemoveActivity)
         */
        public void visit(RemoveActivity operation) throws RepositoryException {
            service.removeActivity(sessionInfo, (NodeId) operation.getRemoveId());
        }

        /**
         * @see OperationVisitor#visit(CreateConfiguration)
         */
        public void visit(CreateConfiguration operation) throws RepositoryException {
            NodeId configId = service.createConfiguration(sessionInfo, operation.getNodeId());
            operation.setNewConfigurationId(configId);
        }
    }

    //------------------------------------------------------< ChangePolling >---
    /**
     * Implements the polling for changes on the repository service.
     */
    private final class ChangePolling implements Runnable {

        /**
         * The polling timeout in milliseconds.
         */
        private final int pollTimeout;

        /**
         * Creates a new change polling with a given polling timeout.
         *
         * @param pollTimeout the timeout in milliseconds.
         */
        private ChangePolling(int pollTimeout) {
            this.pollTimeout = pollTimeout;
        }

        public void run() {
            String wspName = sessionInfo.getWorkspaceName();            
            while (!Thread.interrupted() && !disposeChangeFeed) {
                try {
                    InternalEventListener[] iel;
                    Subscription subscr;
                    synchronized (listeners) {
                        while (subscription == null) {
                            listeners.wait();
                        }
                        iel = listeners.toArray(new InternalEventListener[listeners.size()]);
                        subscr = subscription;
                    }

                    log.debug("calling getEvents() (Workspace={})", wspName);
                    EventBundle[] bundles = service.getEvents(subscr, pollTimeout);
                    log.debug("returned from getEvents() (Workspace={})", wspName);
                    // check if thread had been interrupted while
                    // getting events
                    if (Thread.interrupted() || disposeChangeFeed) {
                        log.debug("Thread interrupted, terminating...");
                        break;
                    }
                    if (bundles.length > 0) {
                        onEventReceived(bundles, iel);
                    }
                } catch (UnsupportedRepositoryOperationException e) {
                    log.error("SPI implementation does not support observation: " + e);
                    // terminate
                    break;
                } catch (RepositoryException e) {
                    log.info("Workspace=" + wspName + ": Exception while retrieving event bundles: " + e);
                    log.debug("Dump:", e);
                } catch (InterruptedException e) {
                    // terminate
                    break;
                }
            }
        }
    }
}
