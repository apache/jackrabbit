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
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.jcr2spi.state.Status;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.ItemNotFoundException;

/** <code>MixinModificationTest</code>... */
public class MixinModificationTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(MixinModificationTest.class);

    private static void assertItemStatus(Item item, int status) throws NotExecutableException {
        if (!(item instanceof ItemImpl)) {
            throw new NotExecutableException("org.apache.jackrabbit.jcr2spi.ItemImpl expected");
        }
        int st = ((ItemImpl) item).getItemState().getStatus();
        assertEquals("Expected status to be " + Status.getName(status) + ", was " + Status.getName(st), status, st);
    }

    public void testAddMixin() throws RepositoryException,
            NotExecutableException {
        Node n = testRootNode.addNode(nodeName1);
        testRootNode.save();

        if (n.isNodeType(mixVersionable)) {
            throw new NotExecutableException();
        }
        try {
            n.addMixin(mixVersionable);
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        assertItemStatus(n, Status.EXISTING_MODIFIED);
        assertTrue(n.hasProperty(jcrMixinTypes));
        Property p = n.getProperty(jcrMixinTypes);

        n.save();

        // after saving the affected target node must be marked 'invalidated'.
        // the property however should be set existing.
        assertItemStatus(n, Status.INVALIDATED);
        assertItemStatus(p, Status.EXISTING);
    }

    public void testAddMixin2() throws RepositoryException,
            NotExecutableException {
        Node n;
        try {
            n = testRootNode.addNode(nodeName1);
            n.addMixin(mixVersionable);
            testRootNode.save();
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        // after saving the affected target node must be marked 'invalidated'
        // even if adding the node and setting a mixin was achieved in the
        // same batch.
        assertItemStatus(n, Status.INVALIDATED);
    }

    public void testRemoveMixin() throws RepositoryException, NotExecutableException {
        String nPath;
        try {
            Node n = testRootNode.addNode(nodeName1);
            nPath = n.getPath();
            n.addMixin(mixReferenceable);
            testRootNode.save();
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        Session testSession = getHelper().getReadWriteSession();
        try {
            Node n = (Node) testSession.getItem(nPath);
            String uuid = n.getUUID();

            // remove the mixin again.
            n.removeMixin(mixReferenceable);
            assertFalse(n.hasProperty(jcrMixinTypes));
            n.save();

            // accessing node by uuid should not be possible any more.
            try {
                Node n2 = testSession.getNodeByUUID(uuid);
                fail();
            } catch (ItemNotFoundException e) {
                // ok
            }

            // however: the added node should still be valid. but not referenceable
            assertItemStatus(n, Status.EXISTING);
            assertFalse(n.isNodeType(mixReferenceable));
            assertTrue(testSession.itemExists(nPath));

            try {
                Node n2 = superuser.getNodeByUUID(uuid);
                fail();
            } catch (ItemNotFoundException e) {
                // ok
            }
        } finally {
            if (testSession != null) {
                testSession.logout();
            }
        }
    }
}