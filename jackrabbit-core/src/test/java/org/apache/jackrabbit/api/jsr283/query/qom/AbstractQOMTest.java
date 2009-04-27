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
package org.apache.jackrabbit.api.jsr283.query.qom;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.jackrabbit.test.api.query.AbstractQueryTest;

import org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModelFactory;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModelConstants;
import org.apache.jackrabbit.core.query.QueryManagerImpl;
import org.apache.jackrabbit.core.query.QueryImpl;
import org.apache.jackrabbit.api.jsr283.query.Row;

import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.Node;

/**
 * <code>AbstractQOMTest</code> is a base class for test cases on the JQOM.
 */
public class AbstractQOMTest
        extends AbstractQueryTest
        implements QueryObjectModelConstants {

    protected QueryObjectModelFactory qomFactory;

    protected void setUp() throws Exception {
        super.setUp();
        QueryManagerImpl qm = (QueryManagerImpl) superuser.getWorkspace().getQueryManager();
        qomFactory = qm.getQOMFactory();
    }

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
        // TODO: remove cast when bindValue() is available on JSR 283 Query
        ((QueryImpl) q).bindValue(var, value);
    }

    protected void checkResult(QueryResult result,
                               String[] selectorNames,
                               Node[][] nodes)
            throws RepositoryException {
        // collect rows
        Set expectedPaths = new HashSet();
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

        Set resultPaths = new HashSet();
        log.println("result:");
        for (RowIterator it = result.getRows(); it.hasNext();) {
            Row r = (Row) it.nextRow();
            StringBuffer aggregatedPaths = new StringBuffer();
            for (int i = 0; i < selectorNames.length; i++) {
                aggregatedPaths.append(getPath(r.getNode(selectorNames[i])));
                aggregatedPaths.append("|");
            }
            resultPaths.add(aggregatedPaths.toString());
            log.println(aggregatedPaths.toString());
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
}
