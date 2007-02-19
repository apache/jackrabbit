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
package org.apache.jackrabbit.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.jackrabbit.test.api.observation.EventIteratorTest;
import org.apache.jackrabbit.test.api.observation.EventTest;
import org.apache.jackrabbit.test.api.observation.GetRegisteredEventListenersTest;
import org.apache.jackrabbit.test.api.observation.LockingTest;
import org.apache.jackrabbit.test.api.observation.NodeAddedTest;
import org.apache.jackrabbit.test.api.observation.NodeRemovedTest;
import org.apache.jackrabbit.test.api.observation.NodeMovedTest;
import org.apache.jackrabbit.test.api.observation.NodeReorderTest;
import org.apache.jackrabbit.test.api.observation.PropertyAddedTest;
import org.apache.jackrabbit.test.api.observation.PropertyChangedTest;
import org.apache.jackrabbit.test.api.observation.PropertyRemovedTest;
import org.apache.jackrabbit.test.api.observation.AddEventListenerTest;
import org.apache.jackrabbit.test.api.observation.WorkspaceOperationTest;

/**
 * <code>TestObservation</code>...
 */
public class TestObservation {

    public static Test suite() {
        TestSuite suite = new TestSuite("javax.jcr.observation");
        suite.addTestSuite(EventIteratorTest.class);
        suite.addTestSuite(EventTest.class);
        suite.addTestSuite(GetRegisteredEventListenersTest.class);
        suite.addTestSuite(LockingTest.class);
        suite.addTestSuite(NodeAddedTest.class);
        suite.addTestSuite(NodeRemovedTest.class);
        suite.addTestSuite(NodeMovedTest.class);
        suite.addTestSuite(NodeReorderTest.class);
        suite.addTestSuite(PropertyAddedTest.class);
        suite.addTestSuite(PropertyChangedTest.class);
        suite.addTestSuite(PropertyRemovedTest.class);
        suite.addTestSuite(AddEventListenerTest.class);
        suite.addTestSuite(WorkspaceOperationTest.class);
        return suite;
    }
}
