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

import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;



/**
 * The interface defines the backend which can be plugged into
 * {@link CachingDataStore}.
 */
public interface Backend {

    /**
     * This method initialize backend with the configuration.
     * 
     * @param store {@link CachingDataStore}
     * @param homeDir path of repository home dir.
     * @param config path of config property file.
     * @throws DataStoreException
     */
    void init(CachingDataStore store, String homeDir, String config)
            throws DataStoreException;

    /**
     * Return inputstream of record identified by identifier.
     * 
     * @param identifier identifier of record.
     * @return inputstream of the record.
     * @throws DataStoreException if record not found or any error.
     */
    InputStream read(DataIdentifier identifier) throws DataStoreException;

    /**
     * Return length of record identified by identifier.
     * 
     * @param identifier identifier of record.
     * @return length of the record.
     * @throws DataStoreException if record not found or any error.
     */
    long getLength(DataIdentifier identifier) throws DataStoreException;

    /**
     * Return lastModified of record identified by identifier.
     * 
     * @param identifier identifier of record.
     * @return lastModified of the record.
     * @throws DataStoreException if record not found or any error.
     */
    long getLastModified(DataIdentifier identifier) throws DataStoreException;

    /**
     * Stores file to backend with identifier used as key. If key pre-exists, it
     * updates the timestamp of the key.
     * 
     * @param identifier key of the file 
     * @param file file that would be stored in backend.
     * @throws DataStoreException for any error.
     */
    void write(DataIdentifier identifier, File file) throws DataStoreException;

    /**
     * Returns identifiers of all records that exists in backend. 
     * @return iterator consisting of all identifiers
     * @throws DataStoreException
     */
    Iterator<DataIdentifier> getAllIdentifiers() throws DataStoreException;

    /**
     * Update timestamp of record identified by identifier if minModifiedDate is
     * greater than record's lastModified else no op.
     * 
     * @throws DataStoreException if record not found.
     */
    void touch(DataIdentifier identifier, long minModifiedDate)
            throws DataStoreException;
    /**
     * This method check the existence of record in backend. 
     * @param identifier identifier to be checked. 
     * @return true if records exists else false.
     * @throws DataStoreException
     */
    boolean exists(DataIdentifier identifier) throws DataStoreException;

    /**
     * Close backend and release resources like database connection if any.
     * @throws DataStoreException
     */
    void close() throws DataStoreException;

    /**
     * Delete all records which are older than timestamp.
     * @param timestamp
     * @return list of identifiers which are deleted. 
     * @throws DataStoreException
     */
    List<DataIdentifier> deleteAllOlderThan(long timestamp) throws DataStoreException;

    /**
     * Delete record identified by identifier. No-op if identifier not found.
     * @param identifier
     * @throws DataStoreException
     */
    void deleteRecord(DataIdentifier identifier) throws DataStoreException;
}
