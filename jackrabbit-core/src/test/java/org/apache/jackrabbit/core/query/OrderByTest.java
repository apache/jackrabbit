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
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Tests queries with order by.
 */
public class OrderByTest extends AbstractQueryTest {

    public void testOrderByScore() throws RepositoryException {
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

        String sql = "SELECT value FROM nt:unstructured WHERE " +
                "jcr:path LIKE '" + testRoot + "/%' ORDER BY jcr:score, value";
        Query q = qm.createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 3);

        String xpath = "/" + testRoot + "/*[@jcr:primaryType='nt:unstructured'] order by jcr:score(), @value";
        q = qm.createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, 3);
    }

    /**
     * Test for JCR-2906
     */
    public void testOrderByMVP() throws RepositoryException {
        Node n1 = testRootNode.addNode("node1");
        Node n2 = testRootNode.addNode("node2");
        Node n3 = testRootNode.addNode("node3");
        Node n4 = testRootNode.addNode("node4");
        Node n5 = testRootNode.addNode("node5");

        n1.setProperty("extra", new String[] { "12345" });
        n1.setProperty("text", new String[] { "ccc" });

        n2.setProperty("text", new String[] { "eee", "bbb" });
        n3.setProperty("text", new String[] { "aaa" });
        n4.setProperty("text", new String[] { "bbb", "aaa" });
        n5.setProperty("text", new String[] { "eee", "aaa" });

        testRootNode.getSession().save();

        String sql = "SELECT value FROM nt:unstructured WHERE "
                + "jcr:path LIKE '" + testRoot + "/%' ORDER BY text";
        checkResultSequence(executeQuery(sql).getRows(), new Node[] { n3, n4,
                n1, n5, n2 });

        String xpath = "/"
                + testRoot
                + "/*[@jcr:primaryType='nt:unstructured'] order by jcr:score(), @text";
        checkResultSequence(executeQuery(xpath).getRows(), new Node[] { n3, n4,
                n1, n5, n2 });
    }

    public void testOrderByUpperCase() throws RepositoryException {
        Node n1 = testRootNode.addNode("node1");
        Node n2 = testRootNode.addNode("node2");
        Node n3 = testRootNode.addNode("node3");

        n1.setProperty("text", "Amundsen");
        n2.setProperty("text", "barents");
        n3.setProperty("text", "Wegener");

        testRootNode.save();

        String xpath = "/" + testRoot + "/*[@jcr:primaryType='nt:unstructured'] order by fn:upper-case(@text)";
        Query q = qm.createQuery(xpath, Query.XPATH);
        QueryResult result = q.execute();
        checkResult(result, new Node[]{n1, n2, n3});
    }

    public void testOrderByLowerCase() throws RepositoryException {
        Node n1 = testRootNode.addNode("node1");
        Node n2 = testRootNode.addNode("node2");
        Node n3 = testRootNode.addNode("node3");

        n1.setProperty("text", "Amundsen");
        n2.setProperty("text", "barents");
        n3.setProperty("text", "Wegener");

        testRootNode.save();

        String xpath = "/" + testRoot + "/*[@jcr:primaryType='nt:unstructured'] order by fn:lower-case(@text)";
        Query q = qm.createQuery(xpath, Query.XPATH);
        QueryResult result = q.execute();
        checkResult(result, new Node[]{n1, n2, n3});
    }

    public void testChildAxisString() throws RepositoryException {
        checkChildAxis(new Value[]{getValue("a"), getValue("b"), getValue("c")});
    }

    public void testChildAxisLong() throws RepositoryException {
        checkChildAxis(new Value[]{getValue(1), getValue(2), getValue(3)});
    }

    public void testChildAxisDouble() throws RepositoryException {
        checkChildAxis(new Value[]{getValue(1.0), getValue(2.0), getValue(3.0)});
    }

    public void testChildAxisBoolean() throws RepositoryException {
        checkChildAxis(new Value[]{getValue(false), getValue(true)});
    }

    public void testChildAxisCalendar() throws RepositoryException {
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c2.add(Calendar.MINUTE, 1);
        Calendar c3 = Calendar.getInstance();
        c3.add(Calendar.MINUTE, 2);
        checkChildAxis(new Value[]{getValue(c1), getValue(c2), getValue(c3)});
    }

    public void testChildAxisName() throws RepositoryException {
        checkChildAxis(new Value[]{getNameValue("a"), getNameValue("b"), getNameValue("c")});
    }

    public void testChildAxisPath() throws RepositoryException {
        checkChildAxis(new Value[]{getPathValue("a"), getPathValue("b"), getPathValue("c")});
    }

    public void testChildAxisDeep() throws RepositoryException {
        Node n1 = testRootNode.addNode("node1");
        n1.addNode("a").addNode("b"); // no property
        Node n2 = testRootNode.addNode("node2");
        n2.addNode("a").addNode("b").addNode("c").setProperty("prop", "a");
        Node n3 = testRootNode.addNode("node3");
        n3.addNode("a").addNode("b").addNode("c").setProperty("prop", "b");
        testRootNode.save();

        List expected = Arrays.asList(new String[]{n1.getPath(), n2.getPath(), n3.getPath()});
        String xpath = testPath + "/* order by a/b/c/@prop";
        assertEquals(expected, collectPaths(executeQuery(xpath)));

        // descending
        Collections.reverse(expected);
        xpath = testPath + "/* order by a/b/c/@prop descending";
        assertEquals(expected, collectPaths(executeQuery(xpath)));
    }

    public void testChildAxisNoValue() throws RepositoryException {
        Node n1 = testRootNode.addNode("node1");
        n1.addNode("child").setProperty("prop", "a");
        Node n2 = testRootNode.addNode("node2");
        n2.addNode("child");
        testRootNode.save();

        List expected = Arrays.asList(new String[]{n2.getPath(), n1.getPath()});
        String xpath = testPath + "/* order by child/@prop";
        assertEquals(expected, collectPaths(executeQuery(xpath)));

        // descending
        Collections.reverse(expected);
        xpath = testPath + "/* order by child/@prop descending";
        assertEquals(expected, collectPaths(executeQuery(xpath)));

        // reverse order in content
        n1.getNode("child").getProperty("prop").remove();
        n2.getNode("child").setProperty("prop", "a");
        testRootNode.save();

        Collections.reverse(expected);
        assertEquals(expected, collectPaths(executeQuery(xpath)));
    }

    public void testChildAxisMixedTypes() throws RepositoryException {
        // when differing types are used then the class name of the type
        // is used for comparison:
        // java.lang.Double < java.lang.Integer
        checkChildAxis(new Value[]{getValue(2.0), getValue(1)});
    }

    //------------------------------< helper >----------------------------------

    private Value getValue(String value) throws RepositoryException {
        return superuser.getValueFactory().createValue(value);
    }

    private Value getValue(long value) throws RepositoryException {
        return superuser.getValueFactory().createValue(value);
    }

    private Value getValue(double value) throws RepositoryException {
        return superuser.getValueFactory().createValue(value);
    }

    private Value getValue(boolean value) throws RepositoryException {
        return superuser.getValueFactory().createValue(value);
    }

    private Value getValue(Calendar value) throws RepositoryException {
        return superuser.getValueFactory().createValue(value);
    }

    private Value getNameValue(String value) throws RepositoryException {
        return superuser.getValueFactory().createValue(value, PropertyType.NAME);
    }

    private Value getPathValue(String value) throws RepositoryException {
        return superuser.getValueFactory().createValue(value, PropertyType.PATH);
    }

    /**
     * Checks if order by with a relative path works on the the passed values.
     * The values are expected to be in ascending order.
     *
     * @param values the values in ascending order.
     * @throws RepositoryException if an error occurs.
     */
    private void checkChildAxis(Value[] values) throws RepositoryException {
        // child/prop is part of the test indexing configuration,
        // this will use SimpleScoreDocComparator internally
        checkChildAxis(values, "child", "property");
        cleanUpTestRoot(superuser);
        // c/p is not in the indexing configuration,
        // this will use RelPathScoreDocComparator internally
        checkChildAxis(values, "c", "p");
    }

    /**
     * Checks if order by with a relative path works on the the passed values.
     * The values are expected to be in ascending order.
     *
     * @param values   the values in ascending order.
     * @param child    the name of the child node.
     * @param property the name of the property.
     * @throws RepositoryException if an error occurs.
     */
    private void checkChildAxis(Value[] values, String child, String property)
            throws RepositoryException {
        List vals = new ArrayList();
        // add initial value null -> property not set
        // inexistent property is always less than any property value set
        vals.add(null);
        vals.addAll(Arrays.asList(values));

        List expected = new ArrayList();
        for (int i = 0; i < vals.size(); i++) {
            Node n = testRootNode.addNode("node" + i);
            expected.add(n.getPath());
            Node c = n.addNode(child);
            if (vals.get(i) != null) {
                c.setProperty(property, (Value) vals.get(i));
            }
        }
        testRootNode.save();

        String xpath = testPath + "/* order by " + child + "/@" + property;
        assertEquals(expected, collectPaths(executeQuery(xpath)));

        // descending
        Collections.reverse(expected);
        xpath += " descending";
        assertEquals(expected, collectPaths(executeQuery(xpath)));

        Collections.reverse(vals);
        for (int i = 0; i < vals.size(); i++) {
            Node c = testRootNode.getNode("node" + i).getNode(child);
            c.setProperty(property, (Value) vals.get(i));
        }
        testRootNode.save();

        Collections.reverse(expected);
        assertEquals(expected, collectPaths(executeQuery(xpath)));
    }

    private static List collectPaths(QueryResult result)
            throws RepositoryException {
        List paths = new ArrayList();
        for (NodeIterator it = result.getNodes(); it.hasNext(); ) {
            paths.add(it.nextNode().getPath());
        }
        return paths;
    }
}
