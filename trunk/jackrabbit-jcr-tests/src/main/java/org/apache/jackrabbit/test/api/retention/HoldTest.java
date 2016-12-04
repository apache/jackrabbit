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
package org.apache.jackrabbit.test.api.retention;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.retention.Hold;
import javax.jcr.retention.RetentionManager;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>RetentionManagerTest</code>...
 */
public class HoldTest extends AbstractRetentionTest {

    private static boolean containsHold(Hold[] holds, Hold toTest) throws RepositoryException {
        for (int i = 0; i < holds.length; i++) {
            if (holds[i].getName().equals(toTest.getName()) && holds[i].isDeep() == toTest.isDeep()) {
                return true;
            }
        }
        return false;
    }

    public void testAddHold() throws RepositoryException, NotExecutableException {
        Hold hold = retentionMgr.addHold(testNodePath, getHoldName(), false);
        Hold[] holds = retentionMgr.getHolds(testNodePath);
        assertTrue("getHolds must return the hold added before.", holds.length >= 1);
        assertTrue("getHolds doesn't return the hold added before", containsHold(holds, hold));
    }

    public void testAddHold2() throws RepositoryException, NotExecutableException {
        Hold[] holdsBefore = retentionMgr.getHolds(testNodePath);
        Hold hold = retentionMgr.addHold(testNodePath, getHoldName(), false);
        assertFalse("The hold added must not have been present before.", containsHold(holdsBefore, hold));
    }

    public void testAddHoldIsTransient() throws RepositoryException, NotExecutableException {
        Hold hold = retentionMgr.addHold(testNodePath, getHoldName(), false);
        Hold[] holds = retentionMgr.getHolds(testNodePath);

        // revert the changes made
        superuser.refresh(false);
        Hold[] holds2 = retentionMgr.getHolds(testNodePath);

        assertEquals("Reverting transient changes must revert the hold added.",
                holds.length -1, holds2.length);
        assertFalse("Reverting transient changes must revert the hold added.",
                containsHold(holds2, hold));
    }
    
    public void testRemoveHold() throws RepositoryException, NotExecutableException {
        Hold hold = retentionMgr.addHold(testNodePath, getHoldName(), false);

        Hold[] holds = retentionMgr.getHolds(testNodePath);

        retentionMgr.removeHold(testNodePath, hold);
        Hold[] holds2 = retentionMgr.getHolds(testNodePath);

        assertEquals("RetentionManager.removeHold should removed the hold added before.",
                holds.length -1, holds2.length);
        assertFalse("RetentionManager.removeHold should removed the hold added before.",
                containsHold(holds2, hold));
    }

    public void testRemoveHoldIsTransient() throws RepositoryException, NotExecutableException {
        Hold hold = retentionMgr.addHold(testNodePath, getHoldName(), false);
        superuser.save();
        try {
            Hold[] holds = retentionMgr.getHolds(testNodePath);

            retentionMgr.removeHold(testNodePath, hold);
            superuser.refresh(false);

            Hold[] holds2 = retentionMgr.getHolds(testNodePath);
            assertEquals("Reverting transient hold removal must restore the original state.",
                    Arrays.asList(holds), Arrays.asList(holds2));
        } finally {
            // clear the hold that was permanently added before.
            retentionMgr.removeHold(testNodePath, hold);
            superuser.save();
        }
    }

    public void testRemoveHoldFromChild() throws RepositoryException, NotExecutableException {
        String childPath = testRootNode.addNode(nodeName2, testNodeType).getPath();
        Hold hold = retentionMgr.addHold(testNodePath, getHoldName(), false);

        try {
            retentionMgr.removeHold(childPath, hold);
            fail("Removing hold from another node must fail");
        } catch (RepositoryException e) {
            // success
            assertTrue(containsHold(retentionMgr.getHolds(testNodePath), hold));
        }

        // check again with persisted hold
        superuser.save();
        try {
            retentionMgr.removeHold(childPath, hold);
            fail("Removing hold from another node must fail");
        } catch (RepositoryException e) {
            // success
            assertTrue(containsHold(retentionMgr.getHolds(testNodePath), hold));
        } finally {
            // clear the hold that was permanently added before.
            retentionMgr.removeHold(testNodePath, hold);
            superuser.save();
        }
    }

