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
package org.apache.jackrabbit.jcr2spi.query;
// DIFF JR: this class uses a different package than the jackrabbit original

import org.apache.jackrabbit.jcr2spi.ItemManager;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.QueryInfo;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

/**
 * Implements the <code>javax.jcr.query.QueryResult</code> interface.
 */
public class QueryResultImpl implements QueryResult {

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(QueryResultImpl.class);

    /**
     * The item manager of the session executing the query
     */
    private final ItemManager itemMgr;

    /**
     * The spi query result.
     */
    private final QueryInfo queryInfo;

    /**
     * The namespace resolver of the session executing the query
     */
    private final NamespaceResolver resolver;

    /**
     * Creates a new query result.
     *
     * @param itemMgr     the item manager of the session executing the query.
     * @param queryInfo   the spi query result.
     * @param resolver    the namespace resolver of the session executing the query.
     */
    public QueryResultImpl(ItemManager itemMgr,
                           QueryInfo queryInfo,
                           NamespaceResolver resolver) {
        this.itemMgr = itemMgr;
        this.queryInfo = queryInfo;
        this.resolver = resolver;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getColumnNames() throws RepositoryException {
        try {
            // DIFF JR: obtain names from QueryInfo
            QName[] names = queryInfo.getColumnNames();
            String[] propNames = new String[names.length];
            for (int i = 0; i < names.length; i++) {
                propNames[i] = resolver.getJCRName(names[i]);
            }
            return propNames;
        } catch (NoPrefixDeclaredException npde) {
            String msg = "encountered invalid property name";
            log.debug(msg);
            throw new RepositoryException(msg, npde);

        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeIterator getNodes() throws RepositoryException {
        return getNodeIterator();
    }

    /**
     * {@inheritDoc}
     */
    public RowIterator getRows() throws RepositoryException {
        return new RowIteratorImpl(getNodeIterator(), queryInfo.getColumnNames(), resolver);
    }

    /**
     * Creates a node iterator over the result nodes.
     * @return a node iterator over the result nodes.
     */
    private ScoreNodeIterator getNodeIterator() throws RepositoryException {
        return new NodeIteratorImpl(itemMgr, resolver, queryInfo);
    }
}
