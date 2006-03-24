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


require_once 'PHPCR/ItemExistsException.php';
require_once 'PHPCR/PathNotFoundException.php';
require_once 'PHPCR/RepositoryException.php';
require_once 'PHPCR/nodetype/ConstraintViolationException.php';
require_once 'PHPCR/lock/LockException.php';


/**
 * A <code>Query</code> object.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 * @subpackage query
 */
interface Query
{
    /**
     * A String constant representing the XPath query language applied to the <i>document view</i>
     * XML mapping of the workspace.
     * <p/>
     * This language must be supported in level 1 repositories.
     * <p/>
     * Used when defining a query using {@link QueryManager#createQuery}.
     * Also among the strings returned by {@link QueryManager#getSupportedQueryLanguages}.
     */
    const XPATH = "xpath";

    /**
     * A String constant representing the SQL query language applied to the <i>database view</i>
     * of the workspace.
     * <p/>
     * This language is optional.
     * <p/>
     * Used when defining a query using {@link QueryManager#createQuery}.
     * Also among the strings returned by {@link QueryManager#getSupportedQueryLanguages}.
     */
    const SQL = "sql";


    /**
     * Executes this query and returns a <code>{@link QueryResult}</code>.
     *
     * @return a <code>QueryResult</code>
     * @throws RepositoryException if an error occurs
     */
    public function execute();

    /**
     * Returns the statement set for this query. Returns <code>null</code>
     * if no statement is currently set.
     *
     * @return the query statement.
     */
    public function getStatement();

    /**
     * Returns the language set for this query. This will be one of the
     * QueryLanguage constants returned by
     * QueryManager.getSupportedQueryLanguages(). If the query was created
     * using a mechanism outside the specification, this method may return 0.
     *
     * @return the query language.
     */
    public function getLanguage();

    /**
     * If this is a Query object that has been stored using
     * <code>storeAsNode($string)</code> (regardless of whether it has
     * been saved yet) or retrieved using
     * <code>QueryManager.getQuery($node)</code>), then this method returns
     * the path of the <code>nt:query</code> node that stores the query. If
     * this is a transient query (that is, a <code>Query</code> object created
     * with <code>QueryManager.createQuery($string, $string)</code> but not
     * yet stored) then this method throws an ItemNotFoundException.
     *
     * @return path of persisted node representing this query in content.
     */
    public function getStoredQueryPath();

    /**
     * Creates a node representing this Query in content.
     *
     * In a level 1 repository this method throws an
     * UnsupportedRepositoryOperationException.
     *
     * In a level 2 repository it creates a node of type nt:query at absPath
     * and returns that node.
     *
     * In order to persist the newly created node, a save must be performed
     * that includes the parent of this new node within its scope. In other
     * words, either a Session.save or an Item.save on the parent or
     * higher-degree ancestor of absPath must be performed.
     *
     * An ItemExistsException will be thrown either immediately (by this
     * method), or on save, if an item at the specified path already exists
     * and same-name siblings are not allowed. Implementations may differ
     * on when this validation is performed.
     *
     * A PathNotFoundException will be thrown either immediately , or on
     * save, if the specified path implies intermediary nodes that do not
     * exist. Implementations may differ on when this validation is performed.
     *
     * A ConstraintViolationExceptionwill be thrown either immediately or
     * on save, if adding the node would violate a node type or
     * implementation-specific constraintor if an attempt is made to add
     * a node as the child of a property. Implementations may differ on when
     * this validation is performed.
     *
     * A VersionException will be thrown either immediately (by this method),
     * or on save, if the node to which the new child is being added is
     * versionable and checked-in or is non-versionable but its nearest
     * versionable ancestor is checked-in. Implementations may differ on when
     * this validation is performed.
     *
     * A LockException will be thrown either immediately (by this method), or
     * on save, if a lock prevents the addition of the node. Implementations
     * may differ on when this validation is performed.
     *
     * @param absPath path at which to persist this query.
     * @return the newly created node.
     * @throws ItemExistsException If an item already exists at the indicated position
     * @throws PathNotFoundException If the path cannot be found
     * @throws ConstraintViolationException If creating the node would violate a
     * node type (or other implementation specific) constraint.
     * @throws VersionException f the node to which the new child is being
     * added is versionable and checked-in or is non-versionable but its
     * nearest versionable ancestor is checked-in and this implementation
     * performs this validation immediately instead of waiting until save.
     * @throws LockException if a lock prevents the addition of the node and
     * this implementation performs this validation immediately instead of
     * waiting until save.
     * @throws RepositoryException If another error occurs.
     */
    public function storeAsNode( $absPath );
}

?>