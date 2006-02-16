/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.name.QName;

/**
 * Property identifier. An instance of this class identifies a single
 * property using the UUID of the parent node and the qualified name of
 * the property. Once created a property identifier instance is immutable.
 */
public class PropertyId extends ItemId {

    /** Serial version UID of this class. */
    static final long serialVersionUID = -3726624437800567892L;

    /** ID of the parent node. */
    private final NodeId parentId;

    /** Qualified name of the property. */
    private final QName propName;

    /**
     * Creates a property identifier instance for the identified property.
     *
     * @param parentId the id of the parent node
     * @param propName qualified name of the property
     */
    public PropertyId(NodeId parentId, QName propName) {
        if (parentId == null) {
            throw new IllegalArgumentException("parentId can not be null");
        }
        if (propName == null) {
            throw new IllegalArgumentException("propName can not be null");
        }
        this.parentId = parentId;
        this.propName = propName;
    }

    /**
     * Returns <code>false</code> as this class represents a property
     * identifier, not a node identifier.
     *
     * @return always <code>false</code>
     * @see ItemId#denotesNode()
     */
    public boolean denotesNode() {
        return false;
    }

    /**
     * Returns the Id of the parent node.
     *
     * @return node Id
     */
    public NodeId getParentId() {
        return parentId;
    }

    /**
     * Returns the qualified name of the property.
     *
     * @return qualified name
     */
    public QName getName() {
        return propName;
    }

    /**
     * Returns a property identifier instance holding the value of the
     * specified string. The string must be in the format returned by the
     * {@link #toString() toString()} method of this class.
     *
     * @param s a <code>String</code> containing the <code>PropertyId</code>
     *          representation to be parsed.
     * @return the <code>PropertyId</code> represented by the argument
     * @throws IllegalArgumentException if the specified string can not be parsed
     *                                  as a <code>PropertyId</code>.
     * @see #toString()
     */
    public static PropertyId valueOf(String s) throws IllegalArgumentException {
        if (s == null) {
            throw new IllegalArgumentException("invalid PropertyId literal");
        }
        int i = s.indexOf('/');
        if (i == -1) {
            throw new IllegalArgumentException("invalid PropertyId literal");
        }
        String uuid = s.substring(0, i);
        QName name = QName.valueOf(s.substring(i + 1));

        return new PropertyId(NodeId.valueOf(uuid), name);
    }

    //-------------------------------------------< java.lang.Object overrides >

    /**
     * Compares property identifiers for equality.
     *
     * @param obj other object
     * @return <code>true</code> if the given object is a property identifier
     *         instance that identifies the same property as this identifier,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PropertyId) {
            PropertyId other = (PropertyId) obj;
            return parentId.equals(other.parentId)
                    && propName.equals(other.propName);
        }
        return false;
    }

    /**
     * Returns a string representation of this property identifier.
     *
     * @return property identifier string
     * @see Object#toString()
     */
    public String toString() {
        return parentId + "/" + propName;
    }

    /**
     * Returns the hash code of this property identifier. The hash code
     * is computed from the parent node UUID and the property name. The
     * hash code is memorized for performance.
     *
     * @return hash code
     * @see Object#hashCode()
     */
    public int hashCode() {
        // PropertyId is immutable, we can store the computed hash code value
        int h = hash;
        if (h == 0) {
            h = 17;
            h = 37 * h + parentId.hashCode();
            h = 37 * h + propName.hashCode();
            hash = h;
        }
        return h;
    }
}
