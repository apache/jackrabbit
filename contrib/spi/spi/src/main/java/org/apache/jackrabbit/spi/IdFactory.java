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
package org.apache.jackrabbit.spi;

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;

/**
 * <code>IdFactory</code> defines methods to construct new <code>ItemId</code>s.
 */
public interface IdFactory {

    /**
     * Creates a new <code>PropertyId</code> from the given parent id and
     * qualified property name.
     *
     * @param parentId
     * @param propertyName
     * @return a new <code>PropertyId</code>.
     */
    public PropertyId createPropertyId(NodeId parentId, QName propertyName);

    /**
     * Creates a new <code>NodeId</code> from the given parent id and
     * the given <code>Path</code> object.
     *
     * @param parentId
     * @param path
     * @return a new <code>NodeId</code>.
     */
    public NodeId createNodeId(NodeId parentId, Path path);

    /**
     * Creates a new <code>NodeId</code> from the given unique id (which identifies
     * an ancestor <code>Node</code>) and the given <code>Path</code> object.
     *
     * @param uniqueID
     * @param path
     * @return a new <code>NodeId</code>.
     * @see ItemId ItemId for a description of the uniqueID defined by the SPI
     * item identifiers.
     */
    public NodeId createNodeId(String uniqueID, Path path);

    /**
     * Creates a new <code>NodeId</code> from the given unique id.
     *
     * @param uniqueID
     * @return a new <code>NodeId</code>.
     * @see ItemId ItemId for a description of the uniqueID defined by the SPI
     * item identifiers.
     */
    public NodeId createNodeId(String uniqueID);
}