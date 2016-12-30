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

/**
 * A relative path whose last element is the parent path element, i.e. "..".
 */
final class ParentPath extends RelativePath {

    /** Serial version UID */
    private static final long serialVersionUID = -688611157827116290L;

    /** The parent path ".." */
    public static final ParentPath PARENT_PATH = new ParentPath(null);

    /** Name of the parent element */
    public static final Name NAME =
        NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, "..");

    public ParentPath(Path parent) {
        super(parent);
    }

    protected int getDepthModifier() {
        return -1;
    }

    protected Path getParent() throws RepositoryException {
        if (isNormalized()) {
            return new ParentPath(this);
        } else {
            return parent.getAncestor(2);
        }
    }

    protected String getElementString() {
        return NAME.getLocalName();
    }

    public Name getName() {
        return NAME;
    }

    /**
     * Returns <code>true</code> as this path ends in the parent element.
     *
     * @return <code>true</code>
     */
    @Override
    public boolean denotesParent() {
        return true;
    }

    /**
     * Returns <code>false</code> as a path with a ".." element is
     * never canonical.
     *
     * @return <code>false</code>
     */
    public boolean isCanonical() {
        return false;
    }

    public boolean isNormalized() {
        return parent == null
            || (parent.isNormalized() && parent.denotesParent());
    }

    public Path getNormalizedPath() throws RepositoryException {
        if (isNormalized()) {
            return this;
        } else {
            // parent is guaranteed to be !null
            Path normalized = parent.getNormalizedPath();
            if (normalized.denotesParent()) {
                return new ParentPath(normalized); // special case: ../..
            } else if (normalized.denotesCurrent()) {
                return new ParentPath(null); // special case: ./..
            } else {
                return normalized.getAncestor(1);
            }
        }
    }

    public Path getCanonicalPath() throws RepositoryException {
        if (parent != null) {
            return parent.getCanonicalPath().getAncestor(1);
        } else {
            throw new RepositoryException(
                    "There is no canonical representation of ..");
        }
    }

    /**
     * Returns the parent path "..".
     *
     * @return parent path
     */
    @Override
    public AbstractPath getLastElement() {
        return PARENT_PATH;
    }

    //--------------------------------------------------------------< Object >

    @Override
    public final boolean equals(Object that) {
        if (this == that) {
            return true;
        } else if (that instanceof Path) {
            Path path = (Path) that;
            return path.denotesParent() && super.equals(that);
        } else {
            return false;
        }
    }

    @Override
    public final int hashCode() {
        return super.hashCode() + 2;
    }

}
