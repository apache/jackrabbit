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
package org.apache.jackrabbit.test.api.query.qom;

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.qom.QueryObjectModel;

/**
 * <code>DescendantNodeTest</code> contains test cases related to QOM
 * DescendantNode constraints.
 */
public class DescendantNodeTest extends AbstractQOMTest {

    public void testDescendantNode() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();

        QueryObjectModel qom = qf.createQuery(qf.selector(testNodeType, "s"),
                qf.descendantNode("s", testRoot), null, null);
        checkQOM(qom, new Node[]{n});
    }

    public void testDescendantNodes() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        Node n21 = n2.addNode(nodeName1, testNodeType);
        superuser.save();

        QueryObjectModel qom = qf.createQuery(qf.selector(testNodeType, "s"),
                qf.descendantNode("s", testRoot), null, null);
        checkQOM(qom, new Node[]{n1, n2, n21});
    }

    public void testPathDoesNotExist() throws RepositoryException {
        QueryObjectModel qom = qf.createQuery(qf.selector(testNodeType, "s"),
                qf.descendantNode("s", testRoot + "/" + nodeName1),
                null, null);
        checkQOM(qom, new Node[]{});
    }

    public void testDescendantNodesDoNotMatchSelector()
            throws RepositoryException, NotExecutableException {
        testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();

        NodeTypeManager ntMgr = superuser.getWorkspace().getNodeTypeManager();
        NodeTypeIterator it = ntMgr.getPrimaryNodeTypes();
        NodeType testNt = ntMgr.getNodeType(testNodeType);
        while (it.hasNext()) {
            NodeType nt = it.nextNodeType();
            if (!testNt.isNodeType(nt.getName())) {
                // perform test
                QueryObjectModel qom = qf.createQuery(qf.selector(nt.getName(), "s"),
                        qf.descendantNode("s", testRoot), null, null);
                checkQOM(qom, new Node[]{});
                return;
            }
        }
        throw new NotExecutableException("No suitable node type found to " +
                "perform test against '" + testNodeType + "' nodes");
    }

    public void testRelativePath() throws RepositoryException {
        try {
            Query q = qf.createQuery(qf.selector(testNodeType, "s"),
                    qf.descendantNode("s", testPath), null, null);
            q.execute();
            fail("DescendantNode with relative path argument must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
        try {
            String stmt = "SELECT * FROM [" + testNodeType + "] AS s WHERE " +
                    "ISDESCENDANTNODE(s, [" + testPath + "])";
            qm.createQuery(stmt, Query.JCR_SQL2).execute();
            fail("ISDESCENDANTNODE() with relative path argument must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testSyntacticallyInvalidPath() throws RepositoryException {
        String invalidPath = testRoot + "/" + nodeName1 + "[";
        try {
            Query q = qf.createQuery(qf.selector(testNodeType, "s"),
                    qf.descendantNode("s", invalidPath), null, null);
            q.execute();
            fail("DescendantNode with syntactically invalid path argument must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
        try {
            String stmt = "SELECT * FROM [" + testNodeType + "] AS s WHERE " +
                    "ISDESCENDANTNODE(s, [" + invalidPath + "])";
            qm.createQuery(stmt, Query.JCR_SQL2).execute();
            fail("ISDESCENDANTNODE() with syntactically invalid path argument must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testNotASelectorName() throws RepositoryException {
        try {
            Query q = qf.createQuery(qf.selector(testNodeType, "s"),
                    qf.descendantNode("x", testRoot), null, null);
            q.execute();
            fail("DescendantNode with an unknown selector name must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
        try {
            String stmt = "SELECT * FROM [" + testNodeType + "] AS s WHERE " +
                    "ISDESCENDANTNODE(x, [" + testRoot + "])";
            qm.createQuery(stmt, Query.JCR_SQL2).execute();
            fail("ISDESCENDANTNODE() with an unknown selector name must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

}
