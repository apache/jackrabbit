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
package org.apache.jackrabbit.core.security.authorization;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.Privilege;
import javax.jcr.version.Version;

/** <code>AbstractVersionAccessTest</code>... */
public abstract class AbstractVersionManagementTest extends AbstractEvaluationTest {

    private static Logger log = LoggerFactory.getLogger(AbstractVersionManagementTest.class);

    protected Node createVersionableNode(Node parent) throws RepositoryException, NotExecutableException {
        Node n = parent.addNode(nodeName1);
        if (n.canAddMixin(mixVersionable)) {
            n.addMixin(mixVersionable);
        } else {
            throw new NotExecutableException();
        }
        parent.save();
        return n;
    }

    public void testAddMixVersionable() throws RepositoryException, NotExecutableException {
        Node trn = getTestNode();
        modifyPrivileges(trn.getPath(), PrivilegeRegistry.REP_WRITE, true);
        modifyPrivileges(trn.getPath(), Privilege.JCR_VERSION_MANAGEMENT, false);
        Node n = trn.addNode(nodeName1);
        try {
            if (n.canAddMixin(mixVersionable)) {
                n.addMixin(mixVersionable);
            } else {
                throw new NotExecutableException();
            }
            trn.save();
            fail("Test session does not have write permission in the version storage -> adding mixin must fail.");
        } catch (AccessDeniedException e) {
            // success
            log.debug(e.getMessage());
            // ... but autocreated versionable node properties must not be present
            assertFalse(n.isNodeType(mixVersionable));
            assertFalse(n.hasProperty("jcr:isCheckedOut"));
            assertFalse(n.hasProperty(jcrVersionHistory));
        }
    }

    public void testAddMixVersionable2() throws RepositoryException, NotExecutableException {
        Node trn = getTestNode();
        modifyPrivileges(trn.getPath(), PrivilegeRegistry.REP_WRITE, true);
        modifyPrivileges(trn.getPath(), Privilege.JCR_NODE_TYPE_MANAGEMENT, true);
        modifyPrivileges(trn.getPath(), Privilege.JCR_VERSION_MANAGEMENT, true);

        Node n = createVersionableNode(trn);
        n.checkin();
        n.checkout();
    }

    public void testWriteVersionStore() throws RepositoryException, NotExecutableException {
        Node trn = getTestNode();
        modifyPrivileges(trn.getPath(), PrivilegeRegistry.REP_WRITE, true);
        modifyPrivileges(trn.getPath(), Privilege.JCR_VERSION_MANAGEMENT, false);

        Node n = createVersionableNode(testRootNode);
        try {
            Node n2 = trn.getNode(nodeName1);
            n2.checkin();
            fail("No write permission in the version storage.");
        } catch (AccessDeniedException e) {
            // success
            log.debug(e.getMessage());
            // ... but the property must not be modified nor indicating
            // checkedIn status
            Property p = n.getProperty("jcr:isCheckedOut");
            assertFalse(p.isModified());
            assertTrue(n.getProperty("jcr:isCheckedOut").getValue().getBoolean());
        }
    }

    public void testRemoveVersion() throws RepositoryException, NotExecutableException {
        Node trn = getTestNode();
        Node n = createVersionableNode(testRootNode);
        modifyPrivileges(trn.getPath(), Privilege.JCR_VERSION_MANAGEMENT, true);

        // test session should now be able to create versionable nodes, checkout
        // and checkin them, read the version/v-histories.

        Node testNode = trn.getNode(nodeName1);
        Version v = testNode.checkin();
        testNode.checkout();
        testNode.checkin();

        // remove ability to edit version information
        // -> VersionHistory.removeVersion must not be allowed.
        modifyPrivileges(trn.getPath(), Privilege.JCR_VERSION_MANAGEMENT, false);
        try {
            testNode.getVersionHistory().removeVersion(v.getName());
            fail("TestSession without remove privilege on the v-storage must not be able to remove a version.");
        } catch (AccessDeniedException e) {
            // success
            log.debug(e.getMessage());
        }
    }

    public void testRemoveVersion2() throws RepositoryException, NotExecutableException {
        Node trn = getTestNode();
        Node n = createVersionableNode(testRootNode);
        modifyPrivileges(trn.getPath(), Privilege.JCR_VERSION_MANAGEMENT, true);

        Node testNode = trn.getNode(nodeName1);
        Version v = testNode.checkin();
        testNode.checkout();
        testNode.checkin();

        // -> VersionHistory.removeVersion must not be allowed.
        try {
            testNode.getVersionHistory().removeVersion(v.getName());
            fail("TestSession without remove privilege on the v-storage must not be able to remove a version.");
        } catch (AccessDeniedException e) {
            // success
            log.debug(e.getMessage());
        }        
    }

    public void testRemoveVersion3() throws RepositoryException, NotExecutableException {
        Node trn = getTestNode();
        Node n = createVersionableNode(testRootNode);

        String path = getTestSession().getRootNode().getPath();        
        JackrabbitAccessControlList tmpl = getPolicy(acMgr, path, testUser.getPrincipal());
        AccessControlEntry entry;
        try {
            // NOTE: don't use 'modifyPrivileges' in order not to have the
            // root-policy cleared on tear-down.
            tmpl.addEntry(testUser.getPrincipal(), privilegesFromName(Privilege.JCR_VERSION_MANAGEMENT), true, getRestrictions(superuser, path));
            acMgr.setPolicy(tmpl.getPath(), tmpl);
            superuser.save();

            Node testNode = trn.getNode(nodeName1);
            Version v = testNode.checkin();
            testNode.checkout();
            testNode.checkin();

            // -> VersionHistory.removeVersion must be allowed            
            testNode.getVersionHistory().removeVersion(v.getName());
        } finally {
            // revert privilege modification (manually remove the ACE added)
            AccessControlEntry[] entries = tmpl.getAccessControlEntries();
            for (AccessControlEntry entry1 : entries) {
                if (entry1.getPrincipal().equals(testUser.getPrincipal())) {
                    tmpl.removeAccessControlEntry(entry1);
                }
            }
            acMgr.setPolicy(tmpl.getPath(), tmpl);
            superuser.save();
        }
    }
}