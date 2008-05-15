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
package org.apache.jackrabbit.api.jsr283.nodetype;

import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.NodeDefinition;

/**
 * The <code>NodeTypeDefinition</code> interface provides methods for
 * discovering the static definition of a node type. These are accessible both
 * before and after the node type is registered. Its subclass
 * <code>NodeType</code> adds methods that are relevant only when the node type
 * is "live"; that is, after it has been registered. Note that the separate
 * <code>NodeDefinition</code> interface only plays a significant role in
 * implementations that support node type registration. In those cases it serves
 * as the superclass of both <code>NodeType</code> and
 * <code>NodeTypeTemplate</code>. In implementations that do not support node
 * type registration, only objects implementing the subinterface
 * <code>NodeType</code> will be encountered.
 *
 * @since JCR 2.0
 */
public interface NodeTypeDefinition {

    /**
     * Returns the name of the node type.
     * <p/>
     * In implementations that support node type registration, if this
     * <code>NodeTypeDefinition</code> object is actually a newly-created empty
     * <code>NodeTypeTemplate</code>, then this method will return
     * <code>null</code>.
     *
     * @return a <code>String</code>
     */
    String getName();

    /**
     * Returns the names of the supertypes actually declared in this node type.
     * <p/>
     * In implementations that support node type registration, if this
     * <code>NodeTypeDefinition</code> object is actually a newly-created empty
     * <code>NodeTypeTemplate</code>, then this method will return an array
     * containing a single string indicating the node type
     * <code>nt:base</code>.
     *
     * @return an array of <code>String</code>s
     */
    String[] getDeclaredSupertypeNames();

    /**
     * Returns <code>true</code> if this is an abstract node type; returns
     * <code>false</code> otherwise.
     * <p/>
     * An abstract node type is one that cannot be assigned as the primary or
     * mixin type of a node but can be used in the definitions of other node
     * types as a superclass.
     * <p/>
     * In implementations that support node type registration, if this
     * <code>NodeTypeDefinition</code> object is actually a newly-created empty
     * <code>NodeTypeTemplate</code>, then this method will return
     * <code>false</code>.
     *
     * @return a <code>boolean</code>
     */
    boolean isAbstract();

    /**
     * Returns <code>true</code> if this is a mixin type; returns
     * <code>false</code> if it is primary.
     * <p/>
     * In implementations that support node type registration, if this
     * <code>NodeTypeDefinition</code> object is actually a newly-created empty
     * <code>NodeTypeTemplate</code>, then this method will return
     * <code>false</code>.
     *
     * @return a <code>boolean</code>
     */
    boolean isMixin();

    /**
     * Returns <code>true</code> if nodes of this type must support orderable
     * child nodes; returns <code>false</code> otherwise. If a node type returns
     * <code>true</code> on a call to this method, then all nodes of that node
     * type <i>must</i> support the method <code>Node.orderBefore</code>. If a
     * node type returns <code>false</code> on a call to this method, then nodes
     * of that node type <i>may</i> support <code>Node.orderBefore</code>. Only
     * the primary node type of a node controls that node's status in this regard.
     * This setting on a mixin node type will not have any effect on the node.
     * <p/>
     * In implementations that support node type registration, if this
     * <code>NodeTypeDefinition</code> object is actually a newly-created empty
     * <code>NodeTypeTemplate</code>, then this method will return
     * <code>false</code>.
     *
     * @return a <code>boolean</code>
     */
    boolean hasOrderableChildNodes();

    /**
     * Returns the name of the primary item (one of the child items of the nodes
     * of this node type). If this node has no primary item, then this method
     * returns <code>null</code>. This indicator is used by the method
     * <code>Node.getPrimaryItem()</code>.
     * <p/>
     * In implementations that support node type registration, if this
     * <code>NodeTypeDefinition</code> object is actually a newly-created empty
     * <code>NodeTypeTemplate</code>, then this method will return
     * <code>null</code>.
     *
     * @return a <code>String</code>
     */
    String getPrimaryItemName();

    /**
     * Returns an array containing the property definitions actually declared in
     * this node type.
     * <p/>
     * In implementations that support node type registration, if this
     * <code>NodeTypeDefinition</code> object is actually a newly-created empty
     * <code>NodeTypeTemplate</code>, then this method will return
     * <code>null</code>.
     *
     * @return an array of <code>PropertyDefinition</code>s
     */
    PropertyDefinition[] getDeclaredPropertyDefinitions();

    /**
     * Returns an array containing the child node definitions actually declared
     * in this node type.
     * <p/>
     * In implementations that support node type registration, if this
     * <code>NodeTypeDefinition</code> object is actually a newly-created empty
     * <code>NodeTypeTemplate</code>, then this method will return
     * <code>null</code>.
     *
     * @return an array of <code>NodeDefinition</code>s
     */
    NodeDefinition[] getDeclaredChildNodeDefinitions();

}
