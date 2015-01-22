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
package org.apache.jackrabbit.spi2jcr;

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.spi.ItemInfoCache;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Subscription;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.Tree;
import org.apache.jackrabbit.spi.commons.EventFilterImpl;
import org.apache.jackrabbit.spi.commons.EventBundleImpl;
import org.apache.jackrabbit.spi.commons.ItemInfoCacheImpl;
import org.apache.jackrabbit.spi.commons.QPropertyDefinitionImpl;
import org.apache.jackrabbit.spi.commons.QNodeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.iterator.Iterators;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.nodetype.NodeTypeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.privilege.PrivilegeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.spi.commons.value.ValueFactoryQImpl;
import org.apache.jackrabbit.JcrConstants;

import javax.jcr.RepositoryException;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ValueFormatException;
import javax.jcr.AccessDeniedException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ItemExistsException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.MergeException;
import javax.jcr.NamespaceException;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.NodeIterator;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Workspace;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Value;
import javax.jcr.ItemVisitor;
import javax.jcr.ValueFactory;
import javax.jcr.GuestCredentials;
import javax.jcr.PropertyIterator;
import javax.jcr.security.Privilege;
import javax.jcr.util.TraversingItemVisitor;
import javax.jcr.observation.ObservationManager;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventJournal;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryManager;
import javax.jcr.query.Query;
import javax.jcr.lock.LockException;
import javax.jcr.lock.Lock;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.nodetype.NodeTypeDefinition;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.security.AccessControlException;

/**
 * <code>RepositoryServiceImpl</code> implements a repository service on top
 * of a JCR Repository.
 */
public class RepositoryServiceImpl implements RepositoryService {

    /**
     * The JCR Repository.
     */
    private final Repository repository;

    /**
     * The configuration map used to determine the maximal depth of child
     * items to be accessed upon a call to {@link #getNodeInfo(SessionInfo, NodeId)}.
     */
    private final BatchReadConfig batchReadConfig;

    /**
     * The id factory.
     */
    private final IdFactoryImpl idFactory = (IdFactoryImpl) IdFactoryImpl.getInstance();

    /**
     * The QValueFactory
     */
    private QValueFactory qValueFactory = QValueFactoryImpl.getInstance();

    /**
     * Set to <code>true</code> if the underlying JCR repository supports
     * observation.
     */
    private final boolean supportsObservation;

    private final int itemInfoCacheSize;

    public RepositoryServiceImpl(Repository repository, BatchReadConfig batchReadConfig) {
        this(repository, batchReadConfig, ItemInfoCacheImpl.DEFAULT_CACHE_SIZE);
    }

    /**
     * Creates a new repository service based on the given
     * <code>repository</code>.
     *
     * @param repository a JCR repository instance.
     * @param batchReadConfig
     * {@link #getNodeInfo(SessionInfo, NodeId)}.
     */
    public RepositoryServiceImpl(Repository repository, BatchReadConfig batchReadConfig, int itemInfoCacheSize) {
        this.repository = repository;
        this.batchReadConfig = batchReadConfig;
        this.supportsObservation = "true".equals(repository.getDescriptor(Repository.OPTION_OBSERVATION_SUPPORTED));
        this.itemInfoCacheSize = itemInfoCacheSize;

        try {
            Session s = repository.login(new GuestCredentials());
            ValueFactory vf = s.getValueFactory();
            if (vf instanceof ValueFactoryQImpl) {
                qValueFactory = ((ValueFactoryQImpl) vf).getQValueFactory();
            }
        } catch (RepositoryException e) {
            // ignore
        }
    }

    /**
     * {@inheritDoc}
     */
    public IdFactory getIdFactory() {
        return idFactory;
    }

