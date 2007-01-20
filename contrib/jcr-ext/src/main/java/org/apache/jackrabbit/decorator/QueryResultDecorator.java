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
import org.apache.jackrabbit.decorator.DecoratingNodeIterator;

import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.RepositoryException;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

/**
 */
public class QueryResultDecorator
        extends AbstractDecorator implements QueryResult {

    protected final QueryResult result;

    public QueryResultDecorator(DecoratorFactory factory, Session session, QueryResult result) {
        super(factory, session);
        this.result = result;
    }

    /**
     * @inheritDoc
     */
    public String[] getColumnNames() throws RepositoryException {
        return result.getColumnNames();
    }

    /**
     * @inheritDoc
     */
    public RowIterator getRows() throws RepositoryException {
        return result.getRows();
    }

    /**
     * @inheritDoc
     */
    public NodeIterator getNodes() throws RepositoryException {
        NodeIterator nodes = result.getNodes();
        return new DecoratingNodeIterator(factory, session, nodes);
    }
}
