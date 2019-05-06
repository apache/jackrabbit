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

import org.apache.jackrabbit.jcr2spi.ItemManager;
import org.apache.jackrabbit.jcr2spi.ManagerProvider;
import org.apache.jackrabbit.spi.QueryInfo;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

/**
 * Implements the <code>javax.jcr.query.QueryResult</code> interface.
 */
class QueryResultImpl implements QueryResult {

    /**
     * The item manager of the session executing the query
     */
    private final ItemManager itemMgr;

    /**
     * Provides various managers.
     */
    private final ManagerProvider mgrProvider;

    /**
     * The spi query result.
     */
    private final QueryInfo queryInfo;

    /**
     * Creates a new query result.
     *
     * @param itemMgr     the item manager of the session executing the query.
     * @param mgrProvider the manager provider.
     * @param queryInfo   the spi query result.
     */
    QueryResultImpl(ItemManager itemMgr,
                    ManagerProvider mgrProvider,
                    QueryInfo queryInfo) {
        this.itemMgr = itemMgr;
        this.mgrProvider = mgrProvider;
        this.queryInfo = queryInfo;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getSelectorNames() throws RepositoryException {
        return queryInfo.getSelectorNames();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getColumnNames() throws RepositoryException {
        return queryInfo.getColumnNames();
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
        return new RowIteratorImpl(queryInfo, mgrProvider.getNamePathResolver(),
                mgrProvider.getJcrValueFactory(), itemMgr,
                mgrProvider.getHierarchyManager());
    }

    /**
     * Creates a node iterator over the result nodes.
     * @return a node iterator over the result nodes.
     */
    private ScoreNodeIterator getNodeIterator() {
        return new NodeIteratorImpl(itemMgr,
                mgrProvider.getHierarchyManager(), queryInfo);
    }
}
