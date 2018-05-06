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
import java.security.acl.Group;
import java.util.Enumeration;
import java.util.Iterator;

import org.apache.jackrabbit.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.rmi.remote.principal.RemoteGroup;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;

public class ServerGroup extends ServerPrincipal implements RemoteGroup {

    public ServerGroup(final Group principal, final RemoteAdapterFactory factory)
            throws RemoteException {
        super(principal, factory);
    }

    public boolean isMember(String member) {
        return isMember(member, (Group) getPrincipal());
    }

    public RemoteIterator members() throws RemoteException {
        Iterator<Principal> members = new Iterator<Principal>() {
            final Enumeration<? extends Principal> base = ((Group) getPrincipal()).members();

            public boolean hasNext() {
                return base.hasMoreElements();
            }

            public Principal next() {
                return base.nextElement();
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
        return getFactory().getRemotePrincipalIterator(members);
    }

    private static boolean isMember(final String memberName, final Group group) {
        Enumeration<? extends Principal> pe = group.members();
        while (pe.hasMoreElements()) {
            Principal p = pe.nextElement();
            if (memberName.equals(p.getName())) {
                return true;
            }

            if ((p instanceof Group) && isMember(memberName, (Group) p)) {
                return true;
            }
        }

        return false;
    }
}