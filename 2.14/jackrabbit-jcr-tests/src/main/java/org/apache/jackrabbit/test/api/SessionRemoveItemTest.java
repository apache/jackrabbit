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
package org.apache.jackrabbit.test.api;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.RepositoryStub;

/** <code>SessionRemoveItemTest</code>... */
public class SessionRemoveItemTest extends AbstractJCRTest {

    private Session adminSession;
    private Session readOnlySession;

    private Node removeNode;
    private String nPath;

    protected void setUp() throws Exception {
        super.setUp();

        adminSession = superuser;

        readOnlySession = getHelper().getReadOnlySession();

        removeNode = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.getSession().save();
        nPath = removeNode.getPath();
    }

    protected void tearDown() throws Exception {
        if (readOnlySession != null) {
            readOnlySession.logout();
        }
        super.tearDown();
    }

    public void testRemoveItem() throws RepositoryException {
        adminSession.removeItem(nPath);
        assertFalse(adminSession.nodeExists(nPath));
    }

    public void testRemoveItem2() throws RepositoryException {
        adminSession.removeItem(nPath);
        try {
            removeNode.getParent();
            fail("Cannot retrieve the parent from a transiently removed item.");
        } catch (InvalidItemStateException e) {
            // success
        }
    }

    public void testRemoveItem3() throws RepositoryException {
        adminSession.removeItem(nPath);
        readOnlySession.refresh(false); // see JCR-3302

        // node must still exist for another session.
        assertTrue(readOnlySession.nodeExists(nPath));
    }

    public void testRemoveItem4() throws RepositoryException {
        try {
            readOnlySession.refresh(false); // see JCR-3302

            readOnlySession.removeItem(nPath);
            readOnlySession.save();
            fail("A read-only session must not be allowed to remove an item");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testRemoveLockedNode() throws RepositoryException, NotExecutableException {
        ensureLockingSupported();
        ensureMixinType(removeNode, mixLockable);
        removeNode.save();

        // make sure the test node is locked.
        removeNode.lock(true, true);
        Session testSession = null;
        try {
            testSession = getHelper().getReadWriteSession();
            // removal of the locked node is a alteration of the parent, which
            // isn't locked -> must succeed.
            testSession.removeItem(nPath);
            testSession.save();
        } finally {
            if (testSession != null) {
                testSession.logout();
            }
        }
    }

    public void testRemoveLockedChildItem() throws RepositoryException, NotExecutableException {
        // add a child property and a child node to test deep lock effect.
        Node childN = removeNode.addNode(nodeName2);
        Value v = getJcrValue(superuser, RepositoryStub.PROP_PROP_VALUE2, RepositoryStub.PROP_PROP_TYPE2, "propvalue2");        
        Property childP = removeNode.setProperty(propertyName2, v);
        removeNode.save();

        ensureMixinType(removeNode, mixLockable);
        removeNode.save();

        // make sure the test node is locked.
        removeNode.lock(true, true);
        Session testSession = null;

        try {
            testSession = getHelper().getReadWriteSession();
            try {
                testSession.removeItem(childN.getPath());
                testSession.save();
                fail("Locked child node cannot be removed by another session.");
            } catch (LockException e) {
                // success
            }
            try {
                testSession.removeItem(childP.getPath());
                testSession.save();
                fail("Locked child node cannot be removed by another session.");
            } catch (LockException e) {
                // success
            }
        } finally {
            if (testSession != null) {
                testSession.logout();
            }
            removeNode.unlock();
        }
    }

    public void testRemoveCheckedInItem() throws RepositoryException, NotExecutableException {
        // add a child property and a child node to test deep lock effect.
        Node childN = removeNode.addNode(nodeName2);
        Value v = getJcrValue(superuser, RepositoryStub.PROP_PROP_VALUE2, RepositoryStub.PROP_PROP_TYPE2, "propvalue2");
        Property childP = removeNode.setProperty(propertyName2, v);
        removeNode.save();

        ensureMixinType(removeNode, mixVersionable);
        removeNode.save();

        removeNode.checkin();
        try {
            adminSession.removeItem(childP.getPath());
            adminSession.save();
            fail("child property of a checked-in node cannot be removed.");
        } catch (VersionException e) {
            // success
        }
        try {
            adminSession.removeItem(childN.getPath());
            adminSession.save();
            fail("child node of a checked-in node cannot be removed.");
        } catch (VersionException e) {
            // success
        }
    }
}