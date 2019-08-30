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
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;

import org.apache.jackrabbit.api.security.principal.GroupPrincipal;
import org.apache.jackrabbit.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.rmi.remote.principal.RemoteGroup;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;

public class ServerGroup extends ServerPrincipal implements RemoteGroup {

    public ServerGroup(final GroupPrincipal principal, final RemoteAdapterFactory factory)
            throws RemoteException {
        super(principal, factory);
    }

    public ServerGroup(final Principal principal, final RemoteAdapterFactory factory)
            throws RemoteException {
        super(principal, factory);
    }

    public boolean isMember(String member) {
        return isMember(member, getPrincipal());
    }

    public RemoteIterator members() throws RemoteException {
        Iterator<Principal> members = new Iterator<Principal>() {
            final Enumeration<? extends Principal> base =  members(getPrincipal());

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

    private static boolean isMember(final String memberName, final Principal group) {
        Enumeration<? extends Principal> pe = members(group);
        while (pe.hasMoreElements()) {
            Principal p = pe.nextElement();
            if (memberName.equals(p.getName())) {
                return true;
            }

            if (isGroup(p) && isMember(memberName, p)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isGroup(Principal principal) {
        return principal instanceof GroupPrincipal;
    }

    private static Enumeration<? extends Principal> members(Principal principal) {
        if (principal instanceof GroupPrincipal) {
            return ((GroupPrincipal) principal).members();
        }
        return Collections.emptyEnumeration();
    }
}
