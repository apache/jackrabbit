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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.cluster.NamespaceEventChannel;
import org.apache.jackrabbit.core.cluster.NamespaceEventListener;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.spi.commons.namespace.AbstractNamespaceResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.util.XMLChar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import javax.jcr.AccessDeniedException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * A <code>NamespaceRegistryImpl</code> ...
 */
public class NamespaceRegistryImpl extends AbstractNamespaceResolver
        implements NamespaceRegistry, NamespaceEventListener {

    private static Logger log = LoggerFactory.getLogger(NamespaceRegistryImpl.class);

    private static final String NS_REG_RESOURCE = "ns_reg.properties";
    private static final String NS_IDX_RESOURCE = "ns_idx.properties";

    private static final HashSet reservedPrefixes = new HashSet();
    private static final HashSet reservedURIs = new HashSet();

    static {
        // reserved prefixes
        reservedPrefixes.add(Name.NS_XML_PREFIX);
        reservedPrefixes.add(Name.NS_XMLNS_PREFIX);
        // predefined (e.g. built-in) prefixes
        reservedPrefixes.add(Name.NS_REP_PREFIX);
        reservedPrefixes.add(Name.NS_JCR_PREFIX);
        reservedPrefixes.add(Name.NS_NT_PREFIX);
        reservedPrefixes.add(Name.NS_MIX_PREFIX);
        reservedPrefixes.add(Name.NS_SV_PREFIX);
        // reserved namespace URI's
        reservedURIs.add(Name.NS_XML_URI);
        reservedURIs.add(Name.NS_XMLNS_URI);
        // predefined (e.g. built-in) namespace URI's
        reservedURIs.add(Name.NS_REP_URI);
        reservedURIs.add(Name.NS_JCR_URI);
        reservedURIs.add(Name.NS_NT_URI);
        reservedURIs.add(Name.NS_MIX_URI);
        reservedURIs.add(Name.NS_SV_URI);
    }

    private HashMap prefixToURI = new HashMap();
    private HashMap uriToPrefix = new HashMap();

    private HashMap indexToURI = new HashMap();
    private HashMap uriToIndex = new HashMap();

    private int lastIndex = 0;

    private NameResolver nameResolver;

    private PathResolver pathResolver;

    private final FileSystem nsRegStore;

    /**
     * Namespace event channel.
     */
    private NamespaceEventChannel eventChannel;

    /**
     * Protected constructor: Constructs a new instance of this class.
     *
     * @param nsRegStore
     * @throws RepositoryException
     */
    protected NamespaceRegistryImpl(FileSystem nsRegStore)
            throws RepositoryException {
        super(true); // enable listener support
        this.nsRegStore = nsRegStore;
        load();
    }

    /**
     * Clears all mappings.
     */
    private void clear() {
        prefixToURI.clear();
        uriToPrefix.clear();
        indexToURI.clear();
        uriToIndex.clear();
    }

    /**
     * Adds a new mapping and automatically assigns a new index.
     *
     * @param prefix the namespace prefix
     * @param uri the namespace uri
     */
    private void map(String prefix, String uri) {
        map(prefix, uri, null);
    }

    /**
     * Adds a new mapping and uses the given index if specified.
     *
     * @param prefix the namespace prefix
     * @param uri the namespace uri
     * @param idx the index or <code>null</code>.
     */
    private void map(String prefix, String uri, Integer idx) {
        prefixToURI.put(prefix, uri);
        uriToPrefix.put(uri, prefix);
        if (!uriToIndex.containsKey(uri)) {
            if (idx == null) {
                idx = new Integer(++lastIndex);
            } else {
                if (idx.intValue() > lastIndex) {
                    lastIndex = idx.intValue();
                }
            }
            indexToURI.put(idx, uri);
            uriToIndex.put(uri, idx);
        }
    }

    private void load() throws RepositoryException {
        FileSystemResource propFile =
                new FileSystemResource(nsRegStore, NS_REG_RESOURCE);
        FileSystemResource idxFile =
                new FileSystemResource(nsRegStore, NS_IDX_RESOURCE);
        try {
            if (!propFile.exists()) {
                // clear existing mappings
                clear();

                // default namespace (if no prefix is specified)
                map(Name.NS_EMPTY_PREFIX, Name.NS_DEFAULT_URI);

                // declare the predefined mappings
                // rep:
                map(Name.NS_REP_PREFIX, Name.NS_REP_URI);
                // jcr:
                map(Name.NS_JCR_PREFIX, Name.NS_JCR_URI);
                // nt:
                map(Name.NS_NT_PREFIX, Name.NS_NT_URI);
                // mix:
                map(Name.NS_MIX_PREFIX, Name.NS_MIX_URI);
                // sv:
                map(Name.NS_SV_PREFIX, Name.NS_SV_URI);
                // xml:
                map(Name.NS_XML_PREFIX, Name.NS_XML_URI);

                // persist mappings
                store();
                return;
            }

            // check if index file exists
            Properties indexes = new Properties();
            if (idxFile.exists()) {
                InputStream in = idxFile.getInputStream();
                try {
                    indexes.load(in);
                } finally {
                    in.close();
                }
            }

            InputStream in = propFile.getInputStream();
            try {
                Properties props = new Properties();
                props.load(in);

                // clear existing mappings
                clear();

                // read mappings from properties
                Iterator iter = props.keySet().iterator();
                while (iter.hasNext()) {
                    String prefix = (String) iter.next();
                    String uri = props.getProperty(prefix);
                    String idx = indexes.getProperty(uri);
                    if (idx != null) {
                        map(prefix, uri, Integer.decode(idx));
                    } else {
                        map(prefix, uri);
                    }
                }
            } finally {
                in.close();
            }
            if (!idxFile.exists()) {
                store();
            }
        } catch (Exception e) {
            String msg = "failed to load namespace registry";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    private void store() throws RepositoryException {
        FileSystemResource propFile =
                new FileSystemResource(nsRegStore, NS_REG_RESOURCE);
        try {
            propFile.makeParentDirs();
            OutputStream os = propFile.getOutputStream();
            Properties props = new Properties();

            // store mappings in properties
            Iterator iter = prefixToURI.keySet().iterator();
            while (iter.hasNext()) {
                String prefix = (String) iter.next();
                String uri = (String) prefixToURI.get(prefix);
                props.setProperty(prefix, uri);
            }

            try {
                props.store(os, null);
            } finally {
                // make sure stream is closed
                os.close();
            }
        } catch (Exception e) {
            String msg = "failed to persist namespace registry";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }

        FileSystemResource indexFile =
                new FileSystemResource(nsRegStore, NS_IDX_RESOURCE);
        try {
            indexFile.makeParentDirs();
            OutputStream os = indexFile.getOutputStream();
            Properties props = new Properties();

            // store mappings in properties
            Iterator iter = uriToIndex.keySet().iterator();
            while (iter.hasNext()) {
                String uri = (String) iter.next();
                String index = uriToIndex.get(uri).toString();
                props.setProperty(uri, index);
            }

            try {
                props.store(os, null);
            } finally {
                // make sure stream is closed
                os.close();
            }
        } catch (Exception e) {
            String msg = "failed to persist namespace registry index.";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    public NameResolver getNameResolver() {
        return nameResolver;
    }

    public PathResolver getPathResolver() {
        return pathResolver;
    }

    /**
     * Returns a prefix that is unique among the already registered prefixes.
     *
     * @param uriHint namespace uri that serves as hint for the prefix generation
     * @return a unique prefix
     */
    public String getUniquePrefix(String uriHint) {
        // @todo smarter unique prefix generation
/*
        int number;
        if (uriHint == null || uriHint.length() == 0) {
            number = prefixToURI.size() + 1;
        } else {
            number = uriHint.hashCode();
        }
        return "_pre" + number;
*/
        return "_pre" + (prefixToURI.size() + 1);
    }

    /**
     * Registers a namespace using the given prefix hint. Does nothing
     * if the namespace is already registered. If the given prefix hint
     * is not yet registered as a prefix, then it is used as the prefix
     * of the registered namespace. Otherwise a unique prefix is generated
     * based on the given hint.
     *
     * @param prefixHint the prefix hint
     * @param uri the namespace URI
     * @throws NamespaceException if an illegal attempt is made to register
     *                            a mapping
     * @throws RepositoryException if an unexpected error occurs
     * @see #registerNamespace(String, String)
     */
    public synchronized void safeRegisterNamespace(String prefixHint, String uri)
            throws NamespaceException, RepositoryException {
        try {
            // Check if the namespace is already registered
            getPrefix(uri);
            // ... it is, so do nothing.
        } catch (NamespaceException e1) {
            // ... it is not, try to find a unique prefix.

            // First, check and replace troublesome prefix hints.
            if (prefixHint.toLowerCase().startsWith(Name.NS_XML_PREFIX)
                    || !XMLChar.isValidNCName(prefixHint)) {
                prefixHint = "_pre";
            }

            // Then, find an appropriate prefix based on the hint
            String prefix = prefixHint;
            try {
                for (int suffix = 2; true; suffix++) {
                    // Is this prefix already registered?
                    getURI(prefix);
                    // ... it is, generate a new prefix and try again.
                    prefix = prefixHint + suffix;
                }
            } catch (NamespaceException e2) {
                // ... it is not, register the namespace with this prefix.
                registerNamespace(prefix, uri);
            }
        }
    }

    /**
     * Set an event channel to inform about changes.
     *
     * @param eventChannel event channel
     */
    public void setEventChannel(NamespaceEventChannel eventChannel) {
        this.eventChannel = eventChannel;
        eventChannel.setListener(this);
    }

    /**
     * Returns the index (i.e. stable prefix) for the given uri.
     *
     * @param uri the uri to retrieve the index for
     * @return the index
     * @throws NamespaceException if the URI is not registered.
     */
    public int getURIIndex(String uri) throws NamespaceException {
        Integer idx = (Integer) uriToIndex.get(uri);
        if (idx == null) {
            throw new NamespaceException("URI " + uri + " is not registered.");
        }
        return idx.intValue();
    }

    /**
     * Returns the URI for a given index (i.e. stable prefix).
     *
     * @param idx the index to retrieve the uri for.
     * @return the uri
     * @throws NamespaceException if the URI is not registered.
     */
    public String getURI(int idx) throws NamespaceException {
        String uri = (String) indexToURI.get(new Integer(idx));
        if (uri == null) {
            throw new NamespaceException("URI for index " + idx +  " not registered.");
        }
        return uri;
    }

    //----------------------------------------------------< NamespaceRegistry >
    /**
     * {@inheritDoc}
     */
    public synchronized void registerNamespace(String prefix, String uri)
            throws NamespaceException, UnsupportedRepositoryOperationException,
            AccessDeniedException, RepositoryException {
        if (prefix == null || uri == null) {
            throw new IllegalArgumentException("prefix/uri can not be null");
        }
        if (Name.NS_EMPTY_PREFIX.equals(prefix) || Name.NS_DEFAULT_URI.equals(uri)) {
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

        if (oldPrefix != null) {
            // remove old prefix mapping
            prefixToURI.remove(oldPrefix);
            uriToPrefix.remove(uri);
        }

        // add new prefix mapping
        map(prefix, uri);

        if (eventChannel != null) {
            eventChannel.remapped(oldPrefix, prefix, uri);
        }

        // persist mappings
        store();

        // notify listeners
        if (oldPrefix != null) {
            // remapped existing namespace uri to new prefix
            notifyNamespaceRemapped(oldPrefix, prefix, uri);
        } else {
            // added new namespace uri mapped to prefix
            notifyNamespaceAdded(prefix, uri);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterNamespace(String prefix)
            throws NamespaceException, UnsupportedRepositoryOperationException,
            AccessDeniedException, RepositoryException {
        if (reservedPrefixes.contains(prefix)) {
            throw new NamespaceException("reserved prefix: " + prefix);
        }
        if (!prefixToURI.containsKey(prefix)) {
            throw new NamespaceException("unknown prefix: " + prefix);
        }
        /**
         * as we can't guarantee that there are no references to the specified
         * namespace (in names of nodes/properties/node types etc.) we simply
         * don't allow it.
         */
        throw new NamespaceException("unregistering namespaces is not supported.");
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPrefixes() throws RepositoryException {
        return (String[]) prefixToURI.keySet().toArray(new String[prefixToURI.keySet().size()]);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getURIs() throws RepositoryException {
        return (String[]) uriToPrefix.keySet().toArray(new String[uriToPrefix.keySet().size()]);
    }

    //--------------------------------< NamespaceRegistry & NamespaceResolver >
    /**
     * {@inheritDoc}
     */
    public String getURI(String prefix) throws NamespaceException {
        String uri = (String) prefixToURI.get(prefix);
        if (uri == null) {
            throw new NamespaceException(prefix
                    + ": is not a registered namespace prefix.");
        }
        return uri;
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefix(String uri) throws NamespaceException {
        String prefix = (String) uriToPrefix.get(uri);
        if (prefix == null) {
            throw new NamespaceException(uri
                    + ": is not a registered namespace uri.");
        }
        return prefix;
    }

    //-----------------------------------------------< NamespaceEventListener >

    /**
     * {@inheritDoc}
     */
    public void externalRemap(String oldPrefix, String newPrefix, String uri)
            throws RepositoryException {

        if (newPrefix == null) {
            /**
             * as we can't guarantee that there are no references to the specified
             * namespace (in names of nodes/properties/node types etc.) we simply
             * don't allow it.
             */
            throw new NamespaceException("unregistering namespaces is not supported.");
        }

        if (oldPrefix != null) {
            // remove old prefix mapping
            prefixToURI.remove(oldPrefix);
            uriToPrefix.remove(uri);
        }

        // add new prefix mapping
        map(newPrefix, uri);

        // persist mappings
        store();

        // notify listeners
        if (oldPrefix != null) {
            // remapped existing namespace uri to new prefix
            notifyNamespaceRemapped(oldPrefix, newPrefix, uri);
        } else {
            // added new namespace uri mapped to prefix
            notifyNamespaceAdded(newPrefix, uri);
        }

    }
}
