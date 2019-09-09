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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ReplaceNodeTest</code>
 */
public class ReplaceNodeTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(ReplaceNodeTest.class);

    private Node removeNode;
    private String uuid;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (testRootNode.hasNode(nodeName1)) {
            throw new NotExecutableException("Parent node must not yet contain a child node '" + nodeName1 + "'.");
        }
        removeNode = testRootNode.addNode(nodeName1, testNodeType);
        // make sure the new node is persisted.
        testRootNode.save();
        // assert removeNode is referenceable
        if (!removeNode.isNodeType(mixReferenceable)) {
            if (!removeNode.canAddMixin(mixReferenceable)) {
                throw new NotExecutableException("Cannot make remove-node '" + nodeName1 + "' mix:referenceable.");
            }
            removeNode.addMixin(mixReferenceable);
            testRootNode.save();
        }
        uuid = removeNode.getUUID();
    }

    @Override
    protected void tearDown() throws Exception {
        removeNode = null;
        super.tearDown();
    }

    public void testAddReplacementAfterRemove() throws RepositoryException {
        // transient removal of the 'removeNode'
        removeNode.remove();
        // add node that replaces the transiently removed node
        Node n = testRootNode.addNode(nodeName2, testNodeType);
        // ... and a child node.
        n.addNode(nodeName3, testNodeType);
        testRootNode.save();

        try {
            // if (for impl reasons) 'n' is referenceable -> it must have a
            // different uuid.
            assertFalse(uuid.equals(n.getUUID()));
        } catch (UnsupportedRepositoryOperationException e) {
            // n has not been made referenceable before -> OK.
        }
    }

    public void testAddReplacementAfterMove() throws RepositoryException {
        // transiently move the 'removeNode'
        superuser.move(removeNode.getPath(), testRootNode.getPath() + "/" + nodeName4);
        // add node that replaces the moved node
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        // ... and a child node.
        n.addNode(nodeName2, testNodeType);
        testRootNode.save();

        try {
            // if (for impl reasons) 'n' is referenceable -> it must have a
            // different uuid.
            assertFalse(uuid.equals(n.getUUID()));
        } catch (UnsupportedRepositoryOperationException e) {
            // n has not been made referenceable before -> OK.
        }
    }
}