/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.rmi.client;

import java.rmi.RemoteException;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.remote.RemoteSession;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteRepository RemoteRepository}
 * inteface. This class makes a remote repository locally available using
 * the JCR {@link javax.jcr.Repository Repository} interface.
 * 
 * @author Jukka Zitting
 * @see javax.jcr.Repository
 * @see org.apache.jackrabbit.rmi.remote.RemoteRepository
 */
public class ClientRepository extends ClientObject implements Repository {

    /** The adapted remote repository. */
    private RemoteRepository remote;
    
    /**
     * Creates a client adapter for the given remote repository.
     * 
     * @param remote remote repository
     * @param factory local adapter factory
     */
    public ClientRepository(
            RemoteRepository remote, LocalAdapterFactory factory) {
        super(factory);
        this.remote = remote;
    }
    
    /** {@inheritDoc} */
    public String getDescriptor(String name) {
        try {
            return remote.getDescriptor(name);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public String[] getDescriptorKeys() {
        try {
            return remote.getDescriptorKeys();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }
    
    /** {@inheritDoc} */
    public Session login() throws LoginException, NoSuchWorkspaceException,
            RepositoryException {
        try {
            RemoteSession session = remote.login();
            return factory.getSession(this, session);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }
    
    /** {@inheritDoc} */
    public Session login(String workspace) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        try {
            RemoteSession session = remote.login(workspace);
            return factory.getSession(this, session);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Session login(Credentials credentials) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        try {
            RemoteSession session = remote.login(credentials);
            return factory.getSession(this, session);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }
    
    /** {@inheritDoc} */
    public Session login(Credentials credentials, String workspace) throws
            LoginException, NoSuchWorkspaceException, RepositoryException {
        try {
            RemoteSession session = remote.login(credentials, workspace);
            return factory.getSession(this, session);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

}
