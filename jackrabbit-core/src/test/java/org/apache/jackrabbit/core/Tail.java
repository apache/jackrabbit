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
package org.apache.jackrabbit.core;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.commons.iterator.FilterIterator;
import org.apache.jackrabbit.commons.predicate.Predicate;

/**
 * <code>Tail</code> is a test utility class to tail and grep a text file.
 */
public class Tail implements Closeable {

    private final String grep;

    private final BufferedReader reader;

    private Tail(File file, String grep) throws IOException {
        this.grep = grep;
        this.reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file)));
        while (reader.skip(Integer.MAX_VALUE) > 0) {
            // skip more, until end of file
        }
    }

    /**
     * Create a tail on the given <code>file</code> with an optional string to
     * match lines.
     *
     * @param file the file to tail.
     * @param grep the string to match or <code>null</code> if all lines should
     *             be returned.
     * @return a tail on the file.
     * @throws IOException if the files does not exist or some other I/O error
     *                     occurs.
     */
    public static Tail start(File file, String grep) throws IOException {
        return new Tail(file, grep);
    }

    /**
     * Returns the lines that were written to the file since
     * <code>Tail.start()</code> or the last call to <code>getLines()</code>.
     *
     * @return the matching lines.
     * @throws IOException if an error occurs while reading from the file.
     */
    public Iterable<String> getLines() throws IOException {
        return new Iterable<String>() {
            public Iterator<String> iterator() {
                Iterator<String> it = IOUtils.lineIterator(reader);
                if (grep == null || grep.length() == 0) {
                    return it;
                } else {
                    // filter
                    return new FilterIterator<String>(it, new Predicate() {
                        public boolean evaluate(Object o) {
                            return o.toString().contains(grep);
                        }
                    });
                }
            }
        };
    }

    /**
     * Releases the underlying stream from the file.
     *
     * @throws IOException If an I/O error occurs.
     */
    public void close() throws IOException {
        reader.close();
    }
}
