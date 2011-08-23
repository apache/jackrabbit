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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Statistical data structure, use to compute stats on operations
 * 
 */
public class CachingOpsPerSecondDto {

    // @ 1 min
    public static long DEFAULT_UPDATE_FREQ_MS = 1000 * 60 * 1;

    private final long updateFreqMs;

    private final ReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock r = rwl.readLock();
    private final Lock w = rwl.writeLock();

    // intermediary values

    private long lastUpdate = System.currentTimeMillis();

    private long startMs = System.currentTimeMillis();

    private long operations = 0;

    private long totalTimeNs = 0;

    // cached stats

    private double opsPerSecond = 0;

    private double opAvgTime = 0;

    public CachingOpsPerSecondDto(long updateFreqMs) {
        this.updateFreqMs = updateFreqMs;
    }

    public CachingOpsPerSecondDto() {
        this(DEFAULT_UPDATE_FREQ_MS);
    }

    public void onOp(long timeNs) {
        w.lock();
        try {
            final long localStart = System.currentTimeMillis() - timeNs / 1000;
            if (localStart < startMs) {
                startMs = localStart;
            }
            operations++;
            totalTimeNs += timeNs;
        } finally {
            w.unlock();
        }
    }

    public double getOpsPerSecond() {
        checkUpdate(false);
        return opsPerSecond;
    }

    public double getOpAvgTime() {
        checkUpdate(false);
        return opAvgTime;
    }

    private void checkUpdate(boolean forceUpdate) {
        r.lock();
        final long now = System.currentTimeMillis();
        try {
            if (!forceUpdate && now - lastUpdate < updateFreqMs) {
                return;
            }
        } finally {
            r.unlock();
        }
        w.lock();
        try {
            if (!forceUpdate && now - lastUpdate < updateFreqMs) {
                return;
            }
            update(now);
        } finally {
            w.unlock();
        }
    }

    private final static BigDecimal thousand = BigDecimal.valueOf(1000);

    private final static MathContext DEFAULT_CONTEXT = new MathContext(3);

    private void update(long now) {
        if (operations == 0) {
            opsPerSecond = 0;
            opAvgTime = 0;
            return;
        }
        long durationMs = now - startMs;
        if (durationMs == 0) {
            durationMs = 1000;
        }
        opsPerSecond = BigDecimal.valueOf(operations).multiply(thousand)
                .divide(BigDecimal.valueOf(durationMs), DEFAULT_CONTEXT)
                .doubleValue();
        opAvgTime = BigDecimal.valueOf(totalTimeNs)
                .divide(BigDecimal.valueOf(operations), DEFAULT_CONTEXT)
                .doubleValue();
        // reset if needed
        if (operations > Long.MAX_VALUE - 5000) {
            reset();
        }
    }

    public void reset() {
        w.lock();
        try {
            opsPerSecond = 0;
            opAvgTime = 0;
            lastUpdate = System.currentTimeMillis();
            operations = 0;
            startMs = lastUpdate;
            totalTimeNs = 0;
        } finally {
            w.unlock();
        }
    }

    protected void refresh() {
        checkUpdate(true);
    }
}
