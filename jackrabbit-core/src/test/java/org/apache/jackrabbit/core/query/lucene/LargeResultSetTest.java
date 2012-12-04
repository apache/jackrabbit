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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import org.apache.jackrabbit.core.query.AbstractIndexingTest;

/**
 * <code>LargeResultSetTest</code>...
 * 
 * TODO what does this test actually do?
 */
public class LargeResultSetTest extends AbstractIndexingTest {

    public void testResultSet() throws RepositoryException {
        int count = createNodes(testRootNode, 10, 5, 0);
        session.save();

        SearchIndex index = getSearchIndex();
        int resultFetchSize = index.getResultFetchSize();
        try {
            String stmt = testPath + "//*[@" + jcrPrimaryType + "] order by @jcr:score descending";

            // with result fetch size Integer.MAX_VALUE
            readResult(executeQuery(stmt), count);

            // with result fetch size 100
            index.setResultFetchSize(100);
            readResult(executeQuery(stmt), count);

            // with 100 limit
            Query query = qm.createQuery(stmt, Query.XPATH);
            query.setLimit(100);
            readResult(query.execute(), 100);
        } finally {
            index.setResultFetchSize(resultFetchSize);
        }
    }

    protected void tearDown() throws Exception {
        int count = 0;
        for (NodeIterator it = testRootNode.getNodes(); it.hasNext();) {
            it.nextNode().remove();
            count++;
            if (count % 10000 == 0) {
                session.save();
            }
        }
        session.save();
        super.tearDown();
    }

    /*
     * use default ws
     */
    protected String getWorkspaceName() {
        return null;
    }

    private void readResult(QueryResult result, int count) throws RepositoryException {
        for (RowIterator rows = result.getRows(); rows.hasNext(); ) {
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
                session.save();
            }
            if (levels > 0) {
                count = createNodes(child, nodesPerLevel, levels, count);
            }
        }
        return count;
    }
}
