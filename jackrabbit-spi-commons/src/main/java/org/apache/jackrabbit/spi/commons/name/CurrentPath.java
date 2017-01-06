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
 * A relative path whose last element is the current path element, i.e. ".".
 */
final class CurrentPath extends RelativePath {

    /** Serial version UID */
    private static final long serialVersionUID = 1729196441091297231L;

    /** The current path "." */
    public static final CurrentPath CURRENT_PATH = new CurrentPath(null);

    /** Name of the current element */
    public static final Name NAME =
        NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, ".");

    public CurrentPath(Path parent) {
        super(parent);
    }

    protected int getDepthModifier() {
        return 0;
    }

    protected Path getParent() throws RepositoryException {
        if (parent != null) {
            return parent.getAncestor(1);
        } else {
            return new ParentPath(null);
        }
    }

    protected String getElementString() {
        return NAME.getLocalName();
    }

    public Name getName() {
        return NAME;
    }

    /**
     * Returns <code>true</code> as this path ends in the current element.
     *
     * @return <code>true</code>
     */
    @Override
    public boolean denotesCurrent() {
        return true;
    }

    /**
     * Returns <code>false</code> as a path with a "." element is
     * never canonical.
     *
     * @return <code>false</code>
     */
    public boolean isCanonical() {
        return false;
    }

    public boolean isNormalized() {
        return parent == null;
    }

    public Path getNormalizedPath() throws RepositoryException {
        if (parent != null) {
            return parent.getNormalizedPath();
        } else {
            return this;
        }
    }

    public Path getCanonicalPath() throws RepositoryException {
        if (parent != null) {
            return parent.getCanonicalPath();
        } else {
            throw new RepositoryException(
                    "There is no canonical representation of .");
        }
    }

    /**
     * Returns the current path ".".
     *
     * @return current path
     */
    @Override
    public AbstractPath getLastElement() {
        return CURRENT_PATH;
    }

    //--------------------------------------------------------------< Object >

    @Override
    public final boolean equals(Object that) {
        if (this == that) {
            return true;
        } else if (that instanceof Path) {
            Path path = (Path) that;
            return path.denotesCurrent() && super.equals(that);
        } else {
            return false;
        }
    }

    @Override
    public final int hashCode() {
        return super.hashCode() + 1;
    }

}
