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

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;
import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;
import org.apache.jackrabbit.rmi.remote.security.RemoteAccessControlList;

/**
 * Local adapter for the JCR-RMI {@link RemoteAccessControlList
 * RemoteAccessControlList} interface. This class makes a remote
 * AccessControlList locally available using the JCR {@link AccessControlList
 * AccessControlList} interface.
 *
 * @see javax.jcr.security.AccessControlList
 * @see org.apache.jackrabbit.rmi.remote.security.RemoteAccessControlList
 */
public class ClientAccessControlList extends ClientAccessControlPolicy
        implements AccessControlList {

    public ClientAccessControlList(final RemoteAccessControlList racl,
            final LocalAdapterFactory factory) {
        super(racl, factory);
    }

    /**
     * @throws UnsupportedRepositoryOperationException This method is not
     *             implemented yet
     */
    public boolean addAccessControlEntry(Principal principal,
            Privilege[] privileges)
            throws UnsupportedRepositoryOperationException {
        // TODO: implement client side of the story
        throw new UnsupportedRepositoryOperationException(
            "addAccessControlEntry");
    }

    /** {@inheritDoc} */
    public AccessControlEntry[] getAccessControlEntries()
            throws RepositoryException {
        try {
            return getFactory().getAccessControlEntry(
                ((RemoteAccessControlList) getRemoteAccessControlPolicy()).getAccessControlEntries());
        } catch (RemoteException re) {
            throw new RemoteRepositoryException(re);
        }
    }

    /**
     * @throws UnsupportedRepositoryOperationException This method is not
     *             implemented yet
     */
    public void removeAccessControlEntry(AccessControlEntry ace)
            throws UnsupportedRepositoryOperationException {
        // TODO: implement client side of the story
        throw new UnsupportedRepositoryOperationException(
            "removeAccessControlEntry");
    }

}