    /**
     * {@inheritDoc}
     */
    public NameFactory getNameFactory() {
        return NameFactoryImpl.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public PathFactory getPathFactory() {
        return PathFactoryImpl.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public QValueFactory getQValueFactory() {
        return qValueFactory;
    }

    /**
     * {@inheritDoc}
     */
    public ItemInfoCache getItemInfoCache(SessionInfo sessionInfo) throws RepositoryException {
        return new ItemInfoCacheImpl(itemInfoCacheSize);
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, QValue[]> getRepositoryDescriptors() throws RepositoryException {
        Map<String, QValue[]> descriptors = new HashMap<String, QValue[]>();
        for (String key : repository.getDescriptorKeys()) {
            if (key.equals(Repository.OPTION_TRANSACTIONS_SUPPORTED)) {
                descriptors.put(key, new QValue[] {qValueFactory.create(false)});
            } else {
                Value[] vs = repository.getDescriptorValues(key);
                QValue[] qvs = new QValue[vs.length];
                for (int i = 0; i < vs.length; i++) {
                    // Name and path resolver that uses a dummy namespace resolver
                    // as Name/Path values are not expected to occur in the
                    // descriptors. TODO: check again.
                    NamePathResolver resolver = new DefaultNamePathResolver(new NamespaceResolver() {
                        public String getURI(String prefix) {
                            return prefix;
                        }
                        public String getPrefix(String uri) {
                            return uri;
                        }
                    });
                    qvs[i] = ValueFormat.getQValue(vs[i], resolver, qValueFactory);
                }
                descriptors.put(key, qvs);
            }
        }
        return descriptors;
    }

    /**
     * {@inheritDoc}
     */
    public SessionInfo obtain(Credentials credentials, String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        Credentials duplicate = SessionInfoImpl.duplicateCredentials(credentials);
        return new SessionInfoImpl(repository.login(credentials, workspaceName), duplicate, getNameFactory(), getPathFactory());
    }

    /**
     * {@inheritDoc}
     */
    public SessionInfo obtain(SessionInfo sessionInfo, String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        Session s = repository.login(sInfo.getCredentials(), workspaceName);
        return new SessionInfoImpl(s, sInfo.getCredentials(), getNameFactory(), getPathFactory());
    }

    /**
     * {@inheritDoc}
     */
    public SessionInfo impersonate(SessionInfo sessionInfo, Credentials credentials) throws LoginException, RepositoryException {
        Credentials duplicate = SessionInfoImpl.duplicateCredentials(credentials);
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        return new SessionInfoImpl(sInfo.getSession().impersonate(credentials), duplicate, getNameFactory(), getPathFactory());
    }

    /**
     * {@inheritDoc}
     */
    public void dispose(SessionInfo sessionInfo) throws RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        for (EventSubscription s : sInfo.getSubscriptions()) {
            s.dispose();
        }
        sInfo.getSession().logout();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getWorkspaceNames(SessionInfo sessionInfo)
            throws RepositoryException {
        Session s = getSessionInfoImpl(sessionInfo).getSession();
        return s.getWorkspace().getAccessibleWorkspaceNames();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isGranted(SessionInfo sessionInfo,
                             ItemId itemId,
                             String[] actions) throws RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        String path = pathForId(itemId, sInfo);

        try {
            String actStr;
            if (actions.length == 1) {
                actStr = actions[0];
            } else {
                String comma = "";
                actStr = "";
                for (String action : actions) {
                    actStr += comma;
                    actStr += action;
                    comma = ",";
                }
            }
            sInfo.getSession().checkPermission(path, actStr);
            return true;
        } catch (AccessControlException e) {
            return false;
        }
    }

    @Override
    public PrivilegeDefinition[] getPrivilegeDefinitions(SessionInfo sessionInfo) throws RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        Session session = sInfo.getSession();
        Workspace wsp = session.getWorkspace();
        Collection<Privilege> privs;
        if (wsp instanceof JackrabbitWorkspace) {
            privs = Arrays.asList(((JackrabbitWorkspace) wsp).getPrivilegeManager().getRegisteredPrivileges());
        } else {
            Privilege jcrAll = session.getAccessControlManager().privilegeFromName(Privilege.JCR_ALL);
            privs = new HashSet<Privilege>();
            privs.add(jcrAll);
            for (Privilege p : jcrAll.getAggregatePrivileges()) {
                privs.add(p);
            }
        }

        PrivilegeDefinition[] pDefs = new PrivilegeDefinition[privs.size()];
        NamePathResolver npResolver = sInfo.getNamePathResolver();
        int i = 0;
        for (Privilege p : privs) {
            Set<Name> aggrnames = null;
            if (p.isAggregate()) {
                aggrnames = new HashSet<Name>();
                for (Privilege dap : p.getDeclaredAggregatePrivileges()) {
                    aggrnames.add(npResolver.getQName(dap.getName()));
                }
            }
            PrivilegeDefinition def = new PrivilegeDefinitionImpl(npResolver.getQName(p.getName()), p.isAbstract(), aggrnames);
            pDefs[i++] = def;
        }
        return pDefs;
    }

    @Override
    public Name[] getPrivilegeNames(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        String path = (nodeId == null) ? null : pathForId(nodeId, sInfo);
        NamePathResolver npResolver = sInfo.getNamePathResolver();
        
        Privilege[] privs = sInfo.getSession().getAccessControlManager().getPrivileges(path);
        List<Name> names = new ArrayList<Name>(privs.length);
        for (Privilege priv : privs) {
            names.add(npResolver.getQName(priv.getName()));
        }
        return names.toArray(new Name[names.size()]);
    }
    
    public PrivilegeDefinition[] getSupportedPrivileges(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        String path = (nodeId == null) ? null : pathForId(nodeId, sInfo);

        Privilege[] privs = sInfo.getSession().getAccessControlManager().getSupportedPrivileges(path);
        PrivilegeDefinition[] pDefs = new PrivilegeDefinition[privs.length];
        NamePathResolver npResolver = sInfo.getNamePathResolver();
        for (int i = 0; i < privs.length; i++) {
            Set<Name> aggrnames = null;
            if (privs[i].isAggregate()) {
                aggrnames = new HashSet<Name>();
                for (Privilege dap : privs[i].getDeclaredAggregatePrivileges()) {
                    aggrnames.add(npResolver.getQName(dap.getName()));
                }
            }
            PrivilegeDefinition def = new PrivilegeDefinitionImpl(npResolver.getQName(privs[i].getName()), privs[i].isAbstract(), aggrnames);
            pDefs[i] = def;
        }
        return pDefs;
    }

    /**
     * {@inheritDoc}
     */
    public QNodeDefinition getNodeDefinition(SessionInfo sessionInfo,
                                             NodeId nodeId)
            throws RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        try {
            return new QNodeDefinitionImpl(getNode(nodeId, sInfo).getDefinition(),
                    sInfo.getNamePathResolver());
        } catch (NameException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public QPropertyDefinition getPropertyDefinition(SessionInfo sessionInfo,
                                                     PropertyId propertyId)
            throws RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        try {
            return new QPropertyDefinitionImpl(
                    getProperty(propertyId, sInfo).getDefinition(),
                    sInfo.getNamePathResolver(),
                    getQValueFactory());
        } catch (NameException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeInfo getNodeInfo(SessionInfo sessionInfo, NodeId nodeId)
            throws ItemNotFoundException, RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        Node node = getNode(nodeId, sInfo);
        try {
            return new NodeInfoImpl(node, idFactory, sInfo.getNamePathResolver());
        } catch (NameException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<? extends ItemInfo> getItemInfos(SessionInfo sessionInfo, ItemId itemId)
            throws ItemNotFoundException, RepositoryException {

        if (!itemId.denotesNode()) {
            PropertyId propertyId = (PropertyId) itemId;
            PropertyInfo propertyInfo = getPropertyInfo(sessionInfo, propertyId);
            return Iterators.singleton(propertyInfo);
        }
        else {
            NodeId nodeId = (NodeId) itemId;
            final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
            Node node = getNode(nodeId, sInfo);
            Name ntName = null;
            try {
                ntName = sInfo.getNamePathResolver().getQName(node.getProperty(JcrConstants.JCR_PRIMARYTYPE).getString());
            } catch (NameException e) {
                // ignore. should never occur
            }
            int depth = batchReadConfig.getDepth(ntName);
            if (depth == BatchReadConfig.DEPTH_DEFAULT) {
                NodeInfo info;
                try {
                    info = new NodeInfoImpl(node, idFactory, sInfo.getNamePathResolver());
                } catch (NameException e) {
                    throw new RepositoryException(e);
                }
                return Collections.singletonList(info).iterator();
            } else {
                final List<ItemInfo> itemInfos = new ArrayList<ItemInfo>();
                ItemVisitor visitor = new TraversingItemVisitor(false, depth) {
                    @Override
                    protected void entering(Property property, int i) throws RepositoryException {
                        try {
                            itemInfos.add(new PropertyInfoImpl(property, idFactory, sInfo.getNamePathResolver(), getQValueFactory()));
                        } catch (NameException e) {
                            throw new RepositoryException(e);
                        }
                    }
                    @Override
                    protected void entering(Node node, int i) throws RepositoryException {
                        try {
                            itemInfos.add(new NodeInfoImpl(node, idFactory, sInfo.getNamePathResolver()));
                        } catch (NameException e) {
                            throw new RepositoryException(e);
                        }
                    }
                    @Override
                    protected void leaving(Property property, int i) {
                        // nothing to do
                    }
                    @Override
                    protected void leaving(Node node, int i) {
                        // nothing to do
                    }
                };
                visitor.visit(node);
                return itemInfos.iterator();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<ChildInfo> getChildInfos(SessionInfo sessionInfo, NodeId parentId)
            throws ItemNotFoundException, RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        NodeIterator children = getNode(parentId, sInfo).getNodes();
        List<ChildInfo> childInfos = new ArrayList<ChildInfo>();
        try {
            while (children.hasNext()) {
                childInfos.add(new ChildInfoImpl(children.nextNode(),
                        sInfo.getNamePathResolver()));
            }
        } catch (NameException e) {
            throw new RepositoryException(e);
        }
        return childInfos.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<PropertyId> getReferences(SessionInfo sessionInfo, NodeId nodeId, Name propertyName, boolean weakReferences) throws ItemNotFoundException, RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        Node node = getNode(nodeId, sInfo);
        String jcrName = (propertyName == null) ? null : sInfo.getNamePathResolver().getJCRName(propertyName);

        List<PropertyId> ids = new ArrayList<PropertyId>();
        PropertyIterator it;
        if (weakReferences) {
            it = node.getWeakReferences(jcrName);
        } else {
            it = node.getReferences(jcrName);
        }
        while (it.hasNext()) {
            ids.add(idFactory.createPropertyId(it.nextProperty(), sInfo.getNamePathResolver()));
        }
        return ids.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public PropertyInfo getPropertyInfo(SessionInfo sessionInfo,
                                        PropertyId propertyId)
            throws ItemNotFoundException, RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        try {
            return new PropertyInfoImpl(getProperty(propertyId, sInfo), idFactory,
                    sInfo.getNamePathResolver(), getQValueFactory());
        } catch (NameException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Batch createBatch(SessionInfo sessionInfo, ItemId itemId)
            throws RepositoryException {
        return new BatchImpl(getSessionInfoImpl(sessionInfo));
    }

    /**
     * {@inheritDoc}
     */
    public void submit(Batch batch) throws PathNotFoundException, ItemNotFoundException, NoSuchNodeTypeException, ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        if (batch instanceof BatchImpl) {
            ((BatchImpl) batch).end();
        } else {
            throw new RepositoryException("Unknown Batch implementation: " +
                    batch.getClass().getName());
        }
    }

    @Override
    public Tree createTree(SessionInfo sessionInfo, Batch batch, Name nodeName, Name primaryTypeName, String uniqueId) throws RepositoryException {
        return new XmlTree(nodeName, primaryTypeName, uniqueId, getSessionInfoImpl(sessionInfo).getNamePathResolver());
    }

    /**
     * {@inheritDoc}
     */
    public void importXml(final SessionInfo sessionInfo,
                          final NodeId parentId,
                          final InputStream xmlStream,
                          final int uuidBehaviour) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                String path = pathForId(parentId, sInfo);
                try {
                    sInfo.getSession().getWorkspace().importXML(path, xmlStream, uuidBehaviour);
                } catch (IOException e) {
                    throw new RepositoryException(e.getMessage(), e);
                }
                return null;
            }
        }, sInfo);
    }

    /**
     * {@inheritDoc}
     */
    public void move(final SessionInfo sessionInfo,
                     final NodeId srcNodeId,
                     final NodeId destParentNodeId,
                     final Name destName) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                String srcPath = pathForId(srcNodeId, sInfo);
                StringBuffer destPath = new StringBuffer(pathForId(destParentNodeId, sInfo));
                if (destPath.length() > 1) {
                    destPath.append("/");
                }
                destPath.append(sInfo.getNamePathResolver().getJCRName(destName));
                sInfo.getSession().getWorkspace().move(srcPath, destPath.toString());
                return null;
            }
        }, sInfo);
    }

