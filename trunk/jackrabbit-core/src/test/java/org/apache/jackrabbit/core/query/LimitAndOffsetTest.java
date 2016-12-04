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
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.api.query.JackrabbitQueryResult;

public class LimitAndOffsetTest extends AbstractQueryTest {

    private Node node1;
    private Node node2;
    private Node node3;

    private Query query;

    protected void setUp() throws Exception {
        super.setUp();

        node1 = testRootNode.addNode("foo");
        node1.setProperty("name", "1");
        node2 = testRootNode.addNode("bar");
        node2.setProperty("name", "2");
        node3 = testRootNode.addNode("baz");
        node3.setProperty("name", "3");

        testRootNode.getSession().save();

        query = qm.createQuery("/jcr:root" + testRoot + "/* order by @name",
                Query.XPATH);
    }

    protected void tearDown() throws Exception {
        node1 = null;
        node2 = null;
        node3 = null;
        query = null;
        super.tearDown();
    }

    protected void checkResult(QueryResult result, Node[] expectedNodes) throws RepositoryException {
        assertEquals(expectedNodes.length, result.getNodes().getSize());
    }

    public void testLimit() throws Exception {
        query.setLimit(1);
        QueryResult result = query.execute();
        checkResult(result, new Node[] { node1 });

        query.setLimit(2);
        result = query.execute();
        checkResult(result, new Node[] { node1, node2 });

        query.setLimit(3);
        result = query.execute();
        checkResult(result, new Node[] { node1, node2, node3 });
    }

    public void testOffset() throws Exception {
        query.setOffset(0);
        QueryResult result = query.execute();
        checkResult(result, new Node[] { node1, node2, node3 });

        query.setOffset(1);
        result = query.execute();
        checkResult(result, new Node[] { node2, node3 });

        query.setOffset(2);
        result = query.execute();
        checkResult(result, new Node[] { node3 });
    }

    public void testOffsetAndLimit() throws Exception {
        query.setOffset(0);
        query.setLimit(1);
        QueryResult result = query.execute();
        checkResult(result, new Node[] { node1 });

        query.setOffset(1);
        query.setLimit(1);
        result = query.execute();
        checkResult(result, new Node[] { node2 });

        query.setOffset(1);
        query.setLimit(2);
        result = query.execute();
        checkResult(result, new Node[] { node2, node3 });

        query.setOffset(0);
        query.setLimit(2);
        result = query.execute();
        checkResult(result, new Node[] { node1, node2 });

        // Added for JCR-1323
        query.setOffset(0);
        query.setLimit(4);
        result = query.execute();
        checkResult(result, new Node[] { node1, node2, node3 });
    }

    public void testOffsetAndSkip() throws Exception {
        query.setOffset(1);
        QueryResult result = query.execute();
        NodeIterator nodes = result.getNodes();
        nodes.skip(1);
        assertTrue(node3.isSame(nodes.nextNode()));
    }

    public void testOffsetAndLimitWithGetSize() throws Exception {
        query.setOffset(1);
        QueryResult result = query.execute();
        NodeIterator nodes = result.getNodes();
        assertEquals(2, nodes.getSize());
        if (result instanceof JackrabbitQueryResult) {
            assertEquals(3, ((JackrabbitQueryResult) result).getTotalSize());
        }

        // JCR-2684: offset higher than total result => size == 0
        query.setOffset(10);
        result = query.execute();
        nodes = result.getNodes();
        assertFalse(nodes.hasNext());
        assertEquals(0, nodes.getSize());
        if (result instanceof JackrabbitQueryResult) {
            assertEquals(3, ((JackrabbitQueryResult) result).getTotalSize());
        }

        query.setOffset(1);
        query.setLimit(1);
        result = query.execute();
        nodes = result.getNodes();
        assertEquals(1, nodes.getSize());
    }
}
