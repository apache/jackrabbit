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

import javax.jcr.security.Privilege;

import org.apache.jackrabbit.rmi.client.ClientObject;
import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;
import org.apache.jackrabbit.rmi.client.RemoteRuntimeException;
import org.apache.jackrabbit.rmi.remote.security.RemotePrivilege;

/**
 * Local adapter for the JCR-RMI {@link RemotePrivilege RemotePrivilege}
 * interface. This class makes a remote Privilege locally available using the
 * JCR {@link Privilege Privilege} interface.
 *
 * @see javax.jcr.security.Privilege
 * @see org.apache.jackrabbit.rmi.remote.security.RemotePrivilege
 */
public class ClientPrivilege extends ClientObject implements Privilege {

    private final RemotePrivilege rp;

    public ClientPrivilege(final RemotePrivilege rp,
            final LocalAdapterFactory factory) {
        super(factory);
        this.rp = rp;
    }

    /** {@inheritDoc} */
    public Privilege[] getAggregatePrivileges() {
        try {
            return getFactory().getPrivilege(rp.getAggregatePrivileges());
        } catch (RemoteException re) {
            throw new RemoteRuntimeException(re);
        }
    }

    /** {@inheritDoc} */
    public Privilege[] getDeclaredAggregatePrivileges() {
        try {
            return getFactory().getPrivilege(
                rp.getDeclaredAggregatePrivileges());
        } catch (RemoteException re) {
            throw new RemoteRuntimeException(re);
        }
    }

    /** {@inheritDoc} */
    public String getName() {
        try {
            return rp.getName();
        } catch (RemoteException re) {
            throw new RemoteRuntimeException(re);
        }
    }

    /** {@inheritDoc} */
    public boolean isAbstract() {
        try {
            return rp.isAbstract();
        } catch (RemoteException re) {
            throw new RemoteRuntimeException(re);
        }
    }

    /** {@inheritDoc} */
    public boolean isAggregate() {
        try {
            return rp.isAggregate();
        } catch (RemoteException re) {
            throw new RemoteRuntimeException(re);
        }
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null) {
            return false;
        }

        if (obj instanceof Privilege) {
            return getName().equals(((Privilege) obj).getName());
        }

        return false;
    }

    public int hashCode() {
        return getName().hashCode();
    }
}
