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
package org.apache.jackrabbit.decorator;

import org.apache.jackrabbit.decorator.DecoratorFactory;
import org.apache.jackrabbit.decorator.NodeDecorator;

import javax.jcr.query.QueryManager;
import javax.jcr.query.Query;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.Session;

/**
 */
public class QueryManagerDecorator
        extends AbstractDecorator implements QueryManager {

    protected final QueryManager manager;

    public QueryManagerDecorator(DecoratorFactory factory, Session session, QueryManager manager) {
        super(factory, session);
        this.manager = manager;
    }

    /**
     * @inheritDoc
     */
    public Query createQuery(String statement, String language)
            throws InvalidQueryException, RepositoryException {
        return factory.getQueryDecorator(session,
                manager.createQuery(statement, language));
    }

    /**
     * @inheritDoc
     */
    public Query getQuery(Node node) throws InvalidQueryException, RepositoryException {
        Query query = manager.getQuery(NodeDecorator.unwrap(node));
        return factory.getQueryDecorator(session, query);
    }

    /**
     * @inheritDoc
     */
    public String[] getSupportedQueryLanguages() throws RepositoryException {
        return manager.getSupportedQueryLanguages();
    }
}
