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
package org.apache.jackrabbit.core.integration;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Test case for JCR-43.
 * 
 * @see <a href="https://issues.apache.org/jira/browse/JCR-43">JCR-43</a>
 */
public class RestoreSameNameSiblingTest extends AbstractJCRTest {

    public void testRestoreSNS() throws RepositoryException {
        Session session = getHelper().getSuperuserSession();

        // - Create a node 'a' with nodetype nt:unstructured
        // (defining it's childnodes to show OPV Version behaviour)
        Node node = session.getRootNode().addNode("RestoreSameNameSiblingTest");
        try {
            // - Create a child node 'b'
            node.addNode("test");
            // - Make 'a' versionable (add mixin mix:versionable)
            node.addMixin(mixVersionable);
            session.save();

            // - Checkin/Checkout 'a'
            Version version = node.checkin();
            node.checkout();
            assertEquals(1, node.getNodes("test").getSize());

            // - Restore any version of 'a'
            node.restore(version, true);
            assertEquals(1, node.getNodes("test").getSize());
        } finally {
            node.remove();
            session.save();
            session.logout();
        }
    }

}
