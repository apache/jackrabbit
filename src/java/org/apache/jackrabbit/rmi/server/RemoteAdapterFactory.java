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

import javax.jcr.Item;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDef;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.rmi.remote.RemoteItem;
import org.apache.jackrabbit.rmi.remote.RemoteLock;
import org.apache.jackrabbit.rmi.remote.RemoteNamespaceRegistry;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteNodeDef;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;
import org.apache.jackrabbit.rmi.remote.RemoteNodeTypeManager;
import org.apache.jackrabbit.rmi.remote.RemoteProperty;
import org.apache.jackrabbit.rmi.remote.RemotePropertyDef;
import org.apache.jackrabbit.rmi.remote.RemoteQueryManager;
import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;

/**
 * Factory interface for creating remote adapters for local resources.
 * This interface defines how the local JCR interfaces are adapted to
 * remote JCR-RMI references. The adaption mechanism can be
 * modified (for example to add extra features) by changing the
 * remote adapter factory used by the repository server.
 * <p>
 * Note that the
 * {@link org.apache.jackrabbit.rmi.server.ServerObject ServerObject}
 * base class provides a number of utility methods designed to work with
 * a remote adapter factory. Adapter implementations may want to inherit
 * that functionality by subclassing from ServerObject.
 *
 * @author Jukka Zitting
 * @author Philipp Koch
 * @see org.apache.jackrabbit.rmi.client.LocalAdapterFactory
 * @see org.apache.jackrabbit.rmi.server.ServerAdapterFactory
 * @see org.apache.jackrabbit.rmi.server.ServerObject
 */
public interface RemoteAdapterFactory {

    /**
     * Factory method for creating a remote adapter for a local repository.
     *
     * @param repository local repository
     * @return remote repository adapter
     * @throws RemoteException on RMI errors
     */
    RemoteRepository getRemoteRepository(Repository repository)
            throws RemoteException;

    /**
     * Factory method for creating a remote adapter for a local session.
     *
     * @param session local session
     * @return remote session adapter
     * @throws RemoteException on RMI errors
     */
    RemoteSession getRemoteSession(Session session) throws RemoteException;

    /**
     * Factory method for creating a remote adapter for a local workspace.
     *
     * @param workspace local workspace
     * @return remote workspace adapter
     * @throws RemoteException on RMI errors
     */
    RemoteWorkspace getRemoteWorkspace(Workspace workspace)
            throws RemoteException;

    /**
     * Factory method for creating a remote adapter for a local namespace
     * registry.
     *
     * @param registry local namespace registry
     * @return remote namespace registry adapter
     * @throws RemoteException on RMI errors
     */
    RemoteNamespaceRegistry getRemoteNamespaceRegistry(
            NamespaceRegistry registry) throws RemoteException;

    /**
     * Factory method for creating a remote adapter for a local node type
     * manager.
     *
     * @param manager local node type manager
     * @return remote node type manager adapter
     * @throws RemoteException on RMI errors
     */
    RemoteNodeTypeManager getRemoteNodeTypeManager(NodeTypeManager manager)
            throws RemoteException;

    /**
     * Factory method for creating a remote adapter for a local item.
     * Note that before calling this method, the server may want to
     * introspect the local item to determine whether to use the
     * {@link #getRemoteNode(Node) getRemoteNode} or
     * {@link #getRemoteProperty(Property) getRemoteProperty} method
     * instead, as the adapter returned by this method will only cover
     * the basic {@link RemoteItem RemoteItem} interface.
     *
     * @param item local item
     * @return remote item adapter
     * @throws RemoteException on RMI errors
     */
    RemoteItem getRemoteItem(Item item) throws RemoteException;

    /**
     * Factory method for creating a remote adapter for a local property.
     *
     * @param property local property
     * @return remote property adapter
     * @throws RemoteException on RMI errors
     */
    RemoteProperty getRemoteProperty(Property property) throws RemoteException;

    /**
     * Factory method for creating a remote adapter for a local node.
     *
     * @param node local node
     * @return remote node adapter
     * @throws RemoteException on RMI errors
     */
    RemoteNode getRemoteNode(Node node) throws RemoteException;

    /**
     * Factory method for creating a remote adapter for a local node type.
     *
     * @param type local node type
     * @return remote node type adapter
     * @throws RemoteException on RMI errors
     */
    RemoteNodeType getRemoteNodeType(NodeType type) throws RemoteException;

    /**
     * Factory method for creating a remote adapter for a local node
     * definition.
     *
     * @param def local node definition
     * @return remote node definition adapter
     * @throws RemoteException on RMI errors
     */
    RemoteNodeDef getRemoteNodeDef(NodeDef def) throws RemoteException;

    /**
     * Factory method for creating a remote adapter for a local property
     * definition.
     *
     * @param def local property definition
     * @return remote property definition adapter
     * @throws RemoteException on RMI errors
     */
    RemotePropertyDef getRemotePropertyDef(PropertyDef def)
            throws RemoteException;

    /**
     * Factory method for creating a remote adapter for a local lock.
     *
     * @param lock local lock
     * @return remote lock adapter
     * @throws RemoteException on RMI errors
     */
    RemoteLock getRemoteLock(Lock lock) throws RemoteException;

    /**
     * Factory method for creating a remote adapter for a local query manager.
     *
     * @param manager local query manager
     * @return remote query manager adapter
     * @throws RemoteException on RMI errors
     */
    RemoteQueryManager getRemoteQueryManager(QueryManager manager)
            throws RemoteException;

}
