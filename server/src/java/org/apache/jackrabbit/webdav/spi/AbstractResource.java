/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.webdav.spi;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.transaction.TransactionResource;
import org.apache.jackrabbit.webdav.transaction.TransactionInfo;
import org.apache.jackrabbit.webdav.transaction.TransactionConstants;
import org.apache.jackrabbit.webdav.transaction.TxLockManager;
import org.apache.jackrabbit.webdav.spi.transaction.TxLockManagerImpl;
import org.apache.jackrabbit.webdav.observation.*;
import org.apache.jackrabbit.webdav.util.Text;
import org.apache.jackrabbit.webdav.version.SupportedMethodSetProperty;
import org.apache.jackrabbit.webdav.lock.*;
import org.apache.jackrabbit.webdav.property.*;

import java.io.InputStream;
import java.util.*;

/**
 * <code>AbstractResource</code> provides functionality common to all
 * resources.
 */
abstract class AbstractResource implements DavResource, ObservationResource,
        TransactionResource {

    private static Logger log = Logger.getLogger(AbstractResource.class);

    private final DavResourceLocator locator;
    private final DavSession session;
    private final DavResourceFactory factory;

    private SubscriptionManager subsMgr;
    private TxLockManagerImpl txMgr;
    private String transactionId;

    private long modificationTime = DavResource.UNDEFINED_MODIFICATIONTIME;

    protected boolean initedProps;
    protected DavPropertySet properties = new DavPropertySet();
    protected SupportedLock supportedLock = new SupportedLock();

    /**
     * Create a new <code>AbstractResource</code>
     *
     * @param locator
     * @param session
     */
    AbstractResource(DavResourceLocator locator, DavSession session, DavResourceFactory factory) {
        if (session == null) {
            throw new IllegalArgumentException("Creating AbstractItemResource: DavSession must not be null.");
        }

        this.locator = locator;
        this.session = session;
        this.factory = factory;
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getLocator()
     */
    public DavResourceLocator getLocator() {
        return locator;
    }

    /**
     * Returns the path of the underlaying repository item or the item to
     * be created (PUT/MKCOL). If the resource exists but does not represent
     * a repository item <code>null</code> is returned.
     *
     * @return path of the underlaying repository item.
     * @see DavResource#getResourcePath()
     * @see org.apache.jackrabbit.webdav.DavResourceLocator#getResourcePath()
     */
    public String getResourcePath() {
        return locator.getResourcePath();
    }

    /**
     * @see DavResource#getHref()
     * @see DavResourceLocator#getHref(boolean)
     */
    public String getHref() {
        return locator.getHref(true);
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getModificationTime()
     */
    public long getModificationTime() {
        return modificationTime;
    }

    /**
     * Set the modificationTime field and adds the {@link DavPropertyName.GETLASTMODIFIED}
     * property to the set of properties.
     * @param modificationTime
     */
    void setModificationTime(long modificationTime) {
        this.modificationTime = modificationTime;
        if (this.modificationTime >= 0) {
            properties.add(new DefaultDavProperty(DavPropertyName.GETLASTMODIFIED,
                    DavConstants.modificationDateFormat.format(new Date(modificationTime))));
        }
    }

    /**
     * Returns <code>null</code>
     *
     * @return Always returns <code>null</code>
     * @see org.apache.jackrabbit.webdav.DavResource#getStream()
     */
    public InputStream getStream() {
        return null;
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getPropertyNames()
     */
    public DavPropertyName[] getPropertyNames() {
        return getProperties().getPropertyNames();
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getProperty(org.apache.jackrabbit.webdav.property.DavPropertyName)
     */
    public DavProperty getProperty(DavPropertyName name) {
        return getProperties().get(name);
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getProperties()
     */
    public DavPropertySet getProperties() {
        if (!initedProps) {
            initProperties();
        }
        return properties;
    }

    /**
     * Throws {@link DavServletResponse#SC_METHOD_NOT_ALLOWED}
     *
     * @param property
     * @throws DavException Always throws {@link DavServletResponse#SC_METHOD_NOT_ALLOWED}
     * @see org.apache.jackrabbit.webdav.DavResource#setProperty(org.apache.jackrabbit.webdav.property.DavProperty)
     */
    public void setProperty(DavProperty property) throws DavException {
        throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Throws {@link DavServletResponse#SC_METHOD_NOT_ALLOWED}
     *
     * @param propertyName
     * @throws DavException Always throws {@link DavServletResponse#SC_METHOD_NOT_ALLOWED}
     * @see org.apache.jackrabbit.webdav.DavResource#removeProperty(org.apache.jackrabbit.webdav.property.DavPropertyName)
     */
    public void removeProperty(DavPropertyName propertyName) throws DavException {
        throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Throws {@link DavServletResponse#SC_METHOD_NOT_ALLOWED}
     *
     * @param destination
     * @throws DavException Always throws {@link DavServletResponse#SC_METHOD_NOT_ALLOWED}
     * @see DavResource#move(org.apache.jackrabbit.webdav.DavResource)
     */
    public void move(DavResource destination) throws DavException {
        throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Throws {@link DavServletResponse#SC_METHOD_NOT_ALLOWED}
     *
     * @param destination
     * @param shallow
     * @throws DavException Always throws {@link DavServletResponse#SC_METHOD_NOT_ALLOWED}
     * @see DavResource#copy(org.apache.jackrabbit.webdav.DavResource, boolean)
     */
    public void copy(DavResource destination, boolean shallow) throws DavException {
        throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
    }


    /**
     * Returns true, if the {@link SupportedLock} property contains an entry
     * with the given type and scope. By default resources allow for {@link org.apache.jackrabbit.webdav.transaction.TransactionConstants.XML_TRANSACTION
     * transaction} lock only.
     *
     * @param type
     * @param scope
     * @return true if this resource may be locked by the given type and scope.
     * @see DavResource#isLockable(org.apache.jackrabbit.webdav.lock.Type, org.apache.jackrabbit.webdav.lock.Scope)
     */
    public boolean isLockable(Type type, Scope scope) {
        return supportedLock.isSupportedLock(type, scope);
    }

    /**
     * Returns true if this resource has a lock applied with the given type and scope.
     *
     * @param type
     * @param scope
     * @return true if this resource has a lock applied with the given type and scope.
     * @see DavResource#hasLock(Type, Scope)
     */
    public boolean hasLock(Type type, Scope scope) {
        return getLock(type, scope) != null;
    }

    /**
     * @see DavResource#getLock(Type, Scope)
     */
    public ActiveLock getLock(Type type, Scope scope) {
        ActiveLock lock = null;
        if (TransactionConstants.TRANSACTION.equals(type)) {
            lock = txMgr.getLock(type, scope, this);
        }
        return lock;
    }

    /**
     * @see DavResource#getLocks()
     * todo improve....
     */
    public ActiveLock[] getLocks() {
        List locks = new ArrayList();
        // tx locks
        ActiveLock l = getLock(TransactionConstants.TRANSACTION, TransactionConstants.LOCAL);
        if (l != null) {
            locks.add(l);
        }
        l = getLock(TransactionConstants.TRANSACTION, TransactionConstants.GLOBAL);
        if (l != null) {
            locks.add(l);
        }
        // write lock (either exclusive or session-scoped).
        l = getLock(Type.WRITE, Scope.EXCLUSIVE);
        if (l != null) {
            locks.add(l);
        } else {
            l = getLock(Type.WRITE, ItemResourceConstants.EXCLUSIVE_SESSION);
            if (l != null) {
                locks.add(l);
            }
        }
        return (ActiveLock[]) locks.toArray(new ActiveLock[locks.size()]);
    }

    /**
     * @see DavResource#lock(org.apache.jackrabbit.webdav.lock.LockInfo)
     */
    public ActiveLock lock(LockInfo reqLockInfo) throws DavException {
        if (isLockable(reqLockInfo.getType(), reqLockInfo.getScope())) {
            return txMgr.createLock(reqLockInfo, this);
        } else {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
        }
    }

    /**
     * Only transaction lock may be available on this resource.
     *
     * @param info
     * @param lockToken
     * @throws DavException
     * @see DavResource#refreshLock(org.apache.jackrabbit.webdav.lock.LockInfo, String)
     */
    public ActiveLock refreshLock(LockInfo info, String lockToken) throws DavException {
        return txMgr.refreshLock(info, lockToken, this);
    }

    /**
     * Throws {@link DavServletResponse#SC_METHOD_NOT_ALLOWED} since only transaction
     * locks may be present on this resource, that need to be released by calling
     * {@link TransactionResource#unlock(String, org.apache.jackrabbit.webdav.transaction.TransactionInfo)}.
     *
     * @param lockToken
     * @throws DavException Always throws {@link DavServletResponse#SC_METHOD_NOT_ALLOWED}
     */
    public void unlock(String lockToken) throws DavException {
        throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
    }

    /**
     * @see DavResource#addLockManager(org.apache.jackrabbit.webdav.lock.LockManager)
     */
    public void addLockManager(LockManager lockMgr) {
        if (lockMgr instanceof TxLockManagerImpl) {
            txMgr = (TxLockManagerImpl) lockMgr;
        }
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getFactory()
     */
    public DavResourceFactory getFactory() {
        return factory;
    }

    //--------------------------------------------------------------------------
    /**
     * @see org.apache.jackrabbit.webdav.transaction.TransactionResource#getSession()
     * @see org.apache.jackrabbit.webdav.observation.ObservationResource#getSession()
     */
    public DavSession getSession() {
        return session;
    }

    //--------------------------------------< ObservationResource interface >---
    /**
     * @see ObservationResource#init(SubscriptionManager)
     */
    public void init(SubscriptionManager subsMgr) {
        this.subsMgr = subsMgr;
    }

    /**
     * @see ObservationResource#subscribe(org.apache.jackrabbit.webdav.observation.SubscriptionInfo, String)
     * @see SubscriptionManager#subscribe(org.apache.jackrabbit.webdav.observation.SubscriptionInfo, String, org.apache.jackrabbit.webdav.observation.ObservationResource)
     */
    public Subscription subscribe(SubscriptionInfo info, String subscriptionId)
            throws DavException {
        return subsMgr.subscribe(info, subscriptionId, this);
    }

    /**
     * @see ObservationResource#unsubscribe(String)
     * @see SubscriptionManager#unsubscribe(String, org.apache.jackrabbit.webdav.observation.ObservationResource)
     */
    public void unsubscribe(String subscriptionId) throws DavException {
        subsMgr.unsubscribe(subscriptionId, this);
    }

    /**
     * @see ObservationResource#poll(String)
     * @see SubscriptionManager#poll(String, org.apache.jackrabbit.webdav.observation.ObservationResource)
     */
    public EventDiscovery poll(String subscriptionId) throws DavException {
        return subsMgr.poll(subscriptionId, this);
    }

    //--------------------------------------< TransactionResource interface >---
    /**
     * @see TransactionResource#init(TxLockManager, String)
     */
    public void init(TxLockManager txMgr, String transactionId) {
        this.txMgr = (TxLockManagerImpl) txMgr;
        this.transactionId = transactionId;
    }

    /**
     * @see TransactionResource#unlock(String, org.apache.jackrabbit.webdav.transaction.TransactionInfo)
     */
    public void unlock(String lockToken, TransactionInfo tInfo) throws DavException {
        txMgr.releaseLock(tInfo, lockToken, this);
    }

    /**
     * @see TransactionResource#getTransactionId()
     */
    public String getTransactionId() {
        return transactionId;
    }

    //--------------------------------------------------------------------------
    /**
     * Fill the set of default properties
     */
    protected void initProperties() {
        if (getDisplayName() != null) {
            properties.add(new DefaultDavProperty(DavPropertyName.DISPLAYNAME, getDisplayName()));
        }
        if (isCollection()) {
            properties.add(new ResourceType(ResourceType.COLLECTION));
            // Windows XP support
            properties.add(new DefaultDavProperty(DavPropertyName.ISCOLLECTION, "1"));
        } else {
            properties.add(new ResourceType(ResourceType.DEFAULT_RESOURCE));
            // Windows XP support
            properties.add(new DefaultDavProperty(DavPropertyName.ISCOLLECTION, "0"));
        }
        // todo: add etag

        // default last modified
        setModificationTime(new Date().getTime());
        // default creation time
        properties.add(new DefaultDavProperty(DavPropertyName.CREATIONDATE, DavConstants.creationDateFormat.format(new Date(0))));

        // supported lock property
        properties.add(supportedLock);

        // set current lock information. If no lock is applied to this resource,
        // an empty lockdiscovery will be returned in the response.
        properties.add(new LockDiscovery(getLocks()));

        // observation resource
        SubscriptionDiscovery subsDiscovery = subsMgr.getSubscriptionDiscovery(this);
        properties.add(subsDiscovery);

        properties.add(new SupportedMethodSetProperty(getSupportedMethods().split(",\\s")));
    }

    /**
     * Create a new <code>DavResource</code> from the given locator.
     * @param loc
     * @return new <code>DavResource</code>
     */
    protected DavResource createResourceFromLocator(DavResourceLocator loc)
            throws DavException {
        DavResource res = factory.createResource(loc, session);
        if (res instanceof AbstractResource) {
            ((AbstractResource)res).transactionId = this.transactionId;
        }
        return res;
    }

    /**
     * Build a <code>DavResourceLocator</code> from the given resource path.
     *
     * @param resourcePath
     * @return a new <code>DavResourceLocator</code>
     * @see DavLocatorFactory#createResourceLocator(String, String, String)
     */
    protected DavResourceLocator getLocatorFromResourcePath(String resourcePath) {
        DavResourceLocator loc = locator.getFactory().createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(), resourcePath);
        return loc;
    }

    /**
     * Retrieve the name/label of a repository item from the given href by
     * splitting of the part after the last slash. If the removeIndex
     * flag is set to true, any trailing index (e.g. '[1]') will be removed.
     *
     * @param resourceHref
     * @param removeIndex
     * @return the name of the item
     */
    protected static String getResourceName(String resourceHref, boolean removeIndex) {
        if (resourceHref == null) {
            return resourceHref;
        }

        // cut the extension
        int pos = resourceHref.lastIndexOf('.');
        if (pos > 0) {
            resourceHref = resourceHref.substring(pos+1);
        } else if (resourceHref.endsWith("/")) {
            resourceHref = resourceHref.substring(0, resourceHref.length()-1);
        }

        // retrieve the last part of the path
        String name = Text.getLabel(resourceHref);
        // remove index
        if (removeIndex) {
            if (name.endsWith("]")) {
                name = name.substring(0, name.lastIndexOf('['));
            }
        }
        return name;
    }
}