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
package org.apache.jackrabbit.test.api.version;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.Version;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Property;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.Value;
import javax.jcr.PropertyType;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import java.util.HashSet;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.io.InputStream;
import java.io.ByteArrayInputStream;


/**
 * <code>VersionHistoryTest</code> provides test methods related to version
 * history methods and general version history issues.
 *
 * @test
 * @sources VersionHistoryTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.VersionHistoryTest
 * @keywords versioning
 */
public class VersionHistoryTest extends AbstractVersionTest {

    protected VersionHistory vHistory;
    private Version version;

    /**
     * helper class used in testAccept()
     */
    private class ItemVisitorTest implements ItemVisitor {

        Node ivtNode;

        public ItemVisitorTest(VersionHistory v) {
            ivtNode = v;
        }

        public void visit(Node node) throws RepositoryException {
            assertTrue("VersionHistory.accept(ItemVisitor) does not provide the right node to the ItemVisitor", ivtNode.isSame(node));
        }

        public void visit(Property property) throws RepositoryException {
        }
    }

    protected void setUp() throws Exception {
        super.setUp();

        version = versionableNode.checkin();

        vHistory = versionableNode.getVersionHistory();


        if (vHistory == null) {
            fail("VersionHistory must be created on persistent creation of a versionable node.");
        }
    }

    protected void tearDown() throws Exception {
        vHistory = null;
        version = null;
        super.tearDown();
    }

    /**
     * Test if initially there is an auto-created root version present in the
     * version history.
     */
    public void testAutocreatedRootVersion() throws RepositoryException {
        Version rootVersion = vHistory.getRootVersion();
        if (rootVersion == null) {
            fail("The version history must contain an autocreated root version");
        }
    }

    /**
     * The version history must initially contain two versions (root version +
     * first test version).
     *
     * @throws RepositoryException
     */
    public void testInitialNumberOfVersions() throws RepositoryException {
        long initialSize = getNumberOfVersions(vHistory);
        assertEquals("VersionHistory.getAllVersions() initially returns an iterator with two versions.", 2, initialSize);
    }

    /**
     * Test if the iterator returned by {@link javax.jcr.version.VersionHistory#getAllVersions()}
     * contains the root version upon creation of the version history.
     *
     * @see javax.jcr.version.VersionHistory#getRootVersion()
     */
    public void testInitiallyGetAllVersionsContainsTheRootVersion() throws RepositoryException {
        Version rootVersion = vHistory.getRootVersion();
        boolean isContained = false;
        for (VersionIterator it = vHistory.getAllVersions(); it.hasNext(); ) {
            isContained |= it.nextVersion().isSame(rootVersion);
        }
        assertTrue("root version must be part of the version history", isContained);
    }

    /**
     * Test that {@link VersionHistory#getAllVersions()} returns an iterator
     * containing the root version and all versions that have been created by
     * Node.checkin().
     *
     * @see javax.jcr.version.VersionHistory#getAllVersions()
     */
    public void testGetAllVersions() throws RepositoryException {
        int cnt = 5;
        HashSet versions = new HashSet();
        versions.add(vHistory.getRootVersion());
        for (int i = 0; i < cnt; i++) {
            versions.add(versionableNode.checkin());
            versionableNode.checkout();
        }

        VersionIterator it = vHistory.getAllVersions();
        while (it.hasNext()) {
            Version v = it.nextVersion();
            if (!versions.contains(v)) {
                fail("VersionHistory.getAllVersions() must only contain the root version and versions, that have been created by a Node.checkin() call.");
            }
            versions.remove(v);
        }
        assertTrue("VersionHistory.getAllVersions() must contain the root version and all versions that have been created with a Node.checkin() call.", versions.isEmpty());
    }

    /**
     * Test if UnsupportedRepositoryOperationException is thrown when calling
     * Node.getVersionHistory() on a non-versionable node.
     */
    public void testGetVersionHistoryOnNonVersionableNode() throws RepositoryException {
        try {
            nonVersionableNode.getVersionHistory();
            fail("Node.getVersionHistory() must throw UnsupportedRepositoryOperationException if the node is not versionable.");
        } catch (UnsupportedRepositoryOperationException e) {
            //success
        }
    }

