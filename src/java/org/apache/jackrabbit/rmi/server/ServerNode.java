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
package org.apache.jackrabbit.rmi.server;

import java.rmi.RemoteException;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.rmi.remote.RemoteItem;
import org.apache.jackrabbit.rmi.remote.RemoteLock;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteNodeDef;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;
import org.apache.jackrabbit.rmi.remote.RemoteProperty;

/**
 * Remote adapter for the JCR {@link javax.jcr.Node Node} interface.
 * This class makes a local node available as an RMI service using
 * the {@link org.apache.jackrabbit.rmi.remote.RemoteNode RemoteNode}
 * interface.
 *
 * @author Jukka Zitting
 * @see javax.jcr.Node
 * @see org.apache.jackrabbit.rmi.remote.RemoteNode
 */
public class ServerNode extends ServerItem implements RemoteNode {

    /** The adapted local node. */
    private Node node;

    /**
     * Creates a remote adapter for the given local node.
     *
     * @param node local node
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerNode(Node node, RemoteAdapterFactory factory)
            throws RemoteException {
        super(node, factory);
        this.node = node;
    }

    /** {@inheritDoc} */
    public RemoteNode addNode(String path) throws ItemExistsException,
            PathNotFoundException, ConstraintViolationException,
            RepositoryException, RemoteException {
        try {
            return factory.getRemoteNode(node.addNode(path));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteNode addNode(String path, String type) throws
            ItemExistsException, PathNotFoundException,
            NoSuchNodeTypeException, ConstraintViolationException,
            RepositoryException, RemoteException {
        try {
            return factory.getRemoteNode(node.addNode(path, type));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteProperty getProperty(String path) throws PathNotFoundException,
            RepositoryException, RemoteException {
        try {
            return factory.getRemoteProperty(node.getProperty(path));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteProperty[] getProperties() throws RepositoryException,
            RemoteException {
        try {
            return getRemotePropertyArray(node.getProperties());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteItem getPrimaryItem() throws ItemNotFoundException,
            RepositoryException, RemoteException {
        try {
            return getRemoteItem(node.getPrimaryItem());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteProperty[] getProperties(String pattern)
            throws RepositoryException, RemoteException {
        try {
            return getRemotePropertyArray(node.getProperties(pattern));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteProperty[] getReferences() throws RepositoryException,
            RemoteException {
        try {
            return getRemotePropertyArray(node.getReferences());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getUUID() throws UnsupportedRepositoryOperationException,
            RepositoryException, RemoteException {
        try {
            return node.getUUID();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasNodes() throws RepositoryException, RemoteException {
        try {
            return node.hasNodes();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasProperties() throws RepositoryException, RemoteException {
        try {
            return node.hasProperties();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasProperty(String path) throws RepositoryException,
            RemoteException {
        try {
            return node.hasProperty(path);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteNodeType[] getMixinNodeTypes() throws RepositoryException,
            RemoteException {
        try {
            return getRemoteNodeTypeArray(node.getMixinNodeTypes());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteNodeType getPrimaryNodeType() throws RepositoryException,
            RemoteException {
        try {
            return factory.getRemoteNodeType(node.getPrimaryNodeType());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isNodeType(String type) throws RepositoryException,
            RemoteException {
        try {
            return node.isNodeType(type);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteNode[] getNodes() throws RepositoryException, RemoteException {
        try {
            return getRemoteNodeArray(node.getNodes());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteNode[] getNodes(String pattern) throws RepositoryException,
            RemoteException {
        try {
            return getRemoteNodeArray(node.getNodes(pattern));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteNode getNode(String path) throws PathNotFoundException,
            RepositoryException, RemoteException {
        try {
            return factory.getRemoteNode(node.getNode(path));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasNode(String path) throws RepositoryException,
            RemoteException {
        try {
            return node.hasNode(path);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteProperty setProperty(String name, Value value)
            throws ValueFormatException, RepositoryException, RemoteException {
        try {
            return factory.getRemoteProperty(node.setProperty(name, value));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void addMixin(String name) throws NoSuchNodeTypeException,
            ConstraintViolationException, RepositoryException, RemoteException {
        try {
            node.addMixin(name);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean canAddMixin(String name) throws RepositoryException,
            RemoteException {
        try {
            return node.canAddMixin(name);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void removeMixin(String name) throws NoSuchNodeTypeException,
            ConstraintViolationException, RepositoryException, RemoteException {
        try {
            node.removeMixin(name);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void orderBefore(String src, String dst)
            throws UnsupportedRepositoryOperationException,
            ConstraintViolationException, ItemNotFoundException,
            RepositoryException, RemoteException {
        try {
            node.orderBefore(src, dst);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteProperty setProperty(String name, Value[] values)
            throws ValueFormatException, RepositoryException, RemoteException {
        try {
            return factory.getRemoteProperty(node.setProperty(name, values));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteNodeDef getDefinition() throws RepositoryException,
            RemoteException {
        try {
            return factory.getRemoteNodeDef(node.getDefinition());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void checkout() throws UnsupportedRepositoryOperationException,
            RepositoryException, RemoteException {
        try {
            node.checkout();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getCorrespondingNodePath(String workspace)
            throws ItemNotFoundException, NoSuchWorkspaceException,
            AccessDeniedException, RepositoryException, RemoteException {
        try {
            return node.getCorrespondingNodePath(workspace);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public int getIndex() throws RepositoryException, RemoteException {
        try {
            return node.getIndex();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void merge(String workspace, boolean bestEffort)
            throws UnsupportedRepositoryOperationException,
            NoSuchWorkspaceException, AccessDeniedException, MergeException,
            RepositoryException, RemoteException {
        try {
            node.merge(workspace, bestEffort);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void restore(String version, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException, RemoteException {
        try {
            node.restore(version, removeExisting);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void restoreByLabel(String label, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException, RemoteException {
        try {
            node.restoreByLabel(label, removeExisting);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void update(String workspace) throws NoSuchWorkspaceException,
            AccessDeniedException, RepositoryException, RemoteException {
        try {
            node.update(workspace);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean holdsLock() throws RepositoryException, RemoteException {
        try {
            return node.holdsLock();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isCheckedOut() throws
            UnsupportedRepositoryOperationException, RepositoryException,
            RemoteException {
        try {
            return node.isCheckedOut();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isLocked() throws RepositoryException, RemoteException {
        try {
            return node.isLocked();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteProperty setProperty(String name, Value[] values, int type)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException, RemoteException {
        try {
            return factory.getRemoteProperty(node.setProperty(name, values, type));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void unlock() throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, RepositoryException,
            RemoteException {
        try {
            node.unlock();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteLock getLock() throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, RepositoryException,
            RemoteException {
        try {
            return factory.getRemoteLock(node.getLock());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteLock lock(boolean isDeep, boolean isSessionScoped)
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, RepositoryException, RemoteException {
        try {
            return factory.getRemoteLock(node.lock(isDeep, isSessionScoped));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
}
