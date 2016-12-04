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
package org.apache.jackrabbit.spi.commons.conversion;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;

import javax.jcr.NamespaceException;

/**
 * Path resolver that parsers and formats prefixed JCR paths.
 * A {@link NameResolver} is used for resolving the path element names.
 */
public class ParsingPathResolver implements PathResolver {

    /**
     * Path factory.
     */
    private final PathFactory pathFactory;

    /**
     * Name resolver.
     */
    private final NameResolver nameResolver;

    /**
     * Identifier resolver.
     */
    private final IdentifierResolver idResolver;

    /**
     * Creates a parsing path resolver.
     *
     * @param pathFactory path factory.
     * @param resolver name resolver
     */
    public ParsingPathResolver(PathFactory pathFactory, NameResolver resolver) {
        this(pathFactory, resolver, null);
    }

    /**
     * Creates a parsing path resolver.
     *
     * @param pathFactory path factory.
     * @param nameResolver name resolver.
     * @param idResolver identifier resolver.
     * @since JCR 2.0
     */
    public ParsingPathResolver(PathFactory pathFactory, NameResolver nameResolver,
                               IdentifierResolver idResolver) {
        this.pathFactory = pathFactory;
        this.nameResolver = nameResolver;
        this.idResolver = idResolver;
    }

    /**
     * Parses the given JCR path into a <code>Path</code> object.
     *
     * @param jcrPath A JCR path String.
     * @return A <code>Path</code> object.
     * @throws MalformedPathException if the JCR path format is invalid.
     * @throws IllegalNameException if any of the JCR names contained in the path are invalid.
     * @throws NamespaceException if a namespace prefix can not be resolved
     */
    public Path getQPath(String jcrPath) throws MalformedPathException, IllegalNameException, NamespaceException {
        return PathParser.parse(jcrPath, nameResolver, idResolver, pathFactory);
    }

    /**
     * Calls {@link PathParser#parse(String, NameResolver, IdentifierResolver, org.apache.jackrabbit.spi.PathFactory)}
     * from the given <code>jcrPath</code>.
     * 
     * @see PathResolver#getQPath(String, boolean)
     */
    public Path getQPath(String jcrPath, boolean normalizeIdentifier) throws MalformedPathException, IllegalNameException, NamespaceException {
        return PathParser.parse(jcrPath, nameResolver, idResolver, pathFactory, normalizeIdentifier);
    }


    /**
     * Returns the JCR path representation for the given <code>Path</code> object.
     *
     * @param path A <code>Path</code> object.
     * @return A JCR path String in the standard form.
     * @throws NamespaceException if a namespace URI can not be resolved.
     * @see PathResolver#getJCRPath(org.apache.jackrabbit.spi.Path) 
     */
    public String getJCRPath(Path path) throws NamespaceException {
        StringBuffer buffer = new StringBuffer();

        Path.Element[] elements = path.getElements();
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) {
                buffer.append('/');
            }
            if (i == 0 && elements.length == 1 && elements[i].denotesRoot()) {
                buffer.append('/');
            } else if (elements[i].denotesCurrent()) {
                buffer.append('.');
            } else if (elements[i].denotesParent()) {
                buffer.append("..");
            } else if (elements[i].denotesIdentifier()) {
                buffer.append(elements[i].getString());
            } else {
                buffer.append(nameResolver.getJCRName(elements[i].getName()));
                /**
                 * FIXME the [1] subscript should only be suppressed if the
                 * item in question can't have same-name siblings.
                 */
                if (elements[i].getIndex() > Path.INDEX_DEFAULT) {
                    buffer.append('[');
                    buffer.append(elements[i].getIndex());
                    buffer.append(']');
                }
            }
        }
        return buffer.toString();
    }
}
