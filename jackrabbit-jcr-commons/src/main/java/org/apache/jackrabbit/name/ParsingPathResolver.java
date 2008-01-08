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
package org.apache.jackrabbit.name;

import javax.jcr.NamespaceException;

/**
 * Path resolver that parsers and formats prefixed JCR paths.
 * A {@link NameResolver} is used for resolving the path element names.
 *
 * @deprecated Use the ParsingPathResolver class from 
 *             the org.apache.jackrabbit.spi.commons.conversion package of
 *             the jackrabbit-spi-commons component.
 */
public class ParsingPathResolver implements PathResolver {

    /**
     * Name resolver.
     */
    private final NameResolver resolver;

    /**
     * Creates a parsing path resolver.
     *
     * @param resolver name resolver
     */
    public ParsingPathResolver(NameResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Parses and returns a single path element.
     *
     * @param path JCR path
     * @param begin begin index of the path element
     * @param end end index of the path element
     * @return path element
     * @throws NameException if the path element format is invalid
     * @throws NamespaceException if the namespace prefix can not be resolved
     */
    private Path.PathElement getElement(String path, int begin, int end)
            throws NameException, NamespaceException {
        if (begin == end) {
            throw new MalformedPathException("Empty path element: " + path);
        } else if (begin + 1 == end && path.charAt(begin) == '.') {
            return Path.CURRENT_ELEMENT;
        } else if (begin + 2 == end && path.charAt(begin) == '.'
                   && path.charAt(begin + 1) == '.') {
            return Path.PARENT_ELEMENT;
        } else if (begin + 3 < end && path.charAt(end - 1) == ']') {
            int index = 0;
            int factor = 1;
            end -= 2;
            char ch = path.charAt(end);
            while (begin < end && '0' <= ch && ch <= '9') {
                index += factor * (ch - '0');
                factor *= 10;
                ch = path.charAt(--end);
            }
            if (index == 0 || ch != '[') {
                throw new MalformedPathException("Invalid path index: " + path);
            }
            QName qname = resolver.getQName(path.substring(begin, end));
            return Path.PathElement.create(qname, index);
        } else {
            QName qname = resolver.getQName(path.substring(begin, end));
            return Path.PathElement.create(qname);
        }
    }

    /**
     * Parses the prefixed JCR path and returns the resolved qualified path.
     *
     * @param path prefixed JCR path
     * @return qualified path
     * @throws NameException if the JCR path format is invalid
     * @throws NamespaceException if a namespace prefix can not be resolved
     */
    public Path getQPath(String path) throws NameException, NamespaceException {
        Path.PathBuilder builder = new Path.PathBuilder();
        int length = path.length();
        int position = 0;
        int slash = path.indexOf('/');

        if (slash == 0) {
            builder.addRoot();
            if (length == 1) {
                return builder.getPath();
            }
            position = 1;
            slash = path.indexOf('/', 1);
        }
        while (slash != -1) {
            builder.addLast(getElement(path, position, slash));
            position = slash + 1;
            slash = path.indexOf('/', position);
        }
        builder.addLast(getElement(path, position, length));

        return builder.getPath();
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

        Path.PathElement[] elements = path.getElements();
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
