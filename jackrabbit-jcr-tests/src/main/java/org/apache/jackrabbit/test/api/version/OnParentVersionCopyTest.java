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
package org.apache.jackrabbit.test.api.version;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.Version;

/**
 * <code>OnParentVersionCopyTest</code> tests the OnParentVersion {@link OnParentVersionAction#COPY COPY}
 * behaviour.
 *
 * @test
 * @sources OnParentVersionCopyTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.OnParentVersionCopyTest
 * @keywords versioning
 */
public class OnParentVersionCopyTest extends AbstractOnParentVersionTest {

    String initialNodePath;

    protected void setUp() throws Exception {
        OPVAction = OnParentVersionAction.COPY;
        super.setUp();
    }

    /**
     * Test the restore of a OnParentVersion-COPY property
     *
     * @throws javax.jcr.RepositoryException
     */
    public void testRestoreProp() throws RepositoryException {
        assertEquals("On restore of a OnParentVersion-COPY property P the copy of P stored will be restored, replacing the current P in the workspace.", p.getString(), initialPropValue);
    }

    /**
     * Test the restore of a OnParentVersion-COPY node
     *
     * @throws javax.jcr.RepositoryException
     */
    public void testRestoreNode() throws RepositoryException {
        // prepare for node test
        Node childNode = addChildNode(OPVAction);
        Node nodeParent = childNode.getParent();
        // todo: added next line. correct? -> angela
        nodeParent.save();

        nodeParent.checkout();
        Version v = nodeParent.checkin();

        initialNodePath = childNode.getPath();
        nodeParent.checkout();
        childNode.remove();
        nodeParent.save();

        nodeParent.restore(v, false);

        if (!superuser.itemExists(initialNodePath)) {
            fail("On restore of a OnParentVersion-COPY child node, the node needs to be restored, replacing the current node in the workspace.");
        }
        // todo: add proper comparison of restored node. equals does not work
        // assertEquals("On restore of a OnParentVersion-COPY child node, the node needs to be restored, replacing the current node in the workspace.", childNode, superuser.getItem(initialNodePath));
    }
}