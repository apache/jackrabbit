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
package org.apache.jackrabbit.core.cluster;

/**
 * Cluster record processor. Pass an implementation of this interface to a
 * <code>ClusterRecord</code> and it will call back the appropriate
 * <code>process</code> method.
 *
 * @see ClusterRecord#process(ClusterRecordProcessor)
 */
public interface ClusterRecordProcessor {

    /**
     * Process a change log record.
     *
     * @param record change log record
     */
    void process(ChangeLogRecord record);

    /**
     * Process a lock record.
     *
     * @param record lock record
     */
    void process(LockRecord record);

    /**
     * Process a namespace record.
     *
     * @param record namespace record
     */
    void process(NamespaceRecord record);

    /**
     * Process a node type record
     *
     * @param record node type record
     */
    void process(NodeTypeRecord record);

    /**
     * Process a privilege record
     *
     * @param record privilege record
     */
    void process(PrivilegeRecord record);

    /**
     * Process a workspace record
     * @param record workspace record
     */
    void process(WorkspaceRecord record);

}
