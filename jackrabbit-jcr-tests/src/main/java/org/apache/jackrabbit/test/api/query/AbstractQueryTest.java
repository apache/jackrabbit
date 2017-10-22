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
package org.apache.jackrabbit.test.api.query;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.api.util.ISO9075;

import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Abstract base class for query test cases.
 */
public abstract class AbstractQueryTest extends AbstractJCRTest {

    /**
     * Resolved Name for jcr:score
     */
    protected String jcrScore;

    /**
     * Resolved Name for jcr:path
     */
    protected String jcrPath;

    /**
     * Resolved Name for jcr:root
     */
    protected String jcrRoot;

    /**
     * Resolved Name for jcr:contains
     */
    protected String jcrContains;

    /**
     * Resolved Name for jcr:deref
     */
    protected String jcrDeref;

    /**
     * The string /${jcrRoot}${testRoot} with all components of the test path
     * properly escaped for XPath.
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-714">JCR-714</a>
     */
    protected String xpathRoot;

    /**
     * The query object model factory for {@link #superuser}.
     */
    protected QueryObjectModelFactory qf;

    /**
     * The value factory for creating literals for the query object model.
     */
    protected ValueFactory vf;

    /**
     * The query manager for {@link #superuser}
     */
    protected QueryManager qm;

    /**
     * The identifier for the "XPATH" query syntax
     */
    @SuppressWarnings("deprecation")
	protected String qsXPATH = Query.XPATH;

    /**
     * The identifier for the "SQL" query syntax
     */
    @SuppressWarnings("deprecation")
	protected String qsSQL = Query.SQL;

    /**
     * Set-up the configuration values used for the test. Per default retrieves
     * a session, configures testRoot, and nodetype and checks if the query
     * language for the current language is available.<br>
     */
    protected void setUp() throws Exception {
        super.setUp();
        jcrScore = superuser.getNamespacePrefix(NS_JCR_URI) + ":score";
        jcrPath = superuser.getNamespacePrefix(NS_JCR_URI) + ":path";
        jcrRoot = superuser.getNamespacePrefix(NS_JCR_URI) + ":root";
        jcrContains = superuser.getNamespacePrefix(NS_JCR_URI) + ":contains";
        jcrDeref = superuser.getNamespacePrefix(NS_JCR_URI) + ":deref";
        xpathRoot = "/" + jcrRoot + ISO9075.encodePath(testRoot);
        qm = superuser.getWorkspace().getQueryManager();
        qf = qm.getQOMFactory();
        vf = superuser.getValueFactory();
    }

    protected void tearDown() throws Exception {
        qm = null;
        qf = null;
        vf = null;
        super.tearDown();
    }

    /**
     * Create a {@link Query} for a given {@link Statement}.
     *
     * @param statement the query should be created for
     * @return
     *
     * @throws RepositoryException
     * @see #createQuery(String, String)
     */
    protected Query createQuery(Statement statement)
        throws RepositoryException, NotExecutableException {
        return createQuery(statement.getStatement(), statement.getLanguage());
    }

    /**
     * Creates a {@link Query} for the given statement in the requested
     * language, treating optional languages gracefully
     * @throws RepositoryException
     */
    protected Query createQuery(String statement, String language) throws RepositoryException, NotExecutableException {
        return createQuery(superuser, statement, language);
    }

    /**
     * Creates a {@link Query} for the given statement in the requested
     * language, treating optional languages gracefully
     * @throws RepositoryException
     */
    protected Query createQuery(Session session, String statement, String language) throws RepositoryException, NotExecutableException {
        log.println("Creating query: " + statement);

        // check for unsupported query languages early
        if (! isSupportedLanguage(language) && !Query.JCR_SQL2.equals(language)) {
            throw new NotExecutableException("Repository does not support " + language + " query syntax");
        }
        return session.getWorkspace().getQueryManager().createQuery(statement, language);
    }

    /**
     * Creates and executes a {@link Query} for the given {@link Statement}
     *
     * @param statement to execute
     * @return
     *
     * @throws RepositoryException
     * @see #execute(String, String)
     */
    protected QueryResult execute(Statement statement)
        throws RepositoryException, NotExecutableException {
        return execute(statement.getStatement(), statement.getLanguage());
    }

