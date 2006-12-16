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
import java.io.InputStream;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerWorkspace;
import org.xml.sax.InputSource;

public class ServerJackrabbitWorkspace extends ServerWorkspace
        implements RemoteJackrabbitWorkspace {

    private final JackrabbitWorkspace workspace;

    public ServerJackrabbitWorkspace(
            JackrabbitWorkspace workspace, RemoteAdapterFactory factory)
            throws RemoteException {
        super(workspace, factory);
        this.workspace = workspace;
    }

    public void createWorkspace(String name, byte[] template)
            throws RepositoryException {
        try {
            if (template != null) {
                InputStream stream = new ByteArrayInputStream(template);
                workspace.createWorkspace(name, new InputSource(stream));
            } else {
                workspace.createWorkspace(name);
            }
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

}
