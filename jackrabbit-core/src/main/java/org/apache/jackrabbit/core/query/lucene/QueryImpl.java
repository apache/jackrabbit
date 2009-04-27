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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.PropertyDefinitionImpl;
import org.apache.jackrabbit.core.query.PropertyTypeRegistry;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.query.AndQueryNode;
import org.apache.jackrabbit.spi.commons.query.DefaultQueryNodeVisitor;
import org.apache.jackrabbit.spi.commons.query.LocationStepQueryNode;
import org.apache.jackrabbit.spi.commons.query.NodeTypeQueryNode;
import org.apache.jackrabbit.spi.commons.query.OrderQueryNode;
import org.apache.jackrabbit.spi.commons.query.QueryNodeFactory;
import org.apache.jackrabbit.spi.commons.query.QueryParser;
import org.apache.jackrabbit.spi.commons.query.QueryRootNode;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the {@link org.apache.jackrabbit.core.query.ExecutableQuery}
 * interface.
 */
public class QueryImpl extends AbstractQueryImpl {

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(QueryImpl.class);

    /**
     * The default selector name 's'.
     */
    public static final Name DEFAULT_SELECTOR_NAME = NameFactoryImpl.getInstance().create("", "s");

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
        this.root = QueryParser.parse(statement, language, session, factory);
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
        Path[] orderProperties = new Path[orderSpecs.length];
        boolean[] ascSpecs = new boolean[orderSpecs.length];
        for (int i = 0; i < orderSpecs.length; i++) {
            orderProperties[i] = orderSpecs[i].getPropertyPath();
            ascSpecs[i] = orderSpecs[i].isAscending();
        }

        return new SingleColumnQueryResult(index, itemMgr,
                session, session.getAccessManager(),
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
    protected Name[] getSelectProperties() throws RepositoryException {
        // get select properties
        List selectProps = new ArrayList();
        selectProps.addAll(Arrays.asList(root.getSelectProperties()));
        if (selectProps.size() == 0) {
            // use node type constraint
            LocationStepQueryNode[] steps = root.getLocationNode().getPathSteps();
            final Name[] ntName = new Name[1];
            steps[steps.length - 1].acceptOperands(new DefaultQueryNodeVisitor() {

                public Object visit(AndQueryNode node, Object data) throws RepositoryException {
                    return node.acceptOperands(this, data);
                }

                public Object visit(NodeTypeQueryNode node, Object data) {
                    ntName[0] = node.getValue();
                    return data;
                }
            }, null);
            if (ntName[0] == null) {
                ntName[0] = NameConstants.NT_BASE;
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
        if (!selectProps.contains(NameConstants.JCR_PATH)) {
            selectProps.add(NameConstants.JCR_PATH);
        }
        if (!selectProps.contains(NameConstants.JCR_SCORE)) {
            selectProps.add(NameConstants.JCR_SCORE);
        }

        return (Name[]) selectProps.toArray(new Name[selectProps.size()]);
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

}
