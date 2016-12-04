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
package org.apache.jackrabbit.webdav.simple;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.server.io.AbstractExportContext;
import org.apache.jackrabbit.server.io.CopyMoveContextImpl;
import org.apache.jackrabbit.server.io.DefaultIOListener;
import org.apache.jackrabbit.server.io.DeleteContextImpl;
import org.apache.jackrabbit.server.io.DeleteManager;
import org.apache.jackrabbit.server.io.ExportContext;
import org.apache.jackrabbit.server.io.ExportContextImpl;
import org.apache.jackrabbit.server.io.IOListener;
import org.apache.jackrabbit.server.io.IOManager;
import org.apache.jackrabbit.server.io.IOUtil;
import org.apache.jackrabbit.server.io.ImportContext;
import org.apache.jackrabbit.server.io.ImportContextImpl;
import org.apache.jackrabbit.server.io.PropertyExportContext;
import org.apache.jackrabbit.server.io.PropertyImportContext;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavCompliance;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.bind.BindConstants;
import org.apache.jackrabbit.webdav.bind.BindableResource;
import org.apache.jackrabbit.webdav.bind.ParentElement;
import org.apache.jackrabbit.webdav.bind.ParentSet;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.jcr.JcrDavSession;
import org.apache.jackrabbit.webdav.jcr.lock.JcrActiveLock;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockDiscovery;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.SupportedLock;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.HrefProperty;
import org.apache.jackrabbit.webdav.property.ResourceType;
import org.apache.jackrabbit.webdav.property.PropEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * DavResourceImpl implements a DavResource.
 */
public class DavResourceImpl implements DavResource, BindableResource, JcrConstants {

    /**
     * the default logger
     */
    private static final Logger log = LoggerFactory.getLogger(DavResourceImpl.class);

    public static final String METHODS = DavResource.METHODS + ", " + BindConstants.METHODS;

    public static final String COMPLIANCE_CLASSES = DavCompliance.concatComplianceClasses(
        new String[] {
            DavCompliance._1_,
            DavCompliance._2_,
            DavCompliance._3_,
            DavCompliance.BIND
        }
    );
    
    private DavResourceFactory factory;
    private LockManager lockManager;
    private JcrDavSession session;
    private Node node;
    private DavResourceLocator locator;

    protected DavPropertySet properties = new DavPropertySet();
    protected boolean propsInitialized = false;
    private boolean isCollection = true;
    private String rfc4122Uri;
    
    private ResourceConfig config;
    private long modificationTime = IOUtil.UNDEFINED_TIME;

    /**
     * Create a new {@link DavResource}.
     *
     * @param locator
     * @param factory
     * @param session
     * @param config
     * @param isCollection
     * @throws DavException
     */
    public DavResourceImpl(DavResourceLocator locator, DavResourceFactory factory,
                           DavSession session, ResourceConfig config,
                           boolean isCollection) throws DavException {
        this(locator, factory, session, config, null);
        this.isCollection = isCollection;
    }

