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
package org.apache.jackrabbit.rmi.client;

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
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;

import org.apache.jackrabbit.rmi.remote.RemoteItem;
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
 * @author Jukka Zitting
 * @author Philipp Koch
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
     * Factory method for creating a local adapter for a remote node type.
     *
     * @param remote remote node type
     * @return local node type adapter
     */
    NodeType getNodeType(RemoteNodeType remote);

    /**
     * Factory method for creating a local adapter for a remote node
     * definition.
     *
     * @param remote remote node definition
     * @return local node definition adapter
     */
    NodeDef getNodeDef(RemoteNodeDef remote);

    /**
     * Factory method for creating a local adapter for a remote property
     * definition.
     *
     * @param remote remote property definition
     * @return local property definition adapter
     */
    PropertyDef getPropertyDef(RemotePropertyDef remote);

    /**
     * Factory method for creating a local adapter for a remote lock.
     *
     * @param node current node
     * @param remote remote lock
     * @return local lock adapter
     */
    Lock getLock(Node node, RemoteLock remote);

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

}
