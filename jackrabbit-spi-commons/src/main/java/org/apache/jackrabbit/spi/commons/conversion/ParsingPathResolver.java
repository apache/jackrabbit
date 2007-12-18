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
    private final NameResolver resolver;

    /**
     * Creates a parsing path resolver.
     *
     * @param resolver name resolver
     */
    public ParsingPathResolver(PathFactory pathFactory, NameResolver resolver) {
        this.pathFactory = pathFactory;
        this.resolver = resolver;
    }

    /**
     * Parses the prefixed JCR path and returns the resolved qualified path.
     *
     * @param path prefixed JCR path
     * @return qualified path
     * @throws MalformedPathException if the JCR path format is invalid.
     * @throws IllegalNameException if any of the JCR names contained in the path are invalid.
     * @throws NamespaceException if a namespace prefix can not be resolved
     */
    public Path getQPath(String path) throws MalformedPathException, IllegalNameException, NamespaceException {
        return PathParser.parse(path, resolver, pathFactory);
    }


    /**
     * Returns the prefixed JCR path for the given qualified path.
     *
     * @param path qualified path
     * @return prefixed JCR path
     * @throws NamespaceException if a namespace URI can not be resolved
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
            } else {
                buffer.append(resolver.getJCRName(elements[i].getName()));
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
