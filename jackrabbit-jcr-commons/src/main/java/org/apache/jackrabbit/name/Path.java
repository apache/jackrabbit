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

import javax.jcr.PathNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.io.Serializable;

/**
 * The <code>Path</code> utility class provides misc. methods to resolve and
 * nornalize JCR-style item paths.
 * <p/>
 * Each path consistnes of path elements and is immutable. It has the following
 * properties:
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
 * <h2>String representations</h2>
 * <p/>
 * The JCR path format is specified by JSR 170 as follows:
 * <pre>
 *  path ::= properpath ['/']
 *  properpath ::= abspath | relpath
 *  abspath ::= '/' relpath
 *  relpath ::= pathelement | relpath '/' pathelement
 *  pathelement ::= name | name '[' number ']' | '..' | '.'
 *  number ::= (* An integer > 0 *)
 *  name ::= simplename | prefixedname
 *  simplename ::= onecharsimplename |
 *                 twocharsimplename |
 *                 threeormorecharname
 *  prefixedname ::= prefix ':' localname
 *  localname ::= onecharlocalname |
 *                twocharlocalname |
 *                threeormorecharname
 *  onecharsimplename ::= (* Any Unicode character except:
 *                     '.', '/', ':', '[', ']', '*',
 *                     ''', '"', '|' or any whitespace
 *                     character *)
 *  twocharsimplename ::= '.' onecharsimplename |
 *                        onecharsimplename '.' |
 *                        onecharsimplename onecharsimplename
 *  onecharlocalname ::= nonspace
 *  twocharlocalname ::= nonspace nonspace
 *  threeormorecharname ::= nonspace string nonspace
 *  prefix ::= (* Any valid XML Name *)
 *  string ::= char | string char
 *  char ::= nonspace | ' '
 *  nonspace ::= (* Any Unicode character except:
 *                  '/', ':', '[', ']', '*',
 *                  ''', '"', '|' or any whitespace
 *                  character *)
 * </pre>
 *
 * @deprecated Use the Path and PathFactory interfaces from
 *             the org.apache.jackrabbit.spi package of
 *             the jackrabbit-spi component. A default implementation
 *             is available as
 *             the org.apache.jackrabbit.spi.commons.name.PathFactoryImpl
 *             class in the jackrabbit-spi-commons component.
 */
public final class Path implements Serializable {

    static final long serialVersionUID = 7272485577196962560L;

    /**
     * the 'root' element. i.e. '/'
     */
    public static final PathElement ROOT_ELEMENT = new RootElement();

    /**
     * the 'current' element. i.e. '.'
     */
    public static final PathElement CURRENT_ELEMENT = new CurrentElement();

    /**
     * the 'parent' element. i.e. '..'
     */
    public static final PathElement PARENT_ELEMENT = new ParentElement();

    /**
     * the root path
     */
    public static final Path ROOT = new Path(new PathElement[]{ROOT_ELEMENT}, true);

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
     * the elements of this path
     */
    private final PathElement[] elements;

    /**
     * flag indicating if this path is normalized
     */
    private final boolean normalized;

    /**
     * flag indicating if this path is absolute
     */
    private final boolean absolute;

    /**
     * the cached hashcode of this path
     */
    private transient int hash = 0;

    /**
     * the cached 'toString' of this path
     */
    private transient String string;

    /**
     * Private constructor
     *
     * @param elements
     * @param isNormalized
     */
    private Path(PathElement[] elements, boolean isNormalized) {
        if (elements == null || elements.length == 0) {
            throw new IllegalArgumentException("Empty paths are not allowed");
        }
        this.elements = elements;
        this.absolute = elements[0].denotesRoot();
        this.normalized = isNormalized;
    }

    //------------------------------------------------------< factory methods >
    /**
     * Creates a new <code>Path</code> from the given <code>jcrPath</code>
     * string. If <code>normalize</code> is <code>true</code>, the returned
     * path will be normalized (or canonicalized if absolute).
     *
     * @param jcrPath
     * @param resolver
     * @param normalize
     * @throws MalformedPathException
     * @deprecated Use PathFormat#parse(String, NamespaceResolver)} instead.
     */
    public static Path create(String jcrPath, NamespaceResolver resolver,
                              boolean normalize)
            throws MalformedPathException {
        Path path = PathFormat.parse(jcrPath, resolver);
        if (normalize) {
            return path.getNormalizedPath();
        } else {
            return path;
        }
    }

