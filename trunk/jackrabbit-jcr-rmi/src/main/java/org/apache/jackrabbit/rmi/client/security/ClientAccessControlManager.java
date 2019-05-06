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

import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import org.apache.jackrabbit.rmi.client.ClientObject;
import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;
import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;
import org.apache.jackrabbit.rmi.remote.security.RemoteAccessControlManager;

/**
 * Local adapter for the JCR-RMI {@link RemoteAccessControlManager
 * RemoteAccessControlManager} interface. This class makes a remote
 * AccessControlManager locally available using the JCR
 * {@link AccessControlManager AccessControlManager} interface.
 *
 * @see javax.jcr.security.AccessControlManager
 * @see org.apache.jackrabbit.rmi.remote.security.RemoteAccessControlManager
 */
public class ClientAccessControlManager extends ClientObject implements
        AccessControlManager {

    private final RemoteAccessControlManager racm;

    public ClientAccessControlManager(final RemoteAccessControlManager racm,
            final LocalAdapterFactory factory) {
        super(factory);
        this.racm = racm;
    }

    /** {@inheritDoc} */
    public AccessControlPolicyIterator getApplicablePolicies(String absPath)
            throws RepositoryException {
        try {
            return getFactory().getAccessControlPolicyIterator(
                racm.getApplicablePolicies(absPath));
        } catch (RemoteException re) {
            throw new RemoteRepositoryException(re);
        }
    }

    /** {@inheritDoc} */
    public AccessControlPolicy[] getEffectivePolicies(String absPath)
            throws PathNotFoundException, AccessDeniedException,
            RepositoryException {
        try {
            return getFactory().getAccessControlPolicy(
                racm.getEffectivePolicies(absPath));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public AccessControlPolicy[] getPolicies(String absPath)
            throws PathNotFoundException, AccessDeniedException,
            RepositoryException {
        try {
            return getFactory().getAccessControlPolicy(
                racm.getPolicies(absPath));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Privilege[] getPrivileges(String absPath)
            throws PathNotFoundException, RepositoryException {
        try {
            return getFactory().getPrivilege(racm.getPrivileges(absPath));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Privilege[] getSupportedPrivileges(String absPath)
            throws PathNotFoundException, RepositoryException {
        try {
            return getFactory().getPrivilege(
                racm.getSupportedPrivileges(absPath));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasPrivileges(String absPath, Privilege[] privileges)
            throws PathNotFoundException, RepositoryException {
        String[] privNames = new String[privileges.length];
        for (int i = 0; i < privNames.length; i++) {
            privNames[i] = privileges[i].getName();
        }

        try {
            return racm.hasPrivileges(absPath, privNames);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }

    }

    /** {@inheritDoc} */
    public Privilege privilegeFromName(String privilegeName)
            throws AccessControlException, RepositoryException {
        try {
            return getFactory().getPrivilege(
                racm.privilegeFromName(privilegeName));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /**
     * @throws UnsupportedRepositoryOperationException This method is not
     *             implemented yet
     */
    public void removePolicy(String absPath, AccessControlPolicy policy)
            throws UnsupportedRepositoryOperationException {
        // TODO: implement client side of the story
        throw new UnsupportedRepositoryOperationException("removePolicy");
    }

    /**
     * @throws UnsupportedRepositoryOperationException This method is not
     *             implemented yet
     */
    public void setPolicy(String absPath, AccessControlPolicy policy)
            throws UnsupportedRepositoryOperationException {
        // TODO: implement client side of the story
        throw new UnsupportedRepositoryOperationException("setPolicy");
    }
}
