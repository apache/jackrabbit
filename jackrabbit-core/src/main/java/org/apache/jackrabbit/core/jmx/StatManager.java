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

import org.apache.jackrabbit.core.jmx.core.CoreStat;
import org.apache.jackrabbit.core.jmx.core.CoreStatImpl;
import org.apache.jackrabbit.core.jmx.query.QueryStat;
import org.apache.jackrabbit.core.jmx.query.QueryStatImpl;
import org.apache.jackrabbit.core.jmx.registry.JmxRegistry;
import org.apache.jackrabbit.core.jmx.registry.JmxRegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StatManager represents a single entry point to the stats objects available.<br>
 * 
 * It is disabled by default, and it can be enabled via a system property called
 * "<code>enableJmxSupport</code>", like:<br>
 * 
 * <code>-DenableJmxSupport=true</code>
 * 
 */
public class StatManager {

    private static final Logger log = LoggerFactory
            .getLogger(StatManager.class);

    private static final Boolean DEFAULT_JMX_SUPPORT = Boolean.valueOf(System
            .getProperty("enableJmxSupport", "false"));

    private JmxRegistry jmxRegistry;

    private final boolean enableJmxSupport;

    /* STATS */
    private final QueryStat queryStat = new QueryStatImpl();

    private final CoreStat coreStat = new CoreStatImpl();

    public StatManager(final boolean enableJmxSupport) {
        this.enableJmxSupport = enableJmxSupport;
    }

    public StatManager() {
        this(DEFAULT_JMX_SUPPORT);
    }

    public void init() {
        coreStat.setEnabled(false);
        queryStat.setEnabled(false);
        if (enableJmxSupport) {
            enableJxmRegistry();
        }
        log.debug("Started StatManager. Jmx support enabled {}.",
                enableJmxSupport);
    }

    protected void enableJxmRegistry() {
        if (jmxRegistry != null) {
            return;
        }
        jmxRegistry = new JmxRegistryImpl(this);
        jmxRegistry.start();
    }

    public void stop() {
        if (jmxRegistry != null) {
            jmxRegistry.stop();
        }
    }

    public CoreStat getCoreStat() {
        return coreStat;
    }

    public QueryStat getQueryStat() {
        return queryStat;
    }
}
