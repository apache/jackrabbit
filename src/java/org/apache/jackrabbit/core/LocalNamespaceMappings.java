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

import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.IllegalNameException;
import org.apache.jackrabbit.name.UnknownPrefixException;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.CachingNamespaceResolver;
import org.apache.xerces.util.XMLChar;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
class LocalNamespaceMappings extends CachingNamespaceResolver {

    /** The underlying global and persistent namespace registry. */
    private final NamespaceRegistryImpl nsReg;

    /** Prefix to URI mappings of local namespaces. */
    private final HashMap prefixToURI = new HashMap();

    /** URI to prefix mappings of local namespaces. */
    private final HashMap uriToPrefix = new HashMap();

    /** The global namespace prefixes hidden by local namespace mappings. */
    private Set hiddenPrefixes = new HashSet();

    /**
     * Creates a local namespace manager with the given underlying
     * namespace registry.
     *
     * @param nsReg namespace registry
     */
    LocalNamespaceMappings(NamespaceRegistryImpl nsReg) {
        super(nsReg, 100);
        this.nsReg = nsReg;
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
        if (QName.NS_EMPTY_PREFIX.equals(prefix)
                || QName.NS_DEFAULT_URI.equals(uri)) {
            throw new NamespaceException("default namespace is reserved and can not be changed");
        }
        // special case: xml namespace
        if (uri.equals(QName.NS_XML_URI)) {
            throw new NamespaceException("xml namespace is reserved and can not be changed.");
        }
        // special case: prefixes xml*
        if (prefix.toLowerCase().startsWith(QName.NS_XML_PREFIX)) {
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

        // invalidate cache
        super.prefixRemapped(prefix, uri);
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
        String uri = (String) prefixToURI.get(prefix);
        if (uri != null) {
            return uri;
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
        String prefix = (String) uriToPrefix.get(uri);
        if (prefix != null) {
            return prefix;
        }

        // check global mappings
        return nsReg.getPrefix(uri);
    }

    /**
     * {@inheritDoc}
     */
    public QName getQName(String name)
            throws IllegalNameException, UnknownPrefixException {
        if (prefixToURI.isEmpty()) {
            // shortcut
            return nsReg.getQName(name);
        }
        try {
            // first try registry, this might result in a wrong QName because
            // of locally overlayed mappings
            QName candidate = nsReg.getQName(name);
            // check if valid
            String prefix = nsReg.getPrefix(candidate.getNamespaceURI());
            if (!hiddenPrefixes.contains(prefix)) {
                return candidate;
            }
        } catch (UnknownPrefixException e) {
            // try using local mappings
        } catch (NamespaceException e) {
            // may be thrown by nsReg.getPrefix() but should never happend
            // because we got the namespace from the nsReg itself
            throw new UnknownPrefixException(name);
        }
        return super.getQName(name);
    }

    /**
     * {@inheritDoc}
     */
    public String getJCRName(QName name)
            throws NoPrefixDeclaredException {
        if (uriToPrefix.isEmpty()) {
            // shortcut
            return nsReg.getJCRName(name);
        }
        if (uriToPrefix.containsKey(name.getNamespaceURI())) {
            // locally re-mappped
            return super.getJCRName(name);
        } else {
            // use global mapping
            return nsReg.getJCRName(name);
        }
    }

    /**
     * @inheritDoc
     * This method gets called when the NamespaceRegistry remapped a namespace
     * to a new prefix or if a new namespace is registered.
     */
    public void prefixRemapped(String prefix, String uri) {
        // todo check overlayed mappings and adjust prefixes if necessary
        super.prefixRemapped(prefix, uri);
    }
}
