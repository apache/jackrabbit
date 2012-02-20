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
package org.apache.jackrabbit.rmi.server;

import java.rmi.RemoteException;

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.rmi.remote.RemoteNodeDefinition;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;

/**
 * Remote adapter for the JCR {@link javax.jcr.nodetype.NodeDefinition NodeDefinition}
 * interface. This class makes a local node definition available as an
 * RMI service using the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteNodeDefinition RemoteNodeDefinition}
 * interface.
 *
 * @see javax.jcr.nodetype.NodeDefinition
 * @see org.apache.jackrabbit.rmi.remote.RemoteNodeDefinition
 */
public class ServerNodeDefinition extends ServerItemDefinition implements RemoteNodeDefinition {

    /** The adapted node definition. */
    private NodeDefinition def;

    /**
     * Creates a remote adapter for the given local node definition.
     *
     * @param def local node definition
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerNodeDefinition(NodeDefinition def, RemoteAdapterFactory factory)
            throws RemoteException {
        super(def, factory);
        this.def = def;
    }

    /** {@inheritDoc} */
    public RemoteNodeType[] getRequiredPrimaryTypes() throws RemoteException {
        return getRemoteNodeTypeArray(def.getRequiredPrimaryTypes());
    }

    /** {@inheritDoc} */
    public RemoteNodeType getDefaultPrimaryType() throws RemoteException {
        NodeType nt = def.getDefaultPrimaryType();
        if (nt == null) {
            return null;
        } else {
            return getFactory().getRemoteNodeType(def.getDefaultPrimaryType());
        }
    }

    /** {@inheritDoc} */
    public boolean allowsSameNameSiblings() throws RemoteException {
        return def.allowsSameNameSiblings();
    }

    /** {@inheritDoc} */
	public String getDefaultPrimaryTypeName() throws RemoteException {
        return def.getDefaultPrimaryTypeName();
	}

    /** {@inheritDoc} */
	public String[] getRequiredPrimaryTypeNames() throws RemoteException {
        return def.getRequiredPrimaryTypeNames();
	}

}
