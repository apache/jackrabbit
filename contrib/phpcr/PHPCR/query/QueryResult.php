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


require_once 'PHPCR/NodeIterator.php';
require_once 'PHPCR/query/RowIterator.php';
require_once 'PHPCR/RepositoryException.php';


/**
 * A QueryResult object. Returned in an iterator by query.Query#execute()
 * Query.execute()
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 * @subpackage query
 */
interface QueryResult
{
    /**
     * Returns an array of all the property names (column names) in this result set.
     *
     * @return array of strings
     * @throws RepositoryException if an error occurs.
     */
    public function getColumnNames();

    /**
     * Returns an iterator over the <code>Row</code>s of the query result table.
     * If an <code>ORDER BY</code> clause was specified in the query, then the
     * order of the returned properties in the iterator will reflect the order
     * specified in that clause. If no items match, an empty iterator is returned.
     *
     * @return a <code>RowIterator</code>
     * @throws RepositoryException if an error occurs.
     */
    public function getRows();

    /**
     * Returns an iterator over all nodes that match the query. If an <code>ORDER BY</code>
     * clause was specified in the query, then the order of the returned nodes in the iterator
     * will reflect the order specified in that clause. If no nodes match, an empty iterator
     * is returned.
     *
     * @return a <code>NodeIterator</code>
     * @throws RepositoryException if an error occurs.
     */
    public function getNodes();
}

?>