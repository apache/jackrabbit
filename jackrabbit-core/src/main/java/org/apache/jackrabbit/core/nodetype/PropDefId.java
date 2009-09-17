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
package org.apache.jackrabbit.core.nodetype;

import java.io.Serializable;

import org.apache.jackrabbit.spi.QPropertyDefinition;

/**
 * <code>PropDefId</code> serves as identifier for a given <code>QPropertyDefinition</code>.
 *
 *
 * uniquely identifies a <code>QPropertyDefinition</code> in the
 * node type registry.
 */
class PropDefId implements Serializable {

    /**
     * Serialization UID of this class.
     */
    static final long serialVersionUID = 3675238890036653593L;

    /**
     * The internal id is computed based on the characteristics of the
     * <code>QPropertyDefinition</code> that this <code>PropDefId</code> identifies.
     */
    private final int id;

    /**
     * Creates a new <code>PropDefId</code> that serves as identifier for
     * the given <code>QPropertyDefinition</code>. An internal id is computed based on
     * the characteristics of the <code>QPropertyDefinition</code> that it identifies.
     *
     * @param def <code>QPropertyDefinition</code> to create identifier for
     */
    public PropDefId(QPropertyDefinition def) {
        if (def == null) {
            throw new IllegalArgumentException("QPropertyDefinition argument can not be null");
        }
        // build key (format: <declaringNodeType>/<name>/<requiredType>/<multiple>)
        StringBuffer sb = new StringBuffer();

        sb.append(def.getDeclaringNodeType().toString());
        sb.append('/');
        if (def.definesResidual()) {
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

    /**
     * Private constructor that creates a <code>PropDefId</code> using an
     * internal id
     *
     * @param id internal id
     */
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
    public static PropDefId valueOf(String s) throws IllegalArgumentException {
        if (s == null) {
            throw new IllegalArgumentException("invalid PropDefId literal");
        }
        return new PropDefId(Integer.parseInt(s));
    }

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * {@inheritDoc}
     */
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
        // the computed 'id' is used as hash code
        return id;
    }
}
