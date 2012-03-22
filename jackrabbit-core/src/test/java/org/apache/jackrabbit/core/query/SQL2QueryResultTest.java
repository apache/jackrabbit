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

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.cnd.CndImporter;

/**
 * <code>QueryResultTest</code> tests various methods on the
 * <code>NodeIterator</code> returned by a <code>QueryResult</code>.
 */
public class SQL2QueryResultTest extends AbstractQueryTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        NodeTypeManager manager = superuser.getWorkspace().getNodeTypeManager();
        if (!manager.hasNodeType("test:RTypeTest")) {
            StringBuilder defs = new StringBuilder();
            defs.append("[test:RTypeTest]\n");
            defs.append("  - prop1\n");
            defs.append("  - prop2\n");
            Reader cndReader = new InputStreamReader(new ByteArrayInputStream(
                    defs.toString().getBytes()));
            CndImporter.registerNodeTypes(cndReader, superuser);
        }

        Node n1 = testRootNode.addNode("node1", "test:RTypeTest");
        n1.setProperty("prop1", "p1");
        n1.setProperty("prop2", "p2");

        testRootNode.getSession().save();
    }

    /**
     * Checks returned columns in a 'select *' vs 'select property' situation.
     * 
     * see <a href="https://issues.apache.org/jira/browse/JCR-2954">JCR-2954</a>
     * 
     */
    public void testSQL2SelectColums() throws RepositoryException {

        executeAndCheckColumns("select * from [test:RTypeTest]", 3,
                "test:RTypeTest.prop1", "test:RTypeTest.jcr:primaryType",
                "test:RTypeTest.prop2");

        executeAndCheckColumns("select * from [test:RTypeTest] as test", 3,
                "test.jcr:primaryType", "test.prop1", "test.prop2");

        executeAndCheckColumns("select test.* from [test:RTypeTest] as test",
                3, "test.jcr:primaryType", "test.prop1", "test.prop2");

        executeAndCheckColumns("select prop1 from [test:RTypeTest]", 1, "prop1");

        executeAndCheckColumns(
                "select prop1 as newProp1 from [test:RTypeTest]", 1, "newProp1");

        executeAndCheckColumns("select prop1 from [test:RTypeTest] as test", 1,
                "prop1");

        executeAndCheckColumns(
                "select prop1 as newProp1 from [test:RTypeTest] as test", 1,
                "newProp1");

        executeAndCheckColumns(
                "select test.prop1 from [test:RTypeTest] as test", 1,
                "test.prop1");

        executeAndCheckColumns(
                "select test.prop1 as newProp1 from [test:RTypeTest] as test",
                1, "newProp1");

    }

    private void executeAndCheckColumns(String sql2, int expected,
            String... cols) throws RepositoryException {
        QueryResult r = executeSQL2Query(sql2);
        assertEquals(
                "Got more columns than expected: "
                        + Arrays.toString(r.getColumnNames()), expected,
                r.getColumnNames().length);
        if (expected > 0) {
            assertEquals(expected, cols.length);
            List<String> expectedCols = new ArrayList<String>(
                    Arrays.asList(cols));
            expectedCols.removeAll(new ArrayList<String>(Arrays.asList(r
                    .getColumnNames())));
            assertTrue("Got unexpected columns: " + expectedCols,
                    expectedCols.isEmpty());
            for (Row row : JcrUtils.getRows(r)) {
                assertNotNull(row.getValues());
                assertEquals(expected, row.getValues().length);
            }
        }
    }
}
