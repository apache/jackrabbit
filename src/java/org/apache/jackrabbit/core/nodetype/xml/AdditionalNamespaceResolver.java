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

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

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
     * Creates a namespace resolver using the namespaces declared
     * in the given XML element.
     *
     * @param element XML element
     */
    public AdditionalNamespaceResolver(Element element) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            if (Constants.NS_XMLNS_PREFIX.equals(attribute.getPrefix())) {
                addNamespace(attribute.getLocalName(), attribute.getValue());
            }
        }
        addNamespace(Constants.NS_EMPTY_PREFIX, Constants.NS_DEFAULT_URI);
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
