/*
 * Copyright 2002-2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.jcr.core;

/**
 * <code>NodeId</code> uniquely identifies a node in the repository.
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.10 $
 */
public class NodeId extends ItemId {

    static final long serialVersionUID = 7026219091360041109L;

    private String uuid;

    public NodeId(String uuid) {
	if (uuid == null) {
	    throw new IllegalArgumentException("uuid can not be null");
	}
	this.uuid = uuid;
    }

    /**
     * @see ItemId#denotesNode
     */
    public boolean denotesNode() {
	return true;
    }

    public String getUUID() {
	return uuid;
    }

    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj instanceof NodeId) {
	    NodeId other = (NodeId) obj;
	    return uuid.equals(other.uuid);
	}
	return false;
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
    public static NodeId valueOf(String s) {
	if (s == null) {
	    throw new IllegalArgumentException("invalid NodeId literal");
	}
	return new NodeId(s);
    }

    public String toString() {
	return uuid;
    }

    public int hashCode() {
	// NodeId is immutable, we can store the computed hash code value
	if (hash == 0) {
	    hash = 1609 * uuid.hashCode();
	}
	return hash;
    }
}
