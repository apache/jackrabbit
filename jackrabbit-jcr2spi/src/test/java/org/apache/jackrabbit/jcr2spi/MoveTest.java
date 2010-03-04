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

import java.io.ByteArrayInputStream;

import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>MoveTest</code>...
 */
public class MoveTest extends AbstractMoveTest {

    private static Logger log = LoggerFactory.getLogger(MoveTest.class);

    @Override
    protected boolean isSessionMove() {
        return true;
    }

    public void testMoveRoot() throws RepositoryException {
        Node root = superuser.getRootNode();
        try {
            doMove(root.getPath(), destinationPath);
            fail("Moving the root node must fail with RepositoryException.");
        } catch (RepositoryException e) {
            // OK
        }
    }

    public void testMoveBelowDescendant() throws RepositoryException {
        try {
            doMove(srcParentNode.getPath(), moveNode.getPath() + "/" + nodeName2);
            fail("Moving the ancestor node below descendant must fail with RepositoryException.");
        } catch (RepositoryException e) {
            // OK
        }
    }

    public void testMoveDestinationWithIndex() throws RepositoryException {
        try {
            doMove(moveNode.getPath(), destinationPath + "[1]");
            fail("Moving to destination with index must fail with RepositoryException.");
        } catch (RepositoryException e) {
            // OK
        }
    }

    /**
     * Test if a moved node returns the specified destination path and by the
     * way test, if the moved node is still valid.
     */
    public void testMovedNodeGetPath() throws RepositoryException, NotExecutableException {
        String oldPath = moveNode.getPath();

        if (destParentNode.hasNode(nodeName2)) {
            throw new NotExecutableException("Move destination already contains a child node with name " + nodeName2);
        }
        //move the node
        doMove(oldPath, destinationPath);
        assertEquals("After successful move the moved node must return the destination path.", destinationPath, moveNode.getPath());
    }

    /**
     * Same as {@link #testMovedNodeGetPath()}, but calls save prior to the
     * test.
     */
    public void testMovedNodeGetPath2() throws RepositoryException, NotExecutableException {
        String oldPath = moveNode.getPath();

        if (destParentNode.hasNode(nodeName2)) {
            throw new NotExecutableException("Move destination already contains a child node with name " + nodeName2);
        }
        //move the node
        doMove(oldPath, destParentNode.getPath() + "/" + nodeName2);
        superuser.save();
        assertEquals("After successful move the moved node must return the destination path.", destinationPath, moveNode.getPath());
    }

    /**
     * Test if a moved node is not accessible by its old path any more
     */
    public void testAccessMovedNodeByOldPath() throws RepositoryException, NotExecutableException {
        NodeIterator it = srcParentNode.getNodes(moveNode.getName());
        int cnt = 0;
        while (it.hasNext()) {
            it.nextNode();
            cnt++;
        }
        if (cnt > 1) {
            throw new NotExecutableException("Move source parent has multiple child nodes with name " + moveNode.getName());
        }

        String oldPath = moveNode.getPath();

        //move the node
        doMove(oldPath, destinationPath);
        try {
            superuser.getItem(oldPath);
            fail("A moved node must not be accessible by its old path any more.");
        } catch (PathNotFoundException e) {
            // ok.
        }
    }

    /**
     * Same as {@link #testAccessMovedNodeByOldPath()} but calls save() prior to
     * the test.
     */
    public void testAccessMovedNodeByOldPath2() throws RepositoryException, NotExecutableException {
        NodeIterator it = srcParentNode.getNodes(moveNode.getName());
        int cnt = 0;
        while (it.hasNext()) {
            it.nextNode();
            cnt++;
        }
        if (cnt > 1) {
           throw new NotExecutableException("Move source parent has multiple child nodes with name " + moveNode.getName());
        }

        String oldPath = moveNode.getPath();

        //move the node
        doMove(oldPath, destinationPath);
        superuser.save();
        try {
            superuser.getItem(oldPath);
            fail("A moved node must not be accessible by its old path any more.");
        } catch (PathNotFoundException e) {
            // ok.
        }
    }

