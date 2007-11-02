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
package org.apache.jackrabbit.core.data;

import java.io.InputStream;
import java.util.Iterator;

/**
 * Append-only store for binary streams. A data store consists of a number
 * of identifiable data records that each contain a distinct binary stream.
 * New binary streams can be added to the data store, but existing streams
 * are never removed or modified.
 * <p>
 * A data store should be fully thread-safe, i.e. it should be possible to
 * add and access data records concurrently. Optimally even separate processes
 * should be able to concurrently access the data store with zero interprocess
 * synchronization.
 */
public interface DataStore {

    /**
     * Returns the identified data record. The given identifier should be
     * the identifier of a previously saved data record. Since records are
     * never removed, there should never be cases where the identified record
     * is not found. Abnormal cases like that are treated as errors and
     * handled by throwing an exception.
     *
     * @param identifier data identifier
     * @return identified data record
     * @throws DataStoreException if the data store could not be accessed,
     *                     or if the given identifier is invalid
     */
    DataRecord getRecord(DataIdentifier identifier) throws DataStoreException;

    /**
     * Creates a new data record. The given binary stream is consumed and
     * a binary record containing the consumed stream is created and returned.
     * If the same stream already exists in another record, then that record
     * is returned instead of creating a new one.
     * <p>
     * The given stream is consumed and <strong>not closed</strong> by this
     * method. It is the responsibility of the caller to close the stream.
     * A typical call pattern would be:
     * <pre>
     *     InputStream stream = ...;
     *     try {
     *         record = store.addRecord(stream);
     *     } finally {
     *         stream.close();
     *     }
     * </pre>
     *
     * @param stream binary stream
     * @return data record that contains the given stream
     * @throws DataStoreException if the data store could not be accessed
     */
    DataRecord addRecord(InputStream stream) throws DataStoreException;

    /**
     * From now on, update the modified date of an object even when reading from it.
     * Usually, the modified date is only updated when creating a new object, 
     * or when a new link is added to an existing object.
     * 
     * @param before - update the modified date to the current time if it is older than this value
     */
    void updateModifiedDateOnRead(long before);

    /**
     * Delete objects that have a modified date older than the specified date.
     * 
     * @param min
     * @return the number of data records deleted
     */
    int deleteAllOlderThan(long min);
    
    /**
     * Get all identifiers.
     */
    Iterator getAllIdentifiers();
    
    /**
     * Initialized the data store
     * 
     * @param homeDir the home directory of the repository
     */
    void init(String homeDir);

    /**
     * Get the minimum size of an object that should be stored in this data store.
     * Depending on the overhead and configuration, each store may return a different value.
     * 
     * @return the minimum size
     */
    int getMinRecordLength();

}
