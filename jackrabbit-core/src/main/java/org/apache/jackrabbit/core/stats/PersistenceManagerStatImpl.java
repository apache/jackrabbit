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

import java.util.concurrent.atomic.AtomicLong;

import org.apache.jackrabbit.core.stats.util.CachingOpsPerSecondDto;

/**
 * Default {@link PersistenceManagerStatCore} impl
 * 
 */
public class PersistenceManagerStatImpl implements PersistenceManagerStatCore {

    private boolean enabled = false;

    private final CachingOpsPerSecondDto bundleWriteOps = new CachingOpsPerSecondDto();

    private final AtomicLong cacheAccessCount = new AtomicLong();

    private final CachingOpsPerSecondDto cacheReadMissLatency = new CachingOpsPerSecondDto();

    public void onReadCacheMiss(long durationMs) {
        if (!enabled) {
            return;
        }
        cacheReadMissLatency.onOp(durationMs);
    }

    public void onBundleWrite(long durationMs) {
        if (!enabled) {
            return;
        }
        bundleWriteOps.onOp(durationMs);
    }

    public void cacheAccessed(long accessCount) {
        if (!enabled) {
            return;
        }
        cacheAccessCount.addAndGet(accessCount);
    }

    /** -- PersistenceManagerStat -- **/

    public long getCacheMissCount() {
        return cacheReadMissLatency.getOperations();
    }

    public double getCacheMissAvgDuration() {
        return cacheReadMissLatency.getOpAvgTime();
    }

    public long getCacheAccessCount() {
        return cacheAccessCount.get();
    }

    public double getBundleWritesPerSecond() {
        return bundleWriteOps.getOpsPerSecond();
    }

    /** -- GENERAL OPS -- **/

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (this.enabled) {
            reset();
        }
    }

    public void reset() {
        bundleWriteOps.reset();
        cacheReadMissLatency.reset();
        cacheAccessCount.set(0);
    }
}
