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
package org.apache.jackrabbit.rmi.client;

import java.rmi.RemoteException;

import javax.jcr.nodetype.ItemDef;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.rmi.remote.RemoteItemDef;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteItemDef RemoteItemDef}
 * inteface. This class makes a remote item definition locally available using
 * the JCR {@link javax.jcr.nodetype.ItemDef ItemDef} interface. Used mainly
 * as the base class for the
 * {@link org.apache.jackrabbit.rmi.client.ClientPropertyDef ClientPropertyDef}
 * and
 * {@link org.apache.jackrabbit.rmi.client.ClientNodeDef ClientNodeDef} adapters.
 * 
 * @author Jukka Zitting
 * @see javax.jcr.nodetype.ItemDef
 * @see org.apache.jackrabbit.rmi.remote.RemoteItemDef
 */
public class ClientItemDef extends ClientObject implements ItemDef {

    /** The adapted remote item definition. */
    private RemoteItemDef remote;
    
    /**
     * Creates a local adapter for the given remote item definition.
     * 
     * @param remote remote item definition
     * @param factory local adapter factory
     */
    public ClientItemDef(RemoteItemDef remote, LocalAdapterFactory factory) {
        super(factory);
        this.remote = remote;
    }

    /** {@inheritDoc} */
    public NodeType getDeclaringNodeType() {
        try {
            return factory.getNodeType(remote.getDeclaringNodeType());
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getName() {
        try {
            return remote.getName();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isAutoCreate() {
        try {
            return remote.isAutoCreate();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isMandatory() {
        try {
            return remote.isMandatory();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public int getOnParentVersion() {
        try {
            return remote.getOnParentVersion();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isProtected() {
        try {
            return remote.isProtected();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

}
