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

import javax.jcr.Node;

import org.apache.jackrabbit.commons.JcrUtils;

/**
 * Test case for Node LocalName queries with JCR_SQL2
 *
 * Inspired by <a
 * href="https://issues.apache.org/jira/browse/JCR-2956">JCR-2956</a>
 */
public class SQL2NodeLocalNameTest extends AbstractQueryTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testRootNode.addNode("SQL2NodeLocalNameTest_node1");
        testRootNode.addNode("SQL2NodeLocalNameTest_node2");
        testRootNode.getSession().save();
    }

    @Override
    protected void tearDown() throws Exception {
        for (Node c : JcrUtils.getChildNodes(testRootNode)) {
            testRootNode.getSession().removeItem(c.getPath());
        }
        superuser.save();
        super.tearDown();
    }

    public void testEq() throws Exception {
        checkResult(
                executeSQL2Query("SELECT * FROM [nt:base] as NODE WHERE localname(NODE) = 'SQL2NodeLocalNameTest_node1' "),
                1);
    }

    public void testLikeEnd() throws Exception {
        checkResult(
                executeSQL2Query("SELECT * FROM [nt:base] as NODE WHERE localname(NODE) like 'SQL2NodeLocalNameTest%' "),
                2);
    }

    public void testLikeMiddle() throws Exception {
        checkResult(
                executeSQL2Query("SELECT * FROM [nt:base] as NODE WHERE localname(NODE) like '%NodeLocalNameTest%' "),
                2);
    }

    public void testLikeNoHits() throws Exception {
        checkResult(
                executeSQL2Query("SELECT * FROM [nt:base] as NODE WHERE localname(NODE) like 'XXX_NodeLocalNameTest_XXX' "),
                0);
    }

    /**
     * funny enough, this will return the <b>root</b> node
     */
    public void testLikeEmpty() throws Exception {
        checkResult(
                executeSQL2Query("SELECT * FROM [nt:base] as NODE WHERE localname(NODE) like '' "),
                1);
    }

    public void testLikeEmptyAsChild() throws Exception {
        checkResult(
                executeSQL2Query("SELECT * FROM [nt:base] as NODE WHERE ischildnode(NODE, ["
                        + testRootNode.getPath()
                        + "]) AND localname(NODE) like '' "), 0);
    }

    public void testLikeAllAsChild() throws Exception {
        checkResult(
                executeSQL2Query("SELECT * FROM [nt:base] as NODE WHERE ischildnode(NODE, ["
                        + testRootNode.getPath()
                        + "]) AND localname(NODE) like '%' "), 2);
    }

    /**
     * test for JCR-3159
     */
    public void testLowerLocalName() throws Exception {
        checkResult(
                executeSQL2Query("SELECT * FROM [nt:base] as NODE WHERE LOWER(localname(NODE)) like 'sql2nodelocalnametest%'"),
                2);
    }

    /**
     * test for JCR-3159
     */
    public void testUpperLocalName() throws Exception {
        checkResult(
                executeSQL2Query("SELECT * FROM [nt:base] as NODE WHERE UPPER(localname(NODE)) like 'SQL2NODELOCALNAMETEST%'"),
                2);
    }

    /**
     * test for JCR-3159
     */
    public void testLowerName() throws Exception {
        checkResult(
                executeSQL2Query("SELECT * FROM [nt:base] as NODE WHERE LOWER(name(NODE)) like 'sql2nodelocalnametest%'"),
                2);
    }

    /**
     * test for JCR-3398
     */
    public void testLowerLocalNameOrContains() throws Exception {
        checkResult(
                executeSQL2Query("SELECT * FROM [nt:base] as NODE WHERE LOWER(localname(NODE)) like 'sql2nodelocalnametest%' OR contains(NODE.*, 'sql2nodelocalnametest')"),
                2);
    }

    /**
     * test for JCR-3398
     */
    public void testUpperLocalNameOrContains() throws Exception {
        checkResult(
                executeSQL2Query("SELECT * FROM [nt:base] as NODE WHERE UPPER(localname(NODE)) like 'SQL2NODELOCALNAMETEST%' OR contains(NODE.*, 'SQL2NODELOCALNAMETEST')"),
                2);
    }
}
