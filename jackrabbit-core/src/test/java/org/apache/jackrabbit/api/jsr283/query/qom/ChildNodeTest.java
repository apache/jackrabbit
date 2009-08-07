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
package org.apache.jackrabbit.api.jsr283.query.qom;

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.InvalidQueryException;

/**
 * <code>ChildNodeTest</code>...
 */
public class ChildNodeTest extends AbstractQOMTest {

    public void testChildNode() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();

        Query q = qomFactory.createQuery(qomFactory.selector(testNodeType, "s"),
                qomFactory.childNode("s", testRoot), null, null);
        checkResult(q.execute(), new Node[]{n});

        // using default selector
        q = qomFactory.createQuery(qomFactory.selector(testNodeType),
                qomFactory.childNode(testRoot), null, null);
        checkResult(q.execute(), new Node[]{n});
    }

    public void testChildNodes() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        Node n3 = testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.save();

        Query q = qomFactory.createQuery(qomFactory.selector(testNodeType, "s"),
                qomFactory.childNode("s", testRoot), null, null);
        checkResult(q.execute(), new Node[]{n1, n2, n3});

        // using default selector
        q = qomFactory.createQuery(qomFactory.selector(testNodeType),
                qomFactory.childNode(testRoot), null, null);
        checkResult(q.execute(), new Node[]{n1, n2, n3});
    }

    public void testPathDoesNotExist() throws RepositoryException {
        Query q = qomFactory.createQuery(qomFactory.selector(testNodeType, "s"),
                qomFactory.childNode("s", testRoot + "/" + nodeName1),
                null, null);
        checkResult(q.execute(), new Node[]{});

        // using default selector
        q = qomFactory.createQuery(qomFactory.selector(testNodeType),
                qomFactory.childNode(testRoot + "/" + nodeName1),
                null, null);
        checkResult(q.execute(), new Node[]{});
    }

    public void testChildNodesDoNotMatchSelector()
            throws RepositoryException, NotExecutableException {
        testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();

        NodeTypeManager ntMgr = superuser.getWorkspace().getNodeTypeManager();
        NodeTypeIterator it = ntMgr.getPrimaryNodeTypes();
        NodeType testNt = ntMgr.getNodeType(testNodeType);
        while (it.hasNext()) {
            NodeType nt = it.nextNodeType();
            if (!testNt.isNodeType(nt.getName())) {
                // perform test
                Query q = qomFactory.createQuery(qomFactory.selector(nt.getName(), "s"),
                        qomFactory.childNode("s", testRoot), null, null);
                checkResult(q.execute(), new Node[]{});

                // using default selector
                q = qomFactory.createQuery(qomFactory.selector(nt.getName()),
                        qomFactory.childNode(testRoot), null, null);
                checkResult(q.execute(), new Node[]{});
                return;
            }
        }
        throw new NotExecutableException("No suitable node type found to " +
                "perform test against '" + testNodeType + "' nodes");
    }

    public void testRelativePath() throws RepositoryException {
        try {
            Query q = qomFactory.createQuery(qomFactory.selector(testNodeType, "s"),
                    qomFactory.childNode("s", testPath), null, null);
            q.execute();
            fail("ChildNode with relative path argument must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }

        // using default selector
        try {
            Query q = qomFactory.createQuery(qomFactory.selector(testNodeType),
                    qomFactory.childNode(testPath), null, null);
            q.execute();
            fail("ChildNode with relative path argument must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testSyntacticallyInvalidPath() throws RepositoryException {
        try {
            Query q = qomFactory.createQuery(qomFactory.selector(testNodeType, "s"),
                    qomFactory.childNode("s", testRoot + "/" + nodeName1 + "["),
                    null, null);
            q.execute();
            fail("ChildNode with syntactically invalid path argument must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }

        // using default selector
        try {
            Query q = qomFactory.createQuery(qomFactory.selector(testNodeType),
                    qomFactory.childNode(testRoot + "/" + nodeName1 + "["),
                    null, null);
            q.execute();
            fail("ChildNode with syntactically invalid path argument must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testNotASelectorName() throws RepositoryException {
        try {
            Query q = qomFactory.createQuery(qomFactory.selector(testNodeType, "s"),
                    qomFactory.childNode("x", testRoot), null, null);
            q.execute();
            fail("ChildNode with an invalid selector name must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }

        // using default selector
        try {
            Query q = qomFactory.createQuery(qomFactory.selector(testNodeType),
                    qomFactory.childNode("x", testRoot), null, null);
            q.execute();
            fail("ChildNode with an invalid selector name must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testDefaultSelector() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();

        Query q = qomFactory.createQuery(qomFactory.selector(testNodeType, "s"),
                qomFactory.childNode(testRoot), null, null);
        checkResult(q.execute(), new Node[]{n});
    }
}
