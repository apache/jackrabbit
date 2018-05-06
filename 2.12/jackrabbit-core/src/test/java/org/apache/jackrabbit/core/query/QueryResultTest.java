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
import javax.jcr.NodeIterator;
import javax.jcr.query.QueryManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.util.NoSuchElementException;

/**
 * <code>QueryResultTest</code> tests various methods on the
 * <code>NodeIterator</code> returned by a <code>QueryResult</code>.
 */
public class QueryResultTest extends AbstractQueryTest {

    private static int INITIAL_NODE_NUM = 55;

    protected void setUp() throws Exception {
        super.setUp();
        for (int i = 0; i < INITIAL_NODE_NUM; i++) {
            testRootNode.addNode("node" + i).setProperty(propertyName1, i);
        }
        testRootNode.save();
    }

    public void testGetSize() throws RepositoryException {
        QueryManager qm = superuser.getWorkspace().getQueryManager();
        for (int i = 0; i < 10; i++) {
            String stmt = testPath + "/*[@" + propertyName1 + " < 1000]";
            QueryResult result = qm.createQuery(stmt, Query.XPATH).execute();
            assertEquals("Wrong size of NodeIterator in result",
                    INITIAL_NODE_NUM - i, result.getNodes().getSize());
            // remove node for the next iteration
            testRootNode.getNode("node" + i).remove();
            testRootNode.save();
        }
    }

    public void testGetSizeOrderByScore() throws RepositoryException {
        QueryManager qm = superuser.getWorkspace().getQueryManager();
        for (int i = 0; i < 10; i++) {
            String stmt = testPath + "/*[@" + propertyName1 + " < 1000] order by jcr:score()";
            QueryResult result = qm.createQuery(stmt, Query.XPATH).execute();
            assertEquals("Wrong size of NodeIterator in result",
                    INITIAL_NODE_NUM - i, result.getNodes().getSize());
            // remove node for the next iteration
            testRootNode.getNode("node" + i).remove();
            testRootNode.save();
        }
    }

    public void testIteratorNext() throws RepositoryException {
        QueryManager qm = superuser.getWorkspace().getQueryManager();
        for (int i = 0; i < 10; i++) {
            String stmt = testPath + "/*[@" + propertyName1 + " < 1000]";
            QueryResult result = qm.createQuery(stmt, Query.XPATH).execute();
            int size = 0;
            for (NodeIterator it = result.getNodes(); it.hasNext(); ) {
                it.nextNode();
                size++;
            }
            assertEquals("Wrong size of NodeIterator in result",
                    INITIAL_NODE_NUM - i, size);
            // remove node for the next iteration
            testRootNode.getNode("node" + i).remove();
            testRootNode.save();
        }
    }

    public void testIteratorNextOrderByScore() throws RepositoryException {
        QueryManager qm = superuser.getWorkspace().getQueryManager();
        for (int i = 0; i < 10; i++) {
            String stmt = testPath + "/*[@" + propertyName1 + " < 1000] order by jcr:score()";
            QueryResult result = qm.createQuery(stmt, Query.XPATH).execute();
            int size = 0;
            for (NodeIterator it = result.getNodes(); it.hasNext(); ) {
                it.nextNode();
                size++;
            }
            assertEquals("Wrong size of NodeIterator in result",
                    INITIAL_NODE_NUM - i, size);
            // remove node for the next iteration
            testRootNode.getNode("node" + i).remove();
            testRootNode.save();
        }
    }

    public void testSkip() throws RepositoryException {
        QueryManager qm = superuser.getWorkspace().getQueryManager();
        for (int i = 0; i < 10; i++) {
            String stmt = testPath + "/*[@" + propertyName1 + " < 1000]";
            QueryResult result = qm.createQuery(stmt, Query.XPATH).execute();
            for (int j = 0; j < INITIAL_NODE_NUM - i; j++) {
                // skip to each node in the result
                NodeIterator it = result.getNodes();
                it.skip(j);
                long propValue = it.nextNode().getProperty(propertyName1).getLong();
                // expected = number of skipped nodes + number of deleted nodes
                long expected = j + i;
                assertEquals("Wrong node after skip()", expected, propValue);
            }
            try {
                NodeIterator it = result.getNodes();
                it.skip(it.getSize() + 1);
                fail("must throw NoSuchElementException");
            } catch (NoSuchElementException e) {
                // correct
            }
            // remove node for the next iteration
            testRootNode.getNode("node" + i).remove();
            testRootNode.save();
        }
    }

