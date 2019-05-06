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
package org.apache.jackrabbit.rmi.server.security;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.rmi.remote.security.RemoteAccessControlManager;
import org.apache.jackrabbit.rmi.remote.security.RemoteAccessControlPolicy;
import org.apache.jackrabbit.rmi.remote.security.RemotePrivilege;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerObject;

public class ServerAccessControlManager extends ServerObject implements
        RemoteAccessControlManager {

    private final AccessControlManager acm;

    public ServerAccessControlManager(AccessControlManager acm,
            RemoteAdapterFactory factory) throws RemoteException {
        super(factory);
        this.acm = acm;
    }

    public RemoteIterator getApplicablePolicies(String absPath)
            throws RepositoryException, RemoteException {
        return getFactory().getRemoteAccessControlPolicyIterator(
            acm.getApplicablePolicies(absPath));
    }

    public RemoteAccessControlPolicy[] getEffectivePolicies(String absPath)
            throws RepositoryException, RemoteException {
        return getFactory().getRemoteAccessControlPolicy(
            acm.getEffectivePolicies(absPath));
    }

    public RemoteAccessControlPolicy[] getPolicies(String absPath)
            throws RepositoryException, RemoteException {
        return getFactory().getRemoteAccessControlPolicy(
            acm.getPolicies(absPath));
    }

    public RemotePrivilege[] getPrivileges(String absPath)
            throws RepositoryException, RemoteException {
        return getFactory().getRemotePrivilege(acm.getPrivileges(absPath));
    }

    public RemotePrivilege[] getSupportedPrivileges(String absPath)
            throws RepositoryException, RemoteException {
        return getFactory().getRemotePrivilege(
            acm.getSupportedPrivileges(absPath));
    }

    public boolean hasPrivileges(String absPath, String[] privileges)
            throws RepositoryException {
        Privilege[] privs = new Privilege[privileges.length];
        for (int i = 0; i < privs.length; i++) {
            privs[i] = acm.privilegeFromName(privileges[i]);
        }

        return acm.hasPrivileges(absPath, privs);
    }

    public RemotePrivilege privilegeFromName(String privilegeName)
            throws RepositoryException, RemoteException {
        return getFactory().getRemotePrivilege(
            acm.privilegeFromName(privilegeName));
    }
}