    /**
     * Test if the accessing the moved node from the session returns the same
     * Node object, than the Node which was moved before.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testMovedNodeIsSame() throws RepositoryException, NotExecutableException {
        if (destParentNode.hasNode(nodeName2)) {
            throw new NotExecutableException(destParentNode + " already has child node " + ". Test cannot be preformed if SNS is present.");
        }

        String oldPath = moveNode.getPath();
        String newPath = destParentNode.getPath() + "/" + nodeName2;

        //move the node
        doMove(oldPath, destinationPath);
        Item movedItem = superuser.getItem(newPath);
        assertTrue("Moved Node must be the same after the move.", movedItem.isSame(moveNode));
    }

    /**
     * Same as {@link #testMovedNodeIsSame()}, but calls save() before executing
     * the comparison.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testMovedNodeIsSame2() throws RepositoryException, NotExecutableException {
        if (destParentNode.hasNode(nodeName2)) {
            throw new NotExecutableException(destParentNode + " already has child node " + ". Test cannot be preformed if SNS is present.");
        }

        String oldPath = moveNode.getPath();

        //move the node
        doMove(oldPath, destinationPath);
        superuser.save();

        Item movedItem = superuser.getItem(destinationPath);
        assertTrue("Moved Node must be the same after the move.", movedItem.isSame(moveNode));
    }

    /**
     * Test if after the move, <code>Node.getParent()</code> returns the
     * destination parent.
     *
     * @throws RepositoryException
     */
    public void testMovedNodeParent() throws RepositoryException {
        //move the node
        doMove(moveNode.getPath(), destinationPath);
        assertTrue("Parent of moved node must be the destination parent node.", moveNode.getParent().isSame(destParentNode));
    }

    /**
     * Same as {@link #testMovedNodeParent()}, but calls save before executing
     * the comparison.
     *
     * @throws RepositoryException
     */
    public void testMovedNodeParent2() throws RepositoryException {
        //move the node
        doMove(moveNode.getPath(), destinationPath);
        superuser.save();

        assertTrue("Parent of moved node must be the destination parent node.", moveNode.getParent().isSame(destParentNode));
    }

    /**
     * Tries to move a node using <code>{@link javax.jcr.Session#move(String src, String dest)}
     * </code> to a location where a property already exists with same name.
     * <br/>
     * With JCR 1.0 this should throw an <code>{@link javax.jcr.ItemExistsException}</code>.
     * With JCR 2.0 the support for same-named property and node is optional and
     * the expected behaviour depends on the
     * {@link Repository#OPTION_NODE_AND_PROPERTY_WITH_SAME_NAME_SUPPORTED} descriptor.
     */
    public void testMovePropertyExists() throws RepositoryException, NotExecutableException {
        // try to create a property with the name of the node to be moved
        // to the destination parent
        Property destProperty;
        try {
            destProperty = destParentNode.setProperty(nodeName2, "anyString");
        } catch (RepositoryException e) {
            throw new NotExecutableException("Cannot create property with name '" +nodeName2+ "' and value 'anyString' at move destination.");
        }

        // TODO: fix 2.0 behaviour according to the OPTION_NODE_AND_PROPERTY_WITH_SAME_NAME_SUPPORTED descriptor
        if ("1.0".equals(getHelper().getRepository().getDescriptor(Repository.SPEC_VERSION_DESC))) {
            try {
                // move the node
                doMove(moveNode.getPath(), destProperty.getPath());
                fail("Moving a node to a location where a property exists must throw ItemExistsException");
            } catch (ItemExistsException e) {
                // ok, works as expected
            }
        } else {
            // move the node: same name property and node must be supported
            // see Issue 725
            doMove(moveNode.getPath(), destProperty.getPath());
        }
    }

    /**
     * Regression tests for JCR-2528
     * @throws RepositoryException
     */
    public void testMoveReferenceableNode() throws RepositoryException {
        moveNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
        moveNode.getSession().save();

        superuser.move(moveNode.getPath(), destParentNode.getPath() + "/" + moveNode.getName());
        superuser.save();
        destParentNode.remove();

        // JCR-2528 caused this call to throw a javax.jcr.InvalidItemStateException: Item has already
        // been removed by someone else. Status = REMOVED
        destParentNode.getSession().save();
    }


    public void testMoveFile() throws RepositoryException, NotExecutableException {
        // create a new file
        String parentPath;
        String filePath;
        try {
            Node parent = testRootNode.addNode("parent");
            Node n = JcrUtils.putFile(parent, "file", "text/plain", new ByteArrayInputStream("data".getBytes()));
            parentPath = parent.getPath();
            filePath = n.getPath();
            superuser.save();
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        Session s = getHelper().getSuperuserSession();
        try {
            Node n1 = s.getNode(filePath);
            Node n2 = n1.getNode("jcr:content");
            n2.setProperty("jcr:data", new java.io.ByteArrayInputStream("data2".getBytes()));
            n2.save();

            String destPath = parentPath + "1";
            if (isSessionMove()) {
                s.move(parentPath, destPath);
                s.save();
            } else {
                s.getWorkspace().move(parentPath, destPath);
            }
            Node n3 = s.getNode(destPath + "/file");
            Node n4 = n3.getNode("jcr:content");
            n4.refresh(false);
            // call must succeed (see JCR-2472)
            Node n5 = n3.getNode("jcr:content");
        } finally {
            s.logout();
        }
    }
}