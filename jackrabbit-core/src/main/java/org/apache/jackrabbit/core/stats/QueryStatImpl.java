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

import java.util.Comparator;
import java.util.PriorityQueue;

import org.apache.jackrabbit.api.stats.QueryStat;
import org.apache.jackrabbit.api.stats.QueryStatDto;
import org.apache.jackrabbit.core.stats.util.CachingOpsPerSecondDto;

/**
 * Default {@link QueryStat} implementation
 * 
 */
public class QueryStatImpl implements QueryStat {

    private final static Comparator<QueryStatDto> comparator = new QueryStatDtoComparator();

    private int queueSize = 15;

    private PriorityQueue<QueryStatDto> queries = new PriorityQueue<QueryStatDto>(
            queueSize + 1, comparator);

    private CachingOpsPerSecondDto qps = new CachingOpsPerSecondDto();

    private boolean enabled = false;

    public QueryStatImpl() {
    }

    public int getSlowQueriesQueueSize() {
        return queueSize;
    }

    public synchronized void setSlowQueriesQueueSize(int size) {
        this.queueSize = size;
        this.queries = new PriorityQueue<QueryStatDto>(this.queueSize + 1,
                comparator);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.queries = new PriorityQueue<QueryStatDto>(this.queueSize + 1,
                comparator);
    }

    public synchronized void logQuery(final String language,
            final String statement, long duration) {
        if (!enabled) {
            return;
        }
        queries.add(new QueryStatDtoImpl(language, statement, duration));
        if (queries.size() > queueSize) {
            queries.remove();
        }
        qps.onOp(duration * 1000);
    }

    public void clearSlowQueriesQueue() {
        this.queries.clear();
    }

    public void reset() {
        clearSlowQueriesQueue();
    }

    public QueryStatDto[] getSlowQueries() {
        return queries.toArray(new QueryStatDto[queries.size()]);
    }

    public double getQueriesPerSecond() {
        return qps.getOpsPerSecond();
    }

    public double getAvgQueryTime() {
        return qps.getOpAvgTime();
    }
}
