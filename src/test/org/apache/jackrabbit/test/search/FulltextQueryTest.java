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
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

/**
 * Performs tests with the <code>TEXTSEARCH</code> clause.
 */
public class FulltextQueryTest extends AbstractQueryTest {

    public void testFulltextSimpleSQL1() throws Exception {
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("mytext", new String[]{"the quick brown fox jumps over the lazy dog."});

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured"
                + " WHERE jcr:path LIKE '" + testRoot + "/%"
                + "' AND CONTAINS('fox')";
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
                + "' AND CONTAINS('fox')";
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
                + "' AND CONTAINS('fox test')";
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
                + "' AND CONTAINS('text ''fox jumps''')";
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
                + "' AND CONTAINS('text ''fox jumps'' -other')";
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
                + "' AND CONTAINS('''fox jumps'' test OR other')";
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
                + "' AND CONTAINS('''fox juMps'' Test OR otheR')";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 2);
    }


}
