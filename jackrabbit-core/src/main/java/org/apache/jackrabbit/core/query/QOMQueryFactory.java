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
import java.util.Arrays;

import javax.jcr.query.Query;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.commons.query.QueryObjectModelBuilderRegistry;

/**
 * <code>QOMQueryFactory</code> implements a query factory that creates QOM
 * based queries.
 */
public class QOMQueryFactory implements QueryFactory {

    /**
     * The query object model factory.
     */
    private final QueryObjectModelFactory qf;

    /**
     * The value factory.
     */
    private final ValueFactory vf;

    /**
     * Creates a new QOM base query factory.
     *
     * @param qf the QOM factory.
     * @param vf the value factory.
     */
    public QOMQueryFactory(QueryObjectModelFactory qf, ValueFactory vf) {
        this.qf = qf;
        this.vf = vf;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getSupportedLanguages() {
        return Arrays.asList(QueryObjectModelBuilderRegistry.getSupportedLanguages());
    }

    /**
     * {@inheritDoc}
     */
    public Query createQuery(String statement, String language)
            throws InvalidQueryException, RepositoryException {
        return QueryObjectModelBuilderRegistry.getQueryObjectModelBuilder(
                language).createQueryObjectModel(statement, qf, vf);
    }
}
