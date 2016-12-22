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

import java.rmi.RemoteException;
import java.security.Principal;
import java.util.Iterator;

import javax.jcr.Item;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.rmi.remote.RemoteEventCollection;
import org.apache.jackrabbit.rmi.remote.RemoteItem;
import org.apache.jackrabbit.rmi.remote.RemoteItemDefinition;
import org.apache.jackrabbit.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.rmi.remote.RemoteLock;
import org.apache.jackrabbit.rmi.remote.RemoteLockManager;
import org.apache.jackrabbit.rmi.remote.RemoteNamespaceRegistry;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteNodeDefinition;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;
import org.apache.jackrabbit.rmi.remote.RemoteNodeTypeManager;
import org.apache.jackrabbit.rmi.remote.RemoteObservationManager;
import org.apache.jackrabbit.rmi.remote.RemoteProperty;
import org.apache.jackrabbit.rmi.remote.RemotePropertyDefinition;
import org.apache.jackrabbit.rmi.remote.RemoteQuery;
import org.apache.jackrabbit.rmi.remote.RemoteQueryManager;
import org.apache.jackrabbit.rmi.remote.RemoteQueryResult;
import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.remote.RemoteRow;
import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.remote.RemoteVersion;
import org.apache.jackrabbit.rmi.remote.RemoteVersionHistory;
import org.apache.jackrabbit.rmi.remote.RemoteVersionManager;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;
import org.apache.jackrabbit.rmi.remote.principal.RemotePrincipal;
import org.apache.jackrabbit.rmi.remote.security.RemoteAccessControlEntry;
import org.apache.jackrabbit.rmi.remote.security.RemoteAccessControlManager;
import org.apache.jackrabbit.rmi.remote.security.RemoteAccessControlPolicy;
import org.apache.jackrabbit.rmi.remote.security.RemotePrivilege;

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
 * @see org.apache.jackrabbit.rmi.client.LocalAdapterFactory
 * @see org.apache.jackrabbit.rmi.server.ServerAdapterFactory
 * @see org.apache.jackrabbit.rmi.server.ServerObject
 */
public interface RemoteAdapterFactory {

