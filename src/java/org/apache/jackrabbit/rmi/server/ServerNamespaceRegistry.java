/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.rmi.server;

import java.rmi.RemoteException;

import javax.jcr.NamespaceException;
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
 * @author Jukka Zitting
 * @see javax.jcr.NamespaceRegistry
 * @see org.apache.jackrabbit.rmi.remote.RemoteNamespaceRegistry
 */
public class ServerNamespaceRegistry extends ServerObject implements
        RemoteNamespaceRegistry {
    
    /** The adapted local namespace registry. */
    protected NamespaceRegistry registry;
    
    /**
     * Creates a remote adapter for the given local namespace registry.
     * 
     * @param registry local namespace registry
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerNamespaceRegistry(NamespaceRegistry registry,
            RemoteAdapterFactory factory) throws RemoteException {
        super(factory);
        this.registry = registry;
    }

    /** {@inheritDoc} */
    public void registerNamespace(String prefix, String uri)
            throws NamespaceException, RepositoryException, RemoteException {
        registry.registerNamespace(prefix, uri);
    }

    /** {@inheritDoc} */
    public void unregisterNamespace(String prefix) throws NamespaceException,
            RepositoryException, RemoteException {
        registry.unregisterNamespace(prefix);
    }

    /** {@inheritDoc} */
    public String[] getPrefixes() throws RepositoryException, RemoteException {
        return registry.getPrefixes();
    }

    /** {@inheritDoc} */
    public String[] getURIs() throws RepositoryException, RemoteException {
        return registry.getURIs();
    }

    /** {@inheritDoc} */
    public String getURI(String prefix) throws NamespaceException,
            RepositoryException, RemoteException {
        return registry.getURI(prefix);
    }

    /** {@inheritDoc} */
    public String getPrefix(String uri) throws NamespaceException,
            RepositoryException, RemoteException {
        return registry.getPrefix(uri);
    }

}
