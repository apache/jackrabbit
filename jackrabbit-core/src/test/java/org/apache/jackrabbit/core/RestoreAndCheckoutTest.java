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
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * <code>RestoreAndCheckoutTest</code> tests for JCR-1197
 * "Node.restore() may throw InvalidItemStateException".
 *
 */
public class RestoreAndCheckoutTest extends AbstractJCRTest {

    private static final int NODES_COUNT = 10;

    public void testRestoreAndCheckout() throws RepositoryException {
        Session session = getHelper().getSuperuserSession();

        Node rootNode = session.getRootNode();
        Node myRoot = rootNode.addNode("myRoot");
        myRoot.addMixin("mix:versionable");
        rootNode.save();
        myRoot.checkin();

        // create n child and grandchild versionable nodes
        for (int i = 0; i < NODES_COUNT; i++) {
            myRoot.checkout();
            Node childNode = myRoot.addNode("child" + i);
            childNode.addMixin("mix:versionable");
            Node grandChildNode = childNode.addNode("grandChild");
            grandChildNode.addMixin("mix:versionable");
            myRoot.save();
            grandChildNode.checkin();
            childNode.checkin();
            myRoot.checkin();
        }

        // restore child, then restore/checkout grandchild nodes
        for (int i = 0; i < NODES_COUNT; i++) {
            Node childNode = myRoot.getNode("child" + i);
            childNode.restore("1.0", false);
            Node grandChildNode = childNode.getNode("grandChild");
            grandChildNode.restore("1.0", false);
            // critical location regarding item state manager caching (see
            // JCR-1197)
            grandChildNode.checkout();
            grandChildNode.checkin();
        }

        session.logout();
    }
}
