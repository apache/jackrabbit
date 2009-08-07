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

import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>GetReferencesNodeTest</code> contains test to check if references are
 * returned from versions.
 *
 * @tck.config versionableNodeType name of a node type which is versionable
 * @tck.config testroot path to test root. Must allow versionable child nodes.
 * @tck.config nodename1 name of a versionable child node.
 * @tck.config nodename2 name of a versionable child node.
 * @tck.config propertyname1 name of a reference property declared in the
 *  versionable node type.
 *
 * @test
 * @sources GetReferencesNodeTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.GetReferencesNodeTest
 * @keywords versioning
 */
public class GetReferencesNodeTest extends AbstractJCRTest {

    private static final String PROP_VERSIONABLE_NODE_TYPE = "versionableNodeType";
    private String versionableNodeType;

    private Node testNode;
    private Node nodeToBeReferenced;

    protected void setUp() throws Exception {
        super.setUp();

        versionableNodeType = getProperty(PROP_VERSIONABLE_NODE_TYPE);
        if (versionableNodeType == null) {
            fail("Property '" + PROP_VERSIONABLE_NODE_TYPE + "' is not defined.");
        }
    }

    protected void tearDown() throws Exception {
        testRoot = null;
        nodeToBeReferenced = null;
        super.tearDown();
    }

    /**
     * Node.getReferences() never returns a reference that is stored in a
     * version. 1. Create some test nodes 2. Create a version 1.0 with reference
     * 3. Create a new version 1.1 after changing reference 4. Check if
     * reference is found by getReferences()
     */
    public void testGetReferencesNeverFromVersions() throws RepositoryException, NotExecutableException {
        // create some test nodes
        initTestNodes();

        // create a version 1.0 and reference test node
        testNode.checkout();
        ensureCanSetProperty(testNode, propertyName1, PropertyType.REFERENCE, false);
        testNode.setProperty(propertyName1, nodeToBeReferenced);

        testRootNode.save();
        testNode.checkin();

        // create a version 1.1 and remove reference
        testNode.checkout();
        testNode.getProperty(propertyName1).remove();
        testRootNode.save();
        testNode.checkin();

        // check if reference is returned
        boolean nodeToBeReferencedIsReference = false;
        PropertyIterator propIter = nodeToBeReferenced.getReferences();
        while (propIter.hasNext()) {
            nodeToBeReferencedIsReference = true;
            fail("Reference found in version.");
            // not successful
        }
        // references in versions should not be found
        assertFalse(nodeToBeReferencedIsReference);
    }

    private void initTestNodes() throws RepositoryException {
        // create a versionable node with reference property
        testNode = testRootNode.addNode(nodeName1, versionableNodeType);
        if (needsMixin(testNode, mixVersionable)) {
          testNode.addMixin(mixVersionable);
        }

        // node to be referenced, does not have to be versionable
        nodeToBeReferenced = testRootNode.addNode(nodeName2, versionableNodeType);
        testRootNode.save();
    }
}