    /**
     * Creates and executes a {@link Query} for a given Statement in a given
     * query language
     *
     * @param statement the query should be build for
     * @param language  query language the stement is written in
     * @return
     *
     * @throws RepositoryException
     */
    protected QueryResult execute(String statement, String language)
            throws RepositoryException, NotExecutableException {
        Query query = createQuery(statement, language);
        return query.execute();
    }

    /**
     * Checks if the <code>result</code> contains a number of
     * <code>hits</code>.
     *
     * @param result the <code>QueryResult</code>.
     * @param hits   the number of expected hits.
     * @throws RepositoryException if an error occurs while iterating over the
     *                             result nodes.
     */
    protected void checkResult(QueryResult result, int hits)
            throws RepositoryException {
        RowIterator itr = result.getRows();
        long count = itr.getSize();
        if (count == 0) {
            log.println(" NONE");
        } else if (count == -1) {
            // have to count in a loop
            count = 0;
            while (itr.hasNext()) {
                itr.nextRow();
                count++;
            }
        }
        assertEquals("Wrong hit count.", hits, count);
    }

    /**
     * Checks if the <code>result</code> contains a number of <code>hits</code>
     * and <code>properties</code>.
     *
     * @param result     the <code>QueryResult</code>.
     * @param hits       the number of expected hits.
     * @param properties the number of expected properties.
     * @throws RepositoryException if an error occurs while iterating over the
     *                             result nodes.
     */
    protected void checkResult(QueryResult result, int hits, int properties)
            throws RepositoryException {
        checkResult(result, hits);
        // now check property count
        int count = 0;
        log.println("Properties:");
        String[] propNames = result.getColumnNames();
        for (RowIterator it = result.getRows(); it.hasNext();) {
            StringBuilder msg = new StringBuilder();
            Value[] values = it.nextRow().getValues();
            for (int i = 0; i < propNames.length; i++, count++) {
                msg.append("  ").append(propNames[i]).append(": ");
                if (values[i] == null) {
                    msg.append("null");
                } else {
                    msg.append(values[i].getString());
                }
            }
            log.println(msg.toString());
        }
        if (count == 0) {
            log.println("  NONE");
        }
        assertEquals("Wrong property count.", properties, count);
    }

    /**
     * Checks if the {@link QueryResult} is ordered according order property in
     * direction of related argument.
     *
     * @param queryResult to be tested
     * @param propName    Name of the porperty to order by
     * @param descending  if <code>true</code> order has to be descending
     * @throws RepositoryException
     * @throws NotExecutableException in case of less than two results or all
     *                                results have same size of value in its
     *                                order-property
     */
    protected void evaluateResultOrder(QueryResult queryResult, String propName,
                                       boolean descending)
            throws RepositoryException, NotExecutableException {
        NodeIterator nodes = queryResult.getNodes();
        if (getSize(nodes) < 2) {
            fail("Workspace does not contain sufficient content to test ordering on result nodes.");
        }
        // need to re-aquire nodes, {@link #getSize} may consume elements.
        nodes = queryResult.getNodes();
        int changeCnt = 0;
        String last = descending ? "\uFFFF" : "";
        while (nodes.hasNext()) {
            String value = nodes.nextNode().getProperty(propName).getString();
            int cp = value.compareTo(last);
            // if value changed evaluate if the ordering is correct
            if (cp != 0) {
                changeCnt++;
                if (cp > 0 && descending) {
                    fail("Repository doesn't order properly descending");
                } else if (cp < 0 && !descending) {
                    fail("Repository doesn't order properly ascending");
                }
            }
            last = value;
        }
        if (changeCnt < 1) {
            fail("Workspace does not contain distinct values for " + propName);
        }
    }

    /**
     * Executes the <code>xpath</code> query and checks the results against
     * the specified <code>nodes</code>.
     * @param session the session to use for the query.
     * @param xpath the xpath query.
     * @param expectedNodes the expected nodes.
     * @throws NotExecutableException
     */
    protected void executeXPathQuery(Session session, String xpath, Node[] expectedNodes)
            throws RepositoryException, NotExecutableException {
        QueryResult res = createQuery(session, xpath, qsXPATH).execute();
        checkResult(res, expectedNodes, null);
    }

