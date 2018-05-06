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
package org.apache.jackrabbit.test.api.version.simple;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionManager;

/**
 * <code>SVCheckoutTest</code> covers tests related to {@link
 * Node#checkout()} and {@link Node#isCheckedOut()} of simple versionable
 * nodes.
 *
 */
public class CheckoutTest extends AbstractVersionTest {

    protected void setUp() throws Exception {
         super.setUp();

         VersionManager versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
         String path = versionableNode.getPath();
         if (!versionManager.isCheckedOut(path)) {
             fail("A versionable node must be checked-out after persistent creation.");
         }
         if (!versionableNode.isCheckedOut()) {
             fail("A versionable node must be checked-out after persistent creation.");
         }
        try {
            versionManager.checkin(path);
        } catch (RepositoryException e) {
            cleanUp();
            throw e;
        }
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
      * Test if VersionManager.isCheckedOut(P) returns true if P is the
      * absolute path of a versionable node that has been checked out before.
      */
     public void testIsCheckedOutJcr2() throws RepositoryException {
         VersionManager versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
         String path = versionableNode.getPath();
         versionManager.checkout(path);
         assertTrue("After successfully calling VersionManager.checkout(P) with P denoting the absolute path of a versionable node, VersionManager.isCheckedOut(P) must return true.", versionManager.isCheckedOut(path));
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
      * Test calling VersionManager.isCheckedOut(P) with P denoting the
      * absolute path of a non-versionable node.
      */
     public void testIsCheckedOutNonVersionableNodeJcr2() throws RepositoryException {
         VersionManager versionManager = nonVersionableNode.getSession().getWorkspace().getVersionManager();
         String path = nonVersionableNode.getPath();
         boolean isCheckedOut = versionManager.isCheckedOut(path);
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
             String parentPath = vParent.getPath();
             if (versionManager.isCheckedOut(parentPath)) {
                 assertTrue("VersionManager.isCheckedOut(P) must return true if P denotes the absolute path of a non-versionable node whose nearest versionable ancestor is checked-out.", isCheckedOut);
             } else {
                 assertFalse("VersionManager.isCheckedOut(P) must return false if P denotes the absolute path of a non-versionable node whose nearest versionable ancestor is checked-in.", isCheckedOut);
             }
         } else {
             assertTrue("VersionManager.isCheckedOut(P) must return true if P denotes the absolute path of a non-versionable node that has no versionable ancestor", isCheckedOut);
         }
     }

     /**
      * Test calling Node.checkout() on a non-versionable node.
      */
     public void testCheckoutNonVersionableNode() throws RepositoryException {
         try {
             nonVersionableNode.checkout();
             fail("Node.checkout() on a non-versionable node must throw UnsupportedRepositoryOperationException");
         } catch (UnsupportedRepositoryOperationException e) {
             //success
         }
     }

     /**
      * Test calling VersionManager.checkout(P) with P denoting the absolute
      * path of a non-versionable node.
      */
     public void testCheckoutNonVersionableNodeJcr2() throws RepositoryException {
         VersionManager versionManager = nonVersionableNode.getSession().getWorkspace().getVersionManager();
         String path = nonVersionableNode.getPath();
         try {
             versionManager.checkout(path);
             fail("VersionManager.checkout(P) with P denoting the absolute path of a non-versionable node must throw UnsupportedRepositoryOperationException");
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
      * Test if VersionManager.checkout(P) doesn't throw any exception if P
      * denotes the absolute path of a versionable node that has been checked
      * out before.
      */
     public void testCheckoutTwiceDoesNotThrowJcr2() throws RepositoryException {
         VersionManager versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
         String path = versionableNode.getPath();
         versionManager.checkout(path);
         versionManager.checkout(path);
     }
}
