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
import org.apache.jackrabbit.spi.Path.Element;

/**
 * Identifier path element.
 */
final class IdentifierElement extends AbstractElement {

    /** Serial version UID */
    private static final long serialVersionUID = 1508076769330719028L;

    /**
     * Identifier of the path element.
     */
    private final String identifier;

    /**
     * Creates a path element from the given identifier.
     *
     * @param identifier identifier of this element
     */
    public IdentifierElement(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns <code>true</code>, as this is an identifier path element.
     *
     * @return <code>true</code>
     */
    @Override
    public boolean denotesIdentifier() {
        return true;
    }

    /**
     * Returns <code>null</code> as identifier path elements have no name.
     *
     * @return <code>null</code>
     */
    public Name getName() {
        return null;
    }

    /**
     * Returns a string representation of this path element. The identifier
     * is expressed using the <code>[identifier]</code> syntax.
     *
     * @return string representation of this path element
     */
    public String getString() {
        return "[" + identifier + "]";
    }

    //--------------------------------------------------------------< Object >

    /**
     * Check for path element equality. Returns true if the given object is
     * an identifier element and contains the same identifier as this one.
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
            return element.denotesIdentifier()
                && getString().equals(element.getString());
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
        return identifier.hashCode();
    }

}
