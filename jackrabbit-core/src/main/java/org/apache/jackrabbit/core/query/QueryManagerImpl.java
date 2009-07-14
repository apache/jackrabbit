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

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.SearchManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelFactoryImpl;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelTree;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * This class implements the {@link QueryManager} interface.
 */
public class QueryManagerImpl implements QueryManager {

    /**
     * The <code>Session</code> for this QueryManager.
     */
    private final SessionImpl session;

    /**
     * The <code>ItemManager</code> of for item retrieval in search results
     */
    private final ItemManager itemMgr;

    /**
     * The <code>SearchManager</code> holding the search index.
     */
    private final SearchManager searchMgr;

    /**
     * The <code>QueryObjectModelFactory</code> for this query manager.
     */
    private final QueryObjectModelFactoryImpl qomFactory;

    /**
     * Creates a new <code>QueryManagerImpl</code> for the passed
     * <code>session</code>
     *
     * @param session   the session for this query manager.
     * @param itemMgr   the item manager of the session.
     * @param searchMgr the search manager of this workspace.
     * @throws RepositoryException if an error occurs while initializing the
     *                             query manager.
     */
    public QueryManagerImpl(final SessionImpl session,
                            final ItemManager itemMgr,
                            final SearchManager searchMgr)
            throws RepositoryException {
        this.session = session;
        this.itemMgr = itemMgr;
        this.searchMgr = searchMgr;
        this.qomFactory = new QueryObjectModelFactoryImpl(session) {
            protected QueryObjectModel createQuery(QueryObjectModelTree qomTree)
                    throws InvalidQueryException, RepositoryException {
                return searchMgr.createQueryObjectModel(
                        session, qomTree, Query.JCR_JQOM, null);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    public Query createQuery(String statement, String language)
            throws InvalidQueryException, RepositoryException {
        sanityCheck();
        QueryFactory qf = new QueryFactoryImpl(language);
        return qf.createQuery(statement, language);
    }

    /**
     * {@inheritDoc}
     */
    public Query getQuery(Node node)
            throws InvalidQueryException, RepositoryException {
        sanityCheck();
        if (!node.isNodeType(session.getJCRName(NameConstants.NT_QUERY))) {
            throw new InvalidQueryException("node is not of type nt:query");
        }
        String statement = node.getProperty(session.getJCRName(NameConstants.JCR_STATEMENT)).getString();
        String language = node.getProperty(session.getJCRName(NameConstants.JCR_LANGUAGE)).getString();

        QueryFactory qf = new QueryFactoryImpl(node, language);
        return qf.createQuery(statement, language);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getSupportedQueryLanguages() throws RepositoryException {
        List<String> languages = new QueryFactoryImpl(Query.JCR_JQOM).getSupportedLanguages();
        return languages.toArray(new String[languages.size()]);
    }

    //---------------------------< JSR 283 >------------------------------------

    /**
     * Returns a <code>QueryObjectModelFactory</code> with which a JCR-JQOM
     * query can be built programmatically.
     *
     * @return a <code>QueryObjectModelFactory</code> object
     * @since JCR 2.0
     */
    public QueryObjectModelFactory getQOMFactory() {
        return qomFactory;
    }

    //-------------------------< Jackrabbit internal >--------------------------

    /**
     * Returns the ids of the nodes that refer to the <code>node</code> by weak
     * references.
     *
     * @param node the target node.
     * @return the referring nodes.
     * @throws RepositoryException if an error occurs.
     */
    public Iterable<Node> getWeaklyReferringNodes(Node node)
            throws RepositoryException {
        sanityCheck();
        List<Node> nodes = new ArrayList<Node>();
        try {
            NodeId nodeId = new NodeId(node.getIdentifier());
            for (NodeId id : searchMgr.getWeaklyReferringNodes(nodeId)) {
                nodes.add(session.getNodeById(id));
            }
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
        return nodes;
    }

    //------------------------< testing only >----------------------------------

    /**
     * @return the query handler implementation.
     */
    QueryHandler getQueryHandler() {
        return searchMgr.getQueryHandler();
    }

    //---------------------------< internal >-----------------------------------

    /**
     * Checks if this <code>QueryManagerImpl</code> instance is still usable,
     * otherwise throws a {@link javax.jcr.RepositoryException}.
     *
     * @throws RepositoryException if this query manager is not usable anymore,
     *                             e.g. the corresponding session is closed.
     */
    private void sanityCheck() throws RepositoryException {
        if (!session.isLive()) {
            throw new RepositoryException("corresponding session has been closed");
        }
    }

    private class QueryFactoryImpl extends CompoundQueryFactory {

        public QueryFactoryImpl(String language) {
            this(null, language);
        }

        public QueryFactoryImpl(final Node node, final String language) {
            super(Arrays.asList(
                new QOMQueryFactory(new QueryObjectModelFactoryImpl(session) {
                    protected QueryObjectModel createQuery(QueryObjectModelTree qomTree)
                            throws InvalidQueryException, RepositoryException {
                        return searchMgr.createQueryObjectModel(
                                session, qomTree, language, node);
                    }
                }, session.getValueFactory()),
                new AQTQueryFactory() {
                    public Query createQuery(String statement,
                                             String language)
                            throws InvalidQueryException, RepositoryException {
                        return searchMgr.createQuery(session, itemMgr, statement, language, node);
                    }
                }));
        }
    }
}
