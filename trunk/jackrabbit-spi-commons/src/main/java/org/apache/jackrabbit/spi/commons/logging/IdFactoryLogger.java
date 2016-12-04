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
package org.apache.jackrabbit.spi.commons.logging;

import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PropertyId;

/**
 * Log wrapper for an {@link IdFactory}.
 */
public class IdFactoryLogger extends AbstractLogger implements IdFactory {
    private final IdFactory idFactory;

    /**
     * Create a new instance for the given <code>idFactory</code> which uses
     * <code>writer</code> for persisting log messages.
     * @param idFactory
     * @param writer
     */
    public IdFactoryLogger(IdFactory idFactory, LogWriter writer) {
        super(writer);
        this.idFactory = idFactory;
    }

    /**
     * @return  the wrapped IdFactory
     */
    public IdFactory getIdFactory() {
        return idFactory;
    }

    public PropertyId createPropertyId(final NodeId parentId, final Name propertyName) {
        return (PropertyId) execute(new SafeCallable() {
            public Object call() {
                return idFactory.createPropertyId(parentId, propertyName);
            }}, "createPropertyId(NodeId, Name)", new Object[]{parentId, propertyName});
    }

    public NodeId createNodeId(final NodeId parentId, final Path path) {
        return (NodeId) execute(new SafeCallable() {
            public Object call() {
                return idFactory.createNodeId(parentId, path);
            }}, "createNodeId(NodeId, Path)", new Object[]{parentId, path});
    }

    public NodeId createNodeId(final String uniqueID, final Path path) {
        return (NodeId) execute(new SafeCallable() {
            public Object call() {
                return idFactory.createNodeId(uniqueID, path);
            }}, "createNodeId(String, Path)", new Object[]{uniqueID, path});
    }

    public NodeId createNodeId(final String uniqueID) {
        return (NodeId) execute(new SafeCallable() {
            public Object call() {
                return idFactory.createNodeId(uniqueID);
            }}, "createNodeId(String)", new Object[]{uniqueID});
    }

    public String toJcrIdentifier(final NodeId nodeId) {
        return (String) execute(new SafeCallable() {
            public Object call() {
                return idFactory.toJcrIdentifier(nodeId);
            }}, "toJcrIdentifier(String)", new Object[]{nodeId});
    }

    public NodeId fromJcrIdentifier(final String jcrIdentifier) {
        return (NodeId) execute(new SafeCallable() {
            public Object call() {
                return idFactory.fromJcrIdentifier(jcrIdentifier);
            }}, "fromJcrIdentifier(String)", new Object[]{jcrIdentifier});
    }

}
