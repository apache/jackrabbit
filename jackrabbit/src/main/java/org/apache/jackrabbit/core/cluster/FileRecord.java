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

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;

/**
 * Represents a file-based record.
 */
class FileRecord {

    /**
     * File record extension.
     */
    static final String EXTENSION = ".log";

    /**
     * Indicator for a literal UUID.
     */
    static final byte UUID_LITERAL = 0x00;

    /**
     * Indicator for a UUID index.
     */
    static final byte UUID_INDEX = 0x01;

    /**
     * Used for padding long string representations.
     */
    private static final String LONG_PADDING = "0000000000000000";

    /**
     * Underlying file.
     */
    private final File file;

    /**
     * Counter.
     */
    private final long counter;

    /**
     * Journal id.
     */
    private final String journalId;

    /**
     * Creates a new file record from an existing file. Retrieves meta data by parsing the file's name.
     *
     * @param file file to use as record
     * @throws IllegalArgumentException if file name is bogus
     */
    public FileRecord(File file) throws IllegalArgumentException {
        this.file = file;

        String name = file.getName();

        int sep1 = name.indexOf('.');
        if (sep1 == -1) {
            throw new IllegalArgumentException("Missing first . separator.");
        }
        try {
            counter = Long.parseLong(name.substring(0, sep1), 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unable to decompose long: " + e.getMessage());
        }
        int sep2 = name.lastIndexOf('.');
        if (sep2 == -1) {
            throw new IllegalArgumentException("Missing second . separator.");
        }
        journalId = name.substring(sep1 + 1, sep2);
    }

    /**
     * Creates a new file record from a counter and instance ID.
     *
     * @param parent parent directory
     * @param counter counter to use
     * @param journalId journal id to use
     */
    public FileRecord(File parent, long counter, String journalId) {
        StringBuffer name = new StringBuffer();
        name.append(toHexString(counter));
        name.append('.');
        name.append(journalId);

        name.append(EXTENSION);

        this.file = new File(parent, name.toString());
        this.counter = counter;
        this.journalId = journalId;
    }

    /**
     * Return the journal counter associated with this record.
     *
     * @return counter
     */
    public long getCounter() {
        return counter;
    }

    /**
     * Return the id of the journal that created this record.
     *
     * @return journal id
     */
    public String getJournalId() {
        return journalId;
    }

    /**
     * Return this record's file.
     *
     * @return file
     */
    public File getFile() {
        return file;
    }

    /**
     * Return a zero-padded long string representation.
     */
    public static String toHexString(long l) {
        String s = Long.toHexString(l);
        int padlen = LONG_PADDING.length() - s.length();
        if (padlen > 0) {
            s = LONG_PADDING.substring(0, padlen) + s;
        }
        return s;
    }
}