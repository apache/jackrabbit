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


require_once 'PHPCR/RepositoryException.php';
require_once 'PHPCR/nodetype/NodeType.php';
require_once 'PHPCR/nodetype/NoSuchNodeTypeException.php';
require_once 'PHPCR/nodetype/NodeTypeIterator.php';


/**
 * Allows for the retrieval of node types.
 * Accessed via Workspace#getNodeTypeManager.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 * @subpackage nodetype
 */
interface NodeTypeManager
{
    /**
     * Returns the named node type.
     * <p>
     * Throws a <code>NoSuchNodeTypeException</code> if a node type by that name does not exist.
     * <p>
     * Throws a <code>RepositoryException</code> if another error occurs.
     *
     * @param nodeTypeName the name of an existing node type.
     * @return A <code>NodeType</code> object.
     * @throws NoSuchNodeTypeException if no node type by the given name exists.
     * @throws RepositoryException if another error occurs.
     */
    public function getNodeType( $nodeTypeName );

    /**
     * Returns an iterator over all available node types (primary and mixin).
     *
     * @return An <code>NodeTypeIterator</code>.
     * @throws RepositoryException if an error occurs.
     */
    public function getAllNodeTypes();

    /**
     * Returns an iterator over all available primary node types.
     *
     * @return An <code>NodeTypeIterator</code>.
     * @throws RepositoryException if an error occurs.
     */
    public function getPrimaryNodeTypes();

    /**
     * Returns an iterator over all available mixin node types.
     * If none are available, an empty iterator is returned.
     *
     * @return An <code>NodeTypeIterator</code>.
     * @throws RepositoryException if an error occurs.
     */
    public function getMixinNodeTypes();
}

?>