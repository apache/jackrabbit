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
package org.apache.jackrabbit.rmi.client.principal;

import java.rmi.RemoteException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;

import org.apache.jackrabbit.api.security.principal.GroupPrincipal;
import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;
import org.apache.jackrabbit.rmi.client.RemoteRuntimeException;
import org.apache.jackrabbit.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.rmi.remote.principal.RemoteGroup;
import org.apache.jackrabbit.rmi.remote.principal.RemotePrincipal;

/**
 * Local adapter for the JCR-RMI {@link RemoteGroup RemoteGroup} interface.
 *
 * @see RemoteGroup
 */
public class ClientGroup extends ClientPrincipal implements GroupPrincipal {

    private final LocalAdapterFactory factory;

    public ClientGroup(final RemotePrincipal p,
            final LocalAdapterFactory factory) {
        super(p);
        this.factory = factory;
    }

    /**
     * @return false - this method is not implemented yet
     */
    public boolean addMember(Principal user) {
        // no support for adding member here
        return false;
    }

    public boolean isMember(Principal member) {
        try {
            return ((RemoteGroup) getRemotePrincipal()).isMember(member.getName());
        } catch (RemoteException re) {
            throw new RemoteRuntimeException(re);
        }
    }

    public Enumeration<? extends Principal> members() {
        try {
            final RemoteIterator remote = ((RemoteGroup) getRemotePrincipal()).members();
            final Iterator<Principal> pi = factory.getPrincipalIterator(remote);
            return new Enumeration<Principal>() {
                public boolean hasMoreElements() {
                    return pi.hasNext();
                }

                public Principal nextElement() {
                    return pi.next();
                }
            };
        } catch (RemoteException re) {
            throw new RemoteRuntimeException(re);
        }
    }

    /**
     * @return false - this method is not implemented yet
     */
    public boolean removeMember(Principal user) {
        // no support for removing member here
        return false;
    }
}
