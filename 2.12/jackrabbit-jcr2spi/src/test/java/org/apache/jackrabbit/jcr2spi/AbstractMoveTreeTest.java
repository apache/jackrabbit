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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>AbstractMoveTreeTest</code>...
 */
abstract class AbstractMoveTreeTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(AbstractMoveTreeTest.class);

    protected Node childNode;
    protected Node grandChildNode;
    protected Property childProperty;

    protected Node srcParentNode;
    protected Node destParentNode;

    protected String srcPath;
    protected String destinationPath;
    protected List<String> childPaths = new ArrayList<String>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        srcParentNode = testRootNode.addNode(nodeName1, testNodeType);
        Node moveNode = srcParentNode.addNode(nodeName2, testNodeType);
        destParentNode = testRootNode.addNode(nodeName3, testNodeType);

        srcPath = moveNode.getPath();
        destinationPath = destParentNode.getPath() + "/" + nodeName4;

        childProperty = moveNode.setProperty(propertyName2, "anyString");
        childNode = moveNode.addNode(nodeName2, testNodeType);
        grandChildNode = childNode.addNode(nodeName3, testNodeType);

        childPaths.add(grandChildNode.getPath());
        childPaths.add(childNode.getPath());

        doMove(moveNode.getPath(), destinationPath);
    }

    @Override
    protected void tearDown() throws Exception {
        childNode = null;
        grandChildNode = null;
        childProperty = null;
        srcParentNode = null;
        destParentNode = null;
        super.tearDown();
    }

    protected abstract boolean saveBeforeMove();

    protected abstract boolean isSessionMove();

    protected void doMove(String srcPath, String destPath) throws RepositoryException, LockException, ConstraintViolationException, ItemExistsException, VersionException {
        if (saveBeforeMove()) {
            testRootNode.save();
        }
        if (isSessionMove()) {
            superuser.move(srcPath, destPath);
        } else {
            superuser.getWorkspace().move(srcPath, destPath);
        }
    }
}