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

package org.apache.jackrabbit.aws.ext.ds;

import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStoreException;

/**
 * A data store backend to support caching data stores.
 */
public interface Backend {

    /**
     * initialize backend
     *
     * @param store
     * @param homeDir
     * @param config
     * @throws DataStoreException
     */
    void init(CachingDataStore store, String homeDir, String config) throws DataStoreException;

    /**
     * Return inputstream of the record
     *
     * @param identifier
     * @return
     * @throws DataStoreException
     */
    InputStream read(DataIdentifier identifier) throws DataStoreException;

    /**
     * Return the length of the record
     *
     * @param identifier
     * @return
     * @throws DataStoreException
     */
    long getLength(DataIdentifier identifier) throws DataStoreException;

    /**
     * Return lastModified of the record
     */
    long getLastModified(DataIdentifier identifier) throws DataStoreException;

    /**
     * Write file to the backend.
     *
     * @param identifier
     * @param file
     * @throws DataStoreException
     */
    void write(DataIdentifier identifier, File file) throws DataStoreException;

    /**
     * Return all identifiers which exist in backend.
     *
     * @return
     * @throws DataStoreException
     */
    Iterator<DataIdentifier> getAllIdentifiers() throws DataStoreException;

    /**
     * Touch record identified by identifier if minModifiedDate > record's lastModified.
     * @param identifier
     * @throws DataStoreException
     */
    void touch(DataIdentifier identifier, long minModifiedDate) throws DataStoreException;

    /**
     * Return true if records exists else false.
     *
     * @param identifier
     * @return
     * @throws DataStoreException
     */
    boolean exists(DataIdentifier identifier) throws DataStoreException;

    /**
     * Close backend
     *
     * @throws DataStoreException
     */
    void close() throws DataStoreException;

    /**
     * Delete all records which are older than min
     *
     * @param min
     * @return
     * @throws DataStoreException
     */
    List<DataIdentifier> deleteAllOlderThan(long min) throws DataStoreException;

    /**
     * Delete record identified by identifier. No-op if identifier not found.
     *
     * @param identifier
     * @throws DataStoreException
     */
    void deleteRecord(DataIdentifier identifier) throws DataStoreException;
}
