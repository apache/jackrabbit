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

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.Serializable;

/**
 * The <code>Path</code> interface defines the qualified representation of
 * a JCR path. It consists of {@link Path.Element} objects and is immutable.
 * It has the following properties:
 * <p/>
 * <code>isAbsolute()</code>:<br>
 * A path is absolute if the first path element denotes the root element '/'.
 * <p/>
 * <code>isNormalized()</code>:<br>
 * A path is normalized if all '.' and '..' path elements are resolved as much
 * as possible. If the path is absolute it is normalized if it contains
 * no such elements. For example the path '../../a' is normalized where as
 * '../../b/../a/.' is not. Normalized paths never have '.' elements.
 * Absolute normalized paths have no and relative normalized paths have no or
 * only leading '..' elements.
 * <p/>
 * <code>isCanonical()</code>:<br>
 * A path is canonical if its absolute and normalized.
 * <p/>
 * Note, that the implementation must implement the equals method such that
 * two <code>Path</code> objects having the equal <code>Element</code>s
 * must be equal.
 */
public interface Path extends Serializable {

    /**
     * Constant representing an undefined index value
     */
    public static final int INDEX_UNDEFINED = 0;

    /**
     * Constant representing the default (initial) index value.
     */
    public static final int INDEX_DEFAULT = 1;

    /**
     * Constant defining the depth of the root path
     */
    public static final int ROOT_DEPTH = 0;

    /**
     * Delimiter used in order to concatenate the Path.Element objects
     * upon {@link Path#getString()}.
     */
    public static final char DELIMITER = '\t';

    /**
     * Tests whether this path represents the root path, i.e. "/".
     *
     * @return true if this path represents the root path; false otherwise.
     */
    public boolean denotesRoot();

    /**
     * Tests whether this path is absolute, i.e. whether it starts with "/".
     *
     * @return true if this path is absolute; false otherwise.
     */
    public boolean isAbsolute();

    /**
     * Tests whether this path is canonical, i.e. whether it is absolute and
     * does not contain redundant elements such as "." and "..".
     *
     * @return true if this path is canonical; false otherwise.
     * @see #isAbsolute()
     */
    public boolean isCanonical();

    /**
     * Tests whether this path is normalized, i.e. whether it does not
     * contain redundant elements such as "." and "..".
     * <p/>
     * Note that a normalized path can still contain ".." elements if they are
     * not redundant, e.g. "../../a/b/c" would be a normalized relative path,
     * whereas "../a/../../a/b/c" wouldn't (although they're semantically
     * equivalent).
     *
     * @return true if this path is normalized; false otherwise.
     * @see #getNormalizedPath()
     */
    public boolean isNormalized();

    /**
     * Returns the normalized path representation of this path. This typically
     * involves removing/resolving redundant elements such as "." and ".." from
     * the path, e.g. "/a/./b/.." will be normalized to "/a", "../../a/b/c/.."
     * will be normalized to "../../a/b", and so on.
     * <p/>
     * If the normalized path results in an empty path (eg: 'a/..') or if an
     * absolute path is normalized that would result in a 'negative' path
     * (eg: /a/../../) a MalformedPathException is thrown.
     *
     * @return a normalized path representation of this path.
     * @throws RepositoryException if the path cannot be normalized.
     * @see #isNormalized()
     */
    public Path getNormalizedPath() throws RepositoryException;

    /**
     * Returns the canonical path representation of this path. This typically
     * involves removing/resolving redundant elements such as "." and ".." from
     * the path.
     *
     * @return a canonical path representation of this path.
     * @throws RepositoryException if this path can not be canonicalized
     * (e.g. if it is relative).
     */
    public Path getCanonicalPath() throws RepositoryException;

    /**
     * Computes the relative path from <code>this</code> absolute path to
     * <code>other</code>.
     *
     * @param other an absolute path.
     * @return the relative path from <code>this</code> path to <code>other</code>
     * path.
     * @throws RepositoryException if either <code>this</code> or
     * <code>other</code> path is not absolute.
     */
    public Path computeRelativePath(Path other) throws RepositoryException;

