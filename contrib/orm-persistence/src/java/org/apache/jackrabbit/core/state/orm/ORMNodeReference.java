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

/**
 * <p>This class represents a node reference, that is to say a property that
 * points to a specific node.</p>
 */
public class ORMNodeReference
    implements Serializable {
    private String targetId;
    private String propertyParentUUID;
    private String propertyName;
    private Integer dbId;
    public ORMNodeReference() {
    }

    public ORMNodeReference(String targetId, String propertyParentUUID, String propertyName) {
        this.targetId = targetId;
        this.propertyParentUUID = propertyParentUUID;
        this.propertyName = propertyName;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public void setPropertyParentUUID(String propertyParentUUID) {
        this.propertyParentUUID = propertyParentUUID;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public void setDbId(Integer dbId) {
        this.dbId = dbId;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getPropertyParentUUID() {
        return propertyParentUUID;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Integer getDbId() {
        return dbId;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ORMNodeParent)) {
            return false;
        }
        ORMNodeReference right = (ORMNodeReference) obj;
        if (getTargetId().equals(right.getTargetId()) &&
            getPropertyParentUUID().equals(right.getPropertyParentUUID()) &&
            getPropertyName().equals(right.getPropertyName()) ) {
            return true;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return (getTargetId() + getPropertyParentUUID() + getPropertyName()).hashCode();
    }
}
