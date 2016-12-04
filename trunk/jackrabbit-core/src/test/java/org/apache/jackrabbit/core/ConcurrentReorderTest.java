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

/**
 * <code>ConcurrentReorderTest</code> checks if a reorder interleaved with
 * a modification by another session throws an InvalidItemStateException.
 */
public class ConcurrentReorderTest extends ConcurrentModificationBase {

    protected void setUp() throws Exception {
        super.setUp();
        testRootNode.addNode("A");
        testRootNode.addNode("B");
        testRootNode.addNode("C");
        superuser.save();
    }

    public void testReorderWithAdd() throws Exception {
        testRootNode.orderBefore("C", "A");
        session.getNode(testRoot).addNode("D");
        session.save();
        try {
            superuser.save();
            fail("must throw InvalidItemStateException");
        } catch (InvalidItemStateException e) {
            // expected
        }
    }

    public void testAddWithReorder() throws Exception {
        testRootNode.addNode("D");
        session.getNode(testRoot).orderBefore("C", "A");
        session.save();
        try {
            superuser.save();
            fail("must throw InvalidItemStateException");
        } catch (InvalidItemStateException e) {
            // expected
        }
    }
}
