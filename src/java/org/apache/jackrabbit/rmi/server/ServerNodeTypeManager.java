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

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.rmi.remote.RemoteNodeType;
import org.apache.jackrabbit.rmi.remote.RemoteNodeTypeManager;

/**
 * Remote adapter for the JCR
 * {@link javax.jcr.nodetype.NodeTypeManager NodeTypeManager}
 * interface. This class makes a local node type manager available as an
 * RMI service using the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteNodeTypeManager RemoteNodeTypeManager}
 * interface.
 * 
 * @author Jukka Zitting
 * @see javax.jcr.nodetype.NodeTypeManager
 * @see org.apache.jackrabbit.rmi.remote.RemoteNodeTypeManager
 */
public class ServerNodeTypeManager extends ServerObject
        implements RemoteNodeTypeManager {

    /** The adapted local node type manager. */
    protected NodeTypeManager manager;
    
    /**
     * Creates a remote adapter for the given local node type manager.
     * 
     * @param manager local node type manager
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerNodeTypeManager(
            NodeTypeManager manager, RemoteAdapterFactory factory)
            throws RemoteException {
        super(factory);
        this.manager = manager;
    }
    
    /** {@inheritDoc} */
    public RemoteNodeType getNodeType(String name) throws
            NoSuchNodeTypeException, RepositoryException, RemoteException {
        return factory.getRemoteNodeType(manager.getNodeType(name));
    }

    /** {@inheritDoc} */
    public RemoteNodeType[] getAllNodeTypes() throws RepositoryException,
            RemoteException {
        return getRemoteNodeTypeArray(manager.getAllNodeTypes());
    }

    /** {@inheritDoc} */
    public RemoteNodeType[] getPrimaryNodeTypes() throws RepositoryException,
            RemoteException {
        return getRemoteNodeTypeArray(manager.getPrimaryNodeTypes());
    }

    /** {@inheritDoc} */
    public RemoteNodeType[] getMixinNodeTypes() throws RepositoryException,
            RemoteException {
        return getRemoteNodeTypeArray(manager.getMixinNodeTypes());
    }

}
