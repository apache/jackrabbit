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
import java.util.Arrays;

import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.query.qom.Literal;
import javax.jcr.query.qom.StaticOperand;
import javax.jcr.RepositoryException;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.SortComparatorSource;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.jackrabbit.spi.commons.query.qom.SelectorImpl;
import org.apache.jackrabbit.spi.commons.query.qom.FullTextSearchImpl;
import org.apache.jackrabbit.spi.commons.query.qom.PropertyExistenceImpl;
import org.apache.jackrabbit.spi.commons.query.qom.SourceImpl;
import org.apache.jackrabbit.spi.commons.query.qom.JoinImpl;
import org.apache.jackrabbit.spi.commons.query.qom.DefaultQOMTreeVisitor;
import org.apache.jackrabbit.spi.commons.query.qom.JoinConditionImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.HierarchyManager;

/**
 * <code>LuceneQueryFactoryImpl</code> implements a lucene query factory.
 */
public class LuceneQueryFactoryImpl implements LuceneQueryFactory {

    /**
     * Session of the user executing this query
     */
    private final SessionImpl session;

    /**
     * The source comparator source.
     */
    private final SortComparatorSource scs;

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
     * Creates a new lucene query factory.
     *
     * @param session         the session that executes the query.
     * @param scs             the sort comparator source of the index.
     * @param hmgr            the hierarchy manager of the workspace.
     * @param nsMappings      the index internal namespace mappings.
     * @param analyzer        the analyzer of the index.
     * @param synonymProvider the synonym provider of the index.
     * @param version         the version of the index format.
     */
    public LuceneQueryFactoryImpl(SessionImpl session,
                                  SortComparatorSource scs,
                                  HierarchyManager hmgr,
                                  NamespaceMappings nsMappings,
                                  Analyzer analyzer,
                                  SynonymProvider synonymProvider,
                                  IndexFormatVersion version) {
        this.session = session;
        this.scs = scs;
        this.hmgr = hmgr;
        this.nsMappings = nsMappings;
        this.analyzer = analyzer;
        this.synonymProvider = synonymProvider;
        this.version = version;
        this.npResolver = NamePathResolverImpl.create(nsMappings);
    }

    /**
     * {@inheritDoc}
     */
    public Query create(SelectorImpl selector) throws RepositoryException {
        List<Term> terms = new ArrayList<Term>();
        String mixinTypesField = npResolver.getJCRName(NameConstants.JCR_MIXINTYPES);
        String primaryTypeField = npResolver.getJCRName(NameConstants.JCR_PRIMARYTYPE);

        NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
        NodeType base = null;
        try {
            base = ntMgr.getNodeType(session.getJCRName(selector.getNodeTypeQName()));
        } catch (RepositoryException e) {
            // node type does not exist
        }

        if (base != null && base.isMixin()) {
            // search for nodes where jcr:mixinTypes is set to this mixin
            Term t = new Term(FieldNames.PROPERTIES,
                    FieldNames.createNamedValue(mixinTypesField,
                            npResolver.getJCRName(selector.getNodeTypeQName())));
            terms.add(t);
        } else {
            // search for nodes where jcr:primaryType is set to this type
            Term t = new Term(FieldNames.PROPERTIES,
                    FieldNames.createNamedValue(primaryTypeField,
                            npResolver.getJCRName(selector.getNodeTypeQName())));
            terms.add(t);
        }

        // now search for all node types that are derived from base
        if (base != null) {
            NodeTypeIterator allTypes = ntMgr.getAllNodeTypes();
            while (allTypes.hasNext()) {
                NodeType nt = allTypes.nextNodeType();
                NodeType[] superTypes = nt.getSupertypes();
                if (Arrays.asList(superTypes).contains(base)) {
                    Name n = session.getQName(nt.getName());
                    String ntName = nsMappings.translateName(n);
                    Term t;
                    if (nt.isMixin()) {
                        // search on jcr:mixinTypes
                        t = new Term(FieldNames.PROPERTIES,
                                FieldNames.createNamedValue(mixinTypesField, ntName));
                    } else {
                        // search on jcr:primaryType
                        t = new Term(FieldNames.PROPERTIES,
                                FieldNames.createNamedValue(primaryTypeField, ntName));
                    }
                    terms.add(t);
                }
            }
        }
        Query q;
        if (terms.size() == 1) {
            q = new JackrabbitTermQuery(terms.get(0));
        } else {
            BooleanQuery b = new BooleanQuery();
            for (Term term : terms) {
                b.add(new JackrabbitTermQuery(term), BooleanClause.Occur.SHOULD);
            }
            q = b;
        }
        return q;
    }

    /**
     * {@inheritDoc}
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
            } else {
                throw new RepositoryException(
                        "Unknown static operand type: " + expr);
            }
        } catch (ParseException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Query create(PropertyExistenceImpl prop) throws RepositoryException {
        String propName = npResolver.getJCRName(prop.getPropertyQName());
        return Util.createMatchAllQuery(propName, version);
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public MultiColumnQuery create(JoinImpl join) throws RepositoryException {
        MultiColumnQuery left = create((SourceImpl) join.getLeft());
        MultiColumnQuery right = create((SourceImpl) join.getRight());
        return new JoinQuery(left, right, join.getJoinTypeInstance(),
                (JoinConditionImpl) join.getJoinCondition(), scs, hmgr);
    }
}
