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

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;

/**
 * <code>NodeTypeManagerImplTest</code>...
 */
public class NodeTypeManagerImplTest extends AbstractJCRTest {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(NodeTypeManagerImplTest.class);

    private NodeTypeManager ntMgr;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        ntMgr = superuser.getWorkspace().getNodeTypeManager();
    }

    public void testRegisterNodeTypes() throws RepositoryException {
        NodeTypeTemplate test = ntMgr.createNodeTypeTemplate();
        test.setName("testNodeType");

        ntMgr.registerNodeType(test, true);

        NodeType nt = ntMgr.getNodeType("testNodeType");
        assertNotNull(nt);
        assertEquals("testNodeType", nt.getName());

        test.setOrderableChildNodes(true);

        ntMgr.registerNodeType(test, true);

        nt = ntMgr.getNodeType("testNodeType");
        assertNotNull(nt);
        assertEquals("testNodeType", nt.getName());
        assertEquals(test.hasOrderableChildNodes(), nt.hasOrderableChildNodes());

        test.setDeclaredSuperTypeNames(new String[] {"nt:unstructured"});

        try {
            ntMgr.registerNodeType(test, false);
            fail("NodeTypeExistsException expected");
        } catch (NodeTypeExistsException e) {
            // success
        }
    }
    
    public void testUnregisterNodeTypes() throws RepositoryException {
        NodeTypeTemplate test = ntMgr.createNodeTypeTemplate();
        test.setName("testNodeType2");

        ntMgr.registerNodeType(test, true);

        NodeType nt = ntMgr.getNodeType("testNodeType2");
        assertNotNull(nt);
        assertEquals("testNodeType2", nt.getName());

        boolean supported = false;
        try {
            ntMgr.unregisterNodeType(test.getName());
            supported = true;
        } catch (UnsupportedRepositoryOperationException e) {
            // ok
        } catch (RepositoryException e) {
            // TODO improve
            if (e.getMessage().contains("not yet implemented")) {
                // ok (original message in jr-core)
            } else {
                throw e;
            }

        }

        if (supported) {
            try {
                ntMgr.getNodeType("testNodeType2");
                fail("should not be available any more");
            } catch (NoSuchNodeTypeException e) {
                // success
            }
        }
    }
}