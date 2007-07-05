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

import java.util.Iterator;

/**
 * <code>NodeInfo</code>...
 */
public interface NodeInfo extends ItemInfo {

    /**
     * @return identifier for the item that is based on this info object. the id
     * can either be an absolute path or a uniqueID (+ relative path).
     * @see RepositoryService#getNodeInfo(SessionInfo, NodeId)
     */
    public NodeId getId();

    /**
     * Index of the node.
     * 
     * @return the index.
     */
    public int getIndex();

    /**
     * @return QName representing the name of the primary nodetype.
     */
    public QName getNodetype();

    /**
     * @return Array of QName representing the names of mixin nodetypes.
     */
    public QName[] getMixins();

    /**
     * @return {@link PropertyId Id}s of the properties that are referencing the
     * node based on this info object or an empty array if the node is not
     * referenceable or no references exist.
     * @see PropertyInfo#getId()
     */
    public PropertyId[] getReferences();

    /**
     * @return {@link PropertyId Id}s of children properties
     * @see PropertyInfo#getId()
     */
    public Iterator getPropertyIds();
}