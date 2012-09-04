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

import java.io.File;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * <code>RemoveAddNodeWithUUIDTest</code> check if no 'overwriting cached entry'
 * warnings are written to the log when a node is re-created with the same UUID.
 * See: JCR-3419
 */
public class RemoveAddNodeWithUUIDTest extends AbstractJCRTest {

    public void testRemoveAdd() throws Exception {
        Tail tail = Tail.start(new File("target", "jcr.log"), "overwriting cached entry");
        try {
            Node test = testRootNode.addNode("test");
            test.setProperty("prop", 1);
            test.addMixin(mixReferenceable);
            superuser.save();
            String testId = test.getIdentifier();

            Session s = getHelper().getSuperuserSession();
            try {
                Node testOther = s.getNode(test.getPath());

                test.remove();
                test = ((NodeImpl) testRootNode).addNodeWithUuid("test", testId);
                test.setProperty("prop", 2);
                superuser.save();

                // now test node instance is not accessible anymore for s
                try {
                    testOther.getProperty("prop");
                    fail("test node instance must not be accessibly anymore");
                } catch (InvalidItemStateException e) {
                    // expected
                }
                // getting it again must succeed and return updated property value
                testOther = s.getNode(test.getPath());
                assertEquals("property outdated", 2, testOther.getProperty("prop").getLong());

                assertFalse("detected 'overwriting cached entry' messages in log", tail.getLines().iterator().hasNext());
            } finally {
                s.logout();
            }
        } finally {
            tail.close();
        }
    }
}
