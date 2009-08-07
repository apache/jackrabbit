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

import java.util.List;

/**
 * The <code>NodeTypeTemplate</code> interface represents a simple container
 * structure used to define node types which are then registered through the
 * <code>NodeTypeManager.registerNodeType</code> method.
 * <p/>
 * <code>NodeTypeTemplate</code>, like <code>NodeType</code>, is a subclass of
 * <code>NodeTypeDefinition</code> so it shares with <code>NodeType</code> those
 * methods that are relevant to a static definition. In addition,
 * <code>NodeTypeTemplate</code> provides methods for setting the attributes of
 * the definition. Implementations of this interface need not contain any
 * validation logic.
 * <p/>
 * See the corresponding <code>get</code> methods for each attribute in
 * <code>NodeTypeDefinition</code> for the default values assumed when a new
 * empty <code>NodeTypeTemplate</code> is created (as opposed to one extracted
 * from an existing <code>NodeType</code>).
 *
 * @since JCR 2.0
 */
public interface NodeTypeTemplate extends NodeTypeDefinition {

    /**
     * Sets the name of the node type.
     *
     * @param name a <code>String</code>.
     */
    void setName(String name);

    /**
     * Sets the names of the supertypes of the node type.
     *
     * @param names a <code>String</code> array.
     */
    void setDeclaredSuperTypeNames(String[] names);

    /**
     * Sets the abstract flag of the node type.
     *
     * @param abstractStatus a <code>boolean</code>.
     */
    void setAbstract(boolean abstractStatus);

    /**
     * Sets the mixin flag of the node type.
     *
     * @param mixin a <code>boolean</code>.
     */
    void setMixin(boolean mixin);

    /**
     * Sets the orderable child nodes flag of the node type.
     *
     * @param orderable a <code>boolean</code>.
     */
    void setOrderableChildNodes(boolean orderable);

    /**
     * Sets the name of the primary item.
     *
     * @param name a <code>String</code>.
     */
    void setPrimaryItemName(String name);

    /**
     * Returns a mutable <code>List</code> of <code>PropertyDefinitionTemplate</code>
     * objects. To define a new <code>NodeTypeTemplate</code> or change an
     * existing one, <code>PropertyDefinitionTemplate</code> objects can be
     * added to or removed from this <code>List</code>.
     *
     * @return a mutable <code>List</code> of <code>PropertyDefinitionTemplate</code> objects.
     */
    List getPropertyDefinitionTemplates();

    /**
     * Returns a mutable <code>List</code> of <code>NodeDefinitionTemplate</code>
     * objects. To define a new <code>NodeTypeTemplate</code> or change an
     * existing one, <code>NodeDefinitionTemplate</code> objects can be added
     * to or removed from this <code>List</code>.
     *
     * @return a mutable <code>List</code> of <code>NodeDefinitionTemplate</code> objects.
     */
    List getNodeDefinitionTemplates();

}
