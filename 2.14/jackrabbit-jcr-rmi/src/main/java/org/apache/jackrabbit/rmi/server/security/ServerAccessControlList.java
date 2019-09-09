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
import java.security.Principal;

import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.rmi.remote.principal.RemotePrincipal;
import org.apache.jackrabbit.rmi.remote.security.RemoteAccessControlEntry;
import org.apache.jackrabbit.rmi.remote.security.RemoteAccessControlList;
import org.apache.jackrabbit.rmi.remote.security.RemotePrivilege;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.server.principal.ServerPrincipal;

public class ServerAccessControlList extends ServerAccessControlPolicy
        implements RemoteAccessControlList {

    public ServerAccessControlList(final AccessControlList acl,
            final RemoteAdapterFactory factory) throws RemoteException {
        super(acl, factory);
    }

    public RemoteAccessControlEntry[] getAccessControlEntries()
            throws RepositoryException, RemoteException {
        return getFactory().getRemoteAccessControlEntry(
            ((AccessControlList) getAccessControlPolicy()).getAccessControlEntries());
    }

    public boolean addAccessControlEntry(RemotePrincipal principal,
            RemotePrivilege[] privileges) throws RepositoryException {

        Principal p = null;
        if (principal instanceof ServerPrincipal) {
            p = ((ServerPrincipal) principal).getPrincipal();
        }
        Privilege[] privs = new Privilege[privileges.length];
        for (int i = 0; privs != null && i < privs.length; i++) {
            if (privileges[i] instanceof ServerPrivilege) {
                privs[i] = ((ServerPrivilege) privileges[i]).getPrivilege();
            } else {
                // not a compatible remote privilege, abort
                privs = null;
            }
        }

        if (p != null && privs != null) {
            return ((AccessControlList) getAccessControlPolicy()).addAccessControlEntry(
                p, privs);
        }

        throw new RepositoryException("Unsupported Remote types");
    }

    public void removeAccessControlEntry(RemoteAccessControlEntry ace)
            throws RepositoryException {
        if (ace instanceof ServerAccessControlEntry) {
            AccessControlEntry lace = ((ServerAccessControlEntry) ace).getAccessControlEntry();
            ((AccessControlList) getAccessControlPolicy()).removeAccessControlEntry(lace);
        } else {
            throw new RepositoryException(
                "Unsupported RemoteAccessControlEntry type " + ace.getClass());
        }
    }
}
