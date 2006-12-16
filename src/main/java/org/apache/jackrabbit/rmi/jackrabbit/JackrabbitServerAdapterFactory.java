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

import java.rmi.RemoteException;

import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.rmi.remote.RemoteNodeTypeManager;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;

public class JackrabbitServerAdapterFactory extends ServerAdapterFactory {

    public RemoteNodeTypeManager getRemoteNodeTypeManager(
            NodeTypeManager manager) throws RemoteException {
        if (manager instanceof JackrabbitNodeTypeManager) {
            return new ServerJackrabbitNodeTypeManager(
                    (JackrabbitNodeTypeManager) manager, this);
        } else {
            return super.getRemoteNodeTypeManager(manager);
        }
    }

    public RemoteWorkspace getRemoteWorkspace(Workspace workspace)
            throws RemoteException {
        if (workspace instanceof JackrabbitWorkspace) {
            return new ServerJackrabbitWorkspace(
                    (JackrabbitWorkspace) workspace, this);
        } else {
            return super.getRemoteWorkspace(workspace);
        }
    }

}
