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
package org.apache.jackrabbit.core.persistence;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.TestHelper;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.persistence.check.ConsistencyReport;

import junit.framework.TestCase;

/**
 * Tests that a corrupt node is automatically fixed.
 */
public class AutoFixCorruptNode extends TestCase {

    private final String TEST_DIR = "target/temp/" + getClass().getSimpleName();

    public void setUp() throws Exception {
        FileUtils.deleteDirectory(new File(TEST_DIR));
    }

    public void tearDown() throws Exception {
        setUp();
    }

    /**
     * Unit test for <a
     * href="https://issues.apache.org/jira/browse/JCR-3069">JCR-3069</a>
     */
    public void testAutoFixWithConsistencyCheck() throws Exception {

        // new repository
        TransientRepository rep = new TransientRepository(new File(TEST_DIR));
        Session s = openSession(rep, false);
        Node root = s.getRootNode();

        // add nodes /test and /test/missing
        Node test = root.addNode("test");
        Node missing = test.addNode("missing");
        missing.addMixin("mix:referenceable");
        UUID id = UUID.fromString(missing.getIdentifier());
        s.save();
        s.logout();

        destroyBundle(id, "workspaces/default");

        s = openSession(rep, false);
        try {
            ConsistencyReport r = TestHelper.checkConsistency(s, false, null);
            assertNotNull(r);
            assertNotNull(r.getItems());
            assertEquals(1, r.getItems().size());
            assertEquals(test.getIdentifier(), r.getItems().iterator().next()
                    .getNodeId());
        } finally {
            s.logout();
            rep.shutdown();
            FileUtils.deleteDirectory(new File("repository"));
        }
    }

    public void testOrphan() throws Exception {

        // new repository
        TransientRepository rep = new TransientRepository(new File(TEST_DIR));
        Session s = openSession(rep, false);

        try {
            Node root = s.getRootNode();

            Node parent = root.addNode("parent");
            Node test = parent.addNode("test");
            test.addMixin("mix:referenceable");

            String lost = test.getIdentifier();

            Node lnf = root.addNode("lost+found");
            lnf.addMixin("mix:referenceable");
            String lnfid = lnf.getIdentifier();

            s.save();

            Node brokenNode = parent;
            UUID destroy = UUID.fromString(brokenNode.getIdentifier());
            s.logout();

            destroyBundle(destroy, "workspaces/default");

            s = openSession(rep, false);
            ConsistencyReport report = TestHelper.checkConsistency(s, false, null);
            assertTrue("Report should have reported broken nodes", !report.getItems().isEmpty());

            // now retry with lost+found functionality
            ConsistencyReport report2 = TestHelper.checkConsistency(s, true, lnfid);
            assertTrue("Report should have reported broken nodes", !report2.getItems().isEmpty());
            s.logout();

            s = openSession(rep, false);
            Node q = s.getNodeByIdentifier(lost);

            // check the node was moved
            assertEquals(lnfid, q.getParent().getIdentifier());
        } finally {
            s.logout();
        }
    }

