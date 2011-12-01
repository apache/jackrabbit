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

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** <code>GetPropertyTest</code>... */
public class GetPropertyTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(GetPropertyTest.class);

    private String node1Path;
    private String prop1Path;
    private String prop2Path;

    private Session readOnly;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Node n = testRootNode.addNode(nodeName1, testNodeType);
        node1Path = n.getPath();

        Property p = n.setProperty(propertyName1, "string1");
        prop1Path = p.getPath();

        p = n.setProperty(propertyName2, "string2");
        prop2Path = p.getPath();

        testRootNode.save();

        readOnly = getHelper().getReadOnlySession();
    }

    @Override
    protected void tearDown() throws Exception {
        if (readOnly != null) {
            readOnly.logout();
            readOnly = null;
        }
        super.tearDown();
    }

    public void testItemExists() throws RepositoryException {
        assertTrue(readOnly.itemExists(prop1Path));
        assertTrue(readOnly.itemExists(prop2Path));
    }

    public void testGetItem() throws RepositoryException {
        assertFalse(readOnly.getItem(prop1Path).isNode());
        assertFalse(readOnly.getItem(prop2Path).isNode());
    }

    public void testHasProperty() throws RepositoryException {
        String testPath = testRootNode.getPath();
        Node trn = (Node) readOnly.getItem(testPath);

        assertTrue(trn.hasProperty(prop1Path.substring(testPath.length() + 1)));
        assertTrue(trn.hasProperty(prop2Path.substring(testPath.length() + 1)));
    }

    public void testGetProperty() throws RepositoryException {
        String testPath = testRootNode.getPath();
        Node trn = (Node) readOnly.getItem(testPath);

        trn.getProperty(prop1Path.substring(testPath.length() + 1));
        trn.getProperty(prop2Path.substring(testPath.length() + 1));
    }

    public void testGetDeepProperty() throws RepositoryException {
        Node n2 = testRootNode.getNode(nodeName1).addNode(nodeName2);
        testRootNode.save();

        // other session directly accesses n2.
        // consequently n1 is not yet resolved
        Node node2 = (Node) readOnly.getItem(n2.getPath());

        // now try to access properties below n1 -> should be existing although
        // n1 has not yet been resolved.
        assertTrue(readOnly.itemExists(prop1Path));
        Property p1 = (Property) readOnly.getItem(prop1Path);
        assertTrue(p1.isSame(node2.getProperty("../" + Text.getName(prop1Path))));

        PropertyIterator it = node2.getParent().getProperties();
        assertTrue(it.getSize() >= 3);
    }

    public void testGetExternallyAddedItems() throws RepositoryException {
        Node node1 = (Node) readOnly.getItem(node1Path);

        Node n2 = testRootNode.getNode(nodeName1).addNode(nodeName2);
        Property p3 = n2.setProperty(propertyName1, "test");
        testRootNode.save();

        node1.refresh(true);

        assertTrue(readOnly.itemExists(n2.getPath()));
        assertTrue(readOnly.itemExists(p3.getPath()));
    }

    public void testGetExternallyChangedNode() throws RepositoryException {
        // Access node1 through session 1
        Node node1 = (Node) readOnly.getItem(node1Path);

        // Add node and property through session 2
        Node n2 = testRootNode.getNode(nodeName1).addNode(nodeName2);
        Property p3 = n2.setProperty(propertyName1, "test");
        testRootNode.save();

        // Assert added nodes are visible in session 1 after refresh
        node1.refresh(false);
        assertTrue(readOnly.itemExists(n2.getPath()));
        assertTrue(readOnly.itemExists(p3.getPath()));

        Item m2 = readOnly.getItem(n2.getPath());
        assertTrue(m2.isNode());
        assertTrue(((Node) m2).hasProperty(propertyName1));

        // Remove property through session 2
        p3.remove();
        testRootNode.save();

        // Assert removal is visible through session 1
        node1.refresh(false);
        assertFalse(((Node) m2).hasProperty(propertyName1));
    }

    public void testGetExternallyChangedProperty() throws RepositoryException {
        // Access node1 through session 1
        Node node1 = (Node) readOnly.getItem(node1Path);

        // Add node and property through session 2
        Node n2 = testRootNode.getNode(nodeName1).addNode(nodeName2);
        Property p3 = n2.setProperty(propertyName1, "test");
        p3.setValue("v3");
        testRootNode.save();

        // Assert added nodes are visible in session 1 after refresh
        node1.refresh(false);
        assertTrue(readOnly.itemExists(n2.getPath()));
        assertTrue(readOnly.itemExists(p3.getPath()));

        Item q3 = readOnly.getItem(p3.getPath());
        assertFalse(q3.isNode());
        assertTrue("v3".equals(((Property) q3).getString()));

        // Change property value through session 2
        p3.setValue("v3_modified");
        testRootNode.save();

        // Assert modification is visible through session 1
        node1.refresh(false);
        assertTrue("v3_modified".equals(((Property) q3).getString()));
    }

    public void testGetDeepSNSProperties() throws RepositoryException, NotExecutableException {
        Node n = testRootNode.getNode(nodeName1);
        if (!n.getDefinition().allowsSameNameSiblings()) {
            throw new NotExecutableException();
        }
        Node sib2 = testRootNode.addNode(nodeName1);
        Property p2 = sib2.setProperty(propertyName1, "sib2-prop");

        Node sib3 = testRootNode.addNode(nodeName1);
        Property p3 = sib3.setProperty(propertyName1, "sib3-prop");
        testRootNode.save();

        Session s = getHelper().getReadWriteSession();
        try {
            Node sibNode = (Node) s.getItem(sib2.getPath());
            sibNode.remove();

            // after transient removal of the sibling2, sibling3 must match its
            // path -> the property must have the proper value.
            Property pp3 = (Property) s.getItem(sib2.getPath() + "/" + propertyName1);
            assertEquals("sib3-prop", pp3.getString());

            // the tree starting with node[3] must not be accessible any more.
            assertFalse(s.itemExists(p3.getPath()));
            assertFalse(s.itemExists(sib3.getPath()));
        } finally {
            s.logout();
        }
    }

    public void testGetDeepRefNodeProperties() throws RepositoryException, NotExecutableException {
        Node n = testRootNode.getNode(nodeName1);
        n.addMixin(mixReferenceable);
        Node n2 = n.addNode(nodeName2);
        Property p3 = n2.setProperty(propertyName1, "test");
        testRootNode.save();

        // other session directly accesses p3.
        // consequently n1 is not yet resolved
        Property prop3 = (Property) readOnly.getItem(p3.getPath());

        // now try to access properties below n1 -> should be existing although
        // n1 has not yet been resolved.
        assertTrue(readOnly.itemExists(prop2Path));
        Property p1 = (Property) readOnly.getItem(prop2Path);

        Node node1 = readOnly.getNodeByUUID(n.getUUID());
        assertTrue(p1.isSame(node1.getProperty(Text.getName(prop2Path))));
    }

    public void testGetPropertyOfRemovedAncestor() throws RepositoryException {
        Session rw = getHelper().getReadWriteSession();
        try {
            // add modification to a property.
            Property p = (Property) rw.getItem(prop1Path);
            p.setValue("changedValue");

            // transiently remove the test root node
            rw.getItem(testRootNode.getPath()).remove();

            try {
                p.getValue();
                fail("modified property must be marked removed upon parent removal");
            } catch (InvalidItemStateException e) {
                // success
            }
            try {
                rw.getItem(prop1Path);
                fail("modified property must be marked removed upon parent removal");
            } catch (PathNotFoundException e) {
                // success
            }
            try {
                Property p2 = (Property) rw.getItem(prop2Path);
                fail("existing property must be marked removed upon parent removal");
            } catch (PathNotFoundException e) {
                // success
            }

            // revert all transient modifications
            rw.refresh(false);
            Property pAgain = (Property) rw.getItem(prop1Path);

            // TODO: for generic jsr 170 test: change assert to p.isSame(pAgain)
            assertTrue(p.isSame(pAgain));
            assertEquals("string1", p.getString());
        } finally {
            rw.logout();
        }
    }

    public void testGetDeepEmptyStringProperty() throws RepositoryException, NotExecutableException {
        Node n = testRootNode.getNode(nodeName1);
        Node n2 = n.addNode(nodeName2);
        Node n3 = n2.addNode(nodeName3);
        Node n4 = n3.addNode(nodeName4);
        Property emptyProp = n4.setProperty(propertyName1, "");
        testRootNode.save();

        Property p = readOnly.getProperty(emptyProp.getPath());
        assertEquals("", p.getString());
    }
}