    /**
     * {@inheritDoc}
     */
    public void copy(final SessionInfo sessionInfo,
                     final String srcWorkspaceName,
                     final NodeId srcNodeId,
                     final NodeId destParentNodeId,
                     final Name destName) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                Workspace ws = sInfo.getSession().getWorkspace();
                String destPath = getDestinationPath(destParentNodeId, destName, sInfo);
                if (ws.getName().equals(srcWorkspaceName)) {
                    // inner-workspace copy
                    String srcPath = pathForId(srcNodeId, sInfo);
                    ws.copy(srcPath, destPath);
                } else {
                    SessionInfoImpl srcInfo = getSessionInfoImpl(obtain(sInfo, srcWorkspaceName));
                    try {
                        String srcPath = pathForId(srcNodeId, srcInfo);
                        ws.copy(srcWorkspaceName, srcPath, destPath);
                    } finally {
                        dispose(srcInfo);
                    }
                }
                return null;
            }
        }, sInfo);
    }

    /**
     * {@inheritDoc}
     */
    public void update(final SessionInfo sessionInfo,
                       final NodeId nodeId,
                       final String srcWorkspaceName)
            throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                getNode(nodeId, sInfo).update(srcWorkspaceName);
                return null;
            }
        }, sInfo);
    }

    /**
     * {@inheritDoc}
     */
    public void clone(final SessionInfo sessionInfo,
                      final String srcWorkspaceName,
                      final NodeId srcNodeId,
                      final NodeId destParentNodeId,
                      final Name destName,
                      final boolean removeExisting) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                SessionInfoImpl srcInfo = getSessionInfoImpl(obtain(sessionInfo, srcWorkspaceName));
                try {
                String srcPath = pathForId(srcNodeId, srcInfo);
                String destPath = getDestinationPath(destParentNodeId, destName, sInfo);

                Workspace wsp = sInfo.getSession().getWorkspace();
                wsp.clone(srcWorkspaceName, srcPath, destPath, removeExisting);
                } finally {
                    dispose(srcInfo);
                }
                return null;
            }
        }, sInfo);
    }

    /**
     * {@inheritDoc}
     */
    public LockInfo getLockInfo(SessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        try {
            Lock lock = getNode(nodeId, sInfo).getLock();
            return LockInfoImpl.createLockInfo(lock, idFactory);
        } catch (LockException e) {
            // no lock present on this node.
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public LockInfo lock(final SessionInfo sessionInfo,
                         final NodeId nodeId,
                         final boolean deep,
                         final boolean sessionScoped)
            throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        return (LockInfo) executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                Node n = getNode(nodeId, sInfo);
                Lock lock = n.lock(deep, sessionScoped);
                return LockInfoImpl.createLockInfo(lock, idFactory);
            }
        }, sInfo);
    }

    /**
     * {@inheritDoc}
     */
    public LockInfo lock(SessionInfo sessionInfo, final NodeId nodeId, final boolean deep, final boolean sessionScoped, final long timeoutHint, final String ownerHint) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        return (LockInfo) executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                Node n = getNode(nodeId, sInfo);
                Lock lock;
                javax.jcr.lock.LockManager lMgr = (sInfo.getSession().getWorkspace()).getLockManager();
                lock = lMgr.lock(n.getPath(), deep, sessionScoped, timeoutHint, ownerHint);
                return LockInfoImpl.createLockInfo(lock, idFactory);
            }
        }, sInfo);
    }

    /**
     * {@inheritDoc}
     */
    public void refreshLock(SessionInfo sessionInfo, NodeId nodeId)
            throws LockException, RepositoryException {
        getNode(nodeId, getSessionInfoImpl(sessionInfo)).getLock().refresh();
    }

    /**
     * {@inheritDoc}
     */
    public void unlock(final SessionInfo sessionInfo, final NodeId nodeId)
            throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                getNode(nodeId, sInfo).unlock();
                return null;
            }
        }, sInfo);
    }

    /**
     * {@inheritDoc}
     */
    public NodeId checkin(final SessionInfo sessionInfo, final NodeId nodeId)
            throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        Version newVersion = (Version) executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                return getNode(nodeId, getSessionInfoImpl(sessionInfo)).checkin();
            }
        }, sInfo);
        return idFactory.createNodeId(newVersion);
    }

    /**
     * {@inheritDoc}
     */
    public void checkout(final SessionInfo sessionInfo, final NodeId nodeId)
            throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                getNode(nodeId, sInfo).checkout();
                return null;
            }
        }, sInfo);
    }

    /**
     * {@inheritDoc}
     */
    public void checkout(final SessionInfo sessionInfo, final NodeId nodeId, NodeId activityId) throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        Node activity = (activityId == null) ? null : getNode(activityId, sInfo);
        VersionManager vMgr = sInfo.getSession().getWorkspace().getVersionManager();
        vMgr.setActivity(activity);
        try {
            executeWithLocalEvents(new Callable() {
                public Object run() throws RepositoryException {
                    getNode(nodeId, sInfo).checkout();
                    return null;
                }
            }, sInfo);
        } finally {
            vMgr.setActivity(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeId checkpoint(SessionInfo sessionInfo, final NodeId nodeId) throws UnsupportedRepositoryOperationException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        Version newVersion = (Version) executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                VersionManager vMgr = sInfo.getSession().getWorkspace().getVersionManager();
                return vMgr.checkpoint(getNodePath(nodeId, sInfo));
            }
        }, sInfo);
        return idFactory.createNodeId(newVersion);
    }

    /**
     * {@inheritDoc}
     */
    public NodeId checkpoint(SessionInfo sessionInfo, final NodeId nodeId, final NodeId activityId) throws UnsupportedRepositoryOperationException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        Node activity = (activityId == null) ? null : getNode(activityId, sInfo);
        VersionManager vMgr = sInfo.getSession().getWorkspace().getVersionManager();
        vMgr.setActivity(activity);
        try {
            Version newVersion = (Version) executeWithLocalEvents(new Callable() {
                public Object run() throws RepositoryException {
                    VersionManager vMgr = sInfo.getSession().getWorkspace().getVersionManager();
                    return vMgr.checkpoint(getNodePath(nodeId, sInfo));
                }
            }, sInfo);
            return idFactory.createNodeId(newVersion);
        } finally {
            vMgr.setActivity(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeVersion(final SessionInfo sessionInfo,
                              final NodeId versionHistoryId,
                              final NodeId versionId)
            throws ReferentialIntegrityException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                Node vHistory = getNode(versionHistoryId, sInfo);
                Node version = getNode(versionId, sInfo);
                if (vHistory instanceof VersionHistory) {
                    ((VersionHistory) vHistory).removeVersion(version.getName());
                } else {
                    throw new RepositoryException("versionHistoryId does not reference a VersionHistor node");
                }
                return null;
            }
        }, sInfo);
    }

    /**
     * {@inheritDoc}
     */
    public void restore(final SessionInfo sessionInfo,
                        final NodeId nodeId,
                        final NodeId versionId,
                        final boolean removeExisting) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                Version v = (Version) getNode(versionId, sInfo);
                if (hasNode(sessionInfo, nodeId)) {
                    Node n = getNode(nodeId, sInfo);
                    n.restore(v, removeExisting);
                } else {
                    // restore with rel-Path part
                    Node n = null;
                    Path relPath = null;
                    Path path = nodeId.getPath();
                    if (nodeId.getUniqueID() != null) {
                        n = getNode(idFactory.createNodeId(nodeId.getUniqueID()), sInfo);
                        relPath = (path.isAbsolute()) ? getPathFactory().getRootPath().computeRelativePath(nodeId.getPath()) : path;
                    } else {
                        int degree = 0;
                        while (degree < path.getLength()) {
                            Path ancestorPath = path.getAncestor(degree);
                            NodeId parentId = idFactory.createNodeId(nodeId.getUniqueID(), ancestorPath);
                            if (hasNode(sessionInfo, parentId)) {
                                n = getNode(parentId, sInfo);
                                relPath = ancestorPath.computeRelativePath(path);
                            }
                            degree++;
                        }
                    }
                    if (n == null) {
                        throw new PathNotFoundException("Path not found " + nodeId);
                    } else {
                        n.restore(v, sInfo.getNamePathResolver().getJCRPath(relPath), removeExisting);
                    }
                }
                return null;
            }
        }, sInfo);
    }

    private boolean hasNode(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        try {
            getNode(nodeId, sInfo);
        } catch (ItemNotFoundException e) {
            return false;
        } catch (PathNotFoundException e) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void restore(final SessionInfo sessionInfo,
                        final NodeId[] versionIds,
                        final boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                Version[] versions = new Version[versionIds.length];
                for (int i = 0; i < versions.length; i++) {
                    Node n = getNode(versionIds[i], sInfo);
                    if (n instanceof Version) {
                        versions[i] = (Version) n;
                    } else {
                        throw new RepositoryException(n.getPath() +
                                " does not reference a Version node");
                    }
                }
                sInfo.getSession().getWorkspace().restore(versions, removeExisting);
                return null;
            }
        }, sInfo);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<NodeId> merge(final SessionInfo sessionInfo,
                          final NodeId nodeId,
                          final String srcWorkspaceName,
                          final boolean bestEffort)
            throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        return (Iterator<NodeId>) executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                String nodePath = getNodePath(nodeId, sInfo);
                NodeIterator it = getVersionManager(sInfo).merge(nodePath, srcWorkspaceName, bestEffort);
                List<NodeId> ids = new ArrayList<NodeId>();
                while (it.hasNext()) {
                    ids.add(idFactory.createNodeId(it.nextNode()
                    ));
                }
                return ids.iterator();
            }
        }, sInfo);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<NodeId> merge(final SessionInfo sessionInfo,
                          final NodeId nodeId,
                          final String srcWorkspaceName,
                          final boolean bestEffort,
                          final boolean isShallow)
            throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        return (Iterator<NodeId>) executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                String nodePath = getNodePath(nodeId, sInfo);
                NodeIterator it = getVersionManager(sInfo).merge(nodePath, srcWorkspaceName, bestEffort, isShallow);
                List<NodeId> ids = new ArrayList<NodeId>();
                while (it.hasNext()) {
                    ids.add(idFactory.createNodeId(it.nextNode()
                    ));
                }
                return ids.iterator();
            }
        }, sInfo);
    }

    /**
     * {@inheritDoc}
     */
    public void resolveMergeConflict(final SessionInfo sessionInfo,
                                     final NodeId nodeId,
                                     final NodeId[] mergeFailedIds,
                                     final NodeId[] predecessorIds)
            throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                Node node = getNode(nodeId, sInfo);
                Version version = null;
                boolean cancel;
                NamePathResolver resolver = sInfo.getNamePathResolver();
                List<NodeId> l = Arrays.asList(mergeFailedIds);
                Property mergeFailed = node.getProperty(resolver.getJCRName(NameConstants.JCR_MERGEFAILED));
                for (Value value : mergeFailed.getValues()) {
                    String uuid = value.getString();
                    if (!l.contains(idFactory.createNodeId(uuid))) {
                        version = (Version) sInfo.getSession().getNodeByIdentifier(uuid);
                        break;
                    }
                }

                l = new ArrayList<NodeId>(predecessorIds.length);
                l.addAll(Arrays.asList(predecessorIds));
                Property predecessors = node.getProperty(resolver.getJCRName(NameConstants.JCR_PREDECESSORS));
                for (Value value : predecessors.getValues()) {
                    NodeId vId = idFactory.createNodeId(value.getString());
                    l.remove(vId);
                }
                cancel = l.isEmpty();
                if (cancel) {
                    node.cancelMerge(version);
                } else {
                    node.doneMerge(version);
                }
                return null;
            }
        }, sInfo);
    }

    /**
     * {@inheritDoc}
     */
    public void addVersionLabel(final SessionInfo sessionInfo,
                                final NodeId versionHistoryId,
                                final NodeId versionId,
                                final Name label,
                                final boolean moveLabel) throws VersionException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                String jcrLabel;
                jcrLabel = sInfo.getNamePathResolver().getJCRName(label);
                Node version = getNode(versionId, sInfo);
                Node vHistory = getNode(versionHistoryId, sInfo);
                if (vHistory instanceof VersionHistory) {
                    ((VersionHistory) vHistory).addVersionLabel(
                            version.getName(), jcrLabel, moveLabel);
                } else {
                    throw new RepositoryException("versionHistoryId does not reference a VersionHistory node");
                }
                return null;
            }
        }, sInfo);
    }

    /**
     * {@inheritDoc}
     */
    public void removeVersionLabel(final SessionInfo sessionInfo,
                                   final NodeId versionHistoryId,
                                   final NodeId versionId,
                                   final Name label) throws VersionException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                String jcrLabel;
                jcrLabel = sInfo.getNamePathResolver().getJCRName((label));
                Node vHistory = getNode(versionHistoryId, sInfo);
                if (vHistory instanceof VersionHistory) {
                    ((VersionHistory) vHistory).removeVersionLabel(jcrLabel);
                } else {
                    throw new RepositoryException("versionHistoryId does not reference a VersionHistory node");
                }
                return null;
            }
        }, sInfo);
    }

    /**
     * {@inheritDoc}
     */
    public NodeId createActivity(SessionInfo sessionInfo, final String title) throws UnsupportedRepositoryOperationException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        final VersionManager vMgr = getVersionManager(sInfo);
        Node activity = (Node) executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                return vMgr.createActivity(title);
            }
        }, getSessionInfoImpl(sessionInfo));
        return idFactory.createNodeId(activity);
    }

    /**
     * {@inheritDoc}
     */
    public void removeActivity(SessionInfo sessionInfo, final NodeId activityId) throws UnsupportedRepositoryOperationException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        final VersionManager vMgr = getVersionManager(sInfo);
        executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                vMgr.removeActivity(getNode(activityId, sInfo));
                return null;
            }
        }, getSessionInfoImpl(sessionInfo));
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<NodeId> mergeActivity(SessionInfo sessionInfo, final NodeId activityId) throws UnsupportedRepositoryOperationException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        return (Iterator<NodeId>) executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                Node node = getNode(activityId, sInfo);
                NodeIterator it = getVersionManager(sInfo).merge(node);
                List<NodeId> ids = new ArrayList<NodeId>();
                while (it.hasNext()) {
                    ids.add(idFactory.createNodeId(it.nextNode()
                    ));
                }
                return ids.iterator();
            }
        }, sInfo);
    }

    /**
     * {@inheritDoc}
     */
    public NodeId createConfiguration(SessionInfo sessionInfo, final NodeId nodeId)
            throws UnsupportedRepositoryOperationException, RepositoryException {
        final SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        final VersionManager vMgr = getVersionManager(sInfo);
        Node configuration = (Node) executeWithLocalEvents(new Callable() {
            public Object run() throws RepositoryException {
                return vMgr.createConfiguration(getNodePath(nodeId, sInfo));
            }
        }, getSessionInfoImpl(sessionInfo));
        return idFactory.createNodeId(configuration);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getSupportedQueryLanguages(SessionInfo sessionInfo)
            throws RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        return sInfo.getSession().getWorkspace().getQueryManager().getSupportedQueryLanguages();
    }

    /**
     * {@inheritDoc}
     */
    public String[] checkQueryStatement(SessionInfo sessionInfo,
                                    String statement,
                                    String language,
                                    Map<String, String> namespaces)
            throws InvalidQueryException, RepositoryException {
        Query q = createQuery(getSessionInfoImpl(sessionInfo).getSession(),
                statement, language, namespaces);
        return q.getBindVariableNames();
    }

    /**
     * {@inheritDoc}
     */
    public QueryInfo executeQuery(SessionInfo sessionInfo, String statement, String language, Map<String, String> namespaces, long limit, long offset, Map<String, QValue> values) throws RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        Query query = createQuery(sInfo.getSession(), statement,
                language, namespaces);
        if (limit != -1) {
            query.setLimit(limit);
        }
        if (offset != -1) {
            query.setOffset(offset);
        }
        if (values != null && !values.isEmpty()) {
            for (Map.Entry<String, QValue> entry : values.entrySet()) {
                Value value = ValueFormat.getJCRValue(entry.getValue(), sInfo.getNamePathResolver(), sInfo.getSession().getValueFactory());
                query.bindValue(entry.getKey(), value);
            }
        }
        return new QueryInfoImpl(query.execute(), idFactory,
                sInfo.getNamePathResolver(), getQValueFactory());
    }

    /**
     * {@inheritDoc}
     */
    public EventFilter createEventFilter(SessionInfo sessionInfo,
                                         int eventTypes,
                                         Path absPath,
                                         boolean isDeep,
                                         String[] uuid,
                                         Name[] nodeTypeName,
                                         boolean noLocal)
            throws UnsupportedRepositoryOperationException, RepositoryException {
        Set<Name> ntNames = null;
        if (nodeTypeName != null) {
            ntNames = new HashSet<Name>(Arrays.asList(nodeTypeName));
        }
        return new EventFilterImpl(eventTypes, absPath, isDeep, uuid, ntNames, noLocal);
    }

    /**
     * {@inheritDoc}
     */
    public Subscription createSubscription(SessionInfo sessionInfo,
                                           EventFilter[] filters)
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return getSessionInfoImpl(sessionInfo).createSubscription(idFactory, qValueFactory, filters);
    }

    /**
     * {@inheritDoc}
     */
    public EventBundle[] getEvents(Subscription subscription, long timeout)
            throws RepositoryException, UnsupportedRepositoryOperationException, InterruptedException {
        if (subscription instanceof EventSubscription) {
            return ((EventSubscription) subscription).getEventBundles(timeout);
        } else {
            throw new RepositoryException("Unknown subscription implementation: "
                    + subscription.getClass().getName());
        }
    }

    /**
     * {@inheritDoc}
     */
    public EventBundle getEvents(SessionInfo sessionInfo,
                                   EventFilter filter,
                                   long after)
            throws RepositoryException, UnsupportedRepositoryOperationException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        EventJournal journal = sInfo.getSession().getWorkspace().getObservationManager().getEventJournal();
        if (journal == null) {
            throw new UnsupportedRepositoryOperationException();
        }
        EventFactory factory = new EventFactory(sInfo.getSession(),
                sInfo.getNamePathResolver(), idFactory, qValueFactory);
        journal.skipTo(after);
        List<Event> events = new ArrayList<Event>();
        int batchSize = 1024;
        boolean distinctDates = true;
        long lastDate = Long.MIN_VALUE;
        while (journal.hasNext() && (batchSize > 0 || !distinctDates)) {
            Event e = factory.fromJCREvent(journal.nextEvent());
            if (filter.accept(e, false)) {
                distinctDates = lastDate != e.getDate();
                lastDate = e.getDate();
                events.add(e);
                batchSize--;
            }
        }
        return new EventBundleImpl(events, false);
    }

    /**
     * {@inheritDoc}
     */
    public void updateEventFilters(Subscription subscription,
                                   EventFilter[] filters)
            throws RepositoryException {
        getEventSubscription(subscription).setFilters(filters);
    }

    /**
     * {@inheritDoc}
     */
    public void dispose(Subscription subscription) throws RepositoryException {
        getEventSubscription(subscription).dispose();
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getRegisteredNamespaces(SessionInfo sessionInfo)
            throws RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        NamespaceRegistry nsReg = sInfo.getSession().getWorkspace().getNamespaceRegistry();
        Map<String, String> namespaces = new HashMap<String, String>();
        for (String prefix : nsReg.getPrefixes()) {
            namespaces.put(prefix, nsReg.getURI(prefix));
        }
        return namespaces;
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespaceURI(SessionInfo sessionInfo, String prefix)
            throws NamespaceException, RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        return sInfo.getSession().getWorkspace().getNamespaceRegistry().getURI(prefix);
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespacePrefix(SessionInfo sessionInfo, String uri)
            throws NamespaceException, RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        return sInfo.getSession().getWorkspace().getNamespaceRegistry().getPrefix(uri);
    }

    /**
     * {@inheritDoc}
     */
    public void registerNamespace(SessionInfo sessionInfo,
                                  String prefix,
                                  String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        Session session = getSessionInfoImpl(sessionInfo).getSession();
        NamespaceRegistry nsReg = session.getWorkspace().getNamespaceRegistry();
        nsReg.registerNamespace(prefix, uri);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterNamespace(SessionInfo sessionInfo, String uri)
            throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        Session session = getSessionInfoImpl(sessionInfo).getSession();
        NamespaceRegistry nsReg = session.getWorkspace().getNamespaceRegistry();
        nsReg.unregisterNamespace(nsReg.getPrefix(uri));
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<QNodeTypeDefinition> getQNodeTypeDefinitions(SessionInfo sessionInfo) throws RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        NodeTypeManager ntMgr = sInfo.getSession().getWorkspace().getNodeTypeManager();
        List<QNodeTypeDefinition> nodeTypes = new ArrayList<QNodeTypeDefinition>();
        try {
            for (NodeTypeIterator it = ntMgr.getAllNodeTypes(); it.hasNext(); ) {
                NodeType nt = it.nextNodeType();
                nodeTypes.add(new QNodeTypeDefinitionImpl(nt,
                        sInfo.getNamePathResolver(), getQValueFactory()));
            }
        } catch (NameException e) {
            throw new RepositoryException(e);
        }
        return nodeTypes.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<QNodeTypeDefinition> getQNodeTypeDefinitions(SessionInfo sessionInfo, Name[] nodetypeNames) throws RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        NodeTypeManager ntMgr = sInfo.getSession().getWorkspace().getNodeTypeManager();
        List<QNodeTypeDefinition> defs = new ArrayList<QNodeTypeDefinition>();
        for (Name nodetypeName : nodetypeNames) {
            try {
                String ntName = sInfo.getNamePathResolver().getJCRName(nodetypeName);
                NodeType nt = ntMgr.getNodeType(ntName);
                defs.add(new QNodeTypeDefinitionImpl(nt,
                        sInfo.getNamePathResolver(), getQValueFactory()));

                // in addition pack all supertypes into the return value
                NodeType[] supertypes = nt.getSupertypes();
                for (NodeType supertype : supertypes) {
                    defs.add(new QNodeTypeDefinitionImpl(supertype,
                            sInfo.getNamePathResolver(), getQValueFactory()));
                }
            } catch (NameException e) {
                throw new RepositoryException(e);
            }
        }
        return defs.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public void registerNodeTypes(SessionInfo sessionInfo, QNodeTypeDefinition[] nodeTypeDefinitions, boolean allowUpdate) throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException, RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        NodeTypeManager ntMgr = sInfo.getSession().getWorkspace().getNodeTypeManager();

        NodeTypeDefinition[] defs = new NodeTypeDefinition[nodeTypeDefinitions.length];
        for (int i = 0; i < nodeTypeDefinitions.length; i++) {
            defs[i] = new NodeTypeDefinitionImpl(nodeTypeDefinitions[i], sInfo.getNamePathResolver(), sInfo.getSession().getValueFactory()) {
            };
        }
        ntMgr.registerNodeTypes(defs, allowUpdate);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterNodeTypes(SessionInfo sessionInfo, Name[] nodeTypeNames) throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException, RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        NodeTypeManager ntMgr = sInfo.getSession().getWorkspace().getNodeTypeManager();
        String[] names = new String[nodeTypeNames.length];
        for (int i = 0; i < nodeTypeNames.length; i++) {
            names[i] = sInfo.getNamePathResolver().getJCRName(nodeTypeNames[i]);
        }
        ntMgr.unregisterNodeTypes(names);
    }

    /**
     * {@inheritDoc}
     */
    public void createWorkspace(SessionInfo sessionInfo, String name, String srcWorkspaceName) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        Workspace wsp = sInfo.getSession().getWorkspace();
        if (srcWorkspaceName == null) {
            wsp.createWorkspace(name);
        } else {
            wsp.createWorkspace(name, srcWorkspaceName);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deleteWorkspace(SessionInfo sessionInfo, String name) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        SessionInfoImpl sInfo = getSessionInfoImpl(sessionInfo);
        Workspace wsp = sInfo.getSession().getWorkspace();
        wsp.deleteWorkspace(name);
    }

    //----------------------------< internal >----------------------------------

    private final class BatchImpl implements Batch {

        private final SessionInfoImpl sInfo;
        /* If this batch needs to remove multiple same-name-siblings starting
           from lower index, the index of the following siblings must be reset
           in order to avoid PathNotFoundException.
         */
        private final Set<NodeId> removedNodeIds = new HashSet<NodeId>();

        private boolean failed = false;

        BatchImpl(SessionInfoImpl sInfo) {
            this.sInfo = sInfo;
        }

        //----------------------------------------------------------< Batch >---
        @Override
        public void addNode(final NodeId parentId,
                            final Name nodeName,
                            final Name nodetypeName,
                            final String uuid) throws RepositoryException {
            executeGuarded(new Callable() {
                public Object run() throws RepositoryException {
                    Session s = sInfo.getSession();
                    Node parent = getParent(parentId, sInfo);

                    String jcrName = getJcrName(nodeName);
                    String ntName = getJcrName(nodetypeName);
                    if (uuid == null) {
                        if (ntName == null) {
                            parent.addNode(jcrName);
                        } else {
                            parent.addNode(jcrName, ntName);
                        }
                    } else {
                        String xml = createXMLFragment(jcrName, ntName, uuid);
                        InputStream in = new ByteArrayInputStream(xml.getBytes());
                        try {
                            s.importXML(parent.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
                        } catch (IOException e) {
                            throw new RepositoryException(e.getMessage(), e);
                        }
                    }
                    return null;
                }
            });
        }

        @Override
        public void addProperty(final NodeId parentId,
                                final Name propertyName,
                                final QValue value)
                throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            executeGuarded(new Callable() {
                public Object run() throws RepositoryException {
                    Session s = sInfo.getSession();
                    Node parent = getParent(parentId, sInfo);
                    Value jcrValue = ValueFormat.getJCRValue(value,
                            sInfo.getNamePathResolver(), s.getValueFactory());
                    parent.setProperty(getJcrName(propertyName), jcrValue);
                    return null;
                }
            });
        }

        @Override
        public void addProperty(final NodeId parentId,
                                final Name propertyName,
                                final QValue[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            executeGuarded(new Callable() {
                public Object run() throws RepositoryException {
                    Session s = sInfo.getSession();
                    Node n = getParent(parentId, sInfo);
                    Value[] jcrValues = new Value[values.length];
                    for (int i = 0; i < jcrValues.length; i++) {
                        jcrValues[i] = ValueFormat.getJCRValue(values[i],
                                sInfo.getNamePathResolver(), s.getValueFactory());
                    }
                    n.setProperty(getJcrName(propertyName), jcrValues);
                    return null;
                }
            });
        }

        @Override
        public void setValue(final PropertyId propertyId, final QValue value)
                throws RepositoryException {
            executeGuarded(new Callable() {
                public Object run() throws RepositoryException {
                    Session s = sInfo.getSession();
                    Value jcrValue = ValueFormat.getJCRValue(value,
                            sInfo.getNamePathResolver(), s.getValueFactory());
                    getProperty(propertyId, sInfo).setValue(jcrValue);
                    return null;
                }
            });
        }

        @Override
        public void setValue(final PropertyId propertyId, final QValue[] values)
                throws RepositoryException {
            executeGuarded(new Callable() {
                public Object run() throws RepositoryException {
                    Session s = sInfo.getSession();
                    Value[] jcrValues = new Value[values.length];
                    for (int i = 0; i < jcrValues.length; i++) {
                        jcrValues[i] = ValueFormat.getJCRValue(values[i],
                                sInfo.getNamePathResolver(), s.getValueFactory());
                    }
                    getProperty(propertyId, sInfo).setValue(jcrValues);
                    return null;
                }
            });
        }

        @Override
        public void remove(final ItemId itemId) throws RepositoryException {
            executeGuarded(new Callable() {
                public Object run() throws RepositoryException {
                    try {
                        if (itemId.denotesNode()) {
                            NodeId nodeId = calcRemoveNodeId(itemId);
                            getNode(nodeId, sInfo).remove();
                        } else {
                            getProperty((PropertyId) itemId, sInfo).remove();
                        }
                    } catch (ItemNotFoundException e) {
                        // item was present in jcr2spi but got removed on the
                        // persistent layer in the mean time.
                        throw new InvalidItemStateException(e);
                    } catch (PathNotFoundException e) {
                        // item was present in jcr2spi but got removed on the
                        // persistent layer in the mean time.
                        throw new InvalidItemStateException(e);
                    }
                    return null;
                }
            });
        }

        private NodeId calcRemoveNodeId(ItemId itemId) throws MalformedPathException {
            NodeId nodeId = (NodeId) itemId;
            Path p = itemId.getPath();
            if (p != null) {
                removedNodeIds.add(nodeId);
                int index = p.getNormalizedIndex();
                if (index > Path.INDEX_DEFAULT) {
                    Path.Element[] elems = p.getElements();
                    PathBuilder pb = new PathBuilder();
                    for (int i = 0; i <= elems.length - 2; i++) {
                        pb.addLast(elems[i]);
                    }
                    pb.addLast(p.getName(), index - 1);

                    NodeId prevSibling = idFactory.createNodeId(itemId.getUniqueID(), pb.getPath());
                    if (removedNodeIds.contains(prevSibling)) {
                        nodeId = prevSibling;
                    }
                }
            }
            return nodeId;
        }

        @Override
        public void reorderNodes(final NodeId parentId,
                                 final NodeId srcNodeId,
                                 final NodeId beforeNodeId)
                throws RepositoryException {
            executeGuarded(new Callable() {
                public Object run() throws RepositoryException {
                    Node parent = getParent(parentId, sInfo);
                    Node srcNode = getNode(srcNodeId, sInfo);
                    Node beforeNode = null;
                    if (beforeNodeId != null) {
                        beforeNode = getNode(beforeNodeId, sInfo);
                    }
                    String srcPath = srcNode.getName();
                    if (srcNode.getIndex() > 1) {
                        srcPath += "[" + srcNode.getIndex() + "]";
                    }
                    String beforePath = null;
                    if (beforeNode != null) {
                        beforePath = beforeNode.getName();
                        if (beforeNode.getIndex() > 1) {
                            beforePath += "[" + beforeNode.getIndex() + "]";
                        }
                    }
                    parent.orderBefore(srcPath, beforePath);
                    return null;
                }
            });
        }

        @Override
        public void setMixins(final NodeId nodeId,
                              final Name[] mixinNodeTypeIds)
                throws RepositoryException {
            executeGuarded(new Callable() {
                public Object run() throws RepositoryException {
                    Set<String> mixinNames = new HashSet<String>();
                    for (Name mixinNodeTypeId : mixinNodeTypeIds) {
                        mixinNames.add(getJcrName(mixinNodeTypeId));
                    }
                    Node n = getNode(nodeId, sInfo);
                    Set<String> currentMixins = new HashSet<String>();
                    for (NodeType nt : n.getMixinNodeTypes()) {
                        currentMixins.add(nt.getName());
                    }
                    Set<String> remove = new HashSet<String>(currentMixins);
                    remove.removeAll(mixinNames);
                    mixinNames.removeAll(currentMixins);
                    for (String mixName : remove) {
                        n.removeMixin(mixName);
                    }
                    for (String mixName : mixinNames) {
                        n.addMixin(mixName);
                    }
                    return null;
                }
            });
        }

        @Override
        public void setPrimaryType(final NodeId nodeId, final Name primaryNodeTypeName) throws RepositoryException {
            executeGuarded(new Callable() {
                public Object run() throws RepositoryException {
                    Node n = getNode(nodeId, sInfo);
                    n.setPrimaryType(getJcrName(primaryNodeTypeName));
                    return null;
                }
            });
        }

        @Override
        public void move(final NodeId srcNodeId,
                         final NodeId destParentNodeId,
                         final Name destName) throws RepositoryException {
            executeGuarded(new Callable() {
                public Object run() throws RepositoryException {
                    String srcPath = pathForId(srcNodeId, sInfo);
                    String destPath = pathForId(destParentNodeId, sInfo);
                    if (destPath.length() > 1) {
                        destPath += "/";
                    }
                    destPath += getJcrName(destName);
                    sInfo.getSession().move(srcPath, destPath);
                    return null;
                }
            });
        }

        @Override
        public void setTree(final NodeId parentId, Tree tree) throws RepositoryException {
            if (!(tree instanceof XmlTree)) {
                throw new RepositoryException("Unknown Tree implementation: " + tree.getClass().getName());
            }

            final XmlTree xmlTree = (XmlTree) tree;
            executeGuarded(new Callable() {
                public Object run() throws RepositoryException {
                    Session s = sInfo.getSession();
                    Node parent = getParent(parentId, sInfo);
                    String xml = xmlTree.toXML();;
                    InputStream in = new ByteArrayInputStream(xml.getBytes());
                    try {
                        s.importXML(parent.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
                    } catch (IOException e) {
                        throw new RepositoryException(e.getMessage(), e);
                    } finally {
                        try {
                            in.close();
                        } catch (IOException e) {
                            throw new RepositoryException(e.getMessage(), e);
                        }
                    }
                    return null;
                }
            });
        }

        //----------------------------------------------------------------------
        private void executeGuarded(Callable call) throws RepositoryException {
            if (failed) {
                return;
            }
            try {
                call.run();
            } catch (RepositoryException e) {
                failed = true;
                sInfo.getSession().refresh(false);
                throw e;
            } catch (RuntimeException e) {
                failed = true;
                sInfo.getSession().refresh(false);
                throw e;
            }
        }

        private String getJcrName(Name name) throws RepositoryException {
            if (name == null) {
                return null;
            }
            return sInfo.getNamePathResolver().getJCRName((name));
        }

        private String createXMLFragment(String nodeName, String ntName, String uuid) {
            StringBuffer xml = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            xml.append("<sv:node xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" ");
            xml.append("xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" ");
            xml.append("sv:name=\"").append(nodeName).append("\">");
            // jcr:primaryType
            xml.append("<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">");
            xml.append("<sv:value>").append(ntName).append("</sv:value>");
            xml.append("</sv:property>");
            // jcr:uuid
            xml.append("<sv:property sv:name=\"jcr:uuid\" sv:type=\"String\">");
            xml.append("<sv:value>").append(uuid).append("</sv:value>");
            xml.append("</sv:property>");
            xml.append("</sv:node>");
            return xml.toString();
        }

        private void end() throws AccessDeniedException, ItemExistsException,
                ConstraintViolationException, InvalidItemStateException,
                VersionException, LockException, NoSuchNodeTypeException,
                RepositoryException {
            executeGuarded(new Callable() {
                public Object run() throws RepositoryException {
                    executeWithLocalEvents(new Callable() {
                        public Object run() throws RepositoryException {
                            sInfo.getSession().save();
                            return null;
                        }
                    }, sInfo);
                    return null;
                }
            });
        }
    }

    private interface Callable {
        public Object run() throws RepositoryException;
    }

    private SessionInfoImpl getSessionInfoImpl(SessionInfo sessionInfo)
            throws RepositoryException {
        if (sessionInfo instanceof SessionInfoImpl) {
            return (SessionInfoImpl) sessionInfo;
        } else {
            throw new RepositoryException("Unknown SessionInfo implementation: "
                    + sessionInfo.getClass().getName());
        }
    }

    private EventSubscription getEventSubscription(Subscription subscription)
            throws RepositoryException {
        if (subscription instanceof EventSubscription) {
            return (EventSubscription) subscription;
        } else {
            throw new RepositoryException("Unknown Subscription implementation: "
                    + subscription.getClass().getName());
        }
    }

    private String getDestinationPath(NodeId destParentNodeId, Name destName, SessionInfoImpl sessionInfo) throws RepositoryException {
        StringBuffer destPath = new StringBuffer(pathForId(destParentNodeId, sessionInfo));
        if (destPath.length() > 1) {
            destPath.append("/");
        }
        destPath.append(sessionInfo.getNamePathResolver().getJCRName(destName));
        return destPath.toString();
    }

    private String pathForId(ItemId id, SessionInfoImpl sessionInfo)
            throws RepositoryException {
        Session session = sessionInfo.getSession();
        StringBuffer path = new StringBuffer();
        if (id.getUniqueID() != null) {
            path.append(session.getNodeByIdentifier(id.getUniqueID()).getPath());
        } else {
            path.append("/");
        }

        if (id.getPath() == null) {
            // we're done
            return path.toString();
        }

        if (id.getPath().isAbsolute()) {
            if (path.length() == 1) {
                // root path ends with slash
                path.setLength(0);
            }
        } else {
            // path is relative
            if (path.length() > 1) {
                path.append("/");
            }
        }
        path.append(sessionInfo.getNamePathResolver().getJCRPath(id.getPath()));
        return path.toString();
    }

    private Node getParent(NodeId parentId, SessionInfoImpl sessionInfo) throws InvalidItemStateException, RepositoryException {
        try {
            return getNode(parentId, sessionInfo);
        } catch (PathNotFoundException e) {
            // if the parent of an batch operation is not available, this indicates
            // that it has been destroyed by another session.
            throw new InvalidItemStateException(e);
        } catch (ItemNotFoundException e) {
            // if the parent of an batch operation is not available, this indicates
            // that it has been destroyed by another session.
            throw new InvalidItemStateException(e);
        }
    }

    private Node getNode(NodeId id, SessionInfoImpl sessionInfo) throws ItemNotFoundException, PathNotFoundException, RepositoryException {
        Session session = sessionInfo.getSession();
        Node n;
        if (id.getUniqueID() != null) {
            n = session.getNodeByIdentifier(id.getUniqueID());
        } else {
            n = session.getRootNode();
        }
        Path path = id.getPath();
        if (path == null || path.denotesRoot()) {
            return n;
        }
        String jcrPath;
        jcrPath = sessionInfo.getNamePathResolver().getJCRPath(path);
        if (path.isAbsolute()) {
            jcrPath = jcrPath.substring(1, jcrPath.length());
        }
        return n.getNode(jcrPath);
    }

    private String getNodePath(NodeId nodeId, SessionInfoImpl sessionInfo) throws RepositoryException {
        // TODO: improve. avoid round trip over node access.
        return getNode(nodeId, sessionInfo).getPath();
    }

    private Property getProperty(PropertyId id, SessionInfoImpl sessionInfo) throws ItemNotFoundException, PathNotFoundException, RepositoryException {
        Session session = sessionInfo.getSession();
        Node n;
        if (id.getUniqueID() != null) {
            n = session.getNodeByIdentifier(id.getUniqueID());
        } else {
            n = session.getRootNode();
        }
        Path path = id.getPath();
        String jcrPath = sessionInfo.getNamePathResolver().getJCRPath(path);
        if (path.isAbsolute()) {
            jcrPath = jcrPath.substring(1, jcrPath.length());
        }
        return n.getProperty(jcrPath);
    }

    private VersionManager getVersionManager(SessionInfoImpl sessionInfo) throws RepositoryException {
        return sessionInfo.getSession().getWorkspace().getVersionManager();
    }

    private Query createQuery(Session session,
                              String statement,
                              String language,
                              Map<String, String> namespaces)
            throws InvalidQueryException, RepositoryException {
        QueryManager qMgr = session.getWorkspace().getQueryManager();

        // apply namespace mappings to session
        Map<String, String> previous = setNamespaceMappings(session, namespaces);
        try {
            return qMgr.createQuery(statement, language);
        } finally {
            // reset namespace mappings
            setNamespaceMappings(session, previous);
        }
    }

    /**
     * Utility method that changes the namespace mappings of the
     * given sessions to include the given prefix to URI mappings.
     *
     * @param session current session
     * @param namespaces prefix to URI mappings
     * @return the previous namespace mappings that were modified
     * @throws RepositoryException if a repository error occurs
     */
    private Map<String, String> setNamespaceMappings(Session session, Map<String, String> namespaces)
            throws RepositoryException {
        Map<String, String> previous = new HashMap<String, String>();

        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            String uri = entry.getValue();
            String prefix = entry.getKey();

            // Get the previous prefix for this URI, throws if
            // URI not found (which is OK, as that's an error)
            String oldPrefix = session.getNamespacePrefix(uri);
            // If the prefixes are different, we need to remap the namespace
            if (!prefix.equals(oldPrefix)) {
                // Check if the new prefix is mapped to some other URI
                String oldURI = safeGetURI(session, prefix);
                if (oldURI != null) {
                    // Find an unused prefix and map the old URI to it
                    int i = 2;
                    String tmpPrefix = oldPrefix + i++;
                    while (safeGetURI(session, tmpPrefix) != null
                            || namespaces.containsKey(tmpPrefix)) {
                        tmpPrefix = oldPrefix + i++;
                    }
                    session.setNamespacePrefix(tmpPrefix, oldURI);
                    previous.put(prefix, oldURI); // remember the old URI
                }
                // It's now safe to remap
                session.setNamespacePrefix(prefix, uri);
                previous.put(oldPrefix, uri); // remember the old prefix
            }
        }

        return previous;
    }

    /**
     * Utility method that returns the namespace URI mapped to the given
     * prefix, or <code>null</code> if the prefix is not mapped.
     *
     * @param session current session
     * @param prefix namespace prefix
     * @return namespace URI or <code>null</code>
     * @throws RepositoryException if a repository error occurs
     */
    private String safeGetURI(Session session, String prefix)
            throws RepositoryException {
        try {
            return session.getNamespaceURI(prefix);
        } catch (NamespaceException e) {
            return null;
        }
    }

    private Object executeWithLocalEvents(Callable call, SessionInfoImpl sInfo)
            throws RepositoryException {
        if (supportsObservation) {
            // register local event listener
            Collection<EventSubscription> subscr = sInfo.getSubscriptions();
            if (subscr.size() != 0) {
                ObservationManager obsMgr = sInfo.getSession().getWorkspace().getObservationManager();
                List<EventListener> listeners = new ArrayList<EventListener>(subscr.size());
                try {
                    for (EventSubscription s : subscr) {
                        EventListener listener = s.getLocalEventListener();
                        listeners.add(listener);
                        obsMgr.addEventListener(listener, EventSubscription.ALL_EVENTS,
                                "/", true, null, null, false);
                    }
                    return call.run();
                } finally {
                    for (EventListener listener : listeners) {
                        try {
                            obsMgr.removeEventListener(listener);
                        } catch (RepositoryException e) {
                            // ignore and remove next
                        }
                    }
                }
            }
        }
        // if we get here simply run as is
        return call.run();
    }
}
