/*
 * Copyright 2004 The Apache Software Foundation.
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

import java.io.Serializable;

/**
 * <code>PropDefId</code> uniquely identifies a <code>PropDef</code> in the
 * node type registry.
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.2 $
 */
public class PropDefId implements Serializable {

    static final long serialVersionUID = 3675238890036653593L;

    private final int id;

    public PropDefId(PropDef def) {
	if (def == null) {
	    throw new IllegalArgumentException("PropDef argument can not be null");
	}
	// build key (format: <declaringNodeType>/<name>/<requiredType>/<multiple>)
	StringBuffer sb = new StringBuffer();

	sb.append(def.getDeclaringNodeType().toString());
	sb.append('/');
	if (def.getName() == null) {
	    sb.append('*');
	} else {
	    sb.append(def.getName().toString());
	}
	sb.append('/');
	sb.append(def.getRequiredType());
	sb.append('/');
	sb.append(def.isMultiple() ? 1 : 0);

	id = sb.toString().hashCode();
    }

    private PropDefId(int id) {
	this.id = id;
    }

    /**
     * Returns a <code>PropDefId</code> holding the value of the specified
     * string. The string must be in the format returned by the
     * <code>PropDefId.toString()</code> method.
     *
     * @param s a <code>String</code> containing the <code>PropDefId</code>
     *          representation to be parsed.
     * @return the <code>PropDefId</code> represented by the argument
     * @throws IllegalArgumentException if the specified string can not be parsed
     *                                  as a <code>PropDefId</code>.
     * @see #toString()
     */
    public static PropDefId valueOf(String s) {
	if (s == null) {
	    throw new IllegalArgumentException("invalid PropDefId literal");
	}
	return new PropDefId(Integer.parseInt(s));
    }

    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj instanceof PropDefId) {
	    PropDefId other = (PropDefId) obj;
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
