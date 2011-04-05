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
package org.apache.jackrabbit.core.jmx.query;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

/**
 * Default {@link QueryStatManager} implementation
 * 
 */
public class QueryStatManagerImpl implements QueryStatManagerImplMBean,
        QueryStatManager {

    private final static Comparator<QueryStat> comparator = new QueryStatComp();

    private final static Comparator<QueryStat> comparatorRev = Collections
            .reverseOrder(comparator);

    private int queueSize = 15;

    private PriorityQueue<QueryStat> queries = new PriorityQueue<QueryStat>(
            queueSize + 1, new QueryStatComp());

    private boolean enabled;

    /**
     * Default constructor. This will disable the monitoring service
     */
    public QueryStatManagerImpl() {
        this(false);
    }

    /**
     * Constructor allowing the enabling/disabling of the service
     * 
     * @param isEnabled
     */
    public QueryStatManagerImpl(boolean isEnabled) {
        this.enabled = isEnabled;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public synchronized void setQueueSize(int size) {
        this.queueSize = size;
        this.queries = new PriorityQueue<QueryStat>(this.queueSize + 1,
                comparator);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.queries = new PriorityQueue<QueryStat>(this.queueSize + 1,
                comparator);
    }

    public QueryStat[] getTopQueries() {
        // TODO cache this call
        QueryStat[] top = queries.toArray(new QueryStat[queries.size()]);
        Arrays.sort(top, comparatorRev);
        for (int i = 0; i < top.length; i++) {
            top[i].setPosition(i + 1);
        }
        return top;
    }

    public TabularData getQueries() {
        TabularDataSupport tds = null;
        try {
            CompositeType ct = QueryStatCompositeTypeFactory.getCompositeType();

            TabularType tt = new TabularType(QueryStat.class.getName(),
                    "Query History", ct, QueryStatCompositeTypeFactory.index);
            tds = new TabularDataSupport(tt);

            for (QueryStat q : getTopQueries()) {
                tds.put(new CompositeDataSupport(ct,
                        QueryStatCompositeTypeFactory.names,
                        QueryStatCompositeTypeFactory.getValues(q)));
            }
            return tds;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public synchronized void logQuery(QueryStat stat) {
        if (!enabled) {
            return;
        }
        queries.add(stat);
        if (queries.size() > queueSize) {
            queries.remove();
        }
    }

    private static class QueryStatComp implements Comparator<QueryStat> {

        public int compare(QueryStat o1, QueryStat o2) {
            return new Long(o1.getDuration()).compareTo(o2.getDuration());
        }
    }

    private static class QueryStatCompositeTypeFactory {

        private final static String[] index = { "position" };

        private final static String[] names = { "position", "duration",
                "language", "statement", "creationTime" };

        private final static String[] descriptions = { "position", "duration",
                "language", "statement", "creationTime" };

        private final static OpenType[] types = { SimpleType.LONG,
                SimpleType.LONG, SimpleType.STRING, SimpleType.STRING,
                SimpleType.STRING };

        public static CompositeType getCompositeType() throws OpenDataException {
            return new CompositeType(QueryStat.class.getName(),
                    QueryStat.class.getName(), names, descriptions, types);
        }

        public static Object[] getValues(QueryStat q) {
            return new Object[] { q.getPosition(), q.getDuration(),
                    q.getLanguage(), q.getStatement(), q.getCreationTime() };
        }
    }

    public void clearQueue() {
        this.queries.clear();
    }
}
