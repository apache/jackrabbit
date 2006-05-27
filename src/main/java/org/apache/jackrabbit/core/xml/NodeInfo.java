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
package org.apache.jackrabbit.core.xml;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.name.QName;

public class NodeInfo {

    private final QName name;

    private final QName nodeTypeName;

    private final QName[] mixinNames;

    private final NodeId id;

    public NodeInfo(QName name, QName nodeTypeName, QName[] mixinNames,
                    NodeId id) {
        this.name = name;
        this.nodeTypeName = nodeTypeName;
        this.mixinNames = mixinNames;
        this.id = id;
    }

    public QName getName() {
        return name;
    }

    public QName getNodeTypeName() {
        return nodeTypeName;
    }

    public QName[] getMixinNames() {
        return mixinNames;
    }

    public NodeId getId() {
        return id;
    }

}
