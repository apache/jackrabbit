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
import javax.jcr.nodetype.ItemDef;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDef;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.rmi.remote.RemoteItem;
import org.apache.jackrabbit.rmi.remote.RemoteItemDef;
import org.apache.jackrabbit.rmi.remote.RemoteLock;
import org.apache.jackrabbit.rmi.remote.RemoteNamespaceRegistry;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteNodeDef;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;
import org.apache.jackrabbit.rmi.remote.RemoteNodeTypeManager;
import org.apache.jackrabbit.rmi.remote.RemoteProperty;
import org.apache.jackrabbit.rmi.remote.RemotePropertyDef;
import org.apache.jackrabbit.rmi.remote.RemoteQuery;
import org.apache.jackrabbit.rmi.remote.RemoteQueryManager;
import org.apache.jackrabbit.rmi.remote.RemoteQueryResult;
import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.remote.RemoteRow;
import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.remote.RemoteVersion;
import org.apache.jackrabbit.rmi.remote.RemoteVersionHistory;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;

/**
 * Factory interface for creating remote adapters for local resources.
 * This interface defines how the local JCR interfaces are adapted to
 * remote JCR-RMI references. The adaption mechanism can be
 * modified (for example to add extra features) by changing the
 * remote adapter factory used by the repository server.
 * <p>
 * Note that the {@link ServerObject ServerObject} base class provides
 * a number of utility methods designed to work with a remote adapter
 * factory. Adapter implementations may want to inherit that functionality
 * by subclassing from ServerObject.
 *
 * @author Jukka Zitting
 * @author Philipp Koch
 * @see org.apache.jackrabbit.rmi.client.LocalAdapterFactory
 * @see org.apache.jackrabbit.rmi.server.ServerAdapterFactory
 * @see org.apache.jackrabbit.rmi.server.ServerObject
 */
public interface RemoteAdapterFactory {

    /**
     * Returns a remote adapter for the given local repository.
     *
     * @param repository local repository
     * @return remote repository adapter
     * @throws RemoteException on RMI errors
     */
    RemoteRepository getRemoteRepository(Repository repository)
            throws RemoteException;

    /**
     * Returns a remote adapter for the given local session.
     *
     * @param session local session
     * @return remote session adapter
     * @throws RemoteException on RMI errors
     */
    RemoteSession getRemoteSession(Session session) throws RemoteException;

    /**
     * Returns a remote adapter for the given local workspace.
     *
     * @param workspace local workspace
     * @return remote workspace adapter
     * @throws RemoteException on RMI errors
     */
    RemoteWorkspace getRemoteWorkspace(Workspace workspace)
            throws RemoteException;

    /**
     * Returns a remote adapter for the given local namespace registry.
     *
     * @param registry local namespace registry
     * @return remote namespace registry adapter
     * @throws RemoteException on RMI errors
     */
    RemoteNamespaceRegistry getRemoteNamespaceRegistry(
            NamespaceRegistry registry) throws RemoteException;

    /**
     * Returns a remote adapter for the given local node type manager.
     *
     * @param manager local node type manager
     * @return remote node type manager adapter
     * @throws RemoteException on RMI errors
     */
    RemoteNodeTypeManager getRemoteNodeTypeManager(NodeTypeManager manager)
            throws RemoteException;

    /**
     * Returns a remote adapter for the given local item. This method
     * will return an adapter that implements <i>only</i> the
     * {@link Item Item} interface. The caller may want to introspect
     * the local item to determine whether to use either the
     * {@link #getRemoteNode(Node) getRemoteNode} or the
     * {@link #getRemoteProperty(Property) getRemoteProperty} method instead.
     *
     * @param item local item
     * @return remote item adapter
     * @throws RemoteException on RMI errors
     */
    RemoteItem getRemoteItem(Item item) throws RemoteException;

    /**
     * Returns a remote adapter for the given local property.
     *
     * @param property local property
     * @return remote property adapter
     * @throws RemoteException on RMI errors
     */
    RemoteProperty getRemoteProperty(Property property) throws RemoteException;

    /**
     * Returns a remote adapter for the given local node.
     *
     * @param node local node
     * @return remote node adapter
     * @throws RemoteException on RMI errors
     */
    RemoteNode getRemoteNode(Node node) throws RemoteException;

    /**
     * Returns a remote adapter for the given local version.
     *
     * @param version local version
     * @return remote version adapter
     * @throws RemoteException on RMI errors
     */
    RemoteVersion getRemoteVersion(Version version) throws RemoteException;

    /**
     * Returns a remote adapter for the given local version history.
     *
     * @param versionHistory local version history
     * @return remote version history adapter
     * @throws RemoteException on RMI errors
     */
    RemoteVersionHistory getRemoteVersionHistory(VersionHistory versionHistory) 
            throws RemoteException;
    
    /**
     * Returns a remote adapter for the given local node type.
     *
     * @param type local node type
     * @return remote node type adapter
     * @throws RemoteException on RMI errors
     */
    RemoteNodeType getRemoteNodeType(NodeType type) throws RemoteException;

    /**
     * Returns a remote adapter for the given local item definition.
     * This method will return an adapter that implements <i>only</i> the
     * {@link ItemDef ItemDef} interface. The caller may want to introspect
     * the local item definition to determine whether to use either the
     * {@link #getRemoteNodeDef(NodeDef) getRemoteNodeDef} or the
     * {@link #getRemotePropertyDef(PropertyDef) getRemotePropertyDef}
     * method instead.
     *
     * @param def local item definition
     * @return remote item definition adapter
     * @throws RemoteException on RMI errors
     */
    RemoteItemDef getRemoteItemDef(ItemDef def) throws RemoteException;

    /**
     * Returns a remote adapter for the given local node definition.
     *
     * @param def local node definition
     * @return remote node definition adapter
     * @throws RemoteException on RMI errors
     */
    RemoteNodeDef getRemoteNodeDef(NodeDef def) throws RemoteException;

    /**
     * Returns a remote adapter for the given local property definition.
     *
     * @param def local property definition
     * @return remote property definition adapter
     * @throws RemoteException on RMI errors
     */
    RemotePropertyDef getRemotePropertyDef(PropertyDef def)
            throws RemoteException;

    /**
     * Returns a remote adapter for the given local lock.
     *
     * @param lock local lock
     * @return remote lock adapter
     * @throws RemoteException on RMI errors
     */
    RemoteLock getRemoteLock(Lock lock) throws RemoteException;

    /**
     * Returns a remote adapter for the given local query manager.
     *
     * @param manager local query manager
     * @return remote query manager adapter
     * @throws RemoteException on RMI errors
     */
    RemoteQueryManager getRemoteQueryManager(QueryManager manager)
            throws RemoteException;

    /**
     * Returns a remote adapter for the given local query.
     *
     * @param query local query
     * @return remote query adapter
     * @throws RemoteException on RMI errors
     */
    RemoteQuery getRemoteQuery(Query query) throws RemoteException;

    /**
     * Returns a remote adapter for the given local query result.
     *
     * @param result local query result
     * @return remote query result adapter
     * @throws RemoteException on RMI errors
     */
    RemoteQueryResult getRemoteQueryResult(QueryResult result)
            throws RemoteException;

    /**
     * Returns a remote adapter for the given local query row.
     *
     * @param row local query row
     * @return remote query row adapter
     * @throws RemoteException on RMI errors
     */
    RemoteRow getRemoteRow(Row row) throws RemoteException;

}
