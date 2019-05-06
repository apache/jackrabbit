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

import javax.jcr.PathNotFoundException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

final class RootPath extends AbstractPath {

    /** Singleton instance */
    public static final RootPath ROOT_PATH = new RootPath();

    /** Serial version UID */
    private static final long serialVersionUID = 8621451607549214925L;

    /** Name of the root element */
    public static final Name NAME =
        NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, "");

    /** Hidden constructor */
    private RootPath() {
    }

    public Name getName() {
        return NAME;
    }

    /**
     * Returns <code>true</code> as this is the root path.
     *
     * @return <code>true</code>
     */
    @Override
    public boolean denotesRoot() {
        return true;
    }

    /**
     * Returns <code>false</code> as this is the root path.
     *
     * @return <code>false</code>
     */
    public boolean isIdentifierBased() {
        return false;
    }

    /**
     * Returns <code>true</code> as this is the root path.
     *
     * @return <code>true</code>
     */
    public boolean isAbsolute() {
        return true;
    }

    /**
     * Returns <code>true</code> as this is the root path.
     *
     * @return <code>true</code>
     */
    public boolean isCanonical() {
        return true;
    }

    /**
     * Returns <code>true</code> as this is the root path.
     *
     * @return <code>true</code>
     */
    public boolean isNormalized() {
        return true;
    }

    /**
     * Returns this path as this is the root path.
     *
     * @return root path
     */
    public Path getNormalizedPath() {
        return this;
    }

    /**
     * Returns this path as this is the root path.
     *
     * @return root path
     */
    public Path getCanonicalPath() {
        return this;
    }

    public Path getAncestor(int degree)
            throws IllegalArgumentException, PathNotFoundException {
        if (degree < 0) {
            throw new IllegalArgumentException(
                    "/.getAncestor(" + degree + ")");
        } else if (degree > 0) {
            throw new PathNotFoundException(
                    "/.getAncestor(" + degree + ")");
        } else {
            return this;
        }
    }

    /**
     * Returns zero as this is the root path.
     *
     * @return zero
     */
    public int getAncestorCount() {
        return 0;
    }

    /**
     * Returns one as this is the root path.
     *
     * @return one
     */
    public int getLength() {
        return 1;
    }

    /**
     * Returns zero as this is the root path.
     *
     * @return zero
     */
    public int getDepth() {
        return 0;
    }

    public Path subPath(int from, int to) throws IllegalArgumentException {
        if (from == 0 && to == 1) {
            return this;
        } else {
            throw new IllegalArgumentException(
                    "/.subPath(" + from + ", " + to + ")");
        }
    }

    public Element[] getElements() {
        return new Element[] { ROOT_PATH };
    }

    public Element getNameElement() {
        return ROOT_PATH;
    }

    public String getString() {
        return "{}";
    }

    //--------------------------------------------------------< Serializable >

    /** Returns the singleton instance of this class */
    public Object readResolve() {
        return ROOT_PATH;
    }

}
