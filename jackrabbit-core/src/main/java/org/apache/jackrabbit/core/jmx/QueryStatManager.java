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
package org.apache.jackrabbit.core.jmx;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

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
import org.apache.jackrabbit.core.stats.QueryStatDtoComparator;

/**
 * The QueryStatManagerMBean default implementation
 * 
 */
public class QueryStatManager implements QueryStatManagerMBean {

    private final QueryStat queryStat;

    private final static Comparator<QueryStatDto> comparatorRev = Collections
            .reverseOrder(new QueryStatDtoComparator());

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

    public int getQueueSize() {
        return queryStat.getSlowQueriesQueueSize();
    }

    public void setQueueSize(int size) {
        this.queryStat.setSlowQueriesQueueSize(size);
    }

    public void clearQueue() {
        this.queryStat.clearSlowQueriesQueue();
    }

    public QueryStatDto[] getTopQueries() {
        QueryStatDto[] top = this.queryStat.getSlowQueries();
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

            TabularType tt = new TabularType(QueryStatDto.class.getName(),
                    "Query History", ct, QueryStatCompositeTypeFactory.index);
            tds = new TabularDataSupport(tt);

            for (QueryStatDto q : getTopQueries()) {
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

        public static Object[] getValues(QueryStatDto q) {
            return new Object[] { q.getPosition(), q.getDuration(),
                    q.getLanguage(), q.getStatement(), q.getCreationTime() };
        }
    }
}
