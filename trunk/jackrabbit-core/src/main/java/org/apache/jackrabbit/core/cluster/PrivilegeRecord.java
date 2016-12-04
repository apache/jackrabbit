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
import org.apache.jackrabbit.spi.PrivilegeDefinition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * <code>PrivilegeRecord</code>...
 */
public class PrivilegeRecord extends ClusterRecord {

    /**
     * Identifier: PRIVILEGES.
     */
    static final char IDENTIFIER = 'A';

    /**
     * Collection of privilege definitions.
     */
    private Collection<PrivilegeDefinition> definitions;

    protected PrivilegeRecord(Record record) {
        super(record);
    }

    protected PrivilegeRecord(Collection<PrivilegeDefinition> definitions, Record record) {
        super(record);

        this.definitions = definitions;
    }

    @Override
    protected void doRead() throws JournalException {
        int size = record.readInt();
        definitions = new HashSet<PrivilegeDefinition>();
        for (int i = 0; i < size; i++) {
            definitions.add(record.readPrivilegeDef());
        }
    }

    @Override
    protected void doWrite() throws JournalException {
        record.writeChar(IDENTIFIER);
        record.writeInt(definitions.size());

        for (PrivilegeDefinition def : definitions) {
            record.writePrivilegeDef(def);
        }
    }

    @Override
    public void process(ClusterRecordProcessor processor) {
        processor.process(this);
    }

    /**
     * Return the collection of privilege definitions.
     *
     * @return unmodifiable collection
     */
    public Collection<PrivilegeDefinition> getDefinitions() {
        return Collections.unmodifiableCollection(definitions);
    }
}