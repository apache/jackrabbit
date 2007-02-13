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
import org.apache.jackrabbit.jcr2spi.WorkspaceManager;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.name.LocalNamespaceMappings;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

/**
 * This class implements the {@link QueryManager} interface.
 */
public class QueryManagerImpl implements QueryManager {

    /**
     * The <code>Session</code> for this QueryManager.
     */
    private final Session session;

    /**
     * The local namespace mappings for this query manager.
     */
    private final LocalNamespaceMappings nsResolver;

    /**
     * The <code>ItemManager</code> of for item retrieval in search results
     */
    private final ItemManager itemMgr;

    /**
     * The <code>HierarchyManager</code> of for item retrieval in search results
     */
    private final HierarchyManager hierarchyManager;

    /**
     * The <code>WorkspaceManager</code> where queries are executed.
     */
    private final WorkspaceManager wspManager;

    /**
     * Creates a new <code>QueryManagerImpl</code> for the passed
     * <code>Session</code>.
     *
     * @param session
     * @param nsResolver
     * @param itemMgr
     * @param hierarchyManager
     * @param wspManager
     */
    public QueryManagerImpl(Session session,
                            LocalNamespaceMappings nsResolver,
                            ItemManager itemMgr,
                            HierarchyManager hierarchyManager,
                            WorkspaceManager wspManager) {
        this.session = session;
        this.nsResolver = nsResolver;
        this.itemMgr = itemMgr;
        this.hierarchyManager = hierarchyManager;
        this.wspManager = wspManager;
    }

    /**
     * @see QueryManager#createQuery(String, String)
     */
    public Query createQuery(String statement, String language)
            throws InvalidQueryException, RepositoryException {
        checkIsAlive();
        QueryImpl query = new QueryImpl(session, nsResolver, itemMgr, hierarchyManager, wspManager, statement, language);
        return query;
    }

    /**
     * @see QueryManager#getQuery(Node)
     */
    public Query getQuery(Node node)
            throws InvalidQueryException, RepositoryException {
        checkIsAlive();
        QueryImpl query = new QueryImpl(session, nsResolver, itemMgr, hierarchyManager, wspManager, node);
        return query;
    }

    /**
     * @see QueryManager#getSupportedQueryLanguages()
     */
    public String[] getSupportedQueryLanguages() throws RepositoryException {
        return wspManager.getSupportedQueryLanguages();
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
}
