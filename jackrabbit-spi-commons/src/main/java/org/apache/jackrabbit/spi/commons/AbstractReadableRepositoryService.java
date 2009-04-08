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

import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Subscription;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.spi.commons.nodetype.compact.QNodeTypeDefinitionsBuilderImpl;
import org.apache.jackrabbit.spi.commons.nodetype.compact.ParseException;

import javax.jcr.RepositoryException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ValueFormatException;
import javax.jcr.AccessDeniedException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ItemExistsException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.MergeException;
import javax.jcr.NamespaceException;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.ConstraintViolationException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/**
 * <code>AbstractReadableRepositoryService</code> provides an abstract base
 * class where all methods that attempt to write throw an
 * {@link UnsupportedRepositoryOperationException}. This class useful for
 * repository service implementation that only provide read access to the
 * underlying content.
 */
public abstract class AbstractReadableRepositoryService
        extends AbstractRepositoryService {

    protected static final Set WRITE_ACTIONS = new HashSet(Arrays.asList(
            new String[]{"add_node", "set_property", "remove"}));

    /**
     * The repository descriptors.
     */
    protected final Map descriptors;

    /**
     * The fixed set of namespaces known to the repository service.
     */
    protected final NamespaceMapping namespaces = new NamespaceMapping();

    /**
     * The fixed set of node type definitions known to the repository service.
     */
    protected final Map nodeTypeDefs = new HashMap();

    /**
     * The node definition of the root node.
     */
    protected QNodeDefinition rootNodeDefinition;

    /**
     * The list of workspaces that this repository service exposes.
     */
    protected final List wspNames;

    /**
     * Creates a new <code>AbstractReadableRepositoryService</code>.
     *
     * @param descriptors the repository descriptors. Maps descriptor keys to
     *                    descriptor values.
     * @param namespaces  the namespaces. Maps namespace prefixes to namespace
     *                    URIs.
     * @param cnd         a reader on the compact node type definition.
     * @param wspNames    a list of workspace names.
     * @throws RepositoryException if the namespace mappings are invalid.
     * @throws ParseException      if an error occurs while parsing the CND.
     */
    public AbstractReadableRepositoryService(Map descriptors,
                                             Map namespaces,
                                             Reader cnd,
                                             List wspNames)
            throws RepositoryException, ParseException {
        this.descriptors = Collections.unmodifiableMap(new HashMap(descriptors));
        for (Iterator it = namespaces.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            this.namespaces.setMapping((String) entry.getKey(),
                    (String) entry.getValue());
        }
        CompactNodeTypeDefReader reader = new CompactNodeTypeDefReader(
                cnd, "", this.namespaces, new QNodeTypeDefinitionsBuilderImpl());
        for (Iterator it = reader.getNodeTypeDefs().iterator(); it.hasNext(); ) {
            QNodeTypeDefinition def = (QNodeTypeDefinition) it.next();
            nodeTypeDefs.put(def.getName(), def);
        }
        this.wspNames = Collections.unmodifiableList(new ArrayList(wspNames));
    }

    //---------------------------< subclass responsibility >--------------------

    /**
     * Create the root node definition.
     *
     * @return the root node definition for a workspace.
     * @throws RepositoryException if an error occurs.
     */
    protected abstract QNodeDefinition createRootNodeDefinition()
            throws RepositoryException;

    /**
     * Checks if the given <code>credentials</code> are valid.
     *
     * @param credentials the credentials to check.
     * @param workspaceName the workspace to access.
     * @throws LoginException if the credentials are invalid.
     */
    protected abstract void checkCredentials(Credentials credentials,
                                             String workspaceName)
            throws LoginException;

    //---------------------< may be overwritten by subclasses>------------------

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

    /**
     * Creates a session info instance for the given <code>userId</code> and
     * <code>workspaceName</code>. This default implementation creates a
     * {@link SessionInfoImpl} instance and sets the userId and workspaceName.
     *
     * @param userId the userId.
     * @param workspaceName the name of the workspace to access.
     * @return a session info instance for the given <code>userId</code> and
     *         <code>workspaceName</code>.
     */
    protected SessionInfo createSessionInfo(String userId,
                                            String workspaceName) {
        SessionInfoImpl s = new SessionInfoImpl();
        s.setUserID(userId);
        s.setWorkspacename(workspaceName);
        return s;
    }

    //----------------------------< login >-------------------------------------

    /**
     * This default implementation does:
     * <ul>
     * <li>calls {@link #checkCredentials(Credentials, String)}</li>
     * <li>checks if the given <code>workspaceName</code> is in
     * {@link #wspNames} otherwise throws a {@link NoSuchWorkspaceException}.</li>
     * <li>calls {@link #createSessionInfo(String, String)} with a <code>null</code>
     * <code>userId</code> or the <code>userId</code> from <code>credentials</code>
     * if it is of type {@link SimpleCredentials}.</li>
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
        if (!wspNames.contains(workspaceName)) {
            throw new NoSuchWorkspaceException(workspaceName);
        }
        String userId = null;
        if (credentials instanceof SimpleCredentials) {
            userId = ((SimpleCredentials) credentials).getUserID();
        }
        return createSessionInfo(userId, workspaceName);
    }

    /**
     * This default implementation returns the session info retuned by the call
     * to {@link #createSessionInfo(String, String)} with the userId taken
     * from the passed <code>sessionInfo</code>.
     */
    public SessionInfo obtain(SessionInfo sessionInfo, String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return createSessionInfo(sessionInfo.getUserID(), workspaceName);
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

    //-----------------------------< reading >----------------------------------

    /**
     * This default implementation returns an iterator over the item infos
     * returned by the call to {@link #getNodeInfo(SessionInfo, NodeId)}.
     */
    public Iterator getItemInfos(SessionInfo sessionInfo, NodeId nodeId) throws
            ItemNotFoundException, RepositoryException {
        return Collections.singleton(getNodeInfo(sessionInfo, nodeId)).iterator();
    }

    //--------------------------< descriptors >---------------------------------

    /**
     * This default implementation returns the descriptors that were passed
     * to the constructor of this repository service.
     */
    public Map getRepositoryDescriptors() throws RepositoryException {
        return descriptors;
    }

    //-------------------------< workspace names >------------------------------

    /**
     * This default implementation first calls {@link #checkSessionInfo(SessionInfo)}
     * with the <code>sessionInfo</code>, then returns the workspaces that were
     * passed to the constructor of this repository service.
     */
    public String[] getWorkspaceNames(SessionInfo sessionInfo) throws
            RepositoryException {
        checkSessionInfo(sessionInfo);
        return (String[]) wspNames.toArray(new String[wspNames.size()]);
    }

    //-------------------------< access control >-------------------------------

    /**
     * This default implementation first calls {@link #checkSessionInfo(SessionInfo)}
     * with the <code>sessionInfo</code>, then returns <code>false</code> if
     * the any of the <code>actions</code> are in {@link #WRITE_ACTIONS};
     * otherwise returns <code>true</code>.
     */
    public boolean isGranted(SessionInfo sessionInfo,
                             ItemId itemId,
                             String[] actions) throws RepositoryException {
        checkSessionInfo(sessionInfo);
        // deny all but read
        for (int i = 0; i < actions.length; i++) {
            if (WRITE_ACTIONS.contains(actions[i])) {
                return false;
            }
        }
        return true;
    }

    //-----------------------------< node types >-------------------------------

    /**
     * This default implementation first calls {@link #checkSessionInfo(SessionInfo)}
     * with the <code>sessionInfo</code>,
     */
    public Iterator getQNodeTypeDefinitions(SessionInfo sessionInfo) throws
            RepositoryException {
        checkSessionInfo(sessionInfo);
        return nodeTypeDefs.values().iterator();
    }

    /**
     * This default implementation first calls {@link #checkSessionInfo(SessionInfo)}
     * with the <code>sessionInfo</code>, then gathers the {@link QNodeTypeDefinition}s
     * with the given <code>nodetypeNames</code>. If one of the nodetypeNames
     * is not a valid node type definition then a {@link RepositoryException}
     * is thrown.
     */
    public Iterator getQNodeTypeDefinitions(SessionInfo sessionInfo, Name[] nodetypeNames)
            throws RepositoryException {
        checkSessionInfo(sessionInfo);
        List ntDefs = new ArrayList();
        for (int i = 0; i < nodetypeNames.length; i++) {
            Object def = nodeTypeDefs.get(nodetypeNames[i]);
            if (def == null) {
                throw new RepositoryException("unknown node type: "
                        + nodetypeNames[i]);
            }
            ntDefs.add(def);
        }
        return ntDefs.iterator();
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
                    rootNodeDefinition = createRootNodeDefinition();
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

    //-----------------------------< namespaces >-------------------------------

    /**
     * This default implementation first calls {@link #checkSessionInfo(SessionInfo)}
     * with the <code>sessionInfo</code>, then returns the prefix to namespace
     * URL mapping that was provided in the constructor of this repository
     * service.
     */
    public Map getRegisteredNamespaces(SessionInfo sessionInfo) throws
            RepositoryException {
        checkSessionInfo(sessionInfo);
        return namespaces.getPrefixToURIMapping();
    }

    /**
     * This default implementation first calls {@link #checkSessionInfo(SessionInfo)}
     * with the <code>sessionInfo</code>, then returns the namepsace URI for the
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
     * @throws UnsupportedRepositoryOperationException always.
     */
    public Iterator merge(SessionInfo sessionInfo,
                          NodeId nodeId,
                          String srcWorkspaceName,
                          boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
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
    public void dispose(Subscription subscription) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    //----------------------< namespace registry >------------------------------

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

    //-------------------------------< query >----------------------------------

    public String[] getSupportedQueryLanguages(SessionInfo sessionInfo) throws
            RepositoryException {
        checkSessionInfo(sessionInfo);
        return new String[0];
    }

    public void checkQueryStatement(SessionInfo sessionInfo, String statement,
                                    String language, Map namespaces) throws
            InvalidQueryException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public QueryInfo executeQuery(SessionInfo sessionInfo, String statement,
                                  String language, Map namespaces) throws
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }
}
