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
 * Default implementation of the
 * {@link org.apache.jackrabbit.rmi.client.LocalAdapterFactory LocalAdapterFactory}
 * interface. This factory uses the client adapters defined in this
 * package as the default adapter implementations. Subclasses can
 * easily override or extend the default adapters by implementing the
 * corresponding factory methods.
 *
 * @author Jukka Zitting
 * @author Philipp Koch
 */
public class ClientAdapterFactory implements LocalAdapterFactory {

    /**
     * Creates and returns a {@link ClientRepository ClientRepository}
     * instance.
     *
     * {@inheritDoc}
     */
    public Repository getRepository(RemoteRepository remote) {
        return new ClientRepository(remote, this);
    }

    /**
     * Creates and returns a {@link ClientSession ClientSession} instance.
     *
     * {@inheritDoc}
     */
    public Session getSession(Repository repository, RemoteSession remote) {
        return new ClientSession(repository, remote, this);
    }

    /**
     * Creates and returns a {@link ClientWorkspace ClientWorkspace} instance.
     *
     * {@inheritDoc}
     */
    public Workspace getWorkspace(Session session, RemoteWorkspace remote) {
        return new ClientWorkspace(session, remote, this);
    }

    /**
     * Creates and returns a
     * {@link ClientNamespaceRegistry ClientClientNamespaceRegistry} instance.
     *
     * {@inheritDoc}
     */
    public NamespaceRegistry getNamespaceRegistry(
            RemoteNamespaceRegistry remote) {
        return new ClientNamespaceRegistry(remote, this);
    }

    /**
     * Creates and returns a
     * {@link ClientNodeTypeManager ClienNodeTypeManager} instance.
     *
     * {@inheritDoc}
     */
    public NodeTypeManager getNodeTypeManager(RemoteNodeTypeManager remote) {
        return new ClientNodeTypeManager(remote, this);
    }

    /**
     * Creates and returns a {@link ClientItem ClientItem} instance.
     *
     * {@inheritDoc}
     */
    public Item getItem(Session session, RemoteItem remote) {
        return new ClientItem(session, remote, this);
    }

    /**
     * Creates and returns a {@link ClientProperty ClientProperty} instance.
     *
     * {@inheritDoc}
     */
    public Property getProperty(Session session, RemoteProperty remote) {
        return new ClientProperty(session, remote, this);
    }

    /**
     * Creates and returns a {@link ClientNode ClientNode} instance.
     *
     * {@inheritDoc}
     */
    public Node getNode(Session session, RemoteNode remote) {
        return new ClientNode(session, remote, this);
    }

    /**
     * Creates and returns a {@link ClientNodeType ClientNodeType} instance.
     *
     * {@inheritDoc}
     */
    public NodeType getNodeType(RemoteNodeType remote) {
        return new ClientNodeType(remote, this);
    }

    /**
     * Creates and returns a {@link ClientNodeDef ClientNodeDef} instance.
     *
     * {@inheritDoc}
     */
    public NodeDef getNodeDef(RemoteNodeDef remote) {
        return new ClientNodeDef(remote, this);
    }

    /**
     * Creates and returns a {@link ClientPropertyDef ClientPropertyDef}
     * instance.
     *
     * {@inheritDoc}
     */
    public PropertyDef getPropertyDef(RemotePropertyDef remote) {
        return new ClientPropertyDef(remote, this);
    }

    /**
     * Creates and returns a {@link ClientLock ClientLock} instance.
     *
     * {@inheritDoc}
     */
    public Lock getLock(Node node, RemoteLock remote) {
        return new ClientLock(node, remote);
    }

    /**
     * Creates and returns a {@link ClientQueryManager ClientQueryManager} instance.
     *
     * {@inheritDoc}
     */
    public QueryManager getQueryManager(
            Session session, RemoteQueryManager remote) {
        return new ClientQueryManager(session, remote, this);
    }

    /**
     * Creates and returns a {@link ClientQuery ClientQuery} instance.
     *
     * {@inheritDoc}
     */
    public Query getQuery(Session session, RemoteQuery remote) {
        return new ClientQuery(session, remote, this);
    }

    /**
     * Creates and returns a {@link ClientQueryResult ClientQueryResult} instance.
     *
     * {@inheritDoc}
     */
    public QueryResult getQueryResult(
            Session session, RemoteQueryResult remote) {
        return new ClientQueryResult(session, remote, this);
    }

    /**
     * Creates and returns a {@link ClientRow ClientRow} instance.
     *
     * {@inheritDoc}
     */
    public Row getRow(RemoteRow remote) {
        return new ClientRow(remote);
    }
}
