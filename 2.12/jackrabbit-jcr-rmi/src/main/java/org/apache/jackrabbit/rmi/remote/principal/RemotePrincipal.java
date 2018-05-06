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

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote version of the JCR {@link java.security.Principal Principal}
 * interface. Used by the
 * {@link org.apache.jackrabbit.rmi.server.principal.ServerPrincipal
 * ServerPrincipal} and
 * {@link org.apache.jackrabbit.rmi.client.principal.ClientPrincipal
 * ClientPrincipal} adapter base classes to provide transparent RMI access to
 * remote item definitions.
 * <p>
 * The methods in this interface are documented only with a reference to a
 * corresponding Principal method. The remote object will simply forward the
 * method call to the underlying Principal instance. Argument and return values,
 * as well as possible exceptions, are copied over the network. Complex return
 * values are returned as remote references to the corresponding remote
 * interface. RMI errors are signaled with RemoteExceptions.
 *
 * @see java.security.Principal
 * @see org.apache.jackrabbit.rmi.client.principal.ClientPrincipal
 * @see org.apache.jackrabbit.rmi.server.principal.ServerPrincipal
 */
public interface RemotePrincipal extends Remote {

    /**
     * @see java.security.Principal#getName()
     */
    public String getName() throws RemoteException;

}
