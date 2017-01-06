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

import java.io.Serializable;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * The <code>Path</code> interface defines the SPI level representation of
 * a JCR path. It consists of an ordered list of {@link Path.Element} objects
 * and is immutable.
 * <p>
 * A {@link Path.Element} is either {@link Path.Element#denotesName() named}
 * or one of the following special elements:
 * <ul>
 * <li>the {@link Element#denotesCurrent() current} element (Notation: "."),</li>
 * <li>the {@link Element#denotesParent() parent} element (Notation: ".."),</li>
 * <li>the {@link Element#denotesRoot() root} element (Notation: {}), which can
 * only occur as the first element in a path, or</li>
 * <li>an {@link Element#denotesIdentifier() identifier} element, which can
 * only occur as the first element in a path.</li>
 * </ul>
 * <p>
 * A <code>Path</code> is defined to have the following characteristics:
 * <p>
 * <strong>Equality:</strong><br>
 * Two paths are equal if they consist of the same elements.
 * <p>
 * <strong>Length:</strong><br>
 * The {@link Path#getLength() length} of a path is the number of its elements.
 * <p>
 * <strong>Depth:</strong><br>
 * The {@link Path#getDepth() depth} of a path is
 * <ul>
 * <li>0 for the root path,</li>
 * <li>0 for a path consisting of an identifier element only,</li>
 * <li>0 for the path consisting of the current element only,</li>
 * <li>-1 for the path consisting of the parent element only,</li>
 * <li>1 for the path consisting of a single named element,</li>
 * <li>depth(P) + depth(Q) for the path P/Q.</li>
 * </ul>
 * <p>
 * The depth of a normalized absolute path equals its length minus 1.
 * <p>
 * <strong>Absolute vs. Relative</strong><br>
 * A path can be absolute or relative:<br>
 * A path {@link #isAbsolute() is absolute} if its first element is the root
 * or an identifier element. A path is relative if it is not absolute.
 * <ul>
 * <li>An absolute path is valid if its depth is greater or equals 0. A relative
 * path is always valid.</li>
 * <li>Two absolute paths are equivalent if "they refer to the same item in the
 * hierarchy".</li>
 * <li>Two relative paths P and Q are equivalent if for every absolute path R such
 * that R/P and R/Q are valid, R/P and R/Q are equivalent.</li>
 * </ul>
 * <p>
 * <strong>Normalization:</strong><br>
 * A path P {@link Path#isNormalized() is normalized} if P has minimal length
 * amongst the set of all paths Q which are equivalent to P.<br>
 * This means that '.' and '..' elements are resolved as much as possible.
 * An absolute path it is normalized if it is not identifier-based and
 * contains no current or parent elements. The normalization of a path
 * is unique.<br>
 * <p>
 * <strong>Equivalence:</strong><br>
 * Path P is {@link Path#isEquivalentTo(Path) equivalent} to path Q (in the above sense)
 * if the normalization of P is equal to the normalization of Q. This is
 * an equivalence relation (i.e. reflexive, transitive,
 * and symmetric).
 * <p>
 * <strong>Canonical Paths:</strong><br>
 * A path {@link Path#isCanonical() is canonical} if its absolute and normalized.
 * <p>
 * <strong>Hierarchical Relationship:</strong><br>
 * The ancestor relationship is a strict partial order (i.e. irreflexive, transitive,
 * and asymmetric). Path P is a direct ancestor of path Q if P is equivalent to Q/..
 * <br>
 * Path P is an {@link Path#isAncestorOf(Path) ancestor} of path Q if
 * <ul>
 * <li>P is a direct ancestor of Q or</li>
 * <li>P is a direct ancestor of some path S which is an ancestor of Q.</li>
 * </ul>
 * <p>
 * Path P is an {@link Path#isDescendantOf(Path) descendant} of path Q if
 * <ul>
 * <li>Path P is a descendant of path Q if Q is an ancestor of P.</li>
 * </ul>
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
     * Returns the name of the last path element, or <code>null</code>
     * for an identifier. The names of the special root, current and parent
     * elements are "", "." and ".." in the default namespace.
     *
     * @return name of the last path element, or <code>null</code>
     */
    Name getName();

    /**
     * Returns the index of the last path element, or {@link #INDEX_UNDEFINED}
     * if the index is not defined or not applicable. The index of an
     * identifier or the special root, current or parent element is always
     * undefined.
     *
     * @return index of the last path element, or {@link #INDEX_UNDEFINED}
     */
    int getIndex();

    /**
     * Returns the normalized index of the last path element. The normalized
     * index of an element with an undefined index is {@link #INDEX_DEFAULT}.
     *
     * @return normalized index of the last path element
     */
    int getNormalizedIndex();

    /**
     * Returns the identifier of a single identifier element. Returns null
     * for non-identifier paths or identifier paths with other relative path
     * elements.
     *
     * @return identifier, or <code>null</code>
     */
    String getIdentifier();

    /**
     * Tests whether this is the root path, i.e. "/".
     *
     * @return <code>true</code> if this is the root path,
     *         <code>false</code> otherwise.
     */
    boolean denotesRoot();

    /**
     * Test if this path consists of a single identifier element.
     *
     * @return <code>true</code> if this path is an identifier
     */
    boolean denotesIdentifier();

    /**
     * Checks if the last path element is the parent element ("..").
     *
     * @return <code>true</code> if the last path element is the parent element,
     *         <code>false</code> otherwise
     */
    boolean denotesParent();

    /**
     * Checks if the last path element is the current element (".").
     *
     * @return <code>true</code> if the last path element is the current element,
     *         <code>false</code> otherwise
     */
    boolean denotesCurrent();

    /**
     * Checks if the last path element is a named and optionally indexed
     * element.
     *
     * @return <code>true</code> if the last path element is a named element,
     *         <code>false</code> otherwise
     */
    boolean denotesName();

    /**
     * Test if this path represents an unresolved identifier-based path.
     *
     * @return <code>true</code> if this path represents an unresolved
     * identifier-based path.
     */
    boolean isIdentifierBased();

    /**
     * Tests whether this path is absolute, i.e. whether it starts with "/" or
     * is an identifier based path.
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
     * <p>
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
     * Returns the normalized path representation of this path.
     * <p>
     * If the path cannot be normalized (e.g. if an absolute path is normalized
     * that would result in a 'negative' path) a RepositoryException is thrown.
     *
     * @return a normalized path representation of this path.
     * @throws RepositoryException if the path cannot be normalized.
     * @see #isNormalized()
     */
    public Path getNormalizedPath() throws RepositoryException;

    /**
     * Returns the canonical path representation of this path.
     * <p>
     * If the path is relative or cannot be normalized a RepositoryException
     * is thrown.
     *
     * @return a canonical path representation of this path.
     * @throws RepositoryException if this path can not be canonicalized
     * (e.g. if it is relative).
     */
    public Path getCanonicalPath() throws RepositoryException;

    /**
     * Resolves the given path element against this path. If the given
     * element is absolute (i.e. the root or an identifier element), then
     * a path containing just that element is returned. Otherwise the
     * returned path consists of this path followed by the given element.
     *
     * @param element path element
     * @return resolved path
     */
    Path resolve(Element element);

    /**
     * Resolves the given path against this path. If the given path is
     * absolute, then it is returned as-is. Otherwise the path is resolved
     * relative to this path and the resulting resolved path is returned.
     *
     * @param relative the path to be resolved
     * @return resolved path
     */
    Path resolve(Path relative);

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
     * Normalizes this path and returns the ancestor path of the specified
     * relative degree.
     * <p>
     * An ancestor of relative degree <i>x</i> is the path that is <i>x</i>
     * levels up along the path.
     * <ul>
     * <li><i>degree</i> = 0 returns this path.
     * <li><i>degree</i> = 1 returns the parent of this path.
     * <li><i>degree</i> = 2 returns the grandparent of this path.
     * <li>And so on to <i>degree</i> = <i>n</i>, where <i>n</i> is the depth
     * of this path, which returns the root path.
     * </ul>
     * <p>
     * If this path is relative the implementation may not be able to determine
     * if the ancestor at <code>degree</code> exists. Such an implementation
     * should properly build the ancestor (i.e. parent of .. is ../..) and
     * leave if it the caller to throw <code>PathNotFoundException</code>.
     *
     * @param degree the relative degree of the requested ancestor.
     * @return the normalized ancestor path of the specified degree.
     * @throws IllegalArgumentException if <code>degree</code> is negative.
     * @throws PathNotFoundException if the implementation is able to determine
     * that there is no ancestor of the specified degree. In case of this
     * being an absolute path, this would be the case if <code>degree</code> is
     * greater that the {@link #getDepth() depth} of this path.
     * @throws RepositoryException If the implementation is not able to determine
     * the ancestor of the specified degree for some other reason.
     */
    public Path getAncestor(int degree) throws IllegalArgumentException, PathNotFoundException, RepositoryException;

    /**
     * Returns the number of ancestors of this path. This is the equivalent
     * of <code>{@link #getDepth()}</code> in case of a absolute path.
     * For relative path the number of ancestors cannot be determined and
     * -1 should be returned.
     *
     * @return the number of ancestors of this path or -1 if the number of
     * ancestors cannot be determined.
     * @see #getDepth()
     * @see #getLength()
     * @see #isCanonical()
     */
    public int getAncestorCount();

    /**
     * Returns the length of this path, i.e. the number of its elements.
     * Note that the root element "/" counts as a separate element, e.g.
     * the length of "/a/b/c" is 4 whereas the length of "a/b/c" is 3.
     * <p>
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
     * and '..' elements into account. The depth of the root path, an
     * identifier and the current path must be 0.
     * <p>
     * Note that the returned value might be negative if this path is not
     * canonical, e.g. the depth of "../../a" is -1.
     *
     * @return the depth this path
     * @see #getLength()
     * @see #getAncestorCount()
     */
    public int getDepth();

    /**
     * Determines if the the <code>other</code> path would be equal to <code>this</code>
     * path if both of them are normalized.
     *
     * @param other Another path.
     * @return true if the given other path is equivalent to this path.
     * @throws IllegalArgumentException if the given path is <code>null</code>
     * or if not both paths are either absolute or relative.
     * @throws RepositoryException if any of the path cannot be normalized.
     */
    public boolean isEquivalentTo(Path other) throws IllegalArgumentException, RepositoryException;

    /**
     * Determines if <i>this</i> path is an ancestor of the specified path,
     * based on their (absolute or relative) hierarchy level as returned by
     * <code>{@link #getDepth()}</code>. In case of undefined ancestor/descendant
     * relationship that might occur with relative paths, the return value
     * should be <code>false</code>.
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
     * <code>{@link #getDepth()}</code>. In case of undefined ancestor/descendant
     * relationship that might occur with relative paths, the return value
     * should be <code>false</code>.
     *
     * @return <code>true</code> if <code>other</code> is an ancestor;
     * otherwise <code>false</code>.
     * @throws IllegalArgumentException if the given path is <code>null</code>
     * or if not both paths are either absolute or relative.
     * @throws RepositoryException if any of the path cannot be normalized.
     * @see #isAncestorOf(Path)
     */
    public boolean isDescendantOf(Path other) throws IllegalArgumentException, RepositoryException;

    /**
     * Returns a new <code>Path</code> consisting of those Path.Element objects
     * between the given <code>from</code>, inclusive, and the given <code>to</code>,
     * exclusive. An <code>IllegalArgumentException</code> is thrown if <code>from</code>
     * is greater or equal than <code>to</code> or if any of both params is
     * out of the possible range.
     *
     * @param from index of the element to start with and low endpoint
     * (inclusive) within the list of elements to use for the sub-path.
     * @param to index of the element outside of the range i.e. high endpoint
     * (exclusive) within the list of elements to use for the sub-path.
     * @return a new <code>Path</code> consisting of those Path.Element objects
     * between the given <code>from</code>, inclusive, and the given
     * <code>to</code>, exclusive.
     * @throws IllegalArgumentException if <code>from</code>
     * is greater or equal than <code>to</code> or if any of both params is
     * out of the possible range.
     */
    public Path subPath(int from, int to) throws IllegalArgumentException;

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
     * Returns a path that consists of only the last element of this path.
     *
     * @see #getFirstElements()
     * @return last element of this path
     */
    Path getLastElement();

    /**
     * Returns a path that consists of all but the last element of this path.
     * Returns <code>null</code> if this path contains just a single element.
     *
     * @see #getLastElement()
     * @return first elements of this path, or <code>null</code>
     */
    Path getFirstElements();

    /**
     * Returns the String representation of this Path as it is used
     * by {@link PathFactory#create(String)}.
     * <p>
     * The String representation must consist of the String representation of
     * its elements separated by {@link Path#DELIMITER}.
     *
     * @see Path.Element#getString()
     * @return Returns the String representation of this Path.
     */
    public String getString();

    //----------------------------------------------------< inner interface >---
    /**
     * Object representation of a single JCR path element. An <code>Element</code>
     * object contains the <code>Name</code> and optional index of a single
     * JCR path element.
     * <p>
     * Once created, a <code>Element</code> object must be immutable.
     * <p>
     * The String presentation of an <code>Element</code> must be in the format
     * "<code>{namespaceURI}localPart</code>" or
     * "<code>{namespaceURI}localPart[index]</code>" case of an index greater
     * than {@link Path#INDEX_DEFAULT}.
     * <p>
     * Note, that the implementation must implement the equals method such, that
     * two <code>Element</code> objects having equals <code>Name</code>s and the
     * same normalized index must be equal.
     */
    public interface Element extends Serializable {

        /**
         * Returns the name of this path element.
         *
         * @return The name of this path element.
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
         * Returns the identifier of an identifier element, or
         * <code>null</code> for other kinds of elements.
         *
         * @return identifier, or <code>null</code>
         */
        String getIdentifier();

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
         * Returns <code>true</code> if this element represents an identifier element.
         * 
         * @return <code>true</code> if this element represents an identifier element.
         * @since JCR 2.0
         */
        public boolean denotesIdentifier();

        /**
         * Return the String presentation of a {@link Path.Element}. It must be
         * in the format "<code>{namespaceURI}localPart</code>" or
         * "<code>{namespaceURI}localPart[index]</code>" in case of an index
         * greater than {@link Path#INDEX_DEFAULT}.
         *
         * @return String representation of a {@link Path.Element}.
         */
        public String getString();

    }
}
