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

import java.rmi.RemoteException;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.rmi.remote.RemoteItemDefinition;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteItemDefinition RemoteItemDefinition}
 * interface. This class makes a remote item definition locally available using
 * the JCR {@link javax.jcr.nodetype.ItemDefinition ItemDef} interface. Used mainly
 * as the base class for the
 * {@link org.apache.jackrabbit.rmi.client.ClientPropertyDefinition ClientPropertyDefinition}
 * and
 * {@link org.apache.jackrabbit.rmi.client.ClientNodeDefinition ClientNodeDefinition} adapters.
 *
 * @see javax.jcr.nodetype.ItemDefinition
 * @see org.apache.jackrabbit.rmi.remote.RemoteItemDefinition
 */
public class ClientItemDefinition extends ClientObject implements ItemDefinition {

    /** The adapted remote item definition. */
    private RemoteItemDefinition remote;

    /**
     * Creates a local adapter for the given remote item definition.
     *
     * @param remote remote item definition
     * @param factory local adapter factory
     */
    public ClientItemDefinition(RemoteItemDefinition remote, LocalAdapterFactory factory) {
        super(factory);
        this.remote = remote;
    }

    /** {@inheritDoc} */
    public NodeType getDeclaringNodeType() {
        try {
            RemoteNodeType nt = remote.getDeclaringNodeType();
            if (nt == null) {
                return null;
            } else {
                return getFactory().getNodeType(nt);
            }
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
    public boolean isAutoCreated() {
        try {
            return remote.isAutoCreated();
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
