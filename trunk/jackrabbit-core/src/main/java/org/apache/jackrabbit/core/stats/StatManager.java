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

import static java.lang.Boolean.getBoolean;

import org.apache.jackrabbit.stats.QueryStatCore;
import org.apache.jackrabbit.stats.QueryStatImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StatManager represents a single entry point to the statistics objects
 * available.<br>
 * 
 */
public class StatManager {

    public static String ALL_STATS_ENABLED_PROPERTY = "org.apache.jackrabbit.api.stats.ALL";
    public static String QUERY_STATS_ENABLED_PROPERTY = "org.apache.jackrabbit.api.stats.QueryStat";

    private static final Logger log = LoggerFactory
            .getLogger(StatManager.class);

    /* STAT OBJECTS */
    private final QueryStatCore queryStat = new QueryStatImpl();

    public StatManager() {
        init();
    }

    protected void init() {
        boolean allEnabled = getBoolean(ALL_STATS_ENABLED_PROPERTY);
        queryStat.setEnabled(allEnabled
                || getBoolean(QUERY_STATS_ENABLED_PROPERTY));
        log.debug(
                "Started StatManager. QueryStat is enabled {}",
                new Object[] { queryStat.isEnabled() });
    }

    public QueryStatCore getQueryStat() {
        return queryStat;
    }

}
