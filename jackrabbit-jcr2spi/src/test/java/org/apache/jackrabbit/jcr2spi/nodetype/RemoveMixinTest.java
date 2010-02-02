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
 * <code>RemoveMixinTest</code>...
 */
public class RemoveMixinTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(RemoveMixinTest.class);

    private NodeTypeManager ntMgr;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ntMgr = testRootNode.getSession().getWorkspace().getNodeTypeManager();
    }

    @Override
    protected void tearDown() throws Exception {
        ntMgr = null;
        super.tearDown();
    }

    /**
     * Implementation specific test for 'removeMixin' only taking effect upon
     * save.
     *
     * @throws NotExecutableException
     * @throws RepositoryException
     */
    public void testRemoveMixinTakingAffectUponSave() throws NotExecutableException, RepositoryException {
        Node node;
        try {
            node = testRootNode.addNode(nodeName1, testNodeType);
            node.addMixin(mixReferenceable);
            testRootNode.save();
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        node.removeMixin(mixReferenceable);
        assertTrue("Removing Mixin must not take effect but after Node has been saved.", node.isNodeType(mixReferenceable));
        List<NodeType> mixins = Arrays.asList(node.getMixinNodeTypes());
        assertTrue("Removing Mixin must not take effect but after Node has been saved.", mixins.contains(ntMgr.getNodeType(mixReferenceable)));
    }

    /**
     * Implementation specific test
     *
     * @throws NotExecutableException
     * @throws RepositoryException
     */
    public void testAddAndRemoveMixinFromNew() throws NotExecutableException, RepositoryException {
        Node node;
        try {
            node = testRootNode.addNode(nodeName1, testNodeType);
            node.addMixin(mixReferenceable);
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        node.removeMixin(mixReferenceable);
        testRootNode.save();

        assertFalse("Adding + Removing a mixin within the same batch must have not effect.", node.isNodeType(mixReferenceable));
        List<NodeType> mixins = Arrays.asList(node.getMixinNodeTypes());
        assertFalse("Adding + Removing a mixin within the same batch must have not effect.", mixins.contains(ntMgr.getNodeType(mixReferenceable)));
    }

    /**
     * Implementation specific test
     *
     * @throws NotExecutableException
     * @throws RepositoryException
     */
    public void testAddAndRemoveMixin() throws NotExecutableException, RepositoryException {
        Node node;
        try {
            node = testRootNode.addNode(nodeName1, testNodeType);
            testRootNode.save();
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        node.addMixin(mixReferenceable);
        node.removeMixin(mixReferenceable);
        testRootNode.save();

        assertFalse("Adding + Removing a mixin within the same batch must have not effect.", node.isNodeType(mixReferenceable));
        List<NodeType> mixins = Arrays.asList(node.getMixinNodeTypes());
        assertFalse("Adding + Removing a mixin within the same batch must have not effect.", mixins.contains(ntMgr.getNodeType(mixReferenceable)));
    }
}