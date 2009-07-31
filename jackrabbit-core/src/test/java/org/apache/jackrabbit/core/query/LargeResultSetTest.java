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

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.RowIterator;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.core.query.lucene.SearchIndex;

/**
 * <code>LargeResultSetTest</code>...
 */
public class LargeResultSetTest extends AbstractQueryTest {

    public void testResultSet() throws RepositoryException {
        createNodes(testRootNode, 10, 5, 0);
        superuser.save();

        SearchIndex index = getSearchIndex();
        int resultFetchSize = index.getResultFetchSize();
        try {
            String stmt = testPath + "//*[@" + jcrPrimaryType + "] order by @jcr:score descending";

            // with result fetch size Integer.MAX_VALUE
            readResult(executeQuery(stmt));

            // with result fetch size 100
            index.setResultFetchSize(100);
            readResult(executeQuery(stmt));

            // with 100 limit
            QueryImpl query = (QueryImpl) qm.createQuery(stmt, Query.XPATH);
            query.setLimit(100);
            readResult(query.execute());
        } finally {
            index.setResultFetchSize(resultFetchSize);
        }

        for (NodeIterator it = testRootNode.getNodes(); it.hasNext(); ) {
            it.nextNode().remove();
            superuser.save();
        }
    }

    private void readResult(QueryResult result) throws RepositoryException {
        for (RowIterator rows = result.getRows(); rows.hasNext(); ) {
            rows.nextRow();
        }
    }

    private int createNodes(Node n, int nodesPerLevel, int levels, int count)
            throws RepositoryException {
        levels--;
        for (int i = 0; i < nodesPerLevel; i++) {
            Node child = n.addNode("node" + i);
            count++;
            if (count % 10000 == 0) {
                superuser.save();
            }
            if (levels > 0) {
                count = createNodes(child, nodesPerLevel, levels, count);
            }
        }
        return count;
    }
}
