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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.jackrabbit.jcr2spi.observation.ObservationTest;

/**
 * <code>TestAll</code>...
 */
public class TestAll extends TestCase {

    public static Test suite() {

        TestSuite suite = new TestSuite("jcr2spi tests");

        suite.addTestSuite(AccessByRelativePathTest.class);
        suite.addTestSuite(GetItemsTest.class);

        // get node(s)
        suite.addTestSuite(SNSIndexTest.class);
        suite.addTestSuite(NodeOrderTest.class);

        // add nodes
        suite.addTestSuite(AddNodeTest.class);
        
        // set/add property
        suite.addTestSuite(GetPropertyTest.class);
        suite.addTestSuite(AddPropertyTest.class);
        suite.addTestSuite(AddNewPropertyTest.class);
        suite.addTestSuite(SingleValuedPropertyTest.class);
        suite.addTestSuite(MultiValuedPropertyTest.class);
        suite.addTestSuite(BinaryTest.class);

        // change mixin types
        suite.addTestSuite(MixinModificationTest.class);

        // move
        suite.addTestSuite(MoveTest.class);
        suite.addTestSuite(MoveReferenceableTest.class);
        suite.addTestSuite(MoveSNSTest.class);
        suite.addTestSuite(MoveTreeTest.class);
        suite.addTestSuite(MoveNewTreeTest.class);
        suite.addTestSuite(MoveMultipleTest.class);
        suite.addTestSuite(WorkspaceMoveTest.class);
        suite.addTestSuite(RevertMoveTest.class);
        suite.addTestSuite(MoveToNewTest.class);
        suite.addTestSuite(MoveCombinedTest.class);

        // refresh
        suite.addTestSuite(RefreshFalseTest.class);
        suite.addTestSuite(RefreshTrueTest.class);
        suite.addTestSuite(RefreshMovedTest.class);

        // remove
        suite.addTestSuite(RemoveNodeTest.class);
        suite.addTestSuite(RemovePropertyTest.class);
        suite.addTestSuite(RemoveReferenceableNodeTest.class);
        suite.addTestSuite(RemoveSNSTest.class);
        suite.addTestSuite(RemoveMovedNodeTest.class);

        // rename
        suite.addTestSuite(RenameTest.class);

        // reorder
        suite.addTestSuite(ReorderTest.class);
        suite.addTestSuite(ReorderReferenceableSNSTest.class);
        suite.addTestSuite(ReorderSNSTest.class);
        suite.addTestSuite(ReorderNewSNSTest.class);
        suite.addTestSuite(ReorderNewTest.class);
        suite.addTestSuite(ReorderNewAndSavedTest.class);
        suite.addTestSuite(ReorderMixedTest.class);
        suite.addTestSuite(ReorderMoveTest.class);

        // update
        suite.addTestSuite(UpdateTest.class);

        // various
        suite.addTestSuite(ReplaceNodeTest.class);
        suite.addTestSuite(HierarchyNodeTest.class);
        suite.addTestSuite(LazyItemIteratorTest.class);
        suite.addTestSuite(ExternalModificationTest.class);
        suite.addTestSuite(IsSameTest.class);

        // repository
        suite.addTestSuite(LoginTest.class);

        // observation
        suite.addTestSuite(ObservationTest.class);

        // workspace mgt
        suite.addTestSuite(WorkspaceTest.class);

        // json (because of remoting servlet)
        suite.addTestSuite(CopyMoveToJsonTest.class);

        return suite;
    }
}