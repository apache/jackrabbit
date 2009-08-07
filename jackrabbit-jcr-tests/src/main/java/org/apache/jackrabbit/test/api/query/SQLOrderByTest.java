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

import javax.jcr.query.Query;


/**
 * This test searches for all nodes of a specific node type and orders them by
 * the property with name configured as {@link #propertyName1}.
 * <p/>
 * The default workspace must at least contain two nodes of type {@link #testNodeType}
 * with String properties named {@link #propertyName1} containing distinct
 * values.
 *
 * @test
 * @sources SQLOrderByTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.SQLOrderByTest
 * @keywords sql
 */
public class SQLOrderByTest extends AbstractQueryTest {

    /**
     * Statement without order by modifier.
     */
    private String baseStatement;

    /**
     * Prepare a statement without the order by modifier to be used for the
     * tests
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
        // setup common base statement
        StringBuffer tmp = new StringBuffer("SELECT ").append(escapeIdentifierForSQL(propertyName1));
        tmp.append(" FROM ").append(escapeIdentifierForSQL(testNodeType));
        tmp.append(" WHERE ").append(escapeIdentifierForSQL(propertyName1)).append(" IS NOT NULL");
        tmp.append(" ORDER BY ");
        tmp.append(escapeIdentifierForSQL(propertyName1));
        baseStatement = tmp.toString();
    }

    /**
     * Test if sort order <i>ascending</i> is respected.
     * <p/>
     * For configuration description see {@link SQLOrderByTest}.
     */
    public void testOrderByAscending() throws Exception {
        Statement stmt = new Statement(baseStatement + " ASC", Query.SQL);
        evaluateResultOrder(execute(stmt), propertyName1, false);
    }

    /**
     * Test if sort order <i>descending</i> is respected.
     * <p/>
     * For configuration description see {@link SQLOrderByTest}.
     */
    public void testOrderByDescending() throws Exception {
        Statement stmt = new Statement(baseStatement + " DESC", Query.SQL);
        evaluateResultOrder(execute(stmt), propertyName1, true);
    }

    /**
     * Test if default sort order is respected and is <i>ascending</i> if the
     * order by modifier is missing.
     * <p/>
     * For configuration description see {@link SQLOrderByTest}.
     */
    public void testOrderByDefault() throws Exception {
        Statement stmt = new Statement(baseStatement, Query.SQL);
        evaluateResultOrder(execute(stmt), propertyName1, false);
    }
}