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

import org.apache.jackrabbit.uuid.UUID;

/**
 * Node identifier. An instance of this class identifies a node using its UUID.
 * Once created a node identifier instance is immutable.
 */
public class NodeId extends ItemId {

    /** Serial version UID of this class. */
    static final long serialVersionUID = 7026219091360041109L;

    /** UUID of the identified node */
    private final UUID uuid;

    /**
     * Creates a node identifier instance for the identified node.
     *
     * @param uuid node UUID
     */
    public NodeId(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid can not be null");
        }
        this.uuid = uuid;
    }

    /**
     * Returns <code>true</code> as this class represents a node identifier,
     * not a property identifier.
     *
     * @return always <code>true</code>
     * @see ItemId#denotesNode()
     */
    public boolean denotesNode() {
        return true;
    }

    /**
     * Returns the UUID of the identified node.
     *
     * @return node UUID
     */
    public UUID getUUID() {
        return uuid;
    }

    /**
     * Returns a <code>NodeId</code> holding the value of the specified
     * string. The string must be in the format returned by the
     * <code>NodeId.toString()</code> method.
     *
     * @param s a <code>String</code> containing the <code>NodeId</code>
     *          representation to be parsed.
     * @return the <code>NodeId</code> represented by the argument
     * @throws IllegalArgumentException if the specified string can not be parsed
     *                                  as a <code>NodeId</code>.
     * @see #toString()
     */
    public static NodeId valueOf(String s) throws IllegalArgumentException {
        if (s == null) {
            throw new IllegalArgumentException("invalid NodeId literal");
        }
        return new NodeId(new UUID(s));
    }

    //-------------------------------------------< java.lang.Object overrides >

    /**
     * Compares node identifiers for equality.
     *
     * @param obj other object
     * @return <code>true</code> if the given object is a node identifier
     *         instance that identifies the same node as this identifier,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NodeId) {
            return uuid.equals(((NodeId) obj).uuid);
        }
        return false;
    }

    /**
     * Returns the node UUID.
     *
     * @return node UUID
     * @see Object#toString()
     */
    public String toString() {
        return uuid.toString();
    }

    /**
     * Returns the hash code of the node UUID. The computed hash code
     * is memorized for better performance.
     *
     * @return hash code
     * @see Object#hashCode()
     */
    public int hashCode() {
        // NodeId is immutable, we can store the computed hash code value
        if (hash == 0) {
            hash = uuid.hashCode();
        }
        return hash;
    }
}
