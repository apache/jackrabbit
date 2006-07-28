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

import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeRegistryImpl;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeStorage;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.jcr2spi.name.NamespaceStorage;
import org.apache.jackrabbit.jcr2spi.name.NamespaceRegistryImpl;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.NoSuchItemStateException;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.state.ChangeLog;
import org.apache.jackrabbit.jcr2spi.state.UpdatableItemStateManager;
import org.apache.jackrabbit.jcr2spi.state.NodeReferences;
import org.apache.jackrabbit.jcr2spi.state.ItemStateManager;
import org.apache.jackrabbit.jcr2spi.state.CachingItemStateManager;
import org.apache.jackrabbit.jcr2spi.state.ItemStateFactory;
import org.apache.jackrabbit.jcr2spi.operation.OperationVisitor;
import org.apache.jackrabbit.jcr2spi.operation.AddNode;
import org.apache.jackrabbit.jcr2spi.operation.AddProperty;
import org.apache.jackrabbit.jcr2spi.operation.Clone;
import org.apache.jackrabbit.jcr2spi.operation.Copy;
import org.apache.jackrabbit.jcr2spi.operation.Move;
import org.apache.jackrabbit.jcr2spi.operation.Remove;
import org.apache.jackrabbit.jcr2spi.operation.SetMixin;
import org.apache.jackrabbit.jcr2spi.operation.SetPropertyValue;
import org.apache.jackrabbit.jcr2spi.operation.ReorderNodes;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.operation.Checkout;
import org.apache.jackrabbit.jcr2spi.operation.Checkin;
import org.apache.jackrabbit.jcr2spi.operation.Update;
import org.apache.jackrabbit.jcr2spi.operation.Restore;
import org.apache.jackrabbit.jcr2spi.operation.ResolveMergeConflict;
import org.apache.jackrabbit.jcr2spi.operation.Merge;
import org.apache.jackrabbit.jcr2spi.operation.LockOperation;
import org.apache.jackrabbit.jcr2spi.operation.LockRefresh;
import org.apache.jackrabbit.jcr2spi.operation.LockRelease;
import org.apache.jackrabbit.jcr2spi.operation.AddLabel;
import org.apache.jackrabbit.jcr2spi.operation.RemoveLabel;
import org.apache.jackrabbit.jcr2spi.security.AccessManager;
import org.apache.jackrabbit.jcr2spi.observation.InternalEventListener;
import org.apache.jackrabbit.util.IteratorHelper;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.EventListener;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinitionIterator;
import org.apache.jackrabbit.spi.EventIterator;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.IdIterator;
import org.apache.jackrabbit.value.QValue;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NamespaceException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.ItemExistsException;
import javax.jcr.Repository;
import javax.jcr.InvalidItemStateException;
import javax.jcr.MergeException;
import javax.jcr.Session;
import javax.jcr.version.VersionException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.ConstraintViolationException;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.Collections;
import java.io.InputStream;
import java.io.IOException;

/**
 * <code>WorkspaceManager</code>...
 */
