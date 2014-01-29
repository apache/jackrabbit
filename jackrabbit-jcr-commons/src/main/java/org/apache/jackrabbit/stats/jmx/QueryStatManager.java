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
package org.apache.jackrabbit.stats.jmx;

import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.apache.jackrabbit.api.jmx.QueryStatManagerMBean;
import org.apache.jackrabbit.api.stats.QueryStat;
import org.apache.jackrabbit.api.stats.QueryStatDto;

/**
 * The QueryStatManagerMBean default implementation
 * 
 */
public class QueryStatManager implements QueryStatManagerMBean {

    private final QueryStat queryStat;

    public QueryStatManager(final QueryStat queryStat) {
        this.queryStat = queryStat;
    }

    public boolean isEnabled() {
        return this.queryStat.isEnabled();
    }

    public void enable() {
        this.queryStat.setEnabled(true);
    }

    public void disable() {
        this.queryStat.setEnabled(false);
    }

    public void reset() {
        this.queryStat.reset();
    }

    public int getSlowQueriesQueueSize() {
        return queryStat.getSlowQueriesQueueSize();
    }

    public void setSlowQueriesQueueSize(int size) {
        this.queryStat.setSlowQueriesQueueSize(size);
    }

    public void clearSlowQueriesQueue() {
        this.queryStat.clearSlowQueriesQueue();
    }

    public int getPopularQueriesQueueSize() {
        return queryStat.getPopularQueriesQueueSize();
    }

    public void setPopularQueriesQueueSize(int size) {
        queryStat.setPopularQueriesQueueSize(size);
    }

    public void clearPopularQueriesQueue() {
        queryStat.clearPopularQueriesQueue();
    }

    public TabularData getSlowQueries() {
        return asTabularData(queryStat.getSlowQueries());
    }

    public TabularData getPopularQueries() {
        return asTabularData(queryStat.getPopularQueries());
    }

    private TabularData asTabularData(QueryStatDto[] data) {
        TabularDataSupport tds = null;
        try {
            CompositeType ct = QueryStatCompositeTypeFactory.getCompositeType();

            TabularType tt = new TabularType(QueryStatDto.class.getName(),
                    "Query History", ct, QueryStatCompositeTypeFactory.index);
            tds = new TabularDataSupport(tt);

            for (QueryStatDto q : data) {
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

    private static class QueryStatCompositeTypeFactory {

        private final static String[] index = { "position" };

        private final static String[] names = { "position", "duration",
                "occurrenceCount", "language", "statement", "creationTime" };

        private final static String[] descriptions = { "position", "duration",
                "occurrenceCount", "language", "statement", "creationTime" };

        private final static OpenType[] types = { SimpleType.LONG,
                SimpleType.LONG, SimpleType.INTEGER, SimpleType.STRING,
                SimpleType.STRING, SimpleType.STRING };

        public static CompositeType getCompositeType() throws OpenDataException {
            return new CompositeType(QueryStat.class.getName(),
                    QueryStat.class.getName(), names, descriptions, types);
        }

        public static Object[] getValues(QueryStatDto q) {
            return new Object[] { q.getPosition(), q.getDuration(),
                    q.getOccurrenceCount(), q.getLanguage(), q.getStatement(),
                    q.getCreationTime() };
        }
    }

}
