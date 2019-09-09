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
package org.apache.jackrabbit.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Remote version of the JCR {@link javax.jcr.Repository Repository} interface.
 * Used by the
 * {@link org.apache.jackrabbit.rmi.server.ServerRepository ServerRepository}
 * and
 * {@link org.apache.jackrabbit.rmi.client.ClientRepository ClientRepository}
 * adapters to provide transparent RMI access to remote repositories.
* <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding Repository method. The remote object will simply
 * forward the method call to the underlying Repository instance.
 * {@link javax.jcr.Session Session} objects are returned as remote references
 * to the {@link RemoteSession RemoteSession} interface. Simple return
 * values and possible exceptions are copied over the network to the client.
 * RMI errors are signaled with RemoteExceptions.
 *
 * @see javax.jcr.Repository
 * @see org.apache.jackrabbit.rmi.client.ClientRepository
 * @see org.apache.jackrabbit.rmi.server.ServerRepository
 */
public interface RemoteRepository extends Remote {

    /**
     * Remote version of the
     * {@link javax.jcr.Repository#getDescriptor(String) Repository.getDescriptor(String)}
     * method.
     *
     * @param key descriptor key
     * @return descriptor value
     * @throws RemoteException on RMI errors
     */
    String getDescriptor(String key) throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Repository#getDescriptorKeys() Repository.getDescriptorKeys()}
     * method.
     *
     * @return descriptor keys
     * @throws RemoteException on RMI errors
     */
    String[] getDescriptorKeys() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Repository#login() Repository.login(}} method.
     *
     * @return remote session
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteSession login() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Repository#login(String) Repository.login(String}}
     * method.
     *
     * @param workspace workspace name
     * @return remote session
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteSession login(String workspace)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Repository#login(Credentials) Repository.login(Credentials}}
     * method.
     *
     * @param credentials client credentials
     * @return remote session
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteSession login(Credentials credentials)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Repository#login(Credentials,String) Repository.login(Credentials,String}}
     * method.
     *
     * @param credentials client credentials
     * @param workspace workspace name
     * @return remote session
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteSession login(Credentials credentials, String workspace)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Repository#getDescriptorValue(String) Repository.getDescriptorValue(String)}
     * method.
     *
     * @return descriptor value
     * @throws RemoteException on RMI errors
     */
	Value getDescriptorValue(String key) throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Repository#getDescriptorValues(String) Repository.getDescriptorValues(String)}
     * method.
     *
     * @return descriptor value array
     * @throws RemoteException on RMI errors
     */
	Value[] getDescriptorValues(String key) throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Repository#isSingleValueDescriptor(String) Repository.isSingleValueDescriptor(String)}
     * method.
     *
     * @return boolean
     * @throws RemoteException on RMI errors
     */
	boolean isSingleValueDescriptor(String key) throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Repository#isStandardDescriptor(String) Repository.isStandardDescriptor(String)}
     * method.
     *
     * @return boolean
     * @throws RemoteException on RMI errors
     */
	boolean isStandardDescriptor(String key) throws RemoteException;

}