public class WorkspaceManager implements UpdatableItemStateManager,
        ItemStateFactory, NamespaceStorage, NodeTypeStorage, AccessManager {

    private static Logger log = LoggerFactory.getLogger(WorkspaceManager.class);

    private final RepositoryService service;
    private final SessionInfo sessionInfo;

    // TODO: TO-BE-FIXED. Major refactoring of caching mechanism with change to SPI ids
    private final CachingItemStateManager cache;

    // TODO: TO-BE-FIXED. With SPI_ItemId rootId must not be stored separately
    private final NodeId rootNodeId;

    private final NamespaceRegistryImpl nsRegistry;
    private final NodeTypeRegistry ntRegistry;

    /**
     * This is the event listener that listens on the repository service
     * for external changes. If <code>null</code> then the underlying repository
     * service does not support observation.
     */
    private final EventListener externalChangeListener;

    /**
     * List of event listener that are set on this WorkspaceManager to get
     * notifications about local and external changes.
     */
    private Set listeners = new HashSet();

    public WorkspaceManager(RepositoryService service, SessionInfo sessionInfo) throws RepositoryException {
        this.service = service;
        this.sessionInfo = sessionInfo;

        cache = new CachingItemStateManager(this, service.getIdFactory());
        addEventListener(cache);

        nsRegistry = createNamespaceRegistry();
        ntRegistry = createNodeTypeRegistry(nsRegistry);
        rootNodeId = createRootNodeId();
        externalChangeListener = createChangeListener();
    }

    public NamespaceRegistryImpl getNamespaceRegistryImpl() {
        return nsRegistry;
    }

    public NodeTypeRegistry getNodeTypeRegistry() {
        return ntRegistry;
    }

    public NodeId getRootNodeId() {
        return rootNodeId;
    }

    public String[] getWorkspaceNames() throws RepositoryException {
        // TODO: review
        return service.getWorkspaceNames(sessionInfo);
    }

    public IdFactory getIdFactory() {
        return service.getIdFactory();
    }

    public LockInfo getLockInfo(NodeId nodeId) throws LockException, RepositoryException {
        return service.getLockInfo(sessionInfo, nodeId);
    }

    public String[] getLockTokens() {
        return sessionInfo.getLockTokens();
    }

    /**
     * This method always succeeds.
     * This is not compliant to the requirements for {@link Session#addLockToken(String)}
     * as defined by JSR170, which defines that at most one single <code>Session</code>
     * may contain the same lock token. However, with SPI it is not possible
     * to determine, whether another session holds the lock, nor can the client
     * determine, which lock this token belongs to. The latter would be
     * necessary in order to build the 'Lock' object properly.
     *
     * TODO: check if throwing an exception would be more appropriate
     *
     * @param lt
     * @throws LockException
     * @throws RepositoryException
     */
    public void addLockToken(String lt) throws LockException, RepositoryException {
        sessionInfo.addLockToken(lt);
        /*
        // TODO: JSR170 defines that a token can be present with one session only.
        //       however, we cannot find out about another session holding the lock.
        //       and neither knows the server, which session is holding a lock token.
        // TODO: check if throwing would be more appropriate
        throw new UnsupportedRepositoryOperationException("Session.addLockToken is not possible on the client.");
        */
    }

    /**
     * Tries to remove the given token from the <code>SessionInfo</code>. If the
     * SessionInfo does not contains the specified token, this method returns
     * silently.<br>
     * Note, that any restriction regarding removal of lock tokens must be asserted
     * before this method is called.
     *
     * @param lt
     * @throws LockException
     * @throws RepositoryException
     */
    public void removeLockToken(String lt) throws LockException, RepositoryException {
        sessionInfo.removeLockToken(lt);
    }

    public String[] getSupportedQueryLanguages() throws RepositoryException {
        // TODO: review
        return service.getSupportedQueryLanguages(sessionInfo);
    }

    public QueryInfo executeQuery(String statement, String language)
            throws RepositoryException {
        return service.executeQuery(sessionInfo, statement, language);
    }

    /**
     * Sets the <code>InternalEventListener</code> that gets notifications about
     * local and external changes.
     * @param listener the new listener.
     */
    public void addEventListener(InternalEventListener listener) {
        listeners.add(listener);
    }

    /**
     *
     * @param listener
     */
    public void removeEventListener(InternalEventListener listener) {
        listeners.remove(listener);
    }
    //--------------------------------------------------------------------------


    private NamespaceRegistryImpl createNamespaceRegistry() throws RepositoryException {
        return new NamespaceRegistryImpl(this, service.getRegisteredNamespaces(sessionInfo));
    }

    private NodeTypeRegistry createNodeTypeRegistry(NamespaceRegistry nsRegistry) throws RepositoryException {
        QNodeDefinition rootNodeDef = service.getNodeDefinition(sessionInfo, service.getRootId(sessionInfo));
        QNodeTypeDefinitionIterator it = service.getNodeTypeDefinitions(sessionInfo);
        List ntDefs = new ArrayList();
        while (it.hasNext()) {
            ntDefs.add(it.nextDefinition());
        }
        return NodeTypeRegistryImpl.create(ntDefs, this, rootNodeDef, nsRegistry);
    }

    /**
     * Creates and registers an EventListener on the RepositoryService that
     * listens for external changes.
     *
     * @return the listener or <code>null</code> if the underlying
     *         <code>RepositoryService</code> does not support observation.
     * @throws RepositoryException if an error occurs while registering the
     *                             event listener.
     */
    private EventListener createChangeListener() throws RepositoryException {
        Properties descriptors = service.getRepositoryDescriptors();
        String desc = descriptors.getProperty(Repository.OPTION_OBSERVATION_SUPPORTED);
        EventListener l = null;
        if (Boolean.getBoolean(desc)) {
            l = new EventListener() {
                public void onEvent(EventIterator events) {
                    onEventReceived(null, events, false); // external
                }
            };
            int allTypes = Event.NODE_ADDED | Event.NODE_REMOVED |
                    Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;
            // register for all events
            service.addEventListener(sessionInfo, service.getRootId(sessionInfo),
                    l, allTypes, true, null, null);
        }
        return l;
    }

    private NodeId createRootNodeId() throws RepositoryException {
        return service.getRootId(sessionInfo);
    }

    //---------------------------------------------------< ItemStateManager >---
    /**
     * @inheritDoc
     */
    public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
        // retrieve through cache
        synchronized (cache) {
            return cache.getItemState(id);
        }
    }

    /**
     * @inheritDoc
     */
    public boolean hasItemState(ItemId id) {
        synchronized (cache) {
            return cache.hasItemState(id);
        }
    }

    /**
     * @inheritDoc
     */
    public NodeReferences getNodeReferences(NodeId id) throws NoSuchItemStateException, ItemStateException {
        synchronized (cache) {
            return cache.getNodeReferences(id);
        }
    }

    /**
     * @inheritDoc
     */
    public boolean hasNodeReferences(NodeId id) {
        synchronized (cache) {
            return cache.hasNodeReferences(id);
        }
    }

    //------ updatable -:>> review ---------------------------------------------
    /**
     * Creates a new <code>Batch</code> from the single workspace operation and
     * executes it.
     *
     * @see UpdatableItemStateManager#execute(Operation)
     */
    public void execute(Operation operation) throws RepositoryException {
        new Batch(sessionInfo).execute(operation);
    }

    /**
     * Creates a new <code>Batch</code> from the given <code>Batch</code> and
     * executes it.
     *
     * @param changes
     * @throws RepositoryException
     */
    public void execute(ChangeLog changes) throws RepositoryException {
        new Batch(sessionInfo).execute(changes);
    }

    public void store(ItemState state) throws IllegalStateException {
    }

    public void destroy(ItemState state) throws IllegalStateException {
    }

    public void dispose() {
        if (externalChangeListener != null) {
            try {
                service.removeEventListener(sessionInfo, rootNodeId, externalChangeListener);
            } catch (RepositoryException e) {
                log.warn("exception while disposing workspace manager: " + e);
            }
        }
    }
    //------------------------------------------------------< AccessManager >---

    // TODO: method can be removed, if jcr2spi uses spi-ids as well
    public boolean isGranted(NodeId parentId, Path relPath, String[] actions) throws ItemNotFoundException, RepositoryException {
        // TODO: 'createNodeId' is basically wrong since isGranted is unspecific for any item.
        ItemId id = getIdFactory().createNodeId(parentId, relPath);
        return isGranted(id, actions);
    }

    public boolean isGranted(ItemId id, String[] actions) throws ItemNotFoundException, RepositoryException {
        return service.isGranted(sessionInfo, id, actions);
    }

    public boolean canRead(ItemId id) throws ItemNotFoundException, RepositoryException {
        return service.isGranted(sessionInfo, id, AccessManager.READ);
    }

    public boolean canRemove(ItemId id) throws ItemNotFoundException, RepositoryException {
        return service.isGranted(sessionInfo, id, AccessManager.REMOVE);
    }

    public boolean canAccess(String workspaceName) throws NoSuchWorkspaceException, RepositoryException {
        String[] wspNames = getWorkspaceNames();
        for (int i = 0; i < wspNames.length; i++) {
            if (wspNames[i].equals(wspNames)) {
                return true;
            }
        }
        return false;
    }

    //---------------------------------------------------------< XML import >---
    public void importXml(NodeId parentId, InputStream xmlStream, int uuidBehaviour) throws RepositoryException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, ItemExistsException, VersionException {
        service.importXml(sessionInfo, parentId, xmlStream, uuidBehaviour);
    }

    //---------------------------------------------------< NamespaceStorage >---
    /**
     * @inheritDoc
     */
    public void registerNamespace(String prefix, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        service.registerNamespace(sessionInfo, prefix, uri);
    }

    /**
     * @inheritDoc
     */
    public void unregisterNamespace(String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        service.unregisterNamespace(sessionInfo, uri);
    }

    //----------------------------------------------------< NodetypeStorage >---
    /**
     * @inheritDoc
     */
    public void registerNodeTypes(QNodeTypeDefinition[] nodeTypeDefs) throws NoSuchNodeTypeException, UnsupportedRepositoryOperationException, RepositoryException {
        service.registerNodeTypes(sessionInfo, nodeTypeDefs);
    }

    /**
     * @inheritDoc
     */
    public void reregisterNodeTypes(QNodeTypeDefinition[] nodeTypeDefs) throws NoSuchNodeTypeException, UnsupportedRepositoryOperationException, RepositoryException {
        service.reregisterNodeTypes(sessionInfo, nodeTypeDefs);
    }

    /**
     * @inheritDoc
     */
    public void unregisterNodeTypes(QName[] nodeTypeNames) throws NoSuchNodeTypeException, UnsupportedRepositoryOperationException, RepositoryException {
        service.unregisterNodeTypes(sessionInfo, nodeTypeNames);
    }

    //---------------------------------------------------< ItemStateFactory >---

    /**
     * Creates the node with information retrieved from the
     * <code>RepositoryService</code>.
     *
     * @inheritDoc
     * @see ItemStateFactory#createNodeState(NodeId, ItemStateManager)
     */
    public NodeState createNodeState(NodeId nodeId, ItemStateManager ism)
            throws NoSuchItemStateException, ItemStateException {
        try {
            NodeInfo info = service.getNodeInfo(sessionInfo, nodeId);

            // get parent
            NodeId parentId = (info.getParentId() != null) ? info.getParentId() : null;
            NodeState parent = (parentId != null) ? (NodeState) ism.getItemState(parentId) : null;

            return createNodeState(info, parent);
        } catch (PathNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (RepositoryException e) {
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    /**
     * Creates the node with information retrieved from the
     * <code>RepositoryService</code>.
     *
     * @inheritDoc
     * @see ItemStateFactory#createNodeState(NodeId, NodeState)
     */
    public NodeState createNodeState(NodeId nodeId, NodeState parent)
            throws NoSuchItemStateException, ItemStateException {
        try {
            NodeInfo info = service.getNodeInfo(sessionInfo, nodeId);
            return createNodeState(info, parent);
        } catch (PathNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (RepositoryException e) {
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    /**
     * Creates the node with information retrieved from <code>info</code>.
     *
     * @param info   the <code>NodeInfo</code> to use to create the
     *               <code>NodeState</code>.
     * @param parent the parent <code>NodeState</code>.
     * @return the new <code>NodeState</code>.
     */
    private NodeState createNodeState(NodeInfo info, NodeState parent)
            throws NoSuchItemStateException, ItemStateException {
        try {
            QName ntName = info.getNodetype();

            // TODO pass parent in constructor of NodeState

            // build the node state
            // NOTE: unable to retrieve definitionId -> needs to be retrieved
            // by the itemManager upon Node creation.
            NodeState state = new NodeState(info.getId(), ntName, parent.getNodeId(), ItemState.STATUS_EXISTING, false, getIdFactory());
            // set mixin nodetypes
            state.setMixinTypeNames(info.getMixins());

            // references to child items
            for (IdIterator it = info.getNodeIds(); it.hasNext(); ) {
                NodeInfo childInfo = service.getNodeInfo(sessionInfo, (NodeId) it.nextId());
                NodeId childId = childInfo.getId();
                state.addChildNodeEntry(childInfo.getQName(), childId);
            }

            // references to properties
            for (IdIterator it = info.getPropertyIds(); it.hasNext(); ) {
                PropertyId pId = (PropertyId) it.nextId();
                state.addPropertyName(pId.getQName());
            }

            // copied from local-state-mgr TODO... check
            // register as listener
            // TODO check if needed
            //state.addListener(this);
            return state;
        } catch (PathNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (RepositoryException e) {
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    /**
     * Creates the property with information retrieved from the
     * <code>RepositoryService</code>.
     *
     * @inheritDoc
     * @see ItemStateFactory#createPropertyState(PropertyId, ItemStateManager)
     */
    public PropertyState createPropertyState(PropertyId propertyId,
                                             ItemStateManager ism)
            throws NoSuchItemStateException, ItemStateException {
        try {
            PropertyInfo info = service.getPropertyInfo(sessionInfo, propertyId);
            NodeState parent = (NodeState) ism.getItemState(info.getParentId());
            return createPropertyState(info, parent);
        } catch (PathNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (RepositoryException e) {
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    /**
     * Creates the property with information retrieved from the
     * <code>RepositoryService</code>.
     *
     * @inheritDoc
     * @see ItemStateFactory#createPropertyState(PropertyId, NodeState)
     */
    public PropertyState createPropertyState(PropertyId propertyId,
                                             NodeState parent)
            throws NoSuchItemStateException, ItemStateException {
        try {
            PropertyInfo info = service.getPropertyInfo(sessionInfo, propertyId);
            return createPropertyState(info, parent);
        } catch (PathNotFoundException e) {
            throw new NoSuchItemStateException(e.getMessage(), e);
        } catch (RepositoryException e) {
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    /**
     * Creates the property with information retrieved from <code>info</code>.
     *
     * @param info   the <code>PropertyInfo</code> to use to create the
     *               <code>PropertyState</code>.
     * @param parent the parent <code>NodeState</code>.
     * @return the new <code>PropertyState</code>.
     * @throws ItemStateException if an error occurs while retrieving the
     *                            <code>PropertyState</code>.
     */
    private PropertyState createPropertyState(PropertyInfo info,
                                              NodeState parent)
            throws ItemStateException {
        try {
            // TODO: pass parent in constructor of PropertyState

            // build the PropertyState
            // NOTE: unable to retrieve definitionId -> needs to be retrieved
            // by the itemManager upon Property creation.
            PropertyState state = new PropertyState(info.getId(), ItemState.STATUS_EXISTING, false);
            state.setMultiValued(info.isMultiValued());
            state.setType(info.getType());
            QValue[] qValues;
            if (info.getType() == PropertyType.BINARY) {
                InputStream[] ins = info.getValuesAsStream();
                qValues = new QValue[ins.length];
                for (int i = 0; i < ins.length; i++) {
                    qValues[i] = QValue.create(ins[i]);
                }
            } else {
                String[] str = info.getValues();
                qValues = new QValue[str.length];
                for (int i = 0; i < str.length; i++) {
                    qValues[i] = QValue.create(str[i], info.getType());
                }
            }

            state.setValues(qValues);

            // register as listener
            // TODO check if needed
            // state.addListener(this);
            return state;
        } catch (IOException e) {
            throw new ItemStateException(e.getMessage(), e);
        }
    }

    //--------------------------------------------------------------------------

    /**
     * Called when local or external events occured. This method is called after
     * changes have been applied to the repository.
     *
     * @param changeLog
     * @param events the events.
     * @param isLocal <code>true</code> if changes were local.
     */
    private void onEventReceived(ChangeLog changeLog, EventIterator events, boolean isLocal) {
        if (changeLog != null) {
            // use current change log for notification
            changeLog.persisted();
        }

        // notify listener
        // need to copy events into a list because we notify multiple listeners
        List eventList = new ArrayList();
        while (events.hasNext()) {
            Event e = events.nextEvent();
            eventList.add(e);
        }

        InternalEventListener[] lstnrs = (InternalEventListener[]) listeners.toArray(new InternalEventListener[listeners.size()]);
        for (int i = 0; i < lstnrs.length; i++) {
           lstnrs[i].onEvent(new EventIteratorImpl(eventList), isLocal);
        }
    }

    /**
     * Executes a sequence of operations on the repository service within
     * a given <code>SessionInfo</code>.
     */
    private final class Batch implements OperationVisitor {

        /**
         * The session info for all operations in this batch.
         */
        private final SessionInfo sessionInfo;

        private org.apache.jackrabbit.spi.Batch batch;
        private EventIterator events;

        private Batch(SessionInfo sessionInfo) {
            this.sessionInfo = sessionInfo;
        }

        /**
         * Executes the operations on the repository service.
         */
        private void execute(ChangeLog changeLog) throws RepositoryException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException {
            try {
                batch = service.createBatch(sessionInfo);
                Iterator it = changeLog.getOperations();
                while (it.hasNext()) {
                    Operation op = (Operation) it.next();
                    log.info("executing: " + op);
                    op.accept(this);
                }
            } finally {
                if (batch != null) {
                    EventIterator events = service.submit(batch);
                    onEventReceived(changeLog, events, true);
                    // reset batch field
                    batch = null;
                }
            }
        }

        /**
         * Executes the operations on the repository service.
         */
        private void execute(Operation workspaceOperation) throws RepositoryException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException {
            boolean success = false;
            try {
                log.info("executing: " + workspaceOperation);
                workspaceOperation.accept(this);
                success = true;
            } finally {
                if (success && events != null) {
                    onEventReceived(null, events, true);
                }
            }
        }
        //-----------------------< OperationVisitor >---------------------------

        public void visit(AddNode operation) throws RepositoryException {
            batch.addNode(operation.getParentId(), operation.getNodeName(), operation.getNodeTypeName(), operation.getUuid());
        }

        public void visit(AddProperty operation) throws RepositoryException {
            org.apache.jackrabbit.spi.NodeId parentId = operation.getParentId();
            QName propertyName = operation.getPropertyName();
            int type = operation.getPropertyType();
            if (operation.isMultiValued()) {
                QValue[] values = operation.getValues();
                if (type == PropertyType.BINARY) {
                    InputStream[] ins = new InputStream[values.length];
                    for (int i = 0; i < values.length; i++) {
                        ins[i] = values[i].getStream();
                    }
                    batch.addProperty(parentId, propertyName, ins, type);
                } else {
                    String[] strs = new String[values.length];
                    for (int i = 0; i < values.length; i++) {
                        strs[i] = values[i].getString();
                    }
                    batch.addProperty(parentId, propertyName, strs, type);
                }
            } else {
                QValue value = operation.getValues()[0];
                if (type == PropertyType.BINARY) {
                    batch.addProperty(parentId, propertyName, value.getStream(), type);
                } else {
                    batch.addProperty(parentId, propertyName, value.getString(), type);
                }
            }
        }

        public void visit(Clone operation) throws NoSuchWorkspaceException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
            events = service.clone(sessionInfo, operation.getWorkspaceName(), operation.getNodeId(), operation.getDestinationParentId(), operation.getDestinationName(), operation.isRemoveExisting());
        }

        public void visit(Copy operation) throws NoSuchWorkspaceException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
            events = service.copy(sessionInfo, operation.getWorkspaceName(), operation.getNodeId(), operation.getDestinationParentId(), operation.getDestinationName());
        }

        public void visit(Move operation) throws LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
            if (batch == null) {
                events = service.move(sessionInfo, operation.getNodeId(), operation.getDestinationParentId(), operation.getDestinationName());
            } else {
                batch.move(operation.getNodeId(), operation.getDestinationParentId(), operation.getDestinationName());
            }
        }

        public void visit(Update operation) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
            events = service.update(sessionInfo, operation.getNodeId(), operation.getSourceWorkspaceName());
        }

        public void visit(Remove operation) throws RepositoryException {
            batch.remove(operation.getRemoveId());
        }

        public void visit(SetMixin operation) throws RepositoryException {
            batch.setMixins(operation.getNodeId(), operation.getMixinNames());
        }

        public void visit(SetPropertyValue operation) throws RepositoryException {
            org.apache.jackrabbit.spi.PropertyId id = operation.getPropertyId();
            int type = operation.getPropertyType();
            if (operation.isMultiValued()) {
                QValue[] values = operation.getValues();
                if (type == PropertyType.BINARY) {
                    InputStream[] ins = new InputStream[values.length];
                    for (int i = 0; i < values.length; i++) {
                        ins[i] = values[i].getStream();
                    }
                    batch.setValue(id, ins, type);
                } else {
                    String[] strs = new String[values.length];
                    for (int i = 0; i < values.length; i++) {
                        strs[i] = values[i].getString();
                    }
                    batch.setValue(id, strs, type);
                }
            } else {
                QValue value = operation.getValues()[0];
                if (operation.getPropertyType() == PropertyType.BINARY) {
                    batch.setValue(id, value.getStream(), type);
                } else {
                    batch.setValue(id, value.getString(), type);
                }
            }
        }

        public void visit(ReorderNodes operation) throws RepositoryException {
            batch.reorderNodes(operation.getParentId(), operation.getInsertNodeId(), operation.getBeforeNodeId());
        }

        public void visit(Checkout operation) throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
            events = service.checkout(sessionInfo, operation.getNodeId());
        }

        public void visit(Checkin operation) throws UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
            events = service.checkin(sessionInfo, operation.getNodeId());
        }

        public void visit(Restore operation) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
            NodeId nId = operation.getNodeId();
            NodeId[] versionIds = operation.getVersionIds();
            NodeId[] vIds = new NodeId[versionIds.length];
            for (int i = 0; i < vIds.length; i++) {
                vIds[i] = versionIds[i];
            }

            if (nId == null) {
                events = service.restore(sessionInfo, vIds, operation.removeExisting());
            } else {
                if (vIds.length > 1) {
                    throw new IllegalArgumentException("Restore from a single node must specify but one single Version.");
                }
                events = service.restore(sessionInfo, nId, vIds[0], operation.removeExisting());
            }
        }

        public void visit(Merge operation) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
            events = service.merge(sessionInfo, operation.getNodeId(), operation.getSourceWorkspaceName(), operation.bestEffort());
            // todo: improve.... inform operation about modified items (build mergefailed iterator)
            operation.getEventListener().onEvent(events, true);
        }

        public void visit(ResolveMergeConflict operation) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
            try {
                NodeId nId = operation.getNodeId();
                NodeId vId = operation.getVersionId();

                PropertyState mergeFailedState = (PropertyState) cache.getItemState(
                        getIdFactory().createPropertyId(nId, QName.JCR_MERGEFAILED));

                QValue[] vs = mergeFailedState.getValues();

                NodeId[] mergeFailedIds = new NodeId[vs.length - 1];
                for (int i = 0, j = 0; i < vs.length; i++) {
                    NodeId id = getIdFactory().createNodeId(vs[i].getString());
                    if (!id.equals(vId)) {
                        mergeFailedIds[j] = id;
                        j++;
                    }
                    // else: the version id is being solved by this call and not
                    // part of 'jcr:mergefailed' any more
                }

                PropertyState predecessorState = (PropertyState) cache.getItemState(
                        getIdFactory().createPropertyId(nId, QName.JCR_PREDECESSORS));

                vs = predecessorState.getValues();

                boolean resolveDone = operation.resolveDone();
                int noOfPredecessors = (resolveDone) ? vs.length + 1 : vs.length;
                NodeId[] predecessorIds = new NodeId[noOfPredecessors];

                int i = 0;
                while (i < vs.length) {
                    predecessorIds[i] = getIdFactory().createNodeId(vs[i].getString());
                    i++;
                }
                if (resolveDone) {
                    predecessorIds[i] = vId;
                }
                events = service.resolveMergeConflict(sessionInfo, nId, mergeFailedIds, predecessorIds);
            } catch (ItemStateException e) {
                // should not occur.
                throw new RepositoryException(e);
            }
        }

        public void visit(LockOperation operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
            events = service.lock(sessionInfo, operation.getNodeId(), operation.isDeep());
        }

        public void visit(LockRefresh operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
            events = service.refreshLock(sessionInfo, operation.getNodeId());
        }

        public void visit(LockRelease operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
            events = service.unlock(sessionInfo, operation.getNodeId());
        }

        public void visit(AddLabel operation) throws VersionException, RepositoryException {
            events = service.addVersionLabel(sessionInfo, operation.getVersionHistoryId(), operation.getVersionId(), operation.getLabel(), operation.moveLabel());
        }

        public void visit(RemoveLabel operation) throws VersionException, RepositoryException {
            events = service.removeVersionLabel(sessionInfo, operation.getVersionHistoryId(), operation.getVersionId(), operation.getLabel());
        }
    }

    private static final class EventIteratorImpl extends IteratorHelper implements EventIterator {

        public EventIteratorImpl(Collection c) {
            super(c);
        }

        public Event nextEvent() {
            return (Event) next();
        }
    }

    /**
     * <code>NodeReferences</code> represents the references (i.e. properties of
     * type <code>REFERENCE</code>) to a particular node (denoted by its uuid).
     */
    public class NodeReferencesImpl implements NodeReferences {

        /**
         * identifier of this <code>NodeReferences</code> instance.
         */
        private NodeId id;

        /**
         * list of PropertyId's (i.e. the id's of the properties that refer to
         * the target node denoted by <code>id.getTargetId()</code>).
         * <p/>
         * note that the list can contain duplicate entries because a specific
         * REFERENCE property can contain multiple references (if it's multi-valued)
         * to potentially the same target node.
         */
        private ArrayList references = new ArrayList();

        /**
         * Package private constructor
         *
         * @param id
         */
        private NodeReferencesImpl(NodeId id) {
            this.id = id;
        }

        //-------------------------------------------------< NodeReferences >---
        /**
         * Returns the identifier of this node references object.
         *
         * @return the id of this node references object.
         */
        public NodeId getId() {
            return id;
        }

        /**
         * Returns a flag indicating whether this object holds any references
         *
         * @return <code>true</code> if this object holds references,
         *         <code>false</code> otherwise
         */
        public boolean hasReferences() {
            return !references.isEmpty();
        }

        /**
         * @return the list of references
         */
        public List getReferences() {
            return Collections.unmodifiableList(references);
        }

        //--------------------------------------------------------< private >---
        /**
         * @param refId
         */
        private void addReference(PropertyId refId) {
            references.add(refId);
        }

        /**
         * @param refId
         * @return <code>true</code> if the reference was removed;
         *        <code>false</code> otherwise.
         */
        private boolean removeReference(PropertyId refId) {
            return references.remove(refId);
        }
    }

}
