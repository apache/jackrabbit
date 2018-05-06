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

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.retention.RetentionManager;
import javax.jcr.retention.RetentionPolicy;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>RetentionPolicyTest</code>...
 */
public class RetentionPolicyTest extends AbstractRetentionTest {

    protected void setUp() throws Exception {
        super.setUp();

        // make sure there is no retention policy defined at testNodePath.
        RetentionPolicy p = retentionMgr.getRetentionPolicy(testNodePath);
        if (p != null) {
            retentionMgr.removeRetentionPolicy(testNodePath);
            superuser.save();
        }
    }
    
    public void testGetRetentionPolicy() throws RepositoryException, NotExecutableException {
        retentionMgr.setRetentionPolicy(testNodePath, getApplicableRetentionPolicy());
        
        RetentionPolicy policy = retentionMgr.getRetentionPolicy(testNodePath);
        assertNotNull("RetentionManager.getRetentionPolicy must return the policy set before.", policy);
    }

    public void testGetRetentionPolicyOnChild() throws RepositoryException, NotExecutableException {
        String childPath = testRootNode.addNode(nodeName2, testNodeType).getPath();
        retentionMgr.setRetentionPolicy(testNodePath, getApplicableRetentionPolicy());
        
        assertNull("RetentionManager.getRetentionPolicy called on child must not return the policy set before.",
                retentionMgr.getRetentionPolicy(childPath));
    }

    public void testRetentionPolicyGetName() throws RepositoryException, NotExecutableException {
        RetentionPolicy p = getApplicableRetentionPolicy();
        retentionMgr.setRetentionPolicy(testNodePath, p);

        RetentionPolicy policy = retentionMgr.getRetentionPolicy(testNodePath);
        assertEquals("RetentionPolicy.getName() must match the name of the policy set before.", p.getName(), policy.getName());
    }

    public void testSetRetentionPolicyIsTransient() throws RepositoryException, NotExecutableException {
        retentionMgr.setRetentionPolicy(testNodePath, getApplicableRetentionPolicy());
        superuser.refresh(false);

        assertNull("Reverting transient changes must remove the pending retention policy.",
                retentionMgr.getRetentionPolicy(testNodePath));
    }

    public void testRemovePendingRetentionPolicy() throws RepositoryException, NotExecutableException {
        retentionMgr.setRetentionPolicy(testNodePath, getApplicableRetentionPolicy());
        retentionMgr.removeRetentionPolicy(testNodePath);

        assertNull("Removing pending retention policy must succeed.", retentionMgr.getRetentionPolicy(testNodePath));
    }

    public void testRemoveRetentionPolicy() throws RepositoryException, NotExecutableException {
        retentionMgr.setRetentionPolicy(testNodePath, getApplicableRetentionPolicy());
        superuser.save();

        retentionMgr.removeRetentionPolicy(testNodePath);
        assertNull("Removing persisted retention policy must succeed.", retentionMgr.getRetentionPolicy(testNodePath));
        superuser.save();
        assertNull("Removing persisted retention policy must succeed.", retentionMgr.getRetentionPolicy(testNodePath));
    }

    public void testRemoveRetentionPolicyIsTransient() throws RepositoryException, NotExecutableException {
        retentionMgr.setRetentionPolicy(testNodePath, getApplicableRetentionPolicy());
        superuser.save();

        try {
            retentionMgr.removeRetentionPolicy(testNodePath);
            superuser.refresh(false);
            assertNotNull("Reverting transient removal must re-add the retention policy.",
                    retentionMgr.getRetentionPolicy(testNodePath));
        } finally {
            retentionMgr.removeRetentionPolicy(testNodePath);
            superuser.save();
        }
    }

    public void testRemoveRetentionPolicyFromChild() throws RepositoryException, NotExecutableException {
        String childPath = testRootNode.addNode(nodeName2, testNodeType).getPath();
        retentionMgr.setRetentionPolicy(testNodePath, getApplicableRetentionPolicy());

        try {
            retentionMgr.removeRetentionPolicy(childPath);
            fail("Removing retention policy from another node must fail");
        } catch (RepositoryException e) {
            // success
            assertNull(retentionMgr.getRetentionPolicy(childPath));
        }

        // check again with persisted policy
        superuser.save();
        try {
            retentionMgr.removeRetentionPolicy(childPath);
            fail("Removing retention policy from another node must fail");
        } catch (RepositoryException e) {
            // success
            assertNull(retentionMgr.getRetentionPolicy(childPath));
        } finally {
            // rm the policy that was permanently added before.
            retentionMgr.removeRetentionPolicy(testNodePath);
            superuser.save();
        }
    }

