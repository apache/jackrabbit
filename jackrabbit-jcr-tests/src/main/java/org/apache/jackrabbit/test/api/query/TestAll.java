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
package org.apache.jackrabbit.test.api.query;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite that includes all testcases for the package
 * <code>javax.jcr.query</code>.
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
        TestSuite suite = new TestSuite("javax.jcr.query tests");

        // ADD TEST CLASSES HERE:
        suite.addTestSuite(SaveTest.class);
        suite.addTestSuite(SQLOrderByTest.class);
        suite.addTestSuite(SQLQueryLevel2Test.class);
        suite.addTestSuite(SQLJoinTest.class);
        suite.addTestSuite(SQLJcrPathTest.class);
        suite.addTestSuite(SQLPathTest.class);
        suite.addTestSuite(XPathPosIndexTest.class);
        suite.addTestSuite(XPathDocOrderTest.class);
        suite.addTestSuite(XPathOrderByTest.class);
        suite.addTestSuite(XPathQueryLevel2Test.class);
        suite.addTestSuite(XPathJcrPathTest.class);

        suite.addTestSuite(DerefQueryLevel1Test.class);
        suite.addTestSuite(ElementTest.class);
        suite.addTestSuite(TextNodeTest.class);
        suite.addTestSuite(GetLanguageTest.class);
        suite.addTestSuite(GetPersistentQueryPathLevel1Test.class);
        suite.addTestSuite(GetPersistentQueryPathTest.class);
        suite.addTestSuite(GetStatementTest.class);
        suite.addTestSuite(GetSupportedQueryLanguagesTest.class);

        suite.addTestSuite(QueryResultNodeIteratorTest.class);
        suite.addTestSuite(GetPropertyNamesTest.class);
        suite.addTestSuite(PredicatesTest.class);
        suite.addTestSuite(SimpleSelectionTest.class);

        suite.addTestSuite(OrderByDateTest.class);
        suite.addTestSuite(OrderByDoubleTest.class);
        suite.addTestSuite(OrderByLongTest.class);
        suite.addTestSuite(OrderByMultiTypeTest.class);
        suite.addTestSuite(OrderByStringTest.class);

        return suite;
    }
}