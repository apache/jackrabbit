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
package org.apache.jackrabbit.core.search;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Tests path predicates in SQL queries.
 */
public class SQLPathTest extends AbstractQueryTest {

    private Node n1;
    private Node n2;
    private Node n11;
    private Node n12;
    private Node n21;
    private Node n22;

    protected void setUp() throws Exception {
        super.setUp();
        n1 = testRootNode.addNode("node1");
        n2 = testRootNode.addNode("node2");
        n11 = n1.addNode("node11");
        n12 = n1.addNode("node12");
        n21 = n2.addNode("node21");
        n22 = n2.addNode("node22");
        testRootNode.save();
    }

    public void testDescendantTestRoot() throws RepositoryException {
        String sql = getStatement(testRoot + "/%");
        executeSQLQuery(sql, new Node[]{n1, n11, n12, n2, n21, n22});
    }

    public void testDescendantSubNode() throws RepositoryException {
        String sql = getStatement(testRoot + "/node1/%");
        executeSQLQuery(sql, new Node[]{n11, n12});
    }

    public void testDescendantLeaf() throws RepositoryException {
        String sql = getStatement(testRoot + "/node1/node11/%");
        executeSQLQuery(sql, new Node[0]);
    }

    public void testDescendantSelfTestRoot() throws RepositoryException {
        String sql = getStatement(testRoot + "/%/node1");
        sql += " OR jcr:path = '" + testRoot + "/node1'";
        executeSQLQuery(sql, new Node[]{n1});
    }

    public void testChildAxisRoot() throws RepositoryException {
        String sql = getStatement("/%");
        sql += " AND NOT jcr:path = '/%/%'";
        Node[] nodes = toArray(superuser.getRootNode().getNodes());
        executeSQLQuery(sql, nodes);
    }

    public void testChildAxisTestRoot() throws RepositoryException {
        String sql = getStatement(testRoot + "/%");
        sql += " AND NOT jcr:path = '" + testRoot + "/%/%'";
        executeSQLQuery(sql, new Node[]{n1, n2});
    }

    public void testChildAxisLeaf() throws RepositoryException {
        String sql = getStatement(testRoot + "/node1/node11/%");
        sql += " AND NOT jcr:path = '" + testRoot + "/node1/node11/%/%'";
        executeSQLQuery(sql, new Node[0]);
    }

    //-----------------------------< internal >---------------------------------

    /**
     * Creates a SQL statement with a path predicate.
     * @param path the path
     * @return the SQL statement.
     */
    private String getStatement(String path) {
        return "SELECT * FROM nt:base WHERE jcr:path LIKE '" + path + "'";
    }
}
