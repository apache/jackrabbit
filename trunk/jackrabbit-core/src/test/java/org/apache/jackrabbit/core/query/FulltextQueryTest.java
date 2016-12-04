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
import java.util.List;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;

import org.apache.jackrabbit.commons.iterator.RowIterable;

/**
 * Performs tests with the <code>CONTAINS</code> function.
 */
public class FulltextQueryTest extends AbstractQueryTest {

    public void testFulltextSimpleSQL1() throws Exception {
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("mytext", new String[]{"the quick brown fox jumps over the lazy dog."});

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured"
                + " WHERE jcr:path LIKE '" + testRoot + "/%"
                + "' AND CONTAINS(., 'fox')";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);
    }

    public void testFulltextSimpleSQL2() throws Exception {
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("mytext", new String[]{"the quick brown fox jumps over the lazy dog."});

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured"
                + " WHERE \"jcr:path\" = '" + testRoot + "/foo"
                + "' AND CONTAINS(., 'fox')";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);
    }

    public void testFulltextMultiWordSQL() throws Exception {
        Node n = testRootNode.addNode("node1");
        n.setProperty("title", new String[]{"test text"});
        n.setProperty("mytext", new String[]{"the quick brown fox jumps over the lazy dog."});

        n = testRootNode.addNode("node2");
        n.setProperty("title", new String[]{"other text"});
        n.setProperty("mytext", new String[]{"the quick brown fox jumps over the lazy dog."});

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured"
                + " WHERE \"jcr:path\" LIKE '" + testRoot + "/%"
                + "' AND CONTAINS(., 'fox test')";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);
    }

    public void testFulltextPhraseSQL() throws Exception {
        Node n = testRootNode.addNode("node1");
        n.setProperty("title", new String[]{"test text"});
        n.setProperty("mytext", new String[]{"the quick brown jumps fox over the lazy dog."});

        n = testRootNode.addNode("node2");
        n.setProperty("title", new String[]{"other text"});
        n.setProperty("mytext", new String[]{"the quick brown fox jumps over the lazy dog."});

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured"
                + " WHERE \"jcr:path\" LIKE '" + testRoot + "/%"
                + "' AND CONTAINS(., 'text \"fox jumps\"')";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);
    }

    public void testFulltextExcludeSQL() throws Exception {
        Node n = testRootNode.addNode("node1");
        n.setProperty("title", new String[]{"test text"});
        n.setProperty("mytext", new String[]{"the quick brown fox jumps over the lazy dog."});

        n = testRootNode.addNode("node2");
        n.setProperty("title", new String[]{"other text"});
        n.setProperty("mytext", new String[]{"the quick brown fox jumps over the lazy dog."});

        superuser.getRootNode().save();

        String sql = "SELECT * FROM nt:unstructured"
                + " WHERE \"jcr:path\" LIKE '" + testRoot + "/%"
                + "' AND CONTAINS(., 'text ''fox jumps'' -other')";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);
    }

    public void testFulltextOrSQL() throws Exception {
        Node n = testRootNode.addNode("node1");
        n.setProperty("title", new String[]{"test text"});
        n.setProperty("mytext", new String[]{"the quick brown fox jumps over the lazy dog."});

        n = testRootNode.addNode("node2");
        n.setProperty("title", new String[]{"other text"});
        n.setProperty("mytext", new String[]{"the quick brown fox jumps over the lazy dog."});

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured"
                + " WHERE \"jcr:path\" LIKE '" + testRoot + "/%"
                + "' AND CONTAINS(., '''fox jumps'' test OR other')";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 2);
    }

    public void testFulltextIntercapSQL() throws Exception {
        Node n = testRootNode.addNode("node1");
        n.setProperty("title", new String[]{"tEst text"});
        n.setProperty("mytext", new String[]{"The quick brown Fox jumps over the lazy dog."});

        n = testRootNode.addNode("node2");
        n.setProperty("title", new String[]{"Other text"});
        n.setProperty("mytext", new String[]{"the quick brown FOX jumPs over the lazy dog."});

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured"
                + " WHERE \"jcr:path\" LIKE '" + testRoot + "/%"
                + "' AND CONTAINS(., '''fox juMps'' Test OR otheR')";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 2);
    }

    public void testContainsStarSQL() throws RepositoryException {
        Node n = testRootNode.addNode("node1");
        n.setProperty("title", new String[]{"tEst text"});
        n.setProperty("mytext", new String[]{"The quick brown Fox jumps over the lazy dog."});

        n = testRootNode.addNode("node2");
        n.setProperty("title", new String[]{"The quick brown Fox jumps over the lazy dog."});
        n.setProperty("mytext", new String[]{"text text"});

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured"
                + " WHERE jcr:path LIKE '" + testRoot + "/%"
                + "' AND CONTAINS(., 'fox jumps')";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        checkResult(q.execute(), 2);
    }

    public void testContainsStarXPath() throws RepositoryException {
        Node n = testRootNode.addNode("node1");
        n.setProperty("title", new String[]{"tEst text"});
        n.setProperty("mytext", new String[]{"The quick brown Fox jumps over the lazy dog."});

        n = testRootNode.addNode("node2");
        n.setProperty("title", new String[]{"The quick brown Fox jumps over the lazy dog."});
        n.setProperty("mytext", new String[]{"text text"});

        testRootNode.save();

        String sql = "/jcr:root" + testRoot + "/element(*, nt:unstructured)"
                + "[jcr:contains(., 'quick fox')]";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.XPATH);
        checkResult(q.execute(), 2);
    }

    public void testContainsPropScopeSQL() throws RepositoryException {
        Node n = testRootNode.addNode("node1");
        n.setProperty("title", new String[]{"tEst text"});
        n.setProperty("mytext", new String[]{"The quick brown Fox jumps over the lazy dog."});

        n = testRootNode.addNode("node2");
        n.setProperty("title", new String[]{"The quick brown Fox jumps over the lazy dog."});
        n.setProperty("mytext", new String[]{"text text"});

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured"
                + " WHERE jcr:path LIKE '" + testRoot + "/%"
                + "' AND CONTAINS(title, 'fox jumps')";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        checkResult(q.execute(), 1);
    }

    public void testContainsPropScopeXPath() throws RepositoryException {
        Node n = testRootNode.addNode("node1");
        n.setProperty("title", new String[]{"tEst text"});
        n.setProperty("mytext", new String[]{"The quick brown Fox jumps over the lazy dog."});

        n = testRootNode.addNode("node2");
        n.setProperty("title", new String[]{"The quick brown Fox jumps over the lazy dog."});
        n.setProperty("mytext", new String[]{"text text"});

        testRootNode.save();

        String sql = "/jcr:root" + testRoot + "/element(*, nt:unstructured)"
                + "[jcr:contains(@title, 'quick fox')]";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.XPATH);
        checkResult(q.execute(), 1);
    }

    public void testWildcard() throws RepositoryException {
        String content = "The quick brown Fox jumps over the lazy dog.";

        // single * wildcard
        executeContainsQuery("qu*", content, true);
        executeContainsQuery("qu*ck", content, true);
        executeContainsQuery("quick*", content, true);
        executeContainsQuery("*quick", content, true);
        executeContainsQuery("qu*Ck", content, true);

        // multiple * wildcard
        executeContainsQuery("*o*", content, true);
        executeContainsQuery("*ump*", content, true);
        executeContainsQuery("qu**ck", content, true);
        executeContainsQuery("q***u**c*k", content, true);
        executeContainsQuery("*uMp*", content, true);

        // single ? wildcard
        executeContainsQuery("quic?", content, true);
        executeContainsQuery("?uick", content, true);
        executeContainsQuery("qu?ck", content, true);
        executeContainsQuery("qu?cK", content, true);

        // multiple ? wildcard
        executeContainsQuery("q??ck", content, true);
        executeContainsQuery("?uic?", content, true);
        executeContainsQuery("??iCk", content, true);

        // no matches
        executeContainsQuery("*ab*", content, false);
        executeContainsQuery("q***j**c*k", content, false);

    }

    public void testMultiByte() throws RepositoryException {
        String content = "some text with multi byte \u7530\u4e2d characters.";

        executeContainsQuery("\u7530\u4e2d*", content, true);
    }

    public void testPredefinedEntityReference() throws RepositoryException {
        String content = "Max&Moritz";

        executeContainsQuery("max&moritz", content, true);
    }

    public void testColonInContains() throws RepositoryException {
        executeContainsQuery("foo:bar", "foo:bar", true);
    }

    public void testMultipleOrExpressions() throws RepositoryException {
        Node n = testRootNode.addNode("node1");
        n.setProperty("prop1", "foo");
        n.setProperty("prop2", "bar");
        n.setProperty("prop3", "baz");

        n = testRootNode.addNode("node2");
        n.setProperty("prop1", "bar");
        n.setProperty("prop2", "foo");
        n.setProperty("prop3", "baz");

        n = testRootNode.addNode("node3");
        n.setProperty("prop1", "bar");
        n.setProperty("prop2", "baz");
        n.setProperty("prop3", "foo");

        superuser.save();

        TreeSet<String> r1 = new TreeSet<String>();
        QueryResult result = qm.createQuery(testPath + "/*[jcr:contains(@prop1, 'foo') or jcr:contains(@prop2, 'foo') or jcr:contains(@prop3, 'foo')] order by @jcr:score descending", Query.XPATH).execute();
        for (Row r : new RowIterable(result.getRows())) {
            r1.add(r.getPath() + ":" + (int) (r.getScore() * 1000));
        }

        TreeSet<String> r2 = new TreeSet<String>();
        result = qm.createQuery(testPath + "/*[jcr:contains(@prop3, 'foo') or jcr:contains(@prop1, 'foo') or jcr:contains(@prop2, 'foo')] order by @jcr:score descending", Query.XPATH).execute();
        for (Row r : new RowIterable(result.getRows())) {
            r2.add(r.getPath() + ":" + (int) (r.getScore() * 1000));
        }

        TreeSet<String> r3 = new TreeSet<String>();
        result = qm.createQuery(testPath + "/*[jcr:contains(@prop2, 'foo') or jcr:contains(@prop3, 'foo') or jcr:contains(@prop1, 'foo')] order by @jcr:score descending", Query.XPATH).execute();
        for (Row r : new RowIterable(result.getRows())) {
            r3.add(r.getPath() + ":" + (int) (r.getScore() * 1000));
        }

        assertEquals(r1, r2);
        assertEquals(r1, r3);
    }

    /**
     * Executes a query and checks if the query matched the test node.
     *
     * @param statement the query statement.
     * @param content   the content for the test node.
     * @param match     if the query matches the node.
     * @throws RepositoryException if an error occurs.
     */
    private void executeContainsQuery(String statement,
                              String content,
                              boolean match) throws RepositoryException {
        while (testRootNode.hasNode(nodeName1)) {
            testRootNode.getNode(nodeName1).remove();
        }
        testRootNode.addNode(nodeName1).setProperty("text", content);
        testRootNode.save();

        assertContainsQuery(statement, match);
    }

    private void assertContainsQuery(String statement, boolean match)
            throws InvalidQueryException, RepositoryException {
        StringBuffer stmt = new StringBuffer();
        stmt.append("/jcr:root").append(testRoot).append("/*");
        stmt.append("[jcr:contains(., '").append(statement);
        stmt.append("')]");

        Query q = superuser.getWorkspace().getQueryManager().createQuery(stmt.toString(), Query.XPATH);
        checkResult(q.execute(), match ? 1 : 0);

        stmt = new StringBuffer();
        stmt.append("SELECT * FROM nt:base ");
        stmt.append("WHERE jcr:path LIKE '").append(testRoot).append("/%' ");
        stmt.append("AND CONTAINS(., '").append(statement).append("')");

        q = superuser.getWorkspace().getQueryManager().createQuery(stmt.toString(), Query.SQL);
        checkResult(q.execute(), match ? 1 : 0);
    }

}
