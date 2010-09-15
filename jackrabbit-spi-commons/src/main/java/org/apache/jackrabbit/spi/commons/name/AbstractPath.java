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
package org.apache.jackrabbit.spi.commons.name;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Path;

/**
 * Abstract base class for paths.
 */
abstract class AbstractPath implements Path, Path.Element {

    /** Serial version UID */
    private static final long serialVersionUID = 3018771833963770499L;

    /**
     * Returns {@link Path#INDEX_UNDEFINED}, except when overridden by the
     * {@link NamePath} subclass.
     *
     * @return {@link Path#INDEX_UNDEFINED}
     */
    public int getIndex() {
        return INDEX_UNDEFINED;
    }

    /**
     * Returns {@link Path#INDEX_DEFAULT}, except when overridden by the
     * {@link NamePath} subclass.
     *
     * @return {@link Path#INDEX_DEFAULT}
     */
    public int getNormalizedIndex() {
        return INDEX_DEFAULT;
    }

    /**
     * Returns <code>null</code>, except when overridden by the
     * {@link IdentifierPath} subclass.
     *
     * @return <code>null</code>
     */
    public String getIdentifier() {
        return null;
    }

    /**
     * Returns <code>false</code>, except when overridden by the
     * {@link RootPath} subclass.
     *
     * @return <code>false</code>
     */
    public boolean denotesRoot() {
        return false;
    }

    /**
     * Returns <code>false</code>, except when overridden by the
     * {@link IdentifierPath} subclass.
     *
     * @return <code>false</code>
     */
    public boolean denotesIdentifier() {
        return false;
    }

    /**
     * Returns <code>false</code>, except when overridden by the
     * {@link ParentPath} subclass.
     *
     * @return <code>false</code>
     */
    public boolean denotesParent() {
        return false;
    }

    /**
     * Returns <code>false</code>, except when overridden by the
     * {@link CurrentPath} subclass.
     *
     * @return <code>false</code>
     */
    public boolean denotesCurrent() {
        return false;
    }

    /**
     * Returns <code>false</code>, except when overridden by the
     * {@link NamePath} subclass.
     *
     * @return <code>false</code>
     */
    public boolean denotesName() {
        return false;
    }

    public Element getNameElement() {
        return getLastElement();
    }

    /**
     * Returns this path, except when overridden by the {@link RelativePath}
     * subclasses.
     *
     * @return this path
     */
    public AbstractPath getLastElement() {
        return this;
    }

    /**
     * Returns <code>null</code>, except when overridden by the
     * {@link RelativePath} subclass.
     *
     * @return <code>null</code>
     */
    public Path getFirstElements() {
        return null;
    }

    public final Path resolve(Element element) {
        if (element.denotesName()) {
            return new NamePath(this, element.getName(), element.getIndex());
        } else if (element.denotesParent()) {
            if (isAbsolute() && getDepth() == 0) {
                throw new IllegalArgumentException(
                        "An absolute paths with negative depth is not allowed");
            }
            return new ParentPath(this);
        } else if (element.denotesCurrent()) {
            return new CurrentPath(this);
        } else if (element.denotesRoot()) {
            return RootPath.ROOT_PATH;
        } else if (element.denotesIdentifier()) {
            return new IdentifierPath(element.getIdentifier());
        } else {
            throw new IllegalArgumentException(
                    "Unknown path element type: " + element);
        }
    }

    public final Path resolve(Path relative) {
        if (relative.isAbsolute()) {
            return relative;
        } else if (relative.getLength() > 1) {
            Path first = relative.getFirstElements();
            Path last = relative.getLastElement();
            return resolve(first).resolve(last);
        } else if (relative.denotesCurrent()) {
            return new CurrentPath(this);
        } else if (relative.denotesParent()) {
            return new ParentPath(this);
        } else if (relative.denotesName()) {
            return new NamePath(this, relative.getName(), relative.getIndex());
        } else {
            throw new IllegalArgumentException(
                    "Unknown path type: " + relative);
        }
    }

