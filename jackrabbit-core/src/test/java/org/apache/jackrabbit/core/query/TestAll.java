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
package org.apache.jackrabbit.core.query;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.jackrabbit.test.ConcurrentTestSuite;

/**
 * Test suite that includes all testcases for the Search module.
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
        TestSuite suite = new ConcurrentTestSuite("Search tests");

        suite.addTestSuite(SimpleQueryTest.class);
        suite.addTestSuite(FulltextQueryTest.class);
        suite.addTestSuite(SelectClauseTest.class);
        suite.addTestSuite(SQLTest.class);
        suite.addTestSuite(JoinTest.class);
        suite.addTestSuite(OrderByTest.class);
        suite.addTestSuite(XPathAxisTest.class);
        suite.addTestSuite(SkipDeletedNodesTest.class);
        suite.addTestSuite(SkipDeniedNodesTest.class);
        suite.addTestSuite(MixinTest.class);
        suite.addTestSuite(DerefTest.class);
        suite.addTestSuite(VersionStoreQueryTest.class);
        suite.addTestSuite(UpperLowerCaseQueryTest.class);
        suite.addTestSuite(ChildAxisQueryTest.class);
        suite.addTestSuite(QueryResultTest.class);
        suite.addTestSuite(FnNameQueryTest.class);
        suite.addTestSuite(PathQueryNodeTest.class);
        suite.addTestSuite(ExcerptTest.class);
        suite.addTestSuite(ShareableNodeTest.class);
        suite.addTestSuite(ParentNodeTest.class);
        suite.addTestSuite(SimilarQueryTest.class);
        suite.addTestSuite(FulltextSQL2QueryTest.class);
        suite.addTestSuite(LimitAndOffsetTest.class);
        suite.addTestSuite(SQL2NodeLocalNameTest.class);
        suite.addTestSuite(SQL2OuterJoinTest.class);
        suite.addTestSuite(SQL2PathEscapingTest.class);
        suite.addTestSuite(SQL2QueryResultTest.class);
        suite.addTestSuite(LimitedAccessQueryTest.class);
        suite.addTestSuite(SQL2OffsetLimitTest.class);
        suite.addTestSuite(SQL2OrderByTest.class);
        suite.addTestSuite(DescendantSelfAxisTest.class);

        return suite;
    }
}