    /**
     * Returns the port number to which the server objects created by
     * this factory are bound. This method is mostly used internally by
     * the {@link ServerObject} constructor to determine which port number
     * to use.
     *
     * @return port number, or 0 for a random port
     */
    int getPortNumber();

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
     * Returns a remote adapter for the given local observation manager.
     *
     * @param observationManager local observation manager
     * @return remote observation manager adapter
     * @throws RemoteException on RMI errors
     */
    RemoteObservationManager getRemoteObservationManager(
            ObservationManager observationManager)
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
     * {@link ItemDefinition ItemDefinition} interface. The caller may want to introspect
     * the local item definition to determine whether to use either the
     * {@link #getRemoteNodeDefinition(NodeDefinition) getRemoteNodeDef} or the
     * {@link #getRemotePropertyDefinition(PropertyDefinition) getRemotePropertyDef}
     * method instead.
     *
     * @param def local item definition
     * @return remote item definition adapter
     * @throws RemoteException on RMI errors
     */
    RemoteItemDefinition getRemoteItemDefinition(ItemDefinition def) throws RemoteException;

    /**
     * Returns a remote adapter for the given local node definition.
     *
     * @param def local node definition
     * @return remote node definition adapter
     * @throws RemoteException on RMI errors
     */
    RemoteNodeDefinition getRemoteNodeDefinition(NodeDefinition def) throws RemoteException;

    /**
     * Returns a remote adapter for the given local property definition.
     *
     * @param def local property definition
     * @return remote property definition adapter
     * @throws RemoteException on RMI errors
     */
    RemotePropertyDefinition getRemotePropertyDefinition(PropertyDefinition def)
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
     * @param session current session
     * @param manager local query manager
     * @return remote query manager adapter
     * @throws RemoteException on RMI errors
     */
    RemoteQueryManager getRemoteQueryManager(
            Session session, QueryManager manager) throws RemoteException;

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

    /**
     * Returns a remote adapter for the given local events.
     *
     * @param listenerId The listener identifier to which the events are to be
     *      dispatched.
     * @param events the local events
     * @return remote event iterator adapter
     * @throws RemoteException on RMI errors
     */
    RemoteEventCollection getRemoteEvent(long listenerId, EventIterator events)
        throws RemoteException;


    /**
     * Returns a remote adapter for the given local node iterator.
     *
     * @param iterator local node iterator
     * @return remote iterator adapter
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getRemoteNodeIterator(NodeIterator iterator)
        throws RemoteException;

    /**
     * Returns a remote adapter for the given local property iterator.
     *
     * @param iterator local property iterator
     * @return remote iterator adapter
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getRemotePropertyIterator(PropertyIterator iterator)
        throws RemoteException;

    /**
     * Returns a remote adapter for the given local version iterator.
     *
     * @param iterator local version iterator
     * @return remote iterator adapter
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getRemoteVersionIterator(VersionIterator iterator)
        throws RemoteException;

    /**
     * Returns a remote adapter for the given local node type iterator.
     *
     * @param iterator local node type iterator
     * @return remote iterator adapter
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getRemoteNodeTypeIterator(NodeTypeIterator iterator)
        throws RemoteException;

    /**
     * Returns a remote adapter for the given local row iterator.
     *
     * @param iterator local row iterator
     * @return remote iterator adapter
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getRemoteRowIterator(RowIterator iterator)
        throws RemoteException;

    RemoteLockManager getRemoteLockManager(LockManager lockManager)
        throws RemoteException;

    RemoteVersionManager getRemoteVersionManager(Session session, VersionManager versionManager)
        throws RemoteException;

    /**
     * Returns a remote adapter for the given local access control manager.
     *
     * @param acm local access control manager
     * @return remote access control manager
     * @throws RemoteException on RMI errors
     */
    RemoteAccessControlManager getRemoteAccessControlManager(
            AccessControlManager acm) throws RemoteException;

    /**
     * Returns a remote adapter for the given local access control manager.
     *
     * @return remote access control manager
     * @throws RemoteException on RMI errors
     */
    public RemotePrivilege getRemotePrivilege(final Privilege local)
            throws RemoteException;

    /**
     * Returns a remote adapter for the given local access control manager.
     *
     * @return remote access control manager
     * @throws RemoteException on RMI errors
     */
    public RemotePrivilege[] getRemotePrivilege(final Privilege[] local)
            throws RemoteException;

    /**
     * Returns a remote adapter for the given local access control manager.
     *
     * @return remote access control manager
     * @throws RemoteException on RMI errors
     */
    public RemoteAccessControlPolicy getRemoteAccessControlPolicy(
            final AccessControlPolicy local) throws RemoteException;

    /**
     * Returns a remote adapter for the given local access control manager.
     *
     * @return remote access control manager
     * @throws RemoteException on RMI errors
     */
    public RemoteAccessControlPolicy[] getRemoteAccessControlPolicy(
            final AccessControlPolicy[] local) throws RemoteException;

    /**
     * Returns a remote adapter for the given local access control manager.
     *
     * @return remote access control manager
     * @throws RemoteException on RMI errors
     */
    public RemoteIterator getRemoteAccessControlPolicyIterator(
            AccessControlPolicyIterator iterator) throws RemoteException;

    /**
     * Returns a remote adapter for the given local access control manager.
     *
     * @return remote access control manager
     * @throws RemoteException on RMI errors
     */
    public RemoteAccessControlEntry getRemoteAccessControlEntry(
            final AccessControlEntry local) throws RemoteException;

    /**
     * Returns a remote adapter for the given local access control manager.
     *
     * @return remote access control manager
     * @throws RemoteException on RMI errors
     */
    public RemoteAccessControlEntry[] getRemoteAccessControlEntry(
            final AccessControlEntry[] local) throws RemoteException;

    /**
     * Returns a remote adapter for the given local access control manager.
     *
     * @return remote access control manager
     * @throws RemoteException on RMI errors
     */
    public RemotePrincipal getRemotePrincipal(final Principal principal)
            throws RemoteException;

    /**
     * Returns a remote adapter for the given local access control manager.
     *
     * @return remote access control manager
     * @throws RemoteException on RMI errors
     */
    public RemoteIterator getRemotePrincipalIterator(
            final Iterator<Principal> principals) throws RemoteException;

}