    /**
     * Computes the relative path from this path to the given other path.
     * Both paths must be absolute.
     *
     * @param other other path
     * @return relative path
     * @throws RepositoryException if the relative path can not be computed
     */
    public final Path computeRelativePath(Path other)
            throws RepositoryException {
        if (other != null && isAbsolute() && other.isAbsolute()) {
            Element[] a = getElements();
            Element[] b = other.getElements();

            // The first elements (root or identifier) must be equal
            if (a.length > 0 && b.length > 0 && a[0].equals(b[0])) {
                int ai = 1;
                int bi = 1;

                while (ai < a.length && bi < b.length) {
                    if (a[ai].equals(b[bi])) {
                        ai++;
                        bi++;
                    } else if (a[ai].denotesCurrent()) {
                        ai++;
                    } else if (b[bi].denotesCurrent()) {
                        bi++;
                    } else {
                        break;
                    }
                }

                Path path = null;

                while (ai < a.length) {
                    if (a[ai].denotesName()) {
                        path = new ParentPath(path);
                        ai++;
                    } else if (a[ai].denotesCurrent()) {
                        ai++;
                    } else {
                        throw new RepositoryException(
                                "Unexpected path element: " + a[ai]);
                    }
                }

                if (path == null) {
                    path = new CurrentPath(null);
                }

                while (bi < b.length) {
                    path = path.resolve(b[bi++]);
                }

                return path;
            }
        }
        throw new RepositoryException(
                "No relative path from " + this  + " to " + other);
    }

    /**
     * Determines if this path is equivalent to the given other path by
     * comparing the normalized paths for equality.
     *
     * @param other other path
     * @return <code>true</code> if this path is equivalent to the other path,
     *         <code>false</code> otherwise
     * @throws IllegalArgumentException if the other path is <code>null</code>
     * @throws RepositoryException if an error occurs
     */
    public final boolean isEquivalentTo(Path other)
            throws IllegalArgumentException, RepositoryException {
        if (other != null) {
            return getNormalizedPath().equals(other.getNormalizedPath());
        } else {
            throw new IllegalArgumentException(
                    this + ".isEquivalentTo(" + other + ")");
        }
    }

    /**
     * Determines if this path is a ancestor of the given other path
     * by comparing the depths of the paths and checking if the corresponding
     * ancestor of the given other path is equivalent to this path.
     *
     * @param other other path
     * @return <code>true</code> if this path is an ancestor of the other path,
     *         <code>false</code> otherwise
     * @throws IllegalArgumentException if the other path is <code>null</code>,
     *                                  or relative when this path is absolute,
     *                                  or vice versa
     * @throws RepositoryException if an error occurs
     */
    public final boolean isAncestorOf(Path other)
            throws IllegalArgumentException, RepositoryException {
        if (other != null
                && isAbsolute() == other.isAbsolute()
                && isIdentifierBased() == other.isIdentifierBased()) {
            int d = other.getDepth() - getDepth();
            return d > 0 && isEquivalentTo(other.getAncestor(d));
        } else {
            throw new IllegalArgumentException(
                    this + ".isAncestorOf(" + other + ")");
        }
    }

    /**
     * Determines if this path is a descendant of the given other path
     * by comparing the depths of the paths and checking if the corresponding
     * ancestor of this path is equivalent to the given other path.
     *
     * @param other other path
     * @return <code>true</code> if this path is a descendant of the other path,
     *         <code>false</code> otherwise
     * @throws IllegalArgumentException if the other path is <code>null</code>,
     *                                  or relative when this path is absolute,
     *                                  or vice versa
     * @throws RepositoryException if an error occurs
     */
    public final boolean isDescendantOf(Path other)
            throws IllegalArgumentException, RepositoryException {
        if (other != null
                && isAbsolute() == other.isAbsolute()
                && isIdentifierBased() == other.isIdentifierBased()) {
            int d = getDepth() - other.getDepth();
            return d > 0 && getAncestor(d).isEquivalentTo(other);
        } else {
            throw new IllegalArgumentException(
                    this + ".isDescendantOf(" + other + ")");
        }
    }

    //--------------------------------------------------------------< Object >

    /**
     * Returns the string representation of this path.
     *
     * @return path string
     */
    public final String toString() {
        return getString();
    }

}