    /**
     * Returns the ancestor path of the specified relative degree.
     * <p/>
     * An ancestor of relative degree <i>x</i> is the path that is <i>x</i>
     * levels up along the path.
     * <ul>
     * <li><i>degree</i> = 0 returns this path.
     * <li><i>degree</i> = 1 returns the parent of this path.
     * <li><i>degree</i> = 2 returns the grandparent of this path.
     * <li>And so on to <i>degree</i> = <i>n</i>, where <i>n</i> is the depth
     * of this path, which returns the root path.
     * </ul>
     * <p/>
     * Note that there migth be an unexpected result if <i>this</i> path is not
     * normalized, e.g. the ancestor of degree = 1 of the path "../.." would
     * be ".." although this is not the parent of "../..".
     *
     * @param degree the relative degree of the requested ancestor.
     * @return the ancestor path of the specified degree.
     * @throws PathNotFoundException if there is no ancestor of the specified degree.
     * @throws IllegalArgumentException if <code>degree</code> is negative.
     */
    public Path getAncestor(int degree) throws IllegalArgumentException, PathNotFoundException;

    /**
     * Returns the number of ancestors of this path. This is the equivalent
     * of <code>{@link #getDepth()} - 1</code>.
     * <p/>
     * Note that the returned value might be negative if this path is not
     * canonical, e.g. the depth of "../../a" is -1, its ancestor count is
     * therefore -2.
     *
     * @return the number of ancestors of this path
     * @see #getDepth()
     * @see #getLength()
     * @see #isCanonical()
     */
    public int getAncestorCount();

    /**
     * Returns the length of this path, i.e. the number of its elements.
     * Note that the root element "/" counts as a separate element, e.g.
     * the length of "/a/b/c" is 4 whereas the length of "a/b/c" is 3.
     * <p/>
     * Also note that the special elements "." and ".." are not treated
     * specially, e.g. both "/a/./.." and "/a/b/c" have a length of 4
     * but this value does not necessarily reflect the true hierarchy level as
     * returned by <code>{@link #getDepth()}</code>.
     *
     * @return the length of this path
     * @see #getDepth()
     * @see #getAncestorCount()
     */
    public int getLength();

    /**
     * Returns the depth of this path. The depth reflects the absolute or
     * relative hierarchy level this path is representing, depending on whether
     * this path is an absolute or a relative path. The depth also takes '.'
     * and '..' elements into account.
     * <p/>
     * Note that the returned value might be negative if this path is not
     * canonical, e.g. the depth of "../../a" is -1.
     *
     * @return the depth this path
     * @see #getLength()
     * @see #getAncestorCount()
     */
    public int getDepth();

    /**
     * Determines if <i>this</i> path is an ancestor of the specified path,
     * based on their (absolute or relative) hierarchy level as returned by
     * <code>{@link #getDepth()}</code>.
     *
     * @return <code>true</code> if <code>other</code> is a descendant;
     * otherwise <code>false</code>.
     * @throws IllegalArgumentException if the given path is <code>null</code>
     * or if not both paths are either absolute or relative.
     * @throws RepositoryException if any of the path cannot be normalized.
     * @see #getDepth()
     */
    public boolean isAncestorOf(Path other) throws IllegalArgumentException, RepositoryException;

    /**
     * Determines if <i>this</i> path is a descendant of the specified path,
     * based on their (absolute or relative) hierarchy level as returned by
     * <code>{@link #getDepth()}</code>.
     *
     * @return <code>true</code> if <code>other</code> is an ancestor;
     * otherwise <code>false</code>
     * @throws IllegalArgumentException If the given path is null.
     * @throws RepositoryException if not both paths are either absolute or
     * relative.
     * @see #getDepth()
     */
    public boolean isDescendantOf(Path other) throws IllegalArgumentException, RepositoryException;

