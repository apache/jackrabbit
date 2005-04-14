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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.core.QName;

import java.io.Serializable;
import java.util.TreeSet;

/**
 * <code>NodeDefId</code> uniquely identifies a <code>NodeDef</code> in the
 * node type registry.
 */
public class NodeDefId implements Serializable {

    /**
     * the serial number
     */
    static final long serialVersionUID = 7020286139887664713L;

    /**
     * the internal id
     */
    private final int id;

    /**
     * Creates a new NodeDefId from the given NodeDef
     * @param def
     */
    NodeDefId(NodeDefImpl def) {
        if (def == null) {
            throw new IllegalArgumentException("NodeDef argument can not be null");
        }
        // build key (format: <declaringNodeType>/<name>/<requiredPrimaryTypes>)
        StringBuffer sb = new StringBuffer();

        sb.append(def.getDeclaringNodeType().toString());
        sb.append('/');
        if (def.definesResidual()) {
            sb.append('*');
        } else {
            sb.append(def.getName().toString());
        }
        sb.append('/');
        // set of required node type names, sorted in ascending order
        TreeSet set = new TreeSet();
        QName[] names = def.getRequiredPrimaryTypes();
        for (int i = 0; i < names.length; i++) {
            set.add(names[i]);
        }
        sb.append(set.toString());

        id = sb.toString().hashCode();
    }

    /**
     * Private constructor that creates an NodeDefId from an internal id
     * @param id
     */
    private NodeDefId(int id) {
        this.id = id;
    }

    /**
     * Returns a <code>NodeDefId</code> holding the value of the specified
     * string. The string must be in the format returned by the
     * <code>NodeDefId.toString()</code> method.
     *
     * @param s a <code>String</code> containing the <code>NodeDefId</code>
     *          representation to be parsed.
     * @return the <code>NodeDefId</code> represented by the argument
     * @throws IllegalArgumentException if the specified string can not be parsed
     *                                  as a <code>NodeDefId</code>.
     * @see #toString()
     */
    public static NodeDefId valueOf(String s) {
        if (s == null) {
            throw new IllegalArgumentException("invalid NodeDefId literal");
        }
        return new NodeDefId(Integer.parseInt(s));
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NodeDefId) {
            NodeDefId other = (NodeDefId) obj;
            return id == other.id;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return Integer.toString(id);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        // id is already the computed hash code
        return id;
    }
}
