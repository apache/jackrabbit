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

import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.Record;

/**
 * Deserialize a record written by a <code>ClusterNode</code>.
 */
public class ClusterRecordDeserializer {

    /**
     * Deserialize a cluster record.
     *
     * @param record basic record containing a cluster record
     * @return deserialized cluster record
     * @throws JournalException if an error occurs
     */
    public ClusterRecord deserialize(Record record) throws JournalException {
        ClusterRecord clusterRecord;

        String workspace = record.readString();
        char c = record.readChar();
        switch (c) {
        case ChangeLogRecord.NODE_IDENTIFIER:
        case ChangeLogRecord.PROPERTY_IDENTIFIER:
        case ChangeLogRecord.EVENT_IDENTIFIER:
        case ChangeLogRecord.DATE_IDENTIFIER:
            clusterRecord = new ChangeLogRecord(c, record, workspace);
            clusterRecord.read();
            break;
        case LockRecord.IDENTIFIER:
            clusterRecord = new LockRecord(record, workspace);
            clusterRecord.read();
            break;
        case NamespaceRecord.IDENTIFIER:
            clusterRecord = new NamespaceRecord(record);
            clusterRecord.read();
            break;
        case NodeTypeRecord.IDENTIFIER:
            clusterRecord = new NodeTypeRecord(record);
            clusterRecord.read();
            break;
        case WorkspaceRecord.IDENTIFIER:
            clusterRecord = new WorkspaceRecord(record);
            clusterRecord.read();
            break;
        case PrivilegeRecord.IDENTIFIER:
            clusterRecord = new PrivilegeRecord(record);
            clusterRecord.read();
            break;
        case ClusterRecord.END_MARKER:
            // JCR-1813: Invalid journal records during XATransactions
            // Some journal records may be empty due to JCR-1813 and other
            // issues. We handle such cases with this dummy sentinel record.
            clusterRecord = new ClusterRecord(record) {
                @Override
                protected void doRead() {
                }
                @Override
                protected void doWrite() {
                }
                @Override
                public void process(ClusterRecordProcessor processor) {
                }
            };
            break;
        default:
            throw new JournalException("Unknown record identifier: " + c);
        }
        return clusterRecord;
    }
}
