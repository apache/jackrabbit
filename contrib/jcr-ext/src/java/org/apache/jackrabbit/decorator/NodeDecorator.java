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
package org.apache.jackrabbit.decorator;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

public class NodeDecorator extends ChainedItemDecorator implements Node {

    private DecoratorFactory factory;

    private Session session;

    private Node node;

    public NodeDecorator(
            ItemDecorator decorator,
            DecoratorFactory factory, Session session, Node node) {
        super(decorator);
        this.factory = factory;
        this.session = session;
        this.node = node;
    }

    public Node addNode(String name) throws ItemExistsException,
            PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        Node child = node.addNode(name);
        return factory.getNodeDecorator(session, child);
    }

    public Node addNode(String name, String type) throws ItemExistsException,
            PathNotFoundException, NoSuchNodeTypeException, LockException,
            VersionException, ConstraintViolationException, RepositoryException {
        Node child = node.addNode(name, type);
        return factory.getNodeDecorator(session, child);
    }

    public void orderBefore(String arg0, String arg1)
            throws UnsupportedRepositoryOperationException, VersionException,
            ConstraintViolationException, ItemNotFoundException, LockException,
            RepositoryException {
        // TODO Auto-generated method stub

    }

    public Property setProperty(String arg0, Value arg1)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Property setProperty(String arg0, Value arg1, int arg2)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Property setProperty(String arg0, Value[] arg1)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Property setProperty(String arg0, Value[] arg1, int arg2)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Property setProperty(String arg0, String[] arg1)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Property setProperty(String arg0, String[] arg1, int arg2)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Property setProperty(String arg0, String arg1)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Property setProperty(String arg0, String arg1, int arg2)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Property setProperty(String arg0, InputStream arg1)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Property setProperty(String arg0, boolean arg1)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Property setProperty(String arg0, double arg1)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Property setProperty(String arg0, long arg1)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Property setProperty(String arg0, Calendar arg1)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Property setProperty(String arg0, Node arg1)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Node getNode(String arg0) throws PathNotFoundException,
            RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public NodeIterator getNodes() throws RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public NodeIterator getNodes(String arg0) throws RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Property getProperty(String arg0) throws PathNotFoundException,
            RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public PropertyIterator getProperties() throws RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public PropertyIterator getProperties(String arg0)
            throws RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Item getPrimaryItem() throws ItemNotFoundException,
            RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getUUID() throws UnsupportedRepositoryOperationException,
            RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public int getIndex() throws RepositoryException {
        // TODO Auto-generated method stub
        return 0;
    }

    public PropertyIterator getReferences() throws RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean hasNode(String arg0) throws RepositoryException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean hasProperty(String arg0) throws RepositoryException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean hasNodes() throws RepositoryException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean hasProperties() throws RepositoryException {
        // TODO Auto-generated method stub
        return false;
    }

    public NodeType getPrimaryNodeType() throws RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isNodeType(String arg0) throws RepositoryException {
        // TODO Auto-generated method stub
        return false;
    }

    public void addMixin(String arg0) throws NoSuchNodeTypeException,
            VersionException, ConstraintViolationException, LockException,
            RepositoryException {
        // TODO Auto-generated method stub

    }

    public void removeMixin(String arg0) throws NoSuchNodeTypeException,
            VersionException, ConstraintViolationException, LockException,
            RepositoryException {
        // TODO Auto-generated method stub

    }

    public boolean canAddMixin(String arg0) throws NoSuchNodeTypeException,
            RepositoryException {
        // TODO Auto-generated method stub
        return false;
    }

    public NodeDefinition getDefinition() throws RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Version checkin() throws VersionException,
            UnsupportedRepositoryOperationException, InvalidItemStateException,
            LockException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public void checkout() throws UnsupportedRepositoryOperationException,
            LockException, RepositoryException {
        // TODO Auto-generated method stub

    }

    public void doneMerge(Version arg0) throws VersionException,
            InvalidItemStateException, UnsupportedRepositoryOperationException,
            RepositoryException {
        // TODO Auto-generated method stub

    }

    public void cancelMerge(Version arg0) throws VersionException,
            InvalidItemStateException, UnsupportedRepositoryOperationException,
            RepositoryException {
        // TODO Auto-generated method stub

    }

    public void update(String arg0) throws NoSuchWorkspaceException,
            AccessDeniedException, LockException, InvalidItemStateException,
            RepositoryException {
        // TODO Auto-generated method stub

    }

    public NodeIterator merge(String arg0, boolean arg1)
            throws NoSuchWorkspaceException, AccessDeniedException,
            VersionException, LockException, InvalidItemStateException,
            RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getCorrespondingNodePath(String arg0)
            throws ItemNotFoundException, NoSuchWorkspaceException,
            AccessDeniedException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isCheckedOut() throws RepositoryException {
        // TODO Auto-generated method stub
        return false;
    }

    public void restore(String arg0, boolean arg1) throws VersionException,
            ItemExistsException, UnsupportedRepositoryOperationException,
            LockException, InvalidItemStateException, RepositoryException {
        // TODO Auto-generated method stub

    }

    public void restore(Version arg0, boolean arg1) throws VersionException,
            ItemExistsException, UnsupportedRepositoryOperationException,
            LockException, RepositoryException {
        // TODO Auto-generated method stub

    }

    public void restore(Version arg0, String arg1, boolean arg2)
            throws PathNotFoundException, ItemExistsException,
            VersionException, ConstraintViolationException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {
        // TODO Auto-generated method stub

    }

    public void restoreByLabel(String arg0, boolean arg1)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {
        // TODO Auto-generated method stub

    }

    public VersionHistory getVersionHistory()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Version getBaseVersion()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Lock lock(boolean arg0, boolean arg1)
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, InvalidItemStateException,
            RepositoryException {
        Lock lock = node.lock(arg0, arg1);
        return factory.getLockDecorator(this, lock);
    }

    public Lock getLock() throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, RepositoryException {
        return node.getLock();
    }

    public void unlock() throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, InvalidItemStateException,
            RepositoryException {
        node.unlock();
    }

    public boolean holdsLock() throws RepositoryException {
        // TODO Auto-generated method stub
        return node.holdsLock();
    }

    public boolean isLocked() throws RepositoryException {
        return node.isLocked();
    }

}
