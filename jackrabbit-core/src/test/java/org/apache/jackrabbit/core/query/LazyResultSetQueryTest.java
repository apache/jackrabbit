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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import org.junit.Ignore;

/**
 * 
 * This is meant to test the limits of queries and lazy fetching of nodes
 * 
 * @see <a href="https://issues.apache.org/jira/browse/JCR-3477">JCR-3477</a>
 */
public class LazyResultSetQueryTest extends AbstractQueryTest {

    @Ignore("JCR-3477")
    public void testResultSet() throws RepositoryException {
        int count = createNodes(testRootNode, 10, 5, 0);
        testRootNode.getSession().save();

        String stmt = testPath + "//*[@" + jcrPrimaryType + "]";
        // + " order by @jcr:score descending";

        readResult(executeQuery(stmt), count);
    }

    protected void tearDown() throws Exception {
        int count = 0;
        for (NodeIterator it = testRootNode.getNodes(); it.hasNext();) {
            it.nextNode().remove();
            count++;
            if (count % 10000 == 0) {
                testRootNode.getSession().save();
            }
        }
        testRootNode.getSession().save();
        super.tearDown();
    }

    private void readResult(QueryResult result, int count)
            throws RepositoryException {
        for (RowIterator rows = result.getRows(); rows.hasNext();) {
            rows.nextRow();
            count--;
        }
        assertEquals(0, count);
    }

    private int createNodes(Node n, int nodesPerLevel, int levels, int count)
            throws RepositoryException {
        levels--;
        for (int i = 0; i < nodesPerLevel; i++) {
            Node child = n.addNode("node" + i);
            count++;
            if (count % 10000 == 0) {
                n.getSession().save();
            }
            if (levels > 0) {
                count = createNodes(child, nodesPerLevel, levels, count);
            }
        }
        return count;
    }
}
