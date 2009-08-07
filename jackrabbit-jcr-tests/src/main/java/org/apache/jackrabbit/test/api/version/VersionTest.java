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

import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.List;
import java.util.Arrays;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

/**
 * <code>VersionTest</code> covers tests related to the methods of the {@link
 * javax.jcr.version.Version} class.
 *
 * @test
 * @sources VersionTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.VersionTest
 * @keywords versioning
 */
public class VersionTest extends AbstractVersionTest {

    private Version version;
    private Version version2;

    /**
     * helper class used in testAccept()
     */
    private class ItemVisitorTest implements ItemVisitor {

        Node ivtNode;

        public ItemVisitorTest(Version v) {
            ivtNode = v;
        }

        public void visit(Node node) throws RepositoryException {
            assertTrue("Version.accept(ItemVisitor) does not provide the right node to the ItemVisitor", ivtNode.isSame(node));
        }

        public void visit(Property property) throws RepositoryException {
        }
    }

    protected void setUp() throws Exception {
        super.setUp();

        // create two versions
        version = versionableNode.checkin();
        versionableNode.checkout();
        version2 = versionableNode.checkin();
    }

    protected void tearDown() throws Exception {
        // check the node out, so that it can be removed
        versionableNode.checkout();
        version = null;
        version2 = null;
        super.tearDown();
    }

    /**
     * Tests if <code>Version.accept(ItemVisitor)</code> accepts a ItemVisitor
     * and if the right Node is provided to that visitor.
     */
    public void testAccept() throws Exception {
        ItemVisitorTest ivt = new ItemVisitorTest(version);
        version.accept(ivt);
    }

