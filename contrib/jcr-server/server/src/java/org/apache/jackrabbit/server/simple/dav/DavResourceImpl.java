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
package org.apache.jackrabbit.server.simple.dav;

import javax.jcr.*;
import javax.jcr.lock.Lock;
import java.util.*;
import java.io.*;

import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.util.Text;
import org.apache.jackrabbit.webdav.spi.lock.JcrActiveLock;
import org.apache.jackrabbit.webdav.spi.JcrDavException;
import org.apache.jackrabbit.webdav.lock.*;
import org.apache.jackrabbit.webdav.property.*;

/**
 * DavResourceImpl imeplements a DavResource.
 */
public class DavResourceImpl implements DavResource {

    private DavResourceFactory factory;
    private LockManager lockManager;
    private DavSession session;
    private Node node;
    private DavResourceLocator locator;

    private DavPropertySet properties;
    private boolean isCollection = true;

    /** is created on initProperties */
    private NodeResource nodeResource;

    /**
     * Create a new {@link DavResource}.
     *
     * @param locator
     * @param factory
     * @param session
     */
    public DavResourceImpl(DavResourceLocator locator, DavResourceFactory factory, DavSession session) {
        this.session = session;
        this.factory = factory;
        this.locator = locator;
        if (locator != null && locator.getResourcePath() != null) {
            try {
                init(session.getRepositorySession().getItem(locator.getResourcePath()));
            } catch (RepositoryException e) {
                // ignore: exists field evaluates to false
            }
        }
    }

    /**
     * Init the webdav resource and retrieve the relevant property.
     *
     * @param repositoryItem
     * @throws RepositoryException
     */
    private void init(Item repositoryItem) throws RepositoryException {
        if (repositoryItem == null || !repositoryItem.isNode()) {
            return;
        }
        node = (Node)repositoryItem;

        // define what is a resource in webdav
        if (node.isNodeType("nt:resource") || node.isNodeType("nt:file")) {
            isCollection = false;
        }
    }

    /**
     * @return DavResource#COMPLIANCE_CLASS
     * @see org.apache.jackrabbit.webdav.DavResource#getComplianceClass()
     */
    public String getComplianceClass() {
        return DavResource.COMPLIANCE_CLASS;
    }

