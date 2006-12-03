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

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.query.Query;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.Value;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Abstract base class for query test cases.
 */
public class AbstractQueryTest extends AbstractJCRTest {

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
        log.println("Nodes:");
        for (NodeIterator nodes = result.getNodes(); nodes.hasNext(); count++) {
            Node n = nodes.nextNode();
            log.println(" " + n.getPath());
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
        List nodes = new ArrayList();
        while (it.hasNext()) {
            nodes.add(it.nextNode());
        }
        return (Node[]) nodes.toArray(new Node[nodes.size()]);
    }

    /**
     * Executes the <code>xpath</code> query and checks the results against
     * the specified <code>nodes</code>.
     * @param xpath the xpath query.
     * @param nodes the expected result nodes.
     */
    protected void executeXPathQuery(String xpath, Node[] nodes)
            throws RepositoryException {
        QueryResult res = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH).execute();
        checkResult(res, nodes);
    }

    /**
     * Executes the <code>sql</code> query and checks the results against
     * the specified <code>nodes</code>.
     * @param sql the sql query.
     * @param nodes the expected result nodes.
     */
    protected void executeSQLQuery(String sql, Node[] nodes)
            throws RepositoryException {
        QueryResult res = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL).execute();
        checkResult(res, nodes);
    }

    /**
     * Checks if the result set contains exactly the <code>nodes</code>.
     * @param result the query result.
     * @param nodes the expected nodes in the result set.
     */
    protected void checkResult(QueryResult result, Node[] nodes)
            throws RepositoryException {
        // collect paths
        Set expectedPaths = new HashSet();
        for (int i = 0; i < nodes.length; i++) {
            expectedPaths.add(nodes[i].getPath());
        }
        Set resultPaths = new HashSet();
        for (NodeIterator it = result.getNodes(); it.hasNext();) {
            resultPaths.add(it.nextNode().getPath());
        }
        // check if all expected are in result
        for (Iterator it = expectedPaths.iterator(); it.hasNext();) {
            String path = (String) it.next();
            assertTrue(path + " is not part of the result set", resultPaths.contains(path));
        }
        // check result does not contain more than expected
        for (Iterator it = resultPaths.iterator(); it.hasNext();) {
            String path = (String) it.next();
            assertTrue(path + " is not expected to be part of the result set", expectedPaths.contains(path));
        }
    }
}
