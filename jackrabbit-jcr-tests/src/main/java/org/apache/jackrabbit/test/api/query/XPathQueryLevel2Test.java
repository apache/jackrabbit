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
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.query.Row;
import javax.jcr.Value;
import javax.jcr.NodeIterator;

/**
 * Tests XPath queries on content written to the workspace by the test itself.
 *
 * @test
 * @sources XPathQueryLevel2Test.java
 * @executeClass org.apache.jackrabbit.test.api.query.XPathQueryLevel2Test
 * @keywords level2
 */
public class XPathQueryLevel2Test extends AbstractQueryLevel2Test {

    /**
     * Test if the optional jcr:score property for full-text search is
     * supported.
     * <p/>
     * For configuration description see {@link #setUpFullTextTest()}.
     */
    public void testScoreColumn() throws Exception {
        setUpFullTextTest();
        QueryResult result = execute(getFullTextStatement());
        RowIterator rows = result.getRows();
        // test mere existence
        rows.nextRow().getValue(jcrScore);
    }

    /**
     * Test full-text search of the repository.<br>
     * <p/>
     * For configuration description see {@link #setUpFullTextTest()}.
     */
    public void testFullTextSearch() throws Exception {
        setUpFullTextTest();
        QueryResult result = execute(getFullTextStatement());

        // must be 1
        checkResult(result, 1);

        // evaluate result
        NodeIterator itr = result.getNodes();
        while (itr.hasNext()) {
            Value value = itr.nextNode().getProperty(propertyName1).getValue();
            if (value != null) {
                String fullText = value.getString();
                if (fullText.indexOf("cat") > 0) {
                    fail("Search Text: full text search not correct, returned prohibited text");
                }
            }
        }
    }

    /**
     * Test range evauluation of Query.<br>
     * <p/>
     * For configuration description see {@link #setUpRangeTest()}.
     */
    public void testRange() throws Exception {
        setUpRangeTest();
        QueryResult result = execute(getRangeStatement());

        // should be 1
        checkResult(result, 1);

        // evaluate result
        checkValue(result.getNodes(), propertyName1, "b");
    }

    /**
     * Test multi-value support of search.<br>
     * <p/>
     * For configuration description see {@link #setUpMultiValueTest()}.
     */
    public void testMultiValueSearch() throws Exception {
        setUpMultiValueTest();
        QueryResult result = execute(getMultiValueStatement());

        // should be 1
        checkResult(result, 1);

        // evaluate result
        checkValue(result.getNodes(), propertyName1, "existence");
    }

    /**
     * Test if the jcr:path pseudo property is contained in result.
     * <p/>
     * For configuration description see {@link #setUpFullTextTest()}.
     */
    public void testPathColumn() throws Exception {
        setUpFullTextTest();
        QueryResult result = execute(getFullTextStatement());
        RowIterator rows = result.getRows();
        if (getSize(rows) < 1) {
            fail("Query result did not return any nodes");
        }
        // re-aquire rows
        rows = result.getRows();

        // test mere existence
        rows.nextRow().getValue(jcrPath);
    }

    //---------------------------< internal >-----------------------------------

    /**
     * @return Statement selecting a node by a phrase, and proper escaped value
     *         and excluding with a word
     */
    private Statement getFullTextStatement() {
        StringBuffer tmp = new StringBuffer("/");
        tmp.append(jcrRoot).append(testRoot);
        tmp.append("/*[").append(jcrContains);
        tmp.append("(., '\"quick brown\" -cat')]");
        return new Statement(tmp.toString(), Query.XPATH);
    }

    /**
     * @return Statement selecting nodes by its value contained in a multi-value
     *         property
     */
    private Statement getMultiValueStatement() {
        StringBuffer tmp = new StringBuffer("/");
        tmp.append(jcrRoot).append(testRoot);
        tmp.append("/*[@").append(propertyName2).append(" = 'two'");
        tmp.append(" and @").append(propertyName1).append(" = 'existence']");
        return new Statement(tmp.toString(), Query.XPATH);
    }

    /**
     * @return Statement selecting nodes by its range in {@link #propertyName1}
     */
    private Statement getRangeStatement() {
        StringBuffer tmp = new StringBuffer("/");
        tmp.append(jcrRoot).append(testRoot);
        tmp.append("/*[@");
        tmp.append(propertyName1);
        tmp.append(" <= 'b' and @");
        tmp.append(propertyName1);
        tmp.append(" > 'a']");
        return new Statement(tmp.toString(), Query.XPATH);
    }
}