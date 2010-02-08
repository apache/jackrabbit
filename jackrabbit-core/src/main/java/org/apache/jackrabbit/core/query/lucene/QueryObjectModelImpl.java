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

import java.util.List;
import java.util.ArrayList;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.QueryResult;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.qom.QueryObjectModelFactory;

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.query.PropertyTypeRegistry;
import org.apache.jackrabbit.core.query.lucene.constraint.Constraint;
import org.apache.jackrabbit.core.query.lucene.constraint.ConstraintBuilder;
import org.apache.jackrabbit.spi.commons.query.qom.BindVariableValueImpl;
import org.apache.jackrabbit.spi.commons.query.qom.ColumnImpl;
import org.apache.jackrabbit.spi.commons.query.qom.DefaultTraversingQOMTreeVisitor;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelTree;
import org.apache.jackrabbit.spi.commons.query.qom.SelectorImpl;
import org.apache.jackrabbit.spi.commons.query.qom.OrderingImpl;
import org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl;

/**
 * <code>QueryObjectModelImpl</code>...
 */
public class QueryObjectModelImpl extends AbstractQueryImpl {

    /**
     * The query object model tree.
     */
    private final QueryObjectModelTree qomTree;

    /**
     * Creates a new query instance from a query string.
     *
     * @param session the session of the user executing this query.
     * @param itemMgr the item manager of the session executing this query.
     * @param index   the search index.
     * @param propReg the property type registry.
     * @param qomTree the query object model tree.
     * @throws InvalidQueryException if the QOM tree is invalid.
     */
    public QueryObjectModelImpl(SessionImpl session,
                                ItemManager itemMgr,
                                SearchIndex index,
                                PropertyTypeRegistry propReg,
                                QueryObjectModelTree qomTree)
            throws InvalidQueryException {
        super(session, itemMgr, index, propReg);
        this.qomTree = qomTree;
        checkNodeTypes();
        extractBindVariableNames();
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
     * Executes this query and returns a <code>{@link QueryResult}</code>.
     *
     * @param offset the offset in the total result set
     * @param limit  the maximum result size
     * @return a <code>QueryResult</code>
     * @throws RepositoryException if an error occurs
     */
    public QueryResult execute(long offset, long limit)
            throws RepositoryException {

        LuceneQueryFactory factory = new LuceneQueryFactoryImpl(session,
                index.getSortComparatorSource(),
                index.getContext().getHierarchyManager(),
                index.getNamespaceMappings(), index.getTextAnalyzer(),
                index.getSynonymProvider(), index.getIndexFormatVersion(),
                getBindVariableValues());

        MultiColumnQuery query = factory.create(qomTree.getSource());

        if (qomTree.getConstraint() != null) {
            Constraint c = ConstraintBuilder.create(qomTree.getConstraint(),
                    getBindVariableValues(), qomTree.getSource().getSelectors(),
                    factory, session.getValueFactory());
            query = new FilterMultiColumnQuery(query, c);
        }


        List<ColumnImpl> columns = new ArrayList<ColumnImpl>();
        // expand columns without name
        for (ColumnImpl column : qomTree.getColumns()) {
            if (column.getColumnName() == null) {
                QueryObjectModelFactory qomFactory = getQOMFactory();
                NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
                SelectorImpl selector = qomTree.getSelector(column.getSelectorQName());
                NodeTypeImpl nt = ntMgr.getNodeType(selector.getNodeTypeQName());
                for (PropertyDefinition pd : nt.getPropertyDefinitions()) {
                    PropertyDefinitionImpl propDef = (PropertyDefinitionImpl) pd;
                    if (!propDef.unwrap().definesResidual() && !propDef.isMultiple()) {
                        String sn = selector.getSelectorName();
                        String pn = propDef.getName();
                        columns.add((ColumnImpl) qomFactory.column(sn, pn, sn + "." + pn));
                    }
                }
            } else {
                columns.add(column);
            }
        }
        OrderingImpl[] orderings = qomTree.getOrderings();
        return new MultiColumnQueryResult(index, itemMgr,
                session, session.getAccessManager(),
                // TODO: spell suggestion missing
                this, query, null, columns.toArray(new ColumnImpl[columns.size()]),
                orderings, orderings.length == 0 && getRespectDocumentOrder(),
                offset, limit);
    }

    //--------------------------< internal >------------------------------------

    /**
     * Extracts all {@link BindVariableValueImpl} from the {@link #qomTree}
     * and adds it to the set of known variable names.
     */
    private void extractBindVariableNames() {
        try {
            qomTree.accept(new DefaultTraversingQOMTreeVisitor() {
                public Object visit(BindVariableValueImpl node, Object data) {
                    addVariableName(node.getBindVariableQName());
                    return data;
                }
            }, null);
        } catch (Exception e) {
            // will never happen
        }
    }

    /**
     * Checks if the selector node types are valid.
     *
     * @throws InvalidQueryException if one of the selector node types is
     *                               unknown.
     */
    private void checkNodeTypes() throws InvalidQueryException {
        try {
            qomTree.accept(new DefaultTraversingQOMTreeVisitor() {
                public Object visit(SelectorImpl node, Object data) throws Exception {
                    String ntName = node.getNodeTypeName();
                    if (!session.getNodeTypeManager().hasNodeType(ntName)) {
                        throw new Exception(ntName + " is not a known node type");
                    }
                    return super.visit(node, data);
                }
            }, null);
        } catch (Exception e) {
            throw new InvalidQueryException(e.getMessage());
        }
    }
}
