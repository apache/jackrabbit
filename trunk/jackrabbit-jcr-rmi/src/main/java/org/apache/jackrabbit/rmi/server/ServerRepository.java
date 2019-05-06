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
package org.apache.jackrabbit.rmi.server;

import java.rmi.RemoteException;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.value.SerialValueFactory;

/**
 * Remote adapter for the JCR {@link javax.jcr.Repository Repository}
 * interface. This class makes a local repository available as an RMI service
 * using the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteRepository RemoteRepository}
 * interface.
 *
 * @see javax.jcr.Repository
 * @see org.apache.jackrabbit.rmi.remote.RemoteRepository
 */
public class ServerRepository extends ServerObject implements RemoteRepository {

    /** The adapted local repository. */
    private Repository repository;

    /**
     * Creates a remote adapter for the given local repository.
     *
     * @param repository local repository
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerRepository(
            Repository repository, RemoteAdapterFactory factory)
            throws RemoteException {
        super(factory);
        this.repository = repository;
    }

    /** {@inheritDoc} */
    public String getDescriptor(String name) throws RemoteException {
        return repository.getDescriptor(name);
    }

    /** {@inheritDoc} */
    public String[] getDescriptorKeys() throws RemoteException {
        return repository.getDescriptorKeys();
    }

    /** {@inheritDoc} */
    public RemoteSession login() throws RepositoryException, RemoteException {
        try {
            Session session = repository.login();
            return getFactory().getRemoteSession(session);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteSession login(String workspace)
            throws RepositoryException, RemoteException {
        try {
            Session session = repository.login(workspace);
            return getFactory().getRemoteSession(session);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteSession login(Credentials credentials)
            throws RepositoryException, RemoteException {
        try {
            Session session = repository.login(credentials);
            return getFactory().getRemoteSession(session);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteSession login(Credentials credentials, String workspace)
            throws RepositoryException, RemoteException {
        try {
            Session session = repository.login(credentials, workspace);
            return getFactory().getRemoteSession(session);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
	public Value getDescriptorValue(String key) throws RemoteException {
    	try {
            return SerialValueFactory.makeSerialValue(repository.getDescriptorValue(key));
    	} catch (RepositoryException ex) {
    		 throw new RemoteException(ex.getMessage(), ex);    		
    	}
	}

    /** {@inheritDoc} */
	public Value[] getDescriptorValues(String key) throws RemoteException {
    	try {
            return SerialValueFactory.makeSerialValueArray(repository.getDescriptorValues(key));
    	} catch (RepositoryException ex) {
    		throw new RemoteException(ex.getMessage(), ex);    		
    	}
	}

    /** {@inheritDoc} */
	public boolean isSingleValueDescriptor(String key) throws RemoteException {
		return repository.isSingleValueDescriptor(key);
	}

    /** {@inheritDoc} */
	public boolean isStandardDescriptor(String key) throws RemoteException {
		return repository.isStandardDescriptor(key);
	}

}
