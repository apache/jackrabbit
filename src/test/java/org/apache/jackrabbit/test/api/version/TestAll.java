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

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite that includes all testcases for the package
 * <code>javax.jcr.version</code>.
 */
public class TestAll extends TestCase {

    /**
     * Returns a <code>Test</code> suite that executes all tests inside this
     * package.
     *
     * @return a <code>Test</code> suite that executes all tests inside this
     *         package.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite("javax.jcr.version tests");

        suite.addTestSuite(VersionTest.class);
        suite.addTestSuite(VersionHistoryTest.class);
        suite.addTestSuite(VersionStorageTest.class);
        suite.addTestSuite(VersionLabelTest.class);
        suite.addTestSuite(CheckoutTest.class);
        suite.addTestSuite(CheckinTest.class);
        suite.addTestSuite(VersionGraphTest.class);
        suite.addTestSuite(RemoveVersionTest.class);
        suite.addTestSuite(RestoreTest.class);
        suite.addTestSuite(WorkspaceRestoreTest.class);
        suite.addTestSuite(OnParentVersionAbortTest.class);
        suite.addTestSuite(OnParentVersionComputeTest.class);
        suite.addTestSuite(OnParentVersionCopyTest.class);
        suite.addTestSuite(OnParentVersionIgnoreTest.class);
        suite.addTestSuite(OnParentVersionInitializeTest.class);
        suite.addTestSuite(GetReferencesNodeTest.class);
        suite.addTestSuite(GetPredecessorsTest.class);
        suite.addTestSuite(GetCreatedTest.class);
        suite.addTestSuite(GetContainingHistoryTest.class);
        suite.addTestSuite(GetVersionableUUIDTest.class);
        suite.addTestSuite(SessionMoveVersionExceptionTest.class);
        suite.addTestSuite(WorkspaceMoveVersionExceptionTest.class);
        suite.addTestSuite(MergeCancelMergeTest.class);
        suite.addTestSuite(MergeCheckedoutSubNodeTest.class);
        suite.addTestSuite(MergeDoneMergeTest.class);
        suite.addTestSuite(MergeNodeIteratorTest.class);
        suite.addTestSuite(MergeNodeTest.class);
        suite.addTestSuite(MergeNonVersionableSubNodeTest.class);
        suite.addTestSuite(MergeSubNodeTest.class);

        return suite;
    }
}