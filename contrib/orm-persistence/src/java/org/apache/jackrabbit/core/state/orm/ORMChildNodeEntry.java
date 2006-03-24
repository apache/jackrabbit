/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.core.state.orm;

import org.apache.jackrabbit.core.state.NodeState.ChildNodeEntry;

import java.io.Serializable;

/**
 * <p>This class represents a child node entry row in the ORM object graph.</p>
 */
public class ORMChildNodeEntry
    implements Serializable, Comparable {
    private String uuid;
    private String parentUUID;
    private String name;
    // this is the index used for same name siblings
    private Integer sameNameIndex;
    private Integer dbId;
    private ORMNodeState parent;
    // this is the index used for conserving the order of child nodes.
    private Integer childrenIndex;
    public ORMChildNodeEntry() {
    }

    public ORMChildNodeEntry(ORMNodeState parent, ChildNodeEntry childNodeEntry, String parentUUID, int childrenIndex) {
        this.parent = parent;
        uuid = childNodeEntry.getId().getUUID().toString();
        this.parentUUID = parentUUID;
        name = childNodeEntry.getName().toString();
        sameNameIndex = new Integer(childNodeEntry.getIndex());
        this.childrenIndex = new Integer(childrenIndex);
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setParentUUID(String parentUUID) {
        this.parentUUID = parentUUID;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSameNameIndex(Integer sameNameIndex) {

        this.sameNameIndex = sameNameIndex;
    }

    public void setDbId(Integer dbId) {
        this.dbId = dbId;
    }

    public void setParent(ORMNodeState parent) {
        this.parent = parent;
    }

    public void setChildrenIndex(Integer childrenIndex) {
        this.childrenIndex = childrenIndex;
    }

    public String getUuid() {
        return uuid;
    }

    public String getParentUUID() {
        return parentUUID;
    }

    public String getName() {
        return name;
    }

    public Integer getSameNameIndex() {

        return sameNameIndex;
    }

    public Integer getDbId() {
        return dbId;
    }

    public ORMNodeState getParent() {
        return parent;
    }

    public Integer getChildrenIndex() {
        return childrenIndex;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ORMChildNodeEntry)) {
            return false;
        }
        ORMChildNodeEntry right = (ORMChildNodeEntry) obj;
            if (getUuid().equals(right.getUuid()) &&
                getName().equals(right.getName()) &&
                (getSameNameIndex().equals(right.getSameNameIndex())) &&
                (getChildrenIndex().equals(right.getChildrenIndex()))) {
                return true;
            } else {
                return false;
            }
    }

    public int compareTo(Object obj) {
        if (equals(obj)) {
            return 0;
        }
        ORMChildNodeEntry right = (ORMChildNodeEntry) obj;
        return (getChildrenIndex() + getUuid() + getName() + getSameNameIndex()).compareTo(right.getChildrenIndex() + right.getUuid() + right.getName() + right.getSameNameIndex());
    }

    public int hashCode() {
        return (getChildrenIndex() + getUuid() + getName() + getSameNameIndex()).hashCode();
    }
}
