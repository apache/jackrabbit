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
package org.apache.jackrabbit.commons;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.util.XMLChar;

/**
 * Helper class for working with JCR namespaces.
 *
 * @since Jackrabbit JCR Commons 1.5
 */
public class NamespaceHelper {

    /**
     * The <code>jcr</code> namespace URI.
     */
    public static final String JCR = "http://www.jcp.org/jcr/1.0";

    /**
     * The <code>nt</code> namespace URI.
     */
    public static final String NT = "http://www.jcp.org/jcr/nt/1.0";

    /**
     * The <code>mix</code> namespace URI.
     */
    public static final String MIX = "http://www.jcp.org/jcr/mix/1.0";

    /**
     * Current session.
     */
    private final Session session;

    /**
     * Current namespace registry.
     */
    private NamespaceRegistry namespaceRegistry;

    /**
     * Creates a namespace helper for the given session.
     *
     * @param session current session
     */
    public NamespaceHelper(Session session) {
        this.session = session;
        // will be set on lazily
        this.namespaceRegistry = null;
    }

    /**
        Get the namespace registry; needs to be done on-demand because the constructor
        does not allow RepositoryException
     */
    private NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        if (namespaceRegistry == null) {
            namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
        }
        return namespaceRegistry;
    }

    /**
     * Returns a map containing all prefix to namespace URI mappings of
     * the current session. The returned map is newly allocated and can
     * can be freely modified by the caller.
     *
     * @see Session#getNamespacePrefixes()
     * @return namespace mappings
     * @throws RepositoryException if the namespaces could not be retrieved
     */
    public Map<String, String> getNamespaces() throws RepositoryException {
        Map<String, String> namespaces = new HashMap<>();
        String[] prefixes = session.getNamespacePrefixes();
        for (String prefix : prefixes) {
            namespaces.put(prefix, session.getNamespaceURI(prefix));
        }
        return namespaces;
    }

    /**
     * Returns the prefix mapped to the given namespace URI in the current
     * session, or {@code null} if the namespace does not exist.
     *
     * @see Session#getNamespacePrefix(String)
     * @param uri namespace URI
     * @return namespace prefix, or {@code null}
     * @throws RepositoryException if the namespace prefix could not be retrieved
     */
    public String getPrefix(String uri) throws RepositoryException {
        try {
            return session.getNamespacePrefix(uri);
        } catch (NamespaceException e) {
            return null;
        }
    }

    /**
     * Returns the namespace URI mapped to the given prefix in the current
     * session, or {@code null} if the namespace does not exist.
     *
     * @see Session#getNamespaceURI(String)
     * @param prefix namespace prefix
     * @return namespace prefix, or {@code null}
     * @throws RepositoryException if the namespace could not be retrieved
     */
    public String getURI(String prefix) throws RepositoryException {
        try {
            return session.getNamespaceURI(prefix);
        } catch (NamespaceException e) {
            return null;
        }
    }

    /**
     * Returns the prefixed JCR name for the given namespace URI and local
     * name in the current session.
     *
     * @param uri namespace URI
     * @param name local name
     * @return prefixed JCR name
     * @throws NamespaceException if the namespace does not exist
     * @throws RepositoryException if the namespace could not be retrieved
     */
    public String getJcrName(String uri, String name)
            throws NamespaceException, RepositoryException {
        if (uri != null && !uri.isEmpty()) {
            return session.getNamespacePrefix(uri) + ":" + name;
        } else {
            return name;
        }
    }

    /**
     * Replaces the standard <code>jcr</code>, <code>nt</code>, or
     * <code>mix</code> prefix in the given name with the prefix
     * mapped to that namespace in the current session.
     * <p>
     * The purpose of this method is to make it easier to write
     * namespace-aware code that uses names in the standard JCR namespaces.
     * For example:
     * <pre>
     *     node.getProperty(helper.getName("jcr:data"));
     * </pre>
     * Note that it is simpler to just use the <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded name</a> wherever supported:
     * <pre>
     *     node.getProperty("http://www.jcp.org/jcr/1.0}data");
     * </pre>
     * Also note the predefined constants in {@link org.apache.jackrabbit.JcrConstants}.
     *
     * @param name prefixed name using the standard JCR prefixes
     * @return prefixed name using the current session namespace mappings
     * @throws IllegalArgumentException if the prefix is unknown
     * @throws RepositoryException if the namespace could not be retrieved
     */
    public String getJcrName(String name)
            throws IllegalArgumentException, RepositoryException {
        String standardPrefix;
        String currentPrefix;

        if (name.startsWith("jcr:")) {
            standardPrefix = "jcr";
            currentPrefix = session.getNamespacePrefix(JCR);
        } else if (name.startsWith("nt:")) {
            standardPrefix = "nt";
            currentPrefix = session.getNamespacePrefix(NT);
        } else if (name.startsWith("mix:")) {
            standardPrefix = "mix";
            currentPrefix = session.getNamespacePrefix(MIX);
        } else {
            throw new IllegalArgumentException("Unknown prefix: " + name);
        }

        if (currentPrefix.equals(standardPrefix)) {
            return name;
        } else {
            return currentPrefix + name.substring(standardPrefix.length());
        }
    }

    /**
     * Safely registers the given namespace. If the namespace already exists,
     * then the prefix mapped to the namespace in the current session is
     * returned. Otherwise, the namespace is registered to the namespace
     * registry. If the given prefix is already registered for some other
     * namespace or otherwise invalid, then another prefix is automatically
     * generated. After the namespace has been registered, the prefix mapped
     * to it in the current session is returned.
     *
     * @see NamespaceRegistry#registerNamespace(String, String)
     * @param prefix namespace prefix
     * @param uri namespace URI
     * @return namespace prefix in the current session
     * @throws RepositoryException if the namespace could not be registered
     */
    public String registerNamespace(String prefix, String uri)
            throws RepositoryException {
        NamespaceRegistry registry = getNamespaceRegistry();
        try {
            // Check if the namespace is registered
            registry.getPrefix(uri);
        } catch (NamespaceException e1) {
             // Throw away Troublesome prefix hints
            if (prefix == null || prefix.isEmpty()
                    || prefix.toLowerCase().startsWith("xml")
                    || !XMLChar.isValidNCName(prefix)) {
                prefix = null;
            }

            if (prefix == null) {
                prefix = suggestPrefix(uri, namespace -> {
                    // prefix checker
                    try {
                        return registry.getPrefix(namespace);
                    } catch (RepositoryException e) {
                        return null;
                    }
                });
            }

            // Register the namespace
            registry.registerNamespace(prefix, uri);
        }

        return session.getNamespacePrefix(uri);
    }

    /**
     * Safely registers all namespaces in the given map from
     * prefixes to namespace URIs.
     *
     * @param namespaces namespace mappings
     * @throws RepositoryException if the namespaces could not be registered
     */
    public void registerNamespaces(Map<String,String> namespaces) throws RepositoryException {
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            registerNamespace(entry.getKey(), entry.getValue());
        }
    }

    // non-public supporting code

    // map with 'optimal' prefix mappings; hard-wired should be ok
    private static final Map<String, String> KNOWN_PREFIXES =
            Map.of("http://purl.org/dc/terms/", "dc",
                    "http://ns.adobe.com/exif/1.0/", "exif");


    // suggest an available prefix for the provided namespace, based on a random UUID
    // (last resort)
    private static String makeUpPrefixByUUID(String namespace, Function<String, String> lookupNamespace) {
        String prefix;

        do {
            prefix = "u-" + UUID.randomUUID();
        } while (namespace.equals(lookupNamespace.apply(prefix)));

        return prefix;
    }

    // compute SHA-256, null when NoSuchAlgorithmException
    private static String getSha256(String namespace) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(namespace.getBytes(StandardCharsets.UTF_8));
            return new BigInteger(1, bytes).toString(16);
        } catch (NoSuchAlgorithmException e) {
            // this really, really should not happen
            return null;
        }
    }

    // suggest an available prefix for the provided namespace, based on the sha-256
    // of the namespace name, and a final fallback to UUID based (while considering pre-existing mappings)
    private static String makeUpPrefixTrySha256(String namespace, Function<String, String> lookupNamespace) {

        String sha = getSha256(namespace);
        if (sha != null) {
            for (int i = 7; i <= sha.length(); i++) {
                String prefix = "s-" + sha.substring(0, i);
                if (!namespace.equals(lookupNamespace.apply(prefix))) {
                    // prefix not in use; so we're ok
                    return prefix;
                }
            }
        }

        // fallback to UUID
        return makeUpPrefixByUUID(namespace, lookupNamespace);
    }

    // suggest an available prefix for the provided namespace, based on the characters
    // in the namespace name (while considering pre-existing mappings)
    private static String makeUpPrefix(String namespace, Function<String, String> lookupNamespace) {
        String prefix = namespace.toLowerCase(Locale.ENGLISH);

        // strip scheme when http(s)
        if (prefix.startsWith("http://")) {
            prefix = prefix.substring("http://".length());
        } else if (prefix.startsWith("https://")) {
            prefix = prefix.substring("httpS://".length());
        }

        // strip common host name prefixes
        if (prefix.startsWith("www.")) {
            prefix = prefix.substring("www.".length());
        } else if (prefix.startsWith("ns")) {
            prefix = prefix.substring("ns".length());
        }

        // replace characters not allowed in prefix (here: '\' and :)
        prefix = prefix.replaceAll("[\\/:]+", "-");

        // strip trailing replacement character
        if (prefix.endsWith("-")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }

        String lookedUpNamespace = lookupNamespace.apply(prefix);
        if (lookedUpNamespace == null || lookedUpNamespace.equals(namespace)) {
            return prefix;
        } else {
            return makeUpPrefixTrySha256(namespace, lookupNamespace);
        }
    }

    // suggest an available prefix for the provided namespace (while considering pre-existing mappings)
    private static String suggestPrefix(String namespace, Function<String, String> lookupPrefix) {
        // try hard-wired map
        String known = KNOWN_PREFIXES.get(namespace);

        // lookup using supplied mapper as well
        String lookedUpNamespace = known != null ? lookupPrefix.apply(known) : null;

        // return hardwired prefix if unused or mapper has the prefix mapped to the same namespace
        if (known != null && (lookedUpNamespace == null || namespace.equals(lookedUpNamespace))) {
            return known;
        } else {
            return makeUpPrefix(namespace, lookupPrefix);
        }
    }
}
