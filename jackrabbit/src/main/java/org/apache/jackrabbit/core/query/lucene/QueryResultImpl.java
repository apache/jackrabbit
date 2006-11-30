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
package org.apache.jackrabbit.core.query.lucene;

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import java.util.Arrays;

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
     * The result nodes and their scores
     */
    private final ScoreNode[] scoreNodes;

    /**
     * The select properties
     */
    private final QName[] selectProps;

    /**
     * The namespace resolver of the session executing the query
     */
    private final NamespaceResolver resolver;

    /**
     * If <code>true</code> nodes are returned in document order.
     */
    private final boolean docOrder;

    /**
     * Creates a new query result.
     *
     * @param itemMgr     the item manager of the session executing the query.
     * @param ids         the Ids of the result nodes.
     * @param scores      the score values of the result nodes.
     * @param selectProps the select properties of the query.
     * @param resolver    the namespace resolver of the session executing the query.
     * @param docOrder    if <code>true</code> the result is returned in document
     *  order.
     */
    public QueryResultImpl(ItemManager itemMgr,
                           NodeId[] ids,
                           Float[] scores,
                           QName[] selectProps,
                           NamespaceResolver resolver,
                           boolean docOrder) {
        this.scoreNodes = new ScoreNode[ids.length];
        this.itemMgr = itemMgr;
        this.selectProps = selectProps;
        this.resolver = resolver;
        this.docOrder = docOrder;
        for (int i = 0; i < ids.length; i++) {
            scoreNodes[i] = new ScoreNode(ids[i], scores[i].floatValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getColumnNames() throws RepositoryException {
        try {
            String[] propNames = new String[selectProps.length];
            for (int i = 0; i < selectProps.length; i++) {
                propNames[i] = NameFormat.format(selectProps[i], resolver);
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
        return new RowIteratorImpl(getNodeIterator(), selectProps, resolver);
    }

    /**
     * Creates a node iterator over the result nodes.
     * @return a node iterator over the result nodes.
     */
    private ScoreNodeIterator getNodeIterator() {
        if (docOrder) {
            return new DocOrderNodeIteratorImpl(itemMgr, Arrays.asList(scoreNodes));
        } else {
            return new NodeIteratorImpl(itemMgr, scoreNodes);
        }
    }
}
