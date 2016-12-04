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
package org.apache.jackrabbit.stats;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.jackrabbit.api.stats.QueryStatDto;

/**
 * Default {@link QueryStatCore} implementation
 * 
 */
public class QueryStatImpl implements QueryStatCore {

    private final static Comparator<QueryStatDto> comparator = new QueryStatDtoComparator();

    private final BoundedPriorityBlockingQueue<QueryStatDto> slowQueries = new BoundedPriorityBlockingQueue<QueryStatDto>(
            15, comparator);

    private final static Comparator<QueryStatDtoImpl> comparatorOccurrence = new QueryStatDtoOccurrenceComparator();

    /**
     * the real queue size will be bigger than the desired number of popular
     * queries by POPULAR_QUEUE_MULTIPLIER times
     */
    private static final int POPULAR_QUEUE_MULTIPLIER = 5;

    private final BoundedPriorityBlockingQueue<QueryStatDtoImpl> popularQueries = new BoundedPriorityBlockingQueue<QueryStatDtoImpl>(
            15 * POPULAR_QUEUE_MULTIPLIER, comparatorOccurrence);

    private static final class BoundedPriorityBlockingQueue<E> extends
            PriorityBlockingQueue<E> {

        private static final long serialVersionUID = 1L;
        private int maxSize;

        public BoundedPriorityBlockingQueue(int maxSize,
                Comparator<? super E> comparator) {
            super(maxSize + 1, comparator);
            this.maxSize = maxSize;
        }

        @Override
        public boolean offer(E e) {
            boolean s = super.offer(e);
            if (!s) {
                return false;
            }
            if (size() > maxSize) {
                poll();
            }
            return true;
        }

        public synchronized void setMaxSize(int maxSize) {
            if (maxSize < this.maxSize) {
                // shrink the queue
                int delta = super.size() - maxSize;
                for (int i = 0; i < delta; i++) {
                    E t = poll();
                    if (t == null) {
                        break;
                    }
                }
            }
            this.maxSize = maxSize;
        }

        public int getMaxSize() {
            return maxSize;
        }
    }

    private boolean enabled = false;

    public QueryStatImpl() {
    }

    public int getSlowQueriesQueueSize() {
        return slowQueries.getMaxSize();
    }

    public void setSlowQueriesQueueSize(int size) {
        slowQueries.setMaxSize(size);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void logQuery(final String language, final String statement,
            long durationMs) {
        if (!enabled) {
            return;
        }
        final QueryStatDtoImpl qs = new QueryStatDtoImpl(language, statement,
                durationMs);
        slowQueries.offer(qs);

        synchronized (popularQueries) {
            Iterator<QueryStatDtoImpl> iterator = popularQueries.iterator();
            while (iterator.hasNext()) {
                QueryStatDtoImpl qsdi = iterator.next();
                if (qsdi.equals(qs)) {
                    qs.setOccurrenceCount(qsdi.getOccurrenceCount() + 1);
                    iterator.remove();
                    break;
                }
            }
            popularQueries.offer(qs);
        }
    }

    public void clearSlowQueriesQueue() {
        slowQueries.clear();
    }

    public QueryStatDto[] getSlowQueries() {
        QueryStatDto[] top = slowQueries.toArray(new QueryStatDto[slowQueries
                .size()]);
        Arrays.sort(top, Collections.reverseOrder(comparator));
        for (int i = 0; i < top.length; i++) {
            top[i].setPosition(i + 1);
        }
        return top;
    }

    public QueryStatDto[] getPopularQueries() {
        QueryStatDtoImpl[] top;
        int size = 0;
        int maxSize = 0;
        synchronized (popularQueries) {
            top = popularQueries.toArray(new QueryStatDtoImpl[popularQueries
                    .size()]);
            size = popularQueries.size();
            maxSize = popularQueries.getMaxSize();
        }
        Arrays.sort(top, Collections.reverseOrder(comparatorOccurrence));
        int retSize = Math.min(size, maxSize / POPULAR_QUEUE_MULTIPLIER);
        QueryStatDto[] retval = new QueryStatDto[retSize];
        for (int i = 0; i < retSize; i++) {
            retval[i] = top[i];
            retval[i].setPosition(i + 1);
        }
        return retval;
    }

    public int getPopularQueriesQueueSize() {
        return popularQueries.getMaxSize() / POPULAR_QUEUE_MULTIPLIER;
    }

    public void setPopularQueriesQueueSize(int size) {
        popularQueries.setMaxSize(size * POPULAR_QUEUE_MULTIPLIER);
    }

    public void clearPopularQueriesQueue() {
        popularQueries.clear();
    }

    public void reset() {
        clearSlowQueriesQueue();
        clearPopularQueriesQueue();
    }
}
