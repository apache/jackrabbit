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

require_once 'PHPCR/nodetype/ItemDefinition.php';


/**
 * A node definition. Used in node typed definition
 *
 * @author Markus Nix <mnix@mayflower.de>
 */
interface NodeDefinition extends ItemDefinition
{
    /**
     * Gets the minimum set of primary node types that the child node must have.
     * Returns an array to support those implementations with multiple
     * inheritance. The simplest case would be to return <code>nt:base</code>,
     * which is the base of all primary node types and therefore, in this
     * context, represents the least restrictive requirement.
     * <p>
     * A node must still have only one assigned primary node type, though
     * this attribute can restrict that node type by taking advantage of any
     * inheritance hierarchy that the implementation may support.
     *
     * @return an array of <code>NodeType</code> objects.
     */
    public function getRequiredPrimaryTypes();

    /**
     * Gets the default primary node type that will be assigned to the child
     * node if it is created without an explicitly specified primary node type.
     * This node type must be a subtype of (or the same type as) the node types
     * returned by <code>getRequiredPrimaryTypes</code>.
     * <p/>
     * If <code>null</code> is returned this indicates that no default primary
     * type is specified and that therefore an attempt to create this node without
     * specifying a node type will throw a <code>ConstraintViolationException</code>.
     *
     * @return a <code>NodeType</code>.
     */
    public function getDefaultPrimaryType();

    /**
     * Reports whether this child node can have same-name siblings. In other
     * words, whether the parent node can have more than one child node of this
     * name.
     *
     * @return a boolean.
     */
    public function allowSameNameSibs();
}

?>
