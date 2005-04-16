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
package org.apache.jackrabbit.base;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * General base class for implementing the JCR NamespaceRegistry interface.
 */
public class BaseNamespaceRegistry implements NamespaceRegistry {

    /**
     * The default constructor is protected to signify that this
     * class needs to be subclassed to be of any real use.
     */
    protected BaseNamespaceRegistry() {
    }

    /** Unsupported repository operation. {@inheritDoc} */
    public void registerNamespace(String prefix, String uri)
            throws NamespaceException, UnsupportedRepositoryOperationException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Unsupported repository operation. {@inheritDoc} */
    public void unregisterNamespace(String prefix) throws NamespaceException,
            UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Unsupported repository operation. {@inheritDoc} */
    public String[] getPrefixes() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Implemented by mapping all prefixes returned by
     * <code>getPrefixes()</code> into namespace URIs using
     * <code>getURI(prefix)</code>.
     * {@inheritDoc}
     */
    public String[] getURIs() throws RepositoryException {
        String[] prefixes = getPrefixes();
        String[] uris = new String[prefixes.length];
        for (int i = 0; i < prefixes.length; i++) {
            uris[i] = getURI(prefixes[i]);
        }
        return uris;
    }

    /** Unsupported repository operation. {@inheritDoc} */
    public String getURI(String prefix) throws NamespaceException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Implemented by iterating over the prefixes returned by
     * <code>getPrefixes()</code> and selecting the prefix for which
     * <code>getURI(prefix)</code> returns the given namespace URI.
     * {@inheritDoc}
     */
    public String getPrefix(String uri) throws NamespaceException,
            RepositoryException {
        String[] prefixes = getPrefixes();
        for (int i = 0; i < prefixes.length; i++) {
            if (uri.equals(getURI(prefixes[i]))) {
                return prefixes[i];
            }
        }
        throw new NamespaceException(
                "Namespace URI " + uri + " is not registered.");
    }

}
