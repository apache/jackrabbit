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
import org.apache.jackrabbit.name.Path;

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
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.query.InvalidQueryException;
import java.util.Map;
import java.util.Collection;
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
    public Map getRepositoryDescriptors() throws RepositoryException;

    //-----------------------------------< SessionInfo creation and release >---
    /**
     * Returns a <code>SessionInfo</code> that will be used by other methods
     * on the <code>RepositoryService</code>.
     * An implementation may choose to authenticate the user using the supplied
     * <code>credentials</code>.
     *
     * @param credentials the credentials of the user.
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
     * @return a <code>SessionInfo</code> if authentication was successful.
     * @throws LoginException           if authentication of the user fails.
     * @throws NoSuchWorkspaceException if the specified <code>workspaceName</code>
     *                                  is not recognized.
     * @throws RepositoryException      if an error occurs.
     */
    public SessionInfo obtain(SessionInfo sessionInfo, String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException;

    /**
     * Indicates to the <code>RepositoryService</code>, that the given SessionInfo
     * will not be used any more.
     *
     * @param sessionInfo
     */
    public void dispose(SessionInfo sessionInfo) throws RepositoryException;

    //--------------------------------------------------------------------------
    /**
     * @param sessionInfo
     * @return an array of workspace ids
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Workspace#getAccessibleWorkspaceNames()
     * @see javax.jcr.Workspace#getName()
     */
    public String[] getWorkspaceNames(SessionInfo sessionInfo) throws RepositoryException;

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
     * characteristics. If the root node can be identified with a unique ID the
     * returned <code>NodeId</code> simply has a uniqueID part and the path
     * part is <code>null</code>. If the root node cannot be identified with a
     * unique ID the uniqueID part is <code>null</code> and the path part will be set
     * to "/".
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
    public NodeInfo getNodeInfo(SessionInfo sessionInfo, NodeId nodeId) throws ItemNotFoundException, RepositoryException;

    /**
     * Returns a collection of child node entries present on the
     * Node represented by the given parentId.
     *
     * @param sessionInfo
     * @param parentId
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public Collection getChildInfos(SessionInfo sessionInfo, NodeId parentId) throws ItemNotFoundException, RepositoryException;

    /**
     * @param sessionInfo
     * @param propertyId
     * @return
     * @throws javax.jcr.ItemNotFoundException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Session#getItem(String)
     * @see javax.jcr.Node#getProperty(String)
     */
    public PropertyInfo getPropertyInfo(SessionInfo sessionInfo, PropertyId propertyId) throws ItemNotFoundException, RepositoryException;

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
    public void submit(Batch batch) throws PathNotFoundException, ItemNotFoundException, NoSuchNodeTypeException, ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

    //-------------------------------------------------------------< Import >---
    /**
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
    public void move(SessionInfo sessionInfo, NodeId srcNodeId, NodeId destParentNodeId, QName destName) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

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
    public void copy(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, QName destName) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException;

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
    public void update(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException;

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
    public void clone(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, QName destName, boolean removeExisting) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException;

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
     * @param sessionScoped
     * @return returns the <code>LockInfo</code> associated with this lock.
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#lock(boolean, boolean)
     */
    public LockInfo lock(SessionInfo sessionInfo, NodeId nodeId, boolean deep, boolean sessionScoped) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException;

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
    public void refreshLock(SessionInfo sessionInfo, NodeId nodeId) throws LockException, RepositoryException;

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
    public void unlock(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException;

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
    public void checkin(SessionInfo sessionInfo, NodeId nodeId) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException;

    /**
     * @param sessionInfo
     * @param nodeId
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#checkout()
     */
    public void checkout(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, LockException, RepositoryException;

    /**
     *
     * @param sessionInfo
     * @param versionHistoryId
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
    public void restore(SessionInfo sessionInfo, NodeId nodeId, NodeId versionId, boolean removeExisting) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException;

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
    public void restore(SessionInfo sessionInfo, NodeId[] versionIds, boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * @param sessionInfo
     * @param nodeId
     * @param srcWorkspaceName
     * @param bestEffort
     * @return an <code>IdIterator</code> over all nodes that received a merge
     * result of "fail" in the course of this operation.
     * @throws javax.jcr.NoSuchWorkspaceException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.MergeException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.InvalidItemStateException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#merge(String, boolean)
     */
    public IdIterator merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException;

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
    public void resolveMergeConflict(SessionInfo sessionInfo, NodeId nodeId, NodeId[] mergeFailedIds, NodeId[] predecessorIds) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * @param sessionInfo
     * @param versionId
     * @param label
     * @param moveLabel
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.version.VersionHistory#addVersionLabel(String, String, boolean)
     */
    public void addVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, QName label, boolean moveLabel) throws VersionException, RepositoryException;

    /**
     * @param sessionInfo
     * @param versionId
     * @param label
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.version.VersionHistory#removeVersionLabel(String)
     */
    public void removeVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, QName label) throws VersionException, RepositoryException;

    //----------------------------------------------------------< Searching >---
    /**
     * @param sessionInfo
     * @return
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.query.QueryManager#getSupportedQueryLanguages()
     */
    public String[] getSupportedQueryLanguages(SessionInfo sessionInfo) throws RepositoryException;

    /**
     * Checks if the query <code>statement</code> is valid according to the
     * specified query <code>language</code>.
     *
     * @param sessionInfo the session info.
     * @param statement   the query statement to check.
     * @param language    the query language.
     * @param namespaces  the locally re-mapped namespace which may be used in
     *                    the query <code>statement</code>.
     * @throws InvalidQueryException if the query statement is invalid or the
     *                               language is not supported.
     * @throws RepositoryException   if an error occurs while checking the
     *                               statement.
     * @see javax.jcr.query.QueryManager#createQuery(String, String)
     */
    public void checkQueryStatement(SessionInfo sessionInfo, String statement, String language, Map namespaces) throws InvalidQueryException, RepositoryException;

    /**
     * @param sessionInfo
     * @param statement
     * @param language
     * @param namespaces  the locally re-mapped namespace which may be used in
     *                    the query <code>statement</code>.
     * @return
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.query.Query#execute()
     */
    public QueryInfo executeQuery(SessionInfo sessionInfo, String statement, String language, Map namespaces) throws RepositoryException;

    //--------------------------------------------------------< Observation >---

    /**
     * Creates an event filter. If the repository supportes observation, the
     * filter created is based on the parameters available in {@link
     * javax.jcr.observation.ObservationManager#addEventListener}.<p/>
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
                                         String[] uuid, QName[] nodeTypeName,
                                         boolean noLocal)
            throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Retrieves the events that occurred since the last call to this
     * method. When this method returns without an exception the bundle
     * identfier in <code>sessionInfo</code> will be updated to reference the
     * most recent event bundle returned by this call. In case an empty array is
     * supplied as event filters it may also happen that the bundle identifier
     * is updated even though no event bundle had been returned.
     * <p/>
     * An implementation is required to accept at least event filter instances
     * created by {@link #createEventFilter}. Optionally an implementation may
     * also support event filters instanciated by the client itself. An
     * implementation may require special deployment in that case, e.g. to make
     * the event filter implementation class available to the repository
     * server.<p/>
     * Note, that an SPI implementation may support observation even if
     * the corresponding {@link javax.jcr.Repository#OPTION_OBSERVATION_SUPPORTED repository descriptor}
     * does return 'false'.
     *
     * @param sessionInfo the session info.
     * @param timeout     a timeout in milliseconds to wait at most for an
     *                    event bundle. If <code>timeout</code> is up
     *                    and no event occurred meanwhile an empty array is
     *                    returned.
     * @param filters     the filters that are applied to the events as
     *                    they occurred on the repository. An event is included
     *                    in an event bundle if it is {@link EventFilter#accept(Event, boolean)
     *                    accept}ed by at least one of the supplied filters. If
     *                    an empty array is passed none of the potential events
     *                    are include in an event bundle. This allows a client
     *                    to skip or ignore events for a certain period of time.
     *                    If <code>null</code> is passed no filtering is done
     *                    and all events are included in the event bundle.
     * @return an array of <code>EventBundle</code>s representing the external
     *         events that occurred.
     * @throws RepositoryException  if an error occurs while retrieving the
     *                              event bundles or the currently set bundle
     *                              identifier in <code>sessionInfo</code>
     *                              references an unknown or outdated event
     *                              bundle.
     * @throws UnsupportedRepositoryOperationException If this SPI implementation
     * does not support observation.
     * @throws InterruptedException if the calling thread is interrupted while
     * waiting for events within the specified <code>timeout</code>.
     */
    public EventBundle[] getEvents(SessionInfo sessionInfo, long timeout,
                                   EventFilter[] filters)
            throws RepositoryException, UnsupportedRepositoryOperationException, InterruptedException;

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
     * @see javax.jcr.NamespaceRegistry#getPrefix(String)
     * @see javax.jcr.NamespaceRegistry#getURI(String)
     */
    public Map getRegisteredNamespaces(SessionInfo sessionInfo) throws RepositoryException;

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
