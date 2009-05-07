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

import javax.jcr.NamespaceException;

/**
 * Resolver for JCR paths.
 */
public interface PathResolver {

    /**
     * Returns the path object for the given JCR path string.
     *
     * @param path prefixed JCR path
     * @return a <code>Path</code> object.
     * @throws MalformedPathException if the JCR path format is invalid.
     * @throws IllegalNameException if any of the JCR names contained in the path are invalid.
     * @throws NamespaceException if a namespace prefix can not be resolved.
     */
    Path getQPath(String path) throws MalformedPathException, IllegalNameException, NamespaceException;

    /**
     * Returns the path object for the given JCR path string.
     *
     * @param path prefixed JCR path
     * @param normalizeIdentifier
     * @return a <code>Path</code> object.
     * @throws MalformedPathException if the JCR path format is invalid.
     * @throws IllegalNameException if any of the JCR names contained in the path are invalid.
     * @throws NamespaceException if a namespace prefix can not be resolved.
     */
    Path getQPath(String path, boolean normalizeIdentifier) throws MalformedPathException, IllegalNameException, NamespaceException;

    /**
     * Returns the given JCR path string for the given path object.
     *
     * @param path a <code>Path</code> object.
     * @return a JCR path string
     * @throws NamespaceException if a namespace URI can not be resolved
     */
    String getJCRPath(Path path) throws NamespaceException;

}
