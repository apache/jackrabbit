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
package org.apache.jackrabbit.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jcr.Value;

/**
 * Remote version of the JCR {@link javax.jcr.nodetype.NodeType NodeType}
 * interface. Used by the
 * {@link org.apache.jackrabbit.rmi.server.ServerNodeType ServerNodeType} and
 * {@link org.apache.jackrabbit.rmi.client.ClientNodeType ClientNodeType}
 * adapters to provide transparent RMI access to remote node types.
 * <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding NodeType method. The remote object will simply forward
 * the method call to the underlying NodeType instance. Return values
 * and possible exceptions are copied over the network. Complex return
 * values (like NodeTypes and PropertyDefs) are retunred as remote
 * references to the corresponding remote interfaces. RMI errors are
 * signalled with RemoteExceptions.
 *
 * @author Jukka Zitting
 * @see javax.jcr.nodetype.NodeType
 * @see org.apache.jackrabbit.rmi.client.ClientNodeType
 * @see org.apache.jackrabbit.rmi.server.ServerNodeType
 */
public interface RemoteNodeType extends Remote {

    /**
     * @see javax.jcr.nodetype.NodeType#getName()
     * @throws RemoteException on RMI errors
     */
    String getName() throws RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeType#isMixin()
     * @throws RemoteException on RMI errors
     */
    boolean isMixin() throws RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeType#hasOrderableChildNodes()
     * @throws RemoteException on RMI errors
     */
    boolean hasOrderableChildNodes() throws RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeType#getSupertypes()
     * @throws RemoteException on RMI errors
     */
    RemoteNodeType[] getSupertypes() throws RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeType#getDeclaredSupertypes()
     * @throws RemoteException on RMI errors
     */
    RemoteNodeType[] getDeclaredSupertypes() throws RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeType#isNodeType(java.lang.String)
     * @throws RemoteException on RMI errors
     */
    boolean isNodeType(String type) throws RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeType#getPropertyDefs()
     * @throws RemoteException on RMI errors
     */
    RemotePropertyDef[] getPropertyDefs() throws RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeType#getDeclaredPropertyDefs()
     * @throws RemoteException on RMI errors
     */
    RemotePropertyDef[] getDeclaredPropertyDefs() throws RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeType#getChildNodeDefs()
     * @throws RemoteException on RMI errors
     */
    RemoteNodeDef[] getChildNodeDefs() throws RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeType#getDeclaredChildNodeDefs()
     * @throws RemoteException on RMI errors
     */
    RemoteNodeDef[] getDeclaredChildNodeDefs() throws RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeType#canSetProperty(java.lang.String, javax.jcr.Value)
     * @throws RemoteException on RMI errors
     */
    boolean canSetProperty(String name, Value value) throws RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeType#canSetProperty(java.lang.String, javax.jcr.Value[])
     * @throws RemoteException on RMI errors
     */
    boolean canSetProperty(String name, Value[] values) throws RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeType#canAddChildNode(java.lang.String)
     * @throws RemoteException on RMI errors
     */
    boolean canAddChildNode(String name) throws RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeType#canAddChildNode(java.lang.String, java.lang.String)
     * @throws RemoteException on RMI errors
     */
    boolean canAddChildNode(String name, String type) throws RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeType#canRemoveItem(java.lang.String)
     * @throws RemoteException on RMI errors
     */
    boolean canRemoveItem(String name) throws RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeType#getPrimaryItemName()
     * @throws RemoteException on RMI errors
     */
    String getPrimaryItemName() throws RemoteException;

}
