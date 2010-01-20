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
package org.apache.jackrabbit.rmi.client;

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
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.apache.jackrabbit.rmi.remote.RemoteItem;
import org.apache.jackrabbit.rmi.remote.RemoteItemDefinition;
import org.apache.jackrabbit.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.rmi.remote.RemoteLock;
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
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;

/**
 * Factory interface for creating local adapters for remote references.
 * This interface defines how remote JCR-RMI references are adapted
 * back to the normal JCR interfaces. The adaption mechanism can be
 * modified (for example to add extra features) by changing the
 * local adapter factory used by the repository client.
 * <p>
 * Note that the
 * {@link org.apache.jackrabbit.rmi.client.ClientObject ClientObject}
 * base class provides a number of utility methods designed to work with
 * a local adapter factory. Adapter implementations may want to inherit
 * that functionality by subclassing from ClientObject.
 *
 * @see org.apache.jackrabbit.rmi.server.RemoteAdapterFactory
 * @see org.apache.jackrabbit.rmi.client.ClientAdapterFactory
 * @see org.apache.jackrabbit.rmi.client.ClientObject
 */
public interface LocalAdapterFactory {

    /**
     * Factory method for creating a local adapter for a remote repository.
     *
     * @param remote remote repository
     * @return local repository adapter
     */
    Repository getRepository(RemoteRepository remote);

    /**
     * Factory method for creating a local adapter for a remote session.
     *
     * @param repository current repository
     * @param remote remote session
     * @return local session adapter
     */
    Session getSession(Repository repository, RemoteSession remote);

    /**
     * Factory method for creating a local adapter for a remote workspace.
     *
     * @param session current session
     * @param remote remote workspace
     * @return local workspace adapter
     */
    Workspace getWorkspace(Session session, RemoteWorkspace remote);

    /**
     * Factory method for creating a local adapter for a remote observation
     * manager.
     *
     * @param workspace current workspace
     * @param remote remote observation manager
     * @return local observation manager adapter
     */
    ObservationManager getObservationManager(Workspace workspace,
        RemoteObservationManager remote);

    /**
     * Factory method for creating a local adapter for a remote namespace
     * registry.
     *
     * @param remote remote namespace registry
     * @return local namespace registry adapter
     */
    NamespaceRegistry getNamespaceRegistry(RemoteNamespaceRegistry remote);

    /**
     * Factory method for creating a local adapter for a remote node type
     * manager.
     *
     * @param remote remote node type manager
     * @return local node type manager adapter
     */
    NodeTypeManager getNodeTypeManager(RemoteNodeTypeManager remote);

    /**
     * Factory method for creating a local adapter for a remote item.
     * Note that before calling this method, the client may want to
     * introspect the remote item reference to determine whether to use the
     * {@link #getNode(Session, RemoteNode) getNode} or
     * {@link #getProperty(Session, RemoteProperty) getProperty} method
     * instead, as the adapter returned by this method will only cover
     * the basic {@link Item Item} interface.
     *
     * @param session current session
     * @param remote remote item
     * @return local item adapter
     */
    Item getItem(Session session, RemoteItem remote);

    /**
     * Factory method for creating a local adapter for a remote property.
     *
     * @param session current session
     * @param remote remote property
     * @return local property adapter
     */
    Property getProperty(Session session, RemoteProperty remote);

    /**
     * Factory method for creating a local adapter for a remote node.
     *
     * @param session current session
     * @param remote remote node
     * @return local node adapter
     */
    Node getNode(Session session, RemoteNode remote);

    /**
     * Factory method for creating a local adapter for a remote version.
     *
     * @param session current session
     * @param remote remote version
     * @return local version adapter
     */
    Version getVersion(Session session, RemoteVersion remote);

    /**
     * Factory method for creating a local adapter for a remote version history.
     *
     * @param session current session
     * @param remote remote version history
     * @return local version history adapter
     */
    VersionHistory getVersionHistory(Session session, RemoteVersionHistory remote);

    /**
     * Factory method for creating a local adapter for a remote node type.
     *
     * @param remote remote node type
     * @return local node type adapter
     */
    NodeType getNodeType(RemoteNodeType remote);

    /**
     * Factory method for creating a local adapter for a remote item
     * definition. Note that before calling this method, the client may want to
     * introspect the remote item definition to determine whether to use the
     * {@link #getNodeDef(RemoteNodeDefinition) getNodeDef} or
     * {@link #getPropertyDef(RemotePropertyDefinition) getPropertyDef} method
     * instead, as the adapter returned by this method will only cover
     * the {@link ItemDefinition ItemDef} base interface.
     *
     * @param remote remote item definition
     * @return local item definition adapter
     */
    ItemDefinition getItemDef(RemoteItemDefinition remote);

    /**
     * Factory method for creating a local adapter for a remote node
     * definition.
     *
     * @param remote remote node definition
     * @return local node definition adapter
     */
    NodeDefinition getNodeDef(RemoteNodeDefinition remote);

    /**
     * Factory method for creating a local adapter for a remote property
     * definition.
     *
     * @param remote remote property definition
     * @return local property definition adapter
     */
    PropertyDefinition getPropertyDef(RemotePropertyDefinition remote);

    /**
     * Factory method for creating a local adapter for a remote lock.
     *
     * @param session current session
     * @param node current node
     * @param remote remote lock
     * @return local lock adapter
     */
    Lock getLock(Session session, Node node, RemoteLock remote);

    /**
     * Factory method for creating a local adapter for a remote query manager.
     *
     * @param session current session
     * @param remote remote query manager
     * @return local query manager adapter
     */
    QueryManager getQueryManager(Session session, RemoteQueryManager remote);

    /**
     * Factory method for creating a local adapter for a remote query.
     *
     * @param session current session
     * @param remote remote query
     * @return local query adapter
     */
    Query getQuery(Session session, RemoteQuery remote);

    /**
     * Factory method for creating a local adapter for a remote query result.
     *
     * @param session current session
     * @param remote remote query result
     * @return local query result adapter
     */
    QueryResult getQueryResult(Session session, RemoteQueryResult remote);

    /**
     * Factory method for creating a local adapter for a remote query row.
     *
     * @param remote remote query row
     * @return local query row adapter
     */
    Row getRow(RemoteRow remote);

    /**
     * Factory method for creating a local adapter for a remote node iterator.
     *
     * @param session current session
     * @param remote remote node iterator
     * @return local node iterator adapter
     */
    NodeIterator getNodeIterator(Session session, RemoteIterator remote);

    /**
     * Factory method for creating a local adapter for a remote property iterator.
     *
     * @param session current session
     * @param remote remote property iterator
     * @return local property iterator adapter
     */
    PropertyIterator getPropertyIterator(Session session, RemoteIterator remote);

    /**
     * Factory method for creating a local adapter for a remote version iterator.
     *
     * @param session current session
     * @param remote remote version iterator
     * @return local version iterator adapter
     */
    VersionIterator getVersionIterator(Session session, RemoteIterator remote);

    /**
     * Factory method for creating a local adapter for a remote
     * node type iterator.
     *
     * @param remote remote node type iterator
     * @return local node type iterator adapter
     */
    NodeTypeIterator getNodeTypeIterator(RemoteIterator remote);

    /**
     * Factory method for creating a local adapter for a remote row iterator.
     *
     * @param remote remote row iterator
     * @return local row iterator adapter
     */
    RowIterator getRowIterator(RemoteIterator remote);

}
