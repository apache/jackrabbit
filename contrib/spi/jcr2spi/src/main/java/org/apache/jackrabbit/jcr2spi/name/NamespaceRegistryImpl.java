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
import org.apache.jackrabbit.util.XMLChar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NamespaceRegistry;
import javax.jcr.NamespaceException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;

/**
 * <code>NamespaceRegistryImpl</code>...
 */
public class NamespaceRegistryImpl extends AbstractNamespaceResolver
    implements NamespaceRegistry, NameCache {

    private static Logger log = LoggerFactory.getLogger(NamespaceRegistryImpl.class);

    private static final HashSet reservedPrefixes = new HashSet();
    private static final HashSet reservedURIs = new HashSet();

    static {
        // reserved prefixes
        reservedPrefixes.add(QName.NS_XML_PREFIX);
        reservedPrefixes.add(QName.NS_XMLNS_PREFIX);
        // predefined (e.g. built-in) prefixes
        reservedPrefixes.add(QName.NS_REP_PREFIX);
        reservedPrefixes.add(QName.NS_JCR_PREFIX);
        reservedPrefixes.add(QName.NS_NT_PREFIX);
        reservedPrefixes.add(QName.NS_MIX_PREFIX);
        reservedPrefixes.add(QName.NS_SV_PREFIX);
        // reserved namespace URI's
        reservedURIs.add(QName.NS_XML_URI);
        reservedURIs.add(QName.NS_XMLNS_URI);
        // predefined (e.g. built-in) namespace URI's
        reservedURIs.add(QName.NS_REP_URI);
        reservedURIs.add(QName.NS_JCR_URI);
        reservedURIs.add(QName.NS_NT_URI);
        reservedURIs.add(QName.NS_MIX_URI);
        reservedURIs.add(QName.NS_SV_URI);
    }

    private final HashMap prefixToURI = new HashMap();
    private final HashMap uriToPrefix = new HashMap();

    private final NameResolver resolver;
    private final NamespaceStorage storage;

    private final boolean level2Repository;

    /**
     * Create a new <code>NamespaceRegistryImpl</code>.
     *
     * @param storage
     * @param level2Repository
     * @throws RepositoryException
     */
    public NamespaceRegistryImpl(NamespaceStorage storage, boolean level2Repository)
        throws RepositoryException {
        super(true); // enable listener support

        resolver = new CachingNameResolver(new ParsingNameResolver(this));
        this.storage = storage;
        this.level2Repository = level2Repository;

        load();
    }

    /**
     * Load all mappings from the <code>NamespaceStorage</code> and update this
     * registry.
     *
     * @throws RepositoryException
     */
    private void load() throws RepositoryException {
        Map nsValues = storage.getRegisteredNamespaces();
        Iterator prefixes = nsValues.keySet().iterator();
        while (prefixes.hasNext()) {
            String prefix = (String) prefixes.next();
            String uri = (String) nsValues.get(prefix);
            addMapping(prefix, uri);
        }
    }

    /**
     * Add a namespace with the given uri and prefix. If for the given
     * <code>uri</code> is already registered with a different prefix, the
     * existing mapping gets replaced.
     *
     * @param prefix
     * @param uri
     */
    private void addMapping(String prefix, String uri) {
        if (uriToPrefix.containsKey(uri)) {
            String oldPrefix = (String) uriToPrefix.get(uri);
            replaceMapping(oldPrefix, prefix, uri);
        } else {
            prefixToURI.put(prefix, uri);
            uriToPrefix.put(uri, prefix);
            notifyNamespaceAdded(prefix, uri);
        }
    }

    /**
     * Remove the entries with the given prefix and uri from the registry
     * and inform all listeners.
     *
     * @param prefix
     * @param uri
     */
    private void removeMapping(String prefix, String uri) {
        prefixToURI.remove(prefix).toString();
        uriToPrefix.remove(uri);
        // notify listeners
        notifyNamespaceRemoved(uri);
    }

    /**
     * Replace an existing registered namespace with the given <code>oldPrefix</code>
     * by an entry with the new prefix. Subsequently all listeners are informed
     * about the remapped namespace.
     *
     * @param oldPrefix
     * @param prefix
     * @param uri
     */
    private void replaceMapping(String oldPrefix, String prefix, String uri) {
        if (oldPrefix.equals(prefix)) {
            // mapping already existing -> nothing to do.
            return;
        }
        prefixToURI.remove(oldPrefix);
        prefixToURI.put(prefix, uri);
        uriToPrefix.put(uri, prefix);
        // notify: remapped existing namespace uri to new prefix
        notifyNamespaceRemapped(oldPrefix, prefix, uri);
    }

    //--------------------------------------------------< NamespaceRegistry >---
    /**
     * @see NamespaceRegistry#registerNamespace(String, String)
     */
    public void registerNamespace(String prefix, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, RepositoryException {
        if (!level2Repository) {
            throw new UnsupportedRepositoryOperationException("Repository is Level1 only.");
        }
        // perform basic validation checks
        if (prefix == null || uri == null) {
            throw new IllegalArgumentException("prefix/uri can not be null");
        }
        if (QName.NS_EMPTY_PREFIX.equals(prefix) || QName.NS_DEFAULT_URI.equals(uri)) {
            throw new NamespaceException("default namespace is reserved and can not be changed");
        }
        if (reservedURIs.contains(uri)) {
            throw new NamespaceException("failed to register namespace "
                + prefix + " -> " + uri + ": reserved URI");
        }
        if (reservedPrefixes.contains(prefix)) {
            throw new NamespaceException("failed to register namespace "
                + prefix + " -> " + uri + ": reserved prefix");
        }
        // special case: prefixes xml*
        if (prefix.toLowerCase().startsWith(QName.NS_XML_PREFIX)) {
            throw new NamespaceException("failed to register namespace "
                + prefix + " -> " + uri + ": reserved prefix");
        }
        // check if the prefix is a valid XML prefix
        if (!XMLChar.isValidNCName(prefix)) {
            throw new NamespaceException("failed to register namespace "
                + prefix + " -> " + uri + ": invalid prefix");
        }

        // check existing mappings
        String oldPrefix = (String) uriToPrefix.get(uri);
        if (prefix.equals(oldPrefix)) {
            throw new NamespaceException("failed to register namespace "
                + prefix + " -> " + uri + ": mapping already exists");
        }
        if (prefixToURI.containsKey(prefix)) {
            /**
             * prevent remapping of existing prefixes because this would in effect
             * remove the previously assigned namespace;
             * as we can't guarantee that there are no references to this namespace
             * (in names of nodes/properties/node types etc.) we simply don't allow it.
             */
            throw new NamespaceException("failed to register namespace "
                + prefix + " -> " + uri
                + ": remapping existing prefixes is not supported.");
        }

        // inform storage before mappings are added to maps and propagated to listeners
        storage.registerNamespace(prefix, uri);
        if (oldPrefix == null) {
            addMapping(prefix, uri);
        } else {
            replaceMapping(oldPrefix, prefix, uri);
        }
    }

    /**
     * @see NamespaceRegistry#unregisterNamespace(String)
     */
    public void unregisterNamespace(String prefix) throws NamespaceException, UnsupportedRepositoryOperationException, RepositoryException {
        if (!level2Repository) {
            throw new UnsupportedRepositoryOperationException("Repository is Level1 only.");
        }

        if (reservedPrefixes.contains(prefix)) {
            throw new NamespaceException("reserved prefix: " + prefix);
        }
        if (!prefixToURI.containsKey(prefix)) {
            throw new NamespaceException("unknown prefix: " + prefix);
        }

        // inform storage before mappings are added to maps and propagated to listeners
        String uri = prefixToURI.get(prefix).toString();
        storage.unregisterNamespace(uri);

        // update caches and notify listeners
        removeMapping(prefix, uri);
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getPrefixes()
     */
    public String[] getPrefixes() throws RepositoryException {
        return (String[]) prefixToURI.keySet().toArray(new String[prefixToURI.keySet().size()]);

    }

    /**
     * @see javax.jcr.NamespaceRegistry#getURIs()
     */
    public String[] getURIs() throws RepositoryException {
        return (String[]) uriToPrefix.keySet().toArray(new String[uriToPrefix.keySet().size()]);
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getURI(String)
     * @see org.apache.jackrabbit.name.NamespaceResolver#getURI(String)
     */
    public String getURI(String prefix) throws NamespaceException {
        if (!prefixToURI.containsKey(prefix)) {
            // reload mappings in order to make sure, the NamespaceRegistry is
            // up to date, and try to retrieve the uri again.
            try {
                load();
            } catch (RepositoryException ex) {
                log.warn("Internal error while loading registered namespaces.");
            }
        }

        String uri = (String) prefixToURI.get(prefix);
        if (uri == null) {
            throw new NamespaceException(prefix + ": is not a registered namespace prefix.");
        }

        return uri;
    }

    /**
     * @see javax.jcr.NamespaceRegistry#getPrefix(String)
     * @see org.apache.jackrabbit.name.NamespaceResolver#getPrefix(String)
     */
    public String getPrefix(String uri) throws NamespaceException {
        if (!uriToPrefix.containsKey(uri)) {
            // reload mappings in order to make sure, the NamespaceRegistry is
            // up to date, and try to retrieve the prefix again.
            try {
                load();
            } catch (RepositoryException ex) {
                log.warn("Internal error while loading registered namespaces.");
            }
        }
        String prefix = (String) uriToPrefix.get(uri);
        if (prefix == null) {
            throw new NamespaceException(uri + ": is not a registered namespace uri.");
        }

        return prefix;
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
}
