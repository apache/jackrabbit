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
package org.apache.jackrabbit.jcr.core.nodetype;

import org.apache.jackrabbit.jcr.core.QName;

import java.io.Serializable;
import java.util.TreeSet;

/**
 * <code>NodeDefId</code> uniquely identifies a <code>ChildNodeDef</code> in the
 * node type registry.
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.3 $
 */
public class NodeDefId implements Serializable {

    static final long serialVersionUID = 7020286139887664713L;

    private final int id;

    public NodeDefId(ChildNodeDef def) {
	if (def == null) {
	    throw new IllegalArgumentException("ChildNodeDef argument can not be null");
	}
	StringBuffer sb = new StringBuffer();

	sb.append(def.getDeclaringNodeType().toString());
	sb.append('/');
	if (def.getName() == null) {
	    sb.append('*');
	} else {
	    sb.append(def.getName().toString());
	}
	sb.append('/');
	if (def.getDefaultPrimaryType() != null) {
	    sb.append(def.getDefaultPrimaryType());
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

    public String toString() {
	return Integer.toString(id);
    }

    public int hashCode() {
	return id;
    }
}
