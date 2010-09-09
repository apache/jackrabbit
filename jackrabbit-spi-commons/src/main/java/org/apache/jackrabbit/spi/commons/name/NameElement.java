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

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Path.Element;

/**
 * Named path element with an optional index.
 */
final class NameElement extends AbstractElement {

    /** Serial version UID */
    private static final long serialVersionUID = -6655583285077651379L;

    /**
     * Name of the path element.
     */
    private final Name name;

    /**
     * Optional index of the path element. Set to {@link Path#INDEX_UNDEFINED}
     * (i.e. 0) if not explicitly specified, otherwise contains the 1-based
     * index.
     */
    private final int index;

    /**
     * Creates a named path element with the given name and optional index.
     *
     * @param name name of this element
     * @param index index of this element, or {@link Path#INDEX_UNDEFINED}
     */
    public NameElement(Name name, int index) {
        if (name != null && index >= 0) {
            this.index = index;
            this.name = name;
        } else {
            throw new IllegalArgumentException(
                    "new NameElement(" + name + ", " + index + ")");
        }
    }

    /**
     * Returns <code>true</code>, as this is a named path element.
     *
     * @return <code>true</code>
     */
    @Override
    public boolean denotesName() {
        return true;
    }

    /**
     * Returns the name of this path element.
     *
     * @param name of this element
     */
    public Name getName() {
        return name;
    }

    /**
     * Returns the 1-based index of this path element, or
     * {@link Path#INDEX_UNDEFINED} if the index is undefined.
     *
     * @param index of this element, or {@link Path#INDEX_UNDEFINED}
     */
    @Override
    public int getIndex() {
        return index;
    }

    /**
     * Returns the normalized index of this path element.
     *
     * @return normalized index of this element
     */
    @Override
    public int getNormalizedIndex() {
        if (index != Path.INDEX_UNDEFINED) {
            return index;
        } else {
            return Path.INDEX_DEFAULT;
        }
    }

    /**
     * Returns a string representation of this path element. The element
     * name is expressed using the <code>{uri}name</code> syntax and the
     * index is added as <code>[index]</code> if it's defined and greater
     * than one.
     *
     * @return string representation of this path element
     */
    public String getString() {
        if (index > Path.INDEX_DEFAULT) {
            return name + "[" + index + "]";
        } else {
            return name.toString();
        }
    }

    //--------------------------------------------------------------< Object >

    /**
     * Check for path element equality. Returns true if the given object is
     * a named path element and contains the same name and index as this one.
     *
     * @param that the object to compare with
     * @return <code>true</code> if the path elements are equal
     */
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        } else if (that instanceof Element) {
            Element element = (Element) that;
            return element.denotesName()
                && name.equals(element.getName())
                && getNormalizedIndex() == element.getNormalizedIndex();
        } else {
            return false;
        }
    }

    /**
     * Computes a hash code for this path element.
     *
     * @return hash code
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = 37 * h + getNormalizedIndex();
        h = 37 * h + name.hashCode();
        return h;
    }

}
