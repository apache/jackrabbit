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

/**
 * Remote version of the JCR {@link javax.jcr.nodetype.ItemDefinition ItemDef}
 * interface. Used by the
 * {@link org.apache.jackrabbit.rmi.server.ServerItemDefinition ServerItemDefinition} and
 * {@link org.apache.jackrabbit.rmi.client.ClientItemDefinition ClientItemDefinition}
 * adapter base classes to provide transparent RMI access to remote item
 * definitions.
 * <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding ItemDef method. The remote object will simply forward
 * the method call to the underlying ItemDef instance. Argument and return
 * values, as well as possible exceptions, are copied over the network.
 * Complex {@link javax.jcr.nodetype.NodeType NodeType} return values
 * are returned as remote references to the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteNodeType RemoteNodeType}
 * interface. RMI errors are signaled with RemoteExceptions.
 *
 * @see javax.jcr.nodetype.ItemDefinition
 * @see org.apache.jackrabbit.rmi.client.ClientItemDefinition
 * @see org.apache.jackrabbit.rmi.server.ServerItemDefinition
 */
public interface RemoteItemDefinition extends Remote {

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.ItemDefinition#getDeclaringNodeType() ItemDef.getDeclaringNodeType()}
     * method.
     *
     * @return declaring node type
     * @throws RemoteException on RMI errors
     */
    RemoteNodeType getDeclaringNodeType() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.ItemDefinition#getName() ItemDef.getName()} method.
     *
     * @return item name
     * @throws RemoteException on RMI errors
     */
    String getName() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.ItemDefinition#isAutoCreated() ItemDef.isAutoCreate()}
     * method.
     *
     * @return <code>true</code> if the item is automatically created,
     *         <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean isAutoCreated() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.ItemDefinition#isMandatory() ItemDef.isMandatory()}
     * method.
     *
     * @return <code>true</code> if the item is mandatory,
     *         <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean isMandatory() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.ItemDefinition#getOnParentVersion() ItemDef.getOnParentVersion()}
     * method.
     *
     * @return parent version behaviour
     * @throws RemoteException on RMI errors
     */
    int getOnParentVersion() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.ItemDefinition#isProtected() ItemDef.isProtected()}
     * method.
     *
     * @return <code>true</code> if the item is protected,
     *         <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean isProtected() throws RemoteException;

}
