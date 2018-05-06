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
package org.apache.jackrabbit.core.version;

import javax.jcr.Node;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Test case for JCR-2930
 */
public class RestoreNodeWithSNSTest extends AbstractJCRTest {

    public void testRestoreWithSNS() throws Exception {
        
        int childCount = 5;
        
        // create a test node with /childCount/ children with the same name
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixVersionable);
        for (int i = 0; i < childCount; i++) {
            Node child = n.addNode(nodeName2);
            child.setProperty("name", nodeName2 + i);
        }
        testRootNode.getSession().save();

        // check the number of children
        assertEquals(childCount, n.getNodes().getSize());

        VersionManager vm = testRootNode.getSession().getWorkspace()
                .getVersionManager();
        vm.checkin(n.getPath());

        // modify one child
        vm.checkout(n.getPath());
        n.getNode(nodeName2).setProperty("name", "modified");
        testRootNode.getSession().save();

        // check the number of children again
        assertEquals(childCount, n.getNodes().getSize());

        // restore base versiob
        Version baseVersion = vm.getBaseVersion(n.getPath());
        vm.restore(baseVersion, true);

        n.getSession().refresh(false);

        // check the number of children again
        assertEquals(childCount, n.getNodes().getSize());
    }
}
