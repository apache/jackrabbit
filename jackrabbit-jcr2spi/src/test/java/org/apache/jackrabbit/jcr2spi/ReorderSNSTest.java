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

import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ReorderSNSTest</code>...
 */
public class ReorderSNSTest extends ReorderTest {

    private static Logger log = LoggerFactory.getLogger(ReorderSNSTest.class);

    @Override
    protected void createOrderableChildren() throws RepositoryException, LockException, ConstraintViolationException, NoSuchNodeTypeException, ItemExistsException, VersionException {
        child1 = testRootNode.addNode(nodeName2, testNodeType);
        child2 = testRootNode.addNode(nodeName2, testNodeType);
        child3 = testRootNode.addNode(nodeName2, testNodeType);
        child4 = testRootNode.addNode(nodeName2, testNodeType);

        testRootNode.save();
    }

    public void testIndexAfterReorder() throws RepositoryException {
        testRootNode.orderBefore(getRelPath(child1), getRelPath(child3));
        assertTrue(child1.getIndex() == 2);
        assertTrue(child2.getIndex() == 1);
        assertTrue(child3.getIndex() == 3);
        assertTrue(child4.getIndex() == 4);

        testRootNode.save();
        assertTrue(child1.getIndex() == 2);
        assertTrue(child2.getIndex() == 1);
        assertTrue(child3.getIndex() == 3);
        assertTrue(child4.getIndex() == 4);
    }

    public void testReorder3() throws RepositoryException {
        String pathBefore = child3.getPath();

        testRootNode.orderBefore(getRelPath(child3), getRelPath(child1));
        testRootNode.save();

        Item itemIndex3 = testRootNode.getSession().getItem(pathBefore);
        assertTrue(itemIndex3.isSame(child2));

        Item item3 = testRootNode.getSession().getItem(child3.getPath());
        assertTrue(item3.isSame(child3));
    }
}
