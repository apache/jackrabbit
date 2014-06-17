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

import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.query.qom.QueryObjectModelFactory;

import org.apache.jackrabbit.core.query.ExecutableQuery;
import org.apache.jackrabbit.core.query.PropertyTypeRegistry;
import org.apache.jackrabbit.core.session.SessionContext;

/**
 * <code>AbstractQueryImpl</code> provides a base class for executable queries
 * based on {@link SearchIndex}.
 */
public abstract class AbstractQueryImpl implements ExecutableQuery {

    /**
     * Component context of the current session
     */
    protected final SessionContext sessionContext;

    /**
     * The actual search index
     */
    protected final SearchIndex index;

    /**
     * The property type registry for type lookup.
     */
    protected final PropertyTypeRegistry propReg;

    /**
     * If <code>true</code> the default ordering of the result nodes is in
     * document order.
     */
    private boolean documentOrder = true;

    protected final PerQueryCache cache = new PerQueryCache();

    /**
     * Creates a new query instance from a query string.
     *
     * @param sessionContext component context of the current session
     * @param index   the search index.
     * @param propReg the property type registry.
     */
    public AbstractQueryImpl(
            SessionContext sessionContext, SearchIndex index,
            PropertyTypeRegistry propReg) {
        this.sessionContext = sessionContext;
        this.index = index;
        this.propReg = propReg;
    }

    /**
     * If set <code>true</code> the result nodes will be in document order
     * per default (if no order by clause is specified). If set to
     * <code>false</code> the result nodes are returned in whatever sequence
     * the index has stored the nodes. That sequence is stable over multiple
     * invocations of the same query, but will change when nodes get added or
     * removed from the index.
     * <p>
     * The default value for this property is <code>true</code>.
     * @return the current value of this property.
     */
    public boolean getRespectDocumentOrder() {
        return documentOrder;
    }

    /**
     * Sets a new value for this property.
     *
     * @param documentOrder if <code>true</code> the result nodes are in
     * document order per default.
     *
     * @see #getRespectDocumentOrder()
     */
    public void setRespectDocumentOrder(boolean documentOrder) {
        this.documentOrder = documentOrder;
    }

    /**
     * @return the query object model factory.
     * @throws RepositoryException if an error occurs.
     */
    protected QueryObjectModelFactory getQOMFactory()
            throws RepositoryException {
        Workspace workspace = sessionContext.getSessionImpl().getWorkspace();
        return workspace.getQueryManager().getQOMFactory();
    }

    /**
     * Returns <code>true</code> if this query node needs items under
     * /jcr:system to be queried.
     *
     * @return <code>true</code> if this query node needs content under
     *         /jcr:system to be queried; <code>false</code> otherwise.
     */
    public abstract boolean needsSystemTree();
}
