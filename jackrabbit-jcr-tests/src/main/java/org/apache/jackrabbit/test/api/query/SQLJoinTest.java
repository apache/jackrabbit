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
 * <ul>
 * <li>{@code testroot} path to node that allows child nodes of type:
 *   <code>nodetype</code>
 * <li>{@code nodetype} name of a node type that allows assignment of mixin
 *   referenceable.
 * <li>{@code nodename1} name of a child node of type: <code>nodetype</code>.
 * <li>{@code nodename2} name of a child node of type: <code>nodetype</code>.
 * </ul>
 */
public class SQLJoinTest extends AbstractQueryTest {

    /**
     * Test a SQL query with a primary and mixin nodetype join.
     */
    public void testJoin() throws RepositoryException, NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        String testMixin = mixReferenceable;
        if (needsMixin(n1, testMixin)) {
            ensureMixinType(n1, testMixin);
        } else {
            testMixin = mixVersionable;
            if (needsMixin(n1, testMixin)) {
                ensureMixinType(n1, testMixin);
            }
        }

        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        testRootNode.getSession().save();

        assertFalse("Node at " + n2.getPath() + " should not have mixin " + testMixin, n2.isNodeType(testMixin));

        StringBuffer query = new StringBuffer("SELECT * FROM ");
        query.append(testNodeType).append(", ").append(testMixin);
        query.append(" WHERE ");
        query.append(testNodeType).append(".").append(jcrPath);
        query.append(" = ");
        query.append(testMixin).append(".").append(jcrPath);
        query.append(" AND ").append(jcrPath).append(" LIKE ");
        query.append("'").append(testRoot).append("/%'");

        executeSqlQuery(superuser, query.toString(), new Node[]{n1});
    }

    /**
     * Test a SQL query with a nt:base primary type and mixin nodetype join.
     */
    public void testJoinNtBase() throws RepositoryException,
            NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        String testMixin = mixReferenceable;
        if (needsMixin(n1, testMixin)) {
            ensureMixinType(n1, testMixin);
        } else {
            testMixin = mixVersionable;
            if (needsMixin(n1, testMixin)) {
                ensureMixinType(n1, testMixin);
            }
        }

        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        testRootNode.getSession().save();

        assertFalse("Node at " + n2.getPath() + " should not have mixin " + testMixin, n2.isNodeType(testMixin));

        StringBuffer query = new StringBuffer("SELECT * FROM ");
        query.append(testNodeType).append(", ").append(testMixin);
        query.append(" WHERE ");
        query.append(testNodeType).append(".").append(jcrPath);
        query.append(" = ");
        query.append(testMixin).append(".").append(jcrPath);
        query.append(" AND ").append(jcrPath).append(" LIKE ");
        query.append("'").append(testRoot).append("/%'");

        executeSqlQuery(superuser, query.toString(), new Node[]{n1});
    }

    /**
     * Test a SQL query with a primary type and mixin nodetype join.
     */
    public void testJoinFilterPrimaryType()
            throws RepositoryException, NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        ensureMixinType(n1, mixReferenceable);
        String nodetype = testNodeTypeNoChildren == null ? ntBase : testNodeTypeNoChildren;
        Node n2 = testRootNode.addNode(nodeName2, nodetype);
        ensureMixinType(n2, mixReferenceable);
        testRootNode.getSession().save();

        StringBuffer query = new StringBuffer("SELECT * FROM ");
        query.append(testNodeType).append(", ").append(ntBase);
        query.append(" WHERE ");
        query.append(testNodeType).append(".").append(jcrPath);
        query.append(" = ");
        query.append(ntBase).append(".").append(jcrPath);
        query.append(" AND ").append(jcrPath).append(" LIKE ");
        query.append("'").append(testRoot).append("/%'");

        executeSqlQuery(superuser, query.toString(), new Node[]{n1});
    }

    /**
     * Test a SQL query with a primary and mixin nodetype join on child nodes
     * with same name siblings.
     * <ul>
     * <li>{@code testroot} path to node that allows child nodes with same name.
     * <li>{@code nodename1} node name of the same name siblings.
     * </ul>
     * @throws NotExecutableException if <code>testroot</code> does not allow
     *  same name siblings.
     */
    public void testJoinSNS() throws RepositoryException, NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        ensureMixinType(n1, mixReferenceable);
        if (!n1.getDefinition().allowsSameNameSiblings()) {
            throw new NotExecutableException("Node at " + testRoot + " does not allow same name siblings with name " + nodeName1);
        }
        testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        ensureMixinType(n2, mixReferenceable);
        testRootNode.getSession().save();

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
