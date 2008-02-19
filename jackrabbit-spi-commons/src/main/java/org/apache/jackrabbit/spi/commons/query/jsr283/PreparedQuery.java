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
package org.apache.jackrabbit.core.query.jsr283;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;

/**
 * A prepared query. A new prepared query is created by calling
 * <code>QueryManager.createPreparedQuery</code>.
 *
 * @since JCR 2.0
 */
public interface PreparedQuery extends Query {

    /**
     * Binds the given <code>value</code> to the variable named <code>varName</code>.
     *
     * @param varName name of variable in query
     * @param value value to bind
     * @throws IllegalArgumentException if <code>varName</code> is not a valid variable in this query.
     * @throws RepositoryException if an error occurs.
     */
    void bindValue(String varName, Value value)
        throws IllegalArgumentException, RepositoryException;

}
