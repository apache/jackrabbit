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
    protected Node node;
    
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
        return factory.getRemoteNode(node.addNode(path));
    }
    
    /** {@inheritDoc} */
    public RemoteNode addNode(String path, String type) throws
            ItemExistsException, PathNotFoundException,
            NoSuchNodeTypeException, ConstraintViolationException,
            RepositoryException, RemoteException {
        return factory.getRemoteNode(node.addNode(path, type));
    }
    
    /** {@inheritDoc} */
    public RemoteProperty getProperty(String path) throws PathNotFoundException,
            RepositoryException, RemoteException {
        return factory.getRemoteProperty(node.getProperty(path));
    }

    /** {@inheritDoc} */
    public RemoteProperty[] getProperties() throws RepositoryException,
            RemoteException {
        return getRemotePropertyArray(node.getProperties());
    }    
    
    /** {@inheritDoc} */
    public RemoteItem getPrimaryItem() throws ItemNotFoundException,
            RepositoryException, RemoteException {
        return getRemoteItem(node.getPrimaryItem());
    }
    
    /** {@inheritDoc} */
    public RemoteProperty[] getProperties(String pattern)
            throws RepositoryException, RemoteException {
        return getRemotePropertyArray(node.getProperties(pattern));
    }
    
    /** {@inheritDoc} */
    public RemoteProperty[] getReferences() throws RepositoryException,
            RemoteException {
        return getRemotePropertyArray(node.getReferences());
    }
    
    /** {@inheritDoc} */
    public String getUUID() throws UnsupportedRepositoryOperationException,
            RepositoryException, RemoteException {
        return node.getUUID();
    }
    
    /** {@inheritDoc} */
    public boolean hasNodes() throws RepositoryException, RemoteException {
        return node.hasNodes();
    }
    
    /** {@inheritDoc} */
    public boolean hasProperties() throws RepositoryException, RemoteException {
        return node.hasProperties();
    }
    
    /** {@inheritDoc} */
    public boolean hasProperty(String path) throws RepositoryException,
            RemoteException {
        return node.hasProperty(path);
    }
    
    /** {@inheritDoc} */
    public RemoteNodeType[] getMixinNodeTypes() throws RepositoryException,
            RemoteException {
        return getRemoteNodeTypeArray(node.getMixinNodeTypes());
    }
    
    /** {@inheritDoc} */
    public RemoteNodeType getPrimaryNodeType() throws RepositoryException,
            RemoteException {
        return factory.getRemoteNodeType(node.getPrimaryNodeType());
    }

    /** {@inheritDoc} */
    public boolean isNodeType(String type) throws RepositoryException,
            RemoteException {
        return node.isNodeType(type);
    }
    
    /** {@inheritDoc} */
    public RemoteNode[] getNodes() throws RepositoryException, RemoteException {
        return getRemoteNodeArray(node.getNodes());
    }

    /** {@inheritDoc} */
    public RemoteNode[] getNodes(String pattern) throws RepositoryException,
            RemoteException {
        return getRemoteNodeArray(node.getNodes(pattern));
    }

    /** {@inheritDoc} */
    public RemoteNode getNode(String path) throws PathNotFoundException,
            RepositoryException, RemoteException {
        return factory.getRemoteNode(node.getNode(path));
    }
    
    /** {@inheritDoc} */
    public boolean hasNode(String path) throws RepositoryException,
            RemoteException {
        return node.hasNode(path);
    }
    
    /** {@inheritDoc} */
    public RemoteProperty setProperty(String name, Value value)
            throws ValueFormatException, RepositoryException, RemoteException {
        return factory.getRemoteProperty(node.setProperty(name, value));
    }
    
    /** {@inheritDoc} */
    public void addMixin(String name) throws NoSuchNodeTypeException,
            ConstraintViolationException, RepositoryException, RemoteException {
        node.addMixin(name);
    }

    /** {@inheritDoc} */
    public boolean canAddMixin(String name) throws RepositoryException,
            RemoteException {
        return node.canAddMixin(name);
    }
    
    /** {@inheritDoc} */
    public void removeMixin(String name) throws NoSuchNodeTypeException,
            ConstraintViolationException, RepositoryException, RemoteException {
        node.removeMixin(name);
    }

    /** {@inheritDoc} */
    public void orderBefore(String src, String dst)
            throws UnsupportedRepositoryOperationException,
            ConstraintViolationException, ItemNotFoundException,
            RepositoryException, RemoteException {
        node.orderBefore(src, dst);
    }
    
    /** {@inheritDoc} */
    public RemoteProperty setProperty(String name, Value[] values)
            throws ValueFormatException, RepositoryException, RemoteException {
        return factory.getRemoteProperty(node.setProperty(name, values));
    }

    /** {@inheritDoc} */
    public RemoteNodeDef getDefinition() throws RepositoryException,
            RemoteException {
        return factory.getRemoteNodeDef(node.getDefinition());
    }
    
    /** {@inheritDoc} */
    public void checkout() throws UnsupportedRepositoryOperationException,
            RepositoryException, RemoteException {
        node.checkout();
    }
    
    /** {@inheritDoc} */
    public String getCorrespondingNodePath(String workspace)
            throws ItemNotFoundException, NoSuchWorkspaceException,
            AccessDeniedException, RepositoryException, RemoteException {
        return node.getCorrespondingNodePath(workspace);
    }
    
    /** {@inheritDoc} */
    public int getIndex() throws RepositoryException, RemoteException {
        return node.getIndex();
    }
    
    /** {@inheritDoc} */
    public void merge(String workspace, boolean bestEffort)
            throws UnsupportedRepositoryOperationException,
            NoSuchWorkspaceException, AccessDeniedException, MergeException,
            RepositoryException, RemoteException {
        node.merge(workspace, bestEffort);
    }
    
    /** {@inheritDoc} */
    public void restore(String version, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException, RemoteException {
        node.restore(version, removeExisting);
    }
    
    /** {@inheritDoc} */
    public void restoreByLabel(String label, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException, RemoteException {
        node.restoreByLabel(label, removeExisting);
    }
    
    /** {@inheritDoc} */
    public void update(String workspace) throws NoSuchWorkspaceException,
            AccessDeniedException, RepositoryException, RemoteException {
        node.update(workspace);
    }
    
    /** {@inheritDoc} */
    public boolean holdsLock() throws RepositoryException, RemoteException {
        return node.holdsLock();
    }

    /** {@inheritDoc} */
    public boolean isCheckedOut() throws
            UnsupportedRepositoryOperationException, RepositoryException,
            RemoteException {
        return node.isCheckedOut();
    }

    /** {@inheritDoc} */
    public boolean isLocked() throws RepositoryException, RemoteException {
        return node.isLocked();
    }

    /** {@inheritDoc} */
    public RemoteProperty setProperty(String name, Value[] values, int type)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException, RemoteException {
        return factory.getRemoteProperty(node.setProperty(name, values, type));
    }

    /** {@inheritDoc} */
    public void unlock() throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, RepositoryException,
            RemoteException {
        node.unlock();
    }
    
    /** {@inheritDoc} */
    public RemoteLock getLock() throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, RepositoryException,
            RemoteException {
        return factory.getRemoteLock(node.getLock());
    }
    
    /** {@inheritDoc} */
    public RemoteLock lock(boolean isDeep, boolean isSessionScoped)
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, RepositoryException, RemoteException {
        return factory.getRemoteLock(node.lock(isDeep, isSessionScoped));
    }
}
