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
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>AbstractMoveTest</code>...
 */
abstract class AbstractMoveTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(AbstractMoveTest.class);

    protected Node srcParentNode;
    protected Node destParentNode;
    protected Node moveNode;

    protected String destinationPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create parent node
        srcParentNode = testRootNode.addNode(nodeName1, testNodeType);
        // create node to be moved
        moveNode = srcParentNode.addNode(nodeName2, testNodeType);
        // create a node that will serve as new parent
        destParentNode = testRootNode.addNode(nodeName3, testNodeType);
        // save the new nodes
        testRootNode.save();

        destinationPath = destParentNode.getPath() + "/" + nodeName2;
    }

    @Override
    protected void tearDown() throws Exception {
        srcParentNode = null;
        destParentNode = null;
        moveNode = null;
        super.tearDown();
    }

    protected abstract boolean isSessionMove();

    protected void doMove(String srcPath, String destPath)
            throws RepositoryException, LockException, ConstraintViolationException, ItemExistsException, VersionException {
        if (isSessionMove()) {
            superuser.move(srcPath, destPath);
        } else {
            superuser.getWorkspace().move(srcPath, destPath);
        }
    }
}