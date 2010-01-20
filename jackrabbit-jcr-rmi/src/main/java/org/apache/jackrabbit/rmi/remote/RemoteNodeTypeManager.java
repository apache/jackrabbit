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
package org.apache.jackrabbit.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

/**
 * Remote version of the JCR
 * {@link javax.jcr.nodetype.NodeTypeManager NodeTypeManager} interface.
 * Used by the
 * {@link org.apache.jackrabbit.rmi.server.ServerNodeTypeManager ServerNodeTypeManager}
 * and
 * {@link org.apache.jackrabbit.rmi.client.ClientNodeTypeManager ClientNodeTypeManager}
 * adapters to provide transparent RMI access to remote node type managers.
 * <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding NodeTypeManager method. The remote object will
 * simply forward the method call to the underlying NodeTypeManager instance.
 * Arguments and possible exceptions are copied over the network. Complex
 * {@link javax.jcr.nodetype.NodeType NodeType} values are returned as
 * remote references to the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteNodeType RemoteNodeType}
 * interface. Iterator values are transmitted as object arrays. RMI errors
 * are signaled with RemoteExceptions.
 *
 * @see javax.jcr.nodetype.NodeTypeManager
 * @see org.apache.jackrabbit.rmi.client.ClientNodeTypeManager
 * @see org.apache.jackrabbit.rmi.server.ServerNodeTypeManager
 */
public interface RemoteNodeTypeManager extends Remote {

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeTypeManager#getNodeType(String) NodeTypeManager.getNodeType(String)}
     * method.
     *
     * @param name node type name
     * @return node type
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteNodeType getNodeType(String name)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeTypeManager#getAllNodeTypes() NodeTypeManager.getAllNodeTypes()}
     * method.
     *
     * @return all node types
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getAllNodeTypes()
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeTypeManager#getPrimaryNodeTypes() NodeTypeManager.getPrimaryNodeTypes()}
     * method.
     *
     * @return primary node types
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getPrimaryNodeTypes()
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeTypeManager#getMixinNodeTypes() NodeTypeManager.getMixinNodeTypes()}
     * method.
     *
     * @return mixin node types
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getMixinNodeTypes()
            throws RepositoryException, RemoteException;

    boolean hasNodeType(String name)
            throws RepositoryException, RemoteException;

    void unregisterNodeTypes(String[] names)
            throws RepositoryException, RemoteException;

}
