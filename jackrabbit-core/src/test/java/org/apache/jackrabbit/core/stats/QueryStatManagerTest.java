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

import javax.jcr.query.Query;

import org.apache.jackrabbit.api.stats.QueryStat;
import org.apache.jackrabbit.api.stats.QueryStatDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * QueryStat tests.
 */
public class QueryStatManagerTest extends AbstractStatTest {

    private static Logger log = LoggerFactory
            .getLogger(QueryStatManagerTest.class);

    private QueryStat qs;

    protected void setUp() throws Exception {
        super.setUp();
        qs = statManager.getQueryStat();
        qs.reset();
    }

    public void testLogDuration() throws Exception {
        int queueSize = 15;
        int runTimes = 50;

        qs.setSlowQueriesQueueSize(queueSize);
        qs.setEnabled(false);

        // just to warm up the cache
        double initial = doStatTest(runTimes);

        qs.setEnabled(false);
        double off = doStatTest(runTimes);

        qs.setEnabled(true);
        double on = doStatTest(runTimes);

        log.info("Logging times: initial=" + initial
                + ", without QueryStatManager = " + off
                + ", with QueryStatManager = " + on);

        QueryStatDto[] top = qs.getSlowQueries();
        assertNotNull("Query Top should not be null", top);
        assertEquals("Query Top should contain entries ",
                Math.min(queueSize, runTimes), top.length);
    }

    private double doStatTest(int times) throws Exception {

        long total = 0;
        for (int i = 0; i < times; i++) {
            long start = System.currentTimeMillis();
            Query q = superuser.getWorkspace().getQueryManager()
                    .createQuery("SELECT * FROM [nt:base]", Query.JCR_SQL2);
            q.execute();
            long dur = System.currentTimeMillis() - start;
            total += dur;
        }
        return (double) total / times;
    }
}