    public void testSkipOrderByProperty() throws RepositoryException {
        QueryManager qm = superuser.getWorkspace().getQueryManager();
        for (int i = 0; i < 10; i++) {
            String stmt = testPath + "/*[@" + propertyName1 +
                    " < 1000] order by @" + propertyName1;
            QueryResult result = qm.createQuery(stmt, Query.XPATH).execute();
            for (int j = 0; j < INITIAL_NODE_NUM - i; j++) {
                // skip to each node in the result
                NodeIterator it = result.getNodes();
                it.skip(j);
                long propValue = it.nextNode().getProperty(propertyName1).getLong();
                // expected = number of skipped nodes + number of deleted nodes
                long expected = j + i;
                assertEquals("Wrong node after skip()", expected, propValue);
            }
            try {
                NodeIterator it = result.getNodes();
                it.skip(it.getSize() + 1);
                fail("must throw NoSuchElementException");
            } catch (NoSuchElementException e) {
                // correct
            }
            // remove node for the next iteration
            testRootNode.getNode("node" + i).remove();
            testRootNode.save();
        }
    }

    public void testGetPosition() throws RepositoryException {
        QueryManager qm = superuser.getWorkspace().getQueryManager();
        for (int i = 0; i < 10; i++) {
            String stmt = testPath + "/*[@" + propertyName1 + " < 1000]";
            QueryResult result = qm.createQuery(stmt, Query.XPATH).execute();
            NodeIterator it = result.getNodes();
            assertEquals("Wrong position", 0, it.getPosition());
            int count = 0;
            while (it.hasNext()) {
                long position = it.getPosition();
                it.nextNode();
                assertEquals("Wrong position", count++, position);
            }
            try {
                it.next();
                fail("must throw NoSuchElementException");
            } catch (Exception e) {
                // correct
            }
            // remove node for the next iteration
            testRootNode.getNode("node" + i).remove();
            testRootNode.save();
        }
    }

    public void testGetPositionOrderBy() throws RepositoryException {
        QueryManager qm = superuser.getWorkspace().getQueryManager();
        for (int i = 0; i < 10; i++) {
            String stmt = testPath + "/*[@" + propertyName1 + " < 1000] order by jcr:score()";
            QueryResult result = qm.createQuery(stmt, Query.XPATH).execute();
            NodeIterator it = result.getNodes();
            assertEquals("Wrong position", 0, it.getPosition());
            int count = 0;
            while (it.hasNext()) {
                long position = it.getPosition();
                it.nextNode();
                assertEquals("Wrong position", count++, position);
            }
            try {
                it.next();
                fail("must throw NoSuchElementException");
            } catch (Exception e) {
                // correct
            }
            // remove node for the next iteration
            testRootNode.getNode("node" + i).remove();
            testRootNode.save();
        }
    }

    public void testPositionEmptyResult() throws RepositoryException {
        QueryManager qm = superuser.getWorkspace().getQueryManager();
        String stmt = testPath + "/*[@" + propertyName1 + " > 1000]";
        QueryResult result = qm.createQuery(stmt, Query.XPATH).execute();
        assertEquals("Wrong position", 0, result.getNodes().getPosition());
        assertEquals("Wrong position", 0, result.getRows().getPosition());
        stmt += " order by jcr:score()";
        result = qm.createQuery(stmt, Query.XPATH).execute();
        assertEquals("Wrong position", 0, result.getNodes().getPosition());
        assertEquals("Wrong position", 0, result.getRows().getPosition());
    }
}
