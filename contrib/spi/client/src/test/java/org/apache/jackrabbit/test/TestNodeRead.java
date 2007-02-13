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
import org.apache.jackrabbit.test.api.RootNodeTest;
import org.apache.jackrabbit.test.api.NodeReadMethodsTest;
import org.apache.jackrabbit.test.api.NodeDiscoveringNodeTypesTest;
import org.apache.jackrabbit.test.api.NodeIteratorTest;
import org.apache.jackrabbit.test.api.ReferenceableRootNodesTest;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

/**
 * <code>TestNodeRead</code>...
 */
public class TestNodeRead extends TestCase {

    private static Logger log = LoggerFactory.getLogger(TestNodeRead.class);

    public static Test suite() {

        TestSuite suite = new TestSuite("javax.jcr Node-Read");

        suite.addTestSuite(RootNodeTest.class);
        suite.addTestSuite(NodeReadMethodsTest.class);
        suite.addTestSuite(NodeDiscoveringNodeTypesTest.class);
        suite.addTestSuite(NodeIteratorTest.class);
        suite.addTestSuite(ReferenceableRootNodesTest.class);

        return suite;
    }
}