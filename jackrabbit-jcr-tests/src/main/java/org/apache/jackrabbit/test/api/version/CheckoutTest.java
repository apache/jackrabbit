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

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.ItemNotFoundException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.PropertyIterator;
import javax.jcr.Property;

/**
 * <code>CheckoutTest</code> covers tests related to {@link
 * javax.jcr.Node#checkout()} and {@link javax.jcr.Node#isCheckedOut()}.
 *
 * @test
 * @sources CheckoutTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.CheckoutTest
 * @keywords versioning
 */
public class CheckoutTest extends AbstractVersionTest {

    protected void setUp() throws Exception {
        super.setUp();

        if (!versionableNode.isCheckedOut()) {
            fail("A versionable node must be checked-out after persistent creation.");
        }
        versionableNode.checkin();
    }

    /**
     * Test if Node.isCheckedOut() returns true, if the versionable node has
     * been checked out before.
     */
    public void testIsCheckedOut() throws RepositoryException {
        versionableNode.checkout();
        assertTrue("After calling Node.checkout() a versionable node N, N.isCheckedOut() must return true.", versionableNode.isCheckedOut());
    }

    /**
     * Test calling Node.isCheckedOut() on a non-versionable.
     */
    public void testIsCheckedOutNonVersionableNode() throws RepositoryException {
        boolean isCheckedOut = nonVersionableNode.isCheckedOut();
        Node vParent = null;
        try {
            vParent = nonVersionableNode.getParent();
            while (!vParent.isNodeType(mixVersionable)) {
                vParent = vParent.getParent();
            }
        } catch (ItemNotFoundException e) {
            // root reached.
        }

        if (vParent != null && vParent.isNodeType(mixVersionable)) {
            if (vParent.isCheckedOut()) {
                assertTrue("Node.isCheckedOut() must return true if the node is non-versionable and its nearest versionable ancestor is checked-out.", isCheckedOut);
            } else {
                assertFalse("Node.isCheckedOut() must return false if the node is non-versionable and its nearest versionable ancestor is checked-in.", isCheckedOut);
            }
        } else {
            assertTrue("Node.isCheckedOut() must return true if the node is non-versionable and has no versionable ancestor", isCheckedOut);
        }
    }

    /**
     * Test calling Node.checkout() on a non-versionable node.
     */
    public void testCheckoutNonVersionableNode() throws RepositoryException {
        try {
            nonVersionableNode.checkout();
            fail("Node.checkout() on a non versionable node must throw UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException e) {
            //success
        }
    }

    /**
     * Test if Node.checkout() doesn't throw any exception if the versionable
     * node has been checked out before.
     */
    public void testCheckoutTwiceDoesNotThrow() throws RepositoryException {
        versionableNode.checkout();
        versionableNode.checkout();
    }

    /**
     * Test if Node.checkout() copies the node's jcr:baseVersion to node's
     * jcr:predecessors property (no save required).
     */
    public void testCheckoutCopiesBaseValueToPredecessorProperty() throws RepositoryException {
        Value baseVersionValue = versionableNode.getProperty(jcrBaseVersion).getValue();
        versionableNode.checkout();
        Value[] predecessorsValues = versionableNode.getProperty(jcrPredecessors).getValues();

        // loop over all values of jcr:predecessors property as it's not sure
        // on which position jcr:baseVersion is copied.
        boolean foundBaseVersionProp = false;
        int i = 0;
        while (i < predecessorsValues.length && !foundBaseVersionProp) {
            if (predecessorsValues[i].equals(baseVersionValue)) {
                foundBaseVersionProp = true;
            }
            i++;
        }
        if (!foundBaseVersionProp) {
            fail("After calling Node.checkout() the current value of node's jcr:baseVersion must be copied to node's jcr:predecessors property");
        }
    }
}