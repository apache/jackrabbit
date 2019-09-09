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

import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.rmi.remote.RemoteNodeDefinition;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;
import org.apache.jackrabbit.rmi.remote.RemotePropertyDefinition;

/**
 * Remote adapter for the JCR {@link javax.jcr.nodetype.NodeType NodeType}
 * interface. This class makes a local node type available as an RMI service
 * using the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteNodeType RemoteNodeType}
 * interface.
 *
 * @see javax.jcr.nodetype.NodeType
 * @see org.apache.jackrabbit.rmi.remote.RemoteNodeType
 */
public class ServerNodeType extends ServerObject implements RemoteNodeType {

    /** The adapted local node type. */
    private NodeType type;

    /**
     * Creates a remote adapter for the given local node type.
     *
     * @param type local node type
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerNodeType(NodeType type, RemoteAdapterFactory factory)
            throws RemoteException {
        super(factory);
        this.type = type;
    }

    /**
     * Utility method for creating an array of remote references for
     * local node definitions. The remote references are created using the
     * remote adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty array.
     *
     * @param defs local node definition array
     * @return remote node definition array
     * @throws RemoteException on RMI errors
     */
    private RemoteNodeDefinition[] getRemoteNodeDefArray(NodeDefinition[] defs)
            throws RemoteException {
        if (defs != null) {
            RemoteNodeDefinition[] remotes =
                new RemoteNodeDefinition[defs.length];
            for (int i = 0; i < defs.length; i++) {
                remotes[i] = getFactory().getRemoteNodeDefinition(defs[i]);
            }
            return remotes;
        } else {
            return new RemoteNodeDefinition[0]; // for safety
        }
    }

    /**
     * Utility method for creating an array of remote references for
     * local property definitions. The remote references are created using the
     * remote adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty array.
     *
     * @param defs local property definition array
     * @return remote property definition array
     * @throws RemoteException on RMI errors
     */
    private RemotePropertyDefinition[] getRemotePropertyDefArray(
            PropertyDefinition[] defs) throws RemoteException {
        if (defs != null) {
            RemotePropertyDefinition[] remotes =
                new RemotePropertyDefinition[defs.length];
            for (int i = 0; i < defs.length; i++) {
                remotes[i] = getFactory().getRemotePropertyDefinition(defs[i]);
            }
            return remotes;
        } else {
            return new RemotePropertyDefinition[0]; // for safety
        }
    }


    /** {@inheritDoc} */
    public String getName() throws RemoteException {
        return type.getName();
    }

    /** {@inheritDoc} */
    public boolean isMixin() throws RemoteException {
        return type.isMixin();
    }

    /** {@inheritDoc} */
    public boolean isAbstract() throws RemoteException {
        return type.isAbstract();
    }

    /** {@inheritDoc} */
    public boolean hasOrderableChildNodes() throws RemoteException {
        return type.hasOrderableChildNodes();
    }

    /** {@inheritDoc} */
    public RemoteNodeType[] getSupertypes() throws RemoteException {
        return getRemoteNodeTypeArray(type.getSupertypes());
    }

    /** {@inheritDoc} */
    public RemoteNodeType[] getDeclaredSupertypes() throws RemoteException {
        return getRemoteNodeTypeArray(type.getDeclaredSupertypes());
    }

    /** {@inheritDoc} */
    public boolean isNodeType(String type) throws RemoteException {
        return this.type.isNodeType(type);
    }

    /** {@inheritDoc} */
    public RemotePropertyDefinition[] getPropertyDefs() throws RemoteException {
        PropertyDefinition[] defs = type.getPropertyDefinitions();
        return getRemotePropertyDefArray(defs);
    }

    /** {@inheritDoc} */
    public RemotePropertyDefinition[] getDeclaredPropertyDefs()
            throws RemoteException {
        PropertyDefinition[] defs = type.getDeclaredPropertyDefinitions();
        return getRemotePropertyDefArray(defs);
    }

    /** {@inheritDoc} */
    public RemoteNodeDefinition[] getChildNodeDefs() throws RemoteException {
        return getRemoteNodeDefArray(type.getChildNodeDefinitions());
    }

    /** {@inheritDoc} */
    public RemoteNodeDefinition[] getDeclaredChildNodeDefs() throws RemoteException {
        return getRemoteNodeDefArray(type.getDeclaredChildNodeDefinitions());
    }

    /** {@inheritDoc} */
    public boolean canSetProperty(String name, Value value)
            throws RemoteException {
        return type.canSetProperty(name, value);
    }

    /** {@inheritDoc} */
    public boolean canSetProperty(String name, Value[] values)
            throws RemoteException {
        return type.canSetProperty(name, values);
    }

    /** {@inheritDoc} */
    public boolean canAddChildNode(String name) throws RemoteException {
        return type.canAddChildNode(name);
    }

    /** {@inheritDoc} */
    public boolean canAddChildNode(String name, String type)
            throws RemoteException {
        return this.type.canAddChildNode(name, type);
    }

    /** {@inheritDoc} */
    public boolean canRemoveItem(String name) throws RemoteException {
        return type.canRemoveItem(name);
    }

    /** {@inheritDoc} */
    public String getPrimaryItemName() throws RemoteException {
        return type.getPrimaryItemName();
    }

    /** {@inheritDoc} */
	public boolean canRemoveNode(String nodeName) {
		return type.canRemoveNode(nodeName);
	}

    /** {@inheritDoc} */
	public boolean canRemoveProperty(String propertyName) {
		return type.canRemoveProperty(propertyName);
	}

    /** {@inheritDoc} */
	public String[] getDeclaredSupertypeNames() {
		return type.getDeclaredSupertypeNames();
	}

    /** {@inheritDoc} */
	public boolean isQueryable() {
		return type.isQueryable();
	}

    /** {@inheritDoc} */
	public RemoteIterator getDeclaredSubtypes() throws RemoteException {
       	return getFactory().getRemoteNodeTypeIterator(type.getDeclaredSubtypes());
	}

    /** {@inheritDoc} */
	public RemoteIterator getSubtypes() throws RemoteException {
		return getFactory().getRemoteNodeTypeIterator(type.getSubtypes());
	}

}
