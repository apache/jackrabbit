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
 * <code>OnParentVersionIgnoreTest</code> tests the OnParentVersion {@link OnParentVersionAction#IGNORE IGNORE}
 * behaviour.
 *
 * @test
 * @sources OnParentVersionIgnoreTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.OnParentVersionIgnoreTest
 * @keywords versioning
 */
public class OnParentVersionIgnoreTest extends AbstractOnParentVersionTest {

    protected void setUp() throws Exception {
        OPVAction = OnParentVersionAction.IGNORE;
        super.setUp();
    }

    /**
     * Test the restore of a OnParentVersion-IGNORE property
     *
     * @throws javax.jcr.RepositoryException
     */
    public void testRestoreProp() throws RepositoryException {

        Node propParent = p.getParent();
        propParent.checkout();
        Version v = propParent.checkin();
        propParent.checkout();

        p.setValue(newPropValue);
        p.save();

        propParent.restore(v, false);

        assertEquals("On restore of a OnParentVersion-IGNORE property P, the current value of P must be left unchanged.", p.getString(), newPropValue);
    }

    /**
     * Test the restore of a OnParentVersion-Ignore node
     *
     * @throws javax.jcr.RepositoryException
     */
    public void testRestoreNode() throws RepositoryException {

        versionableNode.checkout();
        Version v = versionableNode.checkin();
        versionableNode.checkout();

        // add 'ignore' child
        String childName = addChildNode(OPVAction).getName();
        versionableNode.save();
        
        versionableNode.restore(v, false);

        if (!versionableNode.hasNode(childName)) {
            fail("On restore of a OnParentVersion-Ignore child node, the node needs to be untouched.");
        }
    }

}