    /**
     * Test VersionHistory.getVersion(String versionName) if 'versionName' is
     * the name of an existing version (created by Node.checkin()).
     *
     * @see VersionHistory#getVersion(String)
     */
    public void testGetVersion() throws RepositoryException {

        Version v = versionableNode.checkin();
        Version v2 = vHistory.getVersion(v.getName());

        assertTrue("VersionHistory.getVersion(String versionName) must return the version that is identified by the versionName specified, if versionName is the name of a version created by Node.checkin().", v.isSame(v2));
    }

    /**
     * Tests if <code>VersionHistory.accept(ItemVisitor)</code> accepts a
     * ItemVisitor and if the right Node is provided to that visitor.
     */
    public void testAccept() throws Exception {
        ItemVisitorTest ivt = new ItemVisitorTest(vHistory);
        vHistory.accept(ivt);
    }

    /**
     * Tests if <code>VersionHistory.addMixin(String)</code> throws a {@link
     * javax.jcr.nodetype.ConstraintViolationException}
     */
    public void testAddMixin() throws Exception {
        try {
            vHistory.addMixin(mixVersionable);
            vHistory.save();
            fail("VersionHistory should be read-only: VersionHistory.addMixin(String) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.addNode(String)</code> and
     * <code>VersionHistory.addNode(String, String)</code> throw a {@link
     * javax.jcr.nodetype.ConstraintViolationException}
     */
    public void testAddNode() throws Exception {
        try {
            vHistory.addNode(nodeName4);
            vHistory.save();
            fail("VersionHistory should be read-only: VersionHistory.addNode(String) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            vHistory.addNode(nodeName4, ntBase);
            vHistory.save();
            fail("VersionHistory should be read-only: VersionHistory.addNode(String,String) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.canAddMixin(String)</code> returns
     * <code>false</code>
     */
    public void testCanAddMixin() throws Exception {
        assertFalse("VersionHistory should be read-only: VersionHistory.canAddMixin(String) returned true", vHistory.canAddMixin(mixVersionable));
    }

    /**
     * Tests if <code>VersionHistory.cancelMerge(Version)</code> throws an
     * {@link javax.jcr.UnsupportedRepositoryOperationException}
     */
    public void testCancelMerge() throws Exception {
        try {
            vHistory.cancelMerge(version);
            fail("VersionHistory.cancelMerge(Version) did not throw an UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.checkin()</code> throws an {@link
     * javax.jcr.UnsupportedRepositoryOperationException}
     */
    public void testCheckin() throws Exception {
        try {
            vHistory.checkin();
            fail("VersionHistory.checkin() did not throw an UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.checkout()</code> throws an {@link
     * javax.jcr.UnsupportedRepositoryOperationException}
     */
    public void testCheckout() throws Exception {
        try {
            vHistory.checkout();
            fail("VersionHistory.checkout() did not throw an UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.doneMerge(Version)</code> throws an {@link
     * javax.jcr.UnsupportedRepositoryOperationException}
     */
    public void testDoneMerge() throws Exception {
        try {
            vHistory.doneMerge(version);
            fail("VersionHistory should not be versionable: VersionHistory.doneMerge(Version) did not throw an UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.getAncestor(int)</code> returns the right
     * ancestor
     */
    public void testGetAncestor() throws Exception {
        assertTrue("VersionHistory.getAncestor(int) does not work", superuser.getRootNode().isSame(vHistory.getAncestor(0)));
    }

    /**
     * Tests if <code>VersionHistory.getBaseVersion()</code> throws an {@link
     * javax.jcr.UnsupportedRepositoryOperationException}
     */
    public void testGetBaseVersion() throws Exception {
        try {
            vHistory.getBaseVersion();
            fail("VersionHistory.getBaseVersion() did not throw an UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.getCorrespondingNodePath(String)</code>
     * returns the right path
     */
    public void testGetCorrespondingNodePath() throws Exception {
        assertEquals("VersionHistory.getCorrespondingNodePath(String) did not return the right path", vHistory.getPath(), vHistory.getCorrespondingNodePath(workspaceName));
    }

    /**
     * Tests if <code>VersionHistory.getDepth()</code> returns the right depth
     */
    public void testGetDepth() throws Exception {
        assertTrue("VersionHistory.getDepth() mismatch", vHistory.getDepth() >= 3);
    }

    /**
     * Tests if <code>VersionHistory.getIndex()</code> returns the right index
     */
    public void testGetIndex() throws Exception {
        assertEquals("VersionHistory.getIndex() mismatch", 1, vHistory.getIndex());
    }

    /**
     * Tests if <code>VersionHistory.getLock()</code> throws an {@link
     * javax.jcr.lock.LockException}
     */
    public void testGetLock() throws Exception {
        try {
            vHistory.getLock();
            fail("VersionHistory should not be lockable: VersionHistory.getLock() did not throw a LockException");
        } catch (LockException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.getMixinNodeTypes()</code> does not return
     * null.
     */
    public void testGetMixinNodeTypes() throws Exception {
        NodeType[] ntArray = vHistory.getMixinNodeTypes();
        assertNotNull("VersionHistory.getMixinNodeTypes() returns null array", ntArray);
    }

    /**
     * Tests if <code>VersionHistory.getName()</code> returns the right name
     */
    public void testGetName() throws Exception {
        assertEquals("VersionHistory.getName() does not return the right name", version.getParent().getName(), vHistory.getName());
    }

    /**
     * Tests if <code>VersionHistory.getNode(String)</code> returns the right
     * child Node
     */
    public void testGetNode() throws Exception {
        assertTrue("VersionHistory.getNode(String) does not return a sub-node of type nt:version", vHistory.getNode(jcrRootVersion).isNodeType(ntVersion));
    }

    /**
     * Tests if <code>VersionHistory.getNodes()</code> and
     * <code>VersionHistory.getNodes(String)</code> returns the right child
     * Node
     */
    public void testGetNodes() throws Exception {
        Node n = vHistory.getNodes().nextNode();
        assertTrue("VersionHistory.getNodes() does not return a sub-node of type nt:version", n.isNodeType(ntVersion) || n.isNodeType(ntVersionLabels));
        assertTrue("VersionHistory.getNodes(String) does not return a sub-node of type nt:version", vHistory.getNodes(superuser.getNamespacePrefix(NS_JCR_URI) + ":r*").nextNode().isNodeType(ntVersion));
    }

    /**
     * Tests if <code>VersionHistory.getParent()</code> returns the right parent
     * Node
     */
    public void testGetParent() throws Exception {
        assertTrue("VersionHistory.getParent() does not return the right parent-node", version.getAncestor(version.getDepth() - 2).isSame(vHistory.getParent()));
    }

    /**
     * Tests if <code>VersionHistory.getPath()</code> returns the right path
     */
    public void testGetPath() throws Exception {
        assertTrue("VersionHistory.getPath() does not return the right path", vHistory.getPath().startsWith("/" + superuser.getNamespacePrefix(NS_JCR_URI) + ":system/" + superuser.getNamespacePrefix(NS_JCR_URI) + ":versionStorage/"));
    }

    /**
     * Tests if <code>VersionHistory.getPrimaryItem()</code> throws a {@link
     * javax.jcr.ItemNotFoundException}
     */
    public void testGetPrimaryItem() throws Exception {
        try {
            vHistory.getPrimaryItem();
            fail("VersionHistory.getPrimaryItem() did not throw a ItemNotFoundException");
        } catch (ItemNotFoundException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.getPrimaryNodeType()</code> returns the
     * right primary node type <code>nt:versionHistory</code>
     */
    public void testGetPrimaryNodeType() throws Exception {
        assertEquals("VersionHistory does not have the primary node type nt:versionHistory", ntVersionHistory, vHistory.getPrimaryNodeType().getName());
    }

    /**
     * Tests if <code>VersionHistory.getProperties()</code> and
     * <code>VersionHistory.getProperties(String)</code> return the right
     * property
     */
    public void testGetProperties() throws Exception {
        PropertyIterator pi = vHistory.getProperties();
        boolean hasPropertyUUID = false;
        while (pi.hasNext()) {
            if (pi.nextProperty().getName().equals(jcrUUID)) {
                hasPropertyUUID = true;
            }
        }
        assertTrue("VersionHistory.getProperties() does not return property jcr:UUID", hasPropertyUUID);

        pi = vHistory.getProperties(superuser.getNamespacePrefix(NS_JCR_URI) + ":*");
        hasPropertyUUID = false;
        while (pi.hasNext()) {
            if (pi.nextProperty().getName().equals(jcrUUID)) {
                hasPropertyUUID = true;
            }
        }
        assertTrue("VersionHistory.getProperties(String) does not return property jcr:UUID", hasPropertyUUID);
    }

    /**
     * Tests if <code>VersionHistory.getProperty(String)</code> returns the
     * right property
     */
    public void testGetProperty() throws Exception {
        assertTrue("VersionHistory.getProperty(String) does not return property jcr:UUID", vHistory.getProperty(jcrUUID).getName().equals(jcrUUID));
    }

    /**
     * Tests if <code>VersionHistory.getReferences()</code> returns the right
     * reference of the versionable node
     */
    public void testGetReferences() throws Exception {
        PropertyIterator pi = vHistory.getReferences();
        boolean hasNodeReference = false;
        while (pi.hasNext()) {
            Property p = pi.nextProperty();
            if (p.getName().equals(jcrVersionHistory) && superuser.getNodeByUUID(p.getString()).isSame(vHistory)) {
                hasNodeReference = true;
                break;
            }
        }
        assertTrue("VersionHistory.getReferences() does not return the jcr:versionHistory property of the versioned Node", hasNodeReference);
    }

    /**
     * Tests if <code>VersionHistory.getSession()</code> returns the right
     * session
     */
    public void testGetSession() throws Exception {
        assertSame("VersionHistory.getSession() did not return the right session", superuser, vHistory.getSession());
    }

    /**
     * Tests if <code>VersionHistory.getUUID()</code> returns the right UUID
     */
    public void testGetUUID() throws Exception {
        assertEquals("VersionHistory.getUUID() did not return the right UUID", versionableNode.getProperty(jcrVersionHistory).getString(), vHistory.getUUID());
    }

    /**
     * Tests if <code>VersionHistory.getVersionHistory()</code> throws an {@link
     * javax.jcr.UnsupportedRepositoryOperationException}
     */
    public void testGetVersionHistory() throws Exception {
        try {
            vHistory.getVersionHistory();
            fail("VersionHistory.getVersionHistory() did not throw an UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.hasNode(String)</code> returns the right
     * <code>boolean</code> value
     */
    public void testHasNode() throws Exception {
        assertTrue("VersionHistory.hasNode(String) did not return true", vHistory.hasNode(jcrRootVersion));
    }

    /**
     * Tests if <code>VersionHistory.hasNodes()</code> returns
     * <code>true</code>
     */
    public void testHasNodes() throws Exception {
        assertTrue("VersionHistory.hasNodes() did not return true", vHistory.hasNodes());
    }

    /**
     * Tests if <code>VersionHistory.hasProperties()</code> returns
     * <code>true</code>
     */
    public void testHasProperties() throws Exception {
        assertTrue("VersionHistory.hasProperties() did not return true", vHistory.hasProperties());
    }

    /**
     * Tests if <code>VersionHistory.hasProperty(String)</code> returns the
     * right <code>boolean</code> value
     */
    public void testHasProperty() throws Exception {
        assertTrue("VersionHistory.hasProperty(String) did not return true", vHistory.hasProperty(jcrUUID));
    }

    /**
     * Tests if <code>VersionHistory.holdsLock()</code> returns
     * <code>false</code>
     */
    public void testHoldsLock() throws Exception {
        assertFalse("VersionHistory.holdsLock() did not return false", vHistory.holdsLock());
    }

    /**
     * Tests if <code>VersionHistory.isCheckedOut()</code> returns
     * <code>true</code>
     */
    public void testIsCheckedOut() throws Exception {
        assertTrue("VersionHistory.isCheckedOut() did not return true", vHistory.isCheckedOut());
    }

    /**
     * Tests if <code>VersionHistory.isLocked()</code> returns
     * <code>false</code>
     */
    public void testIsLocked() throws Exception {
        assertFalse("VersionHistory.isLocked() did not return false", vHistory.isLocked());
    }

    /**
     * Tests if <code>VersionHistory.isModified()</code> returns
     * <code>false</code>
     */
    public void testIsModified() throws Exception {
        assertFalse("VersionHistory.isModified() did not return false", vHistory.isModified());
    }

    /**
     * Tests if <code>VersionHistory.isNew()</code> returns <code>false</code>
     */
    public void testIsNew() throws Exception {
        assertFalse("VersionHistory.isNew() did not return false", vHistory.isNew());
    }

    /**
     * Tests if <code>VersionHistory.isNode()</code> returns <code>true</code>
     */
    public void testIsNode() throws Exception {
        assertTrue("VersionHistory.isNode() did not return true", vHistory.isNode());
    }

    /**
     * Tests if <code>VersionHistory.isNodeType(String)</code> returns the right
     * <code>boolean</code> value
     */
    public void testIsNodeType() throws Exception {
        assertTrue("VersionHistory.isNodeType(String) did not return true for nt:versionHistory", vHistory.isNodeType(ntVersionHistory));
    }

    /**
     * Tests if <code>VersionHistory.isSame()</code> returns the right
     * <code>boolean</code> value
     */
    public void testIsSame() throws Exception {
        assertTrue("VersionHistory.isSame(Item) did not return true", vHistory.isSame(version.getParent()));
    }

    /**
     * Tests if <code>VersionHistory.lock(boolean, boolean)</code> throws a
     * {@link javax.jcr.lock.LockException}
     */
    public void testLock() throws Exception {
        try {
            vHistory.lock(true, true);
            fail("VersionHistory should not be lockable: VersionHistory.lock(true,true) did not throw a LockException");
        } catch (LockException success) {
        }
        try {
            vHistory.lock(true, false);
            fail("VersionHistory should not be lockable: VersionHistory.lock(true,false) did not throw a LockException");
        } catch (LockException success) {
        }
        try {
            vHistory.lock(false, true);
            fail("VersionHistory should not be lockable: VersionHistory.lock(false,true) did not throw a LockException");
        } catch (LockException success) {
        }
        try {
            vHistory.lock(false, false);
            fail("VersionHistory should not be lockable: VersionHistory.lock(false,false) did not throw a UnsupportedRepositoryOperationException");
        } catch (LockException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.merge(String)</code> throws an
     * {@link javax.jcr.nodetype.ConstraintViolationException}
     */
    public void testMerge() throws Exception {
        try {
            vHistory.merge(workspaceName, true);
            fail("VersionHistory.merge(String, true) did not throw an ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            vHistory.merge(workspaceName, false);
            fail("VersionHistory.merge(String, false) did not throw an ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.orderBefore(String, String)</code> throws
     * an {@link javax.jcr.UnsupportedRepositoryOperationException}
     */
    public void testOrderBefore() throws Exception {
        try {
            vHistory.orderBefore(jcrFrozenNode, null);
            fail("VersionHistory.orderBefore(String,String) did not throw an UnsupportedRepositoryOperationException or a ConstraintViolationException");
        } catch (UnsupportedRepositoryOperationException success) {
        } catch (ConstraintViolationException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.refresh(boolean)</code> works as expected
     * (do nothing and return quietly)
     */
    public void testRefresh() throws Exception {
        // should do nothing and return quietly
        vHistory.refresh(true);
        vHistory.refresh(false);
    }

    /**
     * Tests if <code>VersionHistory.remove()</code> throws an {@link
     * javax.jcr.nodetype.ConstraintViolationException}
     */
    public void testRemove() throws Exception {
        try {
            Node vHistoryParent = vHistory.getParent();
            vHistory.remove();
            vHistoryParent.save();
            fail("VersionHistory should be read-only: VersionHistory.remove() did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.removeMixin(String)</code> throws an {@link
     * javax.jcr.nodetype.NoSuchNodeTypeException}
     */
    public void testRemoveMixin() throws Exception {
        try {
            vHistory.removeMixin(mixReferenceable);
            fail("VersionHistory does not have mixins: VersionHistory.removeMixin(String) did not throw a NoSuchNodeTypeException.");
        } catch (ConstraintViolationException success) {
        } catch (NoSuchNodeTypeException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.restore(String, boolean)</code> and
     * <code>VersionHistory.restore(Version, boolean)</code> throw an {@link
     * UnsupportedRepositoryOperationException} and <code>VersionHistory.restore(Version,
     * String, boolean)</code> throws a {@link ConstraintViolationException}.
     */
    public void testRestore() throws Exception {
        try {
            vHistory.restore("abc", true);
            fail("VersionHistory.restore(String,boolean) did not throw an UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException success) {
        }
        try {
            vHistory.restore(version, true);
            fail("VersionHistory.restore(Version,boolean) did not throw an UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException success) {
        }
        try {
            vHistory.restore(version, "abc", true);
            fail("VersionHistory.restore(Version,String,boolean) did not throw an ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.restoreByLabel(String, boolean)</code>
     * throws an {@link javax.jcr.UnsupportedRepositoryOperationException}
     */
    public void testRestoreByLabel() throws Exception {
        try {
            vHistory.restoreByLabel("abc", true);
            fail("VersionHistory.restoreByLabel(String,boolean) did not throw an UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException success) {
        }
    }

    /**
     * Tests if <ul> <li><code>VersionHistory.setProperty(String,
     * String[])</code></li> <li><code>VersionHistory.setProperty(String,
     * String[], int)</code></li> <li><code>VersionHistory.setProperty(String,
     * Value[])</code></li> <li><code>VersionHistory.setProperty(String,
     * Value[], int)</code></li> <li><code>VersionHistory.setProperty(String,
     * boolean)</code></li> <li><code>VersionHistory.setProperty(String,
     * double)</code></li> <li><code>VersionHistory.setProperty(String,
     * InputStream)</code></li> <li><code>VersionHistory.setProperty(String,
     * String)</code></li> <li><code>VersionHistory.setProperty(String,
     * Calendar)</code></li> <li><code>VersionHistory.setProperty(String,
     * Node)</code></li> <li><code>VersionHistory.setProperty(String,
     * Value)</code></li> <li><code>VersionHistory.setProperty(String,
     * long)</code></li> </ul> all throw a {@link javax.jcr.nodetype.ConstraintViolationException}
     */
    public void testSetProperty() throws Exception {

        // create Value[] object
        Value[] vArray = new Value[3];
        vArray[0] = superuser.getValueFactory().createValue("abc");
        vArray[1] = superuser.getValueFactory().createValue("xyz");
        vArray[2] = superuser.getValueFactory().createValue("123");

        // create String array
        String[] s = {"abc", "xyz", "123"};

        try {
            vHistory.setProperty(propertyName1, s);
            vHistory.save();
            fail("VersionHistory should be read-only: VersionHistory.setProperty(String,String[]) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            vHistory.setProperty(propertyName1, s, PropertyType.STRING);
            vHistory.save();
            fail("VersionHistory should be read-only: VersionHistory.setProperty(String,String[],int) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            vHistory.setProperty(propertyName1, vArray);
            vHistory.save();
            fail("VersionHistory should be read-only: VersionHistory.setProperty(String,Value[]) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            vHistory.setProperty(propertyName1, vArray, PropertyType.STRING);
            vHistory.save();
            fail("VersionHistory should be read-only: VersionHistory.setProperty(String,Value[],int]) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            vHistory.setProperty(propertyName1, true);
            vHistory.save();
            fail("VersionHistory should be read-only: VersionHistory.setProperty(String,boolean) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            vHistory.setProperty(propertyName1, 123);
            vHistory.save();
            fail("VersionHistory should be read-only: VersionHistory.setProperty(String,double) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            byte[] bytes = {73, 26, 32, -36, 40, -43, -124};
            InputStream inpStream = new ByteArrayInputStream(bytes);
            vHistory.setProperty(propertyName1, inpStream);
            vHistory.save();
            fail("VersionHistory should be read-only: VersionHistory.setProperty(String,InputStream) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            vHistory.setProperty(propertyName1, "abc");
            vHistory.save();
            fail("VersionHistory should be read-only: VersionHistory.setProperty(String,String) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            Calendar c = new GregorianCalendar(1945, 1, 6, 16, 20, 0);
            vHistory.setProperty(propertyName1, c);
            vHistory.save();
            fail("VersionHistory should be read-only: VersionHistory.setProperty(String,Calendar) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            vHistory.setProperty(propertyName1, testRootNode);
            vHistory.save();
            fail("VersionHistory should be read-only: VersionHistory.setProperty(String,Node) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            Value v = superuser.getValueFactory().createValue("abc");
            vHistory.setProperty(propertyName1, v);
            vHistory.save();
            fail("VersionHistory should be read-only: VersionHistory.setProperty(String,Value) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            vHistory.setProperty(propertyName1, -2147483650L);
            vHistory.save();
            fail("VersionHistory should be read-only: VersionHistory.setProperty(String,long) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.unlock()</code> throws a {@link
     * javax.jcr.lock.LockException}
     */
    public void testUnlock() throws Exception {
        try {
            vHistory.unlock();
            fail("VersionHistory should not be lockable: VersionHistory.unlock() did not throw a LockException");
        } catch (LockException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.update(String)</code> throws an
     * {@link javax.jcr.nodetype.ConstraintViolationException}
     */
    public void testUpdate() throws Exception {
        try {
            vHistory.update(workspaceName);
            fail("VersionHistory.update(String) did not throw an ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
    }

}