    public void testMissingVHR() throws Exception {

        // new repository
        TransientRepository rep = new TransientRepository(new File(TEST_DIR));
        Session s = openSession(rep, false);

        String oldVersionRecoveryProp = System
                .getProperty("org.apache.jackrabbit.version.recovery");

        try {
            Node root = s.getRootNode();

            Node test = root.addNode("test");
            test.addMixin("mix:versionable");

            s.save();

            Node vhr = s.getWorkspace().getVersionManager()
                    .getVersionHistory(test.getPath());

            assertNotNull(vhr);

            Node brokenNode = vhr;
            String vhrRootVersionId = vhr.getNode("jcr:rootVersion").getIdentifier();
            UUID destroy = UUID.fromString(brokenNode.getIdentifier());
            s.logout();

            destroyBundle(destroy, "version");

            s = openSession(rep, false);

            ConsistencyReport report = TestHelper.checkVersionStoreConsistency(s, false, null);
            assertTrue("Report should have reported broken nodes", !report.getItems().isEmpty());

            try {
                test = s.getRootNode().getNode("test");
                vhr = s.getWorkspace().getVersionManager()
                        .getVersionHistory(test.getPath());
                fail("should not get here");
            } catch (Exception ex) {
                // expected
            }

            s.logout();

            System.setProperty("org.apache.jackrabbit.version.recovery", "true");

            s = openSession(rep, false);

            test = s.getRootNode().getNode("test");
            // versioning should be disabled now
            assertFalse(test.isNodeType("mix:versionable"));

            try {
                // try to enable versioning again
                test.addMixin("mix:versionable");
                s.save();

                fail("enabling versioning succeeded unexpectedly");
            }
            catch (Exception e) {
                // we expect this to fail
            }

            s.logout();

            // now redo after running fixup on versioning storage
            s = openSession(rep, false);

            report = TestHelper.checkVersionStoreConsistency(s, true, null);
            assertTrue("Report should have reported broken nodes", !report.getItems().isEmpty());
            int reportitems = report.getItems().size();

            // problems should now be fixed
            report = TestHelper.checkVersionStoreConsistency(s, false, null);
            assertTrue("Some problems should have been fixed but are not: " + report, report.getItems().size() < reportitems);

            // get a fresh session
            s.logout();
            s = openSession(rep, false);

            test = s.getRootNode().getNode("test");
            // versioning should be disabled now
            assertFalse(test.isNodeType("mix:versionable"));

            // try to enable versioning again
            test.addMixin("mix:versionable");
            s.save();

            Node oldRootVersion = s.getNodeByIdentifier(vhrRootVersionId);
            try {
                String path = oldRootVersion.getPath();
                fail("got path " + path + " for a node believed to be orphaned");
            }
            catch (ItemNotFoundException ex) {
                // orphaned
            }

            Node newRootVersion = s.getWorkspace().getVersionManager()
                    .getVersionHistory(test.getPath()).getRootVersion();
            assertFalse(
                    "new root version should be a different node than the one destroyed by the test case",
                    newRootVersion.getIdentifier().equals(vhrRootVersionId));
            assertNotNull("new root version should have a intact path",
                    newRootVersion.getPath());
        } finally {
            s.logout();
            System.setProperty("org.apache.jackrabbit.version.recovery",
                    oldVersionRecoveryProp == null ? ""
                            : oldVersionRecoveryProp);
        }
    }

    public void testMissingRootVersion() throws Exception {

        // new repository
        TransientRepository rep = new TransientRepository(new File(TEST_DIR));
        Session s = openSession(rep, false);

        String oldVersionRecoveryProp = System
                .getProperty("org.apache.jackrabbit.version.recovery");

        try {
            Node root = s.getRootNode();

            // add nodes /test and /test/missing
            Node test = root.addNode("test", "nt:file");
            test.addNode("jcr:content", "nt:unstructured");
            test.addMixin("mix:versionable");
            s.save();

            s.getWorkspace().getVersionManager().checkout(test.getPath());
            s.getWorkspace().getVersionManager().checkin(test.getPath());
            
            Node vhr = s.getWorkspace().getVersionManager()
                    .getVersionHistory(test.getPath());

            assertNotNull(vhr);

            Node brokenNode = vhr.getNode("jcr:rootVersion");
            String vhrId = vhr.getIdentifier();

            UUID destroy = UUID.fromString(brokenNode.getIdentifier());
            s.logout();

            destroyBundle(destroy, "version");

            s = openSession(rep, false);

            ConsistencyReport report = TestHelper.checkVersionStoreConsistency(s, false, null);
            assertTrue("Report should have reported broken nodes", !report.getItems().isEmpty());

            try {
                test = s.getRootNode().getNode("test");
                vhr = s.getWorkspace().getVersionManager()
                        .getVersionHistory(test.getPath());
                fail("should not get here");
            } catch (Exception ex) {
                // expected
            }

            s.logout();

            System.setProperty("org.apache.jackrabbit.version.recovery", "true");

            s = openSession(rep, false);

            test = s.getRootNode().getNode("test");
            // versioning should be disabled now
            assertFalse(test.isNodeType("mix:versionable"));

            try {
                // try to enable versioning again
                test.addMixin("mix:versionable");
                s.save();

                fail("enabling versioning succeeded unexpectedly");
            }
            catch (Exception e) {
                // we expect this to fail
            }

            s.logout();

            // now redo after running fixup on versioning storage
            s = openSession(rep, false);

            report = TestHelper.checkVersionStoreConsistency(s, true, null);
            assertTrue("Report should have reported broken nodes", !report.getItems().isEmpty());
            int reportitems = report.getItems().size();

            // problems should now be fixed
            report = TestHelper.checkVersionStoreConsistency(s, false, null);
            assertTrue("Some problems should have been fixed but are not: " + report, report.getItems().size() < reportitems);

            test = s.getRootNode().getNode("test");
            // versioning should be disabled now
            assertFalse(test.isNodeType("mix:versionable"));
            // jcr:uuid property should still be present
            assertTrue(test.hasProperty("jcr:uuid"));
            // ...and have a proper definition
            assertNotNull(test.getProperty("jcr:uuid").getDefinition().getName());
            // try to enable versioning again
            test.addMixin("mix:versionable");
            s.save();

            Node oldVHR = s.getNodeByIdentifier(vhrId);
            Node newVHR = s.getWorkspace().getVersionManager().getVersionHistory(test.getPath());

            assertTrue("old and new version history path should be different: "
                    + oldVHR.getPath() + " vs " + newVHR.getPath(), !oldVHR
                    .getPath().equals(newVHR.getPath()));

            // name should be same plus suffix
            assertTrue(oldVHR.getName().startsWith(newVHR.getName()));

            // try a checkout / checkin
            s.getWorkspace().getVersionManager().checkout(test.getPath());
            s.getWorkspace().getVersionManager().checkin(test.getPath());

            validateDisconnectedVHR(oldVHR);            
        } finally {
            s.logout();
            System.setProperty("org.apache.jackrabbit.version.recovery",
                    oldVersionRecoveryProp == null ? ""
                            : oldVersionRecoveryProp);
        }
    }

