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
 * <p>This class represents a single entry of a mixin type for a node.</p>
 */
public class ORMNodeMixinType
    implements Serializable {
    private String nodeUUID;
    private String mixinTypeName;
    private Integer dbId;
    private ORMNodeState node;
    public ORMNodeMixinType() {
    }

    public ORMNodeMixinType(ORMNodeState node, String nodeUUID, String mixinTypeName) {
        this.node = node;
        this.nodeUUID = nodeUUID;
        this.mixinTypeName = mixinTypeName;
    }

    public void setNodeUUID(String nodeUUID) {
        this.nodeUUID = nodeUUID;
    }

    public void setMixinTypeName(String mixinTypeName) {

        this.mixinTypeName = mixinTypeName;
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

    public String getMixinTypeName() {

        return mixinTypeName;
    }

    public Integer getDbId() {
        return dbId;
    }

    public ORMNodeState getNode() {
        return node;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ORMChildNodeEntry)) {
            return false;
        }
        ORMNodeMixinType right = (ORMNodeMixinType) obj;
        if (getMixinTypeName().equals(right.getMixinTypeName())) {
            return true;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return getMixinTypeName().hashCode();
    }

}
