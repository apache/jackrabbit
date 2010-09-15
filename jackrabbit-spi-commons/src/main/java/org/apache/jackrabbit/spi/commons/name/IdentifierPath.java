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

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

final class IdentifierPath extends AbstractPath {

    /** Serial version UID */
    private static final long serialVersionUID = 1602959709588338642L;

    private final String identifier;

    public IdentifierPath(String identifier) {
        assert identifier != null;
        this.identifier = identifier;
    }

    /**
     * Returns <code>null</code> as an identifier element has no name.
     *
     * @return <code>null</code>
     */
    public Name getName() {
        return null;
    }

    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns <code>true</code> as this is an identifier-based path.
     *
     * @return <code>true</code>
     */
    @Override
    public boolean denotesIdentifier() {
        return true;
    }

    /**
     * Returns <code>true</code> as this is an identifier-based path.
     *
     * @return <code>true</code>
     */
    public boolean isIdentifierBased() {
        return true;
    }

    /**
     * Returns <code>true</code> as an identifier-based path with no other
     * elements is absolute.
     *
     * @return <code>true</code>
     */
    public boolean isAbsolute() {
        return true;
    }

    /**
     * Returns <code>true</code> as an identifier-based path with no other
     * elements is canonical.
     *
     * @return <code>true</code>
     */
    public boolean isCanonical() {
        return true;
    }

    /**
     * Returns <code>false</code> as an identifier-based path is never
     * normalized.
     *
     * @return <code>false</code>
     */
    public boolean isNormalized() {
        return false;
    }

    public Path getNormalizedPath() throws RepositoryException {
        throw new RepositoryException(
                "Cannot normalize the identifier-based path " + this);
    }

    public Path getCanonicalPath() {
        return this;
    }

    public Path getAncestor(int degree)
            throws IllegalArgumentException, RepositoryException {
        if (degree < 0) {
            throw new IllegalArgumentException(
                    this + ".getAncestor(" + degree + ")");
        } else if (degree > 0) {
            throw new RepositoryException(
                    "Cannot construct ancestor path from an identifier");
        } else {
            return this;
        }
    }

    public int getAncestorCount() {
        return 0;
    }

    public int getLength() {
        return 1;
    }

    public int getDepth() {
        return 0;
    }

    public Path subPath(int from, int to) throws IllegalArgumentException {
        if (from == 0 && to == 1) {
            return this;
        } else {
            throw new IllegalArgumentException(
                    this + ".subPath(" + from + ", " + to + ")");
        }
    }

    public Element[] getElements() {
        return new Element[] { getNameElement() };
    }

    public String getString() {
        return "[" + identifier + "]";
    }

    //--------------------------------------------------------------< Object >

    public final boolean equals(Object that) {
        if (this == that) {
            return true;
        } else if (that instanceof Path) {
            Path path = (Path) that;
            return path.denotesIdentifier()
                && identifier.equals(path.getIdentifier());
        } else {
            return false;
        }
    }

    public final int hashCode() {
        return identifier.hashCode();
    }

}
