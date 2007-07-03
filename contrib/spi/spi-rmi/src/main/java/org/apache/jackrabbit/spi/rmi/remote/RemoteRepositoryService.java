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
package org.apache.jackrabbit.spi.rmi.remote;

import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.io.InputStream;

/**
 * <code>RemoteRepositoryService</code> is the remote version of the interface
 * {@link org.apache.jackrabbit.spi.RepositoryService}.
 */
public interface RemoteRepositoryService extends Remote {

    /**
     * @return key-value pairs for repository descriptor keys and values.
     * @throws RemoteException on RMI errors.
     * @see org.apache.jackrabbit.spi.RepositoryService#getRepositoryDescriptors()
     */
    public Map getRepositoryDescriptors()
            throws RepositoryException, RemoteException;

    /**
     * Returns a <code>RemoteSessionInfo</code> that will be used by other
     * methods on the <code>RepositoryService</code>. An implementation may
     * choose to authenticate the user using the supplied
     * <code>credentials</code>.
     *
     * @param credentials the credentials of the user.
     * @return a <code>RemoteSessionInfo</code> if authentication was
     *         successful.
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#obtain(javax.jcr.Credentials, String)
     */
    public RemoteSessionInfo obtain(Credentials credentials,
                                    String workspaceName)
            throws RepositoryException, RemoteException;

    /**
     * Returns a new <code>RemoteSessionInfo</code> for the given workspace name
     * that will be used by other methods on the <code>RepositoryService</code>.
     *
     * @param sessionInfo for another workspace
     * @return a <code>RemoteSessionInfo</code> if authentication was
     *         successful.
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#obtain(org.apache.jackrabbit.spi.SessionInfo, String)
     */
    public RemoteSessionInfo obtain(RemoteSessionInfo sessionInfo,
                                    String workspaceName)
            throws RepositoryException, RemoteException;

    /**
     * Returns a new <code>RemoteSessionInfo</code>.
     *
     * @param sessionInfo
     * @param credentials
     * @return a <code>RemoteSessionInfo</code> if impersonate was
     *         successful.
     * @throws RemoteException if an error occurs.
     * @see RepositoryService#impersonate(SessionInfo, Credentials)
     */
    public RemoteSessionInfo impersonate(RemoteSessionInfo sessionInfo,
                                         Credentials credentials)
            throws RepositoryException, RemoteException;

    /**
     * Indicates to the <code>RepositoryService</code>, that the given
     * RemoteSessionInfo will not be used any more.
     *
     * @param sessionInfo
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#dispose(org.apache.jackrabbit.spi.SessionInfo)
     */
    public void dispose(RemoteSessionInfo sessionInfo)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @return an array of workspace ids
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#getWorkspaceNames(org.apache.jackrabbit.spi.SessionInfo)
     */
    public String[] getWorkspaceNames(RemoteSessionInfo sessionInfo)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param itemId
     * @param actions
     * @return true if the session with the given <code>SessionInfo</code> has
     *         the specified rights for the given item.
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#isGranted(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.ItemId, String[])
     */
    public boolean isGranted(RemoteSessionInfo sessionInfo,
                             ItemId itemId,
                             String[] actions)
            throws RepositoryException, RemoteException;

    /**
     * The <code>NodeId</code> of the root node may basically have two
     * characteristics. If the root node can be identified with a unique ID the
     * returned <code>NodeId</code> simply has a uniqueID part and the path
     * part is <code>null</code>. If the root node cannot be identified with a
     * unique ID the uniqueID part is <code>null</code> and the path part will be set
     * to "/".
     *
     * @param sessionInfo
     * @return
     * @throws RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#getRootId(org.apache.jackrabbit.spi.SessionInfo)
     */
    public NodeId getRootId(RemoteSessionInfo sessionInfo)
            throws RepositoryException, RemoteException;

    /**
     *
     * @param sessionInfo
     * @param nodeId
     * @return
     * @throws RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#getNodeDefinition(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId)
     */
    public QNodeDefinition getNodeDefinition(RemoteSessionInfo sessionInfo,
                                             NodeId nodeId)
            throws RepositoryException, RemoteException;