    /**
     * Creates a new <code>Path</code> out of the given <code>parent</code> path
     * and a relative path string. If <code>canonicalize</code> is
     * <code>true</code>, the returned path will be canonicalized.
     *
     * @param parent
     * @param relJCRPath
     * @param resolver
     * @param canonicalize
     * @throws MalformedPathException
     * @deprecated Use {@link PathFormat#parse(Path, String, NamespaceResolver)} instead.
     */
    public static Path create(Path parent, String relJCRPath,
                              NamespaceResolver resolver, boolean canonicalize)
            throws MalformedPathException {
        Path path = PathFormat.parse(parent, relJCRPath, resolver);
        if (canonicalize) {
            return path.getCanonicalPath();
        } else {
            return path;
        }
    }

    /**
     * Creates a new <code>Path</code> out of the given <code>parent<code> path
     * string and the given relative path string. If <code>normalize</code> is
     * <code>true</code>, the returned path will be normalized (or
     * canonicalized, if the parent path is absolute).
     *
     * @param parent
     * @param relPath
     * @param normalize
     * @throws MalformedPathException if <code>relPath</code> is absolute
     */
    public static Path create(Path parent, Path relPath, boolean normalize)
            throws MalformedPathException {
        if (relPath.isAbsolute()) {
            throw new MalformedPathException("relPath is not a relative path");
        }

        PathBuilder pb = new PathBuilder(parent);
        pb.addAll(relPath.getElements());

        Path path = pb.getPath();
        if (normalize) {
            return path.getNormalizedPath();
        } else {
            return path;
        }
    }

    /**
     * Creates a new <code>Path</code> out of the given <code>parent<code> path
     * string and the give name. If <code>normalize</code> is <code>true</code>,
     * the returned path will be normalized (or canonicalized, if the parent
     * path is absolute).
     *
     * @param parent the parent path
     * @param name the name of the new path element.
     * @param normalize
     * @return the new path.
     */
    public static Path create(Path parent, QName name, boolean normalize) throws MalformedPathException {
        PathBuilder pb = new PathBuilder(parent);
        pb.addLast(name);

        Path path = pb.getPath();
        if (normalize) {
            return path.getNormalizedPath();
        } else {
            return path;
        }
    }

    /**
     * Creates a new <code>Path</code> out of the given <code>parent<code> path
     * and the give name and index.
     *
     * @param parent    the paren tpath.
     * @param name      the name of the new path element.
     * @param index     the index of the new path element.
     * @param normalize
     * @return the new path.
     */
    public static Path create(Path parent, QName name, int index, boolean normalize)
            throws MalformedPathException {
        PathBuilder pb = new PathBuilder(parent);
        pb.addLast(name, index);

        Path path = pb.getPath();
        if (normalize) {
            return path.getNormalizedPath();
        } else {
            return path;
        }
    }

    /**
     * Creates a relative path based on a {@link QName} and an index.
     *
     * @param name  single {@link QName} for this relative path.
     * @param index index of the sinlge name element.
     * @return the relative path created from <code>name</code>.
     * @throws IllegalArgumentException if <code>index</code> is negative.
     */
    public static Path create(QName name, int index)
            throws IllegalArgumentException {
        if (index < Path.INDEX_UNDEFINED) {
            throw new IllegalArgumentException("index must not be negative: " + index);
        }
        PathElement elem;
        if (index < Path.INDEX_DEFAULT) {
            elem = PathElement.create(name);
        } else {
            elem = PathElement.create(name, index);
        }
        return new Path(new PathElement[]{elem}, !elem.denotesCurrent());
    }

    //------------------------------------------------------< utility methods >
    /**
     * Checks if <code>jcrPath</code> is a valid JCR-style absolute or relative
     * path.
     *
     * @param jcrPath the path to be checked
     * @throws MalformedPathException If <code>jcrPath</code> is not a valid
     *                                JCR-style path.
     * @deprecated Use {@link PathFormat#checkFormat(String)} instead.
     */
    public static void checkFormat(String jcrPath) throws MalformedPathException {
        PathFormat.checkFormat(jcrPath);
    }

    //-------------------------------------------------------< public methods >
    /**
     * Tests whether this path represents the root path, i.e. "/".
     *
     * @return true if this path represents the root path; false otherwise.
     */
    public boolean denotesRoot() {
        return absolute && elements.length == 1;
    }

    /**
     * Tests whether this path is absolute, i.e. whether it starts with "/".
     *
     * @return true if this path is absolute; false otherwise.
     */
    public boolean isAbsolute() {
        return absolute;
    }

