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

import org.apache.jackrabbit.api.jmx.CoreStatManagerMBean;
import org.apache.jackrabbit.api.stats.CoreStat;

/**
 * The CoreStatManagerMBean default implementation
 * 
 */
public class CoreStatManager implements CoreStatManagerMBean {

    private final CoreStat coreStat;

    public CoreStatManager(final CoreStat coreStat) {
        this.coreStat = coreStat;
    }

    public long getNumberOfSessions() {
        return coreStat.getNumberOfSessions();
    }

    public void resetNumberOfSessions() {
        this.coreStat.resetNumberOfSessions();
    }

    public boolean isEnabled() {
        return this.coreStat.isEnabled();
    }

    public void reset() {
        this.coreStat.reset();
    }

    public double getReadOpsPerSecond() {
        return this.coreStat.getReadOpsPerSecond();
    }

    public double getWriteOpsPerSecond() {
        return this.coreStat.getWriteOpsPerSecond();
    }

    public void resetNumberOfOperations() {
        this.coreStat.resetNumberOfOperations();

    }

    public void enable() {
        this.coreStat.setEnabled(true);
    }

    public void disable() {
        this.coreStat.setEnabled(false);
    }
}
