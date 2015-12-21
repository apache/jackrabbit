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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.jackrabbit.test.ConcurrentTestSuite;

/**
 * Test suite that includes all testcases for the Core module.
 */
public class TestAll extends TestCase {

    /**
     * @return a <code>Test</code> suite that executes all tests inside this
     *         package, except the multi-threading related ones.
     */
    public static Test suite() {
        TestSuite suite = new ConcurrentTestSuite("Core tests");

        suite.addTestSuite(ReplacePropertyWhileOthersReadTest.class);
        suite.addTestSuite(CachingHierarchyManagerTest.class);
        suite.addTestSuite(ShareableNodeTest.class);
        suite.addTestSuite(MultiWorkspaceShareableNodeTest.class);
        suite.addTestSuite(TransientRepositoryTest.class);
        suite.addTestSuite(XATest.class);
        suite.addTestSuite(RestoreAndCheckoutTest.class);
        suite.addTestSuite(NodeImplTest.class);
        suite.addTestSuite(RetentionRegistryImplTest.class);
        suite.addTestSuite(InvalidDateTest.class);
        suite.addTestSuite(SessionGarbageCollectedTest.class);
        suite.addTestSuite(ReferencesTest.class);
        suite.addTestSuite(ReplaceTest.class);

        // test related to NodeStateMerger
        suite.addTestSuite(ConcurrentImportTest.class);
        suite.addTestSuite(ConcurrentAddRemoveMoveTest.class);
        suite.addTestSuite(ConcurrentAddRemovePropertyTest.class);
        suite.addTestSuite(ConcurrentMixinModificationTest.class);
        suite.addTestSuite(ConcurrentModificationWithSNSTest.class);
        suite.addTestSuite(ConcurrentMoveTest.class);
        suite.addTestSuite(ConcurrentReorderTest.class);
        suite.addTestSuite(ConcurrentAddRemoveNodeTest.class);

        suite.addTestSuite(LostFromCacheIssueTest.class);

        // TODO: These tests pass, but they cause some instability in other
        // parts of the test suite, most likely due to uncleaned test data
        if (Boolean.getBoolean("org.apache.jackrabbit.test.integration")) {
            suite.addTestSuite(ConcurrencyTest.class);
//            // suite.addTestSuite(ConcurrencyTest3.class);
            suite.addTestSuite(ConcurrentVersioningTest.class);
//            // suite.addTestSuite(ConcurrentVersioningWithTransactionsTest.class);
//            suite.addTestSuite(ConcurrentCheckinMixedTransactionTest.class);
//            suite.addTestSuite(ConcurrentLoginTest.class);
//            suite.addTestSuite(ConcurrentNodeModificationTest.class);
//            suite.addTestSuite(ConcurrentReadWriteTest.class);
            suite.addTestSuite(ConcurrentRenameTest.class);
            suite.addTestSuite(ConcurrentSaveTest.class);
//            suite.addTestSuite(ConcurrentWorkspaceCopyTest.class);
        }

        suite.addTestSuite(UserPerWorkspaceSecurityManagerTest.class);
        suite.addTestSuite(OverlappingNodeAddTest.class);
        suite.addTestSuite(NPEandCMETest.class);
        suite.addTestSuite(ConsistencyCheck.class);
        suite.addTestSuite(RemoveAddNodeWithUUIDTest.class);
        suite.addTestSuite(MoveAtRootTest.class);

        return suite;
    }
}
