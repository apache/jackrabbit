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
package org.apache.jackrabbit.core.jmx.core;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.jmx.AbstractJmxTest;
import org.apache.jackrabbit.core.jmx.util.CachingOpsPerSecondDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMX test cases for Core Stats.
 */
public class CoreStatSessionTest extends AbstractJmxTest {

    private static Logger log = LoggerFactory
            .getLogger(CoreStatSessionTest.class);

    private CoreStat cs;

    protected void setUp() throws Exception {
        super.setUp();
        cs = jmxRegistry.getCoreStat();
        cs.resetNumberOfSessions();
    }

    public void testNumberOfSessions1() throws Exception {

        cs.setEnabled(true);
        assertEquals(0, cs.getNumberOfSessions());

        Session s = superuser.impersonate(new SimpleCredentials("anonymous", ""
                .toCharArray()));
        assertNotNull(s);
        assertEquals(1, cs.getNumberOfSessions());

        s.logout();
        assertEquals(0, cs.getNumberOfSessions());
    }

    public void testNumberOfSessions2() throws Exception {

        cs.setEnabled(false);
        assertEquals(0, cs.getNumberOfSessions());

        Session s = superuser.impersonate(new SimpleCredentials("anonymous", ""
                .toCharArray()));
        assertNotNull(s);
        assertEquals(0, cs.getNumberOfSessions());

        s.logout();
        assertEquals(0, cs.getNumberOfSessions());
    }

    public void testNumberOfSessions3() throws Exception {

        cs.setEnabled(true);
        assertEquals(0, cs.getNumberOfSessions());

        Session s = superuser.impersonate(new SimpleCredentials("anonymous", ""
                .toCharArray()));
        assertNotNull(s);
        assertEquals(1, cs.getNumberOfSessions());

        cs.resetNumberOfSessions();
        assertEquals(0, cs.getNumberOfSessions());

        s.logout();
        assertEquals(0, cs.getNumberOfSessions());
    }

    public void _testNumberOfSessionsImpact() throws Exception {

        int times = 10;

        for (int i = 0; i < times; i++) {

            int operations = 100;
            int threads = 25;

            // no stats
            cs.setEnabled(false);
            long take1 = doLoginLogout(superuser, threads, operations);

            // stats
            cs.setEnabled(true);
            cs.resetNumberOfSessions();
            long take2 = doLoginLogout(superuser, threads, operations);

            long diff = take2 - take1;
            double extra = (double) diff / operations;
            BigDecimal p = new BigDecimal(extra * 100 / take1, new MathContext(
                    2));

            log.info(threads + " thread(s) performing " + operations
                    + " each: overhead total diff time " + diff + "(" + p
                    + "%). overhead per op " + extra);

        }
    }

    private long doLoginLogout(final Session admin, final int threads,
            final int times) throws Exception {

        ExecutorService es = Executors.newFixedThreadPool(threads);
        List<Future<Long>> futures = new ArrayList<Future<Long>>();

        long t = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            futures.add(es.submit(new Callable<Long>() {
                public Long call() throws Exception {
                    return doLoginLogoutOp(admin, times);
                }
            }));
        }
        es.shutdown();
        for (Future<Long> f : futures) {
            f.get();
        }
        return System.currentTimeMillis() - t;
    }

    private long doLoginLogoutOp(Session admin, int times) throws Exception {
        long t = System.currentTimeMillis();

        for (int i = 0; i < times; i++) {
            Session s = superuser.impersonate(new SimpleCredentials(
                    "anonymous", "".toCharArray()));
            s.logout();
        }
        return System.currentTimeMillis() - t;
    }

    static {
        CachingOpsPerSecondDto.DEFAULT_UPDATE_FREQ_MS = 4000;
    }

    public void testNumberOfOpsPerSecond1() throws Exception {

        cs.setEnabled(true);

        int go = 0;
        while (true) {
            for (int i = 0; i < 50; i++) {
                long t = System.currentTimeMillis();
                int cnt = 10000;
                for (int j = 0; j < cnt; j++) {
                    superuser.getNode(testRoot);
                }
                t = System.currentTimeMillis() - t;
                System.out.println("took " + t + " ms for " + cnt);
            }
            TimeUnit.SECONDS.sleep(3);
            go++;
            if (go == 500) {
                return;
            }
        }
    }
}
