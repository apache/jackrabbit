/*
 * Copyright 2004 The Apache Software Foundation.
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
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

/**
 * Performs tests with on the <code>SELECT</code> clause.
 */
public class SelectClauseTest extends AbstractQueryTest {

    public void testSelect() throws RepositoryException {
        Node n = testRootNode.addNode("node1", NT_UNSTRUCTURED);
        n.setProperty("myvalue", new String[]{"foo"});
        n = testRootNode.addNode("node2", NT_UNSTRUCTURED);
        n.setProperty("myvalue", new String[]{"bar"});
        n = testRootNode.addNode("node3", NT_UNSTRUCTURED);
        n.setProperty("yourvalue", new String[]{"foo"});

        testRootNode.save();

        String jcrql = "SELECT myvalue FROM * LOCATION " + testRoot + "//";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
        QueryResult result = q.execute();
        checkResult(result, 2);

        jcrql = "SELECT myvalue FROM * LOCATION " + testRoot + "// WHERE yourvalue = \"foo\"";
        q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
        result = q.execute();
        checkResult(result, 0);

        jcrql = "SELECT myvalue FROM *";
        q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
        result = q.execute();
        checkResult(result, 2);

    }

    public void testPropertyCount() throws RepositoryException {
        Node n = testRootNode.addNode("node1", NT_UNSTRUCTURED);
        n.setProperty("myvalue", new String[]{"foo"});
        n = testRootNode.addNode("node2", NT_UNSTRUCTURED);
        n.setProperty("myvalue", new String[]{"bar"});
        n = testRootNode.addNode("node3", NT_UNSTRUCTURED);
        n.setProperty("yourvalue", new String[]{"foo"});

        testRootNode.save();

        String jcrql = "SELECT myvalue FROM * LOCATION " + testRoot + "//";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
        QueryResult result = q.execute();
        checkResult(result, 2, 2);

        jcrql = "SELECT myvalue FROM * LOCATION " + testRoot + "// WHERE yourvalue = \"foo\"";
        q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
        result = q.execute();
        checkResult(result, 0, 0);

        jcrql = "SELECT myvalue FROM *";
        q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
        result = q.execute();
        checkResult(result, 2, 2);

        jcrql = "SELECT * FROM * LOCATION " + testRoot + "// WHERE myvalue LIKE \"*\"";
        q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
        result = q.execute();
        checkResult(result, 2, 4);
    }

    public void testSameNameSibling() throws RepositoryException {
        Node n = testRootNode.addNode("node", NT_UNSTRUCTURED);
        n.setProperty("myvalue", new String[]{"foo"});
        n = testRootNode.addNode("node", NT_UNSTRUCTURED);
        n.setProperty("myvalue", new String[]{"bar"});
        n = testRootNode.addNode("node", NT_UNSTRUCTURED);
        n.setProperty("yourvalue", new String[]{"foo"});

        testRootNode.save();

        String jcrql = "SELECT myvalue FROM * LOCATION " + testRoot + "/node";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(jcrql, Query.JCRQL);
        QueryResult result = q.execute();
        checkResult(result, 2, 2);

    }

}
