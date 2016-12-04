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
import java.util.ArrayList;

import javax.jcr.query.Query;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.RepositoryException;

/**
 * <code>CompoundQueryFactory</code> implements a query factory that consists of
 * multiple other query factories.
 */
public class CompoundQueryFactory implements QueryFactory {

    /**
     * The query factories.
     */
    private List<QueryFactory> factories = new ArrayList<QueryFactory>();

    /**
     * Creates a compound query factory that consists of multiple other query
     * factories.
     *
     * @param factories the query factories.
     */
    public CompoundQueryFactory(List<QueryFactory> factories) {
        this.factories.addAll(factories);
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getSupportedLanguages() {
        List<String> languages = new ArrayList<String>();
        for (QueryFactory factory : factories) {
            for (String lang : factory.getSupportedLanguages()) {
                languages.add(lang);
            }
        }
        return languages;
    }

    /**
     * {@inheritDoc}
     */
    public Query createQuery(String statement, String language)
            throws InvalidQueryException, RepositoryException {
        for (QueryFactory factory : factories) {
            if (factory.getSupportedLanguages().contains(language)) {
                return factory.createQuery(statement, language);
            }
        }
        throw new InvalidQueryException("Unsupported language: " + language);
    }
}
