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

import org.apache.jackrabbit.core.journal.AppendRecord;
import org.apache.jackrabbit.core.journal.DefaultRecordProducer;
import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.MemoryJournal;
import org.apache.jackrabbit.core.journal.RecordProducer;

/**
* <code>TestJournal</code> extends the MemoryJournal with a static hook to
* refuse lock acquisition.
*/
public final class TestJournal extends MemoryJournal {

    static boolean refuseLock = false;

    static boolean failRecordWrite = false;

    @Override
    protected void doLock() throws JournalException {
        if (refuseLock) {
            throw new JournalException("lock refused");
        } else {
            super.doLock();
        }
    }

    @Override
    protected RecordProducer createProducer(final String identifier) {
        return new DefaultRecordProducer(this, identifier) {
            @Override
            protected AppendRecord createRecord() throws JournalException {
                return new AppendRecord(TestJournal.this, identifier) {
                    @Override
                    public void writeString(String s) throws JournalException {
                        if (failRecordWrite) {
                            throw new JournalException("write failed");
                        } else {
                            super.writeString(s);
                        }
                    }
                };
            }
        };
    }
}
