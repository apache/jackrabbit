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

import java.rmi.RemoteException;

/**
 * Remote version of the JCR {@link javax.jcr.nodetype.NodeDefinition NodeDefinition}
 * interface. Used by the
 * {@link org.apache.jackrabbit.rmi.server.ServerNodeDefinition ServerNodeDefinition} and
 * {@link org.apache.jackrabbit.rmi.client.ClientNodeDefinition ClientNodeDefinition}
 * adapters to provide transparent RMI access to remote node definitions.
 * <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding NodeDef method. The remote object will simply forward
 * the method call to the underlying NodeDef instance. Return values
 * and possible exceptions are copied over the network. Complex
 * {@link javax.jcr.nodetype.NodeType NodeType} return values
 * are returned as remote references to the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteNodeType RemoteNodeType}
 * interface. RMI errors are signaled with RemoteExceptions.
 *
 * @see javax.jcr.nodetype.NodeDefinition
 * @see org.apache.jackrabbit.rmi.client.ClientNodeDefinition
 * @see org.apache.jackrabbit.rmi.server.ServerNodeDefinition
 */
public interface RemoteNodeDefinition extends RemoteItemDefinition {

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeDefinition#getRequiredPrimaryTypes() NodeDef.getRequiredPrimaryTypes()}
     * method.
     *
     * @return required primary node types
     * @throws RemoteException on RMI errors
     */
    RemoteNodeType[] getRequiredPrimaryTypes() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeDefinition#getDefaultPrimaryType() NodeDef.getDefaultPrimaryType()}
     * method.
     *
     * @return default primary node type
     * @throws RemoteException on RMI errors
     */
    RemoteNodeType getDefaultPrimaryType() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeDefinition#allowsSameNameSiblings() NodeDef.allowSameNameSibs()}
     * method.
     *
     * @return <code>true</code> if same name siblings are allowed,
     *         <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean allowsSameNameSiblings() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeDefinition#getDefaultPrimaryTypeName() NodeDef.getDefaultPrimaryTypeName()}
     * method.
     *
     * @return a String
     * @throws RemoteException on RMI errors
     */
	String getDefaultPrimaryTypeName() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeDefinition#getRequiredPrimaryTypeNames() NodeDef.getRequiredPrimaryTypeNames()}
     * method.
     *
     * @return a String array
     * @throws RemoteException on RMI errors
     */
	String[] getRequiredPrimaryTypeNames() throws RemoteException;

}
