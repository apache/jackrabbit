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
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

/**
 */
public class SQLTest extends AbstractQueryTest {


    public void testSimpleQuery1() throws Exception {
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("bla", new String[]{"bla"});

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured WHERE bla='bla' " +
                "AND jcr:path LIKE '" + testRoot + "/%'";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);
    }

    public void testFulltextSimple() throws Exception {
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("mytext", new String[]{"the quick brown fox jumps over the lazy dog."});

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured WHERE contains(., 'fox') " +
                "AND jcr:path LIKE '" + testRoot + "/%'";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);
    }

    /**
     * I'm not sure what this test is supposed to verify. It only works because
     * the sql parser is not strict enough and the column values are never
     * actually checked
     */
    public void _testFulltextComplex() throws Exception {
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("mytext", new String[]{"the quick brown fox jumps over the lazy dog."});

        testRootNode.save();

        String sql = "SELECT foo.mytext, bla.foo FROM nt:unstructured WHERE " +
                "contains(., 'fox') AND NOT contains(., 'bla') " +
                "AND jcr:path LIKE '" + testRoot + "/%'";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 1);
    }


}
