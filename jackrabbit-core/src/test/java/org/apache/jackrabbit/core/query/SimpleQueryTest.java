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
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import java.math.BigDecimal;
import java.util.Calendar;

/**
 * Performs various query test cases.
 */
public class SimpleQueryTest extends AbstractQueryTest {


    public void testSimpleQuerySQL1() throws Exception {
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("bla", new String[]{"bla"});

        testRootNode.save();

        String sql = "SELECT * FROM nt:base"
                + " WHERE jcr:path LIKE '" + testRoot + "/foo'"
                + " AND bla = 'bla'";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);
    }

    public void testSimpleQuerySQL2() throws Exception {
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("bla", new String[]{"bla"});
        Node bla = testRootNode.addNode("bla");
        bla.setProperty("bla", new String[]{"bla"});

        superuser.getRootNode().save();

        String sql = "SELECT * FROM nt:file" +
                " WHERE jcr:path LIKE '" + testRoot + "/%'"
                + " AND bla = 'bla'";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 0);
    }

    public void testSimpleQuerySQL3() throws Exception {
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("bla", new String[]{"bla"});
        Node bla = testRootNode.addNode("bla");
        bla.setProperty("bla", new String[]{"bla"});

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured" +
                " WHERE jcr:path LIKE '" + testRoot + "/%'"
                + " AND bla = 'bla'";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 2);
    }

    public void testSimpleQuerySQL4() throws Exception {
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("bla", new String[]{"bla"});
        Node bla = testRootNode.addNode("bla");
        bla.setProperty("bla", new String[]{"bla"});

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured WHERE jcr:path LIKE '" + testRoot + "/%'";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 2);
    }

    public void testDateField1() throws Exception {
        Node n = testRootNode.addNode("marcel");
        Calendar marcel = Calendar.getInstance();
        marcel.set(1976, 4, 20, 15, 40);
        n.setProperty("birth", new Value[]{superuser.getValueFactory().createValue(marcel)});

        n = testRootNode.addNode("vanessa");
        Calendar vanessa = Calendar.getInstance();
        vanessa.set(1975, 4, 10, 13, 30);
        n.setProperty("birth", new Value[]{superuser.getValueFactory().createValue(vanessa)});

        testRootNode.save();

        String sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND birth > TIMESTAMP '1976-01-01T00:00:00.000+01:00'";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);

        sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND birth > TIMESTAMP '1975-01-01T00:00:00.000+01:00'";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 2);
    }

    public void testDoubleField() throws Exception {
        Node n = testRootNode.addNode("node1");
        n.setProperty("value", new Value[]{superuser.getValueFactory().createValue(1.9928375d)});
        n = testRootNode.addNode("node2");
        n.setProperty("value", new Value[]{superuser.getValueFactory().createValue(0.0d)});
        n = testRootNode.addNode("node3");
        n.setProperty("value", new Value[]{superuser.getValueFactory().createValue(-1.42982475d)});

        testRootNode.save();

        String sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value > 0.1e-0";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);

        sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value > -0.1";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 2);

        sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value > -1.5";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 3);
    }

    public void testBigDecimalField() throws Exception {
        Node n1 = testRootNode.addNode("node1");
        n1.setProperty(
                "value",
                superuser.getValueFactory().createValue(
                        new BigDecimal(1.9928375d)));
        Node n2 = testRootNode.addNode("node2");
        n2.setProperty("value",
                superuser.getValueFactory().createValue(new BigDecimal(0.0d)));
        Node n3 = testRootNode.addNode("node3");
        n3.setProperty(
                "value",
                superuser.getValueFactory().createValue(
                        new BigDecimal(-1.42982475d)));
        testRootNode.getSession().save();

        String baseSql2 = "SELECT * FROM [nt:base] WHERE ISCHILDNODE(["
                + testRoot + "]) AND value";
        checkResult(qm.createQuery(baseSql2 + " > 0.1", Query.JCR_SQL2)
                .execute(), new Node[] { n1 });
        checkResult(
                qm.createQuery(baseSql2 + " = CAST('0' AS DECIMAL)",
                        Query.JCR_SQL2).execute(), new Node[] { n2 });
        checkResult(
                qm.createQuery(baseSql2 + " = CAST(0 AS DECIMAL)",
                        Query.JCR_SQL2).execute(), new Node[] { n2 });
        checkResult(qm.createQuery(baseSql2 + " > -0.1", Query.JCR_SQL2)
                .execute(), new Node[] { n1, n2 });
        checkResult(qm.createQuery(baseSql2 + " > -1.5", Query.JCR_SQL2)
                .execute(), new Node[] { n1, n2, n3 });
    }

    public void testLongField() throws Exception {
        Node n = testRootNode.addNode("node1");
        n.setProperty("value", new Value[]{superuser.getValueFactory().createValue(1)});
        n = testRootNode.addNode("node2");
        n.setProperty("value", new Value[]{superuser.getValueFactory().createValue(0)});
        n = testRootNode.addNode("node3");
        n.setProperty("value", new Value[]{superuser.getValueFactory().createValue(-1)});

        testRootNode.save();

        String sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value > 0";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);

        sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value > -1";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 2);

        sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value > -2";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 3);
    }

    public void testLikePattern() throws Exception {
        Node n = testRootNode.addNode("node1");
        n.setProperty("value", new String[]{"king"});
        n = testRootNode.addNode("node2");
        n.setProperty("value", new String[]{"ping"});
        n = testRootNode.addNode("node3");
        n.setProperty("value", new String[]{"ching"});

        testRootNode.save();

        String sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value LIKE 'ping'";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);

        sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value LIKE '_ing'";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 2);

        sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value LIKE '%ing'";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 3);
    }

    public void testLikePatternBetween() throws Exception {
        Node n = testRootNode.addNode("node1");
        n.setProperty("value", new String[]{"ping"});
        n = testRootNode.addNode("node2");
        n.setProperty("value", new String[]{"pong"});
        n = testRootNode.addNode("node3");
        n.setProperty("value", new String[]{"puung"});

        testRootNode.save();

        String sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value LIKE 'ping'";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);

        sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value LIKE 'p_ng'";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 2);

        sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value LIKE 'p%ng'";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 3);
    }

    public void testLikePatternEnd() throws Exception {
        Node n = testRootNode.addNode("node1");
        n.setProperty("value", new String[]{"bli"});
        n = testRootNode.addNode("node2");
        n.setProperty("value", new String[]{"bla"});
        n = testRootNode.addNode("node3");
        n.setProperty("value", new String[]{"blub"});

        testRootNode.save();

        String sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value LIKE 'bli'";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);

        sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value LIKE 'bl_'";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 2);

        sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value LIKE 'bl%'";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 3);
    }

    public void testLikePatternEscaped() throws Exception {
        Node n = testRootNode.addNode("node1");
        n.setProperty("value", new String[]{"foo\\_bar"});
        n = testRootNode.addNode("node2");
        n.setProperty("value", new String[]{"foobar"});
        n = testRootNode.addNode("node3");
        n.setProperty("value", new String[]{"foo_bar"});
        n = testRootNode.addNode("node4");
        n.setProperty("value", new String[]{"foolbar"});

        testRootNode.save();

        String sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value LIKE 'foo\\_bar' ESCAPE '\\'"; // matches node3
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);

        sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value LIKE 'foo_bar'";    // matches node3 and node4
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 2);

        sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value LIKE 'foo%bar'";  // matches all nodes
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 4);

        sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value LIKE 'foo\\\\\\_bar' ESCAPE '\\'";  // matches node1
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 1);

        sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value LIKE 'foo\\_bar'";  // matches node1
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 1);
    }

    public void testLikeWithLineTerminator() throws Exception {
        Node n = testRootNode.addNode("node1");
        n.setProperty("value", new String[]{"foo\nbar"});

        testRootNode.save();

        String sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value LIKE 'foo%bar'";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);

        sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value LIKE 'foo_bar'";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 1);
    }

    public void testNotEqual() throws Exception {
        Node n = testRootNode.addNode("node1");
        n.setProperty("value", new String[]{"foo"});
        n = testRootNode.addNode("node2");
        n.setProperty("value", new String[]{"bar"});
        n = testRootNode.addNode("node3");
        n.setProperty("value", new String[]{"foobar"});

        testRootNode.save();

        String jcrql = "SELECT * FROM nt:base WHERE jcr:path LIKE '" + testRoot + "/%' AND value <> 'bar'";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 2);

    }

    public void testIsNull() throws Exception {
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("mytext", "the quick brown fox jumps over the lazy dog.");
        Node bar = testRootNode.addNode("bar");
        bar.setProperty("text", "the quick brown fox jumps over the lazy dog.");

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured WHERE mytext is null and jcr:path LIKE '"
                + testRoot + "/%'";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);

        String xpath = "/" + testRoot + "/*[@jcr:primaryType='nt:unstructured' and fn:not(@mytext)]";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, 1);

        xpath = "/jcr:root" + testRoot + "/*[@jcr:primaryType='nt:unstructured' and fn:not(@mytext)]";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, 1);
    }

    public void testIsNotNull() throws Exception {
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("mytext", "the quick brown fox jumps over the lazy dog.");
        Node bar = testRootNode.addNode("bar");
        bar.setProperty("text", "the quick brown fox jumps over the lazy dog.");
        // documents which field name is not exactly "mytext" should not match (JCR-1051)
        bar.setProperty("mytextwhichstartswithmytext", "the quick brown fox jumps over the lazy dog.");

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured WHERE mytext is not null";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);

        String xpath = "//*[@jcr:primaryType='nt:unstructured' and @mytext]";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, 1);
    }

    public void testNegativeNumber() throws Exception {
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("number", -10);
        Node bar = testRootNode.addNode("bar");
        bar.setProperty("number", -20);

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured WHERE number = -10";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);

        String xpath = "//*[@jcr:primaryType='nt:unstructured' and @number = -10]";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, 1);

        sql = "SELECT * FROM nt:unstructured WHERE number <= -10";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 2);

        xpath = "//*[@jcr:primaryType='nt:unstructured' and @number <= -10]";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, 2);
    }

    public void testQuotes() throws Exception {
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("foo", "bar'bar");

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured WHERE foo = 'bar''bar'";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);

        String xpath = "//*[@jcr:primaryType='nt:unstructured' and @foo ='bar''bar']";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, 1);

        xpath = "//*[@jcr:primaryType='nt:unstructured' and jcr:like(@foo,'%ar''ba%')]";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, 1);

        xpath = "//*[@jcr:primaryType='nt:unstructured' and jcr:like(@foo,\"%ar'ba%\")]";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, 1);
    }

    public void testGeneralComparison() throws Exception {
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("text", new String[]{"foo", "bar"}); // mvp
        Node bar = testRootNode.addNode("bar");
        bar.setProperty("text", new String[]{"foo"}); // mvp with one value
        Node bla = testRootNode.addNode("bla");
        bla.setProperty("text", "foo"); // svp
        Node blu = testRootNode.addNode("blu");
        blu.setProperty("text", "bar"); // svp

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured WHERE 'foo' IN text " +
                "and jcr:path LIKE '" + testRoot + "/%'";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, new Node[]{foo, bar, bla});

        String xpath = "/" + testRoot + "/*[@jcr:primaryType='nt:unstructured' and @text = 'foo']";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, new Node[]{foo, bar, bla});

        xpath = "/jcr:root" + testRoot + "/*[@jcr:primaryType='nt:unstructured' and @text = 'foo']";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, new Node[]{foo, bar, bla});

        //----------------------------------------------------------------------

        // the text = 'foo' is now also a general comparison
        sql = "SELECT * FROM nt:unstructured WHERE text = 'foo' " +
                "and jcr:path LIKE '" + testRoot + "/%'";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, new Node[]{foo, bar, bla});

        xpath = "/" + testRoot + "/*[@jcr:primaryType='nt:unstructured' and @text eq 'foo']";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, new Node[]{bar, bla});

        xpath = "/jcr:root" + testRoot + "/*[@jcr:primaryType='nt:unstructured' and @text eq 'foo']";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, new Node[]{bar, bla});

        //----------------------------------------------------------------------

        sql = "SELECT * FROM nt:unstructured WHERE 'bar' NOT IN text " +
                "and jcr:path LIKE '" + testRoot + "/%'";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, new Node[]{foo, bar, bla});

        xpath = "/" + testRoot + "/*[@jcr:primaryType='nt:unstructured' and @text != 'bar']";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, new Node[]{foo, bar, bla});

        xpath = "/jcr:root" + testRoot + "/*[@jcr:primaryType='nt:unstructured' and @text != 'bar']";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, new Node[]{foo, bar, bla});

        //----------------------------------------------------------------------

        sql = "SELECT * FROM nt:unstructured WHERE 'foo' NOT IN text " +
                "and jcr:path LIKE '" + testRoot + "/%'";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, new Node[]{foo, blu});

        xpath = "/" + testRoot + "/*[@jcr:primaryType='nt:unstructured' and @text != 'foo']";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, new Node[]{foo, blu});

        xpath = "/jcr:root" + testRoot + "/*[@jcr:primaryType='nt:unstructured' and @text != 'foo']";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, new Node[]{foo, blu});
    }

    public void testLogicalExpression() throws Exception {
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("a", 1);
        foo.setProperty("b", 2);
        foo.setProperty("c", 3);
        Node bar = testRootNode.addNode("bar");
        bar.setProperty("a", 0);
        bar.setProperty("b", 2);
        bar.setProperty("c", 0);
        Node bla = testRootNode.addNode("bla");
        bla.setProperty("a", 1);
        bla.setProperty("b", 0);
        bla.setProperty("c", 3);
        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured WHERE a=1 and b=2 or c=3";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, new Node[]{foo, bla});

        String xpath = "//*[@a=1 and @b=2 or @c=3] ";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, new Node[]{foo, bla});
    }
}