    /**
     * Tests whether this path is canonical, i.e. whether it is absolute and
     * does not contain redundant elements such as "." and "..".
     *
     * @return true if this path is canonical; false otherwise.
     * @see #isAbsolute()
     */
    public boolean isCanonical() {
        return absolute && normalized;
    }

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
    public boolean isNormalized() {
        return normalized;
    }

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
     * @return a normalized path representation of this path
     * @throws MalformedPathException if the path cannot be normalized.
     * @see #isNormalized()
     */
    public Path getNormalizedPath() throws MalformedPathException {
        if (isNormalized()) {
            return this;
        }
        LinkedList queue = new LinkedList();
        PathElement last = PARENT_ELEMENT;
        for (int i = 0; i < elements.length; i++) {
            PathElement elem = elements[i];
            if (elem.denotesParent() && !last.denotesParent()) {
                if (last.denotesRoot()) {
                    // the first element is the root element;
                    // ".." would refer to the parent of root
                    throw new MalformedPathException(
                            "Path can not be canonicalized: unresolvable '..' element");
                }
                queue.removeLast();
                if (queue.isEmpty()) {
                    last = PARENT_ELEMENT;
                } else {
                    last = (PathElement) queue.getLast();
                }
            } else if (!elem.denotesCurrent()) {
                last = elem;
                queue.add(last);
            }
        }
        if (queue.isEmpty()) {
            throw new MalformedPathException("Path can not be normalized: would result in an empty path.");
        }
        return new Path((PathElement[]) queue.toArray(new PathElement[queue.size()]), true);
    }

    /**
     * Returns the canonical path representation of this path. This typically
     * involves removing/resolving redundant elements such as "." and ".." from
     * the path.
     *
     * @return a canonical path representation of this path
     * @throws MalformedPathException if this path can not be canonicalized
     *                                (e.g. if it is relative)
     */
    public Path getCanonicalPath() throws MalformedPathException {
        if (isCanonical()) {
            return this;
        }
        if (!isAbsolute()) {
            throw new MalformedPathException("only an absolute path can be canonicalized.");
        }
        return getNormalizedPath();
    }

    /**
     * Computes the relative path from <code>this</code> absolute path to
     * <code>other</code>.
     *
     * @param other an absolute path
     * @return the relative path from <code>this</code> path to
     *         <code>other</code> path
     * @throws MalformedPathException if either <code>this</code> or
     *                                <code>other</code> path is not absolute
     */
    public Path computeRelativePath(Path other) throws MalformedPathException {
        if (other == null) {
            throw new IllegalArgumentException("null argument");
        }

        // make sure both paths are absolute
        if (!isAbsolute() || !other.isAbsolute()) {
            throw new MalformedPathException("not an absolute path");
        }

        // make sure we're comparing canonical paths
        Path p0 = getCanonicalPath();
        Path p1 = other.getCanonicalPath();

        if (p0.equals(p1)) {
            // both paths are equal, the relative path is therefore '.'
            PathBuilder pb = new PathBuilder();
            pb.addLast(CURRENT_ELEMENT);
            return pb.getPath();
        }

        // determine length of common path fragment
        int lengthCommon = 0;
        for (int i = 0; i < p0.getElements().length && i < p1.getElements().length; i++) {
            if (!p0.getElement(i).equals(p1.getElement(i))) {
                break;
            }
            lengthCommon++;
        }

        PathBuilder pb = new PathBuilder();
        if (lengthCommon < p0.getElements().length) {
            /**
             * the common path fragment is an ancestor of this path;
             * this has to be accounted for by prepending '..' elements
             * to the relative path
             */
            int tmp = p0.getElements().length - lengthCommon;
            while (tmp-- > 0) {
                pb.addFirst(PARENT_ELEMENT);
            }
        }
        // add remainder of other path
        for (int i = lengthCommon; i < p1.getElements().length; i++) {
            pb.addLast(p1.getElement(i));
        }
        // we're done
        return pb.getPath();
    }

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
     * @throws PathNotFoundException if there is no ancestor of the specified degree
     * @throws IllegalArgumentException if <code>degree</code> is negative
     */
    public Path getAncestor(int degree)
            throws IllegalArgumentException, PathNotFoundException {
        if (degree < 0) {
            throw new IllegalArgumentException("degree must be >= 0");
        } else if (degree == 0) {
            return this;
        }
        int length = elements.length - degree;
        if (length < 1) {
            throw new PathNotFoundException("no such ancestor path of degree " + degree);
        }
        PathElement[] elements = new PathElement[length];
        System.arraycopy(this.elements, 0, elements, 0, length);
        return new Path(elements, normalized);
    }

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
    public int getAncestorCount() {
        return getDepth() - 1;
    }

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
    public int getLength() {
        return elements.length;
    }

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
    public int getDepth() {
        int depth = ROOT_DEPTH;
        for (int i = 0; i < elements.length; i++) {
            if (elements[i].denotesParent()) {
                depth--;
            } else if (!elements[i].denotesCurrent()) {
                depth++;
            }
        }
        return depth;
    }

