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
import java.util.Arrays;
import java.util.Collection;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.jcr2spi.ItemManager;
import org.apache.jackrabbit.jcr2spi.ManagerProvider;
import org.apache.jackrabbit.jcr2spi.WorkspaceManager;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;

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
     * The maximum result size
     */
    private long limit = -1;

    /**
     * The offset in the total result set
     */
    private long offset = -1;

    /**
     * The name/value pairs collected upon calls to {@link #bindValue(String, Value)}.
     */
    private final Map<String, QValue> boundValues = new HashMap<String, QValue>();

    /**
     * The names of the bind variables as returned by the SPI implementation
     * after checking the query statement.
     */
    private final Collection<String> varNames;

    /**
     * Creates a new query.
     *
     * @param session     the session that created this query.
     * @param mgrProvider the manager provider.
     * @param itemMgr     the item manager of that session.
     * @param wspManager  the workspace manager that belongs to the session.
     * @param statement   the query statement.
     * @param language    the language of the query statement.
     * @param node        the node from where the query was read or
     *                    <code>null</code> if this query is not a stored
     *                    query.
     * @throws InvalidQueryException if the query is invalid.
     */
    public QueryImpl(Session session,
                     ManagerProvider mgrProvider,
                     ItemManager itemMgr,
                     WorkspaceManager wspManager,
                     String statement,
                     String language,
                     Node node)
            throws InvalidQueryException, RepositoryException {
        this.session = session;
        this.mgrProvider = mgrProvider;
        this.itemManager = itemMgr;
        this.statement = statement;
        this.language = language;
        this.wspManager = wspManager;
        this.varNames = Arrays.asList(this.wspManager.checkQueryStatement(
                statement, language, getNamespaceMappings()));
        this.node = node;
    }

    /**
     * @see Query#execute()
     */
    public QueryResult execute() throws RepositoryException {
        QueryInfo qI = wspManager.executeQuery(
                statement, language, getNamespaceMappings(), limit, offset, boundValues);
        return new QueryResultImpl(itemManager, mgrProvider, qI);
    }

    /***
     * Utility method that returns the namespace mappings of the current
     * session.
     *
     * @return namespace mappings (prefix -&gt; uri)
     * @throws RepositoryException if a repository error occurs
     */
    private Map<String, String> getNamespaceMappings() throws RepositoryException {
        Map<String, String> mappings = new HashMap<String, String>();
        for (String prefix : session.getNamespacePrefixes()) {
            mappings.put(prefix, session.getNamespaceURI(prefix));
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
            queryNode.setProperty(resolver.getJCRName(NameConstants.JCR_LANGUAGE), getLanguage());
            queryNode.setProperty(resolver.getJCRName(NameConstants.JCR_STATEMENT), getStatement());
            node = queryNode;
            return node;
        } catch (NameException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
    }

    /**
     * @see Query#getBindVariableNames()
     */
    public String[] getBindVariableNames() throws RepositoryException {
        return varNames.toArray(new String[varNames.size()]);
    }

    /**
     * @see Query#bindValue(String, Value)
     */
    public void bindValue(String varName, Value value) throws RepositoryException {
        if (!varNames.contains(varName)) {
            throw new IllegalArgumentException(varName + " is not a known bind variable name in this query");
        }
        if (value == null) {
            boundValues.remove(varName);
        } else {
            boundValues.put(varName, ValueFormat.getQValue(value, mgrProvider.getNamePathResolver(), mgrProvider.getQValueFactory()));
        }
    }

    /**
     * @see Query#setLimit(long)
     */
    public void setLimit(long limit) {
        this.limit = limit;
    }

    /**
     * @see Query#setOffset(long)
     */
    public void setOffset(long offset) {
        this.offset = offset;
    }
}
