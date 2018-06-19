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
package org.apache.jackrabbit.jcr2spi;

import java.io.ByteArrayInputStream;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.lock.LockException;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * <code>AddNodeTest</code>...
 */
public class AddNodeTest extends AbstractJCRTest {

    /**
     * Writing to a locked node must throw LockException even if the lock
     * isn't detected withing Jcr2Spi.
     *
     * @throws Exception
     * @see <a href="https://issues.apache.org/jira/browse/JCR-2585">JCR-2585</a>
     */
    public void testAddNodeOnLocked() throws Exception {
        Session s = getHelper().getSuperuserSession();
        try {
            Node node = s.getNode(testRootNode.getPath());
            Node n = node.addNode(nodeName1);
            n.setProperty(propertyName1, "value");

            testRootNode.lock(true, true);

            s.save();
        } catch (LockException e) {
            // success
        } finally {
            s.logout();
        }
    }

    public void testAddNodeNonASCII() throws Exception {
        String testName = "test - \u20ac";
        Session s = getHelper().getSuperuserSession();
        try {
            Node node = s.getNode(testRootNode.getPath());
            Node n = node.addNode(testName, "nt:file");
            Node c = n.addNode("jcr:content", "nt:resource");
            c.setProperty("jcr:data", s.getValueFactory().createBinary(new ByteArrayInputStream("hello world".getBytes("UTF-8"))));
            s.save();
        } finally {
            s.logout();
        }

        Session s2 = getHelper().getReadOnlySession();
        try {
            Node node = s2.getNode(testRootNode.getPath()).getNode(testName);
            assertEquals(testName, node.getName());
        } finally {
            s2.logout();
        }
    }
}