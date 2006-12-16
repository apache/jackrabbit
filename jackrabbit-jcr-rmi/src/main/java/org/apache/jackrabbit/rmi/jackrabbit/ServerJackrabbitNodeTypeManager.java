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
package org.apache.jackrabbit.rmi.jackrabbit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerNodeTypeManager;

public class ServerJackrabbitNodeTypeManager extends ServerNodeTypeManager
        implements RemoteJackrabbitNodeTypeManager {

    private final JackrabbitNodeTypeManager manager;

    public ServerJackrabbitNodeTypeManager(
            JackrabbitNodeTypeManager manager, RemoteAdapterFactory factory)
            throws RemoteException {
        super(manager, factory);
        this.manager = manager;
    }

    public boolean hasNodeType(String name) throws RepositoryException {
        return manager.hasNodeType(name);
    }

    public RemoteNodeType[] registerNodeTypes(
            byte[] content, String type) throws RepositoryException {
        try {
            InputStream stream = new ByteArrayInputStream(content);
            NodeType[] types = manager.registerNodeTypes(stream, type);
            RemoteNodeType[] remotes = new RemoteNodeType[types.length];
            for (int i = 0; i < types.length; i++) {
                remotes[i] = getFactory().getRemoteNodeType(types[i]);
            }
            return remotes;
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

}
