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

import java.rmi.RemoteException;

import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.rmi.remote.RemoteNodeDef;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteNodeDef RemoteNodeDef}
 * inteface. This class makes a remote node definition locally available using
 * the JCR {@link javax.jcr.nodetype.NodeDef NodeDef} interface.
 *
 * @author Jukka Zitting
 * @see javax.jcr.nodetype.NodeDef
 * @see org.apache.jackrabbit.rmi.remote.RemoteNodeDef
 */
public class ClientNodeDef extends ClientItemDef implements NodeDef {

    /** The adapted remote node definition. */
    private RemoteNodeDef remote;

    /**
     * Creates a local adapter for the given remote node definition.
     *
     * @param remote remote node definition
     * @param factory local adapter factory
     */
    public ClientNodeDef(RemoteNodeDef remote, LocalAdapterFactory factory) {
        super(remote, factory);
        this.remote = remote;
    }

    /** {@inheritDoc} */
    public NodeType[] getRequiredPrimaryTypes() {
        try {
            return getNodeTypeArray(remote.getRequiredPrimaryTypes());
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeType getDefaultPrimaryType() {
        try {
            return factory.getNodeType(remote.getDefaultPrimaryType());
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean allowSameNameSibs() {
        try {
            return remote.allowSameNameSibs();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

}
