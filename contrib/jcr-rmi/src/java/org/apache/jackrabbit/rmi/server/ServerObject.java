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
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.MergeException;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.RowIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.apache.jackrabbit.rmi.remote.RemoteItem;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteNodeDefinition;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;
import org.apache.jackrabbit.rmi.remote.RemoteProperty;
import org.apache.jackrabbit.rmi.remote.RemotePropertyDefinition;
import org.apache.jackrabbit.rmi.remote.RemoteRow;
import org.apache.jackrabbit.rmi.remote.RemoteVersion;

/**
 * Base class for remote adapters. The purpose of this class is to
 * centralize the handling of the RemoteAdapterFactory instance used
 * to instantiate new server adapters.
 *
 * @author Jukka Zitting
 */
public class ServerObject extends UnicastRemoteObject {

    /** Factory for creating server adapters. */
    private RemoteAdapterFactory factory;

    /**
     * Creates a basic server adapter that uses the given factory
     * to create new adapters.
     *
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    protected ServerObject(RemoteAdapterFactory factory)
            throws RemoteException {
        this.factory = factory;
    }

    /**
     * Returns the remote adapter factory used to create new adapters.
     *
     * @return remote adapter factory
     */
    protected RemoteAdapterFactory getFactory() {
        return factory;
    }

    /**
     * Returns a cleaned version of the given exception. In some cases
     * the underlying repository implementation may throw exceptions
     * that are either unserializable, use exception subclasses that are
     * only locally available, contain references to unserializable or
     * only locally available classes. This method returns a cleaned
     * version of such an exception. The returned exception contains only
     * the message string from the original exception, and uses the public
     * JCR exception class that most specifically matches the original
     * exception.
     *
     * @param ex the original exception
     * @return clean exception
     */
    protected RepositoryException getRepositoryException(
            RepositoryException ex) {
        if (ex instanceof AccessDeniedException) {
            return new AccessDeniedException(ex.getMessage());
        } else if (ex instanceof ConstraintViolationException) {
            return new ConstraintViolationException(ex.getMessage());
        } else if (ex instanceof InvalidItemStateException) {
            return new InvalidItemStateException(ex.getMessage());
        } else if (ex instanceof InvalidQueryException) {
            return new InvalidQueryException(ex.getMessage());
        } else if (ex instanceof InvalidSerializedDataException) {
            return new InvalidSerializedDataException(ex.getMessage());
        } else if (ex instanceof ItemExistsException) {
            return new ItemExistsException(ex.getMessage());
        } else if (ex instanceof ItemNotFoundException) {
            return new ItemNotFoundException(ex.getMessage());
        } else if (ex instanceof LockException) {
            return new LockException(ex.getMessage());
        } else if (ex instanceof LoginException) {
            return new LoginException(ex.getMessage());
        } else if (ex instanceof MergeException) {
            return new MergeException(ex.getMessage());
        } else if (ex instanceof NamespaceException) {
            return new NamespaceException(ex.getMessage());
        } else if (ex instanceof NoSuchNodeTypeException) {
            return new NoSuchNodeTypeException(ex.getMessage());
        } else if (ex instanceof NoSuchWorkspaceException) {
            return new NoSuchWorkspaceException(ex.getMessage());
        } else if (ex instanceof PathNotFoundException) {
            return new PathNotFoundException(ex.getMessage());
        } else if (ex instanceof ReferentialIntegrityException) {
            return new ReferentialIntegrityException(ex.getMessage());
        } else if (ex instanceof UnsupportedRepositoryOperationException) {
            return new UnsupportedRepositoryOperationException(ex.getMessage());
        } else if (ex instanceof ValueFormatException) {
            return new ValueFormatException(ex.getMessage());
        } else if (ex instanceof VersionException) {
            return new VersionException(ex.getMessage());
        } else {
            return new RepositoryException(ex.getMessage());
        }
    }

