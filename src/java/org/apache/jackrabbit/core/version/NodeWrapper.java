/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.QName;

import javax.jcr.*;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import java.io.InputStream;
import java.util.Calendar;

/**
 * This Class implements a generic wrapper around a node object.
 */
public class NodeWrapper implements Node {

    /**
     * the internal node
     */
    private final NodeImpl delegatee;

    /**
     * Creates a new wrapper for the given node.
     *
     * @param delegatee
     */
    public NodeWrapper(NodeImpl delegatee) {
        this.delegatee = delegatee;
    }

    /**
     * Returns the delegatee of this wrapper.
     *
     * @return the node that is wrapped.
     */
    public Node unwrap() {
        return delegatee;
    }

    /**
     * @see Node#addNode(String)
     */
    public Node addNode(String relPath) throws ItemExistsException, PathNotFoundException, ConstraintViolationException, RepositoryException {
        return delegatee.addNode(relPath);
    }

    /**
     * @see Node#addNode(String, String)
     */
    public Node addNode(String relPath, String primaryNodeTypeName) throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, ConstraintViolationException, RepositoryException {
        return delegatee.addNode(relPath, primaryNodeTypeName);
    }

    /**
     * @see Node#orderBefore(String, String)
     */
    public void orderBefore(String srcName, String destName) throws UnsupportedRepositoryOperationException, ConstraintViolationException, ItemNotFoundException, RepositoryException {
        delegatee.orderBefore(srcName, destName);
    }

    /**
     * @see Node#setProperty(String, javax.jcr.Value)
     */
    public Property setProperty(String name, Value value) throws ValueFormatException, RepositoryException {
        return delegatee.setProperty(name, value);
    }

    /**
     * @see Node#setProperty(String, javax.jcr.Value[])
     */
    public Property setProperty(String name, Value[] values) throws ValueFormatException, RepositoryException {
        return delegatee.setProperty(name, values);
    }

    /**
     * @see Node#setProperty(String, String[])
     */
    public Property setProperty(String name, String[] values) throws ValueFormatException, RepositoryException {
        return delegatee.setProperty(name, values);
    }

    /**
     * @see Node#setProperty(String, String)
     */
    public Property setProperty(String name, String value) throws ValueFormatException, RepositoryException {
        return delegatee.setProperty(name, value);
    }

    /**
     * @see Node#setProperty(String, java.io.InputStream)
     */
    public Property setProperty(String name, InputStream value) throws ValueFormatException, RepositoryException {
        return delegatee.setProperty(name, value);
    }

    /**
     * @see Node#setProperty(String, boolean)
     */
    public Property setProperty(String name, boolean value) throws ValueFormatException, RepositoryException {
        return delegatee.setProperty(name, value);
    }

    /**
     * @see Node#setProperty(String, double)
     */
    public Property setProperty(String name, double value) throws ValueFormatException, RepositoryException {
        return delegatee.setProperty(name, value);
    }

    /**
     * @see Node#setProperty(String, long)
     */
    public Property setProperty(String name, long value) throws ValueFormatException, RepositoryException {
        return delegatee.setProperty(name, value);
    }

    /**
     * @see Node#setProperty(String, java.util.Calendar)
     */
    public Property setProperty(String name, Calendar value) throws ValueFormatException, RepositoryException {
        return delegatee.setProperty(name, value);
    }

    /**
     * @see Node#setProperty(String, javax.jcr.Node)
     */
    public Property setProperty(String name, Node value) throws ValueFormatException, RepositoryException {
        return delegatee.setProperty(name, value);
    }

    /**
     * @see Node#getNode(String)
     */
    public Node getNode(String relPath) throws PathNotFoundException, RepositoryException {
        return delegatee.getNode(relPath);
    }

    /**
     * @see Node#getNodes()
     */
    public NodeIterator getNodes() throws RepositoryException {
        return delegatee.getNodes();
    }

    /**
     * @see Node#getNodes(String)
     */
    public NodeIterator getNodes(String namePattern) throws RepositoryException {
        return delegatee.getNodes(namePattern);
    }

    /**
     * @see Node#getProperty(String)
     */
    public Property getProperty(String relPath) throws PathNotFoundException, RepositoryException {
        return delegatee.getProperty(relPath);
    }