    /**
     * Tests if <code>Version.addMixin(String)</code> throws a {@link
     * javax.jcr.nodetype.ConstraintViolationException}
     */
    public void testAddMixin() throws Exception {
        try {
            version.addMixin(mixVersionable);
            fail("Version should be read-only: Version.addMixin(String) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
    }

    /**
     * Tests if <code>Version.addNode(String)</code> and
     * <code>Version.addNode(String, String)</code> throw a {@link
     * javax.jcr.nodetype.ConstraintViolationException}
     */
    public void testAddNode() throws Exception {
        try {
            version.addNode(nodeName4);
            version.save();
            fail("Version should be read-only: Version.addNode(String) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            version.addNode(nodeName4, ntBase);
            version.save();
            fail("Version should be read-only: Version.addNode(String,String) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
    }

    /**
     * Tests if <code>Version.canAddMixin(String)</code> returns
     * <code>false</code>
     */
    public void testCanAddMixin() throws Exception {
        assertFalse("Version should be read-only: Version.canAddMixin(String) returned true", version.canAddMixin(mixVersionable));
    }

    /**
     * Tests if <code>Version.cancelMerge(Version)</code> throws an {@link
     * javax.jcr.UnsupportedRepositoryOperationException}
     */
    public void testCancelMerge() throws Exception {
        try {
            version.cancelMerge(version2);
            fail("Version.cancelMerge(Version) did not throw an UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException success) {
        }
    }

    /**
     * Tests if <code>Version.checkin()</code> throws an {@link
     * javax.jcr.UnsupportedRepositoryOperationException}
     */
    public void testCheckin() throws Exception {
        try {
            version.checkin();
            fail("Version.checkin() did not throw an UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException success) {
        }
    }

    /**
     * Tests if <code>Version.checkout()</code> throws an {@link
     * javax.jcr.UnsupportedRepositoryOperationException}
     */
    public void testCheckout() throws Exception {
        try {
            version.checkout();
            fail("Version.checkout() did not throw an UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException success) {
        }
    }

    /**
     * Tests if <code>Version.doneMerge(Version)</code> throws an {@link
     * javax.jcr.UnsupportedRepositoryOperationException}
     */
    public void testDoneMerge() throws Exception {
        try {
            version.doneMerge(version2);
            fail("Version should not be versionable: Version.doneMerge(Version) did not throw an UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException success) {
        }
    }

    /**
     * Tests if <code>Version.getAncestor(int)</code> returns the right
     * ancestor
     */
    public void testGetAncestor() throws Exception {
        assertTrue("Version.getAncestor(int) does not work", superuser.getRootNode().isSame(version.getAncestor(0)));
    }

    /**
     * Tests if <code>Version.getBaseVersion()</code> throws an {@link
     * javax.jcr.UnsupportedRepositoryOperationException}
     */
    public void testGetBaseVersion() throws Exception {
        try {
            version.getBaseVersion();
            fail("Version.getBaseVersion() did not throw an UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException success) {
        }
    }

    /**
     * Tests if <code>Version.getCorrespondingNodePath(String)</code> returns
     * the right path
     */
    public void testGetCorrespondingNodePath() throws Exception {
        assertEquals("Version.getCorrespondingNodePath(String) did not return the right path", version.getPath(), version.getCorrespondingNodePath(workspaceName));
    }

    /**
     * Tests if <code>Version.getDepth()</code> returns the right depth
     */
    public void testGetDepth() throws Exception {
        assertTrue("Version.getDepth() mismatch", version.getDepth() >= 4);
    }

    /**
     * Tests if <code>Version.getIndex()</code> returns the right index
     */
    public void testGetIndex() throws Exception {
        assertEquals("Version.getIndex() mismatch", 1, version.getIndex());
    }

    /**
     * Tests if <code>Version.getLock()</code> throws a {@link
     * javax.jcr.lock.LockException}
     */
    public void testGetLock() throws Exception {
        try {
            version.getLock();
            fail("Version should not be lockable: Version.getLock() did not throw a LockException");
        } catch (LockException success) {
        }
    }

    /**
     * Tests if <code>Version.getMixinNodeTypes()</code> does not return null.
     */
    public void testGetMixinNodeTypes() throws Exception {
        NodeType[] ntArray = version.getMixinNodeTypes();
        assertNotNull("Version.getMixinNodeTypes returns null array", ntArray);
    }

    /**
     * Tests if <code>Version.getName()</code> returns the right name
     */
    public void testGetName() throws Exception {
        assertTrue("Version.getName() does not return the right name", versionableNode.getVersionHistory().getVersion(version.getName()).isSame(version));
    }

    /**
     * Tests if <code>Version.getNode(String)</code> returns the right child
     * Node
     */
    public void testGetNode() throws Exception {
        assertTrue("Version.getNode(String) does not return a sub-node of type nt:frozenNode", version.getNode(jcrFrozenNode).isNodeType(ntFrozenNode));
    }

    /**
     * Tests if <code>Version.getNodes()</code> and <code>Version.getNodes(String)</code>
     * returns the right child Node
     */
    public void testGetNodes() throws Exception {
        assertTrue("Version.getNodes() does not return a sub-node of type nt:frozenNode", version.getNodes().nextNode().isNodeType(ntFrozenNode));
        assertTrue("Version.getNodes(String) does not return a sub-node of type nt:frozenNode", version.getNodes(superuser.getNamespacePrefix(NS_JCR_URI) + ":*").nextNode().isNodeType(ntFrozenNode));
    }

    /**
     * Tests if <code>Version.getParent()</code> returns the right parent Node
     */
    public void testGetParent() throws Exception {
        assertTrue("Version.getParent() does not return a parent-node of type nt:versionHistory", version.getParent().isNodeType(ntVersionHistory));
    }

    /**
     * Tests if <code>Version.getPath()</code> returns the right path
     */
    public void testGetPath() throws Exception {
        assertTrue("Version.getPath() does not return the right path", version.getPath().startsWith("/" + superuser.getNamespacePrefix(NS_JCR_URI) + ":system/" + superuser.getNamespacePrefix(NS_JCR_URI) + ":versionStorage/"));
    }

    /**
     * Tests if <code>Version.getPrimaryItem()</code> throws a {@link
     * javax.jcr.ItemNotFoundException}
     */
    public void testGetPrimaryItem() throws Exception {
        try {
            version.getPrimaryItem();
            fail("Version.getPrimaryItem() did not throw a ItemNotFoundException");
        } catch (ItemNotFoundException success) {
        }
    }

    /**
     * Tests if <code>Version.getPrimaryNodeType()</code> returns the right
     * primary node type <code>nt:version</code>
     */
    public void testGetPrimaryNodeType() throws Exception {
        assertEquals("Version does not have the primary node type nt:version", ntVersion, version.getPrimaryNodeType().getName());
    }

    /**
     * Tests if <code>Version.getProperties()</code> and
     * <code>Version.getProperties(String)</code> return the right property
     */
    public void testGetProperties() throws Exception {
        PropertyIterator pi = version.getProperties();
        boolean hasPropertyCreated = false;
        while (pi.hasNext()) {
            if (pi.nextProperty().getName().equals(jcrCreated)) {
                hasPropertyCreated = true;
            }
        }
        assertTrue("Version.getProperties() does not return property jcr:created", hasPropertyCreated);

        pi = version.getProperties(superuser.getNamespacePrefix(NS_JCR_URI) + ":*");
        hasPropertyCreated = false;
        while (pi.hasNext()) {
            if (pi.nextProperty().getName().equals(jcrCreated)) {
                hasPropertyCreated = true;
            }
        }
        assertTrue("Version.getProperties(String) does not return property jcr:created", hasPropertyCreated);
    }

    /**
     * Tests if <code>Version.getProperty(String)</code> returns the right
     * property
     */
    public void testGetProperty() throws Exception {
        assertTrue("Version.getProperty(String) does not return property jcr:created", version.getProperty(jcrCreated).getName().equals(jcrCreated));
    }

    /**
     * Tests if <code>Version.getSession()</code> returns the right session
     */
    public void testGetSession() throws Exception {
        assertSame("Version.getSession() did not return the right session", superuser, version.getSession());
    }

    /**
     * Tests if <code>Version.getVersionHistory()</code> throws an {@link
     * javax.jcr.UnsupportedRepositoryOperationException}
     */
    public void testGetVersionHistory() throws Exception {
        try {
            version.getVersionHistory();
            fail("Version.getVersionHistory() did not throw an UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException success) {
        }
    }

    /**
     * Tests if <code>Version.getUUID()</code> returns the right UUID
     */
    public void testGetUUID() throws Exception {
        List successorValues = Arrays.asList(versionableNode.getVersionHistory().getRootVersion().getProperty(jcrSuccessors).getValues());
        assertTrue("Version.getUUID() did not return the right UUID", successorValues.contains(superuser.getValueFactory().createValue(version)));
    }

    /**
     * Tests if <code>Version.hasNode(String)</code> returns the right
     * <code>boolean</code> value
     */
    public void testHasNode() throws Exception {
        assertTrue("Version.hasNode(String) did not return true", version.hasNode(jcrFrozenNode));
    }

    /**
     * Tests if <code>Version.hasNodes()</code> returns <code>true</code>
     */
    public void testHasNodes() throws Exception {
        assertTrue("Version.hasNodes() did not return true", version.hasNodes());
    }

    /**
     * Tests if <code>Version.hasProperties()</code> returns <code>true</code>
     */
    public void testHasProperties() throws Exception {
        assertTrue("Version.hasProperties() did not return true", version.hasProperties());
    }

    /**
     * Tests if <code>Version.hasProperty(String)</code> returns the right
     * <code>boolean</code> value
     */
    public void testHasProperty() throws Exception {
        assertTrue("Version.hasProperty(String) did not return true", version.hasProperty(jcrCreated));
    }

    /**
     * Tests if <code>Version.holdsLock()</code> returns <code>false</code>
     */
    public void testHoldsLock() throws Exception {
        assertFalse("Version.holdsLock() did not return false", version.holdsLock());
    }

    /**
     * Tests if <code>Version.isCheckedOut()</code> returns <code>true</code>
     */
    public void testIsCheckedOut() throws Exception {
        assertTrue("Version.isCheckedOut() did not return true", version.isCheckedOut());
    }

    /**
     * Tests if <code>Version.isLocked()</code> returns <code>false</code>
     */
    public void testIsLocked() throws Exception {
        assertFalse("Version.isLocked() did not return false", version.isLocked());
    }

    /**
     * Tests if <code>Version.isModified()</code> returns <code>false</code>
     */
    public void testIsModified() throws Exception {
        assertFalse("Version.isModified() did not return false", version.isModified());
    }

    /**
     * Tests if <code>Version.isNew()</code> returns <code>false</code>
     */
    public void testIsNew() throws Exception {
        assertFalse("Version.isNew() did not return false", version.isNew());
    }

    /**
     * Tests if <code>Version.isNode()</code> returns <code>true</code>
     */
    public void testIsNode() throws Exception {
        assertTrue("Version.isNode() did not return true", version.isNode());
    }

    /**
     * Tests if <code>Version.isNodeType(String)</code> returns the right
     * <code>boolean</code> value
     */
    public void testIsNodeType() throws Exception {
        assertTrue("Version.isNodeType(String) did not return true for nt:version", version.isNodeType(ntVersion));
    }

    /**
     * Tests if <code>Version.isSame()</code> returns the right
     * <code>boolean</code> value
     */
    public void testIsSame() throws Exception {
        assertTrue("Version.isSame(Item) did not return true", version2.isSame(versionableNode.getBaseVersion()));
    }

    /**
     * Tests if <code>Version.lock(boolean, boolean)</code> throws a {@link
     * LockException}
     */
    public void testLock() throws Exception {
        try {
            version.lock(true, true);
            fail("Version should not be lockable: Version.lock(true,true) did not throw a LockException");
        } catch (LockException success) {
        }
        try {
            version.lock(true, false);
            fail("Version should not be lockable: Version.lock(true,false) did not throw a LockException");
        } catch (LockException success) {
        }
        try {
            version.lock(false, true);
            fail("Version should not be lockable: Version.lock(false,true) did not throw a LockException");
        } catch (LockException success) {
        }
        try {
            version.lock(false, false);
            fail("Version should not be lockable: Version.lock(false,false) did not throw a LockException");
        } catch (LockException success) {
        }
    }

    /**
     * Tests if <code>Version.merge(String)</code> throws an
     * {@link javax.jcr.nodetype.ConstraintViolationException}
     */
    public void testMerge() throws Exception {
        try {
            version.merge(workspaceName, true);
            fail("Version.merge(String, true) did not throw an ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            version.merge(workspaceName, false);
            fail("Version.merge(String, false) did not throw an ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
    }

    /**
     * Tests if <code>Version.orderBefore(String, String)</code> throws an
     * {@link javax.jcr.UnsupportedRepositoryOperationException}
     */
    public void testOrderBefore() throws Exception {
        try {
            version.orderBefore(jcrFrozenNode, null);
            fail("Version.orderBefore(String,String) did not throw an UnsupportedRepositoryOperationException or a ConstraintViolationException");
        } catch (UnsupportedRepositoryOperationException success) {
        } catch (ConstraintViolationException success) {
        }
    }

    /**
     * Tests if <code>Version.refresh(boolean)</code> works as expected (do
     * nothing and return quietly)
     */
    public void testRefresh() throws Exception {
        // should do nothing and return quietly
        version.refresh(true);
        version.refresh(false);
    }

    /**
     * Tests if <code>Version.remove()</code> throws an {@link
     * javax.jcr.nodetype.ConstraintViolationException}
     */
    public void testRemove() throws Exception {
        try {
            version.remove();
            versionableNode.getVersionHistory().save();
            fail("Version should be read-only: Version.remove() did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
    }

    /**
     * Tests if <code>Version.removeMixin(String)</code> throws an {@link
     * javax.jcr.nodetype.ConstraintViolationException}
     */
    public void testRemoveMixin() throws Exception {
        try {
            version.removeMixin(mixVersionable);
            fail("Version should be read-only: Version.removeMixin(String) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
    }

    /**
     * Tests if <code>Version.restore(String, boolean)</code> and
     * <code>Version.restore(Version, boolean)</code> throw an
     * {@link UnsupportedRepositoryOperationException} and
     * <code>Version.restore(Version, String, boolean)</code> throws a
     * {@link ConstraintViolationException}.
     */
    public void testRestore() throws Exception {
        try {
            version.restore("abc", true);
            fail("Version.restore(String,boolean) did not throw an UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException success) {
        }
        try {
            version.restore(version2, true);
            fail("Version.restore(Version,boolean) did not throw an UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException success) {
        }
        try {
            version.restore(version2, "abc", true);
            fail("Version.restore(Version,String,boolean) did not throw an ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
    }

    /**
     * Tests if <code>Version.restoreByLabel(String, boolean)</code> throws an
     * {@link javax.jcr.UnsupportedRepositoryOperationException}
     */
    public void testRestoreByLabel() throws Exception {
        try {
            version.restoreByLabel("abc", true);
            fail("Version.restoreByLabel(String,boolean) did not throw an UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException success) {
        }
    }

    /**
     * Tests if
     * <ul> <li><code>Version.setProperty(String, String[])</code></li>
     * <li><code>Version.setProperty(String, String[], int)</code></li>
     * <li><code>Version.setProperty(String, Value[])</code></li>
     * <li><code>Version.setProperty(String, Value[], int)</code></li>
     * <li><code>Version.setProperty(String, boolean)</code></li>
     * <li><code>Version.setProperty(String, double)</code></li>
     * <li><code>Version.setProperty(String, InputStream)</code></li>
     * <li><code>Version.setProperty(String, String)</code></li>
     * <li><code>Version.setProperty(String, Calendar)</code></li>
     * <li><code>Version.setProperty(String, Node)</code></li>
     * <li><code>Version.setProperty(String, Value)</code></li>
     * <li><code>Version.setProperty(String, long)</code></li>
     * </ul> all throw a
     * {@link javax.jcr.nodetype.ConstraintViolationException}
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
            version.setProperty(propertyName1, s);
            version.save();
            fail("Version should be read-only: Version.setProperty(String,String[]) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            version.setProperty(propertyName1, s, PropertyType.STRING);
            version.save();
            fail("Version should be read-only: Version.setProperty(String,String[],int) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            version.setProperty(propertyName1, vArray);
            version.save();
            fail("Version should be read-only: Version.setProperty(String,Value[]) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            version.setProperty(propertyName1, vArray, PropertyType.STRING);
            version.save();
            fail("Version should be read-only: Version.setProperty(String,Value[],int]) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            version.setProperty(propertyName1, true);
            version.save();
            fail("Version should be read-only: Version.setProperty(String,boolean) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            version.setProperty(propertyName1, 123);
            version.save();
            fail("Version should be read-only: Version.setProperty(String,double) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            byte[] bytes = {73, 26, 32, -36, 40, -43, -124};
            InputStream inpStream = new ByteArrayInputStream(bytes);
            version.setProperty(propertyName1, inpStream);
            version.save();
            fail("Version should be read-only: Version.setProperty(String,InputStream) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            version.setProperty(propertyName1, "abc");
            version.save();
            fail("Version should be read-only: Version.setProperty(String,String) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            Calendar c = new GregorianCalendar(1945, 1, 6, 16, 20, 0);
            version.setProperty(propertyName1, c);
            version.save();
            fail("Version should be read-only: Version.setProperty(String,Calendar) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            version.setProperty(propertyName1, testRootNode);
            version.save();
            fail("Version should be read-only: Version.setProperty(String,Node) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            Value v = superuser.getValueFactory().createValue("abc");
            version.setProperty(propertyName1, v);
            version.save();
            fail("Version should be read-only: Version.setProperty(String,Value) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
        try {
            version.setProperty(propertyName1, -2147483650L);
            version.save();
            fail("Version should be read-only: Version.setProperty(String,long) did not throw a ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
    }

    /**
     * Tests if <code>Version.unlock()</code> throws a {@link
     * javax.jcr.lock.LockException}
     */
    public void testUnlock() throws Exception {
        try {
            version.unlock();
            fail("Version should not be lockable: Version.unlock() did not throw a LockException");
        } catch (LockException success) {
        }
    }

    /**
     * Tests if <code>VersionHistory.update(String)</code> throws an
     * {@link javax.jcr.nodetype.ConstraintViolationException}
     */
    public void testUpdate() throws Exception {
        try {
            version.update(workspaceName);
            fail("VersionHistory.update(String) did not throw an ConstraintViolationException");
        } catch (ConstraintViolationException success) {
        }
    }

    /**
     * Tests if the jcr:frozenUuid property has the correct type
     * @throws Exception
     */
    public void testFrozenUUID() throws Exception {
        Property p = version.getNode(jcrFrozenNode).getProperty(jcrFrozenUuid);
        assertEquals("jcr:fronzenUuid should be of type string", PropertyType.TYPENAME_STRING, PropertyType.nameFromValue(p.getType()));
    }
}
