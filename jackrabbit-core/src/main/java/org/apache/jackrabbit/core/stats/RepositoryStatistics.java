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
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class RepositoryStatistics {

    private final Map<String, TimeSeriesRecorder> recorders =
            new HashMap<String, TimeSeriesRecorder>();

    public RepositoryStatistics(ScheduledExecutorService executor) {
        executor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                recordOneSecond();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public AtomicLong getCounter(String name) {
        return getOrCreateRecorder(name).getCounter();
    }

    public TimeSeries getTimeSeries(String name) {
        return getOrCreateRecorder(name);
    }

    private synchronized TimeSeriesRecorder getOrCreateRecorder(String name) {
        TimeSeriesRecorder recorder = recorders.get(name);
        if (recorder == null) {
            recorder = new TimeSeriesRecorder();
            recorders.put(name, recorder);
        }
        return recorder;
    }

    private synchronized void recordOneSecond() {
        for (TimeSeriesRecorder recorder : recorders.values()) {
            recorder.recordOneSecond();
        }
    }

}
