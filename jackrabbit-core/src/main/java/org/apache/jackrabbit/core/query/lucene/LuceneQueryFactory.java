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

import static org.apache.jackrabbit.core.query.lucene.FieldNames.PROPERTIES;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_PRIMARYTYPE;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.qom.Literal;
import javax.jcr.query.qom.StaticOperand;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.query.qom.BindVariableValueImpl;
import org.apache.jackrabbit.spi.commons.query.qom.DefaultQOMTreeVisitor;
import org.apache.jackrabbit.spi.commons.query.qom.JoinConditionImpl;
import org.apache.jackrabbit.spi.commons.query.qom.SelectorImpl;
import org.apache.jackrabbit.spi.commons.query.qom.FullTextSearchImpl;
import org.apache.jackrabbit.spi.commons.query.qom.PropertyExistenceImpl;
import org.apache.jackrabbit.spi.commons.query.qom.SourceImpl;
import org.apache.jackrabbit.spi.commons.query.qom.JoinImpl;

/**
 * Factory that creates Lucene queries from QOM elements.
 */
public class LuceneQueryFactory {

    /**
     * Session of the user executing this query
     */
    private final SessionImpl session;

    /**
     * Node type manager
     */
    private final NodeTypeManager ntManager;

    /**
     * The hierarchy manager.
     */
    private final HierarchyManager hmgr;

    /**
     * Namespace mappings to internal prefixes
     */
    private final NamespaceMappings nsMappings;

    /**
     * NamePathResolver to map namespace mappings to internal prefixes
     */
    private final NamePathResolver npResolver;

    /**
     * The analyzer instance to use for contains function query parsing
     */
    private final Analyzer analyzer;

    /**
     * The synonym provider or <code>null</code> if none is configured.
     */
    private final SynonymProvider synonymProvider;

    /**
     * The index format version.
     */
    private final IndexFormatVersion version;

    /**
     * The Bind Variable values.
     */
    private final Map<Name, Value> bindVariables;

    private final String mixinTypesField;

    private final String primaryTypeField;

    /**
     * Creates a new lucene query factory.
     *
     * @param session         the session that executes the query.
     * @param scs             the sort comparator source of the index.
     * @param hmgr            the hierarchy manager of the workspace.
     * @param nsMappings      the index internal namespace mappings.
     * @param analyzer        the analyzer of the index.
     * @param synonymProvider the synonym provider of the index.
     * @param version         the version of the index format.
     * @param bindVariables   the bind variable values of the query
     */
    public LuceneQueryFactory(
            SessionImpl session, HierarchyManager hmgr,
            NamespaceMappings nsMappings, Analyzer analyzer,
            SynonymProvider synonymProvider, IndexFormatVersion version,
            Map<Name, Value> bindVariables) throws RepositoryException {
        this.session = session;
        this.ntManager = session.getWorkspace().getNodeTypeManager();
        this.hmgr = hmgr;
        this.nsMappings = nsMappings;
        this.analyzer = analyzer;
        this.synonymProvider = synonymProvider;
        this.version = version;
        this.npResolver = NamePathResolverImpl.create(nsMappings);
        this.bindVariables = bindVariables;
        this.mixinTypesField = nsMappings.translateName(JCR_MIXINTYPES);
        this.primaryTypeField = nsMappings.translateName(JCR_PRIMARYTYPE);
    }

    /**
     * Creates a lucene query for the given QOM selector.
     *
     * @param selector the selector.
     * @return a lucene query for the given selector.
     * @throws RepositoryException if an error occurs while creating the query.
     */
    public Query create(SelectorImpl selector) throws RepositoryException {
        List<Term> terms = new ArrayList<Term>();

        String name = selector.getNodeTypeName();
        NodeTypeIterator allTypes = ntManager.getAllNodeTypes();
        while (allTypes.hasNext()) {
            NodeType nt = allTypes.nextNodeType();
            if (nt.isNodeType(name)) {
                terms.add(createNodeTypeTerm(nt));
            }
        }

        if (terms.size() == 1) {
            return new JackrabbitTermQuery(terms.get(0));
        } else {
            BooleanQuery b = new BooleanQuery();
            for (Term term : terms) {
                b.add(new JackrabbitTermQuery(term), SHOULD);
            }
            return b;
        }
    }

