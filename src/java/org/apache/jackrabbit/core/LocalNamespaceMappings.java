/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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

import org.apache.xml.utils.XMLChar;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>LocalNamespaceMappings</code> ...
 */
class LocalNamespaceMappings implements NamespaceResolver {

    // the global persistent namespace registry
    private final NamespaceRegistryImpl nsReg;

    // local prefix/namespace mappings
    private final HashMap prefixToURI = new HashMap();
    private final HashMap uriToPrefix = new HashMap();

    // prefixes in global namespace registry hidden by local mappings
    private Set hiddenPrefixes = new HashSet();

    /**
     * Constructor
     *
     * @param nsReg
     */
    LocalNamespaceMappings(NamespaceRegistryImpl nsReg) {
        this.nsReg = nsReg;
    }

    /**
     * Rename a persistently registered namespace URI to the new prefix.
     *
     * @param prefix
     * @param uri
     * @throws NamespaceException
     * @throws RepositoryException
     */
    void setNamespacePrefix(String prefix, String uri)
            throws NamespaceException, RepositoryException {
        if (prefix == null || uri == null) {
            throw new IllegalArgumentException("prefix/uri can not be null");
        }
        if (Constants.NS_EMPTY_PREFIX.equals(prefix)
                || Constants.NS_DEFAULT_URI.equals(uri)) {
            throw new NamespaceException("default namespace is reserved and can not be changed");
        }
        // special case: prefixes xml*
        if (prefix.toLowerCase().startsWith(Constants.NS_XML_PREFIX)) {
            throw new NamespaceException("reserved prefix: " + prefix);
        }
        // check if the prefix is a valid XML prefix
        if (!XMLChar.isValidNCName(prefix)) {
            throw new NamespaceException("invalid prefix: " + prefix);
        }

        // check if namespace exists (the following call will
        // trigger a NamespaceException if it doesn't)
        String globalPrefix = nsReg.getPrefix(uri);

        // check new prefix for collision
        String globalURI = null;
        try {
            globalURI = nsReg.getURI(prefix);
        } catch (NamespaceException nse) {
            // ignore
        }
        if (globalURI != null) {
            // prefix is already mapped in global namespace registry;
            // check if it is redundant or if it refers to a namespace
            // that has been locally remapped, thus hiding it
            if (!hiddenPrefixes.contains(prefix)) {
                if (uri.equals(globalURI) && prefix.equals(globalPrefix)) {
                    // redundant mapping, silently ignore
                    return;
                }
                // we don't allow to hide a namespace because we can't
                // guarantee that there are no references to it
                // (in names of nodes/properties/node types etc.)
                throw new NamespaceException(prefix + ": prefix is already mapped to the namespace: " + globalURI);
            }
        }

        // check if namespace is already locally mapped
        String oldPrefix = (String) uriToPrefix.get(uri);
        if (oldPrefix != null) {
            if (oldPrefix.equals(prefix)) {
                // redundant mapping, silently ignore
                return;
            }
            // resurrect hidden global prefix
            hiddenPrefixes.remove(nsReg.getPrefix(uri));
            // remove old mapping
            uriToPrefix.remove(uri);
            prefixToURI.remove(oldPrefix);
        }

        // check if prefix is already locally mapped
        String oldURI = (String) prefixToURI.get(prefix);
        if (oldURI != null) {
            // resurrect hidden global prefix
            hiddenPrefixes.remove(nsReg.getPrefix(oldURI));
            // remove old mapping
            uriToPrefix.remove(oldURI);
            prefixToURI.remove(prefix);
        }

        if (!prefix.equals(globalPrefix)) {
            // store new mapping
            prefixToURI.put(prefix, uri);
            uriToPrefix.put(uri, prefix);
            hiddenPrefixes.add(globalPrefix);
        }
    }

    /**
     * Returns all prefixes.
     *
     * @return
     * @throws RepositoryException
     */
    String[] getPrefixes() throws RepositoryException {
        if (prefixToURI.isEmpty()) {
            // shortcut
            return nsReg.getPrefixes();
        }

        HashSet prefixes = new HashSet();
        // global prefixes
        String[] globalPrefixes = nsReg.getPrefixes();
        for (int i = 0; i < globalPrefixes.length; i++) {
            if (!hiddenPrefixes.contains(globalPrefixes[i])) {
                prefixes.add(globalPrefixes[i]);
            }
        }
        // local prefixes
        prefixes.addAll(prefixToURI.keySet());

        return (String[]) prefixes.toArray(new String[prefixes.size()]);
    }

    //----------------------------------------------------< NamespaceResolver >
    /**
     * {@inheritDoc}
     */
    public String getURI(String prefix) throws NamespaceException {
        if (prefixToURI.isEmpty()) {
            // shortcut
            return nsReg.getURI(prefix);
        }
        // check local mappings
        if (prefixToURI.containsKey(prefix)) {
            return (String) prefixToURI.get(prefix);
        }

        // check global mappings
        if (!hiddenPrefixes.contains(prefix)) {
            return nsReg.getURI(prefix);
        }

        throw new NamespaceException(prefix + ": unknown prefix");
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefix(String uri) throws NamespaceException {
        if (prefixToURI.isEmpty()) {
            // shortcut
            return nsReg.getPrefix(uri);
        }

        // check local mappings
        if (uriToPrefix.containsKey(uri)) {
            return (String) uriToPrefix.get(uri);
        }

        // check global mappings
        return nsReg.getPrefix(uri);
    }
}
