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

import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_LANGUAGE;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_STATEMENT;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.NT_QUERY;

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

import org.apache.jackrabbit.core.SearchManager;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.session.SessionOperation;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelFactoryImpl;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelTree;

/**
 * This class implements the {@link QueryManager} interface.
 */
public class QueryManagerImpl implements QueryManager {

    /**
     * Component context of the current session.
     */
    private final SessionContext sessionContext;

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
     * @param sessionContext component context of the current session
     * @param searchMgr the search manager of this workspace.
     * @throws RepositoryException if an error occurs while initializing the
     *                             query manager.
     */
    public QueryManagerImpl(
            final SessionContext sessionContext, final SearchManager searchMgr)
            throws RepositoryException {
        this.sessionContext = sessionContext;
        this.searchMgr = searchMgr;
        this.qomFactory = new QueryObjectModelFactoryImpl(sessionContext) {
            protected QueryObjectModel createQuery(QueryObjectModelTree qomTree)
                    throws InvalidQueryException, RepositoryException {
                return searchMgr.createQueryObjectModel(
                        sessionContext, qomTree, Query.JCR_JQOM, null);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    public Query createQuery(final String statement, final String language)
            throws RepositoryException {
        return perform(new SessionOperation<Query>() {
            public Query perform(SessionContext context)
                    throws RepositoryException {
                QueryFactory qf = new QueryFactoryImpl(language);
                return qf.createQuery(statement, language);
            }
            public String toString() {
                return "node.createQuery(" + statement + ", " + language + ")";
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public Query getQuery(final Node node) throws RepositoryException {
        return perform(new SessionOperation<Query>() {
            public Query perform(SessionContext context)
                    throws RepositoryException {
                if (!node.isNodeType(context.getJCRName(NT_QUERY))) {
                    throw new InvalidQueryException(
                            "Node is not of type nt:query: " + node);
                }
                String statement =
                    node.getProperty(context.getJCRName(JCR_STATEMENT)).getString();
                String language =
                    node.getProperty(context.getJCRName(JCR_LANGUAGE)).getString();

                QueryFactory qf = new QueryFactoryImpl(node, language);
                return qf.createQuery(statement, language);
            }
            public String toString() {
                return "queryManager.getQuery(node)";
            }
        });
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
    public Iterable<Node> getWeaklyReferringNodes(final Node node)
            throws RepositoryException {
        return perform(new SessionOperation<Iterable<Node>>() {
            public Iterable<Node> perform(SessionContext context)
                    throws RepositoryException {
                List<Node> nodes = new ArrayList<Node>();
                try {
                    NodeId nodeId = new NodeId(node.getIdentifier());
                    for (NodeId id : searchMgr.getWeaklyReferringNodes(nodeId)) {
                        nodes.add(sessionContext.getSessionImpl().getNodeById(id));
                    }
                } catch (IOException e) {
                    throw new RepositoryException(e);
                }
                return nodes;
            }
            public String toString() {
                return "queryManager.getWeaklyReferringNodes(node)";
            }
        });
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
     * Performs the given session operation.
     */
    private <T> T perform(SessionOperation<T> operation)
            throws RepositoryException {
        return sessionContext.getSessionState().perform(operation);
    }

    private class QueryFactoryImpl extends CompoundQueryFactory {

        public QueryFactoryImpl(String language) {
            this(null, language);
        }

        public QueryFactoryImpl(final Node node, final String language) {
            super(Arrays.asList(
                new QOMQueryFactory(new QueryObjectModelFactoryImpl(
                        sessionContext.getSessionImpl()) {
                    @Override
                    protected QueryObjectModel createQuery(
                            QueryObjectModelTree qomTree)
                            throws RepositoryException {
                        return searchMgr.createQueryObjectModel(
                                sessionContext, qomTree, language, node);
                    }
                },
                sessionContext.getSessionImpl().getValueFactory()),
                new AQTQueryFactory() {
                    public Query createQuery(String statement, String language)
                            throws RepositoryException {
                        return searchMgr.createQuery(
                                sessionContext, statement, language, node);
                    }
                }));
        }
    }
}
