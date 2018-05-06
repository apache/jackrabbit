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

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Calendar;

/** <code>IsSameTest</code>... */
public class IsSameTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(IsSameTest.class);

    public void testIsSameProperty() throws RepositoryException {

        Node n = testRootNode.addNode(nodeName1, testNodeType);
        Property p = n.setProperty(propertyName1, "anyvalue");
        testRootNode.save();

        // access same property through different session
        Session otherSession = getHelper().getReadOnlySession();
        try {
            Property otherProperty = (Property) otherSession.getItem(p.getPath());
            assertTrue(p.isSame(otherProperty));
        } finally {
            otherSession.logout();
        }
    }

    public void testIsSameProperty2() throws RepositoryException {

        Node n = testRootNode.addNode(nodeName1, testNodeType);
        Property p = n.setProperty(propertyName1, "anyvalue");
        testRootNode.save();

        // add transient modification to the property:
        p.setValue("someOtherValue");

        // access same property through different session
        Session otherSession = getHelper().getReadOnlySession();
        try {
            Property otherProperty = (Property) otherSession.getItem(p.getPath());
            assertTrue(p.isSame(otherProperty));
        } finally {
            otherSession.logout();
        }
    }

    public void testIsSameProperty3() throws RepositoryException {
        // create a node (nt:resource) that implicitly is referenceable
        Node n = testRootNode.addNode("aFile", "nt:file");
        n = n.addNode("jcr:content", "nt:resource");
        n.setProperty("jcr:lastModified", Calendar.getInstance());
        n.setProperty("jcr:mimeType", "text/plain");
        Property jcrData = n.setProperty("jcr:data", "abc", PropertyType.BINARY);
        testRootNode.save();

        // access same property through different session
        Session otherSession = getHelper().getReadOnlySession();
        try {
            Property otherProperty = (Property) otherSession.getItem(jcrData.getPath());
            assertTrue(jcrData.isSame(otherProperty));
        } finally {
            otherSession.logout();
        }
    }

    public void testIsSameProperty4() throws RepositoryException {
        // create a node (nt:resource) that implicitly is referenceable
        Node n = testRootNode.addNode("aFile", "nt:file");
        n = n.addNode("jcr:content", "nt:resource");
        n.setProperty("jcr:lastModified", Calendar.getInstance());
        n.setProperty("jcr:mimeType", "text/plain");
        Property jcrData = n.setProperty("jcr:data", "abc", PropertyType.BINARY);
        testRootNode.save();

        // access same property through different session
        Session otherSession = getHelper().getReadOnlySession();
        try {
            Property otherProperty = (Property) otherSession.getItem(jcrData.getPath());
            assertTrue(n.getProperty("jcr:data").isSame(otherProperty));
        } finally {
            otherSession.logout();
        }
    }

    public void testIsSameNode() throws RepositoryException {
        // create a node (nt:resource) that implicitly is referenceable
        Node n = testRootNode.addNode("aFile", "nt:file");
        n = n.addNode("jcr:content", "nt:resource");
        n.setProperty("jcr:lastModified", Calendar.getInstance());
        n.setProperty("jcr:mimeType", "text/plain");
        n.setProperty("jcr:data", "abc", PropertyType.BINARY);
        testRootNode.save();

        // access nt:resource node through different session
        Session otherSession = getHelper().getReadOnlySession();
        try {
            Node otherNode = (Node) otherSession.getItem(n.getPath());
            assertTrue(n.isSame(otherNode));
        } finally {
            otherSession.logout();
        }
    }

    public void testIsSameNode2() throws RepositoryException {
        // create a node (nt:resource) that implicitly is referenceable
        Node n = testRootNode.addNode("aFile", "nt:file");
        n = n.addNode("jcr:content", "nt:resource");
        n.setProperty("jcr:lastModified", Calendar.getInstance());
        n.setProperty("jcr:mimeType", "text/plain");
        n.setProperty("jcr:data", "abc", PropertyType.BINARY);
        testRootNode.save();

        // access nt:resource node through different session
        Session otherSession = getHelper().getReadOnlySession();
        try {
            Node otherNode = (Node) otherSession.getItem(n.getPath());
            assertTrue(otherNode.isSame(n));
        } finally {
            otherSession.logout();
        }
    }

    public void testIsSameNode3() throws RepositoryException {

        Node n = testRootNode.addNode(nodeName1, testNodeType);
        Property p = n.setProperty(propertyName1, "anyvalue");
        testRootNode.save();

        String srcPath = n.getPath();
        String destPath = testRootNode.getPath() + "/" + nodeName2;

        // transiently move the node.
        testRootNode.getSession().move(srcPath, destPath);
        assertTrue(n.isSame(superuser.getItem(destPath)));
    }

    public void testIsSameNode4() throws RepositoryException {

        Node n = testRootNode.addNode(nodeName1, testNodeType);
        Property p = n.setProperty(propertyName1, "anyvalue");
        testRootNode.save();

        // transiently move the node.
        String srcPath = n.getPath();
        String destPath = testRootNode.getPath() + "/" + nodeName2;
        testRootNode.getSession().move(srcPath, destPath);

        Session otherSession = getHelper().getReadOnlySession();
        try {
            Node otherNode = (Node) otherSession.getItem(srcPath);
            assertTrue(n.isSame(otherNode));
            assertTrue(superuser.getItem(destPath).isSame(otherNode));
        } finally {
            otherSession.logout();
        }
    }

    public void testIsSameNode5() throws RepositoryException {

        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.addMixin(mixReferenceable);
        testRootNode.save();

        // transiently move the node.
        String srcPath = n.getPath();
        String destPath = testRootNode.getPath() + "/" + nodeName2;

        Session otherSession = getHelper().getReadOnlySession();
        try {
            Node otherNode = (Node) otherSession.getItem(srcPath);

            testRootNode.getSession().getWorkspace().move(srcPath, destPath);

            assertTrue(otherNode.isSame(n));
            assertTrue(n.isSame(otherNode));
        } finally {
            otherSession.logout();
        }
    }

    public void testIsSameNode6() throws RepositoryException {

        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.addMixin(mixReferenceable);
        testRootNode.save();

        // transiently move the node.
        String srcPath = n.getPath();
        String destPath = testRootNode.getPath() + "/" + nodeName2;

        Session otherSession = getHelper().getReadOnlySession();
        try {
            Node otherNode = (Node) otherSession.getItem(srcPath);

            testRootNode.getSession().getWorkspace().move(srcPath, destPath);

            otherNode.refresh(false);
            try {
                assertTrue(n.isSame(otherNode));
            } catch (InvalidItemStateException e) {
                // ok as well.
            }
            try {
                assertTrue(otherNode.isSame(n));
            } catch (InvalidItemStateException e) {
                // ok as well.
            }
        } finally {
            otherSession.logout();
        }
    }

    public void testIsSameNode7() throws RepositoryException {

        Node n = testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = n.addNode(nodeName2);
        Node n3 = n2.addNode(nodeName3);
        testRootNode.save();

        n.addMixin(mixReferenceable);
        testRootNode.save();

        Session otherSession = getHelper().getReadOnlySession();
        try {
            Node otherNode3 = (Node) otherSession.getItem(n3.getPath());

            assertTrue(otherNode3.isSame(n3));
            Node parent = otherNode3.getParent();
            assertTrue(parent.isSame(n2));
            parent = parent.getParent();
            assertTrue(parent.isSame(n));
            parent = parent.getParent();
            assertTrue(testRootNode.isSame(parent));
        } finally {
            otherSession.logout();
        }
    }

    public void testSameInstanceIsSame() throws RepositoryException {
        assertTrue(testRootNode.isSame(testRootNode));

        Property p = testRootNode.getProperty(jcrPrimaryType);
        assertTrue(p.isSame(p));
    }

    public void testNewNodeIsSame() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        assertTrue(n.isSame(n));

        n.remove();
        Node n2 = testRootNode.addNode(nodeName1, testNodeType);

        try {
            assertFalse(n2.isSame(n));
        } catch (InvalidItemStateException e) {
            // ok as well
        }
        try {
            assertFalse(n.isSame(n2));
        } catch (InvalidItemStateException e) {
            // ok as well
        }
    }

    public void testNewPropertyIsSame() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        Property p = n.setProperty(propertyName1, "anyValue");

        assertTrue(p.isSame(p));
    }

    public void testNewItemFromDifferentSessions() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        Property p = n.setProperty(propertyName1, "anyValue");

        Session s2 = getHelper().getReadWriteSession();
        try {
            Node trn = (Node) s2.getItem(testRootNode.getPath());
            Node n2 = trn.addNode(nodeName1, testNodeType);
            Property p2 = n2.setProperty(propertyName1, "anyValue");

            assertFalse(n.isSame(n2));
            assertFalse(n2.isSame(n));
            assertFalse(p.isSame(p2));
            assertFalse(p2.isSame(p));
        } finally {
            s2.logout();
        }
    }

    public void testDifferentItemType() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        Property p = n.setProperty(propertyName1, "anyValue");

        assertFalse(p.isSame(n));
        assertFalse(n.isSame(p));
    }

    public void testShadowingItems() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);

        Property p = n.setProperty(propertyName1, "anyValue");
        testRootNode.save();

        testRootNode.getSession().move(n.getPath(), n2.getPath() + "/destination");

        Node replaceNode = testRootNode.addNode(nodeName1, testNodeType);
        Property replaceProp = replaceNode.setProperty(propertyName1, "anyValue");

        assertFalse(replaceNode.isSame(n));
        assertFalse(replaceProp.isSame(p));
    }

    public void testShadowingItems2() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        Property p = n.setProperty(propertyName1, "anyValue");

        testRootNode.getSession().move(n.getPath(), n2.getPath() + "/destination");

        Node replaceNode = testRootNode.addNode(nodeName1, testNodeType);
        Property replaceProp = replaceNode.setProperty(propertyName1, "anyValue");

        assertFalse(replaceNode.isSame(n));
        assertFalse(replaceProp.isSame(p));
    }

    public void testShadowingItems3() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        Property p = n.setProperty(propertyName1, "anyValue");
        testRootNode.save();

        p.remove();
        Property p2 = n.setProperty(propertyName1, "anyValue");
        try {
            assertFalse(p2.isSame(p));
        } catch (InvalidItemStateException e) {
            // ok as well.
        }
    }

    /**
    * 283 specific test where node and prop with same name can be siblings.
    *
    * @throws RepositoryException
    */
    /*
    public void testIsSameDifferentItemType() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();

        Session s2 = helper.getReadWriteSession();
        try {
            Node trn = (Node) s2.getItem(testRootNode.getPath());
            Property p = trn.setProperty(nodeName1, "anyValue");
            trn.save();

            assertFalse(n.isSame(p));

        } finally {
            s2.logout();
        }
    }
    */


}