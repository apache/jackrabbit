/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.search.lucene;

import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.PropertyDefImpl;
import org.apache.jackrabbit.core.search.QueryParser;
import org.apache.jackrabbit.core.search.QueryRootNode;
import org.apache.jackrabbit.core.search.OrderQueryNode;
import org.apache.jackrabbit.core.search.LocationStepQueryNode;
import org.apache.jackrabbit.core.search.QueryNode;
import org.apache.jackrabbit.core.search.NodeTypeQueryNode;
import org.apache.jackrabbit.core.search.DefaultQueryNodeVisitor;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.Path;
import org.apache.jackrabbit.core.MalformedPathException;
import org.apache.jackrabbit.core.NoPrefixDeclaredException;
import org.apache.jackrabbit.core.AccessManagerImpl;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.AccessManager;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Query;

import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDef;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;

/**
 */
class QueryImpl implements javax.jcr.query.Query {

    /**
     * jcr:statement
     */
    private static final QName PROP_STATEMENT =
            new QName(NamespaceRegistryImpl.NS_JCR_URI, "statement");

    /**
     * jcr:language
     */
    private static final QName PROP_LANGUAGE =
            new QName(NamespaceRegistryImpl.NS_JCR_URI, "language");

    private final QueryRootNode root;

    private final SessionImpl session;

    private final ItemManager itemMgr;

    private final SearchIndex index;

    private final String statement;

    private final String language;

    private Path path;

    public QueryImpl(SessionImpl session,
                     ItemManager itemMgr,
                     SearchIndex index,
                     String statement,
                     String language) throws InvalidQueryException {
        this.session = session;
        this.itemMgr = itemMgr;
        this.index = index;
        this.statement = statement;
        this.language = language;

        // parse query according to language
        // build query tree
        this.root = QueryParser.parse(statement, language, session.getNamespaceResolver());
    }

    public QueryImpl(SessionImpl session,
                     ItemManager itemMgr,
                     SearchIndex index,
                     String absPath)
            throws ItemNotFoundException, InvalidQueryException, RepositoryException {

        this.session = session;
        this.itemMgr = itemMgr;
        this.index = index;

        try {
            Node query = null;
            if (session.getRootNode().hasNode(absPath)) {
                query = session.getRootNode().getNode(absPath);
                // assert query has mix:referenceable
                query.getUUID();
                NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
                NodeType ntQuery = ntMgr.getNodeType(NodeTypeRegistry.NT_QUERY.toJCRName(session.getNamespaceResolver()));
                if (!query.getPrimaryNodeType().equals(ntQuery)) {
                    throw new InvalidQueryException("node is not of type nt:query");
                }
            } else {
                throw new ItemNotFoundException(absPath);
            }

            path = Path.create(absPath, session.getNamespaceResolver(), true);

            statement = query.getProperty(PROP_STATEMENT.toJCRName(session.getNamespaceResolver())).getString();
            language = query.getProperty(PROP_LANGUAGE.toJCRName(session.getNamespaceResolver())).getString();

            // parse query according to language
            // build query tree and pass to QueryImpl
            QueryRootNode root = QueryParser.parse(statement, language, session.getNamespaceResolver());
            this.root = root;
        } catch (NoPrefixDeclaredException e) {
            throw new InvalidQueryException(e.getMessage(), e);
        } catch (MalformedPathException e) {
            throw new ItemNotFoundException(absPath, e);
        }
    }

    public QueryResult execute() throws RepositoryException {
        // build lucene query
        Query query = LuceneQueryBuilder.createQuery(root,
                session, index.getNamespaceMappings(), index.getAnalyzer());

        OrderQueryNode orderNode = root.getOrderNode();

        OrderQueryNode.OrderSpec[] orderSpecs = null;
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


        List uuids;
        AccessManagerImpl accessMgr = session.getAccessManager();

        // execute it
        try {
            Hits result = index.executeQuery(query, orderProperties, ascSpecs);
            uuids = new ArrayList(result.length());
            for (int i = 0; i < result.length(); i++) {
                String uuid = result.doc(i).get(FieldNames.UUID);
                // check access
                if (accessMgr.isGranted(new NodeId(uuid), AccessManager.READ)) {
                    uuids.add(uuid);
                }
            }
        } catch (IOException e) {
            uuids = Collections.EMPTY_LIST;
        }

        // get select properties
        QName[] selectProps = root.getSelectProperties();
        if (selectProps.length == 0) {
            // use node type constraint
            LocationStepQueryNode[] steps = root.getLocationNode().getPathSteps();
            final QName[] ntName = new QName[1];
            steps[steps.length - 1].acceptOperands(new DefaultQueryNodeVisitor() {
                public Object visit(NodeTypeQueryNode node, Object data) {
                    ntName[0] = node.getValue();
                    return data;
                }
            }, null);
            if (ntName[0] == null) {
                ntName[0] = NodeTypeRegistry.NT_BASE;
            }
            NodeTypeImpl nt = session.getNodeTypeManager().getNodeType(ntName[0]);
            PropertyDef[] propDefs = (PropertyDef[]) nt.getPropertyDefs();
            List tmp = new ArrayList();
            for (int i = 0; i < propDefs.length; i++) {
                if (!propDefs[i].isMultiple()) {
                    tmp.add(((PropertyDefImpl) propDefs[i]).getQName());
                }
            }
            selectProps = (QName[]) tmp.toArray(new QName[tmp.size()]);
        }

        // return QueryResult
        return new QueryResultImpl(itemMgr,
                (String[]) uuids.toArray(new String[uuids.size()]),
                selectProps,
                session.getNamespaceResolver());
    }

    public String getStatement() {
        return statement;
    }

    public String getLanguage() {
        return language;
    }

    public String getPersistentQueryPath() throws ItemNotFoundException {
        if (path == null) {
            throw new ItemNotFoundException("not a persistent query");
        }
        try {
            return path.toJCRPath(session.getNamespaceResolver());
        } catch (NoPrefixDeclaredException e) {
            // should not happen actually
            throw new ItemNotFoundException(path.toString());
        }
    }

    public void save(String absPath)
            throws ItemExistsException,
            PathNotFoundException,
            VersionException,
            ConstraintViolationException,
            LockException,
            UnsupportedRepositoryOperationException,
            RepositoryException {
        try {
            NamespaceResolver resolver = session.getNamespaceResolver();
            Path p = Path.create(absPath, resolver, true);
            if (!p.isAbsolute()) {
                throw new RepositoryException(absPath + " is not absolut");
            }
            if (!session.getRootNode().hasNode(p.getAncestor(1).toJCRPath(resolver))) {
                throw new PathNotFoundException(p.getAncestor(1).toJCRPath(resolver));
            }
            if (session.getRootNode().hasNode(p.toJCRPath(resolver))) {
                throw new ItemExistsException(p.toJCRPath(resolver));
            }
            Node queryNode = session.getRootNode().addNode(p.toJCRPath(resolver),
                    NodeTypeRegistry.NT_QUERY.toJCRName(resolver));
            // set properties
            queryNode.setProperty(PROP_LANGUAGE.toJCRName(resolver), language);
            queryNode.setProperty(PROP_STATEMENT.toJCRName(resolver), statement);
            // add mixin referenceable
            queryNode.addMixin(NodeTypeRegistry.MIX_REFERENCEABLE.toJCRName(resolver));
            // FIXME do anything else?
            queryNode.save();

        } catch (MalformedPathException e) {
            throw new RepositoryException(e.getMessage(), e);
        } catch (NoPrefixDeclaredException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
        session.getRootNode().getNode(absPath);
    }

}
