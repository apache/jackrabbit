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

import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>MoveReferenceableTest</code>...
 */
public class MoveReferenceableTest extends AbstractMoveTest {

    private static Logger log = LoggerFactory.getLogger(MoveReferenceableTest.class);

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (!moveNode.canAddMixin(mixReferenceable)) {
            throw new NotExecutableException("Cannot add mix:referencable to node to be moved.");
        }
        // prepare move-node
        moveNode.addMixin(mixReferenceable);
        moveNode.save();
    }

    @Override
    protected boolean isSessionMove() {
        return true;
    }

    /**
     * Test if a moved referenceable node still has the same uuid.
     */
    public void testMovedReferenceable() throws RepositoryException, NotExecutableException {

        String uuid = moveNode.getUUID();
        //move the node
        doMove(moveNode.getPath(), destinationPath);
        assertEquals("After successful moving a referenceable node node, the uuid must not have changed.", uuid, moveNode.getUUID());
    }

    /**
     * Same as {@link #testMovedReferenceable()}, but calls save before
     * executing the comparison.
     */
    public void testMovedReferenceable2() throws RepositoryException, NotExecutableException {

        String uuid = moveNode.getUUID();
        //move the node
        doMove(moveNode.getPath(), destinationPath);
        superuser.save();
        assertEquals("After successful moving a referenceable node node, the uuid must not have changed.", uuid, moveNode.getUUID());
    }

    /**
     * Test if a moved referenceable node returns the same item than the moved
     * node.
     */
    public void testAccessMovedReferenceableByUUID() throws RepositoryException, NotExecutableException {

        String uuid = moveNode.getUUID();
        //move the node
        doMove(moveNode.getPath(), destinationPath);

        Node n = superuser.getNodeByUUID(uuid);
        assertTrue("After successful moving a referenceable node node, accessing the node by uuid must return the same node.", n.isSame(moveNode));
    }

    /**
     * Same as {@link #testAccessMovedReferenceableByUUID()} but calls save()
     * before accessing the node again.
     */
    public void testAccessMovedReferenceableByUUID2() throws RepositoryException, NotExecutableException {

        String uuid = moveNode.getUUID();
        //move the node
        doMove(moveNode.getPath(), destinationPath);
        superuser.save();

        Node n = superuser.getNodeByUUID(uuid);
        assertTrue("After successful moving a referenceable node node, accessing the node by uuid must return the same node.", n.isSame(moveNode));
    }

    /**
     * Move a versionable (referenceable) node twice
     * 
     * @throws RepositoryException
     * @see <a href="https://issues.apache.org/jira/browse/JCR-2572">JCR-2572</a>
     */
    public void testMoveTwice() throws RepositoryException {
        moveNode.addMixin(mixVersionable);
        superuser.save();
        
        // move the node
        doMove(moveNode.getPath(), destinationPath);
        superuser.save();

        // move second time
        String destinationPath2 = destParentNode.getPath() + "/" + nodeName3;
        doMove(destinationPath, destinationPath2);
        superuser.save();
    }
}