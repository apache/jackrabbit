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

import java.util.Arrays;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>UpdateTest</code>...
 */
public class UpdateTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(UpdateTest.class);

    private String currentWorkspace;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        currentWorkspace = testRootNode.getSession().getWorkspace().getName();
    }

    public void testInvalidSrcWorkspace() throws RepositoryException {
        String nonExistingWorkspace = "nonExistingWorkspace";
        String[] accessibleWorkspaces = testRootNode.getSession().getWorkspace().getAccessibleWorkspaceNames();
        List<String> l = Arrays.asList(accessibleWorkspaces);
        while (l.contains(nonExistingWorkspace)) {
            nonExistingWorkspace = nonExistingWorkspace + "_";
        }

        try {
            testRootNode.update(nonExistingWorkspace);
        } catch (NoSuchWorkspaceException e) {
            //  ok
        }
    }

    public void testNoCorrespondingNode() throws RepositoryException, NotExecutableException {
        Node n = testRootNode.addNode(nodeName2, testNodeType);
        testRootNode.save();

        String srcWorkspace = null;
        String wspName = getHelper().getProperty("org.apache.jackrabbit.jcr2spi.workspace2.name");
        if (wspName == null) {
            throw new NotExecutableException("Cannot run update. Missing config param.");
        }
        try {
            n.getCorrespondingNodePath(wspName);
        } catch (ItemNotFoundException e) {
            srcWorkspace = wspName;
        } catch (RepositoryException e) {
            throw new NotExecutableException("Cannot run update. Workspace " + srcWorkspace + " does not exist or is not accessible.");
        }
        if (srcWorkspace == null) {
            throw new NotExecutableException("Cannot run update. No workspace found, that misses the corresponding node.");
        }

        try {
            // update without corresponding node must be a nop
            testRootNode.update(srcWorkspace);
        } catch (RepositoryException e) {
            fail("Update with workspace that doesn't contain the corresponding node must work.");
        }
    }

    public void testSameWorkspace() throws RepositoryException, NotExecutableException {
        try {
            // update without corresponding node must be a nop
            testRootNode.update(currentWorkspace);
        } catch (RepositoryException e) {
            fail("Update with srcWorkspace == this workspace must return silently.");
        }
    }

    public void testPendingChangesSameWorkspace() throws RepositoryException, NotExecutableException {
        testRootNode.addNode(nodeName2, testNodeType);

        try {
            testRootNode.update(currentWorkspace);
            fail("Update while changes are pending must fail with InvalidItemStateException");
        } catch (InvalidItemStateException  e) {
            // ok
        }
    }

    public void testPendingChanges() throws RepositoryException, NotExecutableException {
        testRootNode.addNode(nodeName2, testNodeType);

        String srcWorkspace = getAnotherWorkspace();
        try {
            testRootNode.update(srcWorkspace);
            fail("Update while changes are pending must fail with InvalidItemStateException");
        } catch (InvalidItemStateException  e) {
            // ok
        }
    }

    public void testPendingChangesOnOtherNode() throws RepositoryException, NotExecutableException {
        try {
        Node root = testRootNode.getSession().getRootNode();
            if (root.isSame(testRootNode)) {
                throw new NotExecutableException();
            }
            if (root.canAddMixin(mixLockable)) {
                root.addMixin(mixLockable);
            } else {
                root.setProperty(propertyName1, "anyValue");
            }
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        String srcWorkspace = getAnotherWorkspace();
        try {
            testRootNode.update(srcWorkspace);
            fail("Update while changes are pending must fail with InvalidItemStateException");
        } catch (InvalidItemStateException  e) {
            // ok
        }
    }

    public void testUpdateRemovesExtraProperty() throws RepositoryException, NotExecutableException {
        // create test node in default workspace
        testRootNode.setProperty(propertyName2, "test");
        testRootNode.save();

        String srcWorkspace = getAnotherWorkspace();
        // get the root node in the second workspace
        Session session2 = getHelper().getSuperuserSession(srcWorkspace);
        try {
            // make sure the source-session has the corresponding node.
            Node testRootW2 = (Node) session2.getItem(testRootNode.getCorrespondingNodePath(srcWorkspace));
            if (testRootW2.hasProperty(propertyName2)) {
                throw new NotExecutableException();
            }

            // call the update method on test node in default workspace
            testRootNode.update(srcWorkspace);

            // ok first check if node has no longer properties
            assertFalse("Node updated with Node.update() should have property removed", testRootNode.hasProperty(propertyName2));
        } catch (PathNotFoundException e) {
            throw new NotExecutableException();
        } catch (ItemNotFoundException e) {
            throw new NotExecutableException();
        } finally {
            session2.logout();
        }
    }

    public void testUpdateAddsMissingSubtree() throws RepositoryException, NotExecutableException {
        String srcWorkspace = getAnotherWorkspace();
        // get the root node in the second workspace
        Session session2 = getHelper().getSuperuserSession(srcWorkspace);
        try {
            // make sure the source-session has the corresponding node.
            Node testRootW2 = (Node) session2.getItem(testRootNode.getCorrespondingNodePath(srcWorkspace));

            // create test node in second workspace
            Node aNode2 = testRootW2.addNode(nodeName1, testNodeType);
            aNode2.addNode(nodeName2, testNodeType);
            aNode2.setProperty(propertyName2, "test");
            Property p2 = testRootW2.setProperty(propertyName1, "test");
            testRootW2.save();

            // call the update method on test node in default workspace
            testRootNode.update(srcWorkspace);

            // ok check if the child has been added
            boolean allPresent = testRootNode.hasNode(nodeName1) &&
                    testRootNode.hasNode(nodeName1+"/"+nodeName2) &&
                    testRootNode.hasProperty(nodeName1+"/"+propertyName2) &&
                    testRootNode.hasProperty(propertyName1);
            assertTrue("Node updated with Node.update() should have received childrens", allPresent);
        } catch (PathNotFoundException e) {
            throw new NotExecutableException();
        } catch (ItemNotFoundException e) {
            throw new NotExecutableException();
        } finally {
            session2.logout();
        }
    }

    /**
     * See JCR-2462
     */
    public void testSetSamePropertyTwice() throws RepositoryException {
        Node node = this.testRootNode.addNode( "test" );
        Session session = node.getSession();
        node.setProperty( "prop", "value1");
        node.setProperty( "prop", "value2");
        node.remove();
        session.save();
    }

    private String getAnotherWorkspace() throws NotExecutableException, RepositoryException {
        String srcWorkspace = getHelper().getProperty("org.apache.jackrabbit.jcr2spi.workspace2.name");;
        if (srcWorkspace == null || srcWorkspace.equals(currentWorkspace)) {
            throw new NotExecutableException("no alternative workspace configured");
        }

        String[] accessible = testRootNode.getSession().getWorkspace().getAccessibleWorkspaceNames();
        for (int i = 0; i < accessible.length; i++) {
            if (accessible[i].equals(srcWorkspace)) {
                return srcWorkspace;
            }
        }
        throw new NotExecutableException("configured workspace does not exist.");
    }
}
