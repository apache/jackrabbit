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

import javax.jcr.Node;
import javax.jcr.InvalidItemStateException;

/**
 * <code>ConcurrentAddRemovePropertyTest</code> checks if concurrently adding
 * and removing properties does not throw an InvalidItemStateException.
 */
public class ConcurrentAddRemovePropertyTest extends ConcurrentModificationBase {

    public void testAdd() throws Exception {
        Node n = testRootNode.addNode(nodeName1);
        superuser.save();
        n.setProperty(propertyName1, "foo");
        session.getNode(testRoot).getNode(nodeName1).setProperty(propertyName2, "bar");
        superuser.save();
        try {
            session.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw InvalidItemStateException");
        }
    }

    public void testAddSameName() throws Exception {
        Node n = testRootNode.addNode(nodeName1);
        superuser.save();
        n.setProperty(propertyName1, "foo");
        session.getNode(testRoot).getNode(nodeName1).setProperty(propertyName1, "bar");
        superuser.save();
        try {
            session.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw InvalidItemStateException");
        }
        assertEquals("bar", n.getProperty(propertyName1).getString());
    }

    public void testRemove() throws Exception {
        Node n = testRootNode.addNode(nodeName1);
        n.setProperty(propertyName1, "foo");
        n.setProperty(propertyName2, "bar");
        superuser.save();
        n.getProperty(propertyName1).remove();
        session.getNode(testRoot).getNode(nodeName1).getProperty(propertyName2).remove();
        superuser.save();
        try {
            session.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw InvalidItemStateException");
        }
    }

    public void testRemoveSameName() throws Exception {
        Node n = testRootNode.addNode(nodeName1);
        n.setProperty(propertyName1, "foo");
        superuser.save();
        n.getProperty(propertyName1).remove();
        session.getNode(testRoot).getNode(nodeName1).getProperty(propertyName1).remove();
        superuser.save();
        try {
            session.save();
        } catch (InvalidItemStateException e) {
            fail("must not throw InvalidItemStateException");
        }
    }
}
