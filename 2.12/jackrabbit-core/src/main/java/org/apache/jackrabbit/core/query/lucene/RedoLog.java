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
package org.apache.jackrabbit.core.query.lucene;

import java.io.IOException;
import java.util.List;

/**
 * Defines a redo log for changes that have not been committed to disk. While
 * nodes are added to and removed from the volatile index (held in memory) a
 * redo log is maintained to keep track of the changes. In case the Jackrabbit
 * process terminates unexpected the redo log is applied when Jackrabbit is
 * restarted the next time.
 */
public interface RedoLog {

    /**
     * Returns <code>true</code> if this redo log contains any entries,
     * <code>false</code> otherwise.
     * @return <code>true</code> if this redo log contains any entries,
     * <code>false</code> otherwise.
     */
    boolean hasEntries();

    /**
     * Returns the number of entries in this redo log.
     * @return the number of entries in this redo log.
     */
    int getSize();

    /**
     * Returns a List with all {@link MultiIndex.Action} instances in the
     * redo log.
     *
     * @return an List with all {@link MultiIndex.Action} instances in the
     *         redo log.
     * @throws IOException if an error occurs while reading from the redo log.
     */
    List<MultiIndex.Action> getActions() throws IOException;

    /**
     * Appends an action to the log.
     *
     * @param action the action to append.
     * @throws IOException if the node cannot be written to the redo
     * log.
     */
    void append(MultiIndex.Action action) throws IOException;

    /**
     * Flushes all pending writes to the redo log.
     *
     * @throws IOException if an error occurs while writing.
     */
    void flush() throws IOException;

    /**
     * Flushes all pending writes to the redo log and closes it.
     *
     * @throws IOException if an error occurs while writing.
     */
    void close() throws IOException;
}
