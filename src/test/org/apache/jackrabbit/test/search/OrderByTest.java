/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.test.search;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Arrays;
import java.util.Collections;
import java.util.TimeZone;

/**
 * Tests queries with order by.
 */
public class OrderByTest extends AbstractQueryTest {

    public void testStringOrder() throws Exception {
        populate(new String[]{"aaaa", "cccc", "bbbb", "dddd"});
        checkOrder(new int[]{1, 3, 2, 4});
    }

    public void testIntegerOrder() throws Exception {
        populate(new int[]{0, -1, 1, 5});
        checkOrder(new int[]{2, 1, 3, 4});
    }

    public void testDateOrder() throws Exception {
        Calendar c1 = Calendar.getInstance();
        c1.set(2000, 4, 20, 14, 35, 14);
        Calendar c2 = Calendar.getInstance();
        c2.set(2000, 5, 20, 14, 35, 14);
        Calendar c3 = Calendar.getInstance();
        c3.set(2000, 4, 20, 14, 35, 13);
        populate(new Calendar[]{c1, c2, c3});
        checkOrder(new int[]{3, 1, 2});
    }

    public void testDateOrderMillis() throws Exception {
        Calendar c1 = Calendar.getInstance();
        c1.set(2000, 6, 12, 14, 35, 19);
        c1.set(Calendar.MILLISECOND, 10);
        Calendar c2 = Calendar.getInstance();
        c2.set(2000, 6, 12, 14, 35, 19);
        c2.set(Calendar.MILLISECOND, 9);
        Calendar c3 = Calendar.getInstance();
        c3.set(2000, 6, 12, 14, 35, 19);
        c3.set(Calendar.MILLISECOND, 11);
        populate(new Calendar[]{c1, c2, c3});
        checkOrder(new int[]{2, 1, 3});
    }

    public void testDateOrderPositiveTimeZone() throws Exception {
        Calendar c1 = Calendar.getInstance(TimeZone.getTimeZone("GMT+1:00"));
        c1.set(2000, 6, 12, 15, 35, 19);
        c1.set(Calendar.MILLISECOND, 10);
        Calendar c2 = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c2.set(2000, 6, 12, 14, 35, 19);
        c2.set(Calendar.MILLISECOND, 9);
        Calendar c3 = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c3.set(2000, 6, 12, 14, 35, 19);
        c3.set(Calendar.MILLISECOND, 11);
        populate(new Calendar[]{c1, c2, c3});
        checkOrder(new int[]{2, 1, 3});
    }

    public void testDateOrderNegativeTimeZone() throws Exception {
        Calendar c1 = Calendar.getInstance(TimeZone.getTimeZone("GMT-1:00"));
        c1.set(2000, 6, 12, 13, 35, 19);
        c1.set(Calendar.MILLISECOND, 10);
        Calendar c2 = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c2.set(2000, 6, 12, 14, 35, 19);
        c2.set(Calendar.MILLISECOND, 9);
        Calendar c3 = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c3.set(2000, 6, 12, 14, 35, 19);
        c3.set(Calendar.MILLISECOND, 11);
        populate(new Calendar[]{c1, c2, c3});
        checkOrder(new int[]{2, 1, 3});
    }

    public void testDoubleOrder1() throws Exception {
        populate(new double[]{-2.4, 4.3, 0.0});
        checkOrder(new int[]{1, 3, 2});
    }

    public void testDoubleOrder2() throws Exception {
        populate(new double[]{-1.5, -1.4, -1.39});
        checkOrder(new int[]{1, 2, 3});
    }

