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
package org.apache.jackrabbit.test.api.query;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.util.TraversingItemVisitor;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests path predicates in SQL queries. The default workspace must contain a
 * node tree at <code>testroot</code> with at least two levels.
 *
 * @test
 * @sources SQLPathTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.SQLPathTest
 * @keywords sql
 */
public class SQLPathTest extends AbstractQueryTest {

    /** A read-only session */
    private Session session;

    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
        session = helper.getReadOnlySession();
        // check precondition for this test
        if (testRootNode.hasNodes()) {
            for (NodeIterator it = testRootNode.getNodes(); it.hasNext();) {
                if (it.nextNode().hasNodes()) {
                    return;
                }
            }
        }
        fail("Default workspace at " + testRoot + " does not contain sufficient content.");
    }

    /**
     * Releases the session aquired in setUp().
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
            session = null;
        }
        super.tearDown();
    }

    /**
     * Tests if &lt;somepath>/% returns the descendants of &lt;somepath>.
     */
    public void testDescendantTestRoot() throws RepositoryException {
        String sql = getStatement(testRoot + "/%");
        executeSqlQuery(session, sql, getDescendants(testRootNode));
    }

    /**
     * Tests if &lt;somepath>/% returns no nodes if node at &lt;somepath>
     * is a leaf.
     */
    public void testDescendantLeaf() throws RepositoryException {
        // find leaf
        Node leaf = testRootNode;
        while (leaf.hasNodes()) {
            leaf = leaf.getNodes().nextNode();
        }
        String sql = getStatement(leaf.getPath() + "/%");
        executeSqlQuery(session, sql, new Node[0]);
    }

    /**
     * Tests if &lt;somepath>/%/&lt;nodename> OR &lt;somepath>/&lt;nodename>
     * returns nodes with name &lt;nodename> which are descendants of
     * node at <code>testroot</code>.
     */
    public void testDescendantSelfTestRoot() throws RepositoryException {
        // get first node which is two levels deeper than node at testroot
        Node n = null;
        for (NodeIterator it = testRootNode.getNodes(); it.hasNext();) {
            Node child = it.nextNode();
            if (child.hasNodes()) {
                n = child.getNodes().nextNode();
                break;
            }
        }
        final String name = n.getName();
        String sql = getStatement(testRoot + "/%/" + name);
        sql += " OR " + jcrPath + " = '" + testRoot + "/" + name + "'";
        // gather the nodes with visitor
        final List nodes = new ArrayList();
        testRootNode.accept(new TraversingItemVisitor.Default() {
            protected void entering(Node node, int level) throws RepositoryException {
                if (node.getName().equals(name) && !testRootNode.isSame(node)) {
                    nodes.add(node);
                }
            }
        });
        executeSqlQuery(session, sql, (Node[]) nodes.toArray(new Node[nodes.size()]));
    }

    /**
     * Tests if /% AND NOT /%/% returns the child nodes of the root node.
     */
    public void testChildAxisRoot() throws RepositoryException {
        String sql = getStatement("/%");
        sql += " AND NOT " + jcrPath + " LIKE '/%/%'";
        Node[] nodes = toArray(session.getRootNode().getNodes());
        executeSqlQuery(session, sql, nodes);
    }

    /**
     * Tests if &lt;somepath>/% AND NOT &lt;somepath>/%/% returns the child
     * nodes of node at &lt;somepath>.
     */
    public void testChildAxisTestRoot() throws RepositoryException {
        String sql = getStatement(testRoot + "/%");
        sql += " AND NOT " + jcrPath + " LIKE '" + testRoot + "/%/%'";
        Node[] nodes = toArray(testRootNode.getNodes());
        executeSqlQuery(session, sql, nodes);
    }

    /**
     * Tests if &lt;somepath>/% AND NOT &lt;somepath>/%/% returns no nodes
     * if the node at &lt;somepath> is a leaf.
     */
    public void testChildAxisLeaf() throws RepositoryException {
        // find leaf
        Node leaf = testRootNode;
        while (leaf.hasNodes()) {
            leaf = leaf.getNodes().nextNode();
        }
        String sql = getStatement(leaf.getPath() + "/%");
        sql += " AND NOT " + jcrPath + " LIKE '" + leaf.getPath() + "/%/%'";
        executeSqlQuery(session, sql, new Node[0]);
    }

    //-----------------------------< internal >---------------------------------

    /**
     * Creates a SQL statement with a path predicate.
     * @param path the path
     * @return the SQL statement.
     */
    private String getStatement(String path) {
        return "SELECT * FROM " + ntBase + " WHERE " + jcrPath + " LIKE '" + path + "'";
    }

    /**
     * Returns the descendants of <code>node</code> as an array in document
     * order.
     * @param node the starting node.
     * @return descendants of <code>node</code>.
     * @throws RepositoryException if an error occurs.
     */
    private Node[] getDescendants(final Node node) throws RepositoryException {
        final List descendants = new ArrayList();

        node.accept(new TraversingItemVisitor.Default() {
            protected void entering(Node n, int level)
                    throws RepositoryException {
                if (!node.isSame(n)) {
                    descendants.add(n);
                }
            }
        });

        return (Node[]) descendants.toArray(new Node[descendants.size()]);
    }
}
