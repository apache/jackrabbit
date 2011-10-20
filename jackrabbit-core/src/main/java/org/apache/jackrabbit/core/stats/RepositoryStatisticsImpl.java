/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.stats;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.jackrabbit.api.stats.RepositoryStatistics;
import org.apache.jackrabbit.api.stats.RepositoryStatistics.Type;
import org.apache.jackrabbit.api.stats.TimeSeries;

public class RepositoryStatisticsImpl implements
        Iterable<Map.Entry<Type, TimeSeries>>, RepositoryStatistics {

    private final Map<Type, TimeSeriesRecorder> recorders =
            new HashMap<Type, TimeSeriesRecorder>();

    public RepositoryStatisticsImpl() {
        getOrCreateRecorder(Type.SESSION_COUNT);
        getOrCreateRecorder(Type.SESSION_LOGIN_COUNTER);
        getOrCreateRecorder(Type.SESSION_READ_COUNTER);
        getOrCreateRecorder(Type.SESSION_READ_DURATION);
        getOrCreateRecorder(Type.SESSION_WRITE_COUNTER);
        getOrCreateRecorder(Type.SESSION_WRITE_DURATION);
        getOrCreateRecorder(Type.BUNDLE_READ_COUNTER);
        getOrCreateRecorder(Type.BUNDLE_READ_DURATION);
        getOrCreateRecorder(Type.BUNDLE_WRITE_COUNTER);
        getOrCreateRecorder(Type.BUNDLE_WRITE_DURATION);
    }

    public RepositoryStatisticsImpl(ScheduledExecutorService executor) {
        this();
        executor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                recordOneSecond();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public synchronized Iterator<Entry<Type, TimeSeries>> iterator() {
        Map<Type, TimeSeries> map = new TreeMap<Type, TimeSeries>();
        map.putAll(recorders);
        return map.entrySet().iterator();
    }

    public AtomicLong getCounter(Type type) {
        return getOrCreateRecorder(type).getCounter();
    }

    public TimeSeries getTimeSeries(Type type) {
        return getOrCreateRecorder(type);
    }

    private synchronized TimeSeriesRecorder getOrCreateRecorder(Type type) {
        TimeSeriesRecorder recorder = recorders.get(type);
        if (recorder == null) {
            recorder = new TimeSeriesRecorder(type);
            recorders.put(type, recorder);
        }
        return recorder;
    }

    private synchronized void recordOneSecond() {
        for (TimeSeriesRecorder recorder : recorders.values()) {
            recorder.recordOneSecond();
        }
    }

}
