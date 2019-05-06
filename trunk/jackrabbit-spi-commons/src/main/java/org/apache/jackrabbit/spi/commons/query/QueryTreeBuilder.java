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
package org.apache.jackrabbit.spi.commons.query;

import javax.jcr.query.InvalidQueryException;

import org.apache.jackrabbit.spi.commons.conversion.NameResolver;

/**
 * Specifies an interface for a query tree builder.
 */
public interface QueryTreeBuilder {

    /**
     * Creates a <code>QueryNode</code> tree from a statement using the passed
     * query node factory.
     *
     * @param statement the statement.
     * @param resolver  the name resolver to use.
     * @param factory   the query node factory to use.
     * @return the <code>QueryNode</code> tree for the statement.
     * @throws javax.jcr.query.InvalidQueryException
     *          if the statement is malformed.
     */
    QueryRootNode createQueryTree(String statement,
                                  NameResolver resolver,
                                  QueryNodeFactory factory)
            throws InvalidQueryException;

    /**
     * Returns <code>true</code> if this query tree builder can handle a
     * statement in <code>language</code>.
     *
     * @param language the language of a query statement to build a query tree.
     * @return <code>true</code> if this builder can handle <code>language</code>;
     *         <code>false</code> otherwise.
     */
    boolean canHandle(String language);

    /**
     * Returns the set of query languages supported by this builder.
     *
     * @return String array containing the names of the supported languages.
     */
    String[] getSupportedLanguages();

    /**
     * Creates a String representation of the query node tree in the syntax this
     * <code>QueryTreeBuilder</code> can handle.
     *
     * @param root     the root of the query node tree.
     * @param resolver to resolve Names.
     * @return a String representation of the query node tree.
     * @throws InvalidQueryException if the query node tree cannot be converted
     *                               into a String representation due to
     *                               restrictions in this syntax.
     */
    String toString(QueryRootNode root, NameResolver resolver)
            throws InvalidQueryException;

}
