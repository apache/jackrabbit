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

import org.apache.jackrabbit.uuid.UUID;

/**
 * Node identifier. An instance of this class identifies a node using its UUID.
 * Once created a node identifier instance is immutable.
 */
public class NodeId extends UUID implements ItemId {

    /**
     * The serial version UID.
     */
    private static final long serialVersionUID = 7348217305215708805L;

    /**
     * Creates a node identifier instance for the identified node.
     *
     * @param uuid node UUID
     */
    public NodeId(UUID uuid) {
        super(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    public NodeId() {
        this(UUID.randomUUID());
    }

    /**
     * Creates a node identifier from the given UUID string.
     *
     * @param uuid UUID string
     * @throws IllegalArgumentException if the UUID string is invalid
     */
    public NodeId(String uuid) throws IllegalArgumentException {
        super(uuid);
    }

    public NodeId(byte[] bytes) {
        super(bytes);
    }

    public NodeId(long msb, long lsb) {
        super(msb, lsb);
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
        return this;
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
        return new NodeId(s);
    }

}
