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

    private final Map<Type, TimeSeriesAverage> avg =
            new HashMap<Type, TimeSeriesAverage>();

    public RepositoryStatisticsImpl() {
        getOrCreateRecorder(Type.SESSION_COUNT);
        getOrCreateRecorder(Type.SESSION_LOGIN_COUNTER);

        createAvg(Type.SESSION_READ_COUNTER, Type.SESSION_READ_DURATION,
                Type.SESSION_READ_AVERAGE);
        createAvg(Type.SESSION_WRITE_COUNTER, Type.SESSION_WRITE_DURATION,
                Type.SESSION_WRITE_AVERAGE);
        createAvg(Type.BUNDLE_CACHE_MISS_COUNTER,
                Type.BUNDLE_CACHE_MISS_DURATION, Type.BUNDLE_CACHE_MISS_AVERAGE);
        createAvg(Type.BUNDLE_WRITE_COUNTER, Type.BUNDLE_WRITE_DURATION,
                Type.BUNDLE_WRITE_AVERAGE);
        createAvg(Type.QUERY_COUNT, Type.QUERY_DURATION, Type.QUERY_AVERAGE);

    }

    private void createAvg(Type count, Type duration, Type avgTs) {
        avg.put(avgTs, new TimeSeriesAverage(getOrCreateRecorder(duration),
                getOrCreateRecorder(count)));
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
        map.putAll(avg);
        return map.entrySet().iterator();
    }

    public AtomicLong getCounter(Type type) {
        return getOrCreateRecorder(type).getCounter();
    }

    public TimeSeries getTimeSeries(Type type) {
        if (avg.containsKey(type)) {
            return avg.get(type);
        } else {
            return getOrCreateRecorder(type);
        }
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