    public void testInvalidPath() throws RepositoryException, NotExecutableException {
        String invalidPath = testPath; // not an absolute path.
        try {
            retentionMgr.getHolds(invalidPath);
            fail("Accessing holds an invalid path must throw RepositoryException.");
        } catch (RepositoryException e) {
            // success
        }
        try {
            retentionMgr.addHold(invalidPath, getHoldName(), true);
            fail("Adding a hold at an invalid path must throw RepositoryException.");
        } catch (RepositoryException e) {
            // success
        }
        try {
            Hold h = retentionMgr.addHold(testNodePath, getHoldName(), true);
            retentionMgr.removeHold(invalidPath, h);
            fail("Removing a hold at an invalid path must throw RepositoryException.");
        } catch (RepositoryException e) {
            // success
        }
    }

    public void testNonExistingNodePath() throws RepositoryException, NotExecutableException {
        String invalidPath = testNodePath + "/nonexisting";
        int cnt = 0;
        while (superuser.nodeExists(invalidPath)) {
            invalidPath += cnt++;
        }

        try {
            retentionMgr.getHolds(invalidPath);
            fail("Accessing holds from non-existing node must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // success
        }
        try {
            retentionMgr.addHold(invalidPath, getHoldName(), true);
            fail("Adding a hold for a non-existing node must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // success
        }
        try {
            Hold h = retentionMgr.addHold(testNodePath, getHoldName(), true);
            retentionMgr.removeHold(invalidPath, h);
            fail("Removing a hold at a non-existing node must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // success
        }
    }

    public void testPropertyPath() throws RepositoryException, NotExecutableException {
        String propPath = null;
        for (PropertyIterator it = testRootNode.getProperties(); it.hasNext();) {
            String path = it.nextProperty().getPath();
            if (! superuser.nodeExists(path)) {
                propPath = path;
                break;
            }
        }
        if (propPath == null) {
            throw new NotExecutableException();
        }
        try {
            retentionMgr.getHolds(propPath);
            fail("Accessing holds from non-existing node must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // success
        }
        try {
            retentionMgr.addHold(propPath, getHoldName(), true);
            fail("Adding a hold for a non-existing node must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // success
        }
        try {
            Hold h = retentionMgr.addHold(testNodePath, getHoldName(), true);
            retentionMgr.removeHold(propPath, h);
            fail("Removing a hold at a non-existing node must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // success
        }
    }

    public void testInvalidName() {
        try {
            String invalidName = "*.[y]";
            retentionMgr.addHold(testNodePath, invalidName, false);
            fail("Adding a hold with an invalid JCR name must fail.");
        } catch (RepositoryException e) {
            // success
        }
    }

    public void testReadOnlySession() throws NotExecutableException, RepositoryException {
        javax.jcr.Session s = getHelper().getReadOnlySession();
        try {
            RetentionManager rmgr = getRetentionManager(s);
            try {
                rmgr.getHolds(testNodePath);
                fail("Read-only session doesn't have sufficient privileges to retrieve holds.");
            } catch (AccessDeniedException e) {
                // success
            }
            try {
                rmgr.addHold(testNodePath, getHoldName(), false);
                fail("Read-only session doesn't have sufficient privileges to retrieve holds.");
            } catch (AccessDeniedException e) {
                // success
            }
        } finally {
            s.logout();
        }
    }

    public void testAddHoldOnLockedNode() throws NotExecutableException, RepositoryException {
        Node child = getLockedChildNode();
        // remember current holds for clean up.
        List<Hold> holdsBefore = Arrays.asList(retentionMgr.getHolds(child.getPath()));

        // get another session.
        javax.jcr.Session otherS = getHelper().getSuperuserSession();
        try {
            RetentionManager rmgr = getRetentionManager(otherS);            
            rmgr.addHold(child.getPath(), getHoldName(), false);
            otherS.save();

            fail("Adding hold on a locked node must throw LockException.");
        } catch (LockException e) {
            // success
        } finally {
            otherS.logout();

            // clear holds (in case of test failure)
            List<Hold> holds = new ArrayList<Hold>(Arrays.asList(retentionMgr.getHolds(child.getPath())));
            if (holds.removeAll(holdsBefore)) {
                for (Iterator<Hold> it = holds.iterator(); it.hasNext();) {
                    retentionMgr.removeHold(child.getPath(), (Hold) it.next());
                }
            }
            superuser.save();
        }
    }

    public void testRemoveHoldOnLockedNode() throws NotExecutableException, RepositoryException {
        Node child = getLockedChildNode();
        Hold h = retentionMgr.addHold(child.getPath(), getHoldName(), false);
        testRootNode.getSession().save();

        javax.jcr.Session otherS = getHelper().getSuperuserSession();
        try {
            RetentionManager rmgr = getRetentionManager(otherS);
            Hold[] holds = rmgr.getHolds(child.getPath());

            if (holds.length > 0) {
                rmgr.removeHold(child.getPath(), holds[0]);
                otherS.save();
                fail("Removing a hold on a locked node must throw LockException.");
            }
        } catch (LockException e) {
            // success
        } finally {
            otherS.logout();

            // clear hold added before
            try {
                retentionMgr.removeHold(child.getPath(), h);
                superuser.save();
            } catch (RepositoryException e) {
                // should not get here if test is correctly executed.
            }
        }
    }

    private Node getLockedChildNode() throws NotExecutableException, RepositoryException {
        checkSupportedOption(Repository.OPTION_LOCKING_SUPPORTED);
        Node child = testRootNode.addNode(nodeName2, testNodeType);
        ensureMixinType(child, mixLockable);
        testRootNode.getSession().save();
        child.lock(false, true); // session-scoped lock clean upon superuser-logout.
        return child;
    }

    public void testAddHoldOnCheckedInNode() throws NotExecutableException, RepositoryException {
        Node child = getVersionableChildNode();
        child.checkout();
        child.checkin();

        // get another session.
        javax.jcr.Session otherS = getHelper().getSuperuserSession();
        try {
            RetentionManager rmgr = getRetentionManager(otherS);
            rmgr.addHold(child.getPath(), getHoldName(), false);
            otherS.save();

            fail("Adding hold on a checked-in node must throw VersionException.");
        } catch (VersionException e) {
            // success
        } finally {
            otherS.logout();

            // clear holds (in case of test failure)
            child.checkout();
            Hold[] holds = retentionMgr.getHolds(child.getPath());
            for (int i = 0; i < holds.length; i++) {
                retentionMgr.removeHold(child.getPath(), holds[i]);
            }
            superuser.save();
        }
    }

    public void testRemoveHoldOnCheckedInNode() throws NotExecutableException, RepositoryException {
        Node vn = getVersionableChildNode();
        vn.checkout();
        Node n = vn.addNode(nodeName2);
        Hold h = retentionMgr.addHold(n.getPath(), getHoldName(), false);
        superuser.save();

        // checkin on the parent node make the hold-containing node checked-in.
        vn.checkin();

        javax.jcr.Session otherS = getHelper().getSuperuserSession();
        try {
            RetentionManager rmgr = getRetentionManager(otherS);
            Hold[] holds = rmgr.getHolds(n.getPath());

            if (holds.length > 0) {
                rmgr.removeHold(n.getPath(), holds[0]);
                otherS.save();
                fail("Removing a hold on a checked-in node must throw VersionException.");
            }
        } catch (VersionException e) {
            // success
        } finally {
            otherS.logout();

            // clear hold added before
            vn.checkout();
            try {
                retentionMgr.removeHold(n.getPath(), h);
                superuser.save();
            } catch (RepositoryException e) {
                // should not get here if test is correctly executed.
            }
        }
    }

    private Node getVersionableChildNode() throws NotExecutableException, RepositoryException {
        checkSupportedOption(Repository.OPTION_VERSIONING_SUPPORTED);
        Node child = testRootNode.addNode(nodeName2, testNodeType);
        ensureMixinType(child, mixVersionable);
        testRootNode.getSession().save();
        return child;
    }

    public void testHoldGetName() throws RepositoryException, NotExecutableException {
        String holdName = getHoldName();
        Hold h = retentionMgr.addHold(testNodePath, getHoldName(), false);
        assertEquals("Hold.getName() must return the specified name.",holdName, h.getName());
    }

    public void testHoldGetName2() throws RepositoryException, NotExecutableException {
        String holdName = getHoldName();
        Hold h = retentionMgr.addHold(testNodePath, getHoldName(), true);
        assertEquals("Hold.getName() must return the specified name.",holdName, h.getName());
    }

    public void testHoldIsDeep() throws RepositoryException, NotExecutableException {
        Hold h = retentionMgr.addHold(testNodePath, getHoldName(), false);
        assertEquals("Hold.isDeep() must reflect the specified flag.", false, h.isDeep());
    }

    public void testHoldIsDeep2() throws RepositoryException, NotExecutableException {
        Hold h = retentionMgr.addHold(testNodePath, getHoldName(), true);
        assertEquals("Hold.isDeep() must reflect the specified flag.", true, h.isDeep());
    }
}