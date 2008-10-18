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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

/**
 * <code>NamespaceRegistryImpl</code> implements the JCR client facing
 * NamespaceRegistry.
 */
public class NamespaceRegistryImpl implements NamespaceRegistry {

    /**
     * The namespace storage.
     */
    private final NamespaceStorage storage;

    private final Map prefixToUri = new HashMap();

    private final Map uriToPrefix = new HashMap();

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
    public synchronized void registerNamespace(String prefix, String uri)
            throws RepositoryException {
        storage.registerNamespace(prefix, uri);
        reloadNamespaces();
    }

    /**
     * @see NamespaceRegistry#unregisterNamespace(String)
     */
    public synchronized void unregisterNamespace(String prefix)
            throws RepositoryException {
        storage.unregisterNamespace(prefix);
        reloadNamespaces();
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getPrefixes()
     */
    public synchronized String[] getPrefixes() throws RepositoryException {
        reloadNamespaces();
        return (String[]) prefixToUri.keySet().toArray(new String[prefixToUri.size()]);
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getURIs()
     */
    public synchronized String[] getURIs() throws RepositoryException {
        reloadNamespaces();
        return (String[]) uriToPrefix.keySet().toArray(new String[uriToPrefix.size()]);
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getURI(String)
     * @see org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver#getURI(String)
     */
    public synchronized String getURI(String prefix)
            throws RepositoryException {
        String uri = (String) prefixToUri.get(prefix);
        if (uri == null) {
            // Not found, try loading latest state from storage
            reloadNamespaces();
            uri = (String) prefixToUri.get(prefix);
        }
        if (uri == null) {
            // Still not found, it's not a known prefix
            throw new NamespaceException("Namespace not found: " + prefix);
        }
        return uri;
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getPrefix(String)
     * @see org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver#getPrefix(String)
     */
    public synchronized String getPrefix(String uri) throws RepositoryException {
        String prefix = (String) uriToPrefix.get(uri);
        if (prefix == null) {
            // Not found, try loading latest state from storage
            reloadNamespaces();
            prefix = (String) uriToPrefix.get(uri);
        }
        if (prefix == null) {
            // Still not found, it's not a known URI
            throw new NamespaceException("Namespace not found: " + uri);
        }
        return prefix;
    }

    //-------------------------------------------------------------< private >

    /**
     * Clears the current namespace cache and loads new mappings from
     * the underlying namespace storage.
     *
     * @throws RepositoryException if new mappings could not be loaded
     */
    private synchronized void reloadNamespaces() throws RepositoryException {
        Map namespaces = storage.getRegisteredNamespaces();

        prefixToUri.clear();
        uriToPrefix.clear();

        Iterator iterator = namespaces.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            prefixToUri.put(entry.getKey(), entry.getValue());
            uriToPrefix.put(entry.getValue(), entry.getKey());
        }
    }

}
