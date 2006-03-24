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
require_once 'PHPCR/RepositoryException.php';
require_once 'PHPCR/ItemNotFoundException.php';


/**
 * A row in the query result table.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 * @subpackage query
 */
interface Row
{
    /**
     * Returns an array of all the values in the same order as the property names
     * (column names) returned by {@link QueryResult#getPropertyNames()}.
     *
     * @return a <code>Value</code> array.
     * @throws RepositoryException if an error occurs
     */
    public function getValues();

    /**
     * Returns the value of the indicated  property in this <code>Row</code>.
     * <p/>
     * If <code>propertyName</code> is not among the column names of the query result
     * table, an <code>ItemNotFoundException</code> is thrown.
     *
     * @return a <code>Value</code>
     * @throws ItemNotFoundException if <code>propertyName</code> s not among the
     * column names of the query result table
     * @throws RepositoryException if anopther error occurs.
     */
    public function getValue( $propertyName );
}

?>