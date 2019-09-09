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
package org.apache.jackrabbit.rmi.client.security;

import java.rmi.RemoteException;
import java.security.Principal;

import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.rmi.client.ClientObject;
import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;
import org.apache.jackrabbit.rmi.client.RemoteRuntimeException;
import org.apache.jackrabbit.rmi.remote.security.RemoteAccessControlEntry;

/**
 * Local adapter for the JCR-RMI {@link RemoteAccessControlEntry
 * RemoteAccessControlEntry} interface. This class makes a remote
 * AccessControlEntry locally available using the JCR {@link AccessControlEntry
 * AccessControlEntry} interface.
 *
 * @see javax.jcr.security.AccessControlEntry
 * @see org.apache.jackrabbit.rmi.remote.security.RemoteAccessControlEntry
 */
public class ClientAccessControlEntry extends ClientObject implements
        AccessControlEntry {

    private final RemoteAccessControlEntry race;

    public ClientAccessControlEntry(final RemoteAccessControlEntry race,
            final LocalAdapterFactory factory) {
        super(factory);
        this.race = race;
    }

    /** {@inheritDoc} */
    public Principal getPrincipal() {
        try {
            return getFactory().getPrincipal(race.getPrincipal());
        } catch (RemoteException re) {
            throw new RemoteRuntimeException(re);
        }
    }

    /** {@inheritDoc} */
    public Privilege[] getPrivileges() {
        try {
            return getFactory().getPrivilege(race.getPrivileges());
        } catch (RemoteException re) {
            throw new RemoteRuntimeException(re);
        }
    }

    RemoteAccessControlEntry getRemoteAccessControlEntry() {
        return race;
    }
}
