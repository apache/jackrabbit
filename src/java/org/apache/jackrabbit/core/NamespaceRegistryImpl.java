/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.log4j.Logger;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

/**
 * A <code>NamespaceRegistryImpl</code> ...
 */
public class NamespaceRegistryImpl implements NamespaceRegistry, NamespaceResolver {

    private static Logger log = Logger.getLogger(NamespaceRegistryImpl.class);

    // default namespace (empty uri)
    public static final String NS_EMPTY_PREFIX = "";
    public static final String NS_DEFAULT_URI = "";

    // reserved namespace for items defined within built-in node types
    public static final String NS_JCR_PREFIX = "jcr";
    public static final String NS_JCR_URI = "http://www.jcp.org/jcr/1.0";

    // reserved namespace for built-in primary node types
    public static final String NS_NT_PREFIX = "nt";
    public static final String NS_NT_URI = "http://www.jcp.org/jcr/nt/1.0";

    // reserved namespace for built-in mixin node types
    public static final String NS_MIX_PREFIX = "mix";
    public static final String NS_MIX_URI = "http://www.jcp.org/jcr/mix/1.0";

    // reserved namespace for the names of property types
    public static final String NS_PT_PREFIX = "pt";
    public static final String NS_PT_URI = "http://www.jcp.org/jcr/pt/1.0";

    // reserved namespace used in the system view XML serialization format
    public static final String NS_SV_PREFIX = "sv";
    public static final String NS_SV_URI = "http://www.jcp.org/jcr/sv/1.0";

    private static final String NS_REG_RESOURCE = "ns_reg.properties";

    // reserved namespaces that must not be used or redefined
    public static final String NS_XML_PREFIX = "xml";
    public static final String NS_XML_URI = "http://www.w3.org/XML/1998/namespace";
    public static final String NS_XMLNS_PREFIX = "xmlns";
    public static final String NS_XMLNS_URI = "http://www.w3.org/2000/xmlns/";

    private static final HashSet reservedPrefixes = new HashSet();
    private static final HashSet reservedURIs = new HashSet();

    static {
        // reserved prefixes
        reservedPrefixes.add(NS_XML_PREFIX);
        reservedPrefixes.add(NS_XMLNS_PREFIX);
        // predefined (e.g. built-in) prefixes
        reservedPrefixes.add(NS_JCR_PREFIX);
        reservedPrefixes.add(NS_NT_PREFIX);
        reservedPrefixes.add(NS_MIX_PREFIX);
        reservedPrefixes.add(NS_PT_PREFIX);
        reservedPrefixes.add(NS_SV_PREFIX);
        // reserved namespace URI's
        reservedURIs.add(NS_XML_URI);
        reservedURIs.add(NS_XMLNS_URI);
        // predefined (e.g. built-in) namespace URI's
        reservedURIs.add(NS_JCR_URI);
        reservedURIs.add(NS_NT_URI);
        reservedURIs.add(NS_MIX_URI);
        reservedURIs.add(NS_PT_URI);
        reservedURIs.add(NS_SV_URI);
    }

    private HashMap prefixToURI = new HashMap();
    private HashMap uriToPrefix = new HashMap();

    private final FileSystem nsRegStore;

    /**
     * Package private constructor: Constructs a new instance of this class.
     *
     * @param nsRegStore
     * @throws RepositoryException
     */
    NamespaceRegistryImpl(FileSystem nsRegStore) throws RepositoryException {
        this.nsRegStore = nsRegStore;
        load();
    }

