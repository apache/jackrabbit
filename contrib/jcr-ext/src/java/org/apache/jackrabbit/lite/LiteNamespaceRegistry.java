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
package org.apache.jackrabbit.lite;

import java.util.Properties;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.base.BaseNamespaceRegistry;

/**
 * TODO
 */
public class LiteNamespaceRegistry extends BaseNamespaceRegistry {

    /** Mapping from registered namespace prefixes to URIs. */
    private final Properties prefixToURI;

    /** Mapping from registered namespace URIs to prefixes. */
    private final Properties uriToPrefix;

    public LiteNamespaceRegistry() {
        this.prefixToURI = new Properties();
        this.uriToPrefix = new Properties();

        setNamespace("jcr", "http://www.jcp.org/jcr/1.0");
        setNamespace("nt", "http://www.jcp.org/jcr/nt/1.0");
        setNamespace("mix", "http://www.jcp.org/jcr/mix/1.0");
        setNamespace("sv", "http://www.jcp.org/jcr/sv/1.0");
        setNamespace("", "");
    }

    protected void setNamespace(String prefix, String uri) {
        prefixToURI.setProperty(prefix, uri);
        uriToPrefix.setProperty(uri, prefix);
    }

    /**
     * Returns the namespace prefix associated with the given URI.
     *
     * @param uri namespace URI
     * @return namespace prefix
     * @throws NamespaceException if the namespace is not registered
     * @throws RepositoryException on repository errors
     */
    public String getPrefix(String uri) throws NamespaceException,
            RepositoryException {
        String prefix = uriToPrefix.getProperty(uri);
        if (prefix != null) {
            return prefix;
        } else {
            throw new NamespaceException(
                    "Namespace URI " + uri + " is not registered");
        }
    }

    /**
     * Returns the namespace URI associated with the given prefix.
     *
     * @param prefix namespace prefix
     * @return namespace URI
     * @throws NamespaceException if the namespace is not registered
     * @throws RepositoryException on repository errors
     */
    public String getURI(String prefix) throws NamespaceException,
            RepositoryException {
        String uri = prefixToURI.getProperty(prefix);
        if (uri != null) {
            return uri;
        } else {
            throw new NamespaceException(
                    "Namespace prefix " + prefix + " is not registered");
        }
    }

    /**
     * Returns the registered namespace prefixes.
     *
     * @return namespace prefixes
     * @throws RepositoryException on repository errors
     */
    public String[] getPrefixes() throws RepositoryException {
        return (String[]) prefixToURI.keySet().toArray(new String[0]);
    }

    /**
     * Returns the registered namespace URIs.
     *
     * @return namespace URIs
     * @throws RepositoryException on repository errors
     */
    public String[] getURIs() throws RepositoryException {
        return (String[]) uriToPrefix.keySet().toArray(new String[0]);
    }

}
