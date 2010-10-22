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
package org.apache.jackrabbit.core.integration;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.jackrabbit.test.JCRTestSuite;
import org.apache.jackrabbit.test.api.query.qom.AndConstraintTest;
import org.apache.jackrabbit.test.api.query.qom.BindVariableValueTest;
import org.apache.jackrabbit.test.api.query.qom.ChildNodeJoinConditionTest;
import org.apache.jackrabbit.test.api.query.qom.ChildNodeTest;
import org.apache.jackrabbit.test.api.query.qom.ColumnTest;
import org.apache.jackrabbit.test.api.query.qom.DescendantNodeJoinConditionTest;
import org.apache.jackrabbit.test.api.query.qom.DescendantNodeTest;
import org.apache.jackrabbit.test.api.query.qom.EquiJoinConditionTest;
import org.apache.jackrabbit.test.api.query.qom.FullTextSearchScoreTest;
import org.apache.jackrabbit.test.api.query.qom.GetQueryTest;
import org.apache.jackrabbit.test.api.query.qom.LengthTest;
import org.apache.jackrabbit.test.api.query.qom.NodeLocalNameTest;
import org.apache.jackrabbit.test.api.query.qom.NodeNameTest;
import org.apache.jackrabbit.test.api.query.qom.NotConstraintTest;
import org.apache.jackrabbit.test.api.query.qom.OrConstraintTest;
import org.apache.jackrabbit.test.api.query.qom.OrderingTest;
import org.apache.jackrabbit.test.api.query.qom.PropertyExistenceTest;
import org.apache.jackrabbit.test.api.query.qom.PropertyValueTest;
import org.apache.jackrabbit.test.api.query.qom.QueryObjectModelFactoryTest;
import org.apache.jackrabbit.test.api.query.qom.RowTest;
import org.apache.jackrabbit.test.api.query.qom.SameNodeJoinConditionTest;
import org.apache.jackrabbit.test.api.query.qom.SameNodeTest;
import org.apache.jackrabbit.test.api.query.qom.SelectorTest;
import org.apache.jackrabbit.test.api.query.qom.UpperLowerCaseTest;

/**
 * Test suite that includes all test suites from jackrabbit-jcr-tests.
 */
public class JCRAPITest extends TestCase {

    public static Test suite() {
        // return new JCRTestSuite();
        TestSuite suite = new TestSuite("QOM tests");

        suite.addTestSuite(AndConstraintTest.class);
        suite.addTestSuite(BindVariableValueTest.class);
        suite.addTestSuite(ChildNodeJoinConditionTest.class);
        suite.addTestSuite(ChildNodeTest.class);
        suite.addTestSuite(ColumnTest.class);
        suite.addTestSuite(DescendantNodeJoinConditionTest.class);
        suite.addTestSuite(DescendantNodeTest.class);
        suite.addTestSuite(EquiJoinConditionTest.class);
        suite.addTestSuite(FullTextSearchScoreTest.class);
        suite.addTestSuite(GetQueryTest.class);
        suite.addTestSuite(LengthTest.class);
        suite.addTestSuite(NodeLocalNameTest.class);
        suite.addTestSuite(NodeNameTest.class);
        suite.addTestSuite(NotConstraintTest.class);
        suite.addTestSuite(OrConstraintTest.class);
        suite.addTestSuite(OrderingTest.class);
        suite.addTestSuite(PropertyExistenceTest.class);
        suite.addTestSuite(PropertyValueTest.class);
        suite.addTestSuite(QueryObjectModelFactoryTest.class);
        suite.addTestSuite(RowTest.class);
        suite.addTestSuite(SameNodeJoinConditionTest.class); 
        suite.addTestSuite(SameNodeTest.class);
        suite.addTestSuite(SelectorTest.class);
        suite.addTestSuite(UpperLowerCaseTest.class);

        return suite;
    }

}
