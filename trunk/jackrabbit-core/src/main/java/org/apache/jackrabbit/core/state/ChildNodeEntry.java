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

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.core.id.NodeId;

/**
 * <code>ChildNodeEntry</code> specifies the name, index (in the case of
 * same-name siblings) and the UUID of a child node entry.
 * <p>
 * <code>ChildNodeEntry</code> instances are immutable.
 */
public final class ChildNodeEntry {

    private int hash;

    private final Name name;
    private final int index; // 1-based index for same-name siblings
    private final NodeId id;

    ChildNodeEntry(Name name, NodeId id, int index) {
        if (name == null) {
            throw new IllegalArgumentException("name can not be null");
        }
        this.name = name;

        if (id == null) {
            throw new IllegalArgumentException("id can not be null");
        }
        this.id = id;

        if (index < 1) {
            throw new IllegalArgumentException("index is 1-based");
        }
        this.index = index;
    }

    public NodeId getId() {
        return id;
    }

    public Name getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    //---------------------------------------< java.lang.Object overrides >
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ChildNodeEntry) {
            ChildNodeEntry other = (ChildNodeEntry) obj;
            return (name.equals(other.name) && id.equals(other.id)
                    && index == other.index);
        }
        return false;
    }

    public String toString() {
        return name.toString() + "[" + index + "] -> " + id;
    }

    public int hashCode() {
        // ChildNodeEntry is immutable, we can store the computed hash code value
        int h = hash;
        if (h == 0) {
            h = 17;
            h = 37 * h + name.hashCode();
            h = 37 * h + id.hashCode();
            h = 37 * h + index;
            hash = h;
        }
        return h;
    }
}
