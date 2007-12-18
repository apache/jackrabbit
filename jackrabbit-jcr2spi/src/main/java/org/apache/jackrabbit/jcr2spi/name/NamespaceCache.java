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

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceListener;
import org.apache.jackrabbit.util.XMLChar;
import org.apache.jackrabbit.spi.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NamespaceException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.Repository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.Collection;
import java.util.WeakHashMap;

/**
 * <code>NamespaceCache</code>...
 */
public class NamespaceCache {

    private static Logger log = LoggerFactory.getLogger(NamespaceCache.class);

    private static final Map INSTANCES = new WeakHashMap();

    private static final Set RESERVED_PREFIXES = new HashSet();
    private static final Set RESERVED_URIS = new HashSet();
    private static final Map RESERVED_NAMESPACES = new HashMap();

    static {
        // reserved prefixes
        RESERVED_PREFIXES.add(Name.NS_XML_PREFIX);
        RESERVED_PREFIXES.add(Name.NS_XMLNS_PREFIX);
        // predefined (e.g. built-in) prefixes
        RESERVED_PREFIXES.add(Name.NS_REP_PREFIX);
        RESERVED_PREFIXES.add(Name.NS_JCR_PREFIX);
        RESERVED_PREFIXES.add(Name.NS_NT_PREFIX);
        RESERVED_PREFIXES.add(Name.NS_MIX_PREFIX);
        RESERVED_PREFIXES.add(Name.NS_SV_PREFIX);
        RESERVED_PREFIXES.add(Name.NS_EMPTY_PREFIX);
        // reserved namespace URI's
        RESERVED_URIS.add(Name.NS_XML_URI);
        RESERVED_URIS.add(Name.NS_XMLNS_URI);
        // predefined (e.g. built-in) namespace URI's
        RESERVED_URIS.add(Name.NS_REP_URI);
        RESERVED_URIS.add(Name.NS_JCR_URI);
        RESERVED_URIS.add(Name.NS_NT_URI);
        RESERVED_URIS.add(Name.NS_MIX_URI);
        RESERVED_URIS.add(Name.NS_SV_URI);
        RESERVED_URIS.add(Name.NS_DEFAULT_URI);
        // reserved and predefined namespaces
        RESERVED_NAMESPACES.put(Name.NS_XML_PREFIX, Name.NS_XML_URI);
        RESERVED_NAMESPACES.put(Name.NS_XMLNS_PREFIX, Name.NS_XMLNS_URI);
        RESERVED_NAMESPACES.put(Name.NS_REP_PREFIX, Name.NS_REP_URI);
        RESERVED_NAMESPACES.put(Name.NS_JCR_PREFIX, Name.NS_JCR_URI);
        RESERVED_NAMESPACES.put(Name.NS_NT_PREFIX, Name.NS_NT_URI);
        RESERVED_NAMESPACES.put(Name.NS_MIX_PREFIX, Name.NS_MIX_URI);
        RESERVED_NAMESPACES.put(Name.NS_SV_PREFIX, Name.NS_SV_URI);
        RESERVED_NAMESPACES.put(Name.NS_EMPTY_PREFIX, Name.NS_DEFAULT_URI);
    }

    private final Set listeners = new HashSet();

    private final HashMap prefixToURI = new HashMap();
    private final HashMap uriToPrefix = new HashMap();

    private final boolean level2Repository;

    /**
     * Returns the NamespaceCache for the given RepositoryService.
     *
     * @param service the repository service.
     * @return the NamespaceCache for the repository service.
     * @throws RepositoryException if an error occurs while reading from the
     *                             service.
     */
    public static NamespaceCache getInstance(RepositoryService service)
            throws RepositoryException {
        synchronized (INSTANCES) {
            NamespaceCache cache = (NamespaceCache) INSTANCES.get(service);
            if (cache == null) {
                cache = new NamespaceCache(service);
                INSTANCES.put(service, cache);
            }
            return cache;
        }
    }

