/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.jackrabbit.test.api;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.*;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * <code>ReferencesTest</code> contains the test cases for the references.
 *
 * @test
 * @sources ReferencesTest.java
 * @executeClass org.apache.jackrabbit.test.api.ReferencesTest
 * @keywords level2
 */
public class ReferencesTest extends AbstractJCRTest {

    /**
     * Tests Node.getReferences()
     */
    public void testReferences() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
	n1.addMixin(mixReferenceable);
	// create references: n2.p1 -> n1
	Node n2 = testRootNode.addNode(nodeName2, testNodeType);
	n2.setProperty(propertyName1, new Value[]{new ReferenceValue(n1)});
	testRootNode.save();
	PropertyIterator iter = n1.getReferences();
	if (iter.hasNext()) {
	    assertEquals("Wrong referencer", iter.nextProperty().getParent().getPath(), n2.getPath());
	} else {
	    fail("no referencer");
	}

	// create references: n3.p1 -> n1
	Node n3 = testRootNode.addNode(nodeName3, testNodeType);
	n3.setProperty(propertyName1, n1);
	testRootNode.save();
	iter = n1.getReferences();
	while (iter.hasNext()) {
	    Property p = iter.nextProperty();
	    if (n2 != null && p.getParent().getPath().equals(n2.getPath())) {
		n2 = null;
	    } else if (n3 != null && p.getParent().getPath().equals(n3.getPath())) {
		n3 = null;
	    } else {
		fail("too many referencers: " + p.getPath());
	    }
	}
	if (n2 != null) {
	    fail("referencer not in references set: " + n2.getPath());
	}
	if (n3 != null) {
	    fail("referencer not in references set: " + n3.getPath());
	}

	// remove reference n3.p1 -> n1
	testRootNode.getNode(nodeName3).getProperty(propertyName1).remove();
	testRootNode.save();
	iter = n1.getReferences();
	if (iter.hasNext()) {
	    assertEquals("Wrong referencer", iter.nextProperty().getParent().getPath(), testRootNode.getNode(nodeName2).getPath());
	} else {
	    fail("no referencer");
	}
	if (iter.hasNext()) {
	    fail("too many referencers: " + iter.nextProperty().getPath());
	}

	// remove reference n2.p1 -> n1
	testRootNode.getNode(nodeName2).getProperty(propertyName1).setValue(new Value[0]);
	testRootNode.save();
	iter = n1.getReferences();
	if (iter.hasNext()) {
	    fail("too many referencers: " + iter.nextProperty().getPath());
	}
    }

    /**
     * Test Property.getNode();
     */
    public void testReferenceTarget() throws RepositoryException {
	Node n1 = testRootNode.addNode(nodeName1, testNodeType);
	n1.addMixin(mixReferenceable);
	// create references: n2.p1 -> n1
	Node n2 = testRootNode.addNode(nodeName2, testNodeType);
	n2.setProperty(propertyName1, n1);
	testRootNode.save();
	assertEquals("Wrong reference target.", n2.getProperty(propertyName1).getNode(), n1);
	n2.remove();
	testRootNode.save();
    }

    /**
     * Test changing a refrence property
     */
    public void testAlterReference() throws RepositoryException {
	Node n1 = testRootNode.addNode(nodeName1, testNodeType);
	n1.addMixin(mixReferenceable);
	Node n2 = testRootNode.addNode(nodeName2, testNodeType);
	n2.addMixin(mixReferenceable);
	// create references: n3.p1 -> n1
	Node n3 = testRootNode.addNode(nodeName3, testNodeType);
	n3.setProperty(propertyName1, n1);
	testRootNode.save();
	assertEquals("Wrong reference target.", n3.getProperty(propertyName1).getNode(), n1);
	PropertyIterator iter = n1.getReferences();
	if (iter.hasNext()) {
	    assertEquals("Wrong referencer", iter.nextProperty().getParent().getPath(), n3.getPath());
	} else {
	    fail("no referencer");
	}
	if (iter.hasNext()) {
	    fail("too many referencers: " + iter.nextProperty().getPath());
	}
	// change reference: n3.p1 -> n2
	n3.setProperty(propertyName1, n2);
	n3.save();
	assertEquals("Wrong reference target.", n3.getProperty(propertyName1).getNode(), n2);
	iter = n1.getReferences();
	if (iter.hasNext()) {
	    fail("too many referencers: " + iter.nextProperty().getPath());
	}
	iter = n2.getReferences();
	if (iter.hasNext()) {
	    assertEquals("Wrong referencer", iter.nextProperty().getParent().getPath(), n3.getPath());
	} else {
	    fail("no referencer");
	}
	if (iter.hasNext()) {
	    fail("too many referencers: " + iter.nextProperty().getPath());
	}

	// clear reference by overwriting by other type
	n3.setProperty(propertyName1, "Hello, world.");
	n3.save();
	iter = n2.getReferences();
	if (iter.hasNext()) {
	    fail("too many referencers: " + iter.nextProperty().getPath());
	}

    }
}