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
package org.apache.jackrabbit.core.security;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

/**
 * <code>AccessManagerTest</code>...
 */
public class AccessManagerTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(AccessManagerTest.class);

    private static AccessManager getAccessManager(Session session) throws NotExecutableException {
        if (session instanceof SessionImpl) {
            return ((SessionImpl) session).getAccessManager();
        } else {
            throw new NotExecutableException();
        }
    }

    private static ItemId getItemId(Item item) throws NotExecutableException {
        if (item instanceof ItemImpl) {
            return ((ItemImpl)item).getId();
        } else {
            throw new NotExecutableException();
        }
    }

    // TODO: add tests for new methods
    // TODO: add specific tests for 'AC-read/modify' privileges

    public void testCheckPermissionReadOnlySession() throws RepositoryException, NotExecutableException {
        Session s = getHelper().getReadOnlySession();
        try {
            AccessManager acMgr = getAccessManager(s);

            NodeId id = (NodeId) getItemId(s.getItem(testRootNode.getPath()));

            acMgr.checkPermission(id, AccessManager.READ);
            try {
                acMgr.checkPermission(id, AccessManager.WRITE);
                fail();
            } catch (AccessDeniedException e) {
                // success
            }

            try {
                acMgr.checkPermission(id, AccessManager.WRITE | AccessManager.REMOVE);
                fail();
            } catch (AccessDeniedException e) {
                // success
            }
        } finally {
            s.logout();
        }
    }

    /**
     */
    public void testCheckPermissionWithNoPermissionFlag() throws RepositoryException, NotExecutableException {
        AccessManager acMgr = getAccessManager(superuser);

        NodeId id = (NodeId) getItemId(superuser.getItem(testRootNode.getPath()));
        // NOTE: backwards compatibility.
        // for deprecated method: invalid perm-flags will be ignored
        acMgr.checkPermission(id, AccessManager.READ - 1);
    }

    public void testCheckPermissionWithInvalidPermission() throws RepositoryException, NotExecutableException {
        AccessManager acMgr = getAccessManager(superuser);

        NodeId id = (NodeId) getItemId(superuser.getItem(testRootNode.getPath()));
        // NOTE: backwards compatibility.
        // for deprecated method: invalid perm-flags will be ignored
        acMgr.checkPermission(id, AccessManager.READ | AccessManager.WRITE | AccessManager.REMOVE  + 1);
    }

    public void testCheckPermissionWithUnknowId() throws RepositoryException, NotExecutableException {
        Session s = getHelper().getReadOnlySession();
        NodeId id = NodeId.randomId();
        try {
            AccessManager acMgr = getAccessManager(s);
            acMgr.checkPermission(id, AccessManager.READ);
            fail("AccessManager.checkPermission should throw ItemNotFoundException with a random (unknown) item id.");
        } catch (ItemNotFoundException e) {
            // ok
        } finally {
            s.logout();
        }
    }

    public void testIsGranted() throws RepositoryException, NotExecutableException {
        Session s = getHelper().getReadOnlySession();
        try {
            AccessManager acMgr = getAccessManager(s);

            NodeId id = (NodeId) getItemId(s.getItem(testRootNode.getPath()));
            assertTrue(acMgr.isGranted(id, AccessManager.READ));
            assertFalse(acMgr.isGranted(id, AccessManager.WRITE));
            assertFalse(acMgr.isGranted(id, AccessManager.WRITE | AccessManager.REMOVE));
        } finally {
            s.logout();
        }
    }

    public void testIsGrantedOnProperty() throws RepositoryException, NotExecutableException {
        Session s = getHelper().getReadOnlySession();
        try {
            AccessManager acMgr = getAccessManager(s);

            PropertyId id = (PropertyId) getItemId(testRootNode.getProperty(jcrPrimaryType));

            assertTrue(acMgr.isGranted(id, AccessManager.READ));
            assertFalse(acMgr.isGranted(id, AccessManager.WRITE));
            assertFalse(acMgr.isGranted(id, AccessManager.WRITE | AccessManager.REMOVE));
        } finally {
            s.logout();
        }
    }

    public void testIsGrantedOnNewNode() throws RepositoryException, NotExecutableException {
        Session s = getHelper().getReadWriteSession();
        try {
            AccessManager acMgr = getAccessManager(s);

            Node newNode = ((Node) s.getItem(testRoot)).addNode(nodeName2, testNodeType);
            NodeId id = (NodeId) getItemId(newNode);

            assertTrue(acMgr.isGranted(id, AccessManager.READ));
            assertTrue(acMgr.isGranted(id, AccessManager.WRITE));
            assertTrue(acMgr.isGranted(id, AccessManager.WRITE | AccessManager.REMOVE));
        } finally {
            s.logout();
        }
    }

    public void testCanAccess() throws RepositoryException, NotExecutableException {
        Session s = getHelper().getReadOnlySession();
        try {
            String wspName = s.getWorkspace().getName();

            assertTrue(getAccessManager(s).canAccess(wspName));
        } finally {
            s.logout();
        }
    }

    public void testCanAccessAllAvailable() throws RepositoryException, NotExecutableException {
        Session s = getHelper().getReadOnlySession();
        try {
            String[] wspNames = s.getWorkspace().getAccessibleWorkspaceNames();
            for (String wspName : wspNames) {
                assertTrue(getAccessManager(s).canAccess(wspName));
            }
        } finally {
            s.logout();
        }
    }

    public void testCanAccessDeniedWorkspace() throws RepositoryException, NotExecutableException {
        Session s = getHelper().getReadOnlySession();
        try {
            Set<String> allAccessibles = new HashSet<String>(Arrays.asList(superuser.getWorkspace().getAccessibleWorkspaceNames()));
            Set<String> sWorkspaceNames = new HashSet<String>(Arrays.asList(s.getWorkspace().getAccessibleWorkspaceNames()));

            if (!allAccessibles.removeAll(sWorkspaceNames) || allAccessibles.isEmpty()) {
                throw new NotExecutableException("No workspace name found that exists but is not accessible for ReadOnly session.");
            }

            String notAccessibleName = allAccessibles.iterator().next();
            assertFalse(getAccessManager(s).canAccess(notAccessibleName));
        } finally {
            s.logout();
        }
    }

    public void testCanAccessNotExistingWorkspace() throws RepositoryException, NotExecutableException {
        Session s = getHelper().getReadOnlySession();
        try {
            List<String> all = Arrays.asList(s.getWorkspace().getAccessibleWorkspaceNames());
            String testName = "anyWorkspace";
            int i = 0;
            while (all.contains(testName)) {
                testName = "anyWorkspace" + i;
                i++;
            }
            assertFalse(getAccessManager(s).canAccess(testName));
        } catch (NoSuchWorkspaceException e) {
            // fine as well.
        } finally {
            s.logout();
        }
    }

    public void testIsGrantedWithRelativePath() throws NotExecutableException {
        AccessManager acMgr = getAccessManager(superuser);
        Path p = PathFactoryImpl.getInstance().create(NameConstants.JCR_DATA);
        try {
            acMgr.isGranted(p, Permission.READ);
            fail("calling AccessManager.isGranted(Path, int) with relative path must fail.");
        } catch (RepositoryException e) {
            // success
        }

        try {
            acMgr.isGranted(p, NameConstants.JCR_CREATED, Permission.READ);
            fail("calling AccessManager.isGranted(Path, int) with relative path must fail.");
        } catch (RepositoryException e) {
            // success
        }
    }

    public void testIsGrantedPathToNonExistingItem() throws NotExecutableException, RepositoryException {
        AccessManager acMgr = getAccessManager(superuser);
        Path p = PathFactoryImpl.getInstance().getRootPath();

        // existing node-path
        assertTrue(acMgr.isGranted(p, Permission.ALL));
        // not existing property:
        assertTrue(acMgr.isGranted(p, NameConstants.JCR_CREATED, Permission.ALL));
        // existing property
        assertTrue(acMgr.isGranted(p, NameConstants.JCR_PRIMARYTYPE, Permission.ALL));
    }

    public void testIsGrantedReadOnlySession() throws NotExecutableException, RepositoryException {
        Session s = getHelper().getReadOnlySession();
        try {
            AccessManager acMgr = getAccessManager(s);
            Path p = PathFactoryImpl.getInstance().getRootPath();

            // existing node-path
            assertTrue(acMgr.isGranted(p, Permission.READ));
            // not existing property:
            assertTrue(acMgr.isGranted(p, NameConstants.JCR_CREATED, Permission.READ));

            // existing node-path
            assertFalse(acMgr.isGranted(p, Permission.ALL));
            // not existing property:
            assertFalse(acMgr.isGranted(p, NameConstants.JCR_CREATED, Permission.ALL));
        } finally {
            s.logout();
        }
    }

    public void testCanReadPathId() throws Exception {
        Session s = getHelper().getReadOnlySession();
        try {
            AccessManager acMgr = getAccessManager(s);

            ItemId id = ((NodeImpl) testRootNode).getId();
            Path path = ((NodeImpl) testRootNode).getPrimaryPath();
            assertTrue(acMgr.canRead(null, id));
            assertTrue(acMgr.canRead(path, null));
            assertTrue(acMgr.canRead(path, id));

            id = ((PropertyImpl) testRootNode.getProperty(jcrPrimaryType)).getId();
            path = ((PropertyImpl) testRootNode.getProperty(jcrPrimaryType)).getPrimaryPath();
            assertTrue(acMgr.canRead(null, id));
            assertTrue(acMgr.canRead(path, null));
            assertTrue(acMgr.canRead(path, id));

        } finally {
            s.logout();
        }

    }

    public void testCanReadNewId() throws Exception {
        Session s = getHelper().getReadOnlySession();
        try {
            NodeImpl n = (NodeImpl) testRootNode.addNode(nodeName1);
            PropertyImpl p = (PropertyImpl) n.setProperty(propertyName1, "somevalue");
            try {
                AccessManager acMgr = getAccessManager(s);
                acMgr.canRead(null, n.getId());
                fail("expected repositoryexception");
            } catch (RepositoryException e) {
                // success
            }
            try {
                AccessManager acMgr = getAccessManager(s);
                acMgr.canRead(null, p.getId());
                fail("expected repositoryexception");
            } catch (RepositoryException e) {
                // success
            }
        } finally {
            s.logout();
        }
    }
}