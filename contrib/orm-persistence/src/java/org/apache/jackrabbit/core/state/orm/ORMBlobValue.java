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
 * BLOB value ORM object
 */
public class ORMBlobValue
    implements Serializable {

    private Integer dbId;
    private String parentUUID;
    private String propertyName;
    private Integer index;
    private Long size;
    private byte[] blobValue;
    public ORMBlobValue() {
    }

    public void setDbId(Integer dbId) {
        this.dbId = dbId;
    }

    public void setParentUUID(String parentUUID) {
        this.parentUUID = parentUUID;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public void setSize(Long size) {

        this.size = size;
    }

    public void setBlobValue(byte[] blobValue) {

        this.blobValue = blobValue;
    }

    public Integer getDbId() {
        return dbId;
    }

    public String getParentUUID() {
        return parentUUID;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Integer getIndex() {
        return index;
    }

    public Long getSize() {

        return size;
    }

    public byte[] getBlobValue() {

        return blobValue;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ORMBlobValue)) {
            return false;
        }
        ORMBlobValue right = (ORMBlobValue) obj;
        if (dbId == null) {
            if (right.getDbId() == null) {
                return true;
            }
            // let's test other values.
            if (parentUUID.equals(right.getParentUUID()) &&
                propertyName.equals(right.getPropertyName()) &&
                index.equals(right.getIndex())) {
                return true;
            }
            return false;
        }
        if (dbId.equals(right.getDbId())) {
            return true;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return getDbId().hashCode();
    }

}
