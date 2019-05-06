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
package org.apache.jackrabbit.core;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;

/**
 * <code>ConcurrentModificationWithSNSTest</code> checks if interleaving node
 * modifications with same name siblings do not throw InvalidItemStateException.
 */
public class ConcurrentModificationWithSNSTest extends ConcurrentModificationBase {

    protected void setUp() throws Exception {
        super.setUp();
        testRootNode.addNode("A");
        testRootNode.addNode("A");
        testRootNode.addNode("A");
        superuser.save();
    }

    public void testAddAdd() throws Exception {
        testRootNode.addNode("A");
        session.getNode(testRoot).addNode("A");
        superuser.save();
        try {
            session.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw InvalidItemStateException");
        }
    }

    public void testAddRemove() throws Exception {
        testRootNode.addNode("A");
        session.getNode(testRoot).getNode("A[2]").remove();
        superuser.save();
        try {
            session.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw InvalidItemStateException");
        }
    }

    public void testRemoveAdd() throws Exception {
        testRootNode.getNode("A[2]").remove();
        session.getNode(testRoot).addNode("A");
        superuser.save();
        try {
            session.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw InvalidItemStateException");
        }
    }

    public void testRemoveRemove() throws Exception {
        testRootNode.getNode("A[1]").remove();
        session.getNode(testRoot).getNode("A[3]").remove();
        superuser.save();
        try {
            session.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw InvalidItemStateException");
        }
    }

    public void testSNSNotAllowed() throws Exception {
        cleanUpTestRoot(superuser);
        Node f = testRootNode.addNode("folder", "nt:folder");
        superuser.save();
        f.addNode("A", "nt:folder");
        session.getNode(f.getPath()).addNode("A", "nt:folder");
        superuser.save();
        try {
            session.save();
            fail("InvalidItemStateException expected");
        } catch (InvalidItemStateException e) {
            // expected
        }
    }
}
