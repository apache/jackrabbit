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
package org.apache.jackrabbit.session;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.xerces.util.XMLChar;

public final class SessionNamespaceRegistry implements NamespaceRegistry {

    /** The underlying global namespace registry. */
    private final NamespaceRegistry registry;

    /** Local prefix to namespace URI mappings. */
    private final Properties prefixToURI;

    /** Local namespace URI to prefix mappings. */
    private final Properties uriToPrefix;

    /** The global prefixes hidden by local mappings. */
    private final Set hiddenPrefixes;

    /**
     * Creates a local namespace registry based on the given global
     * namespace registry. The local namespace mappings are initially empty.
     *
     * @param registry global namespace registry
     */
    public SessionNamespaceRegistry(NamespaceRegistry registry) {
        this.registry = registry;
        this.prefixToURI = new Properties();
        this.uriToPrefix = new Properties();
        this.hiddenPrefixes = new HashSet();
    }

    /**
     * Creates a local namespace mapping. See the JCR specification
     * for the details of this rather complex operation.
     * <p>
     * This method implements the specified semantics of the
     * Session.setNamespacePrefix method. Session implementations can use
     * this method as follows:
     * <pre>
     *     NamespaceRegistry registry = new SessionNamespaceRegistry(
     *             getWorkspace().getNamespaceRegistry());
     *
     *     public void setNamespacePrefix(String prefix, String uri)
     *             throws NamespaceException, RepositoryException {
     *         return registry.registerNamespace(prefix, uri);
     *     }
     * </pre>
     *
     * @param prefix namespace prefix
     * @param uri    namespace URI
     * @throws NamespaceException  if the given namespace mapping is invalid
     * @throws RepositoryException on repository errors
     * @see NamespaceRegistry#registerNamespace(String, String)
     * @see javax.jcr.Session#setNamespacePrefix(String, String)
     */
    public void registerNamespace(String prefix, String uri)
            throws NamespaceException, RepositoryException {
        if (prefix.length() == 0) {
            throw new NamespaceException("The empty prefix is reserved");
        } else if (uri.length() == 0) {
            throw new NamespaceException("The empty namespace URI is reserved");
        } else if (prefix.toLowerCase().startsWith("xml")) {
            throw new NamespaceException("The xml* prefixes are reserved");
        } else if (!XMLChar.isValidNCName(prefix)) {
            throw new NamespaceException("Invalid prefix format");
        }

        // Note: throws a NamespaceException if the URI is not registered
        String oldPrefix = getPrefix(uri);

        String oldURI;
        try {
            oldURI = getURI(prefix);
        } catch (NamespaceException e) {
            oldURI = null;
        }
        if (oldURI == null) {
            hiddenPrefixes.add(oldPrefix);
            prefixToURI.remove(oldPrefix);
            prefixToURI.setProperty(prefix, uri);
            uriToPrefix.setProperty(uri, prefix);
        } else if (!uri.equals(oldURI)) {
            throw new NamespaceException(
                    "Cannot hide an existing namespace mapping");
        }
    }

    /**
     * Not implemented. It is not possible to unregister namespaces from
     * a session, you need to access the global namespace registry directly.
     *
     * @param prefix namespace prefix
     * @throws UnsupportedRepositoryOperationException always thrown
     */
    public void unregisterNamespace(String prefix)
            throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Returns the currently mapped namespace prefixes. The returned set
     * contains all locally mapped prefixes and those global prefixes that
     * have not been hidden by local mappings.
     * <p>
     * This method implements the specified semantics of the
     * Session.getNamespacePrefixes method. Session implementations can use
     * this method as follows:
     * <pre>
     *     NamespaceRegistry registry = new SessionNamespaceRegistry(
     *             getWorkspace().getNamespaceRegistry());
     *
     *     public String getNamespacePrefixes() throws RepositoryException {
     *         return registry.getPrefixes();
     *     }
     * </pre>
     *
     * @return namespace prefixes
     * @throws RepositoryException on repository errors
     * @see NamespaceRegistry#getPrefixes()
     * @see javax.jcr.Session#getNamespacePrefixes()
     */
    public String[] getPrefixes() throws RepositoryException {
        HashSet prefixes = new HashSet();
        prefixes.addAll(Arrays.asList(registry.getPrefixes()));
        prefixes.removeAll(hiddenPrefixes);
        prefixes.addAll(prefixToURI.keySet());
        return (String[]) prefixes.toArray(new String[prefixes.size()]);
    }

    /**
     * Returns the registered namespace URIs. This method call is simply
     * forwarded to the underlying global namespace registry as it is not
     * possible to locally add new namespace URIs.
     *
     * @return namespace URIs
     * @throws RepositoryException on repository errors
     * @see NamespaceRegistry#getURIs()
     */
    public String[] getURIs() throws RepositoryException {
        return registry.getURIs();
    }

    /**
     * Returns the namespace URI that is mapped to the given prefix.
     * Returns the local namespace mapping if the prefix is locally
     * mapped, otherwise falls back to the underlying global namespace
     * registry unless the prefix has been hidden by local namespace
     * mappings.
     * <p>
     * This method implements the specified semantics of the
     * Session.getNamespaceURI method. Session implementations can use
     * this method as follows:
     * <pre>
     *     NamespaceRegistry registry = new SessionNamespaceRegistry(
     *             getWorkspace().getNamespaceRegistry());
     *
     *     public String getNamespaceURI(String prefix)
     *             throws NamespaceException, RepositoryException {
     *         return registry.getURI(prefix);
     *     }
     * </pre>
     *
     * @param prefix namespace prefix
     * @return namespace URI
     * @throws NamespaceException  if the prefix is not registered or
     *                             currently visible
     * @throws RepositoryException on repository errors
     * @see NamespaceRegistry#getURI(String)
     * @see javax.jcr.Session#getNamespaceURI(String)
     */
    public String getURI(String prefix)
            throws NamespaceException, RepositoryException {
        String uri = prefixToURI.getProperty(prefix);
        if (uri != null) {
            return uri;
        } else if (!hiddenPrefixes.contains(prefix)) {
            return registry.getURI(prefix);
        } else {
            throw new NamespaceException(
                    "Namespace mapping not found for prefix " + prefix);
        }
    }

    /**
     * Returns the prefix that is mapped to the given namespace URI.
     * Returns the local prefix if the namespace URI is locally mapped,
     * otherwise falls back to the underlying global namespace registry.
     * <p>
     * This method implements the specified semantics of the
     * Session.getNamespacePrefix method. Session implementations can use
     * this method as follows:
     * <pre>
     *     NamespaceRegistry registry = new SessionNamespaceRegistry(
     *             getWorkspace().getNamespaceRegistry());
     *
     *     public String getNamespacePrefix(String uri)
     *             throws NamespaceException, RepositoryException {
     *         return registry.getPrefix(uri);
     *     }
     * </pre>
     *
     * @param uri namespace URI
     * @return namespace prefix
     * @throws NamespaceException  if the namespace URI is not registered
     * @throws RepositoryException on repository errors
     * @see NamespaceRegistry#getPrefix(String)
     * @see javax.jcr.Session#getNamespacePrefix(String)
     */
    public String getPrefix(String uri)
            throws NamespaceException, RepositoryException {
        String prefix = uriToPrefix.getProperty(uri);
        if (prefix != null) {
            return prefix;
        } else {
            return registry.getPrefix(uri);
        }
    }

}
