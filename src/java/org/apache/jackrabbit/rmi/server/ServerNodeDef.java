/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.rmi.server;

import java.rmi.RemoteException;

import javax.jcr.nodetype.NodeDef;

import org.apache.jackrabbit.rmi.remote.RemoteNodeDef;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;

/**
 * Remote adapter for the JCR {@link javax.jcr.nodetype.NodeDef NodeDef}
 * interface. This class makes a local node definition available as an
 * RMI service using the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteNodeDef RemoteNodeDef}
 * interface.
 * 
 * @author Jukka Zitting
 * @see javax.jcr.nodetype.NodeDef
 * @see org.apache.jackrabbit.rmi.remote.RemoteNodeDef
 */
public class ServerNodeDef extends ServerItemDef implements RemoteNodeDef {

    /** The adapted node definition. */
    protected NodeDef def;
    
    /**
     * Creates a remote adapter for the given local node definition.
     * 
     * @param def local node definition
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerNodeDef(NodeDef def, RemoteAdapterFactory factory)
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
        return factory.getRemoteNodeType(def.getDefaultPrimaryType());
    }

    /** {@inheritDoc} */
    public boolean allowSameNameSibs() throws RemoteException {
        return def.allowSameNameSibs();
    }

}
