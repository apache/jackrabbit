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
 * Cluster record representing a namespace registration, reregistration or
 * unregistration.
 */
public class NamespaceRecord extends ClusterRecord {

    /**
     * Identifier: NAMESPACE.
     */
    static final char IDENTIFIER = 'S';

    /**
     * Old prefix.
     */
    private String oldPrefix;

    /**
     * New prefix.
     */
    private String newPrefix;

    /**
     * URI.
     */
    private String uri;

    /**
     * Create a new instance of this class. Used when serializing a namespace
     * operation.
     *
     * @param oldPrefix old prefix
     * @param newPrefix new prefix
     * @param uri URI
     * @param record journal record
     */
    public NamespaceRecord(String oldPrefix, String newPrefix, String uri,
                           Record record) {
        super(record);

        this.oldPrefix = oldPrefix;
        this.newPrefix = newPrefix;
        this.uri = uri;
    }

    /**
     * Create a new instance of this class. Used when deserializing.
     *
     * @param record journal record
     */
    NamespaceRecord(Record record) {
        super(record);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doRead() throws JournalException {
        oldPrefix = record.readString();
        newPrefix = record.readString();
        uri = record.readString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doWrite() throws JournalException {
        record.writeChar(IDENTIFIER);
        record.writeString(oldPrefix);
        record.writeString(newPrefix);
        record.writeString(uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(ClusterRecordProcessor processor) {
        processor.process(this);
    }

    /**
     * Return the old prefix.
     *
     * @return old prefix
     */
    public String getOldPrefix() {
        return oldPrefix;
    }

    /**
     * Return the new prefix.
     *
     * @return new prefix
     */
    public String getNewPrefix() {
        return newPrefix;
    }

    /**
     * Return the URI.
     * @return URI
     */
    public String getUri() {
        return uri;
    }
}
