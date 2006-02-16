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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.uuid.UUID;

/**
 * Identifies a <code>NodeReferences</code> object.
 */
public class NodeReferencesId extends NodeId {

    /**
     * Create a new instance of this class. Takes a UUID as parameter.
     *
     * @param uuid uuid of target node
     */
    public NodeReferencesId(UUID uuid) {
        super(uuid);
    }

    /**
     * Create a new instance of this class. Takes a id as parameter.
     *
     * @param id the id of target node
     */
    public NodeReferencesId(NodeId id) {
        super(id == null ? null : id.getUUID());
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
    public static NodeId valueOf(String s) throws IllegalArgumentException {
        if (s == null) {
            throw new IllegalArgumentException("invalid NodeReferencesId literal");
        }
        return new NodeReferencesId(NodeId.valueOf(s));
    }

}
