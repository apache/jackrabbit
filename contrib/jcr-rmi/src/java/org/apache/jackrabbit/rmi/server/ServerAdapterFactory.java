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
 * Default implementation of the
 * {@link org.apache.jackrabbit.rmi.server.RemoteAdapterFactory RemoteAdapterFactory}
 * interface. This factory uses the server adapters defined in this
 * package as the default adapter implementations. Subclasses can
 * easily override or extend the default adapters by implementing the
 * corresponding factory methods.
 *
 * @author Jukka Zitting
 * @author Philipp Koch
 */
public class ServerAdapterFactory implements RemoteAdapterFactory {

    /**
     * Creates and returns a {@link ServerRepository ServerRepository} instance.
     *
     * {@inheritDoc}
     */
    public RemoteRepository getRemoteRepository(Repository repository)
            throws RemoteException {
        return new ServerRepository(repository, this);
    }

    /**
     * Creates and returns a {@link ServerSession ServerSession} instance.
     *
     * {@inheritDoc}
     */
    public RemoteSession getRemoteSession(Session session)
            throws RemoteException {
        return new ServerSession(session, this);
    }

    /**
     * Creates and returns a {@link ServerWorkspace ServerWorkspace} instance.
     *
     * {@inheritDoc}
     */
    public RemoteWorkspace getRemoteWorkspace(Workspace workspace)
            throws RemoteException {
        return new ServerWorkspace(workspace, this);
    }

    /**
     * Creates and returns a
     * {@link ServerNamespaceRegistry ServerNamespaceRegistry} instance.
     *
     * {@inheritDoc}
     */
    public RemoteNamespaceRegistry getRemoteNamespaceRegistry(
            NamespaceRegistry registry) throws RemoteException {
        return new ServerNamespaceRegistry(registry, this);
    }

    /**
     * Creates and returns a
     * {@link ServerNodeTypeManager ServerNodeTypeManager} instance.
     *
     * {@inheritDoc}
     */
    public RemoteNodeTypeManager getRemoteNodeTypeManager(
            NodeTypeManager manager) throws RemoteException {
        return new ServerNodeTypeManager(manager, this);
    }

    /**
     * Creates and returns a {@link ServerItem ServerItem} instance.
     *
     * {@inheritDoc}
     */
    public RemoteItem getRemoteItem(Item item) throws RemoteException {
        return new ServerItem(item, this);
    }

    /**
     * Creates and returns a {@link ServerProperty ServerProperty} instance.
     *
     * {@inheritDoc}
     */
    public RemoteProperty getRemoteProperty(Property property)
            throws RemoteException {
        return new ServerProperty(property, this);
    }

    /**
     * Creates and returns a {@link ServerNode ServerNode} instance.
     *
     * {@inheritDoc}
     */
    public RemoteNode getRemoteNode(Node node) throws RemoteException {
        return new ServerNode(node, this);
    }

    /**
     * Creates and returns a {@link ServerNodeType ServerNodeType} instance.
     *
     * {@inheritDoc}
     */
    public RemoteNodeType getRemoteNodeType(NodeType type)
            throws RemoteException {
        return new ServerNodeType(type, this);
    }

    /**
     * Creates and returns a {@link ServerNodeDef ServerNodeDef} instance.
     *
     * {@inheritDoc}
     */
    public RemoteNodeDef getRemoteNodeDef(NodeDef def)
            throws RemoteException {
        return new ServerNodeDef(def, this);
    }

    /**
     * Creates and returns a {@link ServerPropertyDef ServerPropertyDef}
     * instance.
     *
     * {@inheritDoc}
     */
    public RemotePropertyDef getRemotePropertyDef(PropertyDef def)
            throws RemoteException {
        return new ServerPropertyDef(def, this);
    }

    /**
     * Creates and returns a {@link ServerLock ServerLock} instance.
     *
     * {@inheritDoc}
     */
    public RemoteLock getRemoteLock(Lock lock) throws RemoteException {
        return new ServerLock(lock);
    }

    /**
     * Creates and returns a {@link ServerQueryManager ServerQueryManager}
     * instance.
     *
     * {@inheritDoc}
     */
    public RemoteQueryManager getRemoteQueryManager(QueryManager manager)
            throws RemoteException {
        return new ServerQueryManager(manager, this);
    }

}