    /**
     * Utility method for creating a remote reference for a local item.
     * Unlike the factory method for creating remote item references, this
     * method introspects the type of the local item and returns the
     * corresponding node, property, or item remote reference using the
     * remote adapter factory.
     * <p>
     * If the <code>item</code>, this method calls the
     * {@link #getRemoteNode(Node)} to return the correct remote type.
     *
     * @param item local node, property, or item
     * @return remote node, property, or item reference
     * @throws RemoteException on RMI errors
     */
    protected RemoteItem getRemoteItem(Item item) throws RemoteException {
        if (item instanceof Property) {
            return factory.getRemoteProperty((Property) item);
        } else if (item instanceof Node) {
            return getRemoteNode((Node) item);
        } else {
            return factory.getRemoteItem(item);
        }
    }

    /**
     * Utility method for creating a remote reference for a local node.
     * Unlike the factory method for creating remote node references, this
     * method introspects the type of the local node and returns the
     * corresponding node, version, or version history remote reference using
     * the remote adapter factory.
     *
     * @param node local version, versionhistory, or normal node
     * @return remote node, property, or item reference
     * @throws RemoteException on RMI errors
     */
    protected RemoteNode getRemoteNode(Node node) throws RemoteException {
        if (node instanceof Version) {
            return factory.getRemoteVersion((Version) node);
        } else if (node instanceof VersionHistory) {
            return factory.getRemoteVersionHistory((VersionHistory) node);
        } else {
            return factory.getRemoteNode(node);
        }
    }

    /**
     * Utility method for creating an array of remote references for
     * local properties. The remote references are created using the
     * remote adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty iterator.
     *
     * @param iterator local property iterator
     * @return remote property array
     * @throws RemoteException on RMI errors
     */
    protected RemoteProperty[] getRemotePropertyArray(PropertyIterator iterator)
            throws RemoteException {
        if (iterator != null) {
            ArrayList remotes = new ArrayList();
            while (iterator.hasNext()) {
                remotes.add(factory.getRemoteProperty(iterator.nextProperty()));
            }
            return (RemoteProperty[]) remotes.toArray(new RemoteProperty[remotes.size()]);
        } else {
            return new RemoteProperty[0]; // for safety
        }
    }

    /**
     * Utility method for creating an array of remote references for
     * local nodes. The remote references are created using the
     * remote adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty iterator.
     *
     * @param iterator local node iterator
     * @return remote node array
     * @throws RemoteException on RMI errors
     */
    protected RemoteNode[] getRemoteNodeArray(NodeIterator iterator)
            throws RemoteException {
        if (iterator != null) {
            ArrayList remotes = new ArrayList();
            while (iterator.hasNext()) {
                remotes.add(getRemoteNode(iterator.nextNode()));
            }
            return (RemoteNode[]) remotes.toArray(new RemoteNode[remotes.size()]);
        } else {
            return new RemoteNode[0]; // for safety
        }
    }

    /**
     * Utility method for creating an array of remote references for
     * local versions. The remote references are created using the
     * remote adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty array.
     *
     * @param versions local version array
     * @return remote version array
     * @throws RemoteException on RMI errors
     */
    protected RemoteVersion[] getRemoteVersionArray(Version[] versions)
            throws RemoteException {
        if (versions != null) {
            RemoteVersion[] remotes = new RemoteVersion[versions.length];
            for (int i = 0; i < remotes.length; i++) {
                remotes[i] = factory.getRemoteVersion(versions[i]);
            }
            return remotes;
        } else {
            return new RemoteVersion[0]; // for safety
        }
    }

    /**
     * Utility method for creating an array of remote references for
     * local versions. The remote references are created using the
     * remote adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty iterator.
     *
     * @param iterator local version iterator
     * @return remote version array
     * @throws RemoteException on RMI errors
     */
    protected RemoteVersion[] getRemoteVersionArray(VersionIterator iterator)
            throws RemoteException {
        if (iterator != null) {
            ArrayList remotes = new ArrayList();
            while (iterator.hasNext()) {
                remotes.add(factory.getRemoteVersion(iterator.nextVersion()));
            }
            return (RemoteVersion[]) remotes.toArray(new RemoteVersion[remotes.size()]);
        } else {
            return new RemoteVersion[0]; // for safety
        }
    }