    /**
     * Create a new {@link DavResource}.
     *
     * @param locator
     * @param factory
     * @param session
     * @param config
     * @param node
     * @throws DavException
     */
    public DavResourceImpl(DavResourceLocator locator, DavResourceFactory factory,
                           DavSession session, ResourceConfig config, Node node) throws DavException {
        if (locator == null || session == null || config == null) {
            throw new IllegalArgumentException();
        }
        JcrDavSession.checkImplementation(session);
        this.session = (JcrDavSession)session;
        this.factory = factory;
        this.locator = locator;
        this.config = config;

        if (locator.getResourcePath() != null) {
            if (node != null) {
                this.node = node;
                // define what is a collection in webdav
                isCollection = config.isCollectionResource(node);
                initRfc4122Uri();
            }
        } else {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * If the Node associated with this DavResource has a UUID that allows for the creation of a rfc4122 compliant
     * URI, we use it as the value of the protected DAV property DAV:resource-id, which is defined by the BIND
     * specification.
     */
    private void initRfc4122Uri() {
        try {
            if (node.isNodeType(MIX_REFERENCEABLE)) {
                String uuid = node.getUUID();
                try {
                    UUID.fromString(uuid);
                    rfc4122Uri = "urn:uuid:" + uuid;
                } catch (IllegalArgumentException e) {
                    //no, this is not a UUID
                }
            }
        } catch (RepositoryException e) {
            log.warn("Error while detecting UUID", e);
        }
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getComplianceClass()
     */
    public String getComplianceClass() {
        return COMPLIANCE_CLASSES;
    }

    /**
     * @return DavResource#METHODS
     * @see org.apache.jackrabbit.webdav.DavResource#getSupportedMethods()
     */
    public String getSupportedMethods() {
        return METHODS;
    }

    /**
     * @see DavResource#exists() )
     */
    public boolean exists() {
        return node != null;
    }

    /**
     * @see DavResource#isCollection()
     */
    public boolean isCollection() {
        return isCollection;
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getLocator()
     */
    public DavResourceLocator getLocator() {
        return locator;
    }

    /**
     * @see DavResource#getResourcePath()
     */
    public String getResourcePath() {
        return locator.getResourcePath();
    }

    /**
     * @see DavResource#getHref()
     */
    public String getHref() {
        return locator.getHref(isCollection());
    }

    /**
     * Returns the the last segment of the resource path.<p>
     * Note that this must not correspond to the name of the underlying
     * repository item for two reasons:<ul>
     * <li>SameNameSiblings have an index appended to their item name.</li>
     * <li>the resource path may differ from the item path.</li>
     * </ul>
     * Using the item name as DAV:displayname caused problems with XP built-in
     * client in case of resources representing SameNameSibling nodes.
     *
     * @see DavResource#getDisplayName()
     */
    public String getDisplayName() {
        String resPath = getResourcePath();
        return (resPath != null) ? Text.getName(resPath) : resPath;
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getModificationTime()
     */
    public long getModificationTime() {
        initProperties();
        return modificationTime;
    }

    /**
     * If this resource exists and the specified context is not <code>null</code>
     * this implementation build a new {@link ExportContext} based on the specified
     * context and forwards the export to its <code>IOManager</code>. If the
     * {@link IOManager#exportContent(ExportContext, DavResource)} fails,
     * an <code>IOException</code> is thrown.
     *
     * @see DavResource#spool(OutputContext)
     * @see ResourceConfig#getIOManager()
     * @throws IOException if the export fails.
     */
    public void spool(OutputContext outputContext) throws IOException {
        if (exists() && outputContext != null) {
            ExportContext exportCtx = getExportContext(outputContext);
            if (!config.getIOManager().exportContent(exportCtx, this)) {
                throw new IOException("Unexpected Error while spooling resource.");
            }
        }
    }

    /**
     * @see DavResource#getProperty(org.apache.jackrabbit.webdav.property.DavPropertyName)
     */
    public DavProperty<?> getProperty(DavPropertyName name) {
        initProperties();
        return properties.get(name);
    }

    /**
     * @see DavResource#getProperties()
     */
    public DavPropertySet getProperties() {
        initProperties();
        return properties;
    }

    /**
     * @see DavResource#getPropertyNames()
     */
    public DavPropertyName[] getPropertyNames() {
        return getProperties().getPropertyNames();
    }

    /**
     * Fill the set of properties
     */
    protected void initProperties() {
        if (!exists() || propsInitialized) {
            return;
        }

        try {
            config.getPropertyManager().exportProperties(getPropertyExportContext(), isCollection());
        } catch (RepositoryException e) {
            log.warn("Error while accessing resource properties", e);
        }

        // set (or reset) fundamental properties
        if (getDisplayName() != null) {
            properties.add(new DefaultDavProperty<String>(DavPropertyName.DISPLAYNAME, getDisplayName()));
        }
        if (isCollection()) {
            properties.add(new ResourceType(ResourceType.COLLECTION));
            // Windows XP support
            properties.add(new DefaultDavProperty<String>(DavPropertyName.ISCOLLECTION, "1"));
        } else {
            properties.add(new ResourceType(ResourceType.DEFAULT_RESOURCE));
            // Windows XP support
            properties.add(new DefaultDavProperty<String>(DavPropertyName.ISCOLLECTION, "0"));
        }

        if (rfc4122Uri != null) {
            properties.add(new HrefProperty(BindConstants.RESOURCEID, rfc4122Uri, true));
        }

        Set<ParentElement> parentElements = getParentElements();
        if (!parentElements.isEmpty()) {
            properties.add(new ParentSet(parentElements));
        }

        /* set current lock information. If no lock is set to this resource,
        an empty lock discovery will be returned in the response. */
        properties.add(new LockDiscovery(getLock(Type.WRITE, Scope.EXCLUSIVE)));

        /* lock support information: all locks are lockable. */
        SupportedLock supportedLock = new SupportedLock();
        supportedLock.addEntry(Type.WRITE, Scope.EXCLUSIVE);
        properties.add(supportedLock);

        propsInitialized = true;
    }

    /**
     * @param property
     * @throws DavException
     * @see DavResource#setProperty(org.apache.jackrabbit.webdav.property.DavProperty)
     */
    public void setProperty(DavProperty<?> property) throws DavException {
        alterProperty(property);
    }

    /**
     * @param propertyName
     * @throws DavException
     * @see DavResource#removeProperty(org.apache.jackrabbit.webdav.property.DavPropertyName)
     */
    public void removeProperty(DavPropertyName propertyName) throws DavException {
        alterProperty(propertyName);
    }

    private void alterProperty(PropEntry prop) throws DavException {
        if (isLocked(this)) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        try {
            List<? extends PropEntry> list = Collections.singletonList(prop);
            alterProperties(list);
            Map<? extends PropEntry, ?> failure = config.getPropertyManager().alterProperties(getPropertyImportContext(list), isCollection());
            if (failure.isEmpty()) {
                node.save();
            } else {
                node.refresh(false);
                // TODO: retrieve specific error from failure-map
                throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (RepositoryException e) {
            // revert any changes made so far
            JcrDavException je = new JcrDavException(e);
            try {
                node.refresh(false);
            } catch (RepositoryException re) {
                // should not happen...
            }
            throw je;
        }
    }

    public MultiStatusResponse alterProperties(List<? extends PropEntry> changeList) throws DavException {
        if (isLocked(this)) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        MultiStatusResponse msr = new MultiStatusResponse(getHref(), null);
        try {
            Map<? extends PropEntry, ?> failures = config.getPropertyManager().alterProperties(getPropertyImportContext(changeList), isCollection());
            if (failures.isEmpty()) {
                // save all changes together (reverted in case this fails)
                node.save();
            } else {
                // set/remove of at least a single prop failed: undo modifications.
                node.refresh(false);
            }
            /* loop over list of properties/names that were successfully altered
               and them to the multistatus response respecting the result of the
               complete action. in case of failure set the status to 'failed-dependency'
               in order to indicate, that altering those names/properties would
               have succeeded, if no other error occured.*/
            for (PropEntry propEntry : changeList) {
                int statusCode;
                if (failures.containsKey(propEntry)) {
                    Object error = failures.get(propEntry);
                    statusCode = (error instanceof RepositoryException)
                            ? new JcrDavException((RepositoryException) error).getErrorCode()
                            : DavServletResponse.SC_INTERNAL_SERVER_ERROR;
                } else {
                    statusCode = (failures.isEmpty()) ? DavServletResponse.SC_OK : DavServletResponse.SC_FAILED_DEPENDENCY;
                }
                if (propEntry instanceof DavProperty) {
                    msr.add(((DavProperty<?>) propEntry).getName(), statusCode);
                } else {
                    msr.add((DavPropertyName) propEntry, statusCode);
                }
            }
            return msr;
        } catch (RepositoryException e) {
            // revert any changes made so far an throw exception
            try {
                node.refresh(false);
            } catch (RepositoryException re) {
                // should not happen
            }
            throw new JcrDavException(e);
        }
    }

    /**
     * @see DavResource#getCollection()
     */
    public DavResource getCollection() {
        DavResource parent = null;
        if (getResourcePath() != null && !getResourcePath().equals("/")) {
            String parentPath = Text.getRelativeParent(getResourcePath(), 1);
            if (parentPath.equals("")) {
                parentPath = "/";
            }
            DavResourceLocator parentloc = locator.getFactory().createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(), parentPath);
            try {
                parent = factory.createResource(parentloc, session);
            } catch (DavException e) {
                // should not occur
            }
        }
        return parent;
    }

    /**
     * @see DavResource#getMembers()
     */
    public DavResourceIterator getMembers() {
        ArrayList<DavResource> list = new ArrayList<DavResource>();
        if (exists() && isCollection()) {
            try {
                NodeIterator it = node.getNodes();
                while (it.hasNext()) {
                    Node n = it.nextNode();
                    if (!isFilteredItem(n)) {
                        DavResourceLocator resourceLocator = locator.getFactory().createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(), n.getPath(), false);
                        DavResource childRes = factory.createResource(resourceLocator, session);
                        list.add(childRes);
                    } else {
                        log.debug("Filtered resource '" + n.getName() + "'.");
                    }
                }
            } catch (RepositoryException e) {
                // should not occur
            } catch (DavException e) {
                // should not occur
            }
        }
        return new DavResourceIteratorImpl(list);
    }

    /**
     * Adds a new member to this resource.
     *
     * @see DavResource#addMember(DavResource, org.apache.jackrabbit.webdav.io.InputContext)
     */
    public void addMember(DavResource member, InputContext inputContext) throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_CONFLICT);
        }
        if (isLocked(this) || isLocked(member)) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
        try {
            // don't allow creation of nodes if this resource represents a protected
            // item or if the new resource would be filtered out
            if (isFilteredResource(member) || node.getDefinition().isProtected()) {
                log.debug("Forbidden to add member: " + member.getDisplayName());
                throw new DavException(DavServletResponse.SC_FORBIDDEN);
            }

            String memberName = Text.getName(member.getLocator().getRepositoryPath());
            ImportContext ctx = getImportContext(inputContext, memberName);
            if (!config.getIOManager().importContent(ctx, member)) {
                // any changes should have been reverted in the importer
                throw new DavException(DavServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            }
            // persist changes after successful import
            node.save();
        } catch (RepositoryException e) {
            log.error("Error while importing resource: " + e.toString());
            throw new JcrDavException(e);
        } catch (IOException e) {
            log.error("Error while importing resource: " + e.toString());
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * @see DavResource#removeMember(DavResource)
     */
    public void removeMember(DavResource member) throws DavException {
        if (!exists() || !member.exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        if (isLocked(this) || isLocked(member)) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }

        // don't allow removal of nodes, that would be filtered out
        if (isFilteredResource(member)) {
            log.debug("Avoid removal of filtered resource: " + member.getDisplayName());
            throw new DavException(DavServletResponse.SC_FORBIDDEN);
        }

        DeleteManager dm = config.getDeleteManager();
        dm.delete(new DeleteContextImpl(getJcrSession()), member);

        // make sure, non-jcr locks are removed, once the removal is completed
        try {
            if (!isJcrLockable()) {
                ActiveLock lock = getLock(Type.WRITE, Scope.EXCLUSIVE);
                if (lock != null) {
                    lockManager.releaseLock(lock.getToken(), member);
                }
            }
        } catch (DavException e) {
            // since check for 'locked' exception has been performed before
            // ignore any error here
        }
    }

    /**
     * @see DavResource#move(DavResource)
     */
    public void move(DavResource destination) throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        if (isLocked(this)) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
        if (isFilteredResource(destination)) {
            throw new DavException(DavServletResponse.SC_FORBIDDEN);
        }
        // make sure, that src and destination belong to the same workspace
        checkSameWorkspace(destination.getLocator());
        if (!config.getCopyMoveManager().move(new CopyMoveContextImpl(getJcrSession()), this, destination)) {
            throw new DavException(DavServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        }
    }

    /**
     * @see DavResource#copy(DavResource, boolean)
     */
    public void copy(DavResource destination, boolean shallow) throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        if (isLocked(destination)) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
        if (isFilteredResource(destination)) {
            throw new DavException(DavServletResponse.SC_FORBIDDEN);
        }
        // make sure, that src and destination belong to the same workspace
        checkSameWorkspace(destination.getLocator());
        if (!config.getCopyMoveManager().copy(new CopyMoveContextImpl(getJcrSession(), shallow), this, destination)) {
            throw new DavException(DavServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        }
    }

    /**
     * @param type
     * @param scope
     * @return true if type is {@link Type#WRITE} and scope is {@link Scope#EXCLUSIVE}
     * @see DavResource#isLockable(org.apache.jackrabbit.webdav.lock.Type, org.apache.jackrabbit.webdav.lock.Scope)
     */
    public boolean isLockable(Type type, Scope scope) {
        return Type.WRITE.equals(type) && Scope.EXCLUSIVE.equals(scope);
    }

    /**
     * @see DavResource#hasLock(org.apache.jackrabbit.webdav.lock.Type, org.apache.jackrabbit.webdav.lock.Scope)
     */
    public boolean hasLock(Type type, Scope scope) {
        return getLock(type, scope) != null;
    }

    /**
     * @see DavResource#getLock(Type, Scope)
     */
    public ActiveLock getLock(Type type, Scope scope) {
        ActiveLock lock = null;
        if (exists() && Type.WRITE.equals(type) && Scope.EXCLUSIVE.equals(scope)) {
            // try to retrieve the repository lock information first
            try {
                if (node.isLocked()) {
                    Lock jcrLock = node.getLock();
                    if (jcrLock != null && jcrLock.isLive()) {
                        lock = new JcrActiveLock(jcrLock);
                        String lockroot = locator
                                .getFactory()
                                .createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(),
                                        jcrLock.getNode().getPath(), false).getHref(false);
                        lock.setLockroot(lockroot);
                    }
                }
            } catch (RepositoryException e) {
                // LockException (no lock applies) >> should never occur
                // RepositoryException, AccessDeniedException or another error >> ignore
            }

            // could not retrieve a jcr-lock. test if a simple webdav lock is present.
            if (lock == null) {
                lock = lockManager.getLock(type, scope, this);
            }
        }
        return lock;
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getLocks()
     */
    public ActiveLock[] getLocks() {
        ActiveLock writeLock = getLock(Type.WRITE, Scope.EXCLUSIVE);
        return (writeLock != null) ? new ActiveLock[]{writeLock} : new ActiveLock[0];
    }

    /**
     * @see DavResource#lock(LockInfo)
     */
    public ActiveLock lock(LockInfo lockInfo) throws DavException {
        ActiveLock lock = null;
        if (isLockable(lockInfo.getType(), lockInfo.getScope())) {
            // TODO: deal with existing locks, that may have been created, before the node was jcr-lockable...
            if (isJcrLockable()) {
                try {
                    javax.jcr.lock.LockManager lockMgr = node.getSession().getWorkspace().getLockManager();
                    long timeout = lockInfo.getTimeout();
                    if (timeout == LockInfo.INFINITE_TIMEOUT) {
                        timeout = Long.MAX_VALUE;
                    } else {
                        timeout = timeout / 1000;
                    }
                    // try to execute the lock operation
                    Lock jcrLock = lockMgr.lock(node.getPath(), lockInfo.isDeep(), false, timeout, lockInfo.getOwner());
                    if (jcrLock != null) {
                        lock = new JcrActiveLock(jcrLock);
                    }
                } catch (RepositoryException e) {
                    throw new JcrDavException(e);
                }
            } else {
                // create a new webdav lock
                lock = lockManager.createLock(lockInfo, this);
            }
        } else {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "Unsupported lock type or scope.");
        }
        return lock;
    }

    /**
     * @see DavResource#refreshLock(LockInfo, String)
     */
    public ActiveLock refreshLock(LockInfo lockInfo, String lockToken) throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        ActiveLock lock = getLock(lockInfo.getType(), lockInfo.getScope());
        if (lock == null) {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "No lock with the given type/scope present on resource " + getResourcePath());
        }

        if (lock instanceof JcrActiveLock) {
            try {
                // refresh JCR lock and return the original lock object.
                node.getLock().refresh();
            } catch (RepositoryException e) {
                throw new JcrDavException(e);
            }
        } else {
            lock = lockManager.refreshLock(lockInfo, lockToken, this);
        }
        /* since lock has infinite lock (simple) or undefined timeout (jcr)
           return the lock as retrieved from getLock. */
        return lock;
    }

    /**
     * @see DavResource#unlock(String)
     */
    public void unlock(String lockToken) throws DavException {
        ActiveLock lock = getLock(Type.WRITE, Scope.EXCLUSIVE);
        if (lock == null) {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
        } else if (lock.isLockedByToken(lockToken)) {
            if (lock instanceof JcrActiveLock) {
                try {
                    node.unlock();
                } catch (RepositoryException e) {
                    throw new JcrDavException(e);
                }
            } else {
                lockManager.releaseLock(lockToken, this);
            }
        } else {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
    }

    /**
     * @see DavResource#addLockManager(org.apache.jackrabbit.webdav.lock.LockManager)
     */
    public void addLockManager(LockManager lockMgr) {
        this.lockManager = lockMgr;
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getFactory()
     */
    public DavResourceFactory getFactory() {
        return factory;
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getSession()
     */
    public DavSession getSession() {
        return session;
    }


    /**
     * @see BindableResource#rebind(DavResource, DavResource)
     */
    public void bind(DavResource collection, DavResource newBinding) throws DavException {
        if (!exists()) {
            //DAV:bind-source-exists
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
        }
        if (isLocked(collection)) {
            //DAV:locked-update-allowed?
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
        if (isFilteredResource(newBinding)) {
            throw new DavException(DavServletResponse.SC_FORBIDDEN);
        }
        checkSameWorkspace(collection.getLocator());
        try {
            if (!node.isNodeType(MIX_SHAREABLE)) {
                if (!node.canAddMixin(MIX_SHAREABLE)) {
                    //DAV:binding-allowed
                    throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
                }
                node.addMixin(MIX_SHAREABLE);
                node.save();
            }
            Workspace workspace = session.getRepositorySession().getWorkspace();
            workspace.clone(workspace.getName(), node.getPath(), newBinding.getLocator().getRepositoryPath(), false);

        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }

    }

    /**
     * @see BindableResource#rebind(DavResource, DavResource)
     */
    public void rebind(DavResource collection, DavResource newBinding) throws DavException {
        if (!exists()) {
            //DAV:rebind-source-exists
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
        }
        if (isLocked(this)) {
            //DAV:protected-source-url-deletion.allowed
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
        }
        if (isLocked(collection)) {
            //DAV:locked-update-allowed?
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
        if (isFilteredResource(newBinding)) {
            throw new DavException(DavServletResponse.SC_FORBIDDEN);
        }
        checkSameWorkspace(collection.getLocator());
        try {
            if (!node.isNodeType(MIX_REFERENCEABLE)) {
                throw new DavException(node.canAddMixin(MIX_REFERENCEABLE)?
                                       DavServletResponse.SC_CONFLICT : DavServletResponse.SC_METHOD_NOT_ALLOWED);
            }
            getJcrSession().getWorkspace().move(locator.getRepositoryPath(), newBinding.getLocator().getRepositoryPath());
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    /**
     * @see org.apache.jackrabbit.webdav.bind.BindableResource#getParentElements()
     */
    public Set<ParentElement> getParentElements() {
        try {
            if (node.getDepth() > 0) {
                Set<ParentElement> ps = new HashSet<ParentElement>();
                NodeIterator sharedSetIterator = node.getSharedSet();
                while (sharedSetIterator.hasNext()) {
                    Node sharednode = sharedSetIterator.nextNode();
                    DavResourceLocator loc = locator.getFactory().createResourceLocator(
                            locator.getPrefix(), locator.getWorkspacePath(), sharednode.getParent().getPath(), false);
                    ps.add(new ParentElement(loc.getHref(true), sharednode.getName()));
                }
                return ps;
            }
        } catch (UnsupportedRepositoryOperationException e) {
            log.debug("unable to calculate parent set", e);
        } catch (RepositoryException e) {
            log.warn("unable to calculate parent set", e);
        }
        return Collections.emptySet();
    }

    /**
     * Returns the node that is wrapped by this resource.
     *
     * @return The underlying JCR node instance.
     */
    protected Node getNode() {
        return node;
    }

    /**
     * Returns a new <code>ImportContext</code>
     *
     * @param inputCtx
     * @param systemId
     * @return a new <code>ImportContext</code>
     * @throws IOException
     */
    protected ImportContext getImportContext(InputContext inputCtx, String systemId) throws IOException {
        return new ImportContextImpl(
                node, systemId, inputCtx,
                (inputCtx != null) ? inputCtx.getInputStream() : null,
                new DefaultIOListener(log), config.getDetector());
    }

    /**
     * Returns a new <code>ExportContext</code>
     *
     * @param outputCtx
     * @return a new <code>ExportContext</code>
     * @throws IOException
     */
    protected ExportContext getExportContext(OutputContext outputCtx) throws IOException {
        return new ExportContextImpl(node, outputCtx);
    }

    /**
     * Returns a new <code>PropertyImportContext</code>.
     *
     * @param changeList
     * @return a new <code>PropertyImportContext</code>.
     */
    protected PropertyImportContext getPropertyImportContext(List<? extends PropEntry> changeList) {
        return new PropertyImportCtx(changeList);
    }

    /**
     * Returns a new <code>PropertyExportContext</code>.
     *
     * @return a new <code>PropertyExportContext</code>
     */
    protected PropertyExportContext getPropertyExportContext() {
        return new PropertyExportCtx();
    }

    /**
     * Returns true, if the underlying node is nodetype jcr:lockable,
     * without checking its current lock status. If the node is not jcr-lockable
     * an attempt is made to add the mix:lockable mixin type.
     *
     * @return true if this resource is lockable.
     */
    private boolean isJcrLockable() {
        boolean lockable = false;
        if (exists()) {
            try {
                lockable = node.isNodeType(MIX_LOCKABLE);
                // not jcr-lockable: try to make the node jcr-lockable
                if (!lockable && node.canAddMixin(MIX_LOCKABLE)) {
                    node.addMixin(MIX_LOCKABLE);
                    node.save();
                    lockable = true;
                }
            } catch (RepositoryException e) {
                // -> node is definitely not jcr-lockable.
            }
        }
        return lockable;
    }

    /**
     * Return true if this resource cannot be modified due to a write lock
     * that is not owned by the given session.
     *
     * @return true if this resource cannot be modified due to a write lock
     */
    private boolean isLocked(DavResource res) {
        ActiveLock lock = res.getLock(Type.WRITE, Scope.EXCLUSIVE);
        if (lock == null) {
            return false;
        } else {
            for (String sLockToken : session.getLockTokens()) {
                if (sLockToken.equals(lock.getToken())) {
                    return false;
                }
            }
            return true;
        }
    }

    private Session getJcrSession() {
        return session.getRepositorySession();
    }

    private boolean isFilteredResource(DavResource resource) {
        // TODO: filtered nodetypes should be checked as well in order to prevent problems.
        ItemFilter filter = config.getItemFilter();
        return filter != null && filter.isFilteredItem(resource.getDisplayName(), getJcrSession());
    }

    private boolean isFilteredItem(Item item) {
        ItemFilter filter = config.getItemFilter();
        return filter != null && filter.isFilteredItem(item);
    }

    private void checkSameWorkspace(DavResourceLocator otherLoc) throws DavException {
        String wspname = getJcrSession().getWorkspace().getName();
        if (!wspname.equals(otherLoc.getWorkspaceName())) {
            throw new DavException(DavServletResponse.SC_FORBIDDEN, "Workspace mismatch: expected '" + wspname + "'; found: '" + otherLoc.getWorkspaceName() + "'");
        }
    }

    //--------------------------------------------------------< inner class >---
    /**
     * ExportContext that writes the properties of this <code>DavResource</code>
     * and provides not stream.
     */
    private class PropertyExportCtx extends AbstractExportContext implements PropertyExportContext {

        private PropertyExportCtx() {
            super(node, false, null);
            // set defaults:
            setCreationTime(IOUtil.UNDEFINED_TIME);
            setModificationTime(IOUtil.UNDEFINED_TIME);
        }

        public OutputStream getOutputStream() {
            return null;
        }

        public void setContentLanguage(String contentLanguage) {
            if (contentLanguage != null) {
                properties.add(new DefaultDavProperty<String>(DavPropertyName.GETCONTENTLANGUAGE, contentLanguage));
            }
        }

        public void setContentLength(long contentLength) {
            if (contentLength > IOUtil.UNDEFINED_LENGTH) {
                properties.add(new DefaultDavProperty<String>(DavPropertyName.GETCONTENTLENGTH, contentLength + ""));
            }
        }

        public void setContentType(String mimeType, String encoding) {
            String contentType = IOUtil.buildContentType(mimeType, encoding);
            if (contentType != null) {
                properties.add(new DefaultDavProperty<String>(DavPropertyName.GETCONTENTTYPE, contentType));
            }
        }

        public void setCreationTime(long creationTime) {
            String created = IOUtil.getCreated(creationTime);
            properties.add(new DefaultDavProperty<String>(DavPropertyName.CREATIONDATE, created));
        }

        public void setModificationTime(long modTime) {
            if (modTime <= IOUtil.UNDEFINED_TIME) {
                modificationTime = new Date().getTime();
            } else {
                modificationTime = modTime;
            }
            String lastModified = IOUtil.getLastModified(modificationTime);
            properties.add(new DefaultDavProperty<String>(DavPropertyName.GETLASTMODIFIED, lastModified));
        }

        public void setETag(String etag) {
            if (etag != null) {
                properties.add(new DefaultDavProperty<String>(DavPropertyName.GETETAG, etag));
            }
        }

        public void setProperty(Object propertyName, Object propertyValue) {
            if (propertyValue == null) {
                log.warn("Ignore 'setProperty' for " + propertyName + "with null value.");
                return;
            }

            if (propertyValue instanceof DavProperty) {
                properties.add((DavProperty<?>)propertyValue);
            } else {
                DavPropertyName pName;
                if (propertyName instanceof DavPropertyName) {
                    pName = (DavPropertyName)propertyName;
                } else {
                    // create property name with default DAV: namespace
                    pName = DavPropertyName.create(propertyName.toString());
                }
                properties.add(new DefaultDavProperty<Object>(pName, propertyValue));
            }
        }
    }

    private class PropertyImportCtx implements PropertyImportContext {

        private final IOListener ioListener = new DefaultIOListener(log);
        private final List<? extends PropEntry> changeList;
        private boolean completed;

        private PropertyImportCtx(List<? extends PropEntry> changeList) {
            this.changeList = changeList;
        }

        /**
         * @see PropertyImportContext#getImportRoot()
         */
        public Item getImportRoot() {
            return node;
        }

        /**
         * @see PropertyImportContext#getChangeList()
         */
        public List<? extends PropEntry> getChangeList() {
            return Collections.unmodifiableList(changeList);
        }

        public IOListener getIOListener() {
            return ioListener;
        }

        public boolean hasStream() {
            return false;
        }

        /**
         * @see PropertyImportContext#informCompleted(boolean)
         */
        public void informCompleted(boolean success) {
            checkCompleted();
            completed = true;
        }

        /**
         * @see PropertyImportContext#isCompleted()
         */
        public boolean isCompleted() {
            return completed;
        }

        /**
         * @throws IllegalStateException if the context is already completed.
         * @see #isCompleted()
         * @see #informCompleted(boolean)
         */
        private void checkCompleted() {
            if (completed) {
                throw new IllegalStateException("PropertyImportContext has already been consumed.");
            }
        }
    }
}
