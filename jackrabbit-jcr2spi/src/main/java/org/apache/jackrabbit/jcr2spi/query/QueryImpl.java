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

import java.util.HashMap;
import java.util.Map;

import org.apache.jackrabbit.jcr2spi.ItemManager;
import org.apache.jackrabbit.jcr2spi.WorkspaceManager;
import org.apache.jackrabbit.jcr2spi.ManagerProvider;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameException;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

/**
 * Provides the default implementation for a JCR query.
 */
public class QueryImpl implements Query {

    /**
     * The session of the user executing this query
     */
    private final Session session;

    /**
     * Provides various managers.
     */
    private final ManagerProvider mgrProvider;

    /**
     * The item manager of the session that executes this query.
     */
    private final ItemManager itemManager;

    /**
     * The query statement
     */
    private String statement;

    /**
     * The syntax of the query statement
     */
    private String language;

    /**
     * The node where this query is persisted. Only set when this is a persisted
     * query.
     */
    private Node node;

    /**
     * The WorkspaceManager used to execute queries.
     */
    private WorkspaceManager wspManager;

    /**
     * Creates a new query.
     *
     * @param session          the session that created this query.
     * @param mgrProvider the manager provider.
     * @param itemMgr          the item manager of that session.
     * @param wspManager       the workspace manager that belongs to the
     *                         session.
     * @param statement        the query statement.
     * @param language         the language of the query statement.
     * @throws InvalidQueryException if the query is invalid.
     */
    public QueryImpl(Session session, ManagerProvider mgrProvider,
                     ItemManager itemMgr, WorkspaceManager wspManager,
                     String statement, String language)
            throws InvalidQueryException, RepositoryException {
        this.session = session;
        this.mgrProvider = mgrProvider;
        this.itemManager = itemMgr;
        this.statement = statement;
        this.language = language;
        this.wspManager = wspManager;
        this.wspManager.checkQueryStatement(
                statement, language, getNamespaceMappings());
    }

    /**
     * Creates a query from a node.
     *
     * @param session    the session that created this query.
     * @param mgrProvider the manager provider.
     * @param itemMgr    the item manager of that session.
     * @param wspManager the workspace manager that belongs to the session.
     * @param node       the node from where to read the query.
     * @throws InvalidQueryException if the query is invalid.
     * @throws RepositoryException   if another error occurs while reading from
     *                               the node.
     */
    public QueryImpl(Session session, ManagerProvider mgrProvider,
                     ItemManager itemMgr, WorkspaceManager wspManager,
                     Node node)
        throws InvalidQueryException, RepositoryException {

        this.session = session;
        this.mgrProvider = mgrProvider;
        this.itemManager = itemMgr;
        this.node = node;
        this.wspManager = wspManager;

        NamePathResolver resolver = mgrProvider.getNamePathResolver();
        if (!node.isNodeType(resolver.getJCRName(NameConstants.NT_QUERY))) {
            throw new InvalidQueryException("Node is not of type nt:query");
        }
        if (node.getSession() != session) {
            throw new InvalidQueryException("Node belongs to a different session.");
        }
        statement = node.getProperty(resolver.getJCRName(NameConstants.JCR_STATEMENT)).getString();
        language = node.getProperty(resolver.getJCRName(NameConstants.JCR_LANGUAGE)).getString();
        this.wspManager.checkQueryStatement(
                statement, language, getNamespaceMappings());
    }

    /**
     * @see Query#execute()
     */
    public QueryResult execute() throws RepositoryException {
        QueryInfo qI = wspManager.executeQuery(
                statement, language, getNamespaceMappings());
        return new QueryResultImpl(itemManager, mgrProvider, qI);
    }

    /***
     * Utility method that returns the namespace mappings of the current
     * session.
     *
     * @return namespace mappings (prefix -&gt; uri)
     * @throws RepositoryException if a repository error occurs
     */
    private Map getNamespaceMappings() throws RepositoryException {
        Map mappings = new HashMap();
        String[] prefixes = session.getNamespacePrefixes();
        for (int i = 0; i < prefixes.length; i++) {
            mappings.put(prefixes[i], session.getNamespaceURI(prefixes[i]));
        }
        return mappings;
    }

    /**
     * @see Query#getStatement()
     */
    public String getStatement() {
        return statement;
    }

    /**
     * @see Query#getLanguage()
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @see Query#getStoredQueryPath()
     */
    public String getStoredQueryPath() throws ItemNotFoundException, RepositoryException {
        if (node == null) {
            throw new ItemNotFoundException("Not a persistent query.");
        }
        return node.getPath();
    }

    /**
     * @see Query#storeAsNode(String)
     */
    public Node storeAsNode(String absPath) throws ItemExistsException,
        PathNotFoundException, VersionException, ConstraintViolationException,
        LockException, UnsupportedRepositoryOperationException, RepositoryException {

        NamePathResolver resolver = mgrProvider.getNamePathResolver();
        try {
            Path p = resolver.getQPath(absPath).getNormalizedPath();
            if (!p.isAbsolute()) {
                throw new RepositoryException(absPath + " is not an absolute path");
            }
            String jcrParent = resolver.getJCRPath(p.getAncestor(1));
            if (!session.itemExists(jcrParent)) {
                throw new PathNotFoundException(jcrParent);
            }
            String relPath = resolver.getJCRPath(p).substring(1);
            String ntName = resolver.getJCRName(NameConstants.NT_QUERY);
            Node queryNode = session.getRootNode().addNode(relPath, ntName);
            // set properties
            queryNode.setProperty(resolver.getJCRName(NameConstants.JCR_LANGUAGE), language);
            queryNode.setProperty(resolver.getJCRName(NameConstants.JCR_STATEMENT), statement);
            node = queryNode;
            return node;
        } catch (NameException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
    }
}

