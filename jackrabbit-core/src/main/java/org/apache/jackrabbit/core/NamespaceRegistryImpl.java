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
import org.apache.jackrabbit.core.fs.BasedFileSystem;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.util.StringIndex;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.util.XMLChar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import javax.jcr.AccessDeniedException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * A <code>NamespaceRegistryImpl</code> ...
 */
public class NamespaceRegistryImpl implements
        NamespaceRegistry, NamespaceEventListener, StringIndex {

    private static Logger log = LoggerFactory.getLogger(NamespaceRegistryImpl.class);

    /**
     * Special property key string to be used instead of an empty key to
     * avoid problems with Java implementations that have problems with
     * empty keys in property files. The selected value ({@value}) would be
     * invalid as either a namespace prefix or a URI, so there's little fear
     * of accidental collisions.
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-888">JCR-888</a>
     */
    private static final String EMPTY_KEY = ".empty.key";

    private static final String NS_REG_RESOURCE = "ns_reg.properties";
    private static final String NS_IDX_RESOURCE = "ns_idx.properties";

    private static final HashSet<String> reservedPrefixes = new HashSet<String>();
    private static final HashSet<String> reservedURIs = new HashSet<String>();

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

    private HashMap<String, String> prefixToURI = new HashMap<String, String>();
    private HashMap<String, String> uriToPrefix = new HashMap<String, String>();

    private HashMap<Integer, String> indexToURI = new HashMap<Integer, String>();
    private HashMap<String, Integer> uriToIndex = new HashMap<String, Integer>();

    private final FileSystem nsRegStore;

    /**
     * Namespace event channel.
     */
    private NamespaceEventChannel eventChannel;

    /**
     * Protected constructor: Constructs a new instance of this class.
     *
     * @param fs repository file system
     * @throws RepositoryException
     */
    public NamespaceRegistryImpl(FileSystem fs) throws RepositoryException {
        this.nsRegStore = new BasedFileSystem(fs, "/namespaces");
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
                // Need to use only 24 bits, since that's what
                // the BundleBinding class stores in bundles
                idx = uri.hashCode() & 0x00ffffff;
                while (indexToURI.containsKey(idx)) {
                    idx = (idx + 1) & 0x00ffffff;
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
                for (Object p : props.keySet()) {
                    String prefix = (String) p;
                    String uri = props.getProperty(prefix);
                    String idx = indexes.getProperty(escapePropertyKey(uri));
                    // JCR-888: Backwards compatibility check
                    if (idx == null && uri.equals("")) {
                        idx = indexes.getProperty(uri);
                    }
                    if (idx != null) {
                        map(unescapePropertyKey(prefix), uri, Integer.decode(idx));
                    } else {
                        map(unescapePropertyKey(prefix), uri);
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
            for (String prefix : prefixToURI.keySet()) {
                String uri = prefixToURI.get(prefix);
                props.setProperty(escapePropertyKey(prefix), uri);
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
            for (String uri : uriToIndex.keySet()) {
                String index = uriToIndex.get(uri).toString();
                props.setProperty(escapePropertyKey(uri), index);
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

    /**
     * Replaces an empty string with the special {@link #EMPTY_KEY} value.
     *
     * @see #unescapePropertyKey(String)
     * @param key property key
     * @return escaped property key
     */
    private String escapePropertyKey(String key) {
        if (key.equals("")) {
            return EMPTY_KEY;
        } else {
            return key;
        }
    }

    /**
     * Converts the special {@link #EMPTY_KEY} value back to an empty string.
     *
     * @see #escapePropertyKey(String)
     * @param key property key
     * @return escaped property key
     */
    private String unescapePropertyKey(String key) {
        if (key.equals(EMPTY_KEY)) {
            return "";
        } else {
            return key;
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
     * Returns <code>true</code> if the specified uri is one of the reserved
     * URIs defined in this registry.
     *
     * @param uri The URI to test.
     * @return <code>true</code> if the specified uri is reserved;
     * <code>false</code> otherwise.
     */
    public boolean isReservedURI(String uri) {
        return reservedURIs.contains(uri);
    }
    
    //-------------------------------------------------------< StringIndex >--

    /**
     * Returns the index (i.e. stable prefix) for the given namespace URI.
     *
     * @param uri namespace URI
     * @return namespace index
     * @throws IllegalArgumentException if the namespace is not registered
     */
    public int stringToIndex(String uri) {
        Integer idx = uriToIndex.get(uri);
        if (idx == null) {
            throw new IllegalArgumentException("Namespace not registered: " + uri);
        }
        return idx;
    }

    /**
     * Returns the namespace URI for a given index (i.e. stable prefix).
     *
     * @param idx namespace index
     * @return namespace URI
     * @throws IllegalArgumentException if the given index is invalid
     */
    public String indexToString(int idx) {
        String uri = indexToURI.get(idx);
        if (uri == null) {
            throw new IllegalArgumentException("Invalid namespace index: " + idx);
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
        String oldPrefix = uriToPrefix.get(uri);
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
        return prefixToURI.keySet().toArray(new String[prefixToURI.keySet().size()]);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getURIs() throws RepositoryException {
        return uriToPrefix.keySet().toArray(new String[uriToPrefix.keySet().size()]);
    }

    /**
     * {@inheritDoc}
     */
    public String getURI(String prefix) throws NamespaceException {
        String uri = prefixToURI.get(prefix);
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
        String prefix = uriToPrefix.get(uri);
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
    }

}
