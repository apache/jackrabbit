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
package org.apache.jackrabbit.spi.commons;

import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.GuestCredentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.MergeException;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.PropertyId;
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
import org.apache.jackrabbit.spi.commons.identifier.IdFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.NodeTypeStorage;
import org.apache.jackrabbit.spi.commons.nodetype.NodeTypeStorageImpl;
import org.apache.jackrabbit.spi.commons.nodetype.QDefinitionBuilderFactory;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;

/**
 * <code>AbstractRepositoryService</code> provides an abstract base class for
 * repository service implementations. This class provides default
 * implementations for the following methods:
 * <ul>
 * <li>{@link #getIdFactory()}</li>
 * <li>{@link #getNameFactory()}</li>
 * <li>{@link #getPathFactory()}</li>
 * <li>{@link #getQValueFactory()}</li>
 * </ul>
 */
public abstract class AbstractRepositoryService implements RepositoryService {

    /**
     * The repository descriptors.
     */
    protected final Map<String, QValue[]> descriptors = new HashMap<String, QValue[]>();

    /**
     * The fixed set of namespaces known to the repository service.
     */
    protected final NamespaceMapping namespaces = new NamespaceMapping();

    /**
     * The fixed set of node type definitions known to the repository service.
     */
    protected final NodeTypeStorage nodeTypeDefs = new NodeTypeStorageImpl();

    /**
     * The node definition of the root node.
     */
    protected QNodeDefinition rootNodeDefinition;

    /**
     * @return {@link IdFactoryImpl#getInstance()}.
     * @throws RepositoryException if an error occurs.
     */
    public IdFactory getIdFactory() throws RepositoryException {
        return IdFactoryImpl.getInstance();
    }

    /**
     * @return {@link NameFactoryImpl#getInstance()}.
     * @throws RepositoryException if an error occurs.
     */
    public NameFactory getNameFactory() throws RepositoryException {
        return NameFactoryImpl.getInstance();
    }

    /**
     * @return {@link PathFactoryImpl#getInstance()}.
     * @throws RepositoryException if an error occurs.
     */
    public PathFactory getPathFactory() throws RepositoryException {
        return PathFactoryImpl.getInstance();
    }

    /**
     * @return {@link QValueFactoryImpl#getInstance()}.
     * @throws RepositoryException if an error occurs.
     */
    public QValueFactory getQValueFactory() throws RepositoryException {
        return QValueFactoryImpl.getInstance();
    }

    protected AbstractRepositoryService() throws RepositoryException {
        QValueFactory qvf = QValueFactoryImpl.getInstance();
        QValue[] vFalse = new QValue[] {qvf.create(false)};

        descriptors.put(Repository.WRITE_SUPPORTED, vFalse);
        descriptors.put(Repository.IDENTIFIER_STABILITY,
                new QValue[] {qvf.create(Repository.IDENTIFIER_STABILITY_SAVE_DURATION, PropertyType.STRING)});
        descriptors.put(Repository.OPTION_XML_IMPORT_SUPPORTED, vFalse);
        descriptors.put(Repository.OPTION_XML_EXPORT_SUPPORTED, vFalse);
        descriptors.put(Repository.OPTION_UNFILED_CONTENT_SUPPORTED, vFalse);
        descriptors.put(Repository.OPTION_VERSIONING_SUPPORTED, vFalse);
        descriptors.put(Repository.OPTION_SIMPLE_VERSIONING_SUPPORTED, vFalse);
        descriptors.put(Repository.OPTION_ACCESS_CONTROL_SUPPORTED, vFalse);
        descriptors.put(Repository.OPTION_LOCKING_SUPPORTED, vFalse);
        descriptors.put(Repository.OPTION_OBSERVATION_SUPPORTED, vFalse);
        descriptors.put(Repository.OPTION_JOURNALED_OBSERVATION_SUPPORTED, vFalse);
        descriptors.put(Repository.OPTION_RETENTION_SUPPORTED, vFalse);
        descriptors.put(Repository.OPTION_LIFECYCLE_SUPPORTED, vFalse);
        descriptors.put(Repository.OPTION_TRANSACTIONS_SUPPORTED, vFalse);
        descriptors.put(Repository.OPTION_WORKSPACE_MANAGEMENT_SUPPORTED, vFalse);
        descriptors.put(Repository.OPTION_UPDATE_PRIMARY_NODE_TYPE_SUPPORTED, vFalse);
        descriptors.put(Repository.OPTION_UPDATE_MIXIN_NODE_TYPES_SUPPORTED, vFalse);
        descriptors.put(Repository.OPTION_SHAREABLE_NODES_SUPPORTED, vFalse);
        descriptors.put(Repository.OPTION_NODE_TYPE_MANAGEMENT_SUPPORTED, vFalse);

        descriptors.put(Repository.QUERY_LANGUAGES, new QValue[0]);
        descriptors.put(Repository.QUERY_STORED_QUERIES_SUPPORTED, vFalse);
        descriptors.put(Repository.QUERY_FULL_TEXT_SEARCH_SUPPORTED, vFalse);
        descriptors.put(Repository.QUERY_JOINS,
                new QValue[] {qvf.create(Repository.QUERY_JOINS_NONE, PropertyType.STRING)});

        descriptors.putAll(descriptors);
    }

