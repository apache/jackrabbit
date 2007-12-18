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

import org.apache.jackrabbit.spi.commons.namespace.AbstractNamespaceResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceListener;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.util.XMLChar;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Manager for local session namespace mappings. This class is
 * used by the {@link SessionImpl SessionImpl} class to implement
 * the local namespace mapping functionality required by the JCR API.
 * <p>
 * This class holds a reference to the underlying global and persistent
 * namespace registry (a {@link NamespaceRegistryImpl NamespaceRegistryImpl}
 * instance) and keeps track of local namespace mappings added by the session.
 * <p>
 * The namespace resolution methods required by the
 * {@link NamespaceResolver NamespaceResolver} are implemented by first
 * looking up the local namespace mapping and then backing to the
 * underlying namespace registry.
 */
class LocalNamespaceMappings extends AbstractNamespaceResolver
        implements NamespaceListener {

    /** The underlying global and persistent namespace registry. */
    private final NamespaceRegistryImpl nsReg;

    /** Prefix to URI mappings of local namespaces. */
    private final HashMap prefixToURI = new HashMap();

    /** URI to prefix mappings of local namespaces. */
    private final HashMap uriToPrefix = new HashMap();

    /**
     * Creates a local namespace manager with the given underlying
     * namespace registry.
     *
     * @param nsReg namespace registry
     */
    LocalNamespaceMappings(NamespaceRegistryImpl nsReg) {
        this.nsReg = nsReg;
        this.nsReg.addListener(this);
    }

    /**
     * Rename a persistently registered namespace URI to the new prefix.
     *
     * @param prefix namespace prefix
     * @param uri namespace URI
     * @throws NamespaceException
     * @throws RepositoryException
     */
    void setNamespacePrefix(String prefix, String uri)
            throws NamespaceException, RepositoryException {
        if (prefix == null || uri == null) {
            throw new IllegalArgumentException("prefix/uri can not be null");
        }
        if (Name.NS_EMPTY_PREFIX.equals(prefix)
                || Name.NS_DEFAULT_URI.equals(uri)) {
            throw new NamespaceException("default namespace is reserved and can not be changed");
        }
        // special case: xml namespace
        if (uri.equals(Name.NS_XML_URI)) {
            throw new NamespaceException("xml namespace is reserved and can not be changed.");
        }
        // special case: prefixes xml*
        if (prefix.toLowerCase().startsWith(Name.NS_XML_PREFIX)) {
            throw new NamespaceException("reserved prefix: " + prefix);
        }
        // check if the prefix is a valid XML prefix
        if (!XMLChar.isValidNCName(prefix)) {
            throw new NamespaceException("invalid prefix: " + prefix);
        }

        // verify that namespace exists (the following call will
        // trigger a NamespaceException if it doesn't)
        nsReg.getPrefix(uri);

        // check new prefix for collision
        if (Arrays.asList(getPrefixes()).contains(prefix)) {
            // prefix is already in use
            if (getURI(prefix).equals(uri)) {
                // redundant mapping, silently ignore
                return;
            }
            throw new NamespaceException("prefix already in use: " + prefix);
        }

        // check if namespace is already locally mapped
        String oldPrefix = (String) uriToPrefix.get(uri);
        if (oldPrefix != null) {
            // remove old mapping
            uriToPrefix.remove(uri);
            prefixToURI.remove(oldPrefix);
        }

        // store new mapping
        prefixToURI.put(prefix, uri);
        uriToPrefix.put(uri, prefix);
    }

    /**
     * Returns all prefixes currently mapped.
     *
     * @return an array holding all currently mapped prefixes
     * @throws RepositoryException if an error occurs
     */
    String[] getPrefixes() throws RepositoryException {
        if (prefixToURI.isEmpty()) {
            // shortcut
            return nsReg.getPrefixes();
        }

        HashSet prefixes = new HashSet();
        String[] uris = nsReg.getURIs();
        for (int i = 0; i < uris.length; i++) {
            // check local mapping
            String prefix = (String) uriToPrefix.get(uris[i]);
            if (prefix == null) {
                // globally mapped
                prefix = nsReg.getPrefix(uris[i]);
            }
            prefixes.add(prefix);
        }

        return (String[]) prefixes.toArray(new String[prefixes.size()]);
    }

    /**
     * Disposes this <code>LocalNamespaceMappings</code>.
     */
    void dispose() {
        nsReg.removeListener(this);
    }

    //-----------------------------------------------------< NamespaceResolver >
    /**
     * {@inheritDoc}
     */
    public String getURI(String prefix) throws NamespaceException {
        if (prefixToURI.isEmpty()) {
            // shortcut
            return nsReg.getURI(prefix);
        }
        // check local mappings
        String uri = (String) prefixToURI.get(prefix);
        if (uri != null) {
            return uri;
        }

        // check global mappings
        uri = nsReg.getURI(prefix);
        if (uri != null) {
            // make sure global prefix is not hidden because of
            // locally remapped uri
            if (!uriToPrefix.containsKey(uri)) {
                return uri;
            }
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
        String prefix = (String) uriToPrefix.get(uri);
        if (prefix != null) {
            return prefix;
        }

        // check global mappings
        return nsReg.getPrefix(uri);
    }

    //-----------------------------------------------------< NamespaceListener >
    /**
     * @inheritDoc
     * This method gets called when a new namespace is registered in
     * the global NamespaceRegistry. Overridden in order to check for/resolve
     * collision of new global prefix with existing local prefix.
     */
    public void namespaceAdded(String prefix, String uri) {
        if (prefixToURI.containsKey(prefix)) {
            // the new global prefix is already in use locally;
            // need to change it locally by appending underscore(s)
            // in order to guarantee unambiguous mappings
            String uniquePrefix = prefix + "_";
            while (prefixToURI.containsKey(uniquePrefix)) {
                uniquePrefix += "_";
            }
            // add new local mapping
            prefixToURI.put(uniquePrefix, uri);
            uriToPrefix.put(uri, uniquePrefix);
        }
    }

    /**
     * @inheritDoc
     * This method gets called when an existing namespace is remapped to a new
     * prefix in the global NamespaceRegistry. Overridden in order to check
     * for/resolve collision of new global prefix with existing local prefix.
     */
    public void namespaceRemapped(String oldPrefix, String newPrefix, String uri) {
        if (prefixToURI.containsKey(newPrefix)) {
            // the new global prefix is already in use locally;
            // check uri
            if (uriToPrefix.containsKey(uri)) {
                // since namespace is already remapped locally to
                // a different prefix there's no collision
                return;
            }
            // need to change enw prefix locally by appending underscore(s)
            // in order to guarantee unambiguous mappings
            String uniquePrefix = newPrefix + "_";
            while (prefixToURI.containsKey(uniquePrefix)) {
                uniquePrefix += "_";
            }
            // add new local mapping
            prefixToURI.put(uniquePrefix, uri);
            uriToPrefix.put(uri, uniquePrefix);
        }
    }

    /**
     * @inheritDoc
     * This method gets called when an existing namespace is removed
     * in the global NamespaceRegistry. Overridden in order to check
     * for/resolve collision of new global prefix with existing local prefix.
     */
    public void namespaceRemoved(String uri) {
        if (uriToPrefix.containsKey(uri)) {
            String prefix = (String)uriToPrefix.remove(uri);
            prefixToURI.remove(prefix);
        }
    }
}
