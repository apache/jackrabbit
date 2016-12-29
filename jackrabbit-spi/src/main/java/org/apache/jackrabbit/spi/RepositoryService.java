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

import java.io.InputStream;
import java.util.Iterator;
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
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

/**
 * The <code>RepositoryService</code> interface defines methods used to
 * retrieve information from the persistent layer of the repository as well
 * as the methods that modify its persistent state.
 * The implementation of this interface is intended to hold only the state of
 * the persistent layer, no session-related state should be held. Consequently,
 * each method that alters persistent state always includes all the information
 * necessary to fully specify and authorize a change.
 * <p>
 * For example, consider the method
 * <pre>
 *    void RepositoryService.copy(SessionInfo sessionInfo,
 *                                String srcWorkspaceName,
 *                                NodeId srcNodeId, NodeId destParentNodeId,
 *                                Name destName)
 * </pre>
 * This method performs an immediate persistent copy of the node identified by
 * srcNodeId and that node's subtree to a position as child of the node
 * identified by destParentNodeId and assigns the newly copied node the name
 * destName.<br>
 * The <code>SessionInfo</code> object provides user and workspace identification
 * as well as eventual lock tokens required to execute the copy.<br>
 * If <code>srcWorkspaceName</code> differs from the workspace name present with
 * the SessionInfo, the copy is corresponds to a copy across workspaces.
 * The source and destination of the copy operation are specified by
 * {@link NodeId}s. The <code>Name</code> holds the new name. Taken together,
 * this information is sufficient to completely specify and authorize the copy
 * operations.
 * <p>
 * The RepositoryService in addition allows to create and submit {@link Batch}
 * objects, that cover lists of operations that have to be applied to the
 * persistent layer at once.
 */
public interface RepositoryService {

    /**
     * Return the <code>IdFactory</code>.
     *
     * @return The <code>IdFactory</code>.
     * @throws RepositoryException If an error occurs.
     */
    public IdFactory getIdFactory() throws RepositoryException;

    /**
     * Return the <code>NameFactory</code>.
     *
     * @return The <code>NameFactory</code>.
     * @throws RepositoryException If an error occurs.
     */
    public NameFactory getNameFactory() throws RepositoryException;

    /**
     * Return the <code>PathFactory</code>.
     *
     * @return The <code>PathFactory</code>.
     * @throws RepositoryException If an error occurs.
     */
    public PathFactory getPathFactory() throws RepositoryException;

    /**
     * Return the <code>QValueFactory</code> defined with this SPI implementation.
     *
     * @return The <code>QValueFactory</code>.
     * @throws RepositoryException If an error occurs.
     */
    public QValueFactory getQValueFactory() throws RepositoryException;

    /**
     * Returns a {@link ItemInfoCache} for the given <code>SessionInfo</code>.
     * @param sessionInfo
     * @return
     * @throws RepositoryException
     */
    public ItemInfoCache getItemInfoCache(SessionInfo sessionInfo) throws RepositoryException;

    //--------------------------------------------------------------------------
    /**
     * Returns all property descriptors that can be exposed with the
     * {@link javax.jcr.Repository Repository} implementation built on top of
     * this <code>RepositoryService</code>.
     *
     * @return key-value pairs for repository descriptor keys and values.
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Repository#getDescriptorKeys()
     * @see javax.jcr.Repository#getDescriptor(String)
     */
    public Map<String, QValue[]> getRepositoryDescriptors() throws RepositoryException;

