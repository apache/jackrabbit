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

import java.io.Serializable;
import org.apache.jackrabbit.name.QName;

/**
 * <p>This class represents a single entry of a property in a node. This
 * class only stores the name of the property, the actual property and it's
 * values are stored in the ORMPropertyState class.</p>
 */
public class ORMPropertyEntry
    implements Serializable {
    private String parentUUID;
    private String name;
    private Integer dbId;
    private ORMNodeState parent;
    public ORMPropertyEntry() {
    }

    public ORMPropertyEntry(ORMNodeState parent, QName propertyName, String parentUUID) {
        this.parent = parent;
        this.parentUUID = parentUUID;
        this.name = propertyName.toString();
    }

    public void setParentUUID(String parentUUID) {
        this.parentUUID = parentUUID;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDbId(Integer dbId) {

        this.dbId = dbId;
    }

    public void setParent(ORMNodeState parent) {
        this.parent = parent;
    }

    public String getParentUUID() {
        return parentUUID;
    }

    public String getName() {
        return name;
    }

    public Integer getDbId() {

        return dbId;
    }

    public ORMNodeState getParent() {
        return parent;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ORMPropertyEntry)) {
            return false;
        }
        ORMPropertyEntry right = (ORMPropertyEntry) obj;
        if (getName().equals(right.getName())) {
            return true;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return (getName()).hashCode();
    }

}
