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

import org.apache.jackrabbit.core.query.ExecutableQuery;
import org.apache.jackrabbit.core.query.PropertyTypeRegistry;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.spi.Name;

import javax.jcr.Value;
import javax.jcr.RepositoryException;
import javax.jcr.query.qom.QueryObjectModelFactory;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * <code>AbstractQueryImpl</code> provides a base class for executable queries
 * based on {@link SearchIndex}.
 */
public abstract class AbstractQueryImpl implements ExecutableQuery {

    /**
     * The session of the user executing this query
     */
    protected final SessionImpl session;

    /**
     * The item manager of the user executing this query
     */
    protected final ItemManager itemMgr;

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

    /**
     * Set&lt;Name>, where Name is a variable name in the query statement.
     */
    private final Set<Name> variableNames = new HashSet<Name>();

    /**
     * Binding of variable name to value. Maps {@link Name} to {@link Value}.
     */
    private final Map<Name, Value> bindValues = new HashMap<Name, Value>();


    /**
     * Creates a new query instance from a query string.
     *
     * @param session the session of the user executing this query.
     * @param itemMgr the item manager of the session executing this query.
     * @param index   the search index.
     * @param propReg the property type registry.
     */
    public AbstractQueryImpl(SessionImpl session,
                             ItemManager itemMgr,
                             SearchIndex index,
                             PropertyTypeRegistry propReg) {
        this.session = session;
        this.itemMgr = itemMgr;
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
     * <p/>
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
     * Binds the given <code>value</code> to the variable named
     * <code>varName</code>.
     *
     * @param varName name of variable in query
     * @param value   value to bind
     * @throws IllegalArgumentException if <code>varName</code> is not a valid
     *                                  variable in this query.
     * @throws RepositoryException      if an error occurs.
     */
    public void bindValue(Name varName, Value value)
            throws IllegalArgumentException, RepositoryException {
        if (!variableNames.contains(varName)) {
            throw new IllegalArgumentException("not a valid variable in this query");
        } else {
            bindValues.put(varName, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Name[] getBindVariableNames() throws RepositoryException {
        return variableNames.toArray(new Name[variableNames.size()]);
    }

    /**
     * Adds a name to the set of variables.
     *
     * @param varName the name of the variable.
     */
    protected void addVariableName(Name varName) {
        variableNames.add(varName);
    }

    /**
     * @return an unmodifieable map, which contains the variable names and their
     *         respective value.
     */
    protected Map<Name, Value> getBindVariableValues() {
        return Collections.unmodifiableMap(bindValues);
    }

    /**
     * @return the query object model factory.
     * @throws RepositoryException if an error occurs.
     */
    protected QueryObjectModelFactory getQOMFactory()
            throws RepositoryException {
        return session.getWorkspace().getQueryManager().getQOMFactory();
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
