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
package org.apache.jackrabbit.core.stats.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.jackrabbit.test.JUnitTest;

public class CachingOpsPerSecondDtoTest extends JUnitTest {

    private final CachingOpsPerSecondDto test = new CachingOpsPerSecondDto(
            1000 * 10000);

    protected void setUp() throws Exception {
        super.setUp();
        test.reset();
    }

    public void testSimple() throws Exception {

        long t = System.currentTimeMillis();
        test.reset();
        test.onOp(10);
        test.onOp(1);
        TimeUnit.MILLISECONDS.sleep(1300);
        test.onOp(2);
        long d_min = System.currentTimeMillis() - t;
        test.refresh();
        long d_max = System.currentTimeMillis() - t;

        // 3 ops in ~ 1.3 seconds = 2.3 ops / sec
        double expected_min = BigDecimal.valueOf(3 * 1000)
                .divide(BigDecimal.valueOf(d_min), new MathContext(3))
                .round(new MathContext(2, RoundingMode.DOWN)).doubleValue();
        double expected_max = BigDecimal.valueOf(3 * 1000)
                .divide(BigDecimal.valueOf(d_max), new MathContext(3))
                .add(new BigDecimal(0.001))
                .round(new MathContext(2, RoundingMode.UP)).doubleValue();
        double opsPerSecond = test.getOpsPerSecond();

        assertTrue(opsPerSecond + "?" + expected_min,
                opsPerSecond >= expected_min);
        assertTrue(opsPerSecond + "?" + expected_max,
                opsPerSecond <= expected_max);
        assertEquals(4.33, test.getOpAvgTime());

    }

    public void testMT() throws Exception {
        int threads = 20;
        int ops = 60 * threads;

        final Random r = new Random();
        test.reset();
        long t = System.currentTimeMillis();
        int aggDuration = 0;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<Void>> futures = new ArrayList<Future<Void>>();
        for (int i = 0; i < ops; i++) {
            int duration = 35 + r.nextInt(10);
            boolean shouldRefresh = i % 10 == 0;
            futures.add(executor.submit(newCallable(test, duration, 75,
                    shouldRefresh)));
            aggDuration += duration;
        }
        executor.shutdown();
        for (Future<Void> f : futures) {
            f.get();
        }

        long d_min = System.currentTimeMillis() - t;
        test.refresh();
        long d_max = System.currentTimeMillis() - t;

        double expected_min = BigDecimal.valueOf(ops * 1000)
                .divide(BigDecimal.valueOf(d_min), new MathContext(3))
                .round(new MathContext(2, RoundingMode.DOWN)).doubleValue();
        double expected_max = BigDecimal.valueOf(ops * 1000)
                .divide(BigDecimal.valueOf(d_max), new MathContext(3))
                .add(new BigDecimal(0.001))
                .round(new MathContext(2, RoundingMode.UP)).doubleValue();
        double opsPerSecond = test.getOpsPerSecond();

        assertTrue(opsPerSecond + "?" + expected_min,
                opsPerSecond >= expected_min);
        assertTrue(opsPerSecond + "?" + expected_max,
                opsPerSecond <= expected_max);

        double expectedAvg = BigDecimal
                .valueOf(aggDuration)
                .divide(BigDecimal.valueOf(ops),
                        new MathContext(2, RoundingMode.DOWN)).doubleValue();
        assertEquals(expectedAvg, BigDecimal.valueOf(test.getOpAvgTime())
                .round(new MathContext(2, RoundingMode.DOWN)).doubleValue());
    }

    private Callable<Void> newCallable(final CachingOpsPerSecondDto test,
            final int duration, final long sleep, final boolean shouldRefresh) {
        return new Callable<Void>() {

            public Void call() throws Exception {
                test.onOp(duration);
                TimeUnit.MILLISECONDS.sleep(sleep);
                if (shouldRefresh) {
                    test.refresh();
                }
                return null;
            }
        };
    }
}
