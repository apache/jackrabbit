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
package org.apache.jackrabbit.spi.commons.query.sql;

import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.query.QueryNodeFactory;
import org.apache.jackrabbit.spi.commons.query.QueryRootNode;
import org.apache.jackrabbit.spi.commons.query.QueryTreeBuilder;

import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;

/**
 * Implements the JCR SQL query tree builder.
 */
public class QueryBuilder implements QueryTreeBuilder {

    /**
     * {@inheritDoc}
     */
    public QueryRootNode createQueryTree(String statement,
                                         NameResolver resolver,
                                         QueryNodeFactory factory)
            throws InvalidQueryException {
        return JCRSQLQueryBuilder.createQuery(statement, resolver, factory);
    }

    /**
     * {@inheritDoc}
     */
    public boolean canHandle(String language) {
        return Query.SQL.equals(language);
    }

    /**
     * This builder supports {@link Query#SQL}.
     * {@inheritDoc}
     */
    public String[] getSupportedLanguages() {
        return new String[]{Query.SQL};
    }

    /**
     * {@inheritDoc}
     */
    public String toString(QueryRootNode root, NameResolver resolver)
            throws InvalidQueryException {
        return JCRSQLQueryBuilder.toString(root, resolver);
    }
}
