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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;

import org.apache.jackrabbit.commons.JcrUtils;

/**
 * Test case for queries with JCR_SQL2 having offset and limit clauses
 * 
 * Inspired by <a
 * href="https://issues.apache.org/jira/browse/JCR-2830">JCR-2830</a>
 * 
 */
public class SQL2OffsetLimitTest extends AbstractQueryTest {

    private final List<String> c = Arrays.asList("a", "b", "c", "d", "e");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        for (String s : c) {
            testRootNode.addNode(s);
        }
        testRootNode.getSession().save();
    }

    @Override
    protected void tearDown() throws Exception {
        for (Node c : JcrUtils.getChildNodes(testRootNode)) {
            testRootNode.getSession().removeItem(c.getPath());
        }
        testRootNode.getSession().save();
        super.tearDown();
    }

    private Query newQuery() throws Exception {
        return qm.createQuery("SELECT * FROM [nt:base] WHERE ISCHILDNODE(["
                + testRoot + "]) order by [jcr:score] ", Query.JCR_SQL2);
    }

    public void testNoConstraints() throws Exception {
        List<String> expected = new ArrayList<String>(c);
        Query q = newQuery();

        List<String> out = qrToPaths(q.execute());
        assertEquals(c.size(), out.size());
        for (String s : out) {
            assertTrue(expected.remove(s));
        }
        assertTrue(expected.isEmpty());
    }

    public void testLimitEqSize() throws Exception {
        List<String> expected = new ArrayList<String>(c);
        Query q = newQuery();
        q.setOffset(0);
        q.setLimit(c.size());

        List<String> out = qrToPaths(q.execute());
        assertEquals(c.size(), out.size());
        for (String s : out) {
            assertTrue(expected.remove(s));
        }
        assertTrue(expected.isEmpty());
    }

    public void testLimitGtSize() throws Exception {
        List<String> expected = new ArrayList<String>(c);
        Query q = newQuery();
        q.setOffset(0);
        q.setLimit(c.size() * 2);

        List<String> out = qrToPaths(q.execute());
        assertEquals(c.size(), out.size());
        for (String s : out) {
            assertTrue(expected.remove(s));
        }
        assertTrue(expected.isEmpty());
    }

    public void testOffsetEqSize() throws Exception {
        Query q = newQuery();
        q.setOffset(c.size() - 1);
        List<String> out = qrToPaths(q.execute());
        assertEquals(1, out.size());
    }

    public void testOffsetGtSize() throws Exception {
        Query q = newQuery();
        q.setOffset(c.size() * 2);
        List<String> out = qrToPaths(q.execute());
        assertTrue(out.isEmpty());
    }

    public void testSimplePagination() throws Exception {
        List<String> expected = new ArrayList<String>(c);
        Query q = newQuery();

        for (int i = 0; i < c.size(); i++) {
            q.setOffset(i);
            q.setLimit(1);
            List<String> out = qrToPaths(q.execute());
            assertEquals(1, out.size());
            assertTrue(expected.remove(out.get(0)));
        }
        assertTrue(expected.isEmpty());
    }

    public void test2BigPages() throws Exception {
        List<String> expected = new ArrayList<String>(c);
        Query q = newQuery();

        int p1 = (int) (c.size() * 0.8);
        int p2 = c.size() - p1;

        q.setOffset(0);
        q.setLimit(p1);
        List<String> out1 = qrToPaths(q.execute());
        assertEquals(p1, out1.size());
        for (String s : out1) {
            assertTrue(expected.remove(s));
        }

        q.setOffset(p1);
        q.setLimit(p2);
        List<String> out2 = qrToPaths(q.execute());
        assertEquals(p2, out2.size());
        for (String s : out2) {
            assertTrue(expected.remove(s));
        }

        assertTrue(expected.isEmpty());
    }

    private List<String> qrToPaths(QueryResult qr) throws RepositoryException {
        List<String> ret = new ArrayList<String>();
        for (Row row : JcrUtils.getRows(qr)) {
            Node n = row.getNode();
            ret.add(n.getPath().replace(n.getParent().getPath() + "/", ""));
        }
        return ret;
    }
}