    private Term createNodeTypeTerm(NodeType type) throws RepositoryException {
        String field;
        if (type.isMixin()) {
            // search for nodes where jcr:mixinTypes is set to this mixin
            field = mixinTypesField;
        } else {
            // search for nodes where jcr:primaryType is set to this type
            field = primaryTypeField;
        }
        String name = nsMappings.translateName(session.getQName(type.getName()));
        return new Term(PROPERTIES, FieldNames.createNamedValue(field, name));
    }

    /**
     * Creates a lucene query for the given QOM full text search.
     *
     * @param constraint the full text search constraint.
     * @return the lucene query for the given constraint.
     * @throws RepositoryException if an error occurs while creating the query.
     */
    public Query create(FullTextSearchImpl fts) throws RepositoryException {
        String fieldname;
        if (fts.getPropertyName() == null) {
            // fulltext on node
            fieldname = FieldNames.FULLTEXT;
        } else {
            // final path element is a property name
            Name propName = fts.getPropertyQName();
            StringBuffer tmp = new StringBuffer();
            tmp.append(nsMappings.getPrefix(propName.getNamespaceURI()));
            tmp.append(":").append(FieldNames.FULLTEXT_PREFIX);
            tmp.append(propName.getLocalName());
            fieldname = tmp.toString();
        }
        QueryParser parser = new JackrabbitQueryParser(
                fieldname, analyzer, synonymProvider);
        try {
            StaticOperand expr = fts.getFullTextSearchExpression();
            if (expr instanceof Literal) {
                return parser.parse(
                        ((Literal) expr).getLiteralValue().getString());
            } else if (expr instanceof BindVariableValueImpl) {
                Value value = this.bindVariables.get(
                        ((BindVariableValueImpl) expr).getBindVariableQName());
                if (value == null) {
                    throw new InvalidQueryException("Bind variable not bound");
                }
                return parser.parse(value.getString());
            } else {
                throw new RepositoryException(
                        "Unknown static operand type: " + expr);
            }
        } catch (ParseException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Creates a lucene query for the given QOM property existence constraint.
     *
     * @param constraint the QOM constraint.
     * @return the lucene query for the given constraint.
     * @throws RepositoryException if an error occurs while creating the query.
     */
    public Query create(PropertyExistenceImpl prop) throws RepositoryException {
        String propName = npResolver.getJCRName(prop.getPropertyQName());
        return Util.createMatchAllQuery(propName, version);
    }

    /**
     * Creates a multi column query for the given QOM source.
     *
     * @param source the QOM source.
     * @return a multi column query for the given source.
     * @throws RepositoryException if an error occurs while creating the query.
     */
    public MultiColumnQuery create(SourceImpl source) throws RepositoryException {
        // source is either selector or join
        try {
            return (MultiColumnQuery) source.accept(new DefaultQOMTreeVisitor() {
                public Object visit(JoinImpl node, Object data) throws Exception {
                    return create(node);
                }

                public Object visit(SelectorImpl node, Object data) throws Exception {
                    return MultiColumnQueryAdapter.adapt(
                            create(node), node.getSelectorQName());
                }
            }, null);
        } catch (RepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Creates a multi column query for the given QOM join.
     *
     * @param join the QOM join.
     * @return the multi column query for the given join.
     * @throws RepositoryException if an error occurs while creating the query.
     */
    public MultiColumnQuery create(JoinImpl join) throws RepositoryException {
        MultiColumnQuery left = create((SourceImpl) join.getLeft());
        MultiColumnQuery right = create((SourceImpl) join.getRight());
        return new JoinQuery(left, right, join.getJoinTypeInstance(),
                (JoinConditionImpl) join.getJoinCondition(), nsMappings, hmgr);
    }

}
