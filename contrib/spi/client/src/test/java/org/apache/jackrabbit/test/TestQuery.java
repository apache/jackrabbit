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
import org.apache.jackrabbit.test.api.query.SaveTest;
import org.apache.jackrabbit.test.api.query.SQLOrderByTest;
import org.apache.jackrabbit.test.api.query.SQLQueryLevel2Test;
import org.apache.jackrabbit.test.api.query.SQLJoinTest;
import org.apache.jackrabbit.test.api.query.SQLJcrPathTest;
import org.apache.jackrabbit.test.api.query.SQLPathTest;
import org.apache.jackrabbit.test.api.query.XPathPosIndexTest;
import org.apache.jackrabbit.test.api.query.XPathDocOrderTest;
import org.apache.jackrabbit.test.api.query.XPathOrderByTest;
import org.apache.jackrabbit.test.api.query.XPathQueryLevel2Test;
import org.apache.jackrabbit.test.api.query.XPathJcrPathTest;
import org.apache.jackrabbit.test.api.query.DerefQueryLevel1Test;
import org.apache.jackrabbit.test.api.query.ElementTest;
import org.apache.jackrabbit.test.api.query.TextNodeTest;
import org.apache.jackrabbit.test.api.query.GetLanguageTest;
import org.apache.jackrabbit.test.api.query.GetPersistentQueryPathLevel1Test;
import org.apache.jackrabbit.test.api.query.GetPersistentQueryPathTest;
import org.apache.jackrabbit.test.api.query.GetStatementTest;
import org.apache.jackrabbit.test.api.query.GetSupportedQueryLanguagesTest;
import org.apache.jackrabbit.test.api.query.QueryResultNodeIteratorTest;
import org.apache.jackrabbit.test.api.query.GetPropertyNamesTest;
import org.apache.jackrabbit.test.api.query.PredicatesTest;
import org.apache.jackrabbit.test.api.query.SimpleSelectionTest;
import org.apache.jackrabbit.test.api.query.OrderByDateTest;
import org.apache.jackrabbit.test.api.query.OrderByDoubleTest;
import org.apache.jackrabbit.test.api.query.OrderByLongTest;
import org.apache.jackrabbit.test.api.query.OrderByMultiTypeTest;
import org.apache.jackrabbit.test.api.query.OrderByStringTest;

/**
 * <code>TestQuery</code>...
 */
public class TestQuery {

    public static Test suite() {
        TestSuite suite = new TestSuite("javax.jcr.query");
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
