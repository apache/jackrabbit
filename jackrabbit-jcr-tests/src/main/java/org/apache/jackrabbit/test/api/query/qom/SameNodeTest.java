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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.qom.QueryObjectModel;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>SameNodeTest</code>...
 */
public class SameNodeTest extends AbstractQOMTest {

    public void testSameNode() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();

        QueryObjectModel qom = qf.createQuery(qf.selector(testNodeType, "s"),
                qf.sameNode("s", testRoot + "/" + nodeName1), null, null);
        checkQOM(qom, new Node[]{n});
    }

    public void testPathDoesNotExist() throws RepositoryException {
        QueryObjectModel qom = qf.createQuery(qf.selector(testNodeType, "s"),
                qf.sameNode("s", testRoot + "/" + nodeName1),
                null, null);
        checkQOM(qom, new Node[]{});
    }

    public void testChildNodesDoNotMatchSelector()
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
                        qf.sameNode("s", testRoot + "/" + nodeName1), null, null);
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
                    qf.sameNode("s", testPath), null, null);
            q.execute();
            fail("SameNode with relative path argument must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
        try {
            String stmt = "SELECT * FROM [" + testNodeType + "] AS s " +
                    "WHERE ISSAMENODE(s, [" + testPath + "]";
            qm.createQuery(stmt, Query.JCR_SQL2).execute();
            fail("ISSAMENODE() with relative path argument must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testSyntacticallyInvalidPath() throws RepositoryException {
        String invalidPath = testRoot + "/" + nodeName1 + "[";
        try {
            Query q = qf.createQuery(qf.selector(testNodeType, "s"),
                    qf.sameNode("s", invalidPath),
                    null, null);
            q.execute();
            fail("SameNode with syntactically invalid path argument must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
        try {
            String stmt = "SELECT * FROM [" + testNodeType + "] AS s " +
                    "WHERE ISSAMENODE(s, [" + invalidPath + "]";
            qm.createQuery(stmt, Query.JCR_SQL2).execute();
            fail("ISSAMENODE() with syntactically invalid path argument must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testNotASelectorName() throws RepositoryException {
        try {
            Query q = qf.createQuery(qf.selector(testNodeType, "s"),
                    qf.sameNode("x", testRoot), null, null);
            q.execute();
            fail("SameNode with an invalid selector name must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
        try {
            String stmt = "SELECT * FROM [" + testNodeType + "] AS s " +
                    "WHERE ISSAMENODE(x, [" + testRoot + "]";
            qm.createQuery(stmt, Query.JCR_SQL2).execute();
            fail("ISSAMENODE with an invalid selector name must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }
}
