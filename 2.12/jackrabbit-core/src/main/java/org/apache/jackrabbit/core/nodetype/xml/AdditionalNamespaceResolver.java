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
package org.apache.jackrabbit.core.nodetype.xml;

import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * A simple namespace resolver implementation, that uses the additional
 * namespaces declared in an XML element.
 */
public class AdditionalNamespaceResolver implements NamespaceResolver {

    /** Map from namespace prefixes to namespace URIs. */
    private final Properties prefixToURI = new Properties();

    /** Map from namespace URIs to namespace prefixes. */
    private final Properties uriToPrefix = new Properties();

    /**
     * Creates a namespace resolver using the namespaces defined in
     * the given prefix-to-URI property set.
     *
     * @param namespaces namespace properties
     */
    public AdditionalNamespaceResolver(Properties namespaces) {
        Enumeration<?> prefixes = namespaces.propertyNames();
        while (prefixes.hasMoreElements()) {
            String prefix = (String) prefixes.nextElement();
            addNamespace(prefix, namespaces.getProperty(prefix));
        }
        addNamespace("", "");
    }

    /**
     * Creates a namespace resolver using the namespaces declared
     * in the given namespace registry.
     *
     * @param registry namespace registry
     * @throws RepositoryException on repository errors
     */
    public AdditionalNamespaceResolver(NamespaceRegistry registry)
            throws RepositoryException {
        String[] prefixes = registry.getPrefixes();
        for (int i = 0; i < prefixes.length; i++) {
            addNamespace(prefixes[i], registry.getURI(prefixes[i]));
        }
    }

    /**
     * Adds the given namespace declaration to this resolver.
     *
     * @param prefix namespace prefix
     * @param uri namespace URI
     */
    private void addNamespace(String prefix, String uri) {
        prefixToURI.put(prefix, uri);
        uriToPrefix.put(uri, prefix);
    }

    /** {@inheritDoc} */
    public String getURI(String prefix) throws NamespaceException {
        String uri = prefixToURI.getProperty(prefix);
        if (uri != null) {
            return uri;
        } else {
            throw new NamespaceException(
                    "Unknown namespace prefix " + prefix + ".");
        }
    }

    /** {@inheritDoc} */
    public String getPrefix(String uri) throws NamespaceException {
        String prefix = uriToPrefix.getProperty(uri);
        if (prefix != null) {
            return prefix;
        } else {
            throw new NamespaceException(
                    "Unknown namespace URI " + uri + ".");
        }
    }

}
