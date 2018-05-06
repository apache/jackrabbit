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
package org.apache.jackrabbit.core.lock;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.lock.Lock;

/**
 * <code>ExtendedLockingTest</code>...
 */
public class ExtendedLockingTest extends AbstractJCRTest {

    public void testRemoveMixLockableFromLockedNode() throws RepositoryException {

        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixLockable);
        testRootNode.save();

        Lock l = n.lock(true, true);

        try {
            n.removeMixin(mixLockable);
            n.save();
            fail("Removing mix:lockable from a locked node must fail.");
        } catch (ConstraintViolationException e) {
            // success
        } finally {
            if (n.isLocked()) {
                n.unlock();
            }
        }
    }
}