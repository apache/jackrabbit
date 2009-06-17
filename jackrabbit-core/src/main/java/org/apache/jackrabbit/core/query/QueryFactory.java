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

import java.util.List;

import javax.jcr.query.Query;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.RepositoryException;

/**
 * <code>QueryFactory</code> defines a simple interface for turning a statement
 * in a given language into a JCR Query instance.
 */
public interface QueryFactory {

    /**
     * @return supported query languages by this factory.
     */
    public List<String> getSupportedLanguages();

    /**
     * Creates a JCR query instance from the given <code>statement</code> in the
     * given <code>language</code>.
     *
     * @param statement the query statement.
     * @param language  the language of the query statement.
     * @return the JCR query instance representing the query.
     * @throws InvalidQueryException if the statement is malformed or the
     *                               language is not supported.
     * @throws RepositoryException   if another error occurs.
     */
    public Query createQuery(String statement, String language)
            throws InvalidQueryException, RepositoryException;
}
