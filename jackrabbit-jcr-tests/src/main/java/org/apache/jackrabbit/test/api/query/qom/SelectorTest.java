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

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.qom.QueryObjectModelConstants;

/**
 * <code>SelectorTest</code>...
 */
public class SelectorTest extends AbstractQOMTest {

    public void testSelector() throws RepositoryException {
        // make sure there's at least one node with this node type
        testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        Query q = qf.createQuery(
                qf.selector(testNodeType, "s"), null, null, null);
        NodeIterator it = q.execute().getNodes();
        while (it.hasNext()) {
            assertTrue("Wrong node type", it.nextNode().isNodeType(testNodeType));
        }
    }

    public void testSyntacticallyInvalidName() throws RepositoryException {
        try {
            Query q = qf.createQuery(
                    qf.selector(testNodeType + "[", "s"), null, null, null);
            q.execute();
            fail("Selector with syntactically invalid name must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testUnknownNodeType() throws RepositoryException {
        NodeTypeManager ntMgr = superuser.getWorkspace().getNodeTypeManager();
        String ntName = testNodeType;
        for (;;) {
            try {
                ntMgr.getNodeType(ntName);
                ntName += "x";
            } catch (NoSuchNodeTypeException e) {
                break;
            }
        }
        Query q = qf.createQuery(qf.selector(ntName, "s"),
                null, null, null);
        assertFalse("Selector must not select nodes for unknown node type",
                q.execute().getNodes().hasNext());
    }

    public void testDuplicateNodeType() throws RepositoryException {
        try {
            Query q = qf.createQuery(
                    qf.join(
                            qf.selector(testNodeType, "nt"),
                            qf.selector(testNodeType, "nt"),
                            QueryObjectModelConstants.JCR_JOIN_TYPE_INNER,
                            qf.descendantNodeJoinCondition("nt", "nt")),
                    null, null, null);
            q.execute();
            fail("Selector with two identical selector names must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

}
