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

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.rmi.remote.RemoteNodeDefinition;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteNodeDefinition RemoteNodeDefinition}
 * interface. This class makes a remote node definition locally available using
 * the JCR {@link javax.jcr.nodetype.NodeDefinition NodeDef} interface.
 *
 * @see javax.jcr.nodetype.NodeDefinition
 * @see org.apache.jackrabbit.rmi.remote.RemoteNodeDefinition
 */
public class ClientNodeDefinition extends ClientItemDefinition implements NodeDefinition {

    /** The adapted remote node definition. */
    private RemoteNodeDefinition remote;

    /**
     * Creates a local adapter for the given remote node definition.
     *
     * @param remote remote node definition
     * @param factory local adapter factory
     */
    public ClientNodeDefinition(RemoteNodeDefinition remote, LocalAdapterFactory factory) {
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
            RemoteNodeType nt = remote.getDefaultPrimaryType();
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
    public boolean allowsSameNameSiblings() {
        try {
            return remote.allowsSameNameSiblings();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getDefaultPrimaryTypeName() {
        try {
            return remote.getDefaultPrimaryTypeName();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public String[] getRequiredPrimaryTypeNames() {
        try {
            return remote.getRequiredPrimaryTypeNames();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

}
