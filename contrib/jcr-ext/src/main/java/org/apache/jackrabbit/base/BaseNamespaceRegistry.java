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
package org.apache.jackrabbit.base;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * NamespaceRegistry base class. The dummy namespace registry implemented
 * by this class only contains the builtin namespaces defined by the JSR 170
 * specification. Subclasses should override the methods of this class to
 * include other namespaces or to support namespace management.
 */
public class BaseNamespaceRegistry implements NamespaceRegistry {

    /** The default namespace prefixes. */
    private static final String[] NAMESPACE_PREFIXES =
        new String[] { "jcr", "nt", "mix", "xml", "" };

    /** The default namespace URIs. */
    private static final String[] NAMESPACE_URIS = new String[] {
            "http://www.jcp.org/jcr/1.0",
            "http://www.jcp.org/jcr/nt/1.0",
            "http://www.jcp.org/jcr/mix/1.0",
            "http://www.w3.org/XML/1998/namespace",
            ""
        };

    /**
     * Unsupported operation. Subclasses should override this method to
     * allow namespace management.
     *
     * @param prefix namespace prefix
     * @param uri namespace URI
     * @see NamespaceRegistry#registerNamespace(String, String)
     */
    public void registerNamespace(String prefix, String uri)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Unsupported operation. Subclasses should override this method to
     * allow namespace management.
     *
     * @param prefix namespace prefix
     * @see NamespaceRegistry#unregisterNamespace(String, String)
     */
    public void unregisterNamespace(String prefix) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Returns the builtin namespace prefixes defined by the JSR 170
     * specification. Subclasses should override this method to return
     * the actual registered namespace prefixes.
     *
     * @return builtin namespace prefixes
     * @see NamespaceRegistry#getPrefixes()
     */
    public String[] getPrefixes() throws RepositoryException {
        return (String[]) NAMESPACE_PREFIXES.clone();
    }

    /**
     * Returns the builtin namespace URIs defined by the JSR 170
     * specification. Subclasses should override this method to return
     * the actual registered namespace URIs.
     *
     * @return builtin namespace prefixes
     * @see NamespaceRegistry#getURIs()
     */
    public String[] getURIs() throws RepositoryException {
        return (String[]) NAMESPACE_URIS.clone();
    }

    /**
     * Returns the builtin namespace URI that is mapped to the given
     * builtin namespace prefix as defined by the JSR 170 specification.
     * Subclasses should override this method to support the actual
     * registered namespaces.
     *
     * @param prefix namespace prefix
     * @return namespace URI
     * @throws NamespaceException if the namespace prefix was not found
     * @see NamespaceRegistry#getURI(String)
     */
    public String getURI(String prefix) throws RepositoryException {
        for (int i = 0; i < NAMESPACE_PREFIXES.length; i++) {
            if (NAMESPACE_PREFIXES[i].equals(prefix)) {
                return NAMESPACE_URIS[i];
            }
        }
        throw new NamespaceException("Prefix " + prefix + " not found");
    }

    /**
     * Returns the builtin namespace prefix that is mapped to the given
     * builtin namespace URI as defined by the JSR 170 specification.
     * Subclasses should override this method to support the actual
     * registered namespaces.
     *
     * @param uri namespace URI
     * @return namespace prefix
     * @throws NamespaceException if the namespace URI was not found
     * @see NamespaceRegistry#getPrefix(String)
     */
    public String getPrefix(String uri) throws RepositoryException {
        for (int i = 0; i < NAMESPACE_URIS.length; i++) {
            if (NAMESPACE_URIS[i].equals(uri)) {
                return NAMESPACE_PREFIXES[i];
            }
        }
        throw new NamespaceException("URI " + uri + " not found");
    }

 }
