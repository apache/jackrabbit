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
package org.apache.jackrabbit.core.nodetype.xml;

import java.util.Properties;

import javax.jcr.NamespaceRegistry;

import org.apache.jackrabbit.Constants;

/**
 * Simple utility implementation of the NamespaceRegistry interface.
 * Used by the node type formatter test cases.
 */
class SimpleNamespaceRegistry implements NamespaceRegistry {

    /** Map from namespace prefixes to namespace URIs. */
    private final Properties prefixToURI = new Properties();

    /** Map from namespace URIs to namespace prefixes. */
    private final Properties uriToPrefix = new Properties();

    /**
     * Creates a simple namespace registry.
     */
    public SimpleNamespaceRegistry() {
        registerNamespace(Constants.NS_JCR_PREFIX, Constants.NS_JCR_URI);
        registerNamespace(Constants.NS_MIX_PREFIX, Constants.NS_MIX_URI);
        registerNamespace(Constants.NS_NT_PREFIX, Constants.NS_NT_URI);
        registerNamespace(Constants.NS_REP_PREFIX, Constants.NS_REP_URI);
        registerNamespace(Constants.NS_EMPTY_PREFIX, Constants.NS_EMPTY_PREFIX);
    }

    /** {@inheritDoc} */
    public void registerNamespace(String prefix, String uri) {
        prefixToURI.put(prefix, uri);
        uriToPrefix.put(uri, prefix);
    }

    /** {@inheritDoc} */
    public void unregisterNamespace(String prefix) {
        if (prefixToURI.contains(prefix)) {
            uriToPrefix.remove(prefixToURI.get(prefix));
            prefixToURI.remove(prefix);
        }
    }

    /** {@inheritDoc} */
    public String[] getPrefixes() {
        return (String[]) prefixToURI.keySet().toArray(new String[0]);
    }

    /** {@inheritDoc} */
    public String[] getURIs() {
        return (String[]) uriToPrefix.keySet().toArray(new String[0]);
    }

    /** {@inheritDoc} */
    public String getURI(String prefix) {
        return prefixToURI.getProperty(prefix);
    }

    /** {@inheritDoc} */
    public String getPrefix(String uri) {
        return uriToPrefix.getProperty(uri);
    }

}
