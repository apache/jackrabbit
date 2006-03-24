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
require_once 'PHPCR/Node.php';
require_once 'PHPCR/query/Query.php';
require_once 'PHPCR/query/InvalidQueryException.php';


/**
 * This interface encapsulates methods for the management of search queries.
 * Provides methods for the creation and retrieval of search queries.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 * @subpackage query
 */
interface QueryManager
{
    /**
     * Creates a new query by specifying the query statement itself and the
     * language in which the query is stated.  If the query statement is
     * syntactically invalid, given the language specified, an
     * InvalidQueryException is thrown. language must specify a query language
     * from among those returned by QueryManager.getSupportedQueryLanguages(); if it is not
     * then an InvalidQueryException is thrown.
     *
     * @throws InvalidQueryException if statement is invalid or language is unsupported.
     * @throws RepositoryException if another error occurs
     * @return A <code>Query</code> object.
     */
    public function createQuery( $statement, $language );

    /**
     * Retrieves an existing persistent query. If <code>node</code>
     * is not a valid persisted query (that is, a node of type
     * <code>nt:query</code>), an <code>InvalidQueryException</code>
     * is thrown.
     * <p/>
     * Persistent queries are created by first using <code>QueryManager.createQuery</code>
     * to create a <code>Query</code> object and then calling <code>Query.save</code> to
     * persist the query to a location in the workspace.
     *
     * @param node a persisted query (that is, a node of type <code>nt:query</code>).
     * @throws InvalidQueryException If <code>node</code> is not a valid persisted query
     * (that is, a node of type <code>nt:query</code>).
     * @throws RepositoryException if another error occurs
     * @return a <code>Query</code> object.
     */
    public function getQuery( Node $node );

    /**
     * Returns an array of integers identifying the supported query languages.
     * See QueryLanguage.
     *
     * @return An string array.
     * @throws RepositoryException if an error occurs.
     */
    public function getSupportedQueryLanguages();
}

?>