    /**
     * Create a new <code>NamespaceCache</code>.
     *
     * @param service the repository service
     * @throws RepositoryException if reading from the service fails.
     */
    private NamespaceCache(RepositoryService service) throws RepositoryException {
        Map descriptors = service.getRepositoryDescriptors();
        this.level2Repository = Boolean.valueOf((String) descriptors.get(Repository.LEVEL_2_SUPPORTED)).booleanValue();
        // prefill with reserved namespaces
        prefixToURI.putAll(RESERVED_NAMESPACES);
        for (Iterator it = RESERVED_NAMESPACES.keySet().iterator(); it.hasNext(); ) {
            String prefix = (String) it.next();
            uriToPrefix.put(prefixToURI.get(prefix), prefix);
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
        prefixToURI.remove(prefix);
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

    /**
     * Syncs the cached namespace mappings with the given namespaces map.
     *
     * @param namespaces the up-to-date namespace mapping.
     */
    private void syncNamespaces(Map namespaces) {
        prefixToURI.clear();
        prefixToURI.putAll(namespaces);
        uriToPrefix.clear();
        for (Iterator it = namespaces.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            uriToPrefix.put(entry.getValue(), entry.getKey());
        }
    }

    /**
     * Registers a new name space.
     *
     * @param storage the underlying namespace storage.
     * @param prefix  the namespace prefix.
     * @param uri     the namespace URI.
     * @throws NamespaceException    if an illegal attempt is made to register a
     *                               mapping.
     * @throws UnsupportedRepositoryOperationException
     *                               in a level 1 implementation
     * @throws AccessDeniedException if the session associated with the
     *                               <code>Workspace</code> object through which
     *                               this registry was acquired does not have
     *                               sufficient permissions to register the
     *                               namespace.
     * @throws RepositoryException   if another error occurs.
     */
    public synchronized void registerNamespace(NamespaceStorage storage, String prefix, String uri)
            throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        if (!level2Repository) {
            throw new UnsupportedRepositoryOperationException("Repository is Level1 only.");
        }
        // perform basic validation checks
        if (prefix == null || uri == null) {
            throw new IllegalArgumentException("prefix/uri can not be null");
        }
        if (Name.NS_EMPTY_PREFIX.equals(prefix) || Name.NS_DEFAULT_URI.equals(uri)) {
            throw new NamespaceException("default namespace is reserved and can not be changed");
        }
        if (RESERVED_URIS.contains(uri)) {
            throw new NamespaceException("failed to register namespace "
                + prefix + " -> " + uri + ": reserved URI");
        }
        if (RESERVED_PREFIXES.contains(prefix)) {
            throw new NamespaceException("failed to register namespace "
                + prefix + " -> " + uri + ": reserved prefix");
        }
        // special case: prefixes xml*
        if (prefix.toLowerCase().startsWith(Name.NS_XML_PREFIX)) {
            throw new NamespaceException("failed to register namespace "
                + prefix + " -> " + uri + ": reserved prefix");
        }
        // check if the prefix is a valid XML prefix
        if (!XMLChar.isValidNCName(prefix)) {
            throw new NamespaceException("failed to register namespace "
                + prefix + " -> " + uri + ": invalid prefix");
        }

        // check existing mappings
        String oldPrefix = null;
        try {
            oldPrefix = getPrefix(storage, uri);
        } catch (NamespaceException e) {
            // does not exist
        }
        if (prefix.equals(oldPrefix)) {
            throw new NamespaceException("failed to register namespace "
                + prefix + " -> " + uri + ": mapping already exists");
        }
        try {
            getURI(storage, prefix);
            /**
             * prevent remapping of existing prefixes because this would in effect
             * remove the previously assigned namespace;
             * as we can't guarantee that there are no references to this namespace
             * (in names of nodes/properties/node types etc.) we simply don't allow it.
             */
            throw new NamespaceException("failed to register namespace "
                + prefix + " -> " + uri
                + ": remapping existing prefixes is not supported.");
        } catch (NamespaceException e) {
            // ok
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
     * Unregisters a namespace.
     *
     * @param storage the namespace storage.
     * @param prefix  the prefix for the namespace to remove.
     * @throws NamespaceException    if an illegal attempt is made to remove a
     *                               mapping.
     * @throws UnsupportedRepositoryOperationException
     *                               in a level 1 implementation
     * @throws AccessDeniedException if the session associated with the
     *                               <code>Workspace</code> object through which
     *                               this registry was acquired does not have
     *                               sufficient permissions to unregister the
     *                               namespace.
     * @throws RepositoryException   if another error occurs.
     */
    public synchronized void unregisterNamespace(NamespaceStorage storage, String prefix)
            throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        if (!level2Repository) {
            throw new UnsupportedRepositoryOperationException("Repository is Level1 only.");
        }

        if (RESERVED_PREFIXES.contains(prefix)) {
            throw new NamespaceException("reserved prefix: " + prefix);
        }

        String uri = getURI(storage, prefix);
        // inform storage before mappings are added to maps and propagated to listeners
        storage.unregisterNamespace(uri);

        // update caches and notify listeners
        removeMapping(prefix, uri);
    }

    /**
     * Returns an array holding all currently registered prefixes.
     *
     * @param storage the namespace storage
     * @return a string array
     * @throws RepositoryException if an error occurs.
     */
    public synchronized String[] getPrefixes(NamespaceStorage storage)
            throws RepositoryException {
        Map namespaces = storage.getRegisteredNamespaces();
        syncNamespaces(namespaces);
        Set prefixes = namespaces.keySet();
        return (String[]) prefixes.toArray(new String[prefixes.size()]);
    }

    /**
     * Returns an array holding all currently registered URIs.
     *
     * @param storage the namespace storage
     * @return a string array
     * @throws RepositoryException if an error occurs.
     */
    public synchronized String[] getURIs(NamespaceStorage storage)
            throws RepositoryException {
        Map namespaces = storage.getRegisteredNamespaces();
        syncNamespaces(namespaces);
        Collection uris = namespaces.values();
        return (String[]) uris.toArray(new String[uris.size()]);
    }

    /**
     * Returns the URI to which the given prefix is mapped.
     *
     * @param storage the namespace storage.
     * @param prefix  a string
     * @return the namespace URI for the <code>prefix</code>.
     * @throws NamespaceException  if the prefix is unknown.
     * @throws RepositoryException is another error occurs
     */
    public synchronized String getURI(NamespaceStorage storage, String prefix)
            throws NamespaceException, RepositoryException {
        String uri = (String) prefixToURI.get(prefix);
        if (uri == null) {
            // try to load the uri
            uri = storage.getURI(prefix);
            prefixToURI.put(prefix, uri);
        }
        return uri;
    }

    /**
     * Returns the prefix to which the given URI is mapped
     *
     * @param storage the namespace storage.
     * @param uri     a string
     * @return the namespace prefix for the <code>uri</code>.
     * @throws NamespaceException  if the URI is unknown.
     * @throws RepositoryException is another error occurs
     */
    public synchronized String getPrefix(NamespaceStorage storage, String uri)
            throws NamespaceException, RepositoryException {
        String prefix = (String) uriToPrefix.get(uri);
        if (prefix == null) {
            // try to load the prefix
            prefix = storage.getPrefix(uri);
            uriToPrefix.put(uri, prefix);
        }
        return prefix;
    }

    //--------------------------------------------< NamespaceListener support >

    /**
     * Registers <code>listener</code> to get notifications when namespace
     * mappings change.
     *
     * @param listener the listener to register.
     * @throws UnsupportedOperationException if listener support is not enabled
     *                                       for this <code>AbstractNamespaceResolver</code>.
     */
    public void addListener(NamespaceListener listener) {
        if (listeners == null) {
            throw new UnsupportedOperationException("addListener");
        }
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Removes the <code>listener</code> from this <code>NamespaceRegistery</code>.
     *
     * @param listener the listener to remove.
     * @throws UnsupportedOperationException if listener support is not enabled
     *                                       for this <code>AbstractNamespaceResolver</code>.
     */
    public void removeListener(NamespaceListener listener) {
        if (listeners == null) {
            throw new UnsupportedOperationException("removeListener");
        }
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Notifies the listeners that a new namespace <code>uri</code> has been
     * added and mapped to <code>prefix</code>.
     *
     * @param prefix the prefix.
     * @param uri    the namespace uri.
     */
    protected void notifyNamespaceAdded(String prefix, String uri) {
        if (listeners == null) {
            throw new UnsupportedOperationException("notifyNamespaceAdded");
        }
        // addition is infrequent compared to listener registration
        // -> use copy-on-read
        NamespaceListener[] currentListeners;
        synchronized (listeners) {
            int i = 0;
            currentListeners = new NamespaceListener[listeners.size()];
            for (Iterator it = listeners.iterator(); it.hasNext();) {
                currentListeners[i++] = (NamespaceListener) it.next();
            }
        }
        for (int i = 0; i < currentListeners.length; i++) {
            currentListeners[i].namespaceAdded(prefix, uri);
        }
    }

    /**
     * Notifies listeners that an existing namespace uri has been remapped
     * to a new prefix.
     *
     * @param oldPrefix the old prefix.
     * @param newPrefix the new prefix.
     * @param uri the associated namespace uri.
     */
    protected void notifyNamespaceRemapped(String oldPrefix,
                                           String newPrefix,
                                           String uri) {
        if (listeners == null) {
            throw new UnsupportedOperationException("notifyNamespaceRemapped");
        }
        // remapping is infrequent compared to listener registration
        // -> use copy-on-read
        NamespaceListener[] currentListeners;
        synchronized (listeners) {
            int i = 0;
            currentListeners = new NamespaceListener[listeners.size()];
            for (Iterator it = listeners.iterator(); it.hasNext();) {
                currentListeners[i++] = (NamespaceListener) it.next();
            }
        }
        for (int i = 0; i < currentListeners.length; i++) {
            currentListeners[i].namespaceRemapped(oldPrefix, newPrefix, uri);
        }
    }

    /**
     * Notifies the listeners that the namespace with the given <code>uri</code>
     * has been removed from the mapping.
     *
     * @param uri the namespace uri.
     * @see NamespaceListener#namespaceRemoved(String)
     */
    protected void notifyNamespaceRemoved(String uri) {
        if (listeners == null) {
            throw new UnsupportedOperationException("notifyNamespaceRemapped");
        }
        // removal is infrequent compared to listener registration
        // -> use copy-on-read
        NamespaceListener[] currentListeners;
        synchronized (listeners) {
            int i = 0;
            currentListeners = new NamespaceListener[listeners.size()];
            for (Iterator it = listeners.iterator(); it.hasNext();) {
                currentListeners[i++] = (NamespaceListener) it.next();
            }
        }
        for (int i = 0; i < currentListeners.length; i++) {
            currentListeners[i].namespaceRemoved(uri);
        }
    }
}
