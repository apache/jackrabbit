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
package org.apache.jackrabbit.core.query;

import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

/**
 * Specifies an interface for a query object implementation that can just be
 * executed.
 * @see QueryImpl
 */
public interface ExecutableQuery {

    /**
     * Executes this query and returns a <code>{@link QueryResult}</code>.
     * @param offset the offset in the total result set
     * @param limit the maximum result size
     *
     * @return a <code>QueryResult</code>
     * @throws RepositoryException if an error occurs
     */
    QueryResult execute(long offset, long limit) throws RepositoryException;

}