    /**
     * Determines if <i>this</i> path is an ancestor of the specified path,
     * based on their (absolute or relative) hierarchy level as returned by
     * <code>{@link #getDepth()}</code>.
     *
     * @return <code>true</code> if <code>other</code> is a descendant;
     *         otherwise <code>false</code>
     * @throws MalformedPathException if not both paths are either absolute or
     *                                relative.
     * @see #getDepth()
     */
    public boolean isAncestorOf(Path other) throws MalformedPathException {
        if (other == null) {
            throw new IllegalArgumentException("null argument");
        }
        // make sure both paths are either absolute or relative
        if (isAbsolute() != other.isAbsolute()) {
            throw new MalformedPathException("cannot compare a relative path with an absolute path");
        }
        // make sure we're comparing normalized paths
        Path p0 = getNormalizedPath();
        Path p1 = other.getNormalizedPath();

        if (p0.equals(p1)) {
            return false;
        }
        // calculate depth of paths (might be negative)
        if (p0.getDepth() >= p1.getDepth()) {
            return false;
        }
        for (int i = 0; i < p0.getElements().length; i++) {
            if (!p0.getElement(i).equals(p1.getElement(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines if <i>this</i> path is a descendant of the specified path,
     * based on their (absolute or relative) hierarchy level as returned by
     * <code>{@link #getDepth()}</code>.
     *
     * @return <code>true</code> if <code>other</code> is an ancestor;
     *         otherwise <code>false</code>
     * @throws MalformedPathException if not both paths are either absolute or
     *                                relative.
     * @see #getDepth()
     */
    public boolean isDescendantOf(Path other) throws MalformedPathException {
        if (other == null) {
            throw new IllegalArgumentException("null argument");
        }
        return other.isAncestorOf(this);
    }

    /**
     * Returns the name element (i.e. the last element) of this path.
     *
     * @return the name element of this path
     */
    public PathElement getNameElement() {
        return elements[elements.length - 1];
    }

    /**
     * Returns the elements of this path.
     *
     * @return the elements of this path.
     */
    public PathElement[] getElements() {
        return elements;
    }

    /**
     * Returns the <code>i</code><sup>th</sup> element of this path.
     *
     * @param i element index.
     * @return the <code>i</code><sup>th</sup> element of this path.
     * @throws ArrayIndexOutOfBoundsException if this path does not have an
     *                                        element at index <code>i</code>.
     */
    public PathElement getElement(int i) {
        return elements[i];
    }

    /**
     * Returns a string representation of this <code>Path</code> in the
     * JCR path format.
     *
     * @param resolver namespace resolver
     * @return JCR path
     * @throws NoPrefixDeclaredException if a namespace can not be resolved
     * @deprecated Use {@link PathFormat#format(Path, NamespaceResolver)} instead.
     */
    public String toJCRPath(NamespaceResolver resolver) throws NoPrefixDeclaredException {
        return PathFormat.format(this, resolver);
    }

    //---------------------------------------------------------------< Object >
    /**
     * Returns the internal string representation of this <code>Path</code>.
     * <p/>
     * Note that the returned string is not a valid JCR path, i.e. the
     * namespace URI's of the individual path elements are not replaced with
     * their mapped prefixes. Call
     * <code>{@link #toJCRPath(NamespaceResolver)}</code>
     * for a JCR path representation.
     *
     * @return the internal string representation of this <code>Path</code>.
     */
    public String toString() {
        // Path is immutable, we can store the string representation
        if (string == null) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < elements.length; i++) {
                if (i > 0) {
                    // @todo find safe path separator char that does not conflict with chars in serialized QName
                    sb.append('\t');
                }
                PathElement element = elements[i];
                String elem = element.toString();
                sb.append(elem);
            }
            string = sb.toString();
        }
        return string;
    }

    /**
     * Returns a <code>Path</code> holding the value of the specified
     * string. The string must be in the format returned by the
     * <code>Path.toString()</code> method.
     *
     * @param s a <code>String</code> containing the <code>Path</code>
     *          representation to be parsed.
     * @return the <code>Path</code> represented by the argument
     * @throws IllegalArgumentException if the specified string can not be parsed
     *                                  as a <code>Path</code>.
     * @see #toString()
     */
    public static Path valueOf(String s) throws IllegalArgumentException {
        if ("".equals(s) || s == null) {
            throw new IllegalArgumentException("invalid Path literal");
        }

        // split into path elements

        // @todo find safe path separator char that does not conflict with chars in serialized QName
        final char delim = '\t';
        int lastPos = 0;
        int pos = s.indexOf(delim);
        ArrayList list = new ArrayList();
        boolean isNormalized = true;
        boolean leadingParent = true;
        while (lastPos >= 0) {
            PathElement elem;
            if (pos >= 0) {
                elem = PathElement.fromString(s.substring(lastPos, pos));
                lastPos = pos + 1;
                pos = s.indexOf(delim, lastPos);
            } else {
                elem = PathElement.fromString(s.substring(lastPos));
                lastPos = -1;
            }
            list.add(elem);
            leadingParent &= elem.denotesParent();
            isNormalized &= !elem.denotesCurrent() && (leadingParent || !elem.denotesParent());
        }

        return new Path((PathElement[]) list.toArray(new PathElement[list.size()]), isNormalized);
    }

    /**
     * Returns a hash code value for this path.
     *
     * @return a hash code value for this path.
     * @see Object#hashCode()
     */
    public int hashCode() {
        // Path is immutable, we can store the computed hash code value
        int h = hash;
        if (h == 0) {
            h = 17;
            for (int i = 0; i < elements.length; i++) {
                h = 37 * h + elements[i].hashCode();
            }
            hash = h;
        }
        return h;
    }

    /**
     * Compares the specified object with this path for equality.
     *
     * @param obj the object to be compared for equality with this path.
     * @return <tt>true</tt> if the specified object is equal to this path.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Path) {
            Path other = (Path) obj;
            return Arrays.equals(elements, other.getElements());
        }
        return false;
    }

    //--------------------------------------------------------< inner classes >
    /**
     * Internal helper class used to build a path from pre-parsed path elements.
     * <p/>
     * <strong>Warning!</strong> This class does neither validate the format of
     * the path elements nor does it validate the format of the entire path!
     * This class should therefore only be used in special situations. The
     * regular way of creating/building a <code>Path</code> object is by calling
     * any of the overloaded <code>Path.create()</code> factory methods.
     */
    public static final class PathBuilder implements Cloneable {

        /**
         * the list of path elements of the constructed path
         */
        private final LinkedList queue;

        /**
         * flag indicating if the current path is normalized
         */
        boolean isNormalized = true;

        /**
         * flag indicating if the current path has leading parent '..' elements
         */
        boolean leadingParent = true;

        /**
         * Creates a new PathBuilder.
         */
        public PathBuilder() {
            queue = new LinkedList();
        }

        /**
         * Creates a new PathBuilder and initialized it with the given path
         * elements.
         *
         * @param elements
         */
        public PathBuilder(PathElement[] elements) {
            this();
            addAll(elements);
        }

        /**
         * Creates a new PathBuilder and initialized it with elements of the
         * given path.
         *
         * @param parent
         */
        public PathBuilder(Path parent) {
            this();
            addAll(parent.getElements());
        }

        /**
         * Adds the {@link Path#ROOT_ELEMENT}.
         */
        public void addRoot() {
            addFirst(ROOT_ELEMENT);
        }

        /**
         * Adds the given elemenets
         *
         * @param elements
         */
        public void addAll(PathElement[] elements) {
            for (int i = 0; i < elements.length; i++) {
                addLast(elements[i]);
            }
        }

        /**
         * Inserts the element at the beginning of the path to be built.
         *
         * @param elem
         */
        public void addFirst(PathElement elem) {
            if (queue.isEmpty()) {
                isNormalized &= !elem.denotesCurrent();
                leadingParent = elem.denotesParent();
            } else {
                isNormalized &= !elem.denotesCurrent() && (!leadingParent || elem.denotesParent());
                leadingParent |= elem.denotesParent();
            }
            queue.addFirst(elem);
        }

        /**
         * Inserts the element at the beginning of the path to be built.
         *
         * @param name
         */
        public void addFirst(QName name) {
            addFirst(PathElement.create(name));
        }

        /**
         * Inserts the element at the beginning of the path to be built.
         *
         * @param name
         * @param index
         */
        public void addFirst(QName name, int index) {
            addFirst(PathElement.create(name, index));
        }

        /**
         * Inserts the element at the end of the path to be built.
         *
         * @param elem
         */
        public void addLast(PathElement elem) {
            queue.addLast(elem);
            leadingParent &= elem.denotesParent();
            isNormalized &= !elem.denotesCurrent() && (leadingParent || !elem.denotesParent());
        }

        /**
         * Inserts the element at the end of the path to be built.
         *
         * @param name
         */
        public void addLast(QName name) {
            addLast(PathElement.create(name));
        }

        /**
         * Inserts the element at the end of the path to be built.
         *
         * @param name
         * @param index
         */
        public void addLast(QName name, int index) {
            addLast(PathElement.create(name, index));
        }

        /**
         * Assembles the built path and returns a new {@link Path}.
         *
         * @return a new {@link Path}
         * @throws MalformedPathException if the internal path element queue is empty.
         */
        public Path getPath() throws MalformedPathException {
            PathElement[] elements = (PathElement[]) queue.toArray(new PathElement[queue.size()]);
            // validate path
            if (elements.length == 0) {
                throw new MalformedPathException("empty path");
            }

            // no need to check the path format, assuming all names correct
            return new Path(elements, isNormalized);
        }
    }

    //---------------------------------------------< PathElement & subclasses >
    /**
     * Object representation of a single JCR path element. A PathElement
     * object contains the qualified name and optional index of a single
     * JCR path element.
     * <p/>
     * Once created, a PathElement object is immutable.
     */
    public abstract static class PathElement implements Serializable {

        /**
         * Qualified name of the path element.
         */
        private final QName name;

        /**
         * Optional index of the path element. Set to zero, if not
         * explicitly specified, otherwise contains the 1-based index.
         */
        private final int index;

        /**
         * Private constructor for creating a path element with the given
         * qualified name and index. Instead of using this constructor directly
         * the factory methods {@link #create(QName)} and {@link #create(QName, int)}
         * should be used.
         *
         * @param name  qualified name
         * @param index index
         */
        private PathElement(QName name, int index) {
            this.index = index;
            this.name = name;
        }

        /**
         * Creates a path element with the given qualified name.
         * The created path element does not contain an explicit index.
         * <p/>
         * If the specified name denotes a <i>special</i> path element (either
         * {@link Path#PARENT_ELEMENT}, {@link Path#CURRENT_ELEMENT} or
         * {@link Path#ROOT_ELEMENT}) then the associated constant is returned.
         *
         * @param name the name of the element
         * @return a path element
         * @throws IllegalArgumentException if the name is <code>null</code>
         */
        public static PathElement create(QName name) {
            if (name == null) {
                throw new IllegalArgumentException("name must not be null");
            } else if (name.equals(PARENT_ELEMENT.getName())) {
                return PARENT_ELEMENT;
            } else if (name.equals(CURRENT_ELEMENT.getName())) {
                return CURRENT_ELEMENT;
            } else if (name.equals(ROOT_ELEMENT.getName())) {
                return ROOT_ELEMENT;
            } else {
                return new NameElement(name, INDEX_UNDEFINED);
            }
        }

        /**
         * Same as {@link #create(QName)} except that an explicit index can be
         * specified.
         * <p/>
         * Note that an IllegalArgumentException will be thrown if the specified
         * name denotes a <i>special</i> path element (either
         * {@link Path#PARENT_ELEMENT}, {@link Path#CURRENT_ELEMENT} or
         * {@link Path#ROOT_ELEMENT}) since an explicit index is not allowed
         * in this context.
         *
         * @param name  the name of the element
         * @param index the 1-based index.
         * @return a path element
         * @throws IllegalArgumentException if the name is <code>null</code>,
         *                                  if the given index is less than 1
         *                                  or if name denoting a special path
         *                                  element and an index greater than 1
         *                                  have been specified.
         */
        public static PathElement create(QName name, int index) {
            if (index < INDEX_DEFAULT) {
                throw new IllegalArgumentException("index is 1-based.");
            } else if (name == null) {
                throw new IllegalArgumentException("name must not be null");
            } else if (name.equals(PARENT_ELEMENT.getName())
                    || name.equals(CURRENT_ELEMENT.getName())
                    || name.equals(ROOT_ELEMENT.getName())) {
                throw new IllegalArgumentException(
                        "special path elements (root, '.' and '..') can not have an explicit index");
            } else {
                return new NameElement(name, index);
            }
        }

        /**
         * Returns the qualified name of this path element.
         *
         * @return qualified name
         */
        public QName getName() {
            return name;
        }

        /**
         * Returns the 1-based index or 0 if no index was specified (which is
         * equivalent to specifying 1).
         *
         * @return Returns the 1-based index or 0 if no index was specified.
         */
        public int getIndex() {
            return index;
        }

        /**
         * Returns the normalized index of this path element, i.e. the index
         * is always equals or greater that {@link Path#INDEX_DEFAULT}.
         */
        public int getNormalizedIndex() {
            if (index == INDEX_UNDEFINED) {
                return INDEX_DEFAULT;
            } else {
                return index;
            }
        }

        /**
         * Returns the JCR name representation of this path element.
         * Note that strictly speaking the returned value is in fact
         * a JCR relative path instead of a JCR name, as it contains
         * the index value if the index is greater than one.
         *
         * @param resolver namespace resolver
         * @return JCR name representation of the path element
         * @throws NoPrefixDeclaredException if the namespace of the path
         *                                   element name can not be resolved
         */
        public String toJCRName(NamespaceResolver resolver)
                throws NoPrefixDeclaredException {
            StringBuffer sb = new StringBuffer();
            toJCRName(resolver, sb);
            return sb.toString();
        }

        /**
         * Appends the JCR name representation of this path element to the
         * given string buffer.
         *
         * @param resolver namespace resolver
         * @param buf      string buffer where the JCR name representation
         *                 should be appended to
         * @throws NoPrefixDeclaredException if the namespace of the path
         *                                   element name can not be resolved
         * @see #toJCRName(NamespaceResolver)
         */
        public void toJCRName(NamespaceResolver resolver, StringBuffer buf)
                throws NoPrefixDeclaredException {
            // name
            NameFormat.format(name, resolver, buf);
            // index
            int index = getIndex();
            /**
             * FIXME the [1] subscript should only be suppressed if the item
             * in question can't have same-name siblings.
             */
            //if (index > 0) {
            if (index > 1) {
                buf.append('[');
                buf.append(index);
                buf.append(']');
            }
        }

        /**
         * Returns a string representation of this path element. Note that
         * the path element name is expressed using the <code>{uri}name</code>
         * syntax. Use the {@link #toJCRName(NamespaceResolver) toJCRName}
         * method to get the prefixed string representation of the path element.
         *
         * @return string representation of the path element
         * @see Object#toString()
         */
        public String toString() {
            StringBuffer sb = new StringBuffer();
            // name
            sb.append(name.toString());
            // index
            int index = getIndex();
            if (index > INDEX_UNDEFINED) {
                sb.append('[');
                sb.append(index);
                sb.append(']');
            }
            return sb.toString();
        }

        /**
         * Parses the given path element string into a path element object.
         *
         * @param s path element string
         * @return path element object
         * @throws IllegalArgumentException if the given path element string
         *                                  is <code>null</code> or if its
         *                                  format is invalid
         */
        public static PathElement fromString(String s) throws IllegalArgumentException {
            if (s == null) {
                throw new IllegalArgumentException("null PathElement literal");
            }
            if (s.equals(RootElement.LITERAL)) {
                return ROOT_ELEMENT;
            } else if (s.equals(CurrentElement.LITERAL)) {
                return CURRENT_ELEMENT;
            } else if (s.equals(ParentElement.LITERAL)) {
                return PARENT_ELEMENT;
            }

            int pos = s.indexOf('[');
            if (pos == -1) {
                QName name = QName.valueOf(s);
                return new NameElement(name, INDEX_UNDEFINED);
            }
            QName name = QName.valueOf(s.substring(0, pos));
            int pos1 = s.indexOf(']');
            if (pos1 == -1) {
                throw new IllegalArgumentException("invalid PathElement literal: " + s + " (missing ']')");
            }
            try {
                int index = Integer.valueOf(s.substring(pos + 1, pos1)).intValue();
                if (index < 1) {
                    throw new IllegalArgumentException("invalid PathElement literal: " + s + " (index is 1-based)");
                }
                return new NameElement(name, index);
            } catch (Throwable t) {
                throw new IllegalArgumentException("invalid PathElement literal: " + s + " (" + t.getMessage() + ")");
            }
        }

        /**
         * Computes a hash code for this path element.
         *
         * @return hash code
         */
        public int hashCode() {
            int h = 17;
            h = 37 * h + normalizeIndex(index);
            h = 37 * h + name.hashCode();
            return h;
        }

        /**
         * Check for path element equality. Returns true if the given
         * object is a PathElement and contains the same name and index
         * as this one.
         *
         * @param obj the object to compare with
         * @return <code>true</code> if the path elements are equal
         */
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof PathElement) {
                PathElement other = (PathElement) obj;
                return name.equals(other.name)
                        && normalizeIndex(index) == normalizeIndex(other.index);
            }
            return false;
        }

        /**
         * Normalizes index value {@link Path#INDEX_UNDEFINED} to
         * {@link Path#INDEX_DEFAULT} for {@link #equals(Object)} and
         * {@link #hashCode()}.
         * @param index
         * @return normalized index
         */
        private int normalizeIndex(int index) {
            return index == Path.INDEX_UNDEFINED ? Path.INDEX_DEFAULT : index;
        }

        /**
         * Returns <code>true</code> if this element denotes the <i>root</i> element,
         * otherwise returns <code>false</code>.
         *
         * @return <code>true</code> if this element denotes the <i>root</i>
         *         element; otherwise <code>false</code>
         */
        abstract public boolean denotesRoot();

        /**
         * Returns <code>true</code> if this element denotes the <i>parent</i>
         * ('..') element, otherwise returns <code>false</code>.
         *
         * @return <code>true</code> if this element denotes the <i>parent</i>
         *         element; otherwise <code>false</code>
         */
        abstract public boolean denotesParent();

        /**
         * Returns <code>true</code> if this element denotes the <i>current</i>
         * ('.') element, otherwise returns <code>false</code>.
         *
         * @return <code>true</code> if this element denotes the <i>current</i>
         *         element; otherwise <code>false</code>
         */
        abstract public boolean denotesCurrent();

        /**
         * Returns <code>true</code> if this element represents a regular name
         * (i.e. neither root, '.' nor '..'), otherwise returns <code>false</code>.
         *
         * @return <code>true</code> if this element represents a regular name;
         *         otherwise <code>false</code>
         */
        abstract public boolean denotesName();

    }

    public static final class RootElement extends PathElement {
        // use a literal that is an illegal name character to avoid collisions
        static final String LITERAL = "*";

        private RootElement() {
            super(QName.ROOT, Path.INDEX_UNDEFINED);
        }

        /**
         * Returns false.
         *
         * @return false
         * @see Path.PathElement#denotesName()
         */
        public boolean denotesName() {
            return false;
        }

        /**
         * Returns true.
         *
         * @return true
         * @see Path.PathElement#denotesRoot()
         */
        public boolean denotesRoot() {
            return true;
        }

        /**
         * Returns false.
         *
         * @return false
         * @see Path.PathElement#denotesParent()
         */
        public boolean denotesParent() {
            return false;
        }

        /**
         * Returns false.
         *
         * @return false
         * @see Path.PathElement#denotesCurrent()
         */
        public boolean denotesCurrent() {
            return false;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * Returns <code>""</code>
         * @return <code>""</code>
         */
        public String toJCRName(NamespaceResolver resolver) {
            return "";
        }

        /**
         * {@inheritDoc}
         */
        public void toJCRName(NamespaceResolver resolver, StringBuffer buf) {
            // append empty string, i.e. nothing.
        }

        /**
         * @return {@link #LITERAL}
         * @see Object#toString()
         */
        public String toString() {
            return LITERAL;
        }
    }


    public static final class CurrentElement extends PathElement {
        static final String LITERAL = ".";

        private CurrentElement() {
            super(new QName(QName.NS_DEFAULT_URI, LITERAL), Path.INDEX_UNDEFINED);
        }

        /**
         * Returns false.
         *
         * @return false
         * @see Path.PathElement#denotesName()
         */
        public boolean denotesName() {
            return false;
        }

        /**
         * Returns false.
         *
         * @return false
         * @see Path.PathElement#denotesRoot()
         */
        public boolean denotesRoot() {
            return false;
        }

        /**
         * Returns false.
         *
         * @return false
         * @see Path.PathElement#denotesParent()
         */
        public boolean denotesParent() {
            return false;
        }

        /**
         * Returns true.
         *
         * @return true
         * @see Path.PathElement#denotesCurrent()
         */
        public boolean denotesCurrent() {
            return true;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * Returns <code>"."</code>
         * @return <code>"."</code>
         */
        public String toJCRName(NamespaceResolver resolver) {
            return LITERAL;
        }

        /**
         * {@inheritDoc}
         */
        public void toJCRName(NamespaceResolver resolver, StringBuffer buf) {
            buf.append(LITERAL);
        }

        /**
         * @return {@link #LITERAL}
         * @see Object#toString()
         */
        public String toString() {
            return LITERAL;
        }
    }

    public static final class ParentElement extends PathElement {
        static final String LITERAL = "..";

        private ParentElement() {
            super(new QName(QName.NS_DEFAULT_URI, LITERAL), Path.INDEX_UNDEFINED);
        }

        /**
         * Returns false.
         *
         * @return false
         * @see Path.PathElement#denotesName()
         */
        public boolean denotesName() {
            return false;
        }

        /**
         * Returns false.
         *
         * @return false
         * @see Path.PathElement#denotesRoot()
         */
        public boolean denotesRoot() {
            return false;
        }

        /**
         * Returns true.
         *
         * @return true
         * @see Path.PathElement#denotesParent()
         */
        public boolean denotesParent() {
            return true;
        }

        /**
         * Returns false.
         *
         * @return false
         * @see Path.PathElement#denotesCurrent()
         */
        public boolean denotesCurrent() {
            return false;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * Returns <code>".."</code>
         * @return <code>".."</code>
         */
        public String toJCRName(NamespaceResolver resolver) {
            return LITERAL;
        }

        /**
         * {@inheritDoc}
         */
        public void toJCRName(NamespaceResolver resolver, StringBuffer buf) {
            buf.append(LITERAL);
        }

        /**
         * @return {@link #LITERAL}
         * @see Object#toString()
         */
        public String toString() {
            return LITERAL;
        }
    }

    public static final class NameElement extends PathElement {

        private NameElement(QName name, int index) {
            super(name, index);
        }

        /**
         * Returns true.
         *
         * @return true
         * @see Path.PathElement#denotesName()
         */
        public boolean denotesName() {
            return true;
        }

        /**
         * Returns false.
         *
         * @return false
         * @see Path.PathElement#denotesRoot()
         */
        public boolean denotesRoot() {
            return false;
        }

        /**
         * Returns false.
         *
         * @return false
         * @see Path.PathElement#denotesParent()
         */
        public boolean denotesParent() {
            return false;
        }

        /**
         * Returns false.
         *
         * @return false
         * @see Path.PathElement#denotesCurrent()
         */
        public boolean denotesCurrent() {
            return false;
        }
    }
}
