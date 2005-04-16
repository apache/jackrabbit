/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.base;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.BinaryValue;
import javax.jcr.BooleanValue;
import javax.jcr.DateValue;
import javax.jcr.DoubleValue;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.LongValue;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.ReferenceValue;
import javax.jcr.RepositoryException;
import javax.jcr.StringValue;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

/**
 * TODO
 */
public class BaseNode extends BaseItem implements Node {

    protected BaseNode() {
    }

    protected BaseNode(Item item) {
        super(item);
    }

    public String getPath() throws RepositoryException {
        if (getDefinition().allowSameNameSibs()) {
            return super.getPath() + "[" + getIndex() + "]";
        } else {
            return super.getPath();
        }
    }

    public void accept(ItemVisitor visitor) throws RepositoryException {
        visitor.visit(this);
    }

    public boolean isNode() {
        return true;
    }

    /** {@inheritDoc} */
    public Node addNode(String relPath) throws ItemExistsException,
            PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public Node addNode(String relPath, String primaryNodeTypeName)
            throws ItemExistsException, PathNotFoundException,
            NoSuchNodeTypeException, LockException, VersionException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public void orderBefore(String srcChildRelPath, String destChildRelPath)
            throws UnsupportedRepositoryOperationException, VersionException,
            ConstraintViolationException, ItemNotFoundException, LockException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, Value value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, Value[] values)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, Value[] values, int type)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, String[] values)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, String[] values, int type)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, String value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        return setProperty(name, new StringValue(value));
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, InputStream value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        return setProperty(name, new BinaryValue(value));
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, boolean value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        return setProperty(name, new BooleanValue(value));
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, double value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        return setProperty(name, new DoubleValue(value));
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, long value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        return setProperty(name, new LongValue(value));
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, Calendar value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        return setProperty(name, new DateValue(value));
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, Node value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        return setProperty(name, new ReferenceValue(value));
    }

    /** {@inheritDoc} */
    public Node getNode(String relPath) throws PathNotFoundException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public NodeIterator getNodes() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public NodeIterator getNodes(String namePattern) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public Property getProperty(String relPath) throws PathNotFoundException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public PropertyIterator getProperties() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public PropertyIterator getProperties(String namePattern)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public Item getPrimaryItem() throws ItemNotFoundException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public String getUUID() throws UnsupportedRepositoryOperationException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public int getIndex() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public PropertyIterator getReferences() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public boolean hasNode(String relPath) throws RepositoryException {
        try {
            getNode(relPath);
            return true;
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    /** {@inheritDoc} */
    public boolean hasProperty(String relPath) throws RepositoryException {
        try {
            getProperty(relPath);
            return true;
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    /** {@inheritDoc} */
    public boolean hasNodes() throws RepositoryException {
        return getNodes().getSize() > 0;
    }

    /** {@inheritDoc} */
    public boolean hasProperties() throws RepositoryException {
        return getProperties().getSize() > 0;
    }

    /** {@inheritDoc} */
    public NodeType getPrimaryNodeType() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public boolean isNodeType(String nodeTypeName) throws RepositoryException {
        if (getPrimaryNodeType().isNodeType(nodeTypeName)) {
            return true;
        } else {
            NodeType[] types = getMixinNodeTypes();
            for (int i = 0; i < types.length; i++) {
                if (types[i].isNodeType(nodeTypeName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    public void addMixin(String mixinName) throws NoSuchNodeTypeException,
            VersionException, ConstraintViolationException, LockException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public void removeMixin(String mixinName) throws NoSuchNodeTypeException,
            VersionException, ConstraintViolationException, LockException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public boolean canAddMixin(String mixinName) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public NodeDef getDefinition() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public Version checkin() throws VersionException,
            UnsupportedRepositoryOperationException, InvalidItemStateException,
            LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public void checkout() throws UnsupportedRepositoryOperationException,
            LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public void doneMerge(Version version) throws VersionException,
            InvalidItemStateException, UnsupportedRepositoryOperationException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public void cancelMerge(Version version) throws VersionException,
            InvalidItemStateException, UnsupportedRepositoryOperationException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public void update(String srcWorkspaceName)
            throws NoSuchWorkspaceException, AccessDeniedException,
            LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public void merge(String srcWorkspace, boolean bestEffort)
            throws UnsupportedRepositoryOperationException,
            NoSuchWorkspaceException, AccessDeniedException, VersionException,
            LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public String getCorrespondingNodePath(String workspaceName)
            throws ItemNotFoundException, NoSuchWorkspaceException,
            AccessDeniedException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public boolean isCheckedOut() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public void restore(String versionName, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public void restore(Version version, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public void restore(Version version, String relPath, boolean removeExisting)
            throws PathNotFoundException, ItemExistsException,
            VersionException, ConstraintViolationException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public void restoreByLabel(String versionLabel, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public VersionHistory getVersionHistory()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public Version getBaseVersion()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public Lock lock(boolean isDeep, boolean isSessionScoped)
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, InvalidItemStateException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public Lock getLock() throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public void unlock() throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, InvalidItemStateException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public boolean holdsLock() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public boolean isLocked() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

}