    public AbstractRepositoryService(Map<String, QValue[]> descriptors,
                                     Map<String, String> namespaces,
                                     QNodeTypeDefinition[] nodeTypeDefs)
            throws RepositoryException {

        this();
        this.descriptors.putAll(descriptors);

        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            this.namespaces.setMapping(entry.getKey(), entry.getValue());
        }

        this.nodeTypeDefs.registerNodeTypes(nodeTypeDefs, true);
    }

    public AbstractRepositoryService(Map<String, QValue[]> descriptors,
                                     Map<String, String> namespaces,
                                     Reader cnd)
            throws RepositoryException {

        this();
        this.descriptors.putAll(descriptors);

        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            this.namespaces.setMapping(entry.getKey(), entry.getValue());
        }

        CompactNodeTypeDefReader<QNodeTypeDefinition, NamespaceMapping> reader;
        try {
            reader = new CompactNodeTypeDefReader<QNodeTypeDefinition, NamespaceMapping>(cnd, "",
                    this.namespaces, new QDefinitionBuilderFactory());

            List<QNodeTypeDefinition> ntds = reader.getNodeTypeDefinitions();
            nodeTypeDefs.registerNodeTypes(ntds.toArray(new QNodeTypeDefinition[ntds.size()]), true);
        }
        catch (ParseException e) {
            throw new RepositoryException("Error reading node type definitions", e);
        }
    }

    //---------------------------< subclass responsibility >--------------------

    /**
     * Create the root node definition.
     * @param sessionInfo  the session info.
     * @return the root node definition for a workspace.
     * @throws RepositoryException if an error occurs.
     */
    protected abstract QNodeDefinition createRootNodeDefinition(SessionInfo sessionInfo)
            throws RepositoryException;

    //---------------------< may be overwritten by subclasses>------------------

    /**
     * Checks if the given <code>credentials</code> are valid. This default
     * implementation is empty thus allowing all credentials.
     *
     * @param credentials the credentials to check.
     * @param workspaceName the workspace to access.
     * @throws LoginException if the credentials are invalid.
     */
    protected void checkCredentials(Credentials credentials, String workspaceName) throws LoginException {
        // empty
    }

    /**
     * Checks if the given workspace is available. The default implementation is empty
     * thus admitting every workspace name.
     * @param workspaceName  Name of the workspace to check
     * @throws NoSuchWorkspaceException   If <code>workspaceName</code> is not available.
     */
    protected void checkWorkspace(String workspaceName) throws NoSuchWorkspaceException {
        // empty
    }

    /**
     * Creates a session info instance for the given <code>credentials</code> and
     * <code>workspaceName</code>. This default implementation creates a
     * {@link SessionInfoImpl} instance and sets the <code>userId</code> and
     * workspaceName. The user <code>userId</code> is <code>null</code> or the
     * <code>userId</code> from <code>credentials</code> if it is of type
     * {@link SimpleCredentials}.
     *
     * @param credentials the credentials.
     * @param workspaceName the name of the workspace to access or <code>null</code>
     *        for the default workspace.
     * @return a session info instance for the given <code>credentials</code> and
     *         <code>workspaceName</code>.
     * @throws RepositoryException
     */
    protected SessionInfo createSessionInfo(Credentials credentials, String workspaceName)
            throws RepositoryException {

        String userId = null;
        if (credentials instanceof SimpleCredentials) {
            userId = ((SimpleCredentials) credentials).getUserID();
        }
        else if (credentials instanceof GuestCredentials) {
            userId = "anonymous";
        }

        SessionInfoImpl s = new SessionInfoImpl();
        s.setUserID(userId);
        s.setWorkspacename(workspaceName);
        return s;
    }

    /**
     * Creates a session info instance for the given <code>sessionInfo</code> and
     * <code>workspaceName</code>. This default implementation creates a
     * {@link SessionInfoImpl} instance and sets the <code>userId</code> and
     * workspaceName. The user <code>userId</code> is set to the return value of
     * {@link SessionInfo#getUserID()}.
     *
     * @param sessionInfo the sessionInfo.
     * @param workspaceName the name of the workspace to access.
     * @return a session info instance for the given <code>credentials</code> and
     *         <code>workspaceName</code>.
     * @throws RepositoryException
     */
    protected SessionInfo createSessionInfo(SessionInfo sessionInfo, String workspaceName)
            throws RepositoryException {

        String userId = sessionInfo.getUserID();

        SessionInfoImpl s = new SessionInfoImpl();
        s.setUserID(userId);
        s.setWorkspacename(workspaceName);
        return s;
    }

    /**
     * Checks the type of the <code>sessionInfo</code> instance. This default
     * implementation checks if <code>sessionInfo</code> is of type
     * {@link SessionInfoImpl}, otherwise throws a {@link RepositoryException}.
     *
     * @param sessionInfo the session info to check.
     * @throws RepositoryException if the given <code>sessionInfo</code> is not
     *                             of the required type for this repository
     *                             service implementation.
     */
    protected void checkSessionInfo(SessionInfo sessionInfo)
            throws RepositoryException {
        if (sessionInfo instanceof SessionInfoImpl) {
            return;
        }
        throw new RepositoryException("SessionInfo not of type "
                + SessionInfoImpl.class.getName());
    }

    //--------------------------< descriptors >---------------------------------

    /**
     * This default implementation returns the descriptors that were passed
     * to the constructor of this repository service.
     */
    public Map<String, QValue[]> getRepositoryDescriptors() throws RepositoryException {
        return descriptors;
    }

    //----------------------------< login >-------------------------------------

    /**
     * This default implementation does:
     * <ul>
     * <li>calls {@link #checkCredentials(Credentials, String)}</li>
     * <li>calls {@link #checkWorkspace(String)}</li>
     * <li>calls {@link #createSessionInfo(Credentials, String)}</li>
     * </ul>
     * @param credentials the credentials for the login.
     * @param workspaceName the name of the workspace to log in.
     * @return the session info.
     * @throws LoginException if the credentials are invalid.
     * @throws NoSuchWorkspaceException if <code>workspaceName</code> is unknown.
     * @throws RepositoryException if another error occurs.
     */
    public SessionInfo obtain(Credentials credentials, String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        checkCredentials(credentials, workspaceName);
        checkWorkspace(workspaceName);
        return createSessionInfo(credentials, workspaceName);
    }

    /**
     * This default implementation returns the session info returned by the call
     * to {@link #createSessionInfo(SessionInfo, String)}.
     */
    public SessionInfo obtain(SessionInfo sessionInfo, String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return createSessionInfo(sessionInfo, workspaceName);
    }


    /**
     * This default implementation returns the session info returned by the call
     * to {@link #obtain(Credentials, String)} with the workspaceName taken from
     * the passed <code>sessionInfo</code>.
     */
    public SessionInfo impersonate(SessionInfo sessionInfo, Credentials credentials)
            throws LoginException, RepositoryException {
        return obtain(credentials, sessionInfo.getWorkspaceName());
    }

    /**
     * This default implementation does nothing.
     */
    public void dispose(SessionInfo sessionInfo) throws RepositoryException {
        // do nothing
    }

    //-----------------------------< node types >-------------------------------

    /**
     * This default implementation first calls {@link #checkSessionInfo(SessionInfo)}
     * with the <code>sessionInfo</code>,
     */
    public Iterator<QNodeTypeDefinition> getQNodeTypeDefinitions(SessionInfo sessionInfo) throws
            RepositoryException {

        checkSessionInfo(sessionInfo);
        return nodeTypeDefs.getAllDefinitions();
    }

    /**
     * This default implementation first calls {@link #checkSessionInfo(SessionInfo)}
     * with the <code>sessionInfo</code>, then gathers the {@link QNodeTypeDefinition}s
     * with the given <code>nodetypeNames</code>. If one of the nodetypeNames
     * is not a valid node type definition then a {@link RepositoryException}
     * is thrown.
     */
    public Iterator<QNodeTypeDefinition> getQNodeTypeDefinitions(SessionInfo sessionInfo, Name[] nodetypeNames)
            throws RepositoryException {

        checkSessionInfo(sessionInfo);
        return nodeTypeDefs.getDefinitions(nodetypeNames);
    }

    /**
     * This default implementation first calls {@link #checkSessionInfo(SessionInfo)}
     * with the <code>sessionInfo</code>, then lazily initializes {@link #rootNodeDefinition}
     * if <code>nodeId</code> denotes the root node; otherwise throws a
     * {@link UnsupportedRepositoryOperationException}.
     */
    public QNodeDefinition getNodeDefinition(SessionInfo sessionInfo,
                                             NodeId nodeId)
            throws RepositoryException {
        checkSessionInfo(sessionInfo);
        if (nodeId.getUniqueID() == null && nodeId.getPath().denotesRoot()) {
            synchronized (this) {
                if (rootNodeDefinition == null) {
                    rootNodeDefinition = createRootNodeDefinition(sessionInfo);
                }
                return rootNodeDefinition;
            }
        }
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public QPropertyDefinition getPropertyDefinition(SessionInfo sessionInfo,
                                                     PropertyId propertyId)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void registerNodeTypes(SessionInfo sessionInfo, QNodeTypeDefinition[] nodeTypeDefinitions, boolean allowUpdate) throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void unregisterNodeTypes(SessionInfo sessionInfo, Name[] nodeTypeNames) throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    //-----------------------------< namespaces >-------------------------------

    /**
     * This default implementation first calls {@link #checkSessionInfo(SessionInfo)}
     * with the <code>sessionInfo</code>, then returns the prefix to namespace
     * URL mapping that was provided in the constructor of this repository
     * service.
     */
    public Map<String, String> getRegisteredNamespaces(SessionInfo sessionInfo) throws
            RepositoryException {
        checkSessionInfo(sessionInfo);
        return namespaces.getPrefixToURIMapping();
    }

    /**
     * This default implementation first calls {@link #checkSessionInfo(SessionInfo)}
     * with the <code>sessionInfo</code>, then returns the namespace URI for the
     * given <code>prefix</code>.
     */
    public String getNamespaceURI(SessionInfo sessionInfo, String prefix)
            throws NamespaceException, RepositoryException {
        checkSessionInfo(sessionInfo);
        return namespaces.getURI(prefix);
    }

    /**
     * This default implementation first calls {@link #checkSessionInfo(SessionInfo)}
     * with the <code>sessionInfo</code>, then return the namespace prefix for
     * the given <code>uri</code>.
     */
    public String getNamespacePrefix(SessionInfo sessionInfo, String uri)
            throws NamespaceException, RepositoryException {
        checkSessionInfo(sessionInfo);
        return namespaces.getPrefix(uri);
    }

    //-----------------------------< write methods >----------------------------

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public Batch createBatch(SessionInfo sessionInfo, ItemId itemId)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void submit(Batch batch) throws PathNotFoundException, ItemNotFoundException, NoSuchNodeTypeException, ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    @Override
    public Tree createTree(SessionInfo sessionInfo, Batch batch, Name nodeName, Name primaryTypeName, String uniqueId) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void importXml(SessionInfo sessionInfo,
                          NodeId parentId,
                          InputStream xmlStream,
                          int uuidBehaviour) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void move(SessionInfo sessionInfo,
                     NodeId srcNodeId,
                     NodeId destParentNodeId,
                     Name destName) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void copy(SessionInfo sessionInfo,
                     String srcWorkspaceName,
                     NodeId srcNodeId,
                     NodeId destParentNodeId,
                     Name destName) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void update(SessionInfo sessionInfo,
                       NodeId nodeId,
                       String srcWorkspaceName)
            throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void clone(SessionInfo sessionInfo,
                      String srcWorkspaceName,
                      NodeId srcNodeId,
                      NodeId destParentNodeId,
                      Name destName,
                      boolean removeExisting) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public LockInfo lock(SessionInfo sessionInfo,
                         NodeId nodeId,
                         boolean deep,
                         boolean sessionScoped)
            throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public LockInfo lock(SessionInfo sessionInfo, NodeId nodeId, boolean deep,
                         boolean sessionScoped, long timeoutHint, String ownerHint)
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @return <code>null</code>.
     */
    public LockInfo getLockInfo(SessionInfo sessionInfo, NodeId nodeId)
            throws AccessDeniedException, RepositoryException {
        return null;
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void refreshLock(SessionInfo sessionInfo, NodeId nodeId)
            throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void unlock(SessionInfo sessionInfo, NodeId nodeId)
            throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public NodeId checkin(SessionInfo sessionInfo, NodeId nodeId)
            throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void checkout(SessionInfo sessionInfo, NodeId nodeId)
            throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void checkout(SessionInfo sessionInfo, NodeId nodeId, NodeId activityId)
            throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public NodeId checkpoint(SessionInfo sessionInfo, NodeId nodeId)
            throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public NodeId checkpoint(SessionInfo sessionInfo, NodeId nodeId, NodeId activityId)
            throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void removeVersion(SessionInfo sessionInfo,
                              NodeId versionHistoryId,
                              NodeId versionId)
            throws ReferentialIntegrityException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void restore(SessionInfo sessionInfo,
                        NodeId nodeId,
                        NodeId versionId,
                        boolean removeExisting) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void restore(SessionInfo sessionInfo,
                        NodeId[] versionIds,
                        boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException
     *          always.
     */
    public Iterator<NodeId> merge(SessionInfo sessionInfo,
                                  NodeId nodeId,
                                  String srcWorkspaceName,
                                  boolean bestEffort) throws
            NoSuchWorkspaceException, AccessDeniedException, MergeException,
            LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException
     *          always.
     */
    public Iterator<NodeId> merge(SessionInfo sessionInfo,
                                  NodeId nodeId,
                                  String srcWorkspaceName,
                                  boolean bestEffort,
                                  boolean isShallow) throws
            NoSuchWorkspaceException, AccessDeniedException, MergeException,
            LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void resolveMergeConflict(SessionInfo sessionInfo,
                                     NodeId nodeId,
                                     NodeId[] mergeFailedIds,
                                     NodeId[] predecessorIds)
            throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void addVersionLabel(SessionInfo sessionInfo,
                                NodeId versionHistoryId,
                                NodeId versionId,
                                Name label,
                                boolean moveLabel) throws VersionException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void removeVersionLabel(SessionInfo sessionInfo,
                                   NodeId versionHistoryId,
                                   NodeId versionId,
                                   Name label) throws VersionException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public NodeId createActivity(SessionInfo sessionInfo, String title) throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void removeActivity(SessionInfo sessionInfo, NodeId activityId) throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();

    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public Iterator<NodeId> mergeActivity(SessionInfo sessionInfo, NodeId activityId) throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();

    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public NodeId createConfiguration(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    //-----------------------------< observation >------------------------------

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public EventFilter createEventFilter(SessionInfo sessionInfo,
                                         int eventTypes,
                                         Path absPath,
                                         boolean isDeep,
                                         String[] uuid,
                                         Name[] nodeTypeName,
                                         boolean noLocal)
            throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public Subscription createSubscription(SessionInfo sessionInfo,
                                           EventFilter[] filters)
            throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void updateEventFilters(Subscription subscription,
                                   EventFilter[] filters)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public EventBundle[] getEvents(Subscription subscription, long timeout)
            throws RepositoryException, InterruptedException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public EventBundle getEvents(SessionInfo sessionInfo, EventFilter filter,
                                   long after) throws
            RepositoryException, UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void dispose(Subscription subscription) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    //-------------------------------------------------< namespace registry >---

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void registerNamespace(SessionInfo sessionInfo,
                                  String prefix,
                                  String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void unregisterNamespace(SessionInfo sessionInfo, String uri)
            throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    //-----------------------------------------------< Workspace Management >---
    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void createWorkspace(SessionInfo sessionInfo, String name, String srcWorkspaceName) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @throws UnsupportedRepositoryOperationException always.
     */
    public void deleteWorkspace(SessionInfo sessionInfo, String name) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    //-------------------------------< query >----------------------------------

    public String[] getSupportedQueryLanguages(SessionInfo sessionInfo) throws RepositoryException {
        checkSessionInfo(sessionInfo);
        return new String[0];
    }

    public String[] checkQueryStatement(SessionInfo sessionInfo, String statement,
                                    String language, Map<String, String> namespaces) throws
            InvalidQueryException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public QueryInfo executeQuery(SessionInfo sessionInfo, String statement,
                                  String language, Map<String, String> namespaces, long limit,
                                  long offset, Map<String, QValue> values) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

}