    /**
     * Executes the <code>sql</code> query and checks the results against
     * the specified <code>nodes</code>.
     * @param session the session to use for the query.
     * @param sql the sql query.
     * @param expectedNodes the expected nodes.
     * @throws NotExecutableException
     */
    protected void executeSqlQuery(Session session, String sql, Node[] expectedNodes)
            throws RepositoryException, NotExecutableException {
    		executeSqlQuery(session, sql, expectedNodes, null);
    }

    /**
     * Executes the <code>sql</code> query and checks the results against
     * the specified <code>nodes</code>.
     * @param session the session to use for the query.
     * @param sql the sql query.
     * @param requiredNodes the nodes that need to be in the result set
     * 		(null if no node is required).
     * @param optionalNodes the nodes that may be in the result set
     * 		(null if no node is optional).
     * @throws NotExecutableException
     */
    protected void executeSqlQuery(Session session, String sql, Node[] requiredNodes, Node[] optionalNodes)
            throws RepositoryException, NotExecutableException {
        QueryResult res = createQuery(session, sql, qsSQL).execute();
        checkResult(res, requiredNodes, optionalNodes);
    }

    /**
     * Checks if the result set contains exactly the <code>nodes</code>.
     * @param result the query result.
     * @param expectedNodes the expected nodes.
     */
    protected void checkResult(QueryResult result, Node[] expectedNodes)
    			throws RepositoryException {
    		checkResult(result, expectedNodes, null);
	}

    /**
     * Checks if the result set contains exactly the <code>nodes</code>.
     * @param result the query result.
     * @param requiredNodes the nodes that need to be in the result set
     * 		(null if no node is required).
     * @param optionalNodes the nodes that may be in the result set
     * 		(null if no node is optional).
     */
    protected void checkResult(QueryResult result, Node[] requiredNodes, Node[] optionalNodes)
            throws RepositoryException {
        // collect paths
        Set<String> requiredPaths = getPathSet(requiredNodes);
        Set<String> optionalPaths = getPathSet(optionalNodes);
        Set<String> resultPaths = new HashSet<String>();
        for (NodeIterator it = result.getNodes(); it.hasNext();) {
            resultPaths.add(it.nextNode().getPath());
        }
        // check if all required nodes are in result
        for (Iterator<String> it = requiredPaths.iterator(); it.hasNext();) {
            String path = it.next();
            assertTrue(path + " is not part of the result set", resultPaths.contains(path));
        }
        // check result does not contain more than expected
        for (Iterator<String> it = resultPaths.iterator(); it.hasNext();) {
            String path = it.next();
            if (!optionalPaths.contains(path)) {
            		assertTrue(path + " is not expected to be part of the result set", requiredPaths.contains(path));
            }
        }
    }

    private static HashSet<String> getPathSet(Node[] nodes) throws RepositoryException {
    		HashSet<String> paths = new HashSet<String>();
    		if (nodes != null) {
    	        for (int i = 0; i < nodes.length; i++) {
    	        		paths.add(nodes[i].getPath());
        		}
    		}
    		return paths;
    }

    /**
     * Returns the nodes in <code>it</code> as an array of Nodes.
     * @param it the NodeIterator.
     * @return the elements of the iterator as an array of Nodes.
     */
    protected Node[] toArray(NodeIterator it) {
        List<Node> nodes = new ArrayList<Node>();
        while (it.hasNext()) {
            nodes.add(it.nextNode());
        }
        return nodes.toArray(new Node[nodes.size()]);
    }

    /**
     * Escape an identifier suitable for the SQL parser
     * <p>
     * TODO: currently only handles dash character
     */
    protected String escapeIdentifierForSQL(String identifier) {

        boolean needsEscaping = identifier.indexOf('-') >= 0;

        if (!needsEscaping) {
            return identifier;
        }
        return '"' + identifier + '"';
    }

    /**
     * @param language a query language.
     * @return <code>true</code> if <code>language</code> is supported;
     *         <code>false</code> otherwise.
     * @throws RepositoryException if an error occurs.
     */
    protected boolean isSupportedLanguage(String language)
            throws RepositoryException {
        return Arrays.asList(qm.getSupportedQueryLanguages()).contains(language);
    }
}
