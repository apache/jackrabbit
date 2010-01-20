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
package org.apache.jackrabbit.rmi.client;

import java.rmi.RemoteException;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.remote.RemoteNamespaceRegistry;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteNamespaceRegistry RemoteNamespaceRegistry}
 * interface. This class makes a remote namespace registry locally available
 * using the JCR {@link javax.jcr.NamespaceRegistry NamespaceRegistry}
 * interface.
 *
 * @see javax.jcr.NamespaceRegistry
 * @see org.apache.jackrabbit.rmi.remote.RemoteNamespaceRegistry
 */
public class ClientNamespaceRegistry extends ClientObject implements
        NamespaceRegistry {

    /** The adapted remote namespace registry. */
    private RemoteNamespaceRegistry remote;

    /**
     * Creates a local adapter for the given remote namespace registry.
     *
     * @param remote remote namespace registry
     * @param factory local adapter factory
     */
    public ClientNamespaceRegistry(
            RemoteNamespaceRegistry remote, LocalAdapterFactory factory) {
        super(factory);
        this.remote = remote;
    }

    /** {@inheritDoc} */
    public void registerNamespace(String prefix, String uri)
            throws RepositoryException {
        try {
            remote.registerNamespace(prefix, uri);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void unregisterNamespace(String prefix) throws RepositoryException {
        try {
            remote.unregisterNamespace(prefix);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String[] getPrefixes() throws RepositoryException {
        try {
            return remote.getPrefixes();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String[] getURIs() throws RepositoryException {
        try {
            return remote.getURIs();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getURI(String prefix) throws RepositoryException {
        try {
            return remote.getURI(prefix);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getPrefix(String uri) throws RepositoryException {
        try {
            return remote.getPrefix(uri);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

}
