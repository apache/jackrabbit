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

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.Set;
import java.util.HashSet;

/**
 * <code>ReferencesTest</code> contains the test cases for the references.
 *
 */
public class ReferencesTest extends AbstractJCRTest {

    /**
     * Tests Node.getReferences()
     */
    public void testReferences() throws RepositoryException, NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        ensureMixinType(n1, mixReferenceable);

        // with some impls. the mixin type has only affect upon save
        testRootNode.getSession().save();

        // make sure the node is now referenceable
        assertTrue("test node should be mix:referenceable", n1.isNodeType(mixReferenceable));

        // create references: n2.p1 -> n1
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);

        Value[] values = new Value[]{superuser.getValueFactory().createValue(n1)};

        // abort test if the repository does not allow setting
        // reference properties on this node
        ensureCanSetProperty(n2, propertyName1, values);

        Property p1 = n2.setProperty(propertyName1, values);
        testRootNode.getSession().save();
        PropertyIterator iter = n1.getReferences();
        if (iter.hasNext()) {
            assertEquals("Wrong referer", iter.nextProperty().getPath(), p1.getPath());
        } else {
            fail("no referer");
        }

        // create references: n3.p1 -> n1
        Node n3 = testRootNode.addNode(nodeName3, testNodeType);
        n3.setProperty(propertyName1, n1);
        testRootNode.getSession().save();
        iter = n1.getReferences();
        while (iter.hasNext()) {
            Property p = iter.nextProperty();
            if (n2 != null && p.getParent().getPath().equals(n2.getPath())) {
                n2 = null;
            } else if (n3 != null && p.getParent().getPath().equals(n3.getPath())) {
                n3 = null;
            } else {
                fail("too many referers: " + p.getPath());
            }
        }
        if (n2 != null) {
            fail("referer not in references set: " + n2.getPath());
        }
        if (n3 != null) {
            fail("referer not in references set: " + n3.getPath());
        }

        // remove reference n3.p1 -> n1
        testRootNode.getNode(nodeName3).getProperty(propertyName1).remove();
        testRootNode.getSession().save();
        iter = n1.getReferences();
        if (iter.hasNext()) {
            assertEquals("Wrong referer", iter.nextProperty().getParent().getPath(), testRootNode.getNode(nodeName2).getPath());
        } else {
            fail("no referer");
        }
        if (iter.hasNext()) {
            fail("too many referers: " + iter.nextProperty().getPath());
        }

        // remove reference n2.p1 -> n1
        testRootNode.getNode(nodeName2).getProperty(propertyName1).setValue(new Value[0]);
        testRootNode.getSession().save();
        iter = n1.getReferences();
        if (iter.hasNext()) {
            fail("too many referers: " + iter.nextProperty().getPath());
        }
    }

    /**
     * Tests Node.getReferences(String)
     */
    public void testGetReferencesWithName() throws RepositoryException, NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        ensureMixinType(n1, mixReferenceable);

        // with some impls. the mixin type has only affect upon save
        testRootNode.getSession().save();

        // make sure the node is now referenceable
        assertTrue("test node should be mix:referenceable", n1.isNodeType(mixReferenceable));

        // create references:
        // n2.p1 -> n1
        // n2.p2 -> n1
        // n3.p1 -> n1
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        Node n3 = testRootNode.addNode(nodeName3, testNodeType);

        Value[] values = new Value[]{superuser.getValueFactory().createValue(n1)};

        // abort test if the repository does not allow setting
        // reference properties on this node
        ensureCanSetProperty(n2, propertyName1, values);
        ensureCanSetProperty(n2, propertyName2, values);
        ensureCanSetProperty(n3, propertyName1, values);

        Property p1 = n2.setProperty(propertyName1, values);
        Property p2 = n2.setProperty(propertyName2, values);
        Property p3 = n3.setProperty(propertyName1, n1);
        testRootNode.getSession().save();

        // get references with name propertyName1
        // (should return p1 and p3))
        PropertyIterator iter = n1.getReferences(propertyName1);
        Set<String> results = new HashSet<String>();
        while (iter.hasNext()) {
            results.add(iter.nextProperty().getPath());
        }
        assertEquals("wrong number of references reported", 2, results.size());
        assertTrue("missing reference property: " + p1.getPath(), results.contains(p1.getPath()));
        assertTrue("missing reference property: " + p3.getPath(), results.contains(p3.getPath()));

        // get references with name propertyName2
        // (should return p2))
        iter = n1.getReferences(propertyName2);
        results.clear();
        while (iter.hasNext()) {
            results.add(iter.nextProperty().getPath());
        }
        assertEquals("wrong number of references reported", 1, results.size());
        assertTrue("missing reference property: " + p2.getPath(), results.contains(p2.getPath()));

        // remove reference n3.p1 -> n1
        testRootNode.getNode(nodeName3).getProperty(propertyName1).remove();
        testRootNode.getSession().save();

        // get references with name propertyName1
        // (should return p1))
        iter = n1.getReferences(propertyName1);
        results.clear();
        while (iter.hasNext()) {
            results.add(iter.nextProperty().getPath());
        }
        assertEquals("wrong number of references reported", 1, results.size());
        assertTrue("missing reference property: " + p1.getPath(), results.contains(p1.getPath()));

        // remove reference n2.p1 -> n1
        p1.remove();
        testRootNode.getSession().save();

        // get references with name propertyName1
        // (should nothing))
        iter = n1.getReferences(propertyName1);
        results.clear();
        while (iter.hasNext()) {
            results.add(iter.nextProperty().getPath());
        }
        assertEquals("wrong number of references reported", 0, results.size());
    }

    /**
     * Tests Property.getNode();
     */
    public void testReferenceTarget() throws RepositoryException, NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        ensureMixinType(n1, mixReferenceable);

        // with some impls. the mixin type has only affect upon save
        testRootNode.getSession().save();

        // make sure the node is now referenceable
        assertTrue("test node should be mix:referenceable", n1.isNodeType(mixReferenceable));

        // create references: n2.p1 -> n1
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);

        // abort test if the repository does not allow setting
        // reference properties on this node
        ensureCanSetProperty(n2, propertyName1, n2.getSession().getValueFactory().createValue(n1));

        n2.setProperty(propertyName1, n1);
        testRootNode.getSession().save();
        assertEquals("Wrong reference target.", n2.getProperty(propertyName1).getNode().getUUID(), n1.getUUID());
        n2.remove();
        testRootNode.getSession().save();
    }

    /**
     * Tests changing a reference property
     */
    public void testAlterReference() throws RepositoryException, NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        ensureMixinType(n1, mixReferenceable);
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        ensureMixinType(n2, mixReferenceable);

        // with some impls. the mixin type has only affect upon save
        testRootNode.getSession().save();

        // make sure the nodes are now referenceable
        assertTrue("test node should be mix:referenceable", n1.isNodeType(mixReferenceable));
        assertTrue("test node should be mix:referenceable", n2.isNodeType(mixReferenceable));

        // create references: n3.p1 -> n1
        Node n3 = testRootNode.addNode(nodeName3, testNodeType);

        // abort test if the repository does not allow setting
        // reference properties on this node
        ensureCanSetProperty(n3, propertyName1, n3.getSession().getValueFactory().createValue(n1));

        n3.setProperty(propertyName1, n1);
        testRootNode.getSession().save();
        assertEquals("Wrong reference target.", n3.getProperty(propertyName1).getNode().getUUID(), n1.getUUID());
        PropertyIterator iter = n1.getReferences();
        if (iter.hasNext()) {
            assertEquals("Wrong referer", iter.nextProperty().getParent().getPath(), n3.getPath());
        } else {
            fail("no referer");
        }
        if (iter.hasNext()) {
            fail("too many referers: " + iter.nextProperty().getPath());
        }
        // change reference: n3.p1 -> n2
        n3.setProperty(propertyName1, n2);
        n3.save();
        assertEquals("Wrong reference target.", n3.getProperty(propertyName1).getNode().getUUID(), n2.getUUID());
        iter = n1.getReferences();
        if (iter.hasNext()) {
            fail("too many referers: " + iter.nextProperty().getPath());
        }
        iter = n2.getReferences();
        if (iter.hasNext()) {
            assertEquals("Wrong referer", iter.nextProperty().getParent().getPath(), n3.getPath());
        } else {
            fail("no referers");
        }
        if (iter.hasNext()) {
            fail("too many referers: " + iter.nextProperty().getPath());
        }

        // clear reference by overwriting by other type
        n3.setProperty(propertyName1, "Hello, world.");
        n3.save();
        iter = n2.getReferences();
        if (iter.hasNext()) {
            fail("too many referers: " + iter.nextProperty().getPath());
        }
    }

    public void testNonReferenceable() throws RepositoryException, NotExecutableException {
        Node nonReferenceable = null;
        if (testRootNode.isNodeType(mixReferenceable)) {
            Node child = testRootNode.addNode(nodeName1, testNodeType);
            superuser.save();
            if (!child.isNodeType(mixReferenceable)) {
                nonReferenceable = child;
            }
        } else {
            nonReferenceable = testRootNode;
        }

        if (nonReferenceable == null) {
            throw new NotExecutableException("Test node is referenceable.");
        }

        // getReferences must return an empty iterator and must not throw.        
        assertFalse(nonReferenceable.getReferences().hasNext());
    }
}
