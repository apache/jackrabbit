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

import java.util.HashMap;
import java.util.Map;

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
     * Creates a namespace helper for the given session.
     *
     * @param session current session
     */
    public NamespaceHelper(Session session) {
        this.session = session;
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
        Map<String, String> namespaces = new HashMap<String, String>();
        String[] prefixes = session.getNamespacePrefixes();
        for (String prefixe : prefixes) {
            namespaces.put(prefixe, session.getNamespaceURI(prefixe));
        }
        return namespaces;
    }

    /**
     * Returns the prefix mapped to the given namespace URI in the current
     * session, or <code>null</code> if the namespace does not exist.
     *
     * @see Session#getNamespacePrefix(String)
     * @param uri namespace URI
     * @return namespace prefix, or <code>null</code>
     * @throws RepositoryException if the namespace could not be retrieved
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
     * session, or <code>null</code> if the namespace does not exist.
     *
     * @see Session#getNamespaceURI(String)
     * @param prefix namespace prefix
     * @return namespace prefix, or <code>null</code>
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
        if (uri != null && uri.length() > 0) {
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
     * returned. Otherwise the namespace is registered to the namespace
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
        NamespaceRegistry registry =
            session.getWorkspace().getNamespaceRegistry();
        try {
            // Check if the namespace is registered
            registry.getPrefix(uri);
        } catch (NamespaceException e1) {
             // Replace troublesome prefix hints
            if (prefix == null || prefix.length() == 0
                    || prefix.toLowerCase().startsWith("xml")
                    || !XMLChar.isValidNCName(prefix)) {
                prefix = "ns"; // ns, ns2, ns3, ns4, ...
            }

            // Loop until an unused prefix is found
            try {
                String base = prefix;
                for (int i = 2; true; i++) {
                    registry.getURI(prefix);
                    prefix = base + i;
                }
            } catch (NamespaceException e2) {
                // Exit the loop
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
        for (Map.Entry<String, String> stringStringEntry : namespaces.entrySet()) {
            Map.Entry<String, String> entry = stringStringEntry;
            registerNamespace(entry.getKey(), entry.getValue());
        }
    }

}
