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
package org.apache.jackrabbit.test.api.nodetype;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite that includes all testcases for the package
 * <code>javax.jcr.nodetype</code>.
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
        TestSuite suite = new TestSuite("javax.jcr.nodetype tests");

        // ADD TEST CLASSES HERE:
        suite.addTestSuite(NodeDefTest.class);
        suite.addTestSuite(NodeTypeManagerTest.class);
        suite.addTestSuite(NodeTypeTest.class);
        suite.addTestSuite(PropertyDefTest.class);

        suite.addTestSuite(PredefinedNodeTypeTest.class);

        suite.addTestSuite(CanSetPropertyBinaryTest.class);
        suite.addTestSuite(CanSetPropertyBooleanTest.class);
        suite.addTestSuite(CanSetPropertyDateTest.class);
        suite.addTestSuite(CanSetPropertyDoubleTest.class);
        suite.addTestSuite(CanSetPropertyLongTest.class);
        suite.addTestSuite(CanSetPropertyMultipleTest.class);
        suite.addTestSuite(CanSetPropertyNameTest.class);
        suite.addTestSuite(CanSetPropertyPathTest.class);
        suite.addTestSuite(CanSetPropertyStringTest.class);
        suite.addTestSuite(CanSetPropertyTest.class);

        suite.addTestSuite(CanAddChildNodeCallWithNodeTypeTest.class);
        suite.addTestSuite(CanAddChildNodeCallWithoutNodeTypeTest.class);

        suite.addTestSuite(CanRemoveItemTest.class);

        return suite;
    }
}