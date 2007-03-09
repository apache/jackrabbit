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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemExistsException;
import javax.jcr.Session;
import javax.jcr.Property;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.lock.LockException;
import java.util.List;
import java.util.Arrays;

/**
 * <code>UpdateTest</code>...
 */
public class UpdateTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(UpdateTest.class);

    private String currentWorkspace;
    private String[] accessibleWorkspaces;

    protected void setUp() throws Exception {
        super.setUp();

        currentWorkspace = testRootNode.getSession().getWorkspace().getName();
        accessibleWorkspaces = testRootNode.getSession().getWorkspace().getAccessibleWorkspaceNames();
    }

    public void testInvalidSrcWorkspace() throws RepositoryException, InvalidItemStateException, AccessDeniedException {
        String nonExistingWorkspace = "nonExistingWorkspace";
        List l = Arrays.asList(accessibleWorkspaces);
        while (l.contains(nonExistingWorkspace)) {
            nonExistingWorkspace = nonExistingWorkspace + "_";
        }

        try {
            testRootNode.update(nonExistingWorkspace);
        } catch (NoSuchWorkspaceException e) {
            //  ok
        }
    }

    public void testNoCorrespondingNode() throws RepositoryException, InvalidItemStateException, AccessDeniedException, NotExecutableException {
        Node n = testRootNode.addNode(nodeName2, testNodeType);
        testRootNode.save();

        String srcWorkspace = null;
        for (int i = 0; i < accessibleWorkspaces.length; i++) {
            if (!accessibleWorkspaces.equals(currentWorkspace)) {
                try {
                    n.getCorrespondingNodePath(accessibleWorkspaces[i]);
                } catch (ItemNotFoundException e) {
                    srcWorkspace = accessibleWorkspaces[i];
                }
            }
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

    public void testSameWorkspace() throws RepositoryException, InvalidItemStateException, AccessDeniedException, NotExecutableException {
        try {
            // update without corresponding node must be a nop
            testRootNode.update(currentWorkspace);
        } catch (RepositoryException e) {
            fail("Update with srcWorkspace == this workspace must return silently.");
        }
    }

    public void testPendingChangesSameWorkspace() throws RepositoryException, InvalidItemStateException, AccessDeniedException, NotExecutableException {
        testRootNode.addNode(nodeName2, testNodeType);

        try {
            testRootNode.update(currentWorkspace);
            fail("Update while changes are pending must fail with InvalidItemStateException");
        } catch (InvalidItemStateException  e) {
            // ok
        }
    }

    public void testPendingChanges() throws RepositoryException, LockException, ConstraintViolationException, NoSuchNodeTypeException, ItemExistsException, VersionException, NotExecutableException {
        testRootNode.addNode(nodeName2, testNodeType);

        String srcWorkspace = getAnotherWorkspace();
        try {
            testRootNode.update(srcWorkspace);
            fail("Update while changes are pending must fail with InvalidItemStateException");
        } catch (InvalidItemStateException  e) {
            // ok
        }
    }

    public void testPendingChangesOnOtherNode() throws RepositoryException, LockException, ConstraintViolationException, NoSuchNodeTypeException, ItemExistsException, VersionException, NotExecutableException {
        try {
        Node root = testRootNode.getSession().getRootNode();
            if (root.isSame(testRootNode)) {
                throw new NotExecutableException();
            }
            if (root.canAddMixin("mixLockable")) {
                root.addMixin("mixLockable");
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
        Session session2 = helper.getSuperuserSession(srcWorkspace);
        // make sure the source-session has the corresponding node.
        Node testRootW2 = (Node) session2.getItem(testRootNode.getCorrespondingNodePath(srcWorkspace));
        if (testRootW2.hasProperty(propertyName2)) {
            throw new NotExecutableException();
        }

        // call the update method on test node in default workspace
        testRootNode.update(srcWorkspace);

        // ok first check if node has no longer propertis
        assertFalse("Node updated with Node.update() should have property removed", testRootNode.hasProperty(propertyName2));
    }

    public void testUpdateAddsMissingSubtree() throws RepositoryException, NotExecutableException {
        String srcWorkspace = getAnotherWorkspace();
        // get the root node in the second workspace
        Session session2 = helper.getSuperuserSession(srcWorkspace);
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
    }

    private String getAnotherWorkspace() throws NotExecutableException {
        String srcWorkspace = null;
        for (int i = 0; i < accessibleWorkspaces.length; i++) {
            if (!accessibleWorkspaces.equals(currentWorkspace)) {
                srcWorkspace = accessibleWorkspaces[i];
            }
        }
        if (srcWorkspace == null) {
            throw new NotExecutableException("Cannot run update. No workspace found, that misses the corresponding node.");
        }
        return srcWorkspace;
    }
}