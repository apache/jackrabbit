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
package org.apache.jackrabbit.core.stats;

import static javax.jcr.query.Query.JCR_SQL2;

import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.core.JackrabbitRepositoryStub;
import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Query stats test cases.
 */
public class QueryStatCoreTest extends AbstractJCRTest {

    private QueryStatCore queryStat;
    private QueryManager qm;

    protected void setUp() throws Exception {
        super.setUp();
        RepositoryContext context = JackrabbitRepositoryStub
                .getRepositoryContext(superuser.getRepository());
        queryStat = context.getStatManager().getQueryStat();
        queryStat.setEnabled(true);

        qm = superuser.getWorkspace().getQueryManager();
    }

    protected void tearDown() throws Exception {
        qm = null;
        super.tearDown();
    }

    private void runRandomQuery() throws Exception {
        String sql = "SELECT * FROM [nt:unstructured] as t where CONTAINS(t, '"
                + System.currentTimeMillis() + "') ";
        qm.createQuery(sql, JCR_SQL2).execute();
    }

    public void testPopularQuery() throws Exception {

        int size = 15;
        queryStat.setPopularQueriesQueueSize(size);

        // test clear
        runRandomQuery();
        queryStat.clearPopularQueriesQueue();
        assertEquals(0, queryStat.getPopularQueries().length);

        // test run one
        queryStat.clearPopularQueriesQueue();
        runRandomQuery();
        assertEquals(1, queryStat.getPopularQueries().length);

        // run more than max size
        queryStat.clearPopularQueriesQueue();
        for (int i = 0; i < size + 5; i++) {
            runRandomQuery();
        }
        assertEquals(size, queryStat.getPopularQueries().length);

        // test shrink
        int newSize = 5;
        queryStat.setPopularQueriesQueueSize(newSize);
        assertEquals(newSize, queryStat.getPopularQueries().length);
    }
}