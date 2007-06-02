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

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.remote.RemoteNamespaceRegistry;

/**
 * Remote adapter for the JCR
 * {@link javax.jcr.NamespaceRegistry NamespaceRegistry} interface.
 * This class makes a local namespace registry available as an RMI service
 * using the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteNamespaceRegistry RemoteNamespaceRegistry}
 * interface.
 *
 * @see javax.jcr.NamespaceRegistry
 * @see org.apache.jackrabbit.rmi.remote.RemoteNamespaceRegistry
 */
public class ServerNamespaceRegistry extends ServerObject implements
        RemoteNamespaceRegistry {

    /** The adapted local namespace registry. */
    private NamespaceRegistry registry;

    /**
     * Creates a remote adapter for the given local namespace registry.
     *
     * @param registry local namespace registry
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerNamespaceRegistry(
            NamespaceRegistry registry, RemoteAdapterFactory factory)
            throws RemoteException {
        super(factory);
        this.registry = registry;
    }

    /** {@inheritDoc} */
    public void registerNamespace(String prefix, String uri)
            throws RepositoryException, RemoteException {
        try {
            registry.registerNamespace(prefix, uri);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void unregisterNamespace(String prefix)
            throws RepositoryException, RemoteException {
        try {
            registry.unregisterNamespace(prefix);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String[] getPrefixes() throws RepositoryException, RemoteException {
        try {
            return registry.getPrefixes();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String[] getURIs() throws RepositoryException, RemoteException {
        try {
            return registry.getURIs();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getURI(String prefix)
            throws RepositoryException, RemoteException {
        try {
            return registry.getURI(prefix);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getPrefix(String uri)
            throws RepositoryException, RemoteException {
        try {
            return registry.getPrefix(uri);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

}
