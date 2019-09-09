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
 * Performs tests with the <code>CONTAINS</code> function in JCR_SQL2 queries.
 */
public class FulltextSQL2QueryTest extends AbstractQueryTest {

    public void testFulltextSimpleSQL() throws Exception {
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("mytext", new String[]{"the quick brown fox jumps over the lazy dog."});

        testRootNode.save();

        String sql = "SELECT * FROM [nt:unstructured]"
                + " WHERE ISCHILDNODE([" + testRoot + "])"
                + " AND CONTAINS(mytext, 'fox')";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = q.execute();
        checkResult(result, 1);
    }

    public void testFulltextBindVariableSQL() throws Exception {
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("mytext", new String[]{"the quick brown fox jumps over the lazy dog."});
        
        testRootNode.save();
        
        String sql = "SELECT * FROM [nt:unstructured]"
            + " WHERE ISCHILDNODE([" + testRoot + "])"
            + " AND CONTAINS(mytext, $searchExpression)";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertEquals("Expected exactly 1 bind variable", 1, q.getBindVariableNames().length);
        assertEquals("searchExpression", q.getBindVariableNames()[0]);

        q.bindValue("searchExpression", superuser.getValueFactory().createValue("fox"));
        QueryResult result = q.execute();
        checkResult(result, 1);
    }

}
