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
package org.apache.jackrabbit.core.id;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

/**
 * Property identifier. An instance of this class identifies a single
 * property using the UUID of the parent node and the name of
 * the property. Once created a property identifier instance is immutable.
 */
public class PropertyId implements ItemId {

    /** Serial version UID of this class. */
    static final long serialVersionUID = 1118783735407446009L;

    /** id of the parent node. */
    private final NodeId parentId;

    /** Name of the property. */
    private final Name propName;

    /** the precalculated hash code */
    private final int hashCode;

    /**
     * Creates a property identifier instance for the identified property.
     *
     * @param parentId the id of the parent node
     * @param propName Name of the property
     */
    public PropertyId(NodeId parentId, Name propName) {
        if (parentId == null) {
            throw new IllegalArgumentException("parentId can not be null");
        }
        if (propName == null) {
            throw new IllegalArgumentException("propName can not be null");
        }
        this.parentId = parentId;
        this.propName = propName;

        int h = 17;
        h = 37 * h + parentId.hashCode();
        h = 37 * h + propName.hashCode();
        this.hashCode = h;
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
     * Returns the identifier of the parent node.
     *
     * @return id of parent node
     */
    public NodeId getParentId() {
        return parentId;
    }

    /**
     * Returns the <code>Name</code> of the property.
     *
     * @return <code>Name</code> of the property.
     */
    public Name getName() {
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
        Name name = NameFactoryImpl.getInstance().create(s.substring(i + 1));

        return new PropertyId(NodeId.valueOf(uuid), name);
    }

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     *
     * Returns the same as <code>this.getParentId() + "/" + this.getName()</code>
     */
    public String toString() {
        return parentId + "/" + propName;
    }

    /**
     * {@inheritDoc}
     *
     * Returns the hash code of this property identifier. The hash code
     * is computed from the parent node id and the property name.
     */
    public int hashCode() {
        return hashCode;
    }
}
