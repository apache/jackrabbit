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
package org.apache.jackrabbit.rmi.remote.security;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import org.apache.jackrabbit.rmi.remote.RemoteIterator;

/**
 * Remote version of the JCR {@link javax.jcr.security.AccessControlManager
 * AccessControlManager} interface. Used by the
 * {@link org.apache.jackrabbit.rmi.server.security.ServerAccessControlManager
 * ServerAccessControlManager} and
 * {@link org.apache.jackrabbit.rmi.client.security.ClientAccessControlManager
 * ClientAccessControlManager} adapter base classes to provide transparent RMI
 * access to remote item definitions.
 * <p>
 * The methods in this interface are documented only with a reference to a
 * corresponding AccessControlManager method. The remote object will simply
 * forward the method call to the underlying AccessControlManager instance.
 * Argument and return values, as well as possible exceptions, are copied over
 * the network. Complex return values are returned as remote references to the
 * corresponding remote interface. RMI errors are signaled with
 * RemoteExceptions.
 *
 * @see javax.jcr.security.AccessControlManager
 * @see org.apache.jackrabbit.rmi.client.security.ClientAccessControlManager
 * @see org.apache.jackrabbit.rmi.server.security.ServerAccessControlManager
 */
public interface RemoteAccessControlManager extends Remote {

    /**
     * @see javax.jcr.security.AccessControlManager#getApplicablePolicies(String)
     */
    public RemoteIterator getApplicablePolicies(String absPath)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.security.AccessControlManager#getEffectivePolicies(String)
     */
    public RemoteAccessControlPolicy[] getEffectivePolicies(String absPath)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.security.AccessControlManager#getPolicies(String)
     */
    public RemoteAccessControlPolicy[] getPolicies(String absPath)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.security.AccessControlManager#getPrivileges(String)
     */
    public RemotePrivilege[] getPrivileges(String absPath)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.security.AccessControlManager#getSupportedPrivileges(String)
     */
    public RemotePrivilege[] getSupportedPrivileges(String absPath)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.security.AccessControlManager#privilegeFromName(String)
     */
    public RemotePrivilege privilegeFromName(String privilegeName)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.security.AccessControlManager#hasPrivileges(String,
     *      javax.jcr.security.Privilege[])
     */
    public boolean hasPrivileges(String absPath, String[] privileges)
            throws RepositoryException, RemoteException;

}
