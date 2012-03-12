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
package org.apache.jackrabbit.api.stats;


/**
 * Statistics on core repository operations
 * 
 */
public interface RepositoryStatistics {

    enum Type {
        BUNDLE_READ_COUNTER(true),
        BUNDLE_WRITE_COUNTER(true),
        BUNDLE_WRITE_DURATION(true),
        BUNDLE_WRITE_AVERAGE(false),
        BUNDLE_CACHE_ACCESS_COUNTER(true),
        BUNDLE_CACHE_SIZE_COUNTER(true),
        BUNDLE_CACHE_MISS_COUNTER(true),
        BUNDLE_CACHE_MISS_DURATION(true),
        BUNDLE_CACHE_MISS_AVERAGE(false),
        BUNDLE_COUNTER(true),
        BUNDLE_WS_SIZE_COUNTER(true),
        SESSION_READ_COUNTER(true),
        SESSION_READ_DURATION(true),
        SESSION_READ_AVERAGE(false),
        SESSION_WRITE_COUNTER(true),
        SESSION_WRITE_DURATION(true),
        SESSION_WRITE_AVERAGE(false),
        SESSION_LOGIN_COUNTER(true),
        SESSION_COUNT(false),
        QUERY_COUNT(true),
        QUERY_DURATION(true),
        QUERY_AVERAGE(true);

        private final boolean resetValueEachSecond;

        Type(final boolean resetValueEachSecond) {
            this.resetValueEachSecond = resetValueEachSecond;
        }

        public static Type getType(String type) {
            Type realType = null;
            try {
                realType = Type.valueOf(type);
            } catch (IllegalArgumentException ignore) {};
            return realType;
        }

        public boolean isResetValueEachSecond() {
            return resetValueEachSecond;
        }
    }

    TimeSeries getTimeSeries(Type type);

    TimeSeries getTimeSeries(String type, boolean resetValueEachSecond);
}
