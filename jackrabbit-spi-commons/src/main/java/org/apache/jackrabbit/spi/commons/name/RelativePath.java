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

abstract class RelativePath extends AbstractPath {

    /** Serial version UID */
    private static final long serialVersionUID = 5707676677044863127L;

    protected final Path parent;

    private final boolean absolute;

    private final boolean identifier;

    private final int depth;

    private final int length;

    protected RelativePath(Path parent) {
        this.parent = parent;
        if (parent != null) {
            this.absolute = parent.isAbsolute();
            this.identifier = parent.isIdentifierBased();
            this.depth = parent.getDepth() + getDepthModifier();
            this.length = parent.getLength() + 1;
        } else {
            this.absolute = false;
            this.identifier = false;
            this.depth = getDepthModifier();
            this.length = 1;
        }
    }

    protected abstract int getDepthModifier();

    protected abstract Path getParent() throws RepositoryException;

    protected abstract String getElementString();

    public final boolean isIdentifierBased() {
        return identifier;
    }

    public final boolean isAbsolute() {
        return absolute;
    }

    public final Path getAncestor(int degree) throws RepositoryException {
        if (degree < 0) {
            throw new IllegalArgumentException(
                    "Invalid ancestor degree " + degree);
        } else if (degree == 0) {
            return getNormalizedPath();
        } else {
            return getParent().getAncestor(degree - 1);
        }
    }

    public final int getAncestorCount() {
        if (absolute) {
            return depth;
        } else {
            return -1;
        }
    }

    public final int getDepth() {
        return depth;
    }

    public final int getLength() {
        return length;
    }

    public final Path subPath(int from, int to) {
        if (from < 0 || length < to || to <= from) {
            throw new IllegalArgumentException(
                    this + ".subPath(" + from + ", " + to + ")");
        } else if (from == 0 && to == length) {
            // this is only case where parent can be null (from = 0, to = 1)
            return this;
        } else if (to < length) {
            return parent.subPath(from, to);
        } else if (from < to - 1) {
            return parent.subPath(from, to - 1).resolve(getNameElement());
        } else {
            return getLastElement();
        }
    }

    public final Element[] getElements() {
        Element[] elements = new Element[length];
        Path path = this;
        for (int i = 1; i <= length; i++) {
            elements[length - i] = path.getNameElement();
            path = path.getFirstElements();
        }
        return elements;
    }

    /**
     * Returns the first elements of this path.
     *
     * @return first elements of this path, or <code>null</code>
     */
    @Override
    public Path getFirstElements() {
        return parent;
    }

    public String getString() {
        if (parent != null) {
            return parent.getString() + Path.DELIMITER + getElementString();
        } else {
            return getElementString();
        }
    }

    //--------------------------------------------------------------< Object >

    public boolean equals(Object that) {
        if (this == that) {
            return true;
        } else if (that instanceof RelativePath) {
            RelativePath path = (RelativePath) that;
            if (parent != null) {
                return parent.equals(path.parent);
            } else {
                return path.parent == null;
            }
        } else {
            return false;
        }
    }

    public int hashCode() {
        if (parent != null) {
            return parent.hashCode();
        } else {
            return 17;
        }
    }

}
