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
package org.apache.jackrabbit.rmi.server.principal;

import java.rmi.RemoteException;
import java.security.Principal;

import org.apache.jackrabbit.rmi.remote.principal.RemotePrincipal;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerObject;

public class ServerPrincipal extends ServerObject implements RemotePrincipal {

    private final Principal principal;

    public ServerPrincipal(final Principal principal,
            final RemoteAdapterFactory factory) throws RemoteException {
        super(factory);
        this.principal = principal;
    }

    public String getName() {
        return principal.getName();
    }

    /**
     * Returns the {@link Principal} encapsulated in this instance.
     * <p>
     * NOTE: This method is intended to only be used in the JCR RMI
     * implementation to be able to "send back" remote principals to the server
     * for implementation of the remote JCR API.
     *
     * @return the {@link Principal} encapsulated in this instance.
     */
    public Principal getPrincipal() {
        return principal;
    }
}
