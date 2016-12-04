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
package org.apache.jackrabbit.test.api.query.qom;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.query.qom.QueryObjectModel;

import org.apache.jackrabbit.test.api.query.AbstractQueryTest;

/**
 * <code>AbstractQOMTest</code> is a base class for test cases on the JQOM.
 */
public abstract class AbstractQOMTest extends AbstractQueryTest {

    /**
     * Binds the given <code>value</code> to the variable named
     * <code>var</code>.
     *
     * @param q     the query
     * @param var   name of variable in query
     * @param value value to bind
     * @throws IllegalArgumentException if <code>var</code> is not a valid
     *                                  variable in this query.
     * @throws RepositoryException      if an error occurs.
     */
    protected void bindVariableValue(Query q, String var, Value value)
            throws RepositoryException {
        q.bindValue(var, value);
    }

    protected void checkResultOrder(QueryObjectModel qom,
                                    String[] selectorNames,
                                    Node[][] nodes)
            throws RepositoryException {
        checkResultOrder(qom.execute(), selectorNames, nodes);
        checkResultOrder(qm.createQuery(qom.getStatement(), Query.JCR_SQL2).execute(),
                selectorNames, nodes);
    }

    protected void checkResultOrder(QueryResult result,
                                    String[] selectorNames,
                                    Node[][] nodes)
            throws RepositoryException {
        // collect rows
        List<String> expectedPaths = new ArrayList<String>();
        log.println("expected:");
        for (int i = 0; i < nodes.length; i++) {
            StringBuffer aggregatedPaths = new StringBuffer();
            for (int j = 0; j < nodes[i].length; j++) {
                aggregatedPaths.append(getPath(nodes[i][j]));
                aggregatedPaths.append("|");
            }
            expectedPaths.add(aggregatedPaths.toString());
            log.println(aggregatedPaths.toString());
        }

        List<String> resultPaths = new ArrayList<String>();
        log.println("result:");
        for (RowIterator it = result.getRows(); it.hasNext();) {
            Row r = it.nextRow();
            StringBuffer aggregatedPaths = new StringBuffer();
            for (int i = 0; i < selectorNames.length; i++) {
                aggregatedPaths.append(getPath(r.getNode(selectorNames[i])));
                aggregatedPaths.append("|");
            }
            resultPaths.add(aggregatedPaths.toString());
            log.println(aggregatedPaths.toString());
        }

        assertEquals("wrong result order", expectedPaths, resultPaths);
    }

    /**
     * Checks the query object model by executing it directly and matching the
     * result against the given <code>nodes</code>. Then the QOM is executed
     * again using {@link QueryObjectModel#getStatement()} with {@link
     * Query#JCR_SQL2}.
     *
     * @param qom   the query object model to check.
     * @param nodes the result nodes.
     * @throws RepositoryException if an error occurs while executing the
     *                             query.
     */
    protected void checkQOM(QueryObjectModel qom, Node[] nodes)
            throws RepositoryException {
        checkResult(qom.execute(), nodes);
        checkResult(qm.createQuery(qom.getStatement(), Query.JCR_SQL2).execute(), nodes);
    }

    /**
     * Checks the query object model by executing it directly and matching the
     * result against the given <code>nodes</code>. Then the QOM is executed
     * again using {@link QueryObjectModel#getStatement()} with
     * {@link Query#JCR_SQL2}.
     *
     * @param qom           the query object model to check.
     * @param selectorNames the selector names of the qom.
     * @param nodes         the result nodes.
     * @throws RepositoryException if an error occurs while executing the
     *                             query.
     */
    protected void checkQOM(QueryObjectModel qom,
                            String[] selectorNames,
                            Node[][] nodes) throws RepositoryException {
        checkResult(qom.execute(), selectorNames, nodes);
        checkResult(qm.createQuery(qom.getStatement(), Query.JCR_SQL2).execute(),
                selectorNames, nodes);
    }

    protected void checkResult(QueryResult result,
                               String[] selectorNames,
                               Node[][] nodes)
            throws RepositoryException {
        // collect rows
        Set<String> expectedPaths = new HashSet<String>();
        log.println("expected:");
        for (int i = 0; i < nodes.length; i++) {
            StringBuffer aggregatedPaths = new StringBuffer();
            for (int j = 0; j < nodes[i].length; j++) {
                aggregatedPaths.append(getPath(nodes[i][j]));
                aggregatedPaths.append("|");
            }
            expectedPaths.add(aggregatedPaths.toString());
            log.println(aggregatedPaths.toString());
        }

        Set<String> resultPaths = new HashSet<String>();
        log.println("result:");
        for (RowIterator it = result.getRows(); it.hasNext();) {
            Row r = it.nextRow();
            StringBuffer aggregatedPaths = new StringBuffer();
            for (int i = 0; i < selectorNames.length; i++) {
                aggregatedPaths.append(getPath(r.getNode(selectorNames[i])));
                aggregatedPaths.append("|");
            }
            resultPaths.add(aggregatedPaths.toString());
            log.println(aggregatedPaths.toString());
        }

        // check if all expected are in result
        for (Iterator<String> it = expectedPaths.iterator(); it.hasNext();) {
            String path = it.next();
            assertTrue(path + " is not part of the result set", resultPaths.contains(path));
        }
        // check result does not contain more than expected
        for (Iterator<String> it = resultPaths.iterator(); it.hasNext();) {
            String path = it.next();
            assertTrue(path + " is not expected to be part of the result set", expectedPaths.contains(path));
        }
    }

    /**
     * Returns the path of the <code>node</code> or an empty string if
     * <code>node</code> is <code>null</code>.
     *
     * @param node a node or <code>null</code>.
     * @return the path of the node or an empty string if <code>node</code> is
     *         <code>null</code>.
     * @throws RepositoryException if an error occurs while reading from the
     *                             repository.
     */
    protected static String getPath(Node node) throws RepositoryException {
        if (node != null) {
            return node.getPath();
        } else {
            return "";
        }
    }

    /**
     * Calls back the <code>callable</code> first with the <code>qom</code> and
     * then a JCR_SQL2 query created from {@link QueryObjectModel#getStatement()}.
     *
     * @param qom      a query object model.
     * @param callable the callback.
     * @throws RepositoryException if an error occurs.
     */
    protected void forQOMandSQL2(QueryObjectModel qom, Callable callable)
            throws RepositoryException {
        List<Query> queries = new ArrayList<Query>();
        queries.add(qom);
        queries.add(qm.createQuery(qom.getStatement(), Query.JCR_SQL2));
        for (Iterator<Query> it = queries.iterator(); it.hasNext();) {
            callable.call(it.next());
        }
    }

    protected interface Callable {

        public Object call(Query query) throws RepositoryException;
    }
}
