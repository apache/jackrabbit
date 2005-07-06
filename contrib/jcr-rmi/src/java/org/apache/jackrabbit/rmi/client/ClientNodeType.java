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

import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.rmi.remote.RemoteNodeDefinition;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;
import org.apache.jackrabbit.rmi.value.SerialValueFactory;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteNodeType RemoteNodeType}
 * inteface. This class makes a remote node type locally available using
 * the JCR {@link javax.jcr.nodetype.NodeType NodeType} interface.
 *
 * @author Jukka Zitting
 * @see javax.jcr.nodetype.NodeType
 * @see org.apache.jackrabbit.rmi.remote.RemoteNodeType
 */
public class ClientNodeType extends ClientObject implements NodeType {

    /** The adapted remote node type. */
    private RemoteNodeType remote;

    /**
     * Creates a local adapter for the given remote node type.
     *
     * @param remote remote node type
     * @param factory local adapter factory
     */
    public ClientNodeType(RemoteNodeType remote, LocalAdapterFactory factory) {
        super(factory);
        this.remote = remote;
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
    public boolean isMixin() {
        try {
            return remote.isMixin();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasOrderableChildNodes() {
        try {
            return remote.hasOrderableChildNodes();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeType[] getSupertypes() {
        try {
            return getNodeTypeArray(remote.getSupertypes());
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeType[] getDeclaredSupertypes() {
        try {
            return getNodeTypeArray(remote.getDeclaredSupertypes());
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isNodeType(String type) {
        try {
            return remote.isNodeType(type);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public PropertyDefinition[] getPropertyDefinitions() {
        try {
            return getPropertyDefArray(remote.getPropertyDefs());
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        try {
            return getPropertyDefArray(remote.getDeclaredPropertyDefs());
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeDefinition[] getChildNodeDefinitions() {
        try {
            RemoteNodeDefinition[] defs = remote.getChildNodeDefs();
            return getNodeDefArray(defs);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        try {
            RemoteNodeDefinition[] defs = remote.getDeclaredChildNodeDefs();
            return getNodeDefArray(defs);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean canSetProperty(String name, Value value) {
        try {
            return remote.canSetProperty(name, SerialValueFactory.makeSerialValue(value));
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean canSetProperty(String name, Value[] values) {
        try {
            Value[] serials = SerialValueFactory.makeSerialValueArray(values);
            return remote.canSetProperty(name, serials);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean canAddChildNode(String name) {
        try {
            return remote.canAddChildNode(name);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean canAddChildNode(String name, String type) {
        try {
            return remote.canAddChildNode(name, type);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean canRemoveItem(String name) {
        try {
            return remote.canRemoveItem(name);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getPrimaryItemName() {
        try {
            return remote.getPrimaryItemName();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }
}
