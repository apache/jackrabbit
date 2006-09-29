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
package org.apache.jackrabbit.spi;

import org.apache.jackrabbit.name.QName;

import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.jcr.version.Version;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.RepositoryException;
import javax.jcr.Credentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ItemExistsException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.MergeException;
import javax.jcr.NamespaceException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ValueFormatException;
import javax.jcr.Node;
import javax.jcr.LoginException;
import java.util.Properties;
import java.io.InputStream;

/**
 * <code>RepositoryService</code>...
 */
public interface RepositoryService {

    /**
     * Return the <code>IdFactory</code>
     * 
     * @return
     */
    public IdFactory getIdFactory();

    //--------------------------------------------------------------------------
    /**
     * @return key-value pairs for repository descriptor keys and values.
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Repository#getDescriptorKeys()
     * @see javax.jcr.Repository#getDescriptor(String)
     */
    public Properties getRepositoryDescriptors() throws RepositoryException;

    //------------------------------------------------------< Initial login >---

    /**
     * Authenticates the user using the supplied <code>credentials</code>. If
     * <code>credentials</code> is <code>null</code> an implementation will use
     * the current security context to obtain the {@link
     * javax.security.auth.Subject}. If <code>credentials</code> is
     * <code>null</code> and there is no <code>Subject</code> present in the
     * current security context a <code>RepositoryException</code> is thrown.
     *
     * @param credentials the credentials of the user.
     * @return a <code>SessionInfo</code> if authentication was successful.
     * @throws LoginException           if authentication of the user fails.
     * @throws NoSuchWorkspaceException if the specified <code>workspaceName</code>
     *                                  is not recognized.
     * @throws RepositoryException      if an error occurs.
     */
    public SessionInfo login(Credentials credentials, String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException;

    //--------------------------------------------------------------------------
    /**
     * @param sessionInfo
     * @return an array of workspace ids
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Workspace#getAccessibleWorkspaceNames()
     * @see javax.jcr.Workspace#getName()
     */
    public String[] getWorkspaceNames(SessionInfo sessionInfo) throws RepositoryException;

    // todo: createWorkspace required????

    //-----------------------------------------------------< Access Control >---
    /**
     * @param sessionInfo
     * @param itemId
     * @param actions
     * @return true if the session with the given <code>SessionInfo</code> has
     * the specified rights for the given item.
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Session#checkPermission(String, String)
     */
    public boolean isGranted(SessionInfo sessionInfo, ItemId itemId, String[] actions) throws RepositoryException;

    //------------------------------------------------------< Reading items >---

    /**
     * The <code>NodeId</code> of the root node may basically have two
     * characteristics. If the root node can be identified with a UUID the
     * returned <code>NodeId</code> simply has a UUID part and the relative path
     * part is <code>null</code>. If the root node cannot be identified with a
     * UUID the UUID part is <code>null</code> and the relative path will be set
     * to '.' (current element).
     *
     * @param sessionInfo
     * @return
     * @throws RepositoryException
     */
    public NodeId getRootId(SessionInfo sessionInfo) throws RepositoryException;

    /**
     *
     * @param sessionInfo
     * @param nodeId
     * @return
     * @throws RepositoryException
     */
    public QNodeDefinition getNodeDefinition(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException;

    /**
     *
     * @param sessionInfo
     * @param propertyId
     * @return
     * @throws RepositoryException
     */
    public QPropertyDefinition getPropertyDefinition(SessionInfo sessionInfo, PropertyId propertyId) throws RepositoryException;

    /**
     * @param sessionInfo
     * @param itemId
     * @return true if the item with the given id exists
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Session#itemExists(String)
     */
    public boolean exists(SessionInfo sessionInfo, ItemId itemId) throws RepositoryException;

    /**
     * @param sessionInfo
     * @param nodeId
     * @return
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Session#getItem(String)
     * @see javax.jcr.Node#getNode(String)
     * @see javax.jcr.version.VersionHistory#getAllVersions()
     * @see javax.jcr.version.VersionHistory#getVersion(String)
     * @see javax.jcr.version.VersionHistory#getVersionByLabel(String)
     * @see javax.jcr.version.VersionHistory#getRootVersion()
     * @see javax.jcr.Node#getBaseVersion()
     * @see javax.jcr.Node#getVersionHistory()
     * @see javax.jcr.version.Version#getContainingHistory()
     */
    public NodeInfo getNodeInfo(SessionInfo sessionInfo, NodeId nodeId) throws PathNotFoundException, RepositoryException;

    /**
     * @param sessionInfo
     * @param propertyId
     * @return
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Session#getItem(String)
     * @see javax.jcr.Node#getProperty(String)
     */
    public PropertyInfo getPropertyInfo(SessionInfo sessionInfo, PropertyId propertyId) throws PathNotFoundException, RepositoryException;

    //-----------------------------------------------< general modification >---
    /**
     * Indicates the start of a set of operations that cause modifications
     * on the underlaying persistence layer. All modification called on the
     * Batch must be executed at once or non must be executed.
     *
     * @param itemId
     * @param sessionInfo
     * @return
     */
    public Batch createBatch(ItemId itemId, SessionInfo sessionInfo) throws RepositoryException;

    /**
     * Completes the this Batch or discard all the previous modifications.
     *
     * @param batch
     * @return EventIterator
     * @throws PathNotFoundException
     * @throws ItemNotFoundException
     * @throws NoSuchNodeTypeException
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     */
    public EventIterator submit(Batch batch) throws PathNotFoundException, ItemNotFoundException, NoSuchNodeTypeException, ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

    //-------------------------------------------------------------< Import >---
    /**
     *
     * @param sessionInfo
     * @param parentId
     * @param xmlStream
     * @param uuidBehaviour
     * @return
     * @throws ItemExistsException
     * @throws PathNotFoundException
     * @throws VersionException
     * @throws ConstraintViolationException
     * @throws LockException
     * @throws AccessDeniedException
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     * @see javax.jcr.Workspace#importXML(String, java.io.InputStream, int)
     */
    public EventIterator importXml(SessionInfo sessionInfo, NodeId parentId, InputStream xmlStream, int uuidBehaviour) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

    //---------------------------------------------------------< Copy, Move >---
    /**
     * @param sessionInfo
     * @param srcNodeId
     * @param destParentNodeId
     * @param destName
     * @throws javax.jcr.ItemExistsException
     * @throws javax.jcr.PathNotFoundException
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.nodetype.ConstraintViolationException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Workspace#move(String, String)
     */
    public EventIterator move(SessionInfo sessionInfo, NodeId srcNodeId, NodeId destParentNodeId, QName destName) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * @param sessionInfo
     * @param srcWorkspaceName
     * @param srcNodeId
     * @param destParentNodeId
     * @param destName
     * @throws javax.jcr.NoSuchWorkspaceException
     * @throws javax.jcr.nodetype.ConstraintViolationException
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.PathNotFoundException
     * @throws javax.jcr.ItemExistsException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Workspace#copy(String, String)
     * @see javax.jcr.Workspace#copy(String, String, String)
     */
    public EventIterator copy(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, QName destName) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException;

    //------------------------------------------------------< Update, Clone >---
    /**
     * @param sessionInfo
     * @param nodeId
     * @param srcWorkspaceName
     * @throws javax.jcr.NoSuchWorkspaceException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#update(String)
     */
    public EventIterator update(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * @param sessionInfo
     * @param srcWorkspaceName
     * @param srcNodeId
     * @param destParentNodeId
     * @param destName
     * @param removeExisting
     * @throws javax.jcr.NoSuchWorkspaceException
     * @throws javax.jcr.nodetype.ConstraintViolationException
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.PathNotFoundException
     * @throws javax.jcr.ItemExistsException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Workspace#clone(String, String, String, boolean)
     */
    public EventIterator clone(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, QName destName, boolean removeExisting) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException;

    //------------------------------------------------------------< Locking >---

    /**
     * Retrieve available lock information for the given <code>NodeId</code>.
     * 
     * @param sessionInfo
     * @param nodeId
     * @return
     * @throws LockException
     * @throws RepositoryException
     * @see Node#getLock()
     */
    public LockInfo getLockInfo(SessionInfo sessionInfo, NodeId nodeId) throws LockException, RepositoryException;

    /**
     * @param sessionInfo
     * @param nodeId
     * @param deep
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#lock(boolean, boolean)
     */
    public EventIterator lock(SessionInfo sessionInfo, NodeId nodeId, boolean deep) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException;

    /**
     * Explicit refresh of an existing lock. Existing locks should be refreshed
     * implicitely with all read and write methods listed here.
     *
     * @param sessionInfo
     * @param nodeId
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.lock.Lock#refresh()
     */
    public EventIterator refreshLock(SessionInfo sessionInfo, NodeId nodeId) throws LockException, RepositoryException;

    /**
     * Releases the lock on the given node.<p/>
     * Please note, that on {@link javax.jcr.Session#logout() logout} all
     * session-scoped locks must be released by calling unlock.
     *
     * @param sessionInfo
     * @param nodeId
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#unlock()
     */
    public EventIterator unlock(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException;

    //---------------------------------------------------------< Versioning >---
    /**
     * @param sessionInfo
     * @param nodeId
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#checkin()
     */
    public EventIterator checkin(SessionInfo sessionInfo, NodeId nodeId) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException;

    /**
     * @param sessionInfo
     * @param nodeId
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#checkout()
     */
    public EventIterator checkout(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, LockException, RepositoryException;

    /**
     * @param sessionInfo
     * @param nodeId
     * @param versionId
     * @param removeExisting
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.PathNotFoundException
     * @throws javax.jcr.ItemExistsException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#restore(String, boolean)
     * @see javax.jcr.Node#restore(javax.jcr.version.Version, boolean)
     * @see javax.jcr.Node#restore(javax.jcr.version.Version, String, boolean)
     * @see javax.jcr.Node#restoreByLabel(String, boolean)
     */
    public EventIterator restore(SessionInfo sessionInfo, NodeId nodeId, NodeId versionId, boolean removeExisting) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * @param sessionInfo
     * @param versionIds
     * @param removeExisting
     * @throws javax.jcr.ItemExistsException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Workspace#restore(javax.jcr.version.Version[], boolean)
     */
    public EventIterator restore(SessionInfo sessionInfo, NodeId[] versionIds, boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * @param sessionInfo
     * @param nodeId
     * @param srcWorkspaceName
     * @param bestEffort
     * @throws javax.jcr.NoSuchWorkspaceException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.MergeException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#merge(String, boolean)
     */
    public EventIterator merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * @param sessionInfo
     * @param nodeId
     * @param mergeFailedIds The <code>NodeId</code>s remaining in the jcr:mergeFailed
     * REFERENCE property. The version id(s) to be resolved were removed from the
     * array and added to the predecessor ids in case of {@link Node#doneMerge(Version)}.
     * In case of a {@link Node#cancelMerge(Version)} the version id only gets
     * removed from the list.
     * @param predecessorIds
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#cancelMerge(javax.jcr.version.Version)
     * @see javax.jcr.Node#doneMerge(javax.jcr.version.Version)
     */
    public EventIterator resolveMergeConflict(SessionInfo sessionInfo, NodeId nodeId, NodeId[] mergeFailedIds, NodeId[] predecessorIds) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * @param sessionInfo
     * @param versionId
     * @param label
     * @param moveLabel
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.version.VersionHistory#addVersionLabel(String, String, boolean)
     */
    public EventIterator addVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, QName label, boolean moveLabel) throws VersionException, RepositoryException;

    /**
     * @param sessionInfo
     * @param versionId
     * @param label
     * @return
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.version.VersionHistory#removeVersionLabel(String)
     */
    public EventIterator removeVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, QName label) throws VersionException, RepositoryException;

    //----------------------------------------------------------< Searching >---
    /**
     * @param sessionInfo
     * @return
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.query.QueryManager#getSupportedQueryLanguages()
     */
    public String[] getSupportedQueryLanguages(SessionInfo sessionInfo) throws RepositoryException;

    /**
     * @param sessionInfo
     * @param statement
     * @param language
     * @return
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.query.Query#execute()
     */
    public QueryInfo executeQuery(SessionInfo sessionInfo, String statement, String language) throws RepositoryException;

    //--------------------------------------------------------< Observation >---

    /**
     * Registers a listener to receive events about changes that were applied
     * by other sessions. In contrast to {@link javax.jcr.observation.ObservationManager#addEventListener)}
     * this method does not have a <code>noLocal</code> flag.
     * </p>
     * The implementation must ensure that {@link EventIterator}s issued to
     * potential listeners and the ones returned by the individual methods
     * are in a proper sequence.
     *
     * @param sessionInfo
     * @param nodeId
     * @param listener
     * @param eventTypes
     * @param isDeep
     * @param uuid
     * @param nodeTypeIds
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.observation.ObservationManager#addEventListener(javax.jcr.observation.EventListener, int, String, boolean, String[], String[], boolean)
     */
    public void addEventListener(SessionInfo sessionInfo, NodeId nodeId, EventListener listener, int eventTypes, boolean isDeep, String[] uuid, QName[] nodeTypeIds) throws RepositoryException;

    /**
     * Removes the registration of the specified <code>EventListener</code>. If
     * the event listener was not registered for the node indentified by <code>nodeId</code>
     * an <code>RepositoryException</code> is thrown. The same applies if the
     * registration timeouted before or an other error occurs.<p/>
     * Please note, that all eventlistener registrations must be removed upon
     * {@link javax.jcr.Session#logout()} logout) of the <code>Session</code>.
     *
     * @param sessionInfo
     * @param nodeId
     * @param listener
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.observation.ObservationManager#removeEventListener(javax.jcr.observation.EventListener)
     */
    public void removeEventListener(SessionInfo sessionInfo, NodeId nodeId, EventListener listener) throws RepositoryException;

    //---------------------------------------------------------< Namespaces >---
    /**
     * Retrieve all registered namespaces.
     *
     * @param sessionInfo
     * @return
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Workspace#getNamespaceRegistry()
     * @see javax.jcr.NamespaceRegistry#getPrefixes()
     * @see javax.jcr.NamespaceRegistry#getURIs()
     * @see javax.jcr.NamespaceRegistry#getPrefix(String)
     * @see javax.jcr.NamespaceRegistry#getURI(String)
     */
    public Properties getRegisteredNamespaces(SessionInfo sessionInfo) throws RepositoryException;

    /**
     * Register a new namespace with the given prefix and uri
     *
     * @param sessionInfo
     * @param prefix
     * @param uri
     * @throws javax.jcr.NamespaceException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.NamespaceRegistry#registerNamespace(String, String)
     */
    public void registerNamespace(SessionInfo sessionInfo, String prefix, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException;

    /**
     * Unregister the namesspace indentified by the given prefix
     *
     * @param sessionInfo
     * @param uri
     * @throws javax.jcr.NamespaceException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.NamespaceRegistry#unregisterNamespace(String)
     */
    public void unregisterNamespace(SessionInfo sessionInfo, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException;

    //----------------------------------------------------------< NodeTypes >---
    /**
     * Retrieve the <code>QNodeTypeDefinition</code>s of all registered nodetypes.
     *
     * @param sessionInfo
     * @return
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Workspace#getNodeTypeManager()
     * @see javax.jcr.nodetype.NodeTypeManager#getAllNodeTypes()
     * @see javax.jcr.nodetype.NodeTypeManager#getMixinNodeTypes()
     * @see javax.jcr.nodetype.NodeTypeManager#getPrimaryNodeTypes()
     * @see javax.jcr.nodetype.NodeTypeManager#getNodeType(String)
     */
    public QNodeTypeDefinitionIterator getNodeTypeDefinitions(SessionInfo sessionInfo) throws RepositoryException;

    /**
     * @param sessionInfo
     * @param nodetypeDefs
     * @throws javax.jcr.nodetype.NoSuchNodeTypeException
     * @throws javax.jcr.RepositoryException
     */
    public void registerNodeTypes(SessionInfo sessionInfo, QNodeTypeDefinition[] nodetypeDefs) throws NoSuchNodeTypeException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * @param sessionInfo
     * @param nodetypeDefs
     * @throws javax.jcr.nodetype.NoSuchNodeTypeException
     * @throws javax.jcr.RepositoryException
     */
    public void reregisterNodeTypes(SessionInfo sessionInfo, QNodeTypeDefinition[] nodetypeDefs) throws NoSuchNodeTypeException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * @param sessionInfo
     * @param nodetypeNames
     * @throws javax.jcr.nodetype.NoSuchNodeTypeException
     * @throws javax.jcr.RepositoryException
     */
    public void unregisterNodeTypes(SessionInfo sessionInfo, QName[] nodetypeNames) throws NoSuchNodeTypeException, UnsupportedRepositoryOperationException, RepositoryException;
}
