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

import java.util.Calendar;
import java.util.Date;

import org.apache.jackrabbit.api.stats.QueryStatDto;

/**
 * Object that holds statistical info about a query.
 * 
 */
public class QueryStatDtoImpl implements QueryStatDto {

    private static final long serialVersionUID = 1L;

    /**
     * lazy, computed at call time
     */
    private long position;

    /**
     * the time that the query was created
     */
    private final Date creationTime;

    /**
     * run duration
     */
    private final long duration;

    /**
     * query language
     */
    private final String language;

    /**
     * query statement
     */
    private final String statement;

    public QueryStatDtoImpl(final String language, final String statement,
            long duration) {
        this.duration = duration;
        this.language = language;
        this.statement = statement;

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis() - duration);
        this.creationTime = c.getTime();
    }

    public long getDuration() {
        return duration;
    }

    public String getLanguage() {
        return language;
    }

    public String getStatement() {
        return statement;
    }

    public String getCreationTime() {
        return creationTime.toString();
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "QueryStat [creationTime=" + creationTime + ", duration="
                + duration + ", language=" + language + ", statement="
                + statement + "]";
    }
}
