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
package org.apache.jackrabbit.jcr2spi;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ReorderNewSNSTest</code>...
 */
public class ReorderNewSNSTest extends ReorderTest {

    private static Logger log = LoggerFactory.getLogger(ReorderNewSNSTest.class);

    @Override
    protected void createOrderableChildren() throws RepositoryException, LockException, ConstraintViolationException, NoSuchNodeTypeException, ItemExistsException, VersionException {
        child1 = testRootNode.addNode(nodeName2, testNodeType);
        child2 = testRootNode.addNode(nodeName2, testNodeType);
        child3 = testRootNode.addNode(nodeName2, testNodeType);
        child4 = testRootNode.addNode(nodeName2, testNodeType);
    }

    @Override
    public void testRevertReorder() throws RepositoryException {
        testRootNode.orderBefore(getRelPath(child4), getRelPath(child2));
        testOrder(testRootNode, new Node[] { child1, child4, child2, child3});

        // NEW child nodes -> must be removed upon refresh
        testRootNode.refresh(false);
        NodeIterator it = testRootNode.getNodes(nodeName2);
        if (it.hasNext()) {
            fail("Reverting creation and reordering of new SNSs must remove the children again.");
        }
    }

    @Override
    public void testRevertReorderToEnd() throws RepositoryException {
        testRootNode.orderBefore(getRelPath(child1), null);
        testOrder(testRootNode, new Node[] { child2, child3, child4, child1});

        // NEW child nodes -> must be removed upon refresh
        testRootNode.refresh(false);
        NodeIterator it = testRootNode.getNodes(nodeName2);
        if (it.hasNext()) {
            fail("Reverting creation and reordering of new SNSs must remove the children again.");
        }
    }
}