    /**
     * Returns a new <code>Path</code> consisting of those Path.Element objects
     * between the given <tt><code>from</code>, inclusive, and the given <code>to</code>,
     * exclusive. An <code>IllegalArgumentException</code> is thrown if <code>from</code>
     * is greater or equal than <code>to</code> or if any of both params is
     * out of the possible range. A <code>RepositoryException</code> is thrown
     * if this <code>Path</code> is not normalized.
     *
     * @param from
     * @param to
     * @return
     * @throws IllegalArgumentException
     * @throws RepositoryException If this Path is not normalized.
     */
    public Path subPath(int from, int to) throws IllegalArgumentException, RepositoryException;

    /**
     * Returns the elements of this path.
     *
     * @return the elements of this path.
     */
    public Element[] getElements();

    /**
     * Returns the name element (i.e. the last element) of this path.
     *
     * @return the name element of this path
     */
    public Element getNameElement();

    /**
     * Returns the String representation of this Path as it is used
     * by {@link PathFactory#create(String)}.<p/>
     * The String representation must consist of the String representation of
     * its elements separated by {@link Path#DELIMITER}.
     *
     * @see Path.Element#getString()
     * @return
     */
    public String getString();

    //----------------------------------------------------< inner interface >---
    /**
     * Object representation of a single JCR path element. An <code>Element</code>
     * object contains the <code>Name</code> and optional index of a single
     * JCR path element.
     * <p/>
     * Once created, a <code>Element</code> object must be immutable.
     * <p/>
     * The String presentation of an <code>Element</code> must be in the format
     * "<code>{namespaceURI}localPart</code>" or
     * "<code>{namespaceURI}localPart[index]</code>" case of an index greater
     * than {@link Path#INDEX_DEFAULT}.
     * <p/>
     * Note, that the implementation must implement the equals method such, that
     * two <code>Element</code> objects having equals <code>Name</code>s and the
     * same normalized index must be equal.
     */
    public interface Element extends Serializable {

        /**
         * Returns the qualified name of this path element.
         *
         * @return qualified name
         */
        public Name getName();

        /**
         * Returns the index of the element as it has been assigned upon creation.
         *
         * @return index of the element as it has been assigned upon creation.
         */
        public int getIndex();

        /**
         * Returns the normalized index of this path element, i.e. the index
         * is always equals or greater that {@link Path#INDEX_DEFAULT}.
         *
         * @return the normalized index.
         */
        public int getNormalizedIndex();

        /**
         * Returns <code>true</code> if this element denotes the <i>root</i> element,
         * otherwise returns <code>false</code>.
         *
         * @return <code>true</code> if this element denotes the <i>root</i>
         *         element; otherwise <code>false</code>
         */
        public boolean denotesRoot();

        /**
         * Returns <code>true</code> if this element denotes the <i>parent</i>
         * ('..') element, otherwise returns <code>false</code>.
         *
         * @return <code>true</code> if this element denotes the <i>parent</i>
         *         element; otherwise <code>false</code>
         */
        public boolean denotesParent();

        /**
         * Returns <code>true</code> if this element denotes the <i>current</i>
         * ('.') element, otherwise returns <code>false</code>.
         *
         * @return <code>true</code> if this element denotes the <i>current</i>
         *         element; otherwise <code>false</code>
         */
        public boolean denotesCurrent();

        /**
         * Returns <code>true</code> if this element represents a regular name
         * (i.e. neither root, '.' nor '..'), otherwise returns <code>false</code>.
         *
         * @return <code>true</code> if this element represents a regular name;
         *         otherwise <code>false</code>
         */
        public boolean denotesName();

        /**
         * Return the String presentation of a{@link Path.Element}. It must be
         * in the format "<code>{namespaceURI}localPart</code>" or
         * "<code>{namespaceURI}localPart[index]</code>" in case of an index
         * greater than {@link Path#INDEX_DEFAULT}.
         * 
         * @return
         */
        public String getString();
    }
}