    // similar to above, but disconnects version history before damaging the repository
    public void testMissingRootVersion2() throws Exception {

        // new repository
        TransientRepository rep = new TransientRepository(new File(TEST_DIR));
        Session s = openSession(rep, false);

        String oldVersionRecoveryProp = System
                .getProperty("org.apache.jackrabbit.version.recovery");

        try {
            Node root = s.getRootNode();

            // add nodes /test and /test/missing
            Node test = root.addNode("test");
            test.addMixin("mix:versionable");

            s.save();

            Node vhr = s.getWorkspace().getVersionManager()
                    .getVersionHistory(test.getPath());

            assertNotNull(vhr);

            Node brokenNode = vhr.getNode("jcr:rootVersion");
            String vhrId = vhr.getIdentifier();

            UUID destroy = UUID.fromString(brokenNode.getIdentifier());

            // disable versioning
            test.removeMixin("mix:versionable");
            s.save();

            s.logout();


            destroyBundle(destroy, "version");

            s = openSession(rep, false);

            ConsistencyReport report = TestHelper.checkVersionStoreConsistency(s, false, null);
            assertTrue("Report should have reported broken nodes", !report.getItems().isEmpty());

            s.logout();

            System.setProperty("org.apache.jackrabbit.version.recovery", "true");

            s = openSession(rep, false);
            s.logout();

            s = openSession(rep, false);

            test = s.getRootNode().getNode("test");
            // versioning should still be disabled
            assertFalse(test.isNodeType("mix:versionable"));

            // try to enable versioning again
            test.addMixin("mix:versionable");
            s.save();

            Node oldVHR = s.getNodeByIdentifier(vhrId);
            Node newVHR = s.getWorkspace().getVersionManager().getVersionHistory(test.getPath());

            assertTrue("old and new version history path should be different: "
                    + oldVHR.getPath() + " vs " + newVHR.getPath(), !oldVHR
                    .getPath().equals(newVHR.getPath()));

            // name should be same plus suffix
            assertTrue(oldVHR.getName().startsWith(newVHR.getName()));

            // try a checkout / checkin
            s.getWorkspace().getVersionManager().checkout(test.getPath());
            s.getWorkspace().getVersionManager().checkin(test.getPath());

            validateDisconnectedVHR(oldVHR);            
        } finally {
            s.logout();
            System.setProperty("org.apache.jackrabbit.version.recovery",
                    oldVersionRecoveryProp == null ? ""
                            : oldVersionRecoveryProp);
        }
    }

