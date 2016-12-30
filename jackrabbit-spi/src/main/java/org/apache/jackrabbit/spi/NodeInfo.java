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

import java.util.Iterator;

/**
 * The <code>NodeInfo</code> provides the basic information required to build
 * nodes making up the repository hierarchy.
 * <p>
 * Note however, that the list of child nodes does not form part of a
 * <code>NodeInfo</code>. It is retrieved by calling
 * {@link RepositoryService#getChildInfos(SessionInfo, NodeId)}. In case of
 * {@link RepositoryService#getItemInfos(SessionInfo, ItemId) batch read} the
 * child nodes might be part of the returned <code>Iterator</code>.
 */
public interface NodeInfo extends ItemInfo {

    /**
     * Returns the <code>NodeId</code> for the node that is based on this info
     * object.
     *
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
     * @return <code>Name</code> representing the name of the primary nodetype.
     */
    public Name getNodetype();

    /**
     * @return Array of <code>Name</code>s representing the names of mixin nodetypes.
     * This includes only explicitly assigned mixin nodetypes. It does not include
     * mixin types inherited through the addition of supertypes to the primary
     * type hierarchy. If there are no mixin node types assigned an empty
     * array will be returned.
     */
    public Name[] getMixins();

    /**
     * Return the {@link PropertyId Id}s of the properties that are referencing the
     * node based on this info object.
     * 
     * @return {@link PropertyId Id}s of the properties that are referencing the
     * node based on this info object or an empty array if the node is not
     * referenceable or no references exist.
     * @see PropertyInfo#getId()
     * @deprecated Use {@link RepositoryService#getReferences(SessionInfo, NodeId, Name, boolean)} instead.
     */
    public PropertyId[] getReferences();

    /**
     * @return {@link PropertyId Id}s of children properties
     * @see PropertyInfo#getId()
     */
    public Iterator<PropertyId> getPropertyIds();

    /**
     * Return all <code>ChildInfo</code>s of the node represent by
     * this info, an empty iterator if that node doesn't have any child nodes
     * or <code>null</code> if the implementation is not able or for some
     * internal reasons not willing to compute the <code>ChildInfo</code>
     * iterator. In the latter case the user of this API must call
     * {@link RepositoryService#getChildInfos(SessionInfo, NodeId)} in order
     * to determine the existence and identity of the child nodes.
     *
     * @return An iterator of <code>ChildInfo</code>s or <code>null</code> if
     * the implementation is not able or willing to compute the set of
     * <code>ChildInfo</code>s (e.g. an implementation may choose to return
     * <code>null</code> if there is a huge amount of child nodes). In this
     * case {@link RepositoryService#getChildInfos(SessionInfo, NodeId)} will
     * be used to load the <code>ChildInfo</code>s.
     */
    public Iterator<ChildInfo> getChildInfos();
}