    private void load() throws RepositoryException {
        FileSystemResource propFile = new FileSystemResource(nsRegStore, NS_REG_RESOURCE);
        try {
            if (!propFile.exists()) {
                // clear existing mappings
                prefixToURI.clear();
                uriToPrefix.clear();

                // default namespace (if no prefix is specified)
                prefixToURI.put(NS_EMPTY_PREFIX, NS_DEFAULT_URI);
                uriToPrefix.put(NS_DEFAULT_URI, NS_EMPTY_PREFIX);
                // declare the predefined mappings
                // jcr:
                prefixToURI.put(NS_JCR_PREFIX, NS_JCR_URI);
                uriToPrefix.put(NS_JCR_URI, NS_JCR_PREFIX);
                // nt:
                prefixToURI.put(NS_NT_PREFIX, NS_NT_URI);
                uriToPrefix.put(NS_NT_URI, NS_NT_PREFIX);
                // mix:
                prefixToURI.put(NS_MIX_PREFIX, NS_MIX_URI);
                uriToPrefix.put(NS_MIX_URI, NS_MIX_PREFIX);
                // pt:
                prefixToURI.put(NS_PT_PREFIX, NS_PT_URI);
                uriToPrefix.put(NS_PT_URI, NS_PT_PREFIX);
                // sv:
                prefixToURI.put(NS_SV_PREFIX, NS_SV_URI);
                uriToPrefix.put(NS_SV_URI, NS_SV_PREFIX);

                // persist mappings
                store();
                return;
            }

            InputStream in = propFile.getInputStream();
            try {
                Properties props = new Properties();
                props.load(in);

                // clear existing mappings
                prefixToURI.clear();
                uriToPrefix.clear();

                // read mappings from properties
                Iterator iter = props.keySet().iterator();
                while (iter.hasNext()) {
                    String prefix = (String) iter.next();
                    String uri = props.getProperty(prefix);

                    prefixToURI.put(prefix, uri);
                    uriToPrefix.put(uri, prefix);
                }
            } finally {
                in.close();
            }
        } catch (Exception e) {
            String msg = "failed to load namespace registry";
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    private void store() throws RepositoryException {
        FileSystemResource propFile = new FileSystemResource(nsRegStore, NS_REG_RESOURCE);
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
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * Returns a prefix that is unique among the already registered prefixes.
     * @param uriHint namespace uri that serves as hint for the prefix generation
     * @return a unique prefix
     */
    public String getUniquePrefix(String uriHint) {
        // @todo smarter prefix generation
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

    //----------------------------------------------------< NamespaceRegistry >
    /**
     * @see NamespaceRegistry#registerNamespace
     */
    public void registerNamespace(String prefix, String uri)
            throws NamespaceException, RepositoryException {
        if (prefix == null || uri == null) {
            throw new IllegalArgumentException("prefix/uri can not be null");
        }
        if (NS_EMPTY_PREFIX.equals(prefix) || NS_DEFAULT_URI.equals(uri)) {
            throw new NamespaceException("default namespace is reserved and can not be changed");
        }
        if (reservedURIs.contains(uri)) {
            throw new NamespaceException("failed to register namespace " + prefix + " -> " + uri + ": reserved URI");
        }
        if (reservedPrefixes.contains(prefix)) {
            throw new NamespaceException("failed to register namespace " + prefix + " -> " + uri + ": reserved prefix");
        }

        String oldPrefix = (String) uriToPrefix.get(uri);
        if (oldPrefix != null) {
            // existing namespace
            if (oldPrefix.equals(prefix)) {
                throw new NamespaceException("failed to register namespace " + prefix + " -> " + uri + ": mapping already exists");
            }
            // remove old prefix
            prefixToURI.remove(oldPrefix);
            uriToPrefix.remove(uri);
        }

        if (prefixToURI.containsKey(prefix)) {
            // prevent remapping of existing prefixes because this would in effect
            // remove the previously assigned namespace;
            // as we can't guarantee that there are no references to this namespace
            // (in names of nodes/properties/node types etc.) we simply don't allow it.
            throw new NamespaceException("failed to register namespace " + prefix + " -> " + uri + ": remapping existing prefixes is not supported.");
        }

        prefixToURI.put(prefix, uri);
        uriToPrefix.put(uri, prefix);

        // persist mappings
        store();
    }

    /**
     * @see NamespaceRegistry#unregisterNamespace
     */
    public void unregisterNamespace(String prefix)
            throws NamespaceException, RepositoryException {
        if (reservedPrefixes.contains(prefix)) {
            throw new NamespaceException("reserved prefix: " + prefix);
        }
        if (!prefixToURI.containsKey(prefix)) {
            throw new NamespaceException("unknown prefix: " + prefix);
        }
        // as we can't guarantee that there are no references to the specified
        // namespace (in names of nodes/properties/node types etc.) we simply
        // don't allow it.
        throw new NamespaceException("unregistering namespaces is not supported.");
    }

    /**
     * @see NamespaceRegistry#getPrefixes
     */
    public String[] getPrefixes() {
        return (String[]) prefixToURI.keySet().toArray(new String[prefixToURI.keySet().size()]);
    }

    /**
     * @see NamespaceRegistry#getURIs
     */
    public String[] getURIs() {
        return (String[]) uriToPrefix.keySet().toArray(new String[uriToPrefix.keySet().size()]);
    }

    //--------------------------------< NamespaceRegistry & NamespaceResolver >
    /**
     * @see NamespaceRegistry#getURI
     * @see NamespaceResolver#getURI
     */
    public String getURI(String prefix) throws NamespaceException {
        if (!prefixToURI.containsKey(prefix)) {
            throw new NamespaceException(prefix + ": is not a registered namespace prefix.");
        }
        return (String) prefixToURI.get(prefix);
    }

    /**
     * @see NamespaceRegistry#getPrefix
     * @see NamespaceResolver#getPrefix
     */
    public String getPrefix(String uri) throws NamespaceException {
        if (!uriToPrefix.containsKey(uri)) {
            throw new NamespaceException(uri + ": is not a registered namespace uri.");
        }
        return (String) uriToPrefix.get(uri);
    }
}
