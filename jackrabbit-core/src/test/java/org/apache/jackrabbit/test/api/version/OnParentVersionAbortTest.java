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

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.VersionException;

/**
 * <code>OnParentVersionAbortTest</code> tests the OnParentVersion {@link OnParentVersionAction#ABORT ABORT}
 * behaviour.
 *
 * @test
 * @sources OnParentVersionAbortTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.OnParentVersionComputeTest
 * @keywords versioning
 */
public class OnParentVersionAbortTest extends AbstractOnParentVersionTest {

    protected void setUp() throws Exception {
        OPVAction = OnParentVersionAction.ABORT;
        super.setUp();
    }

    /**
     * Test the restore of a OnParentVersion-ABORT property
     *
     * @throws javax.jcr.RepositoryException
     */
    public void testRestoreProp() throws RepositoryException {
        try {
            p.getParent().checkout();
            p.getParent().checkin();
            fail("On checkin of N which has a property with OnParentVersion ABORT defined, an UnsupportedRepositoryOperationException must be thrown.");
        } catch (VersionException e) {
            // success
        }
    }

    /**
     * Test the restore of a OnParentVersion-ABORT node
     * 
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testRestoreNode() throws RepositoryException, NotExecutableException {
        // create child node with OPV-ABORT behaviour
        addChildNode(OPVAction);
        testRootNode.save();
        try {
            versionableNode.checkin();
            fail("On checkin of N which has a child node with OnParentVersion ABORT defined, an UnsupportedRepositoryOperationException must be thrown.");
        } catch (VersionException e) {
            // success
        }
    }
}