<?php

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


require_once 'PHPCR/Value.php';
require_once 'PHPCR/nodetype/PropertyDefinition.php';
require_once 'PHPCR/nodetype/NodeDefinition.php';
require_once 'PHPCR/nodetype/NodeType.php';


/**
 * Represents a node type.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 * @subpackage nodetype
 */
interface NodeType
{
    /**
     * Returns the name of the node type.
     *
     * @return the name of the node type
     */
    public function getName();

    /**
     * Returns <code>true</code> if this node type is a mixin node type.
     * Returns <code>false</code> if this node type is a primary node type.
     *
     * @return a boolean
     */
    public function isMixin();

    /**
     * Returns <code>true</code> if nodes of this type must support orderable child nodes; returns <code>false</code>
     * otherwise. If a node type returns <code>true</code> on a call to this method, then all nodes of that node type
     * <i>must</i> support the method {@link Node#orderBefore}. If a node type returns <code>false</code>
     * on a call to this method, then nodes of that node type <i>may</i> support these ordering methods. Only the primary
     * node type of a node controls that node's status in this regard. This setting on a mixin node type will not have any effect
     * on the node.
     *
     * @return Returns <code>true</code> if nodes of this type must support orderable child nodes; returns
     * <code>false</code> otherwise.
     */
    public function hasOrderableChildNodes();

    /**
     * Returns the name of the primary item (one of the child items of the node's of this node type).
     * If this node has no primary item, then this method returns <code>null</code>.
     * This indicator is used by the method {@link Node#getPrimaryItem()}.
     *
     * @return the name of the primary item.
     */
    public function getPrimaryItemName();

    /**
     * Returns all supertypes of this node type including both those directly
     * declared and those inherited. For primary types, this list will always
     * include at least <code>nt:base</code>. For mixin types, there is no
     * required base type.
     *
     * @see #getDeclaredSupertypes
     *
     * @return an array of <code>NodeType</code> objects.
     */
    public function getSupertypes();

    /**
     * Returns all <i>direct</i> supertypes as specified in the declaration of
     * <i>this</i> node type. In single inheritance systems this will always be
     * an array of size 0 or 1. In systems that support multiple inheritance of
     * node types this array may be of size greater than 1.
     *
     * @see #getSupertypes
     *
     * @return an array of <code>NodeType</code> objects.
     */
    public function getDeclaredSupertypes();

    /**
     * Returns true if this node type is <code>nodeTypeName</code>
     * or a subtype of <code>nodeTypeName</code>, otherwise returns
     * <code>false</code>.
     * @param nodeTypeName the name of a node type.
     * @return a boolean
     */
    public function isNodeType( $nodeTypeName );

    /**
     * Returns an array containing the property definitions of this node type,
     * including the property definitions inherited from supertypes of this node
     * type.
     *
     * @see #getDeclaredPropertyDefinitions
     *
     * @return an array containing the property definitions.
     */
    public function getPropertyDefinitions();

    /**
     * Returns an array containing the property definitions explicitly specified
     * in the declaration of <i>this</i> node type. This does <i>not</i> include
     * property definitions inherited from supertypes of this node type.
     *
     * @see #getPropertyDefinitions
     *
     * @return an array containing the property definitions.
     */
    public function getDeclaredPropertyDefinitions();

    /**
     * Returns an array containing the child node definitions of this node type,
     * including the child node definitions inherited from supertypes of this
     * node type.
     *
     * @see #getDeclaredChildNodeDefinitions
     *
     * @return an array containing the child node definitions.
     */
    public function getChildNodeDefinitions();

    /**
     * Returns an array containing the child node definitions explicitly
     * specified in the declaration of <i>this</i> node type. This does
     * <i>not</i> include child node definitions inherited from supertypes of
     * this node type.
     *
     * @see #getChildNodeDefinitions
     * @return an array containing the child node definitions.
     */
    public function getDeclaredChildNodeDefinitions();

    /**
     * Returns <code>true</code> if setting <code>propertyName</code> to
     * <code>value</code> is allowed by this node type. Otherwise returns
     * <code>false</code>.
     *
     * @param propertyName The name of the property
     * @param value A <code>Value</code> object.
     */
    public function canSetProperty( $propertyName, $value );

    /**
     * Returns <code>true</code> if adding a child node called
     * <code>childNodeName</code> is allowed by this node type.
     * <p>
     *
     * @param childNodeName The name of the child node.
     * @param nodeTypeName The name of the node type of the child node.
     */
    public function canAddChildNode( $childNodeName, $nodeTypeName = null );

    /**
     * Returns true if removing the child item called <code>itemName</code> is allowed by this node type.
     * Otherwise returns <code>false</code>.
     *
     * @param itemName The name of the child item
     */
    public function canRemoveItem( $itemName );
}

?>