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

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;

/**
 * <code>AddNodeTest</code>...
 */
public class AddNodeTest extends AbstractJCRTest {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(AddNodeTest.class);

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
}