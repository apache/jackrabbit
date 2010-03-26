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
package org.apache.jackrabbit.rmi.server;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

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
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.security.AccessControlException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.rmi.remote.RemoteItem;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;
import org.apache.jackrabbit.rmi.value.SerialValueFactory;

/**
 * Base class for remote adapters. The purpose of this class is to
 * centralize the handling of the RemoteAdapterFactory instance used
 * to instantiate new server adapters.
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
        super(factory.getPortNumber());
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
        } else if (ex instanceof AccessControlException) {
            return new AccessControlException(ex.getMessage());
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
     * Utility method for preparing an array of values for serialization.
     * The returned array will contain serializable versions of all the
     * given values.
     * <p>
     * If the given array is <code>null</code>, then an empty array is
     * returned.
     *
     * @param values the values to be decorated
     * @return array of decorated values
     * @throws RepositoryException if the values can not be serialized
     */
    protected Value[] getSerialValues(Value[] values)
            throws RepositoryException {
        List<Value> serials = new ArrayList<Value>();
        if (values != null) {
            for (Value value : values) {
                if (value != null) {
                    serials.add(getSerialValue(value));
                }
            }
        }
        return serials.toArray(new Value[serials.size()]);
    }

    /**
     * Utility method for decorating a value. Note that the contents of the
     * original values will only be copied when the decorators are serialized.
     * Null referenced and already serializable values are passed as-is.
     *
     * @param value the value to be decorated, or <code>null</code>
     * @return the decorated value, or <code>null</code>
     * @throws RepositoryException if the value can not be serialized
     */
    protected Value getSerialValue(Value value) throws RepositoryException {
        // if the value is null or already serializable, just return it
        if (value == null || value instanceof Serializable) {
            return value;
        } else {
            return SerialValueFactory.makeSerialValue(value);
        }
    }

}
