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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.uuid.UUID;

import java.io.Serializable;

/**
 * Identifies a <code>NodeReferences</code> object.
 */
public class NodeReferencesId implements Serializable {

    /** Serialization UID of this class. */
    static final long serialVersionUID = -3819311769214730025L;

    /**
     * The id of the target node.
     */
    private final NodeId targetId;

    /**
     * Creates a new instance of this class. Takes the uuid of the target node
     * as parameter.
     *
     * @param uuid uuid of target node
     * @throws IllegalArgumentException if <code>uuid</code> is <code>null</code>.
     */
    public NodeReferencesId(UUID uuid) {
        targetId = new NodeId(uuid);
    }

    /**
     * Creates a new instance of this class. Takes the id of the target node
     * as parameter.
     *
     * @param targetId id of target node
     * @throws IllegalArgumentException if <code>targetId</code> is <code>null</code>.
     */
    public NodeReferencesId(NodeId targetId) {
        if (targetId == null) {
            throw new IllegalArgumentException("targetId must not be null");
        }
        this.targetId = targetId;
    }

    /**
     * Returns the id of the target node.
     *
     * @return the id of the target node.
     */
    public NodeId getTargetId() {
        return targetId;
    }

    /**
     * Returns a <code>NodeReferencesId</code> holding the value of the specified
     * string. The string must be in the format returned by the
     * <code>NodeReferencesId.toString()</code> method.
     *
     * @param s a <code>String</code> containing the <code>NodeReferencesId</code>
     *          representation to be parsed.
     * @return the <code>NodeReferencesId</code> represented by the argument
     * @throws IllegalArgumentException if the specified string can not be parsed
     *                                  as a <code>NodeReferencesId</code>.
     * @see #toString()
     */
    public static NodeReferencesId valueOf(String s) throws IllegalArgumentException {
        if (s == null) {
            throw new IllegalArgumentException("invalid NodeReferencesId literal");
        }
        return new NodeReferencesId(NodeId.valueOf(s));
    }

    /**
     * Returns the same as <code>this.getTargetId().toString()</code>.
     *
     * @return the same as <code>this.getTargetId().toString()</code>.
     */
    public String toString() {
        return targetId.toString();
    }

    /**
     * Returns the same as <code>this.getTargetId().hashCode()</code>.
     *
     * @return the same as <code>this.getTargetId().hashCode()</code>.
     */
    public int hashCode() {
        return targetId.hashCode();
    }

    /**
     * @inheritDoc
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NodeReferencesId) {
            return targetId.getUUID().equals(((NodeReferencesId) obj).targetId.getUUID());
        }
        return false;
    }
}
