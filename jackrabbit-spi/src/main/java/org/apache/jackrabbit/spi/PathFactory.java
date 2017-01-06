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
package org.apache.jackrabbit.spi;

import javax.jcr.RepositoryException;

/**
 * <code>PathFactory</code>...
 */
public interface PathFactory {

    /**
     * Return a new <code>Path</code> out of the given <code>parent</code> path
     * and the given relative path. If <code>normalize</code> is
     * <code>true</code>, the returned path will be normalized (or
     * canonicalized, if the parent path is absolute).
     *
     * @param parent
     * @param relPath
     * @param normalize
     * @return
     * @throws IllegalArgumentException if <code>relPath</code> is absolute.
     * @throws RepositoryException If the <code>normalized</code> is
     * <code>true</code> and the resulting path cannot be normalized.
     */
    public Path create(Path parent, Path relPath, boolean normalize) throws IllegalArgumentException, RepositoryException;

    /**
     * Creates a new <code>Path</code> out of the given <code>parent</code> path
     * and the give name. If <code>normalize</code> is <code>true</code>,
     * the returned path will be normalized (or canonicalized, if the parent
     * path is absolute). Use {@link PathFactory#create(Path, Name, int, boolean)}
     * in order to build a <code>Path</code> having an index with his name element.
     *
     * @param parent the parent path
     * @param name the name of the new path element.
     * @param normalize If true the Path is normalized before being returned.
     * @return
     * @throws RepositoryException If the <code>normalized</code> is
     * <code>true</code> and the resulting path cannot be normalized.
     */
    public Path create(Path parent, Name name, boolean normalize) throws RepositoryException;

    /**
     * Creates a new <code>Path</code> out of the given <code>parent</code> path
     * and the give name and normalized index. See also
     * {@link PathFactory#create(Path, Name, boolean)}.
     *
     * @param parent the parent path.
     * @param name the name of the new path element.
     * @param index the index of the new path element.
     * @param normalize If true the Path is normalized before being returned.
     * @return
     * @throws IllegalArgumentException If the given index is lower than
     * {@link Path#INDEX_UNDEFINED}.
     * @throws RepositoryException If the <code>normalized</code> is
     * <code>true</code> and the resulting path cannot be normalized.
     */
    public Path create(Path parent, Name name, int index, boolean normalize) throws IllegalArgumentException, RepositoryException;

    /**
     * Creates a relative path based on a {@link Name}.
     *
     * @param name  single {@link Name} for this relative path.
     * @return the relative path created from <code>name</code>.
     * @throws IllegalArgumentException if the name is <code>null</code>.
     */
    public Path create(Name name) throws IllegalArgumentException;

    /**
     * Creates a relative path based on a {@link Name} and a normalized index.
     * Same as {@link #create(Name)} but allows to explicitly specify an
     * index.
     *
     * @param name  single {@link Name} for this relative path.
     * @param index index of the single name element.
     * @return the relative path created from <code>name</code> and <code>normalizedIndex</code>.
     * @throws IllegalArgumentException if <code>index</code> is lower
     * than {@link Path#INDEX_UNDEFINED} or if the name is not valid.
     */
    public Path create(Name name, int index) throws IllegalArgumentException;

    /**
     * Creates a path from the given element.
     *
     * @param element path element
     * @return the created path
     * @throws IllegalArgumentException if the given element is <code>null</code>
     */
    Path create(Path.Element element) throws IllegalArgumentException;

    /**
     * Create a new <code>Path</code> from the given elements.
     *
     * @param elements
     * @return the <code>Path</code> created from the elements.
     * @throws IllegalArgumentException If the given elements are <code>null</code>
     * or have a length of 0 or would otherwise constitute an invalid path.
     */
    public Path create(Path.Element[] elements) throws IllegalArgumentException;

    /**
     * Returns a <code>Path</code> holding the value of the specified
     * string. The string must be in the format returned by the
     * <code>Path.getString()</code> method.
     *
     * @param pathString a <code>String</code> containing the <code>Path</code>
     * representation to be parsed.
     * @return the <code>Path</code> represented by the argument
     * @throws IllegalArgumentException if the specified string can not be parsed
     * as a <code>Path</code>.
     * @see Path#getString()
     * @see Path#DELIMITER
     */
    public Path create(String pathString) throws IllegalArgumentException;

    /**
     * Creates a path element from the given <code>name</code>.
     * The created path element does not contain an explicit index.
     * <p>
     * If the specified name denotes a <i>special</i> path element (either
     * {@link PathFactory#getParentElement()}, {@link PathFactory#getCurrentElement()} or
     * {@link PathFactory#getRootElement()}) then the associated constant is returned.
     *
     * @param name the name of the element
     * @return a path element
     * @throws IllegalArgumentException if the name is <code>null</code>
     */
    public Path.Element createElement(Name name) throws IllegalArgumentException;

    /**
     * Same as {@link #createElement(Name)} except that an explicit index can be
     * specified.
     * <p>
     * Note that an IllegalArgumentException will be thrown if the specified
     * name denotes a <i>special</i> path element (either
     * {@link PathFactory#getParentElement()}, {@link PathFactory#getCurrentElement()} or
     * {@link PathFactory#getRootElement()}) since an explicit index is not allowed
     * in this context.
     *
     * @param name  the name of the element
     * @param index the index if the element.
     * @return a path element
     * @throws IllegalArgumentException if the name is <code>null</code>,
     * if the given index is lower than {@link Path#INDEX_UNDEFINED} or if name
     * denoting a special path element.
     */
    public Path.Element createElement(Name name, int index) throws IllegalArgumentException;

    /**
     * Creates a path element from the given <code>identifier</code>.
     *
     * @param identifier Node identifier for which the path element should be created.
     * @return a path element.
     * @throws IllegalArgumentException If the <code>identifier</code> is <code>null</code>.
     * @since JCR 2.0
     */
    public Path.Element createElement(String identifier) throws IllegalArgumentException;

    /**
     * Return the current element.
     *
     * @return the current element.
     */
    public Path.Element getCurrentElement();

    /**
     * Return the parent element.
     *
     * @return the parent element.
     */
    public Path.Element getParentElement();

    /**
     * Return the root element.
     *
     * @return the root element.
     */
    public Path.Element getRootElement();

    /**
     * Return the <code>Path</code> of the root node.
     *
     * @return the <code>Path</code> of the root node.
     */
    public Path getRootPath();
}