    public void testMultipleOrder() throws Exception {
        Node n1 = testRootNode.addNode("node1");
        Node n2 = testRootNode.addNode("node2");
        Node n3 = testRootNode.addNode("node3");

        n1.setProperty("text", "aaa");
        n1.setProperty("value", 3);
        n2.setProperty("text", "bbb");
        n2.setProperty("value", 2);
        n3.setProperty("text", "ccc");
        n3.setProperty("value", 2);

        testRootNode.save();

        // both ascending
        String sql = "SELECT value FROM nt:unstructured WHERE " +
                "jcr:path LIKE '/" + testRoot + "/%' ORDER BY value, text";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResultOrder(result, new String[]{"node2", "node3", "node1"});

        String xpath = "/" + testRoot + "/*[@jcr:primaryType='nt:unstructured'] order by @value, @text";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResultOrder(result, new String[]{"node2", "node3", "node1"});

        // both descending
        sql = "SELECT value FROM nt:unstructured WHERE " +
                "jcr:path LIKE '/" + testRoot + "/%' ORDER BY value DESC, text DESC";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResultOrder(result, new String[]{"node1", "node3", "node2"});

        xpath = "/" + testRoot + "/*[@jcr:primaryType='nt:unstructured'] order by @value descending, @text descending";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResultOrder(result, new String[]{"node1", "node3", "node2"});

        // mixed ascending and descending
        sql = "SELECT value FROM nt:unstructured WHERE " +
                "jcr:path LIKE '/" + testRoot + "/%' ORDER BY value DESC, text";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResultOrder(result, new String[]{"node1", "node2", "node3"});

        xpath = "/" + testRoot + "/*[@jcr:primaryType='nt:unstructured'] order by @value descending, @text";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResultOrder(result, new String[]{"node1", "node2", "node3"});
    }

    //------------------< internal >--------------------------------------------

    private void populate(String[] values) throws RepositoryException {
        for (int i = 0; i < values.length; i++) {
            Node node = testRootNode.addNode("node" + (i + 1));
            node.setProperty("value", values[i]);
        }
        testRootNode.save();
    }

    private void populate(Calendar[] values) throws RepositoryException {
        for (int i = 0; i < values.length; i++) {
            Node node = testRootNode.addNode("node" + (i + 1));
            node.setProperty("value", values[i]);
        }
        testRootNode.save();
    }

    private void populate(int[] values) throws RepositoryException {
        for (int i = 0; i < values.length; i++) {
            Node node = testRootNode.addNode("node" + (i + 1));
            node.setProperty("value", values[i]);
        }
        testRootNode.save();
    }

    private void populate(double[] values) throws RepositoryException {
        for (int i = 0; i < values.length; i++) {
            Node node = testRootNode.addNode("node" + (i + 1));
            node.setProperty("value", values[i]);
        }
        testRootNode.save();
    }

    private void checkOrder(int[] order) throws RepositoryException {
        String nodeNames[] = new String[order.length];
        for (int i = 0; i < order.length; i++) {
            nodeNames[i] = "node" + order[i];
        }
        // first check ascending
        String sql = "SELECT value FROM nt:unstructured WHERE " +
                "jcr:path LIKE '/" + testRoot + "/%' ORDER BY value";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResultOrder(result, nodeNames);

        String xpath = "/" + testRoot + "/*[@jcr:primaryType='nt:unstructured'] order by @value";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResultOrder(result, nodeNames);

        // then check descending
        Collections.reverse(Arrays.asList(nodeNames));

        sql = "SELECT value FROM nt:unstructured WHERE " +
                "jcr:path LIKE '/" + testRoot + "/%' ORDER BY value DESC";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResultOrder(result, nodeNames);

        xpath = "/" + testRoot + "/*[@jcr:primaryType='nt:unstructured'] order by @value descending";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResultOrder(result, nodeNames);
    }

    private void checkResultOrder(QueryResult result, String[] nodeNames)
            throws RepositoryException {
        List nodes = new ArrayList();
        for (NodeIterator it = result.getNodes(); it.hasNext();) {
            nodes.add(it.nextNode());
        }
        assertEquals("Wrong hit count:", nodeNames.length, nodes.size());

        for (int i = 0; i < nodeNames.length; i++) {
            String name = ((Node) nodes.get(i)).getName();
            assertEquals("Wrong order of nodes:", nodeNames[i], name);
        }
    }

}
