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
package org.apache.jackrabbit.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

/**
 * Remote version of the JCR
 * {@link javax.jcr.NamespaceRegistry NamespaceRegistry} interface.
 * Used by the
 * {@link org.apache.jackrabbit.rmi.server.ServerNamespaceRegistry ServerNamespaceRegistry}
 * and
 * {@link org.apache.jackrabbit.rmi.client.ClientNamespaceRegistry ClientNamespaceRegistry}
 * adapters to provide transparent RMI access to remote namespace registries.
 * <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding NamespaceRegistry method. The remote object will
 * simply forward the method call to the underlying NamespaceRegistry instance.
 * Argument and return values, as well as possible exceptions, are copied
 * over the network. RMI errors are signalled with RemoteExceptions.
 *
 * @author Jukka Zitting
 * @see javax.jcr.NamespaceRegistry
 * @see org.apache.jackrabbit.rmi.client.ClientNamespaceRegistry
 * @see org.apache.jackrabbit.rmi.server.ServerNamespaceRegistry
 */
public interface RemoteNamespaceRegistry extends Remote {

    /**
     * @see javax.jcr.NamespaceRegistry#registerNamespace(java.lang.String,java.lang.String)
     * @throws RemoteException on RMI errors
     */
    void registerNamespace(String prefix, String uri)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.NamespaceRegistry#unregisterNamespace(java.lang.String)
     * @throws RemoteException on RMI errors
     */
    void unregisterNamespace(String prefix)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.NamespaceRegistry#getPrefixes()
     * @throws RemoteException on RMI errors
     */
    String[] getPrefixes() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.NamespaceRegistry#getURIs()
     * @throws RemoteException on RMI errors
     */
    String[] getURIs() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.NamespaceRegistry#getURI(java.lang.String)
     * @throws RemoteException on RMI errors
     */
    String getURI(String prefix) throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.NamespaceRegistry#getPrefix(java.lang.String)
     * @throws RemoteException on RMI errors
     */
    String getPrefix(String uri) throws RepositoryException, RemoteException;

}
