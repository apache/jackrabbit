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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.test.api.AddNodeTest;
import org.apache.jackrabbit.test.api.NodeTest;
import org.apache.jackrabbit.test.api.NodeUUIDTest;
import org.apache.jackrabbit.test.api.NodeOrderableChildNodesTest;
import org.apache.jackrabbit.test.api.ReferencesTest;
import org.apache.jackrabbit.test.api.NodeItemIsModifiedTest;
import org.apache.jackrabbit.test.api.NodeItemIsNewTest;
import org.apache.jackrabbit.test.api.NodeAddMixinTest;
import org.apache.jackrabbit.test.api.NodeCanAddMixinTest;
import org.apache.jackrabbit.test.api.NodeRemoveMixinTest;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

/**
 * <code>TestNodeWrite</code>...
 */
public class TestNodeWrite extends TestCase {

    private static Logger log = LoggerFactory.getLogger(TestNodeWrite.class);

    public static Test suite() {

        TestSuite suite = new TestSuite("javax.jcr Node-Write");
        suite.addTestSuite(AddNodeTest.class);
        suite.addTestSuite(NodeTest.class);
        suite.addTestSuite(NodeUUIDTest.class);
        suite.addTestSuite(NodeOrderableChildNodesTest.class);
        suite.addTestSuite(ReferencesTest.class);
        suite.addTestSuite(NodeItemIsModifiedTest.class);
        suite.addTestSuite(NodeItemIsNewTest.class);

        suite.addTestSuite(NodeAddMixinTest.class);
        suite.addTestSuite(NodeCanAddMixinTest.class);
        suite.addTestSuite(NodeRemoveMixinTest.class);

        return suite;
    }
}