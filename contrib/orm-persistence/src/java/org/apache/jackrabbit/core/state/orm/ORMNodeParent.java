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
 * <p>This class represents a single entry of a node's parents.</p>
 */
public class ORMNodeParent
    implements Serializable {
    private String nodeUUID;
    private String parentUUID;
    private Integer dbId;
    private ORMNodeState node;
    public ORMNodeParent() {
    }

    public ORMNodeParent(ORMNodeState node, String nodeUUID, String parentUUID) {
        this.node = node;
        this.nodeUUID = nodeUUID;
        this.parentUUID = parentUUID;
    }

    public void setNodeUUID(String nodeUUID) {
        this.nodeUUID = nodeUUID;
    }

    public void setParentUUID(String parentUUID) {
        this.parentUUID = parentUUID;
    }

    public void setDbId(Integer dbId) {
        this.dbId = dbId;
    }

    public void setNode(ORMNodeState node) {
        this.node = node;
    }

    public String getNodeUUID() {
        return nodeUUID;
    }

    public String getParentUUID() {
        return parentUUID;
    }

    public Integer getDbId() {
        return dbId;
    }

    public ORMNodeState getNode() {
        return node;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ORMNodeParent)) {
            return false;
        }
        ORMNodeParent right = (ORMNodeParent) obj;
        if (getParentUUID().equals(right.getParentUUID())) {
            return true;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return getParentUUID().hashCode();
    }
}
