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
package org.apache.jackrabbit.core.query;

import static javax.jcr.query.Query.JCR_SQL2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.query.qom.QueryObjectModelFactory;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Abstract base class for query test cases.
 */
public class AbstractQueryTest extends AbstractJCRTest {

    protected QueryManager qm;

    protected QueryObjectModelFactory qomFactory;

    protected void setUp() throws Exception {
        super.setUp();
        qm = superuser.getWorkspace().getQueryManager();
        qomFactory = qm.getQOMFactory();
    }

    protected void tearDown() throws Exception {
        qm = null;
        qomFactory = null;
        super.tearDown();
    }

    /**
     * Checks if the <code>result</code> contains a number of <code>hits</code>.
     *
     * @param result the <code>QueryResult</code>.
     * @param hits   the number of expected hits.
     * @throws RepositoryException if an error occurs while iterating over
     *                             the result nodes.
     */
    protected void checkResult(QueryResult result, int hits)
            throws RepositoryException {
        int count = 0;
        log.println("Rows:");
        for (Row row : JcrUtils.getRows(result)) {
            log.println(" " + row);
            count++;
        }
        if (count == 0) {
            log.println(" NONE");
        }
        assertEquals("Wrong hit count.", hits, count);
    }

    /**
     * Checks if the <code>result</code> contains a number of <code>hits</code>
     * and <code>columns</code>.
     *
     * @param result  the <code>QueryResult</code>.
     * @param hits    the number of expected hits.
     * @param columns the number of expected columns.
     * @throws RepositoryException if an error occurs while iterating over the
     *                             result nodes.
     */
    protected void checkResult(QueryResult result, int hits, int columns)
            throws RepositoryException {
        checkResult(result, hits);
        // now check column count
        int count = 0;
        log.println("Properties:");
        String[] propNames = result.getColumnNames();
        for (RowIterator it = result.getRows(); it.hasNext(); count++) {
            StringBuffer msg = new StringBuffer();
            Value[] values = it.nextRow().getValues();
            for (int i = 0; i < propNames.length; i++) {
                msg.append("  ").append(propNames[i]).append(": ");
                if (values[i] == null) {
                    msg.append("null");
                } else {
                    msg.append(values[i].getString());
                }
            }
            log.println(msg);
        }
        if (count == 0) {
            log.println("  NONE");
        }
        assertEquals("Wrong column count.", columns, count);
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
     * Executes the <code>xpath</code> query and checks the results against
     * the specified <code>nodes</code>.
     * @param xpath the xpath query.
     * @param nodes the expected result nodes.
     * @throws RepositoryException if an error occurs while executing the query
     *                             or checking the result.
     */
    protected void executeXPathQuery(String xpath, Node[] nodes)
            throws RepositoryException {
        QueryResult res = qm.createQuery(xpath, Query.XPATH).execute();
        checkResult(res, nodes);
    }

    /**
     * Executes the <code>sql</code> query and checks the results against
     * the specified <code>nodes</code>.
     * @param sql the sql query.
     * @param nodes the expected result nodes.
     * @throws RepositoryException if an error occurs while executing the query
     *                             or checking the result.
     */
    protected void executeSQLQuery(String sql, Node[] nodes)
            throws RepositoryException {
        QueryResult res = qm.createQuery(sql, Query.SQL).execute();
        checkResult(res, nodes);
    }

    /**
     * Checks if the result set contains exactly the <code>nodes</code>.
     * @param result the query result.
     * @param nodes the expected nodes in the result set.
     * @throws RepositoryException if an error occurs while reading from the result.
     */
    protected void checkResult(QueryResult result, Node[] nodes)
            throws RepositoryException {
        checkResult(result.getNodes(), nodes);
    }

    /**
     * Checks if the result contains exactly the <code>nodes</code>.
     * @param result the query result.
     * @param nodes the expected nodes in the result set.
     * @throws RepositoryException if an error occurs while reading from the result.
     */
    protected void checkResult(RowIterator result, Node[] nodes)
            throws RepositoryException {
        checkResult(new NodeIteratorAdapter(result) {
            public Object next() throws NoSuchElementException {
                Row next = (Row) super.next();
                try {
                    return superuser.getItem(next.getValue("jcr:path").getString());
                } catch (RepositoryException e) {
                    throw new NoSuchElementException();
                }
            }
        }, nodes);
    }

    /**
     * Checks if the result contains exactly the <code>nodes</code>.
     * @param result the query result.
     * @param nodes the expected nodes in the result set.
     * @throws RepositoryException if an error occurs while reading from the result.
     */
    protected void checkResult(NodeIterator result, Node[] nodes)
            throws RepositoryException {
        // collect paths
        Set<String> expectedPaths = new HashSet<String>();
        for (Node n : nodes) {
            expectedPaths.add(n.getPath());
        }
        Set<String> resultPaths = new HashSet<String>();
        while (result.hasNext()) {
            resultPaths.add(result.nextNode().getPath());
        }
        // check if all expected are in result
        for (Iterator<String> it = expectedPaths.iterator(); it.hasNext();) {
            String path = it.next();
            assertTrue(path + " is not part of the result set "+ resultPaths, resultPaths.contains(path));
        }
        // check result does not contain more than expected
        for (Iterator<String> it = resultPaths.iterator(); it.hasNext();) {
            String path = it.next();
            assertTrue(path + " is not expected to be part of the result set " + expectedPaths, expectedPaths.contains(path));
        }
    }

    /**
     * Checks if the result set contains exactly the <code>nodes</code> in the
     * given sequence.
     *
     * @param result the query result.
     * @param nodes the expected nodes in the result set.
     * @throws RepositoryException if an error occurs while reading from the result.
     */
    protected void checkResultSequence(RowIterator result, Node[] nodes)
            throws RepositoryException {
        for (int i = 0; i < nodes.length; i++) {
            assertTrue("No more results, expected " + nodes[i].getPath(), result.hasNext());
            String path = result.nextRow().getValue("jcr:path").getString();
            assertEquals("Wrong sequence", nodes[i].getPath(), path);
        }
        assertFalse("No more result expected", result.hasNext());
    }

    /**
     * Executes the query specified by <code>statement</code> and returns the
     * query result.
     *
     * @param statement either a SQL or XPath statement.
     * @return the query result.
     * @throws RepositoryException if an error occurs.
     */
    protected QueryResult executeQuery(String statement)
            throws RepositoryException {
        if (statement.trim().toLowerCase().startsWith("select")) {
            return qm.createQuery(statement, Query.SQL).execute();
        } else {
            return qm.createQuery(statement, Query.XPATH).execute();
        }
    }

    protected QueryResult executeSQL2Query(String statement)
            throws RepositoryException {
        return qm.createQuery(statement, JCR_SQL2).execute();
    }

    protected void executeSQL2Query(String statement, Node[] nodes)
            throws RepositoryException {
        QueryResult res = qm.createQuery(statement, JCR_SQL2).execute();
        checkResult(res, nodes);
    }
}
