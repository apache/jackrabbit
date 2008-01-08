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
package org.apache.jackrabbit.util.name;

import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.AbstractNamespaceResolver;

import javax.jcr.NamespaceException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * A Simple Namespace Mapping table. Mappings can be added
 * and then the object can be used as a NamespaceResolver. Additionally, it can
 * be based on a underlying NamespaceResolver
 *
 * @deprecated Use the NamespaceMapping class from 
 *             the org.apache.jackrabbit.spi.commons.namespace package of
 *             the jackrabbit-spi-commons component.
 */
public class NamespaceMapping extends AbstractNamespaceResolver {

    /** local uris */
    private final Properties prefixToURI = new Properties();

    /** local prefix */
    private final Properties URIToPrefix = new Properties();

    /** base */
    private final NamespaceResolver base;

    public NamespaceMapping() {
        this.base = null;
    }

    /**
     * Constructor
     */
    public NamespaceMapping(NamespaceResolver base) {
        this.base = base;
    }


    /**
     * {@inheritDoc}
     */
    public String getPrefix(String uri) throws NamespaceException {
        if (URIToPrefix.containsKey(uri)) {
            return URIToPrefix.getProperty(uri);
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
            return prefixToURI.getProperty(prefix);
        } else if (base == null) {
            throw new NamespaceException("No URI for pefix '" + prefix + "' declared.");
        } else {
            return base.getURI(prefix);
        }
    }

    /**
     * Returns true if prefix is already mapped to some URI. Returns false otherwise.
     */
    public boolean hasPrefix(String prefix) {
        return prefixToURI.containsKey(prefix);
    }

    /**
     * Set a prefix == URI one-to-one mapping
     *
     * @param prefix
     * @param uri
     * @throws NamespaceException
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
     * Return a Map of prefix to URI mappings currently registered.
     * The returned Map is a copy of the internal Map.
     * @return Map
     */
    public Map getPrefixToURIMapping() {
        return new HashMap(prefixToURI);
    }

    /**
     * Return a Map of URI to prefix mappings currently registered.
     * The returned Map is a copy of the internal Map.
     * @return Map
     */
    public Map getURIToPrefixMapping() {
        return new HashMap(URIToPrefix);
    }

     /**
     * Override equals()
      *
     * @param obj
     * @return boolean
     */
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
     * Override toString()
     *
     * @return String
     */
    public String toString() {
        String s = "";
        Set mapping = prefixToURI.entrySet();
        for (Iterator i = mapping.iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String prefix = (String) entry.getKey();
            String uri = (String) entry.getValue();
            s += "'" + prefix + "' == '" + uri + "'\n";
        }
        return s;
    }
}
