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
package org.apache.jackrabbit.rmi.server.security;

import java.rmi.RemoteException;
import java.rmi.server.RemoteStub;

import javax.jcr.security.Privilege;

import org.apache.jackrabbit.rmi.remote.security.RemotePrivilege;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerObject;

public class ServerPrivilege extends ServerObject implements RemotePrivilege {

    private final Privilege privilege;

    public ServerPrivilege(final Privilege privilege,
            final RemoteAdapterFactory factory) throws RemoteException {
        super(factory);
        this.privilege = privilege;
    }

    public RemotePrivilege[] getAggregatePrivileges() throws RemoteException {
        return getFactory().getRemotePrivilege(
            privilege.getAggregatePrivileges());
    }

    public RemotePrivilege[] getDeclaredAggregatePrivileges()
            throws RemoteException {
        return getFactory().getRemotePrivilege(
            privilege.getDeclaredAggregatePrivileges());
    }

    public String getName() {
        return privilege.getName();
    }

    public boolean isAbstract() {
        return privilege.isAbstract();
    }

    public boolean isAggregate() {
        return privilege.isAggregate();
    }

    Privilege getPrivilege() {
        return privilege;
    }
}
