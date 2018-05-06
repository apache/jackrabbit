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
package org.apache.jackrabbit.core.persistence.check;

import java.util.Set;

public class ConsistencyReportImpl implements ConsistencyReport {

    private final int nodeCount;
    private final long elapsedTimeMs;
    private final Set<ReportItem> reports;

    public ConsistencyReportImpl(int nodeCount, long elapsedTimeMs,
            Set<ReportItem> reports) {
        this.nodeCount = nodeCount;
        this.elapsedTimeMs = elapsedTimeMs;
        this.reports = reports;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public long getElapsedTimeMs() {
        return elapsedTimeMs;
    }

    public Set<ReportItem> getItems() {
        return reports;
    }

    @Override
    public String toString() {
        return "elapsedTimeMs " + elapsedTimeMs + ", nodeCount " + nodeCount
                + ", reports: " + reports;
    }
}
