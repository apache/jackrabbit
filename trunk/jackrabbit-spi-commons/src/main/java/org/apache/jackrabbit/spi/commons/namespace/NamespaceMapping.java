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
package org.apache.jackrabbit.spi.commons.namespace;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.NamespaceException;

/**
 * A Simple Namespace Mapping table. Mappings can be added
 * and then the object can be used as a NamespaceResolver. Additionally, it can
 * be based on a underlying NamespaceResolver
 */
public class NamespaceMapping implements NamespaceResolver {

    /** local uris */
    private final Map<String, String> prefixToURI = new HashMap<String, String>();

    /** local prefix */
    private final Map<String, String> URIToPrefix = new HashMap<String, String>();

    /** base */
    private final NamespaceResolver base;

    public NamespaceMapping() {
        this.base = null;
    }

    /**
     * Constructor
     * @param base fallback resolver
     */
    public NamespaceMapping(NamespaceResolver base) {
        this.base = base;
    }


    //--------------------------------------------------< NamespaceResolver >---
    /**
     * {@inheritDoc}
     */
    public String getPrefix(String uri) throws NamespaceException {
        if (URIToPrefix.containsKey(uri)) {
            return URIToPrefix.get(uri);
        } else if (base == null) {
            throw new NamespaceException("No prefix for URI '" + uri + "' declared.");
        } else {
            return base.getPrefix(uri);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getURI(String prefix) throws NamespaceException {
        if (prefixToURI.containsKey(prefix)) {
            return prefixToURI.get(prefix);
        } else if (base == null) {
            throw new NamespaceException("No URI for prefix '" + prefix + "' declared.");
        } else {
            return base.getURI(prefix);
        }
    }

    //-------------------------------------------------------------< public >---
    /**
     * Returns true if prefix is already mapped to some URI. Returns false otherwise.
     * @param prefix prefix to check
     * @return <code>true</code> if prefix is mapped
     */
    public boolean hasPrefix(String prefix) {
        return prefixToURI.containsKey(prefix);
    }

    /**
     * Set a prefix == URI one-to-one mapping
     *
     * @param prefix prefix to map
     * @param uri uri to map
     * @throws NamespaceException if an error occurs
     */
    public void setMapping(String prefix, String uri) throws NamespaceException {
        if (prefix == null) {
            throw new NamespaceException("Prefix must not be null");
        }
        if (uri == null) {
            throw new NamespaceException("URI must not be null");
        }
        if (URIToPrefix.containsKey(uri)) {
            // remove mapping
            prefixToURI.remove(URIToPrefix.remove(uri));
        }
        if (prefixToURI.containsKey(prefix)) {
            // remove mapping
            URIToPrefix.remove(prefixToURI.remove(prefix));
        }
        prefixToURI.put(prefix, uri);
        URIToPrefix.put(uri, prefix);
    }

    /**
     * Clear the mapping for an URI
     *
     * @param uri  URI to clear the mapping for
     * @return  The prefix the URI was mapped to or <code>null</code> if it was not mapped.
     */
    public String removeMapping(String uri) {
        String prefix = URIToPrefix.remove(uri);
        if (prefix != null) {
            prefixToURI.remove(prefix);
        }

        return prefix;
    }

    /**
     * Return a Map of prefix to URI mappings currently registered.
     * The returned Map is a copy of the internal Map.
     * @return Map
     */
    public Map<String, String> getPrefixToURIMapping() {
        return new HashMap<String, String>(prefixToURI);
    }

    /**
     * Return a Map of URI to prefix mappings currently registered.
     * The returned Map is a copy of the internal Map.
     * @return Map
     */
    public Map<String, String> getURIToPrefixMapping() {
        return new HashMap<String, String>(URIToPrefix);
    }

    //-------------------------------------------------------------< Object >---
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NamespaceMapping) {
            NamespaceMapping other = (NamespaceMapping) obj;
            return this.getPrefixToURIMapping().equals(other.getPrefixToURIMapping())
                   && this.getURIToPrefixMapping().equals(other.getURIToPrefixMapping());
        }
        return false;
    }

    /**
     * Override {@link Object#toString()}
     *
     * @return String
     */
    @Override
    public String toString() {
        String s = "";
        for (Map.Entry<String, String> entry: prefixToURI.entrySet()) {
            String prefix = entry.getKey();
            String uri = entry.getValue();
            s += "'" + prefix + "' == '" + uri + "'\n";
        }
        return s;
    }
}
