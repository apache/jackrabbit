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

import javax.jcr.*;

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
        // with some impls. the mixin type has only affect upon save
        testRootNode.save();

        // create references: n2.p1 -> n1
	Node n2 = testRootNode.addNode(nodeName2, testNodeType);
	Property p1 = n2.setProperty(propertyName1, new Value[]{superuser.getValueFactory().createValue(n1)});
	testRootNode.save();
	PropertyIterator iter = n1.getReferences();
	if (iter.hasNext()) {
	    assertEquals("Wrong referer", iter.nextProperty().getPath(), p1.getPath());
	} else {
	    fail("no referer");
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
	testRootNode.save();
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
	testRootNode.save();
	iter = n1.getReferences();
	if (iter.hasNext()) {
	    fail("too many referers: " + iter.nextProperty().getPath());
	}
    }

    /**
     * Tests Property.getNode();
     */
    public void testReferenceTarget() throws RepositoryException {
	Node n1 = testRootNode.addNode(nodeName1, testNodeType);
	n1.addMixin(mixReferenceable);
        // with some impls. the mixin type has only affect upon save
        testRootNode.save();

        // create references: n2.p1 -> n1
	Node n2 = testRootNode.addNode(nodeName2, testNodeType);
	n2.setProperty(propertyName1, n1);
	testRootNode.save();
	assertEquals("Wrong reference target.", n2.getProperty(propertyName1).getNode(), n1);
	n2.remove();
	testRootNode.save();
    }

    /**
     * Tests changing a reference property
     */
    public void testAlterReference() throws RepositoryException {
	Node n1 = testRootNode.addNode(nodeName1, testNodeType);
	n1.addMixin(mixReferenceable);
	Node n2 = testRootNode.addNode(nodeName2, testNodeType);
	n2.addMixin(mixReferenceable);
        // with some impls. the mixin type has only affect upon save
        testRootNode.save();

        // create references: n3.p1 -> n1
	Node n3 = testRootNode.addNode(nodeName3, testNodeType);
	n3.setProperty(propertyName1, n1);
	testRootNode.save();
	assertEquals("Wrong reference target.", n3.getProperty(propertyName1).getNode(), n1);
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
	assertEquals("Wrong reference target.", n3.getProperty(propertyName1).getNode(), n2);
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
}