    public Property getProperty(QName name) throws PathNotFoundException, RepositoryException {
        return delegatee.getProperty(name);
    }

    /**
     * @see javax.jcr.Node#getProperties()
     */
    public PropertyIterator getProperties() throws RepositoryException {
        return delegatee.getProperties();
    }

    /**
     * @see Node#getProperties(String)
     */
    public PropertyIterator getProperties(String namePattern) throws RepositoryException {
        return delegatee.getProperties(namePattern);
    }

    /**
     * @see javax.jcr.Node#getPrimaryItem()
     */
    public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
        return delegatee.getPrimaryItem();
    }

    /**
     * @see javax.jcr.Node#getUUID()
     */
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        return delegatee.getUUID();
    }

    /**
     * @see javax.jcr.Node#getReferences()
     */
    public PropertyIterator getReferences() throws RepositoryException {
        return delegatee.getReferences();
    }

    /**
     * @see Node#hasNode(String)
     */
    public boolean hasNode(String relPath) throws RepositoryException {
        return delegatee.hasNode(relPath);
    }

    /**
     * @see Node#hasProperty(String)
     */
    public boolean hasProperty(String relPath) throws RepositoryException {
        return delegatee.hasProperty(relPath);
    }

    public boolean hasProperty(QName name) throws RepositoryException {
        return delegatee.hasProperty(name);
    }

    /**
     * @see javax.jcr.Node#hasNodes()
     */
    public boolean hasNodes() throws RepositoryException {
        return delegatee.hasNodes();
    }

    /**
     * @see javax.jcr.Node#hasProperties()
     */
    public boolean hasProperties() throws RepositoryException {
        return delegatee.hasProperties();
    }

    /**
     * @see javax.jcr.Node#getPrimaryNodeType()
     */
    public NodeType getPrimaryNodeType() throws RepositoryException {
        return delegatee.getPrimaryNodeType();
    }

    /**
     * @see javax.jcr.Node#getMixinNodeTypes()
     */
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        return delegatee.getMixinNodeTypes();
    }

    /**
     * @see Node#isNodeType(String)
     */
    public boolean isNodeType(String nodeTypeName) throws RepositoryException {
        return delegatee.isNodeType(nodeTypeName);
    }

    /**
     * @see Node#addMixin(String)
     */
    public void addMixin(String mixinName) throws NoSuchNodeTypeException, ConstraintViolationException, RepositoryException {
        delegatee.addMixin(mixinName);
    }

    /**
     * @see Node#removeMixin(String)
     */
    public void removeMixin(String mixinName) throws NoSuchNodeTypeException, ConstraintViolationException, RepositoryException {
        delegatee.removeMixin(mixinName);
    }

    /**
     * @see Node#canAddMixin(String)
     */
    public boolean canAddMixin(String mixinName) throws RepositoryException {
        return delegatee.canAddMixin(mixinName);
    }

    /**
     * @see javax.jcr.Node#getDefinition()
     */
    public NodeDef getDefinition() throws RepositoryException {
        return delegatee.getDefinition();
    }

    /**
     * @see javax.jcr.Node#checkin()
     */
    public Version checkin() throws UnsupportedRepositoryOperationException, RepositoryException {
        return delegatee.checkin();
    }

    /**
     * @see javax.jcr.Node#checkout()
     */
    public void checkout() throws UnsupportedRepositoryOperationException, RepositoryException {
        delegatee.checkout();
    }

    /**
     * @see Node#addPredecessor(javax.jcr.version.Version)
     */
    public void addPredecessor(Version v) throws VersionException, UnsupportedRepositoryOperationException, RepositoryException {
        delegatee.addPredecessor(v);
    }

    /**
     * @see Node#removePredecessor(javax.jcr.version.Version)
     */
    public void removePredecessor(Version v) throws VersionException, UnsupportedRepositoryOperationException, RepositoryException {
        delegatee.removePredecessor(v);
    }

    /**
     * @see Node#update(String)
     */
    public void update(String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
        delegatee.update(srcWorkspaceName);
    }

    /**
     * @see Node#merge(String, boolean)
     */
    public void merge(String srcWorkspace, boolean bestEffort) throws UnsupportedRepositoryOperationException, NoSuchWorkspaceException, AccessDeniedException, MergeException, RepositoryException {
        delegatee.merge(srcWorkspace, bestEffort);
    }

    /**
     * @see javax.jcr.Node#isCheckedOut()
     */
    public boolean isCheckedOut() throws UnsupportedRepositoryOperationException, RepositoryException {
        return delegatee.isCheckedOut();
    }

    /**
     * @see Node#restore(String)
     */
    public void restore(String versionName) throws VersionException, UnsupportedRepositoryOperationException, RepositoryException {
        delegatee.restore(versionName);
    }

    /**
     * @see Node#restore(javax.jcr.version.Version)
     */
    public void restore(Version version) throws UnsupportedRepositoryOperationException, RepositoryException {
        delegatee.restore(version);
    }

    /**
     * @see Node#restore(javax.jcr.version.Version, String)
     */
    public void restore(Version version, String relPath) throws PathNotFoundException, ItemExistsException, ConstraintViolationException, UnsupportedRepositoryOperationException, RepositoryException {
        delegatee.restore(version, relPath);
    }

    /**
     * @see Node#restoreByLabel(String)
     */
    public void restoreByLabel(String versionLabel) throws UnsupportedRepositoryOperationException, RepositoryException {
        delegatee.restoreByLabel(versionLabel);
    }

    /**
     * @see javax.jcr.Node#getVersionHistory()
     */
    public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
        return delegatee.getVersionHistory();
    }

    /**
     * @see javax.jcr.Node#getBaseVersion()
     */
    public Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
        return delegatee.getBaseVersion();
    }

    /**
     * @see Node#lock(boolean, boolean)
     */
    public Lock lock(boolean isDeep, boolean isSessionScoped) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        return delegatee.lock(isDeep, isSessionScoped);
    }

    /**
     * @see javax.jcr.Node#getLock()
     */
    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        return delegatee.getLock();
    }

    /**
     * @see javax.jcr.Node#unlock()
     */
    public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        delegatee.unlock();
    }

    /**
     * @see javax.jcr.Node#holdsLock()
     */
    public boolean holdsLock() throws RepositoryException {
        return delegatee.holdsLock();
    }

    /**
     * @see javax.jcr.Node#isLocked()
     */
    public boolean isLocked() throws RepositoryException {
        return delegatee.isLocked();
    }

    /**
     * @see javax.jcr.Node#getPath()
     */
    public String getPath() throws RepositoryException {
        return delegatee.getPath();
    }

    /**
     * @see javax.jcr.Node#getName()
     */
    public String getName() throws RepositoryException {
        return delegatee.getName();
    }

    /**
     * @see Node#getAncestor(int)
     */
    public Item getAncestor(int depth) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        return delegatee.getAncestor(depth);
    }

    /**
     * @see javax.jcr.Node#getParent()
     */
    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        return delegatee.getParent();
    }

    /**
     * @see javax.jcr.Node#getDepth()
     */
    public int getDepth() throws RepositoryException {
        return delegatee.getDepth();
    }

    /**
     * @see javax.jcr.Node#getSession()
     */
    public Session getSession() throws RepositoryException {
        return delegatee.getSession();
    }

    /**
     * @see javax.jcr.Node#isNode()
     */
    public boolean isNode() {
        return delegatee.isNode();
    }

    /**
     * @see javax.jcr.Node#isNew()
     */
    public boolean isNew() {
        return delegatee.isNew();
    }

    /**
     * @see javax.jcr.Node#isModified()
     */
    public boolean isModified() {
        return delegatee.isModified();
    }

    /**
     * @see Node#isSame(javax.jcr.Item)
     */
    public boolean isSame(Item otherItem) {
        return delegatee.isSame(otherItem);
    }

    /**
     * @see Node#accept(javax.jcr.ItemVisitor)
     */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        delegatee.accept(visitor);
    }

    /**
     * @see Node#save()
     */
    public void save() throws AccessDeniedException, LockException, ConstraintViolationException, InvalidItemStateException, RepositoryException {
        delegatee.save();
    }

    /**
     * @see Node#refresh(boolean)
     */
    public void refresh(boolean keepChanges) throws InvalidItemStateException, RepositoryException {
        delegatee.refresh(keepChanges);
    }

    /**
     * @see Node#remove()
     */
    public void remove() throws RepositoryException {
        delegatee.remove();
    }
}