    /**
     * Utility method for creating an array of remote references for
     * local node types. The remote references are created using the
     * remote adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty array.
     *
     * @param types local node type array
     * @return remote node type array
     * @throws RemoteException on RMI errors
     */
    protected RemoteNodeType[] getRemoteNodeTypeArray(NodeType[] types)
            throws RemoteException {
        if (types != null) {
            RemoteNodeType[] remotes = new RemoteNodeType[types.length];
            for (int i = 0; i < types.length; i++) {
                remotes[i] = factory.getRemoteNodeType(types[i]);
            }
            return remotes;
        } else {
            return new RemoteNodeType[0]; // for safety
        }
    }

    /**
     * Utility method for creating an array of remote references for
     * local node types. The remote references are created using the
     * remote adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty iterator.
     *
     * @param iterator local node type iterator
     * @return remote node type array
     * @throws RemoteException on RMI errors
     */
    protected RemoteNodeType[] getRemoteNodeTypeArray(NodeTypeIterator iterator)
            throws RemoteException {
        if (iterator != null) {
            ArrayList remotes = new ArrayList();
            while (iterator.hasNext()) {
                remotes.add(factory.getRemoteNodeType(iterator.nextNodeType()));
            }
            return (RemoteNodeType[]) remotes.toArray(new RemoteNodeType[remotes.size()]);
        } else {
            return new RemoteNodeType[0]; // for safety
        }
    }

    /**
     * Utility method for creating an array of remote references for
     * local node definitions. The remote references are created using the
     * remote adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty array.
     *
     * @param defs local node definition array
     * @return remote node definition array
     * @throws RemoteException on RMI errors
     */
    protected RemoteNodeDefinition[] getRemoteNodeDefArray(NodeDefinition[] defs)
            throws RemoteException {
        if (defs != null) {
            RemoteNodeDefinition[] remotes = new RemoteNodeDefinition[defs.length];
            for (int i = 0; i < defs.length; i++) {
                remotes[i] = factory.getRemoteNodeDefinition(defs[i]);
            }
            return remotes;
        } else {
            return new RemoteNodeDefinition[0]; // for safety
        }
    }

    /**
     * Utility method for creating an array of remote references for
     * local property definitions. The remote references are created using the
     * remote adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty array.
     *
     * @param defs local property definition array
     * @return remote property definition array
     * @throws RemoteException on RMI errors
     */
    protected RemotePropertyDefinition[] getRemotePropertyDefArray(PropertyDefinition[] defs)
            throws RemoteException {
        if (defs != null) {
            RemotePropertyDefinition[] remotes = new RemotePropertyDefinition[defs.length];
            for (int i = 0; i < defs.length; i++) {
                remotes[i] = factory.getRemotePropertyDefinition(defs[i]);
            }
            return remotes;
        } else {
            return new RemotePropertyDefinition[0]; // for safety
        }
    }

    /**
     * Utility method for creating an array of remote references for
     * local query result rows. The remote references are created using the
     * remote adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty iterator.
     *
     * @param iterator local query result row iterator
     * @return remote query result row array
     * @throws RemoteException on RMI errors
     */
    protected RemoteRow[] getRemoteRowArray(RowIterator iterator)
            throws RemoteException {
        if (iterator != null) {
            ArrayList remotes = new ArrayList();
            while (iterator.hasNext()) {
                remotes.add(getFactory().getRemoteRow(iterator.nextRow()));
            }
            return (RemoteRow[]) remotes.toArray(new RemoteRow[remotes.size()]);
        } else {
            return new RemoteRow[0]; // for safety
        }
    }

}
