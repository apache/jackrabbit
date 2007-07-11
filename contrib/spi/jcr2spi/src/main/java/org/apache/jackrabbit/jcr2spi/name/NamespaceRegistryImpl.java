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
package org.apache.jackrabbit.jcr2spi.name;

import org.apache.jackrabbit.name.AbstractNamespaceResolver;
import org.apache.jackrabbit.name.IllegalNameException;
import org.apache.jackrabbit.name.UnknownPrefixException;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameCache;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.CachingNameResolver;
import org.apache.jackrabbit.name.ParsingNameResolver;
import org.apache.jackrabbit.name.NameResolver;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.NamespaceListener;
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
public class NamespaceRegistryImpl extends AbstractNamespaceResolver
    implements NamespaceRegistry, NameCache {

    private static Logger log = LoggerFactory.getLogger(NamespaceRegistryImpl.class);

    private final NameResolver resolver;
    private final NamespaceStorage storage;
    private final NamespaceCache nsCache;

    /**
     * Create a new <code>NamespaceRegistryImpl</code>.
     *
     * @param storage
     */
    public NamespaceRegistryImpl(NamespaceStorage storage,
                                 NamespaceCache nsCache) {
        // listener support in AbstractNamespaceResolver is not needed
        // because we delegate listeners to NamespaceCache
        super(false);
        this.storage = storage;
        this.nsCache = nsCache;
        this.resolver = new CachingNameResolver(new ParsingNameResolver(this));
    }

    //--------------------------------------------------< NamespaceRegistry >---
    /**
     * @see NamespaceRegistry#registerNamespace(String, String)
     */
    public void registerNamespace(String prefix, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, RepositoryException {
        nsCache.registerNamespace(storage, prefix, uri);
    }

    /**
     * @see NamespaceRegistry#unregisterNamespace(String)
     */
    public void unregisterNamespace(String prefix) throws NamespaceException, UnsupportedRepositoryOperationException, RepositoryException {
        nsCache.unregisterNamespace(storage, prefix);
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getPrefixes()
     */
    public String[] getPrefixes() throws RepositoryException {
        return nsCache.getPrefixes(storage);
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getURIs()
     */
    public String[] getURIs() throws RepositoryException {
        return nsCache.getURIs(storage);
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getURI(String)
     * @see org.apache.jackrabbit.name.NamespaceResolver#getURI(String)
     */
    public String getURI(String prefix) throws NamespaceException {
        // try to load the uri
        try {
            return nsCache.getURI(storage, prefix);
        } catch (RepositoryException ex) {
            log.debug("Internal error while loading registered namespaces.");
            throw new NamespaceException(prefix + ": is not a registered namespace prefix.");
        }
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getPrefix(String)
     * @see org.apache.jackrabbit.name.NamespaceResolver#getPrefix(String)
     */
    public String getPrefix(String uri) throws NamespaceException {
        // try to load the prefix
        try {
            return nsCache.getPrefix(storage, uri);
        } catch (RepositoryException ex) {
            log.debug("Internal error while loading registered namespaces.");
            throw new NamespaceException(uri + ": is not a registered namespace uri.");
        }
    }

    /**
     * @see org.apache.jackrabbit.name.NamespaceResolver#getQName(String)
     * @deprecated
     */
    public QName getQName(String name)
            throws IllegalNameException, UnknownPrefixException {
        return NameFormat.parse(name, this);
    }

    /**
     * @see org.apache.jackrabbit.name.NamespaceResolver#getJCRName(QName)
     * @deprecated
     */
    public String getJCRName(QName name) throws NoPrefixDeclaredException {
        return NameFormat.format(name, this);
    }

    //----------------------------------------------------------< NameCache >---
    /**
     * {@inheritDoc}
     */
    public QName retrieveName(String jcrName) {
        try {
            return resolver.getQName(jcrName);
        } catch (NameException e) {
            return null;
        } catch (NamespaceException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public String retrieveName(QName name) {
        try {
            return resolver.getJCRName(name);
        } catch (NamespaceException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cacheName(String jcrName, QName name) {
    }

    /**
     * {@inheritDoc}
     */
    public void evictAllNames() {
    }

    //-----------------------< AbstractNamespaceResolver >----------------------

    /**
     * Unregister on <code>NamespaceCache</code>.
     * @param listener the namespace listener.
     */
    public void removeListener(NamespaceListener listener) {
        nsCache.removeListener(listener);
    }

    /**
     * Register on <code>NamespaceCache</code>.
     * @param listener the namespace listener.
     */
    public void addListener(NamespaceListener listener) {
        nsCache.addListener(listener);
    }
}
