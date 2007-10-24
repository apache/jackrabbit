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
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.PropertyDefinitionImpl;
import org.apache.jackrabbit.core.query.DefaultQueryNodeVisitor;
import org.apache.jackrabbit.core.query.ExecutableQuery;
import org.apache.jackrabbit.core.query.LocationStepQueryNode;
import org.apache.jackrabbit.core.query.NodeTypeQueryNode;
import org.apache.jackrabbit.core.query.OrderQueryNode;
import org.apache.jackrabbit.core.query.PathQueryNode;
import org.apache.jackrabbit.core.query.PropertyTypeRegistry;
import org.apache.jackrabbit.core.query.QueryParser;
import org.apache.jackrabbit.core.query.QueryRootNode;
import org.apache.jackrabbit.core.query.AndQueryNode;
import org.apache.jackrabbit.core.query.QueryNodeFactory;
import org.apache.jackrabbit.name.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.search.Query;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implements the {@link ExecutableQuery} interface.
 */
public class QueryImpl extends AbstractQueryImpl {

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(QueryImpl.class);

    /**
     * Represents a query that selects all nodes. E.g. in XPath: //*
     */
    protected final QueryRootNode allNodesQueryNode;

    /**
     * The root node of the query tree
     */
    protected final QueryRootNode root;

    /**
     * Creates a new query instance from a query string.
     *
     * @param session   the session of the user executing this query.
     * @param itemMgr   the item manager of the session executing this query.
     * @param index     the search index.
     * @param propReg   the property type registry.
     * @param statement the query statement.
     * @param language  the syntax of the query statement.
     * @param factory   the query node factory.
     * @throws InvalidQueryException if the query statement is invalid according
     *                               to the specified <code>language</code>.
     */
    public QueryImpl(SessionImpl session,
                     ItemManager itemMgr,
                     SearchIndex index,
                     PropertyTypeRegistry propReg,
                     String statement,
                     String language,
                     QueryNodeFactory factory) throws InvalidQueryException {
        super(session, itemMgr, index, propReg);
        // parse query according to language
        // build query tree using the passed factory
        this.root = QueryParser.parse(statement, language,
                session.getNamespaceResolver(), factory);
        allNodesQueryNode = createMatchAllNodesQuery(factory);
    }
    
    /**
     * Executes this query and returns a <code>{@link QueryResult}</code>.
     *
     * @param offset the offset in the total result set
     * @param limit the maximum result size
     * @return a <code>QueryResult</code>
     * @throws RepositoryException if an error occurs
     */
    public QueryResult execute(long offset, long limit) throws RepositoryException {
        if (log.isDebugEnabled()) {
            log.debug("Executing query: \n" + root.dump());
        }

        // check for special query
        if (allNodesQueryNode.equals(root)) {
            return new WorkspaceTraversalResult(session,
                    new QName[] { QName.JCR_PRIMARYTYPE, QName.JCR_PATH, QName.JCR_SCORE },
                    session.getNamespaceResolver());
        }

        // build lucene query
        Query query = LuceneQueryBuilder.createQuery(root, session,
                index.getContext().getItemStateManager(),
                index.getNamespaceMappings(), index.getTextAnalyzer(),
                propReg, index.getSynonymProvider(),
                index.getIndexFormatVersion());

        OrderQueryNode orderNode = root.getOrderNode();

        OrderQueryNode.OrderSpec[] orderSpecs;
        if (orderNode != null) {
            orderSpecs = orderNode.getOrderSpecs();
        } else {
            orderSpecs = new OrderQueryNode.OrderSpec[0];
        }
        QName[] orderProperties = new QName[orderSpecs.length];
        boolean[] ascSpecs = new boolean[orderSpecs.length];
        for (int i = 0; i < orderSpecs.length; i++) {
            orderProperties[i] = orderSpecs[i].getProperty();
            ascSpecs[i] = orderSpecs[i].isAscending();
        }

        return new QueryResultImpl(index, itemMgr,
                session.getNamespaceResolver(), session.getAccessManager(),
                this, query, new SpellSuggestion(index.getSpellChecker(), root),
                getSelectProperties(), orderProperties, ascSpecs,
                getRespectDocumentOrder(), offset, limit);
    }

    /**
     * Returns the select properties for this query.
     *
     * @return array of select property names.
     * @throws RepositoryException if an error occurs.
     */
    protected QName[] getSelectProperties() throws RepositoryException {
        // get select properties
        List selectProps = new ArrayList();
        selectProps.addAll(Arrays.asList(root.getSelectProperties()));
        if (selectProps.size() == 0) {
            // use node type constraint
            LocationStepQueryNode[] steps = root.getLocationNode().getPathSteps();
            final QName[] ntName = new QName[1];
            steps[steps.length - 1].acceptOperands(new DefaultQueryNodeVisitor() {

                public Object visit(AndQueryNode node, Object data) {
                    return node.acceptOperands(this, data);
                }

                public Object visit(NodeTypeQueryNode node, Object data) {
                    ntName[0] = node.getValue();
                    return data;
                }
            }, null);
            if (ntName[0] == null) {
                ntName[0] = QName.NT_BASE;
            }
            NodeTypeImpl nt = session.getNodeTypeManager().getNodeType(ntName[0]);
            PropertyDefinition[] propDefs = nt.getPropertyDefinitions();
            for (int i = 0; i < propDefs.length; i++) {
                PropertyDefinitionImpl propDef = (PropertyDefinitionImpl) propDefs[i];
                if (!propDef.definesResidual() && !propDef.isMultiple()) {
                    selectProps.add(propDef.getQName());
                }
            }
        }

        // add jcr:path and jcr:score if not selected already
        if (!selectProps.contains(QName.JCR_PATH)) {
            selectProps.add(QName.JCR_PATH);
        }
        if (!selectProps.contains(QName.JCR_SCORE)) {
            selectProps.add(QName.JCR_SCORE);
        }

        return (QName[]) selectProps.toArray(new QName[selectProps.size()]);
    }
    
    /**
     * Returns <code>true</code> if this query node needs items under
     * /jcr:system to be queried.
     * 
     * @return <code>true</code> if this query node needs content under
     *         /jcr:system to be queried; <code>false</code> otherwise.
     */
    public boolean needsSystemTree() {
        return this.root.needsSystemTree();
    }

    //----------------------------< internal >----------------------------------

    /**
     * Creates an abstract query tree that matches all nodes. XPath example:
     * //element(*, nt:base)
     *
     * @param factory the query node factory.
     * @return the abstract query tree.
     */
    private static QueryRootNode createMatchAllNodesQuery(
            QueryNodeFactory factory) {
        QueryRootNode allNodesQueryNode = factory.createQueryRootNode();
        PathQueryNode pathNode = factory.createPathQueryNode(allNodesQueryNode);
        LocationStepQueryNode lsNode = factory.createLocationStepQueryNode(pathNode);
        lsNode.setNameTest(null);
        lsNode.setIncludeDescendants(true);
        pathNode.addPathStep(lsNode);
        pathNode.setAbsolute(true);
        allNodesQueryNode.setLocationNode(pathNode);
        return allNodesQueryNode;
    }
}
