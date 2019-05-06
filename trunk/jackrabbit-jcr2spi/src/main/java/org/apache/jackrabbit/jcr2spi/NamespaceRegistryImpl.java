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
package org.apache.jackrabbit.jcr2spi;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NamespaceRegistry;
import javax.jcr.NamespaceException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.RepositoryException;

/**
 * <code>NamespaceRegistryImpl</code> implements the JCR client facing
 * NamespaceRegistry.
 */
public class NamespaceRegistryImpl implements NamespaceRegistry {

    private static Logger log = LoggerFactory.getLogger(NamespaceRegistryImpl.class);

    private final NamespaceStorage storage;

    /**
     * Create a new <code>NamespaceRegistryImpl</code>.
     *
     * @param storage
     */
    public NamespaceRegistryImpl(NamespaceStorage storage) {
        this.storage = storage;
    }

    //--------------------------------------------------< NamespaceRegistry >---

    /**
     * @see NamespaceRegistry#registerNamespace(String, String)
     */
    public void registerNamespace(String prefix, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, RepositoryException {
        storage.registerNamespace(prefix, uri);
    }

    /**
     * @see NamespaceRegistry#unregisterNamespace(String)
     */
    public void unregisterNamespace(String prefix) throws NamespaceException, UnsupportedRepositoryOperationException, RepositoryException {
        storage.unregisterNamespace(getURI(prefix));
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getPrefixes()
     */
    public String[] getPrefixes() throws RepositoryException {
        Collection<String> prefixes = storage.getRegisteredNamespaces().keySet();
        return prefixes.toArray(new String[prefixes.size()]);
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getURIs()
     */
    public String[] getURIs() throws RepositoryException {
        Collection<String> uris = storage.getRegisteredNamespaces().values();
        return uris.toArray(new String[uris.size()]);
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getURI(String)
     * @see org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver#getURI(String)
     */
    public String getURI(String prefix) throws NamespaceException {
        // try to load the uri
        try {
            return storage.getURI(prefix);
        } catch (RepositoryException ex) {
            log.debug("Internal error while loading registered namespaces.");
            throw new NamespaceException(prefix + ": is not a registered namespace prefix.");
        }
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getPrefix(String)
     * @see org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver#getPrefix(String)
     */
    public String getPrefix(String uri) throws NamespaceException {
        // try to load the prefix
        try {
            return storage.getPrefix(uri);
        } catch (RepositoryException ex) {
            log.debug("Internal error while loading registered namespaces.");
            throw new NamespaceException(uri + ": is not a registered namespace uri.");
        }
    }

}