    /**
     * @return DavResource#METHODS
     * @see org.apache.jackrabbit.webdav.DavResource#getSupportedMethods()
     */
    public String getSupportedMethods() {
        return DavResource.METHODS;
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
     * @see DavResource#getDisplayName()
     */
    public String getDisplayName() {
        String name = null;
        if (exists()) {
            try {
                name = node.getName();
            } catch (RepositoryException e) {
                // ignore
            }
        }
        if (name == null && getResourcePath() != null) {
            name = Text.getLabel(getResourcePath());
        }
        return name;
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getModificationTime()
     */
    public long getModificationTime() {
	initProperties();
	return nodeResource == null ? 0 : nodeResource.getModificationTime();
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getStream()
     */
    public InputStream getStream() {
	initProperties();
	return nodeResource == null ? null : nodeResource.getStream();
    }

    /**
     * @see DavResource#getProperty(org.apache.jackrabbit.webdav.property.DavPropertyName)
     */
    public DavProperty getProperty(DavPropertyName name) {
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
    private void initProperties() {
        if (properties == null && exists()) {
            properties = new DavPropertySet();
            try {
                nodeResource = new NodeResource(this, node);
                properties.add(new DefaultDavProperty(DavPropertyName.GETCONTENTLENGTH, nodeResource.getContentLength()+""));
                properties.add(new DefaultDavProperty(DavPropertyName.CREATIONDATE, nodeResource.getCreationDate()));
                properties.add(new DefaultDavProperty(DavPropertyName.GETLASTMODIFIED, nodeResource.getLastModified()));
                String contentType = nodeResource.getContentType();
                if (contentType != null) {
                    properties.add(new DefaultDavProperty(DavPropertyName.GETCONTENTTYPE, contentType));
                }
                properties.add(new DefaultDavProperty(DavPropertyName.GETETAG, nodeResource.getETag()));
            } catch (RepositoryException e) {
                // should not occure....
            }

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

            /* set current lock information. If no lock is set to this resource,
            an empty lockdiscovery will be returned in the response. */
            properties.add(new LockDiscovery(getLock(Type.WRITE, Scope.EXCLUSIVE)));

            /* lock support information: all locks are lockable. */
            SupportedLock supportedLock = new SupportedLock();
            supportedLock.addEntry(Type.WRITE, Scope.EXCLUSIVE);
            properties.add(supportedLock);
        }
    }

    /**
     * @param property
     * @throws DavException
     * @see DavResource#setProperty(org.apache.jackrabbit.webdav.property.DavProperty)
     */
    public void setProperty(DavProperty property) throws DavException {
        if (isLocked(this)) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
        throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * @param propertyName
     * @throws DavException
     * @see DavResource#removeProperty(org.apache.jackrabbit.webdav.property.DavPropertyName)
     */
    public void removeProperty(DavPropertyName propertyName) throws DavException {
        if (isLocked(this)) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
        throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * @see DavResource#getCollection()
     */
    public DavResource getCollection() {
        DavResource parent = null;
        if (getResourcePath() != null && !getResourcePath().equals("/")) {
            String parentPath = Text.getRelativeParent(getResourcePath(), 1);
            if (parentPath.equals("")) {
                parentPath="/";
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
        ArrayList list = new ArrayList();
        if (exists() && isCollection()) {
            try {
                NodeIterator it = node.getNodes();
                while(it.hasNext()) {
                    list.add(buildResourceFromItem(it.nextNode()));
                }
            } catch (RepositoryException e) {
                // should not occure
            } catch (DavException e) {
                // should not occure
            }
        }
        return new DavResourceIteratorImpl(list);
    }

    /**
     * @see DavResource#addMember(DavResource, InputStream)
     */
    public void addMember(DavResource member, InputStream in) throws DavException {
        if (!exists() || in == null) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }
	if (isLocked(this)) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }

        try {
            String fileName = member.getDisplayName();
	    Node file;
	    boolean makeVersionable = true; // todo: to be configurable somewhere
	    if (node.hasNode(fileName)) {
		file = node.getNode(fileName);
		if (file.hasNode("jcr:content")) {
		    // remove an existing repository entry for 'overwriting' is not possible
		    file.getNode("jcr:content").remove();
		}
	    } else {
		file = node.addNode(fileName, "nt:file");
		if (makeVersionable) {
		    file.addMixin("mix:versionable");
		}
	    }

	    if (fileName.endsWith(".xml")) {
		importXml(file, in, "text/xml");
	    } else {
		// todo: retrieve proper mimetype from filename
		importFile(file, in, "application/octet-stream");
	    }
            session.getRepositorySession().save();
        } catch (ItemExistsException e) {
            // according to RFC 2518: MKCOL only possible on non-existing/deleted resource
            throw new JcrDavException(e, DavServletResponse.SC_METHOD_NOT_ALLOWED);
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        } catch (IOException e) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Imports a xml into the repository
     * @param parentNode
     * @param in
     * @param contentType
     * @throws RepositoryException
     * @throws IOException
     */
    private void importXml(Node parentNode, InputStream in, String contentType)
	    throws RepositoryException, IOException {
	Node content = parentNode.addNode("jcr:content", "nt:unstructured");
	content.setProperty("jcr:mimeType", contentType);
	content.setProperty("jcr:lastModified", Calendar.getInstance());
	session.getRepositorySession().importXML(content.getPath(), in);
    }

    /**
     * Imports a plain file to the repository
     * @param parentNode
     * @param in
     * @param contentType
     * @throws RepositoryException
     */
    private void importFile(Node parentNode, InputStream in, String contentType)
	    throws RepositoryException {
	Node content = parentNode.addNode("jcr:content", "nt:resource");
	content.setProperty("jcr:mimeType", contentType);
	content.setProperty("jcr:encoding", "");
	content.setProperty("jcr:data", in);
	content.setProperty("jcr:lastModified", Calendar.getInstance());
    }

    /**
     * @see DavResource#addMember(DavResource)
     */
    public void addMember(DavResource member) throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_CONFLICT);
        }
	if (isLocked(this)) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
        try {
            node.addNode(member.getDisplayName(), "nt:folder");
            node.save();
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
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

        try {
            // make sure, non-jcr locks are removed.
            if (!isJsrLockable()) {
                ActiveLock lock = getLock(Type.WRITE, Scope.EXCLUSIVE);
                if (lock != null) {
                    lockManager.releaseLock(lock.getToken(), member);
                }
            }
	    ActiveLock lock = getLock(Type.WRITE, Scope.EXCLUSIVE);
	    if (lock != null && lockManager.hasLock(lock.getToken(), member)) {
		lockManager.releaseLock(lock.getToken(), member);
	    }

            Session s = session.getRepositorySession();
            Item memItem = s.getItem(member.getResourcePath());
            memItem.remove();
            s.save();
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
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
        try {
            session.getRepositorySession().getWorkspace().move(getResourcePath(), destination.getResourcePath());
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
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
        // TODO: support shallow and deep copy
        if (shallow) {
            throw new DavException(DavServletResponse.SC_FORBIDDEN, "Unable to perform shallow copy.");
        }
        try {
            session.getRepositorySession().getWorkspace().copy(getResourcePath(), destination.getResourcePath());
        } catch (PathNotFoundException e) {
            // according to rfc 2518: missing parent
            throw new DavException(DavServletResponse.SC_CONFLICT, e.getMessage());
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
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
	    if (isJsrLockable()) {
                try {
                    Lock jcrLock = node.getLock();
                    if (jcrLock != null && jcrLock.isLive()) {
                        lock = new JcrActiveLock(jcrLock);
                    }
                } catch (RepositoryException e) {
                    // LockException: no lock applies to this node >> ignore
                    // RepositoryException, AccessDeniedException or another error >> ignore
                }
            } else {
                // not-jcr lockable >> check for webdav lock
                lock = lockManager.getLock(type, scope, this);
            }
        }
        return lock;
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getLocks()
     */
    public ActiveLock[] getLocks() {
        return new ActiveLock[] {getLock(Type.WRITE, Scope.EXCLUSIVE)};
    }

    /**
     * @see DavResource#lock(LockInfo)
     */
    public ActiveLock lock(LockInfo lockInfo) throws DavException {
	ActiveLock lock = null;
	if (isLockable(lockInfo.getType(), lockInfo.getScope())) {
	    if (isJsrLockable()) {
		try {
		    // try to execute the lock operation
		    Lock jcrLock = node.lock(lockInfo.isDeep(), false);
		    if (jcrLock != null) {
			lock = new JcrActiveLock(jcrLock);
		    }
		} catch (RepositoryException e) {
		    throw new JcrDavException(e);
		}
	    } else {
		// create a new lock which creates a random lock token
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
    public ActiveLock refreshLock(LockInfo lockInfo, String lockToken) throws DavException{
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        /* since lock is always has infinite timeout >> no extra refresh needed
        return a lockdiscovery with the lock-info and the default scope and type */
        ActiveLock lock = getLock(lockInfo.getType(), lockInfo.getScope());
        if (lock == null) {
           throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "No lock present on resource " + getResourcePath());
        } else if (lock.isLockedByToken(lockToken)) {
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
        } else {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
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
     * Returns true, if this webdav resource allows for locking without checking
     * its current lock status.
     *
     * @return true if this resource is lockable.
     */
    private boolean isJsrLockable() {
        boolean lockable = false;
        if (exists()) {
            try {
                lockable =  node.isNodeType("mix:lockable");
            } catch (RepositoryException e) {
                // not jcr-lockable
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
            String[] sLockTokens = session.getLockTokens();
            for (int i = 0; i < sLockTokens.length; i++) {
                if (sLockTokens[i].equals(lock.getToken())) {
                   return false;
                }
            }
            return true;
        }
    }

    /**
     * @param item
     * @return
     * @throws DavException
     * @throws RepositoryException
     */
    private DavResource buildResourceFromItem(Item item) throws DavException, RepositoryException {
        DavResourceLocator parentloc = locator.getFactory().createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(), item.getPath());
        return factory.createResource(parentloc, session);
    }
}
