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
package org.apache.jackrabbit.core.journal;

import java.util.NoSuchElementException;

/**
 * RecordIterator interface.
 */
public interface RecordIterator {

    /**
     * Return a flag indicating whether there are more records.
     *
     * @return <code>true</code> if there are more records;
     *         <code>false</code> otherwise
     */
    boolean hasNext();

    /**
     * Return the next record. If there are no more records, throws
     * a <code>NoSuchElementException</code>. If an error occurs,
     * throws a <code>JournalException</code>.
     *
     * @return next record
     * @throws NoSuchElementException if there are no more records
     * @throws JournalException if another error occurs
     */
    Record nextRecord() throws NoSuchElementException, JournalException;

    /**
     * Close this iterator. Releases all associated resources.
     */
    void close();

}