    // tests recovery from a broken hierarchy in the version store
    public void testBrokenVhrParent() throws Exception {

        // new repository
        TransientRepository rep = new TransientRepository(new File(TEST_DIR));
        Session s = openSession(rep, false);

        try {
            Node root = s.getRootNode();

            // add node /test
            Node test = root.addNode("test");
            test.addMixin("mix:versionable");

            s.save();

            Node vhr = s.getWorkspace().getVersionManager().getVersionHistory(test.getPath());

            assertNotNull(vhr);

            Node brokenNode = vhr.getParent().getParent();

            UUID destroy = UUID.fromString(brokenNode.getIdentifier());

            // disable versioning
            test.removeMixin("mix:versionable");
            s.save();

            s.logout();

            destroyBundle(destroy, "version");

            s = openSession(rep, false);

            ConsistencyReport report = TestHelper.checkVersionStoreConsistency(s, true, null);
            assertTrue("Report should have reported broken nodes", !report.getItems().isEmpty());

            s.logout();

            s = openSession(rep, false);

            test = s.getRootNode().getNode("test");
            // versioning should still be disabled
            assertFalse(test.isNodeType("mix:versionable"));

            // try to enable versioning again
            test.addMixin("mix:versionable");
            s.save();

            // try a checkout / checkin
            s.getWorkspace().getVersionManager().checkout(test.getPath());
            s.getWorkspace().getVersionManager().checkin(test.getPath());
        } finally {
            s.logout();
        }
    }

    public void testAutoFix() throws Exception {

        // new repository
        TransientRepository rep = new TransientRepository(new File(TEST_DIR));
        Session s = openSession(rep, false);
        Node root = s.getRootNode();

        // add nodes /test and /test/missing
        Node test = root.addNode("test");
        Node missing = test.addNode("missing");
        missing.addMixin("mix:referenceable");
        UUID id = UUID.fromString(missing.getIdentifier());
        s.save();
        s.logout();

        destroyBundle(id, "workspaces/default");

        // login and try the operation
        s = openSession(rep, false);
        test = s.getRootNode().getNode("test");

        // try to add a node with the same name
        try {
            test.addNode("missing");
            s.save();
        } catch (RepositoryException e) {
            // expected
        }

        s.logout();

        s = openSession(rep, true);
        test = s.getRootNode().getNode("test");
        // iterate over all child nodes fixes the corruption
        NodeIterator it = test.getNodes();
        while (it.hasNext()) {
            it.nextNode();
        }

        // try to add a node with the same name
        test.addNode("missing");
        s.save();

        // try to delete the parent node
        test.remove();
        s.save();

        s.logout();
        rep.shutdown();

        FileUtils.deleteDirectory(new File("repository"));
    }

    private void destroyBundle(UUID id, String where) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:derby:" + TEST_DIR
                + "/" + where + "/db");
        String table = where.equals("version") ? "VERSION_BUNDLE" : "DEFAULT_BUNDLE";
        PreparedStatement prep = conn.prepareStatement("delete from " + table
                + " where NODE_ID_HI=? and NODE_ID_LO=?");
        prep.setLong(1, id.getMostSignificantBits());
        prep.setLong(2, id.getLeastSignificantBits());
        prep.executeUpdate();
        conn.close();
    }

    private Session openSession(Repository rep, boolean autoFix)
            throws RepositoryException {
        SimpleCredentials cred = new SimpleCredentials("admin",
                "admin".toCharArray());
        if (autoFix) {
            cred.setAttribute("org.apache.jackrabbit.autoFixCorruptions",
                    "true");
        }
        return rep.login(cred);
    }
    
    // JCR-4118: check that the old VHR can be retrieved
    private void validateDisconnectedVHR(Node oldVHR) throws RepositoryException {
        Session s = oldVHR.getSession();
        Node old = s.getNode(oldVHR.getPath());
        assertNotNull("disconnected VHR should be accessible", old);

        assertEquals("nt:versionHistory", old.getPrimaryNodeType().getName());
        NodeIterator ni = old.getNodes();
        while (ni.hasNext()) {
            Node n = ni.nextNode();
            String type = n.getPrimaryNodeType().getName();
            assertTrue("node type of VHR child nodes should be nt:version or nt:versionLabels",
                    "nt:version".equals(type) || "nt:versionLabels".equals(type));
        }

        try {
            old.remove();
            s.save();
            fail("removal of node using remove() should throw because it's in the versioning workspace");
        } catch (ConstraintViolationException expected) {
        }
    }
}
