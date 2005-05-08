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
package org.apache.jackrabbit.name;

import javax.jcr.Item;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Content path. Instances of this class are used to represent item paths
 * within a JCR content repository.
 * <p>
 * A path instance consists of a sequence of path elements, that are
 * resolved one by one in the specified order to reach the target item from
 * a given context item.
 * <p>
 * Once created, a path instance is immutable.
 *
 * @see PathElement
 * @see PathParser
 * @see PathBuilder
 */
public final class Path {

    /** Path elements */
    private final PathElement[] elements;

    /**
     * Creates a path instance that contains the given path elements.
     *
     * @param elements path elements
     */
    Path(PathElement[] elements) {
        this.elements = elements;
    }

    /**
     * Resolves this path starting from the given context item. Returns
     * the result of the path resolution.
     *
     * @param item the context item from which to resolve this path
     * @return the resolved target item
     * @throws PathNotFoundException if the path can not be resolved
     * @throws RepositoryException   if another error occurs
     */
    public Item resolve(Item item)
            throws PathNotFoundException, RepositoryException {
        for (int i = 0; i < elements.length; i++) {
            item = elements[i].resolve(item);
        }
        return item;
    }

    /**
     * Parses the given JCR path string. Namespace prefixes within the path
     * are resolved using the current session.
     *
     * @param session current session
     * @param path    JCR path
     * @return path instance
     * @throws IllegalArgumentException if the given path is invalid
     * @throws RepositoryException      if another error occurs
     * @see PathParser
     */
    public static Path parse(Session session, String path)
            throws IllegalArgumentException, RepositoryException {
        return new PathParser(session).parsePath(path);
    }

    /**
     * Resolves the given JCR path from the given context item. Returns
     * the result of the path resolution. Namespace prefixes within the path
     * are resolved using the session associated with the context item.
     *
     * @param item context item
     * @param path JCR path
     * @return target item
     * @throws IllegalArgumentException if the given path is invalid
     * @throws PathNotFoundException    if the given path can not be resolved
     * @throws RepositoryException      if another error occurs
     */
    public static Item resolve(Item item, String path)
            throws IllegalArgumentException, PathNotFoundException,
            RepositoryException {
        return parse(item.getSession(), path).resolve(item);
    }

}
