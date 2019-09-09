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
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.rmi.remote.RemoteItem;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;
import org.apache.jackrabbit.rmi.remote.RemoteProperty;
import org.apache.jackrabbit.rmi.remote.RemoteVersion;
import org.apache.jackrabbit.rmi.remote.RemoteVersionHistory;

/**
 * Base class for client adapter objects. The only purpose of
 * this class is to centralize the handling of the
 * local adapter factory used by the client adapters to
 * instantiate new adapters.
 */
public class ClientObject {

    /** Local adapter factory. */
    private LocalAdapterFactory factory;

    /**
     * Creates a basic client adapter that uses the given factory
     * to create new adapters.
     *
     * @param factory local adapter factory
     */
    protected ClientObject(LocalAdapterFactory factory) {
        this.factory = factory;
    }

    /**
     * Returns the local adapter factory used to create new adapters.
     *
     * @return local adapter factory
     */
    protected LocalAdapterFactory getFactory() {
        return factory;
    }

    /**
     * Utility method to create a local adapter for a remote item.
     * This method introspects the remote reference to determine
     * whether to instantiate a {@link javax.jcr.Property},
     * a {@link Node Node}, or an {@link Item Item} adapter using
     * the local adapter factory.
     * <p>
     * If the remote item is a {@link RemoteNode}, this method delegates
     * to {@link #getNode(Session, RemoteNode)}.
     *
     * @param session current session
     * @param remote remote item
     * @return local property, node, or item adapter
     */
    protected Item getItem(Session session, RemoteItem remote) {
        if (remote instanceof RemoteProperty) {
            return factory.getProperty(session, (RemoteProperty) remote);
        } else if (remote instanceof RemoteNode) {
            return getNode(session, (RemoteNode) remote);
        } else {
            return factory.getItem(session, remote);
        }
    }

    /**
     * Utility method to create a local adapter for a remote node.
     * This method introspects the remote reference to determine
     * whether to instantiate a {@link Node Node},
     * a {@link javax.jcr.version.VersionHistory VersionHistory}, or a
     *  {@link javax.jcr.version.Version Version} adapter using
     * the local adapter factory.
     *
     * @param session current session
     * @param remote remote node
     * @return local node, version, or version history adapter
     */
    protected Node getNode(Session session, RemoteNode remote) {
        if (remote instanceof RemoteVersion) {
            return factory.getVersion(session, (RemoteVersion) remote);
        } else if (remote instanceof RemoteVersionHistory) {
            return factory.getVersionHistory(session, (RemoteVersionHistory) remote);
        } else {
            return factory.getNode(session, remote);
        }
    }

    /**
     * Utility method for creating an array of local node type adapters
     * for an array of remote node types. The node type adapters are created
     * using the local adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty array.
     *
     * @param remotes remote node types
     * @return local node type array
     */
    protected NodeType[] getNodeTypeArray(RemoteNodeType[] remotes) {
        if (remotes != null) {
            NodeType[] types = new NodeType[remotes.length];
            for (int i = 0; i < remotes.length; i++) {
                types[i] = factory.getNodeType(remotes[i]);
            }
            return types;
        } else {
            return new NodeType[0]; // for safety
        }
    }

}
