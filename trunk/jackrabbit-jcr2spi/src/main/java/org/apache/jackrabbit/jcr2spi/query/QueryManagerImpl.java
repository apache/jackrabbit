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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;

import org.apache.jackrabbit.commons.query.QueryObjectModelBuilder;
import org.apache.jackrabbit.commons.query.QueryObjectModelBuilderRegistry;
import org.apache.jackrabbit.jcr2spi.ItemManager;
import org.apache.jackrabbit.jcr2spi.ManagerProvider;
import org.apache.jackrabbit.jcr2spi.WorkspaceManager;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelFactoryImpl;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelTree;

/**
 * This class implements the {@link QueryManager} interface.
 */
public class QueryManagerImpl implements QueryManager {

    /**
     * The <code>Session</code> for this QueryManager.
     */
    private final Session session;

    /**
     * The value factory.
     */
    private final ValueFactory valueFactory;

    /**
     * Provides various managers.
     */
    private final ManagerProvider mgrProvider;

    /**
     * The <code>ItemManager</code> of for item retrieval in search results
     */
    private final ItemManager itemMgr;

    /**
     * The <code>WorkspaceManager</code> where queries are executed.
     */
    private final WorkspaceManager wspManager;

    /**
     * Creates a new <code>QueryManagerImpl</code> for the passed
     * <code>Session</code>.
     *
     * @param session     the current session.
     * @param mgrProvider the manager provider.
     * @param itemMgr     the item manager of the current session.
     * @param wspManager  the workspace manager.
     * @throws RepositoryException if an error occurs while initializing this
     *                             query manager.
     */
    public QueryManagerImpl(Session session,
                            ManagerProvider mgrProvider,
                            ItemManager itemMgr,
                            WorkspaceManager wspManager) throws RepositoryException {
        this.session = session;
        this.valueFactory = mgrProvider.getJcrValueFactory();
        this.mgrProvider = mgrProvider;
        this.itemMgr = itemMgr;
        this.wspManager = wspManager;
    }

    /**
     * @see QueryManager#createQuery(String, String)
     */
    public Query createQuery(String statement, String language)
            throws InvalidQueryException, RepositoryException {
        checkIsAlive();
        return new QueryImpl(session, mgrProvider, itemMgr, wspManager,
                statement, language, null);
    }

    /**
     * @see QueryManager#getQuery(Node)
     */
    public Query getQuery(Node node)
            throws InvalidQueryException, RepositoryException {
        checkIsAlive();

        NamePathResolver resolver = mgrProvider.getNamePathResolver();
        if (!node.isNodeType(resolver.getJCRName(NameConstants.NT_QUERY))) {
            throw new InvalidQueryException("Node is not of type nt:query");
        }
        if (node.getSession() != session) {
            throw new InvalidQueryException("Node belongs to a different session.");
        }
        String statement = node.getProperty(resolver.getJCRName(NameConstants.JCR_STATEMENT)).getString();
        String language = node.getProperty(resolver.getJCRName(NameConstants.JCR_LANGUAGE)).getString();

        if (Query.JCR_JQOM.equals(language)) {
            QueryObjectModelFactory qomFactory = new QOMFactory(node, resolver);
            QueryObjectModelBuilder builder = QueryObjectModelBuilderRegistry.getQueryObjectModelBuilder(language);
            return builder.createQueryObjectModel(statement, qomFactory, valueFactory);
        } else {
            return new QueryImpl(session, mgrProvider, itemMgr, wspManager,
                    statement, language, node);
        }
    }

    /**
     * @see QueryManager#getSupportedQueryLanguages()
     */
    public String[] getSupportedQueryLanguages() throws RepositoryException {
        return wspManager.getSupportedQueryLanguages();
    }

    /**
     * @see QueryManager#getQOMFactory()
     */
    public QueryObjectModelFactory getQOMFactory() {
        return new QOMFactory(null, mgrProvider.getNamePathResolver());
    }

    //------------------------------------------------------------< private >---
    /**
     * Checks if this <code>QueryManagerImpl</code> instance is still usable,
     * otherwise throws a {@link javax.jcr.RepositoryException}.
     *
     * @throws RepositoryException if this query manager is not usable anymore,
     *                             e.g. the corresponding session is closed.
     */
    private void checkIsAlive() throws RepositoryException {
        if (!session.isLive()) {
            throw new RepositoryException("corresponding session has been closed");
        }
    }

    private class QOMFactory extends QueryObjectModelFactoryImpl {

        private final Node node;

        public QOMFactory(Node node, NamePathResolver resolver) {
            super(resolver);
            this.node = node;
        }

        @Override
        protected QueryObjectModel createQuery(QueryObjectModelTree qomTree)
                throws InvalidQueryException, RepositoryException {
            return new QueryObjectModelImpl(session, mgrProvider, itemMgr,
                    wspManager, qomTree, node);
        }
    }
}
