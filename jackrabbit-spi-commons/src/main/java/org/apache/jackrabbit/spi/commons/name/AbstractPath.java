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
abstract class AbstractPath implements Path {

    /** Serial version UID */
    private static final long serialVersionUID = 3018771833963770499L;

    protected Path createPath(Path parent, Element element)
            throws RepositoryException {
        if (element.denotesCurrent()) {
            return new CurrentPath(parent);
        } else if (element.denotesParent()) {
            return new ParentPath(parent);
        } else if (element.denotesName()) {
            return new NamePath(parent, element);
        } else {
            throw new RepositoryException("Unknown path element: " + element);
        }
    }

    public final Path resolve(Path relative) throws RepositoryException {
        if (relative.isAbsolute()) {
            return relative;
        } else {
            Element element = relative.getNameElement();
            int n = relative.getLength();
            if (n > 1) {
                Path parent = relative.subPath(0, n - 1);
                return createPath(resolve(parent), element);
            } else {
                return createPath(this, element);
            }
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

                while (bi < b.length) {
                    path = createPath(path, b[bi++]);
                }

                if (path != null) {
                    return path;
                } else {
                    return new CurrentPath(null);
                }
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
                && denotesIdentifier() == other.denotesIdentifier()) {
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
                && denotesIdentifier() == other.denotesIdentifier()) {
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
