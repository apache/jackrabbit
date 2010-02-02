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
package org.apache.jackrabbit.jcr2spi.nodetype;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>AddMixinTest</code>...
 */
public class AddMixinTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(AddMixinTest.class);

    private NodeTypeManager ntMgr;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ntMgr = testRootNode.getSession().getWorkspace().getNodeTypeManager();
    }


    @Override
    protected void tearDown() throws Exception {
        testRootNode.refresh(false);
        ntMgr = null;
        super.tearDown();
    }

    /**
     * Implementation specific test for 'addMixin' only taking effect upon
     * save.
     *
     * @throws NotExecutableException
     * @throws RepositoryException
     */
    public void testAddMixinToNewNode() throws NotExecutableException, RepositoryException {
        Node newNode;
        try {
            newNode = testRootNode.addNode(nodeName1, testNodeType);
            newNode.addMixin(mixReferenceable);
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        assertFalse("Mixin must not be active before Node has been saved.", newNode.isNodeType(mixReferenceable));
        NodeType[] mixins = newNode.getMixinNodeTypes();
        for (int i = 0; i < mixins.length; i++) {
            if (mixins[i].getName().equals(testNodeType)) {
                fail("Mixin must not be active before Node has been saved.");
            }
        }
    }

    /**
     * Implementation specific test adding a new Node with a nodeType, that has
     * a mixin-supertype. The mixin must only take effect upon save.
     *
     * @throws NotExecutableException
     * @throws RepositoryException
     */
    public void testImplicitMixinOnNewNode() throws NotExecutableException, RepositoryException {
        Node newNode;
        try {
            String ntResource = superuser.getNamespacePrefix(NS_NT_URI) + ":resource";
            newNode = testRootNode.addNode(nodeName1, ntResource);
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        assertFalse("Implict Mixin inherited by primary Nodetype must not be active before Node has been saved.", newNode.isNodeType(mixReferenceable));
        NodeType[] mixins = newNode.getMixinNodeTypes();
        for (int i = 0; i < mixins.length; i++) {
            if (mixins[i].getName().equals(testNodeType)) {
                fail("Implict Mixin inherited by primary Nodetype must not be active before Node has been saved.");
            }
        }
    }

    /**
     * Implementation specific test adding a new Node with a nodeType, that has
     * a mixin-supertype. The mixin must only take effect upon save.
     *
     * @throws NotExecutableException
     * @throws RepositoryException
     */
    public void testAddMultipleAtOnce() throws NotExecutableException, RepositoryException {
        Node node;
        try {
            node = testRootNode.addNode(nodeName1, testNodeType);
            node.addMixin(mixReferenceable);
            node.addMixin(mixLockable);
            testRootNode.save();
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        assertTrue("Adding 2 mixins at once -> both must be present.", node.isNodeType(mixReferenceable) && node.isNodeType(mixLockable));
    }

    /**
     * Implementation specific test adding a new Node with a nodeType, that has
     * a mixin-supertype. The mixin must only take effect upon save.
     *
     * @throws NotExecutableException
     * @throws RepositoryException
     */
    public void testAddMultipleAtOnce2() throws NotExecutableException, RepositoryException {
        Node node;
        try {
            node = testRootNode.addNode(nodeName1, testNodeType);
            node.addMixin(mixReferenceable);
            node.addMixin(mixLockable);
            testRootNode.save();
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        List<NodeType> mixins = Arrays.asList(node.getMixinNodeTypes());
        assertTrue("Adding 2 mixins at once -> both must be present.", mixins.contains(ntMgr.getNodeType(mixReferenceable)) && mixins.contains(ntMgr.getNodeType(mixLockable)));
    }

    /**
     * Implementation specific test adding a new Node with a nodeType, that has
     * a mixin-supertype. The mixin must only take effect upon save.
     *
     * @throws NotExecutableException
     * @throws RepositoryException
     */
    public void testAddMultipleSeparately() throws NotExecutableException, RepositoryException {
        Node node;
        try {
            node = testRootNode.addNode(nodeName1, testNodeType);
            node.addMixin(mixReferenceable);
            testRootNode.save();
            node.addMixin(mixLockable);
            testRootNode.save();
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        assertTrue("Adding 2 mixins at once -> both must be present.", node.isNodeType(mixReferenceable) && node.isNodeType(mixLockable));
        List<NodeType> mixins = Arrays.asList(node.getMixinNodeTypes());
        assertTrue("Adding 2 mixins at once -> both must be present.", mixins.contains(ntMgr.getNodeType(mixReferenceable)) && mixins.contains(ntMgr.getNodeType(mixLockable)));
    }
}