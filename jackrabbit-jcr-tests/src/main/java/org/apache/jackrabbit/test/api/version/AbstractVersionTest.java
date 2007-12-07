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
package org.apache.jackrabbit.test.api.version;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionHistory;

/**
 * <code>AbstractVersionTest</code> is the abstract base class for all
 * versioning related test classes.
 */
public class AbstractVersionTest extends AbstractJCRTest {

    protected NodeType versionableNodeType;
    protected NodeType nonVersionableNodeType;

    protected Node versionableNode;
    protected Node nonVersionableNode;

    protected String propertyValue;

    protected void setUp() throws Exception {
        super.setUp();

        NodeTypeManager ntMgr = superuser.getWorkspace().getNodeTypeManager();

        // assert that this repository support versioning
        try {
            NodeType versionableNt = ntMgr.getNodeType(mixVersionable);
            if (versionableNt == null) {
               fail("Repository does not support Versioning: mixin nodetype 'mix:versionable' is missing.");
            }
        } catch (NoSuchNodeTypeException e) {
            fail("Repository does not support Versioning: mixin nodetype 'mix:versionable' is missing.");
        }

        // retrieve versionable nodetype
        String versionableNodeTypeName = getProperty("versionableNodeType");
        try {
            versionableNodeType = ntMgr.getNodeType(versionableNodeTypeName);
            if (versionableNodeType == null) {
               fail("Property 'versionableNodeType' does not define a valid nodetype: '"+versionableNodeTypeName+"'");
            }
        } catch (NoSuchNodeTypeException e) {
            fail("Property 'versionableNodeType' does not define an existing nodetype: '"+versionableNodeTypeName+"'");
        }

        // make sure 'non-versionable' nodetype is properly defined
        try {
            nonVersionableNodeType = ntMgr.getNodeType(testNodeType);
            if (nonVersionableNodeType == null || nonVersionableNodeType.isNodeType(mixVersionable)) {
               fail("Property 'testNodeType' does define a versionable nodetype: '"+testNodeType+"'");
            }
        } catch (NoSuchNodeTypeException e) {
            fail("Property 'testNodeType' does not define an existing nodetype: '"+testNodeType+"'");
        }

        // build persistent versionable and non-versionable nodes
        try {
            versionableNode = createVersionableNode(testRootNode, nodeName1, versionableNodeType);
        } catch (RepositoryException e) {
            fail("Failed to create versionable test node." + e.getMessage());
        }
        try {
            nonVersionableNode = testRootNode.addNode(nodeName3, nonVersionableNodeType.getName());
            testRootNode.save();
        } catch (RepositoryException e) {
            fail("Failed to create non-versionable test node." + e.getMessage());
        }

        propertyValue = getProperty("propertyValue");
        if (propertyValue == null) {
            fail("Property 'propertyValue' is not defined.");
        }
    }

    protected void tearDown() throws Exception {
        // remove versionable nodes
        try {
            versionableNode.remove();
            testRootNode.save();
        } catch (Exception e) {
            log.println("Exception in tearDown: " + e.toString());
        } finally {
            versionableNodeType = null;
            nonVersionableNodeType = null;
            versionableNode = null;
            nonVersionableNode = null;
            super.tearDown();
        }
    }

    /**
     * Retrieve the number of versions present in the given version history.
     *
     * @param vHistory
     * @return number of versions
     * @throws RepositoryException
     */
    protected long getNumberOfVersions(VersionHistory vHistory) throws RepositoryException {
        return getSize(vHistory.getAllVersions());
    }

    /**
     * Create a versionable node below the given parent node. If the specified
     * nodetype name is not mix:versionable an attempt is made to add the
     * mix:versionable mixin type to the created child node.
     *
     * @param parent
     * @param name
     * @param nodetype
     * @return versionable node.
     * @throws RepositoryException
     */
    protected Node createVersionableNode(Node parent, String name, NodeType nodetype) throws RepositoryException {
        Node versionableNode = parent.addNode(name, nodetype.getName());
        if (!nodetype.isNodeType(mixVersionable)) {
            versionableNode.addMixin(mixVersionable);
        }
        parent.save();

        return versionableNode;
    }
}