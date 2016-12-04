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
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ReorderNewAndSavedTest</code>...
 */
public class ReorderNewAndSavedTest extends ReorderTest {

    private static Logger log = LoggerFactory.getLogger(ReorderNewAndSavedTest.class);

    @Override
    protected void createOrderableChildren() throws RepositoryException, LockException, ConstraintViolationException, NoSuchNodeTypeException, ItemExistsException, VersionException {
        child1 = testRootNode.addNode(nodeName1, testNodeType);
        child2 = testRootNode.addNode(nodeName2, testNodeType);
        testRootNode.save();

        child3 = testRootNode.addNode(nodeName3, testNodeType);
        child4 = testRootNode.addNode(nodeName4, testNodeType);
    }

    @Override
    public void testRevertReorder() throws RepositoryException {
        testRootNode.orderBefore(getRelPath(child4), getRelPath(child2));
        testOrder(testRootNode, new Node[] { child1, child4, child2, child3});

        testRootNode.refresh(false);
        testOrder(testRootNode, new Node[] { child1, child2 });
    }

    @Override
    public void testRevertReorderToEnd() throws RepositoryException {
        testRootNode.orderBefore(getRelPath(child1), null);
        testOrder(testRootNode, new Node[] { child2, child3, child4, child1});

        testRootNode.refresh(false);
        testOrder(testRootNode, new Node[] { child1, child2 });
    }
}