    public void testInvalidPath() throws RepositoryException, NotExecutableException {
        String invalidPath = testPath; // not an absolute path
        try {
            retentionMgr.getRetentionPolicy(invalidPath);
            fail("Accessing retention policy for an invalid path must throw RepositoryException.");
        } catch (RepositoryException e) {
            // success
        }
        try {
            retentionMgr.setRetentionPolicy(invalidPath, getApplicableRetentionPolicy());
            fail("Setting retention policy with an invalid path must throw RepositoryException.");
        } catch (RepositoryException e) {
            // success
        }
        try {
            retentionMgr.removeRetentionPolicy(invalidPath);
            fail("Removing retention policy with an invalid path must throw RepositoryException.");
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
            retentionMgr.getRetentionPolicy(invalidPath);
            fail("Accessing retention policy from non-existing node must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // success
        }
        try {
            retentionMgr.setRetentionPolicy(invalidPath, getApplicableRetentionPolicy());
            fail("Setting retention policy for a non-existing node must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // success
        }
        try {
            retentionMgr.removeRetentionPolicy(invalidPath);
            fail("Removing retention policy at a non-existing node must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // success
        }
    }

    public void testPropertyPath() throws RepositoryException, NotExecutableException {
        String propPath = null;
        for (PropertyIterator it = testRootNode.getProperties(); it.hasNext();) {
            String path = it.nextProperty().getPath();
            if (!superuser.nodeExists(path)) {
                propPath = path;
                break;
            }
        }
        if (propPath == null) {
            throw new NotExecutableException();
        }
        try {
            retentionMgr.getRetentionPolicy(propPath);
            fail("Accessing retention policy from property must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // success
        }
        try {
            retentionMgr.setRetentionPolicy(propPath, getApplicableRetentionPolicy());
            fail("Setting retention policy for property must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // success
        }
        try {
            retentionMgr.removeRetentionPolicy(propPath);
            fail("Removing retention policy at property path must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // success
        }
    }

    public void testInvalidName() {
        try {
            RetentionPolicy rp = new RetentionPolicy() {
                public String getName() throws RepositoryException {
                    return "*.[y]";
                }
            };
            retentionMgr.setRetentionPolicy(testNodePath, rp);
            fail("Setting a policy with an invalid JCR name must fail.");
        } catch (RepositoryException e) {
            // success
        } catch (IllegalArgumentException e) {
            // fine as well.
        }
    }

    public void testReadOnlySession() throws NotExecutableException, RepositoryException {
        Session s = getHelper().getReadOnlySession();
        try {
            RetentionManager rmgr = getRetentionManager(s);
            try {
                rmgr.getRetentionPolicy(testNodePath);
                fail("Read-only session doesn't have sufficient privileges to retrieve retention policy.");
            } catch (AccessDeniedException e) {
                // success
            }
            try {
                rmgr.setRetentionPolicy(testNodePath, getApplicableRetentionPolicy());
                fail("Read-only session doesn't have sufficient privileges to retrieve retention policy.");
            } catch (AccessDeniedException e) {
                // success
            }
        } finally {
            s.logout();
        }
    }

    public void testSetRetentionPolicyOnLockedNode() throws NotExecutableException, RepositoryException {
        String childPath = getLockedChildNode().getPath();

        // get another session.
        Session otherS = getHelper().getSuperuserSession();
        try {
            RetentionManager rmgr = getRetentionManager(otherS);
            rmgr.setRetentionPolicy(childPath, getApplicableRetentionPolicy());
            otherS.save();

            fail("Setting a retention policy on a locked node must throw LockException.");
        } catch (LockException e) {
            // success
        } finally {
            otherS.logout();

            if (retentionMgr.getRetentionPolicy(childPath) != null) {
                retentionMgr.removeRetentionPolicy(childPath);
            }
            superuser.save();
        }
    }

    public void testRemoveRetentionPolicyOnLockedNode() throws NotExecutableException, RepositoryException {
        String childPath = getLockedChildNode().getPath();
        retentionMgr.setRetentionPolicy(childPath, getApplicableRetentionPolicy());
        testRootNode.getSession().save();

        Session otherS = getHelper().getSuperuserSession();
        try {
            RetentionManager rmgr = getRetentionManager(otherS);
            rmgr.removeRetentionPolicy(childPath);
            fail("Removing a retention policy on a locked node must throw LockException.");
        } catch (LockException e) {
            // success
        } finally {
            otherS.logout();

            // clear  retention policy added before
            try {
                retentionMgr.removeRetentionPolicy(childPath);
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

    public void testSetRetentionPolicyOnCheckedInNode() throws NotExecutableException, RepositoryException {
        Node child = getVersionableChildNode();
        child.checkout();
        child.checkin();
        String childPath = child.getPath();

        // get another session.
        Session otherS = getHelper().getSuperuserSession();
        try {
            RetentionManager rmgr = getRetentionManager(otherS);
            rmgr.setRetentionPolicy(childPath, getApplicableRetentionPolicy());
            otherS.save();

            fail("Setting a retention policy on a checked-in node must throw VersionException.");
        } catch (VersionException e) {
            // success
        } finally {
            otherS.logout();

            // clear policies (in case of test failure)
            try {
                retentionMgr.removeRetentionPolicy(childPath);
                superuser.save();
            } catch (RepositoryException e) {
                // ignore.
            }
        }
    }

    public void testRemoveRetentionPolicyOnCheckedInNode() throws NotExecutableException, RepositoryException {
        Node child = getVersionableChildNode();
        child.checkout();
        retentionMgr.setRetentionPolicy(child.getPath(), getApplicableRetentionPolicy());
        superuser.save();
        child.checkin();

        Session otherS = getHelper().getSuperuserSession();
        try {
            RetentionManager rmgr = getRetentionManager(otherS);
            rmgr.removeRetentionPolicy(child.getPath());
            otherS.save();
            fail("Removing a retention policy on a checked-in node must throw VersionException.");
        } catch (VersionException e) {
            // success
        } finally {
            otherS.logout();

            // clear policy added before
            child.checkout();
            try {
                retentionMgr.removeRetentionPolicy(child.getPath());
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


    public void testSetRetentionPolicyBelow() throws RepositoryException, NotExecutableException {
        Node childN = testRootNode.addNode(nodeName2);
        superuser.save();

        try {
            retentionMgr.setRetentionPolicy(testNodePath, getApplicableRetentionPolicy());
            retentionMgr.setRetentionPolicy(childN.getPath(), getApplicableRetentionPolicy());
            superuser.save();
        } finally {
            superuser.refresh(false);
            if (retentionMgr.getRetentionPolicy(testNodePath) != null) {
                retentionMgr.removeRetentionPolicy(testNodePath);
            }
            if (retentionMgr.getRetentionPolicy(childN.getPath()) != null) {
                retentionMgr.removeRetentionPolicy(childN.getPath());
            }
            superuser.save();
        }
    }

    public void testOtherSessionSetsRetentionPolicyBelow() throws RepositoryException, NotExecutableException {
        Node childN = testRootNode.addNode(nodeName2);
        superuser.save();

        Session otherS = getHelper().getSuperuserSession();
        try {
            retentionMgr.setRetentionPolicy(testNodePath, getApplicableRetentionPolicy());
            superuser.save();

            getRetentionManager(otherS).setRetentionPolicy(childN.getPath(), getApplicableRetentionPolicy());
            otherS.save();
        } finally {
            // logout the other session
            otherS.logout();

            // remove the retention policies again.
            superuser.refresh(false);
            if (retentionMgr.getRetentionPolicy(testNodePath) != null) {
                retentionMgr.removeRetentionPolicy(testNodePath);
            }
            if (retentionMgr.getRetentionPolicy(childN.getPath()) != null) {
                retentionMgr.removeRetentionPolicy(childN.getPath());
            }
            superuser.save();
        }
    }

}