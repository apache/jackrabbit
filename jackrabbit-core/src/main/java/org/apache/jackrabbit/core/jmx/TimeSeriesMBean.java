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
package org.apache.jackrabbit.core.jmx;

/**
 * Interface for a time series of the number of events per
 * second, minute, hour and day. The type of the events is arbitrary; it
 * could be cache hits or misses, disk reads or writes, created sessions,
 * completed transactions, or pretty much anything of interest.
 *
 * @since Apache Jackrabbit 2.3.1
 */
public interface TimeSeriesMBean {

    /**
     * Returns the number of events per second over the last minute.
     *
     * @return number of events per second, in chronological order
     */
    long[] getEventsPerSecond();

    /**
     * Returns the number of events per minute over the last hour.
     *
     * @return number of events per minute, in chronological order
     */
    long[] getEventsPerMinute();

    /**
     * Returns the number of events per hour over the last week.
     *
     * @return number of events per hour, in chronological order
     */
    long[] getEventsPerHour();

    /**
     * Returns the number of events per week over the last three years.
     *
     * @return number of events per week, in chronological order
     */
    long[] getEventsPerWeek();

}
