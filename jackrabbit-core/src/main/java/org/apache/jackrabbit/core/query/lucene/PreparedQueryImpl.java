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

import org.apache.jackrabbit.core.query.ExecutablePreparedQuery;
import org.apache.jackrabbit.core.query.PropertyTypeRegistry;
import org.apache.jackrabbit.core.query.jsr283.qom.QueryObjectModelConstants;
import org.apache.jackrabbit.core.query.qom.QueryObjectModelTree;
import org.apache.jackrabbit.core.query.qom.DefaultTraversingQOMTreeVisitor;
import org.apache.jackrabbit.core.query.qom.BindVariableValueImpl;
import org.apache.jackrabbit.core.query.qom.ColumnImpl;
import org.apache.jackrabbit.core.query.qom.OrderingImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.lucene.search.Query;

import javax.jcr.Value;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>PreparedQueryImpl</code>...
 */
public class PreparedQueryImpl
        extends AbstractQueryImpl
        implements ExecutablePreparedQuery {

    /**
     * The query object model tree.
     */
    private final QueryObjectModelTree qomTree;

    /**
     * Set&lt;Name>, where Name is a variable name in the QOM tree.
     */
    private final Set variableNames = new HashSet();

    /**
     * Binding of variable name to value. Maps {@link Name} to {@link Value}.
     */
    private final Map bindValues = new HashMap();

    /**
     * Creates a new query instance from a query string.
     *
     * @param session the session of the user executing this query.
     * @param itemMgr the item manager of the session executing this query.
     * @param index   the search index.
     * @param propReg the property type registry.
     * @param qomTree the query object model tree.
     */
    public PreparedQueryImpl(SessionImpl session,
                             ItemManager itemMgr,
                             SearchIndex index,
                             PropertyTypeRegistry propReg,
                             QueryObjectModelTree qomTree) {
        super(session, itemMgr, index, propReg);
        this.qomTree = qomTree;
        extractBindVariableNames(qomTree, variableNames);
    }

    /**
     * Returns <code>true</code> if this query node needs items under
     * /jcr:system to be queried.
     *
     * @return <code>true</code> if this query node needs content under
     *         /jcr:system to be queried; <code>false</code> otherwise.
     */
    public boolean needsSystemTree() {
        // TODO: analyze QOM tree
        return true;
    }

    //-------------------------< ExecutableQuery >------------------------------

    /**
     * Executes this query and returns a <code>{@link javax.jcr.query.QueryResult}</code>.
     *
     * @param offset the offset in the total result set
     * @param limit  the maximum result size
     * @return a <code>QueryResult</code>
     * @throws RepositoryException if an error occurs
     */
    public QueryResult execute(long offset, long limit)
            throws RepositoryException {
        Query query = JQOM2LuceneQueryBuilder.createQuery(qomTree, session,
                index.getContext().getItemStateManager(),
                index.getNamespaceMappings(), index.getTextAnalyzer(),
                propReg, index.getSynonymProvider(), bindValues);

        ColumnImpl[] columns = qomTree.getColumns();
        Name[] selectProps = new Name[columns.length];
        for (int i = 0; i < columns.length; i++) {
            selectProps[i] = columns[i].getPropertyQName();
        }
        OrderingImpl[] orderings = qomTree.getOrderings();
        // TODO: there are many kinds of DynamicOperand that can be ordered by
        Name[] orderProps = new Name[orderings.length];
        boolean[] orderSpecs = new boolean[orderings.length];
        for (int i = 0; i < orderings.length; i++) {
            orderSpecs[i] = orderings[i].getOrder() == QueryObjectModelConstants.ORDER_ASCENDING;
        }
        return new QueryResultImpl(index, itemMgr,
                session.getNamePathResolver(), session.getAccessManager(),
                // TODO: spell suggestion missing
                this, query, null, selectProps, orderProps, orderSpecs,
                getRespectDocumentOrder(), offset, limit);
    }

    //-----------------------< ExecutablePreparedQuery >------------------------

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

    //--------------------------< internal >------------------------------------

    /**
     * Extracts all {@link BindVariableValueImpl} from the <code>qomTree</code>
     * into the <code>bindVariablesNames</code> set.
     *
     * @param qomTree            the QOM tree.
     * @param bindVariableNames where to put the bind variable names.
     */
    private void extractBindVariableNames(QueryObjectModelTree qomTree,
                                           final Set bindVariableNames) {
        try {
            qomTree.accept(new DefaultTraversingQOMTreeVisitor() {
                public Object visit(BindVariableValueImpl node, Object data) {
                    bindVariableNames.add(node.getBindVariableQName());
                    return data;
                }
            }, null);
        } catch (Exception e) {
            // will never happen
        }
    }
}