    //-----------------------------------< SessionInfo creation and release >---
    /**
     * Returns a <code>SessionInfo</code> that will be used by other methods
     * on the <code>RepositoryService</code>.
     * An implementation may choose to authenticate the user using the supplied
     * <code>credentials</code>.
     *
     * @param credentials the credentials of the user.
     * @param workspaceName the name of the workspace the <code>SessionInfo</code>
     * should be built for. If the specified workspaceName is <code>null</code>
     * the implementation should select a default workspace.
     * @return a <code>SessionInfo</code> if authentication was successful.
     * @throws LoginException           if authentication of the user fails.
     * @throws NoSuchWorkspaceException if the specified <code>workspaceName</code>
     *                                  is not recognized.
     * @throws RepositoryException      if an error occurs.
     */
    public SessionInfo obtain(Credentials credentials, String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException;

    /**
     * Returns a new <code>SessionInfo</code> for the given workspace name that
     * will be used by other methods on the <code>RepositoryService</code>.
     *
     * @param sessionInfo for another workspace
     * @param workspaceName the name of the workspace the new <code>SessionInfo</code>
     * should be built for. If the specified workspaceName is <code>null</code>
     * the implementation should select a default workspace.
     * @return a <code>SessionInfo</code> if authentication was successful.
     * @throws LoginException           if authentication of the user fails.
     * @throws NoSuchWorkspaceException if the specified <code>workspaceName</code>
     *                                  is not recognized.
     * @throws RepositoryException      if an error occurs.
     */
    public SessionInfo obtain(SessionInfo sessionInfo, String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException;

    /**
     * Returns a <code>SessionInfo</code> that will be used by other methods
     * on the <code>RepositoryService</code>.
     *
     * @param sessionInfo
     * @param credentials
     * @return a <code>SessionInfo</code> if impersonate was successful.
     * @throws LoginException
     * @throws RepositoryException
     * @see javax.jcr.Session#impersonate(javax.jcr.Credentials)
     */
    public SessionInfo impersonate(SessionInfo sessionInfo, Credentials credentials) throws LoginException, RepositoryException;

    /**
     * Indicates to the <code>RepositoryService</code>, that the given SessionInfo
     * will not be used any more.
     *
     * @param sessionInfo
     * @throws javax.jcr.RepositoryException
     */
    public void dispose(SessionInfo sessionInfo) throws RepositoryException;

    //--------------------------------------------------------------------------
    /**
     * Return all workspace names available for the given <code>SessionInfo</code>.
     *
     * @param sessionInfo
     * @return An array of workspace names.
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Workspace#getAccessibleWorkspaceNames()
     * @see javax.jcr.Workspace#getName()
     */
    public String[] getWorkspaceNames(SessionInfo sessionInfo) throws RepositoryException;

    //-----------------------------------------------------< Access Control >---
    /**
     * Returns true if all actions defined in the specified array are granted
     * to given <code>SessionInfo</code>. False otherwise.
     *
     * @param sessionInfo
     * @param itemId
     * @param actions
     * @return true if the session with the given <code>SessionInfo</code> has
     * the specified rights for the given item.
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Session#checkPermission(String, String)
     * @see javax.jcr.Session#hasPermission(String, String) 
     */
    public boolean isGranted(SessionInfo sessionInfo, ItemId itemId, String[] actions) throws RepositoryException;

    /**
     * TODO
     *
     *
     * @param sessionInfo
     * @return
     * @throws RepositoryException
     */
    public PrivilegeDefinition[] getPrivilegeDefinitions(SessionInfo sessionInfo) throws RepositoryException;

    /**
     * TODO
     * 
     *
     * @param sessionInfo
     * @param id
     * @return
     * @throws RepositoryException
     */
    public Name[] getPrivilegeNames(SessionInfo sessionInfo, NodeId id) throws RepositoryException;
    
    /**
     * TODO
     *
     * @param sessionInfo
     * @param nodeId
     * @return
     * @throws RepositoryException
     */
    public PrivilegeDefinition[] getSupportedPrivileges(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException;

    //------------------------------------------------------< Reading items >---
    /**
     * Returns the <code>QNodeDefinition</code> for the <code>Node</code>
     * identified by the given id. This method should only be used if the
     * caller is not able to unambiguously determine the applicable definition
     * from the parent node type definition or if no parent exists (i.e. for
     * the root).
     *
     * @param sessionInfo
     * @param nodeId
     * @return The node definition applicable to the <code>Node</code> identified
     * by the given id.
     * @throws javax.jcr.RepositoryException
     */
    public QNodeDefinition getNodeDefinition(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException;

    /**
     * Returns the <code>QPropertyDefinition</code> for the <code>Property</code>
     * identified by the given id. This method should only be used if the
     * caller is not able to unambiguously determine the applicable definition
     * from the parent node type definition.
     *
     * @param sessionInfo
     * @param propertyId
     * @return The property definition applicable for the <code>Property</code>
     * identified by the given id.
     * @throws javax.jcr.RepositoryException
     */
    public QPropertyDefinition getPropertyDefinition(SessionInfo sessionInfo, PropertyId propertyId) throws RepositoryException;

    /**
     * Retrieve the <code>NodeInfo</code> for the node identified by the given
     * <code>NodeId</code>. See {@link #getItemInfos(SessionInfo, ItemId)} for
     * a similar method that in addition may return <code>ItemInfo</code>s of
     * children <code>Item</code>s.
     *
     * @param sessionInfo
     * @param nodeId
     * @return The <code>NodeInfo</code> for the node identified by the given id.
     * @throws javax.jcr.ItemNotFoundException
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
     * @deprecated Use {@link #getItemInfos(SessionInfo, ItemId)}
     */
    public NodeInfo getNodeInfo(SessionInfo sessionInfo, NodeId nodeId) throws ItemNotFoundException, RepositoryException;

    /**
     * Method used to 'batch-read' from the persistent storage. It returns the
     * <code>ItemInfo</code> for the given <code>ItemId</code> as the first
     * element in the <code>Iterator</code>. In addition the iterator may contain
     * arbitrary <code>ItemInfo</code>s.
     *
     * @param sessionInfo
     * @param itemId
     * @return An <code>Iterator</code> of <code>ItemInfo</code>s containing
     * at least a single element: the <code>ItemInfo</code> that represents
     * the Item identified by the given <code>ItemId</code>. If the Iterator
     * contains multiple elements, the first is expected to represent the Item
     * identified by the given <code>ItemId</code>.
     * @throws javax.jcr.ItemNotFoundException
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
    public Iterator<? extends ItemInfo> getItemInfos(SessionInfo sessionInfo, ItemId itemId) throws ItemNotFoundException, RepositoryException;

    /**
     * Returns an Iterator of <code>ChildInfo</code>s present on the
     * Node represented by the given parentId.
     *
     * @param sessionInfo
     * @param parentId
     * @return An Iterator of <code>ChildInfo</code>s present on the
     * Node represented by the given parentId.
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public Iterator<ChildInfo> getChildInfos(SessionInfo sessionInfo, NodeId parentId) throws ItemNotFoundException, RepositoryException;

    /**
     * Returns the {@link PropertyId Id}s of the properties that are referencing
     * the node identified by the given <code>nodeId</code>. If
     * <code>weakReferences</code> is <code>true</code> the ids of
     * {@link javax.jcr.PropertyType#WEAKREFERENCE WEAKREFERENCE} properties are
     * returned, otherwise the property must be of type {@link javax.jcr.PropertyType#REFERENCE REFERENCE}.
     *
     * @param sessionInfo
     * @param nodeId
     * @param propertyName name filter of referring properties to be returned;
     * if <code>null</code> then all references are returned.
     * @param weakReferences If <code>true</code> the properties must be of type
     * {@link javax.jcr.PropertyType#WEAKREFERENCE}, otherwise of type
     * {@link javax.jcr.PropertyType#REFERENCE}.
     * @return An Iterator of {@link PropertyId Id}s of the properties that are
     * referencing the node identified by the given <code>nodeId</code> or an
     * empty iterator if the node is not referenceable or no references exist.
     * @throws ItemNotFoundException
     * @throws RepositoryException
     * @see PropertyInfo#getId()
     * @since JCR 2.0
     */
    public Iterator<PropertyId> getReferences(SessionInfo sessionInfo, NodeId nodeId, Name propertyName, boolean weakReferences) throws ItemNotFoundException, RepositoryException;

    /**
     * Returns the <code>PropertyInfo</code> for the <code>Property</code>
     * identified by the given id.
     *
     * @param sessionInfo
     * @param propertyId
     * @return The <code>PropertyInfo</code> for the <code>Property</code>
     * identified by the given id.
     * @throws javax.jcr.ItemNotFoundException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Session#getItem(String)
     * @see javax.jcr.Node#getProperty(String)
     * @deprecated Use {@link #getItemInfos(SessionInfo, ItemId)}
     */
    public PropertyInfo getPropertyInfo(SessionInfo sessionInfo, PropertyId propertyId) throws ItemNotFoundException, RepositoryException;

    //-----------------------------------------------< general modification >---
    /**
     * Indicates the start of a set of operations that cause modifications
     * on the underlying persistence layer. All modification called on the
     * {@link Batch} must be executed at once or non must be executed upon
     * calling {@link #submit(Batch)}.
     *
     * @param sessionInfo
     * @param itemId Id of the Item that is a common ancestor of all
     * <code>Item</code>s affected upon batch execution. This <code>Item</code>
     * might itself be modified within the scope of the <code>Batch</code>.
     * @return A Batch indicating the start of a set of transient modifications
     * that will be execute at once upon {@link #submit(Batch)}.
     * @throws RepositoryException
     * @see javax.jcr.Item#save()
     * @see javax.jcr.Session#save()
     * @see Batch
     */
    public Batch createBatch(SessionInfo sessionInfo, ItemId itemId) throws RepositoryException;

    /**
     * Completes the given {@link Batch} or discard all the previous modifications.
     * See {@link #createBatch(SessionInfo,ItemId)} for additional information
     * regarding batch creation.
     *
     * @param batch
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
     * @see Batch
     */
    public void submit(Batch batch) throws PathNotFoundException, ItemNotFoundException, NoSuchNodeTypeException, ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Creates a new {@code Tree} that can be populated and later on be applied
     * to the specified {@code Batch} by calling {@code #setTree}.
     *
     * @param nodeName
     * @param primaryTypeName
     * @param uniqueId
     * @return a new {@code Tree} instance.
     * @throws RepositoryException
     */
    public Tree createTree(SessionInfo sessionInfo, Batch batch, Name nodeName, Name primaryTypeName, String uniqueId) throws RepositoryException;

    //-------------------------------------------------------------< Import >---
    /**
     * Imports the data present in the given <code>InputStream</code> into the
     * persistent layer. Note, that the implementation is responsible for
     * validating the data presented and for the integrity of the repository
     * upon completion.
     *
     * @param sessionInfo
     * @param parentId
     * @param xmlStream
     * @param uuidBehaviour
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
    public void importXml(SessionInfo sessionInfo, NodeId parentId, InputStream xmlStream, int uuidBehaviour) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

    //---------------------------------------------------------< Copy, Move >---
    /**
     * Moves the node identified by the given <code>srcNodeId</code> (and its
     * entire subtree) to the new location defined by <code>destParentNodeId</code>
     * and a new name (<code>destName</code>).
     *
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
    public void move(SessionInfo sessionInfo, NodeId srcNodeId, NodeId destParentNodeId, Name destName) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Clone the subtree identified by the given <code>srcNodeId</code>
     * in workspace named <code>srcWorkspaceName</code> to the destination
     * in the workspace specified by the given <code>SessionInfo</code>. The
     * destination is composed by the given parent id and the new name
     * as indicated by <code>destName</code>.
     * <p>
     * Note, that <code>srcWorkspaceName</code> may be the same as the one
     * specified within the <code>SessionInfo</code>. In this case the copy
     * corresponds to a copy within a single workspace.
     *
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
    public void copy(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, Name destName) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException;

    //------------------------------------------------------< Update, Clone >---
    /**
     * Updates the node identified by the given <code>NodeId</code> replacing
     * it (an the complete subtree) with a clone of its corresponding node
     * present in the workspace with the given <code>srcWorkspaceName</code>.
     *
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
    public void update(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * Clone the subtree identified by the given <code>srcNodeId</code>
     * in workspace named <code>srcWorkspaceName</code> to the destination
     * in the workspace specified by the given <code>SessionInfo</code>. The
     * destination is composed by the given parent id and the new name
     * as indicated by <code>destName</code>.
     *
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
    public void clone(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, Name destName, boolean removeExisting) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException;

    //------------------------------------------------------------< Locking >---
    /**
     * Returns the lock information that applies to <code>Node</code> identified
     * by the given <code>NodeId</code> or <code>null</code>. If the implementation
     * does not support locking at all, this method always returns <code>null</code>.
     *
     * @param sessionInfo
     * @param nodeId
     * @return The lock information for the Node identified by the given
     * <code>nodeId</code> or <code>null</code> if no lock applies to that Node.
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.RepositoryException If some other error occurs.
     * @see Node#getLock()
     */
    public LockInfo getLockInfo(SessionInfo sessionInfo, NodeId nodeId) throws AccessDeniedException, RepositoryException;

    /**
     * Create a lock on the <code>Node</code> identified by the given id.
     *
     * @param sessionInfo
     * @param nodeId
     * @param deep
     * @param sessionScoped
     * @return The <code>LockInfo</code> associated with the new lock
     * that has been created.
     * @throws javax.jcr.UnsupportedRepositoryOperationException If this SPI
     * implementation does not support locking at all.
     * @throws javax.jcr.lock.LockException If the Node identified by the given
     * id cannot be locked due to an existing lock or due to missing mixin type.
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.RepositoryException If another error occurs.
     * @see javax.jcr.Node#lock(boolean, boolean)
     */
    public LockInfo lock(SessionInfo sessionInfo, NodeId nodeId, boolean deep, boolean sessionScoped) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException;

    /**
     * Create a lock on the <code>Node</code> identified by the given id.
     *
     * @param sessionInfo
     * @param nodeId
     * @param deep
     * @param sessionScoped
     * @param timeoutHint long indicating the desired lock timeout in seconds.
     * The implementation is free to ignore the hint.
     * @param ownerHint String indicating the desired lockOwner info. The
     * implementation is free to ignore the hint.
     * @return The <code>LockInfo</code> associated with the new lock
     * that has been created.
     * @throws javax.jcr.UnsupportedRepositoryOperationException If this SPI
     * implementation does not support locking at all.
     * @throws javax.jcr.lock.LockException If the Node identified by the given
     * id cannot be locked due to an existing lock or due to missing mixin type.
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.RepositoryException If another error occurs.
     * @see javax.jcr.lock.LockManager#lock(String, boolean, boolean, long, String)
     * @since JCR 2.0
     */
    public LockInfo lock(SessionInfo sessionInfo, NodeId nodeId, boolean deep, boolean sessionScoped, long timeoutHint, String ownerHint) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException;

    /**
     * Explicit refresh of an existing lock. Existing locks should be refreshed
     * implicitly with all read and write methods listed here.
     *
     * @param sessionInfo
     * @param nodeId
     * @throws javax.jcr.UnsupportedRepositoryOperationException If this SPI
     * implementation does not support locking at all.
     * @throws javax.jcr.lock.LockException If the Node identified by the given
     * id is not locked (any more) or if the <code>SessionInfo</code> does not
     * contain the token associated with the lock to be refreshed.
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.RepositoryException If another error occurs.
     * @see javax.jcr.lock.Lock
     */
    public void refreshLock(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException;

    /**
     * Releases the lock on the <code>Node</code> identified by the given
     * <code>NodeId</code>.
     * <p>
     * Please note, that on {@link javax.jcr.Session#logout() logout} all
     * session-scoped locks must be released by calling unlock.
     *
     * @param sessionInfo
     * @param nodeId
     * @throws javax.jcr.UnsupportedRepositoryOperationException If this SPI
     * implementation does not support locking at all.
     * @throws javax.jcr.lock.LockException If the Node identified by the given
     * id is not locked or if the <code>SessionInfo</code> does not contain the
     * token associated with the lock to be released.
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.RepositoryException If another error occurs.
     * @see javax.jcr.Node#unlock()
     */
    public void unlock(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException;

    //---------------------------------------------------------< Versioning >---
    /**
     * Performs a checkin for the <code>Node</code> identified by the given
     * <code>NodeId</code>.
     *
     * @param sessionInfo
     * @param nodeId
     * @return <code>NodeId</code> of newly created version
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#checkin()
     */
    public NodeId checkin(SessionInfo sessionInfo, NodeId nodeId) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException;

    /**
     * Performs a checkout for the <code>Node</code> identified by the given
     * <code>NodeId</code>. Same as {@link #checkout(SessionInfo, NodeId, NodeId)}
     * where the <code>activityId</code> is <code>null</code>.
     *
     * @param sessionInfo
     * @param nodeId
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#checkout()
     */
    public void checkout(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, LockException, RepositoryException;

    /**
     * Performs a checkout for the <code>Node</code> identified by the given
     * <code>NodeId</code> and for activity identified by the specified
     * <code>activityId</code>. If the <code>activityId</code> is <code>null</code>
     * this corresponds to {@link #checkout(SessionInfo, NodeId)}
     *
     * @param sessionInfo
     * @param nodeId
     * @param activityId  Id of the activity node set to the editing session or
     * <code>null</code> if no activity is in effect.
     * @throws UnsupportedRepositoryOperationException
     * @throws LockException
     * @throws RepositoryException
     * @since JCR 2.0
     */
    public void checkout(SessionInfo sessionInfo, NodeId nodeId, NodeId activityId) throws UnsupportedRepositoryOperationException, LockException, RepositoryException;

    /**
     * Performs a checkpoint for the <code>Node</code> identified by the given
     * <code>NodeId</code>.
     *
     * @param sessionInfo
     * @param nodeId
     * @return <code>NodeId</code> of newly created version
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.version.VersionManager#checkpoint(String)
     * @since JCR 2.0
     */
    public NodeId checkpoint(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Performs a checkpoint for the <code>Node</code> identified by the given
     * <code>NodeId</code>. For the checkout part the specified <code>activityId</code>
     * is taken into account as specified in {@link #checkout(SessionInfo, NodeId, NodeId)}.
     *
     * @param sessionInfo
     * @param nodeId
     * @param activityId Id of the activity node set to the editing session or
     * <code>null</code> if no activity is in effect.
     * @throws UnsupportedRepositoryOperationException
     * @throws LockException
     * @throws RepositoryException
     * @since JCR 2.0
     */
    public NodeId checkpoint(SessionInfo sessionInfo, NodeId nodeId, NodeId activityId)  throws UnsupportedRepositoryOperationException, RepositoryException;
    /**
     * Remove the version identified by the specified <code>versionId</code>.
     *
     * @param sessionInfo
     * @param versionHistoryId <code>NodeId</code> identifying the version
     * history the version identified by <code>versionId</code> belongs to.
     * @param versionId
     * @throws ReferentialIntegrityException
     * @throws AccessDeniedException
     * @throws UnsupportedRepositoryOperationException
     * @throws VersionException
     * @throws RepositoryException
     * @see javax.jcr.version.VersionHistory#removeVersion(String)
     */
    public void removeVersion(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId) throws ReferentialIntegrityException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException;

    /**
     * Restores the node identified by <code>nodeId</code> to the state defined
     * by the version with the specified <code>versionId</code>.
     *
     * @param sessionInfo
     * @param nodeId
     * @param versionId
     * @param removeExisting boolean flag indicating how to deal with an
     * identifier collision that may occur if a node exists outside the subtree
     * to be restored with the same identified as a node that would be
     * introduces by the restore. If the <code>removeExisting</code> is
     * <code>true</code> the restored node takes precedence and the
     * existing node is removed. Otherwise the restore fails.
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
    public void restore(SessionInfo sessionInfo, NodeId nodeId, NodeId versionId, boolean removeExisting) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * Restore multiple versions at once. The versions to be restored are
     * identified by the given array of <code>NodeId</code>s.
     *
     * @param sessionInfo
     * @param versionIds
     * @param removeExisting boolean flag indicating how to deal with an
     * identifier collision that may occur if a node exists outside the subtrees
     * to be restored with the same identified as any node that would be
     * introduces by the restore. If the <code>removeExisting</code> is
     * <code>true</code> the node to be restored takes precedence and the
     * existing node is removed. Otherwise the restore fails.
     * @throws javax.jcr.ItemExistsException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Workspace#restore(javax.jcr.version.Version[], boolean)
     */
    public void restore(SessionInfo sessionInfo, NodeId[] versionIds, boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * Merge the node identified by the given <code>NodeId</code> and its subtree
     * with the corresponding node present in the workspace with the name of
     * <code>srcWorkspaceName</code>.
     *
     * @param sessionInfo
     * @param nodeId
     * @param srcWorkspaceName
     * @param bestEffort
     * @return an <code>Iterator</code> over the {@link NodeId}s of all nodes that
     * received a merge result of "fail" in the course of this operation.
     * @throws javax.jcr.NoSuchWorkspaceException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.MergeException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#merge(String, boolean)
     */
    public Iterator<NodeId> merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * Merge the node identified by the given <code>NodeId</code> and its subtree
     * with the corresponding node present in the workspace with the name of
     * <code>srcWorkspaceName</code>.
     *
     * @param sessionInfo
     * @param nodeId
     * @param srcWorkspaceName
     * @param bestEffort
     * @return an <code>Iterator</code> over the {@link NodeId}s of all nodes that
     * received a merge result of "fail" in the course of this operation.
     * @throws javax.jcr.NoSuchWorkspaceException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.MergeException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.version.VersionManager#merge(String, String, boolean, boolean)
     * @since JCR 2.0
     */
    public Iterator<NodeId> merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName, boolean bestEffort, boolean isShallow) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * Resolve an existing merge conflict present with the node identified by
     * the given <code>NodeId</code>.
     *
     * @param sessionInfo
     * @param nodeId
     * @param mergeFailedIds The <code>NodeId</code>s remaining in the jcr:mergeFailed
     * REFERENCE property. The version id(s) to be resolved were removed from the
     * array and added to the predecessor ids in case of {@link Node#doneMerge(Version)}.
     * In case of a {@link Node#cancelMerge(Version)} the version id only gets
     * removed from the list.
     * @param predecessorIds The complete set of predecessor id including those
     * that have been added in order to resolve a merge conflict.
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#cancelMerge(javax.jcr.version.Version)
     * @see javax.jcr.Node#doneMerge(javax.jcr.version.Version)
     */
    public void resolveMergeConflict(SessionInfo sessionInfo, NodeId nodeId, NodeId[] mergeFailedIds, NodeId[] predecessorIds) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Add the given version label in the persistent layer.
     *
     * @param sessionInfo
     * @param versionHistoryId <code>NodeId</code> identifying the version
     * history the version identified by <code>versionId</code> belongs to.
     * @param versionId <code>NodeId</code> identifying the version the
     * label belongs to.
     * @param label The label to be added.
     * @param moveLabel If the label is already assigned to a version within
     * the same version history this parameter has the following effect: If <code>true</code>
     * the label already present gets moved to be now be a label of the version
     * indicated by <code>versionId</code>. If <code>false</code> this method
     * fails and the label remains with the original version.
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.version.VersionHistory#addVersionLabel(String, String, boolean)
     */
    public void addVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, Name label, boolean moveLabel) throws VersionException, RepositoryException;

    /**
     * Remove the given version label in the persistent layer.
     *
     * @param sessionInfo
     * @param versionHistoryId <code>NodeId</code> identifying the version
     * history the version identified by <code>versionId</code> belongs to.
     * @param versionId <code>NodeId</code> identifying the version the
     * label belongs to.
     * @param label The label to be removed.
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.version.VersionHistory#removeVersionLabel(String)
     */
    public void removeVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, Name label) throws VersionException, RepositoryException;

    /**
     * Create a new activity.
     *
     * @param sessionInfo
     * @param title
     * @return the <code>NodeId</code> of the new activity node.
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.version.VersionManager#createActivity(String)
     * @since JCR 2.0
     */
    public NodeId createActivity(SessionInfo sessionInfo, String title) throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Removes the activity identified by the specified <code>activityId</code>.
     *
     * @param sessionInfo
     * @param activityId
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.version.VersionManager#removeActivity(Node)
     * @since JCR 2.0
     */
    public void removeActivity(SessionInfo sessionInfo, NodeId activityId) throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Merges the activity identified by the given <code>activityId</code> into
     * the workspace the specified <code>sessionInfo</code> has been created for.
     *
     * @param sessionInfo
     * @param activityId
     * @return an <code>Iterator</code> over the {@link NodeId}s of all nodes that
     * received a merge result of "fail" in the course of this operation.
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     */
    public Iterator<NodeId> mergeActivity(SessionInfo sessionInfo, NodeId activityId) throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     *
     * @param sessionInfo
     * @param nodeId
     * @return
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     * @see javax.jcr.version.VersionManager#createConfiguration(String)
     */
    public NodeId createConfiguration(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, RepositoryException;

    //----------------------------------------------------------< Searching >---
    /**
     * Returns a String array identifying all query languages supported by this
     * SPI implementation.
     *
     * @param sessionInfo
     * @return String array identifying all query languages supported by this
     * SPI implementation.
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.query.QueryManager#getSupportedQueryLanguages()
     */
    public String[] getSupportedQueryLanguages(SessionInfo sessionInfo) throws RepositoryException;

    /**
     * Checks if the query <code>statement</code> is valid according to the
     * specified query <code>language</code> and returns the bind variable
     * names found in the query statement.
     *
     * @param sessionInfo the session info.
     * @param statement   the query statement to check.
     * @param language    the query language.
     * @param namespaces  the locally re-mapped namespace which may be used in
     *                    the query <code>statement</code>.
     * @return the bind variable names.
     * @throws InvalidQueryException if the query statement is invalid or the
     *                               language is not supported.
     * @throws RepositoryException   if an error occurs while checking the
     *                               statement.
     * @see javax.jcr.query.QueryManager#createQuery(String, String)
     */
    public String[] checkQueryStatement(SessionInfo sessionInfo, String statement, String language, Map<String, String> namespaces) throws InvalidQueryException, RepositoryException;

    /**
     * Execute the given query statement with the specified query language. The
     * additional <code>namespaces</code> parameter provides a mapping of prefix
     * to namespace uri in order to be able to properly resolve prefix:localname
     * patterns present within the query statement.
     *
     * @param sessionInfo the session info that wants to execute the query.
     * @param statement   the query statement to be execute.
     * @param language    the query language used to parse the query
     *                    <code>statement</code>.
     * @param namespaces  the locally re-mapped namespace which may be used in
     *                    the query <code>statement</code>.
     * @param limit       The maximum result size or <code>-1</code> is no
     *                    maximum is set.
     * @param offset      The offset in the total result set or <code>-1</code>
     *                    is no offset is set.
     * @param values      A Map of name/value pairs collected upon calls to
     *                    {@link javax.jcr.query.Query#bindValue(String,
     *                    javax.jcr.Value)}.
     * @return The query info.
     * @throws RepositoryException if an error occurs.
     * @see javax.jcr.query.Query#execute()
     */
    public QueryInfo executeQuery(SessionInfo sessionInfo,
                                  String statement,
                                  String language,
                                  Map<String, String> namespaces,
                                  long limit,
                                  long offset,
                                  Map<String, QValue> values)
            throws RepositoryException;

    //--------------------------------------------------------< Observation >---
    /**
     * Creates an event filter. If the repository supports observation, the
     * filter created is based on the parameters available in {@link
     * javax.jcr.observation.ObservationManager#addEventListener}.
     * <p>
     * Note, that an SPI implementation may support observation even if
     * the corresponding {@link javax.jcr.Repository#OPTION_OBSERVATION_SUPPORTED repository descriptor}
     * does not return 'true'.
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
     * @throws UnsupportedRepositoryOperationException if this SPI implementation
     * does not allow to create event filters.
     * @throws RepositoryException if an error occurs while creating the
     * EventFilter.
     * @see javax.jcr.observation.ObservationManager#addEventListener(javax.jcr.observation.EventListener, int, String, boolean, String[], String[], boolean)
     */
    public EventFilter createEventFilter(SessionInfo sessionInfo, int eventTypes,
                                         Path absPath, boolean isDeep,
                                         String[] uuid, Name[] nodeTypeName,
                                         boolean noLocal)
            throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Creates a new {@link Subscription} for events with an initial set of
     * {@link EventFilter}s. The returned subscription must provide events from
     * the time when the subscription was created. If an empty array of filters
     * is passed no events will be available through the created subscription
     * unless the filters are later updated by calling
     * {@link RepositoryService#updateEventFilters(Subscription, EventFilter[])}.
     *
     * @param sessionInfo the session info.
     * @param filters the initial event filters for the subscription.
     * @return
     * @throws UnsupportedRepositoryOperationException
     *                             if this SPI implementation does not support
     *                             observation.
     * @throws RepositoryException if an error occurs while creating the
     *                             Subscription.
     */
    public Subscription createSubscription(SessionInfo sessionInfo, EventFilter[] filters)
            throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Updates events filters on the subscription. When this method returns all
     * events that go through the passed subscription and have been generated
     * after this method call must be filtered using the passed
     * <code>filters</code>.
     * <p>
     * An implementation is required to accept at least event filter instances
     * created by {@link RepositoryService#createEventFilter}. Optionally an
     * implementation may also support event filters instanciated by the client
     * itself. An implementation may require special deployment in that case,
     * e.g. to make the event filter implementation class available to the
     * repository server.
     * <p>
     * <b>Note on thread-safety:</b> it is permissible to call this methods
     * while another thread is blocked in calling {@link
     * RepositoryService#getEvents(Subscription, long)} using the same
     * subscription instance as a parameter.
     *
     * @param subscription the subscription where the event filters are
     *                     applied.
     * @param filters the filters that are applied to the events as they
     *                occurred on the repository. An event is included in an
     *                event bundle if it is {@link EventFilter#accept(Event,
     *                boolean) accept}ed by at least one of the supplied
     *                filters. If an empty array is passed none of the potential
     *                events are include in an event bundle. This allows a
     *                client to skip or ignore events for a certain period of
     *                time.
     * @throws RepositoryException  if an error occurs while updating the event
     *                              filters.
     */
    public void updateEventFilters(Subscription subscription, EventFilter[] filters)
            throws RepositoryException;

    /**
     * Retrieves the events that occurred since the last call to this method for
     * the passed subscription.
     * <p>
     * Note, that an SPI implementation may support observation even if the
     * corresponding {@link javax.jcr.Repository#OPTION_OBSERVATION_SUPPORTED
     * repository descriptor} does return 'false'.
     * <p>
     * An implementation should un-block a calling thread and let it return if
     * the associated subscription is disposed by another thread.
     *
     * @param subscription a subscription.
     * @param timeout      a timeout in milliseconds to wait at most for an
     *                     event bundle. If <code>timeout</code> is up and no
     *                     event occurred meanwhile an empty array is returned.
     * @return an array of <code>EventBundle</code>s representing the events
     *         that occurred.
     * @throws RepositoryException  if an error occurs while retrieving the
     *                              event bundles.
     * @throws InterruptedException if the calling thread is interrupted while
     *                              waiting for events within the specified
     *                              <code>timeout</code>.
     */
    public EventBundle[] getEvents(Subscription subscription, long timeout)
            throws RepositoryException, InterruptedException;

    /**
     * Returns events from the <code>EventJournal</code> after a given point in
     * time. The returned event bundle may only contain events up to a given
     * time. In order to retrieve more events a client must call this method
     * again with the timestamp from the last event bundle. An empty bundle
     * indicates that there are no more events.
     *
     * @param sessionInfo the session info.
     * @param filter      the event filter to apply. Please note: the
     *                    <code>noLocal</code> flag is ignored.
     * @param after       retrieve events that occurred after the given
     *                    timestamp.
     * @return the event bundle.
     * @throws RepositoryException if an error occurs.
     * @throws UnsupportedRepositoryOperationException
     *                             if the underlying implementation does not
     *                             support event journaling.
     */
    public EventBundle getEvents(SessionInfo sessionInfo, EventFilter filter, long after)
            throws RepositoryException, UnsupportedRepositoryOperationException;

    /**
     * Indicates that the passed subscription is no longer needed.
     * <p>
     * <b>Note on thread-safety:</b> it is permissible to call this methods
     * while another thread is blocked in calling {@link
     * RepositoryService#getEvents(Subscription, long)} using the same
     * subscription instance as a parameter.
     *
     * @throws RepositoryException if an error occurs while the subscription is
     *                             disposed.
     */
    public void dispose(Subscription subscription) throws RepositoryException;

    //---------------------------------------------------------< Namespaces >---
    /**
     * Retrieve all registered namespaces. The namespace to prefix mapping is
     * done using the prefix as key and the namespace as value in the Map.
     *
     * @param sessionInfo
     * @return
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Workspace#getNamespaceRegistry()
     * @see javax.jcr.NamespaceRegistry#getPrefixes()
     * @see javax.jcr.NamespaceRegistry#getURIs()
     */
    public Map<String, String> getRegisteredNamespaces(SessionInfo sessionInfo) throws RepositoryException;

    /**
     * Returns the namespace URI for the given namespace <code>prefix</code>.
     *
     * @param sessionInfo the session info.
     * @param prefix a namespace prefix to resolve.
     * @return the namespace URI for the given namespace <code>prefix</code>.
     * @throws NamespaceException if prefix is not mapped to a namespace URI.
     * @throws RepositoryException if another error occurs.
     * @see javax.jcr.NamespaceRegistry#getURI(String)
     */
    public String getNamespaceURI(SessionInfo sessionInfo, String prefix)
            throws NamespaceException, RepositoryException;

    /**
     * Returns the namespace prefix for the given namespace <code>uri</code>.
     *
     * @param sessionInfo the session info.
     * @param uri the namespace URI.
     * @return the namespace prefix.
     * @throws NamespaceException if the URI unknown.
     * @throws RepositoryException if another error occurs.
     * @see javax.jcr.NamespaceRegistry#getPrefix(String)
     */
    public String getNamespacePrefix(SessionInfo sessionInfo, String uri)
            throws NamespaceException, RepositoryException;

    /**
     * Register a new namespace with the given prefix and uri.
     *
     * @param sessionInfo
     * @param prefix Prefix of the namespace to be registered.
     * @param uri Namespace URI to be registered.
     * @throws javax.jcr.NamespaceException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.NamespaceRegistry#registerNamespace(String, String)
     */
    public void registerNamespace(SessionInfo sessionInfo, String prefix, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException;

    /**
     * Unregister the namespace identified by the given uri
     *
     * @param sessionInfo
     * @param uri Namespace URI to be unregistered.
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
     * @return Iterator of {@link QNodeTypeDefinition}s.
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Workspace#getNodeTypeManager()
     * @see javax.jcr.nodetype.NodeTypeManager#getAllNodeTypes()
     * @see javax.jcr.nodetype.NodeTypeManager#getMixinNodeTypes()
     * @see javax.jcr.nodetype.NodeTypeManager#getPrimaryNodeTypes()
     * @see javax.jcr.nodetype.NodeTypeManager#getNodeType(String)
     */
    public Iterator<QNodeTypeDefinition> getQNodeTypeDefinitions(SessionInfo sessionInfo) throws RepositoryException;

    /**
     * Retrieve <code>QNodeTypeDefinition</code>s for the given names. The
     * implementation is free to return additional definitions which will (probably)
     * be needed by the caller due to node type inheritance. The caller must be
     * able to deal with any kind of additional <code>QNodeTypeDefinition</code>s
     * present in the <code>Iterator</code> irrespective whether they have been
     * loaded before or not.
     *
     * @param sessionInfo
     * @param nodetypeNames names of node types to retrieve
     * @return {@link QNodeTypeDefinition}
     * @throws javax.jcr.nodetype.NoSuchNodeTypeException if for any of the given
     * names no <code>QNodeTypeDefinition</code> exists.
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Workspace#getNodeTypeManager()
     * @see javax.jcr.nodetype.NodeTypeManager#getAllNodeTypes()
     * @see javax.jcr.nodetype.NodeTypeManager#getMixinNodeTypes()
     * @see javax.jcr.nodetype.NodeTypeManager#getPrimaryNodeTypes()
     * @see javax.jcr.nodetype.NodeTypeManager#getNodeType(String)
     */
    public Iterator<QNodeTypeDefinition> getQNodeTypeDefinitions(SessionInfo sessionInfo, Name[] nodetypeNames) throws RepositoryException;

    /**
     * Registers the node types with the specified <code>QNodeTypeDefinition</code>s.
     * If <code>allowUpdate</code> is <code>true</code> this method may also be
     * used to reregister existing node types with a modified definition, otherwise
     * this method will fail with <code>NodeTypeExistsException</code> if any of
     * the specified definition has the name of an already registered node type.
     *
     * @param sessionInfo
     * @param nodeTypeDefinitions
     * @param allowUpdate
     * @throws InvalidNodeTypeDefinitionException If any of the specified definitions
     * is invalid.
     * @throws NodeTypeExistsException If any of the specified definitions has the
     * name of an already registered node type and <code>allowUpdate</code> is <code>false</code>.
     * @throws UnsupportedRepositoryOperationException If registering node types
     * is not supported.
     * @throws RepositoryException If another error occurs.
     * @see javax.jcr.nodetype.NodeTypeManager#registerNodeTypes(javax.jcr.nodetype.NodeTypeDefinition[], boolean)
     */
    public void registerNodeTypes(SessionInfo sessionInfo, QNodeTypeDefinition[] nodeTypeDefinitions, boolean allowUpdate) throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Unregisters the node types with the specified <code>names</code>.
     *
     * @param sessionInfo
     * @param nodeTypeNames
     * @throws UnsupportedRepositoryOperationException If unregistering node types
     * is not supported.
     * @throws NoSuchNodeTypeException If any of the specified names has no
     * corresponding registered node type.
     * @throws RepositoryException If another error occurs.
     * @see javax.jcr.nodetype.NodeTypeManager#unregisterNodeTypes(String[])
     */
    public void unregisterNodeTypes(SessionInfo sessionInfo, Name[] nodeTypeNames) throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException, RepositoryException;

    //-----------------------------------------------< Workspace Management >---
    /**
     * Create a new workspace with the specified <code>name</code>. If
     * <code>srcWorkspaceName</code> isn't <code>null</code> the content of
     * that workspace is 'cloned' to the new workspace as inital content,
     * otherwise an empty workspace will be created.
     *
     * @param sessionInfo
     * @param name The name of the new workspace.
     * @param srcWorkspaceName The name of the workspace from which the initial
     * content of the new workspace will be 'cloned'.
     * @throws AccessDeniedException
     * @throws UnsupportedRepositoryOperationException
     * @throws NoSuchWorkspaceException
     * @throws RepositoryException
     * @see javax.jcr.Workspace#createWorkspace(String)
     * @see javax.jcr.Workspace#createWorkspace(String, String)
     * @since JCR 2.0
     */
    public void createWorkspace(SessionInfo sessionInfo, String name, String srcWorkspaceName) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException;

    /**
     * Deletes the workspace with the specified <code>name</code>.
     *
     * @param sessionInfo
     * @param name  The name of the workspace to be deleted.
     * @throws AccessDeniedException
     * @throws UnsupportedRepositoryOperationException
     * @throws NoSuchWorkspaceException
     * @throws RepositoryException
     * @see javax.jcr.Workspace#deleteWorkspace(String)
     * @since JCR 2.0
     */
    public void deleteWorkspace(SessionInfo sessionInfo, String name) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException;

}
