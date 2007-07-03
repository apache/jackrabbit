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
package org.apache.jackrabbit.spi.rmi.common;

import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.identifier.AbstractIdFactory;

import java.io.Serializable;

/**
 * <code>SerializableIdFactory</code> implements an id factory with serializable
 * item ids.
 */
public class SerializableIdFactory extends AbstractIdFactory {

    private static final SerializableIdFactory INSTANCE = new SerializableIdFactory();

    private SerializableIdFactory() {}

    public static SerializableIdFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Checks if the passed <code>nodeId</code> is serializable and if it is not
     * creates a serializable version for the given <code>nodeId</code>.
     *
     * @param nodeId the node id to check.
     * @return a serializable version of <code>nodeId</code> or the passed
     *         nodeId itself it is already serializable.
     */
    public NodeId createSerializableNodeId(NodeId nodeId) {
        if (nodeId instanceof Serializable) {
            return nodeId;
        } else {
            return INSTANCE.createNodeId(nodeId.getUniqueID(), nodeId.getPath());
        }
    }

    /**
     * Checks if the passed <code>propId</code> is serializable and if it is not
     * creates a serializable version for the given <code>propId</code>.
     *
     * @param propId the property id to check.
     * @return a serializable version of <code>propId</code> or the passed
     *         propId itself it is already serializable.
     */
    public PropertyId createSerializablePropertyId(PropertyId propertyId) {
        if (propertyId instanceof Serializable) {
            return propertyId;
        } else {
            return INSTANCE.createPropertyId(
                    createSerializableNodeId(propertyId.getParentId()),
                    propertyId.getQName());
        }
    }
}