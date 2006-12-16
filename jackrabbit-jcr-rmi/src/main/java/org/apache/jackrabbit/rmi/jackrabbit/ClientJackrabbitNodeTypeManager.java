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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.rmi.client.ClientNodeTypeManager;
import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;
import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ClientJackrabbitNodeTypeManager extends ClientNodeTypeManager
        implements JackrabbitNodeTypeManager {

    private final RemoteJackrabbitNodeTypeManager remote;

    public ClientJackrabbitNodeTypeManager(
            RemoteJackrabbitNodeTypeManager remote,
            LocalAdapterFactory factory) {
        super(remote, factory);
        this.remote = remote;
    }

    public boolean hasNodeType(String name) throws RepositoryException {
        try {
            return remote.hasNodeType(name);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }


    public NodeType[] registerNodeTypes(InputSource in)
            throws SAXException, RepositoryException {
        try {
            return registerNodeTypes(in.getByteStream(), TEXT_XML);
        } catch (IOException e) {
            throw new SAXException("Error reading node type stream", e);
        }
    }


    public NodeType[] registerNodeTypes(InputStream in, String contentType)
            throws IOException, RepositoryException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1000];
            for (int n = in.read(buffer); n != -1; n = in.read(buffer)) {
                out.write(buffer, 0, n);
            }

            RemoteNodeType[] remotes =
                remote.registerNodeTypes(out.toByteArray(), contentType);
            NodeType[] types = new NodeType[remotes.length];
            for (int i = 0; i < remotes.length; i++) {
                types[i] = getFactory().getNodeType(remotes[i]);
            }
            return types;
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        } finally {
            in.close();
        }
    }

}
