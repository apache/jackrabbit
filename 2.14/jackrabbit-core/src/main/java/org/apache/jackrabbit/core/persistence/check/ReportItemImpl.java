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

public class ReportItemImpl implements ReportItem {

    private final String nodeId;
    private final String message;
    private final Type type;
    private final boolean repaired;

    public ReportItemImpl(String nodeId, String message, Type type, boolean repaired) {
        this.nodeId = nodeId;
        this.message = message;
        this.type = type;
        this.repaired = repaired;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean isRepaired() {
        return repaired;
    }

    @Override
    public String toString() {
        return type + ": " + nodeId + " -- " + message;
    }
}
