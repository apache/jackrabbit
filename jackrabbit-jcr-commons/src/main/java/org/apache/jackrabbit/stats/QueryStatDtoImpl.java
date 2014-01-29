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
     * run duration in ms
     */
    private final long durationMs;

    /**
     * query language
     */
    private final String language;

    /**
     * query statement
     */
    private final String statement;

    /**
     * used in popular queries list
     */
    private int occurrenceCount = 1;

    public QueryStatDtoImpl(final String language, final String statement,
            long durationMs) {
        this.durationMs = durationMs;
        this.language = language;
        this.statement = statement;

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis() - durationMs);
        this.creationTime = c.getTime();
    }

    public long getDuration() {
        return durationMs;
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
                + durationMs + ", position " + position + ", language="
                + language + ", statement=" + statement + "]";
    }

    public int getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(int occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((language == null) ? 0 : language.hashCode());
        result = prime * result
                + ((statement == null) ? 0 : statement.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        QueryStatDtoImpl other = (QueryStatDtoImpl) obj;
        if (language == null) {
            if (other.language != null)
                return false;
        } else if (!language.equals(other.language))
            return false;
        if (statement == null) {
            if (other.statement != null)
                return false;
        } else if (!statement.equals(other.statement))
            return false;
        return true;
    }

}
