/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.jackrabbit.rmi.remote.principal;

import java.rmi.RemoteException;
import org.apache.jackrabbit.rmi.remote.RemoteIterator;

/**
 * Remote version of the JCR {@link org.apache.jackrabbit.api.security.principal.GroupPrincipal GroupPrincipal} interface.
 * Used by the {@link org.apache.jackrabbit.rmi.server.principal.ServerGroup
 * ServerGroup} and
 * {@link org.apache.jackrabbit.rmi.client.principal.ClientGroup ClientGroup}
 * adapter base classes to provide transparent RMI access to remote item
 * definitions.
 * <p>
 * The methods in this interface are documented only with a reference to a
 * corresponding Group method. The remote object will simply forward the method
 * call to the underlying Group instance. Argument and return values, as well as
 * possible exceptions, are copied over the network. Complex return values are
 * returned as remote references to the corresponding remote interface. RMI
 * errors are signaled with RemoteExceptions.
 *
 * @see org.apache.jackrabbit.api.security.principal.GroupPrincipal
 * @see org.apache.jackrabbit.rmi.client.principal.ClientGroup
 * @see org.apache.jackrabbit.rmi.server.principal.ServerGroup
 */
public interface RemoteGroup extends RemotePrincipal {

    /**
     * @see org.apache.jackrabbit.api.security.principal.GroupPrincipal#isMember(java.security.Principal)
     */
    boolean isMember(String member) throws RemoteException;

    /**
     * @see org.apache.jackrabbit.api.security.principal.GroupPrincipal#members()
     */
    RemoteIterator members() throws RemoteException;

}