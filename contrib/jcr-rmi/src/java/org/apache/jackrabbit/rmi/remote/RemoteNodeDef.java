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
package org.apache.jackrabbit.rmi.remote;

import java.rmi.RemoteException;

/**
 * Remote version of the JCR {@link javax.jcr.nodetype.NodeDef NodeDef}
 * interface. Used by the 
 * {@link org.apache.jackrabbit.rmi.server.ServerNodeDef ServerNodeDef} and
 * {@link org.apache.jackrabbit.rmi.client.ClientNodeDef ClientNodeDef}
 * adapters to provide transparent RMI access to remote node definitions.
 * <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding NodeDef method. The remote object will simply forward
 * the method call to the underlying NodeDef instance. Return values
 * and possible exceptions are copied over the network. Complex
 * {@link javax.jcr.nodetype.NodeType NodeType} return values 
 * are returned as remote references to the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteNodeType RemoteNodeType}
 * interface. RMI errors are signalled with RemoteExceptions.
 *
 * @author Jukka Zitting
 * @see javax.jcr.nodetype.NodeDef
 * @see org.apache.jackrabbit.rmi.client.ClientNodeDef
 * @see org.apache.jackrabbit.rmi.server.ServerNodeDef
 */
public interface RemoteNodeDef extends RemoteItemDef {
    
    /**
     * @see javax.jcr.nodetype.NodeDef#getRequiredPrimaryTypes()
     * @throws RemoteException on RMI errors
     */
    public RemoteNodeType[] getRequiredPrimaryTypes() throws RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeDef#getDefaultPrimaryType()
     * @throws RemoteException on RMI errors
     */
    public RemoteNodeType getDefaultPrimaryType() throws RemoteException;

    /**
     * @see javax.jcr.nodetype.NodeDef#allowSameNameSibs()
     * @throws RemoteException on RMI errors
     */
    public boolean allowSameNameSibs() throws RemoteException;

}
