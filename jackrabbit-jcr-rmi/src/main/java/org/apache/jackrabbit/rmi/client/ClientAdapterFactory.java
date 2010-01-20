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
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeDefinition;
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

import org.apache.jackrabbit.rmi.client.iterator.ClientNodeIterator;
import org.apache.jackrabbit.rmi.client.iterator.ClientNodeTypeIterator;
import org.apache.jackrabbit.rmi.client.iterator.ClientPropertyIterator;
import org.apache.jackrabbit.rmi.client.iterator.ClientRowIterator;
import org.apache.jackrabbit.rmi.client.iterator.ClientVersionIterator;
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
import org.apache.jackrabbit.rmi.remote.RemoteXASession;

/**
 * Default implementation of the
 * {@link org.apache.jackrabbit.rmi.client.LocalAdapterFactory LocalAdapterFactory}
 * interface. This factory uses the client adapters defined in this
 * package as the default adapter implementations. Subclasses can
 * easily override or extend the default adapters by implementing the
 * corresponding factory methods.
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
     * In case the remote session is transaction enabled, the returned session
     * will be transaction enabled too through the {@link ClientXASession}.
     *
     * {@inheritDoc}
     */
    public Session getSession(Repository repository, RemoteSession remote) {
        if (remote instanceof RemoteXASession) {
            return new ClientXASession(
                    repository, (RemoteXASession) remote, this);
        } else {
            return new ClientSession(repository, remote, this);
        }
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
     * {@link ClientObservationManager ClientObservationManager} instance.
     *
     * {@inheritDoc}
     */
    public ObservationManager getObservationManager(Workspace workspace,
            RemoteObservationManager remote) {
        return new ClientObservationManager(workspace, remote);
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
     * Creates and returns a {@link ClientVersion ClientVersion} instance.
     *
     * {@inheritDoc}
     */
    public Version getVersion(Session session, RemoteVersion remote) {
        return new ClientVersion(session, remote, this);
    }

    /**
     * Creates and returns a {@link ClientVersionHistory ClientVersionHistory}
     * instance.
     *
     * {@inheritDoc}
     */
    public VersionHistory getVersionHistory(Session session, RemoteVersionHistory remote) {
        return new ClientVersionHistory(session, remote, this);
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
     * Creates and returns a {@link ClientItemDefinition ClientItemDefinition} instance.
     *
     * {@inheritDoc}
     */
    public ItemDefinition getItemDef(RemoteItemDefinition remote) {
        return new ClientItemDefinition(remote, this);
    }

    /**
     * Creates and returns a {@link ClientNodeDefinition ClientNodeDefinition} instance.
     *
     * {@inheritDoc}
     */
    public NodeDefinition getNodeDef(RemoteNodeDefinition remote) {
        return new ClientNodeDefinition(remote, this);
    }

    /**
     * Creates and returns a {@link ClientPropertyDefinition ClientPropertyDefinition}
     * instance.
     *
     * {@inheritDoc}
     */
    public PropertyDefinition getPropertyDef(RemotePropertyDefinition remote) {
        return new ClientPropertyDefinition(remote, this);
    }

    /**
     * Creates and returns a {@link ClientLock ClientLock} instance.
     *
     * {@inheritDoc}
     */
    public Lock getLock(Session session, Node node, RemoteLock remote) {
        return new ClientLock(session, remote, this);
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

    /**
     * Creates and returns a {@link ClientNodeIterator} instance.
     * {@inheritDoc}
     */
    public NodeIterator getNodeIterator(
            Session session, RemoteIterator remote) {
        return new ClientNodeIterator(remote, session, this);
    }

    /**
     * Creates and returns a {@link ClientPropertyIterator} instance.
     * {@inheritDoc}
     */
    public PropertyIterator getPropertyIterator(
            Session session, RemoteIterator remote) {
        return new ClientPropertyIterator(remote, session, this);
    }

    /**
     * Creates and returns a {@link ClientVersionIterator} instance.
     * {@inheritDoc}
     */
    public VersionIterator getVersionIterator(
            Session session, RemoteIterator remote) {
        return new ClientVersionIterator(remote, session, this);
    }

    /**
     * Creates and returns a {@link ClientNodeTypeIterator} instance.
     * {@inheritDoc}
     */
    public NodeTypeIterator getNodeTypeIterator(RemoteIterator remote) {
        return new ClientNodeTypeIterator(remote, this);
    }

    /**
     * Creates and returns a {@link ClientRowIterator} instance.
     * {@inheritDoc}
     */
    public RowIterator getRowIterator(RemoteIterator remote) {
        return new ClientRowIterator(remote, this);
    }

}
