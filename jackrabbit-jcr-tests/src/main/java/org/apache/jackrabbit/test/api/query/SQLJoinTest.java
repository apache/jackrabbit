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

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Node;

/**
 * Tests SQL statements with a join of a node type with a mixin type.
 *
 * @tck.config testroot path to node that allows child nodes of type:
 *   <code>nodetype</code>
 * @tck.config nodetype name of a node type that allows assignment of mixin
 *   referenceable.
 * @tck.config nodename1 name of a child node of type: <code>nodetype</code>.
 * @tck.config nodename2 name of a child node of type: <code>nodetype</code>.
 *
 * @test
 * @sources SQLJoinTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.SQLJoinTest
 * @keywords sql
 */
public class SQLJoinTest extends AbstractQueryTest {

    /**
     * Test a SQL query with a primary and mixin nodetype join.
     */
    public void testJoin() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        String testMixin = mixReferenceable;
        if (needsMixin(n1, testMixin)) {
            n1.addMixin(testMixin);
        }
        else {
            testMixin = mixVersionable;
            if (needsMixin(n1, testMixin)) {
                n1.addMixin(testMixin);
            }
        }

        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        testRootNode.save();

        assertFalse("Node at " + n2.getPath() + " should not have mixin " + testMixin, n2.isNodeType(testMixin));

        StringBuffer query = new StringBuffer("SELECT * FROM ");
        query.append(testNodeType).append(", ").append(testMixin);
        query.append(" WHERE ");
        query.append(testNodeType).append(".").append(jcrPath);
        query.append(" = ");
        query.append(mixReferenceable).append(".").append(jcrPath);
        query.append(" AND ").append(jcrPath).append(" LIKE ");
        query.append("'").append(testRoot).append("/%'");

        executeSqlQuery(superuser, query.toString(), new Node[]{n1});
    }

    /**
     * Test a SQL query with a nt:base primary type and mixin nodetype join.
     */
    public void testJoinNtBase() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        String testMixin = mixReferenceable;
        if (needsMixin(n1, testMixin)) {
            n1.addMixin(testMixin);
        }
        else {
            testMixin = mixVersionable;
            if (needsMixin(n1, testMixin)) {
                n1.addMixin(testMixin);
            }
        }

        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        testRootNode.save();

        assertFalse("Node at " + n2.getPath() + " should not have mixin " + testMixin, n2.isNodeType(testMixin));

        StringBuffer query = new StringBuffer("SELECT * FROM ");
        query.append(ntBase).append(", ").append(testMixin);
        query.append(" WHERE ");
        query.append(testNodeType).append(".").append(jcrPath);
        query.append(" = ");
        query.append(mixReferenceable).append(".").append(jcrPath);
        query.append(" AND ").append(jcrPath).append(" LIKE ");
        query.append("'").append(testRoot).append("/%'");

        executeSqlQuery(superuser, query.toString(), new Node[]{n1});
    }

    /**
     * Test a SQL query with a primary type and mixin nodetype join.
     */
    public void testJoinFilterPrimaryType() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        if (needsMixin(n1, mixReferenceable)) {
            n1.addMixin(mixReferenceable);
        }
        Node n2 = testRootNode.addNode(nodeName2, ntBase);
        if (needsMixin(n2, mixReferenceable)) {
            n2.addMixin(mixReferenceable);
        }
        testRootNode.save();

        StringBuffer query = new StringBuffer("SELECT * FROM ");
        query.append(testNodeType).append(", ").append(ntBase);
        query.append(" WHERE ");
        query.append(testNodeType).append(".").append(jcrPath);
        query.append(" = ");
        query.append(mixReferenceable).append(".").append(jcrPath);
        query.append(" AND ").append(jcrPath).append(" LIKE ");
        query.append("'").append(testRoot).append("/%'");

        executeSqlQuery(superuser, query.toString(), new Node[]{n1});
    }

    /**
     * Test a SQL query with a primary and mixin nodetype join on child nodes
     * with same name siblings.
     * @tck.config testroot path to node that allows child nodes with same name.
     * @tck.config nodename1 node name of the same name siblings.
     * @throws NotExecutableException if <code>testroot</code> does not allow
     *  same name siblings.
     */
    public void testJoinSNS() throws RepositoryException, NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        if (needsMixin(n1, mixReferenceable)) {
            n1.addMixin(mixReferenceable);
        }
        if (!n1.getDefinition().allowsSameNameSiblings()) {
            throw new NotExecutableException("Node at " + testRoot + " does not allow same name siblings with name " + nodeName1);
        }
        testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        n2.addMixin(mixReferenceable);
        testRootNode.save();

        StringBuffer query = new StringBuffer("SELECT * FROM ");
        query.append(testNodeType).append(", ").append(mixReferenceable);
        query.append(" WHERE ");
        query.append(testNodeType).append(".").append(jcrPath);
        query.append(" = ");
        query.append(mixReferenceable).append(".").append(jcrPath);
        query.append(" AND ").append(jcrPath).append(" LIKE ");
        query.append("'").append(testRoot).append("/%'");

        executeSqlQuery(superuser, query.toString(), new Node[]{n1, n2});
    }
}
