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
package org.apache.jackrabbit.core.xml;

import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

import javax.jcr.NamespaceException;
import java.util.HashMap;
import java.util.Map;

/**
 * Hierarchically scoped namespace resolver. Each NamespaceContext instance
 * contains an immutable set of namespace mappings and a reference (possibly
 * <code>null</code>) to a parent NamespaceContext. Namespace resolution is
 * performed by first looking at the local namespace mappings and then using
 * the parent resolver if no local match is found.
 * <p>
 * The local namespace mappings are stored internally as two hash maps, one
 * that maps the namespace prefixes to namespace URIs and another that contains
 * the reverse mapping.
 */
class NamespaceContext implements NamespaceResolver {

    /**
     * The parent namespace context.
     */
    private final NamespaceContext parent;

    /**
     * The namespace prefix to namespace URI mapping.
     */
    private final Map<String, String> prefixToURI;

    /**
     * The namespace URI to namespace prefix mapping.
     */
    private final Map<String, String> uriToPrefix;

    /**
     * Creates a NamespaceContext instance with the given parent context
     * and local namespace mappings.
     *
     * @param parent parent context
     * @param mappings local namespace mappings (prefix -> URI)
     */
    public NamespaceContext(NamespaceContext parent, Map<String, String> mappings) {
        this.parent = parent;
        this.prefixToURI = new HashMap<String, String>();
        this.uriToPrefix = new HashMap<String, String>();

        for (Map.Entry<String, String> mapping : mappings.entrySet()) {
            prefixToURI.put(mapping.getKey(), mapping.getValue());
            uriToPrefix.put(mapping.getValue(), mapping.getKey());
        }
    }

    /**
     * Returns the parent namespace context.
     *
     * @return parent namespace context
     */
    public NamespaceContext getParent() {
        return parent;

    }
    //------------------------------------------------< NamespaceResolver >

    /**
     * Returns the namespace URI mapped to the given prefix.
     *
     * @param prefix namespace prefix
     * @return namespace URI
     * @throws NamespaceException if the prefix is not mapped
     */
    public String getURI(String prefix) throws NamespaceException {
        NamespaceContext current = this;
        while (current != null) {
            String uri = (String) current.prefixToURI.get(prefix);
            if (uri != null) {
                return uri;
            }
            current = current.parent;
        }
        throw new NamespaceException("Unknown prefix: " + prefix);
    }

    /**
     * Returns the namespace prefix mapped to the given URI.
     *
     * @param uri namespace URI
     * @return namespace prefix
     * @throws NamespaceException if the URI is not mapped
     */
    public String getPrefix(String uri) throws NamespaceException {
        NamespaceContext current = this;
        while (current != null) {
            String prefix = (String) current.uriToPrefix.get(uri);
            if (prefix != null) {
                return prefix;
            }
            current = current.parent;
        }
        throw new NamespaceException("Unknown URI: " + uri);
    }
}
