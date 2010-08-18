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
import javax.jcr.PropertyType;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

/**
 * Test case for
 * <a href="https://issues.apache.org/jira/browse/JCR-2718">JCR-2718</a>
 */
public class JoinTest extends AbstractQueryTest {

    private Node node;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        node = testRootNode.addNode("jointest");

        Node n1 = node.addNode("node1");
        n1.addMixin(NodeType.MIX_REFERENCEABLE);
        testRootNode.getSession().save();

        Node n2 = node.addNode("node2");
        n2.addMixin(NodeType.MIX_REFERENCEABLE);
        testRootNode.getSession().save();

        Node n3 = node.addNode("node3");
        n3.addMixin(NodeType.MIX_REFERENCEABLE);
        n3.setProperty(
                "testref",
                new String[] { n1.getIdentifier(), n2.getIdentifier() },
                PropertyType.REFERENCE);
        testRootNode.getSession().save();
    }

    @Override
    protected void tearDown() throws Exception {
        node.remove();
        testRootNode.getSession().save();
        super.tearDown();
    }

    public void testMultiValuedReferenceJoin() throws Exception {
        String join =
            "SELECT a.*, b.*"
            + " FROM [nt:base] AS a"
            + " INNER JOIN [nt:base] AS b ON a.[jcr:uuid] = b.testref";
        QueryResult result = qm.createQuery(join, Query.JCR_SQL2).execute();
        checkResult(result, 2);
    }

}