    /**
     *
     * @param sessionInfo
     * @param propertyId
     * @return
     * @throws RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#getPropertyDefinition(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.PropertyId)
     */
    public QPropertyDefinition getPropertyDefinition(RemoteSessionInfo sessionInfo,
                                                     PropertyId propertyId)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param itemId
     * @return true if the item with the given id exists
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#exists(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.ItemId)
     */
    public boolean exists(RemoteSessionInfo sessionInfo, ItemId itemId)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param nodeId
     * @return
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#getNodeInfo(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId)
     */
    public NodeInfo getNodeInfo(RemoteSessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param nodeId
     * @return
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#getNodeInfo(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId)
     */
    public RemoteIterator getItemInfos(RemoteSessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException, RemoteException;

    /**
     * Returns a collection of child node entries present on the
     * Node represented by the given parentId.
     *
     * @param sessionInfo
     * @param parentId
     * @return
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#getChildInfos(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId)
     */
    public RemoteIterator getChildInfos(RemoteSessionInfo sessionInfo,
                                  NodeId parentId)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param propertyId
     * @return
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#getPropertyInfo(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.PropertyId)
     */
    public PropertyInfo getPropertyInfo(RemoteSessionInfo sessionInfo,
                                        PropertyId propertyId)
            throws RepositoryException, RemoteException;

    /**
     * Indicates the start of a set of operations that cause modifications
     * on the underlaying persistence layer. All modification called on the
     * Batch must be executed at once or non must be executed.
     *
     * @param itemId
     * @param sessionInfo
     * @return
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#createBatch(org.apache.jackrabbit.spi.ItemId, org.apache.jackrabbit.spi.SessionInfo)
     */
    public RemoteBatch createBatch(ItemId itemId,
                                   RemoteSessionInfo sessionInfo)
            throws RepositoryException, RemoteException;

    /**
     * Submits a batch.
     *
     * @param batch the remote batch
     * @throws RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#submit(org.apache.jackrabbit.spi.Batch)
     */
    public void submit(RemoteBatch batch) throws RepositoryException, RemoteException;

    /**
     *
     * @param sessionInfo
     * @param parentId
     * @param xmlStream
     * @param uuidBehaviour
     * @throws RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#importXml(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, java.io.InputStream, int)
     */
    public void importXml(RemoteSessionInfo sessionInfo,
                          NodeId parentId,
                          InputStream xmlStream,
                          int uuidBehaviour)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param srcNodeId
     * @param destParentNodeId
     * @param destName
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#move(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.name.QName)
     */
    public void move(RemoteSessionInfo sessionInfo,
                     NodeId srcNodeId,
                     NodeId destParentNodeId,
                     QName destName)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param srcWorkspaceName
     * @param srcNodeId
     * @param destParentNodeId
     * @param destName
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#copy(org.apache.jackrabbit.spi.SessionInfo, String, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.name.QName)
     */
    public void copy(RemoteSessionInfo sessionInfo,
                     String srcWorkspaceName,
                     NodeId srcNodeId,
                     NodeId destParentNodeId,
                     QName destName)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param nodeId
     * @param srcWorkspaceName
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#update(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, String)
     */
    public void update(RemoteSessionInfo sessionInfo,
                       NodeId nodeId,
                       String srcWorkspaceName)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param srcWorkspaceName
     * @param srcNodeId
     * @param destParentNodeId
     * @param destName
     * @param removeExisting
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#clone(org.apache.jackrabbit.spi.SessionInfo, String, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.name.QName, boolean)
     */
    public void clone(RemoteSessionInfo sessionInfo,
                      String srcWorkspaceName,
                      NodeId srcNodeId,
                      NodeId destParentNodeId,
                      QName destName,
                      boolean removeExisting)
            throws RepositoryException, RemoteException;

    /**
     * Retrieve available lock information for the given <code>NodeId</code>.
     *
     * @param sessionInfo
     * @param nodeId
     * @return
     * @throws RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#getLockInfo(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId)
     */
    public LockInfo getLockInfo(RemoteSessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param nodeId
     * @param deep
     * @param sessionScoped
     * @return returns the <code>LockInfo</code> associated with this lock.
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#lock(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, boolean, boolean)
     */
    public LockInfo lock(RemoteSessionInfo sessionInfo,
                         NodeId nodeId,
                         boolean deep,
                         boolean sessionScoped)
            throws RepositoryException, RemoteException;

    /**
     * Explicit refresh of an existing lock. Existing locks should be refreshed
     * implicitely with all read and write methods listed here.
     *
     * @param sessionInfo
     * @param nodeId
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#refreshLock(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId)
     */
    public void refreshLock(RemoteSessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException, RemoteException;

    /**
     * Releases the lock on the given node.<p/>
     * Please note, that on {@link javax.jcr.Session#logout() logout} all
     * session-scoped locks must be released by calling unlock.
     *
     * @param sessionInfo
     * @param nodeId
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#unlock(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId)
     */
    public void unlock(RemoteSessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param nodeId
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#checkin(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId)
     */
    public void checkin(RemoteSessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param nodeId
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#checkout(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId)
     */
    public void checkout(RemoteSessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException, RemoteException;

    /**
     *
     * @param sessionInfo
     * @param versionHistoryId
     * @param versionId
     * @throws RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#checkout(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId)
     */
    public void removeVersion(RemoteSessionInfo sessionInfo,
                              NodeId versionHistoryId,
                              NodeId versionId)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param nodeId
     * @param versionId
     * @param removeExisting
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#restore(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.spi.NodeId, boolean)
     */
    public void restore(RemoteSessionInfo sessionInfo,
                        NodeId nodeId,
                        NodeId versionId,
                        boolean removeExisting)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param versionIds
     * @param removeExisting
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#restore(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId[], boolean)
     */
    public void restore(RemoteSessionInfo sessionInfo,
                        NodeId[] versionIds,
                        boolean removeExisting)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param nodeId
     * @param srcWorkspaceName
     * @param bestEffort
     * @return an <code>IdIterator</code> over all nodes that received a merge
     * result of "fail" in the course of this operation.
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#merge(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, String, boolean)
     */
    public RemoteIterator merge(RemoteSessionInfo sessionInfo,
                            NodeId nodeId,
                            String srcWorkspaceName,
                            boolean bestEffort)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param nodeId
     * @param mergeFailedIds
     * @param predecessorIds
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#resolveMergeConflict(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.spi.NodeId[], org.apache.jackrabbit.spi.NodeId[])
     */
    public void resolveMergeConflict(RemoteSessionInfo sessionInfo,
                                     NodeId nodeId,
                                     NodeId[] mergeFailedIds,
                                     NodeId[] predecessorIds)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param versionId
     * @param label
     * @param moveLabel
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#addVersionLabel(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.name.QName, boolean)
     */
    public void addVersionLabel(RemoteSessionInfo sessionInfo,
                                NodeId versionHistoryId,
                                NodeId versionId,
                                QName label,
                                boolean moveLabel)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param versionId
     * @param label
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#removeVersionLabel(org.apache.jackrabbit.spi.SessionInfo, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.name.QName)
     */
    public void removeVersionLabel(RemoteSessionInfo sessionInfo,
                                   NodeId versionHistoryId,
                                   NodeId versionId,
                                   QName label)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @return
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#getSupportedQueryLanguages(org.apache.jackrabbit.spi.SessionInfo)
     */
    public String[] getSupportedQueryLanguages(RemoteSessionInfo sessionInfo)
            throws RepositoryException, RemoteException;

    /**
     * Checks if the query <code>statement</code> is valid according to the
     * specified query <code>language</code>.
     *
     * @param sessionInfo the session info.
     * @param statement   the query statement to check.
     * @param language    the query language.
     * @param namespaces  the locally re-mapped namespace which may be used in
     *                    the query <code>statement</code>.
     * @throws RepositoryException   if an error occurs while checking the
     *                               statement.
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#checkQueryStatement(org.apache.jackrabbit.spi.SessionInfo, String, String, java.util.Map)
     */
    public void checkQueryStatement(RemoteSessionInfo sessionInfo,
                                    String statement,
                                    String language,
                                    Map namespaces)
            throws RepositoryException, RemoteException;

    /**
     * @param sessionInfo
     * @param statement
     * @param language
     * @param namespaces  the locally re-mapped namespace which may be used in
     *                    the query <code>statement</code>.
     * @return
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#executeQuery(org.apache.jackrabbit.spi.SessionInfo, String, String, java.util.Map)
     */
    public RemoteQueryInfo executeQuery(RemoteSessionInfo sessionInfo,
                                        String statement,
                                        String language,
                                        Map namespaces)
            throws RepositoryException, RemoteException;

    /**
     * Creates an event filter.
     *
     * @param sessionInfo the session info which requests an event filter.
     * @param eventTypes A combination of one or more event type constants
     * encoded as a bitmask.
     * @param absPath An absolute path.
     * @param isDeep A <code>boolean</code>.
     * @param uuid Array of jcr:uuid properties.
     * @param nodeTypeName Array of node type names.
     * @param noLocal A <code>boolean</code>.
     * @return the event filter instance with the given parameters.
     * @throws RepositoryException if an error occurs while creating the
     * EventFilter.
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#createEventFilter(org.apache.jackrabbit.spi.SessionInfo, int, org.apache.jackrabbit.name.Path, boolean, String[], org.apache.jackrabbit.name.QName[], boolean)
     */
    public EventFilter createEventFilter(RemoteSessionInfo sessionInfo, int eventTypes,
                                         Path absPath, boolean isDeep,
                                         String[] uuid, QName[] nodeTypeName,
                                         boolean noLocal)
            throws RepositoryException, RemoteException;

    /**
     * Retrieves the events that occurred since the last call to this
     * method.
     *
     * @param sessionInfo the session info.
     * @param timeout     a timeout in milliseconds to wait at most for an
     *                    event bundle.
     * @param filters     the filters that are applied to the events as
     *                    they occurred on the repository.
     * @return an array of <code>EventBundle</code>s representing the external
     *         events that occurred.
     * @throws RepositoryException  if an error occurs while retrieving the
     *                              event bundles or the currently set bundle
     *                              identifier in <code>sessionInfo</code>
     *                              references an unknown or outdated event
     *                              bundle.
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#getEvents(org.apache.jackrabbit.spi.SessionInfo, long, org.apache.jackrabbit.spi.EventFilter[])
     */
    public EventBundle[] getEvents(RemoteSessionInfo sessionInfo, long timeout,
                                   EventFilter[] filters)
            throws RepositoryException, InterruptedException, RemoteException;

    /**
     * Retrieve all registered namespaces. The namespace to prefix mapping is
     * done using the prefix as key and the namespace as value in the Map.
     *
     * @param sessionInfo
     * @return
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#getRegisteredNamespaces(org.apache.jackrabbit.spi.SessionInfo)
     */
    public Map getRegisteredNamespaces(RemoteSessionInfo sessionInfo)
            throws RepositoryException, RemoteException;

    /**
     * Returns the namespace URI for the given namespace <code>prefix</code>.
     *
     * @param sessionInfo the session info.
     * @param prefix a namespace prefix to resolve.
     * @return the namespace URI for the given namespace <code>prefix</code>.
     * @throws RepositoryException if another error occurs.
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#getNamespaceURI(org.apache.jackrabbit.spi.SessionInfo, String)
     */
    public String getNamespaceURI(RemoteSessionInfo sessionInfo, String prefix)
            throws RepositoryException, RemoteException;

    /**
     * Returns the namespace prefix for the given namespace <code>uri</code>.
     *
     * @param sessionInfo the session info.
     * @param uri the namespace URI.
     * @return the namespace prefix.
     * @throws RepositoryException if another error occurs.
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#getNamespacePrefix(org.apache.jackrabbit.spi.SessionInfo, String)
     */
    public String getNamespacePrefix(RemoteSessionInfo sessionInfo, String uri)
            throws RepositoryException, RemoteException;

    /**
     * Register a new namespace with the given prefix and uri
     *
     * @param sessionInfo
     * @param prefix
     * @param uri
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#registerNamespace(org.apache.jackrabbit.spi.SessionInfo, String, String)
     */
    public void registerNamespace(RemoteSessionInfo sessionInfo,
                                  String prefix,
                                  String uri)
            throws RepositoryException, RemoteException;

    /**
     * Unregister the namesspace indentified by the given prefix
     *
     * @param sessionInfo
     * @param uri
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#unregisterNamespace(org.apache.jackrabbit.spi.SessionInfo, String)
     */
    public void unregisterNamespace(RemoteSessionInfo sessionInfo, String uri)
            throws RepositoryException, RemoteException;

    /**
     * Retrieve the <code>QNodeTypeDefinition</code>s of all registered nodetypes.
     *
     * @param sessionInfo
     * @return
     * @throws javax.jcr.RepositoryException
     * @throws RemoteException if an error occurs.
     * @see org.apache.jackrabbit.spi.RepositoryService#getNodeTypeDefinitions(org.apache.jackrabbit.spi.SessionInfo)
     */
    public QNodeTypeDefinition[] getNodeTypeDefinitions(RemoteSessionInfo sessionInfo)
            throws RepositoryException, RemoteException;
}
