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

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.Name;

/**
 * Information about a node being imported. This class is used
 * by the XML import handlers to pass the parsed node information through
 * the {@link Importer} interface to the actual import process.
 * <p>
 * An instance of this class is simply a container for the node name,
 * node identifier, and the node type information. See the {@link PropInfo}
 * class for the related carrier of property information.
 */
public class NodeInfo {

    /**
     * Name of the node being imported.
     */
    private final Name name;

    /**
     * Name of the primary type of the node being imported.
     */
    private final Name nodeTypeName;

    /**
     * Names of the mixin types of the node being imported.
     */
    private final Name[] mixinNames;

    /**
     * Identifier of the node being imported.
     */
    private final NodeId id;

    /**
     * Creates a node information instance.
     *
     * @param name name of the node being imported
     * @param nodeTypeName name of the primary type of the node being imported
     * @param mixinNames names of the mixin types of the node being imported
     * @param id identifier of the node being imported
     */
    public NodeInfo(Name name, Name nodeTypeName, Name[] mixinNames,
                    NodeId id) {
        this.name = name;
        this.nodeTypeName = nodeTypeName;
        this.mixinNames = mixinNames;
        this.id = id;
    }

    /**
     * Returns the name of the node being imported.
     *
     * @return node name
     */
    public Name getName() {
        return name;
    }

    /**
     * Returns the name of the primary type of the node being imported.
     *
     * @return primary type name
     */
    public Name getNodeTypeName() {
        return nodeTypeName;
    }

    /**
     * Returns the names of the mixin types of the node being imported.
     *
     * @return mixin type names
     */
    public Name[] getMixinNames() {
        return mixinNames;
    }

    /**
     * Returns the identifier of the node being imported.
     *
     * @return node identifier
     */
    public NodeId getId() {
        return id;
    }

}
