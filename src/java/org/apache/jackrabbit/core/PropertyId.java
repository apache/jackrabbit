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
package org.apache.jackrabbit.core;

/**
 * <code>PropertyId</code> uniquely identifies a property in the repository.
 */
public class PropertyId extends ItemId {

    static final long serialVersionUID = -3726624437800567892L;

    private String parentUUID;
    private QName propName;

    public PropertyId(String parentUUID, QName propName) {
        if (parentUUID == null) {
            throw new IllegalArgumentException("parentUUID can not be null");
        }
        if (propName == null) {
            throw new IllegalArgumentException("propName can not be null");
        }
        this.parentUUID = parentUUID;
        this.propName = propName;
    }

    /**
     * @see ItemId#denotesNode
     */
    public boolean denotesNode() {
        return false;
    }

    public String getParentUUID() {
        return parentUUID;
    }

    public QName getName() {
        return propName;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PropertyId) {
            PropertyId other = (PropertyId) obj;
            return parentUUID.equals(other.parentUUID)
                    && propName.equals(other.propName);
        }
        return false;
    }

    /**
     * Returns a <code>PropertyId</code> holding the value of the specified
     * string. The string must be in the format returned by the
     * <code>PropertyId.toString()</code> method.
     *
     * @param s a <code>String</code> containing the <code>PropertyId</code>
     *          representation to be parsed.
     * @return the <code>PropertyId</code> represented by the argument
     * @throws IllegalArgumentException if the specified string can not be parsed
     *                                  as a <code>PropertyId</code>.
     * @see #toString()
     */
    public static PropertyId valueOf(String s) {
        if (s == null) {
            throw new IllegalArgumentException("invalid PropertyId literal");
        }
        int i = s.indexOf('/');
        if (i == -1) {
            throw new IllegalArgumentException("invalid PropertyId literal");
        }
        String uuid = s.substring(0, i);
        QName name = QName.valueOf(s.substring(i + 1));

        return new PropertyId(uuid, name);
    }

    public String toString() {
        return parentUUID + "/" + propName.toString();
    }

    public int hashCode() {
        // PropertyId is immutable, we can store the computed hash code value
        int h = hash;
        if (h == 0) {
            h = 17;
            h = 37 * h + parentUUID.hashCode();
            h = 37 * h + propName.hashCode();
            hash = h;
        }
        return h;
    }
}
