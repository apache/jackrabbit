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

public final class ConcurrentAddRemoveMoveTest extends ConcurrentModificationBase {

    /**
     * {@inheritDoc}
     */
    public void setUp() throws Exception {
        super.setUp();
        testRootNode.addNode("A").addNode("B");
        testRootNode.addNode("C");
        testRootNode.getSession().save();
    }

    public void testAddWithMoveFrom() throws Exception {
        testRootNode.getNode("A").addNode("D");
        session.move(testRoot + "/A/B", testRoot + "/C/B");

        superuser.save();

        try {
            session.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }

    public void testAddWithMoveTo() throws Exception {
        testRootNode.getNode("A").addNode("D");
        session.move(testRoot + "/C", testRoot + "/A/C");

        superuser.save();

        try {
            session.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }

    public void testRemoveWithMoveFrom() throws Exception {
        Node d = testRootNode.getNode("A").addNode("D");
        superuser.save();
        d.remove();
        session.move(testRoot + "/A/B", testRoot + "/C/B");

        superuser.save();

        try {
            session.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }

    public void testRemoveWithMoveTo() throws Exception {
        Node d = testRootNode.getNode("A").addNode("D");
        superuser.save();
        d.remove();
        session.move(testRoot + "/C", testRoot + "/A/C");

        superuser.save();

        try {
            session.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }

    public void testMoveFromWithAdd() throws Exception {
        superuser.move(testRoot + "/A/B", testRoot + "/C/B");
        session.getNode(testRoot).getNode("A").addNode("D");

        superuser.save();

        try {
            session.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }

    public void testMoveToWithAdd() throws Exception {
        superuser.move(testRoot + "/C", testRoot + "/A/C");
        session.getNode(testRoot).getNode("A").addNode("D");

        superuser.save();

        try {
            session.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }

    public void testMoveFromWithRemove() throws Exception {
        Node d = session.getNode(testRoot).getNode("A").addNode("D");
        session.save();

        superuser.move(testRoot + "/A/B", testRoot + "/C/B");
        d.remove();

        superuser.save();

        try {
            session.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }

    public void testMoveToWithRemove() throws Exception {
        Node d = session.getNode(testRoot).getNode("A").addNode("D");
        session.save();

        superuser.move(testRoot + "/C", testRoot + "/A/C");
        d.remove();

        testRootNode.getSession().save();

        try {
            session.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }

    //-------------------------< concurrent add >-------------------------------

    public void testAddAdd() throws Exception {
        testRootNode.getNode("A").addNode("D");
        session.getNode(testRoot).getNode("A").addNode("E");

        superuser.save();

        try {
            session.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }

    //-------------------------< concurrent remove >----------------------------

    public void testRemoveRemove() throws Exception {
        Node d = testRootNode.getNode("A").addNode("D");
        superuser.save();
        d.remove();
        session.getNode(testRoot).getNode("A").getNode("B").remove();

        superuser.save();

        try {
            session.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw exception");
        }
